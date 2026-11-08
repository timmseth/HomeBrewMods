package com.seth.worldbound.worldgen;

import com.seth.worldbound.Earth;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.Strategy;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Fast terrain fill for Earth worlds, replacing the per-block noise fill (which in a 4,064-block-tall world
 * spends most of its time writing stone one block at a time).
 *
 * Every column is known exactly (Earth.at): solid up to {@code solidTop}, then river/lake water or sea up to
 * sea level, air above. Whole 16³ sections that are solid stone, deepslate or water are written as single-value
 * sections; only the band around the surface (and the cave band) is written block by block. The surface rules
 * then only need to run over the top {@link #RULE_BAND} blocks (see SurfaceSystemMixin).
 *
 * Caves: cheese caves and tunnels from 3-D noise, 6 to ~260 blocks below the local ground, never under water.
 */
public final class EarthFill {
    private EarthFill() {}

    /** Surface rules run over this many blocks below the lowest ground in the chunk. */
    public static final int RULE_BAND = 80;
    /** Deepslate below this y; mixed between it and +8 (matches the shifted vanilla rule). */
    public static final int DEEPSLATE_Y = -763;
    private static final int CAVE_TOP = 6, CAVE_BOTTOM = 260;

    private static final BlockState AIR = Blocks.AIR.defaultBlockState(), STONE = Blocks.STONE.defaultBlockState(),
            DEEPSLATE = Blocks.DEEPSLATE.defaultBlockState(), WATER = Blocks.WATER.defaultBlockState(),
            BEDROCK = Blocks.BEDROCK.defaultBlockState();
    private static final Strategy<BlockState> STRATEGY = Strategy.createForBlockStates(Block.BLOCK_STATE_REGISTRY);

    /** Lowest ground y in a chunk, cached per thread for the surface-system hook. */
    private static final ThreadLocal<long[]> FLOOR = ThreadLocal.withInitial(() -> new long[]{Long.MIN_VALUE, 0});

    public static int ruleFloor(ChunkAccess chunk) {
        ChunkPos cp = chunk.getPos();
        long key = cp.pack();
        long[] c = FLOOR.get();
        if (c[0] == key) return (int) c[1];
        Earth e = Earth.get();
        int min = Integer.MAX_VALUE;
        int bx = cp.getMinBlockX(), bz = cp.getMinBlockZ();
        for (int i = 0; i < 256; i++) min = Math.min(min, e.at(bx + (i & 15), bz + (i >> 4)).solidTop());
        int floor = Math.max(chunk.getMinY(), min - RULE_BAND);
        c[0] = key;
        c[1] = floor;
        return floor;
    }

    public static boolean isEarthChunk(ChunkAccess chunk) {
        return chunk.getMinY() == Earth.get().curve.minY && chunk.getHeight() == Earth.get().curve.maxY - Earth.get().curve.minY + 1;
    }

    public static ChunkAccess fill(ChunkAccess chunk) {
        return fill(chunk, null);
    }

    /**
     * Structures that ask for terrain adaptation (villages, trail ruins, ancient cities…) get vanilla's
     * "beardifier" applied in the band around their pieces: foundations under them, ground cut away above.
     */
    public static ChunkAccess fill(ChunkAccess chunk, net.minecraft.world.level.StructureManager structures) {
        Earth e = Earth.get();
        int sea = e.curve.seaLevelY, minY = chunk.getMinY();
        ChunkPos cp = chunk.getPos();
        int bx = cp.getMinBlockX(), bz = cp.getMinBlockZ();
        int wbx = e.transform.wrapX(bx); // caves repeat with the world (wrap-around)
        boolean caves = e.config.caves;

        int[] solid = new int[256], water = new int[256], caveLo = new int[256], caveHi = new int[256];
        int minSolid = Integer.MAX_VALUE, maxTop = Integer.MIN_VALUE, minCave = Integer.MAX_VALUE, maxWaterBottom = Integer.MIN_VALUE, minWaterTop = Integer.MAX_VALUE;
        for (int i = 0; i < 256; i++) {
            Earth.Surface s = e.at(bx + (i & 15), bz + (i >> 4));
            int st = s.solidTop();
            int wt = s.waterDepth() > 0 ? s.surfaceY() : s.ocean() ? sea : st;
            solid[i] = st;
            water[i] = wt;
            boolean wet = wt > st;
            caveHi[i] = caves && !wet ? st - CAVE_TOP : Integer.MIN_VALUE;
            caveLo[i] = Math.max(minY + 8, st - CAVE_BOTTOM);
            minSolid = Math.min(minSolid, st);
            maxTop = Math.max(maxTop, Math.max(st, wt));
            if (caveHi[i] > caveLo[i]) minCave = Math.min(minCave, caveLo[i]);
            maxWaterBottom = Math.max(maxWaterBottom, st);
            minWaterTop = Math.min(minWaterTop, wt);
        }
        // Terrain adaptation band (only chunks touched by such a structure pay for it).
        net.minecraft.world.level.levelgen.Beardifier beard = null;
        int bLo = Integer.MAX_VALUE, bHi = Integer.MIN_VALUE;
        if (structures != null) {
            var starts = structures.startsForStructure(cp, st -> st.terrainAdaptation() != net.minecraft.world.level.levelgen.structure.TerrainAdjustment.NONE);
            int cx0 = bx - 12, cz0 = bz - 12, cx1 = bx + 27, cz1 = bz + 27;
            for (var start : starts) {
                for (var piece : start.getPieces()) {
                    var b = piece.getBoundingBox();
                    if (b.maxX() < cx0 || b.minX() > cx1 || b.maxZ() < cz0 || b.minZ() > cz1) continue;
                    bLo = Math.min(bLo, b.minY() - 14);
                    bHi = Math.max(bHi, b.maxY() + 14);
                }
            }
            if (bLo <= bHi) {
                beard = net.minecraft.world.level.levelgen.Beardifier.forStructuresInChunk(structures, cp);
                maxTop = Math.max(maxTop, bHi);
            }
        }
        int ruleFloor = Math.max(minY, minSolid - RULE_BAND);
        int perBlockFloor = Math.min(ruleFloor, minCave);
        int maxCaveHi = Integer.MIN_VALUE;
        for (int i = 0; i < 256; i++) if (caveHi[i] > caveLo[i]) maxCaveHi = Math.max(maxCaveHi, caveHi[i]);
        CaveGrid grid = maxCaveHi > Integer.MIN_VALUE ? new CaveGrid(wbx, bz, minCave, maxCaveHi) : null;

        Heightmap ocean = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap surface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        LevelChunkSection[] sections = chunk.getSections();

        for (int si = 0; si < sections.length; si++) {
            int sy = minY + si * 16, ey = sy + 15;
            if (sy > maxTop) break; // air from here up
            LevelChunkSection sec = sections[si];
            BlockState bulk = null;
            boolean inBand = beard != null && ey >= bLo && sy <= bHi;
            if (inBand) {
                // per-block below
            } else if (sy > minY + 4 && ey < perBlockFloor) {
                if (ey < DEEPSLATE_Y) bulk = DEEPSLATE;
                else if (sy >= DEEPSLATE_Y + 8) bulk = STONE;
            } else if (sy > maxWaterBottom && ey <= minWaterTop) {
                bulk = WATER;
            }
            if (bulk != null) {
                LevelChunkSection n = new LevelChunkSection(new PalettedContainer<>(bulk, STRATEGY), sec.getBiomes());
                n.recalcBlockCounts();
                sections[si] = n;
                continue;
            }
            if (!inBand && ey <= minSolid) {
                // Under the ground in every column: start solid and write only what differs (caves, the
                // deepslate transition, bedrock) instead of setting all 4,096 blocks one by one.
                BlockState base = ey < DEEPSLATE_Y && ey < ruleFloor ? DEEPSLATE : STONE;
                LevelChunkSection n = new LevelChunkSection(new PalettedContainer<>(base, STRATEGY), sec.getBiomes());
                n.recalcBlockCounts();
                sections[si] = n;
                for (int i = 0; i < 256; i++) {
                    int lx = i & 15, lz = i >> 4, st = solid[i];
                    for (int y = sy; y <= ey; y++) {
                        BlockState b = solidBlock(bx + lx, y, bz + lz, lx, lz, i, minY, ruleFloor, caveLo, caveHi, grid);
                        if (b != base) n.setBlockState(lx, y & 15, lz, b, false);
                    }
                    if (st >= sy && st <= ey) { // this column's ground top is in here
                        ocean.update(lx, st, lz, STONE);
                        surface.update(lx, st, lz, STONE);
                    }
                }
                continue;
            }
            for (int i = 0; i < 256; i++) {
                int lx = i & 15, lz = i >> 4, st = solid[i], wt = water[i];
                int top = Math.min(ey, Math.max(Math.max(st, wt), inBand ? bHi : Integer.MIN_VALUE));
                for (int y = sy; y <= top; y++) {
                    BlockState b;
                    boolean band = inBand && y >= bLo && y <= bHi;
                    boolean solidHere = y <= st;
                    if (band) {
                        double d = Math.max(-1, Math.min(1, (st - y + 0.5) / 32.0))
                                + beard.compute(new net.minecraft.world.level.levelgen.DensityFunction.SinglePointContext(bx + lx, y, bz + lz));
                        solidHere = d > 0;
                    }
                    if (solidHere) {
                        b = band ? (y <= minY + 4 && (y == minY || hash(bx + lx, y, bz + lz) % 5 >= y - minY) ? BEDROCK : STONE)
                                : solidBlock(bx + lx, y, bz + lz, lx, lz, i, minY, ruleFloor, caveLo, caveHi, grid);
                    } else if (wt > st && y <= wt) {
                        b = WATER;
                    } else {
                        b = AIR;
                    }
                    if (b == AIR) continue;
                    sec.setBlockState(lx, y & 15, lz, b, false);
                    if (y >= st - 1 || band) {
                        ocean.update(lx, y, lz, b);
                        surface.update(lx, y, lz, b);
                    }
                }
            }
        }
        return chunk;
    }

    /** What a solid (below-ground) position holds: bedrock at the floor, cave air, deepslate deep down, else stone. */
    private static BlockState solidBlock(int x, int y, int z, int lx, int lz, int i, int minY, int ruleFloor,
                                         int[] caveLo, int[] caveHi, CaveGrid grid) {
        if (y <= minY + 4 && (y == minY || hash(x, y, z) % 5 >= y - minY)) return BEDROCK;
        if (grid != null && y >= caveLo[i] && y <= caveHi[i] && grid.cave(lx, y, lz, y - caveLo[i], caveHi[i] - y)) return AIR;
        if (y < ruleFloor && (y < DEEPSLATE_Y || (y < DEEPSLATE_Y + 8 && hash(x, y, z) % 8 >= y - DEEPSLATE_Y))) return DEEPSLATE;
        return STONE;
    }

    /**
     * Cave noise for one chunk on a 4-block lattice (5x5 columns, every 4 blocks of height through the cave band),
     * interpolated per block. The noises vary over 20-96 blocks, so this matches per-block sampling closely at a
     * fraction of the cost (a chunk's cave band is ~70,000 blocks; the lattice is ~2,000 points).
     */
    static final class CaveGrid {
        private static final int STEP = 4, NX = 5;
        private final int y0, ny;
        private final float[] cheese, ta, tb;

        CaveGrid(int wbx, int bz, int yLo, int yHi) {
            y0 = Math.floorDiv(yLo, STEP) * STEP;
            ny = (Math.floorDiv(yHi, STEP) * STEP - y0) / STEP + 2;
            cheese = new float[NX * NX * ny];
            ta = new float[cheese.length];
            tb = new float[cheese.length];
            for (int iy = 0; iy < ny; iy++) {
                int y = y0 + iy * STEP;
                for (int iz = 0; iz < NX; iz++) {
                    int z = bz + iz * STEP;
                    for (int ix = 0; ix < NX; ix++) {
                        int x = wbx + ix * STEP, k = (iy * NX + iz) * NX + ix;
                        cheese[k] = (float) (noise(x / 96.0, y / 40.0, z / 96.0, 11) * 0.65 + noise(x / 40.0, y / 20.0, z / 40.0, 12) * 0.35);
                        ta[k] = (float) noise(x / 72.0, y / 36.0, z / 72.0, 21);
                        tb[k] = (float) noise(x / 72.0, y / 36.0, z / 72.0, 22);
                    }
                }
            }
        }

        /** distBottom/distTop: blocks from the cave band's ends, used to fade caves out at the edges. */
        boolean cave(int lx, int y, int lz, int distBottom, int distTop) {
            int ix = lx >> 2, iz = lz >> 2, ry = y - y0, iy = ry >> 2;
            float fx = (lx & 3) * 0.25f, fz = (lz & 3) * 0.25f, fy = (ry & 3) * 0.25f;
            int k = (iy * NX + iz) * NX + ix;
            double fade = Math.min(1, Math.min(distBottom, distTop) / 24.0);
            // Cheese: big blobby chambers.
            if (lerp3(cheese, k, fx, fy, fz) > 0.80 - 0.05 * fade + (1 - fade) * 0.3) return true;
            // Spaghetti: where two noises both cross zero, a winding tunnel.
            double a = lerp3(ta, k, fx, fy, fz), b = lerp3(tb, k, fx, fy, fz);
            return a * a + b * b < 0.0035 * fade;
        }

        private static float lerp3(float[] v, int k, float fx, float fy, float fz) {
            int kz = k + NX, ky = k + NX * NX, kyz = ky + NX;
            float x00 = v[k] + (v[k + 1] - v[k]) * fx, x01 = v[kz] + (v[kz + 1] - v[kz]) * fx;
            float x10 = v[ky] + (v[ky + 1] - v[ky]) * fx, x11 = v[kyz] + (v[kyz + 1] - v[kyz]) * fx;
            float y0v = x00 + (x01 - x00) * fz, y1v = x10 + (x11 - x10) * fz;
            return y0v + (y1v - y0v) * fy;
        }
    }

    // ---- caves ---------------------------------------------------------------------------------

    private static int hash(int x, int y, int z) {
        int h = x * 0x27d4eb2d ^ y * 0x165667b1 ^ z * 0x1b873593;
        h ^= h >>> 15;
        h *= 0x85ebca6b;
        h ^= h >>> 13;
        return h & 0x7fffffff;
    }

    /** Smooth 3-D value noise in [-1, 1]. */
    static double noise(double x, double y, double z, int seed) {
        int x0 = (int) Math.floor(x), y0 = (int) Math.floor(y), z0 = (int) Math.floor(z);
        double fx = x - x0, fy = y - y0, fz = z - z0;
        fx = fx * fx * (3 - 2 * fx);
        fy = fy * fy * (3 - 2 * fy);
        fz = fz * fz * (3 - 2 * fz);
        double c000 = v(x0, y0, z0, seed), c100 = v(x0 + 1, y0, z0, seed), c010 = v(x0, y0 + 1, z0, seed), c110 = v(x0 + 1, y0 + 1, z0, seed);
        double c001 = v(x0, y0, z0 + 1, seed), c101 = v(x0 + 1, y0, z0 + 1, seed), c011 = v(x0, y0 + 1, z0 + 1, seed), c111 = v(x0 + 1, y0 + 1, z0 + 1, seed);
        double x00 = c000 + (c100 - c000) * fx, x10 = c010 + (c110 - c010) * fx, x01 = c001 + (c101 - c001) * fx, x11 = c011 + (c111 - c011) * fx;
        double y0v = x00 + (x10 - x00) * fy, y1v = x01 + (x11 - x01) * fy;
        return y0v + (y1v - y0v) * fz;
    }

    private static double v(int x, int y, int z, int seed) {
        return (hash(x + seed * 7919, y - seed * 104729, z + seed * 1299709) & 0xFFFF) / 32767.5 - 1.0;
    }
}
