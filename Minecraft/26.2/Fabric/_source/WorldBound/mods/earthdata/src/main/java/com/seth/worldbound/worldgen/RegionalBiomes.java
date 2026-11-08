package com.seth.worldbound.worldgen;

import com.seth.worldbound.Earth;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Real-world flavour on top of {@link BiomeTable}: named places and regions get the biome that fits them,
 * from Terralith / Terrestria when installed, vanilla otherwise. Every rule returns candidates in order of
 * preference ending in a vanilla biome, so the mod works without the other mods.
 *
 * Rules only replace "ordinary" land biomes (the Köppen table result); peaks, beaches, rivers, lakes,
 * oceans and caves are left alone. {@code patch} is low-frequency noise in [-1, 1] (~20 km blobs) used to
 * mix two biomes in large swaths instead of noise-speckle.
 */
public final class RegionalBiomes {
    private RegionalBiomes() {}

    private record Box(double s, double n, double w, double e) {
        boolean has(double lat, double lon) {
            return lat >= s && lat <= n && lon >= w && lon <= e;
        }
    }

    private static final Set<String> ORDINARY = Set.of(
            "jungle", "sparse_jungle", "savanna", "savanna_plateau", "desert", "badlands", "wooded_badlands", "eroded_badlands",
            "plains", "forest", "dark_forest", "birch_forest", "taiga", "old_growth_spruce_taiga", "old_growth_pine_taiga",
            "snowy_taiga", "snowy_plains", "meadow", "grove", "bamboo_jungle", "swamp", "stony_shore");

    // ---- named places (exact-ish boxes) ----
    private static final Box YELLOWSTONE = new Box(44.13, 45.10, -111.15, -109.83);
    private static final Box YOSEMITE_VALLEY = new Box(37.65, 37.80, -119.75, -119.45);
    private static final Box BRYCE = new Box(37.45, 37.75, -112.30, -112.05);
    private static final Box GRAND_CANYON = new Box(35.90, 36.55, -113.60, -111.60);
    private static final Box MONUMENT_VALLEY = new Box(36.60, 37.30, -110.60, -109.40);
    private static final Box DOVER = new Box(51.00, 51.30, 0.95, 1.60);
    private static final Box PROVENCE = new Box(43.60, 44.30, 4.80, 6.60);

    // ---- regions ----
    private static final Box REDWOOD_COAST = new Box(36.5, 42.5, -124.6, -123.0);
    private static final Box PACIFIC_NW = new Box(42.5, 60.5, -140.0, -121.0);
    private static final Box JAPAN_KOREA = new Box(30.0, 46.0, 125.0, 146.5);
    private static final Box SIBERIA = new Box(50.0, 73.0, 60.0, 180.0);
    private static final Box CENTRAL_ASIA_STEPPE = new Box(38.0, 56.0, 30.0, 125.0);
    private static final Box US_WEST = new Box(30.0, 50.0, -125.0, -102.0);
    private static final Box AUSTRALIA = new Box(-36.0, -11.0, 112.0, 154.0);
    private static final Box US_SOUTHEAST = new Box(25.0, 37.5, -97.5, -75.0);
    private static final Box AMAZON = new Box(-17.0, 6.0, -80.0, -44.0);
    private static final Box SOUTH_CHINA = new Box(18.0, 31.0, 98.0, 122.0);
    private static final Box WEST_EUROPE = new Box(43.0, 58.0, -10.0, 30.0);
    private static final Box EAST_US = new Box(32.0, 47.5, -95.0, -66.0);
    private static final Box CANADIAN_SHIELD = new Box(45.0, 62.0, -102.0, -58.0);
    private static final Box SCANDI_RUSSIA = new Box(55.0, 70.0, 5.0, 60.0);
    private static final Box PATAGONIA = new Box(-55.0, -38.0, -75.0, -63.0);
    private static final Box SAHARA_ARABIA = new Box(12.0, 35.0, -17.0, 60.0);

