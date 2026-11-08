package com.seth.worldbound.worldgen;

import com.seth.worldbound.Earth;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * BIOME_TABLE.md as code — keep the two in sync. Returns vanilla biome paths ("plains").
 * Pure data + logic, no Minecraft types, so it is unit-testable.
 */
public final class BiomeTable {
    private BiomeTable() {}

    /** id 1..30 -> {code, default, tree, bare, snowline}. Index 0 unused. */
    private static final String[][] ROWS = {
        null,
        {"Af", "jungle", "jungle", "sparse_jungle", "4800"},
        {"Am", "sparse_jungle", "jungle", "savanna", "4800"},
        {"Aw", "savanna", "sparse_jungle", "savanna_plateau", "4800"},
        {"BWh", "desert", "desert", "desert", "4500"},
        {"BWk", "badlands", "wooded_badlands", "eroded_badlands", "3800"},
        {"BSh", "savanna_plateau", "savanna", "desert", "4500"},
        {"BSk", "plains", "wooded_badlands", "badlands", "3600"},
        {"Csa", "plains", "forest", "stony_shore", "3200"},
        {"Csb", "forest", "forest", "plains", "3000"},
        {"Csc", "taiga", "taiga", "meadow", "2400"},
        {"Cwa", "forest", "bamboo_jungle", "savanna", "4200"},
        {"Cwb", "meadow", "forest", "plains", "4000"},
        {"Cwc", "grove", "taiga", "meadow", "3400"},
        {"Cfa", "forest", "dark_forest", "plains", "3800"},
        {"Cfb", "forest", "dark_forest", "meadow", "2800"},
        {"Cfc", "taiga", "old_growth_spruce_taiga", "meadow", "1600"},
        {"Dsa", "plains", "forest", "badlands", "3400"},
        {"Dsb", "forest", "old_growth_pine_taiga", "meadow", "3000"},
        {"Dsc", "taiga", "old_growth_spruce_taiga", "snowy_plains", "2200"},
        {"Dsd", "snowy_taiga", "snowy_taiga", "snowy_plains", "1800"},
        {"Dwa", "plains", "forest", "plains", "3400"},
        {"Dwb", "forest", "birch_forest", "meadow", "3000"},
        {"Dwc", "taiga", "taiga", "snowy_plains", "2200"},
        {"Dwd", "snowy_taiga", "snowy_taiga", "snowy_plains", "1800"},
        {"Dfa", "plains", "forest", "plains", "3400"},
        {"Dfb", "forest", "birch_forest", "meadow", "3000"},
        {"Dfc", "taiga", "old_growth_spruce_taiga", "snowy_plains", "2000"},
        {"Dfd", "snowy_taiga", "snowy_taiga", "snowy_plains", "1600"},
        {"ET", "snowy_plains", "snowy_taiga", "snowy_slopes", "800"},
        {"EF", "frozen_peaks", "frozen_peaks", "ice_spikes", "0"},
    };

    public static final String[] OCEANS = {
        "warm_ocean", "lukewarm_ocean", "ocean", "cold_ocean", "frozen_ocean",
        "deep_lukewarm_ocean", "deep_ocean", "deep_cold_ocean", "deep_frozen_ocean",
    };
    public static final String[] OVERRIDES = {
        "river", "swamp", "mangrove_swamp", "snowy_plains", "frozen_peaks", "jagged_peaks", "snowy_slopes",
        "stony_peaks", "beach", "snowy_beach", "stony_shore",
        "dripstone_caves", "lush_caves", "deep_dark",
    };

    /** Every biome path this table can return, for BiomeSource.collectPossibleBiomes. */
    public static Set<String> allBiomes() {
        Set<String> s = new LinkedHashSet<>();
        for (int i = 1; i < ROWS.length; i++) {
            s.add(ROWS[i][1]); s.add(ROWS[i][2]); s.add(ROWS[i][3]);
        }
        for (String o : OCEANS) s.add(o);
        for (String o : OVERRIDES) s.add(o);
        return s;
    }

    public static String code(int koppen) {
        return koppen >= 1 && koppen < ROWS.length ? ROWS[koppen][0] : "none";
    }

    public static int snowline(int koppen) {
        return koppen >= 1 && koppen < ROWS.length ? Integer.parseInt(ROWS[koppen][4]) : 3000;
    }

