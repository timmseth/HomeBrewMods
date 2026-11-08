package com.seth.worldbound.client;

import com.seth.worldbound.Earth;
import com.seth.worldbound.WorldBoundMod;
import com.seth.worldbound.net.LandmarkActionPayload;
import com.seth.worldbound.net.LandmarkSyncPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Landmark menu key (K), the guide trail of breeze puffs toward the chosen landmark, and its distance readout. */
public final class LandmarkClient {
    static boolean enabled;
    static int at = -1;
    static final Set<Integer> discovered = new HashSet<>();
    static final Map<Integer, int[]> placed = new HashMap<>();
    /** Landmark being guided to, or -1. */
    static int guide = -1;
    private static KeyMapping key;
    /** Places this player has discovered or travelled to ({lat, lon}) and the travel range around each. */
    static final java.util.List<double[]> anchors = new java.util.ArrayList<>();
    static double rangeKm = 100;

    static void register(KeyMapping.Category cat) {
        key = net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.worldbound.travel", com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM, 75 /* K */, cat));
        ClientPlayNetworking.registerGlobalReceiver(LandmarkSyncPayload.TYPE, (p, ctx) -> {
            enabled = p.enabled();
            discovered.clear();
            for (int i : p.discovered()) discovered.add(i);
            placed.clear();
            for (int i = 0; i < p.placed().length; i++) placed.put(p.placed()[i], new int[]{p.placedX()[i], p.placedZ()[i]});
            if (p.open()) {
                at = p.at();
                Minecraft.getInstance().setScreenAndShow(new LandmarkScreen());
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(com.seth.worldbound.net.TravelPayloads.Anchors.TYPE, (p, ctx) -> {
            anchors.clear();
            double[] a = p.latLon();
            for (int i = 0; i + 1 < a.length; i += 2) anchors.add(new double[]{a[i], a[i + 1]});
            rangeKm = p.rangeKm();
        });
        ClientPlayNetworking.registerGlobalReceiver(com.seth.worldbound.net.TravelPayloads.Progress.TYPE, (p, ctx) -> {
            Minecraft mc = Minecraft.getInstance();
            if (p.done()) {
                if (mc.gui.screen() instanceof TravelScreen) mc.setScreenAndShow(null);
            } else if (mc.gui.screen() instanceof TravelScreen ts) {
                ts.update(p.label(), p.percent());
            } else {
                mc.setScreenAndShow(new TravelScreen(p.label(), p.percent()));
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((h, mc) -> {
            anchors.clear();
            enabled = false;
            guide = -1;
            discovered.clear();
            placed.clear();
        });
        ClientTickEvents.END_CLIENT_TICK.register(LandmarkClient::tick);
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(WorldBoundMod.ID, "landmark_guide"), LandmarkClient::draw);
    }

    /** The travel map: click anywhere inside the green rings (100 km around places you've found) to go there. */
    public static void openMap() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        var t = Earth.get().activeTransform();
        double lat = t.lat(mc.player.getZ()), lon = t.lon(mc.player.getX());
        mc.setScreenAndShow(new EarthPickerScreen(null, lat, lon, "Travel here", ll -> {
            if (ClientPlayNetworking.canSend(com.seth.worldbound.net.TravelPayloads.Request.TYPE))
                ClientPlayNetworking.send(new com.seth.worldbound.net.TravelPayloads.Request(ll[0], ll[1]));
        }).travelMode(new java.util.ArrayList<>(anchors), rangeKm, LandmarkClient::openList));
    }

    public static void openList() {
        if (enabled) requestMenu();
        else if (Minecraft.getInstance().player != null)
            Minecraft.getInstance().player.sendOverlayMessage(Component.literal("Landmark obelisks are off in this world"));
    }

    static void requestMenu() {
        if (ClientPlayNetworking.canSend(LandmarkActionPayload.TYPE)) ClientPlayNetworking.send(new LandmarkActionPayload(LandmarkActionPayload.OPEN, -1));
    }

    static void travel(int i) {
        ClientPlayNetworking.send(new LandmarkActionPayload(LandmarkActionPayload.TRAVEL, i));
    }

    /** Block x, z of landmark i: the obelisk if it's built, otherwise the landmark's own coordinates. */
    static double[] target(int i) {
        int[] c = placed.get(i);
        if (c != null) return new double[]{c[0] + 0.5, c[1] + 0.5};
        if (i < 0 || i >= com.seth.worldbound.api.WorldBound.landmarks().size()) return new double[]{0, 0};
        var lm = com.seth.worldbound.api.WorldBound.landmarks().get(i);
        var t = Earth.get().activeTransform();
        return new double[]{t.blockX(lm.lon()), t.blockZ(lm.lat())};
    }

    /** {dx, dz} from the player to landmark i, the short way around the world. */
    static double[] delta(double px, double pz, int i) {
        double[] t = target(i);
        double dx = t[0] - px, dz = t[1] - pz;
        double w = Earth.get().activeTransform().widthBlocks;
        if (w > 0) dx = dx - Math.rint(dx / w) * w;
        return new double[]{dx, dz};
    }

    private static void tick(Minecraft mc) {
        while (key.consumeClick()) if (mc.player != null && Earth.earthWorld()) openMap();
        if (guide >= com.seth.worldbound.api.WorldBound.landmarks().size()) guide = -1;
        if (guide < 0 || mc.player == null || mc.level == null || mc.level.dimension() != Level.OVERWORLD) return;
        double[] d = delta(mc.player.getX(), mc.player.getZ(), guide);
        double dist = Math.hypot(d[0], d[1]);
        if (dist < 12) {
            mc.player.sendOverlayMessage(Component.literal("§dYou've reached " + com.seth.worldbound.api.GeoText.plain(com.seth.worldbound.api.WorldBound.landmarks().get(guide).name())));
            guide = -1;
            return;
        }
        // A trail of breeze puffs along the ground for the next 30 blocks, drifting forward like a current.
        if (mc.player.tickCount % 2 != 0) return;
        double ux = d[0] / dist, uz = d[1] / dist;
        int phase = (mc.player.tickCount / 2) % 4;
        double reach = Math.min(30, dist);
        for (double s = 3 + phase * 0.75; s <= reach; s += 3) {
            double x = mc.player.getX() + ux * s, z = mc.player.getZ() + uz * s;
            int bx = (int) Math.floor(x), bz = (int) Math.floor(z);
            double y = mc.level.getChunkSource().hasChunk(bx >> 4, bz >> 4)
                    ? mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING, bx, bz) + 0.6 : mc.player.getY() + 0.5;
            if (Math.abs(y - mc.player.getY()) > 24) y = mc.player.getY() + 0.5;   // cliffs / flying: stay near eye line
            mc.level.addParticle(ParticleTypes.SMALL_GUST, x, y, z, ux * 0.05, 0.01, uz * 0.05);
        }
    }

    private static void draw(GuiGraphicsExtractor g, net.minecraft.client.DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (guide < 0 || guide >= com.seth.worldbound.api.WorldBound.landmarks().size() || mc.player == null || mc.getDebugOverlay().showDebugScreen()) return;
        double[] d = delta(mc.player.getX(), mc.player.getZ(), guide);
        double dist = Math.hypot(d[0], d[1]);
        // arrow relative to where you're facing
        double bearing = Math.toDegrees(Math.atan2(-d[0], d[1]));   // Minecraft yaw convention
        double rel = net.minecraft.util.Mth.wrapDegrees(bearing - mc.player.getYRot());
        String[] arrows = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};
        String arrow = arrows[Math.floorMod((int) Math.round(rel / 45.0), 8)];
        String name = com.seth.worldbound.api.GeoText.plain(com.seth.worldbound.api.WorldBound.landmarks().get(guide).name());
        double km = dist * Earth.get().activeTransform().metresPerBlock / 1000;
        String s = String.format(Locale.ROOT, "%s  %s  ·  %,d blocks%s", arrow, name, Math.round(dist),
                km >= 1 ? String.format(Locale.ROOT, "  (%,.0f km)", km) : "");
        var font = mc.font;
        int w = font.width(s) + 10, x = (g.guiWidth() - w) / 2, y = 6;
        g.fill(x, y, x + w, y + 14, 0x90000000);
        g.text(font, s, x + 5, y + 3, discovered.contains(guide) ? 0xFFB0FFC0 : 0xFFE0B0FF);
    }
}
