package cn.academy.client;

import cn.academy.network.SafeSpotMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "academy", value = Dist.CLIENT)
public final class ClientSafeSpotTracker {

    private ClientSafeSpotTracker() {}

    private static final int MIN_GAP_TICKS = 20;

    private static final double MAX_STEP_SQR = 16.0 * 16.0;

    private static int cooldown;

    private static double prevX = Double.NaN;
    private static double prevY;
    private static double prevZ;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) {
            reset();
            return;
        }

        double x = p.getX();
        double y = p.getY();
        double z = p.getZ();
        boolean jumped = false;
        if (!Double.isNaN(prevX)) {
            double dx = x - prevX;
            double dy = y - prevY;
            double dz = z - prevZ;
            jumped = dx * dx + dy * dy + dz * dz > MAX_STEP_SQR;
        }
        prevX = x;
        prevY = y;
        prevZ = z;

        if (cooldown > 0) {
            cooldown--;
            return;
        }
        if (jumped) {
            return;
        }

        if (!cn.lambdalib2.datapart.EntityData.isReady(p)) {
            return;
        }

        double minY = p.level().getMinBuildHeight();
        double maxY = p.level().getMaxBuildHeight();
        if (y < minY || y > maxY) {
            return;
        }
        cooldown = MIN_GAP_TICKS;
        SafeSpotMessage.send(x, y, z);
    }

    private static void reset() {
        prevX = Double.NaN;
        cooldown = 0;
    }
}
