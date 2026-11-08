package com.seth.worldbound.planet;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import java.util.stream.IntStream;

/**
 * Plate tectonics as a single kinematic step: a supercontinent (one or two blobs of continental crust) is cut into
 * plates, every plate turns about its own Euler pole for one "era", and the present day is read off where the plates
 * ended up. That gives the things that make Earth's geography look the way it does, in the right places relative to
 * each other: oceans opening behind dispersing continents with a mid-ocean ridge down the middle, coasts that fit
 * together across them, active margins (trench offshore, volcanic range inland) on the leading edges, broad shelves on
 * the trailing ones, continent-continent collisions with a plateau behind them, old worn-down ranges along the lines
 * where the supercontinent rifted, and hotspot island chains dragged out along the plate motion.
 *
 * Everything here is on a coarse grid (~40 km); {@link Relief} turns it into elevation at full resolution.
 */
final class Tectonics {
    static final byte OLD_OCEAN = 0, NEW_OCEAN = 1, CONT = 2;

    final Grid g;
    final int plates;
    final int contPlates;
    final double[][] seed;
    final double[][] axis;
    final double[] angle;
    final double tSim;
    final boolean[] continental;

    final byte[] label0;
    final float[] f0;
    final float f0Thresh;
    final float[] d0;

    final byte[] owner;
    final byte[] crust;
    final float[] age;
    final float[] sdCont;
    final float[] dTrench;
    final float[] arc;
    final byte[] arcSide;
    final float[] coll;
    final float[] dColl;
    /** km from the edge of a collision zone into it (0 outside). */
    final float[] dCollIn;
    final float[] dP0;
    /** km to an old interior mountain belt (a worn-down suture inside a continent, like the Urals), in the owner's frame. */
    final float[] dOld;
    /** Rift distance: km from the nearest new ocean for continental cells (young passive margins, rift shoulders). */
    final float[] dNew;
    final List<double[]> cones = new ArrayList<>();

    private final double[][] rotInv; // per plate: 3x3 row-major inverse rotation (present -> time 0)

