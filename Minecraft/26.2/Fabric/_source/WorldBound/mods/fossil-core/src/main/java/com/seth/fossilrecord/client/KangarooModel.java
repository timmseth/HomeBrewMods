package com.seth.fossilrecord.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * A kangaroo, blocky: a pear of a body leaning forward, small chest and arms, a long-snouted head with tall ears, big
 * thighs folding down to long flat feet, and a thick tail that rests on the ground. It bounds with both feet together:
 * the whole animal lifts off on each stride, the legs swing back at take-off and forward to land, the tail rises as a
 * counterweight. Grazing drops the head; a kick rocks it back on its tail with both feet thrown forward.
 * Texture 64x64 (painted by the dev tool FaunaArt from these boxes).
 */
public class KangarooModel extends EntityModel<KangarooRenderer.State> {
    final ModelPart body, chest, head, earL, earR, armL, armR, tail, tail2, legL, legR, shinL, shinR, footL, footR;

    public KangarooModel(ModelPart root) {
        super(root);
        body = root.getChild("body");
        chest = body.getChild("chest");
        head = chest.getChild("head");
        earL = head.getChild("ear_l");
        earR = head.getChild("ear_r");
        armL = chest.getChild("arm_l");
        armR = chest.getChild("arm_r");
        tail = body.getChild("tail");
        tail2 = tail.getChild("tail2");
        legL = root.getChild("leg_l");
        legR = root.getChild("leg_r");
        shinL = legL.getChild("shin");
        shinR = legR.getChild("shin");
        footL = shinL.getChild("foot");
        footR = shinR.getChild("foot");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // texture regions (64x64): body 0,0 · chest 0,18 · arms 24,18 · head 32,0 · snout 34,9 · ears 52,0 · tail 0,32 ·
        // tail tip 28,32 · thigh 0,46 · shin 22,46 · foot 30,46
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4f, -8f, -4.5f, 8, 9, 9),
                PartPose.offsetAndRotation(0f, 14f, 1.5f, 0.6f, 0f, 0f));
        PartDefinition chest = body.addOrReplaceChild("chest", CubeListBuilder.create().texOffs(0, 18).addBox(-3f, -5f, -3f, 6, 5, 6),
                PartPose.offsetAndRotation(0f, -7.5f, -0.5f, -0.25f, 0f, 0f));
        PartDefinition head = chest.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(32, 0).addBox(-2.5f, -4f, -3.5f, 5, 4, 5)
                        .texOffs(34, 9).addBox(-1.5f, -3f, -7f, 3, 3, 4),
                PartPose.offsetAndRotation(0f, -4.5f, -0.5f, -0.35f, 0f, 0f));
        head.addOrReplaceChild("ear_l", CubeListBuilder.create().texOffs(52, 0).addBox(-1f, -5f, -0.5f, 2, 5, 1),
                PartPose.offsetAndRotation(1.6f, -3.5f, 0.5f, -0.15f, 0f, 0.25f));
        head.addOrReplaceChild("ear_r", CubeListBuilder.create().texOffs(52, 0).mirror().addBox(-1f, -5f, -0.5f, 2, 5, 1),
                PartPose.offsetAndRotation(-1.6f, -3.5f, 0.5f, -0.15f, 0f, -0.25f));
        chest.addOrReplaceChild("arm_l", CubeListBuilder.create().texOffs(24, 18).addBox(-1f, 0f, -1f, 2, 6, 2),
                PartPose.offsetAndRotation(2.6f, -3.5f, -2f, -0.75f, 0f, -0.1f));
        chest.addOrReplaceChild("arm_r", CubeListBuilder.create().texOffs(24, 18).mirror().addBox(-1f, 0f, -1f, 2, 6, 2),
                PartPose.offsetAndRotation(-2.6f, -3.5f, -2f, -0.75f, 0f, 0.1f));
        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 32).addBox(-2.5f, -2.5f, 0f, 5, 5, 9),
                PartPose.offsetAndRotation(0f, -1f, 3.5f, -1.5f, 0f, 0f));
        tail.addOrReplaceChild("tail2", CubeListBuilder.create().texOffs(28, 32).addBox(-1.5f, -1.5f, 0f, 3, 3, 9),
                PartPose.offsetAndRotation(0f, 0.5f, 8.5f, 0.4f, 0f, 0f));
        for (int side = 0; side < 2; side++) {
            float s = side == 0 ? 1 : -1;
            CubeListBuilder thigh = CubeListBuilder.create().texOffs(0, 46);
            CubeListBuilder shin = CubeListBuilder.create().texOffs(22, 46);
            CubeListBuilder foot = CubeListBuilder.create().texOffs(30, 46);
            if (side == 1) { thigh.mirror(); shin.mirror(); foot.mirror(); }
            PartDefinition leg = root.addOrReplaceChild(side == 0 ? "leg_l" : "leg_r", thigh.addBox(-2f, -1f, -3.5f, 4, 7, 7),
                    PartPose.offsetAndRotation(3.0f * s, 13.5f, 2f, -0.8f, 0f, 0f));
            PartDefinition sh = leg.addOrReplaceChild("shin", shin.addBox(-1f, 0f, -1f, 2, 5, 2), PartPose.offsetAndRotation(0f, 6f, 0.5f, 1.35f, 0f, 0f));
            sh.addOrReplaceChild("foot", foot.addBox(-1.5f, 0f, -8.5f, 3, 2, 10), PartPose.offsetAndRotation(0f, 4.5f, 0f, -0.55f, 0f, 0f));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static final float DEG = Mth.DEG_TO_RAD;

    @Override
    public void setupAnim(KangarooRenderer.State s) {
        super.setupAnim(s);
        float speed = Math.min(1f, s.walkAnimationSpeed * 1.5f);
        float phase = s.walkAnimationPos * 0.55f;
        float air = Math.max(0f, Mth.sin(phase));                       // 0 on the ground, 1 at the top of a bound
        float lift = air * 6f * speed;
        body.y -= lift;
        legL.y -= lift;
        legR.y -= lift;
        float lean = 0.55f * speed;
        body.xRot += lean;                                               // into the bound
        float swing = Mth.cos(phase) * 0.85f * speed;                    // both legs together
        legL.xRot += swing;
        legR.xRot += swing;
        shinL.xRot -= swing * 0.5f;
        shinR.xRot -= swing * 0.5f;
        // feet flat while on the ground, trailing behind in the air
        footL.xRot = -(legL.xRot + shinL.xRot) + air * 0.6f * speed;
        footR.xRot = -(legR.xRot + shinR.xRot) + air * 0.6f * speed;
        tail.xRot += (0.25f - 0.45f * air) * speed;                     // off the ground and up as a counterweight
        tail2.xRot -= 0.25f * speed;
        armL.xRot -= 0.25f * speed;
        armR.xRot -= 0.25f * speed;

        // the head looks about and stays level against the lean
        head.yRot = s.yRot * DEG;
        head.xRot += s.xRot * DEG - lean * 0.8f;
        // grazing
        if (s.eat > 0) {
            body.xRot += 0.55f * s.eat;
            head.xRot += 0.75f * s.eat;
            armL.xRot -= 0.5f * s.eat;
            armR.xRot -= 0.5f * s.eat;
        }
        // the kick: back on the tail, both feet thrown forward, arms up
        if (s.kick > 0) {
            float k = Mth.sin(s.kick * Mth.PI);
            body.xRot -= 0.75f * k;
            tail.xRot += 0.95f * k;
            legL.xRot -= 1.3f * k;
            legR.xRot -= 1.3f * k;
            shinL.xRot -= 0.6f * k;
            shinR.xRot -= 0.6f * k;
            footL.xRot = -(legL.xRot + shinL.xRot) - 0.4f * k;
            footR.xRot = -(legR.xRot + shinR.xRot) - 0.4f * k;
            armL.xRot -= 0.9f * k;
            armR.xRot -= 0.9f * k;
        }
        // the ears never quite keep still
        earL.zRot += 0.08f * Mth.sin(s.ageInTicks * 0.13f);
        earR.zRot -= 0.08f * Mth.sin(s.ageInTicks * 0.11f + 1.3f);
        earL.xRot -= 0.06f * Mth.sin(s.ageInTicks * 0.07f);
    }
}
