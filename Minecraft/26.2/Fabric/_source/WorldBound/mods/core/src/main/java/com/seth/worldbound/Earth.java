package com.seth.worldbound;

import com.google.gson.Gson;
import com.seth.worldbound.data.RasterStack;
import com.seth.worldbound.data.TileSource;
import com.seth.worldbound.data.Tiling;
import com.seth.worldbound.geo.GeoTransform;
import com.seth.worldbound.geo.HeightCurve;
import com.seth.worldbound.geo.ValueNoise;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Everything the worldgen hooks need, in one place: config, projection, height curve, data.
 * Built once at mod init ({@link WorldBoundMod}); the biome source and density functions are
 * created by codecs with no arguments, so they reach the data through {@link #get()}.
 */
public final class Earth {
    private static volatile Earth INSTANCE;

    /** True while an Earth world is running (set by the server lifecycle hooks) and seasons are enabled. */
    private static volatile boolean earthWorldRunning;

    public static void setEarthWorldRunning(boolean v) {
        earthWorldRunning = v;
    }

    /** Set on the client when the server says it is an Earth world (works on dedicated servers too). */
    private static volatile com.seth.worldbound.geo.GeoTransform remoteTransform;
    private static volatile boolean remoteSeasons;

    public static void setRemoteWorld(com.seth.worldbound.geo.GeoTransform t, boolean seasons) {
        remoteTransform = t;
        remoteSeasons = seasons;
    }

    /** An Earth world is being generated/played (server side, or a client told by its server). */
    public static boolean earthWorld() {
        return earthWorldRunning || remoteTransform != null;
    }

    /** Real-date seasons are on for the world being played (server config, or what the server told the client). */
    public static boolean seasonsActive() {
        Earth e = INSTANCE;
        if (e == null) return false;
        if (earthWorldRunning) return e.config.seasons;
        return remoteTransform != null && remoteSeasons;
    }

    private static volatile boolean wildMode;

    public static void setWildMode(boolean w) {
        wildMode = w;
    }

    /** Wild biome mode: real terrain, biomes from every installed biome mod in big patches. */
    public static boolean wildMode() {
        return wildMode;
    }

    /** Weather follows the real climate map in Earth worlds (always; seasons only add the yearly swing). */
    public static boolean climateWeather() {
        return INSTANCE != null && earthWorld();
    }

    /** The projection of the world being played: this server's, else the one the server sent. */
    public com.seth.worldbound.geo.GeoTransform activeTransform() {
        com.seth.worldbound.geo.GeoTransform r = remoteTransform;
        return earthWorldRunning || r == null ? transform : r;
    }

    public static Earth get() {
        Earth e = INSTANCE;
        if (e == null) throw new IllegalStateException("WorldBound not initialised");
        return e;
    }

    private static WorldBoundConfig BASE;
    private static Path GAME_DIR;
    private static final Map<String, WorldKind> KINDS = new java.util.concurrent.ConcurrentHashMap<>();

    public static void init(WorldBoundConfig cfg, Path gameDir) {
        BASE = cfg;
        GAME_DIR = gameDir;
        INSTANCE = new Earth(cfg, gameDir, defaultSource(cfg, gameDir));
    }

    /**
     * A world kind ("earth" from Earth Data World Gen, "earthish" from Earthish World Gen). Registering the real
     * Earth also makes it the data used before any world loads (the spawn picker, the map).
     */
    public static synchronized void registerKind(WorldKind k) {
        KINDS.put(k.id(), k);
        if (BASE != null && INSTANCE != null && INSTANCE.source == Source.NONE) {
            Source d = defaultSource(BASE, GAME_DIR);
            if (d != Source.NONE) INSTANCE = new Earth(INSTANCE.config, GAME_DIR, d);
        }
    }

    public static WorldKind kind(String id) {
        return KINDS.get(id);
    }

    public static boolean hasKind(String id) {
        return KINDS.containsKey(id);
    }

    private static Source defaultSource(WorldBoundConfig cfg, Path gameDir) {
        for (WorldKind k : KINDS.values()) {
            Source s = k.defaultSource(cfg, gameDir.resolve(cfg.data_dir));
            if (s != null) return s;
        }
        return Source.NONE;
    }

    /** The config file's settings (spawn choice, next world's scale); saved by the picker. */
    public static WorldBoundConfig baseConfig() {
        return BASE;
    }

    /** Switch to a world's own scale/origin (stored in its worldbound.json) on the default data (the real Earth). */
    public static synchronized void useWorld(double scale, double originLat, double originLon) {
        useWorld(scale, originLat, originLon, defaultSource(BASE, GAME_DIR));
    }

    /** Same, with the world's own data: a generated planet (Earthish) or the real Earth. */
    public static synchronized void useWorld(double scale, double originLat, double originLon, Source source) {
        Earth e = INSTANCE;
        if (e != null && e.config.scale_m_per_block == scale && e.config.origin_lat == originLat && e.config.origin_lon == originLon
                && e.source == source) return;
        WorldBoundConfig c = BASE.copy();
        c.scale_m_per_block = scale;
        c.origin_lat = originLat;
        c.origin_lon = originLon;
        INSTANCE = new Earth(c, GAME_DIR, source);
        WorldBoundMod.LOG.info("WorldBound: world scale 1:{} origin {},{}{}", scale, originLat, originLon,
                source.generated() ? " on Earthish planet " + source.seed() : "");
    }

    /** Back to the default data with the config's settings (leaving a remote Earthish server). */
    public static synchronized void resetToReal() {
        useWorld(BASE.scale_m_per_block, BASE.origin_lat, BASE.origin_lon, defaultSource(BASE, GAME_DIR));
        for (WorldKind k : KINDS.values()) k.unload();
    }

    /** Where this world's data comes from: the real Earth's datasets or a generated planet. */
    public final Source source;

    /** An Earthish world: a generated planet, not the real Earth (no real places, landmarks or downloads). */
    public boolean generated() {
        return source.generated();
    }

    /** The generated planet's seed (Earthish), 0 on the real Earth. */
    public long planetSeed() {
        return source.seed();
    }

    /** Where streamed tiles are cached. */
    public Path cacheDir() {
        return dataDir.resolve("cache");
    }

    /** A length given in blocks at 1:10 (how this mod was tuned), converted to this world's scale. */
    public double tuned(double blocksAt10) {
        return blocksAt10 * 10.0 / config.scale_m_per_block;
    }

    /** Everything we know about one block column. */
    public record Surface(double lon, double lat, double elevM, int surfaceY, int climate, int cover,
                          double slope, boolean nearOcean, int waterDepth, int veg, boolean dry, boolean cliff) {
        /** Open sea: below sea level, not a lake/river, not a dry depression (Death Valley, Qattara...). */
        public boolean ocean() {
            return elevM < 0 && cover != 80 && !dry;
        }

        /** Top solid block (under any river/lake water). */
        public int solidTop() {
            return surfaceY - waterDepth;
        }
    }

    /**
     * A world's data: fills the raster stack with its layers (elevation, climate, cover...) and may supply rivers.
     * Earth Data World Gen supplies the real Earth; Earthish World Gen a generated planet.
     */
    public interface Source {
        /** Nothing installed that supplies a world: every layer reads as "no data". */
        Source NONE = (e, stack, packs) -> packs.add("no world data (install Earth Data World Gen or Earthish World Gen)");

        void register(Earth earth, RasterStack stack, List<String> packs);

        default com.seth.worldbound.data.Rivers rivers() { return null; }

        default boolean generated() { return false; }

        default long seed() { return 0; }
    }

    public final WorldBoundConfig config;
    public final GeoTransform transform;
    public final HeightCurve curve;
    public final RasterStack stack;
    public final Path dataDir;
    public final List<String> packs = new ArrayList<>();
    /** River channels: HydroRIVERS (rivers.bin in the data dir) or the planet's own; null when there are none. */
    public final com.seth.worldbound.data.Rivers rivers;
    private final ValueNoise jitter = new ValueNoise(0x5EEDL);
    private final ValueNoise rough = new ValueNoise(0x7E44A1L);
    private final ValueNoise caves = new ValueNoise(0xCA7E5L);

    private Earth(WorldBoundConfig cfg, Path gameDir, Source source) {
        this.config = cfg;
        this.source = source;
        this.transform = new GeoTransform(cfg.origin_lat, cfg.origin_lon, cfg.scale_m_per_block);
        this.curve = cfg.curve();
        this.stack = new RasterStack((long) cfg.tile_cache_mb << 20, WorldBoundMod.LOG::warn);
        this.dataDir = gameDir.resolve(cfg.data_dir);
        source.register(this, stack, packs);
        this.rivers = source.rivers();
    }

    /** Add one pack's layers to the stack (for sources). */
    public void register(TileSource.PackInfo info, TileSource src) {
        double[] b = info.bounds() == null ? new double[]{-180, -90, 180, 90} : info.bounds();
        for (Map.Entry<String, Integer> e : info.layers().entrySet()) {
            stack.add(new RasterStack.Spec(Tiling.layerId(e.getKey()), e.getValue(), src, b[0], b[1], b[2], b[3]));
        }
    }

    // ---- sampling ---------------------------------------------------------------------------

    /** Per-thread cache of the last few chunks' columns; worldgen asks for the same columns many times. */
    private static final class ColumnCache {
        final long[] keys = new long[8];
        final Surface[][] cols = new Surface[8][];
        int next;
        long generation = -1;
        ColumnCache() { java.util.Arrays.fill(keys, Long.MIN_VALUE); }
    }

    private final ThreadLocal<ColumnCache> cache = ThreadLocal.withInitial(ColumnCache::new);

    public Surface at(int blockX, int blockZ) {
        blockX = transform.wrapX(blockX);
        long ck = ((long) (blockX >> 4) << 32) ^ ((blockZ >> 4) & 0xFFFFFFFFL);
        ColumnCache c = cache.get();
        long g = stack.generation();
        if (c.generation != g) { // new data landed: forget columns computed from the old
            java.util.Arrays.fill(c.keys, Long.MIN_VALUE);
            java.util.Arrays.fill(c.cols, null);
            c.generation = g;
        }
        Surface[] col = null;
        for (int i = 0; i < 8; i++) if (c.keys[i] == ck) { col = c.cols[i]; break; }
        if (col == null) {
            col = new Surface[256];
            c.keys[c.next] = ck;
            c.cols[c.next] = col;
            c.next = (c.next + 1) & 7;
        }
        int idx = ((blockZ & 15) << 4) | (blockX & 15);
        Surface s = col[idx];
        if (s == null) col[idx] = s = compute(blockX, blockZ);
        return s;
    }

    /** Finest tile zoom worth reading when samples are {@code spacingBlocks} apart (pixels about half the spacing). */
    public int zoomForSpacing(double spacingBlocks) {
        double m = Math.max(1, spacingBlocks * config.scale_m_per_block / 2);
        return (int) Math.floor(Math.log(40_075_000.0 / (Tiling.TILE * m)) / Math.log(2));
    }

    /**
     * A cheap surface for distant views (Distant Horizons LODs) when samples are {@code spacing} blocks apart:
     * reads only map layers as fine as the spacing, and skips the coast, river-valley and lake-shore searches
     * that only matter block by block. Close up (spacing under 8) it is the same as {@link #at}.
     */
    public Surface lodAt(int blockX, int blockZ, int spacing) {
        if (spacing < 8) return at(blockX, blockZ);
        blockX = transform.wrapX(blockX);
        int mz = zoomForSpacing(spacing);
        double lon = transform.lon(blockX + 0.5), lat = transform.lat(blockZ + 0.5);
        double elev = stack.elevation(lon, lat, config.missing_data_elevation_m, mz);
        int climate = stack.climate(lon, lat, mz);
        int cover = stack.cover(lon, lat, mz);
        if (cover == 80) cover = 0;
        int veg = elev > -500 ? stack.veg(lon, lat, mz) : 0;
        boolean dry = elev < 0 && elev > -500 && climate != 0 && (veg != 0 || stack.specs(Tiling.LAYER_VEG).isEmpty()) && veg != 80;
        if (dry && elev > -15) elev = 0.5;
        else if (dry) {
            // As in compute(): a coastal pixel where the 1 km climate map overlaps the sea isn't a depression.
            double d = 2000.0 / GeoTransform.M_PER_DEG_LAT, dLo2 = d / Math.max(0.05, Math.cos(Math.toRadians(lat)));
            for (int i = 0; i < 4 && dry; i++) {
                double a = i * Math.PI / 2, qlon = lon + Math.cos(a) * dLo2, qlat = lat + Math.sin(a) * d;
                if (stack.climate(qlon, qlat, mz) == 0 && stack.elevation(qlon, qlat, 0, mz) < 0) dry = false;
            }
        }
        double y = curve.y(elev);
        int surfaceY = (int) Math.round(Math.max(curve.minY + 1, Math.min(curve.maxY, y)));
        double dLon = transform.degLonPerBlock() * spacing, dLat = transform.degLatPerBlock() * spacing;
        double ex = stack.elevation(lon + dLon, lat, elev, mz) - stack.elevation(lon - dLon, lat, elev, mz);
        double ez = stack.elevation(lon, lat + dLat, elev, mz) - stack.elevation(lon, lat - dLat, elev, mz);
        // Same units as compute(): rise per 1:10-scale block, which every steepness threshold was tuned in.
        double slope = Math.hypot(ex, ez) / curve.metresPerBlockAt(elev) / (2.0 * spacing) * tuned(1);
        int waterDepth = 0;
        int lakeM = stack.lakeSurface(lon, lat, mz);
        if (lakeM != Tiling.NODATA_ELEV) {
            int top = (int) Math.floor(curve.y(lakeM));
            if (top > curve.seaLevelY || lakeM < 0) {
                waterDepth = (int) Math.max(1, Math.min(400, Math.round(Math.max(1, stack.lakeDepth(lon, lat)) / curve.metresPerBlockAt(lakeM))));
                surfaceY = top;
                cover = 80;
            }
        }
        boolean nearOcean = elev >= 0 && elev < 8;
        return new Surface(lon, lat, elev, surfaceY, climate, cover, slope, nearOcean, waterDepth, veg, dry, false);
    }

    private Surface compute(int blockX, int blockZ) {
        double lon = transform.lon(blockX + 0.5), lat = transform.lat(blockZ + 0.5);
        double elev = stack.elevationSmooth(lon, lat, config.missing_data_elevation_m);

        // Class layers (climate ~1 km, vegetation ~300 m, land cover 10 m) are each looked up at a point warped by
        // noise scaled to that layer's own pixel size, so their edges come out as irregular natural boundaries
        // instead of squares the size of a data pixel.
        int climate, cover, veg;
        if (config.class_jitter_px > 0) {
            double[] w = WARP.get();
            classWarp(w, blockX, blockZ, Tiling.LAYER_CLIMATE, lon, lat, 0);
            climate = stack.climate(lon + w[0], lat + w[1]);
            classWarp(w, blockX, blockZ, Tiling.LAYER_COVER, lon, lat, 1);
            cover = stack.cover(lon + w[0], lat + w[1]);
            classWarp(w, blockX, blockZ, Tiling.LAYER_VEG, lon, lat, 2);
            veg = elev > -500 ? stack.veg(lon + w[0], lat + w[1]) : 0;
        } else {
            climate = stack.climate(lon, lat);
            cover = stack.cover(lon, lat);
            veg = elev > -500 ? stack.veg(lon, lat) : 0;
        }
        if (cover == 80) cover = 0; // mapped rivers and lakes decide water; land-cover "water" pixels don't

        // Land below sea level: the climate map (land only) and land cover agree it is land. Shallow ones
        // (coastal smear, polders) become shore at sea level; deep ones stay dry (Death Valley, Dead Sea shore).
        boolean dry = false;
        if (elev < 0 && elev > -500 && climate != 0 && (veg != 0 || stack.specs(Tiling.LAYER_VEG).isEmpty()) && veg != 80) {
            if (elev > -15) elev = 0.5;
            else {
                // Coastal pixels where the 1 km climate map overlaps the sea are not depressions:
                // dry only if no open sea within ~2 km.
                double d = 2000.0 / GeoTransform.M_PER_DEG_LAT, dLo2 = d / Math.max(0.05, Math.cos(Math.toRadians(lat)));
                dry = true;
                for (int i = 0; i < 8 && dry; i++) {
                    double a = i * Math.PI / 4, qlon = lon + Math.cos(a) * dLo2, qlat = lat + Math.sin(a) * d;
                    if (stack.climate(qlon, qlat) == 0 && stack.elevation(qlon, qlat, 0) < 0) dry = false;
                }
            }
        }

        // Coasts: sea within 300 m (700 m for low ground) makes a shore. Where the land climbs >80 m within
        // 700 m it is a cliff coast: the strip next to the sea is raised toward the inland height so the drop
        // happens at the water (Dover, Big Sur, Moher) instead of a long smeared slope.
        boolean nearOcean = false, cliff = false;
        if (elev >= 0 && elev < 150 && !dry) {
            double mLat = 1.0 / GeoTransform.M_PER_DEG_LAT, mLon = mLat / Math.max(0.05, Math.cos(Math.toRadians(lat)));
            boolean sea300 = false, sea700 = false;
            double inland = elev;
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4, ca = Math.cos(a), sa = Math.sin(a);
                double e3 = stack.elevation(lon + ca * 300 * mLon, lat + sa * 300 * mLat, elev);
                double e7 = stack.elevation(lon + ca * 700 * mLon, lat + sa * 700 * mLat, elev);
                if (e3 < 0) sea300 = true; else inland = Math.max(inland, e3);
                if (e7 < 0) sea700 = true; else inland = Math.max(inland, e7);
            }
            if (sea300 || (sea700 && elev < 8)) {
                if (inland - elev > 40 && sea300) {
                    cliff = true;
                    elev = Math.max(elev, inland * 0.75);
                } else if (elev < 20) {
                    nearOcean = true;
                } else if (elev >= 40) {
                    cliff = true;
                }
                nearOcean |= cliff;
            }
        }

        // Slope: elevation change over ±4 blocks in x and z, expressed as blocks of rise per block.
        // ±40 m whatever the scale, so slope means the same real steepness in every world (tuned at 1:10).
        double dLon = transform.degLonPerBlock() * tuned(4), dLat = transform.degLatPerBlock() * tuned(4);
        double ex = stack.elevation(lon + dLon, lat, elev) - stack.elevation(lon - dLon, lat, elev);
        double ez = stack.elevation(lon, lat + dLat, elev) - stack.elevation(lon, lat - dLat, elev);
        double mPerBlock = curve.metresPerBlockAt(elev);
        double slope = Math.hypot(ex, ez) / mPerBlock / 8.0;

        double y = curve.y(elev);
        if (elev > 0) {
            if (config.roughness_blocks > 0)
                y += rough.fbm(blockX, blockZ, tuned(config.roughness_wavelength_blocks)) * config.roughness_blocks;
            y += microRelief(blockX, blockZ, elev, slope);
        }
        int surfaceY = (int) Math.round(Math.max(curve.minY + 1, Math.min(curve.maxY, y)));

        int waterDepth = 0;
        if (rivers != null && elev > 0) {
            com.seth.worldbound.data.Rivers.Hit h = rivers.query(lon, lat);
            if (h != null) {
                // Rivers sit on the valley floor: the lowest ground within ~300 m (coarse DEMs smear valleys
                // up their sides), one block below it so the water never sits proud of its banks.
                double r = 300.0;
                double dLa = r / GeoTransform.M_PER_DEG_LAT, dLo = r / (GeoTransform.M_PER_DEG_LON_EQUATOR * Math.max(0.05, Math.cos(Math.toRadians(lat))));
                double floor = Math.min(elev, Math.min(
                        Math.min(stack.elevation(lon + dLo, lat, elev), stack.elevation(lon - dLo, lat, elev)),
                        Math.min(stack.elevation(lon, lat + dLa, elev), stack.elevation(lon, lat - dLa, elev))));
                int waterTop = (int) Math.floor(Math.min(curve.y(Math.max(floor, 0)) - 1, surfaceY - 1));
                if (waterTop > curve.seaLevelY) {
                    double depthBlocks = h.depthM() / curve.metresPerBlockAt(floor) + 1;
                    waterDepth = (int) Math.max(1, Math.round(depthBlocks * Math.sqrt(h.centreFactor())));
                    waterDepth = Math.min(waterDepth, 16);
                    surfaceY = waterTop;
                    cover = 80;
                }
            }
        }
        if (waterDepth == 0 && elev > -500) { // lakes down to the Dead Sea's level; not the open ocean
            // Lakes (HydroLAKES): ragged shores via the same jitter as climate, at lake-pixel scale.
            double lj = Tiling.degPerPx(10) * 0.6;
            double llon = lon + jitter.fbm(blockX + 333, blockZ, 18) * lj, llat = lat + jitter.fbm(blockX, blockZ - 333, 18) * lj;
            int lakeM = stack.lakeSurface(llon, llat, Integer.MAX_VALUE);
            if (lakeM != Tiling.NODATA_ELEV) {
                int top = (int) Math.floor(curve.y(lakeM));
                // Lakes below sea level (Caspian, Dead Sea, Salton Sea) keep their own level too.
                if (top > curve.seaLevelY || lakeM < 0) {
                    // Shallower toward shore: how much of the ground 120 m / 400 m around is also lake.
                    int in = 0;
                    for (double r : new double[]{120, 400}) {
                        double dLa = r / GeoTransform.M_PER_DEG_LAT, dLo = r / (GeoTransform.M_PER_DEG_LON_EQUATOR * Math.max(0.05, Math.cos(Math.toRadians(lat))));
                        if (stack.lakeSurface(llon + dLo, llat, Integer.MAX_VALUE) != Tiling.NODATA_ELEV) in++;
                        if (stack.lakeSurface(llon - dLo, llat, Integer.MAX_VALUE) != Tiling.NODATA_ELEV) in++;
                        if (stack.lakeSurface(llon, llat + dLa, Integer.MAX_VALUE) != Tiling.NODATA_ELEV) in++;
                        if (stack.lakeSurface(llon, llat - dLa, Integer.MAX_VALUE) != Tiling.NODATA_ELEV) in++;
                    }
                    double f = (in + 1) / 9.0;
                    double depthM = Math.max(1, stack.lakeDepth(llon, llat)) * f * 1.6;
                    waterDepth = (int) Math.max(1, Math.min(400, Math.round(depthM / curve.metresPerBlockAt(lakeM))));
                    surfaceY = top;
                    cover = 80;
                }
            }
        }
        return new Surface(lon, lat, elev, surfaceY, climate, cover, slope, nearOcean, waterDepth, veg, dry, cliff);
    }

    private final com.seth.worldbound.geo.GradientNoise warpX = new com.seth.worldbound.geo.GradientNoise(0xC1A55E5L),
            warpZ = new com.seth.worldbound.geo.GradientNoise(0x5EC0DDL), relief = new com.seth.worldbound.geo.GradientNoise(0x7E4A11FL);
    private static final ThreadLocal<double[]> WARP = ThreadLocal.withInitial(() -> new double[2]);

    /** Lookup offset (degrees) for a class layer: ~0.7 of its pixel, varying over ~1.7 pixels, three octaves. */
    private void classWarp(double[] out, int blockX, int blockZ, int layer, double lon, double lat, int salt) {
        double res = stack.finestResolution(layer, lon, lat);
        double pxBlocks = Math.max(1, res / transform.degLonPerBlock());
        double wl = Math.max(12, pxBlocks * 1.7), amp = Math.max(config.class_jitter_px, 0.7) * res;
        double ox = salt * 1013.0, oz = -salt * 977.0;
        out[0] = warpX.fbm(blockX + ox, blockZ + oz, wl, 3) * amp;
        out[1] = warpZ.fbm(blockX - oz, blockZ + ox, wl, 3) * amp;
    }

    /**
     * Ground texture the 30-90 m data can't have, in blocks: single-block bumps everywhere on land (none on flat
     * shores), and on steep ground ribs, ledges and gullies that grow with the steepness. {@code slope} is in this
     * class's units (rise per 1:10 block).
     */
    double microRelief(int blockX, int blockZ, double elevM, double slope) {
        double shore = Math.min(1, elevM / 25.0);
        double h = relief.fbm(blockX, blockZ, 11, 2) * 0.55 * shore;
        double crag = Math.max(0, Math.min(1, (slope - 1.4) / 2.5));
        if (crag > 0) h += (relief.ridged(blockX + 9000, blockZ - 9000, 26, 3) - 0.45) * 7.0 * crag;
        return h;
    }

    private final ValueNoise patches = new ValueNoise(0x9A7C4L);

    /** Large-blob noise in [-1, 1] for mixing regional biomes in swaths; wavelength in blocks. */
    public double patchNoise(int blockX, int blockZ, double wavelength) {
        return Math.max(-1, Math.min(1, patches.fbm(blockX, blockZ, wavelength) * 1.6));
    }

    /** Low-frequency 2-D noise in [-1, 1] used to pick cave biomes; wavelength ~200 blocks. */
    public double caveNoise(int blockX, int blockZ) {
        return caves.fbm(blockX, blockZ, 200);
    }
}
