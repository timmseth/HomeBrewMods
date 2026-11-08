package com.seth.worldbound.planet;

import java.util.stream.IntStream;

/**
 * A planet's climate from its geography, by the physics that shapes Earth's climate map, not by painting bands:
 *
 * <ul>
 * <li>Temperature: a sea-level profile by latitude, cooled with height (6.5 °C/km), a yearly swing that grows with
 * latitude and with how far the prevailing wind has come over land (continentality: Yakutsk vs Bergen), and coastal
 * currents: cold upwelling off subtropical west coasts, warm currents off east coasts, a warm drift on high-latitude
 * west coasts.</li>
 * <li>Pressure and wind for January and July: the zonal belts (equatorial trough, subtropical highs strongest on the
 * eastern side of each ocean, subpolar lows, polar highs) shifted with the sun, plus thermal lows over summer
 * continents and highs over winter ones. Wind blows down the pressure gradient turned by Coriolis.</li>
 * <li>Rain: every land cell traces its air back 2,800 km upwind; the air picks up moisture over warm sea, rains it
 * out where it rises (low pressure, storm tracks, up mountain sides) and dries where it sinks (subtropical highs,
 * lee slopes, over cold upwelling), with part of the rain recycled by the land. That gives rain forests, monsoons,
 * Mediterranean summers, deserts in the subtropics, continental interiors and rain shadows.</li>
 * </ul>
 *
 * Output per cell of a coarse grid (~40 km): sea-level annual mean temperature, annual range, annual precipitation and
 * the share of it falling in the summer half-year — enough for Köppen at any resolution with the local elevation.
 */
final class Climate {
    static final int JAN = 0, JUL = 1;

    final Grid g;
    final boolean[] land;
    final float[] elev;
    /** Annual mean temperature reduced to sea level, °C (land: with continentality and currents; sea: SST). */
    final float[] t0;
    /** Warmest minus coldest month, °C. */
    final float[] range;
    /** Annual precipitation, mm. */
    final float[] map;
    /** Fraction of the annual precipitation that falls in the local summer half-year. */
    final float[] summer;
    /** Continentality 0..1 (how far the air has come over land). */
    final float[] cont;
    final float[][] windU = new float[2][], windV = new float[2][], press = new float[2][], precip = new float[2][];

    // ---- tunables (calibrated against Earth's own Köppen map, see ClimateCalibrate) ----
    static double E0 = 0.004, TAU = 5.0, LAND_ZONAL = 0.6, DRY_HEAT = 0.0, E_STORM = 0.005, E_POLAR = 0.004, K_ORO = 0.0004, K_LEE = 0.045, RELAX = 0.05, RECYC = 0.25, P_SCALE = 2800;
    static double EXCESS = 0.4, CLIM_WIND = 0.15, GRAD_REF = 3.0, K_SEA = 0.8, DIFF = 0.3;
    static int ITERS = 130;
    static double TAU_HIGH = 2.0, K_HIGH = 0.6, K_LOW = 1.2, BASIN_HIGH = 4.0, STAB = 0.75, RAIN_LOW = 1.3, RAIN_HIGH = 0.3, CONT_KM = 1200, COAST_KM = 600;
    static final double STEP_KM = 35;
    static final int STEPS = 80;

    // sea-level maritime annual mean (°C) and yearly range over the sea / deep inland, by |latitude| every 10°
    static final double[] T_SEA = {27, 27, 25, 20.5, 15, 9.5, 4, -3, -12, -18};
    static final double[] D_CONT = {0, 1, 2, 1.5, -1, -2.5, -4.5, -6.5, -7, -5};
    static final double[] R_SEA = {1.5, 2, 3.5, 5.5, 7, 7.5, 7, 12, 22, 28};
    static final double[] R_LAND = {2.5, 5, 10, 18, 30, 37, 45, 50, 46, 42};

    static double lerpLat(double[] t, double absLat) {
        double x = Math.min(9, Math.max(0, absLat / 10));
        int i = (int) Math.floor(x);
        if (i >= 9) return t[9];
        double f = x - i;
        return t[i] * (1 - f) + t[i + 1] * f;
    }

    static double g(double x, double mu, double sigma) {
        double d = (x - mu) / sigma;
        return Math.exp(-d * d);
    }

    static double smooth(double e0, double e1, double x) {
        double t = Math.max(0, Math.min(1, (x - e0) / (e1 - e0)));
        return t * t * (3 - 2 * t);
    }

