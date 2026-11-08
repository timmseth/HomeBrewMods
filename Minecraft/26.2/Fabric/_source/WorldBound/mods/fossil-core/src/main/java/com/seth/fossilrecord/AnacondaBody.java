package com.seth.fossilrecord;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Iterator;

/**
 * The snake's body as a chain of points from the head (0) to the tail (N), in world space: it trails along the path the
 * head has taken while the snake moves, settles into a flat coil (head raised in the middle) once it has rested a
 * little while, and winds up whatever it is squeezing in a helix from the victim's shoulders down. Updated once a tick
 * on the client; the renderer lays the body's segments between these points. No Minecraft client classes, so the
 * dev-time preview can use it too.
 */
public final class AnacondaBody {
    /** Segments drawn. */
    public static final int N = 24;
    /** Points head .. tail (N + 1 of them), world coordinates. */
    public final double[] x = new double[N + 1], y = new double[N + 1], z = new double[N + 1];
    private final double[] wx = new double[N + 1], wy = new double[N + 1], wz = new double[N + 1];
    /** Where the head has been, newest first (world positions, about a tenth of a block apart). */
    private final ArrayDeque<Vec3> trail = new ArrayDeque<>();
    private int still = 0;
    private boolean ready;

    /** Body thickness along its length, as a share of the thickest part: a narrow neck, a heavy middle, a thin tail. */
    public static double taper(double u) {
        if (u < 0.08) return 0.62 + u / 0.08 * 0.2;
        if (u < 0.35) return 0.82 + (u - 0.08) / 0.27 * 0.18;
        if (u < 0.62) return 1.0;
        return Math.max(0.18, 1.0 - (u - 0.62) / 0.38 * 0.82);
    }

    /** How thick (blocks) a snake of this length is at its thickest. */
    public static double thickness(double length) { return 0.18 + 0.05 * length; }

    /**
     * One tick. head: the entity's position; yawDeg: its body yaw; held: the box of what it squeezes (or null);
     * moving: it moved this tick (or is swimming about).
     */
    public void update(Vec3 head, float yawDeg, double length, AABB held, boolean moving) {
        double step = length / N, thick = thickness(length);
        Vec3 last = trail.peekFirst();
        if (last == null || last.distanceToSqr(head) > 0.01) trail.addFirst(head);
        trimTrail(length * 1.4);
        still = moving ? 0 : still + 1;
        double alpha;
        boolean trailing = false;
        if (held != null) { wrap(head, held, length, thick, step); alpha = 0.35; }
        else if (still > 30 || trail.size() < 3) { coil(head, yawDeg, length, thick); alpha = ready ? 0.12 : 1; }
        else { follow(head, yawDeg, step, thick); alpha = still > 0 ? 0.5 : 1; trailing = true; }
        for (int i = 0; i <= N; i++) {
            if (!ready) { x[i] = wx[i]; y[i] = wy[i]; z[i] = wz[i]; continue; }
            x[i] += (wx[i] - x[i]) * alpha;
            y[i] += (wy[i] - y[i]) * alpha;
            z[i] += (wz[i] - z[i]) * alpha;
        }
        if (trailing) { x[0] = head.x; y[0] = head.y + 0.5 * thick * taper(0); z[0] = head.z; }
        ready = true;
        // once it has settled, its coil becomes its trail: moving off again, it unrolls from the coil
        if (still == 60) {
            trail.clear();
            for (int i = 0; i <= N; i++) trail.addLast(new Vec3(x[i], y[i] - 0.5 * thick * taper((double) i / N), z[i]));
        }
    }

    private void trimTrail(double keep) {
        double run = 0;
        Vec3 prev = null;
        for (Iterator<Vec3> it = trail.iterator(); it.hasNext(); ) {
            Vec3 p = it.next();
            if (prev != null) run += p.distanceTo(prev);
            if (run > keep) { it.remove(); while (it.hasNext()) { it.next(); it.remove(); } break; }
            prev = p;
        }
    }

