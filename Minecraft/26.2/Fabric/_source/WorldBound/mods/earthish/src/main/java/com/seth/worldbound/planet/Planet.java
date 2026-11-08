package com.seth.worldbound.planet;

import java.util.SplittableRandom;
import java.util.function.Consumer;
import java.util.stream.IntStream;

/**
 * An Earthish planet: a world generated from a seed the way Earth's geography came about, tuned to Earth's own
 * statistics. Built once per world (a few seconds) and kept for the session:
 *
 * <ol>
 * <li>{@link Tectonics}: plates, a dispersed supercontinent, ridges, trenches, collisions (~40 km grid).</li>
 * <li>{@link Relief}: elevation from that layout (~10 km grid, the same layout as the z2 tile level).</li>
 * <li>{@link Hypsometry}: heights matched to Earth's distribution of land and sea floor.</li>
 * <li>{@link Climate}: winds, rain and temperature from the geography (~40 km), then ice sheets where summers
 * never thaw, which raise the ground and are re-run through the climate.</li>
 * <li>{@link Hydrology}: rivers and lakes from rain and slope.</li>
 * </ol>
 *
 * {@link PlanetSource} turns this into the same tile layers the real Earth comes from, with detail added below the
 * ~10 km grid procedurally, so the rest of the mod treats an Earthish world like Earth.
 */
public final class Planet {
    public static final int W = 4096, H = 2048, ZOOM = 2;
    public static final int CW = 1024, CH = 512;
    /** Bumped whenever generation changes in a way that would change an existing world's terrain. */
    public static final int VERSION = 1;
    static int EROSION_ROUNDS = 12;
    /** Development harnesses keep the build-time arrays (discharge, receivers) to inspect them. */
    public static boolean KEEP_BUILD_DATA = false;
    static double EROSION_K = 0.08;

    public final long seed;
    public final Grid grid, cgrid;
    /** Surface elevation (m, whole metres), ice included. */
    public final float[] elev;
    /** 1 where an ice sheet covers the ground. */
    public final byte[] ice;
    /** Köppen class 1..30 per z2 cell (0 for sea). */
    public final byte[] koppen;
    /** Local relief (m): how rugged the ground around each z2 cell is, for the detail generator. */
    public final float[] rugged;
    public final Climate climate;
    public Hydrology hydro;
    /** The planet as tile layers and river channels (built with the planet). */
    public final PlanetSource source;
    public final double landFraction;
    public final int plates;
    public final int spawnCell;

