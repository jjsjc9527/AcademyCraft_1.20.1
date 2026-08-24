package cn.academy.mixin.client;

import cn.academy.ability.vanilla.mentalout.DazeState;
import cn.academy.client.render.AllyCamFeed;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Shadow
    private AtomicBoolean needsFrustumUpdate;

    @Shadow
    private void applyFrustum(Frustum frustum) {
        throw new AssertionError();
    }

    @Inject(method = "setupRender", at = @At("HEAD"), cancellable = true)
    private void academy$feedBorrowsChunkGraph(Camera camera, Frustum frustum,
                                               boolean hasCapturedFrustum, boolean isSpectator,
                                               CallbackInfo ci) {
        if (!AllyCamFeed.isRendering()) {
            return;
        }
        applyFrustum(new Frustum(frustum).offsetToFullyIncludeCameraCube(8));
        needsFrustumUpdate.set(true);
        ci.cancel();
    }

    @Inject(method = "shouldShowEntityOutlines()Z", at = @At("HEAD"), cancellable = true)
    private void academy$feedSkipsOutline(CallbackInfoReturnable<Boolean> cir) {
        if (AllyCamFeed.isRendering()) {
            cir.setReturnValue(false);
        }
    }

    @Redirect(
        method = "renderLevel",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;isDetached()Z"),
        require = 0, expect = 1
    )
    private boolean academy$castShowsBody(Camera camera) {
        if (camera.isDetached()) {
            return true;
        }

        return cn.academy.client.render.AllyCastView.isCastMainView()
                && camera.getEntity() instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon;
    }

    @ModifyVariable(method = "renderEntity", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private float academy$dazeFreezeEntityLerp(float partialTick) {
        return DazeState.isLocalPlayerDazed() ? 1.0F : partialTick;
    }

    @Inject(method = "renderEntity", at = @At("HEAD"), cancellable = true)
    private void academy$dazeHideNewEntities(Entity entity, double camX, double camY, double camZ,
                                             float partialTick, PoseStack poseStack,
                                             MultiBufferSource buffers, CallbackInfo ci) {
        if (DazeState.hiddenFromDazedVision(entity)) {
            ci.cancel();
        }
    }

    @Inject(method = "compileChunks", at = @At("HEAD"), cancellable = true)
    private void academy$dazeFreezeChunkRebuild(net.minecraft.client.Camera camera, CallbackInfo ci) {
        if (DazeState.isLocalPlayerDazed()) {
            ci.cancel();
        }
    }

    @Inject(method = "tick()V", at = @At("HEAD"), cancellable = true)
    private void academy$dazeFreezeWorldBackdrop(CallbackInfo ci) {
        if (DazeState.isLocalPlayerDazed()) {
            ci.cancel();
        }
    }
}
