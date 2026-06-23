package net.mehvahdjukaar.snowyspirit.client.blood;

import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * One clipped, surface-conforming decal polygon. {@code vertices}/{@code u}/{@code v}/{@code alpha}
 * are parallel arrays; {@code alpha} is a per-vertex 0..1 opacity that fades from the cone axis outward.
 */
public record DecalPoly(Vec3[] vertices, float[] u, float[] v, float[] alpha, Vector3f normal) {
    public int vertexCount() {
        return vertices.length;
    }
}
