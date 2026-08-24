package cn.academy.client.gui;

import cn.academy.block.container.WindgenMainMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class WindgenMainScreen extends TechUIContainerScreen<WindgenMainMenu> {

    private static final ResourceLocation TEX_WINDMAIN = tex("ui_windmain");

    public WindgenMainScreen(WindgenMainMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, 332, 190);
    }

    @Override
    protected void renderBg(GuiGraphics gg, float partialTick, int mouseX, int mouseY) {
        drawLeftWindow(gg, TEX_WINDMAIN, partialTick);
        drawPageIcons(gg, ICON_INV);

        new InfoArea()
                .property("gui.academy.common.prop.altitude", String.valueOf(menu.getPos().getY()))
                .draw(gg, this, leftPos, topPos);
    }

    @Override
    protected void renderLabels(GuiGraphics gg, int mouseX, int mouseY) {

    }
}
