package com.seth.worldbound.client.horizon;

import com.seth.worldbound.Earth;
import com.seth.worldbound.worldgen.BiomeTable;
import com.seth.worldbound.worldgen.Seasons;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.biome.Biome;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Colours for the horizon mesh: what a column of the real world would look like from far away, from the same
 * biome / ground / canopy / snow rules the chunks and the Distant Horizons generator use. Tints come from the
 * biome itself (grass, foliage, water), so modded biome colours carry through.
 */
final class HorizonColors {
    /** A column's look: top colour, side colour (what the cliff under it is made of) and the top's height. */
    record Look(int top, int side, int topY, boolean water) {}

    private record Tint(int grass, int foliage, int water) {}

    private final Map<String, Tint> tints = new ConcurrentHashMap<>();
    private static final Tint DEFAULT = new Tint(0x91BD59, 0x77AB2F, 0x3F76E4);

    // Average colours of the block textures (what a block looks like at a distance).
    static final int DIRT = 0x866043, STONE = 0x7E7E7E, SAND = 0xDBD0A3, SANDSTONE = 0xD9CE9E, RED_SAND = 0xBE6B2E,
            TERRACOTTA = 0x985E43, GRAVEL = 0x838180, SNOW = 0xF4FAFA, PACKED_ICE = 0x8DB4FA, MUD = 0x3D3A3E,
            DEEPSLATE = 0x505050, SPRUCE = 0x3A6A3A, BIRCH = 0x6E9A4A, CHERRY = 0xE7A8C9;

    /** Biome id -> {grass, foliage, water} colours, or null for defaults (headless tests). */
    private final java.util.function.Function<String, int[]> tinter;

    HorizonColors(java.util.function.Function<String, int[]> tinter) {
        this.tinter = tinter;
    }

    /** The in-game tinter: the biome registry of the level being played. */
    static int[] registryTint(String biomeId) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return null;
            var opt = mc.level.registryAccess().lookupOrThrow(Registries.BIOME).get(Identifier.parse(biomeId));
            if (opt.isEmpty()) return null;
            Biome b = opt.get().value();
            return new int[]{b.getGrassColor(0, 0) & 0xFFFFFF, b.getFoliageColor() & 0xFFFFFF, b.getWaterColor() & 0xFFFFFF};
        } catch (RuntimeException e) {
            return null;
        }
    }

    private Tint tint(String biomeId) {
        Tint t = tints.get(biomeId);
        if (t != null) return t;
        int[] c = tinter == null ? null : tinter.apply(biomeId);
        t = c == null ? DEFAULT : new Tint(c[0], c[1], c[2]);
        tints.put(biomeId, t);
        return t;
    }

    /** What one column looks like; {@code coarse} = closed canopies instead of speckled trees (far levels). */
    Look look(Earth e, Earth.Surface s, int bx, int bz, int spacing, boolean coarse) {
        String biome = BiomeTable.surface(s);
        String id = biome.indexOf(':') < 0 ? "minecraft:" + biome : biome;
        String p = biome.substring(biome.indexOf(':') + 1);
        Tint t = tint(id);
        int minY = e.curve.minY, sea = e.curve.seaLevelY;
        int st = Math.max(minY + 4, Math.min(e.curve.maxY - 8, s.solidTop()));
        int wt = s.waterDepth() > 0 ? s.surfaceY() : s.ocean() ? sea : st;
        if (wt > st) {
            // Water: the biome's colour, darker the deeper it is (the sea floor shows through in the shallows).
            int depth = wt - st;
            int floor = s.elevM() < -1500 ? GRAVEL : SAND;
            int water = scale(t.water, 0.9);
            int top = depth < 12 ? mix(floor, water, 0.55 + 0.45 * depth / 12.0) : scale(water, Math.max(0.45, 1.0 - (depth - 12) / 120.0));
            return new Look(top, scale(t.water, 0.5), wt, true);
        }
        boolean steep = s.slope() > 1.4;
        int top, side;
        if (p.contains("desert") || p.equals("beach")) { top = SAND; side = SANDSTONE; }
        else if (p.contains("badlands")) { top = steep ? TERRACOTTA : RED_SAND; side = TERRACOTTA; }
        else if (p.equals("frozen_peaks") || p.contains("ice_spikes")) { top = PACKED_ICE; side = PACKED_ICE; }
        else if (p.equals("jagged_peaks")) { top = steep ? STONE : SNOW; side = STONE; }
        else if (p.contains("snowy") || p.equals("grove") || p.contains("frozen")) { top = steep ? STONE : SNOW; side = steep ? STONE : DIRT; }
        else if (p.contains("stony") || p.contains("gravelly")) { top = p.contains("gravelly") ? GRAVEL : STONE; side = STONE; }
        else if (p.equals("mangrove_swamp")) { top = MUD; side = MUD; }
        else if (steep) { top = STONE; side = STONE; }
        else { top = scale(t.grass, 0.78); side = DIRT; }   // grass_block: the biome tint over the grey texture
        if (top != SNOW && top != PACKED_ICE && Seasons.freezing(s.lat(), s.lon(), s.climate(), s.elevM())) top = SNOW;
        int topY = st;
        // Forest canopy: the column rises to the tree tops and takes the leaf colour (speckled close up, a
        // closed canopy far away), as the Distant Horizons generator does.
        if (top != SNOW && !p.contains("peaks") && !p.contains("desert") && !p.contains("beach") && !p.contains("ocean")
                && !p.contains("snowy_slopes")) {
            int cover = e.stack.treeCover(s.lon(), s.lat(), e.zoomForSpacing(spacing));
            if (cover <= 0 || cover > 100) cover = s.cover() == 10 ? 60 : 0;
            if (cover > 0) {
                int h = (int) ((bx * 734287L + bz * 912931L) * 0x9E3779B9L >>> 25) % 100;
                boolean trees = coarse ? cover >= 35 : h < cover;
                if (trees) {
                    int leaves = p.contains("taiga") || p.equals("grove") ? SPRUCE : p.contains("birch") ? BIRCH
                            : p.contains("cherry") ? CHERRY : scale(t.foliage, 0.62);
                    if (p.contains("jungle") || p.contains("dark_forest")) leaves = scale(leaves, 0.85);
                    top = coarse ? mix(top, leaves, Math.min(1.0, cover / 70.0)) : leaves;
                    topY = st + 6;
                    side = scale(leaves, 0.75);
                }
            }
        }
        return new Look(top, side, topY, false);
    }

    static int scale(int rgb, double f) {
        int r = (int) Math.min(255, ((rgb >> 16) & 255) * f), g = (int) Math.min(255, ((rgb >> 8) & 255) * f), b = (int) Math.min(255, (rgb & 255) * f);
        return (r << 16) | (g << 8) | b;
    }

    static int mix(int a, int b, double t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }
}
