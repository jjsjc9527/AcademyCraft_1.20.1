package cn.academy.client.render.util;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class HandRenderOverride {

    public interface IHandRenderer {
        void applyTransform(PoseStack ps, float partialTicks);
    }

    private static IHandRenderer current = null;

    private HandRenderOverride() {}

    public static void addInterrupt(IHandRenderer r) {
        current = r;
    }

    public static void stopInterrupt(IHandRenderer r) {
        if (r == current) current = null;
    }

    public static IHandRenderer get() {
        return current;
    }

    public static boolean isPresent() {
        return current != null;
    }
}
