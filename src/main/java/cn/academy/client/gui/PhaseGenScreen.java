package cn.academy.client.gui;

import cn.academy.block.container.PhaseGenMenu;
import cn.academy.block.tileentity.PhaseGenBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class PhaseGenScreen extends TechUIContainerScreen<PhaseGenMenu> {

    private static final ResourceLocation TEX_PHASEGEN = tex("ui_phasegen");

    private static final int LIQUID_COLOR = 0xFFB983FB;

    public PhaseGenScreen(PhaseGenMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, 332, 190);
    }

    @Override
    protected void renderBg(GuiGraphics gg, float partialTick, int mouseX, int mouseY) {
        if (page == 1) {
            drawWirelessPage(gg, partialTick);
            drawPageIcons(gg, ICON_INV, ICON_WIRELESS);
            return;
        }
        drawLeftWindow(gg, TEX_PHASEGEN, partialTick);
        drawPageIcons(gg, ICON_INV, ICON_WIRELESS);

        new InfoArea()
                .histogram(
                        InfoArea.histEnergy(menu.getEnergy(), PhaseGenBlockEntity.BUFFER_SIZE),
                        new InfoArea.HistElement("IF", LIQUID_COLOR,
                                menu.getLiquid() / (double) PhaseGenBlockEntity.TANK_SIZE,
                                menu.getLiquid() + " mB"))
                .draw(gg, this, leftPos, topPos);
    }

    @Override
    protected void renderLabels(GuiGraphics gg, int mouseX, int mouseY) {

        if (page == 1) {
            drawWirelessLabels(gg);
        }
    }
}
