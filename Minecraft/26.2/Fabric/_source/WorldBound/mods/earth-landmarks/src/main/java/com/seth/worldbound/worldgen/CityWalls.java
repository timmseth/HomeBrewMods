package com.seth.worldbound.worldgen;

import com.seth.worldbound.Earth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

/**
 * Walled cities: every city of 500,000+ people gets a ring wall around its core, 7 blocks thick with a 5-wide walkway
 * on top and a lit hallway inside, square towers every ~70 blocks, four gatehouses, and a keep at the centre with a
 * landmark obelisk on top. The ring is an irregular (noise-shaped) loop of radius 1.5–4.5 km real, sized by
 * population; it follows the ground and breaks for water. The look depends on the region of the world.
 */
public final class CityWalls {
    public static final long MIN_POP = 500_000;
    /** Wall cross-section, from the outer face: 7 wide. */
    public static final int THICK = 7, HALL_H = 5, FOUND = 2;
    public static final int HALL_Y0 = FOUND, CEIL_Y = FOUND + HALL_H, WALK_Y = CEIL_Y + 1, PARAPET_H = 2, TOWER_EXTRA = 9;

    public record Style(String name, Block wall, Block accent, Block cracked, Block parapet, Block floor, Block hallFloor, Block pillar, Block roof, Block light, Block trim) {}

    public static final Style STONE = new Style("stone", Blocks.STONE_BRICKS, Blocks.CHISELED_STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS, Blocks.STONE_BRICKS,
            Blocks.COBBLESTONE, Blocks.STONE_BRICKS, Blocks.STRIPPED_DARK_OAK_LOG, Blocks.DARK_OAK_PLANKS, Blocks.LANTERN, Blocks.MOSSY_STONE_BRICKS);
    public static final Style SANDSTONE = new Style("sandstone", Blocks.SMOOTH_SANDSTONE, Blocks.CHISELED_SANDSTONE, Blocks.SANDSTONE, Blocks.CUT_SANDSTONE,
            Blocks.SANDSTONE, Blocks.CUT_SANDSTONE, Blocks.ACACIA_LOG, Blocks.ACACIA_PLANKS, Blocks.LANTERN, Blocks.TERRACOTTA);
    public static final Style RED_FORT = new Style("red fort", Blocks.SMOOTH_RED_SANDSTONE, Blocks.CHISELED_RED_SANDSTONE, Blocks.RED_SANDSTONE, Blocks.CUT_RED_SANDSTONE,
            Blocks.RED_SANDSTONE, Blocks.CUT_RED_SANDSTONE, Blocks.QUARTZ_PILLAR, Blocks.QUARTZ_BLOCK, Blocks.LANTERN, Blocks.CHISELED_QUARTZ_BLOCK);
    public static final Style EASTERN = new Style("eastern", Blocks.DEEPSLATE_BRICKS, Blocks.CHISELED_DEEPSLATE, Blocks.CRACKED_DEEPSLATE_BRICKS, Blocks.DEEPSLATE_TILES,
            Blocks.POLISHED_DEEPSLATE, Blocks.DEEPSLATE_TILES, Blocks.STRIPPED_DARK_OAK_LOG, Blocks.DYED_TERRACOTTA.red(), Blocks.LANTERN, Blocks.DYED_TERRACOTTA.red());
    public static final Style KREMLIN = new Style("kremlin", Blocks.BRICKS, Blocks.CHISELED_STONE_BRICKS, Blocks.BRICKS, Blocks.BRICKS,
            Blocks.STONE_BRICKS, Blocks.BRICKS, Blocks.STRIPPED_SPRUCE_LOG, Blocks.DYED_TERRACOTTA.green(), Blocks.LANTERN, Blocks.CALCITE);
    public static final Style MUD = new Style("mud", Blocks.MUD_BRICKS, Blocks.PACKED_MUD, Blocks.PACKED_MUD, Blocks.MUD_BRICKS,
            Blocks.PACKED_MUD, Blocks.MUD_BRICKS, Blocks.ACACIA_LOG, Blocks.ACACIA_PLANKS, Blocks.TORCH, Blocks.TERRACOTTA);
    public static final Style COLONIAL = new Style("colonial", Blocks.BRICKS, Blocks.POLISHED_GRANITE, Blocks.BRICKS, Blocks.STONE_BRICKS,
            Blocks.GRANITE, Blocks.STONE_BRICKS, Blocks.POLISHED_GRANITE, Blocks.SPRUCE_PLANKS, Blocks.LANTERN, Blocks.CHISELED_STONE_BRICKS);
    public static final Style INCA = new Style("inca", Blocks.POLISHED_ANDESITE, Blocks.CHISELED_STONE_BRICKS, Blocks.ANDESITE, Blocks.POLISHED_ANDESITE,
            Blocks.COBBLED_DEEPSLATE, Blocks.POLISHED_ANDESITE, Blocks.ANDESITE, Blocks.DYED_TERRACOTTA.orange(), Blocks.TORCH, Blocks.GOLD_BLOCK);
    public static final Style TROPICAL = new Style("tropical", Blocks.MOSSY_STONE_BRICKS, Blocks.CHISELED_STONE_BRICKS, Blocks.MOSSY_COBBLESTONE, Blocks.STONE_BRICKS,
            Blocks.COBBLESTONE, Blocks.STONE_BRICKS, Blocks.JUNGLE_PLANKS, Blocks.JUNGLE_PLANKS, Blocks.LANTERN, Blocks.PRISMARINE_BRICKS);

