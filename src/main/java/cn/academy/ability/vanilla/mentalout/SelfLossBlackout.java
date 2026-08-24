package cn.academy.ability.vanilla.mentalout;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@OnlyIn(Dist.CLIENT)
public final class SelfLossBlackout {

    private SelfLossBlackout() {}

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new SelfLossBlackout());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRenderGui(RenderGuiEvent.Post event) {
        if (!Helpless.isLocalPlayerBlind()) {
            return;
        }
        GuiGraphics gg = event.getGuiGraphics();
        gg.fill(0, 0, gg.guiWidth(), gg.guiHeight(), BLACKOUT_Z, 0xFF000000);
    }

    private static final int BLACKOUT_Z = 1000;
}