    Tectonics(long worldSeed, int w, int h) {
        g = new Grid(w, h);
        SplittableRandom r = new SplittableRandom(worldSeed ^ 0x7EC70A1CL);
        Noise3 warpN = new Noise3(r.nextLong()), warpN2 = new Noise3(r.nextLong()), warpN3 = new Noise3(r.nextLong());
        Noise3 contN = new Noise3(r.nextLong()), contN2 = new Noise3(r.nextLong());

        // ---- the supercontinent: one or two irregular blobs ---------------------------------------------
        int blobs = r.nextDouble() < 0.6 ? 1 : 2;
        double[][] centre = new double[blobs][];
        double[] reach = new double[blobs];
        centre[0] = randomUnit(r);
        reach[0] = 1.0;
        if (blobs == 2) {
            // a second, smaller mass, 60-120 degrees away (Laurasia / Gondwana)
            double[] c;
            do c = randomUnit(r); while (dot(c, centre[0]) > Math.cos(Math.toRadians(60)) || dot(c, centre[0]) < Math.cos(Math.toRadians(120)));
            centre[1] = c;
            reach[1] = 0.75 + 0.2 * r.nextDouble();
        }
        double contFrac = 0.33 + 0.04 * r.nextDouble();
        double lobe = 0.55 + 0.25 * r.nextDouble();
        f0 = new float[g.n];
        IntStream.range(0, g.h).parallel().forEach(j -> {
            for (int i = 0; i < g.w; i++) {
                int k = j * g.w + i;
                double x = g.vx[k], y = g.vy[k], z = g.vz[k];
                double cap = -2;
                for (int b = 0; b < blobs; b++) cap = Math.max(cap, (x * centre[b][0] + y * centre[b][1] + z * centre[b][2]) * reach[b]);
                // big lobes and bays, then the ragged outline
                double v = cap + lobe * contN.fbm(x, y, z, 1.3, 5, 0.55) + 0.18 * contN2.fbm(x, y, z, 6.0, 4, 0.55);
                f0[k] = (float) v;
            }
        });
        f0Thresh = g.quantile(f0, null, 1 - contFrac);
        boolean[] crust0 = new boolean[g.n];
        for (int k = 0; k < g.n; k++) crust0[k] = f0[k] > f0Thresh;

        // ---- plates at time 0: the supercontinent cut into pieces, each with an apron of sea floor, and the
        //      surrounding ocean split into oceanic plates (warped Voronoi, additively weighted) -----------------
        int contPlates = (blobs == 1 ? 3 : 5) + r.nextInt(4), oceanPlates = 4 + r.nextInt(5);
        plates = contPlates + oceanPlates;
        this.contPlates = contPlates;
        seed = new double[plates][];
        double[] bias = new double[plates];
        for (int i = 0; i < plates; i++) {
            boolean wantCont = i < contPlates;
            double[] v;
            int tries = 0;
            while (true) {
                v = randomUnit(r);
                tries++;
                int c = g.cellOf(v[0], v[1], v[2]);
                if (crust0[c] != wantCont && tries < 5000) continue;
                if (!tooClose(v, seed, i, tries < 2000 ? (wantCont ? 0.35 : 0.5) : 0.15)) break;
            }
            seed[i] = v;
            double u = r.nextDouble();
            bias[i] = u < 0.3 ? 0.18 + 0.12 * r.nextDouble() : u < 0.75 ? 0.06 + 0.1 * r.nextDouble() : r.nextDouble() * 0.05;
        }
        // apron: sea floor within 0-450 km of the crust goes with the continental piece next to it
        int[] nearC = new int[g.n];
        float[] dCrust = new float[g.n];
        g.nearest(crust0, nearC, dCrust);
        label0 = new byte[g.n];
        IntStream.range(0, g.h).parallel().forEach(j -> {
            for (int i = 0; i < g.w; i++) {
                int k = j * g.w + i;
                double x = g.vx[k], y = g.vy[k], z = g.vz[k];
                boolean contSide = crust0[k];
                if (!contSide) {
                    double apron = 220 + 230 * warpN.fbm(x + 5.5, y - 2.2, z + 1.1, 3.0, 3, 0.5);
                    contSide = dCrust[k] < apron;
                }
                double qx = x, qy = y, qz = z;
                if (contSide && !crust0[k] && nearC[k] >= 0) { qx = g.vx[nearC[k]]; qy = g.vy[nearC[k]]; qz = g.vz[nearC[k]]; }
                double wx = qx + 0.30 * warpN.fbm(qx, qy, qz, 1.6, 5, 0.62), wy = qy + 0.30 * warpN2.fbm(qx, qy, qz, 1.6, 5, 0.62),
                        wz = qz + 0.30 * warpN3.fbm(qx, qy, qz, 1.6, 5, 0.62);
                double inv = 1 / Math.sqrt(wx * wx + wy * wy + wz * wz);
                wx *= inv; wy *= inv; wz *= inv;
                int best = 0;
                double bd = Double.MAX_VALUE;
                int p0 = contSide ? 0 : contPlates, p1 = contSide ? contPlates : plates;
                for (int p = p0; p < p1; p++) {
                    double d = Math.acos(Math.max(-1, Math.min(1, wx * seed[p][0] + wy * seed[p][1] + wz * seed[p][2]))) - bias[p];
                    if (d < bd) { bd = d; best = p; }
                }
                label0[k] = (byte) best;
            }
        });

        // ---- distance to the time-0 plate boundaries (old sutures, rift lines) ---------------------------
        boolean[] b0 = new boolean[g.n];
        for (int j = 0; j < g.h; j++) {
            for (int i = 0; i < g.w; i++) {
                int k = j * g.w + i;
                byte l = label0[k];
                // only the lines where the supercontinent itself was cut (rifts, old sutures between its pieces)
                if (l >= contPlates) continue;
                byte e = label0[g.idx(i + 1, j)], s2 = j + 1 < g.h ? label0[k + g.w] : l;
                b0[k] = (e != l && e < contPlates) || (s2 != l && s2 < contPlates);
            }
        }
        int[] near = new int[g.n];
        d0 = new float[g.n];
        g.nearest(b0, near, d0);

        // ---- plate motions --------------------------------------------------------------------------------
        double[][] centroid = new double[plates][3];
        double[] area = new double[plates], contArea = new double[plates];
        for (int j = 0; j < g.h; j++) {
            for (int i = 0; i < g.w; i++) {
                int k = j * g.w + i, p = label0[k];
                double a = g.areaW[j];
                centroid[p][0] += g.vx[k] * a;
                centroid[p][1] += g.vy[k] * a;
                centroid[p][2] += g.vz[k] * a;
                area[p] += a;
                if (f0[k] > f0Thresh) contArea[p] += a;
            }
        }
        axis = new double[plates][];
        angle = new double[plates];
        continental = new boolean[plates];
        tSim = 110 + 60 * r.nextDouble();
        double spread = 0.8 + 0.45 * r.nextDouble(); // how far this planet's continents have dispersed
        for (int p = 0; p < plates; p++) {
            double[] m = normalize(centroid[p]);
            if (m == null) m = seed[p];
            continental[p] = p < contPlates;
            // nearest supercontinent centre
            double[] c = centre[0];
            for (int b = 1; b < blobs; b++) if (dot(centre[b], m) > dot(c, m)) c = centre[b];
            double[] ax = normalize(cross(c, m));
            double cosToCentre = dot(c, m);
            if (continental[p] && ax != null && cosToCentre < 0.97) {
                // move away from the supercontinent's middle, give or take; now and then sideways into a
                // neighbour (India into Asia, Africa into Europe)
                double turn = r.nextDouble() < 0.3 ? (r.nextDouble() - 0.5) * Math.toRadians(220) : (r.nextDouble() - 0.5) * Math.toRadians(70);
                ax = rotateAbout(ax, m, turn);
                angle[p] = spread * (0.16 + 0.26 * r.nextDouble());
            } else if (continental[p]) {
                ax = randomUnit(r); // the core plate (Africa): barely moves
                angle[p] = 0.02 + 0.07 * r.nextDouble();
            } else {
                ax = randomUnit(r);
                angle[p] = spread * (0.03 + 0.12 * r.nextDouble());
            }
            axis[p] = ax;
        }
        rotInv = new double[plates][];
        for (int p = 0; p < plates; p++) rotInv[p] = rotation(axis[p], -angle[p]);

        // ---- the present day: which plates claim each cell -------------------------------------------------
        owner = new byte[g.n];
        crust = new byte[g.n];
        byte[] lost = new byte[g.n];      // plate that went under this cell (subduction), -1 none
        boolean[] collision = new boolean[g.n];
        double[] buoy = new double[plates];
        for (int p = 0; p < plates; p++) buoy[p] = r.nextDouble() + (p < contPlates ? 1 : 0); // old open-ocean floor sinks first
        IntStream.range(0, g.h).parallel().forEach(j -> {
            int[] cl = new int[plates];
            boolean[] cc = new boolean[plates];
            for (int i = 0; i < g.w; i++) {
                int k = j * g.w + i;
                double x = g.vx[k], y = g.vy[k], z = g.vz[k];
                int nc = 0;
                for (int p = 0; p < plates; p++) {
                    double[] m = rotInv[p];
                    double x0 = m[0] * x + m[1] * y + m[2] * z, y0 = m[3] * x + m[4] * y + m[5] * z, z0 = m[6] * x + m[7] * y + m[8] * z;
                    int c0 = g.cellOf(x0, y0, z0);
                    if (label0[c0] == p) {
                        cl[nc] = p;
                        cc[nc] = g.sample(f0, x0, y0, z0) > f0Thresh;
                        nc++;
                    }
                }
                lost[k] = -1;
                if (nc == 0) {
                    owner[k] = -1; // a gap: new sea floor, filled in below
                    crust[k] = NEW_OCEAN;
                } else if (nc == 1) {
                    owner[k] = (byte) cl[0];
                    crust[k] = cc[0] ? CONT : OLD_OCEAN;
                } else {
                    int conts = 0, best = -1;
                    for (int q = 0; q < nc; q++) if (cc[q]) { conts++; if (best < 0 || contArea[cl[q]] > contArea[best]) best = cl[q]; }
                    if (conts == 0) for (int q = 0; q < nc; q++) if (best < 0 || buoy[cl[q]] > buoy[best]) best = cl[q];
                    owner[k] = (byte) best;
                    crust[k] = conts > 0 ? CONT : OLD_OCEAN;
                    collision[k] = conts >= 2;
                    for (int q = 0; q < nc; q++) if (cl[q] != best && !(conts >= 2 && cc[q])) { lost[k] = (byte) cl[q]; break; }
                }
            }
        });

        // gaps belong to whichever plate is nearest; the line where two plates meet inside a gap is the ridge
        boolean[] claimed = new boolean[g.n];
        for (int k = 0; k < g.n; k++) claimed[k] = owner[k] >= 0;
        float[] dEdge = new float[g.n];
        g.nearest(claimed, near, dEdge);
        for (int k = 0; k < g.n; k++) if (owner[k] < 0) owner[k] = near[k] >= 0 ? owner[near[k]] : 0;
        boolean[] ridge = new boolean[g.n];
        for (int j = 0; j < g.h; j++) {
            for (int i = 0; i < g.w; i++) {
                int k = j * g.w + i;
                if (crust[k] != NEW_OCEAN) continue;
                int e = g.idx(i + 1, j), s = j + 1 < g.h ? k + g.w : k;
                if ((crust[e] == NEW_OCEAN && owner[e] != owner[k]) || (crust[s] == NEW_OCEAN && owner[s] != owner[k])) ridge[k] = true;
            }
        }
        float[] dRidge = new float[g.n];
        g.nearest(ridge, near, dRidge);
        boolean[] isNew = new boolean[g.n];
        for (int k = 0; k < g.n; k++) isNew[k] = crust[k] == NEW_OCEAN;
        dNew = new float[g.n];
        g.nearest(isNew, near, dNew);
        age = new float[g.n];
        for (int k = 0; k < g.n; k++) {
            if (crust[k] == NEW_OCEAN) {
                double dr = Math.min(dRidge[k], 1e5), de = dEdge[k];
                age[k] = (float) (tSim * dr / Math.max(1, dr + de));
            } else if (crust[k] == OLD_OCEAN) {
                double dn = Math.min(dNew[k], 1e5);
                age[k] = (float) Math.min(180, tSim + 55 * (1 - Math.exp(-dn / 1500)));
            } else {
                age[k] = Float.NaN;
            }
        }

        // ---- continental crust: signed distance to its edge -----------------------------------------------
        boolean[] cont = new boolean[g.n], notCont = new boolean[g.n];
        for (int k = 0; k < g.n; k++) { cont[k] = crust[k] == CONT; notCont[k] = !cont[k]; }
        float[] dIn = new float[g.n], dOut = new float[g.n];
        g.nearest(notCont, near, dIn);
        g.nearest(cont, near, dOut);
        sdCont = new float[g.n];
        for (int k = 0; k < g.n; k++) sdCont[k] = cont[k] ? Math.min(dIn[k], 4000) : -Math.min(dOut[k], 4000);

        // ---- subduction: trench line on the subducting plate along the edge of the overriding one -----------
        boolean[] trench = new boolean[g.n];
        float[] strength = new float[g.n];
        byte[] over = new byte[g.n], under = new byte[g.n];
        for (int j = 0; j < g.h; j++) {
            for (int i = 0; i < g.w; i++) {
                int k = j * g.w + i;
                int[] nbs = {g.idx(i + 1, j), g.idx(i - 1, j), j > 0 ? k - g.w : k, j + 1 < g.h ? k + g.w : k};
                for (int nb : nbs) {
                    if (lost[nb] >= 0 && lost[nb] == owner[k] && owner[nb] != owner[k]) {
                        trench[k] = true;
                        over[k] = owner[nb];
                        under[k] = owner[k];
                        // convergence: how far apart the two plates' time-0 positions of this point are
                        double[] a = apply(rotInv[owner[nb]], g.vx[k], g.vy[k], g.vz[k]), b = apply(rotInv[owner[k]], g.vx[k], g.vy[k], g.vz[k]);
                        double km = Grid.R_KM * Math.acos(Math.max(-1, Math.min(1, dot(a, b))));
                        strength[k] = (float) Math.max(0.35, Math.min(1.0, km / 1800));
                        break;
                    }
                }
            }
        }
        dTrench = new float[g.n];
        g.nearest(trench, near, dTrench);
        arc = new float[g.n];
        arcSide = new byte[g.n];
        for (int k = 0; k < g.n; k++) {
            int t = near[k];
            if (t < 0) { dTrench[k] = Float.POSITIVE_INFINITY; continue; }
            arc[k] = strength[t];
            arcSide[k] = owner[k] == over[t] ? (byte) 1 : owner[k] == under[t] ? (byte) 2 : 0;
        }

        // ---- collisions ----------------------------------------------------------------------------------------
        dColl = new float[g.n];
        g.nearest(collision, near, dColl);
        boolean[] notColl = new boolean[g.n];
        for (int k = 0; k < g.n; k++) notColl[k] = !collision[k];
        dCollIn = new float[g.n];
        g.nearest(notColl, near, dCollIn);
        for (int k = 0; k < g.n; k++) if (!collision[k]) dCollIn[k] = 0;
        coll = new float[g.n];
        for (int k = 0; k < g.n; k++) coll[k] = collision[k] ? 1f : 0f;
        float[] collB = g.blur(coll, 120);
        System.arraycopy(collB, 0, coll, 0, g.n);

        // ---- old sutures / rift shoulders: time-0 boundary distance in the owner's frame -------------------------
        dP0 = new float[g.n];
        for (int k = 0; k < g.n; k++) {
            if (crust[k] != CONT) { dP0[k] = Float.POSITIVE_INFINITY; continue; }
            double[] p0 = apply(rotInv[owner[k]], g.vx[k], g.vy[k], g.vz[k]);
            dP0[k] = g.sample(d0, p0[0], p0[1], p0[2]);
        }

        // ---- old interior belts: a few great-circle arcs inside the supercontinent, carried with their plates ----
        int nOld = 1 + r.nextInt(3);
        double[][] arcA = new double[nOld][], arcB = new double[nOld][], arcN = new double[nOld][];
        for (int q = 0; q < nOld; q++) {
            double[] c;
            int tries = 0;
            do { c = randomUnit(r); tries++; } while (!crust0[g.cellOf(c[0], c[1], c[2])] && tries < 5000);
            double[] dir = normalize(cross(c, randomUnit(r)));
            if (dir == null) dir = normalize(cross(c, new double[]{0, 0, 1}));
            double half = (600 + 1300 * r.nextDouble()) / Grid.R_KM;
            double[] ax2 = normalize(cross(c, dir));
            arcA[q] = rotateAbout(c, ax2, -half);
            arcB[q] = rotateAbout(c, ax2, half);
            arcN[q] = ax2;
        }
        dOld = new float[g.n];
        for (int k = 0; k < g.n; k++) {
            if (crust[k] != CONT) { dOld[k] = Float.POSITIVE_INFINITY; continue; }
            double[] p0 = apply(rotInv[owner[k]], g.vx[k], g.vy[k], g.vz[k]);
            double best = Double.MAX_VALUE;
            for (int q = 0; q < nOld; q++) best = Math.min(best, arcDistKm(p0, arcA[q], arcB[q], arcN[q]));
            dOld[k] = (float) best;
        }

        // ---- hotspots: volcano chains dragged out along the plate motion ------------------------------------------
        int hot = 4 + r.nextInt(7);
        for (int q = 0; q < hot; q++) {
            double[] hs = randomUnit(r);
            int k = g.cellOf(hs[0], hs[1], hs[2]);
            int p = owner[k];
            double rate = angle[p] / tSim;                    // rad per Myr
            double chainMyr = Math.min(tSim, 30 + 50 * r.nextDouble());
            double strengthH = 0.6 + 0.6 * r.nextDouble();
            for (double t = 0; t <= chainMyr; t += 1.2 + 2.2 * r.nextDouble()) {
                double[] c = apply(rotation(axis[p], rate * t), hs[0], hs[1], hs[2]);
                double hM = strengthH * (2500 + 6500 * Math.exp(-t / 14.0)) * (0.7 + 0.5 * r.nextDouble());
                double radKm = 25 + 35 * Math.exp(-t / 30.0) * (0.6 + 0.6 * r.nextDouble());
                cones.add(new double[]{c[0], c[1], c[2], hM, radKm});
            }
        }
    }

