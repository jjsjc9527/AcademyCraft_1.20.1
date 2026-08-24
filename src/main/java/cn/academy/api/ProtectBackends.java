package cn.academy.api;

import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ProtectBackends {

    private static final Logger LOG = LoggerFactory.getLogger("AcademyCraft/Protect");

    private ProtectBackends() {}

    private static final ProtectBackend VANILLA = new ProtectBackend() {
        @Override
        public String id() {
            return "vanilla";
        }
    };

    private static volatile ProtectBackend active = VANILLA;

    public static synchronized void register(ProtectBackend backend) {
        if (backend == null) {
            if (active != VANILLA) {
                LOG.info("[protect] backend unregistered, falling back to the built-in implementation");
            }
            active = VANILLA;
            return;
        }
        if (active != VANILLA) {
            LOG.warn("[protect] backend {} is already installed, refusing to register {} -- install only one protection addon",
                    active.id(), backend.id());
            return;
        }
        active = backend;
        LOG.info("[protect] protection backend took over: {} (preventsRemoval={})",
                backend.id(), safePreventsRemoval(backend));
    }

    public static ProtectBackend get() {
        return active;
    }

    public static boolean isExternal() {
        return active != VANILLA;
    }

    public static void onTakeOver(ServerPlayer player) {
        ProtectBackend b = active;
        if (b == VANILLA) {
            return;
        }
        try {
            b.onTakeOver(player);
        } catch (Throwable t) {
            fail(b, "onTakeOver", t);
        }
    }

    public static void onRelease(ServerPlayer player) {
        ProtectBackend b = active;
        if (b == VANILLA) {
            return;
        }
        try {
            b.onRelease(player);
        } catch (Throwable t) {
            fail(b, "onRelease", t);
        }
    }

    public static boolean preventsRemoval() {
        ProtectBackend b = active;
        return b != VANILLA && safePreventsRemoval(b);
    }

    private static boolean safePreventsRemoval(ProtectBackend b) {
        try {
            return b.preventsRemoval();
        } catch (Throwable t) {
            fail(b, "preventsRemoval", t);
            return false;
        }
    }

    private static void fail(ProtectBackend b, String where, Throwable t) {
        LOG.error("[protect] backend {} threw in {}, detached and fell back to the built-in implementation", b.id(), where, t);
        active = VANILLA;
    }
}
