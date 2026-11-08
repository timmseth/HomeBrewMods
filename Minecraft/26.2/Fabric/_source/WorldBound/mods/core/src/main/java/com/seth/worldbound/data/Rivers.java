package com.seth.worldbound.data;

/** River channels: real ones (rivers.bin from HydroRIVERS) or a generated planet's. Thread-safe. */
public interface Rivers {
    /** The river a point falls in: distance from its centreline, its width and depth in metres. */
    record Hit(double distM, double widthM, double depthM) {
        /** 1 at the centreline, 0 at the bank. */
        public double centreFactor() {
            double h = widthM / 2;
            return h <= 0 ? 0 : Math.max(0, 1 - (distM / h) * (distM / h));
        }
    }

    interface SegmentVisitor {
        /** Return false to stop visiting this cell. */
        boolean visit(float lon1, float lat1, float lon2, float lat2, int widthM);
    }

    /** The widest river whose channel contains (lon, lat), or null. */
    Hit query(double lon, double lat);

    /** Visit segments overlapping the bbox, widest first per cell, skipping those narrower than minWidthM. */
    void forEach(double west, double south, double east, double north, int minWidthM, SegmentVisitor v);
}
