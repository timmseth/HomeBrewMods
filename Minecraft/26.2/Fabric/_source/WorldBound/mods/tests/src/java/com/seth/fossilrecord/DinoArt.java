package com.seth.fossilrecord;

import com.mojang.blaze3d.vertex.PoseStack;
import com.seth.fossilrecord.client.dino.DinoModel;
import com.seth.fossilrecord.client.dino.DinoRenderer;
import com.seth.fossilrecord.dino.Body;
import com.seth.fossilrecord.dino.DinoSpecies;
import com.seth.fossilrecord.dino.Rig;
import com.seth.fossilrecord.dino.Roster;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.seth.fossilrecord.FaunaArt.mix;
import static com.seth.fossilrecord.FaunaArt.shade;

/**
 * Dev tool: paints every dinosaur's texture from its model boxes and renders previews.
 * {@code DinoArt <assets/worldbound dir> <preview dir> [species...]}
 * <p>
 * Each texel is painted knowing where it sits on the animal, in the part's own frame (for eyes, nostrils, the mouth
 * line, toe claws) and in the posed model (so stripes, spots and saddles run on across body, neck, tail and legs).
 */
public final class DinoArt {
    public static void main(String[] a) throws IOException {
        File res = new File(a[0]), prev = new File(a[1]);
        new File(res, "textures/entity/dino").mkdirs();
        new File(res, "textures/item").mkdirs();
        prev.mkdirs();
        List<DinoSpecies> todo = new ArrayList<>();
        for (DinoSpecies s : Roster.ALL) if (a.length <= 2 || List.of(a).subList(2, a.length).contains(s.id)) todo.add(s);
        List<BufferedImage> shots = new ArrayList<>();
        int sheet = 1;
        for (DinoSpecies s : todo) {
            LayerDefinition layer = DinoModel.createBodyLayer(s);
            ModelPart root = layer.bakeRoot();
            int[] size = texSize(s);
            BufferedImage tex = paint(s, root, size[0], size[1]);
            ImageIO.write(tex, "png", new File(res, "textures/entity/dino/" + s.id + ".png"));
            ImageIO.write(FaunaArt.egg(s.eggBase, s.eggSpot, s.look.accent(), s.id.hashCode()), "png", new File(res, "textures/item/" + s.id + "_spawn_egg.png"));
            Rig r = s.rig();
            System.out.printf("%-18s L %5.1f m  hip %4.1f  mass %7.0f kg  tex %dx%d  U %.1f  hitbox %.2f x %.2f eye %.2f  reach %.1f/%.1f  hp %.0f dmg %.0f%n",
                    s.id, s.body.L, s.body.hip, s.mass, size[0], size[1], r.U, r.width(), r.height(), r.eye(), r.frontReach(), r.backReach(), s.health(), s.damage());
            // standing three-quarter view, and side-on mid-stride
            DinoModel m = new DinoModel(DinoModel.createBodyLayer(s).bakeRoot(), s);
            float k = 16f / r.U;
            for (int shot = 0; shot < 2; shot++) {
                DinoRenderer.State st = new DinoRenderer.State();
                st.ageInTicks = 20;
                if (shot == 1) { st.gait = Math.max(0.5f, 1.6f * s.body.hip) * 0.3f; st.gaitSpeed = s.walk / 20f * 1.2f; }
                m.setupAnim(st);
                FaunaArt.Scene sc = new FaunaArt.Scene();
                sc.model(m, tex, 0f, k, Vec3.ZERO);
                double L = s.body.L;
                // a person for scale: behind the animal in the three-quarter view, ahead of its snout and beyond it side on
                if (shot == 0) sc.player(new Vec3(-(r.width() * 0.5 + 1.0), 0, -L * 0.08));
                else sc.player(new Vec3(r.width() * 0.5 + 1.0, 0, r.frontReach() + 0.8));
                double dist = Math.max(2.6, L * 1.35 + r.topReach() * 0.6);
                Vec3 look = new Vec3(0, Math.max(0.6, r.topReach() * 0.42), (r.backReach() - r.frontReach()) * 0.5);
                shots.add(label(sc.render(560, 420, shot == 0 ? 215 : 90, shot == 0 ? 16 : 6, dist, look), s.shortName + (shot == 0 ? "" : " (walking)")));
            }
            if (shots.size() >= 12) { ImageIO.write(FaunaArt.sheet(shots, 4), "png", new File(prev, "dinos_" + sheet++ + ".png")); shots.clear(); }
            ImageIO.write(FaunaArt.scaleUp(tex, Math.max(1, 512 / Math.max(size[0], size[1]))), "png", new File(prev, "tex_" + s.id + ".png"));
        }
        if (!shots.isEmpty()) ImageIO.write(FaunaArt.sheet(shots, 4), "png", new File(prev, "dinos_" + sheet + ".png"));
        if (todo.size() > 3) lineup(todo, prev);
        System.out.println("painted " + todo.size() + " dinosaurs into " + res + ", previews in " + prev);
    }

