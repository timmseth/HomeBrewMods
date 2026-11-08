package com.seth.worldbound.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * An ordered stack of tile sources. Every lookup tries the finest layer that covers the point
 * and falls back to coarser ones on a missing tile or a NODATA pixel, so a query never fails
 * as long as something coarse covers the planet (the jar base does).
 *
 * Thread-safe: chunk generation calls this from many worker threads.
 */
public final class RasterStack {
    /** One source at one zoom for one layer. A pack contributes one Spec per layer it declares. */
    public record Spec(int layer, int zoom, TileSource source, double west, double south, double east, double north) {
        boolean covers(double lon, double lat) {
            return lon >= west && lon < east && (lat > south || (south <= -90 && lat >= south)) && lat <= north;
        }
    }

    private static final Tile MISSING = new Tile(0, 0, 0, 0, 0, 0, 0, new short[0]);

    private final List<Spec> elev = new ArrayList<>();
    private final List<Spec> climate = new ArrayList<>();
    private final List<Spec> cover = new ArrayList<>();
    private final List<Spec> lake = new ArrayList<>();
    private final List<Spec> lakeDepth = new ArrayList<>();
    private final List<Spec> veg = new ArrayList<>();
    private final List<Spec> treeCover = new ArrayList<>();
    private final LinkedHashMap<Long, Tile> cache;
    private final long budgetBytes;
    private long usedBytes;
    private final Consumer<String> log;

    public RasterStack(long budgetBytes, Consumer<String> log) {
        this.budgetBytes = budgetBytes;
        this.log = log;
        this.cache = new LinkedHashMap<>(256, 0.75f, true);
    }

    /** Register a source; finest zoom wins within a layer, later registration wins at equal zoom. */
    public synchronized void add(Spec spec) {
        List<Spec> l = list(spec.layer());
        l.add(0, spec);
        l.sort(Comparator.comparingInt((Spec s) -> -s.zoom()));
        List<Spec> snap = List.copyOf(l);
        switch (spec.layer()) {
            case Tiling.LAYER_ELEV -> elevSnap = snap;
            case Tiling.LAYER_CLIMATE -> climateSnap = snap;
            case Tiling.LAYER_LAKE -> lakeSnap = snap;
            case Tiling.LAYER_LAKEDEPTH -> lakeDepthSnap = snap;
            case Tiling.LAYER_VEG -> vegSnap = snap;
            case Tiling.LAYER_TREECOVER -> treeSnap = snap;
            default -> coverSnap = snap;
        }
    }

    private volatile List<Spec> elevSnap = List.of(), climateSnap = List.of(), coverSnap = List.of(), lakeSnap = List.of(), lakeDepthSnap = List.of(), vegSnap = List.of(), treeSnap = List.of();

    public List<Spec> specs(int layer) {
        return switch (layer) {
            case Tiling.LAYER_ELEV -> elevSnap;
            case Tiling.LAYER_CLIMATE -> climateSnap;
            case Tiling.LAYER_COVER -> coverSnap;
            case Tiling.LAYER_LAKE -> lakeSnap;
            case Tiling.LAYER_LAKEDEPTH -> lakeDepthSnap;
            case Tiling.LAYER_VEG -> vegSnap;
            case Tiling.LAYER_TREECOVER -> treeSnap;
            default -> throw new IllegalArgumentException("layer " + layer);
        };
    }

    private List<Spec> list(int layer) {
        return switch (layer) {
            case Tiling.LAYER_ELEV -> elev;
            case Tiling.LAYER_CLIMATE -> climate;
            case Tiling.LAYER_COVER -> cover;
            case Tiling.LAYER_LAKE -> lake;
            case Tiling.LAYER_LAKEDEPTH -> lakeDepth;
            case Tiling.LAYER_VEG -> veg;
            case Tiling.LAYER_TREECOVER -> treeCover;
            default -> throw new IllegalArgumentException("layer " + layer);
        };
    }

    // ---- sampling -------------------------------------------------------------------------

    /** Elevation in metres (bilinear), or {@code fallback} when nothing covers the point. */
    public double elevation(double lon, double lat, double fallback) {
        return elevation(lon, lat, fallback, Integer.MAX_VALUE);
    }

