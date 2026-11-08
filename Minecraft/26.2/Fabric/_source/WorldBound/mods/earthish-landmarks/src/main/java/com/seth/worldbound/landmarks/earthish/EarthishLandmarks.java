package com.seth.worldbound.landmarks.earthish;

import com.seth.earthish.EarthishMod;
import com.seth.worldbound.api.GeoText;
import com.seth.worldbound.api.WorldBound;
import com.seth.worldbound.landmarks.LandmarksMod;
import net.fabricmc.api.ModInitializer;

import java.util.Locale;

/**
 * Earthish Landmarks: obelisks at a generated planet's own landmarks (its highest peaks, greatest river mouths,
 * deserts, rainforests and ice caps, each named from the seed), with discovery and travel as on Earth; and the
 * compass names where you are by the nearest of them.
 */
public final class EarthishLandmarks implements ModInitializer {
    @Override
    public void onInitialize() {
        LandmarksMod.init();
        LandmarksMod.registerKind("earthish");
        WorldBound.setGeneratedLandmarks(() -> PlanetLandmarks.of(EarthishMod.planet()));
        WorldBound.setPlaceNamer((lat, lon) -> {
            if (!com.seth.worldbound.Earth.get().generated()) return null;
            var best = (com.seth.worldbound.api.Places.Place) null;
            double bestKm = Double.MAX_VALUE;
            for (var l : PlanetLandmarks.of(EarthishMod.planet())) {
                double d = GeoText.distanceKm(lat, lon, l.lat(), l.lon());
                if (d < bestKm) { bestKm = d; best = l; }
            }
            if (best == null || bestKm > 400) return null;
            if (bestKm < 2) return "at " + best.name();
            return String.format(Locale.ROOT, "%,.0f km %s of %s", bestKm, GeoText.direction(best.lat(), best.lon(), lat, lon), best.name());
        });
    }
}
