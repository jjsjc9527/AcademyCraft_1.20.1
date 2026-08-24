package cn.academy.terminal.app.settings;

import cn.academy.terminal.App;
import cn.academy.terminal.AppEnvironment;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class AppSettings extends App {

    public static AppSettings instance = new AppSettings();

    private AppSettings() {
        super("settings");
        setPreInstalled();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public AppEnvironment createEnvironment() {
        return new AppEnvironment() {
            @Override
            @OnlyIn(Dist.CLIENT)
            public void onStart() {
                Minecraft.getInstance().setScreen(new SettingsUI());
            }
        };
    }
}
