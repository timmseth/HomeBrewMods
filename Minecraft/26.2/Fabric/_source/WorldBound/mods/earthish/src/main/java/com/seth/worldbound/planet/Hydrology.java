package com.seth.worldbound.planet;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Where the rain goes, on the ~10 km planet grid:
 *
 * <ul>
 * <li>Priority-flood (Barnes 2014) fills every closed hollow to its spill point, so every land cell drains to the sea
 * or into a lake; flow then follows the steepest descent of the filled surface.</li>
 * <li>Runoff is rain minus what the ground and plants give back (Budyko curve, Fu's form, with a temperature-driven
 * potential evaporation); summed downstream it gives each cell's discharge in m³/s.</li>
 * <li>A filled hollow holds a lake if enough water reaches it: one whose inflow can't make up for the evaporation off
 * its full surface settles lower, where the two balance, and has no outlet (the Caspian, the Great Salt Lake); one
 * that barely gets any water is a dry pan.</li>
 * <li>Every cell carrying 12 m³/s or more is a river; {@link PlanetRivers} draws them as smooth meandering channels.</li>
 * </ul>
 */
public final class Hydrology {
    public static final double Q_RIVER = 12;
    public static final short NO_LAKE = Short.MIN_VALUE;
    static int MIN_LAKE_CELLS = 3;
    static double MIN_LAKE_DEPTH = 25;

    final Planet p;
    final Grid g;
    /** Downstream cell, -1 at the sea or in a closed lake (build only; dropped afterwards). */
    int[] receiver;
    /** Discharge, m³/s (build only; dropped afterwards). */
    public float[] discharge;
    /** Lake surface (m) for cells under a lake, else NO_LAKE. */
    public final short[] lakeLevel;
    /** Filled surface (m): the ground with every hollow brimmed to its spill point (build only). */
    float[] filled;
    /** River cells (discharge >= Q_RIVER, not under a lake), largest discharge first. */
    final int[] rivers;
    public final int lakes, endorheic;

