package com.seth.worldbound.planet;

/** Public entry to the planet climate rules (tests, commands). */
public final class ClimateAccess {
    private ClimateAccess() {}

    /** Köppen class 1..30 for annual mean °C, yearly range °C, annual precipitation mm and summer-half share. */
    public static int koppen(double t, double range, double precip, double summerShare) {
        return Climate.koppen(t, range, precip, summerShare);
    }
}