    /** Saturation moisture relative to 0 °C (Magnus formula). */
    static double qsat(double t) {
        return Math.exp(17.27 * t / (t + 237.3));
    }

    /** A climate read back from a planet cache (no recomputation). */
    private Climate(Grid g, boolean[] land, float[] elev, float[] t0, float[] range, float[] map, float[] summer, float[] cont) {
        this.g = g;
        this.land = land;
        this.elev = elev;
        this.t0 = t0;
        this.range = range;
        this.map = map;
        this.summer = summer;
        this.cont = cont;
    }

    static Climate restore(Grid g, boolean[] land, float[] elev, float[] t0, float[] range, float[] map, float[] summer, float[] cont) {
        return new Climate(g, land, elev, t0, range, map, summer, cont);
    }

    Climate(Grid g, float[] elevMean, float[] landFrac) {
        this.g = g;
        int n = g.n;
        land = new boolean[n];
        elev = new float[n];
        for (int k = 0; k < n; k++) {
            land[k] = landFrac[k] >= 0.5f;
            elev[k] = land[k] ? Math.max(0, elevMean[k]) : 0;
        }
        t0 = new float[n];
        range = new float[n];
        map = new float[n];
        summer = new float[n];
        cont = new float[n];

        // ---- distances to the sea / to land along each row, and to the nearest sea ----------------------------
        float[] dWestSea = new float[n], dEastSea = new float[n], dWestLand = new float[n], dEastLand = new float[n];
        rowScan(dWestSea, dEastSea, false);
        rowScan(dWestLand, dEastLand, true);
        boolean[] sea = new boolean[n];
        for (int k = 0; k < n; k++) sea[k] = !land[k];
        float[] dSea = new float[n];
        g.nearest(sea, new int[n], dSea);

        // ---- sea surface temperature with currents -----------------------------------------------------------
        float[] sstAnom = new float[n];
        for (int j = 0; j < g.h; j++) {
            double al = Math.abs(g.lat[j]);
            double band1 = smooth(8, 15, al) * (1 - smooth(35, 45, al)), band2 = smooth(40, 50, al) * (1 - smooth(65, 75, al));
            for (int i = 0; i < g.w; i++) {
                int k = j * g.w + i;
                if (land[k]) continue;
                double e = dEastLand[k], w = dWestLand[k];
                // boundary currents need a wide ocean behind them (the Arabian Sea has no California Current)
                double wide1 = smooth(2500, 5000, w), wide2 = smooth(2500, 5000, e);
                sstAnom[k] = (float) (band1 * (-5 * Math.exp(-e / 700) * wide1 + 3 * Math.exp(-w / 900) * wide2)
                        + band2 * (6 * Math.exp(-e / 1500) - 4 * Math.exp(-w / 900)));
            }
        }
        // what the coast feels of the current offshore
        float[] seaW = new float[n], anomW = new float[n];
        for (int k = 0; k < n; k++) { seaW[k] = land[k] ? 0 : 1; anomW[k] = land[k] ? 0 : sstAnom[k]; }
        float[] seaB = g.blur(seaW, COAST_KM), anomB = g.blur(anomW, COAST_KM);
        float[] coastAnom = new float[n];
        for (int k = 0; k < n; k++) coastAnom[k] = seaB[k] > 0.01f ? anomB[k] / seaB[k] * Math.min(1, seaB[k] * 3) : 0;

        // ---- first guess of continentality from plain distance to the sea; refined from upwind traces -----------
        for (int k = 0; k < n; k++) cont[k] = land[k] ? (float) (1 - Math.exp(-dSea[k] / 500)) : 0;
        float[] dryness = new float[n];
        dry = dryness;
        for (int pass = 0; pass < 3; pass++) {
            temperatures(sstAnom, coastAnom, dryness);
            for (int s = 0; s < 2; s++) pressureAndWind(s, dEastLand, dWestLand);
            float[] upLand = new float[n];
            sstA = sstAnom;
            for (int s = 0; s < 2; s++) rainField(s, upLand);
            if (pass < 2) {
                // continentality: distance the air has travelled over land (winter counts most)
                for (int k = 0; k < n; k++) if (land[k]) cont[k] = (float) (1 - Math.exp(-upLand[k] / CONT_KM));
            }
            // deserts run hot (dry air, clear skies), wet tropics don't
            for (int k = 0; k < n; k++) {
                if (!land[k]) continue;
                double mm = 6 * (precip[JAN][k] + precip[JUL][k]) * P_SCALE;
                dryness[k] = (float) (1 - Math.min(1, mm / 700));
            }
        }
        temperatures(sstAnom, coastAnom, dryness);
        for (int k = 0; k < n; k++) {
            int j = k / g.w;
            boolean north = g.lat[j] >= 0;
            double pj = precip[JAN][k], pl = precip[JUL][k];
            map[k] = (float) (6 * (pj + pl) * P_SCALE);
            double ps = north ? pl : pj, tot = pj + pl;
            summer[k] = (float) (tot <= 1e-9 ? 0.5 : ps / tot);
        }
    }

