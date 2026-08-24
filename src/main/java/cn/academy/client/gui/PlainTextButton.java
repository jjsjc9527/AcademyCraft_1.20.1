package cn.academy.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class PlainTextButton extends AbstractButton {

    private final Runnable action;

    public PlainTextButton(int x, int y, int w, int h, Component msg, Runnable action) {
        super(x, y, w, h, msg);
        this.action = action;
    }

    @Override
    public void onPress() {
        action.run();
    }

    @Override
    protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        int lum = isHoveredOrFocused() ? 0xFF : 0xCC;
        int color = 0xFF000000 | (lum << 16) | (lum << 8) | lum;
        gg.drawCenteredString(Minecraft.getInstance().font, getMessage(),
                getX() + width / 2, getY() + (height - 8) / 2, color);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
