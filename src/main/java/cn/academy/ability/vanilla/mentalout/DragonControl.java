package cn.academy.ability.vanilla.mentalout;

import cn.academy.mixin.EnderDragonPhaseAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.AbstractDragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhaseManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.DragonFireball;
import net.minecraft.world.phys.Vec3;

public final class DragonControl {

    public static EnderDragonPhase<ControlPhase> PHASE;

    public static final float FLY_SPEED = 2.5f;

    private DragonControl() {}

    public static void register() {
        if (PHASE == null) {
            PHASE = EnderDragonPhaseAccessor.academy$create(ControlPhase.class, "AcademyControl");
        }
    }

    public static void steer(EnderDragon dragon, Vec3 goal) {
        if (PHASE == null || isDying(dragon)) {
            return;
        }
        ControlPhase p = dragon.getPhaseManager().getPhase(PHASE);
        p.target = goal;
        dragon.getPhaseManager().setPhase(PHASE);
    }

    public static boolean isDying(EnderDragon dragon) {
        return dragon.isDeadOrDying()
                || dragon.getPhaseManager().getCurrentPhase().getPhase() == EnderDragonPhase.DYING;
    }

    private static final String CY_VICTIM = "mo_fc_dragon_victim";

    private static final String CY_STAGE = "mo_fc_dragon_stage";

    private static final String CY_SINCE = "mo_fc_dragon_since";

    private static final String CY_WARMUP = "mo_fc_dragon_warmup";

    private static final int STAGE_CHARGE = 0;
    private static final int STAGE_BREATH = 1;

    private static final double CHARGE_OVERSHOOT = 12.0;

    private static final double CHARGE_REACH = 120.0;

    private static final double BREATH_ALTITUDE = 10.0;

    private static final double BREATH_RANGE = 64.0;

    private static final int BREATH_WARMUP = 5;

    private static final float BREATH_AIM_DEG = 10.0f;

    public static void attack(EnderDragon dragon, LivingEntity victim) {
        if (PHASE == null || victim == null || isDying(dragon)) {
            return;
        }
        CompoundTag d = dragon.getPersistentData();
        long now = dragon.level().getGameTime();

        if (!d.contains(CY_SINCE) || d.getInt(CY_VICTIM) != victim.getId()) {
            d.putInt(CY_VICTIM, victim.getId());
            beginStage(dragon, victim, STAGE_CHARGE, now);
            return;
        }

        int stage = d.getInt(CY_STAGE);

        if (now - d.getLong(CY_SINCE) >= stageLimit(stage)) {
            beginStage(dragon, victim, stage == STAGE_CHARGE ? STAGE_BREATH : STAGE_CHARGE, now);
            return;
        }

        if (stage == STAGE_CHARGE) {
            chargeStage(dragon, victim, now);
        } else {
            spitStage(dragon, victim, d, now);
        }
    }

    private static void beginStage(EnderDragon dragon, LivingEntity victim, int stage, long now) {
        CompoundTag d = dragon.getPersistentData();
        d.putInt(CY_STAGE, stage);
        d.putLong(CY_SINCE, now);
        d.putInt(CY_WARMUP, 0);

        if (stage == STAGE_CHARGE) {
            EnderDragonPhaseManager pm = dragon.getPhaseManager();

            if (pm.getCurrentPhase().getPhase() == EnderDragonPhase.CHARGING_PLAYER) {
                pm.setPhase(PHASE);
            }
            pm.setPhase(EnderDragonPhase.CHARGING_PLAYER);
            pm.getPhase(EnderDragonPhase.CHARGING_PLAYER).setTarget(chargeAim(dragon, victim));
        }
    }

    private static void chargeStage(EnderDragon dragon, LivingEntity victim, long now) {
        if (dragon.getPhaseManager().getCurrentPhase().getPhase() == EnderDragonPhase.CHARGING_PLAYER) {
            return;
        }
        beginStage(dragon, victim, STAGE_BREATH, now);
    }

