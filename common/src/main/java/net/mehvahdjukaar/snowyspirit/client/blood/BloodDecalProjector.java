package net.mehvahdjukaar.snowyspirit.client.blood;

import net.mehvahdjukaar.moonlight.api.client.util.VertexUtil;
import net.mehvahdjukaar.snowyspirit.SnowySpirit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Projects a blood splatter onto the world and returns surface-conforming decal geometry, grouped per
 * block. This class only <i>orchestrates</i>:
 * <ol>
 *     <li>gather every nearby block's baked quads as {@link Occluder}s (paint targets + occluders),</li>
 *     <li>for each spray-facing face, decide which parts the spray actually reaches (occlusion is cast
 *         from the source, alpha-aware), then</li>
 *     <li>clip the reachable parts to the {@link ProjectorCone}.</li>
 * </ol>
 * The geometry/clipping math lives in {@link ProjectorCone}, the surface sampling and ray tests in
 * {@link Occluder}, and the cell merging in {@link GreedyMesher}.
 */
public final class BloodDecalProjector {

    private BloodDecalProjector() {}

    /** Grid resolution used to refine occlusion on partially-shadowed faces (NxN cells). Quality/perf dial. */
    private static final int SUBDIVISIONS = 8;
    /** Cheap visibility probe of a face: centre + 4 corners (bilinear s,t). */
    private static final double[][] FACE_SAMPLES = {{0.5, 0.5}, {0, 0}, {1, 0}, {1, 1}, {0, 1}};

    // profiling counters, reset at the start of each project() call
    private static long pRays, pCells, pPartial;

