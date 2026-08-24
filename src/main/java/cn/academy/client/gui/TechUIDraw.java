package cn.academy.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class TechUIDraw {

    private TechUIDraw() {}

    public static ResourceLocation tex(String name) {
        return new ResourceLocation("academy", "textures/gui/" + name + ".png");
    }

    public static float breathe(float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        float t = mc.level != null ? (mc.level.getGameTime() + partialTick) / 20f : 0f;
        return 0.675f + 0.175f * (float) ((1 + Math.sin(t / 0.8)) * 0.5);
    }

    public static void panel(GuiGraphics gg, int px, int py, int pw, int ph) {
        gg.fill(px, py, px + pw, py + ph, 0x80000000);
        gg.fill(px + 2, py + 1, px + pw - 2, py + 2, 0xD8FFFFFF);
        gg.fill(px + 2, py + ph - 2, px + pw - 2, py + ph - 1, 0xD8FFFFFF);
        gg.fill(px, py + 1, px + 1, py + ph - 1, 0x14FFFFFF);
        gg.fill(px + pw - 1, py + 1, px + pw, py + ph - 1, 0x14FFFFFF);
    }

    public static void icon(GuiGraphics gg, ResourceLocation icon, int x, int y, int size, float alpha, int srcSize) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        gg.setColor(1f, 1f, 1f, alpha);
        gg.blit(icon, x, y, size, size, 0f, 0f, srcSize, srcSize, srcSize, srcSize);
        gg.setColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    public static boolean inRect(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
