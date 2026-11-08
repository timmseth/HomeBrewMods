package com.seth.tatv.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.seth.tatv.TatvEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class TatvRenderer extends EntityRenderer<TatvEntity, TatvRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("tatv", "tatv"), "main");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("tatv", "textures/entity/tatv.png");
    private final TatvModel model;

    public TatvRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.9f;
        this.model = new TatvModel(ctx.bakeLayer(LAYER));
    }

    @Override
    public TatvRenderState createRenderState() {
        return new TatvRenderState();
    }

    @Override
    public void extractRenderState(TatvEntity e, TatvRenderState s, float partial) {
        super.extractRenderState(e, s, partial);
        s.yRot = e.getYRot(partial);
        s.pitch = Mth.lerp(partial, e.visualPitchO, e.visualPitch);
        s.roll = Mth.lerp(partial, e.visualRollO, e.visualRoll);
        s.hurtTime = e.getHurtTime() - partial;
        s.hurtDir = e.getHurtDir();
        s.damageTime = Math.max(e.getDamage() - partial, 0);
        s.mode = e.mode().ordinal();
        s.anchored = e.anchored();
    }

    @Override
    public void submit(TatvRenderState s, PoseStack pose, SubmitNodeCollector out, CameraRenderState cam) {
        pose.pushPose();
        pose.translate(0, 0.05 + (s.mode == 0 ? 0 : Mth.sin(s.ageInTicks * 0.15f) * 0.04f), 0);
        pose.mulPose(Axis.YP.rotationDegrees(180f - s.yRot));
        pose.mulPose(Axis.XP.rotationDegrees(s.pitch));
        pose.mulPose(Axis.ZP.rotationDegrees(s.roll));
        if (s.hurtTime > 0) pose.mulPose(Axis.ZP.rotationDegrees(Mth.sin(s.hurtTime) * s.hurtTime * s.damageTime / 10f * s.hurtDir));
        pose.scale(-1, -1, 1);
        pose.translate(0, -1.501, 0);
        out.submitModel(model, s, pose, TEXTURE, s.lightCoords, OverlayTexture.NO_OVERLAY, s.outlineColor, null);
        pose.popPose();
        super.submit(s, pose, out, cam);
    }
}