    /** Distance (km) along the row to the nearest cell of the other kind, westward and eastward. */
    private void rowScan(float[] west, float[] east, boolean toLand) {
        IntStream.range(0, g.h).parallel().forEach(j -> {
            int w = g.w;
            double km = Math.max(5, g.kmX[j]);
            int base = j * w;
            // westward: walk east, remembering the last cell of the wanted kind
            int last = Integer.MIN_VALUE / 2;
            for (int s = 0; s < 2 * w; s++) {
                int i = s % w;
                if (land[base + i] == toLand) last = s;
                if (s >= w) west[base + i] = (float) Math.min(8000, (s - last) * km);
            }
            last = Integer.MAX_VALUE / 2;
            for (int s = 2 * w - 1; s >= 0; s--) {
                int i = s % w;
                if (land[base + i] == toLand) last = s;
                if (s < w) east[base + i] = (float) Math.min(8000, (last - s) * km);
            }
        });
    }

    private float[] tLand0;   // sea-level annual mean for land (before season)
    private float[] dry;      // 0 wet .. 1 desert, from the previous pass

    /** Annual mean (sea level) and range for every cell. */
    private void temperatures(float[] sstAnom, float[] coastAnom, float[] dryness) {
        tLand0 = new float[g.n];
        for (int j = 0; j < g.h; j++) {
            double al = Math.abs(g.lat[j]);
            double tSea = lerpLat(T_SEA, al), dCont = lerpLat(D_CONT, al), rSea = lerpLat(R_SEA, al), rLand = lerpLat(R_LAND, al);
            double desertBand = smooth(8, 16, al) * (1 - smooth(34, 42, al));
            for (int i = 0; i < g.w; i++) {
                int k = j * g.w + i;
                if (!land[k]) {
                    t0[k] = (float) (tSea + sstAnom[k]);
                    range[k] = (float) rSea;
                    continue;
                }
                double c = cont[k];
                double t = tSea + dCont * c + coastAnom[k] * (1 - 0.7 * c) + 3.5 * dryness[k] * desertBand;
                t0[k] = (float) t;
                tLand0[k] = (float) t;
                // cold currents also flatten the yearly swing (San Francisco, Lima)
                // wet tropics barely have seasons; deserts swing more
                double rl = rLand * (1 - (1 - smooth(25, 38, al)) * 0.45 * (1 - dryness[k]));
                double r = rSea + Math.max(0, rl - rSea) * c;
                if (coastAnom[k] < 0) r *= 1 + 0.08 * coastAnom[k] * (1 - c);
                range[k] = (float) Math.max(1, r);
            }
        }
    }

    /** Temperature of a cell in the given season, at its own height (land) or SST (sea). */
    /** +1 in the summer hemisphere, -1 in the winter one, easing through zero across the equator (no seam). */
    double seasonSign(int j, int s) {
        double v = Math.max(-1, Math.min(1, g.lat[j] / 8.0));
        return s == JUL ? v : -v;
    }

    double seasonTemp(int k, int s) {
        int j = k / g.w;
        double sign = seasonSign(j, s);
        if (!land[k]) return t0[k] + sign * range[k] * 0.3;
        return t0[k] + sign * range[k] / 2 - 0.0065 * elev[k];
    }

    /** Season temperature at sea level (no lapse), for the pressure pattern. */
    private double seasonTemp0(int k, int s) {
        int j = k / g.w;
        double sign = seasonSign(j, s);
        return land[k] ? t0[k] + sign * range[k] / 2 : t0[k] + sign * range[k] * 0.3;
    }

