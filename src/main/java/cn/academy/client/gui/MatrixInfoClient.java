package cn.academy.client.gui;

import cn.academy.network.MatrixInfoMessage;
import net.minecraft.client.Minecraft;

public final class MatrixInfoClient {

    private MatrixInfoClient() {}

    public static void accept(MatrixInfoMessage m) {
        if (Minecraft.getInstance().screen instanceof WirelessMatrixScreen screen) {
            screen.onInfo(m);
        }
    }
}
