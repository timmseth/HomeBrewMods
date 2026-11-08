package com.seth.earthdata;

import com.google.gson.Gson;
import com.seth.worldbound.Earth;
import com.seth.worldbound.WorldBoundMod;
import com.seth.worldbound.data.RasterStack;
import com.seth.worldbound.data.RiverIndex;
import com.seth.worldbound.data.Rivers;
import com.seth.worldbound.data.Streamer;
import com.seth.worldbound.data.TileSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * The real Earth's data: the coarse global base shipped in this jar, region packs and the streamed-tile cache in
 * the data folder (worldbound/packs, worldbound/cache), and HydroRIVERS (worldbound/rivers.bin).
 */
final class RealEarth implements Earth.Source {
    /** One shared instance: every Earth world reads the same data. */
    static final RealEarth INSTANCE = new RealEarth();

    private Rivers rivers;
    private boolean riversLoaded;

    @Override
    public void register(Earth earth, RasterStack stack, List<String> packs) {
        loadJarBase(earth, packs);
        loadPacks(earth, packs);
        synchronized (this) {
            if (!riversLoaded) {
                riversLoaded = true;
                Path rf = earth.dataDir.resolve("rivers.bin");
                if (Files.isRegularFile(rf)) {
                    try {
                        rivers = new RiverIndex(rf);
                        WorldBoundMod.LOG.info("rivers: {}", rf);
                    } catch (IOException | RuntimeException e) {
                        WorldBoundMod.LOG.warn("rivers {}: {}", rf, e.toString());
                    }
                }
            }
        }
        if (rivers != null) packs.add("rivers");
    }

    @Override
    public Rivers rivers() {
        return rivers;
    }

    private static void loadJarBase(Earth earth, List<String> packs) {
        try (InputStream in = RealEarth.class.getResourceAsStream("/worldbound/base/pack.json")) {
            if (in == null) {
                WorldBoundMod.LOG.warn("no base tiles in the jar (run tools/build_base.py); the world will be ocean where no pack covers it");
                return;
            }
            TileSource.PackInfo info = new Gson().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), TileSource.PackInfo.class);
            earth.register(info, new TileSource.Jar("/worldbound/base"));
            packs.add("jar base");
        } catch (IOException e) {
            WorldBoundMod.LOG.warn("jar base: {}", e.toString());
        }
    }

    private static void loadPacks(Earth earth, List<String> packs) {
        Path packsDir = earth.dataDir.resolve("packs");
        Path cacheDir = earth.dataDir.resolve("cache");
        try {
            Files.createDirectories(packsDir);
            Files.createDirectories(cacheDir);
        } catch (IOException e) {
            WorldBoundMod.LOG.warn("data dir {}: {}", earth.dataDir, e.toString());
            return;
        }
        List<Path> roots = new ArrayList<>();
        try (Stream<Path> s = Files.list(packsDir)) {
            s.filter(p -> Files.isRegularFile(p.resolve("pack.json"))).sorted().forEach(roots::add);
        } catch (IOException e) {
            WorldBoundMod.LOG.warn("packs: {}", e.toString());
        }
        if (earth.config.streaming.enabled) {
            Streamer.upgradeCache(cacheDir);
            try {
                Files.writeString(cacheDir.resolve("pack.json"), "{ \"name\": \"stream\", \"bounds\": [-180, -90, 180, 90], \"layers\": { \"elev\": "
                        + Streamer.ELEV_Z + ", \"cover\": " + Streamer.COVER_Z + " } }\n");
            } catch (IOException e) {
                WorldBoundMod.LOG.warn("stream cache {}: {}", cacheDir, e.toString());
            }
        }
        if (Files.isRegularFile(cacheDir.resolve("pack.json"))) roots.add(cacheDir);
        for (Path root : roots) {
            try {
                TileSource.PackInfo info = new Gson().fromJson(Files.readString(root.resolve("pack.json")), TileSource.PackInfo.class);
                earth.register(info, new TileSource.Dir(root, info.name()));
                packs.add(info.name() + " " + info.layers());
                WorldBoundMod.LOG.info("pack {} at {} layers {}", info.name(), root, info.layers());
            } catch (IOException | RuntimeException e) {
                WorldBoundMod.LOG.warn("pack {}: {}", root, e.toString());
            }
        }
    }
}
