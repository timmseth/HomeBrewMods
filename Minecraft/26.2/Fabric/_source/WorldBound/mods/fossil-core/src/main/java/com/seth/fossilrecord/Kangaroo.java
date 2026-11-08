package com.seth.fossilrecord;

import com.seth.worldbound.Earth;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.EatBlockGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A kangaroo. Red kangaroos in the dry interior, eastern greys in the wetter east and south (chosen from the climate
 * where it spawns). They graze in mobs, bound off when frightened, and a grown male ("boomer") stands his ground and
 * kicks back when hit, hard enough to knock you flying. They step up whole blocks in their stride. Wheat or grass to
 * tempt and breed them.
 */
public class Kangaroo extends Animal {
    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(Kangaroo.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> MALE = SynchedEntityData.defineId(Kangaroo.class, EntityDataSerializers.BOOLEAN);
    private static final byte EVENT_KICK = 4, EVENT_EAT = 10;

    /** Client: ticks left of the kick (entity event 4) and of grazing (event 10), for the model. */
    public int kickTicks, eatTicks;

    public Kangaroo(EntityType<? extends Kangaroo> type, Level level) { super(type, level); }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 18.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.ATTACK_KNOCKBACK, 1.6)
                .add(Attributes.STEP_HEIGHT, 1.0)
                .add(Attributes.SAFE_FALL_DISTANCE, 6.0)
                .add(Attributes.FOLLOW_RANGE, 20.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.5, true) {
            @Override public boolean canUse() { return fighter() && super.canUse(); }
            @Override public boolean canContinueToUse() { return fighter() && super.canContinueToUse(); }
        });
        goalSelector.addGoal(2, new PanicGoal(this, 1.8) {
            @Override public boolean canUse() { return !standsGround() && super.canUse(); }
        });
        goalSelector.addGoal(3, new BreedGoal(this, 1.0));
        goalSelector.addGoal(4, new TemptGoal(this, 1.1, this::isFood, false));
        goalSelector.addGoal(5, new FollowParentGoal(this, 1.15));
        goalSelector.addGoal(6, new EatBlockGoal(this));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override public boolean canUse() { return fighter() && super.canUse(); }
        });
    }

    /** A grown male, away from Peaceful: he fights back. */
    boolean fighter() {
        return isMale() && !isBaby() && level().getDifficulty() != Difficulty.PEACEFUL;
    }

    /** A fighter who has someone to fight doesn't run (one hit by a creative player, who can't be fought, still runs). */
    private boolean standsGround() {
        LivingEntity by = getLastHurtByMob();
        return fighter() && by != null && by.canBeSeenAsEnemy();
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        level.broadcastEntityEvent(this, EVENT_KICK);
        playSound(Fauna.KANGAROO_KICK, 1.0f, 0.85f + random.nextFloat() * 0.3f);
        return super.doHurtTarget(level, target);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_KICK) kickTicks = 10;
        else if (id == EVENT_EAT) eatTicks = 40;
        else super.handleEntityEvent(id);
    }

    @Override
    public void aiStep() {
        if (level().isClientSide()) {
            if (kickTicks > 0) kickTicks--;
            if (eatTicks > 0) eatTicks--;
        }
        super.aiStep();
    }

    @Override
    public boolean isFood(ItemStack stack) { return stack.is(Items.WHEAT) || stack.is(Items.SHORT_GRASS); }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        Kangaroo joey = Fauna.KANGAROO.create(level, EntitySpawnReason.BREEDING);
        if (joey != null) {
            joey.setVariant(other instanceof Kangaroo k && random.nextBoolean() ? k.getVariant() : getVariant());
            joey.setMale(random.nextBoolean());
        }
        return joey;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, SpawnGroupData data) {
        int variant = random.nextInt(2);
        if (Earth.earthWorld()) {
            try { variant = Habitat.kangarooVariant(Earth.get().at(getBlockX(), getBlockZ()).climate()); } catch (RuntimeException ignored) { }
        }
        setVariant(variant);
        setMale(random.nextFloat() < 0.4f);
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    // ---- what it is

    public int getVariant() { return entityData.get(VARIANT); }
    public void setVariant(int v) { entityData.set(VARIANT, v); }
    public boolean isMale() { return entityData.get(MALE); }
    public void setMale(boolean m) { entityData.set(MALE, m); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(VARIANT, Habitat.GREY);
        b.define(MALE, false);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("Variant", getVariant());
        out.putBoolean("Male", isMale());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        setVariant(in.getIntOr("Variant", Habitat.GREY));
        setMale(in.getBooleanOr("Male", false));
    }

    // ---- sounds (WorldBound's sounds.json points them at vanilla recordings, pitched)

    @Override protected SoundEvent getAmbientSound() { return Fauna.KANGAROO_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return Fauna.KANGAROO_HURT; }
    @Override protected SoundEvent getDeathSound() { return Fauna.KANGAROO_DEATH; }
    @Override public int getAmbientSoundInterval() { return 300; }
}
