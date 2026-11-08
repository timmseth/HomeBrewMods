package com.seth.fossilrecord.dino;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import java.util.Map;

/**
 * A dinosaur. One class for every species; what it is ({@link DinoSpecies}) decides its size, health, speed, blow,
 * food and behaviour:
 * <ul>
 * <li>hunters keep a hunger clock; when hungry they take prey from about a two-hundredth of their weight up to
 * their own (pack hunters, several times theirs), and they go for a player who comes too close or, hungry, one
 * in sight - never one in creative or spectator, never on Peaceful, and a crouching player is noticed at half
 * the distance;</li>
 * <li>plant-eaters that stand their ground fight back when hit and their herd joins in; the rest run from the
 * hunters that would eat them and from players who walk up on them (crouch to get close);</li>
 * <li>herd animals keep together; plant-eaters graze; big ones push through leaves (mob griefing on) and shake the
 * ground; the big hunters roar when they pick a target;</li>
 * <li>they breed with their food (ferns and leaves for plant-eaters, raw meat for hunters) and their young grow
 * slowly, a hatchling small enough to hold, the giants taking over an hour.</li>
 * </ul>
 */
public class Dino extends TamableAnimal {
    private static final EntityDataAccessor<Float> GROWTH = SynchedEntityData.defineId(Dino.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> MALE = SynchedEntityData.defineId(Dino.class, EntityDataSerializers.BOOLEAN);
    static final byte EV_ATTACK = 4, EV_EAT = 10, EV_ROAR = 61, EV_CALL = 62;

    public final DinoSpecies sp;

    /** Client: ticks left of each animation; distance walked and speed (blocks, blocks/tick) for the stride. */
    public int attackTicks, eatTicks, roarTicks, callTicks;
    public float gait, gaitO, gaitSpeed, gaitSpeedO;

    /** Server: ticks until a hunter is hungry again; cooldowns. */
    int hunger, roarCooldown, leafCooldown;
    /** Server: a tame female raptor lays an egg when this runs out, if she has been fed. */
    int eggTime = 12000 + (int) (Math.random() * 12000);

    public int hunger() { return hunger; }
    public void setHunger(int ticks) { hunger = ticks; }

    public Dino(EntityType<? extends Dino> type, Level level) {
        super(type, level);
        sp = Dinos.species(type);
        if (sp.aquatic) { setPathfindingMalus(PathType.WATER, 0f); setPathfindingMalus(PathType.WATER_BORDER, 0f); }
        hunger = random.nextInt(6000);
    }

    public static AttributeSupplier.Builder createAttributes(DinoSpecies s) {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, s.health())
                .add(Attributes.MOVEMENT_SPEED, s.speedAttribute())
                .add(Attributes.ATTACK_DAMAGE, s.damage())
                .add(Attributes.ATTACK_KNOCKBACK, s.knockback())
                .add(Attributes.KNOCKBACK_RESISTANCE, s.knockbackResistance())
                .add(Attributes.ARMOR, s.armor())
                .add(Attributes.STEP_HEIGHT, s.stepHeight())
                .add(Attributes.FOLLOW_RANGE, s.carnivore() ? 32.0 : 20.0)
                .add(Attributes.SAFE_FALL_DISTANCE, Math.max(3.0, s.body.hip * 1.5))
                .add(Attributes.WATER_MOVEMENT_EFFICIENCY, s.aquatic ? 0.8 : 0.0);
    }

    @Override
    protected void registerGoals() {
        DinoGoals.register(this, Dinos.species(getType()), goalSelector, targetSelector);
    }

    // ------------------------------------------------------------------ who it fights, hunts and runs from

    /** Roughly what an animal weighs, kilograms (dinosaurs by species and age, the rest from a table or their size). */
    static float massOf(LivingEntity e) {
        if (e instanceof Dino d) { float g = d.scaleNow(); return d.sp.mass * g * g * g; }
        if (e instanceof Player) return 75f;
        Float m = MASS.get(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath());
        if (m != null) return m;
        return Math.max(1f, e.getBbWidth() * e.getBbWidth() * e.getBbHeight() * 250f);
    }

