package com.seth.earthish;

import com.seth.worldbound.Earth;
import com.seth.worldbound.WorldBoundMod;
import com.seth.worldbound.WorldInfo;
import com.seth.worldbound.WorldKind;
import com.seth.worldbound.data.RasterStack;
import com.seth.worldbound.data.Rivers;
import com.seth.worldbound.planet.Planet;
import net.fabricmc.api.ModInitializer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.List;

/**
 * Earthish World Gen: whole planets generated from the world seed (plate tectonics, erosion, climate, rivers),
 * built by the WorldBound engine exactly as it builds the real Earth.
 */
public final class EarthishMod implements ModInitializer {
    public static final String ID = "earthish";

    /** A generated planet as the engine's data. */
    public record PlanetData(Planet planet) implements Earth.Source {
        @Override public void register(Earth earth, RasterStack stack, List<String> packs) {
            planet.source.register(stack);
            packs.add("earthish planet " + planet.seed);
        }

        @Override public Rivers rivers() { return planet.source.rivers; }

        @Override public boolean generated() { return true; }

        @Override public long seed() { return planet.seed; }
    }

    /** The running world's planet, or null. */
    public static Planet planet() {
        return Earth.get().source instanceof PlanetData d ? d.planet() : null;
    }

    @Override
    public void onInitialize() {
        WorldBoundMod.init();
        Earth.registerKind(new WorldKind() {
            private PlanetData current;

            @Override public String id() { return "earthish"; }

            @Override public Earth.Source load(MinecraftServer server, ServerLevel level, WorldInfo w, boolean first) {
                // the planet comes from the world seed (kept in worldbound.json from the first load on)
                long seed = first ? level.getSeed() : w.planet_seed;
                Planet planet = Planet.get(seed, WorldBoundMod.LOG::info,
                        server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("worldbound").resolve("planet.bin"));
                if (first) {
                    w.planet = "earthish";
                    w.planet_seed = seed;
                    w.planet_version = Planet.VERSION;
                    if (!w.spawn_set) {
                        // start where the planet suggests, and put the map origin there so the scale is true where you play
                        w.spawn_lat = planet.spawnLat();
                        w.spawn_lon = planet.spawnLon();
                        w.origin_lat = w.spawn_lat;
                        w.origin_lon = w.spawn_lon;
                    }
                    w.save();
                }
                if (current == null || current.planet() != planet) current = new PlanetData(planet);
                return current;
            }

            @Override public Earth.Source remote(long seed) {
                Planet p = Planet.get(seed, WorldBoundMod.LOG::info);
                return current != null && current.planet() == p ? current : (current = new PlanetData(p));
            }

            @Override public void unload() {
                current = null;
                Planet.release();
            }
        });
    }
}
