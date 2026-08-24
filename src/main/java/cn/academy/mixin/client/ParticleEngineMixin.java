package cn.academy.mixin.client;

import cn.academy.ability.vanilla.mentalout.DazeState;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {

    @Inject(method = "add", at = @At("HEAD"), cancellable = true)
    private void academy$dazeNoNewParticles(net.minecraft.client.particle.Particle particle,
                                            CallbackInfo ci) {
        if (DazeState.isLocalPlayerDazed()) {
            ci.cancel();
        }
    }

    @Inject(method = "tick()V", at = @At("HEAD"), cancellable = true)
    private void academy$dazeFreezeParticleTick(CallbackInfo ci) {
        if (DazeState.isLocalPlayerDazed()) {
            ci.cancel();
        }
    }

    @ModifyVariable(
        method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;"
               + "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;"
               + "Lnet/minecraft/client/renderer/LightTexture;"
               + "Lnet/minecraft/client/Camera;F"
               + "Lnet/minecraft/client/renderer/culling/Frustum;)V",
        at = @At("HEAD"), ordinal = 0, argsOnly = true, remap = false
    )
    private float academy$dazeFreezeParticleLerp(float partialTick) {
        return DazeState.isLocalPlayerDazed() ? 1.0F : partialTick;
    }
}
