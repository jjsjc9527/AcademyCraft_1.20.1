package cn.academy.client.gui;

import cn.academy.block.container.WirelessGeneratorMenu;
import cn.academy.block.tileentity.WirelessGeneratorBlockEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class WirelessGeneratorScreen extends TechUIContainerScreen<WirelessGeneratorMenu> {

    private static final ResourceLocation TEX_SOLAR = tex("ui_windbase");
    private static final ResourceLocation TEX_EFFECT = tex("effect_solar");
    private static final int EFF_W = 104, EFF_H = 70, EFF_TEX_H = 210;

    private static final int EFF_X = 56, EFF_Y = 23;
    private static final int EFF_DRAW_W = 62, EFF_DRAW_H = 42;

    public WirelessGeneratorScreen(WirelessGeneratorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, 332, 190);
    }

    @Override
    protected void renderBg(GuiGraphics gg, float partialTick, int mouseX, int mouseY) {
        if (page == 1) {
            drawWirelessPage(gg, partialTick);
            drawPageIcons(gg, ICON_INV, ICON_WIRELESS);
            return;
        }
        drawLeftWindow(gg, TEX_SOLAR, partialTick);
        drawPageIcons(gg, ICON_INV, ICON_WIRELESS);

        int frame = switch (menu.getStatus()) {
            case WirelessGeneratorBlockEntity.STATUS_STRONG -> 0;
            case WirelessGeneratorBlockEntity.STATUS_WEAK -> 2;
            default -> 1;
        };
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        gg.blit(TEX_EFFECT, leftPos + EFF_X, topPos + EFF_Y, EFF_DRAW_W, EFF_DRAW_H,
                0f, frame * EFF_H, EFF_W, EFF_H, EFF_W, EFF_TEX_H);
        RenderSystem.disableBlend();

        double gen = switch (menu.getStatus()) {
            case WirelessGeneratorBlockEntity.STATUS_STRONG -> 3.0;
            case WirelessGeneratorBlockEntity.STATUS_WEAK -> 0.6;
            default -> 0.0;
        };
        new InfoArea()
                .histogram(InfoArea.histBuffer(menu.getEnergy(), WirelessGeneratorBlockEntity.BUFFER_SIZE))
                .seplineInfo()

                .property("gui.academy.common.prop.gen_speed", String.format("%.2fIF/T", gen))
                .draw(gg, this, leftPos, topPos);
    }

    @Override
    protected void renderLabels(GuiGraphics gg, int mouseX, int mouseY) {

        if (page == 1) {
            drawWirelessLabels(gg);
        }
    }
}
