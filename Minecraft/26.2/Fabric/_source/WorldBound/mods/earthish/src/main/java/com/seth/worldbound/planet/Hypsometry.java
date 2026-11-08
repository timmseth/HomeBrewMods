package com.seth.worldbound.planet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Earth's area-weighted height distribution (worldbound/planet/hypsometry.txt: 1001 quantiles of land and of sea floor,
 * ice caps left out), and the remap that gives a generated planet the same one: the raw relief decides what is
 * higher than what, Earth decides how high. Land and sea are matched separately, so the coastline is wherever the
 * chosen land fraction puts it and the shelves, abyssal plains and mountain belts get Earth's proportions.
 */
final class Hypsometry {
    final double[] land, ocean;

    private Hypsometry(double[] land, double[] ocean) {
        this.land = land;
        this.ocean = ocean;
    }

    private static Hypsometry earth;

    static synchronized Hypsometry earth() {
        if (earth != null) return earth;
        try (InputStream in = Hypsometry.class.getResourceAsStream("/worldbound/planet/hypsometry.txt")) {
            if (in == null) throw new IllegalStateException("hypsometry.txt missing from the jar");
            BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            List<double[]> rows = new ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] p = line.split("\\s+");
                double[] v = new double[p.length];
                for (int i = 0; i < p.length; i++) v[i] = Double.parseDouble(p[i]);
                rows.add(v);
            }
            earth = new Hypsometry(rows.get(0), rows.get(1));
            return earth;
        } catch (IOException e) {
            throw new IllegalStateException("hypsometry.txt: " + e, e);
        }
    }

    private static double interp(double[] q, double u) {
        double x = Math.max(0, Math.min(1, u)) * (q.length - 1);
        int i = (int) Math.floor(x);
        if (i >= q.length - 1) return q[q.length - 1];
        double f = x - i;
        return q[i] * (1 - f) + q[i + 1] * f;
    }

    /**
     * Remap {@code e} in place so an area fraction {@code landFrac} is above sea level with Earth's land
     * distribution, the rest below with Earth's sea-floor distribution. {@code mountainScale} stretches (or squashes)
     * heights above 1,000 m so planets differ in how dramatic their ranges are.
     */
    void remap(Grid g, float[] e, double landFrac, double mountainScale) {
        float lo = Float.MAX_VALUE, hi = -Float.MAX_VALUE;
        for (float v : e) { lo = Math.min(lo, v); hi = Math.max(hi, v); }
        int bins = 1 << 18;
        double[] cdf = new double[bins + 1];
        double scale = (bins - 1) / Math.max(1e-6, (double) hi - lo);
        for (int j = 0; j < g.h; j++) {
            double a = g.areaW[j];
            for (int i = 0; i < g.w; i++) cdf[(int) ((e[j * g.w + i] - lo) * scale) + 1] += a;
        }
        for (int b = 1; b <= bins; b++) cdf[b] += cdf[b - 1];
        double total = cdf[bins];
        for (int b = 0; b <= bins; b++) cdf[b] /= total;
        double sea = 1 - landFrac;
        for (int k = 0; k < e.length; k++) {
            double pos = (e[k] - lo) * scale;
            int b = (int) pos;
            double f = pos - b;
            // the cell's own bin is spread evenly, so equal raw values still get distinct quantiles
            double u = cdf[b] + (cdf[Math.min(bins, b + 1)] - cdf[b]) * f;
            double m;
            if (u >= sea) {
                m = interp(land, (u - sea) / landFrac);
                if (m > 1000) m = 1000 + (m - 1000) * mountainScale;
                m = Math.max(1, m);
            } else {
                m = Math.min(-1, interp(ocean, u / sea));
            }
            e[k] = (float) m;
        }
    }
}
