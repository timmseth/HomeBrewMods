package com.seth.worldbound.mixin;

import com.seth.worldbound.Earth;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Earth worlds: vanilla's cave and canyon carvers only when {@code vanilla_caves} is on (default). What they fill
 * carved space with is decided by {@link FluidPickerMixin} (air in rock, water in the sea; never the lava that
 * vanilla's rule put below y -700 here).
 */
@Mixin(NoiseBasedChunkGenerator.class)
public abstract class CarverMixin {
    @Inject(method = "applyCarvers", at = @At("HEAD"), cancellable = true)
    private void worldbound$noCarvers(WorldGenRegion region, long seed, RandomState random, BiomeManager biomes,
                                      StructureManager structures, ChunkAccess chunk, CallbackInfo ci) {
        NoiseBasedChunkGenerator self = (NoiseBasedChunkGenerator) (Object) this;
        if (self.generatorSettings().unwrapKey().map(k -> k.identifier().getNamespace().equals("worldbound")).orElse(false)
                && Earth.get().config.fast_fill && !Earth.get().config.vanilla_caves) ci.cancel();
    }
}
