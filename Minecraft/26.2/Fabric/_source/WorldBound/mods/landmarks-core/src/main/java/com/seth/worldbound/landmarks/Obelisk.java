package com.seth.worldbound.landmarks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The landmark obelisk: a 9-wide rounded platform of polished blackstone with four soul-lantern posts, an
 * obsidian shaft 5 wide, glowing crying-obsidian seams up each face, a tapered crown and an end-rod spire.
 * 13 blocks tall above the platform. {@code c} is the centre of the platform's top layer.
 */
public final class Obelisk {
    public static final int HEIGHT = 13, RADIUS = 4;

    public static void build(LevelAccessor level, BlockPos c) {
        int cx = c.getX(), cy = c.getY(), cz = c.getZ();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        // clear the space (trees, snow, boulders)
        for (int dx = -5; dx <= 5; dx++) for (int dz = -5; dz <= 5; dz++) {
            if (dx * dx + dz * dz > 30) continue;
            for (int y = 1; y <= HEIGHT + 3; y++) set(level, p.set(cx + dx, cy + y, cz + dz), Blocks.AIR.defaultBlockState());
        }
        // platform, with a foundation down to the ground so it never floats
        for (int dx = -RADIUS; dx <= RADIUS; dx++) for (int dz = -RADIUS; dz <= RADIUS; dz++) {
            int d2 = dx * dx + dz * dz;
            if (d2 > 20) continue;
            BlockState top = d2 >= 13 ? Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
                    : (Math.abs(dx) + Math.abs(dz)) % 2 == 0 ? Blocks.POLISHED_BLACKSTONE.defaultBlockState() : Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState();
            set(level, p.set(cx + dx, cy, cz + dz), top);
            for (int y = cy - 1; y > cy - 40; y--) {
                BlockState below = level.getBlockState(p.set(cx + dx, y, cz + dz));
                if (below.isSolid() && y < cy - 2) break;
                set(level, p, Blocks.POLISHED_BLACKSTONE.defaultBlockState());
            }
        }
        // lantern posts
        for (int[] q : new int[][]{{3, 3}, {3, -3}, {-3, 3}, {-3, -3}}) {
            set(level, p.set(cx + q[0], cy + 1, cz + q[1]), Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState());
            set(level, p.set(cx + q[0], cy + 2, cz + q[1]), Blocks.SOUL_LANTERN.defaultBlockState());
        }
        // shaft: 5 wide, 9 tall, seams of crying obsidian up the middle of each face, gilded band
        for (int y = 1; y <= 9; y++) {
            for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
                if (dx * dx + dz * dz > 6) continue;
                boolean face = (Math.abs(dx) == 2 && dz == 0) || (Math.abs(dz) == 2 && dx == 0);
                BlockState b = face && y >= 2 && y <= 8 ? Blocks.CRYING_OBSIDIAN.defaultBlockState()
                        : y == 1 ? Blocks.GILDED_BLACKSTONE.defaultBlockState() : Blocks.OBSIDIAN.defaultBlockState();
                set(level, p.set(cx + dx, cy + y, cz + dz), b);
            }
        }
        // crown: 3 wide for 2, then the cap and spire
        for (int y = 10; y <= 11; y++)
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
                set(level, p.set(cx + dx, cy + y, cz + dz), (dx == 0 || dz == 0) && y == 11 ? Blocks.CRYING_OBSIDIAN.defaultBlockState() : Blocks.OBSIDIAN.defaultBlockState());
        set(level, p.set(cx, cy + 12, cz), Blocks.CRYING_OBSIDIAN.defaultBlockState());
        set(level, p.set(cx, cy + 13, cz), Blocks.END_ROD.defaultBlockState());
        // the lodestone "key" at the front, where you click to travel
        set(level, p.set(cx, cy + 1, cz + 3), Blocks.LODESTONE.defaultBlockState());
    }

    private static void set(LevelAccessor level, BlockPos p, BlockState s) {
        level.setBlock(p, s, 2);
    }

    /** Is this block part of the obelisk centred at c? */
    static boolean contains(BlockPos c, BlockPos q) {
        int dx = q.getX() - c.getX(), dz = q.getZ() - c.getZ(), dy = q.getY() - c.getY();
        return dx * dx + dz * dz <= 20 && dy >= 0 && dy <= HEIGHT;
    }
}