    Hydrology(Planet planet) {
        this.p = planet;
        this.g = planet.grid;
        int n = g.n, w = g.w, h = g.h;
        float[] e = planet.elev;

        // ---- priority flood (over the ground plus a few metres of wobble, so flats drain in natural lines) -----
        float[] wob = Erosion.routingNoise(g, planet.seed, 6);
        filled = new float[n];
        boolean[] done = new boolean[n];
        Heap q = new Heap(n / 3 + 1024);
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                int k = j * w + i;
                if (e[k] > 0) continue;
                filled[k] = e[k];
                done[k] = true;
                // only sea cells touching land seed the flood
                boolean coast = false;
                for (int d = 0; d < 8 && !coast; d++) {
                    int nb = nb(i, j, d);
                    if (nb >= 0 && e[nb] > 0) coast = true;
                }
                if (coast) q.push(e[k], k);
            }
        }
        while (q.size > 0) {
            int c = q.popIndex();
            float fc = filled[c];
            int ci = c % w, cj = c / w;
            for (int d = 0; d < 8; d++) {
                int nb = nb(ci, cj, d);
                if (nb < 0 || done[nb]) continue;
                done[nb] = true;
                float v = e[nb] + wob[nb];
                float lift = Math.max(Math.ulp(fc) * 4, 0.002f);
                filled[nb] = v <= fc ? fc + lift : v;
                q.push(filled[nb], nb);
            }
        }

        // ---- steepest descent on the filled surface -------------------------------------------------------
        receiver = new int[n];
        IntStream.range(0, h).parallel().forEach(j -> {
            for (int i = 0; i < w; i++) {
                int k = j * w + i;
                if (e[k] <= 0) { receiver[k] = -1; continue; }
                int best = -1;
                double bs = 0;
                for (int d = 0; d < 8; d++) {
                    int nb = nb(i, j, d);
                    if (nb < 0) continue;
                    double drop = filled[k] - filled[nb];
                    if (drop <= 0) continue;
                    double dist = d < 4 ? (d % 2 == 0 ? g.kmX[j] : g.kmY) : Math.hypot(g.kmX[j], g.kmY);
                    double s = drop / dist;
                    if (s > bs) { bs = s; best = nb; }
                }
                receiver[k] = best;
            }
        });

        // ---- runoff and discharge, upstream to downstream ---------------------------------------------------
        double[] runoff = new double[n];
        Grid cg = planet.cgrid;
        Climate cl = planet.climate;
        IntStream.range(0, h).parallel().forEach(j -> {
            double areaM2 = g.kmX[j] * g.kmY * 1e6;
            for (int i = 0; i < w; i++) {
                int k = j * w + i;
                if (e[k] <= 0) continue;
                double pmm = cg.sample(cl.map, g.lon[i], g.lat[j]);
                double t = cg.sample(cl.t0, g.lon[i], g.lat[j]) - 0.0065 * e[k];
                double r = pmm - evapotranspiration(pmm, pet(t));
                runoff[k] = Math.max(0, r) * areaM2 / 1000 / 31_557_600.0;
            }
        });
        int land = 0;
        for (int k = 0; k < n; k++) if (e[k] > 0) land++;
        int[] order = new int[land];
        float[] key = new float[land];
        int o = 0;
        for (int k = 0; k < n; k++) if (e[k] > 0) { order[o] = k; key[o] = filled[k]; o++; }
        sortDescending(order, key);
        discharge = new float[n];
        double[] acc = new double[n];
        // lakes: hollows deeper than a few metres; labelled by the cell they spill from
        lakeLevel = new short[n];
        Arrays.fill(lakeLevel, NO_LAKE);
        int[] lakeOf = new int[n];
        Arrays.fill(lakeOf, -1);
        for (int idx = 0; idx < land; idx++) {
            int k = order[idx];
            acc[k] += runoff[k];
        }
        // pass 1: plain accumulation (needed to know each hollow's inflow)
        for (int idx = 0; idx < land; idx++) {
            int k = order[idx], r = receiver[k];
            if (r >= 0 && e[r] > 0) acc[r] += acc[k];
        }
        // find hollows: connected cells with filled - ground > 3 m, grouped by flood fill
        int nLakes = 0, nEndo = 0;
        boolean[] hollow = new boolean[n];
        for (int k = 0; k < n; k++) hollow[k] = e[k] > 0 && filled[k] - (e[k] + wob[k]) > 3 && planet.ice[k] == 0;
        int[] stack = new int[n / 8 + 1024];
        int[] members = new int[1 << 16];
        double[] kept = new double[n]; // discharge lost to closed lakes, removed downstream
        for (int k0 = 0; k0 < n; k0++) {
            if (!hollow[k0] || lakeOf[k0] >= 0) continue;
            // gather the hollow
            int sp = 0, nm = 0;
            stack = push(stack, sp++, k0);
            lakeOf[k0] = nLakes;
            double lvl = filled[k0];
            while (sp > 0) {
                int c = stack[--sp];
                if (nm == members.length) members = Arrays.copyOf(members, nm * 2);
                members[nm++] = c;
                lvl = Math.max(lvl, filled[c]);
                int ci = c % w, cj = c / w;
                for (int d = 0; d < 8; d++) {
                    int nb = nb(ci, cj, d);
                    if (nb < 0 || !hollow[nb] || lakeOf[nb] >= 0) continue;
                    lakeOf[nb] = nLakes;
                    stack = push(stack, sp++, nb);
                }
            }
            // inflow: the largest accumulated flow among the hollow's cells is at its outlet
            double inflow = 0, areaFull = 0, tSum = 0;
            int outlet = members[0];
            for (int m = 0; m < nm; m++) {
                int c = members[m];
                if (acc[c] > inflow) { inflow = acc[c]; outlet = c; }
                int cj = c / w;
                areaFull += g.kmX[cj] * g.kmY * 1e6;
                tSum += cg.sample(cl.t0, g.lon[c % w], g.lat[cj]) - 0.0065 * e[c];
            }
            double evapRate = pet(tSum / nm) / 1000 / 31_557_600.0; // m/s off open water
            double level = lvl;
            boolean closed = false;
            if (inflow < evapRate * areaFull) {
                // not enough water to fill it: it settles where evaporation off its surface equals the inflow
                closed = true;
                float[] beds = new float[nm];
                double[] area = new double[nm];
                Integer[] ord = new Integer[nm];
                for (int m = 0; m < nm; m++) ord[m] = m;
                final int[] mem = members;
                Arrays.sort(ord, (a, b) -> Float.compare(e[mem[a]], e[mem[b]]));
                double a = 0;
                level = Double.NaN;
                for (int m = 0; m < nm; m++) {
                    int c = members[ord[m]];
                    a += g.kmX[c / w] * g.kmY * 1e6;
                    if (evapRate * a > inflow) {
                        level = m == 0 ? Double.NaN : e[members[ord[m - 1]]] + 0.5 * (e[c] - e[members[ord[m - 1]]]);
                        break;
                    }
                }
            }
            // a lake needs a real basin: big enough and deep enough (small, shallow hollows are dry dips)
            double maxDepth = 0;
            for (int m = 0; m < nm; m++) maxDepth = Math.max(maxDepth, lvl - e[members[m]]);
            if (nm < MIN_LAKE_CELLS || maxDepth < MIN_LAKE_DEPTH) level = Double.NaN;
            if (!Double.isNaN(level) && nm >= 2) {
                short lv = (short) Math.max(-500, Math.min(8000, Math.round(level)));
                int under = 0;
                for (int m = 0; m < nm; m++) if (e[members[m]] < level) { lakeLevel[members[m]] = lv; under++; }
                if (under > 0) {
                    nLakes++;
                    if (closed) nEndo++;
                }
            }
            if (closed) kept[outlet] += inflow;           // nothing flows on
            else kept[outlet] += Math.min(inflow, evapRate * areaFull); // the lake's own evaporation
        }
        lakes = nLakes;
        endorheic = nEndo;
        // pass 2: accumulate again, taking out what lakes keep
        Arrays.fill(acc, 0);
        for (int idx = 0; idx < land; idx++) {
            int k = order[idx];
            acc[k] += runoff[k];
            acc[k] = Math.max(0, acc[k] - kept[k]);
            int r = receiver[k];
            if (r >= 0 && e[r] > 0) acc[r] += acc[k];
        }
        for (int k = 0; k < n; k++) discharge[k] = (float) acc[k];

        // ---- river cells -------------------------------------------------------------------------------------
        int nr = 0;
        for (int k = 0; k < n; k++) if (e[k] > 0 && acc[k] >= Q_RIVER && lakeLevel[k] == NO_LAKE) nr++;
        int[] rv = new int[nr];
        float[] rq = new float[nr];
        nr = 0;
        for (int k = 0; k < n; k++) if (e[k] > 0 && acc[k] >= Q_RIVER && lakeLevel[k] == NO_LAKE) { rv[nr] = k; rq[nr] = (float) acc[k]; nr++; }
        sortDescending(rv, rq);
        rivers = rv;
    }

    /** Free what only the build needed (~100 MB). */
    void dropBuildData() {
        receiver = null;
        discharge = null;
        filled = null;
    }

    /** Potential evaporation (mm/yr) for an annual mean temperature. */
    static double pet(double t) {
        return 1800 * (1 - Math.exp(-Math.max(0, t + 5) / 22));
    }

    /** Actual evapotranspiration (mm/yr): Fu's form of the Budyko curve. */
    static double evapotranspiration(double p, double pet) {
        if (p <= 0) return 0;
        double phi = pet / p, w = 2.6;
        return p * (1 + phi - Math.pow(1 + Math.pow(phi, w), 1 / w));
    }

    // neighbours: 0 E, 1 S, 2 W, 3 N, 4 SE, 5 SW, 6 NW, 7 NE
    private static final int[] DX = {1, 0, -1, 0, 1, -1, -1, 1}, DY = {0, 1, 0, -1, 1, 1, -1, -1};

    private int nb(int i, int j, int d) {
        int jj = j + DY[d];
        if (jj < 0 || jj >= g.h) return -1;
        return jj * g.w + Math.floorMod(i + DX[d], g.w);
    }

    private static int[] push(int[] a, int at, int v) {
        if (at >= a.length) a = Arrays.copyOf(a, a.length * 2);
        a[at] = v;
        return a;
    }

    /** Sort idx by key, descending (parallel arrays), radix-free: by packed long. */
    static void sortDescending(int[] idx, float[] key) {
        long[] packed = new long[idx.length];
        for (int i = 0; i < idx.length; i++) {
            int bits = Float.floatToIntBits(key[i]);
            bits ^= (bits >> 31) & 0x7FFFFFFF; // total order for floats
            packed[i] = ((long) bits << 32) | (idx[i] & 0xFFFFFFFFL);
        }
        Arrays.parallelSort(packed);
        int n = idx.length;
        for (int i = 0; i < n; i++) {
            long v = packed[n - 1 - i];
            idx[i] = (int) v;
            int bits = (int) (v >> 32);
            bits ^= (bits >> 31) & 0x7FFFFFFF;
            key[i] = Float.intBitsToFloat(bits);
        }
    }

    /** Binary min-heap of (float key, int value). */
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
