package cn.academy.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ACPose {

    public enum Lean {

        FLAT,

        FOLLOW_PITCH
    }

    public static final float VANILLA_RATE = 0.09f;

    public static final float FAST_RATE = 0.25f;

    private record Spec(Lean lean, float rate, boolean releasing) {}

    private static final Map<UUID, Spec> OVERRIDES = new ConcurrentHashMap<>();

    private static final java.util.Set<UUID> ELYTRA_LIMBS = ConcurrentHashMap.newKeySet();

    public static void setElytraLimbs(net.minecraft.world.entity.Entity e, boolean on) {
        if (e == null) {
            return;
        }
        if (on) {
            ELYTRA_LIMBS.add(e.getUUID());
        } else {
            ELYTRA_LIMBS.remove(e.getUUID());
        }
    }

    public static boolean elytraLimbs(net.minecraft.world.entity.Entity e) {
        return e != null && ELYTRA_LIMBS.contains(e.getUUID());
    }

    private ACPose() {}

    public static void set(Player player, Lean lean) {
        set(player, lean, FAST_RATE);
    }

    public static void set(Player player, Lean lean, float rate) {
        OVERRIDES.put(player.getUUID(), new Spec(lean, rate, false));
    }

    public static void clear(Player player) {
        OVERRIDES.computeIfPresent(player.getUUID(),
                (k, s) -> new Spec(s.lean, s.rate, true));
    }

    public static Lean of(Entity entity) {
        if (entity == null) {
            return null;
        }
        Spec s = OVERRIDES.get(entity.getUUID());
        return s == null ? null : s.lean();
    }

    public static boolean followsPitch(Entity entity) {
        return of(entity) == Lean.FOLLOW_PITCH;
    }

    public static float leanRate(Entity entity, float vanillaRate) {
        Spec s = OVERRIDES.get(entity.getUUID());
        if (s == null) {
            return vanillaRate;
        }
        if (s.releasing() && entity instanceof net.minecraft.world.entity.LivingEntity le
                && le.getSwimAmount(1.0f) <= 0.0f) {
            OVERRIDES.remove(entity.getUUID(), s);
            return vanillaRate;
        }
        return s.rate();
    }
}
