package com.seth.fossilrecord;

import com.seth.worldbound.Earth;
import com.seth.fossilrecord.dino.Dino;
import com.seth.fossilrecord.dino.DinoSpecies;
import com.seth.fossilrecord.dino.Dinos;
import com.seth.fossilrecord.dino.Roster;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.seth.fossilrecord.FaunaTests.later;
import static com.seth.fossilrecord.FaunaTests.say;

/**
 * /earth fauna dinotest: dinosaurs turn up at their fossil sites and nowhere else checked; then, on a stone pad in
 * the sky over Hell Creek: a fed T. rex goes for a survival player only up close (a crouching one closer still), a
 * hungry one from further, never a creative player or anyone on Peaceful; a hungry T. rex runs down a cow at about
 * its estimated top speed; a Triceratops hit by a player fights back and its herd joins in; a Gallimimus runs; a
 * pack of Velociraptors takes a sheep together; a hatchling is small and grows; and one of every species lives
 * through a few seconds of ticking.
 */
public final class DinoTests {
    private DinoTests() {}

    static int pass, fail;

    static void check(CommandSourceStack src, boolean ok, String what) {
        if (ok) pass++; else fail++;
        say(src, (ok ? "dinotest ok: " : "dinotest FAIL: ") + what);
    }

    public static void run(CommandSourceStack src) {
        ServerLevel level = src.getLevel();
        pass = fail = 0;
        if (!Earth.earthWorld()) { say(src, "dinotest: not an Earth world"); return; }
        if (Earth.get().generated()) say(src, "dinotest: on a generated planet the site checks don't apply; behaviour only");
        else sites(src, level);

        // ---- the pad, high over Hell Creek
        int px = (int) Math.floor(Earth.get().transform.blockX(-106.5)), pz = (int) Math.floor(Earth.get().transform.blockZ(47.4));
        int py = padY(level, px, pz);
        for (int cx = -3; cx <= 3; cx++) for (int cz = -3; cz <= 3; cz++) level.setChunkForced((px >> 4) + cx, (pz >> 4) + cz, true);
        for (int dx = -40; dx <= 40; dx++) for (int dz = -40; dz <= 40; dz++) {
            level.setBlock(new BlockPos(px + dx, py - 1, pz + dz), Blocks.STONE.defaultBlockState(), 2);
            for (int dy = 0; dy < 10; dy++) level.setBlock(new BlockPos(px + dx, py + dy, pz + dz), Blocks.AIR.defaultBlockState(), 2);
        }
        for (int dx = -41; dx <= 41; dx++) for (int dy = 0; dy < 3; dy++) {                  // a low wall so nothing walks off
            level.setBlock(new BlockPos(px + dx, py + dy, pz - 41), Blocks.STONE.defaultBlockState(), 2);
            level.setBlock(new BlockPos(px + dx, py + dy, pz + 41), Blocks.STONE.defaultBlockState(), 2);
            level.setBlock(new BlockPos(px - 41, py + dy, pz + dx), Blocks.STONE.defaultBlockState(), 2);
            level.setBlock(new BlockPos(px + 41, py + dy, pz + dx), Blocks.STONE.defaultBlockState(), 2);
        }
        // Forced chunks reach entity ticking a few ticks (or, with the chunk system busy after the site checks, seconds)
        // later; an animal placed before that stands frozen and the timed checks below misjudge it. Start when ready.
        whenTicking(src, level, px, pz, 0, () -> behaviour(src, level, px, py, pz));
    }

    static void whenTicking(CommandSourceStack src, ServerLevel level, int px, int pz, int waited, Runnable then) {
        int frozen = 0;
        for (int dx = -48; dx <= 48; dx += 16) for (int dz = -48; dz <= 48; dz += 16)
            if (!level.isPositionEntityTicking(new BlockPos(px + dx, 0, pz + dz))) frozen++;
        if (frozen == 0 || waited >= 1200) {
            if (frozen > 0) say(src, "dinotest: " + frozen + " pad chunks still not ticking after " + waited / 20 + " s; going ahead");
            else if (waited > 0) say(src, "dinotest: pad ready after " + waited + " ticks");
            then.run();
            return;
        }
        later(5, () -> whenTicking(src, level, px, pz, waited + 5, then));
    }

    static void behaviour(CommandSourceStack src, ServerLevel level, int px, int py, int pz) {
        FakePlayer fp = FakePlayer.get(level);
        fp.setGameMode(GameType.SURVIVAL);
        FakePlayer builder = FakePlayer.get(level, new com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes("wb-creative".getBytes()), "wb_creative"));
        builder.setGameMode(GameType.CREATIVE);

