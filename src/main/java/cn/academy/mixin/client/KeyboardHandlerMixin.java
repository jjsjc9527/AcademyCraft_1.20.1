package cn.academy.mixin.client;

import cn.academy.ability.vanilla.mentalout.Helpless;
import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {

    private static final int ACADEMY$KEY_F1 = 290;

    @Inject(method = "keyPress(JIIII)V", at = @At("HEAD"), cancellable = true)
    private void academy$blockHideGuiWhenHelpless(long window, int key, int scancode,
                                                  int action, int mods, CallbackInfo ci) {
        if (key == ACADEMY$KEY_F1 && Helpless.isLocalPlayerHelpless()) {
            ci.cancel();
        }
    }
}
