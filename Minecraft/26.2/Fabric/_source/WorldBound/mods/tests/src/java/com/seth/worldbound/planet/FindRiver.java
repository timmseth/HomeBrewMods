package com.seth.worldbound.planet;

/** A point exactly on a mid-size river of the planet, near its spawn. */
public final class FindRiver {
    public static void main(String[] a) {
        Planet p = Planet.get(Long.parseLong(a[0]), null);
        PlanetRivers r = p.source.rivers;
        double sLat = p.spawnLat(), sLon = p.spawnLon(), best = 1e9;
        int bi = -1;
        for (int i = 0; i < r.count; i++) {
            if (r.width[i] < 70 || r.width[i] > 220) continue;
            double d = Math.abs(r.lat0(i) - sLat) + Math.abs(((r.lon0(i) - sLon + 540) % 360) - 180) * 0.5;
            if (d < best) { best = d; bi = i; }
        }
        double[] pt = new double[2];
        double kx = PlanetRivers.KX0 * Math.cos(Math.toRadians(r.lat0(bi)));
        r.point(bi, 0.5, kx, pt);
        double lon = r.lon0(bi) + pt[0] / kx, lat = r.lat0(bi) + pt[1] / PlanetRivers.KY;
        System.out.printf(java.util.Locale.ROOT, "river %.5f %.5f width %.0f m depth %.1f m bed %.0f..%.0f m hit=%s%n", lat, lon, r.width[bi], r.depth[bi], r.bed0[bi], r.bed1[bi], r.query(lon, lat));
    }
}
