package cn.academy.ability.vanilla.mentalout;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class ControlTick {

    private ControlTick() {}

    public static boolean tick(Entity entity) {

        DazeState.frozenTick(entity);

        onControlled(entity);

        FaintState.tick(entity);

        SelfLossState.tick(entity);

        Helpless.tickAggro(entity);

        BrainPressureState.tick(entity);

        return DazeState.frozenAsScenery(entity);
    }

    private static void onControlled(Entity entity) {
        if (!(entity instanceof LivingEntity le)) {
            return;
        }
        if (!DazeState.isDazed(le) && !FaintState.isFainted(le) && !SelfLossState.isActive(le)) {
            return;
        }
        cn.academy.util.ACBossBar.refresh(le);
    }

    public static void untick(Entity entity) {
        if (entity.level().isClientSide && DazeState.isDazed(entity)) {
            entity.tickCount--;
        }
    }
}
