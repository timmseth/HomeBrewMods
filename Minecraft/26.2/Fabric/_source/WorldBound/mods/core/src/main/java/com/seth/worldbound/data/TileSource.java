package com.seth.worldbound.data;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** Where tiles come from. Implementations return null for a tile that does not exist. */
public interface TileSource {
    String name();

    /** null when the tile is absent (that is normal: ocean, outside the pack, not yet streamed). */
    Tile load(int layer, int z, int tx, int ty) throws IOException;

    static String relPath(int layer, int z, int tx, int ty) {
        return "layers/" + Tiling.layerName(layer) + "/" + z + "/" + tx + "_" + ty + ".etile";
    }

    /** A pack directory or the stream cache: <root>/pack.json + <root>/layers/... */
    final class Dir implements TileSource {
        private final Path root;
        private final String name;

        public Dir(Path root, String name) {
            this.root = root;
            this.name = name;
        }

        public Path root() {
            return root;
        }

        @Override public String name() {
            return name;
        }

        @Override public Tile load(int layer, int z, int tx, int ty) throws IOException {
            Path p = root.resolve(relPath(layer, z, tx, ty));
            if (!Files.isRegularFile(p)) return null;
            return EtileCodec.decode(Files.readAllBytes(p));
        }
    }

    /** The base tiles shipped inside the mod jar under /worldbound/base/. */
    final class Jar implements TileSource {
        private final String prefix;

        public Jar(String prefix) {
            this.prefix = prefix.endsWith("/") ? prefix : prefix + "/";
        }

        @Override public String name() {
            return "jar";
        }

        @Override public Tile load(int layer, int z, int tx, int ty) throws IOException {
            try (InputStream in = Jar.class.getResourceAsStream(prefix + relPath(layer, z, tx, ty))) {
                if (in == null) return null;
                return EtileCodec.read(in);
            }
        }
    }

    /** What a pack.json declares. */
    record PackInfo(String name, double[] bounds, Map<String, Integer> layers) {}
}