    /** Along the path the head took, at even spacing; past its end, straight on back the way it faced. */
    private void follow(Vec3 head, float yawDeg, double step, double thick) {
        Vec3[] pts = trail.toArray(new Vec3[0]);
        int k = 0;
        double along = 0;
        Vec3 a = head;
        double yaw = Math.toRadians(yawDeg);
        Vec3 back = new Vec3(Math.sin(yaw), 0, -Math.cos(yaw));   // behind the head
        for (int i = 0; i <= N; i++) {
            double want = i * step;
            while (k < pts.length && along + a.distanceTo(pts[k]) < want) { along += a.distanceTo(pts[k]); a = pts[k]; k++; }
            Vec3 p;
            if (k < pts.length) {
                double seg = a.distanceTo(pts[k]);
                p = seg < 1e-6 ? a : a.add(pts[k].subtract(a).scale((want - along) / seg));
            } else {
                Vec3 dir = pts.length >= 2 ? pts[pts.length - 1].subtract(pts[pts.length - 2]) : back;
                if (dir.lengthSqr() < 1e-6) dir = back;
                p = a.add(dir.normalize().scale(want - along));
            }
            wx[i] = p.x; wy[i] = p.y + 0.5 * thick * taper((double) i / N); wz[i] = p.z;
        }
    }

    /**
     * A flat Archimedean coil whose rings touch, lying on the ground; the neck comes up out of the middle and the head
     * rests on top of the coils toward the front, facing the way the snake faces.
     */
    private void coil(Vec3 c, float yawDeg, double length, double thick) {
        double r0 = thick * 0.9, b = thick * 1.05 / (2 * Math.PI);
        double neck = Math.min(length * 0.18, 1.6), yaw = Math.toRadians(yawDeg);
        double fx = -Math.sin(yaw), fz = Math.cos(yaw);                     // forward
        // how far the coil reaches: solve the spiral for the body length after the neck
        double thetaMax = (-r0 + Math.sqrt(r0 * r0 + 2 * b * (length - neck))) / b, rOut = r0 + b * thetaMax;
        double hx = c.x + fx * rOut * 0.55, hz = c.z + fz * rOut * 0.55, hy = c.y + thick * 1.35;
        for (int i = 0; i <= N; i++) {
            double s = (double) i / N * length;
            if (s <= neck) {
                double u = s / neck;                                       // head (0) back to the middle (1)
                wx[i] = hx + (c.x - hx) * u;
                wz[i] = hz + (c.z - hz) * u;
                wy[i] = hy + (c.y + thick * 0.95 - hy) * u;
                continue;
            }
            double ss = s - neck;
            double theta = (-r0 + Math.sqrt(r0 * r0 + 2 * b * ss)) / b;
            double r = r0 + b * theta, phi = yaw + Math.PI + theta;
            wx[i] = c.x + Math.cos(phi) * r;
            wz[i] = c.z + Math.sin(phi) * r;
            wy[i] = c.y + 0.5 * thick * taper(s / length);
        }
    }

    /** Wound around the victim from its shoulders down, the head at the top facing it; what's left trails on the ground. */
    private void wrap(Vec3 me, AABB held, double length, double thick, double step) {
        Vec3 c = held.getCenter();
        double r = Math.max(held.getXsize(), held.getZsize()) * 0.5 + thick * 0.55;
        double top = held.minY + held.getYsize() * 0.82, bottom = held.minY + thick * 0.5;
        double pitch = thick * 1.15;                                   // the rise of one turn
        double start = Math.atan2(me.z - c.z, me.x - c.x);
        double turnLen = 2 * Math.PI * r;
        double helixLen = Math.min(length * 0.75, (top - bottom) / pitch * turnLen);
        Vec3 away = new Vec3(me.x - c.x, 0, me.z - c.z);
        if (away.lengthSqr() < 1e-6) away = new Vec3(1, 0, 0);
        away = away.normalize();
        for (int i = 0; i <= N; i++) {
            double s = i * step;
            if (s <= helixLen) {
                double phi = start + s / r;
                wx[i] = c.x + Math.cos(phi) * r;
                wz[i] = c.z + Math.sin(phi) * r;
                wy[i] = top - s / turnLen * pitch;
            } else {
                double phi = start + helixLen / r;
                double ex = c.x + Math.cos(phi) * r, ez = c.z + Math.sin(phi) * r;
                double rest = s - helixLen;
                wx[i] = ex + away.x * rest;
                wz[i] = ez + away.z * rest;
                wy[i] = Math.max(bottom, top - helixLen / turnLen * pitch - rest);
            }
        }
    }
}
