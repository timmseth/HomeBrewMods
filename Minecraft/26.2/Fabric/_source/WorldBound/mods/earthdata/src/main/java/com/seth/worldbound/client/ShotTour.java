package com.seth.worldbound.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.seth.worldbound.Earth;
import com.seth.worldbound.WorldBoundMod;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Screenshot tour: saved views (`/earthshot add`) and an automatic view of every landmark from above at 45°,
 * each at day, sunset, night and sunrise, saved as screenshots/worldbound/<view>__<time>__<version>.png with a
 * gallery.html alongside. Runs in spectator mode and restores your game mode afterwards.
 */
public final class ShotTour {
    public static final class Shot {
        public String name;
        public double x, y, z;
        public float yaw, pitch;
        Shot() {}
        Shot(String name, double x, double y, double z, float yaw, float pitch) {
            this.name = name; this.x = x; this.y = y; this.z = z; this.yaw = yaw; this.pitch = pitch;
        }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String[] TIMES = {"day", "sunset", "night", "sunrise"};
    private static final int[] TICKS = {6000, 12400, 18000, 23200};
    private static List<Shot> saved;

    // tour state
    private static final List<Shot> queue = new ArrayList<>();
    private static int index, timeIndex, phase, ticks, failures;
    private static boolean running, hudWasHidden;
    private static String[] times = TIMES;
    private static int[] ticksOfDay = TICKS;
    private static String version = "dev";
    private static int minSettle = 120, maxWait = 600;

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("worldbound_shots.json");
    }

    static List<Shot> saved() {
        if (saved == null) {
            try {
                saved = Files.exists(file()) ? GSON.fromJson(Files.readString(file()), new TypeToken<List<Shot>>() {}.getType()) : null;
            } catch (Exception e) {
                WorldBoundMod.LOG.warn("shots: {}", e.toString());
            }
            if (saved == null) saved = new ArrayList<>();
        }
        return saved;
    }

    static void save() {
        try {
            Files.writeString(file(), GSON.toJson(saved()));
        } catch (Exception e) {
            WorldBoundMod.LOG.warn("shots: {}", e.toString());
        }
    }

    public static void register() {
        FabricLoader.getInstance().getModContainer(WorldBoundMod.ID).ifPresent(c -> version = c.getMetadata().getVersion().getFriendlyString());
        ClientCommandRegistrationCallback.EVENT.register((d, ctx) -> d.register(ClientCommands.literal("earthshot")
                .then(ClientCommands.literal("add").then(ClientCommands.argument("name", StringArgumentType.greedyString()).executes(c -> {
                    Minecraft mc = c.getSource().getClient();
                    if (mc.player == null) return 0;
                    String name = StringArgumentType.getString(c, "name").trim();
                    saved().removeIf(s -> s.name.equalsIgnoreCase(name));
                    saved().add(new Shot(name, mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYRot(), mc.player.getXRot()));
                    save();
                    say(mc, "Saved view \"" + name + "\" (" + saved().size() + " saved). /earthshot tour views");
                    return 1;
                })))
                .then(ClientCommands.literal("remove").then(ClientCommands.argument("name", StringArgumentType.greedyString()).executes(c -> {
                    String name = StringArgumentType.getString(c, "name").trim();
                    boolean ok = saved().removeIf(s -> s.name.equalsIgnoreCase(name));
                    save();
                    say(c.getSource().getClient(), ok ? "Removed \"" + name + "\"" : "No view called \"" + name + "\"");
                    return 1;
                })))
                .then(ClientCommands.literal("list").executes(c -> {
                    StringBuilder sb = new StringBuilder("Saved views (" + saved().size() + "):");
                    for (Shot s : saved()) sb.append("\n  ").append(s.name).append(String.format(Locale.ROOT, "  (%.0f, %.0f, %.0f)", s.x, s.y, s.z));
                    say(c.getSource().getClient(), sb.toString());
                    return 1;
                }))
                .then(ClientCommands.literal("go").then(ClientCommands.argument("name", StringArgumentType.greedyString()).executes(c -> {
                    String name = StringArgumentType.getString(c, "name").trim();
                    for (Shot s : saved()) if (s.name.equalsIgnoreCase(name)) { tp(c.getSource().getClient(), s); return 1; }
                    say(c.getSource().getClient(), "No view called \"" + name + "\"");
                    return 0;
                })))
                .then(ClientCommands.literal("stop").executes(c -> { stop(c.getSource().getClient()); return 1; }))
                .then(ClientCommands.literal("gallery").executes(c -> { gallery(c.getSource().getClient()); return 1; }))
                .then(ClientCommands.literal("tour")
                        .executes(c -> start(c.getSource().getClient(), "all", "all"))
                        .then(ClientCommands.argument("what", StringArgumentType.word())
                                .executes(c -> start(c.getSource().getClient(), StringArgumentType.getString(c, "what"), "all"))
                                .then(ClientCommands.argument("times", StringArgumentType.word())
                                        .executes(c -> start(c.getSource().getClient(), StringArgumentType.getString(c, "what"), StringArgumentType.getString(c, "times"))))))));
        ClientTickEvents.END_CLIENT_TICK.register(ShotTour::tick);
    }

