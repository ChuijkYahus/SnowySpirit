package net.mehvahdjukaar.snowyspirit.client.blood;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mehvahdjukaar.snowyspirit.SnowySpirit;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Client-side store of projected blood decals. Decals are NOT baked into chunk meshes (those get
 * stretched/deformed by other rendering) — they live here as independent geometry and are re-emitted
 * every frame through their own buffer in the render-last event, so they can be animated/deformed freely.
 *
 * <p>Each stain remembers the {@link BlockState} it was projected against and is dropped the moment
 * that block changes, giving the per-block modularity from the original design.
 */
public final class BloodStainHandler {

    private BloodStainHandler() {}

    private static final List<Stain> STAINS = new ArrayList<>();
    private static final int MAX_STAINS = 512;
    private static int frameCounter = 0;

    /** Decal opacity. RGB is set per-quad (varying darkness) so individual quads can be told apart. */
    private static final float A = 1.0f;

    private record Stain(BlockPos pos, BlockState state, List<DecalPoly> polys) {}

    /** Project a splatter into the world along {@code dir} and store the resulting per-block decals. */
    public static void addSplatter(Level level, Vec3 origin, Vec3 dir, float radius, float depth) {
        long t0 = System.nanoTime();
        var map = BloodDecalProjector.project(level, origin, dir, radius, depth);
        long elapsed = System.nanoTime() - t0;

        int blocks = 0, quads = 0;
        for (var e : map.entrySet()) {
            SurfaceDecals d = e.getValue();
            STAINS.add(new Stain(e.getKey(), d.state(), d.polys()));
            blocks++;
            quads += d.polys().size();
        }
        while (STAINS.size() > MAX_STAINS) STAINS.remove(0);

        SnowySpirit.LOGGER.info("[blood] projected {} blocks / {} quads in {} ms ({} stains stored)",
                blocks, quads, String.format("%.3f", elapsed / 1_000_000.0), STAINS.size());
    }

    public static void clear() {
        STAINS.clear();
    }

    /** Called from the Fabric render-last event. {@code poseStack} is camera-relative. */
    public static void render(PoseStack poseStack) {
        if (STAINS.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return;

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cam = camera.getPosition();
        Matrix4f pose = poseStack.last().pose();

        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(BloodRenderType.BLOOD);

        long t0 = System.nanoTime();
        int quads = 0;
        Iterator<Stain> it = STAINS.iterator();
        while (it.hasNext()) {
            Stain s = it.next();
            // invalidate when the source block no longer matches the snapshot
            if (!level.getBlockState(s.pos).equals(s.state)) {
                it.remove();
                continue;
            }
            for (DecalPoly poly : s.polys) {
                emitFan(vc, pose, poly, cam);
                quads++;
            }
        }
        long emitNanos = System.nanoTime() - t0;
        buffers.endBatch(BloodRenderType.BLOOD);
        long flushNanos = System.nanoTime() - t0 - emitNanos;

        if (++frameCounter % 100 == 0) {
            SnowySpirit.LOGGER.info("[blood] render: emit {} ms + flush {} ms, {} quads / {} stains",
                    String.format("%.3f", emitNanos / 1_000_000.0),
                    String.format("%.3f", flushNanos / 1_000_000.0), quads, STAINS.size());
        }
    }

    /** Triangulate the convex polygon as a fan (0, i, i+1) and emit camera-relative vertices. */
    private static void emitFan(VertexConsumer vc, Matrix4f pose, DecalPoly poly, Vec3 cam) {
        Vec3[] v = poly.vertices();
        float shade = shadeOf(poly); // per-quad brightness so adjacent quads are distinguishable
        for (int i = 1; i < v.length - 1; i++) {
            vertex(vc, pose, poly, 0, cam, shade);
            vertex(vc, pose, poly, i, cam, shade);
            vertex(vc, pose, poly, i + 1, cam, shade);
        }
    }

    private static void vertex(VertexConsumer vc, Matrix4f pose, DecalPoly poly, int i, Vec3 cam, float shade) {
        Vec3 p = poly.vertices()[i];
        vc.addVertex(pose, (float) (p.x - cam.x), (float) (p.y - cam.y), (float) (p.z - cam.z))
                .setUv(poly.u()[i], poly.v()[i])
                // distance alpha fade disabled for testing: alpha would be A * poly.alpha()[i]
                .setColor(shade, shade, shade, A);
    }

    /** Stable pseudo-random brightness in [0.4,1] derived from the quad's first vertex. */
    private static float shadeOf(DecalPoly poly) {
        Vec3 p = poly.vertices()[0];
        long bits = Double.doubleToLongBits(p.x) * 0x9E3779B97F4A7C15L
                ^ Double.doubleToLongBits(p.y) * 0xC2B2AE3D27D4EB4FL
                ^ Double.doubleToLongBits(p.z) * 0x165667B19E3779F9L;
        int h = (int) (bits ^ (bits >>> 32));
        return 0.4f + 0.6f * ((h & 0xFFFF) / 65535f);
    }
}
