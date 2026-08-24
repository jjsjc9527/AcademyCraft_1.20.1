package cn.academy.client.gui.developer;

import cn.academy.ability.develop.PortableDevData;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class DeveloperPortableOpener {

    private DeveloperPortableOpener() {}

    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        mc.setScreen(DeveloperUI.openPortable(PortableDevData.get(mc.player)));
    }
}
