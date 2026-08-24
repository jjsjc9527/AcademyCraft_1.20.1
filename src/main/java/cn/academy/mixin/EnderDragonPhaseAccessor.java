package cn.academy.mixin;

import net.minecraft.world.entity.boss.enderdragon.phases.DragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(EnderDragonPhase.class)
public interface EnderDragonPhaseAccessor {

    @Invoker("create")
    static <T extends DragonPhaseInstance> EnderDragonPhase<T> academy$create(
            Class<T> instanceClass, String name) {
        throw new AssertionError("mixin was not applied");
    }
}
