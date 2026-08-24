package cn.academy.mixin;

import cn.academy.gravity.ACGravity;
import cn.academy.gravity.RotationUtil;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Player.class, priority = 1001)
public abstract class PlayerMixin {

    @Shadow @Final private Abilities abilities;
    @Shadow protected abstract boolean isStayingOnGroundSurface();

    @Inject(
        method = "remove(Lnet/minecraft/world/entity/Entity$RemovalReason;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void academy$abnormalKillGuard(Entity.RemovalReason reason, CallbackInfo ci) {
        if (reason != Entity.RemovalReason.KILLED
                || !(((Object) this) instanceof net.minecraft.server.level.ServerPlayer p)) {
            return;
        }
        if (p.getHealth() <= 0.0F || p.isRemoved()) {
            return;
        }

        ci.cancel();
        p.hurt(p.damageSources().genericKill(), Float.MAX_VALUE);
    }

    @Inject(method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At("RETURN"))
    private void academy$notePlayerHurtRefused(net.minecraft.world.damagesource.DamageSource src,
                                               float amount,
                                               CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.FALSE.equals(cir.getReturnValue())) {
            cn.academy.util.ACDefense.noteHurtRefused((Player) (Object) this);
        }
    }

    @Redirect(method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;isDeadOrDying()Z"),
            require = 0, expect = 1)
    private boolean academy$playerGuardedCanStillBeHurt(Player self) {
        if (cn.academy.util.ACLife.isGuardedFakeDeath(self)) {
            return cn.academy.util.ACLife.trueLife(self) <= 0.0f;
        }
        return self.isDeadOrDying();
    }

    @Redirect(method = "actuallyHurt(Lnet/minecraft/world/damagesource/DamageSource;F)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;getHealth()F"),
            require = 0, expect = 1)
    private float academy$playerGuardedHurtsFromTrueLife(Player self) {
        if (cn.academy.util.ACLife.isGuardedFakeDeath(self)) {
            return cn.academy.util.ACLife.trueLife(self);
        }
        return self.getHealth();
    }

    @Inject(
        method = "maybeBackOffFromEdge(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/entity/MoverType;)Lnet/minecraft/world/phys/Vec3;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void academy$maybeBackOffFromEdge(Vec3 movement, MoverType type,
                                              CallbackInfoReturnable<Vec3> cir) {
        Entity self = (Entity) (Object) this;
        Direction g = ACGravity.getGravityDirection(self);
        if (g == Direction.DOWN) return;

        Vec3 local = RotationUtil.vecWorldToPlayer(movement, g);

        if (this.abilities.flying || local.y > 0.0D
                || !(type == MoverType.SELF || type == MoverType.PLAYER)
                || !this.isStayingOnGroundSurface() || !academy$isAboveGround(self, g)) {
            cir.setReturnValue(movement);
            return;
        }

        float step = self.maxUpStep();
        double d = local.x;
        double e = local.z;

        while (d != 0.0D && self.level().noCollision(self,
                self.getBoundingBox().move(RotationUtil.vecPlayerToWorld(d, -step, 0.0D, g)))) {
            if (d < 0.05D && d >= -0.05D) d = 0.0D;
            else if (d > 0.0D) d -= 0.05D;
            else d += 0.05D;
        }

        while (e != 0.0D && self.level().noCollision(self,
                self.getBoundingBox().move(RotationUtil.vecPlayerToWorld(0.0D, -step, e, g)))) {
            if (e < 0.05D && e >= -0.05D) e = 0.0D;
            else if (e > 0.0D) e -= 0.05D;
            else e += 0.05D;
        }

        while (d != 0.0D && e != 0.0D && self.level().noCollision(self,
                self.getBoundingBox().move(RotationUtil.vecPlayerToWorld(d, -step, e, g)))) {
            if (d < 0.05D && d >= -0.05D) d = 0.0D;
            else if (d > 0.0D) d -= 0.05D;
            else d += 0.05D;
            if (e < 0.05D && e >= -0.05D) e = 0.0D;
            else if (e > 0.0D) e -= 0.05D;
            else e += 0.05D;
        }

        cir.setReturnValue(RotationUtil.vecPlayerToWorld(d, local.y, e, g));
    }

    private boolean academy$isAboveGround(Entity self, Direction g) {
        if (self.onGround()) {
            return true;
        }
        float step = self.maxUpStep();
        if (!(self.fallDistance < step)) {
            return false;
        }
        Vec3 w = RotationUtil.vecPlayerToWorld(0.0D, self.fallDistance - step, 0.0D, g);
        return !self.level().noCollision(self, self.getBoundingBox().move(w.x, w.y, w.z));
    }
}
