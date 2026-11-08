package com.seth.tatv;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

/**
 * The Truly All-Terrain Vehicle. Movement is simulated by the rider's client (like boats) from {@link Controls};
 * the server only simulates it when nobody is driving.
 *
 * Modes: DRIVE (ground/water with momentum: ramps, launches, jumps, climbs walls by driving into them),
 * HOVER (holds a height above ground or water), VTOL (lifts straight up, then flies at a held altitude),
 * GLIDE (elytra-style). On water it can SUBMERGE into a submarine. ANCHOR freezes it anywhere, walls included.
 */
public class TatvEntity extends VehicleEntity {
    public enum Mode { DRIVE, HOVER, VTOL, GLIDE }

    /** What the rider is doing this tick (filled in on the client). */
    public record Controls(double throttle, double steer, boolean up, boolean down, boolean jumpPressed, boolean boost) {
        public static final Controls NONE = new Controls(0, 0, false, false, false, false);
    }

    /** Set by the client entrypoint: reads keyboard / controller for the local rider. */
    public static java.util.function.Function<Player, Controls> CLIENT_CONTROLS = p -> Controls.NONE;
    /** Set by the client entrypoint: sends a {@link TatvAction} to the server. */
    public static java.util.function.BiConsumer<TatvEntity, Integer> CLIENT_ACTION = (t, a) -> {};

    public static final int ACT_MODE = 0, ACT_ANCHOR = 1, ACT_SUBMERGE = 2, ACT_LAND = 3, ACT_EXIT = 4;

    public static final float[] SPEEDS = {0.25f, 0.5f, 1f, 2f, 4f, 8f};

