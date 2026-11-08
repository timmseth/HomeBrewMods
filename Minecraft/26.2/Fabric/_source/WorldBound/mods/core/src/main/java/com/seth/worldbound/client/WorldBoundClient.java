package com.seth.worldbound.client;

import com.seth.worldbound.Earth;
import com.seth.worldbound.WorldKind;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Engine client hooks: the compass HUD, the built-in horizon, Distant Horizons, and the server's world info. */
public final class WorldBoundClient implements ClientModInitializer {
    /** Opens the in-world travel map (set by Landmarks); null when nothing provides one. */
    public static volatile Runnable travelMap;

    private static boolean initialised;

    @Override
    public void onInitializeClient() {
        init();
    }

    public static synchronized void init() {
        if (initialised) return;
        initialised = true;
        EarthHud.register();
        com.seth.worldbound.client.horizon.Horizon.register();
        if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("distanthorizons"))
            net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(
                    com.seth.worldbound.compat.DhClientCompat::tick);
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(com.seth.worldbound.net.EarthInfoPayload.TYPE,
                (p, ctx) -> {
                    Earth.setRemoteWorld(new com.seth.worldbound.geo.GeoTransform(p.originLat(), p.originLon(), p.scale()), p.seasons());
                    Earth.setWildMode(p.wild());
                    // a dedicated Earthish server: build the same planet here (in the background) for the map and horizon
                    if (p.planet() && !ctx.client().hasSingleplayerServer()) {
                        Earth cur = Earth.get();
                        WorldKind k = Earth.kind("earthish");
                        if (k != null && (!cur.generated() || cur.planetSeed() != p.planetSeed())) {
                            Thread t = new Thread(() -> {
                                Earth.Source src = k.remote(p.planetSeed());
                                if (src != null) Earth.useWorld(p.scale(), p.originLat(), p.originLon(), src);
                            }, "worldbound-planet");
                            t.setDaemon(true);
                            t.setPriority(Thread.MIN_PRIORITY);
                            t.start();
                        }
                    }
                });
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((h, mc) -> {
            Earth.setRemoteWorld(null, false);
            Earth.setWildMode(false);
            if (!mc.hasSingleplayerServer() && Earth.get().generated()) Earth.resetToReal();
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, ctx) -> dispatcher.register(
                ClientCommands.literal("earthhorizon")
                        .executes(c -> { Minecraft mc = c.getSource().getClient(); mc.execute(() -> mc.setScreenAndShow(new com.seth.worldbound.client.horizon.HorizonScreen(null))); return 1; })
                        .then(ClientCommands.literal("status").executes(c -> { c.getSource().sendFeedback(Component.literal(com.seth.worldbound.client.horizon.Horizon.get().status())); return 1; }))
                        .then(ClientCommands.literal("on").executes(c -> horizon(c.getSource(), true)))
                        .then(ClientCommands.literal("off").executes(c -> horizon(c.getSource(), false)))
                        .then(ClientCommands.literal("reload").executes(c -> { com.seth.worldbound.client.horizon.Horizon.get().clear(); c.getSource().sendFeedback(Component.literal("horizon tiles rebuilt")); return 1; }))));
    }

    private static int horizon(net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource src, boolean on) {
        com.seth.worldbound.client.horizon.Horizon.setEnabled(on);
        src.sendFeedback(Component.literal("WorldBound horizon " + (on ? "on" : "off")));
        return 1;
    }
}
