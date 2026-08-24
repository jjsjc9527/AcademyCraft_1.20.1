package cn.academy.mixin.client;

import cn.academy.ability.vanilla.mentalout.DazeState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Inject(method = "pauseGame(Z)V", at = @At("HEAD"), cancellable = true)
    private void academy$castEatsEscape(boolean pauseOnly, CallbackInfo ci) {
        if (cn.academy.client.render.AllyCastView.isCasting()) {
            cn.academy.client.render.AllyCastView.end(null);
            ci.cancel();
        }
    }

    @Inject(method = "getMainRenderTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;",
            at = @At("HEAD"), cancellable = true)
    private void academy$feedOwnsMainTarget(
            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<
                    com.mojang.blaze3d.pipeline.RenderTarget> cir) {
        com.mojang.blaze3d.pipeline.RenderTarget t =
                cn.academy.client.render.AllyCamFeed.currentTarget();
        if (t != null) {
            cir.setReturnValue(t);
        }
    }

    @Inject(method = "useShaderTransparency()Z", at = @At("HEAD"), cancellable = true)
    private static void academy$feedIgnoresFabulous(
            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        if (cn.academy.client.render.AllyCamFeed.isRendering()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "handleKeybinds()V", at = @At("HEAD"), cancellable = true)
    private void academy$dazeBlockKeybinds(CallbackInfo ci) {
        if (DazeState.isLocalPlayerDazed()
                || cn.academy.ability.vanilla.mentalout.Helpless.isLocalPlayerHelpless()

                || cn.academy.ability.vanilla.mentalout.ProxyClientDrive.beingDriven()) {

            cn.academy.ability.vanilla.mentalout.ProxyClientDrive.passThroughPerspective();
            ci.cancel();
        }
    }

    @Redirect(method = "setScreen(Lnet/minecraft/client/gui/screens/Screen;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;isDeadOrDying()Z"),
            require = 0, expect = 1)
    private boolean academy$guardedNotDyingForScreen(net.minecraft.client.player.LocalPlayer player) {
        boolean dying = player.isDeadOrDying();
        if (!dying) {
            return false;
        }

        if (cn.academy.util.ACLife.serverConfirmedDeath()
                && !cn.academy.util.ACLife.guardTookOver(player)) {
            return true;
        }

        return !cn.academy.util.ACLife.isGuardedFakeDeath(player);
    }

    @Redirect(method = "tick()V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;isDeadOrDying()Z"),
            require = 0, expect = 1)
    private boolean academy$guardedNotDyingInTick(net.minecraft.client.player.LocalPlayer player) {

        if (cn.academy.util.ACLife.prendereVeroVitta(player) > 0.0f
                && cn.academy.util.ACLife.guardCovers(player)) {
            return false;
        }
        return player.isDeadOrDying();
    }

    @Inject(method = "setScreen(Lnet/minecraft/client/gui/screens/Screen;)V",
            at = @At("HEAD"), cancellable = true)
    private void academy$guardedNoDeathScreen(
            net.minecraft.client.gui.screens.Screen screen, CallbackInfo ci) {
        Minecraft mc = (Minecraft) (Object) this;
        if (mc.player == null) {
            return;
        }

        boolean isDeathScreen = screen instanceof DeathScreen;
        boolean deathPath = isDeathScreen
                || (screen == null && mc.screen == null && mc.player.isDeadOrDying());

        if (!deathPath) {
            return;
        }
        if (cn.academy.util.ACLife.serverConfirmedDeath()
                && !cn.academy.util.ACLife.guardTookOver(mc.player)) {

            return;
        }
        if (cn.academy.util.ACLife.isGuardedFakeDeath(mc.player)) {
            ci.cancel();
        }
    }

    @Inject(method = "tick()V", at = @At("RETURN"))
    private void academy$closeStaleDeathScreen(CallbackInfo ci) {
        Minecraft mc = (Minecraft) (Object) this;
        if (!(mc.screen instanceof DeathScreen) || mc.player == null) {
            return;
        }

        if (cn.academy.util.ACLife.serverConfirmedDeath()
                && !cn.academy.util.ACLife.guardTookOver(mc.player)) {
            return;
        }

        if (cn.academy.util.ACLife.prendereVeroVitta(mc.player) > 0.0f
                && cn.academy.util.ACLife.guardCovers(mc.player)) {
            mc.setScreen(null);
        }
    }

}
