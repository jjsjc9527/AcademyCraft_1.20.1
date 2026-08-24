package cn.academy.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

public final class MotionProbe {

    private MotionProbe() {}

    private static final org.slf4j.Logger LOG =
            org.slf4j.LoggerFactory.getLogger("ACMotionProbe");

    private static final Map<String, Long> SEEN = new HashMap<>();

    private static final long THROTTLE_MS = 5000L;

    private static boolean ours(String cls) {
        return cls.startsWith("net.minecraft.")
                || cls.startsWith("net.minecraftforge.")
                || cls.startsWith("cn.academy.")
                || cls.startsWith("cn.lambdalib2.")
                || cls.startsWith("cpw.mods.")
                || cls.startsWith("org.spongepowered.")
                || cls.startsWith("java.")
                || cls.startsWith("jdk.")
                || cls.startsWith("sun.");
    }

    public static boolean shouldBlock(Entity e, Vec3 next) {
        try {
            if (!(e instanceof Player p)) {
                return false;
            }

            if (!cn.academy.api.ACImmortal.covers(p) || p.level().isClientSide) {
                return false;
            }
            Vec3 cur = p.getDeltaMovement();

            if (cur.distanceToSqr(next) < 1.0e-4) {
                return false;
            }
            String who = thirdParty();
            if (who == null) {
                return false;
            }

            boolean block = cn.academy.api.ACImmortal.isImmortal(p);

            log("velocity", p, who, fmt(cur) + " → " + fmt(next), block);
            return block;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void notePos(Entity e, double x, double y, double z) {
        try {
            if (!cn.academy.api.ACImmortal.DIAG || !(e instanceof Player p)) {
                return;
            }
            if (!cn.academy.api.ACImmortal.covers(p)) {
                return;
            }
            double dx = x - p.getX(), dy = y - p.getY(), dz = z - p.getZ();

            if (dx * dx + dy * dy + dz * dz < 2.25) {
                return;
            }
            String who = thirdParty();
            if (who == null) {
                return;
            }
            log("position", p, who,
                    String.format("(%.1f, %.1f, %.1f) → (%.1f, %.1f, %.1f)",
                            p.getX(), p.getY(), p.getZ(), x, y, z),
                    false);
        } catch (Throwable ignored) {

        }
    }

    private static String thirdParty() {
        return StackWalker.getInstance()
                .walk(s -> s.limit(24)
                        .map(StackWalker.StackFrame::getClassName)
                        .filter(n -> !ours(n))
                        .findFirst())
                .orElse(null);
    }

    private static void log(String kind, Player p, String who, String change, boolean blocked) {
        if (!cn.academy.api.ACImmortal.DIAG) {
            return;
        }
        String key = p.getId() + ":" + kind + ":" + who;
        long now = System.currentTimeMillis();
        Long last = SEEN.get(key);
        if (last != null && now - last < THROTTLE_MS) {
            return;
        }
        if (SEEN.size() > 128) {
            SEEN.clear();
        }
        SEEN.put(key, now);
        if (cn.academy.util.ACDiag.ON)
        LOG.warn("[motion/{}] {} side | {} of {} was overwritten by {}: {} | {}",
                kind, p.level().isClientSide ? "client" : "server",
                p.getName().getString(), kind, who, change,
                blocked ? "blocked (discarded)" : "allowed (observed only)");
    }

    private static String fmt(Vec3 v) {
        return String.format("(%.3f, %.3f, %.3f)", v.x, v.y, v.z);
    }

    public static void clear() {
        SEEN.clear();
    }
}
