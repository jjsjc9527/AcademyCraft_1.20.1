package cn.academy.client.render.util;

import cn.academy.Resources;
import cn.lambdalib2.util.Color;
import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.HudUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ACRenderingHelper {

    private static ResourceLocation glowtex(String name) {
        return Resources.getTexture("gui/glow_" + name);
    }

    public static final ResourceLocation
            GLOW_L = glowtex("left"),
            GLOW_R = glowtex("right"),
            GLOW_U = glowtex("up"),
            GLOW_D = glowtex("down"),
            GLOW_RU = glowtex("ru"),
            GLOW_RD = glowtex("rd"),
            GLOW_LU = glowtex("lu"),
            GLOW_LD = glowtex("ld");

    public static void drawGlow(double x, double y, double width, double height, double size, Color glowColor) {
        Colors.bindToGL(glowColor);

        final double s = size;
        gdraw(GLOW_L, x - s, y, s, height);
        gdraw(GLOW_R, x + width, y, s, height);
        gdraw(GLOW_U, x, y - s, width, s);
        gdraw(GLOW_D, x, y + height, width, s);
        gdraw(GLOW_RU, x + width, y - s, s, s);
        gdraw(GLOW_RD, x + width, y + height, s, s);
        gdraw(GLOW_LU, x - s, y - s, s, s);
        gdraw(GLOW_LD, x - s, y + height, s, s);
    }

    private static void gdraw(ResourceLocation tex, double x, double y, double w, double h) {
        HudUtils.loadTexture(tex);
        HudUtils.rect(x, y, w, h);
    }
}
