package com.seth.earthdata;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.seth.worldbound.Earth;
import com.seth.worldbound.Hooks;
import com.seth.worldbound.WorldBoundConfig;
import com.seth.worldbound.WorldBoundMod;
import com.seth.worldbound.WorldInfo;
import com.seth.worldbound.WorldKind;
import com.seth.worldbound.api.EarthCommandsApi;
import com.seth.worldbound.api.GeoText;
import com.seth.worldbound.api.WorldBound;
import com.seth.worldbound.client.Gazetteer;
import com.seth.worldbound.command.EarthCommands;
import com.seth.worldbound.data.Streamer;
import com.seth.worldbound.worldgen.RegionalBiomes;
import net.fabricmc.api.ModInitializer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * Earth Data World Gen: the real Earth for the WorldBound engine. Supplies the "earth" world kind (shipped base
 * data, region packs, streamed fine data, rivers), the named-region biomes, the gazetteer of real places, the spawn
 * screen, and /earth goto | nearby | stream.
 */
public final class EarthDataMod implements ModInitializer {
    public static final String ID = "earthdata";

    @Override
    public void onInitialize() {
        WorldBoundMod.init();
        registerData();
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(StreamHooks::prefetch);
        registerCommands();
    }

    /** The real Earth's data, region biomes, streaming and places (no game hooks: the tests call this too). */
    public static void registerData() {
        Earth.registerKind(new WorldKind() {
            @Override public String id() { return "earth"; }

            @Override public Earth.Source defaultSource(WorldBoundConfig cfg, Path dataDir) { return RealEarth.INSTANCE; }

            @Override public Earth.Source load(MinecraftServer server, ServerLevel level, WorldInfo w, boolean first) {
                return RealEarth.INSTANCE;
            }
        });
        Hooks.regional = new Hooks.Regional() {
            @Override public Set<String> allIds() { return RegionalBiomes.allIds(); }

            @Override public List<String> pick(Earth.Surface s, String base, double patch, double patch2) {
                return RegionalBiomes.pick(s, base, patch, patch2);
            }
        };
        Hooks.streaming = new Hooks.Streaming() {
            @Override public void chunk(Earth earth, int blockX, int blockZ) { StreamHooks.chunk(earth, blockX, blockZ); }

            @Override public String status() { return Streamer.enabled() ? Streamer.get().status() : "streaming off"; }

            @Override public void prepareSpawn(double lon, double lat, int radiusTiles) {
                if (Streamer.enabled()) Streamer.get().prepareSpawn(lon, lat, radiusTiles);
            }
        };
        WorldBound.setPlaces(new GazetteerPlaces());
    }

    private static void registerCommands() {
        EarthCommandsApi.register(earth -> earth
                .then(Commands.literal("stream").executes(c -> {
                    String m = Hooks.streaming.status();
                    c.getSource().sendSuccess(() -> Component.literal(m), false);
                    return 1;
                }))
                .then(Commands.literal("goto").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("place", StringArgumentType.greedyString())
                                .suggests((ctx, b) -> {
                                    String q = b.getRemaining();
                                    if (q.length() >= 2) for (var p : Gazetteer.get().search(q, 12)) b.suggest(GeoText.plain(p.name()));
                                    return b.buildFuture();
                                })
                                .executes(EarthDataMod::gotoPlace)))
                .then(Commands.literal("nearby").executes(EarthDataMod::nearby)));
    }

    /** Teleport to a named place: city, country, US state or ZIP, landmark. */
    private static int gotoPlace(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        if (Earth.get().generated()) {
            c.getSource().sendFailure(Component.literal("This is an Earthish world: there are no real places on it."));
            return 0;
        }
        String q = StringArgumentType.getString(c, "place");
        var hits = Gazetteer.get().search(q, 1);
        if (hits.isEmpty()) {
            c.getSource().sendFailure(Component.literal("No place called \"" + q + "\" - try a city, country, US state, ZIP or landmark."));
            return 0;
        }
        var p = hits.get(0);
        return EarthCommands.teleport(c, p.lat(), p.lon(), p.label());
    }

    /** The nearest landmarks, with distance and direction. */
    private static int nearby(CommandContext<CommandSourceStack> c) {
        Earth e = Earth.get();
        if (e.generated()) {
            c.getSource().sendFailure(Component.literal("This is an Earthish world: there are no real places on it."));
            return 0;
        }
        var t = e.activeTransform();
        double lat = t.lat(c.getSource().getPosition().z), lon = t.lon(c.getSource().getPosition().x);
        var g = Gazetteer.get();
        StringBuilder sb = new StringBuilder("Near you: ").append(g.near(lat, lon)).append(". Landmarks:");
        for (var p : g.nearestLandmarks(lat, lon, 3)) {
            sb.append(String.format(java.util.Locale.ROOT, "\n  %s, %,.0f km %s", p.name(),
                    GeoText.distanceKm(lat, lon, p.lat(), p.lon()), GeoText.direction(lat, lon, p.lat(), p.lon())));
        }
        String m = sb.toString();
        c.getSource().sendSuccess(() -> Component.literal(m), false);
        return 1;
    }
}
