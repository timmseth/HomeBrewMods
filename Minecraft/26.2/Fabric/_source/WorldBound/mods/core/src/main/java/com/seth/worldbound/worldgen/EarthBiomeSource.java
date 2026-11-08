package com.seth.worldbound.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.seth.worldbound.Earth;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/** {@code worldbound:raster} — biome per column from the raster stack via {@link BiomeTable}. */
public final class EarthBiomeSource extends BiomeSource {
    public static final MapCodec<EarthBiomeSource> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            RegistryOps.retrieveGetter(Registries.BIOME),
            com.mojang.serialization.Codec.STRING.optionalFieldOf("planet", "earth").forGetter(EarthBiomeSource::planet)
    ).apply(i, EarthBiomeSource::new));

    /** "earth" (the real one) or "earthish" (a generated planet from the world seed). */
    private final String planet;

    public String planet() {
        return planet;
    }

    public boolean earthish() {
        return "earthish".equals(planet);
    }

    private final HolderGetter<Biome> biomes;
    private final Map<String, Holder<Biome>> byPath = new HashMap<>();
    private final Holder<Biome> fallback;
    private final WildBiomes wild;

    public EarthBiomeSource(HolderGetter<Biome> biomes) {
        this(biomes, "earth");
    }

    public EarthBiomeSource(HolderGetter<Biome> biomes, String planet) {
        this.planet = planet == null ? "earth" : planet;
        this.biomes = biomes;
        this.wild = new WildBiomes(biomes);
        for (String path : BiomeTable.allBiomes()) byPath.put(path, resolve(path));
        // Regional picks: vanilla extras always resolve; Terralith / Terrestria ones only when installed.
        for (String id : com.seth.worldbound.Hooks.regional == null ? java.util.Set.<String>of() : com.seth.worldbound.Hooks.regional.allIds()) {
            if (byPath.containsKey(id)) continue;
            Identifier ident = id.contains(":") ? Identifier.parse(id) : Identifier.withDefaultNamespace(id);
            // Asking the loading-time getter for an absent id registers an unbound reference and fails the
            // whole world load, so only look up modded biomes whose mod is actually installed.
            if (!ident.getNamespace().equals("minecraft")
                    && !net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded(ident.getNamespace())) continue;
            biomes.get(ResourceKey.create(Registries.BIOME, ident)).ifPresent(h -> byPath.put(id, h));
        }
        this.fallback = byPath.get("plains");
    }

    private Holder<Biome> resolve(String path) {
        return biomes.getOrThrow(ResourceKey.create(Registries.BIOME, Identifier.withDefaultNamespace(path)));
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        java.util.Set<Holder<Biome>> all = new java.util.LinkedHashSet<>(byPath.values());
        all.addAll(wild.all());
        return List.copyOf(all).stream();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int qx, int qy, int qz, Climate.Sampler sampler) {
        Earth earth = Earth.get();
        streamCheck(earth, QuartPos.toBlock(qx), QuartPos.toBlock(qz));
        // Minecraft asks for every 4x4x4 cell of the ~3,500-tall chunk (14,000+ calls per chunk), walking x, y, z
        // with z innermost, so consecutive calls hit different columns. Everything but the cave biome is the same
        // all the way up a column, so the 16 columns of the chunk are each worked out once.
        Cols c = COLS.get();
        long key = ((long) (qx >> 2) << 32) ^ ((qz >> 2) & 0xFFFFFFFFL);
        long gen = earth.stack.generation();
        if (c.key != key || c.earth != earth || c.owner != this || c.generation != gen) {
            c.key = key;
            c.earth = earth;
            c.owner = this;
            c.generation = gen;
            java.util.Arrays.fill(c.cols, null);
        }
        int idx = (qx & 3) | (qz & 3) << 2;
        Column col = c.cols[idx];
        if (col == null) c.cols[idx] = col = column(earth, qx, qz);
        int by = QuartPos.toBlock(qy);
        if (by >= col.surfaceY - 32 || col.ocean) return col.top;
        // Cave biomes change with depth in steps; remember the last answer for this column.
        if (by == col.lastCaveY) return col.lastCave;
        Holder<Biome> h = byPath.get(BiomeTable.cave(col.path, by, col.surfaceY, earth.curve.minY, col.caveNoise));
        col.lastCaveY = by;
        col.lastCave = h != null ? h : fallback;
        return col.lastCave;
    }

    private Column column(Earth earth, int qx, int qz) {
        int bx = earth.transform.wrapX(QuartPos.toBlock(qx)), bz = QuartPos.toBlock(qz);
        Earth.Surface s = earth.at(bx, bz);
        Column c = new Column();
        c.path = BiomeTable.surface(s);
        c.surfaceY = s.surfaceY();
        c.ocean = s.ocean();
        c.caveNoise = earth.caveNoise(bx, bz);
        Holder<Biome> top = null;
        if (Earth.wildMode() && wild.ready()) top = wild.pick(earth, s, bx, bz, c.path);
        if (top == null && com.seth.worldbound.Hooks.regional != null) for (String id : com.seth.worldbound.Hooks.regional.pick(s, c.path, earth.patchNoise(bx, bz, earth.tuned(2000)), earth.patchNoise(bx + 7777, bz - 7777, earth.tuned(700)))) {
            top = byPath.get(id);
            if (top != null) break;
        }
        if (top == null) top = byPath.get(c.path);
        c.top = top != null ? top : fallback;
        return c;
    }

    private static final class Column {
        String path;
        int surfaceY;
        boolean ocean;
        double caveNoise;
        Holder<Biome> top;
        int lastCaveY = Integer.MIN_VALUE;
        Holder<Biome> lastCave;
    }

    /** The 16 biome columns (4x4 quarts) of the chunk this thread is working on. */
    private static final class Cols {
        long key = Long.MIN_VALUE;
        Earth earth;
        EarthBiomeSource owner;
        long generation = -1;
        final Column[] cols = new Column[16];
    }

    private static final ThreadLocal<Cols> COLS = ThreadLocal.withInitial(Cols::new);

    /** First look at a chunk on this thread: let the data supplier fetch finer data for it (Earth Data World Gen streams). */
    static void streamCheck(Earth earth, int blockX, int blockZ) {
        var h = com.seth.worldbound.Hooks.streaming;
        if (h != null && !earth.generated()) h.chunk(earth, blockX, blockZ);
    }

    @Override
    public void addDebugInfo(List<String> lines, BlockPos pos, Climate.Sampler sampler) {
        Earth.Surface s = Earth.get().at(pos.getX(), pos.getZ());
        lines.add(String.format("Earth: %.4f, %.4f  %.0f m  y%d  %s cover %d slope %.2f",
                s.lat(), s.lon(), s.elevM(), s.surfaceY(), BiomeTable.code(s.climate()), s.cover(), s.slope()));
    }
}