    /** One walled city, in block coordinates of the world being generated. */
    public record City(String name, double cx, double cz, double radius, long seed, Style style, double[] shape, double[] gates) {
        /** Ring radius at angle θ (irregular loop). */
        public double r(double theta) {
            double m = 1 + shape[0] * Math.cos(theta + shape[1]) + shape[2] * Math.cos(2 * theta + shape[3]) + shape[4] * Math.cos(3 * theta + shape[5]);
            return radius * m;
        }
    }

    private static List<City> cities;
    private static double builtForScale = -1;

    public static synchronized List<City> cities() {
        Earth e = Earth.get();
        if (cities != null && builtForScale == e.config.scale_m_per_block) return cities;
        List<City> l = new ArrayList<>();
        var t = e.transform;
        for (com.seth.worldbound.api.Places.Place p : com.seth.worldbound.api.WorldBound.places().cities()) {
            if (p.population() < MIN_POP) continue;
            double rM = 1500 + 650 * Math.log(p.population() / 500_000.0);
            double radius = rM / e.config.scale_m_per_block;
            long seed = (p.name() + p.country()).hashCode() * 0x9E3779B97F4A7C15L;
            java.util.Random rnd = new java.util.Random(seed);
            double[] shape = {0.08 + rnd.nextDouble() * 0.08, rnd.nextDouble() * Math.PI * 2, 0.05 + rnd.nextDouble() * 0.07, rnd.nextDouble() * Math.PI * 2,
                    0.02 + rnd.nextDouble() * 0.05, rnd.nextDouble() * Math.PI * 2};
            double g0 = rnd.nextDouble() * Math.PI / 2;
            double[] gates = {g0, g0 + Math.PI / 2, g0 + Math.PI, g0 + Math.PI * 1.5};
            l.add(new City(p.name(), t.blockX(p.lon()), t.blockZ(p.lat()), radius, seed, styleFor(p.lat(), p.lon(), p.country()), shape, gates));
        }
        cities = l;
        builtForScale = e.config.scale_m_per_block;
        return l;
    }

    static Style styleFor(double lat, double lon, String country) {
        if (country.equals("RU") || country.equals("BY") || country.equals("UA") && lat > 48) return KREMLIN;
        if (lon >= -30 && lon <= 60 && lat >= 35) return STONE;                       // Europe, Anatolia, Caucasus
        if (lon >= -20 && lon <= 75 && lat >= 10 && lat < 42) return SANDSTONE;        // North Africa, Middle East, Central Asia
        if (lon > 60 && lon <= 93 && lat >= 5 && lat < 37) return RED_FORT;            // South Asia
        if (lon > 93 && lat >= 18) return EASTERN;                                     // China, Korea, Japan, Mongolia
        if (lon > 93 && lat < 18 && lat > -12) return TROPICAL;                        // Southeast Asia
        if (lon >= -20 && lon <= 55 && lat < 10 && lat > -36) return MUD;              // Sub-Saharan Africa
        if (lon < -30 && lat > 23) return COLONIAL;                                    // North America
        if (lon < -30) return INCA;                                                    // Latin America
        return STONE;                                                                  // Australia, Pacific, rest
    }

    /** Which cities' rings or keeps can touch a chunk; {city, kind}: kind 0 = ring, 1 = keep. */
    public static List<City> near(int chunkX, int chunkZ) {
        List<City> out = null;
        double cx = chunkX * 16 + 8, cz = chunkZ * 16 + 8;
        for (City c : cities()) {
            double d = Math.hypot(cx - c.cx, cz - c.cz);
            if (d < c.radius * 1.35 + 64) {
                if (out == null) out = new ArrayList<>(2);
                out.add(c);
            }
        }
        return out == null ? List.of() : out;
    }
}