    private void pressureAndWind(int s, float[] dEastLand, float[] dWestLand) {
        int n = g.n;
        double shift = s == JUL ? 5 : -5, shiftP = s == JUL ? 2 : -2;
        float[] thermal = new float[n], dyn = new float[n];
        for (int j = 0; j < g.h; j++) {
            double lat = g.lat[j], al = Math.abs(lat), x = lat - shift, xp = lat - shiftP;
            double highs = 6.5 * (g(x, 31, 10) + g(x, -31, 10));
            double pz = -6 * g(x, 0, 11) + highs - 9 * (g(xp, 63, 9) + g(xp, -63, 9)) + 4 * (g(lat, 90, 10) + g(lat, -90, 10));
            double basinBand = smooth(12, 20, al) * (1 - smooth(38, 46, al));
            // land-sea contrast: each cell against the mean of its latitude this season; warmer than its row
            // draws a low (summer continents, winter oceans: the Aleutian and Icelandic lows), colder builds a
            // high (winter continents: the Siberian High; summer oceans: the Azores and Pacific highs)
            double mean = 0;
            for (int i = 0; i < g.w; i++) mean += seasonTemp0(j * g.w + i, s);
            mean /= g.w;
            for (int i = 0; i < g.w; i++) {
                int k = j * g.w + i;
                double p = pz;
                double d = seasonTemp0(k, s) - mean;
                double kk = d > 0 ? K_LOW : K_HIGH;
                if (!land[k]) {
                    // subtropical highs sit toward the eastern side of each ocean
                    p += basinBand * (BASIN_HIGH * Math.exp(-dEastLand[k] / 2500) - 1.0 * Math.exp(-dWestLand[k] / 1500));
                    thermal[k] = (float) (-kk * d * K_SEA);
                } else {
                    // the zonal belts are an ocean thing; over summer land the heating of the ground rules (the
                    // subtropical high doesn't sit over Georgia or Shanghai in July)
                    boolean summerHemi = (s == JUL) == (lat >= 0);
                    if (summerHemi) p -= highs * (1 - LAND_ZONAL);
                    if (d > 0) d *= 1 + DRY_HEAT * dry[k]; // dry ground heats hardest: the Thar and Saharan heat lows
                    thermal[k] = (float) (-kk * d * (0.3 + 0.7 * cont[k]));
                }
                dyn[k] = (float) p;
            }
        }
        thermal = g.blur(thermal, 700);
        float[] p = new float[n];
        for (int k = 0; k < n; k++) p[k] = dyn[k] + thermal[k];
        p = g.blur(p, 350);
        press[s] = p;
        thermalP[s] = thermal;
        dynP[s] = dyn;
        // convergence of the surface wind: friction turns the flow partly down the gradient, so air piles into lows
        // in proportion to the pressure field's Laplacian (cos of the turning angle)
        float[] conv = new float[n];
        for (int j = 0; j < g.h; j++) {
            double lat = g.lat[j];
            double ca = Math.cos(Math.toRadians(60) * Math.min(1, Math.abs(lat) / 12.0));
            double kmx = Math.max(15, g.kmX[j]) / 1000, kmy = g.kmY / 1000;
            int jn = Math.max(0, j - 1), js = Math.min(g.h - 1, j + 1);
            for (int i = 0; i < g.w; i++) {
                int k = j * g.w + i;
                double lx = (p[g.idx(i + 1, j)] - 2 * p[k] + p[g.idx(i - 1, j)]) / (kmx * kmx);
                double ly = (p[jn * g.w + i] - 2 * p[k] + p[js * g.w + i]) / (kmy * kmy);
                conv[k] = (float) (ca * (lx + ly));
            }
        }
        convP[s] = g.blur(conv, 200);
        float[] u = new float[n], v = new float[n];
        for (int j = 0; j < g.h; j++) {
            double lat = g.lat[j];
            double ang = Math.toRadians(60) * Math.min(1, Math.abs(lat) / 12.0);
            double ca = Math.cos(ang), sa = Math.sin(ang);
            boolean north = lat >= 0;
            double kmx = Math.max(15, g.kmX[j]);
            for (int i = 0; i < g.w; i++) {
                int k = j * g.w + i;
                double dpx = (p[g.idx(i + 1, j)] - p[g.idx(i - 1, j)]) / (2 * kmx);
                int jn = Math.max(0, j - 1), js = Math.min(g.h - 1, j + 1);
                double dpy = (p[jn * g.w + i] - p[js * g.w + i]) / ((js - jn) * g.kmY);
                double fx = -dpx * 1000 / GRAD_REF, fy = -dpy * 1000 / GRAD_REF; // toward low pressure, ~1 for a typical gradient
                double wx, wy;
                if (north) { wx = fx * ca + fy * sa; wy = -fx * sa + fy * ca; }   // turned clockwise
                else { wx = fx * ca - fy * sa; wy = fx * sa + fy * ca; }          // counter-clockwise
                // the mean zonal flow the pressure pattern rides on: trades, westerlies, polar easterlies (it keeps
                // the trades blowing from the east right across the equator)
                double al = Math.abs(lat);
                double uc = al < 30 ? -1 + smooth(22, 30, al) : al < 62 ? smooth(30, 38, al) * (1 - smooth(55, 62, al)) : -0.5 * smooth(62, 70, al);
                wx += CLIM_WIND * uc;
                double l = Math.hypot(wx, wy);
                if (l < 1e-9) { wx = -1; wy = 0; l = 1; }
                u[k] = (float) (wx / l);
                v[k] = (float) (wy / l);
            }
        }
        windU[s] = u;
        windV[s] = v;
    }