    /**
     * Same, but ignores layers finer than {@code maxZoom}. Map rendering uses this so a zoomed-out view
     * reads the coarse base instead of touching thousands of fine tiles.
     */
    public double elevation(double lon, double lat, double fallback, int maxZoom) {
        List<Spec> specs = specs(Tiling.LAYER_ELEV);
        for (Spec s : specs) {
            if (s.zoom() > maxZoom || !s.covers(lon, lat)) continue;
            double v = bilinear(s, lon, lat);
            if (!Double.isNaN(v)) return v;
        }
        // Nothing coarse enough: use the coarsest layer there is rather than returning nothing.
        for (int i = specs.size() - 1; i >= 0; i--) {
            Spec s = specs.get(i);
            if (s.zoom() <= maxZoom || !s.covers(lon, lat)) continue;
            double v = bilinear(s, lon, lat);
            if (!Double.isNaN(v)) return v;
        }
        return fallback;
    }

    /**
     * Elevation with a smooth (Catmull-Rom bicubic) surface between pixels, for the block-level terrain. Bilinear
     * makes every pixel a flat facet with a crease at its edges, which shows in game as planes and grid lines on
     * slopes; the cubic surface has continuous slope and still passes through every sample.
     */
    public double elevationSmooth(double lon, double lat, double fallback) {
        List<Spec> specs = specs(Tiling.LAYER_ELEV);
        for (Spec s : specs) {
            if (!s.covers(lon, lat)) continue;
            double v = bicubic(s, lon, lat);
            if (!Double.isNaN(v)) return v;
        }
        return elevation(lon, lat, fallback);
    }

    /** Degrees per pixel of the finest source of this layer covering the point (the scale of its class edges). */
    public double finestResolution(int layer, double lon, double lat) {
        for (Spec s : specs(layer)) if (s.covers(lon, lat)) return Tiling.degPerPx(s.zoom());
        return 1.0;
    }

    private static double catmull(double p0, double p1, double p2, double p3, double t) {
        return p1 + 0.5 * t * (p2 - p0 + t * (2 * p0 - 5 * p1 + 4 * p2 - p3 + t * (3 * (p1 - p2) + p3 - p0)));
    }

    private double bicubic(Spec s, double lon, double lat) {
        int z = s.zoom();
        double gx = Tiling.globalPx(z, lon) - 0.5, gy = Tiling.globalPy(z, lat) - 0.5;
        int x0 = (int) Math.floor(gx), y0 = (int) Math.floor(gy);
        double fx = gx - x0, fy = gy - y0;
        int nod = Tiling.NODATA_ELEV;
        int[] p = NBH.get();
        int lx = Math.floorMod(x0, Tiling.TILE), ly = y0 - Math.floorDiv(y0, Tiling.TILE) * Tiling.TILE;
        int down = Tiling.tilesDown(z) * Tiling.TILE;
        if (lx >= 1 && lx <= Tiling.TILE - 3 && ly >= 1 && ly <= Tiling.TILE - 3 && y0 - 1 >= 0 && y0 + 2 < down) {
            // Fast path: the whole 4x4 neighbourhood is inside one tile.
            int across = Tiling.tilesAcross(z) * Tiling.TILE;
            int gxx = Math.floorMod(x0, across);
            Tile t = tile(s, gxx / Tiling.TILE, y0 / Tiling.TILE);
            if (t == null) return Double.NaN;
            for (int j = 0; j < 4; j++) for (int i = 0; i < 4; i++) p[j * 4 + i] = t.get(lx - 1 + i, ly - 1 + j);
        } else {
            for (int j = 0; j < 4; j++) for (int i = 0; i < 4; i++) p[j * 4 + i] = pixel(s, x0 - 1 + i, y0 - 1 + j);
        }
        int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
        for (int k = 0; k < 16; k++) {
            if (p[k] == nod) return bilinear(s, lon, lat); // next to a hole or an edge: the simple way
            lo = Math.min(lo, p[k]);
            hi = Math.max(hi, p[k]);
        }
        double r0 = catmull(p[0], p[1], p[2], p[3], fx), r1 = catmull(p[4], p[5], p[6], p[7], fx);
        double r2 = catmull(p[8], p[9], p[10], p[11], fx), r3 = catmull(p[12], p[13], p[14], p[15], fx);
        double v = catmull(r0, r1, r2, r3, fy);
        // A little overshoot is right (peaks rise a bit above the 30-90 m average, valleys dip below it); a clamp at
        // the neighbourhood's min/max would flatten every summit. Only big ringing next to cliffs is cut.
        double m = (hi - lo) * 0.12;
        return Math.max(lo - m, Math.min(hi + m, v));
    }

    private static final ThreadLocal<int[]> NBH = ThreadLocal.withInitial(() -> new int[16]);

    /** Köppen class 1..30, or 0 when nothing covers the point. */
    public int climate(double lon, double lat) {
        return nearest(Tiling.LAYER_CLIMATE, lon, lat, 0, Integer.MAX_VALUE);
    }

