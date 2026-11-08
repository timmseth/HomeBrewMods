package com.seth.worldbound.client.horizon;

import com.seth.worldbound.Earth;
import com.seth.worldbound.WorldBoundConfig;

import java.nio.ByteBuffer;
import java.nio.file.Path;

/** Headless: mesh a few tiles around Tokyo / Fuji from the run dir's data and sanity-check the geometry. */
public final class HorizonMeshTest {
    public static void main(String[] args) {
        Path run = Path.of(args.length > 0 ? args[0] : "run");
        WorldBoundConfig cfg = WorldBoundConfig.load(run.resolve("config/worldbound.json"));
        cfg.scale_m_per_block = 3; cfg.origin_lat = 35.3606; cfg.origin_lon = 138.7274; // Fuji at 0,0
        Earth.init(cfg, run);
        Earth e = Earth.get();
        HorizonMesher m = new HorizonMesher(new HorizonColors(null));
        int checks = 0;
        for (int level = 0; level < 9; level++) {
            long t0 = System.nanoTime();
            HorizonMesher.Mesh mesh = m.build(e, level, 0, 0);
            long ms = (System.nanoTime() - t0) / 1_000_000;
            ByteBuffer b = mesh.data();
            if (b.remaining() != mesh.quads() * 64) throw new AssertionError("size " + b.remaining() + " vs quads " + mesh.quads());
            if (mesh.quads() < 1024) throw new AssertionError("too few quads " + mesh.quads());
            float maxY = -1e9f, minY = 1e9f; int T = HorizonMesher.tileBlocks(level);
            for (int i = 0; i < mesh.quads() * 4; i++) {
                float x = b.getFloat(i * 16), y = b.getFloat(i * 16 + 4), z = b.getFloat(i * 16 + 8);
                if (x < 0 || x > T || z < 0 || z > T) throw new AssertionError("vertex outside tile " + x + "," + z);
                maxY = Math.max(maxY, y); minY = Math.min(minY, y);
                if (y < e.curve.minY - 2 * HorizonMesher.spacing(level) || y > e.curve.maxY + 2) throw new AssertionError("y " + y);
            }
            if (maxY > mesh.maxY() || minY < mesh.minY()) throw new AssertionError("bounds " + minY + ".." + maxY + " vs " + mesh.minY() + ".." + mesh.maxY());
            System.out.printf("level %d (%d-block columns, %d-block tile): %d quads, y %.0f..%.0f, %d ms%n", level, HorizonMesher.spacing(level), T, mesh.quads(), minY, maxY, ms);
            mesh.free();
            checks++;
        }
        // Fuji summit column at the origin should be high and snowy-white on top.
        Earth.Surface s = e.lodAt(0, 0, 2);
        HorizonColors.Look look = new HorizonColors(null).look(e, s, 0, 0, 2, false);
        System.out.printf("Fuji: top y %d colour %06x (elev %.0f m)%n", look.topY(), look.top(), s.elevM());
        if (look.topY() < 400) throw new AssertionError("Fuji too low: " + look.topY());
        System.out.println("HorizonMeshTest: " + checks + " tiles ok");
    }
}