    private final float[][] thermalP = new float[2][], dynP = new float[2][], convP = new float[2][];

    /** Large-scale rain efficiency: rising air in lows and storm tracks, sinking air in highs. */
    private double effBase(int k, int s) {
        int j = k / g.w;
        double al = Math.abs(g.lat[j]);
        boolean winterHemi = (s == JUL) != (g.lat[j] >= 0);
        double th = thermalP[s][k];
        // a heat low over wet ground is a monsoon trough; over a desert it is shallow and the sinking air above it wins
        double pa = dynP[s][k] + (th < 0 ? RAIN_LOW * (1 - dry[k]) : RAIN_HIGH) * th;
        // rising air rains, sinking air (the subtropical highs) shuts rain down hard
        double e = E0 * Math.exp(pa < 0 ? -Math.max(-14, pa) / TAU : -Math.min(25, pa) / TAU_HIGH);
        double st = winterHemi ? 40 : 52;
        e += E_STORM * g(al, st, 12) + E_POLAR * g(al, 62, 12);
        return e;
    }

    private float[] sstA;

    private void rain(int s, float[] sstAnom, float[] dSea, float[] upLand, boolean last) {
        sstA = sstAnom;
        int n = g.n;
        float[] out = new float[n];
        IntStream.range(0, g.h).parallel().forEach(j -> {
            int[] path = new int[STEPS + 1];
            double[] res = new double[2];
            for (int i = 0; i < g.w; i++) {
                int k0 = j * g.w + i;
                if (!land[k0]) continue;
                trace(k0, s, path, res, null);
                out[k0] = (float) res[0];
                upLand[k0] += (float) (res[1] * (s == JAN == (g.lat[j] >= 0) ? 0.65 : 0.35));
            }
        });
        precip[s] = out;
    }

