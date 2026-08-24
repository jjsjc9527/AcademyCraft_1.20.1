package cn.academy.api;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DamageBackends {

    private static final Logger LOG = LoggerFactory.getLogger("AcademyCraft/Damage");

    private DamageBackends() {}

    private static final DamageBackend VANILLA = new DamageBackend() {
        @Override
        public String id() {
            return "vanilla";
        }
    };

    private static volatile DamageBackend active = VANILLA;

    public static synchronized void register(DamageBackend backend) {
        if (backend == null) {
            if (active != VANILLA) {
                LOG.info("[damage] backend unregistered, falling back to the built-in implementation");
            }
            active = VANILLA;
            return;
        }
        if (active != VANILLA) {
            LOG.warn("[damage] backend {} is already installed, refusing to register {} -- install only one damage addon",
                    active.id(), backend.id());
            return;
        }
        active = backend;
        LOG.info("[damage] damage backend took over: {}", backend.id());
    }

    public static DamageBackend get() {
        return active;
    }

    public static boolean isExternal() {
        return active != VANILLA;
    }

    public static boolean handles(Player attacker, LivingEntity target) {
        DamageBackend b = active;
        if (b == VANILLA) {
            return false;
        }
        try {
            return b.handles(attacker, target);
        } catch (Throwable t) {
            fail(b, "handles", t);
            return false;
        }
    }

    @Nullable
    public static Boolean strike(Player attacker, LivingEntity target,
                                 DamageSource normal, float damage) {
        DamageBackend b = active;
        if (b == VANILLA) {
            return null;
        }
        try {
            return b.strike(attacker, target, normal, damage);
        } catch (Throwable t) {
            fail(b, "strike", t);
            return null;
        }
    }

    private static void fail(DamageBackend b, String where, Throwable t) {
        LOG.error("[damage] backend {} threw in {}, detached and fell back to the built-in implementation", b.id(), where, t);
        active = VANILLA;
    }
}
