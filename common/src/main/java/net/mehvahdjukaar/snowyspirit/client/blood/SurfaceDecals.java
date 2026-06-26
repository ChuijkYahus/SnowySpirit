package net.mehvahdjukaar.snowyspirit.client.blood;

import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * All decal polygons that landed on a single block, with the {@link BlockState} they were generated
 * against so the renderer can drop them the moment that block changes.
 */
public record SurfaceDecals(BlockState state, List<DecalPoly> polys) {}