    /**
     * Steady-state moisture for one season on the whole grid: each iteration carries the moisture one step (35 km)
     * downwind (semi-Lagrangian, bilinear), tops it up over the sea toward saturation, rains part of it out over land
     * (recycling some), and mixes it with its neighbours (the transport by passing weather systems a mean wind
     * misses). 130 iterations reach ~4,500 km inland. Also carries the distance the air has travelled over land.
     */
    private void rainField(int s, float[] upLand) {
        int n = g.n, w = g.w;
        float[] u = windU[s], v = windV[s];
        // upwind sample point of every cell: 4 neighbours + weights
        int[] ui = new int[n * 4];
        float[] uw = new float[n * 4];
        float[] eff = new float[n], cap = new float[n], rec = new float[n], sat = new float[n], zUp = new float[n];
        for (int j = 0; j < g.h; j++) {
            for (int i = 0; i < w; i++) {
                int k = j * w + i;
                double cl = Math.max(0.12, Math.cos(Math.toRadians(g.lat[j])));
                double lon = g.lon[i] - u[k] * STEP_KM / (111.32 * cl), lat = g.lat[j] - v[k] * STEP_KM / 110.57;
                lat = Math.max(-89.9, Math.min(89.9, lat));
                double x = (lon + 180.0) * w / 360.0 - 0.5, y = (90.0 - lat) * g.h / 180.0 - 0.5;
                int x0 = (int) Math.floor(x), y0 = (int) Math.floor(y);
                double tx = x - x0, ty = y - y0;
                int y1 = Math.min(g.h - 1, y0 + 1);
                if (y0 < 0) { y0 = 0; ty = 0; }
                int xa = Math.floorMod(x0, w), xb = Math.floorMod(x0 + 1, w);
                ui[k * 4] = y0 * w + xa; uw[k * 4] = (float) ((1 - tx) * (1 - ty));
                ui[k * 4 + 1] = y0 * w + xb; uw[k * 4 + 1] = (float) (tx * (1 - ty));
                ui[k * 4 + 2] = y1 * w + xa; uw[k * 4 + 2] = (float) ((1 - tx) * ty);
                ui[k * 4 + 3] = y1 * w + xb; uw[k * 4 + 3] = (float) (tx * ty);
                double zu = 0;
                for (int q = 0; q < 4; q++) zu += uw[k * 4 + q] * elev[ui[k * 4 + q]];
                zUp[k] = (float) zu;
                double tc = seasonTemp(k, s);
                if (land[k]) {
                    double e = effBase(k, s);
                    double up = (elev[k] - zu) / STEP_KM;
                    e += K_ORO * Math.max(0, up);
                    if (up < 0) e *= Math.exp(K_LEE * up);
                    eff[k] = (float) Math.min(0.8, e);
                    double tsl = tc + 0.0065 * elev[k];
                    if (tsl < 0) tsl *= 0.65;
                    cap[k] = (float) (qsat(tsl) * Math.exp(-elev[k] / 4000));
                    rec[k] = (float) (RECYC * smooth(-5, 15, tc) * (1 - 0.75 * dry[k]));
                } else {
                    sat[k] = (float) qsat(tc);
                    eff[k] = (float) Math.min(0.8, effBase(k, s));
                }
            }
        }
        // cold upwelling: stable air for a few hundred km downwind of a cold coast
        float[] stab = new float[n];
        for (int k = 0; k < n; k++) if (!land[k]) stab[k] = (float) Math.max(0, -sstA[k] / 5);
        float[] m = new float[n], m2 = new float[n], p = new float[n], dl = new float[n], dl2 = new float[n], st2 = new float[n];
        for (int k = 0; k < n; k++) m[k] = land[k] ? 0.3f * (float) qsat(seasonTemp(k, s)) : sat[k];
        float decay = (float) Math.exp(-STEP_KM / 300);
        for (int it = 0; it < ITERS; it++) {
            final float[] mi = m, mo = m2, di = dl, dout = dl2, si = stab, so = st2;
            final boolean lastIt = it == ITERS - 1;
            IntStream.range(0, g.h).parallel().forEach(j -> {
                for (int i = 0; i < w; i++) {
                    int k = j * w + i, b = k * 4;
                    double mv = 0, dv = 0, sv = 0;
                    for (int q = 0; q < 4; q++) {
                        int c = ui[b + q];
                        float wt = uw[b + q];
                        mv += wt * mi[c];
                        dv += wt * di[c];
                        sv += wt * si[c];
                    }
                    if (!land[k]) {
                        mv += (sat[k] - mv) * RELAX;
                        mo[k] = (float) mv;
                        dout[k] = 0;
                        so[k] = stab[k];
                        // rain at sea too (the sea is the source, so it isn't depleted): what an island would get
                        if (lastIt) p[k] = (float) (mv * eff[k] * (1 - STAB * stab[k]));
                        continue;
                    }
                    double e = eff[k] * (1 - STAB * sv);
                    double pr = mv * e;
                    if (mv - pr > cap[k]) pr += (mv - pr - cap[k]) * (elev[k] > zUp[k] + 5 ? EXCESS : 0.06);
                    mv -= pr * (1 - rec[k]);
                    mo[k] = (float) Math.max(0, mv);
                    dout[k] = (float) (dv + STEP_KM);
                    so[k] = (float) (sv * decay);
                    if (lastIt) p[k] = (float) pr;
                }
            });
            // eddy mixing
            IntStream.range(0, g.h).parallel().forEach(j -> {
                int jn = Math.max(0, j - 1), js = Math.min(g.h - 1, j + 1);
                for (int i = 0; i < w; i++) {
                    int k = j * w + i;
                    double avg = 0.25 * (mo[g.idx(i - 1, j)] + mo[g.idx(i + 1, j)] + mo[jn * w + i] + mo[js * w + i]);
                    mi[k] = (float) (mo[k] + DIFF * (avg - mo[k]));
                    di[k] = dout[k];
                    si[k] = so[k];
                }
            });
        }
        precip[s] = p;
        boolean[] dummy = null;
        for (int k = 0; k < n; k++) {
            if (!land[k]) continue;
            int j = k / w;
            upLand[k] += (float) (Math.min(6000, dl[k]) * (s == JAN == (g.lat[j] >= 0) ? 0.65 : 0.35));
        }
    }

