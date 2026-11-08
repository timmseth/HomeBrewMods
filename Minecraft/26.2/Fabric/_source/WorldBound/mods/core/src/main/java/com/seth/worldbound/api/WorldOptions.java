package com.seth.worldbound.api;

import com.seth.worldbound.Earth;
import com.seth.worldbound.WorldInfo;
import net.minecraft.server.MinecraftServer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * World-creation options that add-ons put on the Earth spawn screen (a button that cycles through the values).
 * The choice for the next new world lives in config/worldbound.json ({@code new_world_options}); each world keeps
 * its own value in its worldbound.json ({@code options}), and {@link #set} changes it in a running world.
 */
public final class WorldOptions {
    private WorldOptions() {}

    /**
     * @param id       key in the files, e.g. "dino_placement"
     * @param title    shown before the value on the button, e.g. "Dinosaurs"
     * @param values   the stored values, first is the default, e.g. ["accurate", "random"]
     * @param labels   what the button says for each value, e.g. ["Accurate (at their fossil sites)", "Random (anywhere)"]
     * @param tooltip  shown on hover
     * @param earthish whether Earthish worlds use it (they have no spawn screen: the config's choice applies)
     */
    public record Option(String id, String title, List<String> values, List<String> labels, String tooltip, boolean earthish) {
        public String label(String value) {
            int i = values.indexOf(value);
            return title + ": " + labels.get(Math.max(0, i));
        }

        public String next(String value) {
            return values.get((values.indexOf(value) + 1) % values.size());
        }
    }

    private static final List<Option> OPTIONS = new CopyOnWriteArrayList<>();
    private static volatile Map<String, String> current = Map.of();

    public static void register(Option o) {
        OPTIONS.removeIf(x -> x.id().equals(o.id()));
        OPTIONS.add(o);
    }

    public static List<Option> all() {
        return List.copyOf(OPTIONS);
    }

    public static Option option(String id) {
        for (Option o : OPTIONS) if (o.id().equals(id)) return o;
        return null;
    }

    /** The choice for the next new world (from the config, else the option's default). */
    public static String nextWorld(String id) {
        String v = Earth.baseConfig().new_world_options.get(id);
        Option o = option(id);
        if (o == null) return v;
        return v != null && o.values().contains(v) ? v : o.values().get(0);
    }

    public static void setNextWorld(String id, String value) {
        Earth.baseConfig().new_world_options.put(id, value);
        Earth.baseConfig().save();
    }

    /** This world's value (a world without one gets the option's default). */
    public static String get(String id) {
        String v = current.get(id);
        Option o = option(id);
        if (o == null) return v;
        return v != null && o.values().contains(v) ? v : o.values().get(0);
    }

    /** Change it in the running world and save it in the world's worldbound.json. */
    public static void set(MinecraftServer server, String id, String value) {
        WorldInfo w = WorldInfo.forWorld(server, false);
        w.options.put(id, value);
        w.save();
        current = Map.copyOf(w.options);
    }

    /** Called by the engine when a world loads (a fresh world first copies the next-world choices in). */
    public static void worldLoaded(WorldInfo w, boolean fresh) {
        if (!fresh) {   // worlds from before the split kept two of these as their own fields
            w.options.putIfAbsent("landmarks", w.landmarks ? "on" : "off");
            w.options.putIfAbsent("dino_placement", w.dino_placement);
        }
        if (fresh) {
            for (Option o : OPTIONS) if (!w.options.containsKey(o.id()) && (o.earthish() || !"earthish".equals(w.planet)))
                w.options.put(o.id(), nextWorld(o.id()));
            w.save();
        }
        current = Map.copyOf(w.options);
    }

    public static void worldClosed() {
        current = Map.of();
    }
}
