package cn.academy.client.gui;

import cn.academy.block.container.ImagFusorMenu;
import cn.academy.block.tileentity.ImagFusorBlockEntity;
import cn.academy.crafting.ImagFusorRecipes;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ImagFusorScreen extends TechUIContainerScreen<ImagFusorMenu> {

    private static final ResourceLocation TEX_FUSOR = tex("ui_imagfusor");
    private static final ResourceLocation TEX_PROGRESS = tex("progress_fusor");

    private static final int PROG_X = 58, PROG_Y = 47, PROG_W = 61, PROG_H = 15;

    private static final int TXT_X = 68, TXT_Y = 12, TXT_W = 44, TXT_H = 12;

    public ImagFusorScreen(ImagFusorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, 332, 190);
    }

    @Override
    protected void renderBg(GuiGraphics gg, float partialTick, int mouseX, int mouseY) {
        if (page == 1) {
            drawWirelessPage(gg, partialTick);
            drawPageIcons(gg, ICON_INV, ICON_WIRELESS);
            return;
        }
        drawLeftWindow(gg, TEX_FUSOR, partialTick);
        drawPageIcons(gg, ICON_INV, ICON_WIRELESS);

        drawProgress(gg);

        new InfoArea()
                .histogram(
                        InfoArea.histEnergy(menu.getEnergy(), ImagFusorBlockEntity.MAX_ENERGY),
                        InfoArea.histPhaseLiquid(menu.getLiquid(), ImagFusorBlockEntity.TANK_SIZE))
                .draw(gg, this, leftPos, topPos);
    }

    private void drawProgress(GuiGraphics gg) {
        double p = Math.max(0, Math.min(1, menu.getProgress()));
        if (p <= 0) return;
        int w = (int) Math.round(PROG_W * p);
        if (w <= 0) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        gg.blit(TEX_PROGRESS, leftPos + PROG_X, topPos + PROG_Y, w, PROG_H,
                0f, 0f, (int) Math.round(126 * p), 30, 126, 30);
        RenderSystem.disableBlend();
    }

    @Override
    protected void renderLabels(GuiGraphics gg, int mouseX, int mouseY) {

        if (page == 1) {
            drawWirelessLabels(gg);
            return;
        }

        ImagFusorRecipes.IFRecipe recipe = menu.getCurrentRecipe();
        String txt = recipe == null ? "IDLE" : String.valueOf(recipe.consumeLiquid);
        int tw = font.width(txt);
        gg.drawString(font, txt,
                TXT_X + (TXT_W - tw) / 2,
                TXT_Y + (TXT_H - font.lineHeight) / 2 + 1,
                0xCCFFFFFF, false);

    }
}