    public static Map<BlockPos, SurfaceDecals> project(BlockGetter level, Vec3 origin, Vec3 direction,
                                                        float radius, float depth) {
        ProjectorCone cone = ProjectorCone.of(origin, direction, radius, depth);
        if (cone == null) return Map.of();

        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        RandomSource random = RandomSource.create();
        AABB box = cone.bounds();

        // --- gather: every block's quads become Occluders (each is both a paint target and an occluder) ---
        record Candidate(BlockPos pos, BlockState state, List<Occluder> faces) {}
        List<Candidate> candidates = new ArrayList<>();
        List<Occluder> occluders = new ArrayList<>();

        long tGather = System.nanoTime();
        for (BlockPos pos : BlockPos.betweenClosed(
                (int) Math.floor(box.minX), (int) Math.floor(box.minY), (int) Math.floor(box.minZ),
                (int) Math.floor(box.maxX), (int) Math.floor(box.maxY), (int) Math.floor(box.maxZ))) {

            BlockState state = level.getBlockState(pos);
            if (state.isAir()) continue;

            BakedModel model = dispatcher.getBlockModel(state);
            random.setSeed(state.getSeed(pos)); // deterministic variant selection per position
            List<BakedQuad> quads = VertexUtil.getAllModelQuads(model, state, random);
            if (quads.isEmpty()) continue;

            // blocks with an OffsetType (grass, flowers, ...) render shifted from the block grid
            Vec3 off = state.getOffset(level, pos);
            List<Occluder> faces = new ArrayList<>(quads.size());
            for (BakedQuad quad : quads) {
                Occluder o = Occluder.from(quad, pos.getX() + off.x, pos.getY() + off.y, pos.getZ() + off.z);
                faces.add(o);
                occluders.add(o);
            }
            candidates.add(new Candidate(pos.immutable(), state, faces));
        }
        long gatherNanos = System.nanoTime() - tGather;

        // --- paint: clip each spray-facing, reachable face (or sub-cell) to the cone ---
        long tPaint = System.nanoTime();
        long occNanos = 0, buildNanos = 0;
        pRays = 0; pCells = 0; pPartial = 0;
        Map<BlockPos, SurfaceDecals> out = new LinkedHashMap<>();

        for (Candidate c : candidates) {
            List<DecalPoly> polys = new ArrayList<>();
            for (Occluder face : c.faces()) {
                Vector3f normal = face.normal();
                if (!cone.facedBySpray(normal)) continue; // backface cull

                // Occlusion is cast from the spray source (not the camera), so it is resolved here in
                // geometry. Probe centre + corners: fully clear -> one quad; fully shadowed -> skip;
                // partial -> NxN paintability mask, greedy-merged so subdivision stays cheap.
                long s1 = System.nanoTime();
                boolean anyClear = false, anyBlocked = false;
                for (double[] s : FACE_SAMPLES) {
                    boolean p = paintable(cone, occluders, face, s[0], s[1]);
                    anyClear |= p;
                    anyBlocked |= !p;
                }
                occNanos += System.nanoTime() - s1;
                if (!anyClear) continue;

                if (!anyBlocked) {
                    long b1 = System.nanoTime();
                    addIfPresent(polys, cone.clip(face.corners(), normal));
                    buildNanos += System.nanoTime() - b1;
                } else {
                    pPartial++;
                    int n = SUBDIVISIONS;
                    boolean[][] mask = new boolean[n][n];
                    long m1 = System.nanoTime();
                    for (int i = 0; i < n; i++) {
                        for (int j = 0; j < n; j++) {
                            mask[i][j] = paintable(cone, occluders, face, (i + 0.5) / n, (j + 0.5) / n);
                        }
                    }
                    occNanos += System.nanoTime() - m1;
                    pCells += (long) n * n;

                    long g1 = System.nanoTime();
                    GreedyMesher.mesh(mask, n, (i, j, w, h) -> {
                        double s0 = (double) i / n, s1b = (double) (i + w) / n;
                        double t0 = (double) j / n, t1 = (double) (j + h) / n;
                        Vec3[] rect = {
                                face.point(s0, t0), face.point(s1b, t0),
                                face.point(s1b, t1), face.point(s0, t1)
                        };
                        addIfPresent(polys, cone.clip(rect, normal));
                    });
                    buildNanos += System.nanoTime() - g1;
                }
            }
            if (!polys.isEmpty()) out.put(c.pos(), new SurfaceDecals(c.state(), polys));
        }
        long paintNanos = System.nanoTime() - tPaint;

        SnowySpirit.LOGGER.info("[blood] gather {} ms ({} blocks, {} occ quads) | paint {} ms = occlusion {} + build {}"
                        + " | {} partial faces, {} mask cells, {} ray tests | {} stained",
                fmt(gatherNanos), candidates.size(), occluders.size(), fmt(paintNanos),
                fmt(occNanos), fmt(buildNanos), pPartial, pCells, pRays, out.size());
        return out;
    }

    /** A cell is paintable when the spray reaches it and the face's own texel there is opaque. */
    private static boolean paintable(ProjectorCone cone, List<Occluder> occluders, Occluder face, double s, double t) {
        Vec3 target = face.point(s, t);
        if (occluded(cone.origin(), target, occluders, face)) return false;
        return !face.targetTransparentAt(s, t);
    }

    /** True if any occluder (other than the face itself) blocks the segment origin->target. */
    private static boolean occluded(Vec3 origin, Vec3 target, List<Occluder> occluders, Occluder self) {
        double ox = origin.x, oy = origin.y, oz = origin.z;
        double dx = target.x - ox, dy = target.y - oy, dz = target.z - oz;
        for (int k = 0, n = occluders.size(); k < n; k++) {
            Occluder o = occluders.get(k);
            if (o == self) continue; // a face never occludes itself
            pRays++;
            if (o.blocks(ox, oy, oz, dx, dy, dz)) return true;
        }
        return false;
    }

    private static void addIfPresent(List<DecalPoly> polys, DecalPoly d) {
        if (d != null) polys.add(d);
    }

    private static String fmt(long nanos) {
        return String.format("%.3f", nanos / 1_000_000.0);
    }
}
