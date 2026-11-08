package com.seth.fossilrecord;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.seth.fossilrecord.dino.DinoSpecies;
import com.seth.worldbound.Earth;
import com.seth.worldbound.WorldBoundMod;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fossil Record's shared part: every dinosaur (bodies, looks, behaviour, taming and eggs), kangaroos and green
 * anacondas. Where they live comes from {@link Homes}, supplied by Earth Fossil Record (fossil sites and real
 * ranges) or Earthish Fossil Record (home ranges on a generated planet); with neither for the running world,
 * nothing is placed (eggs and /earth fauna spawn still work).
 */
public final class FossilRecord {
    public static final String ID = "fossilrecord";
    public static final Logger LOG = LoggerFactory.getLogger("Fossil Record");

    private FossilRecord() {}

    /** config/fossilrecord.json */
    public static final class Config {
        public boolean enabled = true;
        /** Multiplies how often they are placed (0.5 = half as many, 2 = twice). */
        public double spawn_scale = 1.0;
        /** Dinosaurs placed in the world (where: each world's dino_placement). Eggs and /earth fauna spawn work either way. */
        public boolean dinosaurs = true;
        /** Developer commands (/earth fauna selftest, dinotest, dinospeed). Also -Dworldbound.debug=true. */
        public boolean debug_commands = false;
    }

    private static Config config = new Config();

    public static Config config() { return config; }

    /** Where the animals live on one kind of world (accurate placement). */
    public interface Homes {
        /** The world kind this covers: "earth" or "earthish". */
        String kind();

        /** Could this dinosaur live here (its sites, its home ranges), before the ground check? */
        boolean dino(DinoSpecies s, double lat, double lon);

        /** The ground check's extra: the column's climate suits it (generated planets care; Earth's sites already do). */
        default boolean dinoClimate(DinoSpecies s, Earth.Surface c) { return true; }

        /** Where to find it: {lat, lon} and a label for each home, for /earth fauna where. */
        List<Map.Entry<String, double[]>> dinoHomes(DinoSpecies s);

        boolean kangaroo(Earth.Surface c);

        boolean anaconda(Earth.Surface c);

        /** For /earth fauna list's last line (where the other animals live). */
        String othersLine();
    }

    private static final Map<String, Homes> HOMES = new ConcurrentHashMap<>();

    public static void registerHomes(Homes h) { HOMES.put(h.kind(), h); }

    /** The homes for the running world, or null (nothing installed for this kind of world). */
    public static Homes homes() {
        if (!Earth.earthWorld()) return null;
        return HOMES.get(Earth.get().generated() ? "earthish" : "earth");
    }

    public static boolean debug() {
        return config.debug_commands || com.seth.worldbound.api.EarthCommandsApi.debug();
    }

    private static boolean initialised;

    /** Register everything once (both Fossil Record jars call this). */
    public static synchronized void init() {
        if (initialised) return;
        initialised = true;
        WorldBoundMod.init();
        loadConfig(FabricLoader.getInstance().getConfigDir().resolve("fossilrecord.json"));
        Fauna.register();
        FaunaCommands.register();
        com.seth.fossilrecord.dino.Dinos.registerOption();
    }

    private static void loadConfig(Path f) {
        Gson g = new GsonBuilder().setPrettyPrinting().create();
        try {
            if (Files.isRegularFile(f)) {
                Config c = g.fromJson(Files.readString(f), Config.class);
                if (c != null) config = c;
            } else {
                // first run after the split: carry over the "fauna" section of config/worldbound.json
                Path old = f.resolveSibling("worldbound.json");
                if (Files.isRegularFile(old)) {
                    var o = com.google.gson.JsonParser.parseString(Files.readString(old)).getAsJsonObject();
                    if (o.has("fauna")) {
                        Config c = g.fromJson(o.get("fauna"), Config.class);
                        if (c != null) config = c;
                    }
                }
            }
            Files.writeString(f, g.toJson(config));
        } catch (Exception e) {
            LOG.warn("config {}: {} (using defaults)", f, e.toString());
        }
    }
}
