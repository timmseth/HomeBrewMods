package com.seth.worldbound;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.nio.file.Path;

/**
 * A kind of world the shared engine can generate, registered by the mod that supplies its data:
 * "earth" (Earth Data World Gen, the real planet) or "earthish" (Earthish World Gen, generated planets).
 * The id matches the {@code planet} field of the world preset's {@code worldbound:raster} biome source.
 */
public interface WorldKind {
    String id();

    /** Data to use when no world of any kind is running (the real Earth supplies this for the spawn picker); null if none. */
    default Earth.Source defaultSource(WorldBoundConfig cfg, Path dataDir) { return null; }

    /**
     * A world of this kind is loading on the server. {@code first} is true the first time this world loads with
     * this kind (the place to fill in {@code w} and save it). Returns the world's data.
     */
    Earth.Source load(MinecraftServer server, ServerLevel level, WorldInfo w, boolean first);

    /** A client joined a remote server running a world of this kind (seed from the server); null to keep the default. */
    default Earth.Source remote(long seed) { return null; }

    /** The world closed: drop anything held for it. */
    default void unload() {}
}