    private static final Map<String, Float> MASS = Map.ofEntries(
            Map.entry("chicken", 2.5f), Map.entry("rabbit", 2f), Map.entry("cod", 1f), Map.entry("salmon", 3f), Map.entry("tropical_fish", 0.3f),
            Map.entry("pufferfish", 0.5f), Map.entry("frog", 0.5f), Map.entry("tadpole", 0.05f), Map.entry("parrot", 1f), Map.entry("bat", 0.05f),
            Map.entry("pig", 100f), Map.entry("sheep", 60f), Map.entry("cow", 600f), Map.entry("mooshroom", 600f), Map.entry("goat", 60f),
            Map.entry("horse", 500f), Map.entry("donkey", 250f), Map.entry("mule", 350f), Map.entry("llama", 140f), Map.entry("trader_llama", 140f),
            Map.entry("camel", 500f), Map.entry("fox", 6f), Map.entry("wolf", 40f), Map.entry("cat", 4f), Map.entry("ocelot", 10f),
            Map.entry("panda", 100f), Map.entry("polar_bear", 450f), Map.entry("armadillo", 5f), Map.entry("turtle", 100f), Map.entry("sniffer", 800f),
            Map.entry("axolotl", 0.2f), Map.entry("squid", 5f), Map.entry("glow_squid", 5f), Map.entry("dolphin", 150f), Map.entry("kangaroo", 50f),
            Map.entry("anaconda", 70f));

    /** Hungry, hunts this? Not players (see {@link #wantsPlayer}), pets, villagers, golems or monsters. */
    public boolean isPrey(LivingEntity e) {
        if (!sp.carnivore() || isBaby() || isTame() || hunger > 0 || e == this || !e.isAlive()) return false;
        if (e instanceof Player || e.getType() == getType() || e instanceof AbstractVillager || e instanceof net.minecraft.world.entity.monster.Enemy) return false;
        if (e instanceof TamableAnimal ta && ta.isTame()) return false;
        if (e instanceof net.minecraft.world.entity.animal.golem.AbstractGolem) return false;
        if (sp.diet == DinoSpecies.Diet.PISCIVORE && e instanceof net.minecraft.world.entity.animal.fish.AbstractFish) return true;
        float m = massOf(e);
        return m >= sp.minPrey() && m <= sp.maxPrey();
    }

    /** Would a hunter of this kind go for that animal, sized as it is? (Plant-eaters run from these.) */
    static boolean preysOn(Dino hunter, LivingEntity e) {
        if (!hunter.sp.carnivore() || hunter.isBaby() || hunter.sp.temper == DinoSpecies.Temper.DEFENDER) return false;
        float m = massOf(e);
        return m >= hunter.sp.minPrey() && m <= hunter.sp.maxPrey();
    }

    /** Goes for this player: hungry and the player in its range, or the player too close; never creative, spectators, Peaceful. */
    public boolean wantsPlayer(Player p) {
        if (!sp.carnivore() || sp.temper == DinoSpecies.Temper.SKULKER || sp.temper == DinoSpecies.Temper.DEFENDER || isBaby()) return false;
        if (isTame() || !canAttack(p)) return false;
        // a raptor watches someone holding out meat instead of going for them (unless they've hurt it)
        if (sp.tameable() && getLastHurtByMob() != p && (isFood(p.getMainHandItem()) || isFood(p.getOffhandItem()))) return false;
        double d = distanceTo(p);
        float sneak = p.isShiftKeyDown() || p.isCrouching() ? 0.5f : 1f;
        float territory = switch (sp.temper) { case APEX -> 11f; case HUNTER -> 7f; case PACK -> 5f; default -> 0f; };
        float hunt = hunger <= 0 ? switch (sp.temper) { case APEX -> 28f; case HUNTER -> 20f; case PACK -> 16f; default -> 0f; } : 0f;
        if (sp.temper == DinoSpecies.Temper.PACK && packmates(16) < 1) { hunt *= 0.5f; territory *= 0.6f; }
        territory += sp.body.L * 0.3f;
        return d < Math.max(territory, hunt) * sneak;
    }

    int packmates(double range) {
        return level().getEntitiesOfClass(Dino.class, getBoundingBox().inflate(range), d -> d != this && kin(d) && !d.isBaby()).size();
    }

