package com.seth.worldbound;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.seth.worldbound.geo.HeightCurve;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** config/worldbound.json — defaults are the locked decisions in CLAUDE.md. */
public final class WorldBoundConfig {
    static final int CONFIG_VERSION = 2;
    /** Bumped when a default changes in a way existing config files should pick up (see load()). 0 = written before versioning. */
    public int config_version = 0;
    /** Scale of worlds created before per-world scale existed (0.4.11 and older). Each world now stores its own. */
    public double scale_m_per_block = 10.0;
    /** Scale for the next new Earth world (metres per block), chosen in the spawn picker. 3 = true proportions. */
    public double new_world_scale = 3.0;
    public double origin_lat = 42.87;
    public double origin_lon = -112.44;
    public int sea_level_y = -700;
    /** Real-date seasons (by hemisphere) add a yearly temperature swing; off = each place's typical climate all year. */
    public boolean seasons = false;
    /** Fast column fill (replaces per-block noise fill for Earth worlds). */
    public boolean fast_fill = true;
    /** Caves under the surface (cheese caves and tunnels), needs fast_fill. */
    public boolean caves = true;
    /** Vanilla's cave tunnels and ravines as well (carved space is air in rock, water under the sea; never lava). */
    public boolean vanilla_caves = true;
    /** Crossing ±180° longitude (mid-Pacific) moves you to the other side of the world: sail around the Earth. */
    public boolean wrap_around = true;
    /** Forests as dense or sparse as real tree cover (needs the treecover pack). */
    public boolean tree_density = true;
    /** Biomes in the next new world: "realistic" (real climate) or "wild" (every installed biome, in big patches). */
    public String new_world_biome_mode = "realistic";
    /** Add-ons' world options for the next new world (dinosaur placement, landmark obelisks...), set on the spawn screen. */
    public java.util.Map<String, String> new_world_options = new java.util.LinkedHashMap<>();
    /** Developer commands (/earth bench, scan, seamtest, structcheck, column, lod, test, /earthshot). Also -Dworldbound.debug=true. */
    public boolean debug_commands = false;
    /** Salt for the wild-mode patch layout. */
    public long wild_seed = 0x5EED;
    /** With Distant Horizons: build its distant terrain straight from Earth data instead of generating full chunks. */
    public boolean dh_lod_generator = true;
    /** With Distant Horizons in an Earth world: no see-through holes when flying high, no cave interiors in the cut-out. */
    public boolean dh_display_tweaks = true;
    /** Compass HUD shown (toggle with J; remembered). */
    public boolean hud = true;
    /** WorldBound's own distant terrain, drawn straight from the Earth data (no Distant Horizons needed). */
    public Horizon horizon = new Horizon();
    public static final class Horizon {
        public boolean enabled = true;
        /** How far out the horizon is drawn, in blocks (32,768 ≈ 100 km at 1:3). */
        public int range_blocks = 32768;
        /** Tiles split into finer ones while the camera is closer than detail × tile size; higher = sharper, heavier. */
        public double detail = 2.0;
        /** The horizon starts at this fraction of the render distance (the real chunks cover the rest). */
        public double overdraw = 0.6;
        /** Background threads building horizon tiles. */
        public int threads = 2;
        /** Also draw it while Distant Horizons is installed (normally DH takes over). */
        public boolean with_distant_horizons = false;
    }

    /** Where new Earth worlds spawn; null = the origin. Set from the picker on the Create World screen. */
    public Double spawn_lat = null;
    public Double spawn_lon = null;
    /** World floor and height: oceans are clamped at 6,000 m with trenches cut below that, so the floor sits at -1504 (3,536 tall). */
    public int min_y = -1504;
    public int height = 3536;
    /** The deepest trench floors (below 10,000 m) open onto the void; false = flat bedrock there instead. */
    public boolean trench_void = true;
    public Curve height_curve = new Curve();
    public String data_dir = "worldbound";
    public Streaming streaming = new Streaming();
    public int tile_cache_mb = 512;
    /** Elevation used where no data covers a point at all (deep ocean) — only matters before build_base.py has run. */
    public double missing_data_elevation_m = -4000;
    /** Amplitude of the terrain roughness noise, in blocks. 0 disables. */
    public double roughness_blocks = 1.5;
    public double roughness_wavelength_blocks = 40;
    /** Jitter applied to class lookups, in source pixels, so biome edges are ragged. */
    public double class_jitter_px = 0.4;

