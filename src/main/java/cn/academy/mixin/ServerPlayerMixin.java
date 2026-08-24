package cn.academy.mixin;

import cn.academy.ability.vanilla.vecmanip.advanced.WhiteWingGuard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    @Inject(method = "die(Lnet/minecraft/world/damagesource/DamageSource;)V",
            at = @At("HEAD"), cancellable = true)
    private void academy$guardFatalBlow(DamageSource src, CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        if (!WhiteWingGuard.onFatalBlow(self, src)) {
            return;
        }
        ((LivingDeadAccessor) self).academy$setDeadFlag(true);
        ci.cancel();
    }

    @Inject(method = "doTick()V", at = @At("HEAD"))
    private void academy$holdLifeBeforeSync(CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        if (cn.academy.api.ACImmortal.covers(self)) {
            cn.academy.api.ACImmortal.holdLife(self);
        }
    }

    @Redirect(method = "doTick()V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;getHealth()F"),
            require = 0, expect = 5)
    private float academy$syncTrueLifeToClient(ServerPlayer self) {
        if (cn.academy.util.ACLife.isGuardedFakeDeath(self)) {
            return cn.academy.util.ACLife.trueLife(self);
        }
        return self.getHealth();
    }
}
