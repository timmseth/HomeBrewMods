package com.seth.worldbound.planet;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * River erosion on a half-resolution copy of the relief (~20 km cells), added back to the full grid as a difference.
 * Noise makes hills, not landscapes: real ground is shaped by the water running off it, into branching valleys that
 * all lead somewhere. This runs the stream-power law (incision ~ drainage area^0.5 x slope) with the implicit scheme
 * of Braun & Willett (2013), which is stable at any step, for a handful of rounds: big rivers cut down toward sea level
 * and carve valleys back into the ranges, headwaters barely change, and most closed hollows get drained.
 */
final class Erosion {
    private Erosion() {}

    /**
     * A few metres of fractal wobble added to the surface water is routed over (never to the ground itself), so flow
     * across gentle plains wanders like real rivers instead of running in the straight lines of an 8-direction grid.
     */
    static float[] routingNoise(Grid g, long seed, double amp) {
        Noise3 nz = new Noise3(seed ^ 0x5017EEDL);
        float[] r = new float[g.n];
        IntStream.range(0, g.h).parallel().forEach(j -> {
            for (int i = 0; i < g.w; i++) {
                int k = j * g.w + i;
                r[k] = (float) (amp * nz.fbm(g.vx[k], g.vy[k], g.vz[k], 120, 4, 0.6));
            }
        });
        return r;
    }

    static void apply(Grid full, float[] e, int rounds, double strength, long seed) {
        int w = full.w / 2, h = full.h / 2;
        Grid g = new Grid(w, h);
        float[] base = new float[g.n];
        IntStream.range(0, h).parallel().forEach(j -> {
            for (int i = 0; i < w; i++) {
                double s = 0;
                for (int b = 0; b < 2; b++) for (int a = 0; a < 2; a++) s += e[(j * 2 + b) * full.w + i * 2 + a];
                base[j * w + i] = (float) (s / 4);
            }
        });
        float[] z = base.clone();
        int n = g.n;
        float[] wob = routingNoise(g, seed, 25);
        int[] rec = new int[n];
        float[] filled = new float[n];
        double[] area = new double[n];
        boolean[] done = new boolean[n];
        float[] dist = new float[n];
        for (int round = 0; round < rounds; round++) {
            // fill + receivers (priority flood from the sea)
            Arrays.fill(done, false);
            Heap q = new Heap(n / 4 + 64);
            for (int j = 0; j < h; j++) {
                for (int i = 0; i < w; i++) {
                    int k = j * w + i;
                    if (z[k] > 0) continue;
                    filled[k] = z[k];
                    done[k] = true;
                    rec[k] = -1;
                    boolean coast = false;
                    for (int d = 0; d < 8 && !coast; d++) {
                        int nb = nb(g, i, j, d);
                        if (nb >= 0 && z[nb] > 0) coast = true;
                    }
                    if (coast) q.push(0f, k);
                }
            }
            int[] order = new int[n];
            int no = 0;
            while (q.size > 0) {
                int c = q.popIndex();
                int ci = c % w, cj = c / w;
                for (int d = 0; d < 8; d++) {
                    int nb = nb(g, ci, cj, d);
                    if (nb < 0 || done[nb]) continue;
                    done[nb] = true;
                    float fc = Math.max(0, filled[c]);
                    filled[nb] = Math.max(z[nb] + wob[nb], fc + 0.01f);
                    rec[nb] = c;
                    dist[nb] = (float) (d < 4 ? (d % 2 == 0 ? g.kmX[nb / w] : g.kmY) : Math.hypot(g.kmX[nb / w], g.kmY));
                    order[no++] = nb;
                    q.push(filled[nb], nb);
                }
            }
            // drainage area, upstream first (reverse of the flood order)
            for (int t = 0; t < no; t++) { int k = order[t]; area[k] = g.kmX[k / w] * g.kmY; }
            for (int t = no - 1; t >= 0; t--) {
                int k = order[t], r = rec[k];
                if (r >= 0 && z[r] > 0) area[r] += area[k];
            }
            // implicit stream-power update, downstream first (flood order: every receiver came before its donors)
            for (int t = 0; t < no; t++) {
                int k = order[t], r = rec[k];
                if (r < 0) continue;
                double zr = z[r] > 0 ? z[r] : 0; // the sea is base level
                if (z[k] <= zr) continue;          // a hollow: water ponds here, nothing to cut
                double f = strength * Math.sqrt(area[k]) / Math.max(1, dist[k]);
                z[k] = (float) Math.max(zr + 0.5, (z[k] + f * zr) / (1 + f));
            }
        }
        // add the change back onto the full-resolution relief
        float[] delta = new float[n];
        for (int k = 0; k < n; k++) delta[k] = z[k] - base[k];
        IntStream.range(0, full.h).parallel().forEach(j -> {
            for (int i = 0; i < full.w; i++) {
                int k = j * full.w + i;
                if (e[k] <= 0) continue;
                float d = g.sample(delta, full.lon[i], full.lat[j]);
                e[k] = Math.max(0.5f, e[k] + d);
            }
        });
    }

    private static final int[] DX = {1, 0, -1, 0, 1, -1, -1, 1}, DY = {0, 1, 0, -1, 1, 1, -1, -1};

    private static int nb(Grid g, int i, int j, int d) {
        int jj = j + DY[d];
        if (jj < 0 || jj >= g.h) return -1;
        return jj * g.w + Math.floorMod(i + DX[d], g.w);
    }

    private static final class Heap {
        float[] k;
        int[] v;
        int size;

        Heap(int cap) {
            k = new float[cap];
            v = new int[cap];
        }

        void push(float key, int val) {
            if (size == k.length) {
                k = Arrays.copyOf(k, size * 2);
                v = Arrays.copyOf(v, size * 2);
            }
            int i = size++;
            while (i > 0) {
                int p = (i - 1) >> 1;
                if (k[p] <= key) break;
                k[i] = k[p];
                v[i] = v[p];
                i = p;
            }
            k[i] = key;
            v[i] = val;
        }

        int popIndex() {
            int top = v[0];
            float lk = k[--size];
            int lv = v[size];
            int i = 0;
            while (true) {
                int c = 2 * i + 1;
                if (c >= size) break;
                if (c + 1 < size && k[c + 1] < k[c]) c++;
                if (k[c] >= lk) break;
                k[i] = k[c];
                v[i] = v[c];
                i = c;
            }
            k[i] = lk;
            v[i] = lv;
            return top;
        }
    }
}
