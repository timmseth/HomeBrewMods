package com.seth.worldbound.client;

import com.seth.worldbound.api.Places.Place;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.zip.GZIPInputStream;

/** Countries, US states, cities and US zips from /worldbound/places.tsv.gz (built by tools/build_gazetteer.py). */
public final class Gazetteer {
    // (Place is com.seth.worldbound.api.Places.Place)
    private static Gazetteer INSTANCE;

    /** Compose accents into single characters (a + combining macron -> ā) so Minecraft's font draws them. */
    static String nfc(String s) {
        return java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFC);
    }

    /** Accent-free plain text: "São Paulo" -> "Sao Paulo". */
    public static String plain(String s) {
        return java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    /** Search text: both the real spelling and the accent-free one match. */
    static String searchKey(String s) {
        String l = s.toLowerCase(Locale.ROOT);
        String p = plain(l);
        return p.equals(l) ? l : l + " " + p;
    }
    private final List<Place> places = new ArrayList<>();
    private final List<String> lower = new ArrayList<>();
    private final List<Place> landmarks = new ArrayList<>();
    private final List<Place> countryLabels = new ArrayList<>();
    private final List<Place> states = new ArrayList<>();
    private final List<Place> bigCities = new ArrayList<>();
    private final Random random = new Random();

    public static synchronized Gazetteer get() {
        if (INSTANCE == null) INSTANCE = new Gazetteer();
        return INSTANCE;
    }

    private Gazetteer() {
        // Landmarks first so they win ties in search; span is stored in the population slot (x1000).
        try (InputStream in = Gazetteer.class.getResourceAsStream("/worldbound/landmarks.tsv")) {
            if (in != null) {
                BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                String line;
                while ((line = r.readLine()) != null) {
                    String[] f = line.split("\t", -1);
                    if (f.length < 5) continue;
                    Place p = new Place("landmark", nfc(f[0]), nfc(f[1]), "", Double.parseDouble(f[2]), Double.parseDouble(f[3]),
                            (long) (Double.parseDouble(f[4]) * 1000));
                    landmarks.add(p);
                    places.add(p);
                    lower.add(searchKey(p.name() + " " + p.region()));
                }
            }
        } catch (IOException | RuntimeException e) {
            // no landmarks; everything else still works
        }
        try (InputStream in = Gazetteer.class.getResourceAsStream("/worldbound/places.tsv.gz")) {
            if (in == null) return;
            BufferedReader r = new BufferedReader(new InputStreamReader(new GZIPInputStream(in), StandardCharsets.UTF_8));
            String line;
            while ((line = r.readLine()) != null) {
                String[] f = line.split("\t", -1);
                if (f.length < 7) continue;
                Place p = new Place(f[0], nfc(f[1]), nfc(f[2]), f[3], Double.parseDouble(f[4]), Double.parseDouble(f[5]), Long.parseLong(f[6]));
                places.add(p);
                lower.add(searchKey(p.name() + " " + p.region() + " " + p.country()));
            }
        } catch (IOException | RuntimeException e) {
            // Search just returns nothing; the picker still works by clicking and typing coordinates.
        }
        // Label positions: a country's label sits at the population-weighted centre of its cities,
        // not at its capital (the "go to" entry keeps the capital).
        java.util.Map<String, double[]> acc = new java.util.HashMap<>();
        for (Place p : places) {
            if (!p.type().equals("city")) continue;
            double w = Math.max(p.population(), 1);
            double[] a = acc.computeIfAbsent(p.country(), k -> new double[3]);
            a[0] += p.lat() * w; a[1] += p.lon() * w; a[2] += w;
            if (p.population() >= 300_000) bigCities.add(p);
        }
        for (Place p : places) {
            if (p.type().equals("country")) {
                double[] a = acc.get(p.country());
                if (a != null && a[2] > 0) countryLabels.add(new Place("country", p.name(), p.region(), p.country(), a[0] / a[2], a[1] / a[2], p.population()));
            } else if (p.type().equals("state")) {
                states.add(p);
            }
        }
        bigCities.sort((a, b) -> Long.compare(b.population(), a.population()));
        countryLabels.sort((a, b) -> Long.compare(b.population(), a.population()));
    }

    public List<Place> countryLabels() {
        return countryLabels;
    }

    public List<Place> states() {
        return states;
    }

    /** Cities of 300k+ people, largest first. */
    public List<Place> bigCities() {
        return bigCities;
    }

    /** All cities (any size), for close zooms. */
    public List<Place> cities() {
        return places.stream().filter(p -> p.type().equals("city")).toList();
    }

    public int size() {
        return places.size();
    }

    public List<Place> landmarks() {
        return landmarks;
    }

    /** Best matches first: prefix matches of the name beat substring matches; larger places beat smaller. */
    public List<Place> search(String query, int limit) {
        String q = plain(query.trim().toLowerCase(Locale.ROOT));
        List<Place> out = new ArrayList<>();
        if (q.isEmpty()) return out;
        List<Place> prefix = new ArrayList<>(), sub = new ArrayList<>();
        for (int i = 0; i < places.size(); i++) {
            String l = lower.get(i);
            Place p = places.get(i);
            String n = plain(p.name().toLowerCase(Locale.ROOT));
            if (n.startsWith(q)) prefix.add(p);
            else if (l.contains(q)) sub.add(p);
            if (prefix.size() > 400) break;
        }
        java.util.Comparator<Place> rank = (a, b) -> {
            boolean la = a.type().equals("landmark"), lb = b.type().equals("landmark");
            if (la != lb) return la ? -1 : 1;
            return Long.compare(b.population(), a.population());
        };
        prefix.sort(rank);
        sub.sort(rank);
        for (Place p : prefix) if (out.size() < limit) out.add(p);
        for (Place p : sub) if (out.size() < limit) out.add(p);
        return out;
    }

    private java.util.Map<Long, List<Place>> grid;

    /** Nearest city to a point (any size), or null if none within ~3 degrees. */
    public synchronized Place nearestCity(double lat, double lon) {
        if (grid == null) {
            grid = new java.util.HashMap<>();
            for (Place p : places) if (p.type().equals("city")) grid.computeIfAbsent(cell((int) Math.floor(p.lat()), (int) Math.floor(p.lon())), k -> new ArrayList<>()).add(p);
        }
        int la = (int) Math.floor(lat), lo = (int) Math.floor(lon);
        Place best = null;
        double bd = Double.MAX_VALUE;
        for (int r = 0; r <= 3 && best == null; r++) {
            for (int dla = -r; dla <= r; dla++) for (int dlo = -r; dlo <= r; dlo++) {
                List<Place> l = grid.get(cell(la + dla, Math.floorMod(lo + dlo + 180, 360) - 180));
                if (l == null) continue;
                for (Place p : l) {
                    double d = distanceKm(lat, lon, p.lat(), p.lon());
                    if (d < bd) { bd = d; best = p; }
                }
            }
        }
        return best;
    }

    private static long cell(int lat, int lon) {
        return ((long) lat << 32) ^ (lon & 0xFFFFFFFFL);
    }

    public static double distanceKm(double la1, double lo1, double la2, double lo2) {
        double p1 = Math.toRadians(la1), p2 = Math.toRadians(la2), dp = p2 - p1, dl = Math.toRadians(lo2 - lo1);
        double a = Math.sin(dp / 2) * Math.sin(dp / 2) + Math.cos(p1) * Math.cos(p2) * Math.sin(dl / 2) * Math.sin(dl / 2);
        return 6371.0 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /** "near Pocatello, ID, US (3 km NE)" — direction is from the city to the point. */
    public String near(double lat, double lon) {
        Place p = nearestCity(lat, lon);
        if (p == null) return "far from any town";
        double d = distanceKm(p.lat(), p.lon(), lat, lon);
        String where = plain(p.name()) + (p.region().isEmpty() ? "" : ", " + plain(p.region())) + ", " + p.country();
        if (d < 1) return "in " + where;
        double brg = Math.toDegrees(Math.atan2((lon - p.lon()) * Math.cos(Math.toRadians(lat)), lat - p.lat()));
        String[] dirs = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        String dir = dirs[(int) Math.floorMod(Math.round(brg / 45.0), 8)];
        return String.format(Locale.ROOT, "near %s (%.0f km %s)", where, d, dir);
    }

    /** "42.871°N 112.440°W" */
    public static String latLon(double lat, double lon) {
        return String.format(Locale.ROOT, "%.3f°%s %.3f°%s", Math.abs(lat), lat >= 0 ? "N" : "S", Math.abs(lon), lon >= 0 ? "E" : "W");
    }

    /** Nearest landmarks to a point, closest first. */
    public List<Place> nearestLandmarks(double lat, double lon, int n) {
        List<Place> l = new ArrayList<>(landmarks);
        l.sort(java.util.Comparator.comparingDouble(p -> distanceKm(lat, lon, p.lat(), p.lon())));
        return l.subList(0, Math.min(n, l.size()));
    }

    /** Compass direction from one point to another ("NE"). */
    public static String direction(double fromLat, double fromLon, double toLat, double toLon) {
        double brg = Math.toDegrees(Math.atan2((toLon - fromLon) * Math.cos(Math.toRadians(fromLat)), toLat - fromLat));
        String[] dirs = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        return dirs[(int) Math.floorMod(Math.round(brg / 45.0), 8)];
    }

    /** A random city (so "random" lands somewhere people live, not mid-ocean). */
    public Place randomCity() {
        List<Place> cities = places.stream().filter(p -> p.type().equals("city")).toList();
        if (cities.isEmpty()) return new Place("city", "Pocatello", "ID", "US", 42.87, -112.44, 0);
        return cities.get(random.nextInt(cities.size()));
    }
}
