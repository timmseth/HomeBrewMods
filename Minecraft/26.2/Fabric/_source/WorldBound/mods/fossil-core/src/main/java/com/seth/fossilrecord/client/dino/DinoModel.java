package com.seth.fossilrecord.client.dino;

import com.seth.fossilrecord.dino.Body;
import com.seth.fossilrecord.dino.DinoSpecies;
import com.seth.fossilrecord.dino.Rig;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 * Every dinosaur's model, built from its {@link Rig}: a body (hips and ribcage) carrying a neck chain, head and jaw,
 * a tail chain and the arms or front legs; hind legs of four segments (thigh, shin, metatarsus, foot) hanging from
 * the hip joints; and the species' ornaments as parts of their own (horns, frill, crests, dome, sail, plates, spikes,
 * club, armour, claws, wing and tail feathers). Boxes get their texture space from a packer (two passes: measure,
 * then place biggest first), so each species gets the smallest texture its boxes need; the dev tool FaunaArt paints
 * that texture from the same boxes.
 * <p>
 * Animation: the hips ride on whichever leg is planted (each leg's reach is worked out from its joint angles, so the
 * body bobs as the legs swing and planted feet stay on the ground), four-leggers walk in a lateral sequence, the tail
 * sways, and the head looks where the animal looks. Attacks play by kind (bite, horn charge, tail swing, head-butt,
 * claw swipe, kick, stamp); grazers lower their heads; the big hunters roar.
 */
public class DinoModel extends EntityModel<DinoRenderer.State> {
    final DinoSpecies sp;
    final Body b;
    final Rig r;
    final ModelPart body, head, jaw;
    final ModelPart[] neck, tail;
    /** Hind legs [left, right] x [thigh, shin, meta, foot]; front legs or arms [left, right] x [upper, fore, hand]. */
    final ModelPart[][] hind = new ModelPart[2][4], front = new ModelPart[2][3];
    final float stride;

    public DinoModel(ModelPart root, DinoSpecies sp) {
        super(root);
        this.sp = sp;
        this.b = sp.body;
        this.r = sp.rig();
        body = root.getChild("body");
        neck = new ModelPart[r.neckLen.length];
        ModelPart p = body;
        for (int i = 0; i < neck.length; i++) { p = p.getChild("neck" + i); neck[i] = p; }
        head = p.getChild("head");
        jaw = head.getChild("jaw");
        tail = new ModelPart[r.tailLen.length];
        p = body;
        for (int i = 0; i < tail.length; i++) { p = p.getChild("tail" + i); tail[i] = p; }
        for (int s = 0; s < 2; s++) {
            String side = s == 0 ? "_l" : "_r";
            ModelPart leg = root.getChild("leg" + side);
            hind[s][0] = leg;
            hind[s][1] = leg.getChild("shin");
            hind[s][2] = hind[s][1].getChild("meta");
            hind[s][3] = hind[s][2].getChild("foot");
            ModelPart arm = body.getChild("arm" + side);
            front[s][0] = arm;
            front[s][1] = arm.getChild("fore");
            front[s][2] = front[s][1].getChild("hand");
        }
        // one full stride (two steps), in blocks walked: about one and a half leg lengths
        stride = Math.max(0.5f, 1.6f * b.hip);
    }

    // ================================================================== building

    /** Texture space for boxes: first a measuring pass, then the boxes placed tallest first in shelves. */
    static final class Packer {
        final List<float[]> sizes = new ArrayList<>();
        int[][] placed;
        int next, texW, texH;

        int[] at(float w, float h, float d) {
            if (placed == null) { sizes.add(new float[]{w, h, d}); return new int[]{0, 0}; }
            return placed[next++];
        }

        void plan() {
            int n = sizes.size();
            Integer[] order = new Integer[n];
            for (int i = 0; i < n; i++) order[i] = i;
            java.util.Arrays.sort(order, (a, c) -> Float.compare(rh(sizes.get(c)), rh(sizes.get(a))));
            for (int w = 64; ; w *= 2) {
                int[][] out = new int[n][];
                int x = 0, y = 0, row = 0, used = 0;
                boolean fits = true;
                for (int i : order) {
                    float[] s = sizes.get(i);
                    int bw = (int) Math.ceil(2 * s[2] + 2 * s[0]) + 1, bh = rh(s);
                    if (bw > w) { fits = false; break; }
                    if (x + bw > w) { x = 0; y += row; row = 0; }
                    out[i] = new int[]{x, y};
                    x += bw;
                    row = Math.max(row, bh);
                    used = Math.max(used, y + row);
                }
                if (fits && (used <= w || w >= 1024)) {
                    placed = out;
                    texW = w;
                    texH = Math.max(32, Integer.highestOneBit(Math.max(1, used - 1)) * 2);
                    if (texH < used) texH *= 2;
                    return;
                }
            }
        }

        static int rh(float[] s) { return (int) Math.ceil(s[2] + s[1]) + 1; }
    }

    /** The texture size this species' boxes need (what its painted texture must be). */
    public static int[] textureSize(DinoSpecies sp) {
        Packer pk = new Packer();
        build(sp, pk, new MeshDefinition());
        pk.plan();
        return new int[]{pk.texW, pk.texH};
    }

    public static LayerDefinition createBodyLayer(DinoSpecies sp) {
        Packer pk = new Packer();
        build(sp, pk, new MeshDefinition());
        pk.plan();
        MeshDefinition mesh = new MeshDefinition();
        build(sp, pk, mesh);
        return LayerDefinition.create(mesh, pk.texW, pk.texH);
    }

    /** A box (x0, y0, z0, w, h, d) with its own texture space. */
    static CubeListBuilder box(CubeListBuilder c, Packer p, float x, float y, float z, float w, float h, float d) {
        int[] uv = p.at(w, h, d);
        return c.texOffs(uv[0], uv[1]).addBox(x, y, z, w, h, d);
    }

