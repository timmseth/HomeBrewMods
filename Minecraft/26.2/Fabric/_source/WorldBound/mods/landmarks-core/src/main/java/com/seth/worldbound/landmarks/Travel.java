package com.seth.worldbound.landmarks;

import com.seth.worldbound.Earth;
import com.seth.worldbound.WorldBoundMod;
import com.seth.worldbound.net.TravelPayloads;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Map travel. A player can travel to any spot within {@link #RANGE_KM} of somewhere they've discovered (a landmark
 * obelisk, a city of 100k+, their spawn, or anywhere they've travelled to). The land around the destination is
 * built first, with a progress bar, so they never arrive in unloaded terrain.
 */
public final class Travel {
    public static final double RANGE_KM = 100;
    static final double CITY_KM = 2.0;
    private static final int LOAD_RADIUS = 3, TIMEOUT_TICKS = 20 * 90;

    private record Trip(double lat, double lon, int x, int z, double y, String label, int startTick, boolean obelisk) {}
    private static final Map<UUID, Trip> trips = new HashMap<>();
    private static final Map<UUID, Integer> lastRequest = new HashMap<>();
    private static List<com.seth.worldbound.api.Places.Place> cities;

    static List<com.seth.worldbound.api.Places.Place> cities() {
        if (cities == null) {
            List<com.seth.worldbound.api.Places.Place> l = new ArrayList<>();
            var places = com.seth.worldbound.api.WorldBound.places();
            if (places != null) for (com.seth.worldbound.api.Places.Place p : places.cities()) if (p.population() >= 100_000) l.add(p);
            cities = l;
        }
        return cities;
    }

    static double km(double la1, double lo1, double la2, double lo2) {
        return com.seth.worldbound.api.GeoText.distanceKm(la1, lo1, la2, lo2);
    }

    static boolean inRange(LandmarkState st, ServerPlayer p, double lat, double lon) {
        for (double[] a : st.anchorsOf(p.getStringUUID())) if (km(a[0], a[1], lat, lon) <= RANGE_KM + 0.5) return true;
        return false;
    }

    static void addAnchor(LandmarkState st, ServerPlayer p, double lat, double lon) {
        List<double[]> l = st.anchorsOf(p.getStringUUID());
        for (double[] a : l) if (km(a[0], a[1], lat, lon) < 5) return;   // close enough to one we have
        l.add(new double[]{lat, lon});
        st.markDirty();
        syncAnchors(st, p);
    }

    static void syncAnchors(LandmarkState st, ServerPlayer p) {
        if (!ServerPlayNetworking.canSend(p, TravelPayloads.Anchors.TYPE)) return;
        List<double[]> l = st.anchorsOf(p.getStringUUID());
        double[] a = new double[l.size() * 2];
        for (int i = 0; i < l.size(); i++) { a[i * 2] = l.get(i)[0]; a[i * 2 + 1] = l.get(i)[1]; }
        ServerPlayNetworking.send(p, new TravelPayloads.Anchors(RANGE_KM, a));
    }

    /** Join: everyone starts with their spawn and where they stand. */
    static void onJoin(LandmarkState st, ServerPlayer p) {
        Earth e = Earth.get();
        var spawn = p.level().getServer().overworld().getRespawnData().pos();
        addAnchor(st, p, e.transform.lat(spawn.getZ()), e.transform.lon(spawn.getX()));
        if (p.level().dimension() == Level.OVERWORLD) addAnchor(st, p, e.transform.lat(p.getZ()), e.transform.lon(p.getX()));
        syncAnchors(st, p);
    }

    /** Every second: cities the player is in. */
    static void discoverCities(LandmarkState st, ServerPlayer p) {
        Earth e = Earth.get();
        if (e.generated()) return;   // a generated planet has no cities
        double lat = e.transform.lat(p.getZ()), lon = e.transform.lon(p.getX());
        double dLat = CITY_KM / 111.0;
        for (com.seth.worldbound.api.Places.Place c : cities()) {
            if (Math.abs(c.lat() - lat) > dLat) continue;
            if (km(c.lat(), c.lon(), lat, lon) > CITY_KM) continue;
            String key = c.name() + "|" + c.country();
            if (!st.citiesOf(p.getStringUUID()).add(key)) continue;
            st.markDirty();
            addAnchor(st, p, c.lat(), c.lon());
            p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§b" + com.seth.worldbound.api.GeoText.plain(c.name()))));
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("City discovered  ·  travel unlocked within " + (int) RANGE_KM + " km")));
            p.level().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.2f);
        }
    }

    /** A trip to a map point (checked against the player's range). */
    static void request(LandmarkState st, ServerPlayer p, double lat, double lon) {
        // Coordinates come from the client: anything not a plain map point is refused before any maths runs on it
        // (a huge longitude passes a haversine range check and then never finishes wrapping into the world).
        if (!Double.isFinite(lat) || !Double.isFinite(lon) || Math.abs(lat) > 89.4 || Math.abs(lon) > 180) return;
        if (trips.containsKey(p.getUUID())) return;   // one trip at a time
        Integer last = lastRequest.get(p.getUUID());
        int now = p.level().getServer().getTickCount();
        if (last != null && now - last < 20) return;   // at most one request a second
        lastRequest.put(p.getUUID(), now);
        if (p.level().dimension() != Level.OVERWORLD) {
            fail(p, "Map travel works in the overworld");
            return;
        }
        if (!inRange(st, p, lat, lon)) {
            fail(p, "Too far: you can travel within " + (int) RANGE_KM + " km of places you've discovered");
            return;
        }
        Earth e = Earth.get();
        int x = (int) Math.floor(e.transform.blockX(lon)), z = (int) Math.floor(e.transform.blockZ(lat));
        start(p, lat, lon, x, z, Double.NaN, placeName(lat, lon), false);
    }

    /** "Mount Fuji" if a landmark is within 15 km, else the nearest city ("Kyoto"), else the coordinates. */
    static String placeName(double lat, double lon) {
        for (com.seth.worldbound.api.Places.Place l : com.seth.worldbound.api.WorldBound.landmarks()) if (km(l.lat(), l.lon(), lat, lon) < 15) return l.name();
        var places = com.seth.worldbound.api.WorldBound.places();
        com.seth.worldbound.api.Places.Place c = places == null || Earth.get().generated() ? null : places.nearestCity(lat, lon);
        if (c != null && km(c.lat(), c.lon(), lat, lon) < 60) return (km(c.lat(), c.lon(), lat, lon) < CITY_KM * 2 ? "" : "near ") + c.name();
        return com.seth.worldbound.api.GeoText.latLon(lat, lon);
    }

    /** A trip to a block position (obelisk travel); range checks are the caller's. */
    static void start(ServerPlayer p, double lat, double lon, int x, int z, double y, String label, boolean obelisk) {
        ServerLevel level = p.level().getServer().overworld();
        level.getChunkSource().addTicketAndLoadWithRadius(TicketType.PORTAL, new ChunkPos(x >> 4, z >> 4), LOAD_RADIUS);
        trips.put(p.getUUID(), new Trip(lat, lon, x, z, y, label, level.getServer().getTickCount(), obelisk));
        progress(p, label, 0, false);
    }

    static void tick(MinecraftServer server, LandmarkState st) {
        if (trips.isEmpty()) return;
        ServerLevel level = server.overworld();
        int now = server.getTickCount();
        for (var it = trips.entrySet().iterator(); it.hasNext(); ) {
            var en = it.next();
            ServerPlayer p = server.getPlayerList().getPlayer(en.getKey());
            Trip t = en.getValue();
            if (p == null) { it.remove(); continue; }
            // Died, or went through a portal while the land was loading: the trip is off.
            if (!p.isAlive() || p.level().dimension() != Level.OVERWORLD) {
                it.remove();
                progress(p, "Travel cancelled", 0, true);
                continue;
            }
            int cx = t.x >> 4, cz = t.z >> 4, total = 0, ready = 0;
            for (int dx = -LOAD_RADIUS + 1; dx < LOAD_RADIUS; dx++)
                for (int dz = -LOAD_RADIUS + 1; dz < LOAD_RADIUS; dz++) {
                    total++;
                    if (level.getChunkSource().getChunkNow(cx + dx, cz + dz) != null) ready++;
                }
            boolean timedOut = now - t.startTick > TIMEOUT_TICKS;
            if (ready < total && !timedOut) {
                if ((now - t.startTick) % 5 == 0) progress(p, t.label, ready * 100 / total, false);
                continue;
            }
            it.remove();
            arrive(level, p, t, st);
        }
    }

    private static void arrive(ServerLevel level, ServerPlayer p, Trip t, LandmarkState st) {
        double y, x = t.x + 0.5, z = t.z + 0.5;
        float yaw = p.getYRot();
        if (!Double.isNaN(t.y)) {
            y = t.y;
            yaw = 180f;   // facing the obelisk
        } else {
            y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, t.x, t.z) + 1;
        }
        level.playSound(null, p.blockPosition(), SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.35f, 1.5f);
        p.stopRiding();
        p.teleportTo(level, x, y, z, Set.of(), yaw, 0f, false);
        level.playSound(null, BlockPos.containing(x, y, z), SoundEvents.PLAYER_TELEPORT, SoundSource.PLAYERS, 1f, 1f);
        addAnchor(st, p, t.lat, t.lon);
        progress(p, t.label, 100, true);
        p.sendSystemMessage(Component.literal("§dArrived: " + com.seth.worldbound.api.GeoText.plain(t.label)), true);
        WorldBoundMod.LOG.info("{} travelled to {}", p.getName().getString(), t.label);
    }

    private static void fail(ServerPlayer p, String why) {
        progress(p, why, 0, true);
        p.sendSystemMessage(Component.literal("§c" + why), true);
    }

    private static void progress(ServerPlayer p, String label, int pct, boolean done) {
        if (ServerPlayNetworking.canSend(p, TravelPayloads.Progress.TYPE))
            ServerPlayNetworking.send(p, new TravelPayloads.Progress(com.seth.worldbound.api.GeoText.plain(label), pct, done));
    }
}
