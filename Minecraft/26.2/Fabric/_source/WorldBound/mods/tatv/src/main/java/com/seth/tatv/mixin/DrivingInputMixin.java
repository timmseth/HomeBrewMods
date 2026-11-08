package com.seth.tatv.mixin;

import com.seth.tatv.TatvEntity;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** While driving a TATV, the triggers / mouse buttons drive instead of attacking, using or picking blocks. */
@Mixin(Minecraft.class)
public abstract class DrivingInputMixin {
    private static boolean driving() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.getVehicle() instanceof TatvEntity;
    }

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void tatv$noAttack(CallbackInfoReturnable<Boolean> cir) {
        if (driving()) cir.setReturnValue(false);
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void tatv$noMining(boolean b, CallbackInfo ci) {
        if (driving()) ci.cancel();
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void tatv$noUse(CallbackInfo ci) {
        if (driving()) ci.cancel();
    }

    @Inject(method = "pickBlockOrEntity", at = @At("HEAD"), cancellable = true)
    private void tatv$noPick(CallbackInfo ci) {
        if (driving()) ci.cancel();
    }
}
