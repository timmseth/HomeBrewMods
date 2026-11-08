package com.seth.worldbound.planet;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import java.util.stream.IntStream;

/**
 * Elevation in metres at full resolution from the tectonic layout: continental platforms with basins and swells,
 * shelves that are wide on passive margins and narrow on active ones, volcanic ranges inland of trenches, plateaus and
 * high ranges where continents collided, worn-down ranges along old rift lines, sea floor that deepens with age away
 * from the ridges, trenches, island arcs, hotspot volcanoes and seamounts. The result is "raw": its shape is right but
 * its height distribution is then matched to Earth's by {@link Hypsometry}.
 */
final class Relief {
    private Relief() {}

    /** Sea-floor depth (m, positive) for crust of this age (Myr): Stein & Stein GDH1. */
    static double depthForAge(double t) {
        if (!(t >= 0)) t = 120;
        return t < 20 ? 2600 + 365 * Math.sqrt(t) : 5651 - 2473 * Math.exp(-0.0278 * t);
    }

    static double smooth(double e0, double e1, double x) {
        double t = Math.max(0, Math.min(1, (x - e0) / (e1 - e0)));
        return t * t * (3 - 2 * t);
    }

    /**
     * Mountain-belt texture in about [0, 1]: ranges running along the belt (crests every {@code spacingKm} across it,
     * using the distance to the belt's driving boundary as the across-belt coordinate, bent by noise), broken up by
     * isotropic ridged noise so they read as chains of peaks and passes rather than rails.
     */
    static double belt(Noise3 n, double x, double y, double z, double acrossKm, double spacingKm, double freq) {
        double u = acrossKm / spacingKm + 1.7 * n.fbm(x, y, z, freq * 0.25, 3, 0.5);
        double par = 1 - Math.abs(Math.cos(Math.PI * u));
        // ridges come and go along the strike, so they read as ranges with gaps, not rails
        par = par * par * smooth(-0.25, 0.45, n.fbm(x - 9.1, y + 3.3, z + 2.2, freq * 0.5, 2, 0.5));
        double iso = n.ridged(x + 1.3, y - 2.6, z + 0.7, freq, 4, 0.5);
        return 0.3 * par + 0.7 * iso * (0.75 + 0.45 * par);
    }

