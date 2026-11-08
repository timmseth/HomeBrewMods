package com.seth.worldbound.planet;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Development harness: builds planets outside Minecraft and writes PNG maps (relief, tectonics, climate) so the
 * generator can be judged by eye. {@code java ... PlanetPreview <outdir> <seed> [<seed> ...]}
 */
public final class PlanetPreview {
    public static void main(String[] a) throws IOException {
        File out = new File(a[0]);
        out.mkdirs();
        for (int s = 1; s < a.length; s++) {
            long seed = Long.parseLong(a[s]);
            Planet.KEEP_BUILD_DATA = true;
            Planet p = Planet.get(seed, System.out::println);
            Hydrology hy = p.hydro;
            System.out.printf("  spawn %.2f, %.2f; rivers %d cells, lakes %d (%d closed); max discharge %.0f m3/s%n",
                    p.spawnLat(), p.spawnLon(), hy.rivers.length, hy.lakes, hy.endorheic, hy.rivers.length > 0 ? hy.discharge[hy.rivers[0]] : 0);
            double[] mix = new double[31];
            double tot = 0;
            for (int j = 0; j < Planet.H; j++) for (int i = 0; i < Planet.W; i++) {
                int k = j * Planet.W + i;
                if (p.koppen[k] > 0) { mix[p.koppen[k]] += p.grid.areaW[j]; tot += p.grid.areaW[j]; }
            }
            StringBuilder sb = new StringBuilder("  koppen %:");
            for (int c = 1; c <= 30; c++) if (mix[c] / tot > 0.004) sb.append(String.format(" %s %.1f", ClimateCalibrate.CODE[c], 100 * mix[c] / tot));
            System.out.println(sb);
            double lakeA = 0, landA = 0;
            for (int j = 0; j < Planet.H; j++) for (int i = 0; i < Planet.W; i++) {
                int k = j * Planet.W + i;
                if (p.elev[k] > 0) { landA += p.grid.areaW[j]; if (hy.lakeLevel[k] != Hydrology.NO_LAKE) lakeA += p.grid.areaW[j]; }
            }
            System.out.printf("  lakes cover %.2f%% of land%n", 100 * lakeA / landA);
            writeRelief(p.grid, p.elev, new File(out, "relief_" + seed + ".png"), 2, p);
            String crop = System.getenv("CROP");
            if (crop != null) {
                String[] c = crop.split(",");
                int x0 = Integer.parseInt(c[0]), y0 = Integer.parseInt(c[1]), cw = Integer.parseInt(c[2]), ch = Integer.parseInt(c[3]);
                BufferedImage img = new BufferedImage(cw, ch, BufferedImage.TYPE_INT_RGB);
                for (int y = 0; y < ch; y++) for (int x = 0; x < cw; x++) {
                    int k = (y0 + y) * Planet.W + Math.floorMod(x0 + x, Planet.W);
                    int kE = (y0 + y) * Planet.W + Math.floorMod(x0 + x + 1, Planet.W), kN = (y0 + y - 1) * Planet.W + Math.floorMod(x0 + x, Planet.W);
                    double gx = (p.elev[kE] - p.elev[k]) / 9000.0, gy = (p.elev[k] - p.elev[kN]) / 9000.0;
                    double shade = Math.max(0.5, Math.min(1.4, 1 + 6 * (-gx * 0.7 + gy * 0.7)));
                    int col = tint(p.elev[k]);
                    col = (int) Math.min(255, (col >> 16 & 255) * shade) << 16 | (int) Math.min(255, (col >> 8 & 255) * shade) << 8 | (int) Math.min(255, (col & 255) * shade);
                    if (hy.lakeLevel[k] != Hydrology.NO_LAKE) col = 0x3A78C8;
                    else if (p.elev[k] > 0 && hy.discharge[k] >= 30) col = lerpRGB(col, 0x1E50B4, Math.min(1, 0.4 + Math.log10(hy.discharge[k] / 30) * 0.4));
                    img.setRGB(x, y, col);
                }
                ImageIO.write(img, "png", new File(out, "crop_" + seed + ".png"));
            }
            writeKoppen(p, new File(out, "koppen_" + seed + ".png"));
        }
    }

