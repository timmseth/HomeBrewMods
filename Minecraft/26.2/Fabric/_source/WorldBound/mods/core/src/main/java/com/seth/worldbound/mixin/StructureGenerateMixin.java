package com.seth.worldbound.mixin;

import com.seth.worldbound.worldgen.EarthStructures;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Earth worlds: fit every new structure to the real ground (see EarthStructures). */
@Mixin(Structure.class)
public abstract class StructureGenerateMixin {
    @Inject(method = "generate", at = @At("RETURN"), cancellable = true)
    private void worldbound$fit(net.minecraft.core.Holder<Structure> holder, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dim,
                                net.minecraft.core.RegistryAccess access, net.minecraft.world.level.chunk.ChunkGenerator gen,
                                net.minecraft.world.level.biome.BiomeSource biomes, net.minecraft.world.level.levelgen.RandomState random,
                                net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager templates, long seed,
                                net.minecraft.world.level.ChunkPos pos, int refs, LevelHeightAccessor height,
                                java.util.function.Predicate<net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome>> validBiome,
                                CallbackInfoReturnable<StructureStart> cir) {
        StructureStart s = cir.getReturnValue();
        StructureStart a = EarthStructures.adjust(s, height);
        if (a != s) cir.setReturnValue(a);
    }
}