    static float[] build(Tectonics t, Grid G, long seed) {
        SplittableRandom r = new SplittableRandom(seed ^ 0x2E11EFL);
        Noise3 nLow = new Noise3(r.nextLong()), nMid = new Noise3(r.nextLong()), nCoast = new Noise3(r.nextLong());
        Noise3 nRidge = new Noise3(r.nextLong()), nRidge2 = new Noise3(r.nextLong()), nVar = new Noise3(r.nextLong());
        Noise3 nAbyss = new Noise3(r.nextLong()), nArc = new Noise3(r.nextLong()), nOld = new Noise3(r.nextLong());
        Noise3 nSea = new Noise3(r.nextLong());
        double arcScale = 0.85 + 0.35 * r.nextDouble(), collScale = 0.85 + 0.35 * r.nextDouble(), oldScale = 0.7 + 0.6 * r.nextDouble();

        Grid cg = t.g;
        // coarse fields made safe for bilinear sampling
        float[] age = t.age.clone(), dTr = t.dTrench.clone(), dP0 = t.dP0.clone(), dColl = t.dColl.clone(), dOld = t.dOld.clone();
        float[] dIn = t.dCollIn;
        float[] over = new float[cg.n], under = new float[cg.n], oceanOver = new float[cg.n];
        for (int k = 0; k < cg.n; k++) {
            if (!(age[k] >= 0)) age[k] = 140;
            dTr[k] = Math.min(dTr[k], 20000);
            dP0[k] = Math.min(dP0[k], 20000);
            dColl[k] = Math.min(dColl[k], 20000);
            dOld[k] = Math.min(dOld[k], 20000);
            over[k] = t.arcSide[k] == 1 ? 1 : 0;
            under[k] = t.arcSide[k] == 2 ? 1 : 0;
        }
        // seamount and hotspot cones, bucketed on the coarse grid
        List<double[]> cones = new ArrayList<>(t.cones);
        int seamounts = 900 + r.nextInt(600);
        for (int q = 0; q < seamounts; q++) {
            double[] v = Tectonics.randomUnit(r);
            cones.add(new double[]{v[0], v[1], v[2], 800 + 2600 * Math.pow(r.nextDouble(), 2.2), 8 + 16 * r.nextDouble()});
        }
        @SuppressWarnings("unchecked") List<double[]>[] bucket = new List[cg.n];
        for (double[] c : cones) {
            double lat = Math.toDegrees(Math.asin(c[2])), lon = Math.toDegrees(Math.atan2(c[1], c[0]));
            double rk = c[4] * 2.2;
            int j0 = (int) Math.floor(cg.fy(lat + rk / 111.0)), j1 = (int) Math.ceil(cg.fy(lat - rk / 111.0));
            double coslat = Math.max(0.02, Math.cos(Math.toRadians(lat)));
            int i0 = (int) Math.floor(cg.fx(lon - rk / 111.0 / coslat)), i1 = (int) Math.ceil(cg.fx(lon + rk / 111.0 / coslat));
            if (i1 - i0 > cg.w) { i0 = 0; i1 = cg.w - 1; }
            for (int j = Math.max(0, j0); j <= Math.min(cg.h - 1, j1); j++) {
                for (int i = i0; i <= i1; i++) {
                    int k = cg.idx(i, j);
                    if (bucket[k] == null) bucket[k] = new ArrayList<>(2);
                    bucket[k].add(c);
                }
            }
        }

        float[] e = new float[G.n];
        IntStream.range(0, G.h).parallel().forEach(j -> {
            double lat = G.lat[j];
            for (int i = 0; i < G.w; i++) {
                int k = j * G.w + i;
                double lon = G.lon[i];
                double x = G.vx[k], y = G.vy[k], z = G.vz[k];
                double sd = cg.sample(t.sdCont, lon, lat);
                double coast = 85 * nCoast.fbm(x, y, z, 14, 6, 0.62);   // km: bays, capes, offshore islands
                double sdn = sd + coast;
                double dt = cg.sample(dTr, lon, lat);
                double ov = cg.sample(over, lon, lat), un = cg.sample(under, lon, lat);
                double arcS = cg.sample(t.arc, lon, lat);
                double active = ov * Math.exp(-dt / 450);
                double v;
                if (sdn > 0) {
                    // ---- continent
                    double base = 470 + 330 * nLow.fbm(x, y, z, 2.6, 4, 0.5) + 150 * nMid.fbm(x, y, z, 10, 3, 0.5);
                    double wShelf = 420 - 350 * active;
                    double c = Math.pow(smooth(0, wShelf, sdn), 0.8);
                    v = -140 + (base + 140) * c;
                    // volcanic range inland of a trench
                    if (ov > 0.01 && dt < 900) {
                        double prof = Math.exp(-Math.pow((dt - 200) / 120, 2)) + 0.35 * Math.exp(-Math.pow((dt - 420) / 160, 2));
                        double hgt = arcS * arcScale * (3000 + 1600 * nVar.fbm(x, y, z, 3, 3, 0.5));
                        double tex = belt(nRidge, x, y, z, dt, 70, 45);
                        v += ov * hgt * prof * (0.35 + 0.85 * tex) * c;
                    }
                    // collision: high ranges at the suture, a plateau behind
                    double co = cg.sample(t.coll, lon, lat), dc = cg.sample(dColl, lon, lat), din = cg.sample(dIn, lon, lat);
                    if (co > 0.01 || dc < 600) {
                        // a plateau a few hundred km into the zone (Tibet), lower ground beyond it (Tarim), and high
                        // ranges along both edges (Himalaya, Kunlun)
                        double plateau = 3300 * smooth(0.1, 0.6, co) * smooth(60, 260, din) * (1 - 0.65 * smooth(650, 1100, din));
                        double edge = Math.min(dc, din);
                        double rim = 2700 * Math.exp(-Math.pow(edge / 150, 2)) + 800 * Math.exp(-Math.pow(dc / 400, 2));
                        double tex = belt(nRidge2, x, y, z, dc > 0 ? dc : din, 80, 70);
                        v += collScale * (plateau * (0.8 + 0.3 * tex) + rim * (0.3 + 0.95 * tex)) * c;
                    }
                    // rift shoulders and escarpments along the lines where the supercontinent split, in stretches
                    double d0 = cg.sample(dP0, lon, lat);
                    if (d0 < 900) {
                        double patch = smooth(0.1, 0.55, nOld.fbm(x, y, z, 4, 3, 0.5));
                        double at = 220 + 120 * nOld.fbm(x - 2.2, y + 4.1, z, 6, 2, 0.5);
                        double prof = Math.exp(-Math.pow((d0 - at) / 110, 2));
                        double hgt = oldScale * (400 + 1000 * patch);
                        double tex = belt(nRidge, x + 3.1, y - 1.7, z + 0.4, d0, 55, 60);
                        v += hgt * prof * patch * (0.3 + 0.9 * tex) * c;
                    }
                    // old worn-down belts inside the continents (Urals, Appalachians)
                    double dold = cg.sample(dOld, lon, lat);
                    if (dold < 500) {
                        double hgt = oldScale * (900 + 700 * nOld.fbm(x + 7.7, y, z - 3.3, 5, 3, 0.5));
                        double tex = belt(nRidge2, x - 4.4, y + 2.2, z, dold, 45, 50);
                        v += hgt * Math.exp(-Math.pow(dold / 150, 2)) * (0.25 + 0.95 * tex) * c;
                    }
                } else {
                    // ---- ocean
                    double a = cg.sample(age, lon, lat);
                    v = -depthForAge(a) + 260 * nAbyss.fbm(x, y, z, 160, 3, 0.55);
                    if (a < 3) v += 420 * (1 - a / 3) * (0.6 + 0.4 * nAbyss.fbm(x, y, z, 600, 2, 0.5)) - 300 * Math.exp(-a * 3);
                    // trench and outer rise on the plate going down
                    if (un > 0.01 && dt < 400) {
                        v -= un * arcS * (2600 + 1400 * nVar.fbm(x, y, z, 6, 2, 0.5)) * Math.exp(-Math.pow(dt / 55, 2));
                        v += un * 280 * Math.exp(-Math.pow((dt - 130) / 60, 2));
                    }
                    // island arc on an oceanic overriding plate
                    if (ov > 0.01 && dt < 500) {
                        double isl = smooth(0.25, 0.7, nArc.ridged(x, y, z, 30, 4, 0.5));
                        v += ov * arcS * (5600 * isl + 1200) * Math.exp(-Math.pow((dt - 160) / 60, 2));
                        v -= ov * arcS * 900 * Math.exp(-Math.pow((dt - 60) / 40, 2)); // forearc
                    }
                    // continental slope and rise down from the shelf break
                    double wSlope = 170 - 110 * active;
                    double s = smooth(0, wSlope, -sdn);
                    v = -140 + (v + 140) * Math.pow(s, 0.7) + 60 * nSea.fbm(x, y, z, 300, 2, 0.5) * (1 - s);
                }
                // volcanoes: hotspot chains and seamounts (on the sea floor)
                List<double[]> bl = bucket[cg.cellOf(x, y, z)];
                if (bl != null) {
                    double add = 0;
                    for (double[] c : bl) {
                        double d = Grid.R_KM * Math.acos(Math.max(-1, Math.min(1, x * c[0] + y * c[1] + z * c[2])));
                        if (d < c[4] * 2.2) add = Math.max(add, c[3] * Math.exp(-2.2 * (d / c[4]) * (d / c[4])));
                    }
                    if (sdn < 0) v += add;
                }
                e[k] = (float) v;
            }
        });
        return e;
    }
}
