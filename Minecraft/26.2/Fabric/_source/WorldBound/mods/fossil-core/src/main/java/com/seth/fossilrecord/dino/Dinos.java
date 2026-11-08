package com.seth.fossilrecord.dino;

import com.seth.worldbound.Earth;
import com.seth.worldbound.WorldBoundConfig;
import com.seth.worldbound.WorldBoundMod;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Registers the dinosaurs (an entity type, spawn egg and five sounds each) and places them: on Earth at their fossil
 * sites, on a generated planet (Earthish) in a few home ranges per species drawn from the planet's seed, where the
 * climate suits them. A world set to random placement ({@code dino_placement} in its worldbound.json) puts any of
 * them anywhere they can stand instead. Called from {@link com.seth.fossilrecord.Fauna}'s spawner.
 */
public final class Dinos {
    private Dinos() {}

    public static final Map<String, EntityType<Dino>> TYPES = new LinkedHashMap<>();
    public static final Map<String, Item> EGGS = new LinkedHashMap<>();
    /** The laid egg (every species; see {@link DinoEggItem}). */
    public static Item EGG_ITEM;
    private static final Map<EntityType<?>, DinoSpecies> BY_TYPE = new HashMap<>();
    private static final Map<String, SoundEvent> SOUNDS = new HashMap<>();
    public static final String[] SOUND_KINDS = {"ambient", "hurt", "death", "attack", "roar"};

    /** New groups per newly generated chunk inside the dinosaurs' country (with random placement: on any land). */
    public static final double DENSITY = 0.03;
    /** The top-up near players stops while this many dinosaurs (times the spawn scale) are within 128 blocks. */
    static final int CROWD = 16;
    private static final RandomSource RNG = RandomSource.create();

    /** Tests can force a placement (null = the world's own setting). */
    private static volatile Boolean forced;

    /** This world's placement: false = accurate (fossil sites / home ranges), true = random (any of them anywhere they can live). */
    public static boolean randomPlacement() {
        Boolean f = forced;
        return f != null ? f : "random".equals(com.seth.worldbound.api.WorldOptions.get("dino_placement"));
    }

    /** For tests: force random (true) or accurate (false) placement, or go back to the world's setting (null). */
    public static void forcePlacement(Boolean random) { forced = random; }

    /** The "Dinosaurs" button on the Earth spawn screen, and the world setting /earth fauna placement changes. */
    public static void registerOption() {
        com.seth.worldbound.api.WorldOptions.register(new com.seth.worldbound.api.WorldOptions.Option("dino_placement", "Dinosaurs",
                java.util.List.of("accurate", "random"), java.util.List.of("Accurate (at their fossil sites)", "Random (anywhere they can live)"),
                "Accurate: each dinosaur lives only where its fossils were found. Random: any of them can turn up anywhere"
                        + " they can live. Change it later in the world with /earth fauna placement.", true));
    }

    public static DinoSpecies species(EntityType<?> type) {
        DinoSpecies s = BY_TYPE.get(type);
        if (s == null) throw new IllegalStateException("not a dinosaur: " + type);
        return s;
    }

    public static EntityType<Dino> type(DinoSpecies s) { return TYPES.get(s.id); }

    public static SoundEvent sound(DinoSpecies s, String kind) { return SOUNDS.get(s.id + "." + kind); }

    static Identifier id(String path) { return Identifier.fromNamespaceAndPath(com.seth.fossilrecord.FossilRecord.ID, path); }

