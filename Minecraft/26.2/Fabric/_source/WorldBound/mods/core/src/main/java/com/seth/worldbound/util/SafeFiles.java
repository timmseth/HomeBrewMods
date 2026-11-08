package com.seth.worldbound.util;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Small-file persistence that survives crashes and transient locks: write to a temp file and move it into place
 * atomically, keeping the previous good version as {@code <name>.bak}; read with retries and fall back to the backup.
 */
public final class SafeFiles {
    private SafeFiles() {}

    public static void writeAtomic(Path file, String content) throws IOException {
        Path dir = file.getParent();
        if (dir != null) Files.createDirectories(dir);
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(tmp, content);
        if (Files.isRegularFile(file) && Files.size(file) > 0) {
            try {
                Files.copy(file, file.resolveSibling(file.getFileName() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {
                // a backup is nice to have, not a reason to fail the save
            }
        }
        try {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** The parsed file, else its backup; null when neither exists. Throws when a file exists but can't be read. */
    public static <T> T read(Path file, java.util.function.Function<String, T> parse) throws IOException {
        IOException last = null;
        for (Path p : new Path[]{file, file.resolveSibling(file.getFileName() + ".bak")}) {
            if (!Files.isRegularFile(p)) continue;
            for (int attempt = 0; attempt < 3; attempt++) {
                try {
                    String s = Files.readString(p);
                    if (s.isBlank()) throw new IOException("empty file " + p);
                    T v = parse.apply(s);
                    if (v != null) return v;
                    throw new IOException("unreadable " + p);
                } catch (IOException | RuntimeException e) {
                    last = e instanceof IOException io ? io : new IOException(p + ": " + e, e);
                    try { Thread.sleep(150); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                }
            }
        }
        if (last != null) throw last;
        return null;
    }
}
