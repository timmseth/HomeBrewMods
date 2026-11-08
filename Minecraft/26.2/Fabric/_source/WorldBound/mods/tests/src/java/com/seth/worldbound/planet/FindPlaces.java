package com.seth.worldbound.planet;

/** Prints some interesting places on a planet (for snapshots). */
public final class FindPlaces {
    public static void main(String[] a) {
        Planet.KEEP_BUILD_DATA = true;
        Planet p = Planet.get(Long.parseLong(a[0]), null);
        Grid g = p.grid;
        double sLat = p.spawnLat(), sLon = p.spawnLon();
        String[] what = {"mountain-river", "lake", "desert", "rainforest", "coast-cliff", "taiga"};
        for (String w : what) {
            double best = -1e18; int bk = -1;
            for (int k = 0; k < g.n; k++) {
                double e = p.elev[k];
                if (e <= 0 || p.ice[k] != 0) continue;
                int kc = p.koppen[k];
                double lat = g.lat[k / Planet.W], lon = g.lon[k % Planet.W];
                double dist = Math.abs(lat - sLat) + Math.abs(lon - sLon) * 0.5; // prefer places near spawn
                double sc;
                switch (w) {
                    case "mountain-river" -> sc = (p.hydro.discharge[k] > 60 && e > 1500 ? p.rugged[k] + e * 0.3 : -1) - dist * 8;
                    case "lake" -> sc = (p.hydro.lakeLevel[k] != Hydrology.NO_LAKE && neighbourLand(p, k) ? 1000 : -1) - dist * 10;
                    case "desert" -> sc = (kc == 4 ? 1000 + p.rugged[k] * 0.3 : -1) - dist * 10;
                    case "rainforest" -> sc = (kc == 1 ? 1000 + p.rugged[k] * 0.2 : -1) - dist * 10;
                    case "coast-cliff" -> sc = (coastal(p, k) ? p.rugged[k] : -1) - dist * 6;
                    case "taiga" -> sc = (kc == 27 ? 1000 : -1) - dist * 10;
                    default -> sc = -1;
                }
                if (sc > best) { best = sc; bk = k; }
            }
            if (bk >= 0) System.out.printf(java.util.Locale.ROOT, "%s %.4f %.4f elev %.0f koppen %d rug %.0f q %.0f%n", w, g.lat[bk / Planet.W], g.lon[bk % Planet.W], p.elev[bk], (int) p.koppen[bk], p.rugged[bk], p.hydro.discharge[bk]);
        }
    }

    static boolean neighbourLand(Planet p, int k) {
        int i = k % Planet.W, j = k / Planet.W;
        for (int d = -2; d <= 2; d++) {
            int kk = j * Planet.W + Math.floorMod(i + d, Planet.W);
            if (p.hydro.lakeLevel[kk] == Hydrology.NO_LAKE && p.elev[kk] > 0) return true;
        }
        return false;
    }

    static boolean coastal(Planet p, int k) {
        int i = k % Planet.W, j = k / Planet.W;
        for (int d = -1; d <= 1; d++) if (p.elev[j * Planet.W + Math.floorMod(i + d, Planet.W)] <= 0) return true;
        return false;
    }
}
