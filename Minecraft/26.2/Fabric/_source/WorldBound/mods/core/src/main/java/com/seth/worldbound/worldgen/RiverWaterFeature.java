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
 * Fills the channels that {@code worldbound:surface_density} carves for rivers (columns with
 * waterDepth &gt; 0) with water sources, once per chunk at RAW_GENERATION. Does nothing in non-Earth worlds.
 */
public final class RiverWaterFeature extends Feature<NoneFeatureConfiguration> {
    public RiverWaterFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        if (!(ctx.chunkGenerator().getBiomeSource() instanceof EarthBiomeSource)) return false;
        WorldGenLevel level = ctx.level();
        Earth earth = Earth.get();
        int x0 = ctx.origin().getX() & ~15, z0 = ctx.origin().getZ() & ~15;
        BlockState water = Blocks.WATER.defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean any = false;
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                Earth.Surface s = earth.at(x0 + dx, z0 + dz);
                int depth = s.waterDepth();
                if (depth <= 0) continue;
                int top = s.surfaceY();
                for (int y = top - depth + 1; y <= top; y++) {
                    pos.set(x0 + dx, y, z0 + dz);
                    if (level.getBlockState(pos).isAir()) {
                        level.setBlock(pos, water, 2);
                        any = true;
                    }
                }
            }
        }
        return any;
    }
}
