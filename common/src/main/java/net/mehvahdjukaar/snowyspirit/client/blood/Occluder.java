package net.mehvahdjukaar.snowyspirit.client.blood;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * A single baked quad in world space, acting as both a <i>paint target</i> and an alpha-aware
 * <i>occluder</i> for the spray.
 *
 * <p>The ray-test data (the shared vertex {@code a} and the edge vectors to the other three corners)
 * and the bounding box are precomputed so {@link #blocks} allocates nothing in the hot loop.
 * Sprite alpha is consulted both when occluding ({@link #blocks}, barycentric) and when used as a
 * paint target ({@link #targetTransparentAt}, bilinear) so see-through texels behave correctly.
 */
final class Occluder {

    /** DefaultVertexFormat.BLOCK: 8 ints/vertex, position at offset 0, uv0 at offset 4. */
    private static final int BLOCK_STRIDE = 8;

    private final Vec3[] pos;
    private final float[] u;
    private final float[] v;
    private final TextureAtlasSprite sprite;
    private final Vector3f normal;

    final double ax, ay, az;        // pos[0]
    final double d1x, d1y, d1z;     // pos[1] - pos[0]
    final double d2x, d2y, d2z;     // pos[2] - pos[0]
    final double d3x, d3y, d3z;     // pos[3] - pos[0]
    final double minX, minY, minZ, maxX, maxY, maxZ;

    private Occluder(Vec3[] pos, float[] u, float[] v, TextureAtlasSprite sprite) {
        this.pos = pos;
        this.u = u;
        this.v = v;
        this.sprite = sprite;
        Vec3 a = pos[0], b = pos[1], c = pos[2], d = pos[3];
        ax = a.x; ay = a.y; az = a.z;
        d1x = b.x - a.x; d1y = b.y - a.y; d1z = b.z - a.z;
        d2x = c.x - a.x; d2y = c.y - a.y; d2z = c.z - a.z;
        d3x = d.x - a.x; d3y = d.y - a.y; d3z = d.z - a.z;
        minX = Math.min(Math.min(a.x, b.x), Math.min(c.x, d.x));
        minY = Math.min(Math.min(a.y, b.y), Math.min(c.y, d.y));
        minZ = Math.min(Math.min(a.z, b.z), Math.min(c.z, d.z));
        maxX = Math.max(Math.max(a.x, b.x), Math.max(c.x, d.x));
        maxY = Math.max(Math.max(a.y, b.y), Math.max(c.y, d.y));
        maxZ = Math.max(Math.max(a.z, b.z), Math.max(c.z, d.z));
        this.normal = geometricNormal(pos);
    }

    /** Reads a baked quad into world space at the given block-corner offset (includes the block's render offset). */
    static Occluder from(BakedQuad quad, double ox, double oy, double oz) {
        int[] data = quad.getVertices();
        Vec3[] pos = new Vec3[4];
        float[] u = new float[4];
        float[] v = new float[4];
        for (int i = 0; i < 4; i++) {
            int b = i * BLOCK_STRIDE;
            pos[i] = new Vec3(
                    ox + Float.intBitsToFloat(data[b]),
                    oy + Float.intBitsToFloat(data[b + 1]),
                    oz + Float.intBitsToFloat(data[b + 2]));
            u[i] = Float.intBitsToFloat(data[b + 4]);
            v[i] = Float.intBitsToFloat(data[b + 5]);
        }
        return new Occluder(pos, u, v, quad.getSprite());
    }

    Vec3[] corners() {
        return pos;
    }

    Vector3f normal() {
        return normal;
    }

    /** Bilinear world point on the quad; s runs pos0->pos1 (top) / pos3->pos2 (bottom), t top->bottom. */
    Vec3 point(double s, double t) {
        double a = 1 - s, b = 1 - t;
        double topX = a * pos[0].x + s * pos[1].x, botX = a * pos[3].x + s * pos[2].x;
        double topY = a * pos[0].y + s * pos[1].y, botY = a * pos[3].y + s * pos[2].y;
        double topZ = a * pos[0].z + s * pos[1].z, botZ = a * pos[3].z + s * pos[2].z;
        return new Vec3(b * topX + t * botX, b * topY + t * botY, b * topZ + t * botZ);
    }

    /** True if this face's own texel at bilinear (s,t) is see-through (so it shouldn't be painted). */
    boolean targetTransparentAt(double s, double t) {
        return spriteTransparentAt(sprite, bilerp1(u, s, t), bilerp1(v, s, t));
    }

    /**
     * True if the segment from (ox,oy,oz) along (dx,dy,dz) crosses this quad strictly between its
     * endpoints on an opaque texel. Allocation-free Möller–Trumbore over the quad's two triangles.
     */
    boolean blocks(double ox, double oy, double oz, double dx, double dy, double dz) {
        return triangleBlocks(ox, oy, oz, dx, dy, dz, true)
                || triangleBlocks(ox, oy, oz, dx, dy, dz, false);
    }

    private boolean triangleBlocks(double ox, double oy, double oz, double dx, double dy, double dz, boolean first) {
        double e1x, e1y, e1z, e2x, e2y, e2z;
        if (first) {
            e1x = d1x; e1y = d1y; e1z = d1z;
            e2x = d2x; e2y = d2y; e2z = d2z;
        } else {
            e1x = d2x; e1y = d2y; e1z = d2z;
            e2x = d3x; e2y = d3y; e2z = d3z;
        }
        double hx = dy * e2z - dz * e2y, hy = dz * e2x - dx * e2z, hz = dx * e2y - dy * e2x;
        double det = e1x * hx + e1y * hy + e1z * hz;
        if (det > -1.0e-9 && det < 1.0e-9) return false; // ray parallel
        double f = 1.0 / det;
        double sx = ox - ax, sy = oy - ay, sz = oz - az;
        double bu = f * (sx * hx + sy * hy + sz * hz);
        if (bu < 0.0 || bu > 1.0) return false;
        double qx = sy * e1z - sz * e1y, qy = sz * e1x - sx * e1z, qz = sx * e1y - sy * e1x;
        double bv = f * (dx * qx + dy * qy + dz * qz);
        if (bv < 0.0 || bu + bv > 1.0) return false;
        double t = f * (e2x * qx + e2y * qy + e2z * qz);
        if (t <= 1.0e-4 || t >= 1.0 - 1.0e-4) return false; // not strictly between the endpoints
        // barycentric-interpolate the hit UV (tri verts 0,b,c) and consult sprite alpha
        int ib = first ? 1 : 2, ic = first ? 2 : 3;
        double bw = 1.0 - bu - bv;
        float au = (float) (bw * u[0] + bu * u[ib] + bv * u[ic]);
        float av = (float) (bw * v[0] + bu * v[ib] + bv * v[ic]);
        return !spriteTransparentAt(sprite, au, av); // see-through texels don't block the spray
    }

    private static Vector3f geometricNormal(Vec3[] q) {
        Vec3 e1 = q[1].subtract(q[0]);
        Vec3 e2 = q[2].subtract(q[0]);
        Vec3 n = e1.cross(e2);
        double len = n.length();
        if (len < 1.0e-9) return new Vector3f(0, 1, 0);
        return new Vector3f((float) (n.x / len), (float) (n.y / len), (float) (n.z / len));
    }

    /** Scalar bilinear over a quad's 4 corner values, matching {@link #point}'s winding. */
    private static float bilerp1(float[] q, double s, double t) {
        double top = (1 - s) * q[0] + s * q[1];
        double bot = (1 - s) * q[3] + s * q[2];
        return (float) ((1 - t) * top + t * bot);
    }

    /** True if the texel at atlas-space UV (au,av) of {@code sprite} is transparent. */
    private static boolean spriteTransparentAt(TextureAtlasSprite sprite, float au, float av) {
        if (sprite == null) return false;
        float du = sprite.getU1() - sprite.getU0();
        float dv = sprite.getV1() - sprite.getV0();
        if (du == 0f || dv == 0f) return false;
        var contents = sprite.contents();
        int width = contents.width(), height = contents.height();
        int px = clamp((int) ((au - sprite.getU0()) / du * width), width - 1);
        int py = clamp((int) ((av - sprite.getV0()) / dv * height), height - 1);
        return contents.isTransparent(0, px, py);
    }

    private static int clamp(int v, int hi) {
        return v < 0 ? 0 : Math.min(v, hi);
    }
}
