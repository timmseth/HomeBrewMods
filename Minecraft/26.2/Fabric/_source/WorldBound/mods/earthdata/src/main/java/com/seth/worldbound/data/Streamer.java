package com.seth.worldbound.data;

import com.seth.worldbound.Earth;
import com.seth.worldbound.WorldBoundMod;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-game streaming of fine data from AWS Open Data into {@code worldbound/cache/} (a normal tile pack):
 * Copernicus DEM GLO-30 elevation (z10, ~38 m) and ESA WorldCover 2021 land cover (z11, ~19 m).
 * Tiles are fetched ahead of players and, when generation reaches an area first, waited for (bounded);
 * offline or slow, generation falls back to the installed packs.
 */
public final class Streamer {
    public static final int ELEV_Z = 10, COVER_Z = 11;
    private static final String GLO30 = System.getProperty("worldbound.glo30", "https://copernicus-dem-30m.s3.amazonaws.com/");
    private static final String WORLDCOVER = System.getProperty("worldbound.worldcover", "https://esa-worldcover.s3.eu-central-1.amazonaws.com/v200/2021/map/");

    /** Streaming ships in 0.4.14; 0.4.13 carries the code switched off. */
    static final boolean RELEASED = true;
    private static Streamer INSTANCE;

    public static synchronized Streamer get() {
        if (INSTANCE == null) INSTANCE = new Streamer();
        return INSTANCE;
    }

