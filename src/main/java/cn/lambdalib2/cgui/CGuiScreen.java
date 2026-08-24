package cn.lambdalib2.cgui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CGuiScreen extends Screen {

    protected CGui gui;

    protected boolean drawBack = true;

    private double openAnimStart = -1;

    public CGuiScreen(CGui _gui) {
        super(Component.empty());
        gui = _gui;
    }

    public CGuiScreen() {
        this(new CGui());
    }

    public CGuiScreen setDrawBack(boolean flag) {
        drawBack = flag;
        return this;
    }

    @Override
    protected void init() {
        gui.resize(width, height);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        gui.resize(width, height);
        if (drawBack) {
            renderBackground(g);
        }

        if (openAnimStart < 0) openAnimStart = cn.lambdalib2.util.GameTimer.getAbsTime();
        float p = GuiOpenAnimation.progress(openAnimStart);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.pose().pushPose();
        GuiOpenAnimation.apply(g.pose(), p);
        gui.draw(g.pose(), mx, my);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        super.render(g, mx, my, partial);
        g.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        gui.mouseClicked((int) mx, (int) my, btn);
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        gui.mouseClickMove((int) mx, (int) my, btn, 0);
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {

        gui.keyTyped('\0', keyCode);
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char ch, int modifiers) {
        gui.keyTyped(ch, 0);
        return super.charTyped(ch, modifiers);
    }

    @Override
    public void removed() {
        gui.dispose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public CGui getGui() {
        return gui;
    }
}
