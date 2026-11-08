package com.seth.worldbound.api;

import java.util.Locale;

/** Small helpers for showing places and coordinates. */
public final class GeoText {
    private GeoText() {}

    /** Accent-free plain text: "São Paulo" -> "Sao Paulo". */
    public static String plain(String s) {
        return java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    /** Great-circle distance in km. */
    public static double distanceKm(double la1, double lo1, double la2, double lo2) {
        double p1 = Math.toRadians(la1), p2 = Math.toRadians(la2), dp = p2 - p1, dl = Math.toRadians(lo2 - lo1);
        double a = Math.sin(dp / 2) * Math.sin(dp / 2) + Math.cos(p1) * Math.cos(p2) * Math.sin(dl / 2) * Math.sin(dl / 2);
        return 6371.0 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /** "42.871°N 112.440°W" */
    public static String latLon(double lat, double lon) {
        return String.format(Locale.ROOT, "%.3f°%s %.3f°%s", Math.abs(lat), lat >= 0 ? "N" : "S", Math.abs(lon), lon >= 0 ? "E" : "W");
    }

    /** Compass direction from one point to another ("NE"). */
    public static String direction(double fromLat, double fromLon, double toLat, double toLon) {
        double brg = Math.toDegrees(Math.atan2((toLon - fromLon) * Math.cos(Math.toRadians(fromLat)), toLat - fromLat));
        String[] dirs = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        return dirs[(int) Math.floorMod(Math.round(brg / 45.0), 8)];
    }
}
