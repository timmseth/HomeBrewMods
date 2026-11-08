package com.seth.worldbound.mixin;

import com.seth.worldbound.Earth;
import com.seth.worldbound.worldgen.EarthFill;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Earth worlds: "above the preliminary surface" (which gates every surface material: grass, dirt, sand, gravel,
 * snow) uses each column's real ground height instead of a blend of four values 16 blocks apart. The blend sat
 * above narrow valley floors and deep lake and river beds, which were left as bare stone.
 */
@Mixin(targets = "net.minecraft.world.level.levelgen.SurfaceRules$Context")
public abstract class SurfaceLevelMixin {
    @Shadow private int blockX;
    @Shadow private int blockZ;
    @Shadow private int surfaceDepth;
    @Shadow private long lastUpdateXZ;
    @Shadow private long lastMinSurfaceLevelUpdate;
    @Shadow private int minSurfaceLevel;
    @Shadow @Final private ChunkAccess chunk;

    @Inject(method = "getMinSurfaceLevel", at = @At("HEAD"), cancellable = true)
    private void worldbound$columnSurface(CallbackInfoReturnable<Integer> cir) {
        if (!EarthFill.isEarthChunk(chunk) || !Earth.get().config.fast_fill) return;
        if (lastMinSurfaceLevelUpdate != lastUpdateXZ) {
            lastMinSurfaceLevelUpdate = lastUpdateXZ;
            minSurfaceLevel = Earth.get().at(blockX, blockZ).solidTop() + surfaceDepth - 8;
        }
        cir.setReturnValue(minSurfaceLevel);
    }
}
