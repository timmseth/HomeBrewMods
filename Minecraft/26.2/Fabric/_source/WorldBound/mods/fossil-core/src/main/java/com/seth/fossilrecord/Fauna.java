package com.seth.fossilrecord;

import com.seth.worldbound.Earth;
import com.seth.worldbound.WorldBoundConfig;
import com.seth.worldbound.WorldBoundMod;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * WorldBound's own animals, living where they live on Earth: kangaroos in Australia, green anacondas in the Amazon.
 * They are not in any biome's spawn list (the vanilla spawner would pick them everywhere and then throw most picks
 * away, thinning out ordinary animals worldwide); instead WorldBound places them itself: a chance in each newly
 * generated chunk (as Minecraft does for animals) and a slow top-up near players, both checked against the real map.
 * Off in worlds that aren't Earth or Earthish.
 */
public final class Fauna {
    private Fauna() {}

    public static EntityType<Kangaroo> KANGAROO;
    public static EntityType<Anaconda> ANACONDA;
    public static Item KANGAROO_SPAWN_EGG, ANACONDA_SPAWN_EGG;
    public static SoundEvent KANGAROO_AMBIENT, KANGAROO_HURT, KANGAROO_DEATH, KANGAROO_KICK, ANACONDA_HISS, ANACONDA_HURT, ANACONDA_DEATH;

    public enum Species {
        KANGAROO(0.045, 12, 2, 6), ANACONDA(0.035, 2, 1, 1);
        /** per newly generated chunk whose sampled column suits it */
        final double chunkChance;
        /** most of its kind within 96 blocks before the top-up stops adding more */
        final int cap;
        final int groupMin, groupMax;

        Species(double chunkChance, int cap, int groupMin, int groupMax) {
            this.chunkChance = chunkChance; this.cap = cap; this.groupMin = groupMin; this.groupMax = groupMax;
        }

        EntityType<? extends Mob> type() { return this == KANGAROO ? Fauna.KANGAROO : Fauna.ANACONDA; }
    }

    static Identifier id(String path) { return Identifier.fromNamespaceAndPath(com.seth.fossilrecord.FossilRecord.ID, path); }

