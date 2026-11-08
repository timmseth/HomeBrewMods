package com.seth.worldbound.mixin;

import com.seth.worldbound.Earth;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Earth worlds: ores, geodes, dungeons etc. are placed at vanilla heights *relative to the local ground*.
 * The height provider is sampled as if in a vanilla world (-64..320, ground ~63), then shifted so vanilla
 * y 63 lands on this column's surface. Diamonds end up ~120 blocks under your feet wherever you are.
 */
@Mixin(HeightRangePlacement.class)
public abstract class HeightRangeMixin {
    private static final LevelHeightAccessor VANILLA = LevelHeightAccessor.create(-64, 384);
    private static final java.util.Map<net.minecraft.world.level.levelgen.placement.PlacedFeature, Boolean> SPRING =
            java.util.Collections.synchronizedMap(new java.util.IdentityHashMap<>());

    private static boolean isSpring(PlacementContext pctx, net.minecraft.world.level.levelgen.placement.PlacedFeature f) {
        Boolean b = SPRING.get(f);
        if (b == null) {
            var id = pctx.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.PLACED_FEATURE).getKey(f);
            b = id != null && id.getPath().contains("spring");
            SPRING.put(f, b);
        }
        return b;
    }

    @Shadow @Final private HeightProvider height;

    @Inject(method = "getPositions", at = @At("HEAD"), cancellable = true)
    private void worldbound$relative(PlacementContext pctx, RandomSource random, BlockPos pos,
                                     CallbackInfoReturnable<java.util.stream.Stream<BlockPos>> cir) {
        Earth e = Earth.get();
        if (pctx.getMinGenY() != e.curve.minY || pctx.getGenDepth() != e.curve.maxY - e.curve.minY + 1) return;
        int yv = height.sample(random, new WorldGenerationContext(pctx.generator(), VANILLA));
        // Outside a vanilla world's height these placements would simply fail; keep vanilla's ore density.
        if (yv < -64 || yv >= 320) { cir.setReturnValue(java.util.stream.Stream.empty()); return; }
        Earth.Surface s = e.at(pos.getX(), pos.getZ());
        int ground = s.solidTop(), y = ground + yv - 63;
        // Water/lava springs: on Earth's huge cliff faces they become sheets of lava and water pouring down the
        // mountains. Keep them deep underground, where they only feed caves.
        boolean drop = pctx.topFeature().map(f -> isSpring(pctx, f)).orElse(false) && y > ground - 40;
        // Never in the water column above a sea/lake bed.
        drop |= y > ground && (s.ocean() || s.waterDepth() > 0);
        drop |= y <= e.curve.minY + 4 || y >= e.curve.maxY;
        cir.setReturnValue(drop ? java.util.stream.Stream.empty() : java.util.stream.Stream.of(pos.atY(y)));
    }
}
