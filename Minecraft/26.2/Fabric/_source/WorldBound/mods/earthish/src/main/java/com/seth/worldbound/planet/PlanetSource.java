package com.seth.worldbound.planet;

import com.seth.worldbound.data.RasterStack;
import com.seth.worldbound.data.Tile;
import com.seth.worldbound.data.TileSource;
import com.seth.worldbound.data.Tiling;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

/**
 * An Earthish planet as tiles, in exactly the layers and format the real Earth data comes in, so the whole mod
 * (terrain, biomes, rivers, lakes, trees, the map, the horizon, Distant Horizons) works on it unchanged:
 *
 * <ul>
 * <li>elevation: the planet grid at z2 (~10 km), then z5 / z7 / z10 (~40 m) with fractal detail below the grid
 * (ridged in mountains, gentle on plains, smooth on ice) and river valleys cut along each channel;</li>
 * <li>climate (Köppen): z2, and z6 (~600 m) with the temperature lapse of the detailed ground, so ranges get their
 * alpine and polar climates;</li>
 * <li>vegetation class and tree cover (Copernicus LC100 codes): z7, from climate, moisture and patchiness;</li>
 * <li>lakes: surface level at z2 and z10 (shores follow the detailed ground), depth at z7.</li>
 * </ul>
 *
 * Tiles are generated on demand on the thread that asks, at most once at a time per tile (others wait for it).
 */
public final class PlanetSource implements TileSource {
    public final Planet p;
    public final PlanetRivers rivers;
    private final Noise3 detail, vegPatch, vegFine, wet;
    private final byte[] vegZ2;

    public PlanetSource(Planet planet) {
        p = planet;
        rivers = new PlanetRivers(planet);
        detail = new Noise3(planet.seed ^ 0xDE7A11L);
        vegPatch = new Noise3(planet.seed ^ 0x7E6E7AL);
        vegFine = new Noise3(planet.seed ^ 0xF1AE5L);
        wet = new Noise3(planet.seed ^ 0x3E71A4DL);
        vegZ2 = new byte[planet.grid.n];
        Grid g = planet.grid;
        IntStream.range(0, g.h).parallel().forEach(j -> {
            for (int i = 0; i < g.w; i++) {
                int k = j * g.w + i;
                if (planet.elev[k] <= 0) continue;
                int[] vt = vegTree(g.lon[i], g.lat[j], planet.elev[k], g.vx[k], g.vy[k], g.vz[k]);
                vegZ2[k] = (byte) vt[0];
            }
        });
    }

    @Override
    public String name() {
        return "earthish";
    }

    /** Put every layer of this planet on a raster stack. */
    public void register(RasterStack stack) {
        for (int z : new int[]{2, 5, 7, 10}) stack.add(spec(Tiling.LAYER_ELEV, z));
        stack.add(spec(Tiling.LAYER_CLIMATE, 2));
        stack.add(spec(Tiling.LAYER_CLIMATE, 6));
        stack.add(spec(Tiling.LAYER_VEG, 2));
        stack.add(spec(Tiling.LAYER_VEG, 7));
        stack.add(spec(Tiling.LAYER_TREECOVER, 7));
        stack.add(spec(Tiling.LAYER_LAKE, 2));
        stack.add(spec(Tiling.LAYER_LAKE, 10));
        stack.add(spec(Tiling.LAYER_LAKEDEPTH, 7));
    }

    private RasterStack.Spec spec(int layer, int z) {
        return new RasterStack.Spec(layer, z, this, -180, -90, 180, 90);
    }

    // ---- tile cache: each tile is made once; a few recent ones are kept for the layers derived from them ------