    public int climate(double lon, double lat, int maxZoom) {
        return nearest(Tiling.LAYER_CLIMATE, lon, lat, 0, maxZoom);
    }

    /** WorldCover class (10, 20, ... 100), or 0 when no fine cover layer covers the point. */
    public int cover(double lon, double lat) {
        return nearest(Tiling.LAYER_COVER, lon, lat, 0, Integer.MAX_VALUE);
    }

    /** Lake surface elevation in metres at this point, or NODATA (-32768) when not in a lake. */
    public int lakeSurface(double lon, double lat, int maxZoom) {
        return nearest(Tiling.LAYER_LAKE, lon, lat, Tiling.NODATA_ELEV, maxZoom);
    }

    /** Mean depth (m) of the lake at this point, 0 when none. */
    public int lakeDepth(double lon, double lat) {
        return nearest(Tiling.LAYER_LAKEDEPTH, lon, lat, 0, Integer.MAX_VALUE);
    }

    /** Copernicus LC100 vegetation class (20 shrub ... 126 open forest), 0 when no veg pack covers the point. */
    public int veg(double lon, double lat) {
        return nearest(Tiling.LAYER_VEG, lon, lat, 0, Integer.MAX_VALUE);
    }

    public int veg(double lon, double lat, int maxZoom) {
        return nearest(Tiling.LAYER_VEG, lon, lat, 0, maxZoom);
    }

    /** Tree cover 0..100 %, or -1 where no tree-cover pack covers the point. */
    public int treeCover(double lon, double lat) {
        return treeCover(lon, lat, Integer.MAX_VALUE);
    }

    public int treeCover(double lon, double lat, int maxZoom) {
        int v = nearest(Tiling.LAYER_TREECOVER, lon, lat, 255, maxZoom);
        return v > 100 ? -1 : v;
    }

    public int cover(double lon, double lat, int maxZoom) {
        return nearest(Tiling.LAYER_COVER, lon, lat, 0, maxZoom);
    }

    /** Degrees per pixel of the finest elevation layer at this point (for slope sampling). */
    public double finestElevResolution(double lon, double lat) {
        for (Spec s : specs(Tiling.LAYER_ELEV)) {
            if (s.covers(lon, lat)) return Tiling.degPerPx(s.zoom());
        }
        return 1.0;
    }

    /** Name of the source that answers for this layer at this point, for /earth status. */
    public String describe(int layer, double lon, double lat) {
        for (Spec s : specs(layer)) {
            if (!s.covers(lon, lat)) continue;
            int v = nearestFrom(s, lon, lat);
            if (v != Tiling.nodata(layer)) return s.source().name() + " z" + s.zoom();
        }
        return "none";
    }

    private int nearest(int layer, double lon, double lat, int fallback, int maxZoom) {
        List<Spec> specs = specs(layer);
        for (Spec s : specs) {
            if (s.zoom() > maxZoom || !s.covers(lon, lat)) continue;
            int v = nearestFrom(s, lon, lat);
            if (v != Tiling.nodata(layer)) return v;
        }
        for (int i = specs.size() - 1; i >= 0; i--) {
            Spec s = specs.get(i);
            if (s.zoom() <= maxZoom || !s.covers(lon, lat)) continue;
            int v = nearestFrom(s, lon, lat);
            if (v != Tiling.nodata(layer)) return v;
        }
        return fallback;
    }

    private int nearestFrom(Spec s, double lon, double lat) {
        int z = s.zoom();
        double gx = Tiling.globalPx(z, lon), gy = Tiling.globalPy(z, lat);
        int px = (int) Math.floor(gx), py = (int) Math.floor(gy);
        return pixel(s, px, py);
    }

    private double bilinear(Spec s, double lon, double lat) {
        int z = s.zoom();
        double gx = Tiling.globalPx(z, lon) - 0.5, gy = Tiling.globalPy(z, lat) - 0.5;
        int x0 = (int) Math.floor(gx), y0 = (int) Math.floor(gy);
        double fx = gx - x0, fy = gy - y0;
        int nod = Tiling.NODATA_ELEV;
        int a = pixel(s, x0, y0), b = pixel(s, x0 + 1, y0), c = pixel(s, x0, y0 + 1), d = pixel(s, x0 + 1, y0 + 1);
        if (a == nod) return Double.NaN;
        // Edge pixels (next to NODATA / outside) degrade gracefully instead of failing the layer.
        if (b == nod) b = a;
        if (c == nod) c = a;
        if (d == nod) d = b;
        return (a * (1 - fx) + b * fx) * (1 - fy) + (c * (1 - fx) + d * fx) * fy;
    }

