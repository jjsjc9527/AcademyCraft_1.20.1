package cn.academy.api;

import cn.academy.ability.vanilla.vecmanip.advanced.WhiteWingGuard;
import cn.academy.config.AbilityConfig;
import cn.academy.datapart.CPData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class ACImmortal {

    private ACImmortal() {}

    private static float cpUnitOf(Player holder) {
        return AbilityConfig.stat("dual_wing", "immortal_cp", WhiteWingGuard.expOf(holder));
    }

    public static boolean anyImmortal() {
        return WhiteWingGuard.hasAnyGuard();
    }

    public static boolean covers(LivingEntity victim) {
        return victim != null && !forceKilling(victim) && WhiteWingGuard.coversId(victim.getId());
    }

    private static final java.util.Map<java.util.UUID, Long> FORCE_KILL = new java.util.HashMap<>();

    private static final long FORCE_KILL_MS = 5000L;

    public static void beginForceKill(LivingEntity e) {
        if (e != null) {
            FORCE_KILL.put(e.getUUID(), System.currentTimeMillis());
        }
    }

    private static boolean forceKilling(LivingEntity e) {
        if (FORCE_KILL.isEmpty()) {
            return false;
        }
        Long at = FORCE_KILL.get(e.getUUID());
        if (at == null) {
            return false;
        }
        if (System.currentTimeMillis() - at > FORCE_KILL_MS) {
            FORCE_KILL.remove(e.getUUID());
            return false;
        }
        return true;
    }

    public static boolean isImmortal(LivingEntity victim) {
        if (!covers(victim)) {
            return false;
        }
        Player holder = WhiteWingGuard.holderOf(victim);
        if (holder == null) {
            return false;
        }
        CPData cp = WhiteWingGuard.cpOfPublic(holder);
        return cp != null && cp.getCP() > 0.0f;
    }

    public static float absorb(LivingEntity victim, float damage) {
        if (victim == null || !(damage > 0.0f)) {
            return 0.0f;
        }
        if (victim.level() == null || victim.level().isClientSide) {
            return 0.0f;
        }
        if (!covers(victim)) {
            diag(victim, "miss", "not covered (coverage is published by WhiteWingGuard.onPlayerTick every tick)");
            return 0.0f;
        }
        Player holder = WhiteWingGuard.holderOf(victim);
        if (holder == null) {
            diag(victim, "noholder", "covered but holder unavailable -- stale entry / cross dimension / holder removed");
            return 0.0f;
        }
        CPData cp = WhiteWingGuard.cpOfPublic(holder);
        if (cp == null) {
            diag(victim, "nocp", "CPData of the holder unavailable (death / entity swap window)");
            return 0.0f;
        }

        float unit = cpUnitOf(holder);
        if (unit <= 0.0f) {

            rescueFromVoid(victim);
            WhiteWingGuard.noteImmortalHit(victim);
            diag(victim, "free", String.format("unit price 0, absorbing all %.2f", damage));
            return damage;
        }

        float avail = cp.getCP();
        if (avail <= 0.0f) {
            diag(victim, "nocp0", String.format(
                    "CP exhausted, immunity off (request %.2f, holder %s)", damage, holder.getName().getString()));
            return 0.0f;
        }

        float eaten = Math.min(damage, avail / unit);
        if (eaten <= 0.0f) {
            diag(victim, "afford0", String.format("CP %.1f cannot pay for even 1 damage (unit price %.2f)", avail, unit));
            return 0.0f;
        }

        boolean paid = cp.perform(0.0f, eaten * unit);
        rescueFromVoid(victim);
        WhiteWingGuard.noteImmortalHit(victim);

        diag(victim, "eat", String.format("absorbed %.2f / requested %.2f | spent %.1f CP (%s) | left %.0f",
                eaten, damage, eaten * unit, paid ? "ok" : "failed", cp.getCP()));
        return eaten;
    }

    public static boolean DIAG = false;

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("ACImmortal");

    private static final java.util.Map<String, Long> DIAG_AT = new java.util.HashMap<>();

    private static void diag(LivingEntity victim, String tag, String msg) {
        if (!DIAG || !(victim instanceof Player)) {
            return;
        }
        try {
            long now = victim.level().getGameTime();
            String key = victim.getId() + ":" + tag;
            Long last = DIAG_AT.get(key);
            if (last != null && now >= last && now - last < 20L) {
                return;
            }
            if (DIAG_AT.size() > 256) {
                DIAG_AT.clear();
            }
            DIAG_AT.put(key, now);
            LOG.info("[immortal/{}] {} | {}", tag, victim.getName().getString(), msg);
        } catch (Throwable ignored) {

        }
    }

    public static void noteLeak(LivingEntity victim, float drop, String why) {
        if (!DIAG) {
            return;
        }
        diag(victim, "leak", String.format("health dropped by %.2f -- %s", drop, why));
    }

    public static void dissolveVettore(LivingEntity victim) {
        if (victim == null || victim.level() == null || victim.level().isClientSide) {
            return;
        }
        float vet = Vettore.get(victim);
        if (vet <= 0.0f) {
            return;
        }
        float eaten = absorb(victim, vet);
        if (eaten <= 0.0f) {
            return;
        }
        float gone = Vettore.reduce(victim, eaten);
        diag(victim, "vettore", String.format(
                "cleared %.1f Vettore (%.1f still attached) -- it pushes getHealth negative and fakes death",
                gone, Vettore.get(victim)));
    }

    public static void dissolveSuppression(LivingEntity victim) {
        if (victim == null || victim.level() == null || victim.level().isClientSide) {
            return;
        }
        float reading = victim.getHealth();
        float real = cn.academy.util.ACLife.trueLife(victim);
        if (real <= 0.0f) {
            return;
        }

        if (reading >= real - 0.01f) {
            return;
        }
        String via = cn.academy.util.FeCompat.unblock(victim);
        if (via == null) {
            diag(victim, "suppress", String.format(
                    "reading suppressed to %.1f (trueLife %.1f) and cannot be cleared -- immunity would lock the player on the death screen", reading, real));
            return;
        }

        absorb(victim, real - reading);
        diag(victim, "suppress", String.format(
                "external suppression cleared (%s): reading %.1f -> %.1f (trueLife %.1f)",
                via, reading, victim.getHealth(), real));
    }

    private static final java.util.Map<java.util.UUID, Float> LAST_LIFE = new java.util.HashMap<>();

    public static void clearLifeMemo() {
        LAST_LIFE.clear();
        DIAG_AT.clear();
        FORCE_KILL.clear();
    }

    public static void holdLife(LivingEntity victim) {
        if (victim == null || victim.level() == null || victim.level().isClientSide) {
            return;
        }
        java.util.UUID id = victim.getUUID();
        float now = cn.academy.util.ACLife.trueLife(victim);
        Float last = LAST_LIFE.put(id, now);
        if (last == null) {
            return;
        }
        float drop = last - now;
        if (drop <= 0.01f) {
            return;
        }

        float eaten = absorb(victim, drop);
        if (eaten <= 0.0f) {
            return;
        }
        float back = Math.min(victim.getMaxHealth(), now + eaten);
        cn.academy.util.ACLife.forceWriteLife(victim, back);
        float after = cn.academy.util.ACLife.trueLife(victim);
        LAST_LIFE.put(id, after);
        diag(victim, "hold", String.format(
                "something bypassed setHealth and drained %.2f health (%.1f -> %.1f) -- restored %.1f",
                drop, last, now, after));
    }

    private static void rescueFromVoid(LivingEntity victim) {
        if (victim.getY() >= victim.level().getMinBuildHeight() - 64.0) {
            return;
        }
        if (!(victim.level() instanceof net.minecraft.server.level.ServerLevel level)) {
            return;
        }
        net.minecraft.core.BlockPos spawn = level.getSharedSpawnPos();

        victim.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        victim.fallDistance = 0.0f;
        victim.teleportTo(spawn.getX() + 0.5, spawn.getY() + 1.0, spawn.getZ() + 0.5);
    }

    public static boolean shouldBlockDeath(Object victim) {
        try {
            if (!(victim instanceof LivingEntity e)) {
                return false;
            }
            return WhiteWingGuard.onForeignDie(e);
        } catch (Throwable ignored) {
            return false;
        }
    }
}
