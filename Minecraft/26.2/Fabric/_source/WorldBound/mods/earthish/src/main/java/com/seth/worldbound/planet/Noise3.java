package com.seth.worldbound.planet;

/**
 * Hash-based 3-D gradient noise (Perlin-style, quintic fade) with fBm and ridged variants. Sampled on the unit
 * sphere scaled to a wavelength, so planets have no seam at the antimeridian and no pinching at the poles. No
 * permutation table, so it never repeats at any scale (fine detail down to tens of metres stays unique).
 */
public final class Noise3 {
    private final long seed;

    public Noise3(long seed) {
        this.seed = seed;
    }

    private long hash(int x, int y, int z) {
        long h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (y * 0xC2B2AE3D27D4EB4FL) ^ (z * 0x165667B19E3779F9L);
        h ^= h >>> 32;
        h *= 0xD6E8FEB86659FD93L;
        h ^= h >>> 32;
        return h;
    }

    // 12 edge directions of a cube (+ 4 repeats), the classic improved-noise gradient set.
    private static final double[] GX = {1, -1, 1, -1, 1, -1, 1, -1, 0, 0, 0, 0, 1, 0, -1, 0};
    private static final double[] GY = {1, 1, -1, -1, 0, 0, 0, 0, 1, -1, 1, -1, 1, -1, 1, -1};
    private static final double[] GZ = {0, 0, 0, 0, 1, 1, -1, -1, 1, 1, -1, -1, 0, 1, 0, -1};

    private double grad(int ix, int iy, int iz, double dx, double dy, double dz) {
        int g = (int) (hash(ix, iy, iz) >>> 60);
        return GX[g] * dx + GY[g] * dy + GZ[g] * dz;
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    /** Noise at (x, y, z) in lattice units, about [-1, 1]. */
    public double at(double x, double y, double z) {
        int x0 = (int) Math.floor(x), y0 = (int) Math.floor(y), z0 = (int) Math.floor(z);
        double dx = x - x0, dy = y - y0, dz = z - z0;
        double u = fade(dx), v = fade(dy), w = fade(dz);
        double n000 = grad(x0, y0, z0, dx, dy, dz), n100 = grad(x0 + 1, y0, z0, dx - 1, dy, dz);
        double n010 = grad(x0, y0 + 1, z0, dx, dy - 1, dz), n110 = grad(x0 + 1, y0 + 1, z0, dx - 1, dy - 1, dz);
        double n001 = grad(x0, y0, z0 + 1, dx, dy, dz - 1), n101 = grad(x0 + 1, y0, z0 + 1, dx - 1, dy, dz - 1);
        double n011 = grad(x0, y0 + 1, z0 + 1, dx, dy - 1, dz - 1), n111 = grad(x0 + 1, y0 + 1, z0 + 1, dx - 1, dy - 1, dz - 1);
        double x00 = n000 + (n100 - n000) * u, x10 = n010 + (n110 - n010) * u;
        double x01 = n001 + (n101 - n001) * u, x11 = n011 + (n111 - n011) * u;
        double y0v = x00 + (x10 - x00) * v, y1v = x01 + (x11 - x01) * v;
        return (y0v + (y1v - y0v) * w) * 0.95;
    }

    /** fBm: octaves doubling in frequency, amplitude times {@code gain} each; normalised to about [-1, 1]. */
    public double fbm(double x, double y, double z, double freq, int octaves, double gain) {
        double sum = 0, amp = 1, norm = 0;
        for (int o = 0; o < octaves; o++) {
            sum += amp * at(x * freq + o * 19.1, y * freq - o * 7.3, z * freq + o * 3.7);
            norm += amp;
            amp *= gain;
            freq *= 2;
        }
        return sum / norm;
    }

    /** Ridged multifractal in [0, 1]: sharp crests (mountain ridges, crags), each octave weighted by the last. */
    public double ridged(double x, double y, double z, double freq, int octaves, double gain) {
        double sum = 0, amp = 1, norm = 0, prev = 1;
        for (int o = 0; o < octaves; o++) {
            double n = 1 - Math.abs(at(x * freq + o * 31.3, y * freq + o * 11.9, z * freq - o * 5.1));
            n *= n;
            sum += n * amp * prev;
            norm += amp;
            prev = Math.min(1, n * 1.8);
            amp *= gain;
            freq *= 2;
        }
        return sum / norm;
    }
}
