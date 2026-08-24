package cn.academy.mixin.client;

import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin {

    @Inject(
        method = "renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;"
               + "Lnet/minecraft/client/renderer/MultiBufferSource;"
               + "Lnet/minecraft/world/entity/LivingEntity;"
               + "Lnet/minecraft/world/entity/EquipmentSlot;I"
               + "Lnet/minecraft/client/model/HumanoidModel;)V",
        at = @At("HEAD")
    )
    private void academy$recordSlot(com.mojang.blaze3d.vertex.PoseStack ps,
                                    net.minecraft.client.renderer.MultiBufferSource buffers,
                                    net.minecraft.world.entity.LivingEntity entity,
                                    EquipmentSlot slot, int light,
                                    net.minecraft.client.model.HumanoidModel model,
                                    CallbackInfo ci) {

        cn.academy.client.render.DualWingLimbs.setInnerArmor(slot == EquipmentSlot.LEGS);
    }
}