    static void writeKoppen(Planet p, File f) throws IOException {
        int down = 2, w = Planet.W / down, h = Planet.H / down;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int k = (y * down) * Planet.W + x * down;
                int c = p.koppen[k] > 0 ? ClimateCalibrate.KCOL[p.koppen[k]] : 0x203040;
                if (p.ice[k] != 0) c = 0xF0F4FF;
                if (p.hydro.lakeLevel[k] != Hydrology.NO_LAKE) c = 0x2060C0;
                img.setRGB(x, y, c);
            }
        }
        ImageIO.write(img, "png", f);
    }

    static int lerpRGB(int a, int b, double t) {
        t = Math.max(0, Math.min(1, t));
        int ar = a >> 16 & 255, ag = a >> 8 & 255, ab = a & 255, br = b >> 16 & 255, bg = b >> 8 & 255, bb = b & 255;
        return (int) (ar + (br - ar) * t) << 16 | (int) (ag + (bg - ag) * t) << 8 | (int) (ab + (bb - ab) * t);
    }

    static final double[] LAND_STOPS = {0, 200, 600, 1200, 2000, 3000, 4500, 6500};
    static final int[] LAND_COLS = {0x4F8A3C, 0x7FA84E, 0xC9C27A, 0xC9A267, 0xA97C55, 0x8C6E5F, 0xD8D3CF, 0xFFFFFF};
    static final double[] SEA_STOPS = {0, -150, -1000, -3000, -5000, -7000, -11000};
    static final int[] SEA_COLS = {0x9ED8F0, 0x6FB8E0, 0x3E86C2, 0x2A5E9E, 0x1E4580, 0x16305E, 0x0A1A3A};

    static int tint(double m) {
        double[] st = m >= 0 ? LAND_STOPS : SEA_STOPS;
        int[] c = m >= 0 ? LAND_COLS : SEA_COLS;
        for (int i = 1; i < st.length; i++) {
            if (m >= 0 ? m <= st[i] : m >= st[i]) return lerpRGB(c[i - 1], c[i], (m - st[i - 1]) / (st[i] - st[i - 1]));
        }
        return c[c.length - 1];
    }

    static void writeRelief(Grid G, float[] e, File f, int down) throws IOException {
        writeRelief(G, e, f, down, null);
    }

    static void writeRelief(Grid G, float[] e, File f, int down, Planet p) throws IOException {
        int w = G.w / down, h = G.h / down;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double s = 0;
                for (int b = 0; b < down; b++) for (int a2 = 0; a2 < down; a2++) s += e[(y * down + b) * G.w + x * down + a2];
                s /= down * down;
                int k = (y * down) * G.w + x * down;
                int kE = (y * down) * G.w + Math.floorMod(x * down + down, G.w), kN = Math.max(0, y * down - down) * G.w + x * down;
                double gx = (e[kE] - e[k]) / (G.kmX[y * down] * down * 1000), gy = (e[k] - e[kN]) / (G.kmY * down * 1000);
                double shade = 1 + (s >= 0 ? 18 : 4) * (-gx * 0.7 + gy * 0.7);
                shade = Math.max(0.55, Math.min(1.35, shade));
                int c = tint(s);
                if (p != null && s > 0 && p.ice[k] != 0) c = 0xEEF2F8;
                int rr = (int) Math.min(255, (c >> 16 & 255) * shade), gg = (int) Math.min(255, (c >> 8 & 255) * shade), bb = (int) Math.min(255, (c & 255) * shade);
                int col = rr << 16 | gg << 8 | bb;
                if (p != null) {
                    double q = 0;
                    boolean lake = false;
                    for (int b = 0; b < down; b++) for (int a2 = 0; a2 < down; a2++) {
                        int kk = (y * down + b) * G.w + x * down + a2;
                        q = Math.max(q, p.hydro.discharge[kk]);
                        lake |= p.hydro.lakeLevel[kk] != Hydrology.NO_LAKE;
                    }
                    if (lake) col = 0x3A78C8;
                    else if (s > 0 && q >= 150) col = lerpRGB(col, 0x1E50B4, Math.min(1, 0.35 + Math.log10(q / 150) * 0.45));
                }
                img.setRGB(x, y, col);
            }
        }
        ImageIO.write(img, "png", f);
    }

    static void writeRaw(Grid G, float[] e, File f, int down) throws IOException {
        int w = G.w / down, h = G.h / down;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double m = e[(y * down) * G.w + x * down];
                img.setRGB(x, y, tint(m));
            }
        }
        ImageIO.write(img, "png", f);
    }

    static void writeTectonics(Tectonics t, File f) throws IOException {
        Grid g = t.g;
        BufferedImage img = new BufferedImage(g.w, g.h, BufferedImage.TYPE_INT_RGB);
        int[] pal = new int[t.plates];
        for (int p = 0; p < t.plates; p++) pal[p] = java.awt.Color.HSBtoRGB(p / (float) t.plates, 0.45f, 0.95f) & 0xFFFFFF;
        for (int j = 0; j < g.h; j++) {
            for (int i = 0; i < g.w; i++) {
                int k = j * g.w + i;
                int c = pal[t.owner[k]];
                if (t.crust[k] == Tectonics.CONT) c = lerpRGB(c, 0x5A4A30, 0.55);
                else if (t.crust[k] == Tectonics.NEW_OCEAN) c = lerpRGB(c, 0x2050A0, 0.25 + 0.5 * Math.min(1, t.age[k] / 120));
                else c = lerpRGB(c, 0x102040, 0.6);
                if (t.dTrench[k] < 30 && t.arcSide[k] == 2) c = 0xFF0000;
                if (t.coll[k] > 0.5) c = lerpRGB(c, 0xFFFF00, 0.5);
                if (t.crust[k] == Tectonics.CONT && t.dP0[k] < 40) c = lerpRGB(c, 0xFFFFFF, 0.5);
                img.setRGB(i, j, c);
            }
        }
        ImageIO.write(img, "png", f);
    }
}
