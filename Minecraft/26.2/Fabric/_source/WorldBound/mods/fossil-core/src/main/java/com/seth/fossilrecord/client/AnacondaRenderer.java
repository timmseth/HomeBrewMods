package com.seth.fossilrecord.client;

import com.seth.worldbound.WorldBoundMod;
import com.seth.fossilrecord.Anaconda;
import com.seth.fossilrecord.AnacondaBody;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;

/**
 * Anacondas: the body's shape (AnacondaBody, world space) is turned into the model's own space every frame. The
 * entity renderer turns model space into the world by the body yaw, a (-1, -1, 1) flip and a 1.501-block drop, so a
 * world offset v from the snake's (interpolated) position lands in model pixels at 16 * (-q.x, 1.501 - q.y, q.z)
 * with q = v turned about Y by (bodyRot - 180) degrees.
 */
public class AnacondaRenderer extends MobRenderer<Anaconda, AnacondaRenderer.State, AnacondaModel> {
    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(com.seth.fossilrecord.FossilRecord.ID, "textures/entity/fauna/anaconda.png");
    static final int N = AnacondaBody.N;

    public static final class State extends LivingEntityRenderState {
        public final float[] px = new float[N + 1], py = new float[N + 1], pz = new float[N + 1];
        public final float[] yaw = new float[N], pitch = new float[N], len = new float[N], thick = new float[N + 1];
        public float headYaw, headPitch, jaw;
        public boolean laidOut;
    }

    public AnacondaRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new AnacondaModel(ctx.bakeLayer(FaunaClient.ANACONDA)), 0.6f);
    }

    @Override public State createRenderState() { return new State(); }

    @Override
    public void extractRenderState(Anaconda a, State s, float partial) {
        super.extractRenderState(a, s, partial);
        AnacondaBody b = a.body;
        layout(b.x, b.y, b.z, s.x, s.y, s.z, s.bodyRot, a.length(), s);
        s.jaw = a.isAggressive() || a.held() != null ? 1f : 0f;
    }

    /** Body points (world) into model-space segments; shared with the dev-time preview. */
    public static void layout(double[] wx, double[] wy, double[] wz, double ex, double ey, double ez, float bodyRot, float length, State s) {
        if (wx[0] == 0 && wy[0] == 0 && wz[0] == 0 && wx[N] == 0) { s.laidOut = false; return; }    // not shaped yet
        double phi = Math.toRadians(bodyRot - 180f), c = Math.cos(phi), sn = Math.sin(phi);
        double thick = AnacondaBody.thickness(length);
        for (int i = 0; i <= N; i++) {
            double vx = wx[i] - ex, vy = wy[i] - ey, vz = wz[i] - ez;
            double qx = vx * c + vz * sn, qz = -vx * sn + vz * c;
            s.px[i] = (float) (-qx * 16);
            s.py[i] = (float) ((1.501 - vy) * 16);
            s.pz[i] = (float) (qz * 16);
            s.thick[i] = (float) (thick * AnacondaBody.taper((double) i / N));
        }
        for (int i = 0; i < N; i++) {
            float dx = s.px[i + 1] - s.px[i], dy = s.py[i + 1] - s.py[i], dz = s.pz[i + 1] - s.pz[i];
            float d = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            s.len[i] = d / 16f;
            if (d < 1e-4f) { s.yaw[i] = i > 0 ? s.yaw[i - 1] : 0f; s.pitch[i] = i > 0 ? s.pitch[i - 1] : 0f; continue; }
            s.yaw[i] = (float) Math.atan2(dx, dz);
            s.pitch[i] = (float) -Math.asin(Math.max(-1f, Math.min(1f, dy / d)));
        }
        // the head looks along the neck, away from the body
        s.headYaw = s.yaw[0];
        s.headPitch = s.pitch[0];
        s.laidOut = true;
    }

    @Override
    protected AABB getBoundingBoxForCulling(Anaconda a) { return a.reach(); }

    @Override
    public Identifier getTextureLocation(State s) { return TEXTURE; }
}
