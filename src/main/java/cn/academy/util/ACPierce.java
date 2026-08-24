package cn.academy.util;

import cn.academy.config.AbilityConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

public final class ACPierce {

    public static final ResourceKey<DamageType> ASPHYXIATION_PIERCE = ResourceKey.create(
            Registries.DAMAGE_TYPE, new ResourceLocation("academy", "asphyxiation_pierce"));

    public static final ResourceKey<DamageType> SKILL_PIERCE = ResourceKey.create(
            Registries.DAMAGE_TYPE, new ResourceLocation("academy", "skill_pierce"));

    public static final ResourceKey<DamageType> VEC_REFLECT_PIERCE = ResourceKey.create(
            Registries.DAMAGE_TYPE, new ResourceLocation("academy", "vec_reflect_pierce"));

    static final float EPS = 1.0e-4f;

    private static LivingEntity armed;
    private static boolean ownBlock;

    private static float seenAmount;

    private static final java.util.Map<java.util.UUID, float[]> KEEP_RATE = new java.util.HashMap<>();

    private static final long KEEP_TTL = 200L;

    private static final int KEEP_MAX = 256;

    private static final boolean DIAG = true;
    private static final long DIAG_EVERY = 20L;
    private static final org.apache.logging.log4j.Logger DIAG_LOG =
            org.apache.logging.log4j.LogManager.getLogger("ACPierce");

    private static final java.util.Map<java.util.UUID, double[]> DIAG_ACC = new java.util.HashMap<>();

    private static boolean diag(LivingEntity target, String path, float damage,
                               float dealt, float seen, float keep, boolean result) {
        if (!DIAG) {
            return result;
        }
        long now = target.level().getGameTime();

        float hp = ACLife.trueLife(target);
        float shown = target.getHealth();
        double[] acc = DIAG_ACC.get(target.getUUID());
        if (acc == null) {
            DIAG_ACC.put(target.getUUID(), new double[]{hp, now, dealt, 1, 0, 0});
            return result;
        }
        acc[2] += dealt;
        acc[3] += 1;
        if (now - acc[1] < DIAG_EVERY) {
            return result;
        }
        double claimed = acc[2];
        double actual = acc[0] - hp;
        double regen = claimed - actual;
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        DIAG_LOG.info(
                "[pierce] {} trueLife={} shown={}{} path={} dmg={} dealt={} seen={} keep={} invulT={} | "
                        + "last {} ticks: {} calls, we removed {}, trueLife dropped {}, delta {} | heal events {} total {} -> {}",
                id, String.format("%.1f", hp), String.format("%.1f", shown),
                Math.abs(hp - shown) > 0.01f ? "reading suppressed" : "",
                path, String.format("%.2f", damage), String.format("%.2f", dealt),
                Float.isNaN(seen) ? "NaN (event not fired)" : String.format("%.2f", seen),
                String.format("%.3f", keep), target.invulnerableTime,
                (long) (now - acc[1]), (int) acc[3],
                String.format("%.1f", claimed), String.format("%.1f", actual),
                String.format("%.1f", regen),
                (int) acc[4], String.format("%.1f", acc[5]), result);
        acc[4] = 0;
        acc[5] = 0;
        acc[0] = hp;
        acc[1] = now;
        acc[2] = 0;
        acc[3] = 0;
        return result;
    }

    private static int diagDrainLeft = 12;

    private static void diagDrain(LivingEntity target, float want, float before, float after) {
        if (!DIAG || diagDrainLeft <= 0) {
            return;
        }
        diagDrainLeft--;

        float rawBefore = Float.NaN;
        float rawAfter = Float.NaN;
        try {
            rawAfter = ACLife.trueLife(target);
        } catch (Throwable ignored) {

        }
        DIAG_LOG.warn("[pierce-drain] {} wanted to remove {} | getHealth: {} -> {} (delta {}) | entityData now {} | maxHP {}",
                ForgeRegistries.ENTITY_TYPES.getKey(target.getType()),
                String.format("%.2f", want),
                String.format("%.2f", before), String.format("%.2f", after),
                String.format("%.2f", before - after),
                Float.isNaN(rawAfter) ? "unreadable" : String.format("%.2f", rawAfter),
                String.format("%.2f", target.getMaxHealth()));
    }

    private static void noteKeepRate(LivingEntity target, float damage, float totalDealt) {
        if (damage <= EPS || totalDealt <= 0.0f) {
            return;
        }
        if (KEEP_RATE.size() > KEEP_MAX) {
            KEEP_RATE.clear();
        }
        float keep = Math.max(0.0f, Math.min(1.0f, totalDealt / damage));
        KEEP_RATE.put(target.getUUID(),
                new float[]{keep, (float) (target.level().getGameTime() & 0xFFFFFF)});
    }

