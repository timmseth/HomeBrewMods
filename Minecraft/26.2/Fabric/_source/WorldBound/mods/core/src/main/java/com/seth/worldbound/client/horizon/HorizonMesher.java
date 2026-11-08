package com.seth.worldbound.client.horizon;

import com.seth.worldbound.Earth;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Builds one horizon tile: a grid of flat-topped columns (a top face each, side faces wherever a neighbour is
 * lower, skirts at the tile edge) sampled straight from the Earth data at the tile's spacing. Vertices are
 * POSITION_COLOR (16 bytes), tile-local; the renderer offsets them to the camera.
 */
final class HorizonMesher {
    /** Columns per tile side. */
    static final int N = 32;
    /** Lowest level's column width in blocks; each level doubles it. */
    static final int BASE_SPACING = 2;
    private static final int VERTEX_BYTES = 16;

    static int spacing(int level) {
        return BASE_SPACING << level;
    }

    static int tileBlocks(int level) {
        return N * spacing(level);
    }

    record Mesh(ByteBuffer data, int quads, int minY, int maxY) {
        void free() {
        }
    }

    private final HorizonColors colors;

    HorizonMesher(HorizonColors colors) {
        this.colors = colors;
    }

    Mesh build(Earth e, int level, int tx, int tz) {
        int s = spacing(level), T = tileBlocks(level);
        int x0 = tx * T, z0 = tz * T;
        int W = N + 2;
        int[] h = new int[W * W], top = new int[W * W], side = new int[W * W];
        boolean[] water = new boolean[W * W];
        boolean coarse = level >= 3;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        for (int j = 0; j < W; j++) {
            for (int i = 0; i < W; i++) {
                int bx = x0 + (i - 1) * s + s / 2, bz = z0 + (j - 1) * s + s / 2;
                Earth.Surface su = e.lodAt(bx, bz, s);
                HorizonColors.Look look = colors.look(e, su, bx, bz, s, coarse);
                int k = j * W + i;
                h[k] = look.topY();
                top[k] = look.top();
                side[k] = look.side();
                water[k] = look.water();
                if (look.topY() < minY) minY = look.topY();
                if (look.topY() > maxY) maxY = look.topY();
            }
        }
        // Count faces, then write them.
        int quads = 0;
        for (int j = 1; j <= N; j++) {
            for (int i = 1; i <= N; i++) {
                quads++;
                int k = j * W + i, y = h[k];
                if (h[k - 1] < y || i == 1) quads++;
                if (h[k + 1] < y || i == N) quads++;
                if (h[k - W] < y || j == 1) quads++;
                if (h[k + W] < y || j == N) quads++;
            }
        }
        ByteBuffer buf = ByteBuffer.allocateDirect(quads * 4 * VERTEX_BYTES).order(ByteOrder.nativeOrder());
        double lx = -0.45, ly = 1.0, lz = -0.35, ll = Math.sqrt(lx * lx + ly * ly + lz * lz);
        lx /= ll; ly /= ll; lz /= ll;
        int skirt = 2 * s;
        for (int j = 1; j <= N; j++) {
            for (int i = 1; i <= N; i++) {
                int k = j * W + i, y = h[k];
                float fx0 = (i - 1) * s, fx1 = i * s, fz0 = (j - 1) * s, fz1 = j * s, fy = y + 1;
                // Top: lit by the slope (sun from the north-west, as a mapmaker would shade it).
                double shade;
                if (water[k]) {
                    shade = 1.0;
                } else {
                    double hx = (h[k + 1] - h[k - 1]) / (2.0 * s), hz = (h[k + W] - h[k - W]) / (2.0 * s);
                    double nl = Math.sqrt(hx * hx + 1 + hz * hz);
                    double d = (-hx * lx + ly - hz * lz) / nl;
                    shade = Math.max(0.5, Math.min(1.0, 0.62 + 0.42 * d));
                }
                int c = HorizonColors.scale(top[k], shade);
                quad(buf, fx0, fy, fz0, fx0, fy, fz1, fx1, fy, fz1, fx1, fy, fz0, c);
                // Sides, down to the lower neighbour (or a skirt below it at the tile edge).
                int sc = side[k];
                if (h[k - 1] < y || i == 1) {       // west face (x = fx0)
                    float by = Math.min(y, h[k - 1]) + 1 - (i == 1 ? skirt : 0);
                    int cc = HorizonColors.scale(y - h[k - 1] <= 3 ? top[k] : sc, 0.6);
                    quad(buf, fx0, by, fz1, fx0, fy, fz1, fx0, fy, fz0, fx0, by, fz0, cc);
                }
                if (h[k + 1] < y || i == N) {       // east face (x = fx1)
                    float by = Math.min(y, h[k + 1]) + 1 - (i == N ? skirt : 0);
                    int cc = HorizonColors.scale(y - h[k + 1] <= 3 ? top[k] : sc, 0.6);
                    quad(buf, fx1, by, fz0, fx1, fy, fz0, fx1, fy, fz1, fx1, by, fz1, cc);
                }
                if (h[k - W] < y || j == 1) {       // north face (z = fz0)
                    float by = Math.min(y, h[k - W]) + 1 - (j == 1 ? skirt : 0);
                    int cc = HorizonColors.scale(y - h[k - W] <= 3 ? top[k] : sc, 0.8);
                    quad(buf, fx0, by, fz0, fx0, fy, fz0, fx1, fy, fz0, fx1, by, fz0, cc);
                }
                if (h[k + W] < y || j == N) {       // south face (z = fz1)
                    float by = Math.min(y, h[k + W]) + 1 - (j == N ? skirt : 0);
                    int cc = HorizonColors.scale(y - h[k + W] <= 3 ? top[k] : sc, 0.8);
                    quad(buf, fx1, by, fz1, fx1, fy, fz1, fx0, fy, fz1, fx0, by, fz1, cc);
                }
            }
        }
        buf.flip();
        return new Mesh(buf, quads, minY - skirt, maxY + 1);
    }

    private static void quad(ByteBuffer b, float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3, int rgb) {
        vertex(b, x0, y0, z0, rgb);
        vertex(b, x1, y1, z1, rgb);
        vertex(b, x2, y2, z2, rgb);
        vertex(b, x3, y3, z3, rgb);
    }

    private static void vertex(ByteBuffer b, float x, float y, float z, int rgb) {
        b.putFloat(x).putFloat(y).putFloat(z);
        b.put((byte) (rgb >> 16)).put((byte) (rgb >> 8)).put((byte) rgb).put((byte) 255);
    }
}
