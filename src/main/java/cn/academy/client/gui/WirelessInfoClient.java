package cn.academy.client.gui;

import cn.academy.network.WirelessInfoMessage;
import net.minecraft.client.Minecraft;

public final class WirelessInfoClient {

    private WirelessInfoClient() {}

    public static void accept(WirelessInfoMessage m) {
        var screen = Minecraft.getInstance().screen;
        if (screen instanceof TechUIContainerScreen<?> s) {
            s.onWirelessInfo(m);
        } else if (screen instanceof cn.academy.client.gui.developer.DeveloperUI.DeveloperScreen s) {
            s.onWirelessInfo(m);
        }
    }
}
