package com.seth.worldbound.compat;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.enums.worldGeneration.EDhApiDistantGeneratorMode;
import com.seibel.distanthorizons.api.enums.worldGeneration.EDhApiWorldGeneratorReturnType;
import com.seibel.distanthorizons.api.interfaces.block.IDhApiBiomeWrapper;
import com.seibel.distanthorizons.api.interfaces.block.IDhApiBlockStateWrapper;
import com.seibel.distanthorizons.api.interfaces.override.worldGenerator.IDhApiWorldGenerator;
import com.seibel.distanthorizons.api.interfaces.world.IDhApiLevelWrapper;
import com.seibel.distanthorizons.api.objects.data.DhApiTerrainDataPoint;
import com.seibel.distanthorizons.api.objects.data.IDhApiFullDataSource;
import com.seth.worldbound.Earth;
import com.seth.worldbound.worldgen.BiomeTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Distant Horizons LODs straight from WorldBound's data: ground height, ground block, water and a forest canopy for
 * each column, with no chunk generation at all. Roughly a thousand times cheaper than letting DH build full
 * 4,064-block-tall chunks, so distant terrain fills in quickly and leaves the CPU for the chunks around the player.
 * Inside the normal render distance DH still uses the real chunks.
 */
public final class EarthLodGenerator implements IDhApiWorldGenerator {
    private final IDhApiLevelWrapper level;
    private final Map<String, IDhApiBlockStateWrapper> blocks = new ConcurrentHashMap<>();
    private final Map<String, IDhApiBiomeWrapper> biomes = new ConcurrentHashMap<>();
    public static final AtomicLong columns = new AtomicLong(), lods = new AtomicLong(), nanos = new AtomicLong();

    public EarthLodGenerator(IDhApiLevelWrapper level) {
        this.level = level;
    }

    @Override public byte getSmallestDataDetailLevel() { return 0; }
    @Override public byte getLargestDataDetailLevel() { return 12; }
    @Override public EDhApiWorldGeneratorReturnType getReturnType() { return EDhApiWorldGeneratorReturnType.API_DATA_SOURCES; }
    @Override public boolean runApiValidation() { return false; }
    @Override public void preGeneratorTaskStart() {}
    @Override public void close() {}

    @Override
    public CompletableFuture<Void> generateLod(int chunkMinX, int chunkMinZ, int lodX, int lodZ, byte detail, IDhApiFullDataSource out,
                                               EDhApiDistantGeneratorMode mode, ExecutorService pool, Consumer<IDhApiFullDataSource> done) {
        return CompletableFuture.runAsync(() -> {
            fill(chunkMinX * 16, chunkMinZ * 16, detail, out);
            done.accept(out);
        }, pool);
    }

