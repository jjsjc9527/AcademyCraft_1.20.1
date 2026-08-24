package cn.academy.terminal;

import cn.academy.client.auxgui.TerminalUI;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AppEnvironment {

    public App app;
    public TerminalUI terminal;

    public void onStart() {
    }

    protected App getApp() {
        return app;
    }

    protected TerminalUI getTerminal() {
        return terminal;
    }

    protected Player getPlayer() {
        return Minecraft.getInstance().player;
    }

}