    /**
     * Where permanent snow starts, by climate and latitude. Snow is a cap on the last stretch of a high mountain,
     * not a blanket: dry and mid-latitude classes carry their snowlines far above the class average (Tibet and
     * the Altiplano are bare at 4,000 m; the Rockies' snow sits above ~3,500 m), and alpine tundra outside the
     * polar regions is meadow and rock rather than snowfield.
     */
    public static int snowline(int koppen, double lat, double lon) {
        int snow = snowline(koppen);
        double absLat = Math.abs(lat);
        if (koppen >= 4 && koppen <= 7 && absLat < 40) snow = Math.max(snow, 5500);         // B: Tibet, Altiplano, Iran bare at 4,000 m
        // Continental (D) and temperate (C) classes: 5,000 m on the Tibetan plateau and the subtropical ranges,
        // easing to the Rockies' ~3,800 m by 40° and the Alps' ~3,400 m by 50°.
        if (absLat < 50 && koppen >= 17 && koppen <= 28) snow = Math.max(snow, (int) (absLat <= 36 ? 5000 : Math.max(3800, 5000 - (absLat - 36) * 300)));
        if (absLat < 50 && koppen >= 8 && koppen <= 16) snow = Math.max(snow, (int) (absLat <= 36 ? 5000 : Math.max(3400, 5000 - (absLat - 36) * 400)));
        if (koppen <= 3) snow = Math.max(snow, 5000);                                           // tropics
        // Alpine tundra / ice classes on high ground outside the polar regions: 5,500 m on the Tibetan plateau,
        // easing to ~3,500 m in the Alps and Rockies and 2,500 m by 60°.
        if (koppen >= 29 && absLat < 60) snow = Math.max(snow, (int) (absLat <= 30 ? 5500 : 5500 - (absLat - 30) * 100));
        // Mount Fuji: the stereotypical snow cap, on the top ~400 m.
        if (Math.abs(lat - 35.36) < 0.1 && Math.abs(lon - 138.73) < 0.12) snow = Math.min(snow, 3350);
        return snow;
    }

    /** Surface biome for a column (PLAN.md §5.3 precedence, BIOME_TABLE.md overrides). */
    public static String surface(Earth.Surface s) {
        double elev = s.elevM();
        int k = s.climate();
        int cover = s.cover();
        double absLat = Math.abs(s.lat());

        if (s.ocean()) {
            int band = absLat < 23 ? 0 : absLat < 40 ? 1 : absLat < 58 ? 2 : absLat < 68 ? 3 : 4;
            boolean deep = elev < -1500;
            if (!deep) return OCEANS[band];
            return OCEANS[Math.max(5, band + 4)]; // warm has no deep variant -> deep_lukewarm
        }
        if (cover == 80) return "river";
        if (cover == 90) return k >= 1 && k <= 3 ? "mangrove_swamp" : "swamp";

        int snow = snowline(k, s.lat(), s.lon());
        boolean steep = s.slope() > 1.0;
        // Glaciers and ice caps (WorldCover or Copernicus snow/ice): packed ice and snow, not a dusting on grass.
        // Steep faces are bare rock under snow (jagged_peaks), gentle slopes snowfields, only flat ground blue-white ice.
        if (cover == 70 || s.veg() == 70) return s.slope() > 1.0 ? "jagged_peaks" : s.slope() < 0.35 ? "frozen_peaks" : "snowy_slopes";
        if (elev >= snow) {
            if (steep) return "jagged_peaks";
            return k == 30 && absLat > 55 && s.slope() < 0.35 ? "frozen_peaks" : "snowy_slopes"; // peaks are snow; ice only on ice caps/glaciers
        }
        if (k >= 4 && k <= 7 && elev >= snow - 600) return "stony_peaks";

        if (s.nearOcean()) {
            if (s.cliff() || steep) return "stony_shore";
            if (k == 29 || k == 30) return "snowy_beach";
            return "beach";
        }
        if (k < 1 || k >= ROWS.length) return elev > 0 ? "plains" : "ocean";
        // High ground under the cap: bare rock where it's steep, alpine meadow where it isn't (treeline ~ snow - 1000).
        if (k < 29 && snow >= 2500 && elev >= snow - 1000) return steep ? "stony_peaks" : "meadow";
        // Alpine tundra outside the polar regions (Tibet, the Altiplano): steppe, not snowfield.
        if (k >= 29 && absLat < 60) return steep ? "stony_peaks" : elev > 3000 ? "meadow" : "plains";
        String[] row = ROWS[k];
        if (s.veg() != 0) {
            String v = fromVeg(k, row, s.veg(), elev);
            if (v != null) {
                // Fine land cover (streamed WorldCover, ~19 m) refines the 300 m class: fields and clearings inside
                // forests, woods inside farmland.
                boolean forest = v.contains("forest") || v.contains("taiga") || v.contains("jungle");
                if (forest && (cover == 20 || cover == 30 || cover == 40)) return k <= 3 ? "savanna" : k >= 29 ? "snowy_plains" : "plains";
                if (!forest && cover == 10 && !row[2].equals(row[1])) return row[2];
                return v;
            }
        }
        if (cover == 10 || cover == 95) return row[2];
        if (cover == 60 || cover == 100) return row[3];
        return row[1];
    }

