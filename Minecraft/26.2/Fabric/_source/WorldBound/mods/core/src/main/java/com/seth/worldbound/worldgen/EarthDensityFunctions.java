package com.seth.worldbound.worldgen;

import com.mojang.serialization.MapCodec;
import com.seth.worldbound.Earth;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Density-function types usable from noise_settings JSON:
 * <ul>
 *   <li>{@code worldbound:elevation} — surface block y of the column (wrap it in minecraft:cache_2d).</li>
 *   <li>{@code worldbound:water_depth} — blocks of standing water above the lowered surface (0 until Phase 3).</li>
 *   <li>{@code worldbound:surface_density} — clamp((surface − y) / 32, −1, 1): positive = solid.
 *       The JSON wraps it in minecraft:interpolated so it is evaluated per cell, not per block.</li>
 * </ul>
 */
public final class EarthDensityFunctions {
    private EarthDensityFunctions() {}

    public static void register() {
        reg("elevation", Elevation.CODEC);
        reg("water_depth", WaterDepth.CODEC);
        reg("surface_density", SurfaceDensity.CODEC);
    }

    private static void reg(String name, KeyDispatchDataCodec<? extends DensityFunction> codec) {
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, Identifier.fromNamespaceAndPath("worldbound", name), codec.codec());
    }

    /** Shared skeleton: a parameterless simple function with a unit codec. */
    private abstract static class Unit implements DensityFunction.SimpleFunction {
        private final KeyDispatchDataCodec<? extends DensityFunction> codec;

        Unit(KeyDispatchDataCodec<? extends DensityFunction> codec) {
            this.codec = codec;
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return codec;
        }
    }

    public static final class Elevation extends Unit {
        public static final Elevation INSTANCE = new Elevation();
        public static final KeyDispatchDataCodec<Elevation> CODEC = KeyDispatchDataCodec.of(MapCodec.unit(INSTANCE));

        private Elevation() {
            super(null);
        }

        @Override public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }

        @Override public double compute(FunctionContext ctx) {
            // The ground (lake and river beds included), not the water surface: surface rules decide sand, gravel,
            // dirt and grass from how far a block is below this, so beds under deep water get them too.
            return Earth.get().at(ctx.blockX(), ctx.blockZ()).solidTop();
        }

        @Override public double minValue() {
            return -4064;
        }

        @Override public double maxValue() {
            return 4064;
        }
    }

    public static final class WaterDepth extends Unit {
        public static final WaterDepth INSTANCE = new WaterDepth();
        public static final KeyDispatchDataCodec<WaterDepth> CODEC = KeyDispatchDataCodec.of(MapCodec.unit(INSTANCE));

        private WaterDepth() {
            super(null);
        }

        @Override public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }

        @Override public double compute(FunctionContext ctx) {
            return Earth.get().at(ctx.blockX(), ctx.blockZ()).waterDepth();
        }

        @Override public double minValue() {
            return 0;
        }

        @Override public double maxValue() {
            return 64;
        }
    }

    public static final class SurfaceDensity extends Unit {
        public static final SurfaceDensity INSTANCE = new SurfaceDensity();
        public static final KeyDispatchDataCodec<SurfaceDensity> CODEC = KeyDispatchDataCodec.of(MapCodec.unit(INSTANCE));

        private SurfaceDensity() {
            super(null);
        }

        @Override public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }

        @Override public double compute(FunctionContext ctx) {
            Earth.Surface s = Earth.get().at(ctx.blockX(), ctx.blockZ());
            double d = (s.surfaceY() - s.waterDepth() - ctx.blockY() + 0.5) / 32.0;
            return d < -1 ? -1 : d > 1 ? 1 : d;
        }

        @Override public double minValue() {
            return -1;
        }

        @Override public double maxValue() {
            return 1;
        }
    }
}