    /** One land cell's air for one season: res[0] = precipitation (model units), res[1] = km travelled over land. */
    void trace(int k0, int s, int[] path, double[] res, StringBuilder log) {
        float[] u = windU[s], v = windV[s];
        int j0 = k0 / g.w, i0 = k0 % g.w;
        double lon = g.lon[i0], lat = g.lat[j0];
        int len = 0, seaRun = 0;
        path[len++] = k0;
        for (int st = 0; st < STEPS; st++) {
            int k = path[len - 1];
            double cl = Math.max(0.12, Math.cos(Math.toRadians(lat)));
            lon -= u[k] * STEP_KM / (111.32 * cl);
            lat -= v[k] * STEP_KM / 110.57;
            if (lat > 89.5) { lat = 179 - lat; lon += 180; }
            if (lat < -89.5) { lat = -179 - lat; lon += 180; }
            lon = ((lon + 180) % 360 + 360) % 360 - 180;
            int kk = cellAt(lon, lat);
            path[len++] = kk;
            seaRun = land[kk] ? 0 : seaRun + 1;
            if (seaRun >= 14) break;
        }
        double overLand = 0;
        for (int q = 1; q < len; q++) {
            if (!land[path[q]]) break;
            overLand += STEP_KM;
        }
        if (len == STEPS + 1 && land[path[len - 1]]) overLand += 1500;
        int far = path[len - 1];
        double m = land[far] ? 0.35 * qsat(seasonTemp(far, s)) : qsat(seasonTemp(far, s));
        double stab = 0, sinceSea = 1e9, p = 0, zPrev = 0;
        double p1 = -1, p2 = -1;
        for (int q = len - 2; q >= 0; q--) {
            int k = path[q], prev = path[q + 1];
            if (!land[k]) {
                double tq = seasonTemp(k, s);
                m += (qsat(tq) - m) * RELAX;
                stab = Math.max(0, -sstA[k] / 5);
                sinceSea = 0;
                p = 0;
                p1 = p2 = -1;
                zPrev = 0;
                if (log != null) log.append(String.format(java.util.Locale.ROOT, "  sea  %6.1f %6.1f T %5.1f m %.2f%n", g.lat[k / g.w], g.lon[k % g.w], tq, m));
                continue;
            }
            sinceSea += STEP_KM;
            double tc = seasonTemp(k, s);
            // moisture the column can hold: by its sea-level temperature (winter inversions keep the air aloft
            // warmer than the frozen ground), less over high ground
            double tsl = tc + 0.0065 * elev[k];
            if (tsl < 0) tsl *= 0.65;
            double cap = qsat(tsl) * Math.exp(-elev[k] / 4000);
            double e = effBase(k, s);
            double up = (elev[k] - (land[prev] ? elev[prev] : 0)) / STEP_KM;
            e += K_ORO * Math.max(0, up);
            if (up < 0) e *= Math.exp(K_LEE * up);
            e *= 1 - STAB * stab * Math.exp(-sinceSea / 300);
            p = m * Math.min(0.8, e);
            // air forced up onto high ground drops what the thinner, colder column can't hold, on the climb
            if (m - p > cap) p += (m - p - cap) * (elev[k] > zPrev + 5 ? EXCESS : 0.06);
            zPrev = elev[k];
            double rec = RECYC * smooth(-5, 15, tc) * (1 - 0.75 * dry[k]);
            m -= p * (1 - rec);
            if (m < 0) m = 0;
            p2 = p1;
            p1 = p;
            if (log != null) log.append(String.format(java.util.Locale.ROOT, "  land %6.1f %6.1f z %5.0f T %5.1f e %.4f m %.2f p %.4f (%.0f mm/mo) P %.1f th %.1f cv %.1f%n",
                    g.lat[k / g.w], g.lon[k % g.w], elev[k], tc, e, m, p, p * P_SCALE, dynP[s][k], thermalP[s][k], convP[s][k]));
        }
        // what falls on a cell is the rain of the last ~100 km of its air's path, not one step's
        res[0] = p2 >= 0 ? (p + p1 + p2) / 3 : p1 >= 0 ? (p + p1) / 2 : p;
        res[1] = overLand;
    }

