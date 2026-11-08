package com.seth.worldbound.mixin;

import com.seth.worldbound.Earth;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Earth worlds: snow doesn't settle on rock faces steeper than ~45° (at world generation or during snowfall). */
@Mixin(Biome.class)
public abstract class SnowSlopeMixin {
    @Inject(method = "shouldSnow", at = @At("HEAD"), cancellable = true)
    private void worldbound$steepShedsSnow(LevelReader level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (level.isClientSide() || !Earth.climateWeather() || Earth.wildMode()) return;
        if (level.getMinY() != Earth.get().curve.minY) return; // not the Earth overworld
        if (Earth.get().at(pos.getX(), pos.getZ()).slope() > 3.0) cir.setReturnValue(false);
    }
}
