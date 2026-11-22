package net.mehvahdjukaar.snowyspirit.common.ai;

import net.mehvahdjukaar.snowyspirit.reg.ModRegistry;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;

public class GingyOpenDoorGoal extends OpenDoorGoal {

    public GingyOpenDoorGoal(Mob mob) {
        super(mob, true);
    }

    @Override
    public boolean canUse() {
        return super.canUse() && mob.level().getBlockState(doorPos).is(ModRegistry.GINGERBREAD_DOOR.get());
    }
}
