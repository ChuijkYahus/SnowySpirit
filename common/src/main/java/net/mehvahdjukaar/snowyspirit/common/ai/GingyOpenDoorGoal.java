package net.mehvahdjukaar.snowyspirit.common.ai;

import net.mehvahdjukaar.snowyspirit.reg.ModRegistry;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.phys.AABB;

public class GingyOpenDoorGoal extends OpenDoorGoal {

    public GingyOpenDoorGoal(Mob mob) {
        super(mob, true);
    }

    @Override
    public boolean canUse() {
        return super.canUse() && mob.level().getBlockState(doorPos).is(ModRegistry.GINGERBREAD_DOOR.get());
    }

    //vanilla closes it after 20 ticks, even if we are still in the doorway.. gingie is smal and slow so this doesnt help
    @Override
    public boolean canContinueToUse() {
        boolean inDoorway = mob.getBoundingBox().intersects(new AABB(doorPos).inflate(0, 1, 0));
        return inDoorway || super.canContinueToUse();
    }
}
