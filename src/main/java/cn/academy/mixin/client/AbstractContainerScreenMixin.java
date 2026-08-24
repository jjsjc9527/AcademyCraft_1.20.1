package cn.academy.mixin.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {

    @Redirect(method = "tick()V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;isAlive()Z"),
            require = 0, expect = 1)
    private boolean academy$guardedContainerStaysOpen(LocalPlayer self) {
        if (cn.academy.util.ACLife.isGuardedFakeDeath(self)) {
            return cn.academy.util.ACLife.trueLife(self) > 0.0f;
        }
        return self.isAlive();
    }
}
