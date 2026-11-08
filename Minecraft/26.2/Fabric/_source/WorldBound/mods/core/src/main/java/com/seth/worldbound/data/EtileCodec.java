package com.seth.worldbound.data;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * The .etile container — mirrors tools/etile.py, keep in sync.
 *
 * <pre>
 * magic "ETIL" | version u8=1 | layer u8 | dtype u8 (1=u8, 2=i16le) | z u8
 * tx u16 | ty u16 | w u16 | h u16 | nodata i16 | zlib(payload)
 * </pre>
 */
public final class EtileCodec {
    public static final int HEADER = 18;
    private static final int MAGIC = 0x4C495445; // "ETIL" little-endian

    private EtileCodec() {}

    public static Tile read(InputStream in) throws IOException {
        return decode(in.readAllBytes());
    }

    public static Tile decode(byte[] data) throws IOException {
        if (data.length < HEADER) throw new IOException("etile: short header");
        ByteBuffer b = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        if (b.getInt() != MAGIC) throw new IOException("etile: bad magic");
        int version = b.get() & 0xFF;
        if (version != 1) throw new IOException("etile: version " + version);
        int layer = b.get() & 0xFF;
        int dtype = b.get() & 0xFF;
        int z = b.get() & 0xFF;
        int tx = b.getShort() & 0xFFFF;
        int ty = b.getShort() & 0xFFFF;
        int w = b.getShort() & 0xFFFF;
        int h = b.getShort() & 0xFFFF;
        int nodata = b.getShort();
        int n = w * h;
        byte[] raw = inflate(data, HEADER, data.length - HEADER, n * (dtype == 1 ? 1 : 2));
        short[] px = new short[n];
        if (dtype == 1) {
            for (int i = 0; i < n; i++) px[i] = (short) (raw[i] & 0xFF);
        } else {
            ByteBuffer rb = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN);
            for (int i = 0; i < n; i++) px[i] = rb.getShort();
        }
        return new Tile(layer, z, tx, ty, w, h, nodata, px);
    }

    /** Encoder, used by tests and by the stream cache when it writes tiles it fetched. */
    public static byte[] encode(Tile t) {
        boolean u8 = t.layer != Tiling.LAYER_ELEV && t.layer != Tiling.LAYER_LAKE;
        int n = t.w * t.h;
        byte[] raw = new byte[n * (u8 ? 1 : 2)];
        ByteBuffer rb = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN);
        for (int y = 0; y < t.h; y++) {
            for (int x = 0; x < t.w; x++) {
                int v = t.get(x, y);
                if (u8) rb.put((byte) v); else rb.putShort((short) v);
            }
        }
        Deflater d = new Deflater(6);
        d.setInput(raw);
        d.finish();
        ByteArrayOutputStream out = new ByteArrayOutputStream(raw.length / 4 + HEADER);
        ByteBuffer hb = ByteBuffer.allocate(HEADER).order(ByteOrder.LITTLE_ENDIAN);
        hb.putInt(MAGIC).put((byte) 1).put((byte) t.layer).put((byte) (u8 ? 1 : 2)).put((byte) t.z)
          .putShort((short) t.tx).putShort((short) t.ty).putShort((short) t.w).putShort((short) t.h)
          .putShort((short) t.nodata);
        out.write(hb.array(), 0, HEADER);
        byte[] buf = new byte[1 << 16];
        while (!d.finished()) {
            int k = d.deflate(buf);
            out.write(buf, 0, k);
        }
        d.end();
        return out.toByteArray();
    }

    private static byte[] inflate(byte[] src, int off, int len, int expected) throws IOException {
        Inflater inf = new Inflater();
        inf.setInput(src, off, len);
        byte[] out = new byte[expected];
        try {
            int got = 0;
            while (got < expected && !inf.finished()) {
                int k = inf.inflate(out, got, expected - got);
                if (k == 0 && (inf.needsInput() || inf.needsDictionary())) break;
                got += k;
            }
            if (got != expected) throw new IOException("etile: payload " + got + " of " + expected + " bytes");
        } catch (DataFormatException e) {
            throw new IOException("etile: corrupt payload", e);
        } finally {
            inf.end();
        }
        return out;
    }
}
