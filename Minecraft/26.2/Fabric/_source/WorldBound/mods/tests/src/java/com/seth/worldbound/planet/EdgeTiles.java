package com.seth.worldbound.planet;

import com.seth.worldbound.data.Tiling;

/** Generates every layer at the poles, the antimeridian and random places; any exception fails. */
public final class EdgeTiles {
    public static void main(String[] a) {
        Planet p = Planet.get(Long.parseLong(a[0]), null);
        double[][] pts = {{89.95, 0}, {-89.95, 120}, {0, 179.99}, {0, -179.99}, {45, 180}, {-60, -179.5}, {70.5, 33.3}};
        java.util.SplittableRandom r = new java.util.SplittableRandom(7);
        int[][] layers = {{Tiling.LAYER_ELEV, 2}, {Tiling.LAYER_ELEV, 5}, {Tiling.LAYER_ELEV, 7}, {Tiling.LAYER_ELEV, 10}, {Tiling.LAYER_CLIMATE, 6},
                {Tiling.LAYER_VEG, 7}, {Tiling.LAYER_TREECOVER, 7}, {Tiling.LAYER_LAKE, 10}, {Tiling.LAYER_LAKEDEPTH, 7}, {Tiling.LAYER_CLIMATE, 2}, {Tiling.LAYER_LAKE, 2}};
        int n = 0;
        for (int q = 0; q < pts.length + 4; q++) {
            double lat = q < pts.length ? pts[q][0] : r.nextDouble() * 160 - 80, lon = q < pts.length ? pts[q][1] : r.nextDouble() * 360 - 180;
            for (int[] l : layers) {
                int z = l[1];
                int tx = Math.floorMod((int) Math.floor(Tiling.globalPx(z, lon) / Tiling.TILE), Tiling.tilesAcross(z));
                int ty = Math.max(0, Math.min(Tiling.tilesDown(z) - 1, (int) Math.floor(Tiling.globalPy(z, lat) / Tiling.TILE)));
                var t = p.source.load(l[0], z, tx, ty);
                if (t == null) throw new IllegalStateException("null tile " + l[0] + " z" + z);
                n++;
            }
            p.source.rivers.query(lon, lat);
        }
        System.out.println("edge tiles ok: " + n);
    }
}