    public static void register() {
        KANGAROO = entity("kangaroo", FabricEntityType.Builder.createMob(Kangaroo::new, MobCategory.CREATURE,
                        m -> m.spawnPlacement(SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                                        (type, level, reason, pos, rnd) -> solidGround(level, pos))
                                .defaultAttributes(Kangaroo::createAttributes))
                .sized(0.8f, 1.6f).eyeHeight(1.4f).clientTrackingRange(10));
        ANACONDA = entity("anaconda", FabricEntityType.Builder.createMob(Anaconda::new, MobCategory.CREATURE,
                        m -> m.spawnPlacement(SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                                        (type, level, reason, pos, rnd) -> anacondaSpot(level, pos))
                                .defaultAttributes(Anaconda::createAttributes))
                .sized(1.2f, 0.7f).eyeHeight(0.45f).clientTrackingRange(10));
        KANGAROO_SPAWN_EGG = egg("kangaroo_spawn_egg", KANGAROO);
        ANACONDA_SPAWN_EGG = egg("anaconda_spawn_egg", ANACONDA);
        ResourceKey<CreativeModeTab> eggs = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace("spawn_eggs"));
        CreativeModeTabEvents.modifyOutputEvent(eggs).register(out -> { out.accept(KANGAROO_SPAWN_EGG); out.accept(ANACONDA_SPAWN_EGG); });
        com.seth.fossilrecord.dino.Dinos.register();
        // a tab of WorldBound's own animals: today's, then the dinosaurs
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, ResourceKey.create(Registries.CREATIVE_MODE_TAB, id("animals")),
                net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab.builder()
                        .title(net.minecraft.network.chat.Component.translatable("itemGroup.fossilrecord.animals"))
                        .icon(() -> new net.minecraft.world.item.ItemStack(com.seth.fossilrecord.dino.Dinos.EGGS.getOrDefault("tyrannosaurus", KANGAROO_SPAWN_EGG)))
                        .build());
        CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, id("animals"))).register(out -> {
            out.accept(KANGAROO_SPAWN_EGG);
            out.accept(ANACONDA_SPAWN_EGG);
            // the dinosaurs A to Z by the name on the egg
            com.seth.fossilrecord.dino.Roster.ALL.stream()
                    .sorted(java.util.Comparator.comparing((com.seth.fossilrecord.dino.DinoSpecies d) -> d.shortName, String.CASE_INSENSITIVE_ORDER))
                    .forEach(d -> out.accept(com.seth.fossilrecord.dino.Dinos.EGGS.get(d.id)));
        });
        KANGAROO_AMBIENT = sound("entity.kangaroo.ambient");
        KANGAROO_HURT = sound("entity.kangaroo.hurt");
        KANGAROO_DEATH = sound("entity.kangaroo.death");
        KANGAROO_KICK = sound("entity.kangaroo.kick");
        ANACONDA_HISS = sound("entity.anaconda.hiss");
        ANACONDA_HURT = sound("entity.anaconda.hurt");
        ANACONDA_DEATH = sound("entity.anaconda.death");

        ServerChunkEvents.CHUNK_GENERATE.register((level, chunk) -> {
            generateEvents++;
            if (level.dimension() == Level.OVERWORLD && enabled()) {
                if (NEW_CHUNKS.size() < 4096) NEW_CHUNKS.add(chunk.getPos().pack());
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(Fauna::tick);
        ServerTickEvents.END_SERVER_TICK.register(FaunaTests::tick);
    }

    private static <T extends Entity> EntityType<T> entity(String name, EntityType.Builder<T> b) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, id(name));
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, b.build(key));
    }

    private static Item egg(String name, EntityType<? extends Mob> type) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id(name));
        return Registry.register(BuiltInRegistries.ITEM, key, new SpawnEggItem(new Item.Properties().setId(key).spawnEgg(type)));
    }

    private static SoundEvent sound(String name) {
        Identifier id = id(name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    static boolean enabled() {
        return FossilRecord.config().enabled && FossilRecord.homes() != null;
    }

    // ------------------------------------------------------------------ where

    /** Can this species live at this column of the current Earth or planet? */
    public static boolean suits(Species s, int x, int z) {
        FossilRecord.Homes h = FossilRecord.homes();
        if (h == null) return false;
        Earth.Surface c = Earth.get().at(x, z);
        return s == Species.KANGAROO ? h.kangaroo(c) : h.anaconda(c);
    }

    static boolean solidGround(LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return below.isSolid() && level.getFluidState(pos).isEmpty() && !below.is(net.minecraft.tags.BlockTags.LEAVES);
    }

    /** In shallow water, or on the ground beside it. */
    static boolean anacondaSpot(LevelReader level, BlockPos pos) {
        if (level.getFluidState(pos.below()).is(FluidTags.WATER)) return true;
        if (!solidGround(level, pos)) return false;
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) for (int dy = -2; dy <= 0; dy++)
            if (level.getFluidState(pos.offset(dx, dy, dz)).is(FluidTags.WATER)) return true;
        return false;
    }

    // ------------------------------------------------------------------ spawning

    private static final ConcurrentLinkedQueue<Long> NEW_CHUNKS = new ConcurrentLinkedQueue<>();
    private static final RandomSource RNG = RandomSource.create();
    /** For /earth fauna and the self-test: groups placed in new chunks, and by the top-up. */
    public static int placedInChunks, placedNearPlayers, chunksLooked, generateEvents, dinoGroupsPlaced;

    private static void tick(MinecraftServer server) {
        if (!enabled()) { NEW_CHUNKS.clear(); return; }
        ServerLevel level = server.overworld();
        for (int n = 0; n < 12; n++) {
            Long c = NEW_CHUNKS.poll();
            if (c == null) break;
            int cx = net.minecraft.world.level.ChunkPos.getX(c), cz = net.minecraft.world.level.ChunkPos.getZ(c);
            if (level.getChunkSource().getChunkNow(cx, cz) == null) continue;
            chunksLooked++;
            try { populate(level, cx, cz); } catch (RuntimeException e) { com.seth.fossilrecord.FossilRecord.LOG.debug("fauna: chunk {} {}: {}", cx, cz, e.toString()); }
        }
        if (server.getTickCount() % 600 != 0) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.isSpectator() || p.level() != level || RNG.nextDouble() > 0.35 * com.seth.fossilrecord.FossilRecord.config().spawn_scale) continue;
            try { topUp(level, p); } catch (RuntimeException e) { com.seth.fossilrecord.FossilRecord.LOG.debug("fauna: top-up: {}", e.toString()); }
        }
    }

    /** One column of a new chunk: what lives there may turn up as a group, as animals do when Minecraft makes a chunk. */
    private static void populate(ServerLevel level, int cx, int cz) {
        int x = cx * 16 + RNG.nextInt(16), z = cz * 16 + RNG.nextInt(16);
        double scale = com.seth.fossilrecord.FossilRecord.config().spawn_scale;
        for (Species s : Species.values()) {
            if (RNG.nextDouble() >= s.chunkChance * scale || !suits(s, x, z)) continue;
            if (spawnGroup(level, s, x, z, EntitySpawnReason.CHUNK_GENERATION) > 0) { placedInChunks++; return; }
        }
        int d = com.seth.fossilrecord.dino.Dinos.populate(level, x, z, scale);
        placedInChunks += d;
        dinoGroupsPlaced += d;
    }

    /** Somewhere 32-56 blocks from the player, out of view distance's worst corners: a group if the land suits and few are about. */
    private static void topUp(ServerLevel level, ServerPlayer p) {
        if (RNG.nextBoolean()) {
            int d = com.seth.fossilrecord.dino.Dinos.topUp(level, p);
            placedNearPlayers += d;
            dinoGroupsPlaced += d;
            if (d > 0) return;
        }
        double a = RNG.nextDouble() * Math.PI * 2, d = 32 + RNG.nextDouble() * 24;
        int x = (int) Math.floor(p.getX() + Math.cos(a) * d), z = (int) Math.floor(p.getZ() + Math.sin(a) * d);
        if (level.getChunkSource().getChunkNow(x >> 4, z >> 4) == null) return;
        for (Species s : Species.values()) {
            if (!suits(s, x, z)) continue;
            int near = level.getEntitiesOfClass(Mob.class, new AABB(x - 96, level.getMinY(), z - 96, x + 96, level.getMaxY(), z + 96), m -> m.getType() == s.type()).size();
            if (near >= s.cap) continue;
            if (spawnGroup(level, s, x, z, EntitySpawnReason.NATURAL) > 0) { placedNearPlayers++; return; }
        }
    }

    /** Places a group of this species around column (x, z); returns how many. */
    public static int spawnGroup(ServerLevel level, Species s, int x, int z, EntitySpawnReason reason) {
        int n = s.groupMin + RNG.nextInt(s.groupMax - s.groupMin + 1), made = 0;
        for (int i = 0, tries = 0; i < n && tries < n * 4; tries++) {
            int px = i == 0 ? x : x + RNG.nextInt(9) - 4, pz = i == 0 ? z : z + RNG.nextInt(9) - 4;
            BlockPos pos = spot(level, s, px, pz);
            if (pos == null) continue;
            Mob m = s.type().create(level, reason);
            if (m == null) break;
            m.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, RNG.nextFloat() * 360f, 0f);
            if (!level.noCollision(m)) { m.discard(); continue; }
            m.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), reason, null);
            if (m instanceof Kangaroo k && i > 1 && RNG.nextDouble() < 0.3) k.setAge(-24000);   // a joey with the mob now and then
            level.addFreshEntityWithPassengers(m);
            made++; i++;
        }
        return made;
    }

    /** Where in column (x, z) this species would stand, or null. */
    private static BlockPos spot(ServerLevel level, Species s, int x, int z) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos pos = new BlockPos(x, y, z);
        if (s == Species.KANGAROO) return solidGround(level, pos) ? pos : null;
        if (level.getFluidState(pos.below()).is(FluidTags.WATER)) {
            // in water: on the bottom if it is shallow, otherwise not here
            for (int dy = 1; dy <= 4; dy++) {
                BlockPos b = pos.below(dy);
                if (!level.getFluidState(b).is(FluidTags.WATER)) return level.getBlockState(b).isSolid() ? b.above() : null;
            }
            return null;
        }
        return anacondaSpot(level, pos) ? pos : null;
    }

    /** For /earth fauna: what may live at this column, and how many of each are within 64 blocks of the player. */
    public static List<String> report(ServerLevel level, ServerPlayer p) {
        List<String> out = new ArrayList<>();
        int x = p.getBlockX(), z = p.getBlockZ();
        for (Species s : Species.values()) {
            int near = level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(64), m -> m.getType() == s.type()).size();
            out.add(s.name().toLowerCase(java.util.Locale.ROOT) + ": " + (suits(s, x, z) ? "lives here" : "not here") + ", " + near + " within 64 blocks");
        }
        List<String> dinos = new ArrayList<>();
        for (var d : com.seth.fossilrecord.dino.Dinos.here(x, z)) dinos.add(d.shortName);
        int dinoNear = level.getEntitiesOfClass(com.seth.fossilrecord.dino.Dino.class, p.getBoundingBox().inflate(128)).size();
        String which = com.seth.fossilrecord.dino.Dinos.randomPlacement()
                ? (dinos.isEmpty() ? "none can live on this spot" : "any of " + dinos.size() + " (random placement, /earth fauna placement)")
                : dinos.isEmpty() ? "none (see /earth fauna list and /earth fauna where <name>)" : String.join(", ", dinos);
        out.add("dinosaurs here: " + which + "; " + dinoNear + " within 128 blocks");
        out.add("fauna " + (enabled() ? "on" : "off") + "; placed " + placedInChunks + " groups in " + chunksLooked + " new chunks ("
                + dinoGroupsPlaced + " of dinosaurs), " + placedNearPlayers + " near players");
        return out;
    }
}
