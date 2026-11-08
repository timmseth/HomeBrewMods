package com.seth.worldbound.geo;

import java.util.List;

/**
 * Metres &lt;-&gt; block y (PLAN.md §3.3). Piecewise linear, monotonic, separate segments for land
 * and ocean, clamped to the world's y range.
 *
 * A segment is {topMetres, metresPerBlock}: the metres per block that apply from the previous
 * breakpoint up to topMetres (land) or down to -topMetres (ocean).
 */
public final class HeightCurve {
    public final int seaLevelY, minY, maxY;
    private final double[] landTop, landRate, oceanTop, oceanRate;
    private final double[] landYAtTop, oceanYAtTop; // block y reached at each breakpoint

    public HeightCurve(int seaLevelY, int minY, int maxY, List<double[]> land, List<double[]> ocean) {
        this.seaLevelY = seaLevelY;
        this.minY = minY;
        this.maxY = maxY;
        landTop = new double[land.size()]; landRate = new double[land.size()]; landYAtTop = new double[land.size()];
        double y = seaLevelY, prev = 0;
        for (int i = 0; i < land.size(); i++) {
            landTop[i] = land.get(i)[0]; landRate[i] = land.get(i)[1];
            y += (landTop[i] - prev) / landRate[i];
            landYAtTop[i] = y; prev = landTop[i];
        }
        oceanTop = new double[ocean.size()]; oceanRate = new double[ocean.size()]; oceanYAtTop = new double[ocean.size()];
        y = seaLevelY; prev = 0;
        for (int i = 0; i < ocean.size(); i++) {
            oceanTop[i] = ocean.get(i)[0]; oceanRate[i] = ocean.get(i)[1];
            y -= (oceanTop[i] - prev) / oceanRate[i];
            oceanYAtTop[i] = y; prev = oceanTop[i];
        }
    }

    /** The locked default: sea at -700, Everest at ~2020, ocean floor true to 6,000 m (y -1459), trenches cut 1 block per 150 m below that (Challenger Deep ~-1492). */
    public static HeightCurve defaults() {
        return new HeightCurve(-700, -1504, 2031,
                List.of(new double[]{3000, 3.0}, new double[]{8849, 3.4}),
                List.of(new double[]{200, 2.0}, new double[]{6000, 8.8}, new double[]{11034, 150}));
    }

    /** Fractional block y for an elevation in metres. */
    public double y(double metres) {
        double y;
        if (metres >= 0) {
            y = seaLevelY; double prev = 0;
            for (int i = 0; i < landTop.length; i++) {
                double seg = Math.min(metres, landTop[i]) - prev;
                if (seg <= 0) break;
                y += seg / landRate[i]; prev = landTop[i];
            }
            if (metres > landTop[landTop.length - 1]) y += (metres - landTop[landTop.length - 1]) / landRate[landRate.length - 1];
        } else {
            double d = -metres;
            y = seaLevelY; double prev = 0;
            for (int i = 0; i < oceanTop.length; i++) {
                double seg = Math.min(d, oceanTop[i]) - prev;
                if (seg <= 0) break;
                y -= seg / oceanRate[i]; prev = oceanTop[i];
            }
            if (d > oceanTop[oceanTop.length - 1]) y -= (d - oceanTop[oceanTop.length - 1]) / oceanRate[oceanRate.length - 1];
        }
        return Math.max(minY + 1, Math.min(maxY, y));
    }

    /** Inverse: metres for a block y (for /earth where and tests). */
    public double metres(double y) {
        if (y >= seaLevelY) {
            double m = 0, yPrev = seaLevelY, mPrev = 0;
            for (int i = 0; i < landTop.length; i++) {
                if (y <= landYAtTop[i]) return mPrev + (y - yPrev) * landRate[i];
                yPrev = landYAtTop[i]; mPrev = landTop[i];
            }
            return mPrev + (y - yPrev) * landRate[landRate.length - 1];
        } else {
            double yPrev = seaLevelY, mPrev = 0;
            for (int i = 0; i < oceanTop.length; i++) {
                if (y >= oceanYAtTop[i]) return -(mPrev + (yPrev - y) * oceanRate[i]);
                yPrev = oceanYAtTop[i]; mPrev = oceanTop[i];
            }
            return -(mPrev + (yPrev - y) * oceanRate[oceanRate.length - 1]);
        }
    }

    /** Metres per block at an elevation — the local vertical scale (for slope calculations). */
    public double metresPerBlockAt(double metres) {
        if (metres >= 0) {
            for (int i = 0; i < landTop.length; i++) if (metres <= landTop[i]) return landRate[i];
            return landRate[landRate.length - 1];
        }
        double d = -metres;
        for (int i = 0; i < oceanTop.length; i++) if (d <= oceanTop[i]) return oceanRate[i];
        return oceanRate[oceanRate.length - 1];
    }
}
