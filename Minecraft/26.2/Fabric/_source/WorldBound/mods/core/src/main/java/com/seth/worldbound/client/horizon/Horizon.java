package com.seth.worldbound.client.horizon;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.seth.worldbound.Earth;
import com.seth.worldbound.WorldBoundConfig;
import com.seth.worldbound.WorldBoundMod;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelTerrainRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * WorldBound's own distant terrain: the world out to the horizon drawn straight from the Earth data, no chunks
 * involved. A quadtree of tiles (32×32 columns each, 2-block columns close up doubling to 512-block ones far
 * out) is meshed on background threads, kept on the GPU, and drawn after the opaque chunks with its own far
 * projection into its own colour+depth; a composite pass then lays it under the real chunks by true distance
 * and writes scene-space depth so clouds and water still sort against it.
 * <p>
 * It never draws over a chunk Minecraft is drawing: a per-chunk mask (one byte per chunk around the camera, set where
 * the chunk's surface section is compiled and faded in, vanilla's own LevelRenderer.isSectionCompiledAndVisible)
 * discards those fragments. Before 0.5.1 it was only kept out of a radius (overdraw × render distance), and beyond
 * that the nearer surface won, so a hole dug or blasted out there showed the horizon's untouched ground on top.
 */
public final class Horizon {
    private static final int LEVELS = 9;                 // spacing 2 .. 512 blocks, tiles 64 .. 16384 blocks
    private static final float NEAR = 4f;
    private static final int MAX_TILES = 2600;
    private static final long EVICT_NANOS = 20_000_000_000L;

    private static Horizon instance;
    private static boolean toggledOff;

    public static Horizon get() {
        if (instance == null) instance = new Horizon();
        return instance;
    }