    static CubeListBuilder box(Packer p, float x, float y, float z, float w, float h, float d) {
        return box(CubeListBuilder.create(), p, x, y, z, w, h, d);
    }

    /** The same box for both sides: one texture space, mirrored on the right. */
    static CubeListBuilder[] pair(Packer p, float x, float y, float z, float w, float h, float d) {
        int[] uv = p.at(w, h, d);
        return new CubeListBuilder[]{CubeListBuilder.create().texOffs(uv[0], uv[1]).addBox(x, y, z, w, h, d),
                CubeListBuilder.create().texOffs(uv[0], uv[1]).mirror().addBox(-x - w, y, z, w, h, d)};
    }

    static PartPose pose(float x, float y, float z, float xr, float yr, float zr) { return PartPose.offsetAndRotation(x, y, z, xr, yr, zr); }

    private static float min(float a, float u) { return Math.max(a, u); }

    static void build(DinoSpecies sp, Packer p, MeshDefinition mesh) {
        Rig r = sp.rig();
        Body b = sp.body;
        float U = r.U, thin = Math.max(0.6f, 0.05f * U);
        PartDefinition root = mesh.getRoot();

        // ---- body: hips and ribcage
        float[] h = r.hips, c = r.chest;
        CubeListBuilder bc = box(p, -h[4], h[2], h[0], 2 * h[4], h[3] - h[2], h[1] - h[0]);
        box(bc, p, -c[4], c[2], c[0], 2 * c[4], c[3] - c[2], c[1] - c[0]);
        if (b.plan == Body.Plan.THERIZINOSAUR || b.plan == Body.Plan.ANKYLOSAUR || b.plan == Body.Plan.CERATOPSIAN)
            box(bc, p, -c[4] * 0.9f, c[3] - 0.1f * (c[3] - c[2]), (c[0] + h[1]) * 0.5f - 0.25f * (h[1] - c[0]),     // a belly
                    1.8f * c[4], 0.22f * (c[3] - c[2]), 0.5f * (h[1] - c[0]));
        PartDefinition bodyPart = root.addOrReplaceChild("body", bc, pose(0, r.hipY, 0, r.bodyRot, 0, 0));
        float top = Math.min(h[2], c[2]);

        // ---- neck and head
        PartDefinition parent = bodyPart;
        float prev = r.bodyRot, py = r.neckBase[0], pz = r.neckBase[1];
        PartDefinition[] neckParts = new PartDefinition[r.neckLen.length];
        for (int i = 0; i < r.neckLen.length; i++) {
            float len = r.neckLen[i], w = r.neckW[i], hh = r.neckH[i];
            CubeListBuilder nc = box(p, -w / 2, -hh / 2, -len * 1.12f, w, hh, len * 1.12f + hh * 0.3f);
            parent = parent.addOrReplaceChild("neck" + i, nc, pose(0, py, pz, r.neckAbs[i] - prev, 0, 0));
            neckParts[i] = parent;
            prev = r.neckAbs[i]; py = 0; pz = -len;
        }
        float Hh = r.headH, Lh = r.headL, W = r.headW;
        float crTop = -0.55f * Hh, crBottom = 0.45f * Hh - r.jawH;
        CubeListBuilder hc = box(p, -W / 2, crTop, -r.craniumL, W, crBottom - crTop, r.craniumL + 0.12f * Lh);
        box(hc, p, -r.snoutWid / 2, r.snoutTop, -Lh, r.snoutWid, crBottom - r.snoutTop, r.snoutL + 0.06f * Lh);
        PartDefinition headPart = parent.addOrReplaceChild("head", hc, pose(0, py, pz, r.headAbs - prev, 0, 0));
        float jawLen = Lh * 0.96f;
        headPart.addOrReplaceChild("jaw", box(p, -r.jawW / 2, 0, -jawLen, r.jawW, r.jawH, jawLen),
                pose(0, crBottom, 0.08f * Lh, 0, 0, 0));
        headOrnaments(sp, r, b, p, headPart, crTop, thin);

        // ---- tail
        parent = bodyPart; prev = r.bodyRot; py = r.tailBase[0]; pz = r.tailBase[1];
        PartDefinition[] tailParts = new PartDefinition[r.tailLen.length];
        for (int i = 0; i < r.tailLen.length; i++) {
            float len = r.tailLen[i], w = r.tailWid[i], hh = r.tailHt[i];
            CubeListBuilder tc = box(p, -w / 2, -hh / 2, -hh * 0.25f, w, hh, len * 1.1f + hh * 0.25f);
            parent = parent.addOrReplaceChild("tail" + i, tc, pose(0, py, pz, r.tailAbs[i] - prev, 0, 0));
            tailParts[i] = parent;
            prev = r.tailAbs[i]; py = 0; pz = len;
        }

        // ---- hind legs (hanging from the hip joints, outside the body so it can bob over them)
        boolean pad = b.quad && b.plan != Body.Plan.HADROSAUR && b.plan != Body.Plan.IGUANODONT;
        CubeListBuilder[] th = pair(p, -r.thighW / 2, -r.thighW * 0.35f, -r.thighW * 0.62f, r.thighW, r.thighL + r.thighW * 0.35f, r.thighW * 1.25f);
        CubeListBuilder[] sh = pair(p, -r.shinW / 2, -r.shinW * 0.25f, -r.shinW * 0.6f, r.shinW, r.shinL + r.shinW * 0.25f, r.shinW * 1.2f);
        CubeListBuilder[] mt = pair(p, -r.metaW / 2, -r.metaW * 0.2f, -r.metaW / 2, r.metaW, r.metaL + r.metaW * 0.2f, r.metaW);
        float fz0 = pad ? -r.footL * 0.62f : -r.footL, fz1 = pad ? r.footL * 0.45f : r.metaW * 0.5f;
        CubeListBuilder[] ft = pair(p, -r.footW / 2, 0, fz0, r.footW, r.footH, fz1 - fz0);
        float skl = b.sickle * U, skw = Math.max(0.5f, skl * 0.28f);
        CubeListBuilder[] sk = b.sickle > 0 ? pair(p, -skw / 2, -skl, -skw / 2, skw, skl, skw) : null;
        float lwl = Math.max(2f, r.shinL * 0.9f), lwh = Math.max(1.2f, r.shinL * 0.8f);
        CubeListBuilder[] lw = b.legWings ? pair(p, -thin / 2, 0, 0, thin, lwh, lwl) : null;
        for (int s = 0; s < 2; s++) {
            float sx = s == 0 ? r.hipX : -r.hipX;
            PartDefinition leg = root.addOrReplaceChild(s == 0 ? "leg_l" : "leg_r", th[s], pose(sx, r.hipY, 0, r.thighAbs, 0, 0));
            PartDefinition shin = leg.addOrReplaceChild("shin", sh[s], pose(0, r.thighL, 0, r.shinAbs - r.thighAbs, 0, 0));
            PartDefinition meta = shin.addOrReplaceChild("meta", mt[s], pose(0, r.shinL, 0, r.metaAbs - r.shinAbs, 0, 0));
            PartDefinition foot = meta.addOrReplaceChild("foot", ft[s], pose(0, r.metaL, 0, -r.metaAbs, 0, 0));
            if (sk != null)
                foot.addOrReplaceChild("sickle", sk[s], pose((s == 0 ? -1 : 1) * r.footW * 0.25f, r.footH * 0.4f, -r.footL * 0.35f, 0.9f, 0, 0));
            if (sk != null && b.sickles > 1)                     // the first toe's claw too, inside and further back
                foot.addOrReplaceChild("sickle2", sk[s], pose((s == 0 ? -1 : 1) * r.footW * 0.48f, r.footH * 0.4f, -r.footL * 0.05f, 0.7f, 0, 0));
            if (lw != null) shin.addOrReplaceChild("legwing", lw[s], pose((s == 0 ? 1 : -1) * r.shinW * 0.6f, 0, r.shinW * 0.3f, -r.shinAbs + 0.6f, 0, 0));
        }

        // ---- arms (two-leggers) or front legs (four-leggers), on the body at the shoulders
        float[] sa = r.shoulderAt;
        CubeListBuilder[] up = pair(p, -r.upperW / 2, -r.upperW * 0.4f, -r.upperW * 0.55f, r.upperW, r.upperL + r.upperW * 0.4f, r.upperW * 1.1f);
        CubeListBuilder[] fo = pair(p, -r.foreW / 2, -r.foreW * 0.2f, -r.foreW / 2, r.foreW, r.foreL + r.foreW * 0.2f, r.foreW);
        float hz = b.quad ? r.handW * 0.65f : r.handW * 0.5f;
        CubeListBuilder[] ha = pair(p, -r.handW / 2, 0, -hz, r.handW, r.handL, hz * 1.7f);
        // folded wings: a panel of arm feathers lying along each flank, hung from the shoulder
        float wl = Math.max(2f, b.armL * U * 0.95f), wh = Math.max(1.2f, b.armL * U * 0.38f);
        CubeListBuilder[] wf = b.wings ? pair(p, -thin / 2, -wh * 0.15f, -wl * 0.12f, thin, wh, wl) : null;
        float cl = b.clawL * U, cw = Math.max(0.4f, Math.min(cl * 0.22f, r.handW * 0.5f));
        CubeListBuilder[] cp = b.clawL > 0 && !b.quad ? pair(p, -cw / 2, 0, -cw / 2, cw, cl, cw) : null;
        float tl = b.thumbSpike * U, tw = Math.max(0.5f, tl * 0.35f);
        CubeListBuilder[] tp = b.thumbSpike > 0 ? pair(p, -tw / 2, -tl, -tw / 2, tw, tl, tw) : null;
        for (int s = 0; s < 2; s++) {
            float sx = s == 0 ? sa[0] : -sa[0];
            PartDefinition arm = bodyPart.addOrReplaceChild(s == 0 ? "arm_l" : "arm_r", up[s], pose(sx, sa[1], sa[2], r.upperAbs - r.bodyRot, 0, 0));
            PartDefinition fore = arm.addOrReplaceChild("fore", fo[s], pose(0, r.upperL, 0, r.foreAbs - r.upperAbs, 0, 0));
            PartDefinition hand = fore.addOrReplaceChild("hand", ha[s], pose(0, r.foreL, 0, r.handAbs - r.foreAbs, 0, 0));
            float side = s == 0 ? 1 : -1;
            if (wf != null) arm.addOrReplaceChild("wing", wf[s], pose(side * r.upperW * 0.7f, 0, 0, r.bodyRot - r.upperAbs - 0.12f, 0, 0));
            if (cp != null) {
                int n = sp.id.equals("tyrannosaurus") || b.plan == Body.Plan.THEROPOD && b.armL < 0.1f * b.L ? 2 : 3;
                for (int k = 0; k < n; k++) {
                    float off = (k - (n - 1) / 2f) * cw * 1.1f;
                    hand.addOrReplaceChild("claw" + k, cp[s], pose(off, r.handL * 0.85f, 0, -0.55f, 0, 0));
                }
            }
            if (tp != null) hand.addOrReplaceChild("thumb", tp[s], pose(-side * r.handW * 0.45f, r.handL * 0.25f, -r.handW * 0.3f, 0.35f, 0, -side * 0.5f));
        }

        bodyOrnaments(sp, r, b, p, bodyPart, neckParts, tailParts, top, thin);
    }

