package com.seth.worldbound.worldgen;

import com.seth.worldbound.Earth;
import com.seth.worldbound.landmarks.Obelisk;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.List;

/** Places the parts of a city's wall, towers, gates and keep that fall inside this chunk (see {@link CityWalls}). */
public final class CityWallsFeature extends Feature<NoneFeatureConfiguration> {
    private static final double TOWER_SPACING = 70, GATE_HALF = 2.5, TOWER_HALF = 4.5;

    public CityWallsFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        if (!(ctx.chunkGenerator().getBiomeSource() instanceof EarthBiomeSource) || !"on".equals(com.seth.worldbound.api.WorldOptions.get("city_walls")) || Earth.get().generated()) return false;
        int x0 = ctx.origin().getX() & ~15, z0 = ctx.origin().getZ() & ~15;
        List<CityWalls.City> cities = CityWalls.near(x0 >> 4, z0 >> 4);
        if (cities.isEmpty()) return false;
        WorldGenLevel level = ctx.level();
        Earth e = Earth.get();
        boolean any = false;
        for (CityWalls.City c : cities) {
            any |= ring(level, e, c, x0, z0);
            any |= keep(level, e, c, x0, z0);
        }
        return any;
    }

    // ---- the ring wall ----------------------------------------------------------------------

    private boolean ring(WorldGenLevel level, Earth e, CityWalls.City c, int x0, int z0) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        CityWalls.Style st = c.style();
        boolean any = false;
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = x0 + dx, z = z0 + dz;
                double rx = x + 0.5 - c.cx(), rz = z + 0.5 - c.cz();
                double dist = Math.hypot(rx, rz), theta = Math.atan2(rz, rx);
                double r = c.r(theta);
                double offset = dist - r;                       // + outside, - inside
                if (Math.abs(offset) > TOWER_HALF) continue;
                double along = theta * r;                       // arc length along the ring
                double towerStep = TOWER_SPACING / r;           // radians between towers
                double tTheta = Math.round(theta / towerStep) * towerStep;
                double towerAlong = (theta - tTheta) * r;       // distance along the ring from the nearest tower centre
                boolean tower = Math.abs(towerAlong) <= TOWER_HALF;
                // gates: the nearest gate angle, and the two gate towers flanking it
                double gateAlong = Double.MAX_VALUE;
                for (double g : c.gates()) {
                    double d = angleDiff(theta, g) * r;
                    if (Math.abs(d) < Math.abs(gateAlong)) gateAlong = d;
                }
                boolean gate = Math.abs(gateAlong) <= GATE_HALF;
                boolean gateTower = Math.abs(Math.abs(gateAlong) - (GATE_HALF + 4.5)) <= TOWER_HALF;
                if (gateTower) tower = true;
                if (gate) tower = false;
                if (!tower && Math.abs(offset) > 3.5) continue;

                // base height for this stretch of wall: the ground at the ring, per ~6-block segment, so the wall steps cleanly
                int seg = (int) Math.floor(along / 6);
                double sTheta = (seg + 0.5) * 6 / r;
                double rs = c.r(sTheta);
                int base = Integer.MIN_VALUE;
                boolean wet = false;
                for (double o : new double[]{-3, 0, 3}) {
                    int sx = (int) Math.floor(c.cx() + Math.cos(sTheta) * (rs + o)), sz = (int) Math.floor(c.cz() + Math.sin(sTheta) * (rs + o));
                    Earth.Surface s = e.at(sx, sz);
                    if (s.ocean() || s.waterDepth() > 0) wet = true;
                    base = Math.max(base, s.solidTop());
                }
                if (wet) continue;   // walls stop at the water's edge
                int yb = base + 1;   // first block above the ground at the ring
                Earth.Surface here = e.at(x, z);
                int colGround = here.solidTop();
                int w = (int) Math.round(3 - offset);   // 0 = outer face … 6 = inner face
                int slot = Math.floorMod((int) Math.floor(along), 1 << 20);

                // foundation down to this column's ground (never floats), and up through the foundation layers
                int from = Math.min(colGround + 1, yb) - (tower ? 0 : 0);
                int top = yb + CityWalls.WALK_Y + CityWalls.PARAPET_H + (tower ? CityWalls.TOWER_EXTRA : 0);
                for (int y = from; y <= top; y++) {
                    int rel = y - yb;
                    BlockState b = tower ? towerBlock(st, rel, offset, towerAlong, slot, y) : wallBlock(st, rel, w, slot, gate, gateAlong);
                    if (b == null) continue;
                    p.set(x, y, z);
                    level.setBlock(p, b, 2);
                    any = true;
                }
            }
        }
        return any;
    }

    private static double angleDiff(double a, double b) {
        double d = a - b;
        while (d > Math.PI) d -= 2 * Math.PI;
        while (d < -Math.PI) d += 2 * Math.PI;
        return d;
    }

    /** Block for a plain wall column at height rel above the wall base, cross-section index w (0 outer … 6 inner). */
    private static BlockState wallBlock(CityWalls.Style st, int rel, int w, int slot, boolean gate, double gateAlong) {
        BlockState wall = (slot % 11 == 3 ? st.cracked() : st.wall()).defaultBlockState();
        if (rel < 0) return wall;                                                 // foundation into the hillside
        if (gate && rel <= CityWalls.HALL_Y0 + 3) {                                // the gate passage: 6 tall, open
            return rel == CityWalls.HALL_Y0 + 3 && Math.abs(gateAlong) > 1.5 ? st.accent().defaultBlockState() : Blocks.AIR.defaultBlockState();
        }
        if (rel < CityWalls.HALL_Y0) return wall;                                  // foundation layers
        if (rel < CityWalls.CEIL_Y) {                                              // the hallway
            if (w == 0) {
                if (rel == CityWalls.HALL_Y0 + 2 && slot % 5 == 0) return Blocks.IRON_BARS.defaultBlockState();   // arrow slits
                return slot % 6 == 0 && rel == CityWalls.HALL_Y0 + 1 ? st.accent().defaultBlockState() : wall;
            }
            if (w == 6) return rel == CityWalls.HALL_Y0 + 2 && slot % 9 == 4 ? Blocks.IRON_BARS.defaultBlockState() : wall;
            if ((w == 1 || w == 5) && rel == CityWalls.HALL_Y0 && slot % 8 == 0) return st.light().defaultBlockState();
            if ((w == 1 || w == 5) && slot % 8 == 4) return st.pillar().defaultBlockState();
            return Blocks.AIR.defaultBlockState();
        }
        if (rel == CityWalls.CEIL_Y) return st.hallFloor().defaultBlockState();   // hallway ceiling = walkway floor
        if (rel == CityWalls.WALK_Y) {                                             // parapets
            if (w == 0 || w == 6) return st.parapet().defaultBlockState();
            if (w == 3 && slot % 12 == 6) return st.light().defaultBlockState();
            return Blocks.AIR.defaultBlockState();
        }
        if (rel == CityWalls.WALK_Y + 1) return w == 0 && slot % 2 == 0 ? st.parapet().defaultBlockState() : Blocks.AIR.defaultBlockState();   // crenels
        return null;
    }

    /** Block for a tower column: 9x9 shell with a room at hall level, a room at walkway level and a roof terrace. */
    private static BlockState towerBlock(CityWalls.Style st, int rel, double offset, double along, int slot, int y) {
        boolean shell = Math.abs(offset) > 3.5 || Math.abs(along) > 3.5;
        boolean corner = Math.abs(offset) > 3.5 && Math.abs(along) > 3.5;
        BlockState wall = st.wall().defaultBlockState();
        int roof = CityWalls.WALK_Y + CityWalls.PARAPET_H + CityWalls.TOWER_EXTRA;
        if (rel < CityWalls.HALL_Y0) return wall;
        if (rel == roof) return shell ? (corner || (slot + (int) Math.floor(offset)) % 2 == 0 ? st.parapet().defaultBlockState() : null) : null;   // crenellated top
        if (rel == roof - 1) return shell ? st.parapet().defaultBlockState() : (Math.abs(offset) < 1 && Math.abs(along) < 1 ? st.light().defaultBlockState() : st.roof().defaultBlockState());
        if (shell) {
            // doorways where the hallway and the walkway run through
            boolean hallDoor = rel >= CityWalls.HALL_Y0 && rel < CityWalls.CEIL_Y && Math.abs(offset) <= 2.5 && Math.abs(along) > 3.5;
            boolean walkDoor = rel > CityWalls.CEIL_Y && rel <= CityWalls.WALK_Y + 2 && Math.abs(offset) <= 2.5 && Math.abs(along) > 3.5;
            if (hallDoor || walkDoor) return Blocks.AIR.defaultBlockState();
            boolean window = rel == CityWalls.WALK_Y + 5 && (Math.abs(offset) < 0.6 || Math.abs(along) < 0.6);
            if (window) return Blocks.IRON_BARS.defaultBlockState();
            if (corner) return st.pillar().defaultBlockState();
            return (rel == CityWalls.CEIL_Y || rel == CityWalls.WALK_Y + 4) ? st.accent().defaultBlockState() : wall;
        }
        // interior
        if (rel == CityWalls.CEIL_Y || rel == CityWalls.WALK_Y + 4) return st.hallFloor().defaultBlockState();   // floors
        if (rel == CityWalls.HALL_Y0 && Math.abs(offset) < 1 && Math.abs(along) < 1) return st.light().defaultBlockState();
        // ladder up the middle of the room from the walkway level to the roof
        if (rel > CityWalls.WALK_Y + 4 && rel < roof - 1 && Math.abs(offset) < 0.6 && Math.abs(along) < 0.6) return st.pillar().defaultBlockState();
        if (rel > CityWalls.WALK_Y + 4 && rel <= roof - 1 && Math.abs(offset) < 0.6 && along >= 0.6 && along < 1.6)
            return Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH);
        if (rel == roof - 1 && Math.abs(offset) < 0.6 && along >= 0.6 && along < 1.6) return Blocks.AIR.defaultBlockState();   // hatch
        return Blocks.AIR.defaultBlockState();
    }

    // ---- the keep with the obelisk on top ---------------------------------------------------

    private boolean keep(WorldGenLevel level, Earth e, CityWalls.City c, int x0, int z0) {
        int cx = (int) Math.floor(c.cx()), cz = (int) Math.floor(c.cz());
        if (cx + 8 < x0 || cx - 8 > x0 + 15 || cz + 8 < z0 || cz - 8 > z0 + 15) return false;
        Earth.Surface centre = e.at(cx, cz);
        if (centre.ocean() || centre.waterDepth() > 0) return false;
        int yb = centre.solidTop() + 1;
        CityWalls.Style st = c.style();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        boolean any = false;
        int tier1 = 12, tier2 = 9;   // heights: 15x15 then 11x11, then the obelisk platform
        for (int dx = -7; dx <= 7; dx++) {
            for (int dz = -7; dz <= 7; dz++) {
                int x = cx + dx, z = cz + dz;
                if (x < x0 || x > x0 + 15 || z < z0 || z > z0 + 15) continue;
                int ground = e.at(x, z).solidTop();
                int ring = Math.max(Math.abs(dx), Math.abs(dz));
                for (int y = Math.min(ground + 1, yb); y < yb + tier1 + tier2; y++) {
                    int rel = y - yb;
                    BlockState b;
                    if (rel < 0) b = st.wall().defaultBlockState();
                    else if (rel < tier1) {
                        boolean shell = ring == 7;
                        if (shell) b = rel == 0 || rel == tier1 - 1 ? st.accent().defaultBlockState()
                                : (ring == 7 && (Math.abs(dx) == 7 && Math.abs(dz) == 7) ? st.pillar().defaultBlockState()
                                : (rel == 5 && (dx == 0 || dz == 0) ? Blocks.IRON_BARS.defaultBlockState()
                                : (rel <= 3 && Math.abs(dx) <= 1 && dz == 7 ? Blocks.AIR.defaultBlockState() : st.wall().defaultBlockState())));   // door on the south side
                        else b = rel == 0 || rel == 6 || rel == tier1 - 1 ? (dx == 1 && dz == 0 && rel > 0 ? Blocks.AIR.defaultBlockState() : st.hallFloor().defaultBlockState())
                                : (dx == 0 && dz == 0 ? st.pillar().defaultBlockState()
                                : (dx == 1 && dz == 0 ? Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.EAST)
                                : ((rel == 1 || rel == 7) && (Math.abs(dx) == 5 && Math.abs(dz) == 5) ? st.light().defaultBlockState() : Blocks.AIR.defaultBlockState())));
                    } else if (ring <= 5) {
                        int r2 = rel - tier1;
                        boolean shell = ring == 5;
                        if (shell) b = r2 == tier2 - 1 ? ((dx + dz) % 2 == 0 ? st.parapet().defaultBlockState() : Blocks.AIR.defaultBlockState())
                                : (Math.abs(dx) == 5 && Math.abs(dz) == 5 ? st.pillar().defaultBlockState() : (r2 == 4 && (dx == 0 || dz == 0) ? Blocks.IRON_BARS.defaultBlockState() : st.wall().defaultBlockState()));
                        else b = r2 == tier2 - 2 ? (dx == 1 && dz == 0 ? Blocks.AIR.defaultBlockState() : st.hallFloor().defaultBlockState())
                                : r2 == tier2 - 1 ? Blocks.AIR.defaultBlockState()
                                : (dx == 0 && dz == 0 ? st.pillar().defaultBlockState()
                                : (dx == 1 && dz == 0 ? Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.EAST) : Blocks.AIR.defaultBlockState()));
                    } else b = ring == 7 && rel == tier1 ? ((dx + dz) % 2 == 0 ? st.parapet().defaultBlockState() : null) : null;   // crenels on the lower terrace
                    if (b == null) continue;
                    p.set(x, y, z);
                    level.setBlock(p, b, 2);
                    any = true;
                }
            }
        }
        // the obelisk on the top platform: placed by the chunk that holds the centre (it reaches 4 blocks out, within the 11x11 top)
        if (cx >= x0 && cx <= x0 + 15 && cz >= z0 && cz <= z0 + 15) {
            Obelisk.build(level, new BlockPos(cx, yb + tier1 + tier2 - 2, cz));
            any = true;
        }
        return any;
    }
}
