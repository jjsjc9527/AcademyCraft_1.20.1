package cn.lambdalib2.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
final class ClientSideAccess {

    private ClientSideAccess() {}

    static Player thePlayer() {
        return Minecraft.getInstance().player;
    }

    static boolean isPlayerInGame() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.level != null;
    }

    static boolean isGamePaused() {
        return Minecraft.getInstance().isPaused();
    }
}
