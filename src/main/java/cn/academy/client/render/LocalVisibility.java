package cn.academy.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class LocalVisibility {

    private LocalVisibility() {}

    public static boolean hiddenFromLocalPlayer(Entity owner) {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && owner.isInvisibleTo(mc.player);
    }
}
