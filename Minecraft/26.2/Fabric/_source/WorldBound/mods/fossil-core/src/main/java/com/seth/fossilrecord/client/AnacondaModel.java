package com.seth.fossilrecord.client;

import com.seth.fossilrecord.AnacondaBody;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * An anaconda: a head (skull, snout, a jaw that drops when it strikes) and a chain of body segments, each a unit box
 * that the renderer places, turns and stretches between two points of the body's shape (AnacondaBody). Four segment
 * looks alternate so the blotches don't repeat in step. Texture 64x64 (painted by the dev tool FaunaArt).
 */
public class AnacondaModel extends EntityModel<AnacondaRenderer.State> {
    static final int N = AnacondaBody.N;
    final ModelPart head, jaw;
    final ModelPart[] segs = new ModelPart[N];

    public AnacondaModel(ModelPart root) {
        super(root);
        head = root.getChild("head");
        jaw = head.getChild("jaw");
        for (int i = 0; i < N; i++) segs[i] = root.getChild("seg" + i);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // the head points along -z from its pivot at the neck; a segment runs along +z from its pivot toward the tail
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-2.5f, -2f, -6f, 5, 4, 6)
                        .texOffs(22, 0).addBox(-2f, -1.5f, -9f, 4, 3, 3),
                PartPose.ZERO);
        head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(36, 0).addBox(-2f, 0f, -6f, 4, 1, 6), PartPose.offset(0f, 1.5f, -2.5f));
        for (int i = 0; i < N; i++)
            root.addOrReplaceChild("seg" + i, CubeListBuilder.create().texOffs(16 * (i % 4), 16).addBox(-2f, -2f, 0f, 4, 4, 4), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(AnacondaRenderer.State s) {
        super.setupAnim(s);
        boolean show = s.laidOut;
        head.visible = show;
        for (ModelPart p : segs) p.visible = show;
        if (!show) return;
        for (int i = 0; i < N; i++) {
            ModelPart p = segs[i];
            p.x = s.px[i]; p.y = s.py[i]; p.z = s.pz[i];
            p.yRot = s.yaw[i]; p.xRot = s.pitch[i]; p.zRot = 0f;
            p.xScale = p.yScale = s.thick[i] * 16f / 4f;
            p.zScale = Math.max(0.05f, s.len[i] * 16f / 4f * 1.12f);  // a little overlap so the bends don't open gaps
        }
        head.x = s.px[0]; head.y = s.py[0]; head.z = s.pz[0];
        head.yRot = s.headYaw; head.xRot = s.headPitch; head.zRot = 0f;
        float hs = s.thick[0] * 16f / 5f * 1.6f;                         // the head a little wider than the neck
        head.xScale = head.yScale = head.zScale = hs;
        jaw.xRot = 0.55f * s.jaw;
    }
}