    public static void register() {
        for (DinoSpecies s : Roster.ALL) {
            Rig r = s.rig();
            float w = r.width(), h = r.height(), eye = Math.min(r.eye(), h + 6f);
            int range = s.body.L > 15 ? 16 : s.body.L > 6 ? 12 : 8;
            ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, id(s.id));
            EntityType<Dino> type = Registry.register(BuiltInRegistries.ENTITY_TYPE, key,
                    FabricEntityType.Builder.<Dino>createMob(Dino::new, MobCategory.CREATURE,
                                    m -> m.spawnPlacement(SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                                                    (t, level, reason, pos, rnd) -> true)
                                            .defaultAttributes(() -> Dino.createAttributes(s)))
                            .sized(w, h).eyeHeight(eye).clientTrackingRange(range).build(key));
            TYPES.put(s.id, type);
            BY_TYPE.put(type, s);
            ResourceKey<Item> ek = ResourceKey.create(Registries.ITEM, id(s.id + "_spawn_egg"));
            EGGS.put(s.id, Registry.register(BuiltInRegistries.ITEM, ek, new SpawnEggItem(new Item.Properties().setId(ek).spawnEgg(type))));
            for (String k : SOUND_KINDS) {
                Identifier sid = id("entity." + s.id + "." + k);
                SOUNDS.put(s.id + "." + k, Registry.register(BuiltInRegistries.SOUND_EVENT, sid, SoundEvent.createVariableRangeEvent(sid)));
            }
        }
        ResourceKey<Item> ek = ResourceKey.create(Registries.ITEM, id("dino_egg"));
        EGG_ITEM = Registry.register(BuiltInRegistries.ITEM, ek, new DinoEggItem(new Item.Properties().setId(ek).stacksTo(16)));
    }

    static boolean enabled() {
        var f = com.seth.fossilrecord.FossilRecord.config();
        return f.enabled && f.dinosaurs && com.seth.fossilrecord.FossilRecord.homes() != null;
    }

    // ------------------------------------------------------------------ where

    /** Köppen class (Beck numbering) to the climate groups of {@link DinoSpecies}. */
    public static int climateGroup(int k) {
        if (k >= 1 && k <= 3) return DinoSpecies.TROPICAL;
        if (k >= 4 && k <= 7) return DinoSpecies.ARID;
        if (k == 8 || k == 11 || k == 14) return DinoSpecies.WARM;
        if (k == 9 || k == 10 || k == 12 || k == 13 || k == 15 || k == 16 || k == 17 || k == 18 || k == 21 || k == 22 || k == 25 || k == 26) return DinoSpecies.TEMPERATE;
        if (k >= 19 && k <= 29) return DinoSpecies.COLD;
        return 0;
    }

    /** Ground any dinosaur can stand on: land below 2,800 m, not ice, open water or town. */
    public static boolean ground(int climate, int cover, double elevM, DinoSpecies s) {
        if (elevM < -60 || elevM > 2800) return false;
        if (climate == 30 || cover == 70 || cover == 50) return false;          // ice sheet, snow, built-up
        if (cover == 80 && !s.aquatic) return false;
        if (climate == 29 && (s.climates & DinoSpecies.COLD) == 0) return false;   // tundra: only the polar kinds
        return true;
    }

    /** On a generated planet: three home ranges per species, from the planet's seed. */
    public static boolean planetHome(DinoSpecies s, long seed, double lat, double lon) {
        for (double[] h : planetHomes(s, seed)) {
            double dLat = lat - h[0], dLon = Math.abs(lon - h[1]);
            if (dLon > 180) dLon = 360 - dLon;
            dLon *= Math.cos(Math.toRadians(lat));
            if (dLat * dLat + dLon * dLon < h[2] * h[2]) return true;
        }
        return false;
    }

    /** {lat, lon, radius} of each home range on this planet. */
    public static List<double[]> planetHomes(DinoSpecies s, long seed) {
        List<double[]> out = new ArrayList<>();
        java.util.Random r = new java.util.Random(seed * 31 + s.id.hashCode());
        for (int i = 0; i < 3; i++) out.add(new double[]{-58 + r.nextDouble() * 116, -180 + r.nextDouble() * 360, 12 + r.nextDouble() * 12});
        return out;
    }

    /** Can this species live at this column of the current Earth or planet? */
    public static boolean suits(DinoSpecies s, int x, int z) {
        Earth e = Earth.get();
        return suits(s, e, e.at(x, z));
    }

    static boolean suits(DinoSpecies s, Earth e, Earth.Surface c) {
        if (!ground(c.climate(), c.cover(), c.elevM(), s) || c.ocean()) return false;
        if (randomPlacement()) return true;
        var h = com.seth.fossilrecord.FossilRecord.homes();
        return h != null && h.dinoClimate(s, c) && h.dino(s, c.lat(), c.lon());
    }

    /**
     * The species that may live at this column. Their sites (or home ranges) are checked first from latitude and
     * longitude alone, so the column's data is looked up only where some dinosaur ranges, and only once. With random
     * placement every species is a candidate and only the ground decides.
     */
    public static List<DinoSpecies> here(int x, int z) {
        Earth e = Earth.get();
        if (randomPlacement()) {
            Earth.Surface c = e.at(x, z);
            List<DinoSpecies> out = new ArrayList<>();
            for (DinoSpecies s : Roster.ALL) if (suits(s, e, c)) out.add(s);
            return out;
        }
        var h = com.seth.fossilrecord.FossilRecord.homes();
        if (h == null) return new ArrayList<>();
        double lat = e.transform.lat(z), lon = e.transform.lon(x);
        List<DinoSpecies> maybe = new ArrayList<>();
        for (DinoSpecies s : Roster.ALL) if (h.dino(s, lat, lon)) maybe.add(s);
        if (maybe.isEmpty()) return maybe;
        Earth.Surface c = e.at(x, z);
        List<DinoSpecies> out = new ArrayList<>();
        for (DinoSpecies s : maybe) if (suits(s, e, c)) out.add(s);
        return out;
    }

    static DinoSpecies pick(List<DinoSpecies> c) {
        int total = 0;
        for (DinoSpecies s : c) total += s.weight;
        int k = RNG.nextInt(Math.max(1, total));
        for (DinoSpecies s : c) if ((k -= s.weight) < 0) return s;
        return c.get(0);
    }

    // ------------------------------------------------------------------ placing

    /** A hatchling of this species at pos; a raptor's is tame to the player who hatched it. Null if there's no room. */
    public static Dino hatch(ServerLevel level, DinoSpecies s, BlockPos pos, net.minecraft.world.entity.player.Player by) {
        Dino d = type(s).create(level, EntitySpawnReason.BREEDING);
        if (d == null) return null;
        d.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, RNG.nextFloat() * 360f, 0f);
        d.setAge(-s.growTicks());
        d.setMale(RNG.nextBoolean());
        d.updateGrowth();
        d.refreshDimensions();
        if (!level.noCollision(d)) { d.discard(); return null; }
        if (s.tameable() && by != null) d.tame(by);
        level.addFreshEntity(d);
        return d;
    }

    /** One column of a newly generated chunk: maybe a group of whatever lives there. Returns groups placed (0 or 1). */
    public static int populate(ServerLevel level, int x, int z, double scale) {
        if (!enabled() || RNG.nextDouble() >= DENSITY * scale) return 0;
        List<DinoSpecies> c = here(x, z);
        if (c.isEmpty()) return 0;
        return spawnGroup(level, pick(c), x, z, EntitySpawnReason.CHUNK_GENERATION) > 0 ? 1 : 0;
    }

    /** Near a player now and then, if few of the kind (and not too many dinosaurs in all) are about. */
    public static int topUp(ServerLevel level, ServerPlayer p) {
        if (!enabled()) return 0;
        double a = RNG.nextDouble() * Math.PI * 2, d = 40 + RNG.nextDouble() * 40;
        int x = (int) Math.floor(p.getX() + Math.cos(a) * d), z = (int) Math.floor(p.getZ() + Math.sin(a) * d);
        if (level.getChunkSource().getChunkNow(x >> 4, z >> 4) == null) return 0;
        List<DinoSpecies> c = here(x, z);
        if (c.isEmpty()) return 0;
        DinoSpecies s = pick(c);
        EntityType<Dino> t = type(s);
        List<Dino> about = level.getEntitiesOfClass(Dino.class, new AABB(x - 128, level.getMinY(), z - 128, x + 128, level.getMaxY(), z + 128));
        if (about.size() >= Math.max(4, Math.round(CROWD * com.seth.fossilrecord.FossilRecord.config().spawn_scale))) return 0;
        int near = 0;
        for (Dino m : about) if (m.getType() == t) near++;
        if (near >= Math.max(2, s.groupMax * 2)) return 0;
        return spawnGroup(level, s, x, z, EntitySpawnReason.NATURAL) > 0 ? 1 : 0;
    }

    /** Places a group of this species around column (x, z); returns how many. */
    public static int spawnGroup(ServerLevel level, DinoSpecies s, int x, int z, EntitySpawnReason reason) {
        int n = s.groupMin + RNG.nextInt(s.groupMax - s.groupMin + 1), made = 0;
        int spread = (int) Math.max(4, Math.min(24, s.body.L * 0.8));
        for (int i = 0, tries = 0; i < n && tries < n * 5; tries++) {
            int px = tries == 0 ? x : x + RNG.nextInt(2 * spread + 1) - spread, pz = tries == 0 ? z : z + RNG.nextInt(2 * spread + 1) - spread;
            BlockPos pos = spot(level, s, px, pz);
            if (pos == null) continue;
            Dino m = type(s).create(level, reason);
            if (m == null) break;
            m.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, RNG.nextFloat() * 360f, 0f);
            if (i >= 2 && RNG.nextDouble() < 0.25) m.setAge(-s.growTicks() / 2 - RNG.nextInt(s.growTicks() / 2));
            m.updateGrowth();
            m.refreshDimensions();
            if (!level.noCollision(m)) { m.discard(); continue; }
            m.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), reason, null);
            if (m.isBaby()) m.updateGrowth();
            level.addFreshEntityWithPassengers(m);
            made++; i++;
        }
        return made;
    }

    /** Where in column (x, z) it would stand: dry solid ground (or shallow water for the swimmers), else null. */
    static BlockPos spot(ServerLevel level, DinoSpecies s, int x, int z) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos pos = new BlockPos(x, y, z);
        BlockState below = level.getBlockState(pos.below());
        if (level.getFluidState(pos.below()).is(FluidTags.WATER)) {
            if (!s.aquatic) return null;
            for (int dy = 1; dy <= 3; dy++) {
                BlockPos b = pos.below(dy);
                if (!level.getFluidState(b).is(FluidTags.WATER)) return level.getBlockState(b).isSolid() ? b.above() : null;
            }
            return null;
        }
        if (!below.isSolid() || below.is(BlockTags.LEAVES) || !level.getFluidState(pos).isEmpty()) return null;
        return pos;
    }

    // ------------------------------------------------------------------ for commands

    /** For /earth fauna placement: this world's setting, in words. */
    public static String placementLine() {
        if (randomPlacement()) return "Dinosaurs in this world: random. Any of the " + Roster.ALL.size() + " can turn up wherever it can live: land below"
                + " 2,800 m that isn't ice, snow or town (the swimmers in water too), with only the cold-country kinds on the tundra.";
        return "Dinosaurs in this world: accurate. Each lives only " + (Earth.get().generated()
                ? "in its home ranges on this planet, where the climate suits it." : "where its fossils were found (/earth fauna where <name>).");
    }

    /** The names people actually call them by, for /earth fauna where and spawn ("trex", "trike", "raptor"). */
    public static final Map<String, String> NICKNAMES = Map.ofEntries(
            Map.entry("trex", "tyrannosaurus"), Map.entry("t_rex", "tyrannosaurus"), Map.entry("rex", "tyrannosaurus"),
            Map.entry("trike", "triceratops"), Map.entry("stego", "stegosaurus"), Map.entry("spino", "spinosaurus"),
            Map.entry("raptor", "velociraptor"), Map.entry("bronto", "brontosaurus"), Map.entry("brachio", "brachiosaurus"),
            Map.entry("anky", "ankylosaurus"), Map.entry("pachy", "pachycephalosaurus"), Map.entry("parasaur", "parasaurolophus"),
            Map.entry("dilo", "dilophosaurus"), Map.entry("allo", "allosaurus"), Map.entry("carno", "carnotaurus"),
            Map.entry("giga", "giganotosaurus"), Map.entry("compy", "compsognathus"), Map.entry("galli", "gallimimus"),
            Map.entry("bary", "baryonyx"));

    public static DinoSpecies byName(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        DinoSpecies s = Roster.get(NICKNAMES.getOrDefault(n.replace('-', '_').replace(".", ""), n));
        if (s != null) return s;
        for (DinoSpecies d : Roster.ALL) if (d.shortName.toLowerCase(Locale.ROOT).equals(n) || d.name.toLowerCase(Locale.ROOT).replace(' ', '_').equals(n) || d.aka.equals(n)) return d;
        return null;
    }

    /** Where to find it: its sites (Earth) or home ranges (Earthish), nearest first, with block coordinates. */
    public static List<String> where(DinoSpecies s, double fromX, double fromZ) {
        List<String> out = new ArrayList<>();
        Earth e = Earth.get();
        out.add(s.name + " - " + s.age + ". " + String.format(Locale.ROOT, "%.1f m long, %s, hips %.1f m high.", s.body.L,
                s.mass >= 1000 ? String.format(Locale.ROOT, "%.1f t", s.mass / 1000f) : String.format(Locale.ROOT, "%.0f kg", s.mass), s.body.hip));
        if (randomPlacement()) out.add("This world places dinosaurs at random, so it can turn up anywhere it can live. "
                + (e.generated() ? "Its home ranges with accurate placement:" : "Where its fossils were found:"));
        List<double[]> pts = new ArrayList<>();
        List<String> names = new ArrayList<>();
        var homes = com.seth.fossilrecord.FossilRecord.homes();
        if (homes != null) for (var h : homes.dinoHomes(s)) { pts.add(h.getValue()); names.add(h.getKey()); }
        Integer[] order = new Integer[pts.size()];
        double[] dist = new double[pts.size()];
        for (int i = 0; i < pts.size(); i++) {
            order[i] = i;
            double bx = e.transform.blockX(pts.get(i)[1]), bz = e.transform.blockZ(pts.get(i)[0]);
            dist[i] = Math.hypot(bx - fromX, bz - fromZ);
        }
        java.util.Arrays.sort(order, (a, b) -> Double.compare(dist[a], dist[b]));
        for (int i : order) {
            double[] p = pts.get(i);
            out.add(String.format(Locale.ROOT, "  %s: %.2f, %.2f -> x %d, z %d (%,.0f blocks away)", names.get(i), p[0], p[1],
                    (long) Math.floor(e.transform.blockX(p[1])), (long) Math.floor(e.transform.blockZ(p[0])), dist[i]));
        }
        return out;
    }
}
