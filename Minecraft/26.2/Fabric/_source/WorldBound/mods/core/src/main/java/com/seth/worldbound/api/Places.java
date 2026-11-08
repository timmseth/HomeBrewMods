package com.seth.worldbound.api;

import java.util.List;

/**
 * Real places (Earth Data World Gen's gazetteer: countries, states, cities, landmarks). Null from
 * {@link WorldBound#places()} when the real Earth isn't installed.
 */
public interface Places {
    /** One named place. {@code type} is country, state, city, landmark...; population 0 when unknown. */
    record Place(String type, String name, String region, String country, double lat, double lon, long population) {
        /** Rough size in degrees, for map labels (landmarks keep theirs in {@code population}, in thousandths). */
        public double span() {
            if (type.equals("landmark")) return population / 1000.0;
            return switch (type) {
                case "country" -> 40.0;
                case "state" -> 12.0;
                case "city" -> 0.6;
                default -> 0.15;
            };
        }

        public String label() {
            return switch (type) {
                case "country" -> name + "  (country)";
                case "state" -> name + ", US  (state)";
                case "city" -> name + (region.isEmpty() ? ", " : ", " + region + ", ") + country;
                case "zip" -> name + "  " + region;
                case "landmark" -> name + "  (" + region + ")";
                default -> name;
            };
        }
    }

    /** "near Boise, Idaho"-style description of a spot. */
    String near(double lat, double lon);

    List<Place> search(String query, int limit);

    /** The famous landmarks (Everest, the Grand Canyon...), in a stable order. */
    List<Place> landmarks();

    /** Cities (with population). */
    List<Place> cities();

    Place nearestCity(double lat, double lon);

    List<Place> nearestLandmarks(double lat, double lon, int n);

    /** A random city (so "random" lands somewhere people live). */
    Place randomCity();

    /** Map labels by zoom: countries, US states, big cities. */
    default List<Place> countryLabels() { return List.of(); }

    default List<Place> states() { return List.of(); }

    default List<Place> bigCities() { return List.of(); }
}
