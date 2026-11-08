package com.seth.worldbound.data;

/** Geographic tiling shared with tools/tiling.py — keep the two in sync. */
public final class Tiling {
    public static final int TILE = 1024;

    public static final int LAYER_CLIMATE = 1;
    public static final int LAYER_ELEV = 2;
    public static final int LAYER_COVER = 3;
    public static final int LAYER_LAKE = 4;       // lake surface elevation, i16 metres
    public static final int LAYER_LAKEDEPTH = 5;  // lake mean depth, u8 metres
    public static final int LAYER_VEG = 6;        // Copernicus LC100 class, u8 (0 = none/ocean)
    public static final int LAYER_TREECOVER = 7;  // Copernicus LC100 tree cover %, u8 (255 = no data)

    public static final int NODATA_CLIMATE = 0;
    public static final int NODATA_ELEV = -32768;
    public static final int NODATA_COVER = 0;

    private Tiling() {}

    public static String layerName(int layer) {
        return switch (layer) {
            case LAYER_CLIMATE -> "climate";
            case LAYER_ELEV -> "elev";
            case LAYER_COVER -> "cover";
            case LAYER_LAKE -> "lake";
            case LAYER_LAKEDEPTH -> "lakedepth";
            case LAYER_VEG -> "veg";
            case LAYER_TREECOVER -> "treecover";
            default -> throw new IllegalArgumentException("layer " + layer);
        };
    }

    public static int layerId(String name) {
        return switch (name) {
            case "climate" -> LAYER_CLIMATE;
            case "elev" -> LAYER_ELEV;
            case "cover" -> LAYER_COVER;
            case "lake" -> LAYER_LAKE;
            case "lakedepth" -> LAYER_LAKEDEPTH;
            case "veg" -> LAYER_VEG;
            case "treecover" -> LAYER_TREECOVER;
            default -> throw new IllegalArgumentException("layer " + name);
        };
    }

    public static int nodata(int layer) {
        return layer == LAYER_ELEV || layer == LAYER_LAKE ? NODATA_ELEV : layer == LAYER_TREECOVER ? 255 : 0;
    }

    public static double degPerPx(int z) {
        return 360.0 / (TILE * (double) (1 << z));
    }

    public static int tilesAcross(int z) {
        return 1 << z;
    }

    public static int tilesDown(int z) {
        return 1 << (z - 1);
    }

    /** Global pixel x at zoom z (fractional), pixel centres at .5. */
    public static double globalPx(int z, double lon) {
        return (lon + 180.0) * TILE * (1 << z) / 360.0;
    }

    /** Global pixel y at zoom z (fractional). */
    public static double globalPy(int z, double lat) {
        return (90.0 - lat) * TILE * (1 << z) / 360.0;
    }

    /** Packs (layer, z, tx, ty) into one long for cache keys. */
    public static long key(int layer, int z, int tx, int ty) {
        return ((long) layer << 56) | ((long) z << 48) | ((long) tx << 24) | (long) ty;
    }
}
