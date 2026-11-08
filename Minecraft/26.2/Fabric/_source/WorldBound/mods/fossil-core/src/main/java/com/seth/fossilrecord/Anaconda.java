package com.seth.fossilrecord;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A green anaconda: four to eight blocks long, and now and then a giant of ten or more. It lies coiled at the water's
 * edge or swims, hunts small animals when it's hungry, hisses at anyone who comes near, and strikes whoever steps
 * right up to it (never creative players, and never on Peaceful) or hits it. What it bites, it wraps up and squeezes:
 * slowed to a crawl and hurt every second until the hold runs out, the victim breaks loose by hurting it, or one of them
 * dies. Slow on land, quick in the water, and it never needs to come up for air.
 */
public class Anaconda extends PathfinderMob {
    private static final EntityDataAccessor<Float> LENGTH = SynchedEntityData.defineId(Anaconda.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> HELD = SynchedEntityData.defineId(Anaconda.class, EntityDataSerializers.INT);

    /** Client: the body's shape, updated every tick. */
    public final AnacondaBody body = new AnacondaBody();
    private int holdTicks, hungry = 1200, hissWait;
    private Vec3 lastPos = Vec3.ZERO;

    public Anaconda(EntityType<? extends Anaconda> type, Level level) {
        super(type, level);
        setPathfindingMalus(PathType.WATER, 0f);
        moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.6f, 1.0f, false);
        lookControl = new SmoothSwimmingLookControl(this, 20);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.16)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.ARMOR, 2.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.STEP_HEIGHT, 1.0)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) { return new AmphibiousPathNavigation(this, level); }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3, true) {
            @Override public boolean canUse() { return held() == null && super.canUse(); }
            @Override public boolean canContinueToUse() { return held() == null && super.canContinueToUse(); }
        });
        goalSelector.addGoal(5, new RandomSwimmingGoal(this, 1.0, 60));
        goalSelector.addGoal(6, new RandomStrollGoal(this, 0.8, 240));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6f));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // an ambush: anyone who walks right up to it
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 5, true, false, (t, lvl) -> t.distanceToSqr(this) < 3.2 * 3.2));
        // when hungry, small animals
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Animal.class, 40, true, false, (t, lvl) -> hungry <= 0 && prey(t)));
    }

    private boolean prey(LivingEntity t) {
        if (t instanceof Anaconda || t instanceof net.minecraft.world.entity.TamableAnimal tame && tame.isTame()) return false;
        return t.getBbWidth() * t.getBbHeight() <= 1.6;
    }

    /** Never a player on Peaceful; never a creative or spectating player (vanilla's own rule, via canBeSeenAsEnemy). */
    @Override
    public boolean canAttack(LivingEntity target) {
        if (target instanceof Player && level().getDifficulty() == Difficulty.PEACEFUL) return false;
        return target.canBeSeenAsEnemy() && super.canAttack(target);
    }

    // ---- size

    public float length() { return entityData.get(LENGTH); }

    public void setLength(float blocks) {
        entityData.set(LENGTH, blocks);
        AttributeInstance hp = getAttribute(Attributes.MAX_HEALTH), dmg = getAttribute(Attributes.ATTACK_DAMAGE);
        if (hp != null) hp.setBaseValue(Math.round(14 + 5.5 * blocks));
        if (dmg != null) dmg.setBaseValue(3 + 0.6 * blocks);
        setHealth(getMaxHealth());
        refreshDimensions();
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        float l = length();
        float h = Mth.clamp(0.35f + 0.05f * l, 0.55f, 0.95f);
        return EntityDimensions.scalable(Mth.clamp(0.25f * l, 0.9f, 2.6f), h).withEyeHeight(h * 0.7f);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        if (LENGTH.equals(key)) refreshDimensions();
        super.onSyncedDataUpdated(key);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, SpawnGroupData data) {
        // most are 4-8 blocks (as big green anacondas go); one in eight is a giant of 9-11
        float l = random.nextFloat() < 0.125f ? 9f + random.nextFloat() * 2.2f : 4f + random.nextFloat() * 4f;
        setLength(l);
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    // ---- the hold

    /** What it is squeezing, or null. */
    public LivingEntity held() {
        int id = entityData.get(HELD);
        return id >= 0 && level().getEntity(id) instanceof LivingEntity le ? le : null;
    }

    private boolean canHold(LivingEntity t) {
        if (t instanceof Player p && (p.isCreative() || p.isSpectator())) return false;
        return t.isAlive() && t.getBbHeight() <= 2.3 && t.getBbWidth() <= 1.6;
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        playSound(Fauna.ANACONDA_HISS, 1.0f, 0.75f + random.nextFloat() * 0.15f);
        if (hit && target instanceof LivingEntity le && held() == null && canHold(le)) {
            entityData.set(HELD, le.getId());
            holdTicks = 80 + (int) (length() * 8);
            getNavigation().stop();
        }
        return hit;
    }

    private void release() {
        entityData.set(HELD, -1);
        holdTicks = 0;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (hungry > 0) hungry--;
        if (hissWait > 0) hissWait--;
        LivingEntity held = held();
        if (held != null) {
            boolean peacefulPlayer = held instanceof Player && level.getDifficulty() == Difficulty.PEACEFUL;
            if (!canHold(held) || peacefulPlayer || held.distanceToSqr(this) > 5 * 5 || --holdTicks <= 0) { release(); return; }
            getNavigation().stop();
            getLookControl().setLookAt(held, 30f, 30f);
            held.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 10, 5, false, false, true));
            Vec3 pull = position().subtract(held.position());
            Vec3 v = held.getDeltaMovement();
            held.setDeltaMovement(new Vec3(v.x * 0.2, Math.min(v.y, 0.0), v.z * 0.2).add(pull.lengthSqr() > 0.6 ? pull.normalize().scale(0.06) : Vec3.ZERO));
            held.hurtMarked = true;
            if (tickCount % 20 == 0) {
                held.hurtServer(level, damageSources().mobAttack(this), (float) (1.5 + 0.25 * length()));
                if (!held.isAlive()) { hungry = 4800; heal(8f); release(); }
            }
            return;
        }
        // a warning first: a hiss and a stare at a player who comes close
        if (hissWait <= 0 && getTarget() == null) {
            Player p = level.getNearestPlayer(this, 6.5);
            if (p != null && canAttack(p)) {
                playSound(Fauna.ANACONDA_HISS, 0.8f, 0.85f + random.nextFloat() * 0.2f);
                getLookControl().setLookAt(p, 30f, 30f);
                hissWait = 100 + random.nextInt(80);
            }
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean hurt = super.hurtServer(level, source, amount);
        // what it holds can fight its way out
        LivingEntity held = held();
        if (hurt && held != null && source.getEntity() == held && random.nextFloat() < 0.3f) release();
        return hurt;
    }

    // ---- moving: slow on land, quick in water, never short of air

    @Override
    public void travel(Vec3 input) {
        if (isInWater()) {
            moveRelative(getSpeed(), input);
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(0.9));
        } else {
            super.travel(input);
        }
    }

    @Override public boolean canBreatheUnderwater() { return true; }
    @Override public boolean isPushedByFluid() { return false; }
    @Override public boolean removeWhenFarAway(double distance) { return false; }

    /** Its wandering keeps it by the water. */
    @Override
    public float getWalkTargetValue(net.minecraft.core.BlockPos pos, LevelReader level) {
        if (level.getFluidState(pos).is(FluidTags.WATER)) return 10f;
        for (net.minecraft.core.Direction d : net.minecraft.core.Direction.Plane.HORIZONTAL)
            if (level.getFluidState(pos.relative(d)).is(FluidTags.WATER)) return 6f;
        return 0f;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            LivingEntity held = held();
            boolean moving = position().distanceToSqr(lastPos) > 0.0004 || (isInWater() && getDeltaMovement().lengthSqr() > 0.0004);
            body.update(position(), yBodyRot, length(), held == null ? null : held.getBoundingBox(), moving);
            lastPos = position();
        }
    }

    // ---- what it is

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(LENGTH, 5f);
        b.define(HELD, -1);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putFloat("Length", length());
        out.putInt("Hungry", hungry);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        entityData.set(LENGTH, in.getFloatOr("Length", 5f));
        hungry = in.getIntOr("Hungry", 1200);
        refreshDimensions();
    }

    @Override protected SoundEvent getAmbientSound() { return random.nextInt(4) == 0 ? Fauna.ANACONDA_HISS : null; }
    @Override public int getAmbientSoundInterval() { return 500; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return Fauna.ANACONDA_HURT; }
    @Override protected SoundEvent getDeathSound() { return Fauna.ANACONDA_DEATH; }
    @Override protected float getSoundVolume() { return 0.8f; }
    @Override public float getVoicePitch() { return 1.15f - length() * 0.04f; }

    /** Bigger than its box: its body reaches out behind it (for the renderer's culling and the like). */
    public AABB reach() { return getBoundingBox().inflate(length() * 0.6, 1.0, length() * 0.6); }
}
