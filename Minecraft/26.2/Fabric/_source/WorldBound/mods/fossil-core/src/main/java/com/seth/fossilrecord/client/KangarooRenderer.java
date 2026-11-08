package com.seth.fossilrecord.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.seth.worldbound.WorldBoundMod;
import com.seth.fossilrecord.Habitat;
import com.seth.fossilrecord.Kangaroo;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/** Kangaroos: red or grey by where they live; grown males a size up, as boomers are. Joeys are drawn at half size. */
public class KangarooRenderer extends MobRenderer<Kangaroo, KangarooRenderer.State, KangarooModel> {
    public static final Identifier RED = Identifier.fromNamespaceAndPath(com.seth.fossilrecord.FossilRecord.ID, "textures/entity/fauna/kangaroo_red.png");
    public static final Identifier GREY = Identifier.fromNamespaceAndPath(com.seth.fossilrecord.FossilRecord.ID, "textures/entity/fauna/kangaroo_grey.png");

    public static final class State extends LivingEntityRenderState {
        public int variant;
        public boolean male;
        /** 0..1 through a kick, 0 when not kicking */
        public float kick;
        /** 0..1 how far the head is down grazing */
        public float eat;
    }

    public KangarooRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new KangarooModel(ctx.bakeLayer(FaunaClient.KANGAROO)), 0.5f);
    }

    @Override public State createRenderState() { return new State(); }

    @Override
    public void extractRenderState(Kangaroo k, State s, float partial) {
        super.extractRenderState(k, s, partial);
        s.variant = k.getVariant();
        s.male = k.isMale();
        s.kick = k.kickTicks > 0 ? Math.max(0f, Math.min(1f, 1f - (k.kickTicks - partial) / 10f)) : 0f;
        float t = k.eatTicks > 0 ? k.eatTicks - partial : 0f;            // 40 ticks: down over 4, graze, up over the last 4
        s.eat = t <= 0 ? 0f : t > 36 ? (40 - t) / 4f : t < 4 ? t / 4f : 1f;
    }

    @Override
    protected void scale(State s, PoseStack pose) {
        if (s.male && !s.isBaby) pose.scale(1.15f, 1.15f, 1.15f);
    }

    @Override
    public Identifier getTextureLocation(State s) { return s.variant == Habitat.RED ? RED : GREY; }
}
