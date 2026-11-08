package com.seth.worldbound.mixin;

import com.seth.worldbound.Earth;
import com.seth.worldbound.worldgen.EarthFill;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.SurfaceSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Earth worlds: surface rules walk each column from the top down to the world floor (~4,000 blocks). With the
 * fast fill, everything more than {@link EarthFill#RULE_BAND} blocks under the lowest ground is already final,
 * so stop there.
 */
@Mixin(SurfaceSystem.class)
public abstract class SurfaceSystemMixin {
    @Redirect(method = "buildSurface(Lnet/minecraft/world/level/levelgen/RandomState;Lnet/minecraft/world/level/biome/BiomeManager;ZLnet/minecraft/world/level/levelgen/WorldGenerationContext;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/world/level/levelgen/NoiseChunk;Lnet/minecraft/world/level/levelgen/SurfaceRules$RuleSource;Ljava/util/Set;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/ChunkAccess;getMinY()I"))
    private int worldbound$ruleFloor(ChunkAccess chunk) {
        if (EarthFill.isEarthChunk(chunk) && Earth.get().config.fast_fill) return EarthFill.ruleFloor(chunk);
        return chunk.getMinY();
    }
}
