package com.seth.worldbound.planet;

import com.seth.worldbound.data.Tiling;

public final class DebugRug {
    public static void main(String[] a) {
        Planet p = Planet.get(3, null);
        int tx = 667, ty = 108, T = 1024, z = 10, y = 400;
        double lat = 90 - (ty * T + y + 0.5) * Tiling.degPerPx(z);
        for (int x = 100; x < 160; x += 4) {
            double lon = -180 + (tx * T + x + 0.5) * Tiling.degPerPx(z);
            double fx = p.grid.fx(lon), fy = p.grid.fy(lat);
            int i = (int) Math.floor(fx), j = (int) Math.floor(fy);
            System.out.printf("x %d lon %.5f fx %.3f fy %.3f rug %.0f  cells %d,%d: %.0f %.0f / %.0f %.0f  elev %.0f %.0f%n", x, lon, fx, fy, p.grid.sample(p.rugged, lon, lat),
                    i, j, p.rugged[j * 4096 + i], p.rugged[j * 4096 + i + 1], p.rugged[(j + 1) * 4096 + i], p.rugged[(j + 1) * 4096 + i + 1], p.elev[j * 4096 + i], p.elev[j * 4096 + i + 1]);
        }
    }
}
