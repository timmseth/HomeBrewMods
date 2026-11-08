package com.seth.worldbound.landmarks;

import com.seth.worldbound.Earth;
import com.seth.worldbound.WorldBoundMod;
import net.fabricmc.api.ModInitializer;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Landmarks' shared part: obelisks at a world's landmarks, discovery, the travel map (K) and travel. Which
 * landmarks a world has comes from Earth Landmarks (the real ones) or Earthish Landmarks (a generated planet's).
 */
public final class LandmarksMod implements ModInitializer {
    public static final String ID = "worldbound_landmarks";
    private static final Set<String> KINDS = ConcurrentHashMap.newKeySet();
    private static boolean initialised;

    @Override
    public void onInitialize() {
        init();
    }

    public static synchronized void init() {
        if (initialised) return;
        initialised = true;
        WorldBoundMod.init();
        Landmarks.register();
        com.seth.worldbound.api.WorldOptions.register(new com.seth.worldbound.api.WorldOptions.Option("landmarks", "Landmark obelisks",
                java.util.List.of("on", "off"), java.util.List.of("On (discover & fast travel)", "Off"),
                "An obelisk at every landmark: walk up to one to discover it, then travel between the ones you've found."
                        + " Discovering cities and landmarks also unlocks map travel near them (K).", true));
        // /earth landmarks: how many this world has, and the nearest few
        com.seth.worldbound.api.EarthCommandsApi.register(earth -> earth.then(net.minecraft.commands.Commands.literal("landmarks").executes(c -> {
            var all = com.seth.worldbound.api.WorldBound.landmarks();
            var t = Earth.get().activeTransform();
            double lat = t.lat(c.getSource().getPosition().z), lon = t.lon(c.getSource().getPosition().x);
            var l = new java.util.ArrayList<>(all);
            l.sort(java.util.Comparator.comparingDouble(p -> com.seth.worldbound.api.GeoText.distanceKm(lat, lon, p.lat(), p.lon())));
            StringBuilder sb = new StringBuilder(all.size() + " landmarks in this world" + (covers(Earth.get()) ? "" : " (no Landmarks mod for this kind of world)") + ". Nearest:");
            for (var p : l.subList(0, Math.min(5, l.size())))
                sb.append(String.format(java.util.Locale.ROOT, "\n  %s (%s), %,.0f km %s", com.seth.worldbound.api.GeoText.plain(p.name()), p.region(),
                        com.seth.worldbound.api.GeoText.distanceKm(lat, lon, p.lat(), p.lon()), com.seth.worldbound.api.GeoText.direction(lat, lon, p.lat(), p.lon())));
            String m = sb.toString();
            c.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(m), false);
            return 1;
        })));
    }

    /** Earth Landmarks or Earthish Landmarks says it supplies landmarks for this kind of world ("earth", "earthish"). */
    public static void registerKind(String kind) { KINDS.add(kind); }

    /** Landmarks are installed for the running world's kind. */
    public static boolean covers(Earth e) { return KINDS.contains(e.generated() ? "earthish" : "earth"); }
}