    /** Horns, frill, crests, dome. */
    static void headOrnaments(DinoSpecies sp, Rig r, Body b, Packer p, PartDefinition head, float crTop, float thin) {
        float U = r.U, Hh = r.headH, Lh = r.headL, W = r.headW;
        float headAbs = r.headAbs;
        if (b.hornBrow > 0) {
            float hl = b.hornBrow * U, bw = Math.max(0.7f, Math.min(hl * 0.3f, W * 0.3f));
            CubeListBuilder[] hp = pair(p, -bw / 2, -hl * 0.6f, -bw / 2, bw, hl * 0.6f, bw);
            CubeListBuilder[] tp = pair(p, -bw * 0.3f, -hl * 0.42f, -bw * 0.3f, bw * 0.6f, hl * 0.42f, bw * 0.6f);
            float z = -r.craniumL * (0.15f + 0.55f * (b.hornBrowZ));
            for (int s = 0; s < 2; s++) {
                float side = s == 0 ? 1 : -1;
                PartDefinition horn = head.addOrReplaceChild(s == 0 ? "horn_l" : "horn_r", hp[s],
                        pose(side * W * 0.3f, crTop + bw * 0.3f, z, (float) Math.toRadians(90 - b.hornBrowUp) - headAbs * 0.5f, 0,
                                side * (float) Math.toRadians(b.hornBrowOut)));
                horn.addOrReplaceChild("tip", tp[s], pose(0, -hl * 0.58f, 0, 0.12f, 0, 0));
            }
        }
        if (b.hornNasal > 0) {
            float hl = b.hornNasal * U, bw = Math.max(0.7f, Math.min(hl * 0.38f, r.snoutWid * 0.55f));
            PartDefinition horn = head.addOrReplaceChild("horn_nasal", box(p, -bw / 2, -hl * 0.6f, -bw / 2, bw, hl * 0.6f, bw),
                    pose(0, r.snoutTop + bw * 0.25f, -Lh * 0.74f, (float) Math.toRadians(90 - b.hornNasalUp) - headAbs * 0.5f, 0, 0));
            horn.addOrReplaceChild("tip", box(p, -bw * 0.3f, -hl * 0.42f, -bw * 0.3f, bw * 0.6f, hl * 0.42f, bw * 0.6f), pose(0, -hl * 0.58f, 0, 0.1f, 0, 0));
        }
        if (b.nasalBoss > 0) {
            float bh = b.nasalBoss * U, bw = r.snoutWid * 0.95f, bl = Lh * 0.32f;
            head.addOrReplaceChild("boss", box(p, -bw / 2, -bh, -bl / 2, bw, bh, bl), pose(0, r.snoutTop + bh * 0.25f, -Lh * 0.7f, 0, 0, 0));
        }
        if (b.hornCheek > 0) {
            float hl = b.hornCheek * U, bw = Math.max(0.6f, hl * 0.5f);
            CubeListBuilder[] cp = pair(p, -bw / 2, 0, -bw / 2, bw, hl, bw);
            for (int s = 0; s < 2; s++) {
                float side = s == 0 ? 1 : -1;
                head.addOrReplaceChild(s == 0 ? "cheek_l" : "cheek_r", cp[s], pose(side * W * 0.48f, 0.3f * Hh, -r.craniumL * 0.55f, 0, 0, -side * 1.0f));
            }
        }
        if (b.frillH > 0) {
            float fh = b.frillH * U, fw = b.frillW * U, ft = Math.max(thin, 0.12f * U);
            float frillRot = (float) -Math.toRadians(90 - b.frillUp) - headAbs;
            PartDefinition frill = head.addOrReplaceChild("frill", box(p, -fw / 2, -fh, 0, fw, fh, ft),
                    pose(0, crTop + 0.15f * Hh, 0.04f * Lh, frillRot, 0, 0));
            if (b.frillSpikes > 0) {
                float sl = b.frillSpikeL * U, sw = Math.max(0.6f, sl * 0.2f);
                for (int k = 0; k < b.frillSpikes; k++) {
                    float t = b.frillSpikes == 1 ? 0.5f : (float) k / (b.frillSpikes - 1);
                    float x = (t - 0.5f) * fw * 0.85f, spread = (t - 0.5f) * 1.6f;
                    frill.addOrReplaceChild("spike" + k, box(p, -sw / 2, -sl, -sw / 2, sw, sl, sw),
                            pose(x, -fh * (0.92f - 0.25f * Math.abs(t - 0.5f) * 2), ft * 0.5f, -0.25f, 0, spread));
                }
            }
        }
        if (b.dome > 0) {
            float d = b.dome * U, dw = W * 0.92f, dl = r.craniumL * 0.95f + 0.12f * Lh;
            PartDefinition dome = head.addOrReplaceChild("dome", box(p, -dw / 2, -d, -dl + 0.12f * Lh, dw, d, dl), pose(0, crTop + d * 0.15f, 0, 0, 0, 0));
            float k = Math.max(0.6f, d * 0.28f);
            CubeListBuilder knobs = CubeListBuilder.create();
            for (int i = 0; i < 5; i++) box(knobs, p, (i - 2) * dw * 0.22f - k / 2, -k * 0.7f, 0.12f * Lh - k * 0.4f, k, k, k);
            dome.addOrReplaceChild("knobs", knobs, PartPose.ZERO);
        }
        switch (b.crest) {
            case TUBE -> {
                float t = Math.max(thin, b.crestH * U), cl = b.crestL * U;
                head.addOrReplaceChild("crest", box(p, -t / 2, -t / 2, 0, t, t, cl), pose(0, r.snoutTop + t * 0.2f, -Lh * 0.62f, 0.32f - headAbs * 0.3f, 0, 0));
            }
            case HELMET -> {
                float ch = b.crestH * U, cl = b.crestL * U, cw = Math.max(thin, W * 0.32f);
                head.addOrReplaceChild("crest", box(p, -cw / 2, -ch, -cl * 0.55f, cw, ch, cl), pose(0, crTop + ch * 0.1f, -r.craniumL * 0.45f, 0, 0, 0));
            }
            case TWIN -> {
                float ch = b.crestH * U, cl = b.crestL * U;
                CubeListBuilder[] tp = pair(p, -thin / 2, -ch, 0, thin, ch, cl);
                for (int s = 0; s < 2; s++)
                    head.addOrReplaceChild(s == 0 ? "crest_l" : "crest_r", tp[s], pose((s == 0 ? 1 : -1) * r.snoutWid * 0.32f, r.snoutTop + ch * 0.15f, -Lh * 0.95f, 0, 0, 0));
            }
            case ROUND -> {
                float ch = b.crestH * U, cl = b.crestL * U, cw = Math.max(thin, r.snoutWid * 0.5f);
                head.addOrReplaceChild("crest", box(p, -cw / 2, -ch, -cl * 0.5f, cw, ch, cl), pose(0, r.snoutTop + ch * 0.12f, -Lh * 0.68f, 0, 0, 0));
            }
            case RIDGE, COMB -> {
                float ch = Math.max(thin * 0.8f, b.crestH * U), cl = b.crestL * U;
                float z = b.crest == Body.Crest.COMB ? -r.craniumL * 0.45f - cl * 0.5f : -Lh * 0.9f;
                float y = b.crest == Body.Crest.COMB ? crTop + ch * 0.1f : r.snoutTop + ch * 0.1f;
                head.addOrReplaceChild("crest", box(p, -thin / 2, -ch, 0, thin, ch, cl), pose(0, y, z, 0, 0, 0));
            }
            case SPIKE -> {
                float t = Math.max(thin, b.crestH * U), cl = b.crestL * U;
                head.addOrReplaceChild("crest", box(p, -t / 2, -t / 2, 0, t, t, cl), pose(0, crTop + t * 0.3f, -r.craniumL * 0.5f, 0.45f, 0, 0));
            }
            case HATCHET, BLADE -> {
                float ch = b.crestH * U, cl = b.crestL * U, cw = Math.max(thin, W * 0.22f);
                head.addOrReplaceChild("crest", box(p, -cw / 2, -ch, -cl * 0.4f, cw, ch, cl), pose(0, crTop + ch * 0.1f, -r.craniumL * 0.6f, b.crest == Body.Crest.HATCHET ? -0.35f : 0.2f, 0, 0));
            }
            default -> { }
        }
    }

