package com.seth.worldbound.worldgen;

import com.seth.worldbound.Earth;
import com.seth.worldbound.WorldBoundMod;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;

import java.util.ArrayList;
import java.util.List;

/**
 * "Wild" biome mode: the real planet's shape (elevation, coasts, rivers, lakes, snow caps, glaciers) covered in
 * big patches of every overworld biome installed (vanilla and every biome mod), instead of the real climate.
 * Patches are ~3–4 km real across, picked from a jittered grid so each one is a single biome with ragged edges.
 */
public final class WildBiomes {
    private final List<Holder<Biome>> lowland = new ArrayList<>(), highland = new ArrayList<>();

    WildBiomes(HolderGetter<Biome> biomes) {
        if (!(biomes instanceof HolderLookup<Biome> lookup)) return;
        lookup.listElements().forEach(h -> {
            String id = h.key().identifier().toString();
            if (!h.is(BiomeTags.IS_OVERWORLD) || h.is(BiomeTags.IS_NETHER) || h.is(BiomeTags.IS_END)) return;
            if (h.is(BiomeTags.IS_OCEAN) || h.is(BiomeTags.IS_DEEP_OCEAN) || h.is(BiomeTags.IS_RIVER) || h.is(BiomeTags.IS_BEACH)) return;
            if (id.contains("ocean") || id.contains("river") || id.contains("beach") || id.contains("shore") || id.contains("cave")
                    || id.contains("void") || id.contains("deep_dark") || id.contains("underground") || id.contains("sky")
                    || id.contains("island") || id.contains("coast") || id.contains("lagoon") || id.contains("reef")) return;
            boolean mountain = h.is(BiomeTags.IS_MOUNTAIN) || h.is(BiomeTags.IS_HILL) || id.contains("peak") || id.contains("slope")
                    || id.contains("mountain") || id.contains("alpine") || id.contains("highland") || id.contains("cliff")
                    || id.contains("plateau") || id.contains("mesa") || id.contains("canyon") || id.contains("grove")
                    || id.contains("hill") || id.contains("volcan") || id.contains("crag") || id.contains("ridge");
            (mountain ? highland : lowland).add(h);
        });
        WorldBoundMod.LOG.info("WorldBound: wild biome pool {} lowland + {} highland biomes", lowland.size(), highland.size());
    }

    boolean ready() {
        return !lowland.isEmpty();
    }

    List<Holder<Biome>> all() {
        List<Holder<Biome>> l = new ArrayList<>(lowland);
        l.addAll(highland);
        return l;
    }

    /** The patch biome for this column, or null to use the realistic choice (water, glaciers, snow caps). */
    Holder<Biome> pick(Earth e, Earth.Surface s, int bx, int bz, String realistic) {
        if (s.ocean() || s.cover() == 80 || s.cover() == 90 || s.cover() == 70 || s.veg() == 70) return null;
        int snow = BiomeTable.snowline(s.climate(), s.lat(), s.lon());
        if (s.elevM() >= snow - 150 || s.nearOcean()) return null;   // caps, glaciers and shores stay real
        boolean high = s.elevM() >= snow - 1400 || s.slope() > 1.2;
        List<Holder<Biome>> pool = high && !highland.isEmpty() ? highland : lowland;
        // jittered grid of cells ~3.5 km real across, warped by low-frequency noise so edges wander
        double cell = e.tuned(1200);   // 1,200 blocks at 1:10 = 12 km... scaled to this world: ~3.6 km real at 1:3
        double wx = bx + e.patchNoise(bx + 4242, bz, e.tuned(300)) * cell * 0.35;
        double wz = bz + e.patchNoise(bx, bz + 4242, e.tuned(300)) * cell * 0.35;
        int cx = (int) Math.floor(wx / cell), cz = (int) Math.floor(wz / cell);
        // nearest of the 3x3 jittered cell centres (Worley), so cells are blobs rather than squares
        long best = 0;
        double bestD = Double.MAX_VALUE;
        for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) {
            long hx = hash(cx + i, cz + j, 1), hz = hash(cx + i, cz + j, 2);
            double px = (cx + i + 0.15 + 0.7 * (hx & 1023) / 1024.0) * cell, pz = (cz + j + 0.15 + 0.7 * (hz & 1023) / 1024.0) * cell;
            double d = (px - wx) * (px - wx) + (pz - wz) * (pz - wz);
            if (d < bestD) { bestD = d; best = hash(cx + i, cz + j, 3); }
        }
        return pool.get((int) Math.floorMod(best, (long) pool.size()));
    }

    private static long hash(int x, int z, int salt) {
        long h = x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL ^ salt * 0x165667B19E3779F9L ^ Earth.get().config.wild_seed;
        h ^= h >>> 29; h *= 0xBF58476D1CE4E5B9L; h ^= h >>> 32;
        return h;
    }
}
