package com.seth.worldbound.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.seth.worldbound.Earth;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * {@code /earth snapshot <lat> <lon> <radius chunks> <name>}: generates the area and writes what its surface looks
 * like (top block, height, water floor, biome tints per column) to {@code snapshots/<name>.wbsnap} for
 * {@code tools/snapshot_render.py} to draw as a map and a 3-D view. A development aid for judging terrain.
 */
final class Snapshot {
    private Snapshot() {}

    static LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("snapshot").requires(EarthCommands.dbg())
                .then(Commands.argument("lat", DoubleArgumentType.doubleArg(-90, 90))
                        .then(Commands.argument("lon", DoubleArgumentType.doubleArg(-180, 180))
                                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 40))
                                        .then(Commands.argument("name", StringArgumentType.word())
                                                .executes(Snapshot::run)))));
    }

    private static int run(CommandContext<CommandSourceStack> c) {
        Earth e = Earth.get();
        double lat = DoubleArgumentType.getDouble(c, "lat"), lon = DoubleArgumentType.getDouble(c, "lon");
        int r = IntegerArgumentType.getInteger(c, "radius");
        String name = StringArgumentType.getString(c, "name");
        ServerLevel level = c.getSource().getLevel();
        int ccx = ((int) Math.floor(e.transform.blockX(lon))) >> 4, ccz = ((int) Math.floor(e.transform.blockZ(lat))) >> 4;
        int n = (2 * r + 1) * 16, x0 = (ccx - r) << 4, z0 = (ccz - r) << 4;
        long t0 = System.nanoTime();
        Map<String, Integer> palette = new HashMap<>(), bpal = new HashMap<>();
        java.util.List<String> names = new java.util.ArrayList<>(), bnames = new java.util.ArrayList<>();
        short[] biome = new short[n * n];
        short[] top = new short[n * n], floor = new short[n * n];
        short[] topB = new short[n * n], floorB = new short[n * n];
        int[] grass = new int[n * n], foliage = new int[n * n], water = new int[n * n];
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        for (int cz = -r; cz <= r; cz++) {
            for (int cx = -r; cx <= r; cx++) {
                ChunkAccess ch = level.getChunk(ccx + cx, ccz + cz, ChunkStatus.FULL, true);
                for (int lz = 0; lz < 16; lz++) {
                    for (int lx = 0; lx < 16; lx++) {
                        int x = ((ccx + cx) << 4) + lx, z = ((ccz + cz) << 4) + lz;
                        int i = (z - z0) * n + (x - x0);
                        int yTop = ch.getHeight(Heightmap.Types.WORLD_SURFACE, lx, lz); // ChunkAccess.getHeight is already the top block
                        int yFloor = ch.getHeight(Heightmap.Types.OCEAN_FLOOR, lx, lz);
                        BlockState bt = ch.getBlockState(mp.set(x, yTop, z)), bf = ch.getBlockState(mp.set(x, yFloor, z));
                        top[i] = (short) yTop;
                        floor[i] = (short) yFloor;
                        topB[i] = (short) id(palette, names, bt);
                        floorB[i] = (short) id(palette, names, bf);
                        net.minecraft.core.Holder<Biome> bh = level.getBiome(mp.set(x, yTop, z));
                        Biome b = bh.value();
                        String bid = bh.unwrapKey().map(k -> k.identifier().toString()).orElse("minecraft:plains");
                        Integer bi = bpal.get(bid);
                        if (bi == null) { bi = bnames.size(); bnames.add(bid); bpal.put(bid, bi); }
                        biome[i] = (short) (int) bi;
                        grass[i] = b.getGrassColor(x, z);
                        foliage[i] = b.getFoliageColor();
                        water[i] = b.getWaterColor();
                    }
                }
            }
        }
        Path dir = level.getServer().getServerDirectory().resolve("snapshots");
        Path out = dir.resolve(name + ".wbsnap");
        try {
            Files.createDirectories(dir);
            try (DataOutputStream o = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(out), 1 << 16))) {
                o.writeBytes("WBSNAP2\n");
                o.writeInt(n);
                o.writeInt(x0);
                o.writeInt(z0);
                o.writeDouble(lat);
                o.writeDouble(lon);
                o.writeDouble(e.config.scale_m_per_block);
                o.writeInt(e.curve.seaLevelY);
                o.writeInt(names.size());
                for (String s : names) o.writeUTF(s);
                for (int i = 0; i < n * n; i++) {
                    o.writeShort(top[i]);
                    o.writeShort(topB[i]);
                    o.writeShort(floor[i]);
                    o.writeShort(floorB[i]);
                    o.writeInt(grass[i]);
                    o.writeInt(foliage[i]);
                    o.writeInt(water[i]);
                }
                o.writeInt(bnames.size());
                for (String b : bnames) o.writeUTF(b);
                for (int i = 0; i < n * n; i++) o.writeShort(biome[i]);
            }
        } catch (IOException ex) {
            c.getSource().sendFailure(Component.literal("snapshot: " + ex));
            return 0;
        }
        double s = (System.nanoTime() - t0) / 1e9;
        c.getSource().sendSuccess(() -> Component.literal(String.format(java.util.Locale.ROOT,
                "snapshot %s: %dx%d blocks around %.4f,%.4f in %.1f s -> %s", name, n, n, lat, lon, s, out)), false);
        return 1;
    }

    private static int id(Map<String, Integer> palette, java.util.List<String> names, BlockState s) {
        String k = BuiltInRegistries.BLOCK.getKey(s.getBlock()).toString();
        Integer v = palette.get(k);
        if (v == null) {
            v = names.size();
            names.add(k);
            palette.put(k, v);
        }
        return v;
    }
}
