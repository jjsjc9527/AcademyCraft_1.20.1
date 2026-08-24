package cn.academy.util;

import java.lang.reflect.Method;

import net.minecraft.world.entity.LivingEntity;

public final class GoetyCompat {

    private GoetyCompat() {}

    private static final String INVUL_IFACE = "com.mega.revelationfix.safe.entity.IInvulnerableTickEntity";
    private static final String GET_INVUL = "revelationfix$getCustomInvulTicks";
    private static final String SET_INVUL = "revelationfix$setCustomInvulTicks";
    private static final String GET_HURT_TICKS = "revelationfix$getCustomHurtTicks";
    private static final String SET_HURT_TICKS = "revelationfix$setCustomHurtTicks";

    private static final String HELPER_CLASS = "z1gned.goetyrevelation.util.ApollyonAbilityHelper";
    private static final String GET_COOLDOWN = "allTitlesApostle_1_20_1$getHitCooldown";
    private static final String SET_COOLDOWN = "allTitlesApostle_1_20_1$setHitCooldown";

    private static Class<?> invulIface;
    private static Method invulGet;
    private static Method invulSet;
    private static Method hurtTicksGet;
    private static Method hurtTicksSet;
    private static Class<?> helper;
    private static Method getter;
    private static Method setter;
    private static boolean resolved;

    private static synchronized void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        try {
            Class<?> c = Class.forName(INVUL_IFACE, false, GoetyCompat.class.getClassLoader());
            invulGet = c.getMethod(GET_INVUL);
            invulSet = c.getMethod(SET_INVUL, int.class);
            hurtTicksGet = c.getMethod(GET_HURT_TICKS);
            hurtTicksSet = c.getMethod(SET_HURT_TICKS, int.class);
            invulIface = c;
        } catch (Throwable ignored) {

            invulIface = null;
            invulGet = null;
            invulSet = null;
            hurtTicksGet = null;
            hurtTicksSet = null;
        }
        try {
            Class<?> c = Class.forName(HELPER_CLASS, false, GoetyCompat.class.getClassLoader());
            Method g = c.getMethod(GET_COOLDOWN);
            Method s = c.getMethod(SET_COOLDOWN, int.class);
            helper = c;
            getter = g;
            setter = s;
        } catch (Throwable ignored) {

            helper = null;
            getter = null;
            setter = null;
        }
    }

    public static boolean inHitCooldown(LivingEntity target) {
        return hitCooldownOf(target) > 0;
    }

    public static int hitCooldownOf(LivingEntity target) {
        resolve();
        if (invulIface != null && invulIface.isInstance(target)) {
            try {
                Object v = invulGet.invoke(target);
                if (v instanceof Integer) {
                    return (Integer) v;
                }
            } catch (Throwable ignored) {

            }
        }
        if (helper == null || !helper.isInstance(target)) {
            return -1;
        }
        try {
            Object v = getter.invoke(target);
            return v instanceof Integer ? (Integer) v : -1;
        } catch (Throwable ignored) {
            return -1;
        }
    }

    public static boolean clearHitCooldown(LivingEntity target) {
        int before = hitCooldownOf(target);
        if (before <= 0) {
            return false;
        }
        resolve();
        boolean viaFacade = invulIface != null && invulIface.isInstance(target);
        try {
            if (viaFacade) {
                invulSet.invoke(target, 0);

                try {
                    if (((Integer) hurtTicksGet.invoke(target)) > 0) {
                        hurtTicksSet.invoke(target, 0);
                    }
                } catch (Throwable ignored) {

                }
            } else {
                setter.invoke(target, 0);
            }
        } catch (Throwable ignored) {
            return false;
        }

        int now = hitCooldownOf(target);
        if (now > 0) {
            warnBlocked(target, now);
            return false;
        }
        return true;
    }

    public static boolean clearAllInvul(LivingEntity target) {
        boolean any = clearHitCooldown(target);
        any |= zeroField(target, "moddedInvul");
        any |= zeroField(target, "obsidianInvul");
        return any;
    }

    private static boolean zeroField(LivingEntity target, String name) {
        try {
            java.lang.reflect.Field f = target.getClass().getField(name);
            if (f.getInt(target) <= 0) {
                return false;
            }
            f.setInt(target, 0);
            return f.getInt(target) <= 0;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static int moddedInvulOf(LivingEntity target) {
        return intFieldOf(target, "moddedInvul");
    }

    public static int obsidianInvulOf(LivingEntity target) {
        return intFieldOf(target, "obsidianInvul");
    }

    public static boolean settingUpSecond(LivingEntity target) {
        try {
            return (Boolean) target.getClass().getMethod("isSettingUpSecond").invoke(target);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static int intFieldOf(LivingEntity target, String name) {
        try {
            return target.getClass().getField(name).getInt(target);
        } catch (Throwable ignored) {
            return -1;
        }
    }

    public static void dumpOnce(LivingEntity target) {
        if (dumped) {
            return;
        }
        dumped = true;
        org.apache.logging.log4j.Logger log =
                org.apache.logging.log4j.LogManager.getLogger("ACPierce");
        StringBuilder sb = new StringBuilder();
        sb.append("\n  === health related ===");
        sb.append("\n    getHealth()      = ").append(target.getHealth());
        sb.append("\n    getMaxHealth()   = ").append(target.getMaxHealth());
        sb.append("\n    getAbsorption()  = ").append(target.getAbsorptionAmount());
        try {
            sb.append("\n    entityData(trueLife) = ").append(ACLife.trueLife(target));
        } catch (Throwable t) {
            sb.append("\n    entityData(trueLife) = unreadable: ").append(t);
        }
        sb.append("\n  === persistentData(ForgeData)===");
        try {
            net.minecraft.nbt.CompoundTag tag = target.getPersistentData();
            if (tag.isEmpty()) {
                sb.append("\n    (empty)");
            } else {
                for (String k : tag.getAllKeys()) {
                    sb.append("\n    ").append(k).append(" = ").append(tag.get(k));
                }
            }
        } catch (Throwable t) {
            sb.append("\n    unreadable: ").append(t);
        }
        sb.append("\n  === float/int fields along the whole inheritance chain (looking for the separate store) ===");
        try {
            for (Class<?> c = target.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
                for (java.lang.reflect.Field f : c.getDeclaredFields()) {
                    if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                        continue;
                    }
                    Class<?> t2 = f.getType();
                    if (t2 != float.class && t2 != int.class && t2 != double.class) {
                        continue;
                    }
                    f.setAccessible(true);
                    Object v = f.get(target);
                    double d = ((Number) v).doubleValue();

                    if (d > 1.0 && d <= target.getMaxHealth() + 1.0) {
                        sb.append("\n    ").append(c.getSimpleName()).append('.')
                                .append(f.getName()).append(" = ").append(v);
                    }
                }
            }
        } catch (Throwable t) {
            sb.append("\n    scan failed: ").append(t);
        }
        log.warn("[goety-dump] health store dump of {}: {}", target.getType(), sb);
    }

    private static volatile boolean dumped;

    private static void warnBlocked(LivingEntity target, int still) {
        if (warned) {
            return;
        }
        warned = true;
        org.apache.logging.log4j.LogManager.getLogger("ACPierce").warn(
                "[goety-compat] clearing hitCooldown of {} did not take effect (still {} after the call)"
                        + " -- most likely a mod intercepts SynchedEntityData#set. moddedInvul={}",
                target.getType(), still, moddedInvulOf(target));
    }

    private static volatile boolean warned;
}