    /** Global pixel lookup with wrap in x and clamp in y. */
    private int pixel(Spec s, int gx, int gy) {
        int z = s.zoom();
        int across = Tiling.tilesAcross(z) * Tiling.TILE;
        int down = Tiling.tilesDown(z) * Tiling.TILE;
        gx = Math.floorMod(gx, across);
        if (gy < 0) gy = 0;
        if (gy >= down) gy = down - 1;
        Tile t = tile(s, gx / Tiling.TILE, gy / Tiling.TILE);
        if (t == null) return Tiling.nodata(s.layer());
        return t.get(gx % Tiling.TILE, gy % Tiling.TILE);
    }

    // ---- cache ----------------------------------------------------------------------------

    /** Spreads tile keys over all 64 bits (raw keys differ only in a few bits, which made HashMap buckets degrade into trees). */
    private static long mix(long k) {
        k ^= k >>> 33;
        k *= 0xff51afd7ed558ccdL;
        k ^= k >>> 33;
        k *= 0xc4ceb9fe1a85ec53L;
        k ^= k >>> 33;
        return k;
    }

    /** Per-thread front cache: most lookups hit the same few tiles, so skip the shared lock entirely. */
    private static final int LOCAL = 16;
    private final ThreadLocal<Object[]> local = ThreadLocal.withInitial(() -> new Object[]{new long[LOCAL], new Tile[LOCAL], new long[1]});
    private final java.util.concurrent.atomic.AtomicLong generation = new java.util.concurrent.atomic.AtomicLong();   // bumped by invalidate(): thread caches drop everything

    /** Changes whenever a tile is replaced (a download landed); caches derived from tiles compare against it. */
    public long generation() {
        return generation.get();
    }

    private Tile tile(Spec s, int tx, int ty) {
        long key = mix(Tiling.key(s.layer(), s.zoom(), tx, ty) ^ ((long) System.identityHashCode(s.source()) << 40));
        Object[] l = local.get();
        long[] lk = (long[]) l[0];
        Tile[] lt = (Tile[]) l[1];
        long[] gen = (long[]) l[2];
        long g = generation.get();
        if (gen[0] != g) {
            java.util.Arrays.fill(lt, null);
            gen[0] = g;
        }
        int slot = (int) (key & (LOCAL - 1));
        if (lt[slot] != null && lk[slot] == key) return lt[slot] == MISSING ? null : lt[slot];
        Tile t = shared(s, tx, ty, key);
        lk[slot] = key;
        lt[slot] = t == null ? MISSING : t;
        return t;
    }

    private Tile shared(Spec s, int tx, int ty, long key) {
        Tile t;
        synchronized (this) {
            t = cache.get(key);
        }
        if (t != null) return t == MISSING ? null : t;
        long g0 = generation.get();
        try {
            t = s.source().load(s.layer(), s.zoom(), tx, ty);
        } catch (IOException e) {
            log.accept("tile " + s.source().name() + " " + Tiling.layerName(s.layer()) + " z" + s.zoom() + " " + tx + "_" + ty + ": " + e);
            t = null;
        }
        synchronized (this) {
            // A tile can land (and be invalidated) while we were finding it missing: don't remember a stale "missing".
            if (t == null && generation.get() != g0) return null;
            Tile prev = cache.putIfAbsent(key, t == null ? MISSING : t);
            if (prev != null) return prev == MISSING ? null : prev;
            usedBytes += t == null ? 64 : t.bytes();
            while (usedBytes > budgetBytes && cache.size() > 1) {
                Map.Entry<Long, Tile> eldest = cache.entrySet().iterator().next();
                usedBytes -= eldest.getValue() == MISSING ? 64 : eldest.getValue().bytes();
                cache.remove(eldest.getKey());
            }
        }
        return t;
    }

    /** Forget a tile (e.g. one just streamed into the cache) so the next lookup reloads it. */
    public void invalidate(int layer, int z, int tx, int ty) {
        for (Spec s : specs(layer)) {
            if (s.zoom() != z) continue;
            long key = mix(Tiling.key(layer, z, tx, ty) ^ ((long) System.identityHashCode(s.source()) << 40));
            synchronized (this) {
                Tile t = cache.remove(key);
                if (t != null) usedBytes -= t == MISSING ? 64 : t.bytes();
            }
            generation.incrementAndGet();
        }
    }

    public synchronized String cacheStatus() {
        return cache.size() + " tiles, " + (usedBytes >> 20) + " / " + (budgetBytes >> 20) + " MB";
    }
}
