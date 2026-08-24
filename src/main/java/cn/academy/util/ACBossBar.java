package cn.academy.util;

import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ACBossBar {

    private static final int INTERVAL = 4;

    private static final Map<Class<?>, List<Field>> CACHE = new ConcurrentHashMap<>();

    private ACBossBar() {}

    public static boolean refresh(LivingEntity e) {
        if (e.level().isClientSide || e.tickCount % INTERVAL != 0) {
            return false;
        }
        List<Field> fs = CACHE.computeIfAbsent(e.getClass(), ACBossBar::collect);
        if (fs.isEmpty()) {
            return false;
        }
        float max = e.getMaxHealth();
        if (max <= 0.0f) {
            return false;
        }
        float p = Math.max(0.0f, Math.min(1.0f, e.getHealth() / max));
        boolean any = false;
        for (Field f : fs) {
            try {
                Object v = f.get(e);
                if (v instanceof ServerBossEvent ev) {

                    ev.setProgress(p);
                    any = true;
                }
            } catch (Throwable ignored) {

            }
        }
        return any;
    }

    private static List<Field> collect(Class<?> cls) {
        List<Field> out = new ArrayList<>();
        for (Class<?> c = cls; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) {
                    continue;
                }
                if (!ServerBossEvent.class.isAssignableFrom(f.getType())) {
                    continue;
                }
                try {
                    f.setAccessible(true);
                    out.add(f);
                } catch (Throwable ignored) {

                }
            }
        }
        return out;
    }
}
