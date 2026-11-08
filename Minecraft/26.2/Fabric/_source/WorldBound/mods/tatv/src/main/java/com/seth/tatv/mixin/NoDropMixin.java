package com.seth.tatv.mixin;

import com.seth.tatv.TatvEntity;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** D-pad down (Controlify's drop) is dive/surface while driving, so no item drops from the seat. */
@Mixin(LocalPlayer.class)
public abstract class NoDropMixin {
    @Inject(method = "drop", at = @At("HEAD"), cancellable = true)
    private void tatv$noDrop(boolean all, CallbackInfoReturnable<Boolean> cir) {
        if (((LocalPlayer) (Object) this).getVehicle() instanceof TatvEntity) cir.setReturnValue(false);
    }
}
