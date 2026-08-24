package cn.academy.api;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AbyssBackends {

    private static final Logger LOG = LoggerFactory.getLogger("AcademyCraft/Abyss");

    private AbyssBackends() {}

    private static final AbyssBackend VANILLA = new AbyssBackend() {
        @Override
        public String id() {
            return "vanilla";
        }
    };

    private static volatile AbyssBackend active = VANILLA;

    public static synchronized void register(AbyssBackend backend) {
        if (backend == null) {
            if (active != VANILLA) {
                LOG.info("[abyss] backend unregistered, Abyss Stride falls back to an empty passive");
            }
            active = VANILLA;
            return;
        }
        if (active != VANILLA) {
            LOG.warn("[abyss] backend {} is already installed, refusing to register {} -- install only one Abyss Stride addon",
                    active.id(), backend.id());
            return;
        }
        active = backend;
        LOG.info("[abyss] Abyss Stride backend took over: {}", backend.id());
    }

    public static AbyssBackend get() {
        return active;
    }

    public static boolean isExternal() {
        return active != VANILLA;
    }

    public static float cpMultiplier() {
        AbyssBackend b = active;
        if (b == VANILLA) {
            return 1.0f;
        }
        try {
            float v = b.cpMultiplier();

            if (!(v > 0.0f) || Float.isInfinite(v)) {
                LOG.error("[abyss] backend {} returned an unusable multiplier {}, treated as 1.0", b.id(), v);
                return 1.0f;
            }
            return v;
        } catch (Throwable t) {
            fail(b, "cpMultiplier", t);
            return 1.0f;
        }
    }

    @Nullable
    public static Player takeoverDamage(Player attacker, LivingEntity target) {
        AbyssBackend b = active;
        try {
            return b.takeoverDamage(attacker, target);
        } catch (Throwable t) {
            fail(b, "takeoverDamage", t);
            return null;
        }
    }

    private static void fail(AbyssBackend b, String where, Throwable t) {
        LOG.error("[abyss] backend {} threw in {}, detached -- Abyss Stride falls back to an empty passive", b.id(), where, t);
        active = VANILLA;
    }
}
