package com.seth.worldbound.planet;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Consumer;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * The expensive part of a planet (final heights and ice, and the climate), kept in the world's folder so reopening an
 * Earthish world rebuilds only the quick parts. Rivers, lakes and tiles follow from these exactly, so a reloaded
 * planet is the same planet. A file from another seed or generator version is ignored (and replaced).
 */
final class PlanetCache {
    private static final int MAGIC = 0x5742504C; // "WBPL"

    record Saved(float[] elev, byte[] ice, Climate climate, double landFraction, int plates) {}

    private PlanetCache() {}

    static Saved read(Path f, long seed, Consumer<String> log) {
        if (!Files.isRegularFile(f)) return null;
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new InflaterInputStream(Files.newInputStream(f), new java.util.zip.Inflater(), 1 << 16), 1 << 16))) {
            if (in.readInt() != MAGIC || in.readInt() != Planet.VERSION || in.readLong() != seed) return null;
            if (in.readInt() != Planet.W || in.readInt() != Planet.H || in.readInt() != Planet.CW || in.readInt() != Planet.CH) return null;
            double lf = in.readDouble();
            int plates = in.readInt();
            int n = Planet.W * Planet.H, cn = Planet.CW * Planet.CH;
            float[] elev = new float[n];
            byte[] buf = new byte[n * 2];
            in.readFully(buf);
            java.nio.ByteBuffer bb = java.nio.ByteBuffer.wrap(buf);
            for (int k = 0; k < n; k++) elev[k] = bb.getShort();
            byte[] ice = new byte[n];
            in.readFully(ice);
            byte[] landB = new byte[cn];
            in.readFully(landB);
            boolean[] land = new boolean[cn];
            for (int k = 0; k < cn; k++) land[k] = landB[k] != 0;
            float[][] f6 = new float[6][cn];
            byte[] fb = new byte[cn * 4];
            for (int a = 0; a < 6; a++) {
                in.readFully(fb);
                java.nio.ByteBuffer.wrap(fb).asFloatBuffer().get(f6[a]);
            }
            Climate c = Climate.restore(new Grid(Planet.CW, Planet.CH), land, f6[0], f6[1], f6[2], f6[3], f6[4], f6[5]);
            return new Saved(elev, ice, c, lf, plates);
        } catch (IOException | RuntimeException e) {
            if (log != null) log.accept("Earthish planet cache " + f + " unreadable (" + e + "); rebuilding");
            return null;
        }
    }

    static void write(Path f, Planet p, Consumer<String> log) {
        try {
            Files.createDirectories(f.getParent());
            Path tmp = f.resolveSibling(f.getFileName() + ".tmp");
            try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new DeflaterOutputStream(Files.newOutputStream(tmp), new Deflater(5), 1 << 16), 1 << 16))) {
                out.writeInt(MAGIC);
                out.writeInt(Planet.VERSION);
                out.writeLong(p.seed);
                out.writeInt(Planet.W);
                out.writeInt(Planet.H);
                out.writeInt(Planet.CW);
                out.writeInt(Planet.CH);
                out.writeDouble(p.landFraction);
                out.writeInt(p.plates);
                int n = Planet.W * Planet.H, cn = Planet.CW * Planet.CH;
                java.nio.ByteBuffer bb = java.nio.ByteBuffer.allocate(n * 2);
                for (int k = 0; k < n; k++) bb.putShort((short) Math.max(-32768, Math.min(32767, Math.round(p.elev[k]))));
                out.write(bb.array());
                out.write(p.ice);
                byte[] landB = new byte[cn];
                for (int k = 0; k < cn; k++) landB[k] = (byte) (p.climate.land[k] ? 1 : 0);
                out.write(landB);
                Climate c = p.climate;
                for (float[] a : new float[][]{c.elev, c.t0, c.range, c.map, c.summer, c.cont}) {
                    java.nio.ByteBuffer fb = java.nio.ByteBuffer.allocate(cn * 4);
                    fb.asFloatBuffer().put(a);
                    out.write(fb.array());
                }
            }
            Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException | RuntimeException e) {
            if (log != null) log.accept("Earthish planet cache " + f + " not written: " + e);
        }
    }
}
