package com.seth.tatv.mixin;

import com.seth.tatv.TatvEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** In the air / underwater modes sneak means "down", not "get out" (V or D-pad up gets out there). */
@Mixin(Player.class)
public abstract class StayAboardMixin {
    @Inject(method = "wantsToStopRiding", at = @At("HEAD"), cancellable = true)
    private void tatv$stay(CallbackInfoReturnable<Boolean> cir) {
        if (((Player) (Object) this).getVehicle() instanceof TatvEntity t && (t.mode() != TatvEntity.Mode.DRIVE || t.submerged()))
            cir.setReturnValue(false);
    }
}
