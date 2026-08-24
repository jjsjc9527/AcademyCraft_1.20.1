package cn.academy.client.gui;

import cn.academy.network.InterfererInfoMessage;
import net.minecraft.client.Minecraft;

public final class InterfererInfoClient {

    private InterfererInfoClient() {}

    public static void accept(InterfererInfoMessage m) {
        if (Minecraft.getInstance().screen instanceof AbilityInterfererScreen screen) {
            screen.onInfo(m);
        }
    }
}
