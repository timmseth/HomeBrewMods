package com.seth.fossilrecord.dino;

/**
 * A species' {@link Body} laid out as model geometry, in model units (vanilla model space: y down, the ground at
 * y = 24, the head toward -z). The model is drawn at {@code 16 / U} blocks per unit, so it stands at its real size.
 * <p>
 * The parts: the body pivots at the hip joint and carries the ribcage and hips, the neck chain (then head and jaw),
 * the tail chain and the arms or front legs; the hind legs hang from the hip joints as parts of their own, so the body
 * can bob over them as they swing. Every rest angle here is an absolute x-rotation; the model turns them into each
 * part's own rotation relative to its parent. Leg segment lengths are solved so the feet stand on the ground.
 * <p>
 * The server takes the hitbox, eye height and reach from the same numbers ({@link #width()}, {@link #height()},
 * {@link #eye()}, {@link #frontReach()}).
 */
public final class Rig {
    public final Body b;
    /** Model units per metre. */
    public final float U;
    public final float hipY, bodyRot;
    /** Hips and ribcage boxes, body-local: {z0, z1, y0, y1, halfWidth}. */
    public final float[] hips, chest;
    /** Neck base and tail base, body-local {y, z}. */
    public final float[] neckBase, tailBase;
    public final float[] neckLen, neckW, neckH, neckAbs;
    public final float headAbs;
    /** Head: total length, height, width; cranium/snout split; jaw. */
    public final float headL, headH, headW, craniumL, snoutL, snoutWid, snoutTop, jawH, jawW;
    public final float[] tailLen, tailWid, tailHt, tailAbs;
    /** Hind leg: lateral offset, segment lengths, widths, absolute angles (radians, x-rotation). */
    public final float hipX, thighL, shinL, metaL, footL, footH, footW, thighW, shinW, metaW;
    public final float thighAbs, shinAbs, metaAbs;
    /** Front legs (four-leggers) or arms (two-leggers): shoulder (body-local {x, y, z}), segments, widths, angles. */
    public final float[] shoulderAt;
    public final float upperL, foreL, handL, upperW, foreW, handW, upperAbs, foreAbs, handAbs;

    public Rig(Body b) {
        this.b = b;
        float u = Math.max(64f / b.L, 4.5f / b.skull);
        u = Math.min(u, 220f / b.L);
        U = u;
        hipY = 24f - b.hip * u;

        float torso = b.torso * u, depth = b.depth * u, width = b.width * u;
        boolean low = b.plan == Body.Plan.CERATOPSIAN || b.plan == Body.Plan.PROTOCERATOPS || b.plan == Body.Plan.STEGOSAUR
                || b.plan == Body.Plan.ANKYLOSAUR || b.plan == Body.Plan.NODOSAUR;
        float hd = b.quad ? 0.95f : 0.88f;
        hips = new float[]{-0.44f * torso, 0.2f * torso, -0.46f * depth * hd, 0.4f * depth * hd, 0.46f * width};
        chest = new float[]{-1.04f * torso, -0.34f * torso, -0.42f * depth, 0.58f * depth, 0.5f * width};
        neckBase = new float[]{low ? 0.02f * depth : -0.14f * depth, -1.0f * torso};
        tailBase = new float[]{-0.08f * depth, 0.16f * torso};

        // the shoulder joint, and the body's pitch: four-leggers tilt so the shoulder sits at its height
        float sY = 0.12f * depth, sZ = b.quad ? -0.9f * torso : -0.86f * torso;
        float rot;
        if (b.quad) {
            float a = -sZ, c = sY, r = (float) Math.sqrt(a * a + c * c), phi = (float) Math.atan2(c, a);
            float want = (b.hip - b.shoulder) * u;
            rot = (float) Math.asin(Math.max(-1f, Math.min(1f, want / r))) - phi;
        } else rot = (float) Math.toRadians(-b.pitch);
        bodyRot = rot;
        shoulderAt = new float[]{(b.quad ? 0.38f : 0.44f) * width, sY, sZ};

        // neck: absolute angles run from (rise + S/2) at the base to (rise - S/2) at the head end
        int n = Math.max(1, b.neckN);
        neckLen = new float[n]; neckW = new float[n]; neckH = new float[n]; neckAbs = new float[n];
        float taper = b.plan == Body.Plan.DIPLODOCID || b.plan == Body.Plan.BRACHIOSAUR || b.plan == Body.Plan.TITANOSAUR ? 0.5f : 0.32f;
        for (int i = 0; i < n; i++) {
            float t = n == 1 ? 0.5f : (float) i / (n - 1);
            neckLen[i] = b.neck * u / n;
            neckH[i] = b.neckD * u * (1f - taper * i / n);
            neckW[i] = neckH[i] * 0.85f;
            neckAbs[i] = (float) -Math.toRadians(b.neckUp + b.neckS * (0.5f - t));
        }
        headAbs = (float) Math.toRadians(b.headDown);
        headL = b.skull * u;
        headH = b.skull * b.skullH * u;
        headW = b.skull * b.skullW * u;
        snoutL = headL * b.snout;
        craniumL = headL - snoutL;
        snoutWid = headW * b.snoutW;
        jawH = 0.3f * headH;
        snoutTop = -0.55f * headH + (1f - b.snoutH) * 0.7f * headH;
        jawW = snoutWid * 0.95f;

        // tail: what L leaves after the head, neck and body (measured along the ground)
        float neckRun = 0;
        for (int i = 0; i < n; i++) neckRun += (float) (neckLen[i] / u * Math.cos(neckAbs[i]));
        float tailM = b.tail > 0 ? b.tail : Math.max(0.15f * b.L, b.L - b.skull * 0.95f - neckRun - b.torso * 1.16f);
        int tn = Math.max(2, b.tailN);
        tailLen = new float[tn]; tailWid = new float[tn]; tailHt = new float[tn]; tailAbs = new float[tn];
        for (int i = 0; i < tn; i++) {
            float t = (float) i / (tn - 1);
            tailLen[i] = tailM * u / tn;
            tailHt[i] = b.tailD * u * (b.tailTaper + (1f - b.tailTaper) * (float) Math.pow(1f - (float) i / tn, b.tailPow));
            tailWid[i] = tailHt[i] * b.tailW;
            tailAbs[i] = (float) Math.toRadians(b.tailUp + b.tailCurve * t);
        }

        // hind legs: the three segments at their angles, scaled so the foot stands on the ground
        hipX = (b.quad ? 0.38f : 0.34f) * width;
        float a1 = (float) Math.toRadians(b.thighA), a2 = (float) Math.toRadians(b.shinA), a3 = (float) Math.toRadians(b.metaA);
        float fh = Math.max(b.footH, 0.5f / u);
        float k = (b.hip - fh) / (float) (b.thighF * Math.cos(a1) + b.shinF * Math.cos(a2) + b.metaF * Math.cos(a3));
        thighL = k * b.thighF * u; shinL = k * b.shinF * u; metaL = k * b.metaF * u;
        footH = fh * u;
        footL = Math.max(b.footL * u, b.legW * u * 0.9f);
        thighW = b.legW * u; shinW = 0.68f * thighW; metaW = 0.52f * thighW; footW = b.footW * metaW * 1.6f;
        thighAbs = -a1; shinAbs = -a2; metaAbs = -a3;

        // front legs (stand on the ground from the shoulder's height) or arms (hang free)
        if (b.quad) {
            float shoulderY = hipY + (float) (sY * Math.cos(rot) - sZ * Math.sin(rot));
            float hF = (24f - shoulderY) / u;
            float e1 = (float) Math.toRadians(b.upperA), e2 = (float) Math.toRadians(b.foreA);
            float kf = (hF - fh) / (float) (b.upperF * Math.cos(e1) + b.foreF * Math.cos(e2) + b.handF);
            upperL = kf * b.upperF * u; foreL = kf * b.foreF * u; handL = kf * b.handF * u + footH;
            upperW = b.frontW * u; foreW = 0.82f * upperW; handW = 0.78f * upperW;
            upperAbs = -e1; foreAbs = -e2; handAbs = 0;
        } else {
            float arm = b.armL * u;
            upperL = 0.42f * arm; foreL = 0.36f * arm; handL = 0.22f * arm;
            upperW = Math.max(b.armW * u, 0.6f); foreW = 0.85f * upperW; handW = 0.8f * upperW;
            boolean folded = b.wings;
            upperAbs = (float) Math.toRadians(folded ? 45 : 32);
            foreAbs = (float) Math.toRadians(folded ? -75 : -55);
            handAbs = (float) Math.toRadians(folded ? -55 : -35);
        }
    }