    private Planet(long seed, Consumer<String> log, java.nio.file.Path cacheFile) {
        this.seed = seed;
        long t0 = System.nanoTime();
        grid = new Grid(W, H);
        cgrid = new Grid(CW, CH);
        PlanetCache.Saved sv = cacheFile == null ? null : PlanetCache.read(cacheFile, seed, log);
        String timing;
        if (sv != null) {
            elev = sv.elev();
            ice = sv.ice();
            climate = sv.climate();
            landFraction = sv.landFraction();
            plates = sv.plates();
            timing = String.format(java.util.Locale.ROOT, "loaded from %s in %.1fs", cacheFile.getFileName(), (System.nanoTime() - t0) / 1e9);
        } else {
            SplittableRandom r = new SplittableRandom(seed ^ 0x9E7A9E7AL);
            Tectonics t = new Tectonics(seed, 1024, 512);
            plates = t.plates;
            long t1 = System.nanoTime();
            float[] e = Relief.build(t, grid, seed);
            long t2 = System.nanoTime();
            Erosion.apply(grid, e, EROSION_ROUNDS, EROSION_K, seed);
            long t2b = System.nanoTime();
            double lf = 0, tot = 0;
            for (int j = 0; j < H; j++) {
                double a = grid.areaW[j];
                for (int i = 0; i < W; i++) if (e[j * W + i] > 0) lf += a;
                tot += a * W;
            }
            lf /= tot;
            double target = Math.max(0.25, Math.min(0.34, lf));
            double mountains = 0.9 + 0.25 * r.nextDouble();
            Hypsometry.earth().remap(grid, e, target, mountains);
            landFraction = target;
            long t3 = System.nanoTime();
            // climate on the bedrock, then ice sheets where summers never thaw, then climate again
            float[][] cl = coarse(e);
            Climate c0 = new Climate(cgrid, cl[0], cl[1]);
            ice = new byte[grid.n];
            float[] withIce = addIce(e, c0);
            float[][] cl2 = coarse(withIce);
            climate = new Climate(cgrid, cl2[0], cl2[1]);
            // whole metres from here on, exactly what the cache keeps, so a reloaded planet is the same planet
            for (int k = 0; k < withIce.length; k++) withIce[k] = Math.round(withIce[k]);
            elev = withIce;
            long t4 = System.nanoTime();
            timing = String.format(java.util.Locale.ROOT, "tectonics %.1fs relief %.1fs erosion %.1fs heights %.1fs climate %.1fs",
                    (t1 - t0) / 1e9, (t2 - t1) / 1e9, (t2b - t2) / 1e9, (t3 - t2b) / 1e9, (t4 - t3) / 1e9);
            if (cacheFile != null) PlanetCache.write(cacheFile, this, log);
        }
        long t4 = System.nanoTime();
        koppen = new byte[grid.n];
        rugged = new float[grid.n];
        IntStream.range(0, H).parallel().forEach(j -> {
            for (int i = 0; i < W; i++) {
                int k = j * W + i;
                double el = elev[k];
                if (el > 0) koppen[k] = (byte) classify(grid.lon[i], grid.lat[j], el);
                // local relief: spread of the 3x3 neighbourhood, land among land and sea floor among sea floor
                // (the drop from a coastal plain to the shelf is not ruggedness)
                double lo = el, hi = el;
                boolean isLand = el > 0;
                for (int b = -1; b <= 1; b++) {
                    int jj = Math.max(0, Math.min(H - 1, j + b));
                    for (int a = -1; a <= 1; a++) {
                        double v = elev[jj * W + Math.floorMod(i + a, W)];
                        if ((v > 0) != isLand) continue;
                        lo = Math.min(lo, v);
                        hi = Math.max(hi, v);
                    }
                }
                rugged[k] = (float) (hi - lo);
            }
        });
        // smoothed, so the detail's strength never changes abruptly from one cell to the next
        float[] rs = grid.blur(rugged, 22);
        System.arraycopy(rs, 0, rugged, 0, rugged.length);
        long t5 = System.nanoTime();
        hydro = new Hydrology(this);
        long t6 = System.nanoTime();
        source = new PlanetSource(this);
        spawnCell = pickSpawn(new SplittableRandom(seed ^ 0x5BA5A5L));
        if (!KEEP_BUILD_DATA) {
            hydro.dropBuildData();
            grid.dropVectors();
            cgrid.dropVectors();
        }
        if (log != null) {
            Runtime rt = Runtime.getRuntime();
            log.accept(String.format(java.util.Locale.ROOT,
                    "Earthish planet %d: land %.0f%%, %d plates; %s; koppen %.1fs rivers %.1fs tiles %.1fs (total %.1fs, heap %d MB used)",
                    seed, landFraction * 100, plates, timing, (t5 - t4) / 1e9, (t6 - t5) / 1e9, (System.nanoTime() - t6) / 1e9, (System.nanoTime() - t0) / 1e9,
                    (rt.totalMemory() - rt.freeMemory()) >> 20));
        }
    }

    private static final java.util.Map<Long, Planet> CACHE = new java.util.HashMap<>();

    /** The planet for a world seed, built on first use and kept (one at a time). */
    public static synchronized Planet get(long seed, Consumer<String> log) {
        return get(seed, log, null);
    }

    /**
     * Same, with a file to keep the expensive part of the planet in (the world's folder), so reopening a world takes
     * a few seconds instead of a full build.
     */
    public static synchronized Planet get(long seed, Consumer<String> log, java.nio.file.Path cacheFile) {
        Planet p = CACHE.get(seed);
        if (p == null) {
            CACHE.clear();
            p = new Planet(seed, log, cacheFile);
            CACHE.put(seed, p);
        }
        return p;
    }

    /** Drop the kept planet (a real-Earth world is loading): it holds a few hundred MB. */
    public static synchronized void release() {
        CACHE.clear();
    }

    /** Mean elevation and land fraction on the climate grid. */
    private float[][] coarse(float[] e) {
        int f = W / CW;
        float[] el = new float[cgrid.n], lf = new float[cgrid.n];
        IntStream.range(0, CH).parallel().forEach(j -> {
            for (int i = 0; i < CW; i++) {
                double s = 0;
                int nl = 0;
                for (int b = 0; b < f; b++) {
                    for (int a = 0; a < f; a++) {
                        float v = e[(j * f + b) * W + i * f + a];
                        if (v > 0) { nl++; s += v; }
                    }
                }
                int k = j * CW + i;
                lf[k] = nl / (float) (f * f);
                el[k] = nl > 0 ? (float) (s / nl) : 0;
            }
        });
        return new float[][]{el, lf};
    }

    /** Warmest-month temperature (°C) at a place and height, from a climate result. */
    private static double tHot(Grid cg, Climate c, double lon, double lat, double elevM) {
        return cg.sample(c.t0, lon, lat) - 0.0065 * Math.max(0, elevM) + cg.sample(c.range, lon, lat) / 2;
    }