    public static final class Curve {
        public List<double[]> land = new ArrayList<>(List.of(new double[]{3000, 3.0}, new double[]{8849, 3.4}));
        public List<double[]> ocean = new ArrayList<>(List.of(new double[]{200, 2.0}, new double[]{6000, 8.8}, new double[]{11034, 150}));
    }

    /** Download fine data (Copernicus 30 m terrain, ESA WorldCover 10 m land cover) from AWS as you explore. */
    public static final class Streaming {
        public boolean enabled = true;
        public boolean elevation = true;
        public boolean land_cover = true;
        public int wait_seconds = 20; // unused (pre-0.4.15), see near_wait_seconds
        /** How long generating a chunk near a player waits for a tile that is still downloading before using coarser data. */
        public int near_wait_seconds = 4;
        /** Only chunks this close to a player wait for downloads; farther ones (and Distant Horizons) never wait. */
        public int wait_radius_blocks = 320;
        public int threads = 3;
        /** Prefetch this many blocks beyond the view distance around each player. */
        public int prefetch_blocks = 768;
        public int prefetch_radius_chunks = 48; // unused (pre-0.4.13)
        /** Streamed tiles beyond this are trimmed, oldest first. */
        public int max_cache_mb = 4096;
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static WorldBoundConfig load(Path file) {
        WorldBoundConfig cfg = new WorldBoundConfig();
        try {
            WorldBoundConfig read = null;
            try {
                read = com.seth.worldbound.util.SafeFiles.read(file, t -> GSON.fromJson(t, WorldBoundConfig.class));
            } catch (IOException e) {
                WorldBoundMod.LOG.error("config {}: {} — using defaults for this session (file left as is)", file, e.toString());
                cfg.file = null;
                return cfg;
            }
            if (read != null) {
                cfg = read;
                // 0.4.27: the world floor rose from -2032 to -1504 (oceans clamped at 6 km, trenches cut below); old
                // configs carry the old numbers explicitly, so bring them up to date.
                if (cfg.min_y == -2032 && cfg.height == 4064) {
                    cfg.min_y = -1504;
                    cfg.height = 3536;
                    cfg.height_curve.ocean = new ArrayList<>(List.of(new double[]{200, 2.0}, new double[]{6000, 8.8}, new double[]{11034, 150}));
                    WorldBoundMod.LOG.info("WorldBound: config updated to the 3,536-block world (6 km ocean clamp, trenches)");
                }
            }
            // Defaults that changed after a config file was first written stay at their old explicit values unless
            // migrated here (Gson keeps whatever the file says).
            if (cfg.config_version < 2) {
                cfg.seasons = false; // 0.4.21: typical climate all year by default (Seth)
                if (cfg.tile_cache_mb == 256) cfg.tile_cache_mb = 512; // 0.4.17 default
                WorldBoundMod.LOG.info("WorldBound: config migrated to version 2 (seasons off)");
            }
            cfg.config_version = CONFIG_VERSION;
            com.seth.worldbound.util.SafeFiles.writeAtomic(file, GSON.toJson(cfg)); // writes defaults / fills in new keys
            cfg.file = file;
        } catch (IOException e) {
            WorldBoundMod.LOG.warn("config {}: {}", file, e.toString());
        }
        return cfg;
    }

    private transient Path file;

    public double spawnLat() {
        return spawn_lat == null ? origin_lat : spawn_lat;
    }

    public double spawnLon() {
        return spawn_lon == null ? origin_lon : spawn_lon;
    }

    /** A detached copy (never saved) for one world's settings. */
    public WorldBoundConfig copy() {
        return GSON.fromJson(GSON.toJson(this), WorldBoundConfig.class);
    }

    public void save() {
        if (file == null) return;
        try {
            com.seth.worldbound.util.SafeFiles.writeAtomic(file, GSON.toJson(this));
        } catch (IOException e) {
            WorldBoundMod.LOG.warn("config save {}: {}", file, e.toString());
        }
    }

    public HeightCurve curve() {
        return new HeightCurve(sea_level_y, min_y, min_y + height - 1, height_curve.land, height_curve.ocean);
    }
}