    // ------------------------------------------------------------------ forward kinematics (rest pose, y-z plane)

    /** A point of the rest pose in model space {y, z}, from body-local {y, z}. */
    public float[] bodyPoint(float y, float z) {
        double c = Math.cos(bodyRot), s = Math.sin(bodyRot);
        return new float[]{(float) (hipY + y * c - z * s), (float) (y * s + z * c)};
    }

    /** Where the head's pivot (the end of the neck) sits in model space {y, z}, and its absolute angle. */
    public float[] headPivot() {
        float[] p = bodyPoint(neckBase[0], neckBase[1]);
        float y = p[0], z = p[1];
        for (int i = 0; i < neckLen.length; i++) {
            // a segment runs along -z in its own frame: (0, -len) turned by its absolute angle
            y += (float) (neckLen[i] * Math.sin(neckAbs[i]));
            z += (float) (-neckLen[i] * Math.cos(neckAbs[i]));
        }
        return new float[]{y, z, neckAbs[neckLen.length - 1]};
    }

    /** The snout tip in model space {y, z}. */
    public float[] snoutTip() {
        float[] h = headPivot();
        return new float[]{h[0] + (float) (headL * Math.sin(headAbs)), h[1] - (float) (headL * Math.cos(headAbs))};
    }

    /** Top of the back above the ground, metres. */
    public float backTop() {
        float top = 1e9f;
        for (float[] box : new float[][]{hips, chest})
            for (float z : new float[]{box[0], box[1]}) top = Math.min(top, bodyPoint(box[2], z)[0]);
        return (24f - top) / U;
    }

    /** Hitbox width, metres: the middle of the body (a bit over a quarter of its length), at most 4 blocks. */
    public float width() { return Math.max(0.3f, Math.min(4f, Math.max(0.28f * b.L, b.width * 1.15f))); }

    /** Hitbox height, metres: the top of the back. */
    public float height() { return Math.max(0.3f, Math.min(8f, backTop())); }

    /** Eye height, metres: the head in its rest pose. */
    public float eye() {
        float[] h = headPivot();
        float y = (24f - (h[0] - 0.2f * headH)) / U;
        return Math.max(0.2f, Math.min(height() + 6f, y));
    }

    /** How far the snout reaches ahead of the hip joint, metres. */
    public float frontReach() { return -snoutTip()[1] / U; }

    /** How far the tail tip reaches behind the hip joint, metres. */
    public float backReach() {
        float total = 0;
        for (float l : tailLen) total += l;
        return (tailBase[1] + total) / U;
    }

    /** Highest point of the rest pose above the ground, metres (the head of a long neck, a sail, a crest). */
    public float topReach() {
        float[] h = headPivot();
        float top = Math.min(h[0] - headH, 24f - backTop() * U - b.sailH * U - b.plateH * U);
        return (24f - top) / U;
    }
}
