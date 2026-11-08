package com.seth.worldbound.landmarks;

import com.seth.worldbound.Earth;
import com.seth.worldbound.WorldBoundMod;
import com.seth.worldbound.WorldInfo;
import com.seth.worldbound.net.LandmarkActionPayload;
import com.seth.worldbound.net.LandmarkSyncPayload;
import com.seth.worldbound.worldgen.EarthBiomeSource;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelResource;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Landmark obelisks: one at each of the landmarks in worldbound/landmarks.tsv, built the first time the ground there
 * is loaded. Walking up to one discovers it; standing at a discovered one you can travel to any other you've found.
 */
public final class Landmarks {
    private static LandmarkState state;
    private static boolean enabled, travelOn;
    private static final int DISCOVER_RADIUS = 24, AT_RADIUS = 10;

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(LandmarkSyncPayload.TYPE, LandmarkSyncPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(LandmarkActionPayload.TYPE, LandmarkActionPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(com.seth.worldbound.net.TravelPayloads.Anchors.TYPE, com.seth.worldbound.net.TravelPayloads.Anchors.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(com.seth.worldbound.net.TravelPayloads.Progress.TYPE, com.seth.worldbound.net.TravelPayloads.Progress.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(com.seth.worldbound.net.TravelPayloads.Request.TYPE, com.seth.worldbound.net.TravelPayloads.Request.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(com.seth.worldbound.net.TravelPayloads.Request.TYPE, (msg, ctx) -> {
            if (travelOn && state != null) Travel.request(state, ctx.player(), msg.lat(), msg.lon());
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ServerLevel ow = server.overworld();
            travelOn = ow.getChunkSource().getGenerator().getBiomeSource() instanceof EarthBiomeSource;
            enabled = travelOn && "on".equals(com.seth.worldbound.api.WorldOptions.get("landmarks")) && LandmarksMod.covers(Earth.get());
            state = travelOn ? LandmarkState.load(server.getWorldPath(LevelResource.ROOT).resolve("worldbound_landmarks.json")) : null;
            if (enabled) WorldBoundMod.LOG.info("WorldBound: landmark obelisks on ({} of {} built so far)", state.placed.size(), list().size());
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (state != null) state.saveIfDirty();
            state = null;
            enabled = travelOn = false;
        });
        ServerTickEvents.END_SERVER_TICK.register(Landmarks::tick);
        ServerPlayConnectionEvents.JOIN.register((h, sender, server) -> {
            if (ServerPlayNetworking.canSend(h, LandmarkSyncPayload.TYPE)) sync(h.player, false, -1);
            if (travelOn && state != null) Travel.onJoin(state, h.player);
        });
        ServerPlayNetworking.registerGlobalReceiver(LandmarkActionPayload.TYPE, (msg, ctx) -> {
            ServerPlayer p = ctx.player();
            if (!enabled || state == null) return;
            if (msg.action() == LandmarkActionPayload.OPEN) sync(p, true, atObelisk(p));
            else if (msg.action() == LandmarkActionPayload.TRAVEL) travel(p, msg.index());
        });
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (!enabled || state == null || level.isClientSide() || level.dimension() != Level.OVERWORLD) return InteractionResult.PASS;
            if (!(player instanceof ServerPlayer sp) || player.isSecondaryUseActive()) return InteractionResult.PASS;
            for (Map.Entry<Integer, int[]> e : state.placed.entrySet()) {
                int[] c = e.getValue();
                if (Obelisk.contains(new BlockPos(c[0], c[1], c[2]), hit.getBlockPos())) {
                    discover(sp, e.getKey());
                    sync(sp, true, e.getKey());
                    return InteractionResult.SUCCESS;
                }
            }
            return InteractionResult.PASS;
        });
    }

    private static List<com.seth.worldbound.api.Places.Place> list() {
        return com.seth.worldbound.api.WorldBound.landmarks();
    }

    private static void tick(MinecraftServer server) {
        if (!travelOn || state == null) return;
        int t = server.getTickCount();
        Travel.tick(server, state);
        if (t % 20 == 0)
            for (ServerPlayer p : server.getPlayerList().getPlayers())
                if (p.level().dimension() == Level.OVERWORLD) Travel.discoverCities(state, p);
        if (t % 200 == 0) state.saveIfDirty();
        if (!enabled) return;
        if (t % 20 == 0) {
            ServerLevel level = server.overworld();
            List<com.seth.worldbound.api.Places.Place> l = list();
            Earth e = Earth.get();
            for (int i = 0; i < l.size(); i++) {
                if (state.placed.containsKey(i)) continue;
                com.seth.worldbound.api.Places.Place lm = l.get(i);
                int x = (int) Math.floor(e.transform.blockX(lm.lon())), z = (int) Math.floor(e.transform.blockZ(lm.lat()));
                tryBuild(level, i, x, z);
            }
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (p.level().dimension() != Level.OVERWORLD) continue;
                for (Map.Entry<Integer, int[]> en : state.placed.entrySet()) {
                    int[] c = en.getValue();
                    double dx = p.getX() - c[0], dz = p.getZ() - c[2], dy = p.getY() - c[1];
                    if (dx * dx + dz * dz < DISCOVER_RADIUS * DISCOVER_RADIUS && Math.abs(dy) < 48) discover(p, en.getKey());
                }
            }
        }
    }

    /** Builds landmark i near (x, z) once everything around it is loaded: on the flattest dry spot within ~40 blocks. */
    private static void tryBuild(ServerLevel level, int i, int x, int z) {
        for (int cx = (x - 48) >> 4; cx <= (x + 48) >> 4; cx++)
            for (int cz = (z - 48) >> 4; cz <= (z + 48) >> 4; cz++)
                if (level.getChunkSource().getChunkNow(cx, cz) == null) return;
        int bestX = x, bestZ = z, bestY = Integer.MIN_VALUE;
        double best = Double.MAX_VALUE;
        for (int dx = -40; dx <= 40; dx += 4) {
            for (int dz = -40; dz <= 40; dz += 4) {
                if (dx * dx + dz * dz > 1600) continue;
                int px = x + dx, pz = z + dz;
                int ground = level.getHeight(Heightmap.Types.OCEAN_FLOOR, px, pz) - 1;
                int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE, px, pz) - 1;
                boolean wet = !level.getFluidState(new BlockPos(px, surface, pz)).isEmpty();
                int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
                for (int[] o : new int[][]{{4, 0}, {-4, 0}, {0, 4}, {0, -4}, {3, 3}, {-3, -3}, {3, -3}, {-3, 3}}) {
                    int h = level.getHeight(Heightmap.Types.OCEAN_FLOOR, px + o[0], pz + o[1]) - 1;
                    lo = Math.min(lo, h);
                    hi = Math.max(hi, h);
                }
                double score = (wet ? 1000 : 0) + (hi - lo) * 3 + Math.sqrt(dx * dx + dz * dz) / 6;
                if (score < best) {
                    best = score;
                    bestX = px;
                    bestZ = pz;
                    bestY = wet ? surface + 1 : ground;
                }
            }
        }
        if (bestY == Integer.MIN_VALUE) return;
        BlockPos c = new BlockPos(bestX, bestY, bestZ);
        Obelisk.build(level, c);
        state.placed.put(i, new int[]{c.getX(), c.getY(), c.getZ()});
        state.markDirty();
        WorldBoundMod.LOG.info("WorldBound: landmark obelisk built for {} at {}", list().get(i).name(), c.toShortString());
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) sync(p, false, -1);
    }

    private static void discover(ServerPlayer p, int i) {
        if (i < 0 || i >= list().size()) return; // index saved by a build with a longer landmark list
        TreeSet<Integer> d = state.of(p.getStringUUID());
        if (!d.add(i)) return;
        state.markDirty();
        int[] oc = state.placed.get(i);
        if (oc != null) Travel.addAnchor(state, p, Earth.get().transform.lat(oc[2]), Earth.get().transform.lon(oc[0]));
        String name = com.seth.worldbound.api.GeoText.plain(list().get(i).name());
        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§d" + name)));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("Landmark discovered  ·  " + d.size() + " / " + list().size())));
        p.level().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.7f, 1.0f);
        sync(p, false, -1);
    }

    /** The obelisk the player is standing at, or -1. */
    private static int atObelisk(ServerPlayer p) {
        if (p.level().dimension() != Level.OVERWORLD) return -1; // same x/z in the Nether isn't the obelisk
        for (Map.Entry<Integer, int[]> en : state.placed.entrySet()) {
            int[] c = en.getValue();
            double dx = p.getX() - c[0], dz = p.getZ() - c[2], dy = p.getY() - c[1];
            if (dx * dx + dz * dz < AT_RADIUS * AT_RADIUS && Math.abs(dy) < 16) return en.getKey();
        }
        return -1;
    }

    private static void travel(ServerPlayer p, int target) {
        int from = atObelisk(p);
        Set<Integer> d = state.of(p.getStringUUID());
        int[] c = state.placed.get(target);
        if (from < 0 || c == null || !d.contains(target) || !d.contains(from)) {
            p.sendSystemMessage(Component.literal("Travel works between landmark obelisks you have discovered."), true);
            return;
        }
        Earth e = Earth.get();
        Travel.start(p, e.transform.lat(c[2]), e.transform.lon(c[0]), c[0], c[2] + 4, c[1] + 1, list().get(target).name(), true);
    }

    static void sync(ServerPlayer p, boolean open, int at) {
        if (!ServerPlayNetworking.canSend(p, LandmarkSyncPayload.TYPE)) return;
        if (!enabled || state == null) {
            ServerPlayNetworking.send(p, new LandmarkSyncPayload(false, open, -1, new int[0], new int[0], new int[0], new int[0]));
            return;
        }
        int[] disc = state.of(p.getStringUUID()).stream().mapToInt(Integer::intValue).toArray();
        int n = state.placed.size(), k = 0;
        int[] idx = new int[n], xs = new int[n], zs = new int[n];
        for (Map.Entry<Integer, int[]> en : state.placed.entrySet()) {
            idx[k] = en.getKey();
            xs[k] = en.getValue()[0];
            zs[k] = en.getValue()[2];
            k++;
        }
        ServerPlayNetworking.send(p, new LandmarkSyncPayload(true, open, at, disc, idx, xs, zs));
    }
}
