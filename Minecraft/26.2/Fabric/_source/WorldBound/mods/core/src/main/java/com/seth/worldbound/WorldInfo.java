package com.seth.worldbound;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** {@code <world>/worldbound.json}: the settings this Earth world was made with (scale, origin, spawn). */
public final class WorldInfo {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public double scale_m_per_block;
    public double origin_lat, origin_lon;
    public int sea_level_y;
    public double spawn_lat, spawn_lon;
    /** Absent in files written before 0.4.12, where the spawn was already set when the file was written. */
    public boolean spawn_set = true;
    /** Landmark obelisks (fast travel between discovered landmarks). Worlds made before 0.4.19 get them too. */
    public boolean landmarks = true;
    /** "realistic" or "wild". */
    public String biome_mode = "realistic";
    /** "accurate" (dinosaurs at their fossil sites, or home ranges on Earthish) or "random" (any of them anywhere they can live). */
    public String dino_placement = "accurate";
    /** Add-ons' per-world options (see api.WorldOptions): "dino_placement", "landmarks", "city_walls"... */
    public java.util.Map<String, String> options = new java.util.LinkedHashMap<>();
    /** "earth" (the real one) or "earthish" (a planet generated from {@link #planet_seed}). */
    public String planet = "earth";
    public long planet_seed;
    /** Generator version the planet was made with (Planet.VERSION). */
    public int planet_version;
    private transient Path file;

    /**
     * Read this world's file, or create it: a brand-new world takes the picker's scale and spawn, an older
     * world without a file gets the pre-0.4.12 global settings so its terrain doesn't change.
     */
    public static synchronized WorldInfo forWorld(MinecraftServer server, boolean fresh) {
        Path f = server.getWorldPath(LevelResource.ROOT).resolve("worldbound.json");
        WorldInfo w = null;
        boolean unreadable = false;
        try {
            w = com.seth.worldbound.util.SafeFiles.read(f, s -> GSON.fromJson(s, WorldInfo.class));
        } catch (IOException e) {
            unreadable = true;
            WorldBoundMod.LOG.error("WorldBound: can't read this world's settings ({}). The world will use your current new-world"
                    + " settings for this session WITHOUT changing the file; fix or restore worldbound.json (a .bak copy may be next to it).", e.toString());
        }
        if (w != null && w.scale_m_per_block <= 0) { // a file that parses but makes no sense: same as unreadable
            unreadable = true;
            WorldBoundMod.LOG.error("WorldBound: this world's worldbound.json has no valid scale; leaving it untouched");
        }
        WorldBoundConfig base = Earth.baseConfig();
        if (w == null || w.scale_m_per_block <= 0) {
            boolean legacy = !unreadable && !fresh; // a world from before 0.4.12 really has no file
            w = new WorldInfo();
            w.scale_m_per_block = legacy ? base.scale_m_per_block : base.new_world_scale;
            w.origin_lat = base.origin_lat;
            w.origin_lon = base.origin_lon;
            w.sea_level_y = base.sea_level_y;
            w.spawn_lat = base.spawnLat();
            w.spawn_lon = base.spawnLon();
            w.spawn_set = !fresh;
            w.biome_mode = fresh ? base.new_world_biome_mode : "realistic";
            if (unreadable) {
                w.file = null; // never overwrite a file we couldn't read
                return w;
            }
            w.file = f;
            w.save();
            WorldBoundMod.LOG.info("WorldBound: {} Earth world, scale 1:{}", fresh ? "new" : "existing", w.scale_m_per_block);
        }
        w.file = f;
        return w;
    }

    public void save() {
        if (file == null) return;
        try {
            com.seth.worldbound.util.SafeFiles.writeAtomic(file, GSON.toJson(this));
        } catch (IOException e) {
            WorldBoundMod.LOG.warn("world settings {}: {}", file, e.toString());
        }
    }
}
