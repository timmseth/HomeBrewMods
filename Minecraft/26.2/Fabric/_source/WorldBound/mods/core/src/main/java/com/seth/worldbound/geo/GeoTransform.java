package com.seth.worldbound.geo;

/**
 * Block (x, z) &lt;-&gt; (lon, lat). Equirectangular with the east–west scale true at the origin
 * latitude (PLAN.md §3.1). +x is east, +z is south.
 */
public final class GeoTransform {
    public static final double M_PER_DEG_LAT = 110_574.0;
    public static final double M_PER_DEG_LON_EQUATOR = 111_320.0;

    public final double originLat, originLon, metresPerBlock;
    private final double blocksPerDegLon, blocksPerDegLat;
    /** Blocks in 360° of longitude, a whole number of chunks so the world repeats exactly (wrap-around). */
    public final int widthBlocks;
    /** Block x of the antimeridian west of the origin; the playable band is [seamWestX, seamWestX + widthBlocks). */
    public final int seamWestX;

    public GeoTransform(double originLat, double originLon, double metresPerBlock) {
        this.originLat = originLat;
        this.originLon = originLon;
        this.metresPerBlock = metresPerBlock;
        double raw = M_PER_DEG_LON_EQUATOR * Math.cos(Math.toRadians(originLat)) / metresPerBlock;
        this.widthBlocks = (int) Math.max(16, Math.round(raw * 360.0 / 16.0) * 16);
        this.blocksPerDegLon = widthBlocks / 360.0; // ~0.0003 % off true scale, so x and x + width match exactly
        this.blocksPerDegLat = M_PER_DEG_LAT / metresPerBlock;
        this.seamWestX = Math.floorDiv((int) Math.floor((-180.0 - originLon) * blocksPerDegLon), 16) * 16; // chunk-aligned
    }

    public double lon(double blockX) {
        double lon = originLon + blockX / blocksPerDegLon;
        // Wrap so the world continues across the antimeridian instead of ending there.
        if (lon >= 180.0) lon -= 360.0;
        if (lon < -180.0) lon += 360.0;
        return lon;
    }

    public double lat(double blockZ) {
        double lat = originLat - blockZ / blocksPerDegLat;
        return Math.max(-90.0, Math.min(90.0, lat));
    }

    /** Block x for a longitude, inside the playable band [seamWestX, seamWestX + widthBlocks). */
    public double blockX(double lon) {
        double x = (lon - originLon) * blocksPerDegLon;
        if (!Double.isFinite(x)) return seamWestX;
        // Closed form (a loop of += width never finishes for a huge input).
        double r = (x - seamWestX) % widthBlocks;
        if (r < 0) r += widthBlocks;
        return seamWestX + r;
    }

    /** The same column inside the playable band (terrain repeats every widthBlocks). */
    public int wrapX(int blockX) {
        return seamWestX + Math.floorMod(blockX - seamWestX, widthBlocks);
    }

    public double blockZ(double lat) {
        return (originLat - lat) * blocksPerDegLat;
    }

    /** Degrees of longitude per block at the origin latitude. */
    public double degLonPerBlock() {
        return 1.0 / blocksPerDegLon;
    }

    public double degLatPerBlock() {
        return 1.0 / blocksPerDegLat;
    }

    /** World width in blocks (360 degrees of longitude). */
    public double worldWidthBlocks() {
        return 360.0 * blocksPerDegLon;
    }

    public double worldHeightBlocks() {
        return 180.0 * blocksPerDegLat;
    }
}
