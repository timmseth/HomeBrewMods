package com.seth.fossilrecord.client.dino;

import com.mojang.blaze3d.vertex.PoseStack;
import com.seth.worldbound.WorldBoundMod;
import com.seth.fossilrecord.dino.Dino;
import com.seth.fossilrecord.dino.DinoSpecies;
import com.seth.fossilrecord.dino.Rig;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;

/**
 * Draws a dinosaur at its real size: the model is built in units of 1/U metre, so it is scaled by 16/U (times its
 * growth for the young). The culling box covers the whole animal (head and tail reach well past the hitbox).
 */
public class DinoRenderer extends MobRenderer<Dino, DinoRenderer.State, DinoModel> {
    public static final class State extends LivingEntityRenderState {
        /** Blocks walked (drives the stride) and speed in blocks per tick. */
        public float gait, gaitSpeed;
        /** 0..1 through each animation, 0 when not playing. */
        public float attack, eat, roar, call;
        public boolean male;
    }

    final DinoSpecies sp;
    final Identifier texture;
    final float unitScale;

    public DinoRenderer(EntityRendererProvider.Context ctx, DinoSpecies sp) {
        super(ctx, new DinoModel(ctx.bakeLayer(layer(sp)), sp), Math.max(0.25f, sp.rig().width() * 0.45f));
        this.sp = sp;
        this.texture = Identifier.fromNamespaceAndPath(com.seth.fossilrecord.FossilRecord.ID, "textures/entity/dino/" + sp.id + ".png");
        this.unitScale = 16f / sp.rig().U;
    }

    public static ModelLayerLocation layer(DinoSpecies sp) {
        return new ModelLayerLocation(Identifier.fromNamespaceAndPath(com.seth.fossilrecord.FossilRecord.ID, sp.id), "main");
    }

    public static void register() {
        for (DinoSpecies sp : com.seth.fossilrecord.dino.Roster.ALL) {
            ModelLayerRegistry.registerModelLayer(layer(sp), () -> DinoModel.createBodyLayer(sp));
            EntityRendererRegistry.register(com.seth.fossilrecord.dino.Dinos.type(sp), ctx -> new DinoRenderer(ctx, sp));
        }
    }

    @Override public State createRenderState() { return new State(); }

    @Override
    public void extractRenderState(Dino d, State s, float partial) {
        super.extractRenderState(d, s, partial);
        s.gait = d.gaitO + (d.gait - d.gaitO) * partial;
        s.gaitSpeed = (d.gaitSpeedO + (d.gaitSpeed - d.gaitSpeedO) * partial) / Math.max(0.1f, d.scaleNow());
        s.gait /= Math.max(0.1f, d.scaleNow());
        int al = d.attackLength();
        s.attack = d.attackTicks > 0 ? clamp(1f - (d.attackTicks - partial) / al) : 0f;
        s.roar = d.roarTicks > 0 ? clamp(1f - (d.roarTicks - partial) / 50f) : 0f;
        s.call = d.callTicks > 0 ? clamp(1f - (d.callTicks - partial) / 16f) : 0f;
        float t = d.eatTicks > 0 ? d.eatTicks - partial : 0f;               // 80 ticks: down over 8, feed, up over the last 8
        s.eat = t <= 0 ? 0f : t > 72 ? (80 - t) / 8f : t < 8 ? t / 8f : 1f;
        s.male = d.isMale();
    }

    private static float clamp(float v) { return v < 0 ? 0 : v > 1 ? 1 : v; }

    @Override
    protected void scale(State s, PoseStack pose) {
        float k = unitScale * s.ageScale;
        pose.scale(k, k, k);
    }

    @Override
    protected AABB getBoundingBoxForCulling(Dino d) {
        Rig r = sp.rig();
        float g = d.scaleNow();
        double reach = Math.max(r.frontReach(), r.backReach()) * g + 1, up = r.topReach() * g + 1;
        AABB bb = d.getBoundingBox();
        return new AABB(d.getX() - reach, bb.minY - 0.5, d.getZ() - reach, d.getX() + reach, bb.minY + up, d.getZ() + reach);
    }

    @Override
    public Identifier getTextureLocation(State s) { return texture; }
}