    private static void say(Minecraft mc, String s) {
        if (mc.player != null) mc.player.sendSystemMessage(Component.literal(s));
    }

    private static void cmd(Minecraft mc, String c) {
        if (mc.player != null) mc.player.connection.sendCommand(c);
    }

    private static void tp(Minecraft mc, Shot s) {
        cmd(mc, String.format(Locale.ROOT, "tp @s %.2f %.2f %.2f %.1f %.1f", s.x, s.y, s.z, s.yaw, s.pitch));
    }

    /** Every landmark seen from the south-east, 45° down, far enough back to take in the whole feature. */
    static List<Shot> landmarkViews() {
        List<Shot> l = new ArrayList<>();
        Earth e = Earth.get();
        var t = e.activeTransform();
        for (com.seth.worldbound.api.Places.Place p : Gazetteer.get().landmarks()) {
            double tx = t.blockX(p.lon()), tz = t.blockZ(p.lat());
            int ground = Math.max(e.at((int) Math.floor(tx), (int) Math.floor(tz)).surfaceY(), e.curve.seaLevelY);
            double d = 230;   // horizontal stand-off; inside a 16-chunk render distance
            double cx = tx + d * 0.7071, cz = tz + d * 0.7071;   // south-east of the target
            int under = Math.max(e.at((int) Math.floor(cx), (int) Math.floor(cz)).surfaceY(), e.curve.seaLevelY);
            double cy = Math.max(ground + d, under + 60);        // 45° down to the target, never inside a hill
            float yaw = (float) Math.toDegrees(Math.atan2(-(tx - cx), tz - cz));   // Minecraft yaw: 0 = +z, 90 = -x
            float pitch = (float) Math.toDegrees(Math.atan2(cy - ground, d));
            l.add(new Shot(Gazetteer.plain(p.name()), cx, cy, cz, yaw, pitch));
        }
        return l;
    }

    private static int start(Minecraft mc, String what, String which) {
        if (mc.player == null || !Earth.earthWorld()) { say(mc, "Join an Earth world first"); return 0; }
        if (running) { say(mc, "A tour is already running (/earthshot stop)"); return 0; }
        queue.clear();
        if (what.equals("views") || what.equals("all")) queue.addAll(saved());
        if (what.equals("landmarks") || what.equals("all")) queue.addAll(landmarkViews());
        if (queue.isEmpty()) { say(mc, "Nothing to shoot: /earthshot add <name> or use \"landmarks\""); return 0; }
        if (which.equals("all")) { times = TIMES; ticksOfDay = TICKS; }
        else {
            List<String> tn = new ArrayList<>(); List<Integer> tt = new ArrayList<>();
            for (int i = 0; i < TIMES.length; i++) if (which.contains(TIMES[i])) { tn.add(TIMES[i]); tt.add(TICKS[i]); }
            if (tn.isEmpty()) { say(mc, "times: all, or any of day,sunset,night,sunrise (e.g. day,sunset)"); return 0; }
            times = tn.toArray(new String[0]); ticksOfDay = tt.stream().mapToInt(Integer::intValue).toArray();
        }
        index = 0; timeIndex = 0; phase = 0; ticks = 0; failures = 0; running = true;
        hudWasHidden = mc.gui.hud.isHidden();
        modeBefore = mc.gameMode != null && mc.gameMode.getPlayerMode() != null ? mc.gameMode.getPlayerMode().getName() : "creative";
        cmd(mc, "gamemode spectator");
        cmd(mc, "gamerule advance_time false"); // 26.2 name (was doDaylightCycle)
        say(mc, String.format(Locale.ROOT, "Tour: %d views × %d times = %d screenshots (~%d min). /earthshot stop to cancel.",
                queue.size(), times.length, queue.size() * times.length, queue.size() * times.length * 9 / 60 + 1));
        return 1;
    }

    private static String modeBefore = "creative";

    private static void stop(Minecraft mc) {
        if (!running) return;
        running = false;
        if (mc.gui.hud.isHidden() != hudWasHidden) mc.gui.hud.toggle();
        cmd(mc, "gamerule advance_time true");
        cmd(mc, "gamemode " + modeBefore);
        say(mc, "Tour finished: screenshots/worldbound/  (gallery.html opens them side by side)");
        gallery(mc);
    }

