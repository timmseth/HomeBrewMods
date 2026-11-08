package com.seth.worldbound.planet;

import com.seth.worldbound.data.Rivers;

import java.util.Arrays;

/**
 * A generated planet's rivers as channels: every river cell of {@link Hydrology} becomes one smooth stretch from a
 * jittered point in its cell to the next one downstream (Catmull-Rom through its upstream and downstream neighbours,
 * so courses bend instead of zig-zagging and tributaries meet the main stem at a point). On top of that the channel
 * meanders, with a wavelength of ~11 channel widths and an amplitude of ~2 widths (less where it cuts through
 * mountains), like real rivers. The same geometry answers "is this point in a river" for terrain generation and lays
 * out the valley floors in {@link PlanetSource}.
 */
public final class PlanetRivers implements Rivers {
    private static final double BUCKET = 0.25;
    private static final int BW = (int) Math.round(360 / BUCKET), BH = (int) Math.round(180 / BUCKET);
    static final double KY = 110_574.0, KX0 = 111_320.0;

    final Planet p;
    final int count;
    final int[] cell;
    final float[] width, depth, amp, wave, valley, bed0, bed1;
    /** Control points per river: u, start, end, d (lon, lat each), longitudes unwrapped around the start. */
    final double[] pts;
    /** Channel corridor bbox per river: west, south, east, north (degrees, west unwrapped like pts). */
    final float[] box;
    final float[] phase;
    private final int[] bStart, bItems;

