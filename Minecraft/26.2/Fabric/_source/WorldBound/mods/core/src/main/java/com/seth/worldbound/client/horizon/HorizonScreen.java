package com.seth.worldbound.client.horizon;

import com.seth.worldbound.Earth;
import com.seth.worldbound.WorldBoundConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;

/** In-game settings for the horizon (key O): on/off, range, detail, start radius, Distant Horizons behaviour. */
public final class HorizonScreen extends Screen {
    private final Screen parent;

    public HorizonScreen(Screen parent) {
        super(Component.literal("WorldBound horizon"));
        this.parent = parent;
    }

    private WorldBoundConfig.Horizon cfg() {
        return Earth.baseConfig().horizon;
    }

    private static final class Slider extends AbstractSliderButton {
        private final double min, max;
        private final DoubleFunction<String> label;
        private final DoubleConsumer apply;

        Slider(int x, int y, int w, double min, double max, double value, DoubleFunction<String> label, DoubleConsumer apply) {
            super(x, y, w, 20, Component.literal(label.apply(value)), (value - min) / (max - min));
            this.min = min;
            this.max = max;
            this.label = label;
            this.apply = apply;
        }

        double real() {
            return min + value * (max - min);
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(label.apply(real())));
        }

        @Override
        protected void applyValue() {
            apply.accept(real());
        }
    }

    private Button onOff, dh;
    /** Slider changes are saved when the screen closes, not on every step of a drag. */
    private boolean dirty, rebuild;

    @Override
    protected void init() {
        int w = 240, x = (width - w) / 2, y = 50;
        onOff = addRenderableWidget(Button.builder(Component.literal(""), b -> {
            Horizon.setEnabled(!Horizon.enabled());
            labels();
        }).bounds(x, y, w, 20).build());
        y += 26;
        addRenderableWidget(new Slider(x, y, w, 4096, 65536, cfg().range_blocks,
                v -> String.format(Locale.ROOT, "Range: %,d blocks (%.0f km at 1:%.0f)", (int) v / 1024 * 1024,
                        v / 1024 * 1024 * Earth.get().config.scale_m_per_block / 1000, Earth.get().config.scale_m_per_block),
                v -> { cfg().range_blocks = (int) v / 1024 * 1024; dirty = true; }));
        y += 24;
        addRenderableWidget(new Slider(x, y, w, 1.0, 4.0, cfg().detail,
                v -> String.format(Locale.ROOT, "Detail: %.1f  (higher = sharper, heavier)", Math.round(v * 4) / 4.0),
                v -> { cfg().detail = Math.round(v * 4) / 4.0; dirty = true; rebuild = true; }));
        y += 24;
        addRenderableWidget(new Slider(x, y, w, 0.3, 1.2, cfg().overdraw,
                v -> String.format(Locale.ROOT, "Starts at %.0f%% of the render distance", Math.round(v * 20) / 20.0 * 100),
                v -> { cfg().overdraw = Math.round(v * 20) / 20.0; dirty = true; }));
        y += 24;
        dh = addRenderableWidget(Button.builder(Component.literal(""), b -> {
            cfg().with_distant_horizons = !cfg().with_distant_horizons;
            save();
            labels();
        }).bounds(x, y, w, 20).build());
        y += 30;
        addRenderableWidget(Button.builder(Component.literal("Rebuild tiles"), b -> Horizon.get().clear()).bounds(x, y, w / 2 - 2, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose()).bounds(x + w / 2 + 2, y, w / 2 - 2, 20).build());
        labels();
    }

    private void labels() {
        onOff.setMessage(Component.literal("Horizon: " + (Horizon.enabled() ? "ON" : "OFF")));
        dh.setMessage(Component.literal("With Distant Horizons installed: " + (cfg().with_distant_horizons ? "draw both" : "let DH do it")));
    }

    private void save() {
        Earth.baseConfig().save();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        super.extractRenderState(g, mouseX, mouseY, partial);
        String t = "WorldBound horizon — distant terrain from the Earth data";
        g.text(font, t, (width - font.width(t)) / 2, 14, 0xFFFFFFFF);
        String s;
        if (!Horizon.enabled()) s = "Off.";
        else if (Horizon.shaderPackInUse()) s = "Paused: a shader pack is active (Iris). Turn shaders off to see it.";
        else if (!Horizon.active()) s = "Idle: only in singleplayer Earth worlds" + (Horizon.dhLoaded() && !cfg().with_distant_horizons ? ", and Distant Horizons is installed." : ".");
        else s = Horizon.get().status();
        g.text(font, s, (width - font.width(s)) / 2, 30, 0xFFB0B8C8);
    }

    @Override
    public void onClose() {
        if (dirty) save();
        if (rebuild) Horizon.get().clear();
        minecraft.setScreenAndShow(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
