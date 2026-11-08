package com.seth.worldbound.mixin;

import com.seth.worldbound.Earth;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Earth worlds: the ground height of a column is known from the data, so structure placement, /locate and spawn
 * searches read it directly instead of evaluating the density functions cell by cell down a 3,500-block column
 * (vanilla's iterateNoiseColumn). Same answer as the fill, a fraction of the cost.
 */
@Mixin(NoiseBasedChunkGenerator.class)
public abstract class BaseHeightMixin {
    @Unique
    private Boolean worldbound$earth;

    @Unique
    private boolean worldbound$isEarth() {
        Boolean b = worldbound$earth;
        if (b == null) {
            NoiseBasedChunkGenerator self = (NoiseBasedChunkGenerator) (Object) this;
            b = self.generatorSettings().unwrapKey().map(k -> k.identifier().getNamespace().equals("worldbound")).orElse(false);
            worldbound$earth = b;
        }
        return b && Earth.get().config.fast_fill;
    }

    @Inject(method = "getBaseHeight", at = @At("HEAD"), cancellable = true)
    private void worldbound$baseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random,
                                       CallbackInfoReturnable<Integer> cir) {
        if (!worldbound$isEarth()) return;
        Earth e = Earth.get();
        Earth.Surface s = e.at(x, z);
        int st = s.solidTop(), wt = s.waterDepth() > 0 ? s.surfaceY() : s.ocean() ? e.curve.seaLevelY : st;
        // Heightmaps that count fluids (world surface, motion blocking) stop at the water; ocean floor doesn't.
        int top = wt > st && type.isOpaque().test(Blocks.WATER.defaultBlockState()) ? wt : st;
        cir.setReturnValue(Math.max(level.getMinY(), Math.min(level.getMaxY(), top) + 1));
    }

    @Inject(method = "getBaseColumn", at = @At("HEAD"), cancellable = true)
    private void worldbound$baseColumn(int x, int z, LevelHeightAccessor level, RandomState random, CallbackInfoReturnable<NoiseColumn> cir) {
        if (!worldbound$isEarth()) return;
        Earth e = Earth.get();
        Earth.Surface s = e.at(x, z);
        int st = s.solidTop(), wt = s.waterDepth() > 0 ? s.surfaceY() : s.ocean() ? e.curve.seaLevelY : st;
        int minY = level.getMinY(), h = level.getHeight();
        BlockState stone = Blocks.STONE.defaultBlockState(), water = Blocks.WATER.defaultBlockState(), air = Blocks.AIR.defaultBlockState();
        BlockState[] col = new BlockState[h];
        for (int i = 0; i < h; i++) {
            int y = minY + i;
            col[i] = y <= st ? stone : y <= wt ? water : air;
        }
        cir.setReturnValue(new NoiseColumn(minY, col));
    }
}
