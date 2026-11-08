package com.seth.earthdata.mixin;

import com.seth.worldbound.Earth;
import com.seth.worldbound.client.Gazetteer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

/** Earth worlds: every screenshot adds a line to screenshots/worldbound-locations.txt with where on Earth it was taken. */
@Mixin(Screenshot.class)
public abstract class ScreenshotMixin {
    @Inject(method = "grab(Ljava/io/File;Ljava/lang/String;Lcom/mojang/blaze3d/pipeline/RenderTarget;ILjava/util/function/Consumer;)V", at = @At("HEAD"))
    private static void worldbound$location(File gameDir, String name, com.mojang.blaze3d.pipeline.RenderTarget target, int downscale,
                                            java.util.function.Consumer<net.minecraft.network.chat.Component> callback, CallbackInfo ci) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || !Earth.earthWorld()) return;
            var t = Earth.get().activeTransform();
            double lat = t.lat(mc.player.getZ()), lon = t.lon(mc.player.getX());
            String line = String.format(java.util.Locale.ROOT, "%s\t%s\t%.5f\t%.5f\t%.0f m\t%s%n",
                    java.time.LocalDateTime.now().withNano(0), name == null ? "" : name, lat, lon,
                    Earth.get().curve.metres(mc.player.getY()), Gazetteer.get().near(lat, lon));
            File dir = new File(gameDir, Screenshot.SCREENSHOT_DIR);
            dir.mkdirs();
            Files.writeString(new File(dir, "worldbound-locations.txt").toPath(), line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ignored) {
            // never break a screenshot over this
        }
    }
}
