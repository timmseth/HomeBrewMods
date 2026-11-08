package com.seth.fossilrecord;

import com.seth.worldbound.Earth;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * /earth fauna selftest: places animals on the real map (Australian outback, Murray country, near Manaus) and none in
 * London; then on a stone pad checks that a male kangaroo fights back and a female flees, that neither turns on a
 * creative player, and that an anaconda constricts what it bites and never targets creative or Peaceful players.
 */
public final class FaunaTests {
    private FaunaTests() {}

    private record Job(long at, Runnable run) {}
    private static final List<Job> JOBS = new ArrayList<>();
    private static MinecraftServer server;

    static void tick(MinecraftServer s) {
        server = s;
        if (JOBS.isEmpty()) return;
        long now = s.getTickCount();
        List<Job> due = new ArrayList<>();
        JOBS.removeIf(j -> { if (j.at() <= now) { due.add(j); return true; } return false; });
        for (Job j : due) j.run().run();
    }

    static void later(int ticks, Runnable r) { JOBS.add(new Job(server.getTickCount() + ticks, r)); }
    static void say(CommandSourceStack src, String s) { src.sendSuccess(() -> Component.literal(s), false); }

    public static void run(CommandSourceStack src) {
        ServerLevel level = src.getLevel();
        server = level.getServer();
        if (!Earth.earthWorld()) { say(src, "fauna selftest: not an Earth world"); return; }
        // 1. the map
        Object[][] places = {{"Australian outback", -23.9, 133.5, Fauna.Species.KANGAROO}, {"Murray country", -34.4, 148.5, Fauna.Species.KANGAROO},
                {"near Manaus", -3.3, -60.6, Fauna.Species.ANACONDA}, {"London", 51.5, -0.1, null}};
        for (Object[] p : places) {
            double lat = (double) p[1], lon = (double) p[2];
            int x0 = (int) Math.floor(Earth.get().transform.blockX(lon)), z0 = (int) Math.floor(Earth.get().transform.blockZ(lat));
            Fauna.Species want = (Fauna.Species) p[3];
            int placed = 0, tried = 0;
            boolean k = Fauna.suits(Fauna.Species.KANGAROO, x0, z0), a = Fauna.suits(Fauna.Species.ANACONDA, x0, z0);
            if (want != null) {
                // look over a few chunks for a spot this one would take (anacondas need water close by)
                for (int t = 0; t < 40 && placed == 0; t++) {
                    int x = x0 + (t % 7 - 3) * 16 + 8, z = z0 + (t / 7 - 3) * 16 + 8;
                    level.getChunk(x >> 4, z >> 4);
                    if (!Fauna.suits(want, x, z)) continue;
                    tried++;
                    placed = Fauna.spawnGroup(level, want, x, z, EntitySpawnReason.COMMAND);
                }
            }
            say(src, String.format(Locale.ROOT, "fauna selftest %s (%.1f, %.1f): kangaroos %s, anacondas %s; placed %d after %d spots%s",
                    p[0], lat, lon, k ? "yes" : "no", a ? "yes" : "no", placed, tried,
                    want == null ? (k || a ? " - FAIL (want none)" : " - ok") : placed > 0 ? " - ok" : " - FAIL"));
        }
        // 1b. the spawner on chunks nobody has generated before: a 9x9 patch somewhere new in the outback each run
        java.util.Random r = new java.util.Random();
        double flat = -27 + r.nextDouble() * 6, flon = 123 + r.nextDouble() * 14;
        int fx = (int) Math.floor(Earth.get().transform.blockX(flon)) >> 4, fz = (int) Math.floor(Earth.get().transform.blockZ(flat)) >> 4;
        int looked0 = Fauna.chunksLooked, placed0 = Fauna.placedInChunks, events0 = Fauna.generateEvents;
        for (int cx = -4; cx <= 4; cx++) for (int cz = -4; cz <= 4; cz++) level.setChunkForced(fx + cx, fz + cz, true);
        later(60, () -> {
            say(src, String.format(Locale.ROOT, "fauna selftest new chunks at %.2f, %.2f: %d generated, the spawner looked at %d and placed %d groups (about %.1f expected)",
                    flat, flon, Fauna.generateEvents - events0, Fauna.chunksLooked - looked0, Fauna.placedInChunks - placed0, (Fauna.chunksLooked - looked0) * Fauna.Species.KANGAROO.chunkChance));
            for (int cx = -4; cx <= 4; cx++) for (int cz = -4; cz <= 4; cz++) level.setChunkForced(fx + cx, fz + cz, false);
        });
        // 2. behaviour, on a stone pad in the sky over the outback
        int px = (int) Math.floor(Earth.get().transform.blockX(133.5)), pz = (int) Math.floor(Earth.get().transform.blockZ(-23.9));
        level.getChunk(px >> 4, pz >> 4);
        int py = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, px, pz) + 40;
        for (int dx = -10; dx <= 10; dx++) for (int dz = -10; dz <= 10; dz++) {
            level.setBlockAndUpdate(new BlockPos(px + dx, py - 1, pz + dz), Blocks.STONE.defaultBlockState());
            for (int dy = 0; dy < 5; dy++) level.setBlockAndUpdate(new BlockPos(px + dx, py + dy, pz + dz), Blocks.AIR.defaultBlockState());
        }
        for (int cx = -1; cx <= 1; cx++) for (int cz = -1; cz <= 1; cz++) level.setChunkForced((px >> 4) + cx, (pz >> 4) + cz, true);
        FakePlayer fp = FakePlayer.get(level);
        fp.snapTo(px + 0.5, py, pz + 0.5, 0f, 0f);
        fp.setGameMode(GameType.SURVIVAL);
        Kangaroo boomer = kangaroo(level, px + 3, py, pz, true), doe = kangaroo(level, px - 3, py, pz, false), calm = kangaroo(level, px, py, pz + 4, true);
        double doeAt = doe.position().distanceTo(fp.position());
        // a second stand-in who stays in creative (game mode is checked when the goal runs, not when the hit lands)
        FakePlayer builder = FakePlayer.get(level, new com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes("wb-creative".getBytes()), "wb_creative"));
        builder.snapTo(px + 0.5, py, pz + 2.5, 0f, 0f);
        builder.setGameMode(GameType.CREATIVE);
        // hit them once they have lived a few ticks (a hurt-by timestamp of 0 reads as "never hurt" to vanilla's goal)
        later(5, () -> {
            boomer.hurtServer(level, level.damageSources().playerAttack(fp), 1f);
            doe.hurtServer(level, level.damageSources().playerAttack(fp), 1f);
            calm.hurtServer(level, level.damageSources().playerAttack(builder), 1f);
        });
        later(35, () -> {
            say(src, "fauna selftest kangaroos: a male hit by a player targets him: " + (boomer.getTarget() == fp) + " (want true); a female flees "
                    + String.format(Locale.ROOT, "%.1f -> %.1f blocks", doeAt, doe.position().distanceTo(fp.position())) + " and targets nobody: " + (doe.getTarget() == null)
                    + "; a male hit by a creative player targets nobody: " + (calm.getTarget() == null));
            boomer.discard(); doe.discard(); calm.discard();
            // the anaconda: a pig beside it
            Anaconda snake = Fauna.ANACONDA.create(level, EntitySpawnReason.COMMAND);
            snake.snapTo(px + 0.5, py, pz - 4.5, 0f, 0f);
            snake.finalizeSpawn(level, level.getCurrentDifficultyAt(snake.blockPosition()), EntitySpawnReason.COMMAND, null);
            snake.setLength(6f);
            level.addFreshEntity(snake);
            var pig = EntityTypes.PIG.create(level, EntitySpawnReason.COMMAND);
            pig.snapTo(px + 0.5, py, pz - 2.8, 0f, 0f);
            pig.setNoAi(true);
            level.addFreshEntity(pig);
            float pigHp = pig.getHealth();
            snake.setTarget(pig);
            fp.setGameMode(GameType.CREATIVE);
            boolean creativeOk = !snake.canAttack(fp);
            fp.setGameMode(GameType.SURVIVAL);
            boolean survivalOk = snake.canAttack(fp);
            Difficulty was = level.getDifficulty();
            server.setDifficulty(Difficulty.PEACEFUL, true);
            boolean peacefulOk = !snake.canAttack(fp);
            server.setDifficulty(was, true);
            int[] heldSeen = {0};
            boolean[] slowed = {false};
            for (int t = 5; t <= 100; t += 5) later(t, () -> {
                if (snake.held() == pig) heldSeen[0]++;
                if (pig.hasEffect(MobEffects.SLOWNESS)) slowed[0] = true;
            });
            later(110, () -> {
                say(src, String.format(Locale.ROOT, "fauna selftest anaconda (6 blocks, %.0f hp): the pig was held %d of 20 checks, slowed %s, pig %s (%.0f -> %.0f hp)",
                        snake.getMaxHealth(), heldSeen[0], slowed[0], pig.isAlive() ? "alive" : "dead", pigHp, Math.max(0, pig.getHealth())));
                say(src, "fauna selftest anaconda targets: creative player " + (creativeOk ? "never" : "YES - FAIL") + ", survival player "
                        + (survivalOk ? "can" : "NO - FAIL") + ", on Peaceful " + (peacefulOk ? "never" : "YES - FAIL"));
                snake.discard();
                if (pig.isAlive()) pig.discard();
                for (int cx = -1; cx <= 1; cx++) for (int cz = -1; cz <= 1; cz++) level.setChunkForced((px >> 4) + cx, (pz >> 4) + cz, false);
                say(src, "fauna selftest: done; the spawner has looked at " + Fauna.chunksLooked + " new chunks and placed " + Fauna.placedInChunks
                        + " groups in them (" + Fauna.placedNearPlayers + " near players); chunk-generate events " + Fauna.generateEvents + ", fauna on " + Fauna.enabled());
            });
        });
    }

    private static Kangaroo kangaroo(ServerLevel level, int x, int y, int z, boolean male) {
        Kangaroo k = Fauna.KANGAROO.create(level, EntitySpawnReason.COMMAND);
        k.snapTo(x + 0.5, y, z + 0.5, 0f, 0f);
        k.finalizeSpawn(level, level.getCurrentDifficultyAt(k.blockPosition()), EntitySpawnReason.COMMAND, null);
        k.setMale(male);
        k.setAge(0);
        level.addFreshEntity(k);
        return k;
    }
}
