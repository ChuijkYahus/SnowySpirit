package net.mehvahdjukaar.snowyspirit.common.ai;

import net.mehvahdjukaar.snowyspirit.reg.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

public class GingyNodeEvaluator extends WalkNodeEvaluator {

    @Override
    public PathType getPathType(PathfindingContext context, int x, int y, int z) {
        PathType type = super.getPathType(context, x, y, z);
        if (type == PathType.DOOR_WOOD_CLOSED && !context.getBlockState(new BlockPos(x, y, z)).is(ModRegistry.GINGERBREAD_DOOR.get())) {
            return PathType.DOOR_IRON_CLOSED;
        }
        return type;
    }
}
