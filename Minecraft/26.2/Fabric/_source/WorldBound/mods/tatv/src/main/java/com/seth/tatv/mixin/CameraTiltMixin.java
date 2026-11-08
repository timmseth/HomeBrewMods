package com.seth.tatv.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.seth.tatv.TatvConfig;
import com.seth.tatv.TatvEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Optional (config / key N): the first-person view tilts with the hull. */
@Mixin(GameRenderer.class)
public abstract class CameraTiltMixin {
    @Inject(method = "bobHurt", at = @At("HEAD"), require = 0)
    private void tatv$tilt(CameraRenderState state, PoseStack pose, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (!TatvConfig.get().first_person_tilt || mc.player == null || !mc.options.getCameraType().isFirstPerson()) return;
        if (!(mc.player.getVehicle() instanceof TatvEntity v)) return;
        Camera cam = ((GameRenderer) (Object) this).mainCamera();
        float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        float p = Mth.lerp(partial, v.visualPitchO, v.visualPitch), r = Mth.lerp(partial, v.visualRollO, v.visualRoll);
        if (Math.abs(p) < 0.05f && Math.abs(r) < 0.05f) return;
        float vy = v.getYRot(partial);
        // hull rotation in world space, as the hull renderer applies it
        Quaternionf hull = new Quaternionf(Axis.YP.rotationDegrees(180f - vy)).mul(Axis.XP.rotationDegrees(p)).mul(Axis.ZP.rotationDegrees(r))
                .mul(Axis.YP.rotationDegrees(vy - 180f));
        // the camera's view rotation; express the inverse hull rotation in view space so the world tilts the other way
        Quaternionf view = new Quaternionf(Axis.XP.rotationDegrees(cam.xRot())).mul(Axis.YP.rotationDegrees(cam.yRot() + 180f));
        Quaternionf m = new Quaternionf(view).mul(hull.conjugate()).mul(new Quaternionf(view).conjugate());
        pose.mulPose(m);
    }
}
