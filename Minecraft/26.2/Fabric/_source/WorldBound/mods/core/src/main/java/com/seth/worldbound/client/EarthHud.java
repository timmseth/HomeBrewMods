package com.seth.worldbound.client;

import com.seth.worldbound.Earth;
import com.seth.worldbound.WorldBoundMod;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

import java.util.Locale;

/** Corner readout in Earth worlds: real lat/lon, altitude, heading and the nearest town. Toggle with J. */
public final class EarthHud {
    private EarthHud() {}

    static KeyMapping toggle;

    private static String nearCache = "";
    private static long nearTick = -1000;
    private static double nearLat = 999, nearLon = 999;

    /** The "WorldBound" key category (add-ons put their keys here too). */
    public static KeyMapping.Category CATEGORY;

    static void register() {
        KeyMapping.Category cat = CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(WorldBoundMod.ID, "main"));
        toggle = net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.worldbound.hud", com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM, 74 /* J */, cat));
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (toggle.consumeClick()) {
                var cfg = Earth.baseConfig();
                cfg.hud = !cfg.hud;
                cfg.save();
            }
        });
        net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(WorldBoundMod.ID, "compass"), EarthHud::draw);
    }

    private static void draw(GuiGraphicsExtractor g, net.minecraft.client.DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (!Earth.baseConfig().hud || mc.player == null || !Earth.earthWorld()) return;
        if (mc.player.level().dimension() != net.minecraft.world.level.Level.OVERWORLD) return;
        if (mc.getDebugOverlay().showDebugScreen()) return;
        Earth e = Earth.get();
        var t = e.activeTransform();
        double lat = t.lat(mc.player.getZ()), lon = t.lon(mc.player.getX());
        double altM = e.curve.metres(mc.player.getY());
        float yaw = net.minecraft.util.Mth.wrapDegrees(mc.player.getYRot() + 180f); // 0 = north
        String[] dirs = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        String heading = dirs[Math.floorMod(Math.round(yaw / 45f), 8)];
        long tick = mc.player.tickCount;
        if (tick - nearTick > 40 || tick < nearTick || Math.abs(lat - nearLat) + Math.abs(lon - nearLon) > 0.01) {
            nearCache = e.generated() && !com.seth.worldbound.api.WorldBound.hasNamer() ? "Earthish" : com.seth.worldbound.api.WorldBound.near(lat, lon);
            nearTick = tick;
            nearLat = lat;
            nearLon = lon;
        }
        String l1 = com.seth.worldbound.api.GeoText.latLon(lat, lon) + "   " + heading;
        String l2 = String.format(Locale.ROOT, "%,.0f m  1:%d", altM, Math.round(t.metresPerBlock)) + "   " + nearCache;
        var font = mc.font;
        int w = Math.max(font.width(l1), font.width(l2)) + 8;
        int x = 4, y = 4;
        g.fill(x - 2, y - 2, x + w, y + 22, 0x80000000);
        g.text(font, l1, x + 2, y + 1, 0xFFFFFFFF);
        g.text(font, l2, x + 2, y + 11, 0xFFD0D8E0);
    }
}