    /** Sails, plates, spikes, clubs, armour, spines, feathers on the body, neck and tail. */
    static void bodyOrnaments(DinoSpecies sp, Rig r, Body b, Packer p, PartDefinition body, PartDefinition[] neck, PartDefinition[] tail, float top, float thin) {
        float U = r.U, torso = b.torso * U, depth = b.depth * U, halfW = b.width * U * 0.5f;
        float z0 = r.chest[0], z1 = r.hips[1];
        if (b.sailH > 0) {
            int n = 7;
            CubeListBuilder sc = CubeListBuilder.create();
            for (int i = 0; i < n; i++) {
                float t0 = b.sailFrom + (b.sailTo - b.sailFrom) * i / n, t1 = b.sailFrom + (b.sailTo - b.sailFrom) * (i + 1) / n, tm = (t0 + t1) / 2;
                float hgt = b.sailH * U * (float) Math.sin(Math.PI * (0.12 + 0.76 * tm));
                float za = z0 + (z1 - z0) * t0, zb = z0 + (z1 - z0) * t1;
                box(sc, p, -thin / 2, top - hgt + depth * 0.06f, za, thin, hgt, zb - za + 0.2f);
            }
            body.addOrReplaceChild("sail", sc, PartPose.ZERO);
        }
        float tailRidge = b.sailOnTail * U;
        if (b.sailH > 0 && b.sailOnTail > 0) tailRidge = b.sailH * U * b.sailOnTail * 0.55f;
        if (tailRidge > 0) {
            for (int i = 0; i < Math.min(3, tail.length); i++) {
                float hgt = tailRidge * (1f - i * 0.3f), len = r.tailLen[i];
                tail[i].addOrReplaceChild("ridge", box(p, -thin / 2, -r.tailHt[i] / 2 - hgt + 0.3f, 0, thin, hgt, len * 1.05f), PartPose.ZERO);
            }
        }
        if (b.tailFin > 0) {
            for (int i = 1; i < tail.length - 1; i++) {
                float t = (float) i / (tail.length - 1), hgt = b.tailFin * U * (float) Math.sin(Math.PI * t) * 0.55f, len = r.tailLen[i];
                CubeListBuilder fc = box(p, -thin / 2, -r.tailHt[i] / 2 - hgt + 0.3f, 0, thin, hgt, len * 1.05f);
                box(fc, p, -thin / 2, r.tailHt[i] / 2 - 0.3f, 0, thin, hgt * 0.7f, len * 1.05f);
                tail[i].addOrReplaceChild("fin", fc, PartPose.ZERO);
            }
        }
        if (b.plates > 0) {
            // stegosaur plates in two staggered rows: small on the neck, biggest over the hips and tail base
            int k = 0;
            for (int i = 0; i < neck.length; i++, k++) {
                float hgt = b.plateH * U * (0.32f + 0.12f * i);
                neck[i].addOrReplaceChild("plates", plate(p, hgt, thin, (k % 2 == 0 ? 1 : -1) * r.neckW[i] * 0.15f, -r.neckH[i] / 2 + 0.3f, -r.neckLen[i] * 0.5f), PartPose.ZERO);
            }
            int nb = 5;
            CubeListBuilder pc = CubeListBuilder.create();
            for (int i = 0; i < nb; i++, k++) {
                float t = (i + 0.5f) / nb, z = z0 + (z1 - z0) * (0.08f + 0.88f * t);
                float hgt = b.plateH * U * (0.55f + 0.45f * t);
                plate(pc, p, hgt, thin, (k % 2 == 0 ? 1 : -1) * halfW * 0.16f, top + depth * 0.05f, z);
            }
            body.addOrReplaceChild("plates", pc, PartPose.ZERO);
            for (int i = 0; i < Math.min(tail.length - 1, 3); i++) {
                CubeListBuilder tc = CubeListBuilder.create();
                for (int j = 0; j < 2; j++, k++) {
                    float hgt = b.plateH * U * (0.95f - 0.22f * i - 0.1f * j);
                    plate(tc, p, hgt, thin, (k % 2 == 0 ? 1 : -1) * r.tailWid[i] * 0.18f, -r.tailHt[i] / 2 + 0.3f, r.tailLen[i] * (0.25f + 0.5f * j));
                }
                tail[i].addOrReplaceChild("plates", tc, PartPose.ZERO);
            }
        }
        if (b.tailSpikes > 0) {
            int last = tail.length - 1;
            float sl = b.tailSpikeL * U, sw = Math.max(0.6f, sl * 0.12f);
            for (int j = 0; j < b.tailSpikes; j++) {
                float side = j % 2 == 0 ? 1 : -1, z = r.tailLen[last] * (j < 2 ? 0.3f : 0.75f);
                tail[last].addOrReplaceChild("spike" + j, box(p, -sw / 2, -sl, -sw / 2, sw, sl, sw),
                        pose(side * r.tailWid[last] * 0.3f, -r.tailHt[last] * 0.3f, z, -0.65f, 0, side * 1.05f));
            }
        }
        if (b.clubW > 0) {
            int last = tail.length - 1;
            float cw = b.clubW * U;
            tail[last].addOrReplaceChild("club", box(p, -cw / 2, -cw * 0.25f, r.tailLen[last] * 0.55f, cw, cw * 0.5f, cw * 0.8f), PartPose.ZERO);
        }
        if (b.armour > 0) {
            float s = Math.max(0.7f, b.armour * U);
            CubeListBuilder ac = CubeListBuilder.create();
            boolean light = b.plan == Body.Plan.THEROPOD || b.plan == Body.Plan.TITANOSAUR;
            int cols = b.armourCols > 0 ? b.armourCols : light ? 2 : 4, rows = light ? 5 : 6;
            for (int i = 0; i < rows; i++) for (int j = 0; j < cols; j++) {
                float z = z0 + (z1 - z0) * (i + 0.5f) / rows, x = cols == 1 ? 0 : (j - (cols - 1) / 2f) * halfW * (cols == 2 ? 0.5f : 0.45f);
                float xx = cols == 1 ? 0 : x;
                box(ac, p, xx - s / 2, top - s * 0.45f, z - s / 2, s, s * 0.7f, s);
            }
            if (b.sideSpikes > 0) {
                float sl = b.sideSpikes * U * 1.6f;
                for (int i = 0; i < 4; i++) {
                    float z = z0 + (z1 - z0) * (i + 0.5f) / 4;
                    box(ac, p, halfW * 0.95f, top + depth * 0.35f, z - s * 0.4f, sl, s * 0.7f, s * 0.8f);
                    box(ac, p, -halfW * 0.95f - sl, top + depth * 0.35f, z - s * 0.4f, sl, s * 0.7f, s * 0.8f);
                }
            }
            body.addOrReplaceChild("armour", ac, PartPose.ZERO);
            for (int i = 0; i < tail.length - 1; i++) {
                float ts = s * (1f - 0.18f * i);
                tail[i].addOrReplaceChild("studs", box(p, -ts / 2, -r.tailHt[i] / 2 - ts * 0.45f, r.tailLen[i] * 0.3f, ts, ts * 0.7f, ts), PartPose.ZERO);
            }
        }
        if (b.shoulderSpikeL > 0) {
            // a broad base and a narrower tip, so long ones (Gigantspinosaurus, Edmontonia) read as spikes, not planks
            float sl = b.shoulderSpikeL * U, sw = Math.max(0.6f, Math.min(sl * 0.25f, 0.6f + sl * 0.14f)), tw = Math.max(0.5f, sw * 0.5f);
            for (int s = 0; s < 2; s++) {
                float side = s == 0 ? 1 : -1;
                PartDefinition spk = body.addOrReplaceChild(s == 0 ? "sspike_l" : "sspike_r", box(p, -sw / 2, -sl * 0.62f, -sw / 2, sw, sl * 0.62f, sw),
                        pose(side * halfW * 0.9f, top + depth * 0.3f, z0 + (z1 - z0) * 0.12f, (float) Math.toRadians(b.shoulderSpikeA), 0, side * 1.2f));
                spk.addOrReplaceChild("tip", box(p, -tw / 2, -sl * 0.42f, -tw / 2, tw, sl * 0.42f, tw), pose(0, -sl * 0.6f, 0, 0, 0, 0));
            }
        }
        if (b.hump > 0) {
            // a low broad base and a taller narrow peak, so it reads as a hump rather than a post (Concavenator)
            float hh = b.hump * U, hl = Math.max(thin * 3, 0.3f * torso), ht = Math.max(thin, 0.9f * thin + 0.02f * hl), zc = -0.18f * torso;
            CubeListBuilder hc = CubeListBuilder.create();
            box(hc, p, -ht / 2, top - hh * 0.55f + depth * 0.05f, zc - hl / 2, ht, hh * 0.55f, hl);
            box(hc, p, -ht / 2, top - hh + depth * 0.05f, zc - hl * 0.22f, ht, hh * 0.45f + 0.01f, hl * 0.44f);
            body.addOrReplaceChild("hump", hc, PartPose.ZERO);
        }
        if (b.neckSpines > 0 && b.pairedSpines) {
            // two rows of tall spines leaning apart, tallest mid-neck, short over the shoulders
            float sh = b.neckSpines * U;
            for (int i = 0; i < neck.length; i++) {
                float k = (float) Math.sin(Math.PI * (0.25 + 0.6 * i / Math.max(1, neck.length - 1)));
                CubeListBuilder[] sp2 = pair(p, -thin / 2, -sh * k, -r.neckLen[i] * 0.85f, thin, sh * k, r.neckLen[i] * 0.7f);
                for (int s2 = 0; s2 < 2; s2++)
                    neck[i].addOrReplaceChild(s2 == 0 ? "spines_l" : "spines_r", sp2[s2],
                            pose((s2 == 0 ? 1 : -1) * r.neckW[i] * 0.2f, -r.neckH[i] * 0.4f, 0, -0.15f, 0, (s2 == 0 ? 1 : -1) * 0.22f));
            }
        } else if (b.neckSpines > 0) {
            float sh = b.neckSpines * U, sw = Math.max(thin, sh * 0.5f);
            for (int i = 0; i < neck.length; i++)
                neck[i].addOrReplaceChild("spines", box(p, -thin / 2, -r.neckH[i] / 2 - sh + 0.2f, -r.neckLen[i] * 0.9f, thin, sh, r.neckLen[i] * 0.85f), PartPose.ZERO);
            body.addOrReplaceChild("spines", box(p, -thin / 2, top - sh + 0.3f, z0, thin, sh, z1 - z0), PartPose.ZERO);
            for (int i = 0; i < tail.length - 2; i++) {
                float th = sh * (1f - 0.15f * i);
                tail[i].addOrReplaceChild("spines", box(p, -thin / 2, -r.tailHt[i] / 2 - th + 0.2f, 0, thin, th, r.tailLen[i]), PartPose.ZERO);
            }
        }
        if (b.tailFan) {
            int last = tail.length - 1;
            float fw = Math.max(r.tailWid[last] * 4f, 0.08f * b.L * U), len = r.tailLen[last] * 1.15f;
            tail[last].addOrReplaceChild("fan", box(p, -fw / 2, -thin / 2, 0, fw, thin, len), PartPose.ZERO);
            if (tail.length > 2) {
                float fw2 = fw * 0.75f;
                tail[last - 1].addOrReplaceChild("fan", box(p, -fw2 / 2, -thin / 2, r.tailLen[last - 1] * 0.2f, fw2, thin, r.tailLen[last - 1] * 0.9f), PartPose.ZERO);
            }
        }
    }

