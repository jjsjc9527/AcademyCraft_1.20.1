package cn.academy.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "academy", value = Dist.CLIENT)
public final class ClientDeathScreenFix {

    private ClientDeathScreenFix() {}

    public static void release() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.isDeadOrDying()) {
            return;
        }
        if (mc.screen instanceof DeathScreen) {
            mc.setScreen(null);
        }
    }

    private static final int FAKE_DEATH_TICKS = 40;

    private static long lastAsk;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) {
            return;
        }

        if (mc.level != null) {
            for (net.minecraft.world.entity.player.Player other : mc.level.players()) {
                if (other == p || other.deathTime <= 0) {
                    continue;
                }
                if (other.getHealth() > 0.0F || cn.academy.util.ACLife.trueLife(other) > 0.0F) {
                    other.deathTime = 0;
                }
            }
        }

        if (mc.screen instanceof DeathScreen && cn.academy.util.ACLife.guardTookOver(p)) {
            mc.setScreen(null);
            return;
        }

        if (p.deathTime > 0
                && (p.getHealth() > 0.0F || cn.academy.util.ACLife.trueLife(p) > 0.0F)) {

            p.deathTime = 0;
            if (mc.screen instanceof DeathScreen) {
                mc.setScreen(null);
            }
            return;
        }

        if (p.getHealth() <= 0.0F
                && p.deathTime > FAKE_DEATH_TICKS
                && !(mc.screen instanceof DeathScreen)) {
            long now = System.currentTimeMillis();
            if (now - lastAsk < 1000L) {
                return;
            }
            lastAsk = now;
            cn.academy.network.FakeDeathResyncMessage.send();
        }
    }
}
