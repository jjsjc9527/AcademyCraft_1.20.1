package cn.academy.mixin;

import cn.academy.ability.vanilla.mentalout.BrainPressureState;
import cn.academy.ability.vanilla.mentalout.ControlTick;
import cn.academy.ability.vanilla.mentalout.DazeState;
import cn.academy.ability.vanilla.mentalout.FaintState;
import cn.academy.ability.vanilla.mentalout.Helpless;
import cn.academy.ability.vanilla.mentalout.SelfLossState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(Level.class)
public abstract class LevelMixin {

    @Inject(
        method = "guardEntityTick(Ljava/util/function/Consumer;Lnet/minecraft/world/entity/Entity;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void academy$dazeFreeze(Consumer<Entity> consumer, Entity entity, CallbackInfo ci) {

        boolean skip = ControlTick.tick(entity);

        if (skip) {
            ci.cancel();
        }
    }

    @Inject(
        method = "guardEntityTick(Ljava/util/function/Consumer;Lnet/minecraft/world/entity/Entity;)V",
        at = @At("RETURN")
    )
    private void academy$dazeUntick(Consumer<Entity> consumer, Entity entity, CallbackInfo ci) {

        ControlTick.untick(entity);
    }

}
