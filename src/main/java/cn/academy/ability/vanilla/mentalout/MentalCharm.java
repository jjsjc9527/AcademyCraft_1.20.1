package cn.academy.ability.vanilla.mentalout;

import cn.academy.config.AbilityConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.UUID;
import cn.academy.util.ACDefense;

public final class MentalCharm {

    private MentalCharm() {}

    private static final String OWNER = "mo_charm_owner";
    private static final String TICKS = "mo_charm_ticks";
    private static final String TOTAL = "mo_charm_total";
    private static final String HOSTILE = "mo_charm_hostile";
    private static final String HIT_CD = "mo_charm_hitcd";
    private static final String HIT_AT = "mo_charm_hitat";
    private static final String BEAT = "mo_charm_beat";
    private static final String NOTED = "mo_charm_noted";

    private static final int TAKEOVER_GRACE = 60;

    private static final int BEAT_STALE = 20;

    private static final int HIT_FRESH = 40;

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new CharmEvents());
    }

    public static int getTicks(Entity e) {
        return e.getPersistentData().getInt(TICKS);
    }

    public static boolean isActive(Entity e) {
        return getTicks(e) > 0;
    }

    public static boolean isHostileFlip(Entity e) {
        return e.getPersistentData().getBoolean(HOSTILE);
    }

    @Nullable
    public static Player getOwner(Entity e) {
        CompoundTag d = e.getPersistentData();
        if (!d.contains(OWNER)) {
            return null;
        }
        try {
            return e.level().getPlayerByUUID(UUID.fromString(d.getString(OWNER)));
        } catch (IllegalArgumentException bad) {
            return null;
        }
    }

    public static boolean isHostileTo(LivingEntity target, Player caster) {
        return target instanceof Enemy
                || (target instanceof Mob m && m.getTarget() == caster);
    }

    public static boolean apply(Player caster, Mob target, int ticks) {
        boolean toFriendly = isHostileTo(target, caster);

        CompoundTag d = target.getPersistentData();
        d.putString(OWNER, caster.getUUID().toString());
        d.putInt(TICKS, ticks);
        d.putInt(TOTAL, ticks);
        d.putBoolean(HOSTILE, !toFriendly);
        d.putInt(HIT_CD, 0);
        d.putInt(HIT_AT, 0);
        d.putInt(BEAT, 0);
        d.putInt(NOTED, 0);

        if (toFriendly) {
            deaggroFrom(target, caster);
            detachGoal(target);
        } else {
            aggroOnto(target, caster);
            ensureGoal(target);
        }
        return toFriendly;
    }

    public static void deaggroFrom(Mob mob, LivingEntity owner) {

        if (mob.getTarget() == owner) {
            mob.setTarget(null);
        }
        Brain<?> brain = mob.getBrain();

        if (brain.isMemoryValue(MemoryModuleType.ATTACK_TARGET, owner)) {
            brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
        }

        if (brain.isMemoryValue(MemoryModuleType.ANGRY_AT, owner.getUUID())) {
            brain.eraseMemory(MemoryModuleType.ANGRY_AT);
        }

        if (mob instanceof NeutralMob n && owner.getUUID().equals(n.getPersistentAngerTarget())) {
            n.stopBeingAngry();
        }

        if (mob instanceof Warden w) {
            w.clearAnger(owner);
        }
    }

    private static void driveHostile(Mob mob, Player owner) {
        CompoundTag d = mob.getPersistentData();

        if (ControlState.isControlled(mob)) {
            detachGoal(mob);
            d.putInt(TOTAL, Math.max(0, d.getInt(TOTAL) - 1));
            return;
        }

        aggroOnto(mob, owner);

        int elapsed = d.getInt(TOTAL) - d.getInt(TICKS);
        boolean graceOver = elapsed >= TAKEOVER_GRACE;
        boolean everHit = d.getInt(HIT_AT) > 0;

        if (needsOwnAttack(mob) || (graceOver && !everHit)) {
            if (findGoal(mob) == null) {
                note(mob, 1, "MentalCharm: %s dealt no damage to the caster for %d ticks after turning hostile, attack taken over"
                        + " (it may ignore the vanilla targeting system or use no melee goal)");
            }
            ensureGoalForced(mob);

            int beat = d.getInt(BEAT);
            if (graceOver && (beat == 0 || mob.tickCount - beat > BEAT_STALE)) {
                note(mob, 2, "MentalCharm: goalSelector of %s appears not to be ticked (goal attached but idle for %d ticks), "
                        + "fell back to direct resolution, no chasing");
                mob.getLookControl().setLookAt(owner, 30.0f, 30.0f);
                tryHit(mob, owner);
            }
        } else {
            ensureGoal(mob);
        }
    }

    private static void note(Mob mob, int bit, String fmt) {
        CompoundTag d = mob.getPersistentData();
        int noted = d.getInt(NOTED);
        if ((noted & bit) != 0) {
            return;
        }
        d.putInt(NOTED, noted | bit);
        cn.lambdalib2.util.Debug.debugFormat(fmt,
                net.minecraft.world.entity.EntityType.getKey(mob.getType()),
                bit == 1 ? TAKEOVER_GRACE : BEAT_STALE);
    }

    public static void aggroOnto(Mob mob, LivingEntity owner) {
        if (mob.getTarget() != owner) {
            mob.setTarget(owner);
        }
        Brain<?> brain = mob.getBrain();
        if (!brain.isMemoryValue(MemoryModuleType.ATTACK_TARGET, owner)) {
            brain.setMemory(MemoryModuleType.ATTACK_TARGET, java.util.Optional.of(owner));
        }
        if (mob instanceof Warden w) {
            w.increaseAngerAt(owner, WARDEN_ANGER, false);
        }
    }

    public static boolean isForcedFoe(Mob mob, Entity target) {
        return ControlState.commandedTarget(mob) == target
                || cn.academy.ability.vanilla.mentalout.advanced.CognitionRewrite
                        .isForcedFoe(mob, target);
    }

    public static boolean isCharmedAllyOf(Mob mob, Player p) {
        return (isActive(mob) && !isHostileFlip(mob) && getOwner(mob) == p)
                || cn.academy.ability.vanilla.mentalout.advanced.CognitionRewrite.isAllyOf(mob, p);
    }

    private static final int WARDEN_ANGER = 100;

    public static void clear(Entity e) {
        CompoundTag d = e.getPersistentData();

        boolean wasHostile = d.getBoolean(HOSTILE);
        Player owner = getOwner(e);

        d.remove(OWNER);
        d.remove(TICKS);
        d.remove(TOTAL);
        d.remove(HOSTILE);
        d.remove(HIT_CD);
        d.remove(HIT_AT);
        d.remove(BEAT);
        d.remove(NOTED);

        if (e instanceof Mob m) {
            detachGoal(m);

            if (wasHostile && owner != null && m.getTarget() == owner) {
                m.setTarget(null);
            }
        }
    }

    private static boolean needsOwnAttack(Mob mob) {
        return !mob.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE);
    }

    public static final ResourceKey<DamageType> MIND_CONTROLLED = ResourceKey.create(
            Registries.DAMAGE_TYPE, new ResourceLocation("academy", "mind_controlled"));

    public static DamageSource mindControlledDamage(Mob mob) {
        return new DamageSource(
                mob.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(MIND_CONTROLLED),
                mob);
    }

    private static void ensureGoal(Mob mob) {
        if (needsOwnAttack(mob)) {
            ensureGoalForced(mob);
        }
    }

    private static void ensureGoalForced(Mob mob) {
        if (findGoal(mob) == null) {
            mob.goalSelector.addGoal(0, new CharmAttackGoal(mob));
        }
    }

    private static void tryHit(Mob mob, Player p) {
        CompoundTag d = mob.getPersistentData();
        int cd = d.getInt(HIT_CD);
        if (cd > 0) {
            d.putInt(HIT_CD, cd - 1);
            return;
        }
        int hitAt = d.getInt(HIT_AT);
        if (hitAt > 0 && mob.tickCount - hitAt <= HIT_FRESH) {
            return;
        }
        if (mob.isWithinMeleeAttackRange(p) && mob.hasLineOfSight(p)) {
            d.putInt(HIT_CD, (int) AbilityConfig.stat("impression", "hostile_interval", 0));
            mob.swing(InteractionHand.MAIN_HAND);
            p.hurt(mindControlledDamage(mob),
                    AbilityConfig.stat("impression", "hostile_damage", 0));
        }
    }

    private static void detachGoal(Mob mob) {
        CharmAttackGoal g = findGoal(mob);
        if (g != null) {
            mob.goalSelector.removeGoal(g);
        }
    }

    @Nullable
    private static CharmAttackGoal findGoal(Mob mob) {

        for (WrappedGoal w : mob.goalSelector.getAvailableGoals()) {
            if (w.getGoal() instanceof CharmAttackGoal g) {
                return g;
            }
        }
        return null;
    }

    private static final class CharmAttackGoal extends Goal {

        private final Mob mob;

        CharmAttackGoal(Mob mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private Player owner() {
            return isActive(mob) && isHostileFlip(mob) && !ControlState.isControlled(mob)
                    ? getOwner(mob) : null;
        }

        @Override
        public boolean canUse() {
            Player p = owner();
            return p != null && p.isAlive() && !p.isSpectator();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void stop() {
            mob.getNavigation().stop();
        }

        @Override
        public void tick() {
            Player p = owner();
            if (p == null) {
                return;
            }

            mob.getPersistentData().putInt(BEAT, Math.max(1, mob.tickCount));

            mob.getLookControl().setLookAt(p, 30.0f, 30.0f);

            if (!MobGapLeap.tryLeap(mob, p.position()) && mob.tickCount % 5 == 0) {

                mob.getNavigation().moveTo(p, AbilityConfig.stat("impression", "chase_speed", 0));
            }
            tryHit(mob, p);
        }
    }

    public static class CharmEvents {

        @SubscribeEvent
        public void onChangeTarget(LivingChangeTargetEvent event) {
            LivingEntity ent = event.getEntity();
            if (ent.level().isClientSide || !isActive(ent) || isHostileFlip(ent)) {
                return;
            }
            Player owner = getOwner(ent);
            if (owner != null && event.getNewTarget() == owner) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public void onAttack(LivingAttackEvent event) {
            Entity src = event.getSource().getEntity();
            if (!(src instanceof LivingEntity attacker)
                    || attacker.level().isClientSide
                    || !isActive(attacker) || isHostileFlip(attacker)) {
                return;
            }
            if (event.getEntity() == getOwner(attacker)) {
                ACDefense.block(event);
            }
        }

        @SubscribeEvent
        public void onHurt(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
            Entity src = event.getSource().getEntity();
            if (!(src instanceof Mob mob) || mob.level().isClientSide
                    || !isActive(mob) || !isHostileFlip(mob)
                    || event.getSource().is(MIND_CONTROLLED)) {
                return;
            }
            if (event.getEntity() == getOwner(mob)) {
                mob.getPersistentData().putInt(HIT_AT, Math.max(1, mob.tickCount));
            }
        }

        @SubscribeEvent
        public void onLivingTick(LivingEvent.LivingTickEvent event) {
            LivingEntity ent = event.getEntity();
            if (ent.level().isClientSide) {
                return;
            }
            int t = getTicks(ent);
            if (t <= 0) {
                return;
            }
            ent.getPersistentData().putInt(TICKS, t - 1);
            if (t - 1 <= 0) {
                clear(ent);
                return;
            }

            Player owner = getOwner(ent);
            if (owner == null || !owner.isAlive() || owner.level() != ent.level()) {
                clear(ent);
                return;
            }
            if (!(ent instanceof Mob mob)) {
                return;
            }

            if (isHostileFlip(mob)) {
                driveHostile(mob, owner);
            } else {

                deaggroFrom(mob, owner);
            }
        }

        @SubscribeEvent
        public void onEffectApplicable(
                net.minecraftforge.event.entity.living.MobEffectEvent.Applicable event) {
            if (!(event.getEntity() instanceof Player p) || p.level().isClientSide
                    || event.getEffectInstance().getEffect()
                            != net.minecraft.world.effect.MobEffects.DARKNESS
                    || !AbilityConfig.impressionBlocksWardenDarkness()

                    || FaintState.isApplyingOwnDarkness()) {
                return;
            }

            for (Warden w : p.level().getEntitiesOfClass(
                    Warden.class, p.getBoundingBox().inflate(20.0))) {

                if (isCharmedAllyOf(w, p)) {
                    event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
                    return;
                }
            }
        }
    }
}
