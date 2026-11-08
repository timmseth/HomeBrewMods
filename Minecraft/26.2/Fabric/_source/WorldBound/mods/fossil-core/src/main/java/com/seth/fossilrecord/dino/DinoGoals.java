package com.seth.fossilrecord.dino;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/** The goals every dinosaur gets, chosen by its temper and diet (see {@link Dino}). */
public final class DinoGoals {
    private DinoGoals() {}

    static void register(Dino d, DinoSpecies s, net.minecraft.world.entity.ai.goal.GoalSelector goals, net.minecraft.world.entity.ai.goal.GoalSelector targets) {
        double run = s.runModifier();
        boolean groups = s.groupMax > 1;
        goals.addGoal(0, new FloatGoal(d));
        if (s.tameable()) {                                              // a tame raptor stays when told, follows otherwise
            goals.addGoal(1, new net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal(d));
            goals.addGoal(6, new net.minecraft.world.entity.ai.goal.FollowOwnerGoal(d, Math.max(1.0, run * 0.8), 10f, 3f));
        }
        if (s.fights()) {
            goals.addGoal(1, new Attack(d, s, run));
            if (s.temper == DinoSpecies.Temper.PACK) goals.addGoal(1, new LeapAtTargetGoal(d, Math.min(0.5f, 0.25f + s.body.hip * 0.2f)));
        }
        // the young of every kind, and plant-eaters that don't fight, run from what would eat them
        goals.addGoal(2, new Flee(d, run));
        goals.addGoal(3, new AvoidEntityGoal<>(d, LivingEntity.class, Math.max(10f, 6f + s.body.L), Math.max(1.0, run * 0.8), run,
                e -> (!d.fighter() || s.temper == DinoSpecies.Temper.SKITTISH || s.temper == DinoSpecies.Temper.SKULKER) && d.threatens(e)));
        goals.addGoal(4, new BreedGoal(d, 1.0));
        goals.addGoal(5, new TemptGoal(d, 1.15, d::isFood, false));
        goals.addGoal(6, new FollowParentGoal(d, 1.2));
        if (groups) goals.addGoal(7, new Herd(d, s));
        if (!s.carnivore()) goals.addGoal(8, new Graze(d));
        goals.addGoal(9, s.aquatic ? new RandomStrollGoal(d, 1.0, 100) : new WaterAvoidingRandomStrollGoal(d, 1.0));
        goals.addGoal(10, new LookAtPlayerGoal(d, Player.class, Math.max(8f, s.body.L)));
        goals.addGoal(11, new RandomLookAroundGoal(d));

        if (s.fights()) targets.addGoal(1, new HurtBy(d));
        if (s.tameable() && s.fights()) {                                // ...and defends its owner
            targets.addGoal(1, new net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal(d));
            targets.addGoal(2, new net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal(d));
        }
        if (s.carnivore() && s.temper != DinoSpecies.Temper.DEFENDER) {
            if (s.temper != DinoSpecies.Temper.SKULKER)
                targets.addGoal(2, new NearestAttackableTargetGoal<>(d, Player.class, 10, true, false, (p, lvl) -> d.wantsPlayer((Player) p)));
            if (s.temper == DinoSpecies.Temper.PACK) targets.addGoal(3, new Pack(d));
            targets.addGoal(4, new NearestAttackableTargetGoal<>(d, LivingEntity.class, 20, true, false, (e, lvl) -> d.isPrey(e)));
        }
    }

    /** A fighter hit by something it can fight doesn't run. */
    static boolean standsGround(Dino d) {
        LivingEntity by = d.getLastHurtByMob();
        return d.fighter() && by != null && by.canBeSeenAsEnemy() && d.canAttack(by);
    }

    /** Runs when hurt (unless it stands its ground): away from whatever hurt it, as far again as it is long. */
    static final class Flee extends PanicGoal {
        final Dino d;

        Flee(Dino d, double speed) { super(d, speed); this.d = d; }

        @Override public boolean canUse() { return !standsGround(d) && super.canUse(); }

        @Override
        protected boolean findRandomPosition() {
            LivingEntity by = d.getLastHurtByMob();
            if (by == null) return super.findRandomPosition();
            int far = (int) Math.max(12, Math.min(32, 10 + d.sp.body.L * 1.5));
            Vec3 to = awayFrom(d, by.position(), far);
            if (to == null) to = net.minecraft.world.entity.ai.util.LandRandomPos.getPosAway(d, far, 7, by.position());
            // never a random spot (it might be toward the attacker): try again next tick
            if (to == null) return false;
            posX = to.x; posY = to.y; posZ = to.z;
            return true;
        }
    }

    /**
     * A spot on the ground roughly {@code far} blocks directly away from a threat (within 50 degrees either side),
     * found by dropping each candidate column to its floor near the animal's own level; null if none is open.
     * (Vanilla's random positions try random heights too, and on flat ground about half the time find nothing.)
     */
    static Vec3 awayFrom(Dino d, Vec3 threat, int far) {
        double dx = d.getX() - threat.x, dz = d.getZ() - threat.z;
        double base = dx * dx + dz * dz < 1e-4 ? d.getRandom().nextDouble() * Math.PI * 2 : Math.atan2(dz, dx);
        int y0 = net.minecraft.util.Mth.floor(d.getY());
        var nav = d.getNavigation();
        var p = new net.minecraft.core.BlockPos.MutableBlockPos();
        for (int i = 0; i < 12; i++) {
            double a = base + (d.getRandom().nextDouble() - 0.5) * Math.toRadians(100), r = far * (0.65 + 0.35 * d.getRandom().nextDouble());
            int x = net.minecraft.util.Mth.floor(d.getX() + Math.cos(a) * r), z = net.minecraft.util.Mth.floor(d.getZ() + Math.sin(a) * r);
            if (!d.level().hasChunkAt(p.set(x, y0, z))) continue;
            for (int y = y0 + 4; y >= y0 - 6; y--) {
                p.set(x, y, z);
                if (net.minecraft.world.entity.ai.util.GoalUtils.isSolid(d, p)) break;            // hit rock above any floor: give up on this column
                if (!nav.isStableDestination(p)) continue;
                if (net.minecraft.world.entity.ai.util.GoalUtils.hasMalus(d, p) || (!d.sp.aquatic && net.minecraft.world.entity.ai.util.GoalUtils.isWater(d, p))) break;
                return Vec3.atBottomCenterOf(p);
            }
        }
        return null;
    }