    /** Herd or pack together: the same species, and either both wild or tamed by the same owner. */
    public boolean kin(Dino o) {
        if (o.getType() != getType() || o.isTame() != isTame()) return false;
        return !isTame() || java.util.Objects.equals(getOwnerReference(), o.getOwnerReference());
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (target instanceof Player && level().getDifficulty() == Difficulty.PEACEFUL) return false;
        return target.canBeSeenAsEnemy() && super.canAttack(target);
    }

    /** Fights at all right now (a grown animal of a kind that fights). */
    boolean fighter() { return !isBaby() && sp.fights(); }

    /** Runs from this (the plant-eaters that don't stand their ground, and every young one). */
    boolean threatens(LivingEntity e) {
        if (e == this || !e.isAlive()) return false;
        if (e instanceof Dino d) return d.getType() != getType() && preysOn(d, this);
        if (e instanceof Player p) return p.canBeSeenAsEnemy() && !p.isShiftKeyDown() && !p.isCrouching() && (sp.temper == DinoSpecies.Temper.SKITTISH || sp.temper == DinoSpecies.Temper.SKULKER || isBaby());
        return false;
    }

    /** Reach: a bite lands as far ahead as the snout, a tail as far as it swings. */
    public float reach() {
        Rig r = sp.rig();
        float half = r.width() / 2;
        float ahead = switch (sp.attack) {
            case TAIL -> Math.max(r.frontReach() * 0.5f, r.backReach() * 0.55f);
            default -> r.frontReach();
        };
        return Math.max(0.6f, Math.min(6f, ahead - half + 0.4f)) * scaleNow();
    }

    @Override
    public boolean isWithinMeleeAttackRange(LivingEntity target) {
        double r = reach();
        return getBoundingBox().inflate(r, Math.max(0.5, r * 0.5), r).intersects(target.getBoundingBox());
    }

