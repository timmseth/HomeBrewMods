package com.seth.tatv.client;

/** Added to LivingEntityRenderState: the TATV a rider sits in, so the rider leans with it. */
public interface TiltState {
    void tatv$set(boolean on, float yaw, float pitch, float roll, double dx, double dy, double dz);
    boolean tatv$on();
    float tatv$yaw();
    float tatv$pitch();
    float tatv$roll();
    double tatv$dx();
    double tatv$dy();
    double tatv$dz();
}