    /** Melee, at the species' pace, while it is a fighter and its target is fair game. */
    static final class Attack extends MeleeAttackGoal {
        final Dino d;
        final DinoSpecies s;

        Attack(Dino d, DinoSpecies s, double speed) { super(d, speed, true); this.d = d; this.s = s; }

        @Override public boolean canUse() { return d.fighter() && !d.isOrderedToSit() && fair() && super.canUse(); }
        @Override public boolean canContinueToUse() { return d.fighter() && fair() && super.canContinueToUse(); }
        private boolean fair() { LivingEntity t = d.getTarget(); return t != null && d.canAttack(t); }
        @Override protected int getAttackInterval() { return Math.max(12, d.attackLength() + 6); }
    }

    /** What the last herd call did (read by the in-game tests). */
    public static volatile String lastAlert = "";

    /** Hit back, and the herd (same species only) joins in. */
    static final class HurtBy extends HurtByTargetGoal {
        final Dino d;

        HurtBy(Dino d) { super(d); this.d = d; }

        @Override public boolean canUse() { return d.fighter() && super.canUse() && d.getLastHurtByMob() != null && d.canAttack(d.getLastHurtByMob()); }

        @Override
        public void start() {
            super.start();
            LivingEntity t = d.getLastHurtByMob();
            if (t == null) return;
            double r = 16 + d.sp.body.L;
            int found = 0, joined = 0;
            StringBuilder why = new StringBuilder();
            for (Dino o : d.level().getEntitiesOfClass(Dino.class, d.getBoundingBox().inflate(r, 8, r), o -> o != d && d.kin(o) && !o.isOrderedToSit())) {
                found++;
                if (o.getTarget() == null && o.fighter() && o.canAttack(t)) { o.setTarget(t); joined++; }
                else why.append(o.getTarget() != null ? " busy with " + o.getTarget().getType().toShortString() : !o.fighter() ? " young" : " can't attack");
            }
            lastAlert = d.sp.id + " called " + joined + " of " + found + " herdmates" + why;
        }

        @Override protected void alertOthers() { }
    }

    /** Pack hunters take up a packmate's quarry. */
    static final class Pack extends TargetGoal {
        final Dino d;
        LivingEntity quarry;

        Pack(Dino d) { super(d, false); this.d = d; setFlags(EnumSet.of(Flag.TARGET)); }

        @Override
        public boolean canUse() {
            if (d.getTarget() != null || d.isBaby() || d.isOrderedToSit() || d.tickCount % 10 != 0) return false;
            for (Dino o : d.level().getEntitiesOfClass(Dino.class, d.getBoundingBox().inflate(24, 8, 24), o -> o != d && d.kin(o))) {
                LivingEntity t = o.getTarget();
                if (t != null && t.isAlive() && d.canAttack(t)) { quarry = t; return true; }
            }
            return false;
        }

        @Override public void start() { d.setTarget(quarry); super.start(); }
    }

    /** Herd animals drift back toward the others of their kind when they stray. */
    static final class Herd extends Goal {
        final Dino d;
        final DinoSpecies s;
        Vec3 to;
        int wait;

        Herd(Dino d, DinoSpecies s) { this.d = d; this.s = s; setFlags(EnumSet.of(Flag.MOVE)); }

        @Override
        public boolean canUse() {
            if (--wait > 0 || d.getTarget() != null || d.isTame()) return false;
            wait = 40 + d.getRandom().nextInt(40);
            double r = 48;
            List<Dino> kin = d.level().getEntitiesOfClass(Dino.class, d.getBoundingBox().inflate(r, 12, r), o -> o != d && o.getType() == d.getType());
            if (kin.isEmpty()) return false;
            double x = 0, y = 0, z = 0;
            for (Dino o : kin) { x += o.getX(); y += o.getY(); z += o.getZ(); }
            Vec3 c = new Vec3(x / kin.size(), y / kin.size(), z / kin.size());
            double keep = Math.max(8, s.body.L * 1.3);
            if (c.distanceTo(d.position()) < keep) return false;
            to = c;
            return true;
        }

        @Override public void start() { d.getNavigation().moveTo(to.x, to.y, to.z, 1.0); }
        @Override public boolean canContinueToUse() { return !d.getNavigation().isDone() && d.getTarget() == null; }
    }

    /** Plant-eaters stop now and then to crop the ground (nothing is eaten from the world). */
    static final class Graze extends Goal {
        final Dino d;
        int left;

        Graze(Dino d) { this.d = d; setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP)); }

        @Override public boolean canUse() { return d.onGround() && d.getTarget() == null && d.getRandom().nextInt(d.isBaby() ? 600 : 300) == 0; }

        @Override
        public void start() {
            left = 80;
            d.getNavigation().stop();
            d.graze();
        }

        @Override public boolean canContinueToUse() { return left > 0 && d.getTarget() == null && d.getLastHurtByMob() == null; }
        @Override public void tick() { left--; }
    }

    static AABB around(Mob m, double r) { return m.getBoundingBox().inflate(r, r / 2, r); }
}