        // ---- 1. who the T. rex goes for
        Dino rex = spawn(level, "tyrannosaurus", px - 20, py, pz - 20);
        rex.setHunger(12000);
        fp.snapTo(px - 20 + 10, py, pz - 20, 0f, 0f);
        boolean near = rex.wantsPlayer(fp);
        fp.setShiftKeyDown(true);
        boolean crouchNear = rex.wantsPlayer(fp);
        fp.setShiftKeyDown(false);
        fp.snapTo(px - 20 + 26, py, pz - 20, 0f, 0f);
        boolean farFed = rex.wantsPlayer(fp);
        rex.setHunger(0);
        boolean farHungry = rex.wantsPlayer(fp);
        builder.snapTo(px - 20 + 6, py, pz - 20, 0f, 0f);
        boolean creative = rex.wantsPlayer(builder);
        Difficulty was = level.getDifficulty();
        level.getServer().setDifficulty(Difficulty.PEACEFUL, true);
        boolean peaceful = rex.wantsPlayer(fp) || rex.canAttack(fp);
        level.getServer().setDifficulty(was, true);
        check(src, near && !crouchNear && !farFed && farHungry && !creative && !peaceful, String.format(Locale.ROOT,
                "T. rex wants: survival player at 10 blocks %s (want yes), crouching %s (no), at 26 fed %s (no), at 26 hungry %s (yes), creative at 6 %s (no), on Peaceful %s (no)",
                near, crouchNear, farFed, farHungry, creative, peaceful));
        rex.discard();

        // ---- 2. (0-10 s) a hungry T. rex runs down a cow 30 blocks off; the only prey in reach
        Dino hunter = spawn(level, "tyrannosaurus", px - 35, py, pz + 30);
        hunter.setHunger(0);
        var cow = EntityTypes.COW.create(level, EntitySpawnReason.COMMAND);
        cow.snapTo(px - 5, py, pz + 30, 0f, 0f);
        cow.setNoAi(true);
        level.addFreshEntity(cow);
        double[] trace = {Double.NaN, 0, 0, 0};
        boolean[] aimed = {false};
        int[] aimedAt = {-1};
        double[] closest = {Double.MAX_VALUE};
        String[] diedOf = {null};
        for (int t = 2; t <= 200; t += 2) {
            int tt = t;
            later(t, () -> {
                if (hunter.getTarget() == cow && !aimed[0]) { aimed[0] = true; aimedAt[0] = tt; }
                if (cow.isAlive()) closest[0] = Math.min(closest[0], hunter.distanceTo(cow));
                else if (diedOf[0] == null) {                                       // who did it, for when it wasn't the rex
                    var src0 = cow.getLastDamageSource();
                    var by = src0 == null ? null : src0.getEntity();
                    diedOf[0] = (src0 == null ? "?" : src0.getMsgId()) + (by == null ? "" : " by " + (by == hunter ? "the rex" : by.getType().toShortString()
                            + String.format(Locale.ROOT, " at %.0f, %.0f", by.getX() - px, by.getZ() - pz))) + " (tick " + tt + ")";
                }
                // speed over 20 ticks of open running, starting 15 ticks after it picks the cow
                if (aimed[0] && Double.isNaN(trace[0])) { trace[0] = tt + 16; }
                if (!Double.isNaN(trace[0]) && tt == (int) trace[0]) { trace[1] = hunter.getX(); trace[2] = hunter.getZ(); }
                if (!Double.isNaN(trace[0]) && tt == (int) trace[0] + 20) trace[3] = Math.hypot(hunter.getX() - trace[1], hunter.getZ() - trace[2]);
            });
        }
        later(202, () -> {
            DinoSpecies s = hunter.sp;
            check(src, aimed[0] && !cow.isAlive(), String.format(Locale.ROOT, "hungry T. rex hunts a cow 30 blocks off: targeted %s (tick %d), came within %.1f blocks, cow %s, hunger now %d (fed), target now %s",
                    aimed[0], aimedAt[0], closest[0], cow.isAlive() ? "alive (" + (int) cow.getHealth() + " hp)" : "dead: " + diedOf[0], hunter.hunger(),
                    hunter.getTarget() == null ? "none" : hunter.getTarget().getType().toShortString()));
            check(src, trace[3] > s.run * 0.7 && trace[3] < s.run * 1.4, String.format(Locale.ROOT, "T. rex chase speed %.1f m/s, near its estimated %.1f", trace[3], s.run));
            hunter.discard();
            if (cow.isAlive()) cow.discard();
        });