    /** Everyone in a row, side on, with a person for scale. */
    static void lineup(List<DinoSpecies> all, File prev) throws IOException {
        List<DinoSpecies> sorted = new ArrayList<>(all);
        sorted.sort((x, y) -> Float.compare(y.body.L, x.body.L));
        int rows = 2;
        List<BufferedImage> imgs = new ArrayList<>();
        for (int row = 0; row < rows; row++) {
            FaunaArt.Scene sc = new FaunaArt.Scene();
            double x = 0, maxH = 0;
            for (int i = row; i < sorted.size(); i += rows) {
                DinoSpecies s = sorted.get(i);
                Rig r = s.rig();
                DinoModel m = new DinoModel(DinoModel.createBodyLayer(s).bakeRoot(), s);
                DinoRenderer.State st = new DinoRenderer.State();
                st.ageInTicks = 20;
                m.setupAnim(st);
                int[] ts = texSize(s);
                BufferedImage tex = paint(s, DinoModel.createBodyLayer(s).bakeRoot(), ts[0], ts[1]);
                double front = r.frontReach(), back = r.backReach();
                x += front + 0.5;
                sc.model(m, tex, 90f, 16f / r.U, new Vec3(x, 0, 0));
                x += back + 1.0;
                maxH = Math.max(maxH, r.topReach());
            }
            sc.player(new Vec3(x + 0.8, 0, 0));
            x += 1.6;
            double cx = x / 2, w = x;
            sc.box(new Vec3(cx, -0.05, 0), new double[][]{{-w / 2 - 2, 0, -3}, {w / 2 + 2, 0.05, 3}}, 0x6D9A4A);
            double t20 = Math.tan(Math.toRadians(20)), aspect = 2400 / 700.0;
            double dist = Math.max(w * 1.04 / (2 * t20 * aspect), maxH * 1.15 / (2 * t20));
            imgs.add(sc.render(2400, 700, 0, 2, dist, new Vec3(cx, maxH * 0.5, 0)));
        }
        BufferedImage out = new BufferedImage(2400, 1400, BufferedImage.TYPE_INT_RGB);
        out.getGraphics().drawImage(imgs.get(0), 0, 0, null);
        out.getGraphics().drawImage(imgs.get(1), 0, 700, null);
        ImageIO.write(out, "png", new File(prev, "lineup.png"));
    }

