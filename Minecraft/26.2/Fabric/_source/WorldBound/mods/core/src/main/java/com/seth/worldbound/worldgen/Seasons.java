package com.seth.worldbound.worldgen;

import com.seth.worldbound.Earth;

import java.time.LocalDate;

/**
 * Real-world temperature for Earth worlds: each Köppen class has a typical annual mean and seasonal
 * half-swing (°C); today's real date and the hemisphere pick where in that swing a place is. Peaks in the
 * north reach their warmest around 19 July, the south around 19 January; the tropics barely change.
 * Replaces vanilla's "colder for every block above sea level", which in a 4,064-tall world freezes whole continents.
 */
public final class Seasons {
    private Seasons() {}

    /** id 0..30 -> {annual mean °C, half-amplitude °C}. 0 = ocean / no data (handled separately). */
    private static final double[][] CLIMATE = {
        {0, 0},
        {27, 1}, {26, 2}, {25, 3},                 // Af Am Aw
        {25, 9}, {12, 13}, {24, 7}, {10, 13},      // BWh BWk BSh BSk
        {17, 8}, {13, 6}, {8, 6},                  // Csa Csb Csc
        {19, 8}, {15, 4}, {10, 5},                 // Cwa Cwb Cwc
        {16, 10}, {10, 6}, {5, 5},                 // Cfa Cfb Cfc
        {10, 14}, {7, 13}, {2, 13}, {-5, 20},      // Dsa Dsb Dsc Dsd
        {9, 17}, {4, 17}, {-2, 19}, {-10, 24},     // Dwa Dwb Dwc Dwd
        {10, 15}, {5, 14}, {-2, 16}, {-12, 25},    // Dfa Dfb Dfc Dfd
        {-8, 10}, {-25, 10},                       // ET EF
    };

    private static volatile long dayCached = -1;
    private static volatile double seasonNorth; // +1 at the northern summer peak, -1 at its winter

    /** +1 = peak of summer for this hemisphere, -1 = peak of winter, from today's real date. */
    public static double season(double lat) {
        long day = LocalDate.now().toEpochDay();
        if (day != dayCached) {
            int doy = LocalDate.now().getDayOfYear();
            seasonNorth = Math.cos(2 * Math.PI * (doy - 200) / 365.25);
            dayCached = day;
        }
        return lat >= 0 ? seasonNorth : -seasonNorth;
    }

    /**
     * Typical air temperature (°C) at a place: the climate's annual mean, plus today's seasonal swing only when
     * seasons are on. With seasons off, only truly cold climates (annual mean at or below -5 °C: polar, tundra,
     * the extreme Siberian classes) get freezing weather; everything else gets rain, so no endless snow.
     */
    public static double celsius(double lat, int koppen) {
        boolean seasons = Earth.seasonsActive();
        double s = seasons ? season(lat) : 0;
        if (koppen < 1 || koppen > 30) {
            // ocean / no data: sea-surface-ish, gentle seasons
            double a = Math.abs(lat);
            return 29 - 0.35 * a - (a > 60 ? (a - 60) * 0.6 : 0) + 3 * s * Math.min(1, a / 30);
        }
        double mean = CLIMATE[koppen][0];
        if (!seasons && mean > -5) mean = Math.max(mean, 2);
        return mean + CLIMATE[koppen][1] * s;
    }

    /** Freezing weather here: cold climate, or the snow cap of a high mountain (just under the biome snowline up). */
    public static boolean freezing(double lat, double lon, int koppen, double elevM) {
        int snow = BiomeTable.snowline(koppen, lat, lon);
        if (elevM >= snow - 150) return true;
        if (alpine(lat, koppen, elevM)) return false;
        return celsius(lat, koppen) < 0;
    }

    /**
     * Mountain pixels of cold classes outside the polar regions (the 1 km climate map labels high Alps, Fuji's
     * summit and the Tibetan plateau ET/EF/Dfc): cool and rainy up to the snow cap, not frozen year-round.
     */
    static boolean alpine(double lat, int koppen, double elevM) {
        return Math.abs(lat) < 60 && elevM > 1200 && (koppen >= 29 || koppen == 10 || koppen == 13 || koppen == 16
                || koppen == 19 || koppen == 20 || koppen == 23 || koppen == 24 || koppen == 27 || koppen == 28);
    }

    /** Minecraft temperature (snow below 0.15) for a block position in an Earth world. */
    public static float minecraftTemperature(int blockX, int blockZ) {
        Earth e = Earth.get();
        var t = e.activeTransform();
        double lon = t.lon(blockX + 0.5), lat = t.lat(blockZ + 0.5);
        int k = e.stack.climate(lon, lat);
        double elev = e.stack.elevation(lon, lat, 0);
        if (elev >= BiomeTable.snowline(k, lat, lon) - 150) return 0.0f;   // snow cap: snowy weather, snow settles
        if (alpine(lat, k, elev)) return 0.15f + 4 * 0.03f;            // cool mountain air, rain
        double c = celsius(lat, k);
        return (float) (0.15 + c * 0.03);
    }
}
