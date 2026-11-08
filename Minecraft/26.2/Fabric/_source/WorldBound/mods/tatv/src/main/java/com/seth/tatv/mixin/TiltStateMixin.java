package com.seth.tatv.mixin;

import com.seth.tatv.client.TiltState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntityRenderState.class)
public abstract class TiltStateMixin implements TiltState {
    @Unique private boolean tatv$on;
    @Unique private float tatv$yaw, tatv$pitch, tatv$roll;
    @Unique private double tatv$dx, tatv$dy, tatv$dz;

    @Override
    public void tatv$set(boolean on, float yaw, float pitch, float roll, double dx, double dy, double dz) {
        tatv$on = on; tatv$yaw = yaw; tatv$pitch = pitch; tatv$roll = roll; tatv$dx = dx; tatv$dy = dy; tatv$dz = dz;
    }
    @Override public boolean tatv$on() { return tatv$on; }
    @Override public float tatv$yaw() { return tatv$yaw; }
    @Override public float tatv$pitch() { return tatv$pitch; }
    @Override public float tatv$roll() { return tatv$roll; }
    @Override public double tatv$dx() { return tatv$dx; }
    @Override public double tatv$dy() { return tatv$dy; }
    @Override public double tatv$dz() { return tatv$dz; }
}
