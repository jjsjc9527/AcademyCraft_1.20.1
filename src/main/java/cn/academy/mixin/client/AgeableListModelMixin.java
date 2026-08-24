package cn.academy.mixin.client;

import cn.academy.client.render.DualWingLimbs;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.HumanoidModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AgeableListModel.class)
public abstract class AgeableListModelMixin {

    private final boolean[] academy$hidden = new boolean[4];

    @Inject(
        method = "renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;"
               + "Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V",
        at = @At("HEAD")
    )
    private void academy$hideArmorLimbs(PoseStack ps, VertexConsumer vc, int light, int overlay,
                                        float r, float g, float b, float a, CallbackInfo ci) {
        if (!DualWingLimbs.frameActive() || !((Object) this instanceof HumanoidArmorModel)) {
            return;
        }
        HumanoidModel<?> self = (HumanoidModel<?>) (Object) this;
        academy$hidden[0] = self.rightArm.visible;
        academy$hidden[1] = self.leftArm.visible;
        academy$hidden[2] = self.rightLeg.visible;
        academy$hidden[3] = self.leftLeg.visible;
        self.rightArm.visible = false;
        self.leftArm.visible = false;
        self.rightLeg.visible = false;
        self.leftLeg.visible = false;
    }

    @Inject(
        method = "renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;"
               + "Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V",
        at = @At("TAIL")
    )
    private void academy$drawArmorLimbs(PoseStack ps, VertexConsumer vc, int light, int overlay,
                                        float r, float g, float b, float a, CallbackInfo ci) {
        if (!DualWingLimbs.frameActive() || !((Object) this instanceof HumanoidArmorModel)) {
            return;
        }
        HumanoidModel<?> self = (HumanoidModel<?>) (Object) this;

        self.rightArm.visible = academy$hidden[0];
        self.leftArm.visible = academy$hidden[1];
        self.rightLeg.visible = academy$hidden[2];
        self.leftLeg.visible = academy$hidden[3];

        DualWingLimbs.renderArmor(ps, vc, light, overlay, self, DualWingLimbs.isInnerArmor());
    }
}
