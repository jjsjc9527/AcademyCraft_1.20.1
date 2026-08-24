package cn.academy.mixin;

import cn.academy.gravity.ACGravity;
import cn.academy.gravity.RotationUtil;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Shadow protected abstract void updateWalkAnimation(float p_268283_);

    @Shadow protected boolean dead;

    @Inject(
        method = "Lnet/minecraft/world/entity/LivingEntity;calculateEntityAnimation(Z)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void academy$calculateEntityAnimation(boolean includeHeight, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        Direction g = ACGravity.getGravityDirection(self);
        if (g == Direction.DOWN) return;
        ci.cancel();
        Vec3 worldDelta = new Vec3(self.getX() - self.xo, self.getY() - self.yo, self.getZ() - self.zo);
        Vec3 localDelta = RotationUtil.vecWorldToPlayer(worldDelta, g);
        float f = (float) Mth.length(localDelta.x, includeHeight ? localDelta.y : 0.0D, localDelta.z);
        this.updateWalkAnimation(f);
    }

    @Redirect(
        method = "Lnet/minecraft/world/entity/LivingEntity;tick()V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;atan2(DD)D", ordinal = 0),
        require = 0, expect = 1
    )
    private double academy$bodyRotAtan2(double worldDz, double worldDx) {
        Entity self = (Entity) (Object) this;
        Direction g = ACGravity.getGravityDirection(self);
        if (g == Direction.DOWN) return Mth.atan2(worldDz, worldDx);
        Vec3 world = new Vec3(self.getX() - self.xo, self.getY() - self.yo, self.getZ() - self.zo);
        Vec3 local = RotationUtil.vecWorldToPlayer(world, g);
        return Mth.atan2(local.z, local.x);
    }

    @ModifyConstant(
        method = "Lnet/minecraft/world/entity/LivingEntity;tick()V",
        constant = @Constant(floatValue = 0.0025000002F)
    )
    private float academy$bodyTurnGate(float original) {
        Entity self = (Entity) (Object) this;
        Direction g = ACGravity.getGravityDirection(self);
        if (g == Direction.DOWN) return original;
        Vec3 world = new Vec3(self.getX() - self.xo, self.getY() - self.yo, self.getZ() - self.zo);
        Vec3 local = RotationUtil.vecWorldToPlayer(world, g);
        double fLocal = local.x * local.x + local.z * local.z;
        return fLocal > original ? -1.0F : Float.MAX_VALUE;
    }

    @ModifyConstant(
        method = "Lnet/minecraft/world/entity/LivingEntity;updateSwimAmount()V",
        constant = @Constant(floatValue = 0.09F)
    )
    private float academy$swimLeanRate(float original) {
        return cn.academy.util.ACPose.leanRate((Entity) (Object) this, original);
    }

    @ModifyVariable(method = "travel(Lnet/minecraft/world/phys/Vec3;)V",
                    at = @At("HEAD"), argsOnly = true)
    private net.minecraft.world.phys.Vec3 academy$proxyDrivesTravel(
            net.minecraft.world.phys.Vec3 input) {
        cn.academy.ability.vanilla.mentalout.ProxyState.Link link =
                cn.academy.ability.vanilla.mentalout.ProxyState
                        .linkDrivingMob((Entity) (Object) this);
        if (link == null) {
            return input;
        }
        net.minecraft.world.entity.LivingEntity self =
                (net.minecraft.world.entity.LivingEntity) (Object) this;
        if (self.getAttributes().hasAttribute(
                net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)) {

            self.setSpeed((float) self.getAttributeValue(
                    net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED));
        }
        return new net.minecraft.world.phys.Vec3(link.strafe, input.y, link.forward);
    }

    @Inject(method = "getFlyingSpeed()F", at = @At("HEAD"), cancellable = true)
    private void academy$proxyAirControl(CallbackInfoReturnable<Float> cir) {
        if (cn.academy.ability.vanilla.mentalout.ProxyState
                .linkDrivingMob((Entity) (Object) this) == null) {
            return;
        }
        cir.setReturnValue(((net.minecraft.world.entity.LivingEntity) (Object) this)
                .getSpeed() * 0.1f);
    }

    @Inject(method = "isImmobile()Z", at = @At("HEAD"), cancellable = true)
    private void academy$faintImmobile(CallbackInfoReturnable<Boolean> cir) {

        if (cn.academy.ability.vanilla.mentalout.ProxyState.isProxied((Entity) (Object) this)) {
            cir.setReturnValue(false);
            return;
        }
        if (cn.academy.ability.vanilla.mentalout.Helpless.isHelpless((Entity) (Object) this)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "setLastHurtByMob(Lnet/minecraft/world/entity/LivingEntity;)V",
            at = @At("HEAD"), cancellable = true)
    private void academy$helplessUnaware(net.minecraft.world.entity.LivingEntity attacker,
                                         CallbackInfo ci) {
        if (attacker != null
                && cn.academy.ability.vanilla.mentalout.Helpless.isHelpless((Entity) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At("RETURN"))
    private void academy$noteHurtRefused(net.minecraft.world.damagesource.DamageSource src,
                                         float amount, CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.FALSE.equals(cir.getReturnValue())) {
            cn.academy.util.ACDefense.noteHurtRefused((LivingEntity) (Object) this);
        }
    }

    @ModifyVariable(method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
                    at = @At("HEAD"), argsOnly = true)
    private float academy$immortalEatDamage(float amount) {
        if (!(amount > 0.0f)) {
            return amount;
        }
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide) {
            return amount;
        }

        if (!cn.academy.api.ACImmortal.covers(self)) {
            return amount;
        }

        float eaten = cn.academy.api.ACImmortal.absorb(self, amount);
        if (eaten <= 0.0f) {
            return amount;
        }
        return Math.max(0.0f, amount - eaten);
    }

    @Unique
    private net.minecraft.world.phys.Vec3 academy$motionBeforeHurt;

    @Inject(method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At("HEAD"))
    private void academy$rememberMotion(net.minecraft.world.damagesource.DamageSource src,
                                        float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide || !cn.academy.api.ACImmortal.covers(self)) {
            academy$motionBeforeHurt = null;
            return;
        }

        if (cn.academy.ability.vanilla.vecmanip.advanced.DualWing.isFlyingNow(self)) {
            academy$motionBeforeHurt = null;
            return;
        }

        academy$motionBeforeHurt = cn.academy.api.ACImmortal.isImmortal(self)
                ? self.getDeltaMovement() : null;
    }

    @Inject(method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At("RETURN"))
    private void academy$restoreMotion(net.minecraft.world.damagesource.DamageSource src,
                                       float amount, CallbackInfoReturnable<Boolean> cir) {
        net.minecraft.world.phys.Vec3 before = academy$motionBeforeHurt;
        academy$motionBeforeHurt = null;
        if (before == null) {
            return;
        }
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.getDeltaMovement().distanceToSqr(before) < 1.0e-6) {
            return;
        }
        self.setDeltaMovement(before);

        if (!cn.academy.ability.vanilla.vecmanip.advanced.DualWing.isFlyingNow(self)) {
            self.hurtMarked = true;
        }
    }

    @Inject(method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
            at = @At("RETURN"), cancellable = true)
    private void academy$immortalHurtAccepted(net.minecraft.world.damagesource.DamageSource src,
                                              float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!Boolean.FALSE.equals(cir.getReturnValue())) {
            return;
        }
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide || !cn.academy.api.ACImmortal.covers(self)) {
            return;
        }
        if (cn.academy.api.ACImmortal.isImmortal(self)) {
            cir.setReturnValue(true);
        }
    }

    @Unique
    private static final org.apache.logging.log4j.Logger ACADEMY$DRAIN_LOG =
            org.apache.logging.log4j.LogManager.getLogger("AcademyCraft/AbnormalDrain");

    @Unique
    private static final java.util.Map<java.util.UUID, Long> ACADEMY$DRAIN_AT =
            new java.util.concurrent.ConcurrentHashMap<>();

    @Unique
    private static void academy$noteAbnormalDrain(LivingEntity victim, float cur) {

        java.util.UUID id = victim.getUUID();
        long now = System.currentTimeMillis();
        Long last = ACADEMY$DRAIN_AT.get(id);
        if (last != null && now - last < 1000L) {
            return;
        }
        if (ACADEMY$DRAIN_AT.size() > 128) {
            ACADEMY$DRAIN_AT.clear();
        }
        ACADEMY$DRAIN_AT.put(id, now);
        ACADEMY$DRAIN_LOG.warn("[{}] blocked a health wipe that bypassed the damage pipeline: {} trueLife={} was forced to <=0.",
                victim.level().isClientSide ? "client" : "server",
                victim.getName().getString(), String.format("%.1f", cur));
    }

    @Unique
    private static boolean academy$viaDamagePipeline() {
        return StackWalker.getInstance().walk(s -> s.limit(32).anyMatch(f -> {
            String m = f.getMethodName();
            return "m_6475_".equals(m) || "actuallyHurt".equals(m)
                    || "m_7378_".equals(m) || "readAdditionalSaveData".equals(m);
        }));
    }

    @Inject(method = "setHealth(F)V", at = @At("HEAD"), cancellable = true)
    private void academy$whiteWingGuard(float newHealth, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;

        float trueCur = cn.academy.util.ACLife.trueLife(self);
        if (newHealth <= 0.0f && trueCur > 0.0f
                && self instanceof net.minecraft.world.entity.player.Player) {
            if (!academy$viaDamagePipeline()) {
                academy$noteAbnormalDrain(self, trueCur);
                ci.cancel();
                return;
            }

        }

        if (self.level().isClientSide) {
            return;
        }

        float cur = cn.academy.util.ACLife.trueLife(self);
        if (newHealth >= cur) {
            return;
        }

        if (!cn.academy.ability.vanilla.vecmanip.advanced.WhiteWingGuard.isGuarded(self)) {

            cn.academy.api.ACImmortal.noteLeak(self, cur - Math.max(0.0f, newHealth),
                    "the guard field does not cover this player (not in the coverage map)");
            return;
        }
        float drop = cur - Math.max(0.0f, newHealth);
        if (!cn.academy.ability.vanilla.vecmanip.advanced.WhiteWingGuard
                .tryTakeOver(self, drop, false)) {

            cn.academy.api.ACImmortal.noteLeak(self, drop, "tryTakeOver refused (CP exhausted or holder unavailable)");
            return;
        }
        ci.cancel();

        float heal = cn.academy.ability.vanilla.vecmanip.advanced.WhiteWingGuard.approvedHeal(self);
        if (heal > 0.0f) {
            self.setHealth(cur + heal);
        }
    }

    @Inject(method = "die(Lnet/minecraft/world/damagesource/DamageSource;)V",
            at = @At("HEAD"), cancellable = true)
    private void academy$guardFatalBlow(net.minecraft.world.damagesource.DamageSource src,
                                        CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide) {
            return;
        }
        if (!cn.academy.ability.vanilla.vecmanip.advanced.WhiteWingGuard.onFatalBlow(self, src)) {
            return;
        }
        this.dead = true;
        ci.cancel();
    }

    @Inject(method = "tick()V", at = @At("HEAD"))
    private void academy$guardHeartbeat(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide) {
            return;
        }

        if (!cn.academy.api.ACImmortal.covers(self)
                && !cn.academy.ability.vanilla.vecmanip.advanced.WhiteWingGuard.hasAnyDeadFlag()) {
            return;
        }

        boolean ours = cn.academy.ability.vanilla.vecmanip.advanced.WhiteWingGuard
                .ownsDeadFlag(self);

        if (cn.academy.ability.vanilla.vecmanip.advanced.WhiteWingGuard
                .tryHeartbeatRevive(self)) {

            self.deathTime = 0;
            if (self.getPose() == net.minecraft.world.entity.Pose.DYING) {
                self.setPose(net.minecraft.world.entity.Pose.STANDING);
            }
        }

        if (ours && cn.academy.ability.vanilla.vecmanip.advanced.WhiteWingGuard
                .shouldReleaseDeadFlag(self)) {
            this.dead = false;
            cn.academy.ability.vanilla.vecmanip.advanced.WhiteWingGuard.clearDeadFlag(self);
        }
    }

    @Inject(method = "tickDeath()V", at = @At("HEAD"), cancellable = true)
    private void academy$guardBlockDeathTick(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (cn.academy.ability.vanilla.vecmanip.advanced.WhiteWingGuard.blockDeathTick(self)) {
            ci.cancel();
        }
    }

    @Inject(method = "isImmobile()Z", at = @At("RETURN"), cancellable = true)
    private void academy$guardedStillMoves(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) {
            return;
        }
        if (cn.academy.util.ACLife.isGuardedFakeDeath((LivingEntity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    @Redirect(method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;isDeadOrDying()Z",
                    ordinal = 0),
            require = 0, expect = 1)
    private boolean academy$guardedCanStillBeHurt(LivingEntity self) {
        if (cn.academy.util.ACLife.isGuardedFakeDeath(self)) {
            return cn.academy.util.ACLife.trueLife(self) <= 0.0f;
        }
        return self.isDeadOrDying();
    }

    @Redirect(method = "heal(F)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;getHealth()F"),
            require = 0, expect = 1)
    private float academy$guardedHealsFromTrueLife(LivingEntity self) {
        if (cn.academy.util.ACLife.isGuardedFakeDeath(self)) {
            return cn.academy.util.ACLife.trueLife(self);
        }
        return self.getHealth();
    }

    @Redirect(method = "actuallyHurt(Lnet/minecraft/world/damagesource/DamageSource;F)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;getHealth()F"),
            require = 0, expect = 1)
    private float academy$guardedHurtsFromTrueLife(LivingEntity self) {
        if (cn.academy.util.ACLife.isGuardedFakeDeath(self)) {
            return cn.academy.util.ACLife.trueLife(self);
        }
        return self.getHealth();
    }

    @Redirect(method = "addAdditionalSaveData(Lnet/minecraft/nbt/CompoundTag;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;getHealth()F"),
            require = 0, expect = 1)
    private float academy$saveTrueLife(LivingEntity self) {
        if (cn.academy.util.ACLife.lifeSuppressed(self)) {
            return cn.academy.util.ACLife.trueLife(self);
        }
        return self.getHealth();
    }
}
