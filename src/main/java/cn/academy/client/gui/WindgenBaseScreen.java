package cn.academy.client.gui;

import cn.academy.block.WindgenConsts;
import cn.academy.block.container.WindgenBaseMenu;
import cn.academy.block.tileentity.WindgenBaseBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class WindgenBaseScreen extends TechUIContainerScreen<WindgenBaseMenu> {

    private static final ResourceLocation TEX_WINDBASE = tex("ui_windbase");
    private static final ResourceLocation ICON_TOWER_MAIN = tex("icon_wind_main");
    private static final ResourceLocation ICON_TOWER_MIDDLE = tex("icon_wind_middle");
    private static final ResourceLocation ICON_TOWER_BASE = tex("icon_wind_base");

    public WindgenBaseScreen(WindgenBaseMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, 332, 190);
    }

    @Override
    protected void renderBg(GuiGraphics gg, float partialTick, int mouseX, int mouseY) {
        if (page == 1) {
            drawWirelessPage(gg, partialTick);
            drawPageIcons(gg, ICON_INV, ICON_WIRELESS);
            return;
        }
        drawLeftWindow(gg, TEX_WINDBASE, partialTick);
        drawPageIcons(gg, ICON_INV, ICON_WIRELESS);

        int comp = menu.getCompleteness();
        float aMain, aMiddle, aBase;
        switch (comp) {
            case WindgenBaseBlockEntity.COMP_COMPLETE -> { aMain = 1.0f; aMiddle = 1.0f; aBase = 1.0f; }
            case WindgenBaseBlockEntity.COMP_COMPLETE_NOT_WORKING -> { aMain = 0.6f; aMiddle = 1.0f; aBase = 1.0f; }
            case WindgenBaseBlockEntity.COMP_NO_TOP -> { aMain = 0.2f; aMiddle = 1.0f; aBase = 1.0f; }
            default -> { aMain = 0.2f; aMiddle = 0.2f; aBase = 1.0f; }
        }
        int ix = leftPos + (LW - 24) / 2;
        drawIcon(gg, ICON_TOWER_MAIN, ix, topPos + 13, 24, aMain);
        drawIcon(gg, ICON_TOWER_MIDDLE, ix, topPos + 31, 24, aMiddle);
        drawIcon(gg, ICON_TOWER_BASE, ix, topPos + 49, 24, aBase);

        new InfoArea()
                .histogram(InfoArea.histBuffer(menu.getEnergy(), WindgenConsts.BUFFER_SIZE))
                .seplineInfo()
                .property("gui.academy.common.prop.altitude", String.valueOf(menu.getPos().getY()))
                .draw(gg, this, leftPos, topPos);
    }

    @Override
    protected void renderLabels(GuiGraphics gg, int mouseX, int mouseY) {

        if (page == 1) {
            drawWirelessLabels(gg);
        }
    }
}