    private static float keepRateOf(LivingEntity target) {
        float[] v = KEEP_RATE.get(target.getUUID());
        if (v == null) {
            return 0.0f;
        }
        long now = target.level().getGameTime() & 0xFFFFFF;
        long age = (now - (long) v[1]) & 0xFFFFFF;
        if (age > KEEP_TTL) {
            KEEP_RATE.remove(target.getUUID());
            return 0.0f;
        }
        return v[0];
    }

    public static void noteOwnBlock(LivingEntity victim) {
        if (armed != null && victim == armed) {
            ownBlock = true;
        }
    }

    private ACPierce() {}

    public static boolean hurtOrPierce(LivingEntity target, DamageSource normal,
                                       ResourceKey<DamageType> pierceKey, float damage) {
        if (target == null || damage <= 0) {
            return false;
        }

        net.minecraft.world.entity.player.Player abyss =
                cn.academy.ability.vanilla.vecmanip.advanced.AbyssStride.takeoverBy(normal, target);
        if (abyss != null) {
            Boolean took = cn.academy.api.DamageBackends.strike(abyss, target, normal, damage);
            if (took != null) {
                return took;
            }
        }

        boolean skip = exempt(target);

        boolean inCooldown = target.invulnerableTime > 10
                && !normal.is(net.minecraft.tags.DamageTypeTags.BYPASSES_COOLDOWN);
        boolean shielded = target.isDamageSourceBlocked(normal);
        float beforeHp = target.getHealth();
        float beforeAbs = target.getAbsorptionAmount();

        LivingEntity prevArmed = armed;
        boolean prevOwn = ownBlock;
        float prevSeen = seenAmount;
        boolean own;
        float seen;
        armed = target;
        ownBlock = false;
        seenAmount = Float.NaN;
        try {
            target.hurt(normal, damage);
        } finally {
            own = ownBlock;
            seen = seenAmount;
            armed = prevArmed;
            ownBlock = prevOwn;
            seenAmount = prevSeen;
        }

        float dealt = (beforeHp - target.getHealth())
                + (beforeAbs - target.getAbsorptionAmount());
        if (dealt > EPS) {

            float extra = capShortfall(target, damage, dealt, seen);
            if (extra > 0.0f) {
                drain(target, normal, extra);
            }

            noteKeepRate(target, damage, dealt + extra);
            return diag(target, "1 hit", damage, dealt + extra, seen, keepRateOf(target), true);
        }

        if (skip) {
            return diag(target, "x exempt (creative / dead / vanilla invulnerable)", damage, 0, seen, 0, false);
        }
        if (own) {
            return diag(target, "x blocked by us", damage, 0, seen, 0, false);
        }
        if (inCooldown) {
            return diag(target, "x vanilla invulnerability frame", damage, 0, seen, 0, false);
        }
        if (shielded) {
            return diag(target, "x shield", damage, 0, seen, 0, false);
        }
        if (!AbilityConfig.pierceEnabled()) {
            return diag(target, "x switch is off", damage, 0, seen, 0, false);
        }
        if (blacklisted(target)) {
            return diag(target, "x blacklisted", damage, 0, seen, 0, false);
        }

        pierce(target, pierceKey, damage);
        if (target.getHealth() < beforeHp - EPS
                || target.getAbsorptionAmount() < beforeAbs - EPS) {
            return diag(target, "2 bypass follow-up hit", damage,
                    (beforeHp - target.getHealth()) + (beforeAbs - target.getAbsorptionAmount()),
                    seen, keepRateOf(target), true);
        }

        if (frozenByUs(target)) {
            boolean r = drainFrozen(target, normal, damage);
            return diag(target, "3 frozen health removal", damage, 0, seen, 0, r);
        }

        if (Float.isNaN(seen) && GoetyCompat.clearAllInvul(target)) {
            float hp2 = target.getHealth();
            float abs2 = target.getAbsorptionAmount();
            target.hurt(normal, damage);
            float again = (hp2 - target.getHealth()) + (abs2 - target.getAbsorptionAmount());
            if (again > EPS) {

                float extra2 = capShortfall(target, damage, again, seen);
                if (extra2 > 0.0f) {
                    drain(target, normal, extra2);
                }
                noteKeepRate(target, damage, again + extra2);
                return diag(target, "3.5 hit after clearing cooldown", damage, again + extra2, seen,
                        keepRateOf(target), true);
            }

            int mi = GoetyCompat.moddedInvulOf(target);
            int oi = GoetyCompat.obsidianInvulOf(target);
            boolean sp = GoetyCompat.settingUpSecond(target);
            if (mi <= 0 && oi <= 0 && !sp) {
                GoetyCompat.dumpOnce(target);
            }

            return diag(target, "x still missed after clearing cooldown (moddedInvul=" + mi
                    + " obelisk immunity=" + oi + " phase two cutscene=" + sp
                    + " post hitCooldown=" + GoetyCompat.hitCooldownOf(target) + ")",
                    damage, 0, seen, keepRateOf(target), false);
        }

        if (Float.isNaN(seen)) {
            if (!AbilityConfig.pierceBreakIFrame()) {
                return diag(target, "x invulnerability piercing switch is off", damage, 0, seen, 0, false);
            }
            float keep = keepRateOf(target);
            if (keep <= 0.0f) {

                return diag(target, "x no retention rate (never actually hit)", damage, 0, seen, keep, false);
            }
            float amount = damage * keep;
            if (amount > EPS) {
                drain(target, normal, amount);
                return diag(target, "4 invulnerability pierced", damage, amount, seen, keep, true);
            }
            return diag(target, "x computed retention rate is too small", damage, amount, seen, keep, false);
        }

        return diag(target, "x event fired but health did not drop (damage zeroed)", damage, 0, seen, keepRateOf(target), false);
    }

