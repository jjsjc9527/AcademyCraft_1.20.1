package cn.academy.mixin.client;

import cn.academy.ability.vanilla.vecmanip.advanced.DualWingAnim;
import cn.academy.ability.vanilla.vecmanip.advanced.DualWingAnimData;
import cn.academy.util.ACPose;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {

    @Redirect(
        method = "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFF)V",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/client/player/AbstractClientPlayer;isInWater()Z"),
        require = 0, expect = 1
    )
    private boolean academy$leanFollowsPitch(AbstractClientPlayer player) {
        return player.isInWater() || ACPose.followsPitch(player);
    }

    @Inject(
        method = "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFF)V",
        at = @At("TAIL")
    )
    private void academy$dualWingRoot(AbstractClientPlayer player, PoseStack poseStack,
                                      float ageInTicks, float rotationYaw, float partialTicks,
                                      CallbackInfo ci) {
        float[] pose = DualWingAnim.pose(player, partialTicks);
        if (pose == null) {
            return;
        }
        int b = DualWingAnimData.B_POSITION;
        float rx = DualWingAnim.rootRotX(pose[DualWingAnim.idx(b, 0)]);
        float ry = DualWingAnim.rootRotY(pose[DualWingAnim.idx(b, 1)]);
        float rz = DualWingAnim.rootRotZ(pose[DualWingAnim.idx(b, 2)]);

        if (rz != 0.0F) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(rz));
        }
        if (ry != 0.0F) {
            poseStack.mulPose(Axis.YP.rotationDegrees(ry));
        }
        if (rx != 0.0F) {
            poseStack.mulPose(Axis.XP.rotationDegrees(rx));
        }
        float fy = DualWingAnim.floatOffset(player, partialTicks);
        if (fy != 0.0F) {
            poseStack.translate(0.0F, fy / 16.0F, 0.0F);
        }
    }

}