    private final ExecutorService pool;
    private final Map<Long, CompletableFuture<Void>> inflight = new ConcurrentHashMap<>();
    private final Map<Long, Boolean> ready = new ConcurrentHashMap<>();
    /** Downloads a chunk already waited the full time for: later chunks don't wait on them again. */
    private final java.util.Set<Long> waitedOut = ConcurrentHashMap.newKeySet();
    private final Map<String, Cog> cogs = new LinkedHashMap<>(64, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, Cog> e) {
            return size() > 48;
        }
    };
    private final Map<String, Boolean> missingFiles = new ConcurrentHashMap<>();
    private final AtomicInteger failures = new AtomicInteger();
    private volatile long offlineUntil;
    public final AtomicInteger fetched = new AtomicInteger(), empty = new AtomicInteger(), errors = new AtomicInteger();
    public final AtomicLong bytes = new AtomicLong();

    /** Players' block positions (x, z pairs), refreshed by the server tick; used to rank and gate downloads. */
    private static volatile double[] players = new double[0];

    public static void setPlayers(double[] xz) {
        players = xz;
    }

    /** Distance in blocks from this column to the nearest player (east-west wrap aware). */
    public static double nearestPlayer(int x, int z) {
        double[] p = players;
        if (p.length == 0) return 0;   // nobody on yet (spawn area being made): treat as near
        double best = Double.MAX_VALUE, width = Earth.get().transform.widthBlocks;
        for (int i = 0; i + 1 < p.length; i += 2) {
            double dx = Math.abs(x - p[i]);
            if (width > 0) dx = Math.min(dx, Math.abs(width - dx));
            double d = Math.hypot(dx, z - p[i + 1]);
            if (d < best) best = d;
        }
        return best;
    }

    /** A download job; nearer to a player runs first. */
    private record Job(double priority, long seq, Runnable task) implements Runnable, Comparable<Job> {
        public void run() { task.run(); }
        public int compareTo(Job o) {
            int c = Double.compare(priority, o.priority);
            return c != 0 ? c : Long.compare(seq, o.seq);
        }
    }
    private final java.util.concurrent.atomic.AtomicLong seq = new java.util.concurrent.atomic.AtomicLong();
    private final java.util.concurrent.PriorityBlockingQueue<Runnable> queue = new java.util.concurrent.PriorityBlockingQueue<>();

    private Streamer() {
        int threads = Math.max(1, Math.min(8, Earth.get().config.streaming.threads));
        pool = new java.util.concurrent.ThreadPoolExecutor(threads, threads, 60, TimeUnit.SECONDS, queue, r -> {
            Thread t = new Thread(r, "worldbound-stream");
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY - 1);
            return t;
        });
        pool.execute(new Job(-1, 0, Streamer::trimCache));
    }

    /** Keep the stream cache under streaming.max_cache_mb by deleting the least recently written tiles. */
    private static void trimCache() {
        try {
            long max = (long) Earth.get().config.streaming.max_cache_mb << 20;
            Path root = cacheDir().resolve("layers");
            if (!Files.isDirectory(root)) return;
            java.util.List<Path> files;
            try (var st = Files.walk(root)) {
                files = st.filter(f -> f.toString().endsWith(".etile")).collect(java.util.stream.Collectors.toList());
            }
            long total = 0;
            java.util.Map<Path, Long> times = new java.util.HashMap<>();
            for (Path f : files) {
                total += Files.size(f);
                times.put(f, Files.getLastModifiedTime(f).toMillis());
            }
            if (total <= max) return;
            files.sort(java.util.Comparator.comparingLong(times::get));
            for (Path f : files) {
                if (total <= max * 9 / 10) break;
                total -= Files.size(f);
                Files.deleteIfExists(f);
            }
            WorldBoundMod.LOG.info("WorldBound streaming: cache trimmed to {} MB", total >> 20);
        } catch (IOException | RuntimeException e) {
            WorldBoundMod.LOG.warn("stream cache trim: {}", e.toString());
        }
    }

    public static boolean enabled() {
        Earth e = Earth.get();
        if (e.generated()) return false; // an Earthish planet is made, not downloaded
        var c = e.config;
        return RELEASED && c.streaming.enabled && (c.streaming.elevation || c.streaming.land_cover);
    }

    private static Path cacheDir() {
        return Earth.get().cacheDir();
    }

    private static long key(int layer, int z, int tx, int ty) {
        return Tiling.key(layer, z, tx, ty);
    }

    private static Path tilePath(int layer, int z, int tx, int ty) {
        return cacheDir().resolve(TileSource.relPath(layer, z, tx, ty));
    }

    private static Path nonePath(int layer, int z, int tx, int ty) {
        return cacheDir().resolve("layers/" + Tiling.layerName(layer) + "/" + z + "/" + tx + "_" + ty + ".none");
    }

    public boolean offline() {
        return System.currentTimeMillis() < offlineUntil;
    }

    /** Make sure the tiles under this point are fetched; optionally wait (bounded) for them. */
    public void ensure(double lon, double lat, boolean wait, double priority) {
        if (!enabled()) return;
        var c = Earth.get().config;
        // One deadline for both layers: a chunk waits at most near_wait_seconds in total.
        long deadline = System.nanoTime() + Math.max(1, Math.min(10, c.streaming.near_wait_seconds)) * 1_000_000_000L;
        if (c.streaming.elevation) ensureTile(Tiling.LAYER_ELEV, ELEV_Z, lon, lat, wait, priority, deadline);
        if (c.streaming.land_cover) ensureTile(Tiling.LAYER_COVER, COVER_Z, lon, lat, wait, priority, deadline);
    }

    /**
     * Before a new world's spawn area generates: fetch the fine tiles under the spawn (and around it) and wait for
     * them up to {@code seconds}, so the spawn has the fine data without every spawn chunk waiting on its own.
     */
    public void prepareSpawn(double lon, double lat, int seconds) {
        if (!enabled() || offline()) return;
        var c = Earth.get().config;
        long deadline = System.nanoTime() + seconds * 1_000_000_000L;
        double d = 0.02; // ~2 km: the spawn area and its surroundings
        for (double dy = -d; dy <= d + 1e-9; dy += d)
            for (double dx = -d; dx <= d + 1e-9; dx += d) {
                double x = ((lon + dx + 180) % 360 + 360) % 360 - 180, y = Math.max(-89.9, Math.min(89.9, lat + dy));
                if (c.streaming.elevation) ensureTile(Tiling.LAYER_ELEV, ELEV_Z, x, y, false, -1, deadline);
                if (c.streaming.land_cover) ensureTile(Tiling.LAYER_COVER, COVER_Z, x, y, false, -1, deadline);
            }
        long t0 = System.nanoTime();
        for (CompletableFuture<Void> f : inflight.values()) {
            long left = deadline - System.nanoTime();
            if (left <= 0) break;
            try { f.get(left, TimeUnit.NANOSECONDS); } catch (Exception ignored) { }
        }
        WorldBoundMod.LOG.info("WorldBound: spawn data ready in {} ms ({})", (System.nanoTime() - t0) / 1_000_000, status());
    }

    private void ensureTile(int layer, int z, double lon, double lat, boolean wait, double priority) {
        ensureTile(layer, z, lon, lat, wait, priority, System.nanoTime() + Math.max(1, Math.min(10, Earth.get().config.streaming.near_wait_seconds)) * 1_000_000_000L);
    }

    private void ensureTile(int layer, int z, double lon, double lat, boolean wait, double priority, long deadline) {
        int tx = (int) Math.floor(Tiling.globalPx(z, lon) / Tiling.TILE);
        int ty = (int) Math.floor(Tiling.globalPy(z, lat) / Tiling.TILE);
        tx = Math.floorMod(tx, Tiling.tilesAcross(z));
        if (ty < 0 || ty >= Tiling.tilesDown(z)) return;
        long k = key(layer, z, tx, ty);
        if (ready.containsKey(k)) return;
        if (Files.exists(tilePath(layer, z, tx, ty)) || noneFresh(nonePath(layer, z, tx, ty))) {
            ready.put(k, true);
            return;
        }
        if (offline()) return;
        if (!wait && queue.size() > 256 && !inflight.containsKey(k)) return;   // backlog: far requests can come later
        final int ftx = tx, fty = ty;
        CompletableFuture<Void> f = inflight.computeIfAbsent(k, kk -> {
            CompletableFuture<Void> cf = new CompletableFuture<>();
            pool.execute(new Job(priority, seq.incrementAndGet(), () -> {
                try {
                    fetch(layer, z, ftx, fty);
                } finally {
                    inflight.remove(kk);
                    waitedOut.remove(kk);
                    cf.complete(null);
                }
            }));
            return cf;
        });
        if (wait && !waitedOut.contains(k)) {
            long left = deadline - System.nanoTime();
            if (left <= 0) return;
            try {
                f.get(left, TimeUnit.NANOSECONDS);
            } catch (java.util.concurrent.TimeoutException e) {
                // Slow download: this chunk generates from the packs we have, and no later chunk waits on it again.
                waitedOut.add(k);
            } catch (Exception ignored) {
                // failed: generate from the packs we have
            }
        }
    }

    /** Queue the tiles around a point (radius in degrees) without waiting. */
    public void prefetch(double lon, double lat, double radiusDegLon, double radiusDegLat) {
        prefetch(lon, lat, radiusDegLon, radiusDegLat, 1000);
    }

    public void prefetch(double lon, double lat, double radiusDegLon, double radiusDegLat, double priority) {
        if (!enabled() || offline()) return;
        var c = Earth.get().config;
        double step = Tiling.degPerPx(ELEV_Z) * Tiling.TILE * 0.5;
        for (double y = lat - radiusDegLat; y <= lat + radiusDegLat + 1e-9; y += step) {
            for (double x = lon - radiusDegLon; x <= lon + radiusDegLon + 1e-9; x += step) {
                double wx = ((x + 180) % 360 + 360) % 360 - 180;
                if (c.streaming.elevation) ensureTile(Tiling.LAYER_ELEV, ELEV_Z, wx, Math.max(-89.9, Math.min(89.9, y)), false, priority);
                if (c.streaming.land_cover) ensureTile(Tiling.LAYER_COVER, COVER_Z, wx, Math.max(-89.9, Math.min(89.9, y)), false, priority);
            }
        }
    }

    public int pending() {
        return inflight.size();
    }

    private String lastLogged = "";

    /** Once a minute, log progress when something changed (for diagnosing slow areas). */
    public void logIfChanged() {
        String st = status();
        String key = fetched.get() + "/" + empty.get() + "/" + errors.get();
        if (!key.equals(lastLogged)) {
            lastLogged = key;
            WorldBoundMod.LOG.info("WorldBound {}", st);
        }
    }

    // ---- fetching ---------------------------------------------------------------------------

    private void fetch(int layer, int z, int tx, int ty) {
        long k = key(layer, z, tx, ty);
        if (offline()) return;
        try {
            double res = Tiling.degPerPx(z);
            double west = tx * Tiling.TILE * res - 180.0, north = 90.0 - ty * Tiling.TILE * res;
            short[] px = new short[Tiling.TILE * Tiling.TILE];
            int nod = Tiling.nodata(layer);
            java.util.Arrays.fill(px, (short) nod);
            boolean any = false;
            boolean elev = layer == Tiling.LAYER_ELEV;
            Sampler smp = new Sampler(res);
            for (int y = 0; y < Tiling.TILE; y++) {
                double lat = north - (y + 0.5) * res;
                for (int x = 0; x < Tiling.TILE; x++) {
                    double lon = west + (x + 0.5) * res;
                    if (!smp.at(layer, lon, lat)) continue;
                    if (elev) {
                        // Bilinear between the source pixels around this point: nearest-pixel sampling from the
                        // 1" source onto our 1.24" grid skipped a source row/column every ~4 pixels, which showed
                        // in game as straight ridges and flat bands every ~40 blocks.
                        double v = smp.bilinear();
                        if (Double.isNaN(v) || v <= 0) continue; // sea and voids: GEBCO / packs show through
                        px[y * Tiling.TILE + x] = (short) Math.round(Math.min(9000, v));
                    } else {
                        float v = smp.nearest();
                        if (Float.isNaN(v)) continue;
                        int cls = Math.round(v);
                        if (cls <= 0 || cls > 100) continue;
                        px[y * Tiling.TILE + x] = (short) cls;
                    }
                    any = true;
                }
            }
            Path out = any ? tilePath(layer, z, tx, ty) : nonePath(layer, z, tx, ty);
            Files.createDirectories(out.getParent());
            Path tmp = out.resolveSibling(out.getFileName() + ".tmp");
            Files.write(tmp, any ? EtileCodec.encode(new Tile(layer, z, tx, ty, Tiling.TILE, Tiling.TILE, nod, px)) : new byte[0]);
            Files.move(tmp, out, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            ready.put(k, true);
            if (any) fetched.incrementAndGet(); else empty.incrementAndGet();
            failures.set(0);
            Earth.get().stack.invalidate(layer, z, tx, ty);
        } catch (IOException | RuntimeException e) {
            errors.incrementAndGet();
            if (failures.incrementAndGet() >= 3 && !offline()) {
                offlineUntil = System.currentTimeMillis() + 10 * 60_000L;
                failures.set(0);
                WorldBoundMod.LOG.warn("WorldBound streaming: can't reach the data servers ({}); using installed packs for 10 minutes", e.toString());
            }
        }
    }

    /**
     * Reads source pixels for one output tile, remembering the current COG and internal tile so the inner loop
     * doesn't look them up (or build keys) for every pixel.
     */
    private final class Sampler {
        private final double res;
        private long cell = Long.MIN_VALUE;
        private Cog cog;
        private int level;
        private Cog.Level lv;
        private double sx, sy;
        private final java.util.HashMap<Long, float[]> tiles = new java.util.HashMap<>();
        private int lastTx = Integer.MIN_VALUE, lastTy = Integer.MIN_VALUE;
        private float[] lastTile;
        private double fcol, frow;

        Sampler(double res) {
            this.res = res;
        }

        /** Position on the source raster for this point; false where no source file covers it. */
        boolean at(int layer, double lon, double lat) throws IOException {
            long c = ((long) Math.floor(lat) << 32) ^ (Math.floorMod((int) Math.floor(lon), 360));
            if (c != cell) {
                cell = c;
                String url = layer == Tiling.LAYER_ELEV ? gloUrl(lon, lat) : worldCoverUrl(lon, lat);
                cog = cog(url);
                tiles.clear();
                lastTx = lastTy = Integer.MIN_VALUE;
                lastTile = null;
                if (cog != null) {
                    level = cog.levelFor(res);
                    lv = cog.levels.get(level);
                    sx = cog.levelScaleX(level);
                    sy = cog.levelScaleY(level);
                }
            }
            if (cog == null) return false;
            fcol = (lon - cog.originX) / sx;
            frow = (cog.originY - lat) / sy;
            return fcol >= 0 && frow >= 0 && fcol < lv.width && frow < lv.height;
        }

        private float pixel(int col, int row) throws IOException {
            col = Math.max(0, Math.min(lv.width - 1, col));
            row = Math.max(0, Math.min(lv.height - 1, row));
            int itx = col / lv.tileW, ity = row / lv.tileH;
            float[] t;
            if (itx == lastTx && ity == lastTy) t = lastTile;
            else {
                long k = ((long) itx << 32) | (ity & 0xFFFFFFFFL);
                t = tiles.get(k);
                if (t == null && !tiles.containsKey(k)) {
                    t = cog.tile(level, itx, ity);
                    tiles.put(k, t);
                    if (t != null) bytes.addAndGet((long) t.length);
                }
                lastTx = itx;
                lastTy = ity;
                lastTile = t;
            }
            if (t == null) return Float.NaN;
            float v = t[(row % lv.tileH) * lv.tileW + (col % lv.tileW)];
            return cog.nodata != null && v == cog.nodata.floatValue() ? Float.NaN : v;
        }

        float nearest() throws IOException {
            return pixel((int) Math.floor(fcol), (int) Math.floor(frow));
        }

        /** Bilinear between pixel centres; NaN neighbours (voids, sea) fall back to the nearest pixel. */
        double bilinear() throws IOException {
            double gx = fcol - 0.5, gy = frow - 0.5;
            int c0 = (int) Math.floor(gx), r0 = (int) Math.floor(gy);
            double fx = gx - c0, fy = gy - r0;
            float a = pixel(c0, r0), b = pixel(c0 + 1, r0), c = pixel(c0, r0 + 1), d = pixel(c0 + 1, r0 + 1);
            if (Float.isNaN(a) || Float.isNaN(b) || Float.isNaN(c) || Float.isNaN(d)) return nearest();
            return (a * (1 - fx) + b * fx) * (1 - fy) + (c * (1 - fx) + d * fx) * fy;
        }
    }

    /** Stream cache format: 2 = elevation sampled bilinearly (0.4.31); older elevation tiles are dropped once. */
    private static final int CACHE_FORMAT = 2;
    /** "No data here" markers are rechecked after this long (a transient failure shouldn't hide an area forever). */
    private static final long NONE_MAX_AGE_MS = 30L * 24 * 3600 * 1000;

    /** Called once when the data directories are set up, before any tile is read from the cache. */
    public static void upgradeCache(Path root) {
        try {
            Path marker = root.resolve("format.txt");
            int have = 1;
            if (Files.isRegularFile(marker)) {
                try { have = Integer.parseInt(Files.readString(marker).trim()); } catch (NumberFormatException ignored) { }
            }
            if (have >= CACHE_FORMAT) return;
            Path layers = root.resolve("layers");
            int n = 0;
            if (Files.isDirectory(layers)) {
                java.util.List<Path> drop;
                try (var st = Files.walk(layers)) {
                    drop = st.filter(f -> {
                        String name = f.toString();
                        return name.endsWith(".none") || (name.endsWith(".etile") && f.getParent() != null
                                && f.getParent().getParent() != null && f.getParent().getParent().getFileName().toString().equals("elev"));
                    }).collect(java.util.stream.Collectors.toList());
                }
                for (Path f : drop) if (Files.deleteIfExists(f)) n++;
            }
            Files.createDirectories(root);
            Files.writeString(marker, CACHE_FORMAT + "\n");
            WorldBoundMod.LOG.info("WorldBound streaming: cache format {} -> {}, {} old elevation tiles / markers dropped (they re-download as you explore)", have, CACHE_FORMAT, n);
        } catch (IOException | RuntimeException e) {
            WorldBoundMod.LOG.warn("stream cache upgrade: {}", e.toString());
        }
    }

    private static boolean noneFresh(Path p) {
        try {
            if (!Files.exists(p)) return false;
            if (System.currentTimeMillis() - Files.getLastModifiedTime(p).toMillis() < NONE_MAX_AGE_MS) return true;
            Files.deleteIfExists(p);
        } catch (IOException ignored) {
        }
        return false;
    }

    private Cog cog(String url) throws IOException {
        if (missingFiles.containsKey(url)) return null;
        synchronized (cogs) {
            Cog c = cogs.get(url);
            if (c != null) return c;
        }
        try {
            Cog c = new Cog(Cog.http(url));
            synchronized (cogs) {
                cogs.put(url, c);
            }
            return c;
        } catch (FileNotFoundException e) {
            missingFiles.put(url, true); // ocean: no tile exists
            return null;
        }
    }

    static String gloUrl(double lon, double lat) {
        int la = (int) Math.floor(lat), lo = (int) Math.floor(lon);
        String n = String.format(Locale.ROOT, "Copernicus_DSM_COG_10_%s%02d_00_%s%03d_00_DEM",
                la >= 0 ? "N" : "S", Math.abs(la), lo >= 0 ? "E" : "W", Math.abs(lo));
        return GLO30 + n + "/" + n + ".tif";
    }

    static String worldCoverUrl(double lon, double lat) {
        int la = Math.floorDiv((int) Math.floor(lat), 3) * 3, lo = Math.floorDiv((int) Math.floor(lon), 3) * 3;
        return WORLDCOVER + String.format(Locale.ROOT, "ESA_WorldCover_10m_2021_v200_%s%02d%s%03d_Map.tif",
                la >= 0 ? "N" : "S", Math.abs(la), lo >= 0 ? "E" : "W", Math.abs(lo));
    }

    public String status() {
        return String.format(Locale.ROOT, "streaming %s%s: %d tiles fetched, %d empty, %d errors, %d pending, %.0f MB decoded",
                enabled() ? "on" : "off", offline() ? " (offline, retrying later)" : "", fetched.get(), empty.get(), errors.get(),
                pending(), bytes.get() * 4 / 1e6);
    }
}