    /**
     * Ice sheets: wherever the warmest month stays below freezing, a dome of ice grows from the margin inward
     * (thickness rising like a real sheet's profile to ~3,300 m a few hundred km in), hiding the bedrock.
     */
    private float[] addIce(float[] e, Climate c) {
        int hw = W / 2, hh = H / 2;
        Grid hg = new Grid(hw, hh);
        boolean[] iceH = new boolean[hg.n], notIce = new boolean[hg.n];
        float[] bedH = new float[hg.n];
        for (int j = 0; j < hh; j++) {
            for (int i = 0; i < hw; i++) {
                int k = j * hw + i;
                double s = 0;
                int nl = 0;
                for (int b = 0; b < 2; b++) for (int a = 0; a < 2; a++) {
                    float v = e[(j * 2 + b) * W + i * 2 + a];
                    s += v;
                    if (v > 0) nl++;
                }
                bedH[k] = (float) (s / 4);
                iceH[k] = nl >= 2 && tHot(cgrid, c, hg.lon[i], hg.lat[j], s / 4) <= 0;
                notIce[k] = !iceH[k];
            }
        }
        float[] dIn = new float[hg.n];
        hg.nearest(notIce, new int[hg.n], dIn);
        float[] thick = new float[hg.n];
        for (int k = 0; k < hg.n; k++) thick[k] = iceH[k] ? (float) (3300 * Math.sqrt(1 - Math.exp(-dIn[k] / 350.0))) : 0;
        float[] bedSmooth = hg.blur(bedH, 160);
        float[] out = e.clone();
        IntStream.range(0, H).parallel().forEach(j -> {
            for (int i = 0; i < W; i++) {
                int k = j * W + i;
                if (e[k] <= 0) continue;
                double th = hg.sample(thick, grid.lon[i], grid.lat[j]);
                if (th < 30) continue;
                double surf = Math.max(0, hg.sample(bedSmooth, grid.lon[i], grid.lat[j])) + th;
                out[k] = (float) Math.min(4700, Math.max(e[k] + 30, surf));
                ice[k] = 1;
            }
        });
        return out;
    }

    /** Köppen class for a land point at a height, from the final climate. */
    public int classify(double lon, double lat, double elevM) {
        Grid cg = cgrid;
        Climate c = climate;
        double t = cg.sample(c.t0, lon, lat) - 0.0065 * Math.max(0, elevM);
        return Climate.koppen(t, cg.sample(c.range, lon, lat), cg.sample(c.map, lon, lat), cg.sample(c.summer, lon, lat));
    }

    /** Annual precipitation (mm) at a point. */
    public double precipitation(double lon, double lat) {
        return cgrid.sample(climate.map, lon, lat);
    }

    /** Warmest-month temperature (°C) at a point and height. */
    public double warmest(double lon, double lat, double elevM) {
        return temperature(lon, lat, elevM) + cgrid.sample(climate.range, lon, lat) / 2;
    }

    /** Annual mean temperature (°C) at a point and height. */
    public double temperature(double lon, double lat, double elevM) {
        return cgrid.sample(climate.t0, lon, lat) - 0.0065 * Math.max(0, elevM);
    }

    /**
     * A good place to start: temperate or warm, wet enough for trees, low ground near a river or the coast,
     * between 25° and 55°, on a big landmass. Scored over every z2 cell; the best of a random sample of the top.
     */
    private int pickSpawn(SplittableRandom r) {
        double best = -1e9;
        int bestK = -1;
        for (int j = 0; j < H; j++) {
            double al = Math.abs(grid.lat[j]);
            if (al < 18 || al > 58) continue;
            for (int i = 0; i < W; i++) {
                int k = j * W + i;
                if (elev[k] <= 2 || ice[k] != 0) continue;
                int kc = koppen[k];
                double score = 0;
                if (kc >= 14 && kc <= 15) score += 3;           // Cfa, Cfb
                else if (kc >= 8 && kc <= 13) score += 2;       // Cs, Cw
                else if (kc == 25 || kc == 26) score += 1.5;    // Dfa, Dfb
                else if (kc >= 1 && kc <= 3) score += 0.5;
                else continue;
                score -= Math.abs(al - 40) / 15;
                score -= Math.max(0, elev[k] - 600) / 400;
                score -= rugged[k] / 800;
                if (hydro != null && hydro.discharge[k] > 50) score += 1.2;
                score += r.nextDouble() * 0.8;
                if (score > best) { best = score; bestK = k; }
            }
        }
        if (bestK < 0) {
            for (int k = 0; k < grid.n; k++) if (elev[k] > 5 && ice[k] == 0) return k;
            return 0;
        }
        return bestK;
    }

    public double spawnLat() {
        return grid.lat[spawnCell / W];
    }

    public double spawnLon() {
        return grid.lon[spawnCell % W];
    }
}
