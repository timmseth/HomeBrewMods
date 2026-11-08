package com.seth.worldbound.planet;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * An equirectangular W x H grid over the whole sphere, laid out like the tile pyramid (cell centres at +0.5, row 0 at
 * the north pole, column 0 at 180° W), with the geometry the planet model needs: unit vectors, cell sizes, a nearest-
 * source distance transform on the sphere, latitude-aware blurs and bilinear sampling with wrap-around in longitude.
 */
public final class Grid {
    public static final double R_KM = 6371.0;

    public final int w, h, n;
    public final double[] lat, lon;          // degrees, per row / per column
    public final double[] cosLat, sinLat, cosLon, sinLon;
    public float[] vx, vy, vz;               // unit vector per cell (dropped after a planet is built: 100 MB at z2)
    public final double[] areaW;             // relative cell area per row (cos lat)
    public final double kmY;                 // cell height in km
    public final double[] kmX;               // cell width in km per row

    public Grid(int w, int h) {
        this.w = w;
        this.h = h;
        this.n = w * h;
        lat = new double[h];
        cosLat = new double[h];
        sinLat = new double[h];
        areaW = new double[h];
        kmX = new double[h];
        for (int j = 0; j < h; j++) {
            lat[j] = 90.0 - (j + 0.5) * 180.0 / h;
            double r = Math.toRadians(lat[j]);
            cosLat[j] = Math.cos(r);
            sinLat[j] = Math.sin(r);
            areaW[j] = cosLat[j];
            kmX[j] = 2 * Math.PI * R_KM * cosLat[j] / w;
        }
        kmY = Math.PI * R_KM / h;
        lon = new double[w];
        cosLon = new double[w];
        sinLon = new double[w];
        for (int i = 0; i < w; i++) {
            lon[i] = -180.0 + (i + 0.5) * 360.0 / w;
            double r = Math.toRadians(lon[i]);
            cosLon[i] = Math.cos(r);
            sinLon[i] = Math.sin(r);
        }
        vx = new float[n];
        vy = new float[n];
        vz = new float[n];
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                int k = j * w + i;
                vx[k] = (float) (cosLat[j] * cosLon[i]);
                vy[k] = (float) (cosLat[j] * sinLon[i]);
                vz[k] = (float) sinLat[j];
            }
        }
    }

    public int idx(int i, int j) {
        return j * w + Math.floorMod(i, w);
    }

    /** Fractional column for a longitude (cell centres at .5 -> integer + 0.5 maps to that cell). */
    public double fx(double lonDeg) {
        return (lonDeg + 180.0) * w / 360.0 - 0.5;
    }

    public double fy(double latDeg) {
        return (90.0 - latDeg) * h / 180.0 - 0.5;
    }

    /** Forget the per-cell unit vectors (only the build needs them). */
    public void dropVectors() {
        vx = vy = vz = null;
    }

    /** Bilinear sample of a byte field (0..255) at lon/lat. */
    public float sample(byte[] f, double lonDeg, double latDeg) {
        double x = fx(lonDeg), y = fy(latDeg);
        int x0 = (int) Math.floor(x), y0 = (int) Math.floor(y);
        double tx = x - x0, ty = y - y0;
        int y1 = Math.min(h - 1, y0 + 1);
        if (y0 < 0) { y0 = 0; ty = 0; }
        if (y0 >= h - 1) { y0 = h - 1; y1 = h - 1; }
        int xa = Math.floorMod(x0, w), xb = Math.floorMod(x0 + 1, w);
        double a = f[y0 * w + xa] & 255, b = f[y0 * w + xb] & 255, c = f[y1 * w + xa] & 255, d = f[y1 * w + xb] & 255;
        return (float) ((a * (1 - tx) + b * tx) * (1 - ty) + (c * (1 - tx) + d * tx) * ty);
    }

    /** Bilinear sample of a float field at lon/lat, wrapping in longitude, clamping in latitude. */
    public float sample(float[] f, double lonDeg, double latDeg) {
        double x = fx(lonDeg), y = fy(latDeg);
        int x0 = (int) Math.floor(x), y0 = (int) Math.floor(y);
        double tx = x - x0, ty = y - y0;
        int y1 = Math.min(h - 1, y0 + 1);
        if (y0 < 0) { y0 = 0; ty = 0; }
        if (y0 >= h - 1) { y0 = h - 1; y1 = h - 1; }
        int xa = Math.floorMod(x0, w), xb = Math.floorMod(x0 + 1, w);
        double a = f[y0 * w + xa], b = f[y0 * w + xb], c = f[y1 * w + xa], d = f[y1 * w + xb];
        return (float) ((a * (1 - tx) + b * tx) * (1 - ty) + (c * (1 - tx) + d * tx) * ty);
    }

    private static double catmull(double p0, double p1, double p2, double p3, double t) {
        return p1 + 0.5 * t * (p2 - p0 + t * (2 * p0 - 5 * p1 + 4 * p2 - p3 + t * (3 * (p1 - p2) + p3 - p0)));
    }

    /** Bicubic (Catmull-Rom) sample: smooth slopes between cell centres, passes through every cell value. */
    public float sampleCubic(float[] f, double lonDeg, double latDeg) {
        double x = fx(lonDeg), y = fy(latDeg);
        int x0 = (int) Math.floor(x), y0 = (int) Math.floor(y);
        double tx = x - x0, ty = y - y0;
        int xm = Math.floorMod(x0 - 1, w), xa = Math.floorMod(x0, w), xb = Math.floorMod(x0 + 1, w), xc = Math.floorMod(x0 + 2, w);
        double r0 = 0, r1 = 0, r2 = 0, r3 = 0;
        for (int q = -1; q <= 2; q++) {
            int row = Math.max(0, Math.min(h - 1, y0 + q)) * w;
            double v = catmull(f[row + xm], f[row + xa], f[row + xb], f[row + xc], tx);
            if (q == -1) r0 = v; else if (q == 0) r1 = v; else if (q == 1) r2 = v; else r3 = v;
        }
        return (float) catmull(r0, r1, r2, r3, ty);
    }

    /** Bilinear sample at a unit vector. */
    public float sample(float[] f, double x, double y, double z) {
        return sample(f, Math.toDegrees(Math.atan2(y, x)), Math.toDegrees(Math.asin(Math.max(-1, Math.min(1, z)))));
    }

    /** Nearest cell index for a unit vector. */
    public int cellOf(double x, double y, double z) {
        double lo = Math.toDegrees(Math.atan2(y, x)), la = Math.toDegrees(Math.asin(Math.max(-1, Math.min(1, z))));
        int i = (int) Math.floor((lo + 180.0) * w / 360.0), j = (int) Math.floor((90.0 - la) * h / 180.0);
        return Math.max(0, Math.min(h - 1, j)) * w + Math.floorMod(i, w);
    }

    /** Great-circle distance in km between two cells. */
    public double distKm(int a, int b) {
        double d = vx[a] * (double) vx[b] + vy[a] * (double) vy[b] + vz[a] * (double) vz[b];
        return R_KM * Math.acos(Math.max(-1, Math.min(1, d)));
    }

    /**
     * Nearest source for every cell: {@code near[k]} = index of the closest cell with {@code src[k]} (great circle),
     * -1 when there is none; {@code distKm} its distance. Vector propagation (two raster sweeps each way, twice, with
     * a wrap-around lap per row), so it is near-exact at a fraction of a brute-force cost.
     */
    public void nearest(boolean[] src, int[] near, float[] distKm) {
        float[] best = new float[n]; // 1 - dot, monotone in distance
        for (int k = 0; k < n; k++) {
            near[k] = src[k] ? k : -1;
            best[k] = src[k] ? 0f : Float.MAX_VALUE;
        }
        for (int pass = 0; pass < 2; pass++) {
            // forward: rows top to bottom, each row swept west to east one and a half laps (wrap)
            for (int j = 0; j < h; j++) {
                for (int s = 0; s < w + w / 2; s++) {
                    int i = s % w, k = j * w + i;
                    relax(k, j * w + Math.floorMod(i - 1, w), near, best);
                    if (j > 0) {
                        relax(k, (j - 1) * w + Math.floorMod(i - 1, w), near, best);
                        relax(k, (j - 1) * w + i, near, best);
                        relax(k, (j - 1) * w + Math.floorMod(i + 1, w), near, best);
                    }
                }
                for (int s = w + w / 2 - 1; s >= 0; s--) { // and back east to west within the row
                    int i = s % w, k = j * w + i;
                    relax(k, j * w + Math.floorMod(i + 1, w), near, best);
                }
            }
            // backward: rows bottom to top
            for (int j = h - 1; j >= 0; j--) {
                for (int s = w + w / 2 - 1; s >= 0; s--) {
                    int i = s % w, k = j * w + i;
                    relax(k, j * w + Math.floorMod(i + 1, w), near, best);
                    if (j < h - 1) {
                        relax(k, (j + 1) * w + Math.floorMod(i + 1, w), near, best);
                        relax(k, (j + 1) * w + i, near, best);
                        relax(k, (j + 1) * w + Math.floorMod(i - 1, w), near, best);
                    }
                }
                for (int s = 0; s < w + w / 2; s++) {
                    int i = s % w, k = j * w + i;
                    relax(k, j * w + Math.floorMod(i - 1, w), near, best);
                }
            }
            // pole rows: every cell of the first/last row is a neighbour of every other across the pole
            polar(0, near, best);
            polar(h - 1, near, best);
        }
        if (distKm != null) {
            for (int k = 0; k < n; k++) {
                distKm[k] = near[k] < 0 ? Float.POSITIVE_INFINITY : (float) (R_KM * Math.acos(Math.max(-1.0, Math.min(1.0, 1.0 - best[k]))));
            }
        }
    }

    private void polar(int j, int[] near, float[] best) {
        int bestSrc = -1;
        float b = Float.MAX_VALUE;
        for (int i = 0; i < w; i++) {
            int k = j * w + i;
            if (best[k] < b) { b = best[k]; bestSrc = near[k]; }
        }
        if (bestSrc < 0) return;
        for (int i = 0; i < w; i++) relax(j * w + i, bestSrc, true, near, best);
    }

    private void relax(int k, int nb, int[] near, float[] best) {
        int s = near[nb];
        if (s < 0 || s == near[k]) return;
        float d = 1f - (vx[k] * vx[s] + vy[k] * vy[s] + vz[k] * vz[s]);
        if (d < best[k]) {
            best[k] = d;
            near[k] = s;
        }
    }

    private void relax(int k, int s, boolean direct, int[] near, float[] best) {
        if (s < 0 || s == near[k]) return;
        float d = 1f - (vx[k] * vx[s] + vy[k] * vy[s] + vz[k] * vz[s]);
        if (d < best[k]) {
            best[k] = d;
            near[k] = s;
        }
    }

    /**
     * Blur on the sphere with an approximately Gaussian kernel of {@code radiusKm} (three box passes each way; the
     * east-west box widens toward the poles so the kernel stays the same size on the ground).
     */
    public float[] blur(float[] f, double radiusKm) {
        float[] a = f.clone(), b = new float[n];
        for (int it = 0; it < 3; it++) {
            final float[] src = a, dst = b;
            IntStream.range(0, h).parallel().forEach(j -> {
                int r = (int) Math.min(w / 2 - 1, Math.round(radiusKm / Math.max(1e-6, kmX[j]) / 1.7));
                boxRow(src, dst, j, r);
            });
            float[] t = a; a = b; b = t;
            int rv = (int) Math.max(0, Math.round(radiusKm / kmY / 1.7));
            final float[] src2 = a, dst2 = b;
            IntStream.range(0, w).parallel().forEach(i -> boxCol(src2, dst2, i, rv));
            t = a; a = b; b = t;
        }
        return a;
    }

    private void boxRow(float[] src, float[] dst, int j, int r) {
        int base = j * w;
        if (r <= 0) {
            System.arraycopy(src, base, dst, base, w);
            return;
        }
        double sum = 0;
        for (int d = -r; d <= r; d++) sum += src[base + Math.floorMod(d, w)];
        double inv = 1.0 / (2 * r + 1);
        for (int i = 0; i < w; i++) {
            dst[base + i] = (float) (sum * inv);
            sum += src[base + Math.floorMod(i + r + 1, w)] - src[base + Math.floorMod(i - r, w)];
        }
    }

    private void boxCol(float[] src, float[] dst, int i, int r) {
        if (r <= 0) {
            for (int j = 0; j < h; j++) dst[j * w + i] = src[j * w + i];
            return;
        }
        double sum = 0;
        for (int d = -r; d <= r; d++) sum += src[Math.max(0, Math.min(h - 1, d)) * w + i];
        double inv = 1.0 / (2 * r + 1);
        for (int j = 0; j < h; j++) {
            dst[j * w + i] = (float) (sum * inv);
            sum += src[Math.min(h - 1, j + r + 1) * w + i] - src[Math.max(0, j - r) * w + i];
        }
    }

    /** Area-weighted mean of a field over cells where {@code mask} is true (or all, when null). */
    public double mean(float[] f, boolean[] mask) {
        double s = 0, a = 0;
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                int k = j * w + i;
                if (mask != null && !mask[k]) continue;
                s += f[k] * areaW[j];
                a += areaW[j];
            }
        }
        return a == 0 ? 0 : s / a;
    }

    /** Value below which an area fraction {@code q} of the (masked) grid lies. */
    public float quantile(float[] f, boolean[] mask, double q) {
        float lo = Float.MAX_VALUE, hi = -Float.MAX_VALUE;
        for (int k = 0; k < n; k++) {
            if (mask != null && !mask[k]) continue;
            lo = Math.min(lo, f[k]);
            hi = Math.max(hi, f[k]);
        }
        if (lo > hi) return 0;
        int bins = 1 << 16;
        double[] hist = new double[bins];
        double scale = (bins - 1) / Math.max(1e-9, hi - lo), total = 0;
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                int k = j * w + i;
                if (mask != null && !mask[k]) continue;
                hist[(int) ((f[k] - lo) * scale)] += areaW[j];
                total += areaW[j];
            }
        }
        double target = q * total, c = 0;
        for (int b = 0; b < bins; b++) {
            if (c + hist[b] >= target) {
                double t = hist[b] == 0 ? 0 : (target - c) / hist[b];
                return (float) (lo + (b + t) / scale);
            }
            c += hist[b];
        }
        return hi;
    }

    /** Downsample by an integer factor (box mean), for a coarser grid of the same layout. */
    public static float[] down(float[] f, int w, int h, int factor) {
        int w2 = w / factor, h2 = h / factor;
        float[] o = new float[w2 * h2];
        float inv = 1f / (factor * factor);
        for (int j = 0; j < h2; j++) {
            for (int i = 0; i < w2; i++) {
                float s = 0;
                for (int b = 0; b < factor; b++) for (int a = 0; a < factor; a++) s += f[(j * factor + b) * w + i * factor + a];
                o[j * w2 + i] = s * inv;
            }
        }
        return o;
    }

    public static void fill(float[] f, float v) {
        Arrays.fill(f, v);
    }
}
