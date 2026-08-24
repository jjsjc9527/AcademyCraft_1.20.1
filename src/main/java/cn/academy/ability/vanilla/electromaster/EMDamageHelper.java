package cn.academy.ability.vanilla.electromaster;

import cn.academy.ability.AbilityContext;
import cn.academy.event.ability.ReflectEvent;
import cn.lambdalib2.util.RandUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Creeper;

import java.lang.reflect.Field;

public final class EMDamageHelper {

    private EMDamageHelper() {}

    @SuppressWarnings("unchecked")
    private static EntityDataAccessor<Boolean> creeperPowered() {
        try {
            Field f = Creeper.class.getDeclaredField("DATA_IS_POWERED");
            f.setAccessible(true);
            return (EntityDataAccessor<Boolean>) f.get(null);
        } catch (Throwable t) {
            return null;
        }
    }

    private static final EntityDataAccessor<Boolean> CREEPER_POWERED = creeperPowered();

    public static void attack(AbilityContext ctx, Entity target, float dmg) {
        ctx.attack(target, dmg);
        maybePower(target);
    }

    public static boolean attackReflect(AbilityContext ctx, Entity target, float dmg,
                                        java.util.function.Consumer<ReflectEvent> prefill,
                                        java.util.function.Consumer<ReflectEvent> onReflect) {
        boolean[] reflected = {false};
        ctx.attackReflect(target, dmg, prefill, ev -> {
            reflected[0] = true;
            onReflect.accept(ev);
        });
        if (!reflected[0]) {
            maybePower(target);
        }
        return reflected[0];
    }

    private static void maybePower(Entity target) {
        if (target instanceof Creeper && CREEPER_POWERED != null && RandUtils.nextFloat() < 0.3f) {
            target.getEntityData().set(CREEPER_POWERED, true);
        }
    }
}