    private final Map<Long, CompletableFuture<Tile>> recent = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, CompletableFuture<Tile>> e) {
            return size() > 20 && e.getValue().isDone();
        }
    };

    @Override
    public Tile load(int layer, int z, int tx, int ty) {
        long key = Tiling.key(layer, z, tx, ty);
        CompletableFuture<Tile> f;
        boolean mine = false;
        synchronized (recent) {
            f = recent.get(key);
            if (f == null) {
                f = new CompletableFuture<>();
                recent.put(key, f);
                mine = true;
            }
        }
        if (mine) {
            try {
                f.complete(generate(layer, z, tx, ty));
            } catch (Throwable t) {
                synchronized (recent) {
                    recent.remove(key);
                }
                f.completeExceptionally(t);
            }
        }
        return f.join();
    }

    private Tile generate(int layer, int z, int tx, int ty) {
        if (z == Planet.ZOOM) return z2(layer, tx, ty);
        return switch (layer) {
            case Tiling.LAYER_ELEV -> elevTile(z, tx, ty);
            case Tiling.LAYER_CLIMATE -> climateTile(z, tx, ty);
            case Tiling.LAYER_VEG, Tiling.LAYER_TREECOVER -> vegTile(layer, z, tx, ty);
            case Tiling.LAYER_LAKE -> lakeTile(z, tx, ty);
            case Tiling.LAYER_LAKEDEPTH -> lakeDepthTile(z, tx, ty);
            default -> null;
        };
    }

    private Tile z2(int layer, int tx, int ty) {
        int T = Tiling.TILE;
        short[] px = new short[T * T];
        Grid g = p.grid;
        for (int y = 0; y < T; y++) {
            for (int x = 0; x < T; x++) {
                int k = (ty * T + y) * g.w + tx * T + x;
                int v;
                switch (layer) {
                    case Tiling.LAYER_ELEV -> v = Math.round(p.elev[k]);
                    case Tiling.LAYER_CLIMATE -> v = p.koppen[k];
                    case Tiling.LAYER_VEG -> v = vegZ2[k] & 255;
                    case Tiling.LAYER_LAKE -> v = p.hydro.lakeLevel[k] == Hydrology.NO_LAKE ? Tiling.NODATA_ELEV : p.hydro.lakeLevel[k];
                    default -> v = Tiling.nodata(layer);
                }
                px[y * T + x] = (short) Math.max(-32768, Math.min(32767, v));
            }
        }
        return new Tile(layer, Planet.ZOOM, tx, ty, T, T, Tiling.nodata(layer), px);
    }

    // ---- elevation --------------------------------------------------------------------------------------------

    private static double smooth(double e0, double e1, double x) {
        double t = Math.max(0, Math.min(1, (x - e0) / (e1 - e0)));
        return t * t * (3 - 2 * t);
    }

    /**
     * Ground height (m) at a point with detail down to {@code pxM} metres: the planet grid (bicubic) plus fractal
     * detail from 20 km down, ~0.6 of the amplitude per halving, scaled to how rugged the ground around is.
     */
    double elevation(double lon, double lat, double x, double y, double z, double pxM) {
        Grid g = p.grid;
        double base = g.sampleCubic(p.elev, lon, lat);
        double rug = g.sample(p.rugged, lon, lat);
        double ice = g.sample(p.ice, lon, lat);
        double a0 = Math.max(5, Math.min(520, 0.24 * rug + 5)) * (1 - 0.85 * ice);
        double wm = smooth(250, 1100, rug);
        // a gentle warp of the detail itself, so its features aren't round blobs
        double wf = 6_371_000.0 / 30000;
        double wx = x + 0.0006 * detail.at(x * wf + 7.7, y * wf, z * wf), wy = y + 0.0006 * detail.at(x * wf, y * wf + 7.7, z * wf),
                wz = z + 0.0006 * detail.at(x * wf, y * wf, z * wf + 7.7);
        double hills = 0, ridges = 0, a = a0, weight = 1, norm = 0;
        double gainH = 0.55, gainR = 0.62;
        double aR = a0;
        int o = 0;
        for (double lam = 20000; lam >= 2.2 * pxM; lam *= 0.5, o++) {
            double f = 6_371_000.0 / lam;
            double nk = detail.at(wx * f + o * 17.17, wy * f - o * 9.31, wz * f + o * 4.53);
            hills += a * nk;
            a *= gainH;
            // ridged multifractal (Musgrave): sharp crests, and each octave only where the last one was high, so
            // valleys between the ridges stay smooth
            double r = 1 - Math.abs(nk);
            r *= r;
            r *= weight;
            weight = Math.max(0, Math.min(1, r * 2.2));
            ridges += aR * r;
            norm += aR;
            aR *= gainR;
        }
        ridges = (ridges - 0.32 * norm) * 1.6;
        double det = (1 - wm) * hills + wm * ridges;
        // near sea level the planet's own coastline rules: detail there only frays it, never drowns a coast or
        // raises reefs out of the shelf
        double coast = base > 0 ? smooth(0, 450, base) : smooth(0, 350, -base);
        return base + det * (0.07 + 0.93 * coast);
    }

    private static double pxLon(int z, int tx, int x) {
        return -180.0 + (tx * Tiling.TILE + x + 0.5) * Tiling.degPerPx(z);
    }

    private static double pxLat(int z, int ty, int y) {
        return 90.0 - (ty * Tiling.TILE + y + 0.5) * Tiling.degPerPx(z);
    }

    /** Raw (uncarved) elevation over a tile, as floats. */
    private float[] rawElev(int z, int tx, int ty) {
        int T = Tiling.TILE;
        double pxM = Tiling.degPerPx(z) * PlanetRivers.KY;
        float[] out = new float[T * T];
        IntStream.range(0, T).parallel().forEach(y -> {
            double lat = pxLat(z, ty, y), cl = Math.cos(Math.toRadians(lat)), sl = Math.sin(Math.toRadians(lat));
            for (int x = 0; x < T; x++) {
                double lon = pxLon(z, tx, x), r = Math.toRadians(lon);
                out[y * T + x] = (float) elevation(lon, lat, cl * Math.cos(r), cl * Math.sin(r), sl, pxM);
            }
        });
        return out;
    }

    private Tile elevTile(int z, int tx, int ty) {
        int T = Tiling.TILE;
        float[] e = rawElev(z, tx, ty);
        carveValleys(e, z, tx, ty);
        short[] px = new short[T * T];
        for (int i = 0; i < px.length; i++) px[i] = (short) Math.max(-11000, Math.min(9000, Math.round(e[i])));
        return new Tile(Tiling.LAYER_ELEV, z, tx, ty, T, T, Tiling.NODATA_ELEV, px);
    }

    /**
     * Cut river valleys into a tile: every channel is drawn as a line of seed pixels (with its bed height and valley
     * half-width), a distance transform finds each pixel's nearest channel, and the ground within the valley is
     * pulled down toward the bed: flat along the floor, rising to the untouched slopes at the valley's edge.
     */
    private void carveValleys(float[] e, int z, int tx, int ty) {
        int T = Tiling.TILE;
        double deg = Tiling.degPerPx(z), pxM = deg * PlanetRivers.KY;
        int marg = (int) Math.ceil(13500 / pxM) + 2;
        int N = T + 2 * marg;
        double west = pxLon(z, tx, -marg) - deg / 2, north = pxLat(z, ty, -marg) + deg / 2;
        double east = west + N * deg, south = north - N * deg;
        double latC = (north + south) / 2;
        double kxPx = pxM * Math.max(0.05, Math.cos(Math.toRadians(latC))), kyPx = pxM;
        int[] seedRiver = new int[N * N];
        float[] seedBed = new float[N * N];
        java.util.Arrays.fill(seedRiver, -1);
        double[] pt = new double[2];
        int[] any = {0};
        rivers.collect(west, Math.max(-90, south), east, Math.min(90, north), i -> {
            if (rivers.valley[i] < pxM * 0.9) return;
            double len = rivers.length(i);
            int steps = (int) Math.max(4, Math.min(20000, Math.ceil(len / (pxM * 0.5))));
            double lon0 = rivers.lon0(i), lat0 = rivers.lat0(i);
            double kx = PlanetRivers.KX0 * Math.max(0.05, Math.cos(Math.toRadians(lat0)));
            for (int s = 0; s <= steps; s++) {
                double t = s / (double) steps;
                rivers.point(i, t, kx, pt, false); // the valley follows the meander belt, not every bend
                double lon = lon0 + pt[0] / kx, lat = lat0 + pt[1] / PlanetRivers.KY;
                double fx = ((lon - west) % 360 + 360) % 360 / deg, fy = (north - lat) / deg;
                int ix = (int) Math.floor(fx), iy = (int) Math.floor(fy);
                if (ix < 0 || iy < 0 || ix >= N || iy >= N) continue;
                int k = iy * N + ix;
                int cur = seedRiver[k];
                if (cur < 0 || rivers.width[i] > rivers.width[cur]) {
                    seedRiver[k] = i;
                    seedBed[k] = (float) (rivers.bed0[i] + (rivers.bed1[i] - rivers.bed0[i]) * t);
                    any[0]++;
                }
            }
        });
        if (any[0] == 0) return;
        // nearest seed for every pixel (vector propagation, two sweeps)
        int[] near = new int[N * N];
        for (int k = 0; k < near.length; k++) near[k] = seedRiver[k] >= 0 ? k : -1;
        for (int pass = 0; pass < 2; pass++) {
            for (int y = 0; y < N; y++) {
                for (int x = 0; x < N; x++) {
                    int k = y * N + x;
                    if (x > 0) relax(near, k, k - 1, x, y, N, kxPx, kyPx);
                    if (y > 0) {
                        relax(near, k, k - N, x, y, N, kxPx, kyPx);
                        if (x > 0) relax(near, k, k - N - 1, x, y, N, kxPx, kyPx);
                        if (x < N - 1) relax(near, k, k - N + 1, x, y, N, kxPx, kyPx);
                    }
                }
                for (int x = N - 2; x >= 0; x--) relax(near, y * N + x, y * N + x + 1, x, y, N, kxPx, kyPx);
            }
            for (int y = N - 1; y >= 0; y--) {
                for (int x = N - 1; x >= 0; x--) {
                    int k = y * N + x;
                    if (x < N - 1) relax(near, k, k + 1, x, y, N, kxPx, kyPx);
                    if (y < N - 1) {
                        relax(near, k, k + N, x, y, N, kxPx, kyPx);
                        if (x < N - 1) relax(near, k, k + N + 1, x, y, N, kxPx, kyPx);
                        if (x > 0) relax(near, k, k + N - 1, x, y, N, kxPx, kyPx);
                    }
                }
                for (int x = 1; x < N; x++) relax(near, y * N + x, y * N + x - 1, x, y, N, kxPx, kyPx);
            }
        }
        IntStream.range(0, T).parallel().forEach(y -> {
            int ey = y + marg;
            for (int x = 0; x < T; x++) {
                int ex = x + marg, k = ey * N + ex, s = near[k];
                if (s < 0) continue;
                int river = seedRiver[s];
                double dx = (ex - s % N) * kxPx, dy = (ey - s / N) * kyPx;
                double d = Math.sqrt(dx * dx + dy * dy);
                // a flat floor wide enough for the meanders, then sides rising to the untouched ground
                double floor = rivers.amp[river] + rivers.width[river] * 0.5 + 15;
                double v = Math.max(rivers.valley[river], floor * 2.2);
                if (d >= v) continue;
                double f = d < floor ? 0 : Math.pow(smooth(floor, v, d), 0.85);
                double bed = seedBed[s];
                int ti = y * T + x;
                if (e[ti] <= 0) continue; // the sea and its shore stay as they are
                e[ti] = (float) (bed + (e[ti] - bed) * f);
            }
        });
    }

    private static void relax(int[] near, int k, int nb, int x, int y, int N, double kx, double ky) {
        int s = near[nb];
        if (s < 0 || s == near[k]) return;
        double dx = (x - s % N) * kx, dy = (y - s / N) * ky;
        double d = dx * dx + dy * dy;
        int c = near[k];
        if (c >= 0) {
            double cx = (x - c % N) * kx, cy = (y - c / N) * ky;
            if (cx * cx + cy * cy <= d) return;
        }
        near[k] = s;
    }

    /** Detailed elevation tile from the cache (or made now) as floats. */
    private float[] elevFloats(int z, int tx, int ty) {
        Tile t = load(Tiling.LAYER_ELEV, z, tx, ty);
        int T = Tiling.TILE;
        float[] e = new float[T * T];
        for (int y = 0; y < T; y++) for (int x = 0; x < T; x++) e[y * T + x] = t.get(x, y);
        return e;
    }

    // ---- climate ------------------------------------------------------------------------------------------------

    private Tile climateTile(int z, int tx, int ty) {
        int T = Tiling.TILE;
        double pxM = Tiling.degPerPx(z) * PlanetRivers.KY;
        short[] px = new short[T * T];
        IntStream.range(0, T).parallel().forEach(y -> {
            double lat = pxLat(z, ty, y), cl = Math.cos(Math.toRadians(lat)), sl = Math.sin(Math.toRadians(lat));
            for (int x = 0; x < T; x++) {
                double lon = pxLon(z, tx, x), r = Math.toRadians(lon);
                double el = elevation(lon, lat, cl * Math.cos(r), cl * Math.sin(r), sl, pxM);
                // the shallows keep a land class too, so coastal ground never reads as "no climate"
                px[y * T + x] = (short) (el > -30 ? p.classify(lon, lat, Math.max(0, el)) : 0);
            }
        });
        return new Tile(Tiling.LAYER_CLIMATE, z, tx, ty, T, T, 0, px);
    }

    // ---- vegetation -----------------------------------------------------------------------------------------------

    private static final int[] TREE_BASE = {0,
            92, 82, 32, 0, 1, 8, 9,            // Af Am Aw BWh BWk BSh BSk
            32, 48, 55, 50, 55, 50,            // Csa Csb Csc Cwa Cwb Cwc
            62, 62, 60,                        // Cfa Cfb Cfc
            30, 45, 50, 40,                    // Dsa Dsb Dsc Dsd
            42, 55, 55, 42,                    // Dwa Dwb Dwc Dwd
            48, 66, 56, 40,                    // Dfa Dfb Dfc Dfd
            2, 0};                             // ET EF

    /** {LC100 class, tree cover %} for a land point. */
    int[] vegTree(double lon, double lat, double el, double x, double y, double z) {
        Grid g = p.grid;
        if (g.sample(p.ice, lon, lat) > 0.5) return new int[]{70, 0};
        int k = p.classify(lon, lat, el);
        double t = p.temperature(lon, lat, el), pr = p.precipitation(lon, lat);
        double wetness = pr / Math.max(150, Hydrology.pet(t));
        double patch = vegPatch.fbm(x, y, z, 6371.0 / 6.0, 3, 0.55);     // ~6 km forest/clearing patches
        double fine = vegFine.fbm(x, y, z, 6371.0 / 0.9, 2, 0.5);        // ~1 km
        double tc = TREE_BASE[k] * Math.max(0.25, Math.min(1.35, 0.45 + 0.55 * wetness));
        tc *= 1 + 0.6 * patch + 0.2 * fine;
        tc *= smooth(6.5, 10.5, p.warmest(lon, lat, el));   // treeline: trees need a summer month above ~10 °C
        int tcv = (int) Math.max(0, Math.min(100, Math.round(tc)));
        int leaf = leaf(k, el, patch, fine);
        int veg;
        if (tcv >= 60) veg = 110 + leaf;
        else if (tcv >= 15) veg = 120 + leaf;
        else if (k >= 4 && k <= 7) veg = (k == 4 || k == 5) ? (pr < 130 ? 60 : 20) : (fine > 0 ? 30 : 20);
        else if (k >= 29) veg = fine > 0.15 ? 30 : 100;
        else if (k <= 3) veg = 30;
        else veg = (k == 8 || k == 9 || k == 17 || k == 18) ? 20 : 30;
        // wetlands: flat, wet lowland in patches (bogs of the boreal plains, swamps of the warm ones)
        double rug = g.sample(p.rugged, lon, lat);
        if (veg != 60 && k < 29 && el < 350 && rug < 90 && wetness > 1.15 && wet.fbm(x, y, z, 6371.0 / 4.0, 2, 0.5) > 0.42) veg = 90;
        return new int[]{veg, tcv};
    }

    /** LC100 leaf type 1..5 (evergreen needle, evergreen broad, deciduous needle, deciduous broad, mixed). */
    private static int leaf(int k, double el, double patch, double fine) {
        if (k >= 17 && k <= 28 || k >= 8 && k <= 16) {
            if (el > 1600) return 1; // montane conifers
        }
        return switch (k) {
            case 1, 2 -> 2;
            case 3 -> 4;
            case 4, 5, 6 -> 4;
            case 7 -> fine > 0 ? 1 : 4;
            case 8 -> fine > 0.2 ? 1 : 2;
            case 9, 10 -> fine > -0.2 ? 1 : 2;
            case 11, 12, 13 -> fine > 0 ? 2 : 4;
            case 14 -> patch > 0.25 ? 2 : fine > 0 ? 5 : 4;
            case 15 -> fine > 0.1 ? 5 : 4;
            case 16 -> 1;
            case 17, 18, 21, 22, 25, 26 -> fine > 0 ? 5 : 4;
            case 19, 27 -> fine > 0.35 ? 5 : 1;
            case 23 -> fine > 0 ? 3 : 1;
            case 20, 24, 28 -> 3;
            default -> 1;
        };
    }

    private Tile vegTile(int layer, int z, int tx, int ty) {
        int T = Tiling.TILE;
        float[] e = elevFloats(z, tx, ty);
        short[] veg = new short[T * T], tree = new short[T * T];
        IntStream.range(0, T).parallel().forEach(y -> {
            double lat = pxLat(z, ty, y), cl = Math.cos(Math.toRadians(lat)), sl = Math.sin(Math.toRadians(lat));
            for (int x = 0; x < T; x++) {
                int i = y * T + x;
                if (e[i] <= 0) { veg[i] = 0; tree[i] = 255; continue; }
                double lon = pxLon(z, tx, x), r = Math.toRadians(lon);
                int[] vt = vegTree(lon, lat, e[i], cl * Math.cos(r), cl * Math.sin(r), sl);
                veg[i] = (short) vt[0];
                tree[i] = (short) vt[1];
            }
        });
        // the other layer of the pair goes in the cache too
        int other = layer == Tiling.LAYER_VEG ? Tiling.LAYER_TREECOVER : Tiling.LAYER_VEG;
        Tile ot = new Tile(other, z, tx, ty, T, T, Tiling.nodata(other), other == Tiling.LAYER_VEG ? veg : tree);
        synchronized (recent) {
            recent.putIfAbsent(Tiling.key(other, z, tx, ty), CompletableFuture.completedFuture(ot));
        }
        return new Tile(layer, z, tx, ty, T, T, Tiling.nodata(layer), layer == Tiling.LAYER_VEG ? veg : tree);
    }

    // ---- lakes --------------------------------------------------------------------------------------------------

    /** The level of the lake this point could belong to: its own z2 cell's, or a neighbour's; NO_LAKE if none. */
    private int lakeNear(double lon, double lat) {
        Grid g = p.grid;
        int i = (int) Math.floor(g.fx(lon) + 0.5), j = (int) Math.floor(g.fy(lat) + 0.5);
        j = Math.max(0, Math.min(g.h - 1, j));
        short own = p.hydro.lakeLevel[j * g.w + Math.floorMod(i, g.w)];
        if (own != Hydrology.NO_LAKE) return own;
        int best = Hydrology.NO_LAKE;
        for (int b = -1; b <= 1; b++) {
            int jj = j + b;
            if (jj < 0 || jj >= g.h) continue;
            for (int a = -1; a <= 1; a++) {
                short v = p.hydro.lakeLevel[jj * g.w + Math.floorMod(i + a, g.w)];
                if (v != Hydrology.NO_LAKE && v > best) best = v;
            }
        }
        return best;
    }

    private Tile lakeTile(int z, int tx, int ty) {
        int T = Tiling.TILE;
        float[] e = elevFloats(z, tx, ty);
        short[] px = new short[T * T];
        IntStream.range(0, T).parallel().forEach(y -> {
            double lat = pxLat(z, ty, y);
            for (int x = 0; x < T; x++) {
                int i = y * T + x;
                int lv = lakeNear(pxLon(z, tx, x), lat);
                px[i] = (short) (lv != Hydrology.NO_LAKE && e[i] < lv ? lv : Tiling.NODATA_ELEV);
            }
        });
        return new Tile(Tiling.LAYER_LAKE, z, tx, ty, T, T, Tiling.NODATA_ELEV, px);
    }

    private Tile lakeDepthTile(int z, int tx, int ty) {
        int T = Tiling.TILE;
        float[] e = elevFloats(z, tx, ty);
        short[] px = new short[T * T];
        IntStream.range(0, T).parallel().forEach(y -> {
            double lat = pxLat(z, ty, y);
            for (int x = 0; x < T; x++) {
                int i = y * T + x;
                int lv = lakeNear(pxLon(z, tx, x), lat);
                px[i] = (short) (lv != Hydrology.NO_LAKE && e[i] < lv ? Math.max(1, Math.min(255, Math.round(lv - e[i]))) : 0);
            }
        });
        return new Tile(Tiling.LAYER_LAKEDEPTH, z, tx, ty, T, T, 0, px);
    }
}
