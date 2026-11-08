package com.seth.worldbound;

import java.util.List;
import java.util.Set;

/**
 * Engine extension points filled in by the mod that supplies the data (Earth Data World Gen: streaming downloads
 * and the named-region biomes). Null when nothing fills them; the engine then does without.
 */
public final class Hooks {
    private Hooks() {}

    /** Downloads finer data as chunks generate. */
    public interface Streaming {
        /** A worldgen thread is about to work on the chunk at this block: fetch (and maybe briefly wait for) its data. */
        void chunk(Earth earth, int blockX, int blockZ);

        /** A new world is about to set its spawn here: fetch the data around it first (blocking, bounded). */
        default void prepareSpawn(double lon, double lat, int radiusTiles) {}

        /** One-line state for /earth status. */
        String status();
    }

    /** Named regions that pick their own biome over the climate table (the Grand Canyon, the Amazon...). */
    public interface Regional {
        /** Every biome id this can return (so the biome source can resolve them up front). */
        Set<String> allIds();

        /** Candidates for this column, best first; empty for no regional override. */
        List<String> pick(Earth.Surface s, String base, double patch, double patch2);
    }

    public static volatile Streaming streaming;
    public static volatile Regional regional;
}
