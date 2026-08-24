package cn.academy.api;

import net.minecraft.world.entity.LivingEntity;

public final class Vettore {

    private Vettore() {}

    public static final String NBT = "AcademyVettore";

    public static float get(LivingEntity e) {
        return -Math.min(0.0f, raw(e));
    }

    public static float raw(LivingEntity e) {
        if (e == null) {
            return 0.0f;
        }
        return e instanceof VettoreHolder h ? Math.min(0.0f, h.academy$vettore()) : 0.0f;
    }

    public static float add(LivingEntity e, float amount) {
        if (e == null || !(amount > 0.0f) || !(e instanceof VettoreHolder)) {
            return 0.0f;
        }
        if (e.level() == null || e.level().isClientSide) {
            return 0.0f;
        }

        if (e instanceof net.minecraft.world.entity.player.Player p
                && (p.getAbilities().invulnerable || p.isSpectator())) {
            return 0.0f;
        }
        try {

            float eaten = cn.academy.api.ACImmortal.absorb(e, amount);
            if (eaten > 0.0f) {
                amount -= eaten;
                if (!(amount > 0.0f)) {
                    return 0.0f;
                }
            }
            traceChain(e);
            float before = e.getHealth();
            float cur = raw(e);

            float floor = -Math.max(1.0f, e.getMaxHealth()) * 10.0f;
            float next = Math.max(floor, cur - amount);
            if (next >= cur) {
                return 0.0f;
            }
            ((VettoreHolder) e).academy$setVettore(next);
            return Math.max(0.0f, before - e.getHealth());
        } catch (Throwable ignored) {
            return 0.0f;
        }
    }

    private static final java.util.Map<Integer, Long> TRACE_AT = new java.util.HashMap<>();

    public static void trace(LivingEntity e, float orig, float capped, float delta) {

        if (!cn.academy.util.ACDiag.ON) {
            return;
        }
        try {
            if (e.level() == null || e.level().isClientSide) {
                return;
            }
            long now = e.level().getGameTime();
            Long last = TRACE_AT.get(e.getId());
            if (last != null && now - last < 20L) {
                return;
            }
            TRACE_AT.put(e.getId(), now);
            org.slf4j.LoggerFactory.getLogger("Vettore").info(
                    "[vettore-hook] {} injection ran | Vettore={} | original return={} | after cap={} | {}",
                    net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(e.getType()),
                    String.format("%.1f", delta), String.format("%.2f", orig),
                    String.format("%.2f", capped),
                    capped < orig ? "return value rewritten" : "unchanged (capped value is not smaller than the original)");
        } catch (Throwable ignored) {

        }
    }

    private static final java.util.Set<String> CHAIN_DONE = new java.util.HashSet<>();

    public static void traceChain(LivingEntity e) {

        if (!cn.academy.util.ACDiag.ON) {
            return;
        }
        try {
            String type = String.valueOf(
                    net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(e.getType()));
            if (!CHAIN_DONE.add(type)) {
                return;
            }
            StringBuilder sb = new StringBuilder();
            String owner = "?";
            for (Class<?> c = e.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
                sb.append("\n").append(c.getName());
                boolean hit = false;
                for (java.lang.reflect.Method m : c.getDeclaredMethods()) {
                    if (m.getParameterCount() == 0
                            && (m.getName().equals("m_21223_") || m.getName().equals("getHealth"))
                            && m.getReturnType() == float.class) {
                        hit = true;
                    }
                }
                if (hit) {
                    sb.append("   <-- overrides getHealth here");
                    if ("?".equals(owner)) {
                        owner = c.getName();
                    }
                }
            }
            org.slf4j.LoggerFactory.getLogger("Vettore").warn(
                    "[vettore-chain] getHealth of {} actually comes from {}{}"
                            + "\n(if this is not net.minecraft.world.entity.LivingEntity, "
                            + "our LivingEntity injection cannot reach it)"
                            + "\ninheritance chain: {}",
                    type, owner, "net.minecraft.world.entity.LivingEntity".equals(owner)
                            ? "  this is the one we injected" : "  this is NOT the one we injected",
                    sb);
        } catch (Throwable ignored) {

        }
    }

    public static float reduce(LivingEntity e, float amount) {
        if (e == null || !(amount > 0.0f) || !(e instanceof VettoreHolder h)) {
            return 0.0f;
        }
        if (e.level() == null || e.level().isClientSide) {
            return 0.0f;
        }
        float cur = Math.min(0.0f, h.academy$vettore());
        if (cur >= 0.0f) {
            return 0.0f;
        }
        float next = Math.min(0.0f, cur + amount);
        h.academy$setVettore(next);
        return next - cur;
    }

    public static void clear(LivingEntity e) {
        if (e == null || e.level() == null || e.level().isClientSide) {
            return;
        }
        if (e instanceof VettoreHolder h && h.academy$vettore() < 0.0f) {
            h.academy$setVettore(0.0f);
        }
    }

    @net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = cn.academy.AcademyCraft.MODID)
    public static final class Cleanup {

        private Cleanup() {}

        @net.minecraftforge.eventbus.api.SubscribeEvent
        public static void onClone(net.minecraftforge.event.entity.player.PlayerEvent.Clone event) {
            if (!event.isWasDeath()) {
                return;
            }
            try {
                trimToSurvivable(event.getEntity());
            } catch (Throwable ignored) {

            }
        }
    }

    public static float trimToSurvivable(LivingEntity e) {
        if (e == null || e.level() == null || e.level().isClientSide) {
            return 0.0f;
        }
        float lethal = e.getMaxHealth() - 1.0f;
        float vet = get(e);
        if (vet <= lethal) {
            return 0.0f;
        }
        return reduce(e, vet - lethal);
    }
}
