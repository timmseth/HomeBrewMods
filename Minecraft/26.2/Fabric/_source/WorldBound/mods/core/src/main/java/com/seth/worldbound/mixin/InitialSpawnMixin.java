package com.seth.worldbound.mixin;

import com.seth.worldbound.Earth;
import com.seth.worldbound.WorldBoundMod;
import com.seth.worldbound.WorldInfo;
import com.seth.worldbound.worldgen.EarthBiomeSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.LevelLoadListener;
import net.minecraft.world.level.storage.ServerLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * New Earth worlds: skip vanilla's world-spawn search. It generates up to ~120 chunks near the map origin to find
 * a spawn by climate noise, and WorldBound then moves the spawn to the place picked on the Create World screen
 * anyway; with fine data still downloading that search could take minutes. Instead the spawn's data tiles are
 * fetched once and the spawn is set straight at the picked place.
 */
@Mixin(MinecraftServer.class)
public abstract class InitialSpawnMixin {
    @Inject(method = "setInitialSpawn", at = @At("HEAD"), cancellable = true)
    private static void worldbound$earthSpawn(ServerLevel level, ServerLevelData data, boolean bonusChest, boolean debug,
                                              LevelLoadListener listener, CallbackInfo ci) {
        if (!(level.getChunkSource().getGenerator().getBiomeSource() instanceof EarthBiomeSource)) return;
        WorldInfo w = WorldInfo.forWorld(level.getServer(), false);
        if (w.spawn_set) return;
        try {
            var st = com.seth.worldbound.Hooks.streaming;
            if (st != null && !com.seth.worldbound.Earth.get().generated()) st.prepareSpawn(w.spawn_lon, w.spawn_lat, 30);
        } catch (RuntimeException e) {
            WorldBoundMod.LOG.warn("WorldBound: spawn data prefetch: {}", e.toString());
        }
        WorldBoundMod.setSpawn(level, w.spawn_lat, w.spawn_lon);
        w.spawn_set = true;
        w.save();
        ci.cancel();
    }
}