    private static Vec3 chargeAim(EnderDragon dragon, LivingEntity victim) {
        Vec3 self = dragon.position();
        Vec3 at = new Vec3(victim.getX(), victim.getY(0.5), victim.getZ());
        Vec3 rel = at.subtract(self);
        double len = rel.length();
        if (len < 1.0e-4) {
            return at;
        }
        Vec3 aim = at.add(rel.scale(CHARGE_OVERSHOOT / len));
        double far = len + CHARGE_OVERSHOOT;
        return far <= CHARGE_REACH ? aim : self.add(aim.subtract(self).scale(CHARGE_REACH / far));
    }

    private static void spitStage(EnderDragon dragon, LivingEntity victim, CompoundTag d, long now) {
        steer(dragon, new Vec3(victim.getX(), victim.getY() + BREATH_ALTITUDE, victim.getZ()));

        int warmup = d.getInt(CY_WARMUP);
        boolean ready = victim.distanceToSqr(dragon) < BREATH_RANGE * BREATH_RANGE
                && dragon.hasLineOfSight(victim);
        if (!ready) {
            d.putInt(CY_WARMUP, Math.max(0, warmup - 1));
            return;
        }
        d.putInt(CY_WARMUP, ++warmup);
        if (warmup < BREATH_WARMUP || !aimedAt(dragon, victim)) {
            return;
        }
        spitBreath(dragon, victim);
        beginStage(dragon, victim, STAGE_CHARGE, now);
    }

    private static boolean aimedAt(EnderDragon dragon, LivingEntity victim) {
        Vec3 toVictim = new Vec3(victim.getX() - dragon.getX(), 0.0,
                victim.getZ() - dragon.getZ()).normalize();
        Vec3 facing = new Vec3(Mth.sin(dragon.getYRot() * ((float) Math.PI / 180f)), 0.0,
                -Mth.cos(dragon.getYRot() * ((float) Math.PI / 180f))).normalize();
        float deg = (float) (Math.acos(facing.dot(toVictim)) * (180.0 / Math.PI)) + 0.5f;
        return deg >= 0.0f && deg < BREATH_AIM_DEG;
    }

    private static void spitBreath(EnderDragon dragon, LivingEntity victim) {
        Vec3 view = dragon.getViewVector(1.0f);
        double x = dragon.head.getX() - view.x;
        double y = dragon.head.getY(0.5) + 0.5;
        double z = dragon.head.getZ() - view.z;
        if (!dragon.isSilent()) {
            dragon.level().levelEvent((Player) null, 1017, dragon.blockPosition(), 0);
        }
        DragonFireball ball = new DragonFireball(dragon.level(), dragon,
                victim.getX() - x, victim.getY(0.5) - y, victim.getZ() - z);
        ball.moveTo(x, y, z, 0.0f, 0.0f);
        dragon.level().addFreshEntity(ball);
    }

    private static int stageLimit(int stage) {
        float t = stage == STAGE_BREATH
                ? cn.academy.config.AbilityConfig.stat("forced_control", "dragon_breath_time", 0f)
                : cn.academy.config.AbilityConfig.stat("forced_control", "dragon_charge_time", 0f);
        return Math.max(1, (int) t);
    }

    public static void restore(EnderDragon dragon) {

        if (PHASE == null || isDying(dragon)) {
            return;
        }
        CompoundTag d = dragon.getPersistentData();
        EnderDragonPhase<?> cur = dragon.getPhaseManager().getCurrentPhase().getPhase();

        boolean oursCharge = cur == EnderDragonPhase.CHARGING_PLAYER
                && d.contains(CY_SINCE) && d.getInt(CY_STAGE) == STAGE_CHARGE;
        if (cur == PHASE || oursCharge) {
            dragon.getPhaseManager().setPhase(EnderDragonPhase.HOLDING_PATTERN);
        }
        d.remove(CY_VICTIM);
        d.remove(CY_STAGE);
        d.remove(CY_SINCE);
        d.remove(CY_WARMUP);
    }

    public static class ControlPhase extends AbstractDragonPhaseInstance {

        Vec3 target;

        public ControlPhase(EnderDragon dragon) {
            super(dragon);
        }

        @Override
        public Vec3 getFlyTargetLocation() {
            return target;
        }

        @Override
        public float getTurnSpeed() {
            return 0.35f;
        }

        @Override
        public float getFlySpeed() {
            return FLY_SPEED;
        }

        @Override
        public EnderDragonPhase<ControlPhase> getPhase() {
            return PHASE;
        }
    }
}
