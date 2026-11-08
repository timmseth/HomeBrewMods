package com.seth.worldbound.worldgen;

import com.seth.worldbound.Earth;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * The hadal zone. Ocean floors are true to 6,000 m; below that the trenches are cut as narrow canyons (1 block per
 * 150 m) with a floor of deepslate, sculk patches, magma vents (downward bubble columns) and soul-sand vents
 * (columns that carry you up). Where the real ocean is deeper than 10,000 m (Challenger Deep, Horizon Deep, a
 * handful of others) the floor is open onto the void, ringed with bedrock.
 */
public final class TrenchFeature extends Feature<NoneFeatureConfiguration> {
    public static final double CLAMP_M = 6000, VOID_M = 10_000, RIM_M = 9_600;

    public TrenchFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        if (!(ctx.chunkGenerator().getBiomeSource() instanceof EarthBiomeSource)) return false;
        WorldGenLevel level = ctx.level();
        Earth e = Earth.get();
        int x0 = ctx.origin().getX() & ~15, z0 = ctx.origin().getZ() & ~15;
        // cheap early-out: the chunk centre has to be deep ocean at all
        if (e.at(x0 + 8, z0 + 8).elevM() > -CLAMP_M + 400) return false;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        boolean any = false, voidOn = e.config.trench_void;
        int minY = level.getMinY();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = x0 + dx, z = z0 + dz;
                Earth.Surface s = e.at(x, z);
                double depth = -s.elevM();
                if (!s.ocean() || depth < CLAMP_M) continue;
                int floor = s.solidTop();
                any = true;
                if (voidOn && depth >= VOID_M) {
                    // open to the void: everything from the world floor up to the sea floor becomes water
                    for (int y = minY; y <= floor; y++) {
                        p.set(x, y, z);
                        level.setBlock(p, Blocks.WATER.defaultBlockState(), 2);
                    }
                    continue;
                }
                boolean rim = voidOn && depth >= RIM_M;
                double n1 = e.patchNoise(x + 911, z - 911, e.tuned(70));    // sculk
                double n2 = e.patchNoise(x - 3131, z + 3131, e.tuned(45));  // magma vents
                double n3 = e.patchNoise(x + 5050, z + 5050, e.tuned(120)); // soul-sand vents
                long h = (x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL) >>> 20;
                for (int y = floor - 3; y <= floor; y++) {
                    BlockState b;
                    if (rim) b = Blocks.BEDROCK.defaultBlockState();
                    else if (y < floor) b = (n2 > 0.45 ? Blocks.BASALT : Blocks.DEEPSLATE).defaultBlockState();
                    else if (n2 > 0.55 && (h & 3) != 0) b = Blocks.MAGMA_BLOCK.defaultBlockState();
                    else if (n2 > 0.45) b = Blocks.SMOOTH_BASALT.defaultBlockState();
                    else if (n3 > 0.72 && (h % 7) < 3) b = Blocks.SOUL_SAND.defaultBlockState();
                    else if (n1 > 0.7 && (h % 23) == 0) b = Blocks.SCULK_CATALYST.defaultBlockState();
                    else if (n1 > 0.35) b = Blocks.SCULK.defaultBlockState();
                    else b = ((h & 7) == 0 ? Blocks.TUFF : Blocks.DEEPSLATE).defaultBlockState();
                    p.set(x, y, z);
                    level.setBlock(p, b, 2);
                }
            }
        }
        return any;
    }
}
