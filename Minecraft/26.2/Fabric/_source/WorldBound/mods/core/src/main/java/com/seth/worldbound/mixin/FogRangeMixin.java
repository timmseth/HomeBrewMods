package com.seth.worldbound.mixin;

import com.seth.worldbound.client.horizon.Horizon;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * While WorldBound's horizon is drawn, the render-distance fog wall moves out to the horizon's edge so the real
 * chunks blend into the distant terrain instead of fading into a fog curtain in front of it. Weather, cave and
 * underwater fog are untouched.
 */
@Mixin(FogRenderer.class)
public abstract class FogRangeMixin {
    @Inject(method = "setupFog", at = @At("RETURN"))
    private void worldbound$horizonFog(CallbackInfoReturnable<FogData> cir) {
        FogData fog = cir.getReturnValue();
        if (fog == null || !Horizon.drawing()) return;
        fog.renderDistanceStart = Math.max(fog.renderDistanceStart, Horizon.fogStart());
        fog.renderDistanceEnd = Math.max(fog.renderDistanceEnd, Horizon.fogEnd());
    }
}
