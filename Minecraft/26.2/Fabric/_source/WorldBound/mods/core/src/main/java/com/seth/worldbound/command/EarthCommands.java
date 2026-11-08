package com.seth.worldbound.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.seth.worldbound.Earth;
import com.seth.worldbound.data.Tiling;
import com.seth.worldbound.worldgen.BiomeTable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;

/** /earth where | tp &lt;lat&gt; &lt;lon&gt; | status */
public final class EarthCommands {
    private EarthCommands() {}

    /** Developer commands: only with debug_commands on (config) or -Dworldbound.debug=true, and for operators. */
    static java.util.function.Predicate<CommandSourceStack> dbg() {
        var op = Commands.<CommandSourceStack>hasPermission(Commands.LEVEL_GAMEMASTERS);
        return s -> com.seth.worldbound.api.EarthCommandsApi.debug() && op.test(s);
    }

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        var earth = Commands.literal("earth")
                .then(Commands.literal("where").executes(EarthCommands::where))
                .then(Commands.literal("status").executes(EarthCommands::status))
                .then(Commands.literal("trees").requires(dbg()).executes(c -> {
                    String m = "trees seen " + com.seth.worldbound.worldgen.TreeStats.SEEN + ", thinned " + com.seth.worldbound.worldgen.TreeStats.THINNED
                            + ", extra tried " + com.seth.worldbound.worldgen.TreeStats.EXTRA_TRY + ", extra placed " + com.seth.worldbound.worldgen.TreeStats.EXTRA_OK + ", failed " + com.seth.worldbound.worldgen.TreeStats.FAILED + ", nocover " + com.seth.worldbound.worldgen.TreeStats.NOCOVER + ", high " + com.seth.worldbound.worldgen.TreeStats.HIGH;
                    c.getSource().sendSuccess(() -> Component.literal(m), false);
                    return 1;
                }))
                .then(Commands.literal("lod").requires(dbg()).executes(c -> {
                    String m = net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("distanthorizons")
                            ? com.seth.worldbound.compat.DhCompat.status() : "Distant Horizons is not installed";
                    c.getSource().sendSuccess(() -> Component.literal(m), false);
                    return 1;
                }).then(Commands.literal("test").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).then(Commands.argument("lat", DoubleArgumentType.doubleArg(-90, 90))
                        .then(Commands.argument("lon", DoubleArgumentType.doubleArg(-180, 180)).executes(c -> {
                            double lat = DoubleArgumentType.getDouble(c, "lat"), lon = DoubleArgumentType.getDouble(c, "lon");
                            String m = net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("distanthorizons")
                                    ? com.seth.worldbound.compat.DhCompat.test(lat, lon) : "Distant Horizons is not installed";
                            c.getSource().sendSuccess(() -> Component.literal(m), false);
                            return 1;
                        })))))
                .then(Commands.literal("column").requires(dbg()).then(Commands.argument("x", com.mojang.brigadier.arguments.IntegerArgumentType.integer())
                        .then(Commands.argument("z", com.mojang.brigadier.arguments.IntegerArgumentType.integer()).executes(c -> {
                    int x = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "x"), z = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "z");
                    var level = c.getSource().getLevel();
                    level.getChunk(x >> 4, z >> 4);
                    int top = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x, z);
                    int water = 0;
                    while (top > level.getMinY() + 2 && level.getBlockState(new net.minecraft.core.BlockPos(x, top - 1, z)).is(net.minecraft.world.level.block.Blocks.WATER)) { top--; water++; }
                    StringBuilder sb = new StringBuilder("column " + x + "," + z + (water > 0 ? " water x" + water + " down to " : " top ") + top + " (minY " + level.getMinY() + "):");
                    String last = null; int run = 0;
                    for (int y = top; y > top - 40; y--) {
                        String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(level.getBlockState(new net.minecraft.core.BlockPos(x, y, z)).getBlock()).getPath();
                        if (id.equals(last)) { run++; continue; }
                        if (last != null) sb.append(' ').append(last).append(run > 1 ? "x" + run : "");
                        last = id; run = 1;
                    }
                    sb.append(' ').append(last).append(run > 1 ? "x" + run : "");
                    String m = sb.toString();
                    c.getSource().sendSuccess(() -> Component.literal(m), false);
                    return 1;
                }))))
                .then(Commands.literal("spawn").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("here").executes(EarthCommands::spawnHere))
                        .then(Commands.argument("lat", DoubleArgumentType.doubleArg(-90, 90))
                                .then(Commands.argument("lon", DoubleArgumentType.doubleArg(-180, 180))
                                        .executes(EarthCommands::spawnAt))))
                .then(Commands.literal("bench").requires(dbg())
                        .then(Commands.argument("lat", DoubleArgumentType.doubleArg(-90, 90))
                                .then(Commands.argument("lon", DoubleArgumentType.doubleArg(-180, 180))
                                        .then(Commands.argument("radius", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 16))
                                                .executes(EarthCommands::bench)))))
                .then(Commands.literal("scan").requires(dbg())
                        .then(Commands.argument("lat", DoubleArgumentType.doubleArg(-90, 90))
                                .then(Commands.argument("lon", DoubleArgumentType.doubleArg(-180, 180))
                                        .executes(EarthCommands::scan))))
                .then(Commands.literal("structcheck").requires(dbg())
                        .then(Commands.argument("id", com.mojang.brigadier.arguments.StringArgumentType.string())
                                .then(Commands.argument("lat", DoubleArgumentType.doubleArg(-90, 90))
                                        .then(Commands.argument("lon", DoubleArgumentType.doubleArg(-180, 180))
                                                .executes(EarthCommands::structcheck)))))
                .then(Commands.literal("seamtest").requires(dbg()).executes(EarthCommands::seamtest))
                .then(Snapshot.command())
                .then(Commands.literal("sun").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(EarthCommands::sun))
                .then(Commands.literal("probe").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("lat", DoubleArgumentType.doubleArg(-90, 90))
                                .then(Commands.argument("lon", DoubleArgumentType.doubleArg(-180, 180))
                                        .executes(EarthCommands::probe))))
                .then(Commands.literal("tp").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("lat", DoubleArgumentType.doubleArg(-90, 90))
                                .then(Commands.argument("lon", DoubleArgumentType.doubleArg(-180, 180))
                                        .executes(EarthCommands::tp))));
        com.seth.worldbound.api.EarthCommandsApi.apply(earth);
        d.register(earth);
    }

    private static int where(CommandContext<CommandSourceStack> c) {
        Earth e = Earth.get();
        int x = (int) Math.floor(c.getSource().getPosition().x), z = (int) Math.floor(c.getSource().getPosition().z);
        Earth.Surface s = e.at(x, z);
        String msg = String.format("%.5f, %.5f — %.0f m (surface y %d), climate %s (%d), today ~%.0f°C, slope %.2f, biome %s",
                s.lat(), s.lon(), s.elevM(), s.surfaceY(), BiomeTable.code(s.climate()), s.climate(),
                com.seth.worldbound.worldgen.Seasons.celsius(s.lat(), s.climate()), s.slope(),
                c.getSource().getLevel().getBiome(net.minecraft.core.BlockPos.containing(c.getSource().getPosition())).unwrapKey().map(k -> k.identifier().toString()).orElse(BiomeTable.surface(s)));
        var pos = net.minecraft.core.BlockPos.containing(c.getSource().getPosition());
        String wx = c.getSource().getLevel().getBiome(pos).value().getPrecipitationAt(pos, c.getSource().getLevel().getSeaLevel()).toString().toLowerCase();
        String near = com.seth.worldbound.api.WorldBound.near(s.lat(), s.lon());
        String url = String.format(java.util.Locale.ROOT, "https://www.openstreetmap.org/?mlat=%.5f&mlon=%.5f#map=12/%.5f/%.5f", s.lat(), s.lon(), s.lat(), s.lon());
        if (e.generated()) {
            String m = msg + ", weather " + wx + " - " + near;
            c.getSource().sendSuccess(() -> Component.literal(m), false);
            return 1;
        }
        var line = Component.literal(msg + ", weather " + wx + " — " + near + " ").append(Component.literal("[open real map]")
                .withStyle(st -> st.withColor(net.minecraft.ChatFormatting.AQUA).withUnderlined(true)
                        .withClickEvent(new net.minecraft.network.chat.ClickEvent.OpenUrl(java.net.URI.create(url)))));
        c.getSource().sendSuccess(() -> line, false);
        return 1;
    }

    /** Set the time of day to the real sun's position at your longitude right now. */
    private static int sun(CommandContext<CommandSourceStack> c) {
        var t = Earth.get().activeTransform();
        double lon = t.lon(c.getSource().getPosition().x);
        var now = java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC);
        double utcHours = now.getHour() + now.getMinute() / 60.0 + now.getSecond() / 3600.0;
        double solar = ((utcHours + lon / 15.0) % 24 + 24) % 24;
        long ticks = Math.round(((solar - 6.0 + 24) % 24) * 1000.0);
        c.getSource().getServer().getCommands().performPrefixedCommand(c.getSource().withSuppressedOutput(), "time set " + ticks);
        String m = String.format(java.util.Locale.ROOT, "Sun set to real local solar time here: %02d:%02d", (int) solar, (int) ((solar % 1) * 60));
        c.getSource().sendSuccess(() -> Component.literal(m), true);
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> c) {
        Earth e = Earth.get();
        int x = (int) Math.floor(c.getSource().getPosition().x), z = (int) Math.floor(c.getSource().getPosition().z);
        Earth.Surface s = e.at(x, z);
        String msg = String.format("scale 1:%.0f, origin %.3f,%.3f, sea y%d | packs: %s | cache %s | here: elev=%s climate=%s cover=%s",
                e.config.scale_m_per_block, e.config.origin_lat, e.config.origin_lon, e.curve.seaLevelY,
                String.join("; ", e.packs), e.stack.cacheStatus(),
                e.stack.describe(Tiling.LAYER_ELEV, s.lon(), s.lat()),
                e.stack.describe(Tiling.LAYER_CLIMATE, s.lon(), s.lat()),
                e.stack.describe(Tiling.LAYER_COVER, s.lon(), s.lat()));
        c.getSource().sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }

    private static int spawnHere(CommandContext<CommandSourceStack> c) {
        Earth e = Earth.get();
        double lat = e.transform.lat(c.getSource().getPosition().z), lon = e.transform.lon(c.getSource().getPosition().x);
        return spawn(c, lat, lon);
    }

    private static int spawnAt(CommandContext<CommandSourceStack> c) {
        return spawn(c, DoubleArgumentType.getDouble(c, "lat"), DoubleArgumentType.getDouble(c, "lon"));
    }

    private static int spawn(CommandContext<CommandSourceStack> c, double lat, double lon) {
        int y = com.seth.worldbound.WorldBoundMod.setSpawn(c.getSource().getLevel(), lat, lon);
        String msg = String.format("world spawn set to %.4f, %.4f (y %d)", lat, lon, y);
        c.getSource().sendSuccess(() -> Component.literal(msg), true);
        return 1;
    }

    /** Generate a square of chunks around a lat/lon and report the time per chunk (testing aid). */
    private static int bench(CommandContext<CommandSourceStack> c) {
        Earth e = Earth.get();
        double lat = DoubleArgumentType.getDouble(c, "lat"), lon = DoubleArgumentType.getDouble(c, "lon");
        int r = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "radius");
        int cx = ((int) Math.floor(e.transform.blockX(lon))) >> 4, cz = ((int) Math.floor(e.transform.blockZ(lat))) >> 4;
        var level = c.getSource().getLevel();
        long t0 = System.nanoTime();
        int n = 0;
        for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) { level.getChunk(cx + dx, cz + dz); n++; }
        double ms = (System.nanoTime() - t0) / 1e6;
        String msg = String.format(java.util.Locale.ROOT, "bench %.3f,%.3f: %d chunks in %.0f ms = %.1f ms/chunk", lat, lon, n, ms, ms / n);
        c.getSource().sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }

    /** Count notable blocks in the 3x3 chunks around a lat/lon: cave air, ores (testing aid). */
    private static int scan(CommandContext<CommandSourceStack> c) {
        Earth e = Earth.get();
        double lat = DoubleArgumentType.getDouble(c, "lat"), lon = DoubleArgumentType.getDouble(c, "lon");
        int cx = ((int) Math.floor(e.transform.blockX(lon))) >> 4, cz = ((int) Math.floor(e.transform.blockZ(lat))) >> 4;
        var level = c.getSource().getLevel();
        java.util.TreeMap<String, Integer> n = new java.util.TreeMap<>();
        int caveAir = 0;
        var mp = new net.minecraft.core.BlockPos.MutableBlockPos();
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            var ch = level.getChunk(cx + dx, cz + dz);
            for (int i = 0; i < 256; i++) {
                int x = ((cx + dx) << 4) + (i & 15), z = ((cz + dz) << 4) + (i >> 4);
                int top = e.at(x, z).solidTop();
                for (int y = top + 1; y < top + 40; y++) {
                    var st = ch.getBlockState(mp.set(x, y, z));
                    if (net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(st.getBlock()).getPath().endsWith("_log")) n.merge("tree logs", 1, Integer::sum);
                }
                for (int y = top - 300; y < top - 4; y++) {
                    var st = ch.getBlockState(mp.set(x, y, z));
                    String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(st.getBlock()).getPath();
                    if (st.isAir()) caveAir++;
                    else if (id.endsWith("_ore") || id.equals("amethyst_block") || id.equals("lava") || id.equals("water") || id.equals("spawner")) n.merge(id, 1, Integer::sum);
                }
            }
        }
        String msg = String.format(java.util.Locale.ROOT, "scan %.3f,%.3f: cave air %d, %s", lat, lon, caveAir, n);
        c.getSource().sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }

    /** Find the nearest structure of a type, generate it, and compare its box with the real ground (testing aid). */
    private static int structcheck(CommandContext<CommandSourceStack> c) {
        Earth e = Earth.get();
        var level = c.getSource().getLevel();
        String id = com.mojang.brigadier.arguments.StringArgumentType.getString(c, "id");
        double lat = DoubleArgumentType.getDouble(c, "lat"), lon = DoubleArgumentType.getDouble(c, "lon");
        var reg = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        var holder = reg.get(net.minecraft.resources.Identifier.parse(id)).orElse(null);
        if (holder == null) { c.getSource().sendFailure(Component.literal("no structure " + id)); return 0; }
        var from = new net.minecraft.core.BlockPos((int) e.transform.blockX(lon), 0, (int) e.transform.blockZ(lat));
        var found = level.getChunkSource().getGenerator().findNearestMapStructure(level, net.minecraft.core.HolderSet.direct(holder), from, 100, false);
        if (found == null) { c.getSource().sendSuccess(() -> Component.literal("structcheck " + id + ": none within 100 chunks"), false); return 1; }
        var p = found.getFirst();
        level.getChunk(p.getX() >> 4, p.getZ() >> 4);
        var start = level.structureManager().getStartForStructure(net.minecraft.core.SectionPos.of(p), holder.value(),
                level.getChunk(p.getX() >> 4, p.getZ() >> 4, net.minecraft.world.level.chunk.status.ChunkStatus.STRUCTURE_STARTS));
        if (start == null || !start.isValid()) { c.getSource().sendSuccess(() -> Component.literal("structcheck " + id + " at " + p + ": no start"), false); return 1; }
        var bb = start.getBoundingBox();
        int gMin = Integer.MAX_VALUE, gMax = Integer.MIN_VALUE;
        for (int x = bb.minX(); x <= bb.maxX(); x += 4) for (int z = bb.minZ(); z <= bb.maxZ(); z += 4) {
            int g = e.at(x, z).solidTop(); gMin = Math.min(gMin, g); gMax = Math.max(gMax, g);
        }
        double dist = Math.hypot(p.getX() - from.getX(), p.getZ() - from.getZ());
        String msg = String.format(java.util.Locale.ROOT, "structcheck %s: %.0f blocks away, box y %d..%d (%dx%d), ground %d..%d, %d pieces",
                id, dist, bb.minY(), bb.maxY(), bb.getXSpan(), bb.getZSpan(), gMin, gMax, start.getPieces().size());
        c.getSource().sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }

    /** Check the world repeats across the antimeridian and that a riding teleport keeps passengers (testing aid). */
    private static int seamtest(CommandContext<CommandSourceStack> c) {
        Earth e = Earth.get();
        var level = c.getSource().getLevel();
        int w = e.transform.widthBlocks, west = e.transform.seamWestX;
        int z = (int) e.transform.blockZ(-17.0); // Fiji latitude: islands right on the seam
        int same = 0, diff = 0;
        StringBuilder ex = new StringBuilder();
        for (int i = -40; i < 40; i += 5) {
            int xa = west + i, xb = xa + w;
            level.getChunk(xa >> 4, z >> 4);
            level.getChunk(xb >> 4, z >> 4);
            int ha = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR, xa, z);
            int hb = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR, xb, z);
            var ba = level.getBlockState(new net.minecraft.core.BlockPos(xa, ha - 1, z)).getBlock();
            var bb = level.getBlockState(new net.minecraft.core.BlockPos(xb, hb - 1, z)).getBlock();
            if (ha == hb && ba == bb) same++; else { diff++; ex.append(i).append(':').append(ha).append('/').append(hb).append(' '); }
        }
        var boat = net.minecraft.world.entity.EntityTypes.OAK_BOAT.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        var pig = net.minecraft.world.entity.EntityTypes.PIG.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        boat.snapTo(west + w + 3.5, e.curve.seaLevelY + 1, z + 0.5, 90f, 0f);
        pig.snapTo(west + w + 3.5, e.curve.seaLevelY + 1, z + 0.5, 90f, 0f);
        level.addFreshEntity(boat);
        level.addFreshEntity(pig);
        pig.startRiding(boat, true, true);
        boat.setDeltaMovement(0.4, 0, 0);
        boat.teleport(new net.minecraft.world.level.portal.TeleportTransition(level, new net.minecraft.world.phys.Vec3(-w, 0, 0),
                net.minecraft.world.phys.Vec3.ZERO, 0f, 0f, net.minecraft.world.entity.Relative.ALL,
                net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING));
        String msg = String.format(java.util.Locale.ROOT, "seamtest W=%d west=%d: %d same, %d differ %s| boat x %.1f (want %.1f), speed %.2f, pig riding %s at x %.1f",
                w, west, same, diff, ex, boat.getX(), west + 3.5, boat.getDeltaMovement().x, pig.isPassenger(), pig.getX());
        c.getSource().sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }

    /** Generate the chunk at a lat/lon and print the top blocks of the column (testing aid). */
    private static int probe(CommandContext<CommandSourceStack> c) {
        Earth e = Earth.get();
        double lat = DoubleArgumentType.getDouble(c, "lat"), lon = DoubleArgumentType.getDouble(c, "lon");
        int x = (int) Math.floor(e.transform.blockX(lon)), z = (int) Math.floor(e.transform.blockZ(lat));
        var level = c.getSource().getLevel();
        level.getChunk(x >> 4, z >> 4);
        int top = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x, z);
        StringBuilder sb = new StringBuilder();
        for (int y = top; y > top - 8; y--) {
            String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(level.getBlockState(new net.minecraft.core.BlockPos(x, y, z)).getBlock()).getPath();
            sb.append(y).append('=').append(id).append(' ');
        }
        String biome = level.getBiome(new net.minecraft.core.BlockPos(x, top, z)).unwrapKey().map(k -> k.identifier().toString()).orElse("?");
        Earth.Surface sf = e.at(x, z);
        String msg = String.format(java.util.Locale.ROOT, "probe %.4f,%.4f block %d,%d %s [%.0f m %s cover %d veg %d slope %.2f snowline %d] : %s", lat, lon, x, z, biome,
                sf.elevM(), BiomeTable.code(sf.climate()), sf.cover(), sf.veg(), sf.slope(), BiomeTable.snowline(sf.climate(), lat, lon), sb);
        c.getSource().sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }

    private static int tp(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return teleport(c, DoubleArgumentType.getDouble(c, "lat"), DoubleArgumentType.getDouble(c, "lon"), null);
    }

    public static int teleport(CommandContext<CommandSourceStack> c, double lat, double lon, String label) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Earth e = Earth.get();
        ServerPlayer p = c.getSource().getPlayerOrException();
        double x = e.transform.blockX(lon), z = e.transform.blockZ(lat);
        Earth.Surface s = e.at((int) Math.floor(x), (int) Math.floor(z));
        double y = Math.max(s.surfaceY(), e.curve.seaLevelY) + 2;
        // Earth coordinates only mean something in the overworld (y -700 is the void in the Nether).
        p.teleportTo(c.getSource().getServer().overworld(), x, y, z, Set.of(), p.getYRot(), p.getXRot(), false);
        String msg = (label != null ? "Teleported to " + com.seth.worldbound.api.GeoText.plain(label) + ": " : "teleported to ")
                + String.format("%.4f, %.4f (block %.0f, %.0f, y %.0f, %.0f m)", lat, lon, x, z, y, s.elevM());
        c.getSource().sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }

}
