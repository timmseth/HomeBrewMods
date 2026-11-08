package com.seth.worldbound.data;

/** One decoded tile. u8 layers are widened into the short[] so one class serves every layer. */
public final class Tile {
    public final int layer, z, tx, ty, w, h, nodata;
    private final short[] px;

    public Tile(int layer, int z, int tx, int ty, int w, int h, int nodata, short[] px) {
        this.layer = layer; this.z = z; this.tx = tx; this.ty = ty; this.w = w; this.h = h;
        this.nodata = nodata; this.px = px;
    }

    /** Value at local pixel (x, y); NODATA outside the tile's own extent. */
    public int get(int x, int y) {
        if (x < 0 || y < 0 || x >= w || y >= h) return nodata;
        return px[y * w + x];
    }

    public boolean isNodata(int v) {
        return v == nodata;
    }

    public int bytes() {
        return px.length * 2 + 64;
    }
}