    static CubeListBuilder plate(Packer p, float hgt, float thin, float x, float y, float z) { return plate(CubeListBuilder.create(), p, hgt, thin, x, y, z); }

    static CubeListBuilder plate(CubeListBuilder c, Packer p, float hgt, float thin, float x, float y, float z) {
        float w = hgt * 0.85f;
        return box(c, p, x - thin / 2, y - hgt, z - w / 2, thin, hgt, w);
    }

    // ================================================================== animation

    @Override
    public void setupAnim(DinoRenderer.State s) {
        super.setupAnim(s);
        float t = s.ageInTicks;
        float v = s.gaitSpeed * 20f;                                           // blocks per second
        float walkV = Math.max(0.3f, sp.walk), runV = Math.max(walkV + 0.1f, sp.run);
        float move = clamp(v / (walkV * 0.6f), 0, 1);                          // 0 standing, 1 walking or faster
        float runK = clamp((v - walkV) / (runV - walkV), 0, 1);
        float ph = (float) (s.gait / stride * Math.PI * 2);
        float amp = (b.quad && !(b.plan == Body.Plan.HADROSAUR || b.plan == Body.Plan.IGUANODONT) ? 0.3f : 0.42f) + 0.22f * runK;
        float flexK = b.quad ? 0.45f : 0.75f;

        // ---- legs: swing, knee lift on the forward stroke, feet kept level while planted
        float[] reach = new float[2];
        for (int sIdx = 0; sIdx < 2; sIdx++) {
            float lp = ph + (sIdx == 0 ? 0 : (float) Math.PI);
            float q = amp * move * (float) Math.sin(lp);
            float f = flexK * move * Math.max(0, (float) Math.cos(lp)) * (0.6f + 0.6f * runK);
            ModelPart[] L = hind[sIdx];
            L[0].xRot = r.thighAbs - q;
            L[1].xRot = (r.shinAbs - r.thighAbs) + f;
            L[2].xRot = (r.metaAbs - r.shinAbs) - f * 0.6f;
            float a1 = r.thighAbs - q, a2 = r.shinAbs - q + f, a3 = r.metaAbs - q + f * 0.4f;
            L[3].xRot = -a3 + f * 0.35f;
            reach[sIdx] = r.thighL * (float) Math.cos(a1) + r.shinL * (float) Math.cos(a2) + r.metaL * (float) Math.cos(a3) + r.footH;
        }
        float restReach = 24f - r.hipY;
        float dy = Math.max(0, restReach - Math.max(reach[0], reach[1]));
        // a little extra bounce when running
        dy += runK * move * 0.03f * restReach * (float) Math.abs(Math.sin(ph));
        body.y += dy;
        hind[0][0].y += dy;
        hind[1][0].y += dy;

        // ---- front legs (lateral-sequence walk) or arms
        if (b.quad) {
            float frontReachRest = r.upperL * (float) Math.cos(r.upperAbs) + r.foreL * (float) Math.cos(r.foreAbs) + r.handL;
            float[] fr = new float[2];
            for (int sIdx = 0; sIdx < 2; sIdx++) {
                float lp = ph + (float) Math.PI * (sIdx == 0 ? 0.5f : 1.5f);
                float q = amp * 0.9f * move * (float) Math.sin(lp);
                float f = 0.5f * move * Math.max(0, (float) Math.cos(lp));
                ModelPart[] A = front[sIdx];
                A[0].xRot = (r.upperAbs - r.bodyRot) - q;
                A[1].xRot = (r.foreAbs - r.upperAbs) + f;
                A[2].xRot = (r.handAbs - r.foreAbs) - f * 0.5f;
                float a1 = r.upperAbs - q, a2 = r.foreAbs - q + f, a3 = r.handAbs - q + f * 0.5f;
                fr[sIdx] = r.upperL * (float) Math.cos(a1) + r.foreL * (float) Math.cos(a2) + r.handL * (float) Math.cos(a3);
            }
            float dyF = Math.max(0, frontReachRest - Math.max(fr[0], fr[1]));
            body.xRot += (float) Math.atan2(dyF - dy, Math.max(1f, -r.shoulderAt[2]));
        } else {
            for (int sIdx = 0; sIdx < 2; sIdx++) {
                float sw = (float) Math.sin(ph + (sIdx == 0 ? Math.PI : 0)) * 0.25f * move;
                front[sIdx][0].xRot = (r.upperAbs - r.bodyRot) + sw + 0.05f * (float) Math.sin(t * 0.05f + sIdx);
                front[sIdx][1].xRot = (r.foreAbs - r.upperAbs) - 0.08f * (float) Math.sin(t * 0.05f + sIdx);
            }
        }

        // ---- tail sway and droop with the stride
        for (int i = 0; i < tail.length; i++) {
            float k = (i + 1f) / tail.length;
            tail[i].yRot = (float) Math.sin(t * 0.06f - i * 0.5f) * 0.035f * k + (float) Math.sin(ph - i * 0.35f) * 0.07f * move * k;
            tail[i].xRot += (float) Math.sin(t * 0.045f - i * 0.4f) * 0.012f + (float) Math.sin(ph * 2 - i * 0.4f) * 0.015f * move;
        }

        // ---- head and neck: look where it looks, bob with the stride
        float yaw = (float) Math.toRadians(clamp(s.yRot, -75, 75)), pitch = (float) Math.toRadians(clamp(s.xRot, -45, 45));
        float share = neck.length > 0 ? 0.55f / neck.length : 0;
        for (ModelPart n : neck) { n.yRot += yaw * share; n.xRot += pitch * share; }
        head.yRot += yaw * 0.45f;
        head.xRot += pitch * 0.45f;
        if (neck.length > 0) neck[0].xRot += (float) Math.sin(ph * 2) * 0.035f * move + (float) Math.sin(t * 0.07f) * 0.01f;
        jaw.xRot = 0.04f + 0.03f * (float) Math.sin(t * 0.09f);

        // ---- grazing / feeding
        if (s.eat > 0) {
            float e = s.eat;
            float down = (b.plan == Body.Plan.BRACHIOSAUR ? -0.15f : 0.5f) * e;
            for (ModelPart n : neck) n.xRot += down * 1.2f / Math.max(1, neck.length) + 0.12f * e;
            head.xRot += 0.45f * e;
            jaw.xRot += 0.18f * e * (0.5f + 0.5f * (float) Math.sin(t * 0.9f));
        }
        // ---- calls: the jaw opens with the sound
        if (s.call > 0) jaw.xRot += 0.55f * (float) Math.sin(Math.PI * s.call);
        // ---- the roar of the big hunters: head up, mouth wide, a shake
        if (s.roar > 0) {
            float e = (float) Math.sin(Math.PI * Math.min(1, s.roar * 1.3f));
            for (ModelPart n : neck) n.xRot -= 0.32f * e / Math.max(1, neck.length);
            head.xRot -= 0.25f * e;
            head.yRot += (float) Math.sin(t * 1.3f) * 0.07f * e;
            jaw.xRot += 0.95f * e;
            body.xRot -= 0.06f * e;
        }
        // ---- the attack, by kind
        if (s.attack > 0) attack(s.attack, t);
    }

