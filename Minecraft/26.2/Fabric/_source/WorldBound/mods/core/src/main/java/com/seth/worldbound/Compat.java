package com.seth.worldbound;

import net.fabricmc.loader.api.FabricLoader;

/** Optional-mod integration, kept out of WorldBoundMod so plain unit tests can load that class. */
final class Compat {
    private Compat() {}

    /**
     * Terralith's own licence doesn't allow shipping its surface rules in another mod, so the engine carries no
     * Terralith pack. tools/terralith_compat.py turns the player's own Terralith jar into a data pack they add to
     * their world; this just says so once.
     */
    static void registerPacks(FabricLoader loader) {
        if (loader.isModLoaded("terralith"))
            WorldBoundMod.LOG.info("WorldBound: Terralith installed. For its ground blocks at Earth heights, add the data pack made by tools/terralith_compat.py (see the README).");
    }

    /** Distant Horizons: feed it LODs from Earth data (only when DH is installed). */
    static void initDistantHorizons(FabricLoader loader) {
        if (!loader.isModLoaded("distanthorizons")) return;
        try {
            com.seth.worldbound.compat.DhCompat.init();
        } catch (Throwable t) {
            WorldBoundMod.LOG.warn("WorldBound: Distant Horizons API not usable ({}); DH will generate chunks itself", t.toString());
        }
    }
}
