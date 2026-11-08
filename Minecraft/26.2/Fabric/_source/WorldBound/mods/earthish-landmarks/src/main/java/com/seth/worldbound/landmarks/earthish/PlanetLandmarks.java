package com.seth.worldbound.landmarks.earthish;

import com.seth.worldbound.api.GeoText;
import com.seth.worldbound.api.Places.Place;
import com.seth.worldbound.planet.Grid;
import com.seth.worldbound.planet.Planet;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * A generated planet's landmarks, worked out from the planet itself (the same list on the server and every
 * client, from the same seed): its highest peaks, the mouths of its greatest rivers, the hearts of its biggest
 * deserts and rainforests, and its great ice caps; each with a name made from the planet's seed.
 */
public final class PlanetLandmarks {
    private PlanetLandmarks() {}

    private static long cachedSeed = Long.MIN_VALUE;
    private static List<Place> cached = List.of();

    public static synchronized List<Place> of(Planet p) {
        if (p == null) return List.of();
        if (p.seed == cachedSeed) return cached;
        cached = List.copyOf(build(p));
        cachedSeed = p.seed;
        return cached;
    }

    static List<Place> build(Planet p) {
        Names names = new Names(p.seed);
        List<Place> out = new ArrayList<>();
        Grid g = p.grid;
        int W = Planet.W, H = Planet.H;

        // ---- peaks: ice-free local maxima of 2,500 m and up, the highest first, at least 900 km apart
        List<int[]> peaks = new ArrayList<>();   // {cell, elev}
        for (int j = 4; j < H - 4; j += 2) {
            if (Math.abs(g.lat[j]) > 72) continue;
            for (int i = 0; i < W; i += 2) {
                int k = j * W + i;
                float e = p.elev[k];
                if (e < 2500 || p.ice[k] != 0) continue;   // ice domes are their own kind of landmark
                boolean top = true;
                for (int dj = -4; dj <= 4 && top; dj++)
                    for (int di = -4; di <= 4 && top; di++)
                        if (p.elev[(j + dj) * W + Math.floorMod(i + di, W)] > e) top = false;
                if (top) peaks.add(new int[]{k, (int) e});
            }
        }
        peaks.sort(Comparator.comparingInt((int[] a) -> -a[1]));
        List<double[]> taken = new ArrayList<>();
        int n = 0;
        for (int[] pk : peaks) {
            if (n >= 14) break;
            double lat = g.lat[pk[0] / W], lon = g.lon[pk[0] % W];
            if (!far(taken, lat, lon, 900)) continue;
            taken.add(new double[]{lat, lon});
            String nm = n == 0 ? "Mount " + names.next() : (n % 3 == 1 ? names.next() + " Peak" : "Mount " + names.next());
            out.add(place(nm, String.format(Locale.ROOT, "peak, %,d m", pk[1]), lat, lon, 60));
            n++;
        }

        // ---- great rivers: the widest channels, where they reach the sea (the low end of their widest stretch)
        List<float[]> mouths = new ArrayList<>();   // {lat, lon, width}
        p.source.rivers.forEach(-180, -85, 180, 85, 400, (lon1, lat1, lon2, lat2, w) -> {
            float e = g.sample(p.elev, lon2, lat2);
            if (e < 15) mouths.add(new float[]{lat2, lon2, w});
            return true;
        });
        mouths.sort((a, b) -> Float.compare(b[2], a[2]));
        n = 0;
        for (float[] m : mouths) {
            if (n >= 10) break;
            if (!far(taken, m[0], m[1], 900)) continue;
            taken.add(new double[]{m[0], m[1]});
            out.add(place("Mouth of the " + names.next() + " River", String.format(Locale.ROOT, "river mouth, %,d m wide", (int) m[2]), m[0], m[1], 80));
            n++;
        }

        // ---- deserts and rainforests: the middle of the widest stretches of one climate (10-degree windows)
        regions(p, out, taken, names, new int[]{4, 5}, 0.7, 4, " Desert", "desert");
        regions(p, out, taken, names, new int[]{1}, 0.6, 3, " Rainforest", "rainforest");

        // ---- ice caps: the highest point of each big ice sheet
        int iceBest = -1;
        for (int hemi = 0; hemi < 2; hemi++) {
            iceBest = -1;
            for (int j = 0; j < H; j++) {
                if ((g.lat[j] > 0) != (hemi == 0)) continue;
                for (int i = 0; i < W; i += 2) {
                    int k = j * W + i;
                    if (p.ice[k] != 0 && (iceBest < 0 || p.elev[k] > p.elev[iceBest])) iceBest = k;
                }
            }
            if (iceBest >= 0 && p.elev[iceBest] > 1500) {
                double lat = g.lat[iceBest / W], lon = g.lon[iceBest % W];
                if (far(taken, lat, lon, 900)) {
                    taken.add(new double[]{lat, lon});
                    out.add(place(names.next() + " Ice Dome", String.format(Locale.ROOT, "ice cap, %,d m", (int) p.elev[iceBest]), lat, lon, 120));
                }
            }
        }
        return out;
    }

