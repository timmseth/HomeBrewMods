package com.seth.tatv;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/** config/tatv.json */
public final class TatvConfig {
    /** How high VTOL mode climbs straight up before levelling off. */
    public int vtol_takeoff_blocks = 50;
    /** Tilt the first-person camera with the vehicle on slopes (off by default: can cause motion sickness). */
    public boolean first_person_tilt = false;

    private static TatvConfig INSTANCE;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("tatv.json");
    }

    public static TatvConfig get() {
        if (INSTANCE == null) {
            try {
                INSTANCE = Files.exists(path()) ? GSON.fromJson(Files.readString(path()), TatvConfig.class) : null;
            } catch (Exception e) {
                INSTANCE = null;
            }
            if (INSTANCE == null) INSTANCE = new TatvConfig();
            save();
        }
        return INSTANCE;
    }

    public static void save() {
        try {
            Files.writeString(path(), GSON.toJson(INSTANCE));
        } catch (Exception ignored) {}
    }
}
