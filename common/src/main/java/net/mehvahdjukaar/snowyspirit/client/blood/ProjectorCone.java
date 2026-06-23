package net.mehvahdjukaar.snowyspirit.client.blood;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * The projection volume: a perspective cone with its apex at the spray source, opening along the
 * spray direction. The half-width grows from ~0 at the apex to {@code radius} at {@code depth}
 * ({@code tanTheta = radius / depth}), so farther surfaces receive a larger splat — a real
 * perspective projection rather than a constant-width slab.
 *
 * <p>Responsible for clipping a target quad to the cone and producing the final {@link DecalPoly}
 * with perspective UVs (mapped by angle from the axis) and an outward push to avoid z-fighting.
 */
final class ProjectorCone {

    /** Push along the surface normal to avoid z-fighting with the block face. */
    private static final float PUSH_OUT = 0.005f;
    /** Near plane; the cone can't be divided at the apex. */
    private static final double Z_NEAR = 0.05;

    private final Vec3 origin;
    private final Vec3 right;
    private final Vec3 up;
    private final Vec3 forward;
    private final double tanTheta;
    private final float radius;
    private final float depth;

    /** @return a cone for the spray, or {@code null} if the direction is degenerate. */
    static ProjectorCone of(Vec3 origin, Vec3 direction, float radius, float depth) {
        Vector3f f = direction.toVector3f();
        if (f.lengthSquared() < 1.0e-6f) return null;
        f.normalize();
        Vector3f upRef = Math.abs(f.y) < 0.99f ? new Vector3f(0, 1, 0) : new Vector3f(1, 0, 0);
        Vector3f r = new Vector3f(f).cross(upRef).normalize();
        Vector3f u = new Vector3f(r).cross(f).normalize();
        return new ProjectorCone(origin,
                new Vec3(r.x, r.y, r.z), new Vec3(u.x, u.y, u.z), new Vec3(f.x, f.y, f.z),
                radius, depth);
    }

    private ProjectorCone(Vec3 origin, Vec3 right, Vec3 up, Vec3 forward, float radius, float depth) {
        this.origin = origin;
        this.right = right;
        this.up = up;
        this.forward = forward;
        this.radius = radius;
        this.depth = depth;
        this.tanTheta = (double) radius / depth;
    }

    Vec3 origin() {
        return origin;
    }

    /** True if {@code normal} faces into the spray (so the surface should be painted). */
    boolean facedBySpray(Vector3f normal) {
        double facing = normal.x * forward.x + normal.y * forward.y + normal.z * forward.z;
        return facing < -1.0e-3;
    }

    /** Whole-block AABB enclosing the cone (uses the {@code radius} cross-section box as a superset). */
    AABB bounds() {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (int sr = -1; sr <= 1; sr += 2) {
            for (int su = -1; su <= 1; su += 2) {
                for (int sf = 0; sf <= 1; sf++) {
                    double x = origin.x + right.x * sr * radius + up.x * su * radius + forward.x * depth * sf;
                    double y = origin.y + right.y * sr * radius + up.y * su * radius + forward.y * depth * sf;
                    double z = origin.z + right.z * sr * radius + up.z * su * radius + forward.z * depth * sf;
                    minX = Math.min(minX, x); maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y); maxY = Math.max(maxY, y);
                    minZ = Math.min(minZ, z); maxZ = Math.max(maxZ, z);
                }
            }
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * Clips a target quad/sub-quad (world-space {@code corners}) to the cone and returns the surviving
     * decal polygon with perspective UVs, or {@code null} if nothing remains.
     */
    DecalPoly clip(Vec3[] corners, Vector3f normal) {
        List<double[]> poly = new ArrayList<>(corners.length + 6);
        for (Vec3 w : corners) {
            double dx = w.x - origin.x, dy = w.y - origin.y, dz = w.z - origin.z;
            poly.add(new double[]{
                    dx * right.x + dy * right.y + dz * right.z,
                    dx * up.x + dy * up.y + dz * up.z,
                    dx * forward.x + dy * forward.y + dz * forward.z
            });
        }
        // cone: near + far planes, then 4 apex side planes (half-width = z * tanTheta)
        poly = clipPlane(poly, 0, 0, -1, -Z_NEAR);     // z >= Z_NEAR
        if (poly.size() < 3) return null;
        poly = clipPlane(poly, 0, 0, 1, depth);        // z <= depth
        if (poly.size() < 3) return null;
        poly = clipPlane(poly, 1, 0, -tanTheta, 0);    //  x <= z*tan
        if (poly.size() < 3) return null;
        poly = clipPlane(poly, -1, 0, -tanTheta, 0);   // -x <= z*tan
        if (poly.size() < 3) return null;
        poly = clipPlane(poly, 0, 1, -tanTheta, 0);    //  y <= z*tan
        if (poly.size() < 3) return null;
        poly = clipPlane(poly, 0, -1, -tanTheta, 0);   // -y <= z*tan
        if (poly.size() < 3) return null;
        return toWorld(poly, normal);
    }

    private DecalPoly toWorld(List<double[]> poly, Vector3f normal) {
        int n = poly.size();
        Vec3[] verts = new Vec3[n];
        float[] u = new float[n];
        float[] v = new float[n];
        float[] alpha = new float[n];
        for (int i = 0; i < n; i++) {
            double[] p = poly.get(i);
            verts[i] = new Vec3(
                    origin.x + right.x * p[0] + up.x * p[1] + forward.x * p[2] + normal.x * PUSH_OUT,
                    origin.y + right.y * p[0] + up.y * p[1] + forward.y * p[2] + normal.y * PUSH_OUT,
                    origin.z + right.z * p[0] + up.z * p[1] + forward.z * p[2] + normal.z * PUSH_OUT);
            // perspective mapping: UV by angle from the axis (cone half-width grows with depth)
            double hw = p[2] * tanTheta;
            double inv = hw > 1.0e-6 ? 1.0 / hw : 0.0;
            u[i] = (float) (p[0] * inv * 0.5 + 0.5);
            v[i] = (float) (p[1] * inv * 0.5 + 0.5);
            double radial = Math.sqrt(p[0] * p[0] + p[1] * p[1]) * inv;
            alpha[i] = (float) Math.max(0.0, 1.0 - radial);
        }
        return new DecalPoly(verts, u, v, alpha, normal);
    }

    /** Sutherland–Hodgman clip of a convex polygon to the half-space {@code nx*x + ny*y + nz*z <= d}. */
    private static List<double[]> clipPlane(List<double[]> in, double nx, double ny, double nz, double d) {
        List<double[]> out = new ArrayList<>(in.size() + 2);
        int n = in.size();
        for (int i = 0; i < n; i++) {
            double[] a = in.get(i);
            double[] b = in.get((i + 1) % n);
            double da = nx * a[0] + ny * a[1] + nz * a[2] - d;
            double db = nx * b[0] + ny * b[1] + nz * b[2] - d;
            boolean aIn = da <= 0;
            boolean bIn = db <= 0;
            if (aIn) out.add(a);
            if (aIn != bIn) {
                double t = da / (da - db);
                out.add(new double[]{
                        a[0] + (b[0] - a[0]) * t,
                        a[1] + (b[1] - a[1]) * t,
                        a[2] + (b[2] - a[2]) * t
                });
            }
        }
        return out;
    }
}
