package com.seth.worldbound.data;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * Minimal Cloud-Optimized GeoTIFF reader: classic and BigTIFF, tiled, one band, uncompressed / Deflate / LZW,
 * predictors 1–3, overviews, EPSG:4326 geo tags. Reads only the byte ranges it needs (HTTP Range or a local file),
 * which is what makes streaming 30 m terrain practical.
 */
public final class Cog {
    /** Where the bytes come from. */
    public interface Source {
        byte[] read(long offset, int length) throws IOException;
        String name();
    }

    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL).build();

    public static Source http(String url) {
        return new Source() {
            @Override public byte[] read(long offset, int length) throws IOException {
                HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30))
                        .header("Range", "bytes=" + offset + "-" + (offset + length - 1))
                        .header("User-Agent", "WorldBound-Minecraft-mod").GET().build();
                try {
                    HttpResponse<byte[]> r = HTTP.send(req, HttpResponse.BodyHandlers.ofByteArray());
                    // Missing key: S3 answers 404, or 403 when the bucket doesn't allow listing. A 403 from anything
                    // else (a proxy, a firewall) is a failure, not "this tile doesn't exist".
                    if (r.statusCode() == 404 || (r.statusCode() == 403 && r.headers().firstValue("x-amz-request-id").isPresent()))
                        throw new FileNotFoundException(url);
                    if (r.statusCode() == 416) return new byte[0];
                    if (r.statusCode() / 100 != 2) throw new IOException("HTTP " + r.statusCode() + " " + url);
                    byte[] b = r.body();
                    if (r.statusCode() == 200 && b.length > length) { // server ignored Range
                        byte[] c = new byte[(int) Math.min(length, Math.max(0, b.length - offset))];
                        System.arraycopy(b, (int) offset, c, 0, c.length);
                        return c;
                    }
                    return b;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("interrupted");
                }
            }

            @Override public String name() {
                return url;
            }
        };
    }

    public static Source file(java.nio.file.Path p) {
        return new Source() {
            @Override public byte[] read(long offset, int length) throws IOException {
                if (!java.nio.file.Files.isRegularFile(p)) throw new FileNotFoundException(p.toString());
                try (RandomAccessFile f = new RandomAccessFile(p.toFile(), "r")) {
                    long n = Math.max(0, Math.min(length, f.length() - offset));
                    byte[] b = new byte[(int) n];
                    f.seek(offset);
                    f.readFully(b);
                    return b;
                }
            }

            @Override public String name() {
                return p.toString();
            }
        };
    }

    /** One resolution level (full image or an overview). */
    public static final class Level {
        public int width, height, tileW, tileH, bits = 8, format = 1, compression = 1, predictor = 1;
        long[] offsets, counts;
        int tilesAcross;
    }

    private final Source src;
    private final ByteOrder order;
    private final boolean big;
    private byte[] head;
    public final List<Level> levels = new ArrayList<>();
    /** Geo: lon/lat of the top-left corner of pixel (0,0) and degrees per pixel at full resolution. */
    public double originX, originY, scaleX, scaleY;
    public Double nodata;

    public Cog(Source src) throws IOException {
        this.src = src;
        head = src.read(0, 65536);
        if (head.length < 16) throw new IOException("not a TIFF: " + src.name());
        order = head[0] == 'I' ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
        ByteBuffer b = buf(head);
        int ver = b.getShort(2) & 0xFFFF;
        big = ver == 43;
        if (ver != 42 && ver != 43) throw new IOException("not a TIFF (" + ver + "): " + src.name());
        long ifd = big ? b.getLong(8) : b.getInt(4) & 0xFFFFFFFFL;
        boolean first = true;
        while (ifd != 0) {
            ifd = readIfd(ifd, first);
            first = false;
        }
        if (levels.isEmpty()) throw new IOException("no image: " + src.name());
    }

    private ByteBuffer buf(byte[] a) {
        return ByteBuffer.wrap(a).order(order);
    }

    private byte[] bytes(long off, int len) throws IOException {
        if (off + len <= head.length) {
            byte[] r = new byte[len];
            System.arraycopy(head, (int) off, r, 0, len);
            return r;
        }
        return src.read(off, len);
    }

    private static int typeSize(int t) {
        return switch (t) {
            case 1, 2, 6, 7 -> 1;
            case 3, 8 -> 2;
            case 4, 9, 11, 13 -> 4;
            case 5, 10, 12, 16, 17, 18 -> 8;
            default -> 1;
        };
    }

    private long readIfd(long off, boolean first) throws IOException {
        int entrySize = big ? 20 : 12;
        ByteBuffer cnt = buf(bytes(off, big ? 8 : 2));
        long n = big ? cnt.getLong(0) : cnt.getShort(0) & 0xFFFF;
        int hdr = big ? 8 : 2;
        ByteBuffer e = buf(bytes(off + hdr, (int) n * entrySize + (big ? 8 : 4)));
        Level lv = new Level();
        boolean mask = false;
        double[] scale = null, tie = null;
        int[] geoKeys = null;
        for (int i = 0; i < n; i++) {
            int p = i * entrySize;
            int tag = e.getShort(p) & 0xFFFF, type = e.getShort(p + 2) & 0xFFFF;
            long count = big ? e.getLong(p + 4) : e.getInt(p + 4) & 0xFFFFFFFFL;
            int valPos = p + (big ? 12 : 8);
            int inlineMax = big ? 8 : 4;
            long bytesNeeded = count * typeSize(type);
            ByteBuffer vb;
            int vbo;
            if (bytesNeeded <= inlineMax) {
                vb = e;
                vbo = valPos;
            } else {
                long vo = big ? e.getLong(valPos) : e.getInt(valPos) & 0xFFFFFFFFL;
                vb = buf(bytes(vo, (int) bytesNeeded));
                vbo = 0;
            }
            switch (tag) {
                case 254 -> mask = (num(vb, vbo, type, 0) & 4) != 0;
                case 256 -> lv.width = (int) num(vb, vbo, type, 0);
                case 257 -> lv.height = (int) num(vb, vbo, type, 0);
                case 258 -> lv.bits = (int) num(vb, vbo, type, 0);
                case 259 -> lv.compression = (int) num(vb, vbo, type, 0);
                case 317 -> lv.predictor = (int) num(vb, vbo, type, 0);
                case 322 -> lv.tileW = (int) num(vb, vbo, type, 0);
                case 323 -> lv.tileH = (int) num(vb, vbo, type, 0);
                case 324 -> lv.offsets = nums(vb, vbo, type, (int) count);
                case 325 -> lv.counts = nums(vb, vbo, type, (int) count);
                case 339 -> lv.format = (int) num(vb, vbo, type, 0);
                case 33550 -> scale = doubles(vb, vbo, (int) count);
                case 33922 -> tie = doubles(vb, vbo, (int) count);
                case 34735 -> {
                    geoKeys = new int[(int) count];
                    for (int k = 0; k < count; k++) geoKeys[k] = (int) num(vb, vbo, type, k);
                }
                case 42113 -> {
                    byte[] s = new byte[(int) count];
                    for (int k = 0; k < count; k++) s[k] = vb.get(vbo + k);
                    String str = new String(s).trim().replace("\0", "");
                    try { nodata = Double.parseDouble(str); } catch (NumberFormatException ignored) {}
                }
                default -> {}
            }
        }
        long next = big ? e.getLong((int) n * entrySize) : e.getInt((int) n * entrySize) & 0xFFFFFFFFL;
        if (!mask && lv.tileW > 0 && lv.offsets != null) {
            lv.tilesAcross = (lv.width + lv.tileW - 1) / lv.tileW;
            levels.add(lv);
        }
        if (first && scale != null && tie != null) {
            scaleX = scale[0];
            scaleY = scale[1];
            originX = tie[3] - tie[0] * scaleX;
            originY = tie[4] + tie[1] * scaleY;
            // GTRasterTypeGeoKey (1025) = 2: tie point is a pixel centre, not its corner.
            if (geoKeys != null) {
                for (int k = 4; k + 3 < geoKeys.length; k += 4) {
                    if (geoKeys[k] == 1025 && geoKeys[k + 1] == 0 && geoKeys[k + 3] == 2) {
                        originX -= scaleX / 2;
                        originY += scaleY / 2;
                    }
                }
            }
        }
        return next;
    }

    private static long num(ByteBuffer b, int off, int type, int i) {
        return switch (type) {
            case 3, 8 -> b.getShort(off + i * 2) & 0xFFFF;
            case 4, 9, 13 -> b.getInt(off + i * 4) & 0xFFFFFFFFL;
            case 16, 17, 18 -> b.getLong(off + i * 8);
            default -> b.get(off + i) & 0xFF;
        };
    }

    private static long[] nums(ByteBuffer b, int off, int type, int n) {
        long[] r = new long[n];
        for (int i = 0; i < n; i++) r[i] = num(b, off, type, i);
        return r;
    }

    private static double[] doubles(ByteBuffer b, int off, int n) {
        double[] r = new double[n];
        for (int i = 0; i < n; i++) r[i] = b.getDouble(off + i * 8);
        return r;
    }

    /** Degrees per pixel at a level. */
    public double levelScaleX(int level) {
        return scaleX * levels.get(0).width / (double) levels.get(level).width;
    }

    public double levelScaleY(int level) {
        return scaleY * levels.get(0).height / (double) levels.get(level).height;
    }

    /** The coarsest level still at least as fine as {@code degPerPx} (with 10 % slack). */
    public int levelFor(double degPerPx) {
        int best = 0;
        for (int i = 1; i < levels.size(); i++) if (levelScaleX(i) <= degPerPx * 1.1) best = i;
        return best;
    }

    /** Decode one internal tile to floats (row-major tileW x tileH); null if the tile is absent (sparse COG). */
    public float[] tile(int level, int tx, int ty) throws IOException {
        Level lv = levels.get(level);
        int idx = ty * lv.tilesAcross + tx;
        if (idx < 0 || idx >= lv.offsets.length || lv.counts[idx] == 0) return null;
        byte[] raw = src.read(lv.offsets[idx], (int) lv.counts[idx]);
        int bps = lv.bits / 8, n = lv.tileW * lv.tileH;
        byte[] data = switch (lv.compression) {
            case 1 -> raw;
            case 8, 32946 -> inflate(raw, n * bps);
            case 5 -> lzw(raw, n * bps);
            default -> throw new IOException("compression " + lv.compression + " unsupported: " + src.name());
        };
        if (data.length < n * bps) {
            byte[] d = new byte[n * bps];
            System.arraycopy(data, 0, d, 0, data.length);
            data = d;
        }
        if (lv.predictor == 2) unpredictHorizontal(data, lv.tileW, lv.tileH, bps);
        if (lv.predictor == 3) data = unpredictFloat(data, lv.tileW, lv.tileH, bps);
        ByteBuffer b = ByteBuffer.wrap(data).order(lv.predictor == 3 ? ByteOrder.BIG_ENDIAN : order);
        float[] out = new float[n];
        for (int i = 0; i < n; i++) {
            out[i] = switch (bps) {
                case 1 -> lv.format == 2 ? b.get(i) : b.get(i) & 0xFF;
                case 2 -> lv.format == 2 ? b.getShort(i * 2) : b.getShort(i * 2) & 0xFFFF;
                case 4 -> lv.format == 3 ? b.getFloat(i * 4) : (float) (lv.format == 2 ? b.getInt(i * 4) : b.getInt(i * 4) & 0xFFFFFFFFL);
                case 8 -> (float) b.getDouble(i * 8);
                default -> Float.NaN;
            };
        }
        return out;
    }

    private static byte[] inflate(byte[] src, int expected) throws IOException {
        Inflater inf = new Inflater();
        inf.setInput(src);
        byte[] out = new byte[expected];
        try {
            int off = 0;
            while (off < expected && !inf.finished()) {
                int k = inf.inflate(out, off, expected - off);
                if (k == 0 && (inf.needsInput() || inf.needsDictionary())) break;
                off += k;
            }
        } catch (DataFormatException e) {
            throw new IOException("deflate: " + e.getMessage());
        } finally {
            inf.end();
        }
        return out;
    }

    /** TIFF LZW (MSB-first codes, early change). */
    static byte[] lzw(byte[] in, int expected) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(expected);
        byte[][] table = new byte[4096][];
        for (int i = 0; i < 256; i++) table[i] = new byte[]{(byte) i};
        int next = 258, width = 9;
        long bitBuf = 0;
        int bits = 0, pos = 0;
        byte[] prev = null;
        while (true) {
            while (bits < width && pos < in.length) {
                bitBuf = (bitBuf << 8) | (in[pos++] & 0xFF);
                bits += 8;
            }
            if (bits < width) break;
            int code = (int) ((bitBuf >> (bits - width)) & ((1 << width) - 1));
            bits -= width;
            if (code == 257) break;
            if (code == 256) {
                next = 258;
                width = 9;
                prev = null;
                continue;
            }
            byte[] entry;
            if (code < next && table[code] != null) entry = table[code];
            else if (prev != null) {
                entry = new byte[prev.length + 1];
                System.arraycopy(prev, 0, entry, 0, prev.length);
                entry[prev.length] = prev[0];
            } else throw new IOException("bad LZW stream");
            out.write(entry, 0, entry.length);
            if (prev != null && next < 4096) {
                byte[] add = new byte[prev.length + 1];
                System.arraycopy(prev, 0, add, 0, prev.length);
                add[prev.length] = entry[0];
                table[next++] = add;
            }
            prev = entry;
            if (next + 1 >= (1 << width) && width < 12) width++;
            if (out.size() >= expected) break;
        }
        return out.toByteArray();
    }

    private void unpredictHorizontal(byte[] d, int w, int h, int bps) {
        ByteBuffer b = ByteBuffer.wrap(d).order(order);
        for (int y = 0; y < h; y++) {
            int row = y * w;
            for (int x = 1; x < w; x++) {
                int i = row + x;
                switch (bps) {
                    case 1 -> d[i] = (byte) (d[i] + d[i - 1]);
                    case 2 -> b.putShort(i * 2, (short) (b.getShort(i * 2) + b.getShort((i - 1) * 2)));
                    case 4 -> b.putInt(i * 4, b.getInt(i * 4) + b.getInt((i - 1) * 4));
                    default -> {}
                }
            }
        }
    }

    /** Floating-point predictor: byte-wise differencing over byte planes (most significant plane first). */
    private static byte[] unpredictFloat(byte[] d, int w, int h, int bps) {
        byte[] out = new byte[d.length];
        int rowBytes = w * bps;
        for (int y = 0; y < h; y++) {
            int r = y * rowBytes;
            for (int i = 1; i < rowBytes; i++) d[r + i] = (byte) (d[r + i] + d[r + i - 1]);
            for (int x = 0; x < w; x++) {
                for (int k = 0; k < bps; k++) out[r + x * bps + k] = d[r + k * w + x];
            }
        }
        return out;
    }
}
