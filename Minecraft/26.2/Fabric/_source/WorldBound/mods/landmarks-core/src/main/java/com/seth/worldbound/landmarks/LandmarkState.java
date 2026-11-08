package com.seth.worldbound.landmarks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.seth.worldbound.WorldBoundMod;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

/** {@code <world>/worldbound_landmarks.json}: where each obelisk was built and who has discovered which. */
public final class LandmarkState {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** landmark index -> {x, y, z} of the obelisk's centre at platform level. */
    public Map<Integer, int[]> placed = new HashMap<>();
    /** player UUID -> discovered landmark indices. */
    public Map<String, TreeSet<Integer>> discovered = new HashMap<>();
    /** player UUID -> places they've discovered or travelled to, as {lat, lon}: map travel reaches anywhere near one. */
    public Map<String, java.util.List<double[]>> anchors = new HashMap<>();
    /** player UUID -> discovered city names (name|country). */
    public Map<String, java.util.TreeSet<String>> cities = new HashMap<>();
    private transient Path file;
    private transient boolean dirty;

    public static LandmarkState load(Path f) {
        LandmarkState s = null;
        try {
            s = com.seth.worldbound.util.SafeFiles.read(f, t -> GSON.fromJson(t, LandmarkState.class));
        } catch (Exception e) {
            // Keep the unreadable file for repair instead of overwriting it with an empty state.
            try {
                Files.move(f, f.resolveSibling(f.getFileName() + ".corrupt-" + System.currentTimeMillis()));
            } catch (Exception ignored) {
            }
            WorldBoundMod.LOG.error("landmarks {}: {} (kept as .corrupt-*; starting fresh)", f, e.toString());
        }
        if (s == null) s = new LandmarkState();
        if (s.placed == null) s.placed = new HashMap<>();
        if (s.discovered == null) s.discovered = new HashMap<>();
        if (s.anchors == null) s.anchors = new HashMap<>();
        if (s.cities == null) s.cities = new HashMap<>();
        s.file = f;
        return s;
    }

    public TreeSet<Integer> of(String uuid) {
        return discovered.computeIfAbsent(uuid, k -> new TreeSet<>());
    }

    public java.util.List<double[]> anchorsOf(String uuid) {
        return anchors.computeIfAbsent(uuid, k -> new java.util.ArrayList<>());
    }

    public java.util.TreeSet<String> citiesOf(String uuid) {
        return cities.computeIfAbsent(uuid, k -> new java.util.TreeSet<>());
    }

    public void markDirty() {
        dirty = true;
    }

    public void saveIfDirty() {
        if (!dirty || file == null) return;
        dirty = false;
        try {
            com.seth.worldbound.util.SafeFiles.writeAtomic(file, GSON.toJson(this));
        } catch (Exception e) {
            WorldBoundMod.LOG.warn("landmarks {}: {}", file, e.toString());
        }
    }
}