    /**
     * Copernicus LC100 land cover -> biome, within the climate row. Closed forest gets the forest of its leaf type,
     * open forest the sparse/savanna version, grass and shrub the open biome, bare ground the row's bare biome.
     * Returns null to fall back to the row default.
     */
    static String fromVeg(int k, String[] row, int veg, double elev) {
        boolean A = k <= 3, B = k >= 4 && k <= 7, C = k >= 8 && k <= 16, D = k >= 17 && k <= 28, E = k >= 29;
        boolean dry = B || k == 8 || k == 17; // deserts, steppes, hot-summer Mediterranean
        int leaf = veg >= 111 && veg <= 116 ? veg - 110 : veg >= 121 && veg <= 126 ? veg - 120 : 0;
        boolean closed = veg >= 111 && veg <= 116;
        if (leaf != 0) {
            if (E) return "snowy_taiga";
            if (B) return closed ? "wooded_badlands".equals(row[2]) ? "wooded_badlands" : "savanna" : row[1];
            switch (leaf) {
                case 1: // evergreen needleleaf
                    if (A) return "savanna_plateau";
                    if (D && (k == 20 || k == 24 || k == 28)) return "snowy_taiga";
                    if (D) return closed ? "old_growth_spruce_taiga" : "taiga";
                    return closed && elev < 1500 ? "old_growth_pine_taiga" : "taiga";
                case 2: // evergreen broadleaf
                    if (A) return closed ? "jungle" : "sparse_jungle";
                    return closed ? "dark_forest" : "forest";
                case 3: // deciduous needleleaf (larch)
                    return D && (k == 20 || k == 24 || k == 28) ? "snowy_taiga" : "taiga";
                case 4: // deciduous broadleaf
                    if (A) return closed ? "sparse_jungle" : "savanna";
                    if (D && (k >= 26 || k == 22 || k == 18)) return "birch_forest";
                    return closed ? (C ? "dark_forest" : "forest") : dry ? "savanna" : "forest";
                case 5: // mixed
                    if (A) return "sparse_jungle";
                    return D ? (closed ? "taiga" : "forest") : "forest";
                default: // unknown leaf type
                    return closed ? row[2] : row[1];
            }
        }
        switch (veg) {
            case 20: // shrubs
                if (A) return "savanna";
                if (B) return row[1];
                if (E) return "snowy_plains";
                return dry ? "savanna" : "plains";
            case 30: // herbaceous (grassland)
                if (A) return "savanna";
                if (E) return "snowy_plains";
                if (B) return k == 4 ? "desert" : "plains";
                return elev > 1800 ? "meadow" : "plains";
            case 40: case 50: // cropland, urban
                if (A) return "savanna";
                if (k == 4) return "desert";
                return E ? "snowy_plains" : "plains";
            case 60: // bare / sparse
                return row[3].equals("meadow") || row[3].equals("plains") ? "stony_shore" : row[3];
            case 90: // herbaceous wetland
                return A ? "mangrove_swamp" : E ? "snowy_plains" : "swamp";
            case 100: // moss and lichen (tundra)
                return C || D || E ? "snowy_plains" : null;
            default:
                return null;
        }
    }

    /** Below the surface by more than 32 blocks. caveNoise in [-1, 1]; minY the world floor. */
    public static String cave(String surfaceBiome, int y, int surfaceY, int minY, double caveNoise) {
        // Deep dark: 80+ blocks under the local ground in some regions (where ancient cities spawn), and at the floor.
        if (y < minY + 300 || (surfaceY - y >= 80 && caveNoise < -0.2)) return "deep_dark";
        if (caveNoise > 0.45) return "lush_caves";
        if (caveNoise < -0.45) return "dripstone_caves";
        return surfaceBiome;
    }
}