    public static void register() {
        LevelRenderEvents.AFTER_OPAQUE_TERRAIN.register(ctx -> {
            try {
                get().render(ctx);
            } catch (RuntimeException e) {
                if (get().failures++ < 3) WorldBoundMod.LOG.error("horizon render failed", e);
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((h, mc) -> get().clear());
        net.minecraft.client.KeyMapping.Category cat = net.minecraft.client.KeyMapping.Category.register(Identifier.fromNamespaceAndPath(WorldBoundMod.ID, "horizon"));
        menuKey = net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(
                new net.minecraft.client.KeyMapping("key.worldbound.horizon", com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM, 79 /* O */, cat));
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (menuKey.consumeClick()) if (mc.level != null && mc.gui.screen() == null) mc.setScreenAndShow(new HorizonScreen(null));
        });
    }

    /** /earthhorizon on|off (this session). */
    public static void setEnabled(boolean on) {
        toggledOff = !on;
        Earth.baseConfig().horizon.enabled = on;
        Earth.baseConfig().save();
        if (!on) get().clear();
    }

    public static boolean enabled() {
        return !toggledOff && Earth.baseConfig().horizon.enabled;
    }

    public static boolean dhLoaded() {
        return DH_LOADED;
    }

    private static net.minecraft.client.KeyMapping menuKey;

    /** True when the horizon is being drawn right now (fog is pushed out to match). */
    public static boolean active() {
        Minecraft mc = Minecraft.getInstance();
        if (toggledOff || mc.level == null || mc.player == null || !Earth.earthWorld() || mc.level.dimension() != Level.OVERWORLD) return false;
        if (!mc.hasSingleplayerServer()) return false; // needs the Earth data locally; servers later
        if (shaderPackInUse()) return false;
        WorldBoundConfig.Horizon h = Earth.baseConfig().horizon;
        if (!h.enabled) return false;
        return h.with_distant_horizons || !DH_LOADED;
    }

    private static final boolean DH_LOADED = net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("distanthorizons");
    private static final boolean IRIS_LOADED = net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("iris");
    private static java.lang.reflect.Method irisInUse;
    private static Object irisApi;
    private static long irisCheckedAt;
    private static boolean irisPackActive;

    /**
     * Iris renders the world through the shader pack's own programs and rewrites the frame at the end; a horizon
     * drawn beside that never reaches the screen. Off while a pack is active (checked once a second).
     */
    public static boolean shaderPackInUse() {
        if (!IRIS_LOADED) return false;
        long t = System.nanoTime();
        if (t - irisCheckedAt < 1_000_000_000L) return irisPackActive;
        irisCheckedAt = t;
        try {
            if (irisApi == null) {
                Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                irisApi = api.getMethod("getInstance").invoke(null);
                irisInUse = api.getMethod("isShaderPackInUse");
            }
            irisPackActive = (Boolean) irisInUse.invoke(irisApi);
        } catch (ReflectiveOperationException | RuntimeException e) {
            irisPackActive = false;
        }
        return irisPackActive;
    }

    /** The horizon drew something last frame (vanilla fog is only pushed out then). */
    public static boolean drawing() {
        return active() && instance != null && instance.lastDrawn > 0;
    }

    public static float fogStart() {
        return Earth.baseConfig().horizon.range_blocks * 0.72f;
    }

    public static float fogEnd() {
        return Earth.baseConfig().horizon.range_blocks;
    }

    // ---- tiles ----------------------------------------------------------------------------------------------

    static final class Tile {
        final long key;
        final int level, tx, tz;
        GpuBuffer vb;
        int quads, minY, maxY;
        long lastUsed;

        Tile(long key, int level, int tx, int tz) {
            this.key = key;
            this.level = level;
            this.tx = tx;
            this.tz = tz;
        }

        void close() {
            if (vb != null) vb.close();
            vb = null;
        }
    }

    private record Built(long key, int level, int tx, int tz, HorizonMesher.Mesh mesh, long epoch) {}

    private static long key(int level, int tx, int tz) {
        return ((long) level << 58) ^ ((tx & 0x1FFFFFFFL) << 29) ^ (tz & 0x1FFFFFFFL);
    }

    private final Map<Long, Tile> tiles = new HashMap<>();
    private final Set<Long> pending = ConcurrentHashMap.newKeySet();
    /** When each missing tile was last asked for; a queued build is skipped once nobody has wanted it for a while. */
    private final Map<Long, Long> wantedAt = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<Built> built = new ConcurrentLinkedQueue<>();
    private final HorizonColors colors = new HorizonColors(HorizonColors::registryTint);
    private final HorizonMesher mesher = new HorizonMesher(colors);
    private ThreadPoolExecutor pool;
    private final AtomicLong generation = new AtomicLong();
    private int failures;
    private final List<Tile> draw = new ArrayList<>();
    private final List<Tile> scratch = new ArrayList<>();
    private double camX, camZ;
    private Frustum frustum;
    private long now;
    public int lastDrawn, lastQuads, lastPending;
    public long lastBuildNanos;

    private final class Job implements Runnable, Comparable<Job> {
        final long key, epoch;
        final int level, tx, tz;
        final double priority;

        Job(long key, int level, int tx, int tz, double priority) {
            this.key = key;
            this.level = level;
            this.tx = tx;
            this.tz = tz;
            this.priority = priority;
            this.epoch = generation.get();
        }

        @Override
        public void run() {
            try {
                Long w = wantedAt.get(key);
                if (w == null || System.nanoTime() - w > 2_000_000_000L) return; // nobody wants it any more
                long t0 = System.nanoTime();
                HorizonMesher.Mesh m = mesher.build(Earth.get(), level, tx, tz);
                lastBuildNanos = System.nanoTime() - t0;
                built.add(new Built(key, level, tx, tz, m, epoch));
            } catch (RuntimeException e) {
                if (failures++ < 3) WorldBoundMod.LOG.warn("horizon tile {}/{}/{}: {}", level, tx, tz, e.toString());
            } finally {
                pending.remove(key);
            }
        }

        @Override
        public int compareTo(Job o) {
            return Double.compare(priority, o.priority);
        }
    }

    private ThreadPoolExecutor pool() {
        if (pool == null) {
            int n = Math.max(1, Earth.baseConfig().horizon.threads);
            pool = new ThreadPoolExecutor(n, n, 10, TimeUnit.SECONDS, new PriorityBlockingQueue<>(), r -> {
                Thread t = new Thread(r, "WorldBound horizon");
                t.setDaemon(true);
                t.setPriority(Thread.MIN_PRIORITY + 1);
                return t;
            });
        }
        return pool;
    }

    /** Drop everything (leaving a world, toggling off). */
    public void clear() {
        if (!RenderSystem.isOnRenderThread()) { // GPU objects may only be touched on the render thread
            Minecraft.getInstance().execute(this::clear);
            return;
        }
        closeTargets();
        for (Tile t : tiles.values()) t.close();
        tiles.clear();
        draw.clear();
        generation.addAndGet(10);
        if (pool != null) pool.getQueue().clear();
        pending.clear();
        wantedAt.clear();
        Built b;
        while ((b = built.poll()) != null) b.mesh().free();
    }

    public int tileCount() {
        return tiles.size();
    }

    // ---- selection ------------------------------------------------------------------------------------------

    /**
     * Picks the tiles to draw under {@code level, tx, tz}: splits while the camera is closer than
     * {@code detail × tile size}, falls back to this tile while finer ones are still building, and queues what is
     * missing. Returns true when this subtree is fully covered.
     */
    private boolean select(int level, int tx, int tz, double detail, double range, double inner, List<Tile> out) {
        int T = HorizonMesher.tileBlocks(level);
        double x0 = (double) tx * T, z0 = (double) tz * T;
        double dx = Math.max(Math.max(x0 - camX, camX - (x0 + T)), 0), dz = Math.max(Math.max(z0 - camZ, camZ - (z0 + T)), 0);
        double d = Math.sqrt(dx * dx + dz * dz);
        if (d > range) return true;
        // Entirely inside the real chunks: nothing to draw.
        double fx = Math.max(Math.abs(x0 - camX), Math.abs(x0 + T - camX)), fz = Math.max(Math.abs(z0 - camZ), Math.abs(z0 + T - camZ));
        if (Math.sqrt(fx * fx + fz * fz) < inner) return true;
        long key = key(level, tx, tz);
        Tile t = tiles.get(key);
        if (level > 0 && d < detail * T) {
            // Children collect into the scratch list; only a fully covered set is kept.
            int n = scratch.size();
            boolean ok = true;
            for (int c = 0; c < 4; c++) ok &= select(level - 1, tx * 2 + (c & 1), tz * 2 + (c >> 1), detail, range, inner, scratch);
            if (ok) {
                if (out != scratch) {
                    out.addAll(scratch.subList(n, scratch.size()));
                    while (scratch.size() > n) scratch.remove(scratch.size() - 1);
                }
                return true;
            }
            while (scratch.size() > n) scratch.remove(scratch.size() - 1);
            if (t != null && t.vb != null) {
                out.add(t);
                return true;
            }
            request(key, level, tx, tz, d);
            return false;
        }
        if (t != null && t.vb != null) {
            out.add(t);
            return true;
        }
        request(key, level, tx, tz, d);
        return false;
    }

    private void request(long key, int level, int tx, int tz, double d) {
        wantedAt.put(key, now);
        if (pending.size() > 64 || !pending.add(key)) return;
        // Coarse tiles first (they cover the most sky), then by distance.
        pool().execute(new Job(key, level, tx, tz, d / HorizonMesher.tileBlocks(level)));
    }

    private void upload(GpuDevice device) {
        Built b;
        int n = 0;
        while (n++ < 12 && (b = built.poll()) != null) {
            if (b.epoch() != generation.get()) { b.mesh().free(); continue; } // built for a world we've left
            final Built fb = b;
            Tile t = tiles.computeIfAbsent(b.key(), k -> new Tile(k, fb.level(), fb.tx(), fb.tz()));
            t.close();
            HorizonMesher.Mesh m = b.mesh();
            if (m.quads() > 0) t.vb = device.createBuffer(() -> "WorldBound horizon tile", GpuBuffer.USAGE_VERTEX, m.data());
            t.quads = m.quads();
            t.minY = m.minY();
            t.maxY = m.maxY();
            t.lastUsed = now;
            m.free();
        }
    }

    private void evict() {
        if ((now & 255) == 0) wantedAt.entrySet().removeIf(e -> now - e.getValue() > 10_000_000_000L);
        if (tiles.size() <= MAX_TILES) {
            for (Iterator<Tile> it = tiles.values().iterator(); it.hasNext(); ) {
                Tile t = it.next();
                if (now - t.lastUsed > EVICT_NANOS) { t.close(); it.remove(); }
            }
            return;
        }
        List<Tile> all = new ArrayList<>(tiles.values());
        all.sort((a, b) -> Long.compare(a.lastUsed, b.lastUsed));
        for (int i = 0; i < all.size() - MAX_TILES * 3 / 4; i++) {
            all.get(i).close();
            tiles.remove(all.get(i).key);
        }
    }

    // ---- GPU --------------------------------------------------------------------------------------------------

    private static final BindGroupLayout HORIZON_INFO = BindGroupLayout.builder().withUniform("HorizonInfo", UniformType.UNIFORM_BUFFER)
            .withSampler("ChunkMask").build();
    private static final BindGroupLayout BLIT_INPUTS = BindGroupLayout.builder().withSampler("LodColor").withSampler("LodDepth")
            .withSampler("SceneDepth").withUniform("HorizonBlit", UniformType.UNIFORM_BUFFER).build();

    private static final RenderPipeline TERRAIN = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath(WorldBoundMod.ID, "pipeline/horizon"))
            .withVertexShader(Identifier.fromNamespaceAndPath(WorldBoundMod.ID, "core/horizon"))
            .withFragmentShader(Identifier.fromNamespaceAndPath(WorldBoundMod.ID, "core/horizon"))
            .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
            .withBindGroupLayout(BindGroupLayouts.FOG)
            .withBindGroupLayout(BindGroupLayouts.SAMPLER2)
            .withBindGroupLayout(HORIZON_INFO)
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withDepthStencilState(DepthStencilState.DEFAULT)
            .withCull(false)
            .withColorTargetState(ColorTargetState.DEFAULT)
            .build();

