package cn.academy.client.gui.developer;

import cn.academy.block.tileentity.DeveloperBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class DeveloperClientOpener {

    private DeveloperClientOpener() {}

    public static void open(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (mc.level.getBlockEntity(pos) instanceof DeveloperBlockEntity be) {
            mc.setScreen(DeveloperUI.open(be));
        }
    }
}
