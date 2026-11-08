package com.seth.worldbound.planet;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Runs the planet climate model on the real Earth (jar base elevation, z3) and scores the Köppen map it predicts
 * against Earth's real one. {@code ClimateCalibrate <earth_elev.npy> <earth_koppen.npy> <out.png> [k=v ...]}
 */
public final class ClimateCalibrate {
    static final int[] KCOL = {0x000000,
            0x0000FF, 0x0078FF, 0x46AAFA, 0xFF0000, 0xFF9696, 0xF5A500, 0xFFDC64, 0xFFFF00, 0xC8C800, 0x969600,
            0x96FF96, 0x64C864, 0x329632, 0xC8FF50, 0x64FF50, 0x32C800, 0xFF00FF, 0xC800C8, 0x963296, 0x966496,
            0xAAAFFF, 0x5A78DC, 0x4B50B4, 0x320087, 0x00FFFF, 0x37C8FF, 0x007D7D, 0x00465F, 0xB2B2B2, 0x666666};
    static final String[] CODE = {"--", "Af", "Am", "Aw", "BWh", "BWk", "BSh", "BSk", "Csa", "Csb", "Csc", "Cwa", "Cwb", "Cwc",
            "Cfa", "Cfb", "Cfc", "Dsa", "Dsb", "Dsc", "Dsd", "Dwa", "Dwb", "Dwc", "Dwd", "Dfa", "Dfb", "Dfc", "Dfd", "ET", "EF"};

    static Object npy(File f) throws IOException {
        try (DataInputStream in = new DataInputStream(new FileInputStream(f))) {
            byte[] magic = new byte[6];
            in.readFully(magic);
            int major = in.readUnsignedByte();
            in.readUnsignedByte();
            int hl = major == 1 ? (in.readUnsignedByte() | in.readUnsignedByte() << 8)
                    : (in.readUnsignedByte() | in.readUnsignedByte() << 8 | in.readUnsignedByte() << 16 | in.readUnsignedByte() << 24);
            byte[] hdr = new byte[hl];
            in.readFully(hdr);
            String h = new String(hdr, StandardCharsets.US_ASCII);
            String shape = h.substring(h.indexOf("'shape': (") + 10);
            shape = shape.substring(0, shape.indexOf(')'));
            String[] dims = shape.split(",");
            int rows = Integer.parseInt(dims[0].trim()), cols = Integer.parseInt(dims[1].trim());
            byte[] all = in.readAllBytes();
            if (h.contains("'<i2'")) {
                short[] a = new short[rows * cols];
                ByteBuffer.wrap(all).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(a);
                return new Object[]{a, cols, rows};
            }
            return new Object[]{all, cols, rows};
        }
    }

