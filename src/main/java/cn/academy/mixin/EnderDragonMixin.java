package cn.academy.mixin;

import cn.academy.ability.vanilla.mentalout.DazeState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(EnderDragon.class)
public abstract class EnderDragonMixin {

    @ModifyVariable(
        method = "getLatencyPos(IF)[D",
        at = @At("HEAD"),
        ordinal = 0,
        argsOnly = true
    )
    private float academy$dazeLatencyPos(float partialTick) {
        Entity self = (Entity) (Object) this;

        boolean frozen = DazeState.renderFrozen(self)
                || ((net.minecraft.world.entity.boss.enderdragon.EnderDragon) self).isNoAi();
        return frozen ? 1.0F : partialTick;
    }
}
