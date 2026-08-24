package cn.academy.network;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;

public final class ClientNetAccess {

    private ClientNetAccess() {}

    public static Level clientLevel() {
        return Minecraft.getInstance().level;
    }
}
