package com.seth.fossilrecord;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.seth.fossilrecord.client.AnacondaModel;
import com.seth.fossilrecord.client.AnacondaRenderer;
import com.seth.fossilrecord.client.KangarooModel;
import com.seth.fossilrecord.client.KangarooRenderer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Dev tool (not in the jar): paints the animals' textures straight from their model boxes (so the paint always lines up
 * with the UVs), the spawn eggs, and software-renders posed previews to check proportions and animation without a
 * game client. {@code FaunaArt <resources assets/worldbound dir> <preview dir>}
 */
public final class FaunaArt {
    public static void main(String[] a) throws IOException {
        File res = new File(a[0]), prev = new File(a[1]);
        new File(res, "textures/entity/fauna").mkdirs();
        new File(res, "textures/item").mkdirs();
        prev.mkdirs();
        ModelPart kRoot = KangarooModel.createBodyLayer().bakeRoot();
        BufferedImage red = paint(kRoot, 64, 64, FaunaArt::kangarooRed), grey = paint(kRoot, 64, 64, FaunaArt::kangarooGrey);
        ImageIO.write(red, "png", new File(res, "textures/entity/fauna/kangaroo_red.png"));
        ImageIO.write(grey, "png", new File(res, "textures/entity/fauna/kangaroo_grey.png"));
        ModelPart sRoot = AnacondaModel.createBodyLayer().bakeRoot();
        BufferedImage snake = paint(sRoot, 64, 64, FaunaArt::anaconda);
        ImageIO.write(snake, "png", new File(res, "textures/entity/fauna/anaconda.png"));
        ImageIO.write(egg(0xB5602F, 0xEADCC4, 0x6B3A1E, 11), "png", new File(res, "textures/item/kangaroo_spawn_egg.png"));
        ImageIO.write(egg(0x5A6A2A, 0x15140F, 0xC9B45A, 23), "png", new File(res, "textures/item/anaconda_spawn_egg.png"));

        // kangaroo poses
        KangarooModel km = new KangarooModel(KangarooModel.createBodyLayer().bakeRoot());
        List<BufferedImage> shots = new ArrayList<>();
        float[][] poses = {{0, 0, 0, 0}, {2.86f, 1f, 0, 0}, {5.7f, 1f, 0, 0}, {0, 0, 0.5f, 0}, {0, 0, 0, 1f}};
        String[] names = {"standing", "bounding (up)", "bounding (down)", "kicking", "grazing"};
        for (int i = 0; i < poses.length; i++) {
            KangarooRenderer.State s = new KangarooRenderer.State();
            s.walkAnimationPos = poses[i][0]; s.walkAnimationSpeed = poses[i][1]; s.kick = poses[i][2]; s.eat = poses[i][3];
            s.bodyRot = 0; s.ageInTicks = 10; s.male = true; s.variant = Habitat.RED;
            km.setupAnim(s);
            Scene sc = new Scene();
            sc.model(km, red, 0f, 1.15f, new Vec3(0, 0, 0));
            sc.player(new Vec3(1.6, 0, -0.8));
            shots.add(sc.render(560, 420, i == 0 ? 215 : 90, i == 0 ? 15 : 8, 5.5, new Vec3(0, 0.85, 0.3)));
            System.out.println("kangaroo " + names[i] + ": height " + String.format("%.2f", sc.maxY) + " blocks, lowest " + String.format("%.2f", sc.minY));
        }
        // the grey one, side on
        {
            KangarooRenderer.State s = new KangarooRenderer.State();
            s.bodyRot = 0; s.ageInTicks = 30; s.variant = Habitat.GREY;
            km.setupAnim(s);
            Scene sc = new Scene();
            sc.model(km, grey, 0f, 1f, Vec3.ZERO);
            shots.add(sc.render(560, 420, 90, 8, 5, new Vec3(0, 0.75, 0)));
        }
        ImageIO.write(sheet(shots, 3), "png", new File(prev, "kangaroo.png"));

        // anaconda shapes
        AnacondaModel am = new AnacondaModel(AnacondaModel.createBodyLayer().bakeRoot());
        shots.clear();
        float len = 7f;
        for (int shape = 0; shape < 3; shape++) {
            AnacondaBody b = new AnacondaBody();
            Vec3 at = Vec3.ZERO;
            float yaw = 0;
            AABB held = null;
            if (shape == 0) for (int t = 0; t < 120; t++) b.update(at, yaw, len, null, false);
            else if (shape == 1) {
                for (int t = 0; t < 160; t++) {
                    double x = t * 0.06, z = Math.sin(t * 0.06) * 1.2;
                    double nx = (t + 1) * 0.06, nz = Math.sin((t + 1) * 0.06) * 1.2;
                    at = new Vec3(x, 0, z);
                    yaw = (float) Math.toDegrees(Math.atan2(-(nx - x), nz - z));
                    b.update(at, yaw, len, null, true);
                }
            } else {
                held = new AABB(0.9, 0, -0.3, 1.5, 1.8, 0.3);
                at = new Vec3(0.2, 0, 0);
                for (int t = 0; t < 60; t++) b.update(at, yaw, len, held, false);
            }
            AnacondaRenderer.State s = new AnacondaRenderer.State();
            s.bodyRot = yaw;
            AnacondaRenderer.layout(b.x, b.y, b.z, at.x, at.y, at.z, yaw, len, s);
            s.jaw = shape == 2 ? 1 : 0;
            am.setupAnim(s);
            Scene sc = new Scene();
            sc.model(am, snake, yaw, 1f, at);
            if (held != null) sc.player(new Vec3(1.2, 0, 0));
            else sc.player(new Vec3(-2.5, 0, 2.5));
            Vec3 look = shape == 1 ? new Vec3(at.x - 3, 0.3, at.z) : new Vec3(0.3, 0.4, 0);
            shots.add(sc.render(560, 420, 30, shape == 0 ? 40 : 25, shape == 1 ? 11 : 7, look));
            if (shape == 1) shots.add(sc.render(560, 420, 120, 30, 3.2, new Vec3(at.x, 0.2, at.z)));   // the head, close up
        }
        ImageIO.write(sheet(shots, 3), "png", new File(prev, "anaconda.png"));
        ImageIO.write(scaleUp(red, 6), "png", new File(prev, "kangaroo_red_tex.png"));
        ImageIO.write(scaleUp(snake, 6), "png", new File(prev, "anaconda_tex.png"));
        System.out.println("wrote textures to " + res + " and previews to " + prev);
    }

