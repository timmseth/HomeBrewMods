package com.seth.worldbound.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * rivers.bin from tools/build_rivers.py (HydroRIVERS): river segments bucketed into a 0.25° grid,
 * widest first within each cell. Memory-mapped; thread-safe (absolute reads only).
 */
public final class RiverIndex implements Rivers {
    private static final int HEADER = 24, REC = 20;

    private final MappedByteBuffer buf;
    private final double cellDeg;
    private final int cols, rows;
    private final long recBase;

    public RiverIndex(Path file) throws IOException {
        try (FileChannel ch = FileChannel.open(file, StandardOpenOption.READ)) {
            buf = ch.map(FileChannel.MapMode.READ_ONLY, 0, ch.size());
        }
        buf.order(ByteOrder.LITTLE_ENDIAN);
        if (buf.getInt(0) != 0x56524257) throw new IOException("rivers.bin: bad magic"); // "WBRV"
        if (buf.getInt(4) != 1) throw new IOException("rivers.bin: version " + buf.getInt(4));
        cellDeg = buf.getFloat(8);
        cols = buf.getInt(12);
        rows = buf.getInt(16);
        recBase = HEADER + 8L * ((long) cols * rows + 1);
    }

    private long offset(int cell) {
        return buf.getLong(HEADER + 8 * cell);
    }

    private int cellOf(double lon, double lat) {
        int c = (int) Math.floor((lon + 180) / cellDeg), r = (int) Math.floor((90 - lat) / cellDeg);
        if (c < 0 || c >= cols || r < 0 || r >= rows) return -1;
        return r * cols + c;
    }

    /** The widest river whose channel contains (lon, lat), or null. */
    @Override
    public Hit query(double lon, double lat) {
        int cell = cellOf(lon, lat);
        if (cell < 0) return null;
        long a = offset(cell), b = offset(cell + 1);
        double kx = 111_320.0 * Math.cos(Math.toRadians(lat)), ky = 110_574.0;
        for (long i = a; i < b; i++) {
            int p = (int) (recBase + i * REC);
            double x1 = (buf.getFloat(p) - lon) * kx, y1 = (buf.getFloat(p + 4) - lat) * ky;
            double x2 = (buf.getFloat(p + 8) - lon) * kx, y2 = (buf.getFloat(p + 12) - lat) * ky;
            double w = buf.getShort(p + 16) & 0xFFFF;
            double dx = x2 - x1, dy = y2 - y1, len2 = dx * dx + dy * dy;
            double t = len2 == 0 ? 0 : Math.max(0, Math.min(1, -(x1 * dx + y1 * dy) / len2));
            double px = x1 + t * dx, py = y1 + t * dy;
            double d = Math.sqrt(px * px + py * py);
            // records are widest first, so the first channel we are inside is the widest one here
            if (d <= w / 2) return new Hit(d, w, (buf.getShort(p + 18) & 0xFFFF) / 10.0);
        }
        return null;
    }

    /** Visit segments in cells overlapping the bbox, widest first per cell, skipping those narrower than minWidthM. */
    @Override
    public void forEach(double west, double south, double east, double north, int minWidthM, SegmentVisitor v) {
        int c0 = Math.max(0, (int) Math.floor((west + 180) / cellDeg)), c1 = Math.min(cols - 1, (int) Math.floor((east + 180) / cellDeg));
        int r0 = Math.max(0, (int) Math.floor((90 - north) / cellDeg)), r1 = Math.min(rows - 1, (int) Math.floor((90 - south) / cellDeg));
        for (int r = r0; r <= r1; r++) {
            for (int c = c0; c <= c1; c++) {
                int cell = r * cols + c;
                long a = offset(cell), b = offset(cell + 1);
                for (long i = a; i < b; i++) {
                    int p = (int) (recBase + i * REC);
                    int w = buf.getShort(p + 16) & 0xFFFF;
                    if (w < minWidthM) break;
                    if (!v.visit(buf.getFloat(p), buf.getFloat(p + 4), buf.getFloat(p + 8), buf.getFloat(p + 12), w)) break;
                }
            }
        }
    }
}