    static BufferedImage label(BufferedImage img, String text) {
        java.awt.Graphics2D g = img.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, 16));
        g.setColor(new java.awt.Color(0, 0, 0, 150));
        g.fillRect(0, 0, g.getFontMetrics().stringWidth(text) + 16, 24);
        g.setColor(java.awt.Color.WHITE);
        g.drawString(text, 8, 17);
        g.dispose();
        return img;
    }

    static int[] texSize(DinoSpecies s) { return DinoModel.textureSize(s); }

    // ================================================================== painting

    record Texel(String part, int index, double lx, double ly, double lz, double fyLocal, double mx, double my, double mz,
                 double nx, double ny, double nz, FaunaArt.Face face, double fx, double fy) {}

    static BufferedImage paint(DinoSpecies sp, ModelPart root, int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Random rnd = new Random(sp.id.hashCode());
        root.visit(new PoseStack(), (pose, path, index, cube) -> {
            String part = path.substring(path.lastIndexOf('/') + 1);
            Matrix4f mat = pose.pose();
            Matrix3f nm = pose.normal();
            float cy0 = cube.minY, cy1 = cube.maxY;
            for (ModelPart.Polygon poly : cube.polygons) {
                ModelPart.Vertex[] vs = poly.vertices();
                float u0 = 1, v0 = 1, u1 = 0, v1 = 0;
                for (ModelPart.Vertex v : vs) { u0 = Math.min(u0, v.u()); u1 = Math.max(u1, v.u()); v0 = Math.min(v0, v.v()); v1 = Math.max(v1, v.v()); }
                // corners by uv: 00, 10, 01, 11
                ModelPart.Vertex[] corner = new ModelPart.Vertex[4];
                for (ModelPart.Vertex v : vs) {
                    int cu = Math.abs(v.u() - u0) < Math.abs(v.u() - u1) ? 0 : 1, cv = Math.abs(v.v() - v0) < Math.abs(v.v() - v1) ? 0 : 1;
                    corner[cu + 2 * cv] = v;
                }
                if (corner[0] == null || corner[1] == null || corner[2] == null || corner[3] == null) continue;
                Vector3f[] wp = new Vector3f[4];
                for (int i = 0; i < 4; i++) wp[i] = mat.transformPosition(corner[i].worldX(), corner[i].worldY(), corner[i].worldZ(), new Vector3f());
                Vector3f n = nm.transform(new Vector3f(poly.normal()), new Vector3f()).normalize();
                org.joml.Vector3fc ln = poly.normal();
                FaunaArt.Face f = ln.y() < -0.5f ? FaunaArt.Face.TOP : ln.y() > 0.5f ? FaunaArt.Face.BOTTOM : ln.z() < -0.5f ? FaunaArt.Face.FRONT
                        : ln.z() > 0.5f ? FaunaArt.Face.BACK : ln.x() > 0.5f ? FaunaArt.Face.RIGHT : FaunaArt.Face.LEFT;
                int x0 = Math.round(u0 * w), x1 = Math.round(u1 * w), y0 = Math.round(v0 * h), y1 = Math.round(v1 * h);
                if (x1 <= x0) x1 = x0 + 1;
                if (y1 <= y0) y1 = y0 + 1;
                for (int y = y0; y < y1; y++) for (int x = x0; x < x1; x++) {
                    if (x < 0 || y < 0 || x >= w || y >= h) continue;
                    double fx = (x - x0 + 0.5) / (x1 - x0), fy = (y - y0 + 0.5) / (y1 - y0);
                    double lx = lerp2(corner, fx, fy, 0), ly = lerp2(corner, fx, fy, 1), lz = lerp2(corner, fx, fy, 2);
                    double mx = lerp2w(wp, fx, fy, 0), my = lerp2w(wp, fx, fy, 1), mz = lerp2w(wp, fx, fy, 2);
                    double fyl = cy1 > cy0 ? (ly - cy0) / (cy1 - cy0) : 0.5;
                    Texel t = new Texel(part, index, lx, ly, lz, fyl, mx, my, mz, n.x, n.y, n.z, f, fx, fy);
                    img.setRGB(x, y, 0xFF000000 | color(sp, t, rnd));
                }
            }
        });
        return img;
    }

    /** Cube-local position (model units) bilinearly across the face. */
    static double lerp2(ModelPart.Vertex[] c, double fx, double fy, int axis) {
        double a = get(c[0], axis), b = get(c[1], axis), d = get(c[2], axis), e = get(c[3], axis);
        return ((a * (1 - fx) + b * fx) * (1 - fy) + (d * (1 - fx) + e * fx) * fy) * 16;
    }

    static double get(ModelPart.Vertex v, int axis) { return axis == 0 ? v.worldX() : axis == 1 ? v.worldY() : v.worldZ(); }

    static double lerp2w(Vector3f[] c, double fx, double fy, int axis) {
        double a = c[0].get(axis), b = c[1].get(axis), d = c[2].get(axis), e = c[3].get(axis);
        return ((a * (1 - fx) + b * fx) * (1 - fy) + (d * (1 - fx) + e * fx) * fy) * 16;
    }

    static double hash(double x, double y, double z) {
        long h = Double.doubleToLongBits(x * 12.9898 + y * 78.233 + z * 37.719);
        h ^= h >>> 33; h *= 0xff51afd7ed558ccdL; h ^= h >>> 33; h *= 0xc4ceb9fe1a85ec53L; h ^= h >>> 33;
        return (h & 0xFFFFFF) / (double) 0x1000000;
    }

    /** Smooth value noise in 3D, 0..1. */
    static double noise(double x, double y, double z) {
        int xi = (int) Math.floor(x), yi = (int) Math.floor(y), zi = (int) Math.floor(z);
        double xf = x - xi, yf = y - yi, zf = z - zi;
        double u = xf * xf * (3 - 2 * xf), v = yf * yf * (3 - 2 * yf), w = zf * zf * (3 - 2 * zf);
        double r = 0;
        for (int dx = 0; dx < 2; dx++) for (int dy = 0; dy < 2; dy++) for (int dz = 0; dz < 2; dz++)
            r += hash(xi + dx, yi + dy, zi + dz) * (dx == 0 ? 1 - u : u) * (dy == 0 ? 1 - v : v) * (dz == 0 ? 1 - w : w);
        return r;
    }

    static int color(DinoSpecies sp, Texel t, Random rnd) {
        DinoSpecies.Look L = sp.look;
        Rig r = sp.rig();
        Body b = sp.body;
        String p = t.part();
        double U = r.U, Lu = b.L * U;
        double grain = 0.94 + 0.12 * hash(Math.floor(t.mx() * 1.0), Math.floor(t.my() * 1.0), Math.floor(t.mz() * 1.0));

        // ---- keratin and bone
        if (p.startsWith("horn") || p.equals("tip") || p.startsWith("spike") || p.startsWith("sspike") || p.equals("thumb") || p.startsWith("cheek")) {
            double along = 1 - t.fyLocal();                                    // 0 at the base, 1 at the tip (horns grow up -y)
            if (p.equals("tip")) along = 0.6 + 0.4 * along;
            else along *= 0.6;
            return shade(mix(shade(L.horn(), 0.8), mix(L.horn(), 0x2A2420, 0.35), along * along), grain);
        }
        if (p.startsWith("claw") || p.startsWith("sickle")) return shade(mix(L.claw(), 0x0E0C0A, t.fyLocal() * 0.6), grain);
        if (p.equals("boss")) return shade(mix(mix(L.horn(), L.mark(), 0.35), L.horn(), noise(t.mx() * 0.7, t.my() * 0.7, t.mz() * 0.7) * 0.6), grain);
        if (p.equals("club") || p.equals("knobs")) return shade(mix(mix(L.mark(), L.horn(), 0.35), L.horn(), noise(t.mx() * 0.5, t.my() * 0.5, t.mz() * 0.5) * 0.5), grain);
        if (p.equals("armour") || p.equals("studs")) {
            double hi = t.face() == FaunaArt.Face.TOP ? 0.25 : 0;
            return shade(mix(mix(L.back(), L.mark(), 0.55), L.horn(), hi + 0.25 * noise(t.mx(), t.my(), t.mz())), grain);
        }
        if (p.equals("dome")) {
            double speck = hash(Math.floor(t.mx()), Math.floor(t.my()), Math.floor(t.mz())) > 0.8 ? 0.25 : 0;
            return shade(mix(L.accent(), shade(L.accent(), 0.75), t.fyLocal() * 0.5 + speck), grain);
        }
        // ---- display structures
        if (p.equals("frill")) {
            double edge = Math.min(Math.min(t.fx(), 1 - t.fx()), t.fy()) ;
            int c = mix(L.accent(), L.base(), Math.max(0, t.fyLocal() - 0.55) * 1.8);
            double cx = Math.abs(t.lx()) / Math.max(1, r.b.frillW * U * 0.5);
            if (t.face() == FaunaArt.Face.BACK || t.face() == FaunaArt.Face.FRONT) {
                double ang = Math.atan2(t.lx(), Math.max(0.5, -t.ly() + 2)) * 5;          // bands fanning out from the neck
                if (Math.sin(ang) > 0.35 && t.fyLocal() < 0.8) c = mix(c, L.mark(), 0.45);
                if (t.fyLocal() < 0.16) c = mix(c, L.accent2(), 0.7);
                if (t.fyLocal() < 0.07 && ((int) Math.floor(t.lx() * 0.9)) % 2 == 0) c = L.horn();   // the scalloped edge
            }
            return shade(c, grain);
        }
        if (p.equals("sail") || p.equals("ridge") || p.equals("fin") || p.equals("hump")) {
            double up = 1 - t.fyLocal();
            int c = mix(mix(L.base(), L.back(), 0.4), L.accent(), Math.min(1, up * 1.4));
            if (Math.sin(t.mz() * 1.1) > 0.55 && up > 0.35) c = mix(c, L.accent2(), 0.55);
            if (up > 0.92) c = mix(c, L.mark(), 0.5);
            return shade(c, grain);
        }
        if (p.equals("plates")) {
            double up = 1 - t.fyLocal();
            int c = mix(L.accent2(), L.accent(), Math.min(1, up * 1.3));
            if (noise(t.mx() * 0.6, t.my() * 0.6, t.mz() * 0.6) > 0.68) c = mix(c, L.mark(), 0.4);   // veining
            return shade(c, grain);
        }
        if (p.startsWith("crest") || p.startsWith("spines")) {
            int c = mix(L.accent(), L.accent2(), Math.sin(t.mz() * 0.9 + t.my() * 0.5) > 0.6 ? 0.45 : 0);
            return shade(mix(c, L.base(), Math.max(0, t.fyLocal() - 0.7) * 2), grain);
        }
        if (p.equals("wing") || p.equals("fan") || p.equals("legwing")) {
            int c = mix(L.back(), L.mark(), 0.35);
            double along = p.equals("fan") ? t.lz() : t.lz();
            if (L.pattern() == DinoSpecies.Pattern.BARS || L.pattern() == DinoSpecies.Pattern.STRIPES) {
                if (Math.sin(t.lz() * 1.4 + t.ly() * 0.2) > 0.45) c = mix(L.belly(), L.mark() == L.belly() ? L.base() : 0xFFFFFF, 0.25);
            } else if (Math.sin(t.lz() * 0.9) > 0.7) c = mix(c, L.belly(), 0.45);
            if (((int) Math.floor(t.ly() * 1.5)) % 3 == 0) c = shade(c, 0.85);                  // feather edges
            double tipF = p.equals("wing") ? t.lz() / Math.max(1, b.armL * U * 0.95) : t.lz() / Math.max(1, r.tailLen[r.tailLen.length - 1] * 1.15);
            if (tipF > 0.82) c = mix(c, 0x161210, 0.4);
            return shade(c, grain);
        }

        // ---- skin (and plumage)
        double nUp = -t.ny();                                                 // model y is down
        int c;
        FaunaArt.Face f = t.face();
        double fy = t.fyLocal();
        if (nUp > 0.6) c = L.back();
        else if (nUp < -0.6) c = L.belly();
        else {
            c = fy < 0.32 ? mix(L.back(), L.base(), fy / 0.32) : fy > 0.62 ? mix(L.base(), L.belly(), Math.min(1, (fy - 0.62) / 0.3)) : L.base();
        }
        boolean upper = nUp > 0.6 || (nUp > -0.6 && fy < 0.62);
        double sp_ = Math.max(1.5, 0.055 * Lu * L.markScale());
        switch (L.pattern()) {
            case STRIPES -> {
                double s = frac(t.mz() / sp_ + 0.12 * Math.sin(t.my() / sp_ * 2.3) + 0.08 * noise(t.mx() * 0.3, t.my() * 0.3, t.mz() * 0.3));
                double width = 0.34 - (nUp > 0.6 ? 0 : fy * 0.3);
                if (upper && s < width) c = mix(c, L.mark(), 0.82);
            }
            case BANDS -> {
                double s = frac(t.mz() / (sp_ * 1.6) + 0.1 * noise(t.mx() * 0.2, t.my() * 0.2, t.mz() * 0.2));
                if ((nUp > 0.6 || fy < 0.45) && s < 0.42) c = mix(c, L.mark(), 0.5);
            }
            case SADDLES -> {
                double s = frac(t.mz() / (sp_ * 2.4));
                if ((nUp > 0.6 || fy < 0.42) && s < 0.3) c = mix(c, L.mark(), 0.75);
            }
            case TAIL_BANDS -> {
                if (p.startsWith("tail") && (nUp > 0.6 || fy < 0.75) && frac(t.mz() / (sp_ * 0.9)) < 0.45) c = mix(c, L.mark(), 0.85);
            }
            case BARS -> {
                double s = frac(t.mz() / (sp_ * 0.7));
                if (upper && s < 0.22) c = mix(c, L.mark(), 0.7);
            }
            case SPOTS -> {
                double cs = sp_ * 0.9;
                double ax = nUp > 0.6 ? Math.abs(t.mx()) : t.my(), az = t.mz();
                double gx = Math.floor(ax / cs), gz = Math.floor(az / cs);
                double ox = 0.25 + 0.5 * hash(gx, gz, 3), oz = 0.25 + 0.5 * hash(gx, gz, 7), rad = 0.18 + 0.15 * hash(gx, gz, 11);
                double dx = ax / cs - gx - ox, dz = az / cs - gz - oz;
                if (upper && dx * dx + dz * dz < rad * rad) c = mix(c, L.mark(), 0.8);
            }
            case MOTTLED -> {
                double nn = noise(t.mx() / sp_ * 1.3, t.my() / sp_ * 1.3, t.mz() / sp_ * 1.3) * 0.65 + noise(t.mx() / sp_ * 3, t.my() / sp_ * 3, t.mz() / sp_ * 3) * 0.35;
                if (nn > 0.56) c = mix(c, L.mark(), Math.min(0.75, (nn - 0.56) * 4));
            }
            default -> { }
        }
        // feathered animals: a combed, streaky look; scaly ones: fine scale texture
        if (b.feathers) c = shade(c, 0.9 + 0.16 * noise(t.mx() * 0.8, t.my() * 3.0, t.mz() * 0.8));
        else if (hash(Math.floor(t.mx() * 1.5), Math.floor(t.my() * 1.5), Math.floor(t.mz() * 1.5)) > 0.86) c = shade(c, 0.86);

        // ---- the head
        if (p.equals("head")) c = head(sp, r, t, c);
        else if (p.equals("jaw")) c = jaw(sp, r, t, c);
        // ---- hands and feet: darker, with claws at the front
        if (p.equals("foot") || p.equals("hand")) {
            c = shade(c, 0.82);
            boolean front = p.equals("foot") ? t.lz() < -r.footL * 0.78 : t.fyLocal() > 0.85;
            if (front && (f == FaunaArt.Face.FRONT || f == FaunaArt.Face.TOP || p.equals("hand"))) c = L.claw();
            if (p.equals("foot") && f == FaunaArt.Face.TOP && !front) {
                double toe = frac(t.lx() / Math.max(0.6, r.footW / 3));
                if (toe < 0.12) c = shade(c, 0.75);
            }
        }
        return shade(c, grain);
    }

    static int head(DinoSpecies sp, Rig r, Texel t, int c) {
        DinoSpecies.Look L = sp.look;
        Body b = sp.body;
        double Hh = r.headH, Lh = r.headL;
        double crTop = -0.55 * Hh, crBottom = 0.45 * Hh - r.jawH;
        boolean side = t.face() == FaunaArt.Face.LEFT || t.face() == FaunaArt.Face.RIGHT;
        boolean beaked = !sp.carnivore() || b.plan == Body.Plan.OVIRAPTOR || b.plan == Body.Plan.ORNITHOMIMID || b.plan == Body.Plan.THERIZINOSAUR;
        // the eye: on the cranium's sides, toward its front, a third of the way down
        if (side && t.index() == 0) {
            double ez = -r.craniumL * 0.62, ey = crTop + 0.33 * (crBottom - crTop);
            double er = Math.max(0.55, 0.11 * Hh * b.eyeK);
            double d = Math.hypot(t.lz() - ez, (t.ly() - ey) * 1.1);
            if (d < er * 0.5) return 0x0B0907;
            if (d < er) return d < er * 0.68 && t.lz() < ez ? mix(L.eye(), 0xFFFFFF, 0.15) : L.eye();
            if (d < er * 1.35) c = shade(c, 0.72);
        }
        // nostril near the snout tip
        if (t.index() == 1 && (side || t.face() == FaunaArt.Face.TOP) && t.lz() < -Lh * 0.86 && t.lz() > -Lh * 0.96 && t.fyLocal() < 0.35) c = shade(c, 0.45);
        // beak: the front of the snout
        if (beaked && t.index() == 1 && t.lz() < -Lh * (b.plan == Body.Plan.CERATOPSIAN || b.plan == Body.Plan.PROTOCERATOPS ? 0.8 : 0.87)) c = mix(L.horn(), 0x1E1A16, 0.25 * t.fyLocal());
        // the mouth line along the bottom edge, teeth for the hunters
        if (side && t.fyLocal() > 0.86) {
            c = shade(c, 0.6);
            if (!beaked && t.index() == 1 && ((int) Math.floor(t.lz() * 1.4)) % 2 == 0) c = 0xEDE6D2;
        }
        if (t.face() == FaunaArt.Face.BOTTOM) c = beaked ? shade(c, 0.85) : 0x9E4F4C;          // inside of the upper jaw
        if (t.face() == FaunaArt.Face.TOP) c = mix(c, L.back(), 0.4);
        return c;
    }

    static int jaw(DinoSpecies sp, Rig r, Texel t, int c) {
        DinoSpecies.Look L = sp.look;
        Body b = sp.body;
        boolean beaked = !sp.carnivore() || b.plan == Body.Plan.OVIRAPTOR || b.plan == Body.Plan.ORNITHOMIMID || b.plan == Body.Plan.THERIZINOSAUR;
        if (t.face() == FaunaArt.Face.TOP) return 0xB35A5A;                                   // inside of the mouth
        c = mix(c, L.belly(), 0.35);
        if (beaked && t.lz() < -r.headL * 0.84) return mix(L.horn(), 0x1E1A16, 0.25);
        boolean side = t.face() == FaunaArt.Face.LEFT || t.face() == FaunaArt.Face.RIGHT;
        if (side && t.fyLocal() < 0.2) {
            c = shade(c, 0.65);
            if (!beaked && ((int) Math.floor(t.lz() * 1.4)) % 2 == 1) c = 0xEDE6D2;
        }
        return c;
    }

    static double frac(double v) { return v - Math.floor(v); }
}