    // ------------------------------------------------------------------ painting

    enum Face { TOP, BOTTOM, FRONT, BACK, LEFT, RIGHT }

    interface Painter { int color(String part, Face face, double fx, double fy, int px, int py, Random rnd); }

    /** Paints every face of every box of a baked model into its UV rectangle. */
    static BufferedImage paint(ModelPart root, int w, int h, Painter painter) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Random rnd = new Random(5);
        root.visit(new PoseStack(), (pose, path, index, cube) -> {
            String part = path.substring(path.lastIndexOf('/') + 1) + (index > 0 ? "#" + index : "");
            for (ModelPart.Polygon poly : cube.polygons) {
                float u0 = 1, v0 = 1, u1 = 0, v1 = 0;
                for (ModelPart.Vertex v : poly.vertices()) { u0 = Math.min(u0, v.u()); u1 = Math.max(u1, v.u()); v0 = Math.min(v0, v.v()); v1 = Math.max(v1, v.v()); }
                int x0 = Math.round(u0 * w), x1 = Math.round(u1 * w), y0 = Math.round(v0 * h), y1 = Math.round(v1 * h);
                Vector3fc n = poly.normal();
                Face f = n.y() < -0.5f ? Face.TOP : n.y() > 0.5f ? Face.BOTTOM : n.z() < -0.5f ? Face.FRONT : n.z() > 0.5f ? Face.BACK
                        : n.x() > 0.5f ? Face.RIGHT : Face.LEFT;
                for (int y = y0; y < y1; y++) for (int x = x0; x < x1; x++) {
                    if (x < 0 || y < 0 || x >= w || y >= h) continue;
                    double fx = (x - x0 + 0.5) / Math.max(1, x1 - x0), fy = (y - y0 + 0.5) / Math.max(1, y1 - y0);
                    img.setRGB(x, y, 0xFF000000 | painter.color(part, f, fx, fy, x, y, rnd));
                }
            }
        });
        return img;
    }

    static int mix(int a, int b, double t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int) ((a >> 16 & 255) * (1 - t) + (b >> 16 & 255) * t), g = (int) ((a >> 8 & 255) * (1 - t) + (b >> 8 & 255) * t), bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return r << 16 | g << 8 | bl;
    }

    static int shade(int c, double k) {
        int r = (int) Math.min(255, (c >> 16 & 255) * k), g = (int) Math.min(255, (c >> 8 & 255) * k), b = (int) Math.min(255, (c & 255) * k);
        return r << 16 | g << 8 | b;
    }

    /** Fur: a little per-pixel brightness noise. */
    static int fur(int c, Random rnd) { return shade(c, 0.93 + rnd.nextDouble() * 0.12); }

    static int kangarooRed(String part, Face f, double fx, double fy, int px, int py, Random rnd) {
        return kangaroo(part, f, fx, fy, rnd, 0xB2562F, 0x9A4526, 0xEADCC4, 0xC48B67, 0xE6B3A0, 0x2A1C16, 0x5A3A26);
    }

    static int kangarooGrey(String part, Face f, double fx, double fy, int px, int py, Random rnd) {
        return kangaroo(part, f, fx, fy, rnd, 0x8D8378, 0x766C62, 0xE4DDD1, 0xA39A8F, 0xD5BDB2, 0x27221F, 0x4E4640);
    }

    static int kangaroo(String part, Face f, double fx, double fy, Random rnd, int base, int back, int belly, int face, int earIn, int dark, int paw) {
        String p = part.replace("#1", "");
        int c = base;
        switch (p) {
            case "body" -> {
                if (f == Face.FRONT) c = mix(belly, base, Math.max(Math.abs(fx - 0.5) * 2 - 0.45, 0) * 1.6);
                else if (f == Face.BACK || f == Face.TOP) c = back;
                else if (f == Face.BOTTOM) c = belly;
                else c = mix(base, mix(base, belly, 0.35), fy);              // sides lighten toward the belly
            }
            case "chest" -> c = f == Face.FRONT ? belly : f == Face.BOTTOM ? belly : f == Face.TOP ? back : mix(base, face, 0.2);
            case "head" -> {
                if (part.endsWith("#1")) {                                   // the snout
                    c = f == Face.FRONT ? (fy < 0.45 && Math.abs(fx - 0.5) < 0.34 ? dark : face) : f == Face.BOTTOM ? belly : face;
                    if (f == Face.TOP) c = mix(face, base, 0.5);
                } else {
                    c = f == Face.FRONT ? face : f == Face.BOTTOM ? belly : f == Face.TOP ? back : base;
                    // the eye: on each side, near the front, a third of the way down
                    if ((f == Face.LEFT || f == Face.RIGHT)) {
                        double ex = f == Face.LEFT ? 0.25 : 0.75;                 // the front edge of each side face
                        if (Math.abs(fx - ex) < 0.12 && Math.abs(fy - 0.38) < 0.15) return 0x120C0A;
                        if (Math.abs(fx - ex) < 0.22 && Math.abs(fy - 0.62) < 0.1) c = mix(c, belly, 0.45);   // pale cheek line
                    }
                }
            }
            case "ear_l", "ear_r" -> c = f == Face.FRONT ? earIn : fy < 0.22 ? mix(base, dark, 0.5) : base;
            case "arm_l", "arm_r" -> c = f == Face.BOTTOM ? paw : fy > 0.8 ? mix(belly, paw, 0.6) : mix(belly, base, 0.3);
            case "leg_l", "leg_r" -> c = f == Face.FRONT ? mix(base, belly, 0.45) : f == Face.BOTTOM ? belly : mix(base, back, fy * 0.5);
            case "shin" -> c = mix(base, paw, 0.35 + fy * 0.3);
            case "foot" -> c = f == Face.TOP ? mix(base, paw, 0.3) : f == Face.FRONT || f == Face.BOTTOM ? paw : mix(base, paw, 0.5 + fy * 0.3);
            case "tail" -> c = f == Face.BOTTOM ? mix(base, belly, 0.4) : f == Face.TOP ? back : base;
            case "tail2" -> {
                double along = f == Face.TOP || f == Face.BOTTOM ? fy : f == Face.BACK ? 1 : f == Face.FRONT ? 0 : (f == Face.LEFT ? 1 - fx : fx);
                c = mix(f == Face.BOTTOM ? mix(base, belly, 0.3) : base, dark, Math.max(0, along - 0.55) * 1.6);
            }
            default -> c = base;
        }
        return fur(c, rnd);
    }

    static int anaconda(String part, Face f, double fx, double fy, int px, int py, Random rnd) {
        int olive = 0x59662B, dorsal = 0x4A5524, belly = 0xCDB65A, black = 0x16150F, ochre = 0xD9962A;
        String p = part.replace("#1", "");
        int c = olive;
        if (p.equals("head")) {
            if (part.endsWith("#1")) {                                          // snout: nostrils on top at the front
                c = f == Face.BOTTOM ? belly : olive;
                if (f == Face.TOP && fy < 0.35 && (Math.abs(fx - 0.25) < 0.12 || Math.abs(fx - 0.75) < 0.12)) c = black;
            } else {
                c = f == Face.BOTTOM ? belly : f == Face.TOP ? dorsal : olive;
                if (f == Face.TOP) {
                    // eyes high on the head, and the dark stripes running back from them
                    if (fy < 0.3 && (Math.abs(fx - 0.15) < 0.12 || Math.abs(fx - 0.85) < 0.12)) return 0xE0B040;
                    if (fy >= 0.3 && (Math.abs(fx - 0.15) < 0.1 || Math.abs(fx - 0.85) < 0.1)) c = black;
                }
                if ((f == Face.LEFT || f == Face.RIGHT) && Math.abs(fy - 0.35) < 0.14) c = mix(ochre, black, Math.abs(fy - 0.35) * 5);   // the orange stripe behind the eye
            }
        } else if (p.equals("jaw")) {
            c = f == Face.TOP ? 0xB86A70 : belly;                              // the inside of the mouth shows when it opens
        } else if (p.startsWith("seg")) {
            int variant = (px / 16) % 4;
            if (f == Face.TOP) {
                c = dorsal;
                double cx = 0.5 + 0.18 * Math.sin(variant * 1.7), cy = 0.25 + 0.5 * ((variant * 3) % 4) / 3.0;
                double dx = (fx - cx) / 0.32, dy = (fy - cy) / 0.24;
                if (dx * dx + dy * dy < 1) c = black;                            // the big dorsal blotches
                double dx2 = (fx - (1 - cx)) / 0.28, dy2 = (fy - ((cy + 0.55) % 1)) / 0.2;
                if (variant % 2 == 0 && dx2 * dx2 + dy2 * dy2 < 1) c = black;
            } else if (f == Face.LEFT || f == Face.RIGHT) {
                c = mix(olive, mix(olive, belly, 0.6), Math.max(0, fy - 0.45) * 1.8);
                double cx = 0.3 + 0.4 * ((variant + (f == Face.LEFT ? 1 : 0)) % 3) / 2.0, cy = 0.42;
                double r = Math.hypot((fx - cx) / 0.3, (fy - cy) / 0.3);
                if (r < 1) c = r < 0.38 ? ochre : black;                         // the side spots: black rings round an orange eye
            } else if (f == Face.BOTTOM) {
                c = belly;
                if ((Math.sin(fx * 7 + variant * 2) + Math.cos(fy * 9 + variant)) > 1.25) c = black;
            } else c = olive;
        }
        return shade(c, 0.94 + rnd.nextDouble() * 0.1);
    }

    /** A spawn egg in the mob's colours, in the vanilla style (16x16). */
    static BufferedImage egg(int base, int spot, int accent, long seed) {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Random r = new Random(seed);
        double cx = 7.5, cy = 8.6;
        List<double[]> spots = new ArrayList<>();
        for (int i = 0; i < 5; i++) spots.add(new double[]{3 + r.nextDouble() * 10, 3 + r.nextDouble() * 11, 1.0 + r.nextDouble() * 1.3});
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
            double dx = (x - cx) / 5.6, dy = (y - cy) / (y < cy ? 7.3 : 6.4);
            double d = dx * dx + dy * dy;
            if (d > 1) continue;
            int c = base;
            for (double[] s : spots) if (Math.hypot(x - s[0], y - s[1]) < s[2]) c = spot;
            if (d > 0.72) c = shade(c, 0.62);                                      // the outline
            else c = shade(c, 1.1 - 0.3 * (dx + 0.5) * 0.5 - 0.2 * Math.max(0, dy));
            if (x == 5 && y == 4 || x == 5 && y == 5 || x == 6 && y == 4) c = mix(c, 0xFFFFFF, 0.6);   // a highlight
            if (accent != 0 && d < 0.72 && d > 0.6) c = mix(c, accent, 0.35);
            img.setRGB(x, y, 0xFF000000 | c);
        }
        return img;
    }

    // ------------------------------------------------------------------ previews (a small software rasteriser)

    static final class Tri { final double[] x = new double[3], y = new double[3], z = new double[3], u = new double[3], v = new double[3]; BufferedImage tex; double light; int flat; }

    static final class Scene {
        final List<Tri> tris = new ArrayList<>();
        double minY = 1e9, maxY = -1e9;

        void model(EntityModel<?> m, BufferedImage tex, float bodyRot, float scale, Vec3 at) {
            PoseStack ps = new PoseStack();
            ps.translate(at.x, at.y, at.z);
            ps.mulPose(Axis.YP.rotationDegrees(180f - bodyRot));
            ps.scale(-1f, -1f, 1f);
            ps.scale(scale, scale, scale);
            ps.translate(0f, -1.501f, 0f);
            m.root().visit(ps, (pose, path, index, cube) -> {
                Matrix4f mat = pose.pose();
                Matrix3f nm = pose.normal();
                for (ModelPart.Polygon poly : cube.polygons) {
                    ModelPart.Vertex[] vs = poly.vertices();
                    Vector3f[] w = new Vector3f[4];
                    for (int i = 0; i < 4; i++) {
                        w[i] = mat.transformPosition(vs[i].worldX(), vs[i].worldY(), vs[i].worldZ(), new Vector3f());
                        minY = Math.min(minY, w[i].y - at.y); maxY = Math.max(maxY, w[i].y - at.y);
                    }
                    Vector3f n = nm.transform(new Vector3f(poly.normal()), new Vector3f()).normalize();
                    double light = 0.55 + 0.45 * Math.max(0, n.dot(new Vector3f(-0.4f, 0.8f, -0.45f).normalize()));
                    for (int[] t : new int[][]{{0, 1, 2}, {0, 2, 3}}) {
                        Tri tri = new Tri();
                        for (int k = 0; k < 3; k++) {
                            tri.x[k] = w[t[k]].x; tri.y[k] = w[t[k]].y; tri.z[k] = w[t[k]].z;
                            tri.u[k] = vs[t[k]].u(); tri.v[k] = vs[t[k]].v();
                        }
                        tri.tex = tex; tri.light = light;
                        tris.add(tri);
                    }
                }
            });
        }

        /** A player-sized box for scale. */
        void player(Vec3 at) {
            double[][] c = {{-0.3, 0, -0.3}, {0.3, 1.8, 0.3}};
            box(at, c, 0x3C6EC8);
        }

        void box(Vec3 at, double[][] c, int col) {
            double x0 = at.x + c[0][0], y0 = at.y + c[0][1], z0 = at.z + c[0][2], x1 = at.x + c[1][0], y1 = at.y + c[1][1], z1 = at.z + c[1][2];
            double[][][] faces = {
                    {{x0, y0, z0}, {x1, y0, z0}, {x1, y1, z0}, {x0, y1, z0}}, {{x0, y0, z1}, {x1, y0, z1}, {x1, y1, z1}, {x0, y1, z1}},
                    {{x0, y0, z0}, {x0, y0, z1}, {x0, y1, z1}, {x0, y1, z0}}, {{x1, y0, z0}, {x1, y0, z1}, {x1, y1, z1}, {x1, y1, z0}},
                    {{x0, y1, z0}, {x1, y1, z0}, {x1, y1, z1}, {x0, y1, z1}}};
            double[] lights = {0.7, 0.8, 0.75, 0.85, 1.0};
            for (int f = 0; f < faces.length; f++) for (int[] t : new int[][]{{0, 1, 2}, {0, 2, 3}}) {
                Tri tri = new Tri();
                for (int k = 0; k < 3; k++) { tri.x[k] = faces[f][t[k]][0]; tri.y[k] = faces[f][t[k]][1]; tri.z[k] = faces[f][t[k]][2]; }
                tri.flat = col; tri.light = lights[f];
                tris.add(tri);
            }
        }

        BufferedImage render(int W, int H, double yawDeg, double pitchDeg, double dist, Vec3 target) {
            BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
            double[] zb = new double[W * H];
            java.util.Arrays.fill(zb, Double.MAX_VALUE);
            double yaw = Math.toRadians(yawDeg), pitch = Math.toRadians(pitchDeg);
            // camera looks at target from yaw (0 = from -z, i.e. at the animal's face) and pitch above
            Vec3 eye = target.add(-Math.sin(yaw) * Math.cos(pitch) * dist, Math.sin(pitch) * dist, -Math.cos(yaw) * Math.cos(pitch) * dist);
            Vec3 fwd = target.subtract(eye).normalize(), right = fwd.cross(new Vec3(0, 1, 0)).normalize(), up = right.cross(fwd);
            double f = H / (2 * Math.tan(Math.toRadians(20)));
            // sky and a ground grid
            for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) img.setRGB(x, y, mix(0xA9C7E8, 0xE6EEF5, (double) y / H));
            for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) {
                Vec3 ray = fwd.add(right.scale((x - W / 2.0) / f)).add(up.scale(-(y - H / 2.0) / f));
                if (ray.y >= -1e-6) continue;
                double t = -eye.y / ray.y;
                Vec3 p = eye.add(ray.scale(t));
                if (Math.abs(p.x - target.x) > 8 || Math.abs(p.z - target.z) > 8) continue;
                boolean chk = ((int) Math.floor(p.x) + (int) Math.floor(p.z) & 1) == 0;
                img.setRGB(x, y, chk ? 0x6D9A4A : 0x7AA653);
                zb[y * W + x] = t * fwd.dot(ray) + 1e-3;
            }
            for (Tri tr : tris) {
                double[] sx = new double[3], sy = new double[3], sz = new double[3];
                boolean behind = false;
                for (int k = 0; k < 3; k++) {
                    Vec3 d = new Vec3(tr.x[k], tr.y[k], tr.z[k]).subtract(eye);
                    double zc = d.dot(fwd);
                    if (zc < 0.05) { behind = true; break; }
                    sx[k] = W / 2.0 + d.dot(right) / zc * f; sy[k] = H / 2.0 - d.dot(up) / zc * f; sz[k] = zc;
                }
                if (behind) continue;
                int x0 = (int) Math.max(0, Math.floor(Math.min(sx[0], Math.min(sx[1], sx[2])))), x1 = (int) Math.min(W - 1, Math.ceil(Math.max(sx[0], Math.max(sx[1], sx[2]))));
                int y0 = (int) Math.max(0, Math.floor(Math.min(sy[0], Math.min(sy[1], sy[2])))), y1 = (int) Math.min(H - 1, Math.ceil(Math.max(sy[0], Math.max(sy[1], sy[2]))));
                double area = (sx[1] - sx[0]) * (sy[2] - sy[0]) - (sx[2] - sx[0]) * (sy[1] - sy[0]);
                if (Math.abs(area) < 1e-9) continue;
                for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) {
                    double px = x + 0.5, py = y + 0.5;
                    double w0 = ((sx[1] - px) * (sy[2] - py) - (sx[2] - px) * (sy[1] - py)) / area;
                    double w1 = ((sx[2] - px) * (sy[0] - py) - (sx[0] - px) * (sy[2] - py)) / area;
                    double w2 = 1 - w0 - w1;
                    if (w0 < -1e-6 || w1 < -1e-6 || w2 < -1e-6) continue;
                    double iz = w0 / sz[0] + w1 / sz[1] + w2 / sz[2], z = 1 / iz;
                    if (z >= zb[y * W + x]) continue;
                    int col;
                    if (tr.tex != null) {
                        double u = (w0 * tr.u[0] / sz[0] + w1 * tr.u[1] / sz[1] + w2 * tr.u[2] / sz[2]) * z;
                        double v = (w0 * tr.v[0] / sz[0] + w1 * tr.v[1] / sz[1] + w2 * tr.v[2] / sz[2]) * z;
                        int tx = Math.min(tr.tex.getWidth() - 1, Math.max(0, (int) (u * tr.tex.getWidth()))), ty = Math.min(tr.tex.getHeight() - 1, Math.max(0, (int) (v * tr.tex.getHeight())));
                        int t = tr.tex.getRGB(tx, ty);
                        if ((t >>> 24) < 128) continue;
                        col = shade(t & 0xFFFFFF, tr.light);
                    } else col = shade(tr.flat, tr.light);
                    zb[y * W + x] = z;
                    img.setRGB(x, y, col);
                }
            }
            return img;
        }
    }

    static BufferedImage sheet(List<BufferedImage> imgs, int cols) {
        int w = imgs.get(0).getWidth(), h = imgs.get(0).getHeight(), rows = (imgs.size() + cols - 1) / cols;
        BufferedImage out = new BufferedImage(w * cols, h * rows, BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < imgs.size(); i++) out.getGraphics().drawImage(imgs.get(i), (i % cols) * w, (i / cols) * h, null);
        return out;
    }

    static BufferedImage scaleUp(BufferedImage in, int k) {
        BufferedImage out = new BufferedImage(in.getWidth() * k, in.getHeight() * k, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < out.getHeight(); y++) for (int x = 0; x < out.getWidth(); x++) out.setRGB(x, y, in.getRGB(x / k, y / k));
        return out;
    }
}
