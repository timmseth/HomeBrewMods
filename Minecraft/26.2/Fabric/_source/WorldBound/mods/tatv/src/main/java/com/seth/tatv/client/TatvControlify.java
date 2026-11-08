package com.seth.tatv.client;

import com.seth.tatv.TatvEntity;
import dev.isxander.controlify.api.ControlifyApi;
import dev.isxander.controlify.api.bind.ControlifyBindApi;
import dev.isxander.controlify.api.bind.InputBinding;
import dev.isxander.controlify.api.bind.InputBindingSupplier;
import dev.isxander.controlify.api.entrypoint.ControlifyEntrypoint;
import dev.isxander.controlify.api.entrypoint.InitContext;
import dev.isxander.controlify.api.entrypoint.PreInitContext;
import dev.isxander.controlify.bindings.BindContext;
import dev.isxander.controlify.bindings.input.AxisInput;
import dev.isxander.controlify.bindings.input.ButtonInput;
import dev.isxander.controlify.controller.input.GamepadInputs;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Controller support through Controlify (only loaded when Controlify is installed). While driving a TATV:
 * RT = gas, LT = brake/reverse, left stick = steer, A = jump/up, B = down, D-pad right = mode, D-pad left = anchor,
 * D-pad down = dive/surface, D-pad up = get out, left-stick click = boost. All rebindable in Controlify's settings under "TATV".
 */
public final class TatvControlify implements ControlifyEntrypoint {
    static InputBindingSupplier GAS, BRAKE, DOWN, MODE, ANCHOR, SUBMERGE, EXIT;

    @Override
    public void onControlifyPreInit(PreInitContext ctx) {
        ControlifyBindApi b = ctx.bindings();
        BindContext driving = new BindContext(Identifier.fromNamespaceAndPath("tatv", "driving"),
                mc -> mc.player != null && mc.gui.screen() == null && mc.player.getVehicle() instanceof TatvEntity);
        b.registerBindContext(driving);
        Component cat = Component.literal("TATV");
        GAS = b.registerBinding(x -> x.id("tatv", "gas").name(Component.literal("Gas")).category(cat).allowedContexts(driving)
                .defaultInput(new AxisInput(GamepadInputs.RIGHT_TRIGGER_AXIS)));
        BRAKE = b.registerBinding(x -> x.id("tatv", "brake").name(Component.literal("Brake / reverse")).category(cat).allowedContexts(driving)
                .defaultInput(new AxisInput(GamepadInputs.LEFT_TRIGGER_AXIS)));
        DOWN = b.registerBinding(x -> x.id("tatv", "down").name(Component.literal("Down")).category(cat).allowedContexts(driving)
                .defaultInput(new ButtonInput(GamepadInputs.EAST_BUTTON)));
        MODE = b.registerBinding(x -> x.id("tatv", "mode").name(Component.literal("Cycle mode")).category(cat).allowedContexts(driving)
                .defaultInput(new ButtonInput(GamepadInputs.DPAD_RIGHT_BUTTON)));
        ANCHOR = b.registerBinding(x -> x.id("tatv", "anchor").name(Component.literal("Anchor")).category(cat).allowedContexts(driving)
                .defaultInput(new ButtonInput(GamepadInputs.DPAD_LEFT_BUTTON)));
        SUBMERGE = b.registerBinding(x -> x.id("tatv", "submerge").name(Component.literal("Dive / surface")).category(cat).allowedContexts(driving)
                .defaultInput(new ButtonInput(GamepadInputs.DPAD_DOWN_BUTTON)));
        EXIT = b.registerBinding(x -> x.id("tatv", "exit").name(Component.literal("Get out")).category(cat).allowedContexts(driving)
                .defaultInput(new ButtonInput(GamepadInputs.DPAD_UP_BUTTON)));
    }

    @Override
    public void onControlifyInit(InitContext ctx) {}

    @Override
    public void onControllersDiscovered(ControlifyApi api) {}

    /** {gas, brake, down, mode, anchor, submerge, exit} (the last four are just-pressed), all 0 when no controller is in use. */
    static float[] read() {
        float[] r = new float[7];
        try {
            var api = ControlifyApi.get();
            if (!api.currentInputMode().isController()) return r;
            var c = api.getCurrentController().orElse(null);
            if (c == null) return r;
            r[0] = val(GAS, c, false);
            r[1] = val(BRAKE, c, false);
            r[2] = val(DOWN, c, false);
            r[3] = val(MODE, c, true);
            r[4] = val(ANCHOR, c, true);
            r[5] = val(SUBMERGE, c, true);
            r[6] = val(EXIT, c, true);
        } catch (Throwable ignored) {
            // controller support is best-effort
        }
        return r;
    }

    private static float val(InputBindingSupplier s, dev.isxander.controlify.controller.ControllerEntity c, boolean press) {
        if (s == null) return 0;
        InputBinding b = s.onOrNull(c);
        if (b == null) return 0;
        return press ? (b.justPressed() ? 1 : 0) : b.analogueNow();
    }
}
