package com.seth.worldbound.client;

import com.seth.worldbound.Earth;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * The landmark list: discovered ones first, then the rest, nearest first. At an obelisk, clicking a discovered
 * landmark travels there; anywhere else (or for an undiscovered one) it sets the guide trail.
 */
public final class LandmarkScreen extends Screen {
    private final List<Button> rows = new ArrayList<>();
    private List<Integer> order = new ArrayList<>();
    private int offset;
    private EditBox search;

    LandmarkScreen() {
        super(Component.literal("Landmarks"));
    }

    @Override
    protected void init() {
        int w = Math.min(420, width - 40), x = (width - w) / 2;
        search = new EditBox(font, x, 40, w, 18, Component.literal("search"));
        search.setHint(Component.literal("Search landmarks…"));
        search.setResponder(s -> { offset = 0; refresh(); });
        addRenderableWidget(search);
        rows.clear();
        int n = Math.max(4, (height - 40 - 22 - 60) / 20);
        for (int i = 0; i < n; i++) {
            int row = i;
            rows.add(addRenderableWidget(Button.builder(Component.literal(""), b -> click(row)).bounds(x, 62 + i * 20, w, 18).build()));
        }
        int by = height - 26;
        addRenderableWidget(Button.builder(Component.literal(LandmarkClient.guide >= 0 ? "Stop guiding" : "No guide set"), b -> {
            LandmarkClient.guide = -1;
            b.setMessage(Component.literal("No guide set"));
        }).bounds(x, by, w / 2 - 2, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Close"), b -> onClose()).bounds(x + w / 2 + 2, by, w / 2 - 2, 20).build());
        refresh();
    }

    private double distance(int i) {
        var p = Minecraft.getInstance().player;
        if (p == null) return 0;
        double[] d = LandmarkClient.delta(p.getX(), p.getZ(), i);
        return Math.hypot(d[0], d[1]);
    }

    private void refresh() {
        var lms = com.seth.worldbound.api.WorldBound.landmarks();
        String q = search == null ? "" : com.seth.worldbound.api.GeoText.plain(search.getValue()).toLowerCase(Locale.ROOT);
        order = new ArrayList<>();
        for (int i = 0; i < lms.size(); i++) {
            var lm = lms.get(i);
            if (!q.isEmpty() && !com.seth.worldbound.api.GeoText.plain(lm.name()).toLowerCase(Locale.ROOT).contains(q) && !lm.region().toLowerCase(Locale.ROOT).contains(q)) continue;
            order.add(i);
        }
        order.sort(Comparator.comparing((Integer i) -> !LandmarkClient.discovered.contains(i)).thenComparingDouble(this::distance));
        offset = Math.max(0, Math.min(offset, order.size() - rows.size()));
        double mpb = Earth.get().activeTransform().metresPerBlock;
        for (int r = 0; r < rows.size(); r++) {
            Button b = rows.get(r);
            int k = offset + r;
            b.visible = k < order.size();
            if (!b.visible) continue;
            int i = order.get(k);
            var lm = lms.get(i);
            boolean found = LandmarkClient.discovered.contains(i);
            double km = distance(i) * mpb / 1000;
            String verb = i == LandmarkClient.at ? "you are here" : found && LandmarkClient.at >= 0 ? "travel" : "guide me";
            String label = (found ? "§a✔ " : "§7? ") + com.seth.worldbound.api.GeoText.plain(lm.name()) + "§r  §8" + lm.region()
                    + String.format(Locale.ROOT, "§r   %,.0f km  · %s", km, verb);
            b.setMessage(Component.literal(label));
            b.active = i != LandmarkClient.at;
        }
    }

    private void click(int row) {
        int k = offset + row;
        if (k >= order.size()) return;
        int i = order.get(k);
        if (LandmarkClient.at >= 0 && LandmarkClient.discovered.contains(i) && i != LandmarkClient.at) {
            LandmarkClient.travel(i);
            LandmarkClient.guide = -1;
        } else {
            LandmarkClient.guide = i;
        }
        onClose();
    }

    @Override
    public boolean mouseScrolled(double x, double y, double sx, double sy) {
        offset = Math.max(0, Math.min(offset - (int) Math.signum(sy) * 3, order.size() - rows.size()));
        refresh();
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        super.extractRenderState(g, mouseX, mouseY, partial);
        int total = com.seth.worldbound.api.WorldBound.landmarks().size();
        String t = "Landmarks  ·  discovered " + LandmarkClient.discovered.size() + " / " + total;
        g.text(font, t, (width - font.width(t)) / 2, 12, 0xFFFFFFFF);
        String h = LandmarkClient.at >= 0 && LandmarkClient.at < com.seth.worldbound.api.WorldBound.landmarks().size()
                ? "At the " + com.seth.worldbound.api.GeoText.plain(com.seth.worldbound.api.WorldBound.landmarks().get(LandmarkClient.at).name()) + " obelisk: click a discovered landmark to travel there."
                : "Click a landmark to be guided to it. Travel works from obelisks you've discovered.";
        g.text(font, h, (width - font.width(h)) / 2, 25, 0xFFB0B8C8);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
