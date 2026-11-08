package com.seth.earthdata;

import com.seth.worldbound.api.Places;
import com.seth.worldbound.client.Gazetteer;

import java.util.List;

/** The gazetteer behind the engine's {@link Places} API (loaded on first use). */
final class GazetteerPlaces implements Places {
    @Override public String near(double lat, double lon) { return Gazetteer.get().near(lat, lon); }

    @Override public List<Place> search(String query, int limit) { return Gazetteer.get().search(query, limit); }

    @Override public List<Place> landmarks() { return Gazetteer.get().landmarks(); }

    @Override public List<Place> cities() { return Gazetteer.get().cities(); }

    @Override public Place nearestCity(double lat, double lon) { return Gazetteer.get().nearestCity(lat, lon); }

    @Override public Place randomCity() { return Gazetteer.get().randomCity(); }

    @Override public List<Place> countryLabels() { return Gazetteer.get().countryLabels(); }

    @Override public List<Place> states() { return Gazetteer.get().states(); }

    @Override public List<Place> bigCities() { return Gazetteer.get().bigCities(); }

    @Override public List<Place> nearestLandmarks(double lat, double lon, int n) { return Gazetteer.get().nearestLandmarks(lat, lon, n); }
}
