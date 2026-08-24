package cn.academy.terminal.app;

import cn.academy.client.gui.developer.DeveloperUI;
import cn.academy.terminal.App;
import cn.academy.terminal.AppEnvironment;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class AppSkillTree extends App {

    public static final AppSkillTree instance = new AppSkillTree();

    public AppSkillTree() {
        super("skill_tree");
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public AppEnvironment createEnvironment() {
        return new AppEnvironment() {
            @Override
            @OnlyIn(Dist.CLIENT)
            public void onStart() {
                Minecraft.getInstance().setScreen(DeveloperUI.openSkillTreeOnly());
            }
        };
    }

}
