package cn.academy.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface LivingLifeAccessor {

    @Accessor("DATA_HEALTH_ID")
    static EntityDataAccessor<Float> academy$dataLifeId() {
        throw new AssertionError("mixin was not applied");
    }
}
