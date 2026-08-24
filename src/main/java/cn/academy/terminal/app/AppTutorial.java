package cn.academy.terminal.app;

import cn.academy.client.gui.GuiTutorial;
import cn.academy.terminal.App;
import cn.academy.terminal.AppEnvironment;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class AppTutorial extends App {

    public static AppTutorial instance = new AppTutorial();

    private AppTutorial() {
        super("tutorial");
        setPreInstalled();

        icon = getTexture("icon_1");
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public AppEnvironment createEnvironment() {
        return new AppEnvironment() {
            @Override
            @OnlyIn(Dist.CLIENT)
            public void onStart() {
                Minecraft.getInstance().setScreen(new GuiTutorial());
            }
        };
    }
}
