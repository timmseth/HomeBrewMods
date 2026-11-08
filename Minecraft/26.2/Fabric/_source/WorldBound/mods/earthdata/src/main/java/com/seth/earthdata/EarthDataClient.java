package com.seth.earthdata;

import com.seth.worldbound.Earth;
import com.seth.worldbound.WorldBoundConfig;
import com.seth.worldbound.client.EarthPickerScreen;
import com.seth.worldbound.client.Gazetteer;
import com.seth.worldbound.client.ShotTour;
import com.seth.worldbound.client.WorldBoundClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.presets.WorldPreset;

import java.util.Locale;

/** The "Earth: choose spawn" button on the Create World screen, /earthmap, and the place list loaded in the background. */
public final class EarthDataClient implements ClientModInitializer {
    private static final ResourceKey<WorldPreset> EARTH = ResourceKey.create(Registries.WORLD_PRESET, Identifier.fromNamespaceAndPath("worldbound", "earth"));

    @Override
    public void onInitializeClient() {
        WorldBoundClient.init();
        if (com.seth.worldbound.api.EarthCommandsApi.debug()) ShotTour.register();
        // Load the place list off-thread so the compass doesn't hitch the first frame.
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.JOIN.register((h, sender, mc) ->
                java.util.concurrent.CompletableFuture.runAsync(Gazetteer::get));
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (!(screen instanceof CreateWorldScreen cws)) return;
            WorldCreationUiState ui = cws.getUiState();
            WorldBoundConfig cfg = Earth.baseConfig();
            // Resizing the window runs this again, but the Create World screen keeps its widgets through a resize:
            // take our old button off before adding the one placed for the new size, or they pile up.
            Button old = SPAWN_BUTTON.get(screen);
            if (old != null) Screens.getWidgets(screen).remove(old);
            Button b = Button.builder(Component.literal(spawnLabel(cfg)), bt -> client.setScreenAndShow(
                    new EarthPickerScreen(cws, cfg.spawnLat(), cfg.spawnLon(), "Use as spawn", ll -> {
                        cfg.spawn_lat = ll[0];
                        cfg.spawn_lon = ll[1];
                        cfg.save();
                    }))).bounds(w / 2 - 155, h - 28 - 24, 150, 20).build();
            b.visible = isEarth(ui);
            SPAWN_BUTTON.put(screen, b);
            if (old == null) ui.addListener(u -> {
                Button cur = SPAWN_BUTTON.get(screen);
                if (cur == null) return;
                cur.visible = isEarth(u);
                cur.setMessage(Component.literal(spawnLabel(cfg)));
            });
            Screens.getWidgets(screen).add(b);
        });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, ctx) -> dispatcher.register(
                ClientCommands.literal("earthmap").executes(c -> {
                    Minecraft mc = c.getSource().getClient();
                    double lat = Earth.get().config.origin_lat, lon = Earth.get().config.origin_lon;
                    if (mc.player != null) {
                        lat = Earth.get().activeTransform().lat(mc.player.getZ());
                        lon = Earth.get().activeTransform().lon(mc.player.getX());
                    }
                    final double fl = lat, fo = lon;
                    // In an Earth world with Landmarks the map is the travel map (range-limited); otherwise a viewer.
                    mc.execute(() -> {
                        Runnable travel = WorldBoundClient.travelMap;
                        if (mc.player != null && Earth.earthWorld() && travel != null) travel.run();
                        else mc.setScreenAndShow(new EarthPickerScreen(null, fl, fo, "Close", ll -> {}));
                    });
                    return 1;
                })));
    }

    /** The spawn button on each open Create World screen (weak: screens come and go). */
    private static final java.util.Map<net.minecraft.client.gui.screens.Screen, Button> SPAWN_BUTTON = new java.util.WeakHashMap<>();

    private static boolean isEarth(WorldCreationUiState ui) {
        try {
            WorldCreationUiState.WorldTypeEntry e = ui.getWorldType();
            Holder<WorldPreset> h = e == null ? null : e.preset();
            return h != null && h.is(EARTH);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private static String spawnLabel(WorldBoundConfig cfg) {
        return String.format(Locale.ROOT, "Earth: %.2f, %.2f · 1:%d", cfg.spawnLat(), cfg.spawnLon(), (int) cfg.new_world_scale);
    }
}