    public static void main(String[] a) throws IOException {
        for (int q = 3; q < a.length; q++) {
            String[] kv = a[q].split("=");
            try {
                java.lang.reflect.Field fld = Climate.class.getDeclaredField(kv[0]);
                fld.setAccessible(true);
                fld.setDouble(null, Double.parseDouble(kv[1]));
            } catch (ReflectiveOperationException e) {
                throw new IllegalArgumentException(kv[0], e);
            }
        }
        Object[] ev = (Object[]) npy(new File(a[0])), kv = (Object[]) npy(new File(a[1]));
        short[] e = (short[]) ev[0];
        byte[] kp = (byte[]) kv[0];
        int W = (int) ev[1], H = (int) ev[2];
        int f = W / 1024;
        Grid g = new Grid(1024, 512);
        float[] elev = new float[g.n], landF = new float[g.n];
        int[] actual = new int[g.n];
        for (int j = 0; j < g.h; j++) {
            for (int i = 0; i < g.w; i++) {
                double s = 0;
                int nl = 0;
                int[] votes = new int[31];
                for (int b = 0; b < f; b++) {
                    for (int c = 0; c < f; c++) {
                        int k = (j * f + b) * W + i * f + c;
                        int kc = kp[k] & 255;
                        if (kc > 0 && kc <= 30) { nl++; s += Math.max(0, e[k]); votes[kc]++; }
                    }
                }
                int k = j * g.w + i;
                landF[k] = nl / (float) (f * f);
                elev[k] = nl > 0 ? (float) (s / nl) : 0;
                int best = 0;
                for (int c = 1; c <= 30; c++) if (votes[c] > votes[best]) best = c;
                actual[k] = best;
            }
        }
        String sweep = System.getenv("SWEEP");
        if (sweep != null) {
            String[] dims = sweep.split(";");
            String[][] vals = new String[dims.length][];
            String[] names = new String[dims.length];
            for (int d = 0; d < dims.length; d++) { names[d] = dims[d].split("=")[0]; vals[d] = dims[d].split("=")[1].split(","); }
            int[] idx = new int[dims.length];
            while (true) {
                StringBuilder cfg = new StringBuilder();
                for (int d = 0; d < dims.length; d++) {
                    setParam(names[d], Double.parseDouble(vals[d][idx[d]]));
                    cfg.append(names[d]).append('=').append(vals[d][idx[d]]).append(' ');
                }
                Climate c2 = new Climate(g, elev, landF);
                double[] sc = score(g, c2, actual);
                System.out.printf(Locale.ROOT, "%-60s main %.1f two %.1f full %.1f  sum %.1f%n", cfg, sc[0], sc[1], sc[2], sc[0] + sc[1]);
                int d = 0;
                while (d < dims.length && ++idx[d] >= vals[d].length) { idx[d] = 0; d++; }
                if (d == dims.length) break;
            }
            return;
        }
        long t0 = System.nanoTime();
        Climate cl = new Climate(g, elev, landF);
        System.out.printf(Locale.ROOT, "climate model: %.1f s%n", (System.nanoTime() - t0) / 1e9);
        int[] pred = new int[g.n];
        double[] aP = new double[31], aA = new double[31];
        double tot = 0, okMain = 0, okTwo = 0, okFull = 0;
        double[][] conf = new double[5][5];
        for (int j = 0; j < g.h; j++) {
            for (int i = 0; i < g.w; i++) {
                int k = j * g.w + i;
                if (!cl.land[k] || actual[k] == 0) continue;
                double t = cl.t0[k] - 0.0065 * cl.elev[k];
                int p = Climate.koppen(t, cl.range[k], cl.map[k], cl.summer[k]);
                pred[k] = p;
                double w = g.areaW[j];
                tot += w;
                aP[p] += w;
                aA[actual[k]] += w;
                int gp = group(p), ga = group(actual[k]);
                conf[ga][gp] += w;
                if (gp == ga) okMain += w;
                if (CODE[p].substring(0, 2).equals(CODE[actual[k]].substring(0, 2))) okTwo += w;
                if (p == actual[k]) okFull += w;
            }
        }
        System.out.printf(Locale.ROOT, "agreement: main group %.1f%%  two letters %.1f%%  full class %.1f%%%n", 100 * okMain / tot, 100 * okTwo / tot, 100 * okFull / tot);
        String gn = "ABCDE";
        System.out.println("confusion (rows actual, cols predicted, % of land):");
        for (int r = 0; r < 5; r++) {
            StringBuilder sb = new StringBuilder("  " + gn.charAt(r) + ":");
            for (int c = 0; c < 5; c++) sb.append(String.format(Locale.ROOT, " %5.1f", 100 * conf[r][c] / tot));
            System.out.println(sb);
        }
        StringBuilder sb = new StringBuilder("class mix (pred/actual %):");
        for (int c = 1; c <= 30; c++) if (aP[c] + aA[c] > tot * 0.003) sb.append(String.format(Locale.ROOT, " %s %.1f/%.1f", CODE[c], 100 * aP[c] / tot, 100 * aA[c] / tot));
        System.out.println(sb);
        String[][] st = {{"Manaus", "-3.1", "-60", "27.6 2300 Af"}, {"Kinshasa", "-4.3", "15.3", "25.3 1400 Aw"}, {"Timbuktu", "16.8", "-3", "28.3 200 BWh"},
                {"Lagos", "6.5", "3.4", "27 1500 Aw"}, {"Nairobi", "-1.3", "36.8", "18 870 Cwb"}, {"Paris", "48.9", "2.3", "12.3 640 Cfb"},
                {"Bergen", "60.4", "5.3", "7.6 2250 Cfb"}, {"Madrid", "40.4", "-3.7", "15 420 Csa"}, {"Rome", "41.9", "12.5", "15.5 800 Csa"},
                {"Moscow", "55.75", "37.6", "5.8 700 Dfb"}, {"Yakutsk", "62", "129.7", "-8.8 238 Dfd"}, {"Ulaanbaatar", "47.9", "106.9", "-0.4 270 Dwc"},
                {"Beijing", "39.9", "116.4", "12.9 570 Dwa"}, {"Shanghai", "31.2", "121.5", "17 1150 Cfa"}, {"Delhi", "28.6", "77.2", "25 790 Cwa"},
                {"Mumbai", "19", "72.8", "27 2200 Am"}, {"Singapore", "1.35", "103.8", "27.5 2340 Af"}, {"Chicago", "41.9", "-87.6", "10 940 Dfa"},
                {"Atlanta", "33.7", "-84.4", "17 1250 Cfa"}, {"Winnipeg", "49.9", "-97.1", "3 520 Dfb"}, {"Denver", "39.7", "-105", "10.4 400 BSk"},
                {"LosAngeles", "34", "-118.2", "18.5 380 Csb"}, {"Seattle", "47.6", "-122.3", "11.5 950 Csb"}, {"Lima", "-12", "-77", "19 15 BWh"},
                {"Brasilia", "-15.8", "-47.9", "21 1500 Aw"}, {"BuenosAires", "-34.6", "-58.4", "18 1200 Cfa"}, {"CapeTown", "-33.9", "18.4", "16.7 500 Csb"},
                {"Perth", "-31.9", "115.9", "18.5 730 Csa"}, {"Sydney", "-33.9", "151.2", "18 1200 Cfa"}, {"Alice", "-23.7", "133.9", "21 280 BWh"}};
        for (String[] row : st) {
            double la = Double.parseDouble(row[1]), lo = Double.parseDouble(row[2]);
            int i = (int) ((lo + 180) / 360 * g.w), j = (int) ((90 - la) / 180 * g.h);
            int k = j * g.w + i;
            double t = cl.t0[k] - 0.0065 * cl.elev[k];
            int p = cl.land[k] ? Climate.koppen(t, cl.range[k], cl.map[k], cl.summer[k]) : 0;
            System.out.printf(Locale.ROOT, "  %-12s T %5.1f R %4.1f P %5.0f sf %.2f c %.2f -> %-3s | map %-3s real %s%n", row[0], t, cl.range[k], cl.map[k], cl.summer[k], cl.cont[k], CODE[p], CODE[actual[k]], row[3]);
        }
        String dbg = System.getenv("TRACE");
        if (dbg != null) {
            for (String row2 : dbg.split(";")) {
                String[] q = row2.split(",");
                double la = Double.parseDouble(q[0]), lo = Double.parseDouble(q[1]);
                int sea = q[2].equals("jul") ? Climate.JUL : Climate.JAN;
                int i = (int) ((lo + 180) / 360 * g.w), j = (int) ((90 - la) / 180 * g.h);
                StringBuilder sb2 = new StringBuilder();
                cl.trace(j * g.w + i, sea, new int[Climate.STEPS + 1], new double[2], sb2);
                String[] lines = sb2.toString().split("\n");
                System.out.println("TRACE " + row2 + " (" + lines.length + " steps, last 25):");
                for (int z = Math.max(0, lines.length - 25); z < lines.length; z++) System.out.println(lines[z]);
            }
        }
        // picture: predicted (top) and actual (bottom), plus precipitation and temperature maps
        BufferedImage img = new BufferedImage(g.w, g.h * 2, BufferedImage.TYPE_INT_RGB);
        for (int j = 0; j < g.h; j++) {
            for (int i = 0; i < g.w; i++) {
                int k = j * g.w + i;
                img.setRGB(i, j, cl.land[k] ? KCOL[pred[k]] : 0x203040);
                img.setRGB(i, j + g.h, actual[k] > 0 ? KCOL[actual[k]] : 0x203040);
            }
        }
        ImageIO.write(img, "png", new File(a[2]));
        BufferedImage pi = new BufferedImage(g.w, g.h * 2, BufferedImage.TYPE_INT_RGB);
        for (int j = 0; j < g.h; j++) {
            for (int i = 0; i < g.w; i++) {
                int k = j * g.w + i;
                double mm = cl.map[k];
                int c = cl.land[k] ? PlanetPreview.lerpRGB(0xC8A060, 0x105020, Math.log1p(mm / 100) / Math.log1p(40)) : 0x203040;
                pi.setRGB(i, j, c);
                double tt = cl.t0[k] - (cl.land[k] ? 0.0065 * cl.elev[k] : 0);
                int ct = PlanetPreview.lerpRGB(0x3050FF, 0xFF3020, (tt + 30) / 60);
                pi.setRGB(i, j + g.h, cl.land[k] ? ct : PlanetPreview.lerpRGB(0x000000, ct, 0.5));
            }
        }
        // wind arrows (July) on the precipitation map
        for (int j = 8; j < g.h; j += 16) {
            for (int i = 8; i < g.w; i += 16) {
                int k = j * g.w + i;
                for (int sx = 0; sx < 9; sx++) {
                    int x = (int) Math.round(i + cl.windU[Climate.JUL][k] * sx), y = (int) Math.round(j - cl.windV[Climate.JUL][k] * sx);
                    if (x >= 0 && x < g.w && y >= 0 && y < g.h) pi.setRGB(x, y, sx > 6 ? 0xFFFFFF : 0x808080);
                }
            }
        }
        ImageIO.write(pi, "png", new File(a[2].replace(".png", "_pt.png")));
        for (int sea = 0; sea < 2; sea++) writeWind(cl, sea, new File(a[2].replace(".png", sea == 0 ? "_wjan.png" : "_wjul.png")));
    }

