package cn.lambdalib2.util;

import cn.lambdalib2.auxgui.AuxGuiHandler;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientUtils {

    private ClientUtils() {}

    public static boolean isPlayerInGame() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.screen == null && !AuxGuiHandler.hasForegroundGui();
    }

    public static boolean isInWorld() {
        return Minecraft.getInstance().player != null;
    }

    public static String getClipboardContent() {
        return Minecraft.getInstance().keyboardHandler.getClipboard();
    }

    public static void setClipboardContent(String content) {
        Minecraft.getInstance().keyboardHandler.setClipboard(content);
    }
}