    private static boolean blacklisted(LivingEntity target) {
        List<? extends String> list = AbilityConfig.pierceBlacklist();
        if (list == null || list.isEmpty()) {
            return false;
        }
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        if (id == null) {
            return false;
        }
        String full = id.toString();
        String anyOfMod = id.getNamespace() + ":*";
        for (String e : list) {
            if (full.equalsIgnoreCase(e) || anyOfMod.equalsIgnoreCase(e)) {
                return true;
            }
        }
        return false;
    }

    private static boolean exempt(LivingEntity target) {
        return !target.isAlive()
                || target.isRemoved()
                || target.isInvulnerable()
                || (target instanceof Player p && p.getAbilities().invulnerable);
    }

    private static float capShortfall(LivingEntity target, float damage, float dealt, float seen) {
        if (!AbilityConfig.pierceBypassCap() || !AbilityConfig.pierceEnabled()) {
            return 0.0f;
        }

        if (Float.isNaN(seen)) {
            return 0.0f;
        }
        float shaved = damage - seen;
        if (shaved <= EPS) {
            return 0.0f;
        }
        if (blacklisted(target) || exempt(target)) {
            return 0.0f;
        }

        if (seen <= EPS) {
            return 0.0f;
        }
        float keep = Math.max(0.0f, Math.min(1.0f, dealt / seen));
        float extra = shaved * keep;
        return extra > EPS ? extra : 0.0f;
    }

    private static void drain(LivingEntity target, DamageSource normal, float amount) {

        float hp = target.getHealth();
        if (hp <= 0.0f || amount <= 0.0f) {
            return;
        }
        float now = Math.max(0.0f, hp - amount);
        target.setHealth(now);
        diagDrain(target, amount, hp, target.getHealth());
        if (now <= 0.0f) {
            target.die(normal);
        }
    }

    public static final class Events {

        @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
        public void onLivingAttack(LivingAttackEvent event) {
            if (armed != null && event.getEntity() == armed) {
                seenAmount = event.getAmount();
            }
        }

        @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
        public void diagHeal(net.minecraftforge.event.entity.living.LivingHealEvent event) {
            if (!DIAG) {
                return;
            }
            double[] acc = DIAG_ACC.get(event.getEntity().getUUID());
            if (acc == null || acc.length < 6) {
                return;
            }
            acc[4] += 1;
            acc[5] += event.getAmount();
            if (!DIAG_HEAL_STACK) {
                return;
            }
            DIAG_HEAL_STACK = false;
            StringBuilder sb = new StringBuilder();
            for (StackTraceElement s : Thread.currentThread().getStackTrace()) {
                String cn = s.getClassName();
                if (cn.startsWith("java.") || cn.startsWith("jdk.")) {
                    continue;
                }
                sb.append("\n    at ").append(cn).append('.').append(s.getMethodName());
            }
            DIAG_LOG.warn("[pierce-heal] {} healed {} -- stack: {}",
                    ForgeRegistries.ENTITY_TYPES.getKey(event.getEntity().getType()),
                    String.format("%.2f", event.getAmount()), sb);
        }
    }

    private static volatile boolean DIAG_HEAL_STACK = true;

    private static boolean frozenByUs(LivingEntity t) {
        return cn.academy.ability.vanilla.mentalout.DazeState.isDazed(t)
                || cn.academy.ability.vanilla.mentalout.FaintState.isFainted(t)
                || cn.academy.ability.vanilla.mentalout.SelfLossState.isActive(t);
    }

    private static boolean drainFrozen(LivingEntity target, DamageSource normal, float damage) {
        if (target.getHealth() <= 0.0f) {
            return false;
        }
        drain(target, normal, damage);
        return true;
    }

    private static boolean pierce(LivingEntity target, ResourceKey<DamageType> key, float damage) {
        Holder<DamageType> type = target.level().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key);
        return target.hurt(new DamageSource(type), damage);
    }

}
