package com.seth.worldbound.mixin;

import com.seth.worldbound.Earth;
import com.seth.worldbound.worldgen.Seasons;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** In an Earth world, weather temperature comes from real climate + today's season instead of block height. */
@Mixin(Biome.class)
public abstract class BiomeTemperatureMixin {
    @Inject(method = "getHeightAdjustedTemperature(Lnet/minecraft/core/BlockPos;I)F", at = @At("HEAD"), cancellable = true)
    private void worldbound$seasonal(BlockPos pos, int seaLevel, CallbackInfoReturnable<Float> cir) {
        if (!Earth.climateWeather()) return;
        Biome self = (Biome) (Object) this;
        if (!self.hasPrecipitation()) return; // Nether, End, deserts: leave vanilla alone
        if (Earth.wildMode()) {   // wild biomes keep their own weather, with no "colder every block up" lapse
            cir.setReturnValue(self.getBaseTemperature());
            return;
        }
        cir.setReturnValue(Seasons.minecraftTemperature(pos.getX(), pos.getZ()));
    }
}