    private int cellAt(double lon, double lat) {
        int i = (int) Math.floor((lon + 180.0) * g.w / 360.0), j = (int) Math.floor((90.0 - lat) * g.h / 180.0);
        return Math.max(0, Math.min(g.h - 1, j)) * g.w + Math.floorMod(i, g.w);
    }

    // ---- Köppen ------------------------------------------------------------------------------------------

    private static final double[] KAPPA_FRAC = new double[801];

    static {
        // summer-half fraction for a von Mises month weighting of concentration kappa (-4..4)
        for (int a = 0; a <= 800; a++) {
            double kap = (a - 400) / 100.0, sum = 0, sum2 = 0;
            for (int m = 0; m < 12; m++) {
                int o = Math.floorMod(m - 6 + 6, 12) - 6; // offset from the warmest month, -6..5
                double w = Math.exp(kap * Math.cos(Math.PI * 2 * o / 12.0));
                sum += w;
                if (o >= -3 && o <= 2) sum2 += w;
            }
            KAPPA_FRAC[a] = sum2 / sum;
        }
    }

    static double kappaFor(double summerFrac) {
        int lo = 0, hi = 800;
        while (hi - lo > 1) {
            int mid = (lo + hi) >>> 1;
            if (KAPPA_FRAC[mid] < summerFrac) lo = mid; else hi = mid;
        }
        return (lo - 400) / 100.0;
    }

    /**
     * Köppen-Geiger class 1..30 (Beck et al. 2018 rules, the order of the climate layer's codes) for a place with
     * annual mean {@code t} (°C, at its own height), yearly range {@code r}, annual precipitation {@code p} (mm) and
     * summer-half share {@code sf}.
     */
    static int koppen(double t, double r, double p, double sf) {
        double kap = kappaFor(Math.max(0.02, Math.min(0.98, sf)));
        double[] tm = new double[12], pm = new double[12];
        double wsum = 0;
        for (int o = -6; o < 6; o++) {
            double c = Math.cos(Math.PI * 2 * o / 12.0);
            tm[o + 6] = t + r / 2 * c;
            pm[o + 6] = Math.exp(kap * c);
            wsum += pm[o + 6];
        }
        double thot = -1e9, tcold = 1e9, pdry = 1e9, psdry = 1e9, pwdry = 1e9, pswet = 0, pwwet = 0, ps = 0;
        int tmon10 = 0;
        for (int m = 0; m < 12; m++) {
            pm[m] = p * pm[m] / wsum;
            thot = Math.max(thot, tm[m]);
            tcold = Math.min(tcold, tm[m]);
            pdry = Math.min(pdry, pm[m]);
            if (tm[m] > 10) tmon10++;
            int o = m - 6;
            boolean sumr = o >= -3 && o <= 2;
            if (sumr) { psdry = Math.min(psdry, pm[m]); pswet = Math.max(pswet, pm[m]); ps += pm[m]; }
            else { pwdry = Math.min(pwdry, pm[m]); pwwet = Math.max(pwwet, pm[m]); }
        }
        if (thot <= 10) return thot > 0 ? 29 : 30;
        double pth = 2 * t + (ps >= 0.7 * p ? 28 : (p - ps) >= 0.7 * p ? 0 : 14);
        if (p < 10 * pth) {
            boolean desert = p < 5 * pth, hot = t >= 18;
            return desert ? (hot ? 4 : 5) : (hot ? 6 : 7);
        }
        if (tcold >= 18) {
            if (pdry >= 60) return 1;
            return pdry >= 100 - p / 25 ? 2 : 3;
        }
        boolean w = pwdry < pswet / 10, s = !w && psdry < 40 && psdry < pwwet / 3;
        int third;
        if (thot >= 22) third = 0;
        else if (tmon10 >= 4) third = 1;
        else if (tcold < -38) third = 3;
        else third = 2;
        if (tcold > 0) {
            if (third == 3) third = 2;
            int base = s ? 8 : w ? 11 : 14;
            return base + third;
        }
        int base = s ? 17 : w ? 21 : 25;
        return base + third;
    }
}
