package com.seth.worldbound.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** "Travelling to …" while the destination's land is built, so you never arrive in unloaded terrain. */
public final class TravelScreen extends Screen {
    private String label;
    private int percent;

    TravelScreen(String label, int percent) {
        super(Component.literal("Travelling"));
        this.label = label;
        this.percent = percent;
    }

    void update(String label, int percent) {
        this.label = label;
        this.percent = Math.max(this.percent, percent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        g.fill(0, 0, width, height, 0xE0080A10);
        String t = "Travelling to " + label;
        g.text(font, t, (width - font.width(t)) / 2, height / 2 - 24, 0xFFE8ECF4);
        int w = Math.min(300, width - 60), x = (width - w) / 2, y = height / 2;
        g.fill(x - 1, y - 1, x + w + 1, y + 9, 0xFF3A4050);
        g.fill(x, y, x + w, y + 8, 0xFF101018);
        g.fill(x, y, x + w * percent / 100, y + 8, 0xFF60D0FF);
        String s = percent + "%  ·  building the land around your arrival point";
        g.text(font, s, (width - font.width(s)) / 2, y + 16, 0xFF9098A8);
        super.extractRenderState(g, mouseX, mouseY, partial);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
