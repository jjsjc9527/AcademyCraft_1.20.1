package cn.academy.client.gui;

import cn.academy.network.NodeInfoMessage;
import net.minecraft.client.Minecraft;

public final class NodeInfoClient {

    private NodeInfoClient() {}

    public static void accept(NodeInfoMessage m) {
        if (Minecraft.getInstance().screen instanceof WirelessNodeScreen screen) {
            screen.onNodeInfo(m);
        }
    }
}
