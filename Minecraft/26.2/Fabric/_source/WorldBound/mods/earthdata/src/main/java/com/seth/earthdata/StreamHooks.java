package com.seth.earthdata;

import com.seth.worldbound.Earth;
import com.seth.worldbound.data.Streamer;

/** Fetching fine data as the world generates and as players move. */
final class StreamHooks {
    private StreamHooks() {}

    private static final ThreadLocal<long[]> LAST_CHUNK = ThreadLocal.withInitial(() -> new long[]{Long.MIN_VALUE});

    /** First look at a chunk on this thread: make sure streamed fine data for it is here (bounded wait). */
    static void chunk(Earth earth, int blockX, int blockZ) {
        if (!Streamer.enabled()) return;
        long ck = ((long) (blockX >> 4) << 32) ^ ((blockZ >> 4) & 0xFFFFFFFFL);
        long[] last = LAST_CHUNK.get();
        if (last[0] == ck) return;
        last[0] = ck;
        var t = earth.transform;
        int x = t.wrapX(blockX);
        // Only chunks right around a player wait (briefly) for a download; the rest generate from what's here.
        double d = Streamer.nearestPlayer(blockX, blockZ);
        boolean wait = d <= earth.config.streaming.wait_radius_blocks;
        Streamer.get().ensure(t.lon(x + 8), t.lat(blockZ + 8), wait, d);
    }

    /** Every 2 s: queue streamed tiles around each player, a bit beyond what they can see. */
    private static final java.util.Map<java.util.UUID, double[]> LAST_POS = new java.util.HashMap<>();

    static void prefetch(net.minecraft.server.MinecraftServer server) {
        if (!Earth.earthWorld()) return;
        int tick = server.getTickCount();
        if (tick % 10 == 0) {   // where the players are: downloads near them go first, only they wait
            var ps = server.getPlayerList().getPlayers();
            double[] xz = new double[ps.size() * 2];
            int n = 0;
            for (var p : ps) {
                if (p.level().dimension() != net.minecraft.world.level.Level.OVERWORLD) continue;
                xz[n++] = p.getX();
                xz[n++] = p.getZ();
            }
            Streamer.setPlayers(java.util.Arrays.copyOf(xz, n));
        }
        if (tick % 1200 == 0 && Streamer.enabled()) Streamer.get().logIfChanged();
        if (tick % 40 != 0 || !Streamer.enabled()) return;
        Earth e = Earth.get();
        int blocks = server.getPlayerList().getViewDistance() * 16 + e.config.streaming.prefetch_blocks;
        var t = e.transform;
        double rLat = blocks * t.degLatPerBlock(), rLon = blocks * t.degLonPerBlock();
        for (net.minecraft.server.level.ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.level().dimension() != net.minecraft.world.level.Level.OVERWORLD) continue;
            double x = p.getX(), z = p.getZ();
            Streamer.get().prefetch(t.lon(x), t.lat(z), rLon, rLat, 100);
            // moving fast (TATV, elytra): also fetch where they'll be in ~10 seconds
            double[] last = LAST_POS.put(p.getUUID(), new double[]{x, z});
            if (last != null) {
                double vx = (x - last[0]) * 5, vz = (z - last[1]) * 5;
                if (vx * vx + vz * vz > 64 * 64 && vx * vx + vz * vz < 50_000.0 * 50_000.0)
                    Streamer.get().prefetch(t.lon(x + vx), t.lat(z + vz), rLon, rLat, 200);
            }
        }
    }
}
