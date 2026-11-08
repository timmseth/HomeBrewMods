package com.seth.worldbound.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.seth.worldbound.Earth;
import com.seth.worldbound.worldgen.BiomeTable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Pick any place on Earth: pan/zoom map or globe, click to select, search countries / states /
 * cities / zips, type coordinates, or go random. What the pick does is up to the caller
 * (set the spawn of a world being created, or teleport in a running world).
 */
public final class EarthPickerScreen extends Screen {
    private static final Identifier TEX = Identifier.fromNamespaceAndPath("worldbound", "earth_picker");
    private static final int PANEL = 212;
    private static final int MAX_TEX_W = 768;

    private final Screen parent;
    private final Consumer<double[]> onPick;
    private final String actionLabel;
    private final MapView view = new MapView();
    private double selLat, selLon;
    private boolean hasSelection;

    // map area in screen pixels
    private int mapX, mapY, mapW, mapH;
    // texture
    private DynamicTexture texture;
    private int texW, texH;
    private boolean texRegistered;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "worldbound-picker");
        t.setDaemon(true);
        return t;
    });
    private final AtomicInteger requested = new AtomicInteger();
    private volatile int[] pending;
    private volatile int pendingVersion = -1, pendingW, pendingH;
    private volatile MapView pendingView;
    private int shownVersion = -1;
    private MapView shownView;
    private boolean rendering;
    private boolean dirty = true;

    // widgets
    private EditBox search, latBox, lonBox;
    private final List<Button> resultButtons = new ArrayList<>();
    private List<com.seth.worldbound.api.Places.Place> results = List.of();
    private Button globeButton, landmarksButton, scaleButton, biomeButton, actionButton;
    /** Travel mode: discovered places ({lat, lon}) and the range around each in km; null = free pick. */
    private java.util.List<double[]> travelAnchors;
    private double travelRangeKm;

    /** The travel map's "Landmarks list…" button (set with travel mode). */
    private Runnable listAction;

    public EarthPickerScreen travelMode(java.util.List<double[]> anchors, double rangeKm, Runnable listAction) {
        travelAnchors = anchors;
        travelRangeKm = rangeKm;
        this.listAction = listAction;
        return this;
    }

    /** The real-place list (search, labels), or an empty one where there's no real Earth. */
    private static com.seth.worldbound.api.Places places() {
        var p = com.seth.worldbound.api.WorldBound.places();
        return p != null && !Earth.get().generated() ? p : NO_PLACES;
    }

    private static final com.seth.worldbound.api.Places NO_PLACES = new com.seth.worldbound.api.Places() {
        public String near(double lat, double lon) { return com.seth.worldbound.api.GeoText.latLon(lat, lon); }
        public List<com.seth.worldbound.api.Places.Place> search(String q, int n) {
            List<com.seth.worldbound.api.Places.Place> out = new ArrayList<>();
            String k = q.trim().toLowerCase(java.util.Locale.ROOT);
            if (k.isEmpty()) return out;
            for (var l : com.seth.worldbound.api.WorldBound.landmarks())
                if (out.size() < n && l.name().toLowerCase(java.util.Locale.ROOT).contains(k)) out.add(l);
            return out;
        }
        public List<com.seth.worldbound.api.Places.Place> landmarks() { return com.seth.worldbound.api.WorldBound.landmarks(); }
        public List<com.seth.worldbound.api.Places.Place> cities() { return List.of(); }
        public com.seth.worldbound.api.Places.Place nearestCity(double lat, double lon) { return null; }
        public List<com.seth.worldbound.api.Places.Place> nearestLandmarks(double lat, double lon, int n) { return List.of(); }
        public com.seth.worldbound.api.Places.Place randomCity() {
            var e = Earth.get();
            return new com.seth.worldbound.api.Places.Place("city", "Somewhere", "", "", e.config.origin_lat, e.config.origin_lon, 0);
        }
    };

    private double nearestAnchorKm(double lat, double lon) {
        double best = Double.MAX_VALUE;
        for (double[] a : travelAnchors) best = Math.min(best, com.seth.worldbound.api.GeoText.distanceKm(a[0], a[1], lat, lon));
        return best;
    }

    /** World scales offered when creating a world: metres per block, label, trade-offs. */
    static final double[] SCALES = {3, 5, 10, 20};
    static String biomeLabel(String mode) {
        return "Biomes: " + ("wild".equals(mode) ? "Wild (every installed biome, in patches)" : "Realistic (real climate)");
    }

    static String scaleLabel(double s) {
        return s == 3 ? "1:3 true proportions" : s == 5 ? "1:5 faster travel" : s == 10 ? "1:10 epic" : s == 20 ? "1:20 quick trips" : "1:" + (int) s;
    }
    static String scaleTradeoffs(double s) {
        int around = (int) Math.round(360 * 111.32 * Math.cos(Math.toRadians(42.87)) / s * 1000 / 1e5) ; // x100k blocks
        int slc = (int) Math.round(250000 / s / 1000.0); // Pocatello -> Salt Lake City, thousand blocks
        String common = String.format(Locale.ROOT, " Pocatello to Salt Lake City: %dk blocks (~%d min by elytra). World: %.1fM blocks around.",
                slc, Math.max(1, Math.round(250000 / s / 33 / 60)), around / 10.0);
        if (s == 3) return "Mountains and valleys at their real steepness. Huge travel times; detail softens between data points." + common;
        if (s == 5) return "Mountains about 1.7x steeper than real. Less travel; still realistic, slightly sharper terrain." + common;
        if (s == 10) return "Mountains about 3.3x steeper than real: dramatic peaks and narrow valleys. Sharpest terrain." + common;
        if (s == 20) return "Mountains about 6.7x steeper than real, spiky. Short trips; small rivers and lakes get thin or vanish." + common;
        return common;
    }
    private boolean showLandmarks;
    private int listOffset;
    private double pressX, pressY;
    private int scaleTextY = -1;
    private boolean dragging;

    public EarthPickerScreen(Screen parent, double startLat, double startLon, String actionLabel, Consumer<double[]> onPick) {
        super(Component.literal("WorldBound — pick a place on Earth"));
        this.parent = parent;
        this.onPick = onPick;
        this.actionLabel = actionLabel;
        view.centerLat = startLat;
        view.centerLon = startLon;
        view.degPerPx = 360.0 / 900;
        selLat = startLat;
        selLon = startLon;
        hasSelection = true;
    }

    @Override
    protected void init() {
        mapX = 8; mapY = 24; mapW = width - PANEL - 16; mapH = height - 46;
        int px = width - PANEL + 2, y = 24, w = PANEL - 10;
        search = new EditBox(font, px, y, w, 18, Component.literal("search"));
        search.setHint(Component.literal("country, state, city, zip"));
        search.setMaxLength(60);
        search.setResponder(this::onSearch);
        addRenderableWidget(search);
        y += 22;
        resultButtons.clear();
        int rows = parent != null ? Math.max(1, 5 - com.seth.worldbound.api.WorldOptions.all().size()) : 8; // room for the world options when creating a world
        for (int i = 0; i < rows; i++) {
            final int idx = i;
            Button b = Button.builder(Component.literal(""), bt -> pickResult(idx)).bounds(px, y, w, 16).build();
            b.visible = false;
            resultButtons.add(addRenderableWidget(b));
            y += 17;
        }
        y += 6;
        latBox = new EditBox(font, px, y, w / 2 - 2, 18, Component.literal("lat"));
        latBox.setHint(Component.literal("lat"));
        lonBox = new EditBox(font, px + w / 2 + 2, y, w / 2 - 2, 18, Component.literal("lon"));
        lonBox.setHint(Component.literal("lon"));
        addRenderableWidget(latBox);
        addRenderableWidget(lonBox);
        y += 22;
        addRenderableWidget(Button.builder(Component.literal("Go to coordinates"), b -> goToTyped()).bounds(px, y, w, 18).build());
        y += 22;
        addRenderableWidget(Button.builder(Component.literal("Random city"), b -> randomPlace()).bounds(px, y, w / 2 - 2, 18).build());
        globeButton = addRenderableWidget(Button.builder(Component.literal(view.globe ? "Flat map" : "Globe"), b -> {
            view.toggleGlobe(mapW, mapH);
            globeButton.setMessage(Component.literal(view.globe ? "Flat map" : "Globe"));
            markDirty();
        }).bounds(px + w / 2 + 2, y, w / 2 - 2, 18).build());
        y += 22;
        addRenderableWidget(Button.builder(Component.literal("Here (Pocatello)"), b -> goTo(Earth.get().config.origin_lat, Earth.get().config.origin_lon, 0.02)).bounds(px, y, w / 2 - 2, 18).build());
        landmarksButton = addRenderableWidget(Button.builder(Component.literal(showLandmarks ? "Landmarks ✓" : "Landmarks"), b -> {
            showLandmarks = !showLandmarks;
            landmarksButton.setMessage(Component.literal(showLandmarks ? "Landmarks ✓" : "Landmarks"));
            if (showLandmarks) search.setValue("");
            onSearch(search.getValue());
        }).bounds(px + w / 2 + 2, y, w / 2 - 2, 18).build());
        y += 22;
        if (parent != null) {
            var cfg = Earth.baseConfig();
            scaleButton = addRenderableWidget(Button.builder(Component.literal("Scale: " + scaleLabel(cfg.new_world_scale)), b -> {
                int i = 0;
                while (i < SCALES.length && SCALES[i] != cfg.new_world_scale) i++;
                cfg.new_world_scale = SCALES[(i + 1) % SCALES.length];
                cfg.save();
                scaleButton.setMessage(Component.literal("Scale: " + scaleLabel(cfg.new_world_scale)));
            }).bounds(px, y, w, 18).build());
            y += 20;
            biomeButton = addRenderableWidget(Button.builder(Component.literal(biomeLabel(cfg.new_world_biome_mode)), b -> {
                cfg.new_world_biome_mode = "wild".equals(cfg.new_world_biome_mode) ? "realistic" : "wild";
                cfg.save();
                biomeButton.setMessage(Component.literal(biomeLabel(cfg.new_world_biome_mode)));
            }).bounds(px, y, w, 18).build());
            // add-ons' options (dinosaur placement, landmark obelisks, city walls...)
            for (var o : com.seth.worldbound.api.WorldOptions.all()) {
                y += 20;
                Button[] ref = new Button[1];
                ref[0] = addRenderableWidget(Button.builder(Component.literal(o.label(com.seth.worldbound.api.WorldOptions.nextWorld(o.id()))), b -> {
                    String v = o.next(com.seth.worldbound.api.WorldOptions.nextWorld(o.id()));
                    com.seth.worldbound.api.WorldOptions.setNextWorld(o.id(), v);
                    ref[0].setMessage(Component.literal(o.label(v)));
                }).tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(o.tooltip()))).bounds(px, y, w, 18).build());
            }
            scaleTextY = y + 22;
        }
        y = height - 46;
        if (travelAnchors != null && listAction != null) {
            addRenderableWidget(Button.builder(Component.literal("Landmarks list…"), b -> {
                onClose();
                if (listAction != null) listAction.run();
            }).bounds(px, y - 22, w, 20).build());
        }
        actionButton = addRenderableWidget(Button.builder(Component.literal(actionLabel), b -> confirm()).bounds(px, y, w, 20).build());
        y += 22;
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose()).bounds(px, y, w, 20).build());
        updateCoordBoxes();
        markDirty();
    }

    private void onSearch(String q) {
        if (q.trim().isEmpty() && showLandmarks) {
            List<com.seth.worldbound.api.Places.Place> all = places().landmarks();
            listOffset = Math.max(0, Math.min(listOffset, all.size() - resultButtons.size()));
            results = all.subList(listOffset, Math.min(all.size(), listOffset + resultButtons.size()));
        } else {
            listOffset = 0;
            results = places().search(q, resultButtons.size());
        }
        for (int i = 0; i < resultButtons.size(); i++) {
            Button b = resultButtons.get(i);
            if (i < results.size()) {
                String label = results.get(i).label();
                b.setMessage(Component.literal(font.plainSubstrByWidth(label, b.getWidth() - 8)));
                b.visible = true;
            } else {
                b.visible = false;
            }
        }
    }

    private void pickResult(int i) {
        if (i >= results.size()) return;
        com.seth.worldbound.api.Places.Place p = results.get(i);
        goTo(p.lat(), p.lon(), p.span() / mapW);
    }

    private void randomPlace() {
        com.seth.worldbound.api.Places.Place p = places().randomCity();
        goTo(p.lat(), p.lon(), 0.6 / mapW);
        search.setValue(p.name());
    }

    private void goToTyped() {
        try {
            double lat = Double.parseDouble(latBox.getValue().trim()), lon = Double.parseDouble(lonBox.getValue().trim());
            if (lat < -90 || lat > 90 || lon < -180 || lon > 180) return;
            goTo(lat, lon, Math.min(view.degPerPx, 0.6 / mapW));
        } catch (NumberFormatException ignored) {
        }
    }

    private void goTo(double lat, double lon, double degPerPx) {
        view.centerLat = view.globe ? MapView.clampLat(lat) : MapView.clampFlat(lat);
        view.centerLon = MapView.wrap(lon);
        if (view.globe) view.radius = 1.0 / Math.toRadians(degPerPx); else view.degPerPx = degPerPx;
        select(lat, lon);
        markDirty();
    }

    private void select(double lat, double lon) {
        selLat = lat;
        selLon = MapView.wrap(lon);
        hasSelection = true;
        updateCoordBoxes();
    }

    private void updateCoordBoxes() {
        if (latBox == null) return;
        latBox.setValue(String.format(Locale.ROOT, "%.5f", selLat));
        lonBox.setValue(String.format(Locale.ROOT, "%.5f", selLon));
    }

    /** Rings of dots: everywhere inside one you can travel to. */
    private void drawTravelRange(GuiGraphicsExtractor g) {
        double rDeg = travelRangeKm / 111.2;
        int n = 0;
        for (double[] a : travelAnchors) {
            double[] c = view.toPixel(a[1], a[0], mapW, mapH);
            if (c != null && c[0] >= 0 && c[1] >= 0 && c[0] < mapW && c[1] < mapH) {
                int x = mapX + (int) c[0], y = mapY + (int) c[1];
                g.fill(x - 2, y - 2, x + 3, y + 3, 0xFF102010);
                g.fill(x - 1, y - 1, x + 2, y + 2, 0xFF60FF80);
            }
            if (++n > 600) continue;   // rings for the first 600 places only (they overlap anyway)
            double cosLat = Math.max(0.05, Math.cos(Math.toRadians(a[0])));
            for (int k = 0; k < 48; k++) {
                double t = k * Math.PI * 2 / 48;
                double[] q = view.toPixel(a[1] + rDeg * Math.cos(t) / cosLat, a[0] + rDeg * Math.sin(t), mapW, mapH);
                if (q == null || q[0] < 0 || q[1] < 0 || q[0] >= mapW || q[1] >= mapH) continue;
                g.fill(mapX + (int) q[0], mapY + (int) q[1], mapX + (int) q[0] + 1, mapY + (int) q[1] + 1, 0xC060FF80);
            }
        }
    }

    private void confirm() {
        if (!hasSelection) return;
        if (travelAnchors != null && nearestAnchorKm(selLat, selLon) > travelRangeKm) return;
        onPick.accept(new double[]{selLat, selLon});
        onClose();
    }

    @Override
    public void onClose() {
        worker.shutdownNow();
        if (texRegistered) {
            Minecraft.getInstance().getTextureManager().release(TEX);
            texRegistered = false;
        }
        Minecraft.getInstance().setScreenAndShow(parent);
    }

    // ---- input ---------------------------------------------------------------------------

    private boolean inMap(double x, double y) {
        return x >= mapX && y >= mapY && x < mapX + mapW && y < mapY + mapH;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
        if (super.mouseClicked(e, doubleClick)) return true;
        if (inMap(e.x(), e.y()) && e.button() == 0) {
            pressX = e.x();
            pressY = e.y();
            dragging = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        if (e.button() == 0 && inMap(pressX, pressY)) {
            if (!dragging && Math.hypot(e.x() - pressX, e.y() - pressY) > 3) dragging = true;
            if (dragging) {
                view.pan(dx, dy, mapW, mapH);
                markDirty();
            }
            return true;
        }
        return super.mouseDragged(e, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent e) {
        if (e.button() == 0 && inMap(pressX, pressY)) {
            if (!dragging && inMap(e.x(), e.y())) {
                com.seth.worldbound.api.Places.Place lm = landmarkAt(e.x(), e.y());
                if (lm != null) {
                    select(lm.lat(), lm.lon());
                    search.setValue(lm.name());
                } else {
                    double[] ll = view.toLonLat(e.x() - mapX, e.y() - mapY, mapW, mapH);
                    if (ll != null) select(ll[1], ll[0]);
                }
            }
            pressX = pressY = -1;
            dragging = false;
            return true;
        }
        return super.mouseReleased(e);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double sx, double sy) {
        if (inMap(x, y)) {
            view.zoom(Math.pow(1.3, sy), x - mapX, y - mapY, mapW, mapH);
            markDirty();
            return true;
        }
        if (x >= width - PANEL && showLandmarks && search.getValue().trim().isEmpty()) {
            listOffset = Math.max(0, listOffset - (int) Math.signum(sy) * 4);
            onSearch("");
            return true;
        }
        return super.mouseScrolled(x, y, sx, sy);
    }

    /** Landmark marker under the cursor, if any (within 6 px). */
    private com.seth.worldbound.api.Places.Place landmarkAt(double sx, double sy) {
        if (!showLandmarks) return null;
        com.seth.worldbound.api.Places.Place best = null;
        double bd = 7;
        for (com.seth.worldbound.api.Places.Place p : places().landmarks()) {
            double[] q = view.toPixel(p.lon(), p.lat(), mapW, mapH);
            if (q == null) continue;
            double d = Math.hypot(q[0] + mapX - sx, q[1] + mapY - sy);
            if (d < bd) { bd = d; best = p; }
        }
        return best;
    }

    @Override
    public boolean keyPressed(KeyEvent e) {
        if (super.keyPressed(e)) return true;
        if (e.key() == 257 || e.key() == 335) { // enter
            if (search.isFocused() && !results.isEmpty()) { pickResult(0); return true; }
            if (latBox.isFocused() || lonBox.isFocused()) { goToTyped(); return true; }
        }
        return false;
    }

    // ---- rendering -----------------------------------------------------------------------

    private void markDirty() {
        dirty = true;
        kick();
    }

    private void kick() {
        if (rendering || !dirty) return;
        dirty = false;
        rendering = true;
        int w = Math.min(MAX_TEX_W, mapW), h = Math.max(1, (int) ((long) mapH * w / Math.max(1, mapW)));
        MapView snap = view.scaled(mapW / (double) w); // texture pixels are bigger than screen pixels
        int version = requested.incrementAndGet();
        try {
            worker.submit(() -> {
                int[] px = snap.render(w, h);
                if (px == null) return;
                pending = px;
                pendingW = w;
                pendingH = h;
                pendingView = snap;
                pendingVersion = version;
            });
        } catch (RuntimeException ex) {
            rendering = false;
        }
    }

    private static MapView copy(MapView v) {
        MapView c = new MapView();
        c.globe = v.globe; c.centerLat = v.centerLat; c.centerLon = v.centerLon; c.degPerPx = v.degPerPx; c.radius = v.radius;
        return c;
    }

    private void uploadIfReady() {
        int[] px = pending;
        if (px == null || pendingVersion == shownVersion) return;
        int w = pendingW, h = pendingH;
        if (texture == null || texW != w || texH != h) {
            if (texRegistered) Minecraft.getInstance().getTextureManager().release(TEX);
            texture = new DynamicTexture(() -> "worldbound picker", w, h, false);
            Minecraft.getInstance().getTextureManager().register(TEX, texture);
            texRegistered = true;
            texW = w; texH = h;
        }
        NativeImage img = texture.getPixels();
        if (img != null) {
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) img.setPixel(x, y, px[y * w + x]);
            texture.upload();
        }
        shownVersion = pendingVersion;
        shownView = pendingView;
        pending = null;
        rendering = false;
        kick();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        uploadIfReady();
        g.fill(0, 0, width, height, 0xFF0E1016);
        g.fill(mapX - 1, mapY - 1, mapX + mapW + 1, mapY + mapH + 1, 0xFF3A4050);
        g.fill(mapX, mapY, mapX + mapW, mapY + mapH, 0xFF101018);
        if (texRegistered && shownView != null) {
            float u0 = 0, v0 = 0, u1 = 1, v1 = 1;
            if (!view.globe && !shownView.globe) {
                // Re-use the last rendered texture while a new one is on its way: remap its UVs to the current view.
                double[] tl = view.toLonLat(0, 0, mapW, mapH), br = view.toLonLat(mapW, mapH, mapW, mapH);
                if (tl != null && br != null) {
                    double[] ptl = shownView.toPixel(tl[0], tl[1], texW, texH), pbr = shownView.toPixel(br[0], br[1], texW, texH);
                    if (ptl != null && pbr != null) {
                        u0 = (float) (ptl[0] / texW); v0 = (float) (ptl[1] / texH);
                        u1 = (float) (pbr[0] / texW); v1 = (float) (pbr[1] / texH);
                    }
                }
            }
            g.enableScissor(mapX, mapY, mapX + mapW, mapY + mapH);
            g.blit(TEX, mapX, mapY, mapW, mapH, u0, u1, v0, v1);
            // markers
            double[] o = view.toPixel(Earth.get().config.origin_lon, Earth.get().config.origin_lat, mapW, mapH);
            if (o != null) drawMarker(g, mapX + (int) o[0], mapY + (int) o[1], 0xFFFFD040, false);
            drawLabels(g);
            if (showLandmarks && !Earth.get().generated()) {
                for (com.seth.worldbound.api.Places.Place p : places().landmarks()) {
                    double[] q = view.toPixel(p.lon(), p.lat(), mapW, mapH);
                    if (q == null || q[0] < 0 || q[1] < 0 || q[0] >= mapW || q[1] >= mapH) continue;
                    int x = mapX + (int) q[0], y = mapY + (int) q[1];
                    g.fill(x - 2, y - 2, x + 3, y + 3, 0xFF202020);
                    g.fill(x - 1, y - 1, x + 2, y + 2, 0xFF60E0FF);
                }
            }
            if (travelAnchors != null) drawTravelRange(g);
            if (hasSelection) {
                double[] s = view.toPixel(selLon, selLat, mapW, mapH);
                if (s != null) drawMarker(g, mapX + (int) s[0], mapY + (int) s[1], 0xFFFF3030, true);
            }
            g.disableScissor();
            com.seth.worldbound.api.Places.Place hoverLm = inMap(mouseX, mouseY) && !Earth.get().generated() ? landmarkAt(mouseX, mouseY) : null;
            if (hoverLm != null) {
                int tx = Math.min(mouseX + 8, mapX + mapW - font.width(hoverLm.name()) - 4);
                g.fill(tx - 2, mouseY - 12, tx + font.width(hoverLm.name()) + 2, mouseY - 1, 0xC0000000);
                g.text(font, hoverLm.name(), tx, mouseY - 10, 0xFFFFFFFF);
            }
        }
        if (travelAnchors != null && actionButton != null) {
            boolean ok = hasSelection && nearestAnchorKm(selLat, selLon) <= travelRangeKm;
            actionButton.active = ok;
            actionButton.setMessage(Component.literal(!hasSelection ? "Click the map to pick a destination"
                    : ok ? "Travel here" : String.format(Locale.ROOT, "Too far (%,.0f km from what you've found)", nearestAnchorKm(selLat, selLon))));
        }
        super.extractRenderState(g, mouseX, mouseY, partial);
        g.text(font, title.getString(), 8, 8, 0xFFFFFFFF);
        String scale = String.format(Locale.ROOT, "%s%.0f m/px  data z%d", view.globe ? "globe  " : "", view.viewDegPerPx() * 111_000, view.dataZoom());
        g.text(font, scale + (rendering ? "  …" : ""), mapX + mapW - font.width(scale) - 14, 8, 0xFFA0A8B8);
        // hover + selection readout
        String hover = "";
        if (inMap(mouseX, mouseY)) {
            double[] ll = view.toLonLat(mouseX - mapX, mouseY - mapY, mapW, mapH);
            if (ll != null) hover = String.format(Locale.ROOT, "cursor %.4f, %.4f", ll[1], ll[0]);
        }
        g.text(font, hover, mapX, height - 16, 0xFF9098A8);
        if (hasSelection) {
            Earth earth = Earth.get();
            // When creating a world, show blocks at the scale that world will use.
            var tf = parent != null ? new com.seth.worldbound.geo.GeoTransform(earth.config.origin_lat, earth.config.origin_lon, Earth.baseConfig().new_world_scale) : earth.activeTransform();
            int bx = (int) Math.floor(tf.blockX(selLon)), bz = (int) Math.floor(tf.blockZ(selLat));
            Earth.Surface s = earth.at(bx, bz);
            String info = String.format(Locale.ROOT, "selected %.4f, %.4f  |  %.0f m  %s  %s  |  block %d, %d",
                    selLat, selLon, s.elevM(), BiomeTable.code(s.climate()), BiomeTable.surface(s), bx, bz);
            g.text(font, font.plainSubstrByWidth(info, mapW), mapX, height - 28, 0xFFE8ECF4);
        }
        g.text(font, "drag: pan   wheel: zoom   click: select", width - PANEL + 2, height - 60 - 12, 0xFF70788A);
        if (scaleTextY > 0) {
            int ty = scaleTextY, bottom = height - 60 - 14;
            for (var line : font.split(Component.literal(scaleTradeoffs(Earth.baseConfig().new_world_scale)), PANEL - 12)) {
                if (ty + 9 > bottom) break;
                g.text(font, line, width - PANEL + 2, ty, 0xFFC8D0DC);
                ty += 10;
            }
        }
    }

    private final List<int[]> labelBoxes = new ArrayList<>();

    /** Place names by zoom level: countries → states & big cities → every city + landmark names. */
    private void drawLabels(GuiGraphicsExtractor g) {
        labelBoxes.clear();
        double dpp = view.viewDegPerPx();
        var gz = places();
        int budget = 60;
        if (dpp > 0.03) {
            budget = labelAll(g, gz.countryLabels(), 0xFFFFFFFF, budget, true);
        }
        if (dpp <= 0.12 && dpp > 0.004) {
            budget = labelAll(g, gz.states(), 0xFFE8E0B0, budget, true);
        }
        if (dpp <= 0.06) {
            budget = labelAll(g, dpp > 0.004 ? gz.bigCities() : gz.cities(), 0xFFFFF0C0, budget, false);
        }
        if (showLandmarks && dpp <= 0.03) {
            labelAll(g, gz.landmarks(), 0xFF80E8FF, budget, false);
        }
    }

    private int labelAll(GuiGraphicsExtractor g, List<com.seth.worldbound.api.Places.Place> places, int color, int budget, boolean upper) {
        for (com.seth.worldbound.api.Places.Place p : places) {
            if (budget <= 0) break;
            double[] q = view.toPixel(p.lon(), p.lat(), mapW, mapH);
            if (q == null || q[0] < -40 || q[1] < 0 || q[0] >= mapW + 40 || q[1] >= mapH) continue;
            String name = upper ? p.name().toUpperCase(Locale.ROOT) : p.name();
            int tw = font.width(name);
            int x = mapX + (int) q[0] - tw / 2, y = mapY + (int) q[1] - (upper ? 4 : 12);
            int[] box = {x - 2, y - 1, x + tw + 2, y + 9};
            boolean clash = false;
            for (int[] b : labelBoxes) {
                if (box[0] < b[2] && box[2] > b[0] && box[1] < b[3] && box[3] > b[1]) { clash = true; break; }
            }
            if (clash) continue;
            labelBoxes.add(box);
            g.text(font, name, x, y, color, true);
            if (!upper) g.fill(mapX + (int) q[0] - 1, mapY + (int) q[1] - 1, mapX + (int) q[0] + 1, mapY + (int) q[1] + 1, color);
            budget--;
        }
        return budget;
    }

    private void drawMarker(GuiGraphicsExtractor g, int x, int y, int color, boolean cross) {
        if (cross) {
            g.fill(x - 7, y, x - 2, y + 1, color);
            g.fill(x + 3, y, x + 8, y + 1, color);
            g.fill(x, y - 7, x + 1, y - 2, color);
            g.fill(x, y + 3, x + 1, y + 8, color);
        } else {
            g.fill(x - 2, y - 2, x + 3, y + 3, color);
            g.fill(x - 1, y - 1, x + 2, y + 2, 0xFF202020);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