    /** Candidates for this column, best first, always ending in a vanilla biome path ("plains" style). */
    public static List<String> pick(Earth.Surface s, String base, double patch, double patch2) {
        if (Earth.get().generated()) return pickAnalog(s, base, patch, patch2);
        List<String> out = new ArrayList<>(3);
        double lat = s.lat(), lon = s.lon(), elev = s.elevM();
        String k = BiomeTable.code(s.climate());
        int snow = BiomeTable.snowline(s.climate(), s.lat(), s.lon());
        boolean wet = s.ocean() || s.cover() == 80;
        // Named places beat everything but water (Yellowstone's plateau is above the generic snowline).
        if (!wet) {
            if (YELLOWSTONE.has(lat, lon)) return of(out, "terralith:yellowstone", base);
            if (YOSEMITE_VALLEY.has(lat, lon)) return of(out, "terralith:yosemite_cliffs", base);
            if (BRYCE.has(lat, lon)) return of(out, "terralith:bryce_canyon", "badlands");
        }
        if (!ORDINARY.contains(base)) {
            out.add(base);
            return out;
        }

        // Named places first.
        if (YELLOWSTONE.has(lat, lon)) return of(out, "terralith:yellowstone", base);
        if (YOSEMITE_VALLEY.has(lat, lon)) return of(out, "terralith:yosemite_cliffs", base);
        if (BRYCE.has(lat, lon)) return of(out, "terralith:bryce_canyon", "badlands");
        // Red and orange rock bands (badlands terracotta) on the canyon walls; forest stays on the rims.
        if (GRAND_CANYON.has(lat, lon) && elev < 2100) return of(out, "badlands", "badlands");
        if (MONUMENT_VALLEY.has(lat, lon)) return of(out, "terralith:sandstone_valley", "badlands");
        if (DOVER.has(lat, lon) && s.nearOcean()) return of(out, "terralith:white_cliffs", base);
        if (PROVENCE.has(lat, lon) && (k.startsWith("Cs") || k.startsWith("Cf"))) return of(out, "terralith:lavender_valley", base);

        // High meadows below the snowline in any mountain climate.
        if (elev > snow - 900 && elev < snow && s.slope() < 0.6 && !k.startsWith("B") && !k.startsWith("E"))
            return of(out, "terralith:alpine_highlands", "meadow");

        boolean forestLike = base.contains("forest") || base.contains("taiga") || base.equals("grove") || base.equals("jungle");

        if (REDWOOD_COAST.has(lat, lon) && elev < 1000 && (k.equals("Csb") || k.equals("Csc") || k.equals("Cfb") || k.equals("Csa")))
            return of(out, patch > 0.35 ? "terrestria:lush_redwood_forest" : "terrestria:redwood_forest", "old_growth_spruce_taiga");
        if (PACIFIC_NW.has(lat, lon) && (k.startsWith("Cf") || k.startsWith("Cs") || k.equals("Dfc") || k.equals("Dsc") || k.equals("Dfb"))) {
            if (elev > 1300) return of(out, k.startsWith("D") ? "terrestria:snowy_hemlock_treeline" : "terrestria:hemlock_treeline", "taiga");
            if (forestLike || base.equals("plains")) return of(out, k.startsWith("D") ? "terrestria:snowy_hemlock_forest" : "terrestria:hemlock_rainforest", "old_growth_spruce_taiga");
        }
        if (JAPAN_KOREA.has(lat, lon) && forestLike && elev > 150 && elev < 1600) {
            if (patch > 0.45) return of(out, "terralith:sakura_grove", "cherry_grove");
            if (patch < -0.45) return of(out, "terrestria:japanese_maple_forest", "forest");
            if (patch2 > 0.6) return of(out, "cherry_grove", base);
        }
        if (SIBERIA.has(lat, lon) && (k.equals("Dfc") || k.equals("Dfd") || k.equals("Dwc") || k.equals("Dwd") || k.equals("Dsc")))
            return of(out, patch > 0.3 ? "terralith:siberian_grove" : "terralith:siberian_taiga", base);
        if (k.equals("BSk")) {
            if (CENTRAL_ASIA_STEPPE.has(lat, lon) || PATAGONIA.has(lat, lon)) return of(out, "terralith:steppe", "plains");
            if (US_WEST.has(lat, lon)) return of(out, patch > 0.5 ? "terralith:cold_shrubland" : "terralith:shrubland", "plains");
        }
        if (AUSTRALIA.has(lat, lon) && (k.equals("BWh") || k.equals("BSh"))) return of(out, "terrestria:outback", "desert");
        if (k.equals("BSh")) return of(out, "terralith:hot_shrubland", base);
        if (SAHARA_ARABIA.has(lat, lon) && k.equals("BWh") && patch > 0.55) return of(out, "terralith:ancient_sands", "desert");
        if (US_SOUTHEAST.has(lat, lon) && k.equals("Cfa") && elev < 60 && patch > 0.1)
            return of(out, "terrestria:cypress_swamp", "swamp");
        if (US_SOUTHEAST.has(lat, lon) && k.equals("Cfa") && forestLike && patch < -0.4) return of(out, "terrestria:cypress_forest", base);
        if (AMAZON.has(lat, lon) && k.equals("Af") && patch > 0.0) return of(out, "terralith:tropical_jungle", "jungle");
        if (SOUTH_CHINA.has(lat, lon) && (k.equals("Cwa") || k.equals("Cfa")) && forestLike && patch > 0.2) return of(out, "bamboo_jungle", base);
        if (WEST_EUROPE.has(lat, lon) && forestLike) {
            if (base.equals("dark_forest") && patch2 > 0.75) return of(out, "pale_garden", base);
            if (patch > 0.55) return of(out, "flower_forest", base);
        }
        if (EAST_US.has(lat, lon) && forestLike) {
            if (patch > 0.55) return of(out, "flower_forest", base);
            if (patch < -0.5) return of(out, "terrestria:dense_woodlands", base);
        }
        if (CANADIAN_SHIELD.has(lat, lon) && (k.equals("Dfb") || k.equals("Dfc")) && patch > 0.4) return of(out, "terralith:shield", base);
        if (SCANDI_RUSSIA.has(lat, lon) && k.equals("Dfb") && forestLike && patch > 0.4) return of(out, "old_growth_birch_forest", base);
        if (base.equals("plains") && patch2 > 0.7 && !k.startsWith("B")) return of(out, "sunflower_plains", base);
        out.add(base);
        return out;
    }

