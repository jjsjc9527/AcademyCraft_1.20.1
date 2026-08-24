package cn.academy.mixin.client;

import cn.academy.ability.vanilla.mentalout.DazeState;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {

    @Inject(
        method = "tickEntities()V",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/client/multiplayer/ClientLevel;tickBlockEntities()V"),
        cancellable = true
    )
    private void academy$dazeBlockEntities(CallbackInfo ci) {
        if (DazeState.isLocalPlayerDazed()) {
            ci.cancel();
        }
    }
}
