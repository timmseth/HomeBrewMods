package com.seth.worldbound.geo;

/** Tiny deterministic 2-D value noise in [-1, 1]; used for sample jitter and cave-biome choice. */
public final class ValueNoise {
    private final long seed;

    public ValueNoise(long seed) {
        this.seed = seed;
    }

    private double lattice(int x, int y) {
        long h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (y * 0xC2B2AE3D27D4EB4FL);
        h ^= h >>> 29; h *= 0xBF58476D1CE4E5B9L; h ^= h >>> 32;
        return ((h & 0xFFFFFF) / (double) 0xFFFFFF) * 2.0 - 1.0;
    }

    private static double smooth(double t) {
        return t * t * (3 - 2 * t);
    }

    /** Noise at (x, y) in lattice units. */
    public double at(double x, double y) {
        int x0 = (int) Math.floor(x), y0 = (int) Math.floor(y);
        double fx = smooth(x - x0), fy = smooth(y - y0);
        double a = lattice(x0, y0), b = lattice(x0 + 1, y0), c = lattice(x0, y0 + 1), d = lattice(x0 + 1, y0 + 1);
        return (a * (1 - fx) + b * fx) * (1 - fy) + (c * (1 - fx) + d * fx) * fy;
    }

    /** Two octaves, wavelength in the same units as x/y. */
    public double fbm(double x, double y, double wavelength) {
        return 0.67 * at(x / wavelength, y / wavelength) + 0.33 * at(x * 2 / wavelength + 17.3, y * 2 / wavelength + 9.1);
    }
}