    static void writeWind(Climate cl, int s, File f) throws IOException {
        Grid g = cl.g;
        int sc = 2;
        BufferedImage img = new BufferedImage(g.w * sc, g.h * sc, BufferedImage.TYPE_INT_RGB);
        float[] p = cl.press[s];
        for (int y = 0; y < g.h * sc; y++) {
            for (int x = 0; x < g.w * sc; x++) {
                int k = (y / sc) * g.w + x / sc;
                double pv = p[k];
                int c = pv < 0 ? PlanetPreview.lerpRGB(0xE0E0E0, 0x2040FF, -pv / 12) : PlanetPreview.lerpRGB(0xE0E0E0, 0xFF3020, pv / 12);
                if (cl.land[k]) c = PlanetPreview.lerpRGB(c, 0x000000, 0.35);
                double mm = cl.precip[s][k] * Climate.P_SCALE;
                if (cl.land[k] && mm > 60) c = PlanetPreview.lerpRGB(c, 0x00A000, Math.min(1, (mm - 60) / 200));
                img.setRGB(x, y, c);
            }
        }
        java.awt.Graphics2D gr = img.createGraphics();
        gr.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        gr.setColor(java.awt.Color.BLACK);
        for (int j = 6; j < g.h; j += 12) {
            for (int i = 6; i < g.w; i += 12) {
                int k = j * g.w + i;
                double u = cl.windU[s][k], v = cl.windV[s][k];
                int x0 = i * sc, y0 = j * sc, x1 = (int) (x0 + u * 18), y1 = (int) (y0 - v * 18);
                gr.drawLine(x0, y0, x1, y1);
                gr.fillOval(x1 - 2, y1 - 2, 4, 4);
            }
        }
        gr.dispose();
        ImageIO.write(img, "png", f);
    }

