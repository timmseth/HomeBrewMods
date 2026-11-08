package com.seth.worldbound.client;

import com.seth.worldbound.Earth;
import com.seth.worldbound.data.RasterStack;

/**
 * Projection math and pixel rendering for the Earth picker. Two modes: a flat equirectangular
 * view (centre + degrees per pixel, unlimited zoom) and an orthographic globe (centre + radius
 * in pixels). Rendering samples the raster stack directly, so zooming in shows whatever the
 * finest loaded pack has — the jar base at world scale, the global pack at 300 m, a region pack
 * where one exists. Pure Java, no GL; the screen uploads the int[] into a texture.
 */
public final class MapView {
    public boolean globe = false;
    public double centerLat = 42.87, centerLon = -112.44;
    /** Flat: degrees per pixel. */
    public double degPerPx = 360.0 / 1024;
    /** Globe: sphere radius in pixels. */
    public double radius = 240;

    // Map land palette by Köppen class (index = class id). Earth tones: greens for wet, sands for dry,
    // grey-greens for cold — never blue, so land is never mistaken for water.
    private static final int[][] LAND = {
        {120, 140, 90},
        {34, 110, 50}, {60, 130, 60}, {120, 160, 70},                       // Af Am Aw
        {232, 205, 130}, {205, 180, 130}, {215, 195, 115}, {190, 185, 125},  // BWh BWk BSh BSk
        {175, 175, 85}, {155, 165, 75}, {135, 150, 85},                      // Csa Csb Csc
        {115, 170, 95}, {100, 160, 110}, {90, 140, 100},                     // Cwa Cwb Cwc
        {85, 160, 80}, {75, 150, 90}, {70, 130, 95},                         // Cfa Cfb Cfc
        {140, 150, 115}, {125, 140, 110}, {110, 130, 105}, {95, 115, 100},   // Dsa Dsb Dsc Dsd
        {120, 140, 135}, {110, 130, 130}, {95, 115, 120}, {85, 100, 110},    // Dwa Dwb Dwc Dwd
        {105, 150, 120}, {95, 140, 120}, {75, 120, 110}, {65, 100, 100},     // Dfa Dfb Dfc Dfd
        {185, 190, 185}, {240, 242, 245},                                    // ET EF
    };

    // ---- projections ----------------------------------------------------------------------

    // Flat mode is Mercator: the familiar wall-map look, scale true along the centre latitude.
    public static final double MAX_LAT = 85.0;

    public static double mercY(double lat) {
        return Math.log(Math.tan(Math.PI / 4 + Math.toRadians(lat) / 2));
    }

    public static double latFromMercY(double y) {
        return Math.toDegrees(2 * Math.atan(Math.exp(y)) - Math.PI / 2);
    }

    /** Pixel -> lon/lat, or null when the pixel is off the globe / beyond the Mercator cut-off. */
    public double[] toLonLat(double px, double py, int w, int h) {
        if (!globe) {
            double lon = centerLon + (px - w / 2.0) * degPerPx;
            double k = Math.toRadians(degPerPx);
            double lat = latFromMercY(mercY(centerLat) - (py - h / 2.0) * k);
            if (lat > MAX_LAT || lat < -MAX_LAT) return null;
            return new double[]{wrap(lon), lat};
        }
        double x = (px - w / 2.0) / radius, y = (h / 2.0 - py) / radius;
        double rho = Math.sqrt(x * x + y * y);
        if (rho > 1) return null;
        double c = Math.asin(rho);
        double phi0 = Math.toRadians(centerLat), lam0 = Math.toRadians(centerLon);
        double sinc = Math.sin(c), cosc = Math.cos(c);
        double lat = rho == 0 ? centerLat : Math.toDegrees(Math.asin(cosc * Math.sin(phi0) + y * sinc * Math.cos(phi0) / rho));
        double lon = rho == 0 ? centerLon : Math.toDegrees(lam0 + Math.atan2(x * sinc, rho * Math.cos(phi0) * cosc - y * Math.sin(phi0) * sinc));
        return new double[]{wrap(lon), lat};
    }

    /** lon/lat -> pixel, or null when on the far side of the globe. */
    public double[] toPixel(double lon, double lat, int w, int h) {
        if (!globe) {
            double dlon = lon - centerLon;
            if (dlon > 180) dlon -= 360;
            if (dlon < -180) dlon += 360;
            double k = Math.toRadians(degPerPx);
            double latc = Math.max(-MAX_LAT, Math.min(MAX_LAT, lat));
            return new double[]{w / 2.0 + dlon / degPerPx, h / 2.0 - (mercY(latc) - mercY(centerLat)) / k};
        }
        double phi = Math.toRadians(lat), lam = Math.toRadians(lon), phi0 = Math.toRadians(centerLat), lam0 = Math.toRadians(centerLon);
        double cosc = Math.sin(phi0) * Math.sin(phi) + Math.cos(phi0) * Math.cos(phi) * Math.cos(lam - lam0);
        if (cosc < 0) return null;
        double x = Math.cos(phi) * Math.sin(lam - lam0);
        double y = Math.cos(phi0) * Math.sin(phi) - Math.sin(phi0) * Math.cos(phi) * Math.cos(lam - lam0);
        return new double[]{w / 2.0 + x * radius, h / 2.0 - y * radius};
    }

