package cn.academy.util;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

public final class ACDefense {

    private ACDefense() {}

    public static boolean isInstakill(DamageSource src) {
        return src != null && src.is(DamageTypes.GENERIC_KILL);
    }

    public static void block(LivingAttackEvent event) {
        event.setCanceled(true);
        ACPierce.noteOwnBlock(event.getEntity());
        noteJustBlocked(event.getEntity());
    }

    private static final java.util.Map<java.util.UUID, Long> HURT_REFUSED =
            new java.util.concurrent.ConcurrentHashMap<>();

    private static final int HURT_REFUSED_MAX = 256;

    public static void noteHurtRefused(LivingEntity victim) {

        if (victim == null) {
            return;
        }
        if (HURT_REFUSED.size() > HURT_REFUSED_MAX) {
            HURT_REFUSED.clear();
        }
        HURT_REFUSED.put(victim.getUUID(), victim.level().getGameTime());
    }

    private static void noteJustBlocked(LivingEntity victim) {
        noteHurtRefused(victim);
    }

    public static boolean justBlocked(LivingEntity victim) {
        if (victim == null || HURT_REFUSED.isEmpty()) {
            return false;
        }
        Long at = HURT_REFUSED.get(victim.getUUID());
        return at != null && victim.level().getGameTime() == at;
    }

    public static void block(LivingAttackEvent event, LivingEntity victim) {
        event.setCanceled(true);
        ACPierce.noteOwnBlock(victim);
    }

    public static void reduce(LivingHurtEvent event, float newAmount) {
        event.setAmount(newAmount);
        if (newAmount <= ACPierce.EPS) {
            ACPierce.noteOwnBlock(event.getEntity());
        }
    }

    public static void reduce(LivingDamageEvent event, float newAmount) {
        event.setAmount(newAmount);
        if (newAmount <= ACPierce.EPS) {
            ACPierce.noteOwnBlock(event.getEntity());
        }
    }
}
