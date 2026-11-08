package com.seth.worldbound.mixin;

import com.seth.worldbound.Earth;
import com.seth.worldbound.worldgen.EarthStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Earth worlds: forests as dense as the real ones (Copernicus tree cover %, pack "treecover").
 * Sparse cover thins vanilla's trees; dense cover (rainforest, boreal forest) adds extra trees beside them.
 */
@Mixin(TreeFeature.class)
public abstract class TreeDensityMixin {
    private static final ThreadLocal<Boolean> EXTRA = ThreadLocal.withInitial(() -> false);

    private static boolean soil(net.minecraft.world.level.block.state.BlockState b) {
        return b.is(BlockTags.DIRT) || b.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK) || b.is(net.minecraft.world.level.block.Blocks.DIRT)
                || b.is(net.minecraft.world.level.block.Blocks.PODZOL) || b.is(net.minecraft.world.level.block.Blocks.COARSE_DIRT)
                || b.is(net.minecraft.world.level.block.Blocks.ROOTED_DIRT) || b.is(net.minecraft.world.level.block.Blocks.MOSS_BLOCK)
                || b.is(net.minecraft.world.level.block.Blocks.MUD);
    }

    private static int cover(FeaturePlaceContext<?> ctx) {
        if (!EarthStructures.isEarth(ctx.level())) return -1;
        Earth e = Earth.get();
        if (!e.config.tree_density) return -1;
        BlockPos o = ctx.origin();
        var t = e.transform;
        return e.stack.treeCover(t.lon(t.wrapX(o.getX()) + 0.5), t.lat(o.getZ() + 0.5));
    }

    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void worldbound$thin(FeaturePlaceContext<TreeConfiguration> ctx, CallbackInfoReturnable<Boolean> cir) {
        if (EXTRA.get()) return;
        com.seth.worldbound.worldgen.TreeStats.SEEN.incrementAndGet();
        int c = cover(ctx);
        if (c < 0 || c >= 50) return;
        double keep = Math.max(0.05, c / 50.0);
        if (ctx.random().nextDouble() > keep) { com.seth.worldbound.worldgen.TreeStats.THINNED.incrementAndGet(); cir.setReturnValue(false); }
    }

    @Inject(method = "place", at = @At("RETURN"))
    private void worldbound$thicken(FeaturePlaceContext<TreeConfiguration> ctx, CallbackInfoReturnable<Boolean> cir) {
        if (EXTRA.get()) return;
        if (!cir.getReturnValueZ()) { com.seth.worldbound.worldgen.TreeStats.FAILED.incrementAndGet(); return; }
        int c = cover(ctx);
        if (c < 0) com.seth.worldbound.worldgen.TreeStats.NOCOVER.incrementAndGet();
        if (c <= 60) return;
        com.seth.worldbound.worldgen.TreeStats.HIGH.incrementAndGet();
        double extra = (c - 60) / 40.0 * 1.5;
        int n = (int) extra + (ctx.random().nextDouble() < extra - (int) extra ? 1 : 0);
        if (n == 0) return;
        EXTRA.set(true);
        try {
            var level = ctx.level();
            for (int i = 0; i < n; i++) {
                int x = ctx.origin().getX() + ctx.random().nextInt(13) - 6, z = ctx.origin().getZ() + ctx.random().nextInt(13) - 6;
                // From the top of the canopy, walk down through leaves/vines/air to the forest floor.
                BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos(x, level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z), z);
                boolean found = false;
                for (int k = 0; k < 48 && !found; k++) {
                    var below = level.getBlockState(m.below());
                    if (soil(below)) found = level.getBlockState(m).canBeReplaced();
                    else if (below.is(BlockTags.LEAVES) || below.is(BlockTags.LOGS) || below.canBeReplaced()) m.move(0, -1, 0);
                    else break;
                }
                if (!found) {
                    if (com.seth.worldbound.worldgen.TreeStats.HIGH.get() < 400 && Boolean.getBoolean("worldbound.debugTrees"))
                        com.seth.worldbound.WorldBoundMod.LOG.info("tree spot {} {} {} below={} at={} origin y {}", x, m.getY(), z, level.getBlockState(m.below()), level.getBlockState(m), ctx.origin().getY());
                    continue;
                }
                BlockPos p = m.immutable();
                com.seth.worldbound.worldgen.TreeStats.EXTRA_TRY.incrementAndGet();
                if (((TreeFeature) (Object) this).place(new FeaturePlaceContext<>(ctx.topFeature(), level, ctx.chunkGenerator(), ctx.random(), p, ctx.config()))) com.seth.worldbound.worldgen.TreeStats.EXTRA_OK.incrementAndGet();
            }
        } catch (RuntimeException ex) {
            if (com.seth.worldbound.worldgen.TreeStats.FAILED.incrementAndGet() < 5) com.seth.worldbound.WorldBoundMod.LOG.warn("extra tree: {}", ex.toString());
        } finally {
            EXTRA.set(false);
        }
    }
}
