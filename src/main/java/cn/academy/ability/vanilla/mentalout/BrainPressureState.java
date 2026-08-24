package cn.academy.ability.vanilla.mentalout;

import cn.academy.ability.AbilityContext;
import cn.academy.ability.vanilla.mentalout.advanced.BrainPressure;
import cn.academy.util.ACPierce;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class BrainPressureState {

    public static final int MODE_INCREASE = 0;

    public static final int MODE_DECREASE = 1;

    public static final int MODES = 2;

    private static final String TICKS = "mo_bp_ticks";
    private static final String NEXT = "mo_bp_next";
    private static final String INTERVAL = "mo_bp_interval";
    private static final String DAMAGE = "mo_bp_damage";
    private static final String OWNER = "mo_bp_owner";

    private static final MobEffect[] DEBUFFS = {MobEffects.WEAKNESS, MobEffects.BLINDNESS};

    private static final ParticleOptions HIT_FX = ParticleTypes.BUBBLE_POP;
    private static final int HIT_FX_COUNT = 8;

    private static final double HIT_FX_SPEED = 0.02;

    private BrainPressureState() {}

    private static String key(String base, int mode) {
        return base + mode;
    }

    public static int getTicks(Entity e, int mode) {
        return e == null ? 0 : e.getPersistentData().getInt(key(TICKS, mode));
    }

    public static boolean isPressured(Entity e, int mode) {
        return getTicks(e, mode) > 0;
    }

    public static boolean isPressured(Entity e) {
        for (int m = 0; m < MODES; ++m) {
            if (isPressured(e, m)) {
                return true;
            }
        }
        return false;
    }

    public static void apply(LivingEntity target, int ticks, int interval, float damage,
                             int mode, Player caster) {
        if (target == null || ticks <= 0 || mode < 0 || mode >= MODES) {
            return;
        }
        CompoundTag d = target.getPersistentData();
        d.putInt(key(TICKS, mode), ticks);
        d.putInt(key(INTERVAL, mode), Math.max(1, interval));
        d.putInt(key(NEXT, mode), Math.max(1, interval));
        d.putFloat(key(DAMAGE, mode), Math.max(0f, damage));
        if (caster != null) {
            d.putUUID(key(OWNER, mode), caster.getUUID());
        } else {
            d.remove(key(OWNER, mode));
        }
        if (mode == MODE_INCREASE) {
            applyDebuffs(target, ticks);
        }
    }

    private static void applyDebuffs(LivingEntity target, int ticks) {
        FaintState.applyDarkness(target, ticks);
        for (MobEffect eff : DEBUFFS) {
            target.addEffect(new MobEffectInstance(eff, ticks, 0, false, false));
        }
    }

    public static void tick(Entity entity) {
        if (!(entity instanceof LivingEntity e)) {
            return;
        }
        if (!isPressured(e)) {
            return;
        }
        if (!e.isAlive()) {
            clear(e);
            return;
        }
        boolean client = e.level().isClientSide;
        for (int m = 0; m < MODES; ++m) {
            int ticks = getTicks(e, m);
            if (ticks <= 0) {
                continue;
            }
            CompoundTag d = e.getPersistentData();
            d.putInt(key(TICKS, m), ticks - 1);
            if (client) {
                continue;
            }
            int next = d.getInt(key(NEXT, m)) - 1;
            if (next <= 0) {
                hurtOnce(e, m);
                next = Math.max(1, d.getInt(key(INTERVAL, m)));
            }
            d.putInt(key(NEXT, m), next);

            if (ticks - 1 <= 0) {
                clear(e, m);
            }
        }
    }

    private static void hurtOnce(LivingEntity e, int mode) {
        float dmg = e.getPersistentData().getFloat(key(DAMAGE, mode));
        if (dmg <= 0f) {
            return;
        }
        dmg = AbilityContext.calcSkillDamage(ownerOf(e, mode), BrainPressure.INSTANCE, e, dmg);

        if (ACPierce.hurtOrPierce(e, asphyxiation(e, mode), ACPierce.ASPHYXIATION_PIERCE, dmg)) {
            WideCastFx.atHead(e, HIT_FX, HIT_FX_COUNT, HIT_FX_SPEED);
        }
    }

    private static DamageSource asphyxiation(LivingEntity target, int mode) {
        Holder<DamageType> type = target.level().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(FaintState.ASPHYXIATION);
        Player owner = ownerOf(target, mode);
        return owner != null ? new DamageSource(type, owner) : new DamageSource(type);
    }

    public static Player ownerOf(LivingEntity target, int mode) {
        CompoundTag d = target.getPersistentData();
        String k = key(OWNER, mode);
        return d.hasUUID(k) ? target.level().getPlayerByUUID(d.getUUID(k)) : null;
    }

    public static void clear(LivingEntity e) {
        for (int m = 0; m < MODES; ++m) {
            clear(e, m);
        }
    }

    public static void clear(LivingEntity e, int mode) {
        if (e == null || mode < 0 || mode >= MODES) {
            return;
        }
        int remain = getTicks(e, mode);
        CompoundTag d = e.getPersistentData();
        d.remove(key(TICKS, mode));
        d.remove(key(NEXT, mode));
        d.remove(key(INTERVAL, mode));
        d.remove(key(DAMAGE, mode));
        d.remove(key(OWNER, mode));
        if (mode == MODE_INCREASE && !e.level().isClientSide) {
            removeIfOurs(e, MobEffects.DARKNESS, remain);
            for (MobEffect eff : DEBUFFS) {
                removeIfOurs(e, eff, remain);
            }
        }
    }

    private static void removeIfOurs(LivingEntity e, MobEffect eff, int remain) {
        MobEffectInstance inst = e.getEffect(eff);
        if (inst != null && inst.getDuration() <= remain + 2) {
            e.removeEffect(eff);
        }
    }
}
