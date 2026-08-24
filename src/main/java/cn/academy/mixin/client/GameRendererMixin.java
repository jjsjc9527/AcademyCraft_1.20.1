package cn.academy.mixin.client;

import cn.academy.gravity.ACGravity;
import cn.academy.gravity.RotationAnimation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Shadow @Final private Camera mainCamera;

    @Inject(
        method = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel(FJLcom/mojang/blaze3d/vertex/PoseStack;)V",
        at = @At("HEAD")
    )
    private void academy$castEnterLevel(float partialTick, long nanos, PoseStack pose, CallbackInfo ci) {
        cn.academy.client.render.AllyCastView.enterLevel();
    }

    @Inject(
        method = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel(FJLcom/mojang/blaze3d/vertex/PoseStack;)V",
        at = @At("RETURN")
    )
    private void academy$castLeaveLevel(float partialTick, long nanos, PoseStack pose, CallbackInfo ci) {
        cn.academy.client.render.AllyCastView.leaveLevel();
    }

    @ModifyArg(
        method = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel(FJLcom/mojang/blaze3d/vertex/PoseStack;)V",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionf;)V", ordinal = 3),
        index = 0
    )
    private Quaternionf academy$camPitch(Quaternionf euler) {
        if (academy$useVanilla()) {
            return euler;
        }

        RotationAnimation anim = academy$anim();
        if (anim != null && anim.isInAnimation()) {
            Minecraft.getInstance().levelRenderer.needsUpdate();
        }
        Quaternionf view = Axis.YP.rotationDegrees(180.0F);
        view.mul(new Quaternionf(this.mainCamera.rotation()).conjugate());
        return view;
    }

    @ModifyArg(
        method = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel(FJLcom/mojang/blaze3d/vertex/PoseStack;)V",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionf;)V", ordinal = 4),
        index = 0
    )
    private Quaternionf academy$camYaw(Quaternionf euler) {
        return academy$useVanilla() ? euler : new Quaternionf();
    }

    private RotationAnimation academy$anim() {
        Entity e = this.mainCamera.getEntity();
        return e == null ? null : ACGravity.getRotationAnimation(e);
    }

    private boolean academy$useVanilla() {
        Entity e = this.mainCamera.getEntity();
        if (e == null) return true;
        if (ACGravity.getGravityDirection(e) != Direction.DOWN) return false;
        RotationAnimation anim = ACGravity.getRotationAnimation(e);
        if (anim == null) return true;
        anim.update(e.level().getGameTime() * 50L + (long) (Minecraft.getInstance().getFrameTime() * 50.0F));
        return !anim.isInAnimation();
    }
}