    // ------------------------------------------------------------------ actions

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        level.broadcastEntityEvent(this, EV_ATTACK);
        playSound(Dinos.sound(sp, "attack"), Math.min(2f, getSoundVolume()), getVoicePitch());
        return super.doHurtTarget(level, target);
    }

    @Override
    public boolean killedEntity(ServerLevel level, LivingEntity victim, DamageSource source) {
        if (sp.carnivore()) {
            hunger = 9000 + random.nextInt(9000);
            heal(getMaxHealth() * 0.3f);
            level.broadcastEntityEvent(this, EV_EAT);
            if (getTarget() == victim) setTarget(null);
        }
        return super.killedEntity(level, victim, source);
    }

    @Override
    public void setTarget(LivingEntity target) {
        LivingEntity was = getTarget();
        super.setTarget(target);
        if (target != null && was == null && !level().isClientSide() && sp.temper == DinoSpecies.Temper.APEX && roarCooldown <= 0 && !isBaby()) {
            roarCooldown = 600 + random.nextInt(600);
            level().broadcastEntityEvent(this, EV_ROAR);
            playSound(Dinos.sound(sp, "roar"), Math.min(5f, getSoundVolume() * 1.8f), getVoicePitch());
        }
    }

    void graze() {
        level().broadcastEntityEvent(this, EV_EAT);
    }

    @Override
    public void playAmbientSound() {
        super.playAmbientSound();
        if (!level().isClientSide()) level().broadcastEntityEvent(this, EV_CALL);
    }

    @Override
    public void handleEntityEvent(byte id) {
        switch (id) {
            case EV_ATTACK -> attackTicks = attackLength();
            case EV_EAT -> eatTicks = 80;
            case EV_ROAR -> roarTicks = 50;
            case EV_CALL -> callTicks = 16;
            default -> super.handleEntityEvent(id);
        }
    }

    /** Ticks an attack animation lasts: big animals strike slower. */
    public int attackLength() { return Math.max(8, Math.min(24, (int) (6 + 2.2f * Math.log(Math.max(2, sp.mass))))); }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            gaitO = gait; gaitSpeedO = gaitSpeed;
            double dx = getX() - xo, dz = getZ() - zo;
            float d = (float) Math.sqrt(dx * dx + dz * dz);
            if (d > 2) d = 0;                                                 // a teleport, not a step
            gait += d;
            gaitSpeed += (d - gaitSpeed) * 0.35f;
            if (attackTicks > 0) attackTicks--;
            if (eatTicks > 0) eatTicks--;
            if (roarTicks > 0) roarTicks--;
            if (callTicks > 0) callTicks--;
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level() instanceof ServerLevel sl) {
            if (sp.carnivore() && hunger > 0) hunger--;
            if (roarCooldown > 0) roarCooldown--;
            // like a hen: a tame, grown, fed female raptor lays now and then
            if (sp.tameable() && isTame() && !isMale() && !isBaby() && hunger > 0 && --eggTime <= 0) {
                eggTime = 12000 + random.nextInt(12000);
                layEgg(sl);
            }
            if (tickCount % 20 == 0) updateGrowth();
            // big animals push through foliage (as ravagers do), when mobs may change the world
            if (sp.mass >= 1500 && !isBaby() && horizontalCollision && --leafCooldown <= 0 && sl.getGameRules().get(GameRules.MOB_GRIEFING)) {
                leafCooldown = 10;
                AABB box = getBoundingBox().inflate(0.25);
                for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
                    BlockState st = sl.getBlockState(pos);
                    if (st.getBlock() instanceof LeavesBlock) sl.destroyBlock(pos, true, this);
                }
            }
        }
    }

    // ------------------------------------------------------------------ growing up

    /** 0 for a hatchling, 1 grown. */
    public float growth() { return entityData.get(GROWTH); }

    /** Drawn and hit-boxed at this fraction of the adult's size. */
    public float scaleNow() {
        float h = sp.hatchlingScale();
        return h + (1f - h) * growth();
    }

    @Override
    public float getAgeScale() { return scaleNow(); }

    public void updateGrowth() {
        float g = getAge() >= 0 ? 1f : Math.max(0f, Math.min(1f, 1f + getAge() / (float) sp.growTicks()));
        if (Math.abs(g - growth()) > 0.004f || g == 1f && growth() != 1f) entityData.set(GROWTH, g);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (GROWTH.equals(key)) refreshDimensions();
    }

    @Override
    public void finalizeSpawnChildFromBreeding(ServerLevel level, Animal partner, AgeableMob baby) {
        super.finalizeSpawnChildFromBreeding(level, partner, baby);
        if (baby instanceof Dino d) { d.setAge(-sp.growTicks()); d.setMale(random.nextBoolean()); d.updateGrowth(); }
    }

    /** Breeding lays an egg (every dinosaur did) instead of giving birth: the female drops it. */
    @Override
    public void spawnChildFromBreeding(ServerLevel level, Animal partner) {
        Dino mother = isMale() && partner instanceof Dino p && !p.isMale() ? p : this;
        mother.layEgg(level);
        setAge(6000);
        partner.setAge(6000);
        resetLove();
        partner.resetLove();
        level.broadcastEntityEvent(this, (byte) 18);
        var cause = getLoveCause() != null ? getLoveCause() : partner.getLoveCause();
        if (cause != null) cause.awardStat(net.minecraft.stats.Stats.ANIMALS_BRED);
        net.minecraft.world.entity.ExperienceOrb.award(level, position(), random.nextInt(7) + 1);
    }

    void layEgg(ServerLevel level) {
        spawnAtLocation(level, DinoEggItem.of(sp, 1));
        playSound(net.minecraft.sounds.SoundEvents.TURTLE_LAY_EGG, 1f, 1.1f + random.nextFloat() * 0.2f);
    }

    /**
     * Raptors, like dogs: hand meat to a wild one and it may take to you (one in three); your own ones sit or stand
     * when you use them empty-handed or with anything but meat, heal on meat, and breed on meat when well.
     */
    @Override
    public net.minecraft.world.InteractionResult mobInteract(Player player, net.minecraft.world.InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!sp.tameable()) return super.mobInteract(player, hand);
        boolean food = isFood(held);
        if (isTame() && isOwnedBy(player)) {
            if (food && getHealth() < getMaxHealth()) {
                if (!level().isClientSide()) {
                    usePlayerItem(player, hand, held);
                    heal(Math.max(2f, getMaxHealth() * 0.25f));
                    hunger = 9000 + random.nextInt(9000);
                    level().broadcastEntityEvent(this, EV_EAT);
                }
                return net.minecraft.world.InteractionResult.SUCCESS;
            }
            if (food) {
                if (!level().isClientSide()) hunger = Math.max(hunger, 9000 + random.nextInt(9000));    // fed: she'll lay
                return super.mobInteract(player, hand);                        // breeding / growing up
            }
            if (!level().isClientSide()) {
                setOrderedToSit(!isOrderedToSit());
                jumping = false;
                getNavigation().stop();
                setTarget(null);
                player.sendOverlayMessage(net.minecraft.network.chat.Component.literal(sp.shortName + (isOrderedToSit() ? " stays" : " follows you")));
            }
            return net.minecraft.world.InteractionResult.SUCCESS;
        }
        if (!isTame() && food) {
            if (!level().isClientSide()) {
                usePlayerItem(player, hand, held);
                hunger = 9000 + random.nextInt(9000);
                level().broadcastEntityEvent(this, EV_EAT);
                if (random.nextInt(3) == 0) {
                    tame(player);
                    getNavigation().stop();
                    setTarget(null);
                    setLastHurtByMob(null);
                    setOrderedToSit(true);
                    level().broadcastEntityEvent(this, (byte) 7);
                    player.sendOverlayMessage(net.minecraft.network.chat.Component.literal(sp.shortName + " is yours. Use it to have it follow or stay."));
                } else level().broadcastEntityEvent(this, (byte) 6);
            }
            return net.minecraft.world.InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        @SuppressWarnings("unchecked") EntityType<Dino> t = (EntityType<Dino>) getType();
        return t.create(level, EntitySpawnReason.BREEDING);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, SpawnGroupData data) {
        SpawnGroupData out = super.finalizeSpawn(level, difficulty, reason, data);
        setMale(random.nextBoolean());
        if (getAge() == -24000) setAge(-sp.growTicks());                  // vanilla's baby age: make it this species' own
        updateGrowth();
        return out;
    }

    @Override
    public boolean isFood(ItemStack s) {
        boolean plants = s.is(Items.FERN) || s.is(Items.LARGE_FERN) || s.is(ItemTags.LEAVES) || s.is(Items.SWEET_BERRIES);
        boolean meat = s.is(Items.BEEF) || s.is(Items.PORKCHOP) || s.is(Items.MUTTON) || s.is(Items.CHICKEN) || s.is(Items.RABBIT);
        return switch (sp.diet) {
            case HERBIVORE -> plants;
            case CARNIVORE -> meat;
            case PISCIVORE -> meat || s.is(Items.COD) || s.is(Items.SALMON);
            case OMNIVORE -> plants || meat || s.is(Items.EGG);
        };
    }

    @Override
    public boolean removeWhenFarAway(double distance) { return false; }

    // ------------------------------------------------------------------ what it is

    public boolean isMale() { return entityData.get(MALE); }
    public void setMale(boolean m) { entityData.set(MALE, m); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(GROWTH, 1f);
        b.define(MALE, false);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putBoolean("Male", isMale());
        out.putInt("Hunger", hunger);
        out.putInt("EggTime", eggTime);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        setMale(in.getBooleanOr("Male", false));
        hunger = in.getIntOr("Hunger", 3000);
        eggTime = in.getIntOr("EggTime", eggTime);
        updateGrowth();
    }

    // ------------------------------------------------------------------ sounds

    @Override protected SoundEvent getAmbientSound() { return Dinos.sound(sp, "ambient"); }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return Dinos.sound(sp, "hurt"); }
    @Override protected SoundEvent getDeathSound() { return Dinos.sound(sp, "death"); }
    @Override public int getAmbientSoundInterval() { return 200 + (int) Math.min(400, sp.mass / 40); }
    @Override protected float getSoundVolume() { return sp.volume() * (0.4f + 0.6f * scaleNow()); }
    @Override public float getVoicePitch() { return super.getVoicePitch() * (isBaby() ? 1.6f : 1f); }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        if (sp.mass >= 2500 && !isBaby()) playSound(net.minecraft.sounds.SoundEvents.RAVAGER_STEP, Math.min(2.5f, sp.mass / 6000f + 0.4f), 0.55f + random.nextFloat() * 0.15f);
        else if (sp.mass >= 300 && !isBaby()) playSound(net.minecraft.sounds.SoundEvents.CAMEL_STEP, 0.6f, 0.7f + random.nextFloat() * 0.2f);
        else super.playStepSound(pos, state);
    }
}
