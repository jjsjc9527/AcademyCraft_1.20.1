package cn.academy.mixin;

import cn.academy.ability.vanilla.mentalout.Helpless;
import cn.academy.ability.vanilla.mentalout.MentalCharm;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.warden.Warden;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Warden.class)
public abstract class WardenMixin {

    @Inject(
        method = "canTargetEntity(Lnet/minecraft/world/entity/Entity;)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void academy$forcedControlCanTarget(Entity target, CallbackInfoReturnable<Boolean> cir) {
        Mob self = (Mob) (Object) this;

        if (Helpless.isHelpless(self)) {
            cir.setReturnValue(false);
            return;
        }
        if (target == null) {
            return;
        }
        if (MentalCharm.isForcedFoe(self, target)) {
            cir.setReturnValue(true);
        }
    }

}
