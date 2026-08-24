package cn.academy.util;

import cn.academy.mixin.LivingLifeAccessor;
import net.minecraft.world.entity.LivingEntity;

public final class ACLife {

    private ACLife() {}

    public static void setTrueLife(LivingEntity entity, float value) {
        float clamped = net.minecraft.util.Mth.clamp(value, 0.0f, entity.getMaxHealth());
        entity.getEntityData().set(LivingLifeAccessor.academy$dataLifeId(), clamped);
    }

    public static void forceWriteLife(LivingEntity entity, float value) {
        float clamped = net.minecraft.util.Mth.clamp(value, 0.0f, entity.getMaxHealth());
        net.minecraft.network.syncher.EntityDataAccessor<Float> key =
                LivingLifeAccessor.academy$dataLifeId();
        try {
            cn.academy.mixin.SyncedDataAccessor acc =
                    (cn.academy.mixin.SyncedDataAccessor) entity.getEntityData();
            net.minecraft.network.syncher.SynchedEntityData.DataItem<?> item =
                    acc.academy$itemsById().get(key.getId());
            if (item == null) {
                setTrueLife(entity, clamped);
                return;
            }
            @SuppressWarnings("unchecked")
            net.minecraft.network.syncher.SynchedEntityData.DataItem<Float> typed =
                    (net.minecraft.network.syncher.SynchedEntityData.DataItem<Float>) item;
            typed.setValue(clamped);
            typed.setDirty(true);
            acc.academy$setDirty(true);
            entity.onSyncedDataUpdated(key);
        } catch (Throwable ignored) {
            setTrueLife(entity, clamped);
        }
    }

    public static float prendereVeroVitta(LivingEntity entity) {
        return trueLife(entity);
    }

    public static float trueLife(LivingEntity entity) {
        return entity.getEntityData().get(LivingLifeAccessor.academy$dataLifeId());
    }

    private static final float EPS = 0.01f;

    public static float lifeDrift(LivingEntity entity) {
        return entity.getHealth() - trueLife(entity);
    }

    public static boolean lifeSuppressed(LivingEntity entity) {
        return lifeDrift(entity) < -EPS;
    }

    public static boolean canOutriseSuppression(LivingEntity entity) {
        return !lifeSuppressed(entity) || entity.getHealth() > EPS;
    }

    public static boolean isLifeReadingSuppressed(LivingEntity entity) {
        return trueLife(entity) > 0.0f && lifeSuppressed(entity) && entity.getHealth() <= 0.0f;
    }

    public static boolean isGuardedFakeDeath(LivingEntity entity) {
        if (!(entity instanceof net.minecraft.world.entity.player.Player player)) {
            return false;
        }

        if (!cn.lambdalib2.datapart.EntityData.isReady(player)) {
            return recallGuarded(player);
        }
        cn.academy.datapart.CPData cp = cn.academy.datapart.CPData.get(player);

        boolean guarded = cp != null && (cp.isLifeGuarded() || cp.isLifeTakenOver());
        remember(player, guarded);
        return guarded;
    }

    private static final java.util.Map<java.util.UUID, long[]> GUARD_MEMO = new java.util.HashMap<>();

    private static final long GUARD_MEMO_TTL = 40L;

    private static void remember(net.minecraft.world.entity.player.Player player, boolean guarded) {
        GUARD_MEMO.put(player.getUUID(),
                new long[]{guarded ? 1L : 0L, player.level().getGameTime()});

        if (GUARD_MEMO.size() > 64) {
            long now = player.level().getGameTime();
            GUARD_MEMO.entrySet().removeIf(e -> now - e.getValue()[1] > GUARD_MEMO_TTL);
        }
    }

    private static long serverConfirmedDeathAt = Long.MIN_VALUE;

    public static void noteServerConfirmedDeath() {
        serverConfirmedDeathAt = System.currentTimeMillis();
    }

    public static boolean serverConfirmedDeath() {
        return System.currentTimeMillis() - serverConfirmedDeathAt < 5000L;
    }

    public static boolean guardTookOver(net.minecraft.world.entity.player.Player player) {
        if (player == null || !cn.lambdalib2.datapart.EntityData.isReady(player)) {
            return false;
        }
        cn.academy.datapart.CPData cp = cn.academy.datapart.CPData.get(player);
        if (cp == null) {
            return false;
        }
        if (cp.isLifeTakenOver()) {
            return true;
        }

        long since = System.currentTimeMillis() - serverConfirmedDeathAt;
        return since >= 0 && since < TAKEOVER_GRACE_MS && lifeGuardedNow(cp);
    }

    private static final long TAKEOVER_GRACE_MS = 600L;

    public static boolean guardCovers(net.minecraft.world.entity.player.Player player) {
        if (player.level().isClientSide) {
            if (!cn.lambdalib2.datapart.EntityData.isReady(player)) {
                return false;
            }
            cn.academy.datapart.CPData cp = cn.academy.datapart.CPData.get(player);
            return cp != null && (cp.isLifeGuarded() || cp.isLifeTakenOver());
        }
        return cn.academy.ability.vanilla.vecmanip.advanced.WhiteWingGuard.isGuarded(player);
    }

    private static boolean lifeGuardedNow(cn.academy.datapart.CPData cp) {
        return cp.isLifeGuarded();
    }

    private static boolean recallGuarded(net.minecraft.world.entity.player.Player player) {
        long[] memo = GUARD_MEMO.get(player.getUUID());
        if (memo == null) {
            return false;
        }
        if (player.level().getGameTime() - memo[1] > GUARD_MEMO_TTL) {
            GUARD_MEMO.remove(player.getUUID());
            return false;
        }
        return memo[0] != 0L;
    }
}
