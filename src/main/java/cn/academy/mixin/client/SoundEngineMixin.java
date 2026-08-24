package cn.academy.mixin.client;

import cn.academy.ability.vanilla.mentalout.DazeState;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {

    @Inject(method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)V",
            at = @At("HEAD"), cancellable = true)
    private void academy$dazeMuteNewSounds(SoundInstance sound, CallbackInfo ci) {
        if (academy$dazeMuted(sound)) {
            ci.cancel();
        }
    }

    @Inject(method = "playDelayed(Lnet/minecraft/client/resources/sounds/SoundInstance;I)V",
            at = @At("HEAD"), cancellable = true)
    private void academy$dazeMuteDelayed(SoundInstance sound, int delay, CallbackInfo ci) {
        if (academy$dazeMuted(sound)) {
            ci.cancel();
        }
    }

    @Inject(method = "tickNonPaused()V", at = @At("HEAD"), cancellable = true)
    private void academy$dazeFreezeSoundTick(CallbackInfo ci) {
        if (DazeState.isLocalPlayerDazed()) {
            ci.cancel();
        }
    }

    @org.spongepowered.asm.mixin.Unique
    private static boolean academy$dazeMuted(SoundInstance sound) {
        return sound != null
                && sound.getSource() != SoundSource.MASTER
                && DazeState.isLocalPlayerDazed();
    }
}
