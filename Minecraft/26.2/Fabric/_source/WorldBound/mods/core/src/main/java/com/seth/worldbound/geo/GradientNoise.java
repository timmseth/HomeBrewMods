package com.seth.worldbound.geo;

/**
 * Deterministic 2-D gradient (Perlin-style) noise in about [-1, 1], with quintic fade so there are no grid
 * creases, plus fBm and ridged variants. Used for terrain micro-relief, boundary warps and Earthish planets.
 */
public final class GradientNoise {
    private final long seed;

    public GradientNoise(long seed) {
        this.seed = seed;
    }

    private static long mix(long h) {
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= h >>> 33;
        return h;
    }

    /** Gradient dot product at lattice point (ix, iy) for offset (dx, dy); 16 evenly spread directions. */
    private double grad(int ix, int iy, double dx, double dy) {
        long h = mix(seed ^ (ix * 0x9E3779B97F4A7C15L) ^ (iy * 0xC2B2AE3D27D4EB4FL));
        int k = (int) (h & 15);
        double a = k * (Math.PI / 8);
        return Math.cos(a) * dx + Math.sin(a) * dy;
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    /** Noise at (x, y) in lattice units, roughly [-1, 1]. */
    public double at(double x, double y) {
        int x0 = (int) Math.floor(x), y0 = (int) Math.floor(y);
        double dx = x - x0, dy = y - y0;
        double u = fade(dx), v = fade(dy);
        double a = grad(x0, y0, dx, dy), b = grad(x0 + 1, y0, dx - 1, dy);
        double c = grad(x0, y0 + 1, dx, dy - 1), d = grad(x0 + 1, y0 + 1, dx - 1, dy - 1);
        double ab = a + (b - a) * u, cd = c + (d - c) * u;
        return (ab + (cd - ab) * v) * 1.41;
    }

    /** Fractal sum: octaves halving in wavelength and amplitude; result roughly [-1, 1]. */
    public double fbm(double x, double y, double wavelength, int octaves) {
        double sum = 0, amp = 1, norm = 0, f = 1 / wavelength;
        for (int o = 0; o < octaves; o++) {
            sum += amp * at(x * f + o * 31.7, y * f - o * 17.3);
            norm += amp;
            amp *= 0.5;
            f *= 2;
        }
        return sum / norm;
    }

    /** Ridged fractal in [0, 1]: sharp crests where the noise crosses zero (ridges, crags, gullies). */
    public double ridged(double x, double y, double wavelength, int octaves) {
        double sum = 0, amp = 1, norm = 0, f = 1 / wavelength, prev = 1;
        for (int o = 0; o < octaves; o++) {
            double n = 1 - Math.abs(at(x * f + o * 47.1, y * f + o * 23.9));
            n *= n;
            sum += n * amp * prev;
            norm += amp;
            prev = n;
            amp *= 0.5;
            f *= 2;
        }
        return sum / norm;
    }
}