    /**
     * A generated (Earthish) planet has no real places, so the regional flavour goes by what the place is like
     * instead: the same climate, height and ground that call for these biomes on Earth.
     */
    static List<String> pickAnalog(Earth.Surface s, String base, double patch, double patch2) {
        List<String> out = new ArrayList<>(3);
        if (!ORDINARY.contains(base)) {
            out.add(base);
            return out;
        }
        String k = BiomeTable.code(s.climate());
        double elev = s.elevM();
        int snow = BiomeTable.snowline(s.climate(), s.lat(), s.lon());
        boolean forestLike = base.contains("forest") || base.contains("taiga") || base.equals("grove") || base.equals("jungle");
        if (elev > snow - 900 && elev < snow && s.slope() < 0.6 && !k.startsWith("B") && !k.startsWith("E"))
            return of(out, "terralith:alpine_highlands", "meadow");
        // wet mild coasts and their hills: redwoods and hemlock rain forest
        if ((k.equals("Csb") || k.equals("Cfb") || k.equals("Csc")) && forestLike && elev < 1000 && patch > 0.45)
            return of(out, patch2 > 0.3 ? "terrestria:lush_redwood_forest" : "terrestria:redwood_forest", "old_growth_spruce_taiga");
        if ((k.equals("Cfb") || k.equals("Cfc")) && forestLike && patch < -0.4)
            return of(out, elev > 1300 ? "terrestria:hemlock_treeline" : "terrestria:hemlock_rainforest", "old_growth_spruce_taiga");
        if ((k.startsWith("Df") || k.startsWith("Dw")) && forestLike && elev > 1300 && patch < -0.4)
            return of(out, "terrestria:snowy_hemlock_forest", "taiga");
        if ((k.equals("Cfa") || k.equals("Cwa") || k.equals("Cfb")) && forestLike && elev > 150 && elev < 1600 && patch2 > 0.72)
            return of(out, patch > 0 ? "terralith:sakura_grove" : "cherry_grove", "cherry_grove");
        if ((k.equals("Dfc") || k.equals("Dfd") || k.equals("Dwc") || k.equals("Dwd") || k.equals("Dsc")) && forestLike)
            return of(out, patch > 0.3 ? "terralith:siberian_grove" : "terralith:siberian_taiga", base);
        if (k.equals("BSk")) return of(out, patch > 0.2 ? "terralith:steppe" : patch < -0.4 ? "terralith:cold_shrubland" : "terralith:shrubland", "plains");
        if (k.equals("BSh")) return of(out, patch < -0.45 ? "terrestria:outback" : "terralith:hot_shrubland", base);
        if (k.equals("BWh") && patch > 0.55) return of(out, "terralith:ancient_sands", "desert");
        if (k.equals("BWh") && patch < -0.55) return of(out, "terrestria:outback", "desert");
        if (k.equals("Cfa") && elev < 60 && s.slope() < 0.3 && patch > 0.35) return of(out, "terrestria:cypress_swamp", "swamp");
        if (k.equals("Cfa") && forestLike && patch < -0.45) return of(out, "terrestria:cypress_forest", base);
        if (k.equals("Af") && patch > 0.0) return of(out, "terralith:tropical_jungle", "jungle");
        if ((k.equals("Cwa") || k.equals("Cfa")) && forestLike && patch > 0.5) return of(out, "bamboo_jungle", base);
        if ((k.equals("Dfb") || k.equals("Dfc")) && s.slope() < 0.3 && elev < 600 && patch > 0.45) return of(out, "terralith:shield", base);
        if (k.equals("Dfb") && forestLike && patch2 > 0.55) return of(out, "old_growth_birch_forest", base);
        if (forestLike && (k.startsWith("Cf") || k.equals("Dfb") || k.equals("Dfa"))) {
            if (base.equals("dark_forest") && patch2 > 0.78) return of(out, "pale_garden", base);
            if (patch > 0.6) return of(out, "flower_forest", base);
            if (patch < -0.55) return of(out, "terrestria:dense_woodlands", base);
        }
        if (base.equals("plains") && patch2 > 0.7 && !k.startsWith("B")) return of(out, "sunflower_plains", base);
        out.add(base);
        return out;
    }

