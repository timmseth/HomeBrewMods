package com.seth.worldbound.compat;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.enums.worldGeneration.EDhApiLevelType;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiLevelLoadEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiEventParam;
import com.seth.worldbound.WorldBoundMod;
import com.seth.worldbound.worldgen.EarthBiomeSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/** Only loaded when Distant Horizons is installed. */
public final class DhCompat {
    public static volatile EarthLodGenerator active;

    public static void init() {
        DhApi.events.bind(DhApiLevelLoadEvent.class, new DhApiLevelLoadEvent() {
            @Override
            public void onLevelLoad(DhApiEventParam<EventParam> p) {
                var lw = p.value.levelWrapper;
                if (lw.getLevelType() != EDhApiLevelType.SERVER_LEVEL) return;
                if (!(lw.getWrappedMcObject() instanceof ServerLevel sl) || sl.dimension() != Level.OVERWORLD) return;
                if (!(sl.getChunkSource().getGenerator().getBiomeSource() instanceof EarthBiomeSource)) return;
                if (!com.seth.worldbound.Earth.get().config.dh_lod_generator) return;
                EarthLodGenerator g = new EarthLodGenerator(lw);
                var r = DhApi.worldGenOverrides.registerWorldGeneratorOverride(lw, g);
                active = g;
                WorldBoundMod.LOG.info("WorldBound: Distant Horizons LODs come straight from Earth data ({})", r.success ? "registered" : r.message);
            }
        });
        WorldBoundMod.LOG.info("WorldBound: Distant Horizons found");
    }

    public static String status() {
        EarthLodGenerator g = active;
        long n = EarthLodGenerator.columns.get(), l = EarthLodGenerator.lods.get();
        return String.format(java.util.Locale.ROOT, "DH LOD generator %s: %d LODs, %d columns, %.1f µs/column",
                g != null ? "active" : "not registered (no DH server level yet)", l, n, n == 0 ? 0 : EarthLodGenerator.nanos.get() / 1000.0 / n);
    }

    /** Builds one LOD at each of a few detail levels around a place and checks every column the way DH does. */
    public static String test(double lat, double lon) {
        EarthLodGenerator g = active;
        if (g == null) return "DH LOD generator not registered";
        var e = com.seth.worldbound.Earth.get();
        int bx = (int) Math.floor(e.transform.blockX(lon)), bz = (int) Math.floor(e.transform.blockZ(lat));
        StringBuilder sb = new StringBuilder();
        try {
            java.lang.reflect.Method validate = Class.forName("com.seibel.distanthorizons.core.dataObjects.transformers.LodDataBuilder")
                    .getMethod("validateOrThrowApiDataColumn", java.util.List.class);
            for (int detail : new int[]{0, 3, 6, 9}) {
                TestSource ts = new TestSource(64);
                long t0 = System.nanoTime();
                g.fill(bx - (32 << detail), bz - (32 << detail), detail, ts);
                long us = (System.nanoTime() - t0) / 1000;
                int bad = 0, pts = 0;
                String first = null;
                for (var col : ts.cols) {
                    pts += col.size();
                    var copy = new java.util.ArrayList<>(col);
                    java.util.Collections.reverse(copy);   // DH validates top-down
                    try { validate.invoke(null, copy); } catch (java.lang.reflect.InvocationTargetException ex) {
                        bad++;
                        if (first == null) first = ex.getCause().getMessage();
                    }
                }
                var mid = ts.cols.get(32 * 64 + 32);
                var topPt = mid.get(mid.size() - 2);
                sb.append(String.format(java.util.Locale.ROOT, "detail %d: %d µs, %d pts, %d bad%s, centre top %s y%d; ", detail, us, pts, bad,
                        first == null ? "" : " (" + first + ")", topPt.blockStateWrapper.getSerialString(), topPt.topYBlockPos + e.curve.minY - 1));
            }
            // contention: 12 threads at once, like DH's pool
            for (int detail : new int[]{0, 5}) {
                int threads = 12, per = 3;
                java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(threads);
                long t0 = System.nanoTime();
                java.util.List<java.util.concurrent.Future<?>> fs = new java.util.ArrayList<>();
                for (int k = 0; k < threads * per; k++) {
                    int ox = bx + (k % 6) * (64 << detail), oz = bz + (k / 6) * (64 << detail);
                    fs.add(pool.submit(() -> g.fill(ox, oz, detail, new TestSource(64))));
                }
                for (var f : fs) f.get();
                pool.shutdown();
                double s = (System.nanoTime() - t0) / 1e9;
                sb.append(String.format(java.util.Locale.ROOT, "parallel detail %d: %d LODs in %.2f s (%.1f µs/column); ", detail, threads * per, s,
                        s * 1e6 / (threads * per * 4096.0)));
            }
        } catch (Exception ex) {
            return "lod test failed: " + ex;
        }
        return sb.toString();
    }

    private static final class TestSource implements com.seibel.distanthorizons.api.objects.data.IDhApiFullDataSource {
        final int w;
        final java.util.List<java.util.List<com.seibel.distanthorizons.api.objects.data.DhApiTerrainDataPoint>> cols = new java.util.ArrayList<>();
        TestSource(int w) {
            this.w = w;
            for (int i = 0; i < w * w; i++) cols.add(null);
        }
        public int getWidthInDataColumns() { return w; }
        public java.util.List<com.seibel.distanthorizons.api.objects.data.DhApiTerrainDataPoint> setApiDataPointColumn(int x, int z,
                com.seibel.distanthorizons.api.enums.worldGeneration.EDhApiWorldGenerationStep step,
                java.util.List<com.seibel.distanthorizons.api.objects.data.DhApiTerrainDataPoint> l) {
            cols.set(z * w + x, new java.util.ArrayList<>(l));
            return l;
        }
        public java.util.List<com.seibel.distanthorizons.api.objects.data.DhApiTerrainDataPoint> getApiDataPointColumn(int x, int z) { return cols.get(z * w + x); }
    }
}
