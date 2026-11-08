package com.seth.worldbound.worldgen;

import com.seth.worldbound.Earth;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;

import java.util.List;

/**
 * Makes vanilla (and modded) structures fit real terrain, checked against the real ground under their footprint:
 * <ul>
 *   <li>Underground structures (trial chambers, mineshafts, strongholds, ancient cities…) are moved down until
 *       fully buried — vanilla's fixed heights put them in mid-air beside real mountains and canyons.</li>
 *   <li>Surface structures (villages, outposts, temples, mansions…) only spawn on reasonably flat ground, and are
 *       set onto the ground if their own height rule left them floating or buried.</li>
 *   <li>Water structures (shipwrecks, ocean ruins) and everything else are left alone.</li>
 * </ul>
 */
public final class EarthStructures {
    private EarthStructures() {}

    public static boolean isEarth(LevelHeightAccessor h) {
        return Earth.earthWorld() && h.getMinY() == Earth.get().curve.minY && h.getHeight() == Earth.get().curve.maxY - Earth.get().curve.minY + 1;
    }

    public static StructureStart adjust(StructureStart start, LevelHeightAccessor h) {
        if (!start.isValid() || !isEarth(h)) return start;
        Structure st = start.getStructure();
        GenerationStep.Decoration step = st.step();
        List<StructurePiece> pieces = start.getPieces();
        BoundingBox bb = new PiecesContainer(pieces).calculateBoundingBox();
        Earth e = Earth.get();

        int span = Math.max(bb.getXSpan(), bb.getZSpan());
        int stepXZ = Math.max(4, Math.min(16, span / 8));
        int gMin = Integer.MAX_VALUE, gMax = Integer.MIN_VALUE, wet = 0, n = 0;
        for (int x = bb.minX(); x <= bb.maxX(); x += stepXZ) {
            for (int z = bb.minZ(); z <= bb.maxZ(); z += stepXZ) {
                Earth.Surface s = e.at(x, z);
                int g = s.solidTop();
                gMin = Math.min(gMin, g);
                gMax = Math.max(gMax, g);
                if (s.ocean() || s.waterDepth() > 0) wet++;
                n++;
            }
        }
        int dy = 0;
        boolean underground = step == GenerationStep.Decoration.UNDERGROUND_STRUCTURES
                || step == GenerationStep.Decoration.UNDERGROUND_DECORATION || step == GenerationStep.Decoration.STRONGHOLDS;
        if (bb.maxY() < gMin - 2) {
            // Already fully buried (strongholds, mineshafts, buried treasure…): keep it, but within reach of a
            // pickaxe: vanilla's fixed depths can put these 1,000+ blocks under Earth's ground.
            if (bb.maxY() < gMin - 160) dy = (gMin - 40) - bb.maxY();
        } else if (underground) {
            // Underground by design but poking out (fixed heights beside real mountains/canyons): bury it.
            dy = (gMin - 6) - bb.maxY();
        } else if (step == GenerationStep.Decoration.SURFACE_STRUCTURES) {
            if (wet * 2 > n) return start; // shipwrecks, ocean ruins…
            int tolerance = Math.max(4, Math.min(14, 3 + span / 10));
            if (gMax - gMin > tolerance) return StructureStart.INVALID_START; // mountainsides: no villages
            if (bb.minY() > gMax + 3) dy = gMin - bb.minY(); // floating: set it on the ground
        } else {
            return start;
        }
        if (bb.minY() + dy < h.getMinY() + 6) return StructureStart.INVALID_START;
        if (dy == 0) return start;
        for (StructurePiece p : pieces) p.move(0, dy, 0);
        return new StructureStart(st, start.getChunkPos(), start.getReferences(), new PiecesContainer(pieces));
    }
}
