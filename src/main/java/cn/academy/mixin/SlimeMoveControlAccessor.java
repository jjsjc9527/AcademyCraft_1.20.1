package cn.academy.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(targets = "net.minecraft.world.entity.monster.Slime$SlimeMoveControl")
public interface SlimeMoveControlAccessor {

    @Invoker("setDirection")
    void academy$setDirection(float yRot, boolean aggressive);

    @Invoker("setWantedMovement")
    void academy$setWantedMovement(double speed);
}