    // ---- vector helpers ----------------------------------------------------------------------------------

    static double[] randomUnit(SplittableRandom r) {
        double z = 2 * r.nextDouble() - 1, t = 2 * Math.PI * r.nextDouble(), s = Math.sqrt(1 - z * z);
        return new double[]{s * Math.cos(t), s * Math.sin(t), z};
    }

    private static boolean tooClose(double[] v, double[][] seeds, int count, double minRad) {
        for (int i = 0; i < count; i++) if (Math.acos(Math.max(-1, Math.min(1, dot(v, seeds[i])))) < minRad) return true;
        return false;
    }

    static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    static double[] cross(double[] a, double[] b) {
        return new double[]{a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    static double[] normalize(double[] a) {
        double l = Math.sqrt(dot(a, a));
        return l < 1e-9 ? null : new double[]{a[0] / l, a[1] / l, a[2] / l};
    }

    /** Rodrigues rotation matrix (row-major 3x3) for a turn of {@code ang} radians about unit {@code ax}. */
    static double[] rotation(double[] ax, double ang) {
        double c = Math.cos(ang), s = Math.sin(ang), t = 1 - c, x = ax[0], y = ax[1], z = ax[2];
        return new double[]{
                t * x * x + c, t * x * y - s * z, t * x * z + s * y,
                t * x * y + s * z, t * y * y + c, t * y * z - s * x,
                t * x * z - s * y, t * y * z + s * x, t * z * z + c};
    }

    static double[] apply(double[] m, double x, double y, double z) {
        return new double[]{m[0] * x + m[1] * y + m[2] * z, m[3] * x + m[4] * y + m[5] * z, m[6] * x + m[7] * y + m[8] * z};
    }

    /** Great-circle distance (km) from unit {@code p} to the arc a-b (n = unit normal of its great circle). */
    static double arcDistKm(double[] p, double[] a, double[] b, double[] n) {
        double pn = dot(p, n);
        double[] q = normalize(new double[]{p[0] - pn * n[0], p[1] - pn * n[1], p[2] - pn * n[2]});
        double ab = Math.acos(Math.max(-1, Math.min(1, dot(a, b))));
        if (q != null) {
            double aq = Math.acos(Math.max(-1, Math.min(1, dot(a, q)))), qb = Math.acos(Math.max(-1, Math.min(1, dot(q, b))));
            if (Math.abs(aq + qb - ab) < 1e-6) return Grid.R_KM * Math.asin(Math.min(1, Math.abs(pn)));
        }
        double da = Math.acos(Math.max(-1, Math.min(1, dot(p, a)))), db = Math.acos(Math.max(-1, Math.min(1, dot(p, b))));
        return Grid.R_KM * Math.min(da, db);
    }

    static double[] rotateAbout(double[] v, double[] ax, double ang) {
        return apply(rotation(ax, ang), v[0], v[1], v[2]);
    }
}
