package com.seth.worldbound.planet;

import com.seth.worldbound.data.Tiling;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/** Raw elevation (no valleys), ruggedness and ice over one z10 tile. */
public final class DebugTile {
    public static void main(String[] a) throws Exception {
        long seed = Long.parseLong(a[1]);
        int tx = Integer.parseInt(a[2]), ty = Integer.parseInt(a[3]);
        Planet p = Planet.get(seed, null);
        int T = 1024, z = 10;
        BufferedImage img = new BufferedImage(T * 3, T, BufferedImage.TYPE_INT_RGB);
        java.lang.reflect.Method m = PlanetSource.class.getDeclaredMethod("rawElev", int.class, int.class, int.class);
        m.setAccessible(true);
        float[] e = (float[]) m.invoke(p.source, z, tx, ty);
        java.lang.reflect.Field f = PlanetSource.class.getDeclaredField("iceF");
        f.setAccessible(true);
        float[] ice = (float[]) f.get(p.source);
        double pxM = Tiling.degPerPx(z) * 110574;
        for (int y = 0; y < T; y++) for (int x = 0; x < T; x++) {
            double lon = -180 + (tx * T + x + 0.5) * Tiling.degPerPx(z), lat = 90 - (ty * T + y + 0.5) * Tiling.degPerPx(z);
            float v = e[y * T + x], vx = e[y * T + Math.min(T - 1, x + 1)], vy = e[Math.max(0, y - 1) * T + x];
            double shade = Math.max(0.45, Math.min(1.45, 1 + 2.2 * (-(vx - v) / pxM * 0.7 + (v - vy) / pxM * 0.7)));
            int c = PlanetPreview.tint(v);
            img.setRGB(x, y, (int) Math.min(255, (c >> 16 & 255) * shade) << 16 | (int) Math.min(255, (c >> 8 & 255) * shade) << 8 | (int) Math.min(255, (c & 255) * shade));
            double r = p.grid.sample(p.rugged, lon, lat);
            int rv = (int) Math.min(255, r / 8);
            img.setRGB(T + x, y, rv << 16 | rv << 8 | rv);
            int iv = (int) (255 * p.grid.sample(ice, lon, lat));
            img.setRGB(2 * T + x, y, iv << 8 | 40);
        }
        ImageIO.write(img, "png", new File(a[0], "debug_" + tx + "_" + ty + ".png"));
    }
}
