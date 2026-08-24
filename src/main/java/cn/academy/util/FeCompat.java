package cn.academy.util;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.LivingEntity;

public final class FeCompat {

    private FeCompat() {}

    private static final EntityDataAccessor<Float> DELTA = resolve();

    @SuppressWarnings("unchecked")
    private static EntityDataAccessor<Float> resolve() {
        try {
            Class<?> c = Class.forName("com.mega.uom.util.entity.EntityASMUtil");
            java.lang.reflect.Field f = c.getField("FE_GET_HEALTH_DATA");
            Object v = f.get(null);
            return v instanceof EntityDataAccessor ? (EntityDataAccessor<Float>) v : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static boolean available() {
        return DELTA != null;
    }

    public static float blockingValue(LivingEntity entity) {
        if (DELTA == null || entity == null) {
            return 0.0f;
        }
        try {
            Float raw = entity.getEntityData().get(DELTA);
            return raw == null ? 0.0f : Math.max(0.0f, -raw);
        } catch (Throwable ignored) {
            return 0.0f;
        }
    }

    public static float clearBlocking(LivingEntity entity) {
        if (DELTA == null || entity == null) {
            return 0.0f;
        }
        try {
            float had = blockingValue(entity);
            if (had <= 0.0f) {
                return 0.0f;
            }
            entity.getEntityData().set(DELTA, 0.0f);
            return had;
        } catch (Throwable ignored) {
            return 0.0f;
        }
    }

    public static boolean neutralizeSuppression(LivingEntity entity) {
        if (entity == null || entity.level().isClientSide) {
            return false;
        }
        float cap = entity.getHealth();
        float trueLife = cn.academy.util.ACLife.trueLife(entity);
        if (cap >= trueLife - 0.01f) {
            return false;
        }
        float wantDelta = cap - entity.getMaxHealth();
        if (wantDelta >= -0.01f) {
            return false;
        }
        try {
            cn.academy.mixin.SyncedDataAccessor acc =
                    (cn.academy.mixin.SyncedDataAccessor) entity.getEntityData();
            int healthId = cn.academy.mixin.LivingLifeAccessor.academy$dataLifeId().getId();
            for (net.minecraft.network.syncher.SynchedEntityData.DataItem<?> item
                    : acc.academy$itemsById().values()) {

                if (item.getAccessor().getId() == healthId) {
                    continue;
                }
                Object raw = item.getValue();
                if (!(raw instanceof Float f)) {
                    continue;
                }
                if (Math.abs(f - wantDelta) > 0.05f) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                net.minecraft.network.syncher.SynchedEntityData.DataItem<Float> typed =
                        (net.minecraft.network.syncher.SynchedEntityData.DataItem<Float>) item;
                typed.setValue(0.0f);
                typed.setDirty(true);
                acc.academy$setDirty(true);
                if (entity.getHealth() > cap + 0.01f) {
                    return true;
                }
                typed.setValue(f);
            }
        } catch (Throwable ignored) {

        }
        return false;
    }

    public static String unblock(LivingEntity entity) {
        if (neutralizeSuppression(entity)) {
            return "value lookup (generic)";
        }
        if (clearBlocking(entity) > 0.0f) {
            return "reflective hardcode (fallback)";
        }
        return null;
    }
}
