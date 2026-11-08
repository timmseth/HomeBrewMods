package com.seth.worldbound.mixin;

import com.seth.worldbound.Earth;
import com.seth.worldbound.worldgen.EarthStructures;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;

/**
 * Earth worlds: jigsaw structures with a fixed start height (trial chambers y −40..−20, ancient cities y −27…)
 * start at that depth *below the local ground* instead (vanilla y 63 = ground), so their biome check and
 * layout happen underground where they belong. Heightmap-projected ones (villages) are untouched.
 */
@Mixin(JigsawStructure.class)
public abstract class JigsawStartMixin {
    private static final LevelHeightAccessor VANILLA = LevelHeightAccessor.create(-64, 384);

    @Shadow @Final private Optional<Heightmap.Types> projectStartToHeightmap;

    @Redirect(method = "findGenerationPoint", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/levelgen/heightproviders/HeightProvider;sample(Lnet/minecraft/util/RandomSource;Lnet/minecraft/world/level/levelgen/WorldGenerationContext;)I"))
    private int worldbound$relativeStart(HeightProvider provider, RandomSource random, WorldGenerationContext wctx, Structure.GenerationContext ctx) {
        if (projectStartToHeightmap.isPresent() || !EarthStructures.isEarth(ctx.heightAccessor())) return provider.sample(random, wctx);
        int yv = provider.sample(random, new WorldGenerationContext(ctx.chunkGenerator(), VANILLA));
        int ground = Earth.get().at(ctx.chunkPos().getMinBlockX() + 8, ctx.chunkPos().getMinBlockZ() + 8).solidTop();
        return Math.max(Earth.get().curve.minY + 24, ground + yv - 63);
    }
}
