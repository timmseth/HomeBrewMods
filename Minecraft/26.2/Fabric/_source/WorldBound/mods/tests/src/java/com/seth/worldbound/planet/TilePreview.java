package com.seth.worldbound.planet;

import com.seth.worldbound.data.Tile;
import com.seth.worldbound.data.Tiling;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

/** Generates the game tiles around a planet's spawn, times them and renders a close-up. {@code TilePreview <out> <seed> [lat lon]} */
public final class TilePreview {
    public static void main(String[] a) throws IOException {
        File out = new File(a[0]);
        long seed = Long.parseLong(a[1]);
        Planet.KEEP_BUILD_DATA = true;
        Planet p = Planet.get(seed, System.out::println);
        double lat = a.length > 3 ? Double.parseDouble(a[2]) : p.spawnLat(), lon = a.length > 3 ? Double.parseDouble(a[3]) : p.spawnLon();
        if (a.length > 2 && a[2].equals("rugged")) {
            // the most rugged ice-free land cell with a big river nearby: a mountain valley
            double best = -1;
            for (int k = 0; k < p.grid.n; k++) {
                if (p.elev[k] <= 500 || p.ice[k] != 0) continue;
                double sc = p.rugged[k] + (p.hydro.discharge[k] > 100 ? 1500 : 0);
                if (sc > best) { best = sc; lat = p.grid.lat[k / Planet.W]; lon = p.grid.lon[k % Planet.W]; }
            }
        }
        System.out.printf(Locale.ROOT, "spawn %.3f %.3f, %d river stretches%n", lat, lon, p.source.rivers.count);
        PlanetSource src = p.source;
        int[][] want = {{Tiling.LAYER_ELEV, 10}, {Tiling.LAYER_LAKE, 10}, {Tiling.LAYER_ELEV, 7}, {Tiling.LAYER_VEG, 7}, {Tiling.LAYER_TREECOVER, 7},
                {Tiling.LAYER_LAKEDEPTH, 7}, {Tiling.LAYER_CLIMATE, 6}, {Tiling.LAYER_ELEV, 5}};
        Tile[] got = new Tile[want.length];
        for (int q = 0; q < want.length; q++) {
            int z = want[q][1];
            int tx = (int) Math.floor(Tiling.globalPx(z, lon) / Tiling.TILE), ty = (int) Math.floor(Tiling.globalPy(z, lat) / Tiling.TILE);
            long t0 = System.nanoTime();
            got[q] = src.load(want[q][0], z, tx, ty);
            System.out.printf(Locale.ROOT, "  %-10s z%-2d %d_%d: %.2f s%n", Tiling.layerName(want[q][0]), z, tx, ty, (System.nanoTime() - t0) / 1e9);
        }
        // close-up of the z10 elevation around the spawn: 1024 px = the whole tile
        Tile e = got[0], lk = got[1];
        int T = Tiling.TILE;
        BufferedImage img = new BufferedImage(T, T, BufferedImage.TYPE_INT_RGB);
        double pxM = Tiling.degPerPx(10) * 110574;
        int lo = 99999, hi = -99999;
        for (int y = 0; y < T; y++) for (int x = 0; x < T; x++) { lo = Math.min(lo, e.get(x, y)); hi = Math.max(hi, e.get(x, y)); }
        for (int y = 0; y < T; y++) {
            for (int x = 0; x < T; x++) {
                int v = e.get(x, y);
                int vx = e.get(Math.min(T - 1, x + 1), y), vy = e.get(x, Math.max(0, y - 1));
                double gx = (vx - v) / pxM, gy = (v - vy) / pxM;
                double shade = Math.max(0.45, Math.min(1.45, 1 + 2.2 * (-gx * 0.7 + gy * 0.7)));
                int c = PlanetPreview.tint(v);
                if (lk.get(x, y) != Tiling.NODATA_ELEV) c = 0x3A78C8;
                img.setRGB(x, y, (int) Math.min(255, (c >> 16 & 255) * shade) << 16 | (int) Math.min(255, (c >> 8 & 255) * shade) << 8 | (int) Math.min(255, (c & 255) * shade));
            }
        }
        // river channels from the same geometry the game uses (sampled)
        int z = 10;
        int tx = (int) Math.floor(Tiling.globalPx(z, lon) / T), ty = (int) Math.floor(Tiling.globalPy(z, lat) / T);
        double deg = Tiling.degPerPx(z), west = -180 + tx * T * deg, north = 90 - ty * T * deg;
        int hits = 0;
        for (int y = 0; y < T; y += 1) {
            for (int x = 0; x < T; x += 1) {
                if ((x & 1) == 1 || (y & 1) == 1) continue;
                var h = src.rivers.query(west + (x + 0.5) * deg, north - (y + 0.5) * deg);
                if (h != null) {
                    hits++;
                    for (int b = 0; b < 2; b++) for (int c = 0; c < 2; c++) img.setRGB(Math.min(T - 1, x + c), Math.min(T - 1, y + b), 0x1040D0);
                }
            }
        }
        System.out.printf(Locale.ROOT, "elevation %d..%d m, %d river samples%n", lo, hi, hits);
        ImageIO.write(img, "png", new File(out, "tile10_" + seed + ".png"));
        // vegetation and climate tiles
        BufferedImage vi = new BufferedImage(T, T, BufferedImage.TYPE_INT_RGB), ci = new BufferedImage(T, T, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < T; y++) {
            for (int x = 0; x < T; x++) {
                int v = got[3].get(x, y), tc = got[4].get(x, y);
                int col = v == 0 ? 0x203040 : v == 70 ? 0xF0F4FF : v == 60 ? 0xD8C8A0 : v == 20 ? 0xB0A060 : v == 30 ? 0xA8C060 : v == 90 ? 0x50A0A0 : v == 100 ? 0xA0A890
                        : v >= 111 && v <= 116 ? PlanetPreview.lerpRGB(0x60A040, 0x104010, (v - 111) / 5.0) : PlanetPreview.lerpRGB(0x90C060, 0x407030, (v - 121) / 5.0);
                vi.setRGB(x, y, col);
                int k = got[6].get(x, y);
                ci.setRGB(x, y, k > 0 ? ClimateCalibrate.KCOL[k] : 0x203040);
            }
        }
        ImageIO.write(vi, "png", new File(out, "veg7_" + seed + ".png"));
        ImageIO.write(ci, "png", new File(out, "clim6_" + seed + ".png"));
    }
}
