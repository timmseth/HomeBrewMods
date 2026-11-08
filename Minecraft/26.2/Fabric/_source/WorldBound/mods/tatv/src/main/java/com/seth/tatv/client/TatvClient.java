package com.seth.tatv.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.seth.tatv.TatvAction;
import com.seth.tatv.TatvConfig;
import com.seth.tatv.TatvEntity;
import com.seth.tatv.TatvMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec2;

import java.util.Locale;

public final class TatvClient implements ClientModInitializer {
    static KeyMapping MODE, ANCHOR, DOWN, FASTER, SLOWER, SUBMERGE, EXIT, TILT;
    private static boolean prevJump;
    private static int statusTicks;
    private static final boolean CONTROLIFY = FabricLoader.getInstance().isModLoaded("controlify");

    @Override
    public void onInitializeClient() {
        ModelLayerRegistry.registerModelLayer(TatvRenderer.LAYER, TatvModel::createLayer);
        EntityRendererRegistry.register(TatvMod.ENTITY, TatvRenderer::new);

        KeyMapping.Category cat = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("tatv", "main"));
        MODE = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.tatv.mode", InputConstants.Type.KEYSYM, 82 /* R */, cat));
        ANCHOR = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.tatv.anchor", InputConstants.Type.KEYSYM, 71 /* G */, cat));
        DOWN = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.tatv.down", InputConstants.Type.KEYSYM, 88 /* X */, cat));
        FASTER = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.tatv.faster", InputConstants.Type.KEYSYM, 93 /* ] */, cat));
        SLOWER = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.tatv.slower", InputConstants.Type.KEYSYM, 91 /* [ */, cat));
        SUBMERGE = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.tatv.submerge", InputConstants.Type.KEYSYM, 342 /* Left Alt */, cat));
        EXIT = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.tatv.exit", InputConstants.Type.KEYSYM, 86 /* V */, cat));
        TILT = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.tatv.tilt", InputConstants.Type.KEYSYM, 78 /* N */, cat));
        TatvConfig.get();
        TatvEntity.CLIENT_ACTION = TatvClient::send;

        TatvEntity.CLIENT_CONTROLS = p -> {
            if (!(p instanceof LocalPlayer lp)) return TatvEntity.Controls.NONE;
            Vec2 mv = lp.input.getMoveVector();
            double throttle = mv.y, steer = -mv.x;
            boolean down = DOWN.isDown() || lp.input.keyPresses.shift();
            if (CONTROLIFY) {
                float[] c = TatvControlify.read();
                throttle += c[0] - c[1];
                down |= c[2] > 0.5f;
            }
            boolean up = lp.input.keyPresses.jump();
            boolean jumpPressed = up && !prevJump;
            prevJump = up;
            boolean boost = lp.input.keyPresses.sprint();
            return new TatvEntity.Controls(Math.max(-1, Math.min(1, throttle)), Math.max(-1, Math.min(1, steer)), up, down, jumpPressed, boost);
        };

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (TILT.consumeClick()) {
                TatvConfig.get().first_person_tilt = !TatvConfig.get().first_person_tilt;
                TatvConfig.save();
                if (mc.player != null) mc.player.sendOverlayMessage(Component.literal("TATV first-person tilt: " + (TatvConfig.get().first_person_tilt ? "on" : "off")));
            }
            if (mc.player == null || !(mc.player.getVehicle() instanceof TatvEntity t)) {
                for (KeyMapping k : new KeyMapping[]{MODE, ANCHOR, SUBMERGE, EXIT, FASTER, SLOWER}) while (k.consumeClick()) {}
                return;
            }
            boolean changed = false;
            float[] c = CONTROLIFY ? TatvControlify.read() : new float[7];
            boolean driver = t.getControllingPassenger() == mc.player;
            while (EXIT.consumeClick()) send(t, TatvEntity.ACT_EXIT);
            if (c[6] > 0) send(t, TatvEntity.ACT_EXIT);
            if (!driver) return;
            while (MODE.consumeClick()) { send(t, TatvEntity.ACT_MODE); changed = true; }
            if (c[3] > 0) { send(t, TatvEntity.ACT_MODE); changed = true; }
            while (ANCHOR.consumeClick()) { send(t, TatvEntity.ACT_ANCHOR); changed = true; }
            if (c[4] > 0) { send(t, TatvEntity.ACT_ANCHOR); changed = true; }
            while (SUBMERGE.consumeClick()) { send(t, TatvEntity.ACT_SUBMERGE); changed = true; }
            if (c[5] > 0) { send(t, TatvEntity.ACT_SUBMERGE); changed = true; }
            while (FASTER.consumeClick()) { t.speedLevel = Math.min(TatvEntity.SPEEDS.length - 1, t.speedLevel + 1); changed = true; }
            while (SLOWER.consumeClick()) { t.speedLevel = Math.max(0, t.speedLevel - 1); changed = true; }
            if (changed) statusTicks = 3;   // refresh once the server's answer has arrived
            if (statusTicks-- <= 0) {
                statusTicks = 30;
                showStatus(mc, t);
            }
        });
    }

    private static void send(TatvEntity t, int action) {
        ClientPlayNetworking.send(new TatvAction(t.getId(), action));
    }

    private static void showStatus(Minecraft mc, TatvEntity t) {
        // the mode/anchor shown is the server's (arrives a tick after a press), so predict the change
        String mode = t.mode().name().charAt(0) + t.mode().name().substring(1).toLowerCase(Locale.ROOT);
        float spd = TatvEntity.SPEEDS[t.speedLevel];
        if (t.submerged()) mode = "Submarine";
        String s = String.format(Locale.ROOT, "TATV  %s%s  ·  speed %sx  ·  %.0f km/h", mode, t.anchored() ? "  ·  ANCHORED" : "",
                spd < 1 ? String.valueOf(spd) : String.valueOf((int) spd), t.getDeltaMovement().length() * 20 * 3.6);
        mc.player.sendOverlayMessage(Component.literal(s));
    }
}