    PlanetRivers(Planet planet) {
        p = planet;
        Hydrology hy = planet.hydro;
        Grid g = planet.grid;
        int[] rv = hy.rivers;
        count = rv.length;
        cell = rv.clone();
        // river index by cell: sorted copy + binary search
        long[] byCell = new long[count];
        for (int i = 0; i < count; i++) byCell[i] = ((long) cell[i] << 32) | i;
        Arrays.sort(byCell);
        width = new float[count];
        depth = new float[count];
        amp = new float[count];
        wave = new float[count];
        valley = new float[count];
        bed0 = new float[count];
        bed1 = new float[count];
        phase = new float[count * 2];
        pts = new double[count * 8];
        box = new float[count * 4];
        // main upstream branch of each river cell (the tributary carrying the most water)
        int[] mainUp = new int[count];
        Arrays.fill(mainUp, -1);
        for (int i = 0; i < count; i++) {
            int r = hy.receiver[cell[i]];
            int ri = r < 0 ? -1 : find(byCell, r);
            if (ri < 0) continue;
            if (mainUp[ri] < 0 || hy.discharge[cell[i]] > hy.discharge[cell[mainUp[ri]]]) mainUp[ri] = i;
        }
        // bed elevation at each node, never rising downstream
        float[] node = new float[count];
        for (int i = 0; i < count; i++) {
            double[] ll = nodeLonLat(g, cell[i]);
            node[i] = (float) Math.max(0.5, Math.min(planet.elev[cell[i]], g.sampleCubic(planet.elev, ll[0], ll[1])));
        }
        Integer[] order = new Integer[count];
        for (int i = 0; i < count; i++) order[i] = i;
        final float[] filled = hy.filled;
        Arrays.sort(order, (a, b) -> Float.compare(filled[cell[b]], filled[cell[a]]));
        for (int oi = 0; oi < count; oi++) {
            int i = order[oi];
            int r = hy.receiver[cell[i]];
            int ri = r < 0 ? -1 : find(byCell, r);
            if (ri >= 0) node[ri] = Math.min(node[ri], node[i]);
        }
        for (int i = 0; i < count; i++) {
            int c = cell[i];
            double q = hy.discharge[c];
            double rug = planet.rugged[c];
            double mtn = Math.max(0, Math.min(1, (rug - 500) / 1000));
            width[i] = (float) Math.max(8, Math.min(3500, 5 * Math.sqrt(q)));
            depth[i] = (float) Math.max(0.8, Math.min(60, 0.3 * Math.pow(q, 0.4)));
            amp[i] = (float) (Math.max(15, Math.min(3000, 2.0 * width[i])) * (1 - 0.6 * mtn));
            wave[i] = (float) Math.max(140, Math.min(30000, 11 * width[i]));
            valley[i] = (float) Math.max(300, Math.min(8000, 350 + 30 * Math.sqrt(q)));
            long h = mix(c * 0x9E3779B97F4A7C15L + planet.seed);
            phase[i * 2] = (float) ((h & 0xFFFF) / 65536.0 * Math.PI * 2);
            phase[i * 2 + 1] = (float) (((h >>> 16) & 0xFFFF) / 65536.0 * Math.PI * 2);
            double[] a = nodeLonLat(g, c);
            int r = hy.receiver[c];
            double[] b;
            boolean sea = r < 0 || planet.elev[r] <= 0;
            boolean lake = !sea && hy.lakeLevel[r] != Hydrology.NO_LAKE;
            if (sea) {
                // mouth: halfway to the sea cell's centre
                double[] cc = r < 0 ? a : centre(g, r);
                b = new double[]{a[0] + wrap(cc[0] - a[0]) * 0.5, a[1] + (cc[1] - a[1]) * 0.5};
            } else {
                b = nodeLonLat(g, r);
            }
            b[0] = a[0] + wrap(b[0] - a[0]);
            double[] u = mainUp[i] >= 0 ? nodeLonLat(g, cell[mainUp[i]]) : new double[]{2 * a[0] - b[0], 2 * a[1] - b[1]};
            u[0] = a[0] + wrap(u[0] - a[0]);
            double[] d;
            int rr = r >= 0 && !sea ? hy.receiver[r] : -1;
            if (rr >= 0 && planet.elev[rr] > 0) {
                d = nodeLonLat(g, rr);
                d[0] = a[0] + wrap(d[0] - a[0]);
            } else d = new double[]{2 * b[0] - a[0], 2 * b[1] - a[1]};
            int o = i * 8;
            pts[o] = u[0]; pts[o + 1] = u[1]; pts[o + 2] = a[0]; pts[o + 3] = a[1];
            pts[o + 4] = b[0]; pts[o + 5] = b[1]; pts[o + 6] = d[0]; pts[o + 7] = d[1];
            bed0[i] = node[i];
            int ri = !sea && r >= 0 ? find(byCell, r) : -1;
            float end = sea ? 0f : lake ? hy.lakeLevel[r] : ri >= 0 ? node[ri] : node[i];
            bed1[i] = Math.min(node[i], end);
            // corridor: hull of the stretch plus the meanders and half the channel
            double padM = amp[i] + width[i] * 0.5 + 30;
            double kx = KX0 * Math.max(0.05, Math.cos(Math.toRadians(a[1])));
            double minLon = Math.min(a[0], b[0]), maxLon = Math.max(a[0], b[0]), minLat = Math.min(a[1], b[1]), maxLat = Math.max(a[1], b[1]);
            double bulgeLon = 0.25 * Math.abs(b[0] - a[0]) + 0.1 * Math.abs(b[1] - a[1]), bulgeLat = 0.25 * Math.abs(b[1] - a[1]) + 0.1 * Math.abs(b[0] - a[0]);
            box[i * 4] = (float) (minLon - bulgeLon - padM / kx);
            box[i * 4 + 1] = (float) (minLat - bulgeLat - padM / KY);
            box[i * 4 + 2] = (float) (maxLon + bulgeLon + padM / kx);
            box[i * 4 + 3] = (float) (maxLat + bulgeLat + padM / KY);
        }
        // buckets (0.25°), widest first within each
        int[] cnt = new int[BW * BH + 1];
        for (int pass = 0; pass < 2; pass++) {
            int[] fill = pass == 1 ? new int[BW * BH] : null;
            int[] items = pass == 1 ? new int[cnt[BW * BH]] : null;
            for (int i = 0; i < count; i++) {
                int x0 = (int) Math.floor((box[i * 4] + 180) / BUCKET), x1 = (int) Math.floor((box[i * 4 + 2] + 180) / BUCKET);
                int y0 = Math.max(0, (int) Math.floor((90 - box[i * 4 + 3]) / BUCKET)), y1 = Math.min(BH - 1, (int) Math.floor((90 - box[i * 4 + 1]) / BUCKET));
                for (int y = y0; y <= y1; y++) {
                    for (int x = x0; x <= x1; x++) {
                        int b = y * BW + Math.floorMod(x, BW);
                        if (pass == 0) cnt[b]++;
                        else items[cnt[b] + fill[b]++] = i;
                    }
                }
            }
            if (pass == 0) {
                int acc = 0;
                for (int b = 0; b < BW * BH; b++) { int c = cnt[b]; cnt[b] = acc; acc += c; }
                cnt[BW * BH] = acc;
            } else {
                bItemsTmp = items;
            }
        }
        bStart = cnt;
        bItems = bItemsTmp;
        bItemsTmp = null;
    }

    private int[] bItemsTmp;

