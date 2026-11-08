package com.seth.worldbound.net;

import com.seth.worldbound.Earth;
import com.seth.worldbound.WorldBoundConfig;
import com.seth.worldbound.worldgen.EarthBiomeSource;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/** Server side of the Earth-world handshake (see {@link EarthInfoPayload}). */
public final class EarthNet {
    private EarthNet() {}

    /** Players (and whatever they ride) crossing the antimeridian are moved one world-width back: same terrain, other side. */
    private static void wrapPlayers(net.minecraft.server.MinecraftServer server) {
        if (!Earth.earthWorld()) return;
        Earth e = Earth.get();
        boolean wrap = e.config.wrap_around;
        int w = e.transform.widthBlocks, west = e.transform.seamWestX, east = west + w;
        for (net.minecraft.server.level.ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.level().dimension() != net.minecraft.world.level.Level.OVERWORLD) continue;
            net.minecraft.world.entity.Entity root = p.getRootVehicle();
            double x = root.getX();
            int dx = !wrap ? 0 : x >= east ? -w : x < west ? w : 0;
            // The poles are the edge of the world: walking past 89.5° turns you back.
            double zN = e.transform.blockZ(89.5), zS = e.transform.blockZ(-89.5), z = root.getZ();
            int dz = z < zN ? (int) Math.ceil(zN - z) + 2 : z > zS ? -(int) Math.ceil(z - zS) - 2 : 0;
            if (dz != 0) {
                root.teleport(new net.minecraft.world.level.portal.TeleportTransition((net.minecraft.server.level.ServerLevel) p.level(),
                        new net.minecraft.world.phys.Vec3(0, 0, dz), net.minecraft.world.phys.Vec3.ZERO, 0f, 0f,
                        net.minecraft.world.entity.Relative.ALL, net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING));
                p.sendSystemMessage(net.minecraft.network.chat.Component.literal(z < zN ? "You've reached the North Pole — the edge of the world." : "You've reached the South Pole — the edge of the world."), true);
            }
            if (dx == 0) continue;
            root.teleport(new net.minecraft.world.level.portal.TeleportTransition((net.minecraft.server.level.ServerLevel) p.level(),
                    new net.minecraft.world.phys.Vec3(dx, 0, 0), net.minecraft.world.phys.Vec3.ZERO, 0f, 0f,
                    net.minecraft.world.entity.Relative.ALL, net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING));
            com.seth.worldbound.WorldBoundMod.LOG.info("{} crossed the antimeridian: x {} -> {}", p.getName().getString(), (long) x, (long) (x + dx));
        }
    }

    public static void registerServer() {
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(EarthNet::wrapPlayers);
        PayloadTypeRegistry.clientboundPlay().register(EarthInfoPayload.TYPE, EarthInfoPayload.CODEC);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (!(server.overworld().getChunkSource().getGenerator().getBiomeSource() instanceof EarthBiomeSource)) return;
            if (!ServerPlayNetworking.canSend(handler, EarthInfoPayload.TYPE)) return;
            WorldBoundConfig c = Earth.get().config;
            Earth e = Earth.get();
            ServerPlayNetworking.send(handler.player, new EarthInfoPayload(c.origin_lat, c.origin_lon, c.scale_m_per_block, c.seasons, Earth.wildMode(),
                    e.generated(), e.planetSeed()));
        });
        // Real-world place of death, told to the player (and logged).
        net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof net.minecraft.server.level.ServerPlayer p) || !Earth.earthWorld() || p.level().dimension() != net.minecraft.world.level.Level.OVERWORLD) return;
            Earth e = Earth.get();
            double lat = e.transform.lat(p.getZ()), lon = e.transform.lon(p.getX());
            String msg = "You died at " + com.seth.worldbound.api.GeoText.latLon(lat, lon) + ", "
                    + com.seth.worldbound.api.WorldBound.near(lat, lon);
            p.sendSystemMessage(net.minecraft.network.chat.Component.literal(msg));
            com.seth.worldbound.WorldBoundMod.LOG.info("{} {}", p.getName().getString(), msg);
        });
    }
}