    private static final EntityDataAccessor<Integer> MODE = SynchedEntityData.defineId(TatvEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> ANCHORED = SynchedEntityData.defineId(TatvEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SUBMERGED = SynchedEntityData.defineId(TatvEntity.class, EntityDataSerializers.BOOLEAN);

    private final InterpolationHandler interpolation = new InterpolationHandler(this, 3);
    /** Client-side: chosen speed level (index into SPEEDS). */
    public int speedLevel = 2;
    private double hoverHeight = 3;
    /** Hull attitude (client only), degrees; pitch > 0 = nose up, roll > 0 = right side up. */
    public float visualPitch, visualPitchO, visualRoll, visualRollO;
    private float targetPitch;
    private boolean flightAttitude;
    private int climbTicks, jumpTicks, groundTicks, actionCooldown;
    private boolean wasGrounded, takingOff, wasSubmerged;
    private Mode lastMode;
    private double flyY = Double.NaN;
    private UUID owner;

    public TatvEntity(EntityType<? extends TatvEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
    }

    // ---- state -----------------------------------------------------------------------------

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(MODE, 0);
        b.define(ANCHORED, false);
        b.define(SUBMERGED, false);
    }

    public boolean submerged() {
        return entityData.get(SUBMERGED);
    }

    public void setSubmerged(boolean b) {
        entityData.set(SUBMERGED, b);
    }

    /** Water here or within a few blocks below (where SUBMERGE is allowed). */
    public boolean overWater() {
        BlockPos p = blockPosition();
        for (int i = 0; i < 5; i++) if (level().getFluidState(p.below(i)).is(net.minecraft.tags.FluidTags.WATER)) return true;
        return false;
    }

    public Mode mode() {
        return Mode.values()[Math.floorMod(entityData.get(MODE), Mode.values().length)];
    }

    public void setMode(Mode m) {
        entityData.set(MODE, m.ordinal());
    }

    public boolean anchored() {
        return entityData.get(ANCHORED);
    }

    public void setAnchored(boolean a) {
        entityData.set(ANCHORED, a);
    }

    public void setOwner(UUID u) {
        owner = u;
    }

    public UUID owner() {
        return owner;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        out.putInt("Mode", entityData.get(MODE));
        out.putBoolean("Anchored", anchored());
        out.putBoolean("Submerged", submerged());
        if (owner != null) out.putString("Owner", owner.toString());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        entityData.set(MODE, in.getIntOr("Mode", 0));
        setAnchored(in.getBooleanOr("Anchored", false));
        setSubmerged(in.getBooleanOr("Submerged", false));
        Optional<String> o = in.getString("Owner");
        o.ifPresent(s -> {
            try { owner = UUID.fromString(s); } catch (IllegalArgumentException ignored) {}
        });
    }

    @Override
    protected Item getDropItem() {
        return TatvMod.ITEM;
    }

    // ---- riding ----------------------------------------------------------------------------

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 at) {
        if (player.isSecondaryUseActive()) return InteractionResult.PASS;
        if (!level().isClientSide()) return player.startRiding(this) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean canAddPassenger(Entity e) {
        return getPassengers().size() < 2;
    }

    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player p ? p : null;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dims, float scale) {
        int i = getPassengers().indexOf(passenger);
        double z = i <= 0 ? -0.05 : -0.8;
        return new Vec3(0, dims.height() * 0.7, z).yRot(-getYRot() * Mth.DEG_TO_RAD);
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean canBeCollidedWith(Entity other) {
        return true;
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    public boolean isFlyingVehicle() {
        return true;
    }

    @Override
    public float maxUpStep() {
        return mode() == Mode.VTOL || mode() == Mode.GLIDE ? 0.6f : 1.6f;
    }

    @Override
    protected void checkFallDamage(double dy, boolean onGround, BlockState state, BlockPos pos) {
        resetFallDistance();
    }

    @Override
    public InterpolationHandler getInterpolation() {
        return interpolation;
    }

    // ---- simulation --------------------------------------------------------------------------

    @Override
    public void tick() {
        if (getHurtTime() > 0) setHurtTime(getHurtTime() - 1);
        if (getDamage() > 0) setDamage(getDamage() - 1);
        super.tick();
        interpolation.interpolate();
        if (isLocalInstanceAuthoritative()) {
            LivingEntity c = getControllingPassenger();
            if (c instanceof Player p && level().isClientSide()) drive(p, CLIENT_CONTROLS.apply(p));
            else idle();
            move(MoverType.SELF, getDeltaMovement());
        } else {
            setDeltaMovement(Vec3.ZERO);
        }
        for (Entity e : getPassengers()) {
            e.resetFallDistance();
            if (submerged() && !level().isClientSide()) e.setAirSupply(e.getMaxAirSupply()); // sealed cabin
        }
        resetFallDistance();
        if (!level().isClientSide() && submerged() && !isInWater() && !overWater()) setSubmerged(false);
        if (level().isClientSide()) updateAttitude();
    }

    /** Tilts the hull to match the ground under it (or the flight path), smoothly. */
    private void updateAttitude() {
        visualPitchO = visualPitch;
        visualRollO = visualRoll;
        float pitch = 0, roll = 0;
        if (flightAttitude && getControllingPassenger() != null) {
            pitch = targetPitch;
        } else if (climbTicks > 3) {
            pitch = 55;
        } else if (!submerged()) {
            Vec3 fwd = Vec3.directionFromRotation(0, getYRot());
            Vec3 right = new Vec3(-fwd.z, 0, fwd.x);
            double y0 = getY();
            double f = groundAt(getX() + fwd.x * 1.3, getZ() + fwd.z * 1.3), b = groundAt(getX() - fwd.x * 1.3, getZ() - fwd.z * 1.3);
            double r = groundAt(getX() + right.x * 0.9, getZ() + right.z * 0.9), l = groundAt(getX() - right.x * 0.9, getZ() - right.z * 0.9);
            boolean near = mode() == Mode.HOVER || y0 - Math.min(Math.min(f, b), Math.min(r, l)) < 2.5;
            if (near && !Double.isNaN(f + b)) pitch = (float) Math.toDegrees(Math.atan2(f - b, 2.6));
            if (near && !Double.isNaN(r + l)) roll = (float) Math.toDegrees(Math.atan2(r - l, 1.8));
            pitch = Mth.clamp(pitch, -40, 40);
            roll = Mth.clamp(roll, -30, 30);
        }
        visualPitch += (pitch - visualPitch) * 0.3f;
        visualRoll += (roll - visualRoll) * 0.3f;
    }

    /** Top of the ground (or water) at x,z, searching from 4 above to 8 below this craft; NaN if none. */
    private double groundAt(double x, double z) {
        Vec3 from = new Vec3(x, getY() + 4, z);
        HitResult r = level().clip(new ClipContext(from, from.add(0, -12, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this));
        return r.getType() == HitResult.Type.MISS ? Double.NaN : r.getLocation().y;
    }

    /** Nobody driving: anchored/hovering/flying craft stay put, a driving one settles to the ground or floats. */
    private void idle() {
        Vec3 v = getDeltaMovement();
        if (anchored() || submerged() || mode() != Mode.DRIVE) {
            setDeltaMovement(v.scale(0.7));
            return;
        }
        double vy = isInWater() ? v.y * 0.6 + 0.05 : v.y - 0.08;
        setDeltaMovement(v.x * (onGround() ? 0.6 : 0.95), vy * 0.98, v.z * (onGround() ? 0.6 : 0.95));
    }

    private void action(int a) {
        if (actionCooldown > 0) return;
        actionCooldown = 10;
        CLIENT_ACTION.accept(this, a);
    }

    private void drive(Player p, Controls c) {
        if (actionCooldown > 0) actionCooldown--;
        Mode m = mode();
        if (m != lastMode) {
            if (m == Mode.VTOL && lastMode != null) {           // take off straight up
                flyY = getY() + TatvConfig.get().vtol_takeoff_blocks;
                takingOff = true;
            }
            if (m == Mode.VTOL && lastMode == null) flyY = getY();
            lastMode = m;
        }
        if (anchored()) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        float lvl = SPEEDS[Mth.clamp(speedLevel, 0, SPEEDS.length - 1)];
        Vec3 v = getDeltaMovement();
        boolean sub = submerged();
        if (sub && !wasSubmerged) v = new Vec3(v.x, -0.4, v.z);  // dive
        wasSubmerged = sub;
        if (sub) v = submarine(p, c, v, lvl);
        else v = switch (m) {
            case DRIVE -> ground(p, c, v, lvl, false);
            case HOVER -> ground(p, c, v, lvl, true);
            case VTOL -> vtol(p, c, v, lvl);
            case GLIDE -> glide(p, c, v, lvl);
        };
        setDeltaMovement(v);
    }

    /** Car-style steering: turns the craft and the rider with it (reversed when backing up). */
    private void steerCar(Player p, Controls c, Vec3 v) {
        double hs = Math.hypot(v.x, v.z);
        double fwdSpeed = v.x * -Mth.sin(getYRot() * Mth.DEG_TO_RAD) + v.z * Mth.cos(getYRot() * Mth.DEG_TO_RAD);
        float turn = (float) (c.steer() * 4.5 * (0.35 + 0.65 * Math.min(1, hs / 0.35)) * (fwdSpeed < -0.02 ? -1 : 1));
        setYRot(getYRot() + turn);
        p.setYRot(p.getYRot() + turn);
        p.setYHeadRot(p.getYHeadRot() + turn);
        yRotO = getYRot();
    }

    /** The nose follows where the rider looks (VTOL, glide). */
    private void faceLook(Player p) {
        setYRot(p.getYRot());
        yRotO = getYRot();
    }

    private Vec3 ground(Player p, Controls c, Vec3 v, float lvl, boolean hover) {
        steerCar(p, c, v);
        flightAttitude = false;
        boolean boost = c.boost();
        Vec3 fwd = Vec3.directionFromRotation(0, getYRot());
        double y0 = getY();
        double below = groundAt(getX(), getZ());
        boolean grounded = onGround() || (!Double.isNaN(below) && y0 - below < 0.35);
        groundTicks = grounded ? 3 : Math.max(0, groundTicks - 1);
        boolean traction = hover || groundTicks > 0 || isInWater();

        double accel = 0.045 * lvl * (boost ? 2.2 : 1);
        double cap = 0.55 * lvl * (boost ? 3 : 1);
        Vec3 h = new Vec3(v.x, 0, v.z).add(fwd.scale(c.throttle() * accel * (traction ? 1 : 0.15)));
        // gravity along the slope: rolls you down hills, slows you up them
        if (!hover && groundTicks > 0) {
            double gx = groundAt(getX() + 0.8, getZ()) - groundAt(getX() - 0.8, getZ());
            double gz = groundAt(getX(), getZ() + 0.8) - groundAt(getX(), getZ() - 0.8);
            if (!Double.isNaN(gx + gz)) h = h.add(-Mth.clamp(gx / 1.6, -2, 2) * 0.05, 0, -Mth.clamp(gz / 1.6, -2, 2) * 0.05);
        }
        // grip: on the ground it goes where it points; drifts a little hovering, keeps its line in the air
        double along = h.dot(fwd);
        Vec3 side = h.subtract(fwd.scale(along));
        h = fwd.scale(along).add(side.scale(hover ? 0.5 : groundTicks > 0 ? 0.15 : 0.95));
        double hs = h.length();
        if (hs > cap) h = h.scale(Math.max(cap / hs, 0.97));    // over the limit (downhill, boost ended): bleed off gently
        double drag = Math.abs(c.throttle()) < 0.05 ? (traction ? 0.92 : 0.995) : (traction ? 0.99 : 0.995);
        h = h.scale(drag);
        hs = h.length();

        // look ahead along the path: lift to meet a slope instead of bonking into each step
        Vec3 dir = hs > 0.02 ? h.normalize() : fwd.scale(Math.signum(c.throttle()));
        double need = -1, nearRise = 0;
        if (hs > 0.02 || Math.abs(c.throttle()) > 0.05) {
            for (double d : new double[]{1.1, 1.1 + hs * 1.5, 1.1 + hs * 3}) {
                double a = groundAt(getX() + dir.x * d, getZ() + dir.z * d);
                double rise = Double.isNaN(a) ? 4.5 : a - y0;
                if (d == 1.1) nearRise = rise;
                if (rise > 0.05) need = Math.max(need, rise * Math.max(hs, 0.15) / d + 0.1);
            }
        }
        boolean climbing = horizontalCollision && c.throttle() > 0.1 && nearRise > 3.5;
        climbTicks = climbing ? climbTicks + 1 : 0;
        if (jumpTicks > 0) jumpTicks--;

        double vy = v.y;
        if (!hover) {
            if (climbing) vy = 0.35 * Math.sqrt(lvl) * (boost ? 1.6 : 1);
            else if (isInWater() && vy < 0.3) vy = vy * 0.6 + 0.06;
            else {
                vy = (vy - 0.08) * 0.98;
                if (groundTicks > 0 && need > 0 && nearRise <= 3.5) vy = Math.max(vy, Math.min(2.5, need));
                else if (groundTicks > 0 && jumpTicks == 0 && vy < 0.1 && hs < 0.6 && !boost && !Double.isNaN(below) && y0 - below < 3.5)
                    vy = Math.min(vy, -(y0 - below) * 0.5 - 0.15); // slow and not boosting: stay planted going downhill
            }
            // leaving a crest under boost: kick it into a proper launch
            if (wasGrounded && !grounded && boost && vy > 0.15) {
                vy *= 1.5;
                h = h.scale(1.1);
                jumpTicks = 10;
            }
            if (c.jumpPressed() && (groundTicks > 0 || isInWater() || climbing)) {
                vy = (0.75 + 0.15 * Math.sqrt(lvl)) * (boost ? 1.6 : 1) + hs * 0.15;
                jumpTicks = 10;
                groundTicks = 0;
            }
            if (c.down() && !grounded) vy -= 0.08;
            if (groundTicks == 0 && !isInWater() && (Double.isNaN(below) || y0 - below > 1.5)) {   // airborne: nose follows the arc
                flightAttitude = true;
                targetPitch = (float) Mth.clamp(Math.toDegrees(Math.atan2(vy, Math.max(0.05, hs))), -35, 35);
            }
        } else {
            if (c.up()) hoverHeight = Math.min(64, hoverHeight + 0.35);
            if (c.down()) hoverHeight = Math.max(1, hoverHeight - 0.35);
            double g = groundBelow();
            double target = g + hoverHeight + Math.max(0, need > 0 ? Math.min(nearRise, 4) : 0);
            vy = Mth.clamp((target - y0) * 0.2, -0.6, 1.0);
            if (climbing) vy = Math.max(vy, 0.4 * Math.sqrt(lvl));
        }
        wasGrounded = grounded;
        return new Vec3(h.x, vy, h.z);
    }

    /** Lifts straight up to the takeoff height, then flies at a held altitude. Jump = higher, sneak / down = lower. */
    private Vec3 vtol(Player p, Controls c, Vec3 v, float lvl) {
        faceLook(p);
        boolean boost = c.boost();
        double y0 = getY();
        if (Double.isNaN(flyY)) flyY = y0;
        double climbRate = 0.5 * (boost ? 2.5 : 1);
        if (c.up()) flyY += climbRate;
        if (c.down()) { flyY -= climbRate; takingOff = false; }
        if (takingOff && y0 >= flyY - 1.5) takingOff = false;
        double below = groundBelow();
        if (!c.down() && y0 - below < 3) flyY = Math.max(flyY, below + 3);   // terrain clearance
        if (horizontalCollision && !takingOff) flyY = Math.max(flyY, y0) + 0.8;   // ridge ahead: climb over it

        Vec3 fwd = Vec3.directionFromRotation(0, getYRot());
        Vec3 right = new Vec3(-fwd.z, 0, fwd.x);
        Vec3 h = new Vec3(v.x, 0, v.z);
        if (!takingOff) h = h.add(fwd.scale(c.throttle() * 0.06 * lvl * (boost ? 2.5 : 1))).add(right.scale(c.steer() * 0.04 * lvl));
        boolean input = Math.abs(c.throttle()) > 0.05 || Math.abs(c.steer()) > 0.05;
        h = h.scale(input ? 0.98 : 0.92);
        double cap = 0.9 * lvl * (boost ? 2.5 : 1);
        if (h.length() > cap) h = h.normalize().scale(cap);
        double vy = Mth.clamp((flyY - y0) * 0.15, -0.9, takingOff ? 1.2 : 0.9);
        flightAttitude = true;
        targetPitch = (float) Mth.clamp(-h.dot(fwd) / Math.max(0.3, cap) * 12, -12, 12);
        return new Vec3(h.x, vy, h.z);
    }

    /** Elytra physics (the same formulas as vanilla), with throttle as thrust and boost like a firework. */
    private Vec3 glide(Player p, Controls c, Vec3 v, float lvl) {
        faceLook(p);
        Vec3 look = p.getLookAngle();
        float pitchRad = p.getXRot() * Mth.DEG_TO_RAD;
        double hl = Math.sqrt(look.x * look.x + look.z * look.z);
        double hs = v.horizontalDistance();
        double lift = Mth.cos(pitchRad);
        lift = lift * lift;
        v = v.add(0, 0.08 * (-1 + lift * 0.75), 0);
        if (v.y < 0 && hl > 0) {
            double d = v.y * -0.1 * lift;
            v = v.add(look.x * d / hl, d, look.z * d / hl);
        }
        if (pitchRad < 0 && hl > 0) {
            double d = hs * -Mth.sin(pitchRad) * 0.04;
            v = v.add(-look.x * d / hl, d * 3.2, -look.z * d / hl);
        }
        if (hl > 0) v = v.add((look.x / hl * hs - v.x) * 0.1, 0, (look.z / hl * hs - v.z) * 0.1);
        v = v.multiply(0.99, 0.98, 0.99);
        if (c.throttle() > 0.05) v = v.add(look.scale(0.02 * c.throttle() * Math.sqrt(lvl)));
        if (c.boost()) {
            double t = 1.5 * Math.max(1, Math.sqrt(lvl));
            v = v.add(look.x * 0.1 + (look.x * t - v.x) * 0.5, look.y * 0.1 + (look.y * t - v.y) * 0.5, look.z * 0.1 + (look.z * t - v.z) * 0.5);
        }
        if (onGround() || isInWater()) action(ACT_LAND);   // touched down: back to drive
        flightAttitude = true;
        targetPitch = (float) Mth.clamp(Math.toDegrees(Math.atan2(v.y, Math.max(0.05, v.horizontalDistance()))), -45, 45);
        return v;
    }

    /** Submarine: steer like a boat, jump = rise, sneak / down = dive; holds its depth. Surfaces when it breaks the water. */
    private Vec3 submarine(Player p, Controls c, Vec3 v, float lvl) {
        steerCar(p, c, v);
        boolean boost = c.boost();
        Vec3 fwd = Vec3.directionFromRotation(0, getYRot());
        Vec3 h = new Vec3(v.x, 0, v.z).add(fwd.scale(c.throttle() * 0.035 * lvl * (boost ? 2 : 1)));
        double along = h.dot(fwd);
        h = fwd.scale(along).add(h.subtract(fwd.scale(along)).scale(0.4));
        double cap = 0.45 * lvl * (boost ? 2 : 1);
        if (h.length() > cap) h = h.normalize().scale(cap);
        if (Math.abs(c.throttle()) < 0.05) h = h.scale(0.9);
        double vy = v.y * 0.85 + (c.up() ? 0.05 : 0) - (c.down() ? 0.05 : 0);
        vy = Mth.clamp(vy, -0.5, 0.5);
        boolean waterAbove = level().getFluidState(BlockPos.containing(getX(), getY() + 1.2, getZ())).is(net.minecraft.tags.FluidTags.WATER);
        if (!waterAbove && vy > 0) {
            if (c.up()) action(ACT_SUBMERGE);   // pushing up at the surface: surface
            else vy = 0;
        }
        if (!isInWater() && !overWater()) action(ACT_SUBMERGE);
        flightAttitude = true;
        targetPitch = (float) Mth.clamp(vy * 40, -25, 25);
        return new Vec3(h.x, vy, h.z);
    }

    /** Height of the ground (or water surface) below, within 96 blocks. */
    private double groundBelow() {
        Vec3 from = position().add(0, 0.5, 0);
        Vec3 to = from.add(0, -96, 0);
        HitResult r = level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this));
        return r.getType() == HitResult.Type.MISS ? getY() - 96 : r.getLocation().y;
    }

    @Override
    protected void destroy(ServerLevel level, net.minecraft.world.damagesource.DamageSource source) {
        super.destroy(level, source);
    }
}