    private static int find(long[] byCell, int c) {
        int lo = 0, hi = byCell.length - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            int v = (int) (byCell[mid] >>> 32);
            if (v < c) lo = mid + 1;
            else if (v > c) hi = mid - 1;
            else return (int) byCell[mid];
        }
        return -1;
    }

    private static double wrap(double d) {
        return d > 180 ? d - 360 : d < -180 ? d + 360 : d;
    }

    static long mix(long h) {
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= h >>> 33;
        return h;
    }

    private static double[] centre(Grid g, int c) {
        return new double[]{g.lon[c % g.w], g.lat[c / g.w]};
    }

    /** A river's point in its cell: off-centre by up to a third of a cell, the same every time. */
    static double[] nodeLonLat(Grid g, int c) {
        long h = mix(c * 0xD1B54A32D192ED03L + 0x51ED);
        double jx = ((h & 0xFFFF) / 65535.0 - 0.5) * 0.66, jy = (((h >>> 16) & 0xFFFF) / 65535.0 - 0.5) * 0.66;
        int i = c % g.w, j = c / g.w;
        return new double[]{-180 + (i + 0.5 + jx) * 360.0 / g.w, 90 - (j + 0.5 + jy) * 180.0 / g.h};
    }

    // ---- geometry ----------------------------------------------------------------------------------------

    /** Point on river i's meandering centreline at parameter t, in metres east/north of its start point. */
    void point(int i, double t, double kx, double[] out) {
        point(i, t, kx, out, true);
    }

    void point(int i, double t, double kx, double[] out, boolean meander) {
        int o = i * 8;
        double ax = 0, ay = 0;
        double ux = (pts[o] - pts[o + 2]) * kx, uy = (pts[o + 1] - pts[o + 3]) * KY;
        double bx = (pts[o + 4] - pts[o + 2]) * kx, by = (pts[o + 5] - pts[o + 3]) * KY;
        double dx = (pts[o + 6] - pts[o + 2]) * kx, dy = (pts[o + 7] - pts[o + 3]) * KY;
        double t2 = t * t, t3 = t2 * t;
        double x = 0.5 * ((2 * ax) + (-ux + bx) * t + (2 * ux - 5 * ax + 4 * bx - dx) * t2 + (-ux + 3 * ax - 3 * bx + dx) * t3);
        double y = 0.5 * ((2 * ay) + (-uy + by) * t + (2 * uy - 5 * ay + 4 * by - dy) * t2 + (-uy + 3 * ay - 3 * by + dy) * t3);
        double tx = 0.5 * ((-ux + bx) + 2 * (2 * ux - 5 * ax + 4 * bx - dx) * t + 3 * (-ux + 3 * ax - 3 * bx + dx) * t2);
        double ty = 0.5 * ((-uy + by) + 2 * (2 * uy - 5 * ay + 4 * by - dy) * t + 3 * (-uy + 3 * ay - 3 * by + dy) * t2);
        double tl = Math.hypot(tx, ty);
        double len = Math.hypot(bx, by);
        double off = 0;
        if (meander && tl > 1e-9 && len > 1) {
            double lam = wave[i];
            double k = Math.max(1, Math.round(2 * len / lam));
            double env = Math.min(1, Math.min(t, 1 - t) * len / (0.5 * lam));
            off = amp[i] * env * Math.sin(Math.PI * k * t + phase[i * 2] + 1.1 * Math.sin(2 * Math.PI * Math.max(0.5, k / 3.3) * t + phase[i * 2 + 1]));
            x += -ty / tl * off;
            y += tx / tl * off;
        }
        out[0] = x;
        out[1] = y;
    }

    private static final ThreadLocal<double[]> TMP = ThreadLocal.withInitial(() -> new double[2]);

    /** Distance (m) from (lon, lat) to river i's centreline; +inf when clearly farther than {@code maxM}. */
    double distance(int i, double lon, double lat, double maxM) {
        int o = i * 8;
        double kx = KX0 * Math.max(0.05, Math.cos(Math.toRadians(pts[o + 3])));
        double qx = wrap(lon - pts[o + 2]) * kx, qy = (lat - pts[o + 3]) * KY;
        double[] pt = TMP.get();
        // coarse pass on the smooth course (no meanders) to find where along the stretch the point is
        double bx = (pts[o + 4] - pts[o + 2]) * kx, by = (pts[o + 5] - pts[o + 3]) * KY;
        double len = Math.max(1, Math.hypot(bx, by));
        double bestT = 0, best = Double.MAX_VALUE, px = 0, py = 0;
        float a0 = amp[i];
        for (int s = 0; s <= 16; s++) {
            double t = s / 16.0;
            point(i, t, kx, pt, false);
            if (s > 0) {
                double d = segDist(qx, qy, px, py, pt[0], pt[1]);
                if (d < best) { best = d; bestT = t - 1 / 32.0; }
            }
            px = pt[0];
            py = pt[1];
        }
        if (best > maxM + a0 + 50) return Double.POSITIVE_INFINITY;
        // fine pass on the meandering channel around there
        double win = (2 * a0 + wave[i] * 0.5) / len + 1 / 16.0;
        double t0 = Math.max(0, bestT - win), t1 = Math.min(1, bestT + win);
        int steps = (int) Math.max(8, Math.min(400, Math.ceil((t1 - t0) * len / (wave[i] / 14))));
        best = Double.MAX_VALUE;
        for (int s = 0; s <= steps; s++) {
            double t = t0 + (t1 - t0) * s / steps;
            point(i, t, kx, pt);
            if (s > 0) best = Math.min(best, segDist(qx, qy, px, py, pt[0], pt[1]));
            px = pt[0];
            py = pt[1];
        }
        return best;
    }

    private static double segDist(double qx, double qy, double x1, double y1, double x2, double y2) {
        double dx = x2 - x1, dy = y2 - y1, l2 = dx * dx + dy * dy;
        double t = l2 == 0 ? 0 : Math.max(0, Math.min(1, ((qx - x1) * dx + (qy - y1) * dy) / l2));
        double ex = x1 + t * dx - qx, ey = y1 + t * dy - qy;
        return Math.sqrt(ex * ex + ey * ey);
    }

    private int bucket(double lon, double lat) {
        int x = Math.floorMod((int) Math.floor((lon + 180) / BUCKET), BW), y = (int) Math.floor((90 - lat) / BUCKET);
        return Math.max(0, Math.min(BH - 1, y)) * BW + x;
    }

    private boolean inBox(int i, double lon, double lat) {
        if (lat < box[i * 4 + 1] || lat > box[i * 4 + 3]) return false;
        double w = box[i * 4];
        double lo = w + (((lon - w) % 360) + 360) % 360;
        return lo <= box[i * 4 + 2];
    }

    @Override
    public Hit query(double lon, double lat) {
        int b = bucket(lon, lat);
        for (int q = bStart[b]; q < bStart[b + 1]; q++) {
            int i = bItems[q];
            if (!inBox(i, lon, lat)) continue;
            double half = width[i] * 0.5;
            double d = distance(i, lon, lat, half);
            if (d <= half) return new Hit(d, width[i], depth[i]);
        }
        return null;
    }

    @Override
    public void forEach(double west, double south, double east, double north, int minWidthM, SegmentVisitor v) {
        int x0 = (int) Math.floor((west + 180) / BUCKET), x1 = (int) Math.floor((east + 180) / BUCKET);
        int y0 = Math.max(0, (int) Math.floor((90 - north) / BUCKET)), y1 = Math.min(BH - 1, (int) Math.floor((90 - south) / BUCKET));
        double[] pt = new double[2];
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                int b = y * BW + Math.floorMod(x, BW);
                for (int q = bStart[b]; q < bStart[b + 1]; q++) {
                    int i = bItems[q];
                    if (width[i] < minWidthM) break;
                    int o = i * 8;
                    double kx = KX0 * Math.max(0.05, Math.cos(Math.toRadians(pts[o + 3])));
                    double plon = pts[o + 2], plat = pts[o + 3];
                    boolean go = true;
                    for (int s = 1; s <= 4 && go; s++) {
                        point(i, s / 4.0, kx, pt);
                        double nlon = pts[o + 2] + pt[0] / kx, nlat = pts[o + 3] + pt[1] / KY;
                        go = v.visit((float) plon, (float) plat, (float) nlon, (float) nlat, (int) width[i]);
                        plon = nlon;
                        plat = nlat;
                    }
                    if (!go) break;
                }
            }
        }
    }

    /** Rivers whose valleys may reach into the bbox (degrees; west may be < -180 or east > 180). */
    void collect(double west, double south, double east, double north, java.util.function.IntConsumer out) {
        double padLat = 13500 / KY, padLon = 13500 / (KX0 * Math.max(0.05, Math.cos(Math.toRadians(Math.max(Math.abs(south), Math.abs(north))))));
        int x0 = (int) Math.floor((west - padLon + 180) / BUCKET), x1 = (int) Math.floor((east + padLon + 180) / BUCKET);
        int y0 = Math.max(0, (int) Math.floor((90 - north - padLat) / BUCKET)), y1 = Math.min(BH - 1, (int) Math.floor((90 - south + padLat) / BUCKET));
        java.util.BitSet seen = new java.util.BitSet(count);
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                int b = y * BW + Math.floorMod(x, BW);
                for (int q = bStart[b]; q < bStart[b + 1]; q++) {
                    int i = bItems[q];
                    if (seen.get(i)) continue;
                    seen.set(i);
                    out.accept(i);
                }
            }
        }
    }

    /** Start point (lon, lat) of river i. */
    double lon0(int i) {
        return pts[i * 8 + 2];
    }

    double lat0(int i) {
        return pts[i * 8 + 3];
    }

    /** Approximate length (m) of river i's stretch. */
    double length(int i) {
        int o = i * 8;
        double kx = KX0 * Math.max(0.05, Math.cos(Math.toRadians(pts[o + 3])));
        return Math.hypot((pts[o + 4] - pts[o + 2]) * kx, (pts[o + 5] - pts[o + 3]) * KY) * 1.15;
    }
}
