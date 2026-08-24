package cn.academy.client.auxgui;

import cn.lambdalib2.auxgui.AuxGuiHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class TerminalInstallClient {

    private TerminalInstallClient() {}

    public static void play() {
        AuxGuiHandler.register(new TerminalInstallEffect());
    }
}