    private static List<String> of(List<String> out, String a, String fallback) {
        out.add(a);
        out.add(fallback);
        return out;
    }

    /** Every non-vanilla or extra-vanilla id the rules can return, for the biome source's possible-biome set. */
    public static Set<String> allIds() {
        return new LinkedHashSet<>(List.of(
                "terralith:yellowstone", "terralith:yosemite_cliffs", "terralith:bryce_canyon", "terralith:desert_canyon",
                "terralith:sandstone_valley", "terralith:white_cliffs", "terralith:lavender_valley", "terralith:alpine_highlands",
                "terrestria:lush_redwood_forest", "terrestria:redwood_forest", "terrestria:snowy_hemlock_treeline",
                "terrestria:hemlock_treeline", "terrestria:snowy_hemlock_forest", "terrestria:hemlock_rainforest",
                "terralith:sakura_grove", "terrestria:japanese_maple_forest", "cherry_grove", "terralith:siberian_grove",
                "terralith:siberian_taiga", "terralith:steppe", "terralith:cold_shrubland", "terralith:shrubland",
                "terrestria:outback", "terralith:hot_shrubland", "terralith:ancient_sands", "terrestria:cypress_swamp",
                "terrestria:cypress_forest", "terralith:tropical_jungle", "bamboo_jungle", "pale_garden", "flower_forest",
                "terrestria:dense_woodlands", "terralith:shield", "old_growth_birch_forest", "sunflower_plains", "meadow",
                "old_growth_spruce_taiga", "swamp", "badlands", "desert", "plains", "jungle", "forest", "taiga"));
    }
}
