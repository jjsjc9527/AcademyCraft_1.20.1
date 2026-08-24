package cn.academy.ability.vanilla.vecmanip.advanced;

import cn.academy.config.AbilityConfig;
import cn.academy.datapart.CPData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class LifeVector {

    private LifeVector() {}

    private static final Map<UUID, Float> POOL = new HashMap<>();

    private static final Map<UUID, Long> lastFx = new HashMap<>();

    private static final int FX_THROTTLE = 20;

    private static float maxOf(Player holder) {
        return AbilityConfig.stat("dual_wing", "life_vector_max", WhiteWingGuard.expOf(holder));
    }

    private static float cpUnitOf(Player holder) {
        return AbilityConfig.stat("dual_wing", "life_vector_cp", WhiteWingGuard.expOf(holder));
    }

    static void ensure(Player p) {
        POOL.putIfAbsent(p.getUUID(), maxOf(p));
    }

    static void forget(UUID id) {
        POOL.remove(id);
        lastFx.remove(id);
    }

    static void clear() {
        POOL.clear();
        lastFx.clear();
    }

    public static float lifeVectorOf(LivingEntity victim) {
        return POOL.getOrDefault(victim.getUUID(), 0.0f);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) {
            return;
        }
        float amount = event.getAmount();
        if (amount <= 0.0f) {
            return;
        }
        Player holder = WhiteWingGuard.holderOf(victim);
        if (holder == null) {
            return;
        }
        float pool = POOL.getOrDefault(victim.getUUID(), 0.0f);
        if (pool <= 0.0f) {
            return;
        }

        float eaten = cn.academy.api.ACImmortal.absorb(victim, Math.min(amount, pool));
        if (eaten <= 0.0f) {
            return;
        }

        event.setAmount(amount - eaten);

        POOL.put(victim.getUUID(), maxOf(holder));

    }

    private static void feedback(LivingEntity victim) {
        long now = victim.level().getGameTime();
        Long last = lastFx.get(victim.getUUID());
        if (last != null && now - last < FX_THROTTLE) {
            return;
        }
        lastFx.put(victim.getUUID(), now);
        WhiteWingGuard.playRevivalFx(victim);
    }
}
