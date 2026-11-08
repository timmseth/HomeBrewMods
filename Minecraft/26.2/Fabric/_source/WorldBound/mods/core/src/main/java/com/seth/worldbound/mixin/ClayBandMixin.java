package com.seth.worldbound.mixin;

import net.minecraft.world.level.levelgen.SurfaceSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Badlands terracotta bands index by y; below y -192 vanilla's modulo goes negative and crashes. Wrap it. */
@Mixin(SurfaceSystem.class)
public abstract class ClayBandMixin {
    @ModifyVariable(method = "getBand", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private int worldbound$wrapY(int y) {
        return y < 0 ? Math.floorMod(y, 192) + 192 : y;
    }
}