        // ---- 3. (0-2.5 s, the far corner) a herd of Triceratops fights back; a Gallimimus runs; a creative player is ignored
        Dino trike = spawn(level, "triceratops", px + 20, py, pz - 30), trike2 = spawn(level, "triceratops", px + 28, py, pz - 30);
        Dino galli = spawn(level, "gallimimus", px + 5, py, pz - 20);
        Dino calm = spawn(level, "triceratops", px + 32, py, pz + 32);                 // out of the herd's hearing
        fp.snapTo(px + 20, py, pz - 23, 0f, 0f);
        builder.snapTo(px + 32, py, pz + 37, 0f, 0f);
        double galliAt = galli.distanceTo(fp);
        int[] joinedAt = {-1};
        double[] galliFar = {galliAt};
        for (int t = 6; t <= 50; t += 2) {
            int tt = t;
            later(t, () -> {
                if (joinedAt[0] < 0 && trike2.getTarget() == fp) joinedAt[0] = tt;
                galliFar[0] = Math.max(galliFar[0], galli.distanceTo(fp));
            });
        }
        later(5, () -> {
            trike.hurtServer(level, level.damageSources().playerAttack(fp), 1f);
            galli.hurtServer(level, level.damageSources().playerAttack(fp), 1f);
            calm.hurtServer(level, level.damageSources().playerAttack(builder), 1f);
        });
        later(50, () -> {
            check(src, trike.getTarget() == fp && trike2.getTarget() == fp, "Triceratops hit by a player targets him: " + (trike.getTarget() == fp) + ", its herdmate joins: " + (trike2.getTarget() == fp)
                    + (trike2.getTarget() == fp ? " (tick " + joinedAt[0] + ")" : " (target " + (trike2.getTarget() == null ? "none" : trike2.getTarget().getType().toShortString()) + ", "
                    + String.format(Locale.ROOT, "%.1f blocks from the first; last herd call: %s)", trike2.distanceTo(trike), com.seth.fossilrecord.dino.DinoGoals.lastAlert)));
            check(src, galli.getTarget() == null && galli.distanceTo(fp) > galliAt + 2, String.format(Locale.ROOT, "Gallimimus hit runs: %.1f -> %.1f blocks (furthest %.1f), targets nobody: %s", galliAt, galli.distanceTo(fp), galliFar[0], galli.getTarget() == null));
            check(src, calm.getTarget() == null, "Triceratops hit by a creative player targets nobody: " + (calm.getTarget() == null ? "true" : "false, it targets " + calm.getTarget().getName().getString()));
            trike.discard(); trike2.discard(); galli.discard(); calm.discard();
        });

        // ---- 4. (10-20 s) a pack of Velociraptors on a (tough) sheep
        later(210, () -> {
            List<Dino> pack = new ArrayList<>();
            for (int i = 0; i < 3; i++) { Dino v = spawn(level, "velociraptor", px - 25 + i * 2, py, pz - 25); v.setHunger(0); pack.add(v); }
            var sheep = EntityTypes.SHEEP.create(level, EntitySpawnReason.COMMAND);
            sheep.snapTo(px - 13, py, pz - 25, 0f, 0f);
            sheep.setNoAi(true);
            sheep.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(300);
            sheep.setHealth(300);
            level.addFreshEntity(sheep);
            int[] packOn = {0};
            for (int t = 10; t <= 160; t += 5) later(t, () -> {
                int n = 0;
                for (Dino v : pack) if (v.getTarget() == sheep) n++;
                packOn[0] = Math.max(packOn[0], n);
            });
            later(165, () -> {
                check(src, packOn[0] == 3, String.format(Locale.ROOT, "Velociraptor pack on a sheep: up to %d of 3 after it at once, sheep down to %.0f of 300 hp", packOn[0], Math.max(0, sheep.getHealth())));
                for (Dino v : pack) v.discard();
                sheep.discard();
            });
        });

        // ---- 5. a hatchling: small, and it grows
        Dino baby = spawn(level, "triceratops", px - 35, py, pz - 35);
        float adultW = baby.getBbWidth();
        baby.setAge(-baby.sp.growTicks());
        baby.updateGrowth();
        later(2, () -> {
            float small = baby.getBbWidth();
            baby.setAge(-baby.sp.growTicks() / 2);
            baby.updateGrowth();
            later(2, () -> {
                float mid = baby.getBbWidth();
                check(src, small < adultW * 0.4f && mid > small && mid < adultW, String.format(Locale.ROOT,
                        "Triceratops hatchling %.2f wide, half grown %.2f, adult %.2f (grows up over %d minutes)", small, mid, adultW, baby.sp.growTicks() / 1200));
                baby.discard();
            });
        });

