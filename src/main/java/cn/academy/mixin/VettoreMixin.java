package cn.academy.mixin;

import cn.academy.api.Vettore;
import cn.academy.api.VettoreHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LivingEntity.class, priority = 10000)
public abstract class VettoreMixin implements VettoreHolder {

    @Unique
    private float academy$vettoreValue;

    @Override
    public float academy$vettore() {
        return academy$vettoreValue;
    }

    @Override
    public void academy$setVettore(float value) {
        academy$vettoreValue = Math.min(0.0f, value);
    }

    @Inject(method = "getHealth", at = @At("RETURN"), cancellable = true)
    private void academy$applyVettore(CallbackInfoReturnable<Float> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        float delta = academy$vettoreValue;
        if (delta >= 0.0f) {
            return;
        }
        float capped = self.getMaxHealth() + delta;
        float orig = cir.getReturnValueF();
        Vettore.trace(self, orig, capped, delta);
        if (capped < orig) {
            cir.setReturnValue(capped);
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    private void academy$saveVettore(CompoundTag tag, CallbackInfo ci) {
        float d = academy$vettoreValue;
        if (d < 0.0f) {
            tag.putFloat(Vettore.NBT, d);
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void academy$loadVettore(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains(Vettore.NBT)) {
            academy$vettoreValue = Math.min(0.0f, tag.getFloat(Vettore.NBT));
        }
    }
}
