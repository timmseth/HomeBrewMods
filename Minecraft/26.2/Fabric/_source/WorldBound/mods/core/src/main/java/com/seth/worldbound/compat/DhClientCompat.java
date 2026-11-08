package com.seth.worldbound.compat;

import com.seibel.distanthorizons.api.DhApi;
import com.seth.worldbound.Earth;
import com.seth.worldbound.WorldBoundMod;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Distant Horizons display tweaks for Earth worlds (temporary API values; DH's own settings are untouched and come
 * back when you leave the world).
 *
 * Holes: DH skips drawing everything within the vanilla render distance, assuming Minecraft draws it. Earth worlds are
 * 4,064 blocks tall, so you can easily be farther above the ground than the render distance; Minecraft then draws
 * nothing there and you look through DH's cut-out into the inside of the LODs (caves, lava). When the camera is high
 * above the ground (or the ground below isn't loaded), DH is told to draw right up to the camera.
 *
 * Cave culling: the cut-out also shows the LODs' caves; with culling on (at any height) DH leaves sealed caves out.
 */
public final class DhClientCompat {
    private static boolean overdrawOff, applied, warned;
    private static double lowest = Double.NaN;

    /** Lowest ground in the render circle: Earth data in singleplayer, loaded chunks otherwise (unloaded = very low). */
    private static double lowestGround(Minecraft mc, double x, double z, double rd) {
        double low = Double.MAX_VALUE;
        boolean local = mc.hasSingleplayerServer();
        for (double f : new double[]{0, 0.35, 0.65, 0.95}) {
            int n = f == 0 ? 1 : 12;
            for (int i = 0; i < n; i++) {
                double a = i * Math.PI * 2 / n;
                int bx = (int) Math.floor(x + Math.cos(a) * rd * f), bz = (int) Math.floor(z + Math.sin(a) * rd * f);
                double g;
                if (local) g = Earth.get().at(bx, bz).solidTop();
                else g = mc.level.getChunkSource().hasChunk(bx >> 4, bz >> 4) ? mc.level.getHeight(Heightmap.Types.WORLD_SURFACE, bx, bz) : Earth.get().curve.minY;
                low = Math.min(low, g);
            }
        }
        return low;
    }

    public static void tick(Minecraft mc) {
        try {
            boolean earth = mc.level != null && mc.player != null && Earth.earthWorld() && mc.level.dimension() == Level.OVERWORLD
                    && Earth.baseConfig().dh_display_tweaks;
            var g = DhApi.Delayed.configs == null ? null : DhApi.Delayed.configs.graphics();
            if (g == null) return;
            if (!earth) {
                if (applied) {
                    g.caveCullingEnabled().clearValue();
                    g.caveCullingHeight().clearValue();
                    g.overdrawPreventionRadius().clearValue();
                    applied = overdrawOff = false;
                }
                return;
            }
            if (!applied) {
                g.caveCullingEnabled().setValue(true);
                g.caveCullingHeight().setValue(Earth.get().curve.maxY);
                applied = true;
            }
            var cam = mc.gameRenderer.mainCamera().position();
            double rd = mc.options.getEffectiveRenderDistance() * 16.0;
            if (mc.level.getGameTime() % 10 == 0 || Double.isNaN(lowest)) lowest = lowestGround(mc, cam.x, cam.z, rd);
            // Minecraft doesn't draw terrain much farther below the camera than the render distance, so anywhere the
            // ground within that circle drops that far (high above the land, or on a peak like Fuji's rim) DH must
            // draw up close or you see through the world.
            double above = cam.y - lowest;
            // hysteresis so it doesn't flicker at the boundary
            boolean off = overdrawOff ? above > rd * 0.4 : above > rd * 0.55;
            if (off != overdrawOff) {
                overdrawOff = off;
                if (off) g.overdrawPreventionRadius().setValue(0f);
                else g.overdrawPreventionRadius().clearValue();
            }
        } catch (Throwable t) {
            if (!warned) {
                warned = true;
                WorldBoundMod.LOG.warn("WorldBound: Distant Horizons display tweaks unavailable ({})", t.toString());
            }
        }
    }
}