    private void attack(float a, float t) {
        float e = (float) Math.sin(Math.PI * a);
        float lunge = 0.05f * b.L * r.U;
        switch (sp.attack) {
            case BITE -> {
                float open = a < 0.55f ? (float) Math.sin(a / 0.55f * Math.PI / 2) : (float) Math.cos((a - 0.55f) / 0.45f * Math.PI / 2);
                jaw.xRot += 0.9f * open;
                for (ModelPart n : neck) n.xRot += 0.22f * e / Math.max(1, neck.length);
                head.xRot -= 0.18f * open;
                body.z -= lunge * e;
            }
            case GORE -> {
                head.xRot += 0.5f * (float) Math.sin(2 * Math.PI * a);
                body.z -= lunge * e * 0.7f;
                body.xRot += 0.05f * e;
            }
            case HEADBUTT -> {
                for (ModelPart n : neck) n.xRot += 0.35f * e / Math.max(1, neck.length);
                head.xRot += 0.7f * e;
                body.z -= lunge * 1.3f * e;
            }
            case TAIL -> {
                for (int i = 0; i < tail.length; i++) {
                    float lag = Math.max(0, Math.min(1, a * 1.2f - i * 0.05f));
                    tail[i].yRot += 0.42f * (float) Math.sin(Math.PI * lag) * (1f + i * 0.15f) / tail.length * 2.2f;
                }
                body.yRot -= 0.12f * e;
            }
            case CLAW -> {
                for (int s = 0; s < 2; s++) {
                    float k = s == 0 ? e : (float) Math.sin(Math.PI * Math.max(0, Math.min(1, a * 1.3f - 0.3f)));
                    front[s][0].xRot -= 1.1f * k;
                    front[s][1].xRot -= 0.5f * k;
                }
                body.xRot -= 0.12f * e;
            }
            case KICK -> {
                for (int s = 0; s < 2; s++) {
                    hind[s][0].xRot -= 0.8f * e;
                    hind[s][1].xRot -= 0.3f * e;
                    hind[s][0].y -= 0.15f * (24f - r.hipY) * e;
                }
                body.y -= 0.15f * (24f - r.hipY) * e;
                body.xRot -= 0.25f * e;
                if (b.wings) for (int s = 0; s < 2; s++) front[s][0].xRot -= 0.6f * e;
            }
            case STOMP -> {
                body.xRot -= 0.12f * e;
                for (int s = 0; s < 2; s++) { front[s][0].xRot -= 0.5f * e; front[s][1].xRot += 0.4f * e; }
                for (ModelPart n : neck) n.xRot -= 0.1f * e / Math.max(1, neck.length);
            }
        }
    }

    static float clamp(float v, float lo, float hi) { return v < lo ? lo : v > hi ? hi : v; }
}
