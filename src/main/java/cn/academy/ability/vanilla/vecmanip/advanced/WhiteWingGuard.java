package cn.academy.ability.vanilla.vecmanip.advanced;

import cn.academy.ACParticles;
import cn.academy.ACSounds;
import cn.academy.ability.Skill;
import cn.academy.config.AbilityConfig;
import cn.academy.datapart.AbilityData;
import cn.academy.datapart.CPData;
import cn.academy.util.ACLife;
import cn.academy.util.ACPierce;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class WhiteWingGuard {

    private WhiteWingGuard() {}

    private static final class Cover {

        Player holder;
        long stamp;

        boolean boosted;

        int entityId;

        Cover(Player holder, long stamp, boolean boosted, int entityId) {
            this.holder = holder;
            this.stamp = stamp;
            this.boosted = boosted;
            this.entityId = entityId;
        }
    }

    private static final Map<UUID, Cover> COVER = new HashMap<>();

    private static final it.unimi.dsi.fastutil.ints.IntOpenHashSet COVER_IDS =
            new it.unimi.dsi.fastutil.ints.IntOpenHashSet();

    public static boolean coversId(int entityId) {
        return !COVER_IDS.isEmpty() && COVER_IDS.contains(entityId);
    }

    private static boolean heldBy(Cover cover, Player holder) {
        return cover.holder.getUUID().equals(holder.getUUID());
    }

    private static final Map<UUID, Long> LAST_TAKEOVER = new HashMap<>();

    private static final int STALE_TICKS = 4;

    private static final java.util.Set<UUID> BOOST_ON = new java.util.HashSet<>();

    public static void setBoostField(Player holder, boolean on) {
        if (holder == null || holder.level().isClientSide) {
            return;
        }
        if (on) {
            BOOST_ON.add(holder.getUUID());
        } else {
            BOOST_ON.remove(holder.getUUID());
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) {
            return;
        }
        Player holder = event.player;
        if (holder == null || holder.level().isClientSide || holder.isRemoved()) {
            return;
        }

        if (!cn.lambdalib2.datapart.EntityData.isReady(holder)) {
            diagTick(holder, "EntityData not ready (death / entity swap window) -- not publishing this tick");
            return;
        }
        AbilityData data = AbilityData.get(holder);
        if (data == null) {
            diagTick(holder, "AbilityData is null -- not publishing this tick");
            return;
        }
        if (!data.hasCategory()) {
            diagTick(holder, "no ability category (hasCategory=false) -- not taking over");
            return;
        }
        if (!data.isSkillLearned(DualWing.INSTANCE)) {
            diagTick(holder, "dual wing not learned (isSkillLearned=false) -- not taking over");
            return;
        }
        List<Player> covered = new java.util.ArrayList<>();
        List<Player> boosted = new java.util.ArrayList<>();
        scanCircle(holder, covered, boosted);
        publish(holder, covered, boosted, holder.level().getGameTime());
        diagTick(holder, String.format("published: %d players covered (boosted %d) | maps COVER=%d IDS=%d | CP=%d",
                covered.size(), boosted.size(), COVER.size(), COVER_IDS.size(), cpCP(holder)));
    }

    private static final Map<UUID, Long> IMMORTAL_TICK_DIAG = new HashMap<>();

    private static long academy$costSum;
    private static int academy$costCount;
    private static long academy$costMax;
    private static long academy$costReportAt;

    private static void academy$noteCost(Player p, long nanos) {
        if (!cn.academy.api.ACImmortal.DIAG) {
            return;
        }
        try {
            academy$costSum += nanos;
            academy$costCount++;
            if (nanos > academy$costMax) {
                academy$costMax = nanos;
            }
            long now = p.level().getGameTime();
            if (academy$costReportAt == 0L) {
                academy$costReportAt = now;
                return;
            }
            if (now - academy$costReportAt < 100L) {
                return;
            }
            academy$costReportAt = now;
            LOG.warn("[immortal/cost] immunity trio: {} calls | avg {} us | peak {} us"
                            + " | total {} ms (for reference one tick is 50ms)",
                    academy$costCount, academy$costSum / Math.max(1, academy$costCount) / 1000,
                    academy$costMax / 1000, academy$costSum / 1_000_000);
            academy$costSum = 0;
            academy$costCount = 0;
            academy$costMax = 0;
        } catch (Throwable ignored) {

        }
    }

    private static void diagTick(Player holder, String msg) {
        if (!cn.academy.api.ACImmortal.DIAG) {
            return;
        }
        try {
            long now = holder.level().getGameTime();
            Long last = IMMORTAL_TICK_DIAG.get(holder.getUUID());
            if (last != null && now >= last && now - last < 100L) {
                return;
            }
            if (IMMORTAL_TICK_DIAG.size() > 128) {
                IMMORTAL_TICK_DIAG.clear();
            }
            IMMORTAL_TICK_DIAG.put(holder.getUUID(), now);
            LOG.info("[immortal/tick] {} | {}", holder.getName().getString(), msg);
        } catch (Throwable ignored) {

        }
    }

    static void scanCircle(Player holder, List<Player> covered, List<Player> boosted) {
        covered.add(holder);
        boolean boostOn = BOOST_ON.contains(holder.getUUID());
        double range = AbilityConfig.stat("dual_wing", "guard_range", expOf(holder));
        double r2 = range * range;
        net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(
                holder.getX() - range, holder.level().getMinBuildHeight(), holder.getZ() - range,
                holder.getX() + range, holder.level().getMaxBuildHeight(), holder.getZ() + range);

        for (Player p : holder.level().getEntitiesOfClass(Player.class, box)) {

            if (p == holder || p.isRemoved() || p.isSpectator()) {
                continue;
            }
            double dx = p.getX() - holder.getX();
            double dz = p.getZ() - holder.getZ();
            if (dx * dx + dz * dz > r2) {
                continue;
            }
            if (!holder.isAlliedTo(p)) {
                continue;
            }
            covered.add(p);
            if (boostOn && hasPassiveSkill(p)) {
                boosted.add(p);
            }
        }
    }

    public static void publish(Player holder, List<Player> covered, List<Player> boosted, long tick) {
        if (holder.level().isClientSide) {
            return;
        }
        float rate = AbilityConfig.stat("dual_wing", "guard_cp_boost", expOf(holder));

        Iterator<Map.Entry<UUID, Cover>> it = COVER.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Cover> e = it.next();
            if (!heldBy(e.getValue(), holder)) {
                continue;
            }
            if (!containsId(covered, e.getKey())) {
                drop(holder, e.getKey(), e.getValue(), "publish: revoked by diff");
                it.remove();
            }
        }

        for (Player p : covered) {
            boolean wantBoost = boosted.contains(p);
            Cover cur = COVER.get(p.getUUID());
            if (cur != null && heldBy(cur, holder)) {

                cur.holder = holder;

                if (cur.entityId != p.getId()) {
                    COVER_IDS.remove(cur.entityId);
                    cur.entityId = p.getId();
                    COVER_IDS.add(cur.entityId);
                }
            }
            if (cur != null && !heldBy(cur, holder)) {

                if (cur.holder.getUUID().equals(p.getUUID())) {
                    continue;
                }

                drop(cur.holder, p.getUUID(), cur, "publish: holder changed");
                cur = null;
            }
            if (cur == null) {
                cur = new Cover(holder, tick, false, p.getId());
                COVER.put(p.getUUID(), cur);
                COVER_IDS.add(cur.entityId);
            }
            cur.stamp = tick;
            applyBoost(p, cur, wantBoost, rate);

            long academy$t0 = System.nanoTime();
            cn.academy.api.ACImmortal.dissolveVettore(p);

            cn.academy.api.ACImmortal.dissolveSuppression(p);

            cn.academy.api.ACImmortal.holdLife(p);
            academy$noteCost(p, System.nanoTime() - academy$t0);
            LifeVector.ensure(p);

            CPData cpOn = cpOf(p);
            if (cpOn != null) {
                cpOn.setLifeGuarded(true);
            }
        }

        sweep(tick);
    }

    public static void revoke(Player holder) {
        if (holder.level().isClientSide) {
            return;
        }
        COVER.entrySet().removeIf(e -> {
            if (!heldBy(e.getValue(), holder)) {
                return false;
            }
            drop(holder, e.getKey(), e.getValue(), "revoke: skill terminated or closed");
            return true;
        });
    }

    private static void applyBoost(Player p, Cover cover, boolean want, float rate) {
        if (want == cover.boosted) {
            if (want) {

                CPData cpKeep = cpOf(p);
                if (cpKeep != null) {
                    cpKeep.setExtraMaxCpRate(rate);
                }
            }
            return;
        }
        cover.boosted = want;
        CPData cpBoost = cpOf(p);
        if (cpBoost != null) {
            cpBoost.setExtraMaxCpRate(want ? rate : 0.0f);
        }
    }

    private static void drop(Player holder, UUID id, Cover cover, String reason) {

        if (DIAG) {
            LOG.info("[guard] guard field revoked ({}) uuid={} boosted={}", reason, id, cover.boosted);
        }

        COVER_IDS.remove(cover.entityId);
        var server = holder.level().getServer();
        Player p = server == null ? null : server.getPlayerList().getPlayer(id);

        LifeVector.forget(id);
        if (p != null) {
            CPData cpOff = cpOf(p);
            if (cpOff != null) {
                cpOff.setLifeGuarded(false);
            }

            releaseDeadFlag(p);
        } else {
            DEAD_FLAG.remove(id);
        }
        if (!cover.boosted) {
            return;
        }
        cover.boosted = false;
        if (p != null) {
            CPData cpDrop = cpOf(p);
            if (cpDrop != null) {
                cpDrop.setExtraMaxCpRate(0.0f);
            }
        }
    }

    private static void sweep(long tick) {
        COVER.entrySet().removeIf(e -> {
            if (tick - e.getValue().stamp <= STALE_TICKS) {
                return false;
            }
            drop(e.getValue().holder, e.getKey(), e.getValue(), "sweep: entry stale timeout");
            return true;
        });
        long keep = Math.max(200L, cooldownTicks() * 10L);
        LAST_TAKEOVER.entrySet().removeIf(e -> tick - e.getValue() > keep);
    }

    private static boolean containsId(List<Player> list, UUID id) {
        for (Player p : list) {
            if (p.getUUID().equals(id)) {
                return true;
            }
        }
        return false;
    }

    public static Player holderOf(LivingEntity victim) {
        if (COVER.isEmpty() || victim == null || victim.level().isClientSide) {
            return null;
        }
        Cover c = COVER.get(victim.getUUID());
        if (c == null) {
            return null;
        }
        long now = victim.level().getGameTime();

        if (now < c.stamp || now - c.stamp > STALE_TICKS) {
            return null;
        }
        Player h = c.holder;

        if (h.isRemoved() || h.level() != victim.level()) {
            return null;
        }
        return h;
    }

    public static boolean isGuarded(LivingEntity victim) {
        return holderOf(victim) != null;
    }

    public static boolean tryTakeOver(LivingEntity victim, float drop, boolean mustHeal) {
        Player holder = holderOf(victim);
        if (holder == null) {

            if (DIAG) {
                LOG.info("[guard] takeover failed: holderOf is null, victim={}", victim.getName().getString());
            }
            return false;
        }
        long now = victim.level().getGameTime();
        float exp = expOf(holder);

        CPData cp = cpOf(holder);
        if (cp == null) {
            return false;
        }

        float eaten = cn.academy.api.ACImmortal.absorb(victim, drop);
        if (eaten <= 0.0f) {

            if (DIAG) {
                LOG.info("[guard] takeover failed: CP exhausted. Could have absorbed {} | current CP={}/{}",
                        drop, cp.getCP(), cp.getMaxCP());
            }
            warnNoCp(holder, now);
            return false;
        }

        float need = victim.getMaxHealth() - ACLife.trueLife(victim);
        float granted = 0.0f;
        if (need > 0 && (mustHeal || healReady(victim, now))) {
            float unit = AbilityConfig.stat("dual_wing", "guard_heal_cp", exp);
            float afford = unit <= 0 ? need : Math.min(need, cp.getCP() / unit);
            if (mustHeal) {

                granted = need;
                cp.performWithForce(0, need * unit);
            } else if (afford > 0) {
                granted = afford;
                cp.perform(0, afford * unit);
            }
        }
        if (granted > 0) {
            healed.put(victim.getUUID(), granted);
            LAST_TAKEOVER.put(victim.getUUID(), now);
        } else {
            healed.remove(victim.getUUID());
        }

        ACPierce.noteOwnBlock(victim);
        feedback(victim, granted > 0);
        return true;
    }

    private static boolean healReady(LivingEntity victim, long now) {
        Long last = LAST_TAKEOVER.get(victim.getUUID());
        if (last == null) {
            return true;
        }

        return now < last || now - last >= cooldownTicks();
    }

    private static final Map<UUID, Float> healed = new HashMap<>();

    public static float approvedHeal(LivingEntity victim) {
        Float f = healed.remove(victim.getUUID());
        return f == null ? 0.0f : f;
    }

    private static final Map<UUID, Long> DEAD_FLAG = new HashMap<>();

    private static final int DEAD_FLAG_MAX_TICKS = 600;

    public static boolean hasAnyGuard() {
        return !COVER.isEmpty() || !DEAD_FLAG.isEmpty();
    }

    public static boolean hasAnyDeadFlag() {
        return !DEAD_FLAG.isEmpty();
    }

    public static boolean onFatalBlow(LivingEntity victim, net.minecraft.world.damagesource.DamageSource src) {
        if (victim.level().isClientSide) {
            return false;
        }

        boolean guarded = isGuarded(victim);
        if (victim instanceof Player) {
            diag(victim, "die() HEAD reached: guarded=" + guarded + " src=" + srcName(src));
        }
        if (!guarded) {
            return false;
        }

        if (!tryTakeOver(victim, victim.getMaxHealth(), true)) {

            releaseDeadFlag(victim);
            diag(victim, "fatal-blow REFUSED (cp?) src=" + srcName(src));
            return false;
        }
        approvedHeal(victim);

        String unblockedVia = cn.academy.util.FeCompat.unblock(victim);
        if (unblockedVia != null) {
            diag(victim, "suppression neutralized via " + unblockedVia
                    + " (getHealth now " + victim.getHealth() + ")");
        }

        float max = victim.getMaxHealth();
        if (ACLife.trueLife(victim) < max - 0.01f) {
            ACLife.forceWriteLife(victim, max);
        }
        DEAD_FLAG.put(victim.getUUID(), victim.level().getGameTime());

        syncTakenOver(victim, true);

        if (victim.getHealth() <= 0.0f) {
            warnReadingSuppressed(victim, src);
        }

        if (victim instanceof net.minecraft.server.level.ServerPlayer takenOver) {
            cn.academy.api.ProtectBackends.onTakeOver(takenOver);
        }

        if (cn.academy.api.ProtectBackends.preventsRemoval()) {
            diag(victim, "fatal-blow TAKEN src=" + srcName(src) + " -> hp=" + victim.getHealth()
                    + " deadFlag=set | shell swap fallback delegated to backend "
                    + cn.academy.api.ProtectBackends.get().id());
            return true;
        }
        scheduleRebuild(victim);
        diag(victim, "fatal-blow TAKEN src=" + srcName(src) + " -> hp=" + victim.getHealth()
                + " deadFlag=set");
        return true;
    }

    private static void scheduleRebuild(LivingEntity victim) {
        if (!(victim instanceof net.minecraft.server.level.ServerPlayer sp)) {
            return;
        }
        net.minecraft.server.MinecraftServer server = sp.getServer();
        if (server == null) {
            return;
        }
        if (!PENDING_REBUILD.add(sp.getUUID())) {
            return;
        }

        cn.academy.util.ACRespawn.snapshotInventory(sp);

        cn.academy.util.ACRespawn.markPending(sp.getUUID(), sp.level().getGameTime() + 100L);
        server.execute(() -> {
            PENDING_REBUILD.remove(sp.getUUID());
            try {

                boolean shellGone = sp.isRemoved();
                boolean readingDead = sp.getHealth() <= 0.0f;
                if (!shellGone && !readingDead) {
                    diag(sp, "rebuild skipped: state is fine, hp=" + sp.getHealth());
                    return;
                }

                diag(sp, "rebuild BEGIN: shellGone=" + shellGone + " readingDead=" + readingDead
                        + " hp=" + sp.getHealth() + " trueLife=" + ACLife.trueLife(sp));
                net.minecraft.server.level.ServerPlayer fresh =
                        cn.academy.util.ACRespawn.rebuildInPlace(sp, sp.getMaxHealth());
                if (fresh != null) {
                    diag(fresh, "rebuild DONE: new instance hp=" + fresh.getHealth()
                            + " removed=" + fresh.isRemoved());
                } else {
                    diag(sp, "rebuild FAILED: rebuildInPlace returned null (server/level unavailable)");
                }
            } finally {

                cn.academy.util.ACRespawn.clearPending(sp.getUUID());
            }
        });
    }

    private static final java.util.Set<java.util.UUID> PENDING_REBUILD =
            new java.util.HashSet<>();

    private static final Map<UUID, Long> impossibleWarn = new HashMap<>();

    private static void warnReadingSuppressed(LivingEntity victim,
                                             net.minecraft.world.damagesource.DamageSource src) {
        long now = victim.level().getGameTime();
        Long last = impossibleWarn.get(victim.getUUID());
        if (last != null && now - last < 100) {
            return;
        }
        impossibleWarn.put(victim.getUUID(), now);

        float drift = ACLife.lifeDrift(victim);
        boolean outrise = ACLife.canOutriseSuppression(victim);
        LOG.warn("[guard] health reading of {} is suppressed: trueLife {} / reading {} / max {} / drift {} -- {}; {}"
                        + " (damage source {}). Guard field took over as usual. "
                        + "Neither un-suppress path (value lookup / reflective hardcode) worked -- "
                        + "whether the player can still act depends on the client side gates (isImmobile / death screen / container GUI).",
                victim.getName().getString(),
                ACLife.trueLife(victim), victim.getHealth(), victim.getMaxHealth(), drift,
                ACLife.isLifeReadingSuppressed(victim)
                        ? "trueLife was written, the reading is suppressed (something subtracts on the health read path)"
                        : "trueLife itself was never written, this is NOT a reading problem and needs a separate check",
                outrise
                        ? "suppression is below max health, writing full trueLife should push the reading back above zero"
                        : "suppression exceeds max health, the reading stays negative even at full trueLife -- only gates A to E can hold",
                srcName(src));
    }

    public static void releaseDeadFlag(LivingEntity victim) {
        if (DEAD_FLAG.remove(victim.getUUID()) == null) {
            return;
        }
        releaseBackend(victim);
        syncTakenOver(victim, false);
        if (victim instanceof cn.academy.mixin.LivingDeadAccessor acc) {
            acc.academy$setDeadFlag(false);
        }
        diag(victim, "deadFlag RELEASED (guarded=" + isGuarded(victim)
                + " trueLife=" + ACLife.trueLife(victim) + ")");
    }

    private static CPData cpOf(Player p) {
        if (p == null || !cn.lambdalib2.datapart.EntityData.isReady(p)) {
            return null;
        }
        return CPData.get(p);
    }

    private static int cpCP(Player p) {
        CPData cp = cpOf(p);
        return cp == null ? -1 : (int) cp.getCP();
    }

    private static void syncTakenOver(LivingEntity victim, boolean value) {
        if (!(victim instanceof Player p) || !cn.lambdalib2.datapart.EntityData.isReady(p)) {
            return;
        }
        CPData cp = CPData.get(p);
        if (cp != null) {
            cp.setLifeTakenOver(value);
        }
    }

    public static boolean ownsDeadFlag(LivingEntity victim) {
        return !DEAD_FLAG.isEmpty() && DEAD_FLAG.containsKey(victim.getUUID());
    }

    private static final boolean BLOCK_REMOVAL = false;

    public static boolean shouldBlockRemoval(net.minecraft.world.entity.Entity e) {
        if (!BLOCK_REMOVAL) {
            return false;
        }

        if (!(e instanceof net.minecraft.server.level.ServerPlayer sp)) {
            return false;
        }

        if (cn.academy.util.ACRespawn.isRebuilding()) {
            return false;
        }
        if (!ownsDeadFlag(sp)) {
            return false;
        }
        diag(sp, "shell wipe intercepted (remove cancelled, no instance rebuild needed)");
        return true;
    }

    public static boolean shouldReleaseDeadFlag(LivingEntity victim) {
        Long since = DEAD_FLAG.get(victim.getUUID());
        if (since == null) {
            return false;
        }

        if (!cn.academy.api.ACImmortal.isImmortal(victim)) {
            return true;
        }
        return victim.level().getGameTime() - since >= DEAD_FLAG_MAX_TICKS;
    }

    public static void clearDeadFlag(LivingEntity victim) {
        DEAD_FLAG.remove(victim.getUUID());
        releaseBackend(victim);
        diag(victim, "deadFlag CLEARED (hp back to " + victim.getHealth() + ")");
    }

    private static void releaseBackend(LivingEntity victim) {
        if (victim instanceof net.minecraft.server.level.ServerPlayer sp) {
            cn.academy.api.ProtectBackends.onRelease(sp);
        }
    }

    public static boolean tryHeartbeatRevive(LivingEntity victim) {

        if (victim.level().isClientSide || !cn.academy.api.ACImmortal.isImmortal(victim)) {
            return false;
        }

        if (ACLife.trueLife(victim) > 0.0f) {
            return false;
        }

        cn.academy.util.FeCompat.unblock(victim);
        ACLife.forceWriteLife(victim, victim.getMaxHealth());

        if (victim.getHealth() <= 0.0f) {
            warnReadingSuppressed(victim, null);
        }
        diag(victim, "HEARTBEAT revive OK: hp<=0 with guard on -> " + victim.getHealth()
                + " (someone bypassed setHealth)");
        feedback(victim, true);
        return true;
    }

    public static boolean blockDeathTick(LivingEntity victim) {

        return !victim.level().isClientSide
                && cn.academy.api.ACImmortal.covers(victim)
                && cn.academy.api.ACImmortal.isImmortal(victim);
    }

    private static final Map<UUID, Long> IMMORTAL_FX = new HashMap<>();

    private static final int IMMORTAL_FX_THROTTLE = 20;

    public static void noteImmortalHit(LivingEntity victim) {
        if (victim == null || victim.level().isClientSide) {
            return;
        }
        long now = victim.level().getGameTime();
        Long last = IMMORTAL_FX.get(victim.getUUID());

        if (last != null && now >= last && now - last < IMMORTAL_FX_THROTTLE) {
            return;
        }
        if (IMMORTAL_FX.size() > 128) {
            IMMORTAL_FX.clear();
        }
        IMMORTAL_FX.put(victim.getUUID(), now);
        playRevivalFx(victim);
    }

    public static boolean onForeignDie(LivingEntity victim) {
        if (victim == null || victim.level() == null || victim.level().isClientSide) {
            return false;
        }
        if (!cn.academy.api.ACImmortal.isImmortal(victim)) {
            return false;
        }
        if (!onFatalBlow(victim, victim.level().damageSources().generic())) {
            return false;
        }

        if (victim instanceof cn.academy.mixin.LivingDeadAccessor acc) {
            acc.academy$setDeadFlag(true);
        }
        diag(victim, "foreign die() INTERCEPTED (agent hook)");
        return true;
    }

    public static boolean DIAG = false;

    private static final org.apache.logging.log4j.Logger LOG =
            org.apache.logging.log4j.LogManager.getLogger("AcademyCraft/WhiteWingGuard");
    private static final Map<UUID, Long> diagThrottle = new HashMap<>();

    public static void diagField(Player holder, boolean guarding, int covered, int boosted) {
        if (!DIAG) {
            return;
        }
        long now = holder.level().getGameTime();
        Long last = diagThrottle.get(holder.getUUID());
        if (last != null && now - last < 40) {
            return;
        }
        diagThrottle.put(holder.getUUID(), now);
        LOG.info("[guard-field] t={} holder={} guarding={} covered={} boosted={} coverTable={} cp={}",
                now, holder.getName().getString(), guarding, covered, boosted,
                COVER.size(), cpCP(holder));
    }

    static void diag(LivingEntity victim, String msg) {
        if (!DIAG) {
            return;
        }
        long now = victim.level().getGameTime();
        Long last = diagThrottle.get(victim.getUUID());
        if (msg.startsWith("HEARTBEAT") && last != null && now - last < 20) {
            return;
        }
        diagThrottle.put(victim.getUUID(), now);
        Player h = holderOf(victim);
        LOG.info("[guard] t={} {} : {} | holder={} cp={} realHp={} maxHp={} deathTime={} guarded={}",
                now, victim.getName().getString(), msg,
                h == null ? "none" : h.getName().getString(),
                cpCP(h),
                victim.getHealth(), victim.getMaxHealth(), victim.deathTime, isGuarded(victim));
    }

    private static String srcName(net.minecraft.world.damagesource.DamageSource src) {
        return src == null ? "null" : src.getMsgId() + "/" + src.type().msgId()
                + (src.getEntity() == null ? " (no attacker)" : " by " + src.getEntity().getName().getString());
    }

    static void playRevivalFx(LivingEntity victim) {
        feedback(victim, true);
    }

    private static void feedback(LivingEntity victim, boolean revived) {
        victim.level().playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                ACSounds.VM_VEC_REFLECTION.get(), SoundSource.PLAYERS, 0.5f, 1.0f);

        spawnGuardWave(victim);
        if (revived) {
            spawnRevivalFeathers(victim);
        }
    }

    private static final double GUARD_FX_SIZE = 1.6;
    private static final double GUARD_FX_SCALE = 2.0;
    private static final int GUARD_FX_SPARKS = 20;

    private static void spawnGuardWave(LivingEntity victim) {
        if (!(victim.level() instanceof net.minecraft.server.level.ServerLevel sl)) {
            return;
        }
        double cx = victim.getX();
        double cy = victim.getY() + victim.getBbHeight() * 0.5;
        double cz = victim.getZ();

        sl.sendParticles(ACParticles.SONIC_WAVE.get(), cx, cy, cz,
                0, 0.0, GUARD_FX_SIZE * GUARD_FX_SCALE, 0.0, 1.0);

        net.minecraft.world.phys.Vec3 n = victim.getLookAngle().scale(-1);
        net.minecraft.world.phys.Vec3 up = Math.abs(n.y) > 0.99
                ? new net.minecraft.world.phys.Vec3(1, 0, 0)
                : new net.minecraft.world.phys.Vec3(0, 1, 0);
        net.minecraft.world.phys.Vec3 u = n.cross(up).normalize();
        net.minecraft.world.phys.Vec3 w = n.cross(u).normalize();
        double phase = victim.getRandom().nextDouble() * Math.PI * 2;
        for (int i = 0; i < GUARD_FX_SPARKS; i++) {
            double a = phase + i * Math.PI * 2 / GUARD_FX_SPARKS;
            net.minecraft.world.phys.Vec3 out = u.scale(Math.cos(a)).add(w.scale(Math.sin(a)));
            net.minecraft.world.phys.Vec3 vel = out.scale(0.35).add(n.scale(0.12));
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                    cx, cy, cz, 0, vel.x, vel.y, vel.z, 1.0);
        }
    }

    private static final int FEATHER_COUNT = 64;

    private static final double FEATHER_RADIUS = 0.9;

    private static final double FEATHER_OMEGA = 0.20;

    private static final double GOLDEN_ANGLE = Math.PI * (3.0 - Math.sqrt(5.0));

    private static void spawnRevivalFeathers(LivingEntity victim) {
        if (!(victim.level() instanceof net.minecraft.server.level.ServerLevel sl)) {
            return;
        }
        double cx = victim.getX();
        double cy = victim.getY() + victim.getBbHeight() * 0.5;
        double cz = victim.getZ();

        for (int i = 0; i < FEATHER_COUNT; i++) {

            double cosPolar = 1.0 - 2.0 * (i + 0.5) / FEATHER_COUNT;
            double sinPolar = Math.sqrt(Math.max(0.0, 1.0 - cosPolar * cosPolar));
            double azimuth = i * GOLDEN_ANGLE;
            double ox = sinPolar * Math.cos(azimuth);
            double oz = sinPolar * Math.sin(azimuth);

            sl.sendParticles(ACParticles.GOLD_FEATHER.get(),
                    cx + ox * FEATHER_RADIUS,
                    cy + cosPolar * FEATHER_RADIUS,
                    cz + oz * FEATHER_RADIUS,
                    0,
                    -oz * FEATHER_OMEGA * FEATHER_RADIUS,
                    0.0,
                    ox * FEATHER_OMEGA * FEATHER_RADIUS,
                    1.0);
        }
    }

    private static void warnNoCp(Player holder, long now) {
        Long last = noCpWarn.get(holder.getUUID());
        if (last != null && now - last < 40) {
            return;
        }
        noCpWarn.put(holder.getUUID(), now);
        holder.displayClientMessage(
                Component.translatable("gui.academy.dual_wing.guard_no_cp"), true);
    }

    private static final Map<UUID, Long> noCpWarn = new HashMap<>();

    private static int cooldownTicks() {
        return (int) AbilityConfig.stat("dual_wing", "guard_cooldown", 0);
    }

    public static float expOf(Player holder) {
        return AbilityData.get(holder).getSkillExp(DualWing.INSTANCE);
    }

    public static CPData cpOfPublic(Player p) {
        return cpOf(p);
    }

    public static boolean hasPassiveSkill(Player p) {
        AbilityData data = AbilityData.get(p);
        if (!data.hasCategory()) {
            return false;
        }
        for (Skill s : data.getLearnedSkillList()) {
            if (s.isPassive()) {
                return true;
            }
        }
        return false;
    }

    public static int coveredCount() {
        return COVER.size();
    }

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    public static void onLivingDeath(
            net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim == null || victim.level().isClientSide) {
            return;
        }

        if (!cn.academy.api.ACImmortal.covers(victim)) {
            return;
        }
        if (onForeignDie(victim)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(
            net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        try {
            Player p = event.getEntity();
            if (p == null || p.level().isClientSide) {
                return;
            }
            float max = p.getMaxHealth();
            float lethal = max - 1.0f;

            float vet = cn.academy.api.Vettore.get(p);
            float cut = cn.academy.api.Vettore.trimToSurvivable(p);
            if (cut > 0.0f) {
                LOG.warn("[immortal] {} logged in carrying {} Vettore (max {}) -- "
                                + "trimmed {} to keep 1 HP, the rest will decay on its own",
                        p.getName().getString(), String.format("%.1f", vet),
                        String.format("%.1f", max), String.format("%.1f", cut));
            }

            cn.academy.api.ACImmortal.dissolveSuppression(p);

        } catch (Throwable t) {
            LOG.error("[immortal] login fallback failed, ignored", t);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(net.minecraftforge.event.server.ServerStoppedEvent event) {
        clear();
    }

    public static void clear() {
        COVER.clear();
        COVER_IDS.clear();
        BOOST_ON.clear();
        IMMORTAL_FX.clear();
        IMMORTAL_TICK_DIAG.clear();

        cn.academy.api.ACImmortal.clearLifeMemo();
        LAST_TAKEOVER.clear();
        healed.clear();
        noCpWarn.clear();
        DEAD_FLAG.clear();
        LifeVector.clear();
        impossibleWarn.clear();
        diagThrottle.clear();
    }
}
