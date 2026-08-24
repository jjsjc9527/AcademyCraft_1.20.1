package cn.academy.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {

    @Inject(method = "handlePlayerCombatKill"
            + "(Lnet/minecraft/network/protocol/game/ClientboundPlayerCombatKillPacket;)V",
            at = @At("HEAD"))
    private void academy$noteServerConfirmedDeath(ClientboundPlayerCombatKillPacket packet,
                                                  CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && packet.getPlayerId() == mc.player.getId()) {
            cn.academy.util.ACLife.noteServerConfirmedDeath();
        }
    }
}
