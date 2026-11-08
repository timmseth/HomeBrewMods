package com.seth.worldbound;

import com.seth.worldbound.command.EarthCommands;
import com.seth.worldbound.worldgen.EarthBiomeSource;
import com.seth.worldbound.worldgen.EarthDensityFunctions;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WorldBoundMod implements ModInitializer {
    public static final String ID = "worldbound";
    public static final Logger LOG = LoggerFactory.getLogger("WorldBound");

    private static boolean initialised;

    @Override
    public void onInitialize() {
        init();
    }

    /**
     * Set up the engine once. The world mods (Earth Data, Earthish) and add-ons call this first from their own
     * initialisers, since Fabric doesn't promise which mod's entrypoint runs first.
     */
    public static synchronized void init() {
        if (initialised) return;
        initialised = true;
        FabricLoader loader = FabricLoader.getInstance();
        WorldBoundConfig cfg = WorldBoundConfig.load(loader.getConfigDir().resolve("worldbound.json"));
        Earth.init(cfg, loader.getGameDir());
        LOG.info("WorldBound engine: sea y{} world height {}", cfg.sea_level_y, cfg.height);

        Registry.register(BuiltInRegistries.BIOME_SOURCE, Identifier.fromNamespaceAndPath(ID, "raster"), EarthBiomeSource.CODEC);
        EarthDensityFunctions.register();
        Registry.register(BuiltInRegistries.FEATURE, Identifier.fromNamespaceAndPath(ID, "river_water"), new com.seth.worldbound.worldgen.RiverWaterFeature());
        Registry.register(BuiltInRegistries.FEATURE, Identifier.fromNamespaceAndPath(ID, "trench"), new com.seth.worldbound.worldgen.TrenchFeature());
        net.fabricmc.fabric.api.biome.v1.BiomeModifications.addFeature(net.fabricmc.fabric.api.biome.v1.BiomeSelectors.all(),
                net.minecraft.world.level.levelgen.GenerationStep.Decoration.RAW_GENERATION,
                net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath(ID, "trench")));
        net.fabricmc.fabric.api.biome.v1.BiomeModifications.addFeature(net.fabricmc.fabric.api.biome.v1.BiomeSelectors.all(),
                net.minecraft.world.level.levelgen.GenerationStep.Decoration.RAW_GENERATION,
                net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath(ID, "river_water")));

        Compat.registerPacks(loader);
        Compat.initDistantHorizons(loader);
        com.seth.worldbound.net.EarthNet.registerServer();
        // As soon as the overworld exists (before spawn chunks generate): switch to this world's own scale, origin and
        // data from <world>/worldbound.json, creating it for a new world from the spawn screen's choices.
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents.LOAD.register((server, level) -> {
            if (level.dimension() != Level.OVERWORLD || !(level.getChunkSource().getGenerator().getBiomeSource() instanceof EarthBiomeSource ebs)) return;
            boolean fresh = level.getGameTime() == 0;
            WorldInfo w = WorldInfo.forWorld(server, fresh);
            String kindId = ebs.earthish() ? "earthish" : "earth";
            WorldKind kind = Earth.kind(kindId);
            Earth.Source src;
            if (kind == null) {
                LOG.error("This world is {} but {} isn't installed: it would generate as empty ocean. Install it and reopen the world.",
                        kindId, ebs.earthish() ? "Earthish World Gen" : "Earth Data World Gen");
                src = Earth.Source.NONE;
            } else {
                boolean first = ebs.earthish() ? !"earthish".equals(w.planet) : true;
                src = kind.load(server, level, w, first && (ebs.earthish() || fresh));
            }
            for (WorldKind k : java.util.List.of("earth", "earthish").stream().map(Earth::kind).filter(java.util.Objects::nonNull).toList())
                if (k != kind) k.unload();
            Earth.useWorld(w.scale_m_per_block, w.origin_lat, w.origin_lon, src);
            Earth.setWildMode("wild".equals(w.biome_mode));
            com.seth.worldbound.api.WorldOptions.worldLoaded(w, fresh);
            Earth.setEarthWorldRunning(true);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            Earth.setEarthWorldRunning(false);
            Earth.setWildMode(false);
            com.seth.worldbound.api.WorldOptions.worldClosed();
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, ctx, selection) -> EarthCommands.register(dispatcher));

        // First start of an Earth world: spawn at the spawn screen's place (or the origin), once.
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ServerLevel ow = server.overworld();
            if (!(ow.getChunkSource().getGenerator().getBiomeSource() instanceof EarthBiomeSource)) return;
            WorldInfo w = WorldInfo.forWorld(server, false);
            if (w.spawn_set) return;
            setSpawn(ow, w.spawn_lat, w.spawn_lon);
            w.spawn_set = true;
            w.save();
        });
    }

    /** Set the overworld spawn to the surface at a lat/lon. */
    public static int setSpawn(ServerLevel level, double lat, double lon) {
        Earth earth = Earth.get();
        int x = (int) Math.floor(earth.transform.blockX(lon)), z = (int) Math.floor(earth.transform.blockZ(lat));
        Earth.Surface s = earth.at(x, z);
        int y = Math.max(s.surfaceY(), earth.curve.seaLevelY) + 1;
        level.setRespawnData(LevelData.RespawnData.of(Level.OVERWORLD, new BlockPos(x, y, z), 0f, 0f));
        LOG.info("Earth world: spawn set to {}, {} -> block ({}, {}, {}) - {} m, {}", lat, lon, x, y, z, (int) s.elevM(),
                com.seth.worldbound.worldgen.BiomeTable.code(s.climate()));
        return y;
    }
}
