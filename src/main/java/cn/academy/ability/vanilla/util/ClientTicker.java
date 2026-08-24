package cn.academy.ability.vanilla.util;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@OnlyIn(Dist.CLIENT)
public final class ClientTicker {

    public static void run(int ticks, Runnable onTick) {
        new ClientTicker(ticks, onTick);
    }

    private final int maxTick;
    private final Runnable onTick;
    private int tick = 0;
    private boolean dead = false;

    private ClientTicker(int maxTick, Runnable onTick) {
        this.maxTick = maxTick;
        this.onTick = onTick;
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || dead) return;
        if (Minecraft.getInstance().isPaused()) return;

        if (tick++ >= maxTick) {
            dead = true;
            MinecraftForge.EVENT_BUS.unregister(this);
            return;
        }
        onTick.run();
    }
}