    private static void tick(Minecraft mc) {
        if (!running) return;
        if (mc.player == null || mc.level == null) { running = false; return; }
        if (index >= queue.size()) { stop(mc); return; }
        Shot s = queue.get(index);
        switch (phase) {
            case 0 -> {   // go there
                cmd(mc, "time set " + ticksOfDay[timeIndex]);
                tp(mc, s);
                ticks = 0;
                phase = 1;
            }
            case 1 -> {   // wait for the world to be there
                ticks++;
                boolean near = mc.player.distanceToSqr(s.x, s.y, s.z) < 4;
                boolean drawn = mc.levelRenderer.hasRenderedAllSections();
                if (near && ticks >= minSettle && (drawn || ticks >= maxWait)) {
                    if (!mc.gui.hud.isHidden()) mc.gui.hud.toggle();
                    ticks = 0;
                    phase = 2;
                } else if (!near && ticks > 100) {   // the teleport didn't land; retry once
                    if (failures++ < 2) phase = 0; else { next(mc); }
                }
            }
            case 2 -> {   // a few frames without HUD, then shoot
                if (++ticks < 3) return;
                String file = "worldbound/" + safe(s.name) + "__" + times[timeIndex] + "__v" + version + ".png";
                new File(mc.gameDirectory, Screenshot.SCREENSHOT_DIR + "/worldbound").mkdirs();
                Screenshot.grab(mc.gameDirectory, file, mc.gameRenderer.mainRenderTarget(), 1, msg -> {});
                phase = 3;
                ticks = 0;
            }
            case 3 -> {   // let the grab finish, HUD back, next
                if (++ticks < 5) return;
                if (mc.gui.hud.isHidden() != hudWasHidden) mc.gui.hud.toggle();
                next(mc);
            }
            default -> {}
        }
    }

    private static void next(Minecraft mc) {
        failures = 0;
        if (++timeIndex >= times.length) {
            timeIndex = 0;
            index++;
            if (index < queue.size() && index % 5 == 0)
                mc.player.sendOverlayMessage(Component.literal("Tour " + index + " / " + queue.size() + ": " + queue.get(index).name));
        }
        phase = 0;
    }

    private static String safe(String s) {
        return s.replaceAll("[^A-Za-z0-9._-]+", "_");
    }

    /** gallery.html: one row per view, a column per time of day; the newest version large, the previous one small beneath. */
    static void gallery(Minecraft mc) {
        try {
            File dir = new File(mc.gameDirectory, Screenshot.SCREENSHOT_DIR + "/worldbound");
            File[] files = dir.listFiles((d, n) -> n.endsWith(".png"));
            if (files == null) return;
            // view -> time -> version -> file
            Map<String, Map<String, TreeMap<String, String>>> m = new TreeMap<>();
            for (File f : files) {
                String[] p = f.getName().replace(".png", "").split("__");
                if (p.length != 3) continue;
                m.computeIfAbsent(p[0], k -> new TreeMap<>()).computeIfAbsent(p[1], k -> new TreeMap<>(ShotTour::compareVersions)).put(p[2], f.getName());
            }
            StringBuilder h = new StringBuilder("<!doctype html><meta charset=utf-8><title>WorldBound screenshot tour</title><style>body{background:#111;color:#ddd;font:14px system-ui;margin:16px}"
                    + "h2{margin:24px 0 6px}img{width:100%;display:block;border-radius:4px}.row{display:grid;grid-template-columns:repeat(4,1fr);gap:8px}.c{background:#1a1a1a;padding:6px;border-radius:6px}"
                    + ".c small{color:#888}.old img{opacity:.85;margin-top:6px}</style><h1>WorldBound screenshot tour</h1><p>" + m.size() + " views. Newest version large; the previous version small beneath it where one exists.</p>");
            for (var v : m.entrySet()) {
                h.append("<h2>").append(v.getKey().replace('_', ' ')).append("</h2><div class=row>");
                for (String t : TIMES) {
                    var vers = v.getValue().get(t);
                    h.append("<div class=c><small>").append(t);
                    if (vers == null) { h.append("</small></div>"); continue; }
                    String newest = vers.lastKey();
                    h.append(" · ").append(newest).append("</small><a href=\"").append(vers.get(newest)).append("\"><img loading=lazy src=\"").append(vers.get(newest)).append("\"></a>");
                    if (vers.size() > 1) {
                        String prev = vers.lowerKey(newest);
                        h.append("<div class=old><small>").append(prev).append("</small><img loading=lazy src=\"").append(vers.get(prev)).append("\"></div>");
                    }
                    h.append("</div>");
                }
                h.append("</div>");
            }
            Files.writeString(new File(dir, "gallery.html").toPath(), h.toString());
        } catch (Exception e) {
            WorldBoundMod.LOG.warn("gallery: {}", e.toString());
        }
    }

    private static int compareVersions(String a, String b) {
        String[] x = a.replace("v", "").split("\\."), y = b.replace("v", "").split("\\.");
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            int p = i < x.length ? parse(x[i]) : 0, q = i < y.length ? parse(y[i]) : 0;
            if (p != q) return Integer.compare(p, q);
        }
        return a.compareTo(b);
    }

    private static int parse(String s) {
        try { return Integer.parseInt(s.replaceAll("\\D.*", "")); } catch (Exception e) { return 0; }
    }
}