    /** Fills every column of the data source; public so /earth lodtest can time and validate it. */
    public void fill(int blockMinX, int blockMinZ, int detail, IDhApiFullDataSource out) {
        long t0 = System.nanoTime();
        Earth e = Earth.get();
        int w = out.getWidthInDataColumns(), step = 1 << detail, half = step / 2;
        int minY = e.curve.minY, top = e.curve.maxY + 1 - minY, sea = e.curve.seaLevelY;
        List<DhApiTerrainDataPoint> col = new ArrayList<>(8);
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < w; j++) {
                int bx = blockMinX + i * step + half, bz = blockMinZ + j * step + half;
                Earth.Surface s = e.lodAt(bx, bz, step);
                String biome = BiomeTable.surface(s);
                IDhApiBiomeWrapper bio = biome(biome.indexOf(':') < 0 ? "minecraft:" + biome : biome);
                int st = Math.max(minY + 4, Math.min(e.curve.maxY - 8, s.solidTop()));
                int wt = s.waterDepth() > 0 ? s.surfaceY() : s.ocean() ? sea : st;
                boolean wet = wt > st;
                String[] ground = ground(biome, s, wet);
                // Seasonal snow, the same rule new chunks use (today's date, this place's climate): matches the
                // snow that appears when the real chunks load.
                if (!wet && !ground[0].endsWith("snow_block") && !ground[0].endsWith("ice")
                        && com.seth.worldbound.worldgen.Seasons.freezing(s.lat(), s.lon(), s.climate(), s.elevM()))
                    ground = new String[]{"minecraft:snow_block", ground[1], ground[2]};
                col.clear();
                int y = 0;
                y = add(col, y, st - 3 - minY, ground[2], bio, 0);
                y = add(col, y, st - minY, ground[1], bio, 0);
                y = add(col, y, st + 1 - minY, ground[0], bio, 0);
                if (wet) {
                    y = add(col, y, wt + 1 - minY, "minecraft:water", bio, 15);
                } else {
                    String leaves = canopy(e, s, biome, bx, bz, detail, step);
                    if (leaves != null) y = add(col, y, st + 7 - minY, leaves, bio, 15);
                }
                add(col, y, top, null, bio, 15);
                out.setApiDataPointColumn(i, j, col);
            }
        }
        columns.addAndGet((long) w * w);
        lods.incrementAndGet();
        nanos.addAndGet(System.nanoTime() - t0);
    }

    private int add(List<DhApiTerrainDataPoint> col, int from, int to, String block, IDhApiBiomeWrapper bio, int sky) {
        if (to <= from) return from;
        IDhApiBlockStateWrapper b = block == null ? DhApi.Delayed.wrapperFactory.getAirBlockStateWrapper() : block(block);
        col.add(DhApiTerrainDataPoint.create((byte) 0, 0, sky, from, to, b, bio));
        return to;
    }

    /** {top, under, deep} blocks, matching what the surface rules put down for this biome. */
    private static String[] ground(String biome, Earth.Surface s, boolean wet) {
        String p = biome.substring(biome.indexOf(':') + 1);
        boolean steep = s.slope() > 1.4;
        if (wet) return s.elevM() < -1500 ? arr("gravel", "gravel") : arr("sand", "sand");
        if (p.contains("desert") || p.equals("beach")) return arr("sand", "sandstone");
        if (p.contains("badlands")) return steep ? arr("terracotta", "terracotta") : arr("red_sand", "terracotta");
        if (p.equals("frozen_peaks") || p.contains("ice_spikes")) return arr("packed_ice", "packed_ice");
        if (p.equals("jagged_peaks")) return steep ? arr("stone", "stone") : arr("snow_block", "stone");
        if (p.contains("snowy") || p.equals("grove") || p.contains("frozen")) return steep ? arr("stone", "stone") : arr("snow_block", "dirt");
        if (p.contains("stony") || p.contains("gravelly")) return arr(p.contains("gravelly") ? "gravel" : "stone", "stone");
        if (p.equals("mangrove_swamp")) return arr("mud", "mud");
        if (steep) return arr("stone", "stone");
        return arr("grass_block", "dirt");
    }

    private static String[] arr(String top, String under) {
        return new String[]{"minecraft:" + top, "minecraft:" + under, "minecraft:stone"};
    }

    /** Leaves for a forested column (speckled up close, a closed canopy far away), or null. */
    private static String canopy(Earth e, Earth.Surface s, String biome, int bx, int bz, int detail, int step) {
        String p = biome.substring(biome.indexOf(':') + 1);
        if (p.contains("snowy_slopes") || p.contains("peaks") || p.contains("desert") || p.contains("beach") || p.contains("ocean")) return null;
        int cover = e.stack.treeCover(s.lon(), s.lat(), e.zoomForSpacing(step));
        if (cover <= 0 || cover > 100) {
            if (s.cover() != 10) return null;     // WorldCover "tree cover" without a density layer
            cover = 60;
        }
        int h = (int) ((bx * 734287L + bz * 912931L) * 0x9E3779B9L >>> 25) % 100;
        if (detail >= 3 ? cover < 35 : h >= cover) return null;
        String leaves = p.contains("taiga") || p.equals("grove") ? "spruce_leaves" : p.contains("birch") ? "birch_leaves"
                : p.contains("jungle") ? "jungle_leaves" : p.contains("dark_forest") ? "dark_oak_leaves"
                : p.contains("savanna") ? "acacia_leaves" : p.contains("cherry") ? "cherry_leaves"
                : p.contains("mangrove") ? "mangrove_leaves" : "oak_leaves";
        return "minecraft:" + leaves;
    }

    private IDhApiBlockStateWrapper block(String id) {
        return blocks.computeIfAbsent(id, k -> {
            try {
                return DhApi.Delayed.wrapperFactory.getDefaultBlockStateWrapper(k, level);
            } catch (Exception ex) {
                return DhApi.Delayed.wrapperFactory.getAirBlockStateWrapper();
            }
        });
    }

    private IDhApiBiomeWrapper biome(String id) {
        IDhApiBiomeWrapper b = biomes.get(id);
        if (b != null) return b;
        try {
            b = DhApi.Delayed.wrapperFactory.getBiomeWrapper(id, level);
        } catch (Exception ex) {
            try {
                b = DhApi.Delayed.wrapperFactory.getBiomeWrapper("minecraft:plains", level);
            } catch (Exception ex2) {
                throw new IllegalStateException(ex2);
            }
        }
        biomes.put(id, b);
        return b;
    }
}
