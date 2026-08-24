package cn.academy.ability.vanilla.mentalout;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@OnlyIn(Dist.CLIENT)
public final class FaintCameraTilt {

    private FaintCameraTilt() {}

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new FaintCameraTilt());
    }

    @SubscribeEvent
    public void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        net.minecraft.client.player.LocalPlayer p = net.minecraft.client.Minecraft.getInstance().player;
        if (p == null || !FaintState.isFainted(p) || !FaintState.liesViaGravity(p)) {
            return;
        }

        if (cn.academy.gravity.ACGravity.getGravityDirection(p) == net.minecraft.core.Direction.DOWN) {
            return;
        }
        float roll = FaintState.rollOf(p);
        if (roll != 0f) {
            event.setRoll(event.getRoll() + roll);
        }
    }
}
