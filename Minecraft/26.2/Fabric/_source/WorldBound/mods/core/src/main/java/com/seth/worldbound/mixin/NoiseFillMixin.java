package com.seth.worldbound.mixin;

import com.seth.worldbound.Earth;
import com.seth.worldbound.worldgen.EarthFill;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

/** Earth worlds: fill terrain column-by-column from the data instead of evaluating density per block. */
@Mixin(NoiseBasedChunkGenerator.class)
public abstract class NoiseFillMixin {
    @Inject(method = "fillFromNoise", at = @At("HEAD"), cancellable = true)
    private void worldbound$fastFill(Blender blender, RandomState random, StructureManager structures, ChunkAccess chunk,
                                     CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir) {
        NoiseBasedChunkGenerator self = (NoiseBasedChunkGenerator) (Object) this;
        if (!self.generatorSettings().unwrapKey().map(k -> k.identifier().getNamespace().equals("worldbound")).orElse(false)) return;
        if (!Earth.get().config.fast_fill) return;
        cir.setReturnValue(CompletableFuture.supplyAsync(() -> EarthFill.fill(chunk, structures), net.minecraft.util.Util.backgroundExecutor()));
    }
}
