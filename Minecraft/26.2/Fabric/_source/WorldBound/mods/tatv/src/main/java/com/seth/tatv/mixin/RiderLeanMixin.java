package com.seth.tatv.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.seth.tatv.TatvEntity;
import com.seth.tatv.client.TiltState;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Riders lean with the TATV: the same pitch/roll as the hull, about the hull's own pivot. */
@Mixin(LivingEntityRenderer.class)
public abstract class RiderLeanMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
    private void tatv$capture(LivingEntity e, LivingEntityRenderState s, float partial, CallbackInfo ci) {
        TiltState t = (TiltState) s;
        if (e.getVehicle() instanceof TatvEntity v) {
            Vec3 d = v.getPosition(partial).subtract(e.getPosition(partial));
            t.tatv$set(true, v.getYRot(partial), Mth.lerp(partial, v.visualPitchO, v.visualPitch), Mth.lerp(partial, v.visualRollO, v.visualRoll), d.x, d.y, d.z);
        } else if (t.tatv$on()) {
            t.tatv$set(false, 0, 0, 0, 0, 0, 0);
        }
    }

    @Inject(method = "setupRotations", at = @At("HEAD"))
    private void tatv$lean(LivingEntityRenderState s, PoseStack pose, float bodyRot, float scale, CallbackInfo ci) {
        TiltState t = (TiltState) s;
        if (!t.tatv$on() || (Math.abs(t.tatv$pitch()) < 0.05f && Math.abs(t.tatv$roll()) < 0.05f)) return;
        pose.translate(t.tatv$dx(), t.tatv$dy(), t.tatv$dz());
        pose.mulPose(Axis.YP.rotationDegrees(180f - t.tatv$yaw()));
        pose.mulPose(Axis.XP.rotationDegrees(t.tatv$pitch()));
        pose.mulPose(Axis.ZP.rotationDegrees(t.tatv$roll()));
        pose.mulPose(Axis.YP.rotationDegrees(t.tatv$yaw() - 180f));
        pose.translate(-t.tatv$dx(), -t.tatv$dy(), -t.tatv$dz());
    }
}
