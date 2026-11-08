package com.seth.worldbound.mixin;

import com.seth.worldbound.Earth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Earth worlds: what fills carved space (vanilla cave and canyon carvers). Vanilla's rule — lava below
 * min(-54, sea level), water up to sea level — put lava in every tunnel below y -700 here: under all lowlands and
 * floating in the deep ocean. Instead, carving through rock leaves air, and carving through the sea or a lake leaves
 * its water, read from the Earth data column by column.
 */
@Mixin(NoiseBasedChunkGenerator.class)
public abstract class FluidPickerMixin {
    @Inject(method = "createFluidPicker", at = @At("HEAD"), cancellable = true)
    private static void worldbound$earthFluids(NoiseGeneratorSettings settings, CallbackInfoReturnable<Aquifer.FluidPicker> cir) {
        Earth e = Earth.get();
        if (settings.seaLevel() != e.curve.seaLevelY || settings.noiseSettings().minY() != e.curve.minY) return;
        Aquifer.FluidStatus dry = new Aquifer.FluidStatus(Integer.MIN_VALUE + 1, Blocks.AIR.defaultBlockState());
        cir.setReturnValue((x, y, z) -> {
            Earth earth = Earth.get();
            Earth.Surface s = earth.at(x, z);
            int st = s.solidTop(), wt = s.waterDepth() > 0 ? s.surfaceY() : s.ocean() ? earth.curve.seaLevelY : st;
            return y > st && y <= wt ? new Aquifer.FluidStatus(wt + 1, Blocks.WATER.defaultBlockState()) : dry;
        });
    }
}
