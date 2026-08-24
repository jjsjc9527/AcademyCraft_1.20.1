package cn.lambdalib2.input;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class KeyHandler {

    public void onKeyDown() {}

    public void onKeyUp() {}

    public void onKeyAbort() {}

    public void onKeyTick() {}

    protected Minecraft getMC() {
        return Minecraft.getInstance();
    }

    protected Player getPlayer() {
        return getMC().player;
    }

}