    private static final RenderPipeline BLIT = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath(WorldBoundMod.ID, "pipeline/horizon_blit"))
            .withVertexShader("core/screenquad")
            .withFragmentShader(Identifier.fromNamespaceAndPath(WorldBoundMod.ID, "core/horizon_blit"))
            .withBindGroupLayout(BLIT_INPUTS)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, true))
            .withCull(false)
            .withColorTargetState(ColorTargetState.DEFAULT)
            .build();

    private GpuTexture lodColor, lodDepth, sceneDepth;
    private GpuTextureView lodColorView, lodDepthView, sceneDepthView;
    private int targetW, targetH;
    private GpuBuffer infoUbo, blitUbo;
    private ProjectionMatrixBuffer projection;
    private final Matrix4f proj = new Matrix4f();
    private final Vector4f white = new Vector4f(1, 1, 1, 1);
    private final Vector3f offset = new Vector3f();
    private final Matrix4f identity = new Matrix4f();

    private void ensureTargets(GpuDevice device, int w, int h) {
        if (lodColor != null && targetW == w && targetH == h) return;
        closeTargets();
        int usage = GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_COPY_SRC | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT;
        lodColor = device.createTexture(() -> "WorldBound horizon colour", usage, GpuFormat.RGBA8_UNORM, w, h, 1, 1);
        lodDepth = device.createTexture(() -> "WorldBound horizon depth", usage, GpuFormat.D32_FLOAT, w, h, 1, 1);
        sceneDepth = device.createTexture(() -> "WorldBound scene depth copy", usage, GpuFormat.D32_FLOAT, w, h, 1, 1);
        lodColorView = device.createTextureView(lodColor);
        lodDepthView = device.createTextureView(lodDepth);
        sceneDepthView = device.createTextureView(sceneDepth);
        targetW = w;
        targetH = h;
    }

    private void closeTargets() {
        for (AutoCloseable c : new AutoCloseable[]{lodColorView, lodDepthView, sceneDepthView, lodColor, lodDepth, sceneDepth, maskView, mask}) {
            try { if (c != null) c.close(); } catch (Exception ignored) { }
        }
        lodColor = lodDepth = sceneDepth = null;
        lodColorView = lodDepthView = sceneDepthView = null;
        mask = null;
        maskView = null;
        maskN = 0;
    }

    // ---- the chunk mask ---------------------------------------------------------------------------------------

    private GpuTexture mask;
    private GpuTextureView maskView;
    private int maskN;
    private ByteBuffer maskBytes;
    /** Chunks Minecraft drew last frame (inside the mask), for /earthhorizon status. */
    public int lastMasked;

    private void ensureMask(GpuDevice device, int n) {
        if (mask != null && maskN == n) return;
        for (AutoCloseable c : new AutoCloseable[]{maskView, mask}) {
            try { if (c != null) c.close(); } catch (Exception ignored) { }
        }
        mask = device.createTexture(() -> "WorldBound horizon chunk mask", GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
                GpuFormat.R8_UNORM, n, n, 1, 1);
        maskView = device.createTextureView(mask);
        maskBytes = ByteBuffer.allocateDirect(n * n);
        maskN = n;
    }

    /**
     * One byte per chunk in the n × n square whose low corner is chunk (cx0, cz0): 255 where Minecraft is drawing the
     * real chunk, i.e. the client has it and the section holding its surface (the WORLD_SURFACE heightmap at the
     * chunk's middle) is compiled and faded in. Returns how many are set.
     */
    private int fillMask(Minecraft mc, int cx0, int cz0, int n) {
        var chunks = mc.level.getChunkSource();
        var renderer = mc.levelRenderer;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int set = 0;
        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                int cx = cx0 + i, cz = cz0 + j;
                LevelChunk c = chunks.getChunk(cx, cz, ChunkStatus.FULL, false);
                boolean real = c != null && renderer.isSectionCompiledAndVisible(p.set(cx * 16 + 8, c.getHeight(Heightmap.Types.WORLD_SURFACE, 8, 8) - 1, cz * 16 + 8));
                maskBytes.put(j * n + i, (byte) (real ? 0xFF : 0));
                if (real) set++;
            }
        }
        return set;
    }

    private void render(LevelTerrainRenderContext ctx) {
        if (!active()) {
            lastDrawn = 0;
            if (!tiles.isEmpty()) clear();
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        WorldBoundConfig.Horizon cfg = Earth.baseConfig().horizon;
        CameraRenderState cam = ctx.levelState().cameraRenderState;
        Vec3 pos = cam.pos;
        if (pos == null || cam.projectionMatrix == null || cam.viewRotationMatrix == null) return;
        now = System.nanoTime();
        camX = pos.x;
        camZ = pos.z;
        GpuDevice device = RenderSystem.getDevice();
        upload(device);

        // Our projection: the scene's lens, our own near/far planes (far enough for the horizon, near enough
        // for depth precision out there).
        boolean zeroToOne = device.getDeviceInfo().isZZeroToOne();
        float range = cfg.range_blocks, far = range * 1.25f + 2048f;
        proj.set(cam.projectionMatrix);
        setPlanes(proj, NEAR, far, zeroToOne);
        frustum = new Frustum(cam.viewRotationMatrix, proj);
        frustum.prepare(pos.x, pos.y, pos.z);

        // Which tiles.
        double inner = mc.options.getEffectiveRenderDistance() * 16.0 * cfg.overdraw;
        draw.clear();
        scratch.clear();
        int topT = HorizonMesher.tileBlocks(LEVELS - 1);
        int tx0 = (int) Math.floor((camX - range) / topT), tx1 = (int) Math.floor((camX + range) / topT);
        int tz0 = (int) Math.floor((camZ - range) / topT), tz1 = (int) Math.floor((camZ + range) / topT);
        for (int tz = tz0; tz <= tz1; tz++)
            for (int tx = tx0; tx <= tx1; tx++)
                select(LEVELS - 1, tx, tz, cfg.detail, range, inner, draw);
        lastPending = pending.size();
        for (Tile t : draw) t.lastUsed = now;
        evict(); // after marking: never close a tile we're about to draw

        // Frustum cull and count.
        int maxIndices = 0, quads = 0;
        for (Iterator<Tile> it = draw.iterator(); it.hasNext(); ) {
            Tile t = it.next();
            int T = HorizonMesher.tileBlocks(t.level);
            double x0 = (double) t.tx * T, z0 = (double) t.tz * T;
            if (t.quads == 0 || !frustum.isVisible(new AABB(x0, t.minY, z0, x0 + T, t.maxY, z0 + T))) { it.remove(); continue; }
            maxIndices = Math.max(maxIndices, t.quads * 6);
            quads += t.quads;
        }
        lastDrawn = draw.size();
        lastQuads = quads;
        if (draw.isEmpty()) return;

        RenderTarget main = mc.gameRenderer.mainRenderTarget();
        int w = main.width, h = main.height;
        ensureTargets(device, w, h);
        CommandEncoder enc = device.createCommandEncoder();
        // The chunk mask: the render distance and two chunks more around the camera's chunk.
        int half = mc.options.getEffectiveRenderDistance() + 2, n = 2 * half + 1;
        int cx0 = Math.floorDiv((int) Math.floor(camX), 16) - half, cz0 = Math.floorDiv((int) Math.floor(camZ), 16) - half;
        ensureMask(device, n);
        boolean masked = mc.levelRenderer != null && mc.level != null;
        lastMasked = masked ? fillMask(mc, cx0, cz0, n) : 0;
        if (!masked) maskBytes.clear().put(new byte[n * n]).clear();
        enc.writeToTexture(mask, maskBytes, 0, 0, 0, 0, n, n);
        if (infoUbo == null) infoUbo = device.createBuffer(() -> "WorldBound horizon info", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, 32);
        if (blitUbo == null) blitUbo = device.createBuffer(() -> "WorldBound horizon blit", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, 32);
        if (projection == null) projection = new ProjectionMatrixBuffer("WorldBound horizon");
        try (MemoryStack st = MemoryStack.stackPush()) {
            float overlap = (float) (mc.options.getEffectiveRenderDistance() * 16.0 * 1.3);
            // x: radius with no horizon at all (only without a mask; the mask does that job chunk by chunk), y/z: the sink;
            // then the mask's low corner relative to the camera (blocks), its size in chunks, and whether it is in use
            enc.writeToBuffer(infoUbo.slice(), Std140Builder.onStack(st, 32)
                    .putVec4(masked ? 0f : (float) inner, overlap, 1f, 0f)
                    .putVec4((float) (cx0 * 16.0 - camX), (float) (cz0 * 16.0 - camZ), n, masked ? 1f : 0f).get());
            Matrix4f sp = cam.projectionMatrix;
            enc.writeToBuffer(blitUbo.slice(), Std140Builder.onStack(st, 32)
                    .putVec4(proj.m22(), proj.m32(), zeroToOne ? 1f : 0f, 0f)
                    .putVec4(sp.m22(), sp.m32(), zeroToOne ? 1f : 0f, 0f).get());
        }
        GpuBufferSlice projSlice = projection.getBuffer(proj);
        RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
        GpuBuffer indexBuffer = indices.getBuffer(maxIndices);

        // Pass 1: the horizon into its own colour + depth.
        try (RenderPass pass = enc.createRenderPass(() -> "WorldBound horizon", lodColorView, Optional.of(new Vector4f(0, 0, 0, 0)),
                lodDepthView, OptionalDouble.of(0.0))) { // reversed depth: 0 = nothing
            pass.setPipeline(TERRAIN);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("Projection", projSlice);
            pass.setUniform("HorizonInfo", infoUbo);
            pass.bindTexture("Sampler2", mc.gameRenderer.levelLightmap(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            pass.bindTexture("ChunkMask", maskView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
            pass.setIndexBuffer(indexBuffer, indices.type());
            for (Tile t : draw) {
                int T = HorizonMesher.tileBlocks(t.level);
                offset.set((float) ((double) t.tx * T - pos.x), (float) -pos.y, (float) ((double) t.tz * T - pos.z));
                pass.setUniform("DynamicTransforms", RenderSystem.getDynamicUniforms().writeTransform(cam.viewRotationMatrix, white, offset, identity));
                pass.setVertexBuffer(0, t.vb.slice());
                pass.drawIndexed(t.quads * 6, 1, 0, 0, 0);
            }
        }

        // Pass 2: under the real chunks, into the scene.
        enc.copyTextureToTexture(main.getDepthTexture(), sceneDepth, 0, 0, 0, 0, 0, w, h);
        try (RenderPass pass = enc.createRenderPass(() -> "WorldBound horizon composite", main.getColorTextureView(), Optional.empty(),
                main.getDepthTextureView(), OptionalDouble.empty())) {
            pass.setPipeline(BLIT);
            var nearest = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
            pass.bindTexture("LodColor", lodColorView, nearest);
            pass.bindTexture("LodDepth", lodDepthView, nearest);
            pass.bindTexture("SceneDepth", sceneDepthView, nearest);
            pass.setUniform("HorizonBlit", blitUbo);
            pass.draw(3, 1, 0, 0);
        }
    }

    /**
     * Near/far planes for the horizon's projection in 26.2's reversed-depth convention (near = 1, far = 0, cleared
     * to 0, GEQUAL test): the same lens as the scene, with a far plane out at the horizon.
     */
    private static void setPlanes(Matrix4f m, float near, float far, boolean zeroToOne) {
        if (zeroToOne) {
            m.m22(near / (far - near));
            m.m32(far * near / (far - near));
        } else {
            m.m22((far + near) / (far - near));
            m.m32(2f * far * near / (far - near));
        }
    }

    public String status() {
        return String.format(java.util.Locale.ROOT, "horizon: %s, %d tiles cached, %d drawn (%,d quads), %d building, last build %.1f ms, range %,d blocks, %d real chunks masked",
                active() ? "on" : enabled() ? "idle" : "off", tiles.size(), lastDrawn, lastQuads, lastPending, lastBuildNanos / 1e6,
                Earth.baseConfig().horizon.range_blocks, lastMasked);
    }
}
