package com.seth.tatv.client;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class TatvRenderState extends EntityRenderState {
    public float yRot, pitch, roll, hurtTime, damageTime;
    public int hurtDir, mode;
    public boolean anchored;
}