    static void setParam(String name, double v) {
        try {
            java.lang.reflect.Field fld = Climate.class.getDeclaredField(name);
            fld.setAccessible(true);
            fld.setDouble(null, v);
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException(name, e);
        }
    }

    static double[] score(Grid g, Climate cl, int[] actual) {
        double tot = 0, okMain = 0, okTwo = 0, okFull = 0;
        for (int j = 0; j < g.h; j++) {
            for (int i = 0; i < g.w; i++) {
                int k = j * g.w + i;
                if (!cl.land[k] || actual[k] == 0) continue;
                int p = Climate.koppen(cl.t0[k] - 0.0065 * cl.elev[k], cl.range[k], cl.map[k], cl.summer[k]);
                double w = g.areaW[j];
                tot += w;
                if (group(p) == group(actual[k])) okMain += w;
                if (CODE[p].substring(0, 2).equals(CODE[actual[k]].substring(0, 2))) okTwo += w;
                if (p == actual[k]) okFull += w;
            }
        }
        return new double[]{100 * okMain / tot, 100 * okTwo / tot, 100 * okFull / tot};
    }

    static int group(int k) {
        return k <= 3 ? 0 : k <= 7 ? 1 : k <= 16 ? 2 : k <= 28 ? 3 : 4;
    }
}
