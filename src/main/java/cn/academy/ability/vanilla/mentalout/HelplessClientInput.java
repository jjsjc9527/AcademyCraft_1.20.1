package cn.academy.ability.vanilla.mentalout;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@OnlyIn(Dist.CLIENT)
public final class HelplessClientInput {

    private HelplessClientInput() {}

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new HelplessClientInput());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onHelplessInput(MovementInputUpdateEvent event) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null || event.getEntity() != p || !Helpless.isHelpless(p)) {
            return;
        }
        event.getInput().jumping = false;
        event.getInput().shiftKeyDown = false;
    }
}