    private static void regions(Planet p, List<Place> out, List<double[]> taken, Names names, int[] classes, double minShare, int max, String suffix, String kind) {
        Grid g = p.grid;
        int W = Planet.W, H = Planet.H;
        List<double[]> cand = new ArrayList<>();   // {share, lat, lon}
        for (double lat0 = -60; lat0 < 60; lat0 += 5) {
            for (double lon0 = -180; lon0 < 180; lon0 += 5) {
                int hit = 0, tot = 0;
                double sLat = 0, sLon = 0;
                for (double lat = lat0; lat < lat0 + 10; lat += 0.5) {
                    for (double lon = lon0; lon < lon0 + 10; lon += 0.5) {
                        int j = (int) Math.min(H - 1, Math.max(0, Math.floor(g.fy(lat)))), i = Math.floorMod((int) Math.floor(g.fx(lon)), W);
                        int kp = p.koppen[j * W + i];
                        tot++;
                        for (int c : classes) if (kp == c) { hit++; sLat += lat; sLon += lon; break; }
                    }
                }
                double share = (double) hit / tot;
                if (share >= minShare) cand.add(new double[]{share, sLat / hit, sLon / hit});
            }
        }
        cand.sort((a, b) -> Double.compare(b[0], a[0]));
        int n = 0;
        for (double[] c : cand) {
            if (n >= max) break;
            if (!far(taken, c[1], c[2], 1800)) continue;
            taken.add(new double[]{c[1], c[2]});
            out.add(place("The " + names.next() + suffix, kind, c[1], c[2], 400));
            n++;
        }
    }

    private static boolean far(List<double[]> taken, double lat, double lon, double km) {
        for (double[] t : taken) if (GeoText.distanceKm(t[0], t[1], lat, lon) < km) return false;
        return true;
    }

    /** {@code spanKm}: how wide the feature is, for map zoom (kept in population as thousandths of a degree). */
    private static Place place(String name, String what, double lat, double lon, double spanKm) {
        return new Place("landmark", name, what, "", lat, lon, Math.round(spanKm / 111.0 * 1000));
    }

    /** Names that sound like a place: two or three syllables from the planet's seed, never the same twice. */
    static final class Names {
        private static final String[] START = {"Ka", "Te", "Mo", "Sa", "Va", "Lo", "Ri", "Na", "Tha", "Kel", "Ar", "Os", "Ume", "Bra", "Dra",
                "Fe", "Gal", "Ho", "Ir", "Ju", "Mar", "Nor", "Pe", "Qua", "Sel", "Tor", "Vel", "Wen", "Yo", "Zan", "El", "An", "Cor", "Ves"};
        private static final String[] MID = {"ra", "li", "na", "do", "ve", "ta", "mi", "so", "ka", "re", "lu", "ni", "sha", "the", "ga", "ro", "be", "ze"};
        private static final String[] END = {"n", "r", "th", "s", "l", "x", "ra", "na", "ia", "on", "ar", "eth", "us", "or", "an", "is", "um", "el", "ys", "ka"};
        private final Random r;
        private final java.util.Set<String> used = new java.util.HashSet<>();

        Names(long seed) { r = new Random(seed ^ 0x1A4D_4A12_5EEDL); }

        String next() {
            for (int tries = 0; ; tries++) {
                StringBuilder s = new StringBuilder(START[r.nextInt(START.length)]);
                int mids = r.nextInt(3) == 0 ? 1 : r.nextInt(4) == 0 ? 2 : 0;
                for (int i = 0; i < mids; i++) s.append(MID[r.nextInt(MID.length)]);
                s.append(END[r.nextInt(END.length)]);
                String n = s.toString();
                if (used.add(n) || tries > 50) return n;
            }
        }
    }
}
