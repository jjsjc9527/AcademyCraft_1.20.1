package cn.academy.client.gui;

import cn.academy.block.container.MetalFormerMenu;
import cn.academy.block.tileentity.MetalFormerBlockEntity;
import cn.academy.crafting.MetalFormerRecipes.Mode;
import cn.academy.network.MetalFormerActionMessage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class MetalFormerScreen extends TechUIContainerScreen<MetalFormerMenu> {

    private static final ResourceLocation TEX_FORMER = tex("ui_metalformer");
    private static final ResourceLocation TEX_PROGRESS = tex("progress_metalformer");
    private static final ResourceLocation BTN_LEFT = tex("button_arrowlefta");
    private static final ResourceLocation BTN_RIGHT = tex("button_arrowrighta");

    private static final ResourceLocation[] MODE_ICONS = {
            tex("icon_former_plate"), tex("icon_former_incise"),
            tex("icon_former_etch"), tex("icon_former_refine"),
    };

    private static final int PROG_X = 60, PROG_Y = 47, PROG_W = 57, PROG_H = 15;
    private static final int PROG_TEX_W = 114, PROG_TEX_H = 30;

    private static final int ICON_X = 76, ICON_Y = 5, ICON_SIZE = 24, ICON_SRC = 48;
    private static final int BTN_Y = 9, BTN_SIZE = 16, BTN_SRC = 32;
    private static final int BTN_L_X = 60, BTN_R_X = 100;

    public MetalFormerScreen(MetalFormerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, 332, 190);
        this.hasWirelessPage = true;
    }

    @Override
    protected void renderBg(GuiGraphics gg, float partialTick, int mouseX, int mouseY) {
        if (page == 1) {
            drawWirelessPage(gg, partialTick);
            drawPageIcons(gg, ICON_INV, ICON_WIRELESS);
            return;
        }
        drawLeftWindow(gg, TEX_FORMER, partialTick);
        drawPageIcons(gg, ICON_INV, ICON_WIRELESS);

        drawModeSelector(gg);
        drawProgress(gg);

        new InfoArea()
                .histogram(InfoArea.histEnergy(menu.getEnergy(), MetalFormerBlockEntity.MAX_ENERGY))
                .draw(gg, this, leftPos, topPos);
    }

    private void drawModeSelector(GuiGraphics gg) {
        drawIcon(gg, MODE_ICONS[menu.getMode().ordinal()],
                leftPos + ICON_X, topPos + ICON_Y, ICON_SIZE, 1.0f, ICON_SRC);
        drawIcon(gg, BTN_LEFT, leftPos + BTN_L_X, topPos + BTN_Y, BTN_SIZE,
                isOverBtn(BTN_L_X) ? 1.0f : 0.8f, BTN_SRC);
        drawIcon(gg, BTN_RIGHT, leftPos + BTN_R_X, topPos + BTN_Y, BTN_SIZE,
                isOverBtn(BTN_R_X) ? 1.0f : 0.8f, BTN_SRC);
    }

    private void drawProgress(GuiGraphics gg) {
        double p = Math.max(0, Math.min(1, menu.getProgress()));
        if (p <= 0) return;
        int w = (int) Math.round(PROG_W * p);
        if (w <= 0) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        gg.blit(TEX_PROGRESS, leftPos + PROG_X, topPos + PROG_Y, w, PROG_H,
                0f, 0f, (int) Math.round(PROG_TEX_W * p), PROG_TEX_H, PROG_TEX_W, PROG_TEX_H);
        RenderSystem.disableBlend();
    }

    @Override
    protected void renderLabels(GuiGraphics gg, int mouseX, int mouseY) {

        if (page == 1) {
            drawWirelessLabels(gg);
            return;
        }

        if (isOverIcon(mouseX - leftPos, mouseY - topPos)) {
            drawModeTextBox(gg, menu.getMode());
        }
    }

    private void drawModeTextBox(GuiGraphics gg, Mode mode) {
        String txt = Component.translatable(
                "gui.academy.metal_former.mode." + mode.name().toLowerCase(java.util.Locale.ROOT)).getString();
        int tw = font.width(txt);
        int x = ICON_X + 6, y = ICON_Y - 10;
        int bx = x - tw / 2;
        int bw = tw + 5 * 2 + 2;
        int bh = font.lineHeight + 2 * 2;
        gg.fill(bx, y, bx + bw, y + bh, 0x80000000);
        gg.drawString(font, txt, x + 5 - tw / 2, y + 2, 0xCCFFFFFF, false);
    }

    private boolean isOverIcon(int mx, int my) {
        return mx >= ICON_X && mx < ICON_X + ICON_SIZE && my >= ICON_Y && my < ICON_Y + ICON_SIZE;
    }

    private boolean isOverBtn(int btnX) {
        return mouseInRect(leftPos + btnX, topPos + BTN_Y, BTN_SIZE, BTN_SIZE);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (page == 0) {
            if (inBox(mx, my, leftPos + BTN_L_X, topPos + BTN_Y, BTN_SIZE, BTN_SIZE)) {
                MetalFormerActionMessage.send(menu.getPos(), -1);
                return true;
            }
            if (inBox(mx, my, leftPos + BTN_R_X, topPos + BTN_Y, BTN_SIZE, BTN_SIZE)) {
                MetalFormerActionMessage.send(menu.getPos(), 1);
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    private static boolean inBox(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