    public void pan(double dx, double dy, int w, int h) {
        if (!globe) {
            centerLon = wrap(centerLon - dx * degPerPx);
            centerLat = clampFlat(latFromMercY(mercY(centerLat) + dy * Math.toRadians(degPerPx)));
        } else {
            double k = Math.toDegrees(1.0 / radius);
            centerLon = wrap(centerLon - dx * k / Math.max(0.25, Math.cos(Math.toRadians(centerLat))));
            centerLat = clampLat(centerLat + dy * k);
        }
    }

    /** Zoom by factor around a pixel so the point under the cursor stays put (flat) or stays near (globe). */
    public void zoom(double factor, double px, double py, int w, int h) {
        if (!globe) {
            double[] before = toLonLat(px, py, w, h);
            degPerPx = Math.max(1e-6, Math.min(360.0 / w, degPerPx / factor));
            if (before != null) {
                double[] after = toLonLat(px, py, w, h);
                if (after != null) {
                    centerLon = wrap(centerLon + before[0] - after[0]);
                    centerLat = clampFlat(latFromMercY(mercY(centerLat) + mercY(before[1]) - mercY(after[1])));
                }
            }
        } else {
            radius = Math.max(40, Math.min(5e7, radius * factor));
        }
    }

    public static double clampFlat(double lat) {
        return Math.max(-MAX_LAT, Math.min(MAX_LAT, lat));
    }

    /** A copy of this view whose pixels are `scale` times bigger (for rendering at lower resolution). */
    public MapView scaled(double scale) {
        MapView c = new MapView();
        c.globe = globe; c.centerLat = centerLat; c.centerLon = centerLon;
        c.degPerPx = degPerPx * scale;
        c.radius = radius / scale;
        return c;
    }

    /** Keep the two modes roughly in step when toggling. */
    public void toggleGlobe(int w, int h) {
        if (globe) {
            degPerPx = Math.toDegrees(1.0 / radius);
        } else {
            radius = 1.0 / Math.toRadians(degPerPx);
        }
        globe = !globe;
    }

    public static double wrap(double lon) {
        lon = ((lon + 180) % 360 + 360) % 360 - 180;
        return lon;
    }

    public static double clampLat(double lat) {
        return Math.max(-89.9, Math.min(89.9, lat));
    }

    // ---- rendering ------------------------------------------------------------------------

    /** Degrees per screen pixel of this view (globe: at the centre). */
    public double viewDegPerPx() {
        return globe ? Math.toDegrees(1.0 / radius) : degPerPx;
    }

    /** The tile zoom whose pixels are about the size of this view's pixels (never finer than needed). */
    public int dataZoom() {
        double dpp = viewDegPerPx();
        int z = 0;
        while (z < 20 && com.seth.worldbound.data.Tiling.degPerPx(z + 1) >= dpp * 0.75) z++;
        return z;
    }