        // ---- 6. (20-40 s) every species, fed, spawned across the pad and left to live for 20 seconds
        later(400, () -> {
            List<Dino> all = new ArrayList<>();
            int i = 0;
            for (DinoSpecies s : Roster.ALL) {
                int cols = (int) Math.ceil(Math.sqrt(Roster.ALL.size())), step = 72 / Math.max(1, cols - 1);
                int gx = px - 36 + (i % cols) * step, gz = pz - 36 + (i / cols) * step;             // all on the pad
                i++;
                Dino d = spawn(level, s.id, gx, py, gz);
                d.setHunger(24000);
                all.add(d);
            }
            later(400, () -> {
                int alive = 0;
                StringBuilder dead = new StringBuilder();
                for (Dino d : all) {
                    // one that wandered off the pad into an unloaded chunk is alive, just saved away with it
                    if (d.isAlive() || d.getRemovalReason() == net.minecraft.world.entity.Entity.RemovalReason.UNLOADED_TO_CHUNK) alive++;
                    else {   // what got it, for when this fails
                        var why = d.getLastDamageSource();
                        dead.append(' ').append(d.sp.id).append(" (").append(d.getRemovalReason()).append(why == null ? "" : ", " + why.getMsgId()
                                + (why.getEntity() == null ? "" : " by " + why.getEntity().getType().toShortString())).append(")");
                    }
                    d.discard();
                }
                check(src, alive == all.size(), "every species spawned and lived 20 s: " + alive + " of " + all.size() + (dead.length() > 0 ? " (gone:" + dead + ")" : ""));
                raptors(src, level, px, py, pz, fp, () -> {
                    for (int cx = -3; cx <= 3; cx++) for (int cz = -3; cz <= 3; cz++) level.setChunkForced((px >> 4) + cx, (pz >> 4) + cz, false);
                    say(src, "dinotest: done, " + pass + " passed, " + fail + " failed");
                });
            });
        });
    }

    /** /earth fauna dinospeed: every species walked, then run, along its own lane; measured against its estimates. */
    public static void speeds(CommandSourceStack src) {
        ServerLevel level = src.getLevel();
        int px = (int) Math.floor(Earth.get().transform.blockX(-106.5)), pz = (int) Math.floor(Earth.get().transform.blockZ(47.4));
        int py = padY(level, px, pz);
        for (int cx = -5; cx <= 5; cx++) for (int cz = -5; cz <= 5; cz++) level.setChunkForced((px >> 4) + cx, (pz >> 4) + cz, true);
        for (int dx = -70; dx <= 70; dx++) for (int dz = -40; dz <= 40; dz++) {
            level.setBlock(new BlockPos(px + dx, py - 1, pz + dz), Blocks.STONE.defaultBlockState(), 2);
            for (int dy = 0; dy < 10; dy++) level.setBlock(new BlockPos(px + dx, py + dy, pz + dz), Blocks.AIR.defaultBlockState(), 2);
        }
        List<DinoSpecies> all = new ArrayList<>(Roster.ALL);
        int perRound = 6;
        double[] sumRatio = {0, 0};
        int[] n = {0};
        for (int round = 0; round * perRound < all.size(); round++) {
            List<DinoSpecies> group = all.subList(round * perRound, Math.min(all.size(), (round + 1) * perRound));
            int start = round * 130;
            later(start + 1, () -> {
                List<Dino> ds = new ArrayList<>();
                for (int i = 0; i < group.size(); i++) {
                    Dino d = spawn(level, group.get(i).id, px - 65, py, pz - 35 + i * 13);
                    d.setHunger(999999);
                    d.setNoAi(false);
                    ds.add(d);
                }
                double[][] at = new double[ds.size()][4];
                // walk east at the walking pace, then run
                later(5, () -> { for (Dino d : ds) d.getNavigation().moveTo(px + 68, py, d.getZ(), 1.0); });
                later(25, () -> { for (int i = 0; i < ds.size(); i++) { at[i][0] = ds.get(i).getX(); } });
                later(45, () -> { for (int i = 0; i < ds.size(); i++) { at[i][1] = ds.get(i).getX();
                    Dino d = ds.get(i); d.getNavigation().moveTo(px + 68, py, d.getZ(), d.sp.runModifier()); } });
                later(65, () -> { for (int i = 0; i < ds.size(); i++) at[i][2] = ds.get(i).getX(); });
                later(85, () -> {
                    for (int i = 0; i < ds.size(); i++) {
                        Dino d = ds.get(i);
                        at[i][3] = d.getX();
                        double walk = (at[i][1] - at[i][0]), run = (at[i][3] - at[i][2]);
                        say(src, String.format(Locale.ROOT, "dinospeed %-18s walk %5.2f m/s (est %4.1f, x%.2f)  run %5.2f m/s (est %4.1f, x%.2f)",
                                d.sp.id, walk, d.sp.walk, walk / d.sp.walk, run, d.sp.run, run / d.sp.run));
                        if (run > 0.5 && d.sp.run < 9) { sumRatio[0] += walk / d.sp.walk; sumRatio[1] += run / d.sp.run; n[0]++; }
                        d.discard();
                    }
                });
            });
        }
        later((all.size() / perRound + 1) * 130 + 5, () -> {
            say(src, String.format(Locale.ROOT, "dinospeed: mean measured/estimate walk x%.2f, run x%.2f over %d species", sumRatio[0] / n[0], sumRatio[1] / n[0], n[0]));
            for (int cx = -5; cx <= 5; cx++) for (int cz = -5; cz <= 5; cz++) level.setChunkForced((px >> 4) + cx, (pz >> 4) + cz, false);
        });
    }

    /** 60 blocks over the ground, measured just outside the pad so an earlier run's pad doesn't lift it higher each time. */
    static int padY(ServerLevel level, int px, int pz) {
        level.getChunk(px >> 4, pz >> 4);
        int ground = Integer.MIN_VALUE;
        for (int cx = -1; cx <= 1; cx += 2) for (int cz = -1; cz <= 1; cz += 2) {
            level.getChunk((px + cx * 52) >> 4, (pz + cz * 52) >> 4);
            ground = Math.max(ground, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, px + cx * 52, pz + cz * 52));
        }
        return ground + 60;
    }

    /** ---- 7. (40-55 s) raptors as pets: calm at meat, tamed by it, sit/follow, defend, lay an egg, hatch tame. */
    static void raptors(CommandSourceStack src, ServerLevel level, int px, int py, int pz, FakePlayer fp, Runnable done) {
        List<net.minecraft.world.entity.Entity> made = new ArrayList<>();
        try {
            Dino w = spawn(level, "velociraptor", px + 10, py, pz + 10);
            made.add(w);
            w.setHunger(0);
            fp.setGameMode(GameType.SURVIVAL);
            fp.snapTo(px + 13, py, pz + 10, 0f, 0f);
            fp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
            boolean emptyHanded = w.wantsPlayer(fp);
            fp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BEEF, 64));
            boolean withMeat = w.wantsPlayer(fp);
            int feeds = 0;
            while (!w.isTame() && feeds < 40) { w.mobInteract(fp, net.minecraft.world.InteractionHand.MAIN_HAND); feeds++; }
            check(src, emptyHanded && !withMeat && w.isTame() && w.isOwnedBy(fp) && w.isOrderedToSit() && !w.wantsPlayer(fp),
                    String.format(Locale.ROOT, "hungry wild Velociraptor goes for an empty-handed player %s (yes), not one holding meat %s (no); tamed after %d feeds %s, sits %s",
                            emptyHanded, withMeat, feeds, w.isTame(), w.isOrderedToSit()));
            fp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
            w.mobInteract(fp, net.minecraft.world.InteractionHand.MAIN_HAND);
            boolean standsUp = !w.isOrderedToSit();

            // defence: the owner (a stand-in the level can look up) is hit by a zombie; the raptor turns on it
            var owner = EntityTypes.COW.create(level, EntitySpawnReason.COMMAND);
            owner.snapTo(px + 14, py, pz + 14, 0f, 0f);
            owner.setNoAi(true);
            level.addFreshEntity(owner);
            made.add(owner);
            Dino guard = spawn(level, "velociraptor", px + 16, py, pz + 16);
            made.add(guard);
            guard.setTame(true, false);
            guard.setOwner(owner);
            guard.setHunger(20000);
            var zombie = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
            zombie.snapTo(px + 14, py, pz + 20, 0f, 0f);
            zombie.setNoAi(true);
            level.addFreshEntity(zombie);
            made.add(zombie);
            // (vanilla notices an owner was hit by the hit's tick stamp, so not on the stand-in's first tick)
            later(10, () -> owner.hurtServer(level, level.damageSources().mobAttack(zombie), 1f));

            // two of fp's raptors, fed, lay an egg
            Dino m = spawn(level, "velociraptor", px + 25, py, pz - 10), f = spawn(level, "velociraptor", px + 27, py, pz - 10);
            made.add(m); made.add(f);
            m.tame(fp); f.tame(fp); m.setOrderedToSit(false); f.setOrderedToSit(false);
            m.setMale(true); f.setMale(false);
            m.setInLove(fp); f.setInLove(fp);
            later(70, () -> check(src, guard.getTarget() == zombie, "a tame Velociraptor goes for what hurt its owner: " + (guard.getTarget() == null ? "nothing" : guard.getTarget().getType().toShortString())));
            later(240, () -> {
                var eggs = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(px + 15, py - 2, pz - 20, px + 37, py + 4, pz),
                        e -> e.getItem().getItem() instanceof com.seth.fossilrecord.dino.DinoEggItem);
                DinoSpecies laid = eggs.isEmpty() ? null : com.seth.fossilrecord.dino.DinoEggItem.species(eggs.get(0).getItem());
                check(src, laid == Roster.get("velociraptor") && standsUp, "two fed tame Velociraptors lay " + (laid == null ? "no egg" : "a " + eggs.get(0).getItem().getHoverName().getString())
                        + "; the first stood up when told: " + standsUp);
                // hatching it: a tame hatchling of fp's
                fp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, com.seth.fossilrecord.dino.DinoEggItem.of(Roster.get("velociraptor"), 1));
                var hit = new net.minecraft.world.phys.BlockHitResult(new Vec3(px - 10.5, py, pz + 25.5), net.minecraft.core.Direction.UP, new BlockPos(px - 11, py - 1, pz + 25), false);
                var r = fp.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(fp, net.minecraft.world.InteractionHand.MAIN_HAND, hit));
                var babies = level.getEntitiesOfClass(Dino.class, new net.minecraft.world.phys.AABB(px - 14, py - 1, pz + 22, px - 8, py + 3, pz + 28));
                Dino baby = babies.isEmpty() ? null : babies.get(0);
                check(src, baby != null && baby.isBaby() && baby.isTame() && baby.isOwnedBy(fp) && fp.getMainHandItem().isEmpty(),
                        "a Velociraptor egg set down hatches a tame hatchling: " + (baby == null ? "nothing (" + r + ")" : "baby " + baby.isBaby() + ", yours " + baby.isOwnedBy(fp)));
                for (var e : eggs) e.discard();
                for (var b : babies) b.discard();
                for (var e : made) e.discard();
                done.run();
            });
        } catch (RuntimeException e) {
            check(src, false, "raptor taming test threw " + e);
            for (var x : made) x.discard();
            done.run();
        }
    }

    static Dino spawn(ServerLevel level, String id, int x, int y, int z) {
        Dino d = Dinos.type(Roster.get(id)).create(level, EntitySpawnReason.COMMAND);
        d.snapTo(x + 0.5, y, z + 0.5, 0f, 0f);
        d.finalizeSpawn(level, level.getCurrentDifficultyAt(d.blockPosition()), EntitySpawnReason.COMMAND, null);
        d.setAge(0);
        d.updateGrowth();
        level.addFreshEntity(d);
        return d;
    }

    /** Placement at the fossil sites (the real map), and nowhere at a few places without any. */
    static void sites(CommandSourceStack src, ServerLevel level) {
        Object[][] places = {
                {"tyrannosaurus", "Hell Creek, Montana", 47.6, -106.9}, {"triceratops", "Hell Creek, Montana", 47.3, -105.5},
                {"velociraptor", "Flaming Cliffs (Bayanzag), Gobi", 44.14, 103.73}, {"gallimimus", "Nemegt Basin, Gobi", 43.5, 101.0},
                {"spinosaurus", "Bahariya Oasis, Egypt", 28.3, 28.9}, {"spinosaurus", "Kem Kem, Morocco", 30.8, -4.4},
                {"argentinosaurus", "Plaza Huincul, Argentina", -38.93, -69.21}, {"iguanodon", "Bernissart, Belgium", 50.47, 3.65},
                {"stegosaurus", "Garden Park, Colorado", 38.55, -105.2}, {"compsognathus", "Solnhofen, Bavaria", 48.9, 11.0},
                {"dilophosaurus", "Kayenta, Arizona", 36.2, -111.3}, {"carnotaurus", "Chubut, Argentina", -42.8, -68.0},
                {"parasaurolophus", "Dinosaur Provincial Park, Alberta", 50.75, -111.5},
                {"albertosaurus", "Drumheller, Alberta", 51.47, -112.7}, {"carcharodontosaurus", "Kem Kem, Morocco", 30.9, -4.0},
                {"acrocanthosaurus", "Antlers Formation, Oklahoma", 34.2, -95.8}, {"utahraptor", "Cedar Mountain, Utah", 38.9, -109.9},
                {"deinonychus", "Cloverly Formation, Montana", 45.2, -108.6}, {"microraptor", "Jiufotang, Liaoning", 41.6, 120.4},
                {"coelophysis", "Ghost Ranch, New Mexico", 36.33, -106.47}, {"baryonyx", "Smokejacks pit, Surrey", 51.15, -0.35},
                {"giraffatitan", "Tendaguru, Tanzania", -9.7, 39.2}, {"mamenchisaurus", "Sichuan Basin", 29.6, 104.8},
                {"amargasaurus", "La Amarga, Neuquén", -39.45, -70.0}, {"plateosaurus", "Trossingen, Germany", 48.07, 8.63},
                {"maiasaura", "Egg Mountain, Montana", 47.9, -112.6}, {"ouranosaurus", "Gadoufaoua, Niger", 16.6, 9.1},
                {"pachyrhinosaurus", "Pipestone Creek, Alberta", 55.03, -119.1}, {"kentrosaurus", "Tendaguru, Tanzania", -9.75, 39.25},
                {"borealopelta", "Suncor mine, Fort McMurray", 57.0, -111.5}, {"psittacosaurus", "Lujiatun, Liaoning", 41.6, 121.2},
                {"megalosaurus", "Stonesfield, Oxfordshire", 51.85, -1.43}, {"concavenator", "Las Hoyas, Cuenca", 40.1, -2.05},
                {"herrerasaurus", "Valle de la Luna, San Juan", -30.0, -67.8}, {"australovenator", "Winton, Queensland", -22.4, 143.0},
                {"saltasaurus", "El Brete, Salta", -26.1, -65.4}, {"europasaurus", "Langenberg quarry, Oker", 51.9, 10.5},
                {"massospondylus", "Elliot Formation, Lesotho", -29.5, 28.0}, {"shantungosaurus", "Zhucheng, Shandong", 36.0, 119.4},
                {"olorotitan", "Kundur, Amur", 49.08, 130.85}, {"leaellynasaura", "Dinosaur Cove, Victoria", -38.7, 143.5},
                {"minmi", "Roma, Queensland", -26.6, 148.8}, {"scelidosaurus", "Charmouth, Dorset", 50.74, -2.85},
                {"zuul", "Judith River, near Havre", 48.5, -109.5}, {"alamosaurus", "Big Bend, Texas", 29.3, -103.4},
                {"pinacosaurus", "Bayan Mandahu, Inner Mongolia", 41.75, 106.75}, {"muttaburrasaurus", "Muttaburra, Queensland", -22.6, 144.55},
                {"miragaia", "Lourinhã, Portugal", 39.25, -9.3}, {"nigersaurus", "Gadoufaoua, Niger", 16.6, 9.1},
                {"kamuysaurus", "Hobetsu, Mukawa, Hokkaido", 42.78, 142.13}, {"fukuiraptor", "Kitadani, Fukui", 36.1, 136.55},
                {"rajasaurus", "Rahioli, Gujarat", 22.95, 73.3}, {"irritator", "Santana do Cariri, Ceará", -7.19, -39.74},
                {"balaur", "near Sebeș, Transylvania", 45.9, 23.5}, {"hungarosaurus", "Iharkút, Bakony", 47.22, 17.65},
                {"ampelosaurus", "Campagne-sur-Aude", 42.92, 2.2}, {"ichthyovenator", "Savannakhet, Laos", 16.5, 105.6},
                {"hadrosaurus", "Gloucester County, New Jersey", 39.7, -75.1}, {"anchiornis", "Jianchang, Liaoning", 40.8, 119.8},
                {"tsintaosaurus", "Laiyang, Shandong", 37.0, 120.6}, {"dreadnoughtus", "Santa Cruz, Patagonia", -50.0, -72.0},
                {"timurlengia", "Dzharakuduk, Kyzylkum", 42.1, 62.6}, {"jobaria", "near In Gall, Niger", 16.8, 7.0},
                {"kulindadromeus", "Kulinda, Transbaikal", 52.5, 116.6}, {"tethyshadros", "Villaggio del Pescatore, Trieste", 45.78, 13.6},
                {"anzu", "Marmarth, North Dakota", 46.33, -103.98}, {"sinosaurus", "Lufeng, Yunnan", 25.18, 102.08},
                {"megaraptor", "Sierra del Portezuelo, Neuquén", -38.9, -69.55}, {"aucasaurus", "Auca Mahuevo, Neuquén", -37.88, -68.52},
                {"lythronax", "Wahweap, Utah", 37.19, -111.65}, {"brachytrachelopan", "Cerro Cóndor, Chubut", -43.2, -69.1},
                {"hylaeosaurus", "Tilgate Forest, Sussex", 51.10, -0.16}, {"saichania", "Khulsan, Gobi", 43.51, 101.13},
                {"nothronychus", "Big Water, Utah", 37.08, -111.66}, {"austroraptor", "Bajo de Santa Rosa, Río Negro", -40.06, -66.80},
                {"eustreptospondylus", "Wolvercote, Oxford", 51.8, -1.3}, {"liliensternus", "Grosser Gleichberg, Thuringia", 50.4, 10.6},
                {"barapasaurus", "Pochampalli, Maharashtra", 18.9, 80.0}, {"atlasaurus", "Wawmda, High Atlas", 32.0, -6.3},
                {"riojasaurus", "Los Colorados, La Rioja", -29.8, -67.9}, {"thecodontosaurus", "Tytherington, near Bristol", 51.6, -2.5},
                {"amurosaurus", "near Blagoveshchensk, Amur", 50.3, 127.5}, {"charonosaurus", "Jiayin, Heilongjiang", 48.9, 130.4},
                {"wuerhosaurus", "Wuerho, Xinjiang", 46.0, 85.8}, {"gasparinisaura", "Cinco Saltos, Río Negro", -38.8, -68.0}};
        for (Object[] p : places) {
            DinoSpecies s = Roster.get((String) p[0]);
            double lat = (double) p[2], lon = (double) p[3];
            int x0 = (int) Math.floor(Earth.get().transform.blockX(lon)), z0 = (int) Math.floor(Earth.get().transform.blockZ(lat));
            int placed = 0, tried = 0;
            boolean listed = false;
            for (int t = 0; t < 49 && placed == 0; t++) {
                int x = x0 + (t % 7 - 3) * 24 + 8, z = z0 + (t / 7 - 3) * 24 + 8;
                level.getChunk(x >> 4, z >> 4);
                if (!Dinos.suits(s, x, z)) continue;
                tried++;
                listed = Dinos.here(x, z).contains(s);                     // the spawner's own lookup agrees
                placed = Dinos.spawnGroup(level, s, x, z, EntitySpawnReason.COMMAND);
            }
            int fx = x0, fz = z0;
            check(src, placed > 0 && listed, String.format(Locale.ROOT, "%s at %s (%.2f, %.2f): placed %d after %d suitable spots%s", s.shortName, p[1], lat, lon, placed, tried, listed ? "" : " (not listed by here())"));
            for (Mob m : level.getEntitiesOfClass(Dino.class, new net.minecraft.world.phys.AABB(fx - 300, level.getMinY(), fz - 300, fx + 300, level.getMaxY(), fz + 300))) m.discard();
        }
        Object[][] none = {{"Tokyo", 35.7, 139.7}, {"Sydney", -33.9, 151.2}, {"central Sahara", 23.0, 10.0}, {"Amazon", -3.3, -60.6}};
        for (Object[] p : none) {
            double lat = (double) p[1], lon = (double) p[2];
            int x = (int) Math.floor(Earth.get().transform.blockX(lon)), z = (int) Math.floor(Earth.get().transform.blockZ(lat));
            List<String> here = new ArrayList<>();
            for (DinoSpecies s : Roster.ALL) if (s.ranges(lat, lon)) here.add(s.id);
            check(src, here.isEmpty(), p[0] + " has no dinosaurs" + (here.isEmpty() ? "" : ": " + here));
        }
        // random placement: the chunk spawner itself puts a group somewhere with no fossil site, then the world's own
        // setting comes back
        Boolean was = null;
        int rx = (int) Math.floor(Earth.get().transform.blockX(10.0)), rz = (int) Math.floor(Earth.get().transform.blockZ(23.0));
        int got = 0, kinds = 0;
        try {
            Dinos.forcePlacement(true);
            for (int t = 0; t < 9 && got == 0; t++) {
                int x = rx + (t % 3 - 1) * 40, z = rz + (t / 3 - 1) * 40;
                level.getChunk(x >> 4, z >> 4);
                kinds = Dinos.here(x, z).size();
                got = Dinos.populate(level, x, z, 100);
            }
        } finally {
            Dinos.forcePlacement(was);
        }
        for (Mob m : level.getEntitiesOfClass(Dino.class, new net.minecraft.world.phys.AABB(rx - 200, level.getMinY(), rz - 200, rx + 200, level.getMaxY(), rz + 200))) m.discard();
        check(src, got > 0 && kinds == Roster.ALL.size(), "random placement: the chunk spawner placed a group in the central Sahara (any of " + kinds + " could)");
    }

    static Vec3 at(LivingEntity e) { return e.position(); }
}