    /** ARGB pixels for a w×h view of the current projection. Safe to call off the render thread; returns null if interrupted. */
    public int[] render(int w, int h) {
        int[] px = new int[w * h];
        double[] elev = new double[w * h];
        int[] clim = new int[w * h];
        boolean[] off = new boolean[w * h];
        Earth earth = Earth.get();
        RasterStack stack = earth.stack;
        double missing = earth.config.missing_data_elevation_m;
        int zoom = dataZoom() + 2; // up to two levels finer than the screen: sharp, still a few hundred tiles at most
        for (int y = 0; y < h; y++) {
            if (Thread.currentThread().isInterrupted()) return null;
            for (int x = 0; x < w; x++) {
                int i = y * w + x;
                double[] ll = toLonLat(x + 0.5, y + 0.5, w, h);
                if (ll == null) { off[i] = true; continue; }
                elev[i] = stack.elevation(ll[0], ll[1], missing, zoom);
                clim[i] = stack.climate(ll[0], ll[1], zoom);
                if (zoom >= 7 && stack.lakeSurface(ll[0], ll[1], zoom) != com.seth.worldbound.data.Tiling.NODATA_ELEV) clim[i] = -1; // lake (z7+ only: never thrash fine tiles)
            }
        }
        // Hillshade from the west/north neighbours; metres per pixel sets how strong a slope has to be.
        double mPerPx = viewDegPerPx() * 111_000.0;
        double k = 1.0 / Math.max(2.0, mPerPx * 0.6);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int i = y * w + x;
                if (off[i]) { px[i] = globe ? 0xFF0B0E1A : 0xFF101018; continue; }
                double e = elev[i];
                double shade = 1.0;
                if (e >= 0) {
                    double ew = x > 0 && !off[i - 1] ? elev[i - 1] : e;
                    double en = y > 0 && !off[i - w] ? elev[i - w] : e;
                    shade = 1.0 + ((ew - e) + (en - e)) * 0.5 * k;
                    shade = Math.max(0.55, Math.min(1.45, shade));
                }
                px[i] = clim[i] == -1 ? 0xFF3A7ED8 : shade(color(e, clim[i]), shade);
                // Coastline: a dark line where land meets water, so continents read at every zoom.
                boolean land = e >= 0 && clim[i] != -1;
                if ((x > 0 && !off[i - 1] && (elev[i - 1] >= 0 && clim[i - 1] != -1) != land) || (y > 0 && !off[i - w] && (elev[i - w] >= 0 && clim[i - w] != -1) != land)) {
                    px[i] = 0xFF1A1E26;
                }
            }
        }
        drawRivers(px, w, h, earth);
        return px;
    }

    /** River centrelines from rivers.bin; only rivers wide enough to matter at this zoom. */
    private void drawRivers(int[] px, int w, int h, Earth earth) {
        com.seth.worldbound.data.Rivers ri = earth.rivers;
        if (ri == null) return;
        double mPerPx = viewDegPerPx() * 111_000.0;
        int minW = (int) Math.max(16, mPerPx / 60);
        double w0 = 180, e0 = -180, s0 = 90, n0 = -90;
        for (int i = 0; i <= 8; i++) for (int j = 0; j <= 8; j++) {
            double[] ll = toLonLat(w * i / 8.0, h * j / 8.0, w, h);
            if (ll == null) continue;
            w0 = Math.min(w0, ll[0]); e0 = Math.max(e0, ll[0]); s0 = Math.min(s0, ll[1]); n0 = Math.max(n0, ll[1]);
        }
        if (globe) { s0 = Math.max(-90, s0 - 5); n0 = Math.min(90, n0 + 5); w0 = -180; e0 = 180; }
        if (e0 < w0 || e0 - w0 > 300) { w0 = -180; e0 = 180; }
        final int riverColor = 0xFF3A7ED8;
        ri.forEach(w0, s0, e0, n0, minW, (x1, y1, x2, y2, wm) -> {
            double[] a = toPixel(x1, y1, w, h), b = toPixel(x2, y2, w, h);
            if (a == null || b == null || Math.abs(a[0] - b[0]) > w / 2.0) return true;
            int t = (int) Math.max(1, Math.round(wm / mPerPx));
            line(px, w, h, a[0], a[1], b[0], b[1], t, riverColor);
            return true;
        });
    }

    private static void line(int[] px, int w, int h, double x0, double y0, double x1, double y1, int t, int color) {
        int steps = (int) Math.max(1, Math.ceil(Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0))));
        int r0 = -(t - 1) / 2, r1 = t / 2;
        for (int i = 0; i <= steps; i++) {
            int x = (int) Math.floor(x0 + (x1 - x0) * i / steps), y = (int) Math.floor(y0 + (y1 - y0) * i / steps);
            for (int dy = r0; dy <= r1; dy++) for (int dx = r0; dx <= r1; dx++) {
                int xx = x + dx, yy = y + dy;
                if (xx >= 0 && yy >= 0 && xx < w && yy < h) px[yy * w + xx] = color;
            }
        }
    }

    private static int shade(int argb, double f) {
        int r = clamp((int) (((argb >> 16) & 0xFF) * f)), g = clamp((int) (((argb >> 8) & 0xFF) * f)), b = clamp((int) ((argb & 0xFF) * f));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int color(double elev, int climate) {
        if (elev < 0 && climate != 30) {
            double d = Math.min(1, -elev / 6000.0);
            int r = (int) (40 - 25 * d), g = (int) (110 - 70 * d), b = (int) (200 - 90 * d);
            return 0xFF000000 | (r << 16) | (g << 8) | b;
        }
        int[] c = (climate >= 1 && climate <= 30) ? LAND[climate] : LAND[0];
        // lighten toward snow with altitude
        double t = Math.min(1, Math.max(0, elev) / 4500.0);
        double t2 = t * t;
        int r = (int) (c[0] * (1 - t2) + 245 * t2), g = (int) (c[1] * (1 - t2) + 245 * t2), b = (int) (c[2] * (1 - t2) + 250 * t2);
        return 0xFF000000 | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    private static int clamp(int v) {
        return v < 0 ? 0 : v > 255 ? 255 : v;
    }
}
