package cn.academy.client.gui;

import cn.academy.block.container.TechUIMenu;
import cn.academy.network.WirelessActionMessage;
import cn.academy.network.WirelessInfoMessage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;

public abstract class TechUIContainerScreen<T extends TechUIMenu> extends AbstractContainerScreen<T> {

    protected static final ResourceLocation TEX_UI_INV = tex("ui_inventory");
    protected static final ResourceLocation TEX_HISTOGRAM = tex("histogram");
    protected static final ResourceLocation ICON_INV = tex("icon_inv");
    protected static final ResourceLocation ICON_WIRELESS = tex("icon_wireless");
    protected static final int TEX_W = 352, TEX_H = 374;
    protected static final int LW = 176, WH = 187;

    protected static final ResourceLocation ICON_TONODE = WirelessPanel.ICON_TONODE;
    protected static final ResourceLocation ICON_TOMATRIX = WirelessPanel.ICON_TOMATRIX;

    protected int page = 0;

    protected boolean hasWirelessPage = false;

    private int pageIconCount = 1;

    private double openAnimStart = -1;

    private WirelessPanel wirelessPanel;

    protected static final int LABEL  = 0xFFBFC6CC;
    protected static final int VALUE  = 0xFFFFFFFF;
    protected static final int HEADER = 0xFF8A9299;
    protected static final int AXIS   = 0xFF6E767E;

    protected static ResourceLocation tex(String name) {
        return TechUIDraw.tex(name);
    }

    protected TechUIContainerScreen(T menu, Inventory inv, Component title, int imageWidth, int imageHeight) {
        super(menu, inv, title);
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;

        renderBackground(gg);

        if (openAnimStart < 0) openAnimStart = cn.lambdalib2.util.GameTimer.getAbsTime();
        float p = cn.lambdalib2.cgui.GuiOpenAnimation.progress(openAnimStart);
        gg.pose().pushPose();
        cn.lambdalib2.cgui.GuiOpenAnimation.apply(gg.pose(), p);
        super.render(gg, mouseX, mouseY, partialTick);
        gg.pose().popPose();
        if (page == 0) {
            renderTooltip(gg, mouseX, mouseY);
        }
    }

    protected float breatheAlpha(float partialTick) {
        return TechUIDraw.breathe(partialTick);
    }

    protected void drawPanel(GuiGraphics gg, int px, int py, int pw, int ph) {
        TechUIDraw.panel(gg, px, py, pw, ph);
    }

    protected void drawLeftWindow(GuiGraphics gg, ResourceLocation machineTex, float partialTick) {
        drawPanel(gg, leftPos, topPos, LW, WH);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        gg.setColor(1f, 1f, 1f, breatheAlpha(partialTick));
        gg.blit(TEX_UI_INV, leftPos, topPos, LW, WH, 0f, 0f, TEX_W, TEX_H, TEX_W, TEX_H);
        if (machineTex != null) {
            gg.blit(machineTex, leftPos, topPos, LW, WH, 0f, 0f, TEX_W, TEX_H, TEX_W, TEX_H);
        }
        gg.setColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    protected void drawIcon(GuiGraphics gg, ResourceLocation icon, int x, int y, int size, float alpha) {
        drawIcon(gg, icon, x, y, size, alpha, 48);
    }

    protected void drawIcon(GuiGraphics gg, ResourceLocation icon, int x, int y, int size, float alpha, int srcSize) {
        TechUIDraw.icon(gg, icon, x, y, size, alpha, srcSize);
    }

    protected void drawPageIcons(GuiGraphics gg, ResourceLocation... icons) {
        pageIconCount = icons.length;
        for (int i = 0; i < icons.length; i++) {
            int x = pageIconX(), y = pageIconY(i);
            boolean hover = isOverPageIcon(i, lastMouseX, lastMouseY);
            drawIcon(gg, icons[i], x, y, 16, (i == page || hover) ? 1.0f : 0.8f);
        }
    }

    private int pageIconX() {
        return leftPos - 21;
    }

    private int pageIconY(int i) {
        return topPos + 1 + i * 22;
    }

    private boolean isOverPageIcon(int i, double mx, double my) {
        int x = pageIconX(), y = pageIconY(i);
        return mx >= x && mx < x + 16 && my >= y && my < y + 16;
    }

    protected ResourceLocation wirelessLogo() {
        return ICON_TONODE;
    }

    protected WirelessPanel wirelessPanel() {
        if (wirelessPanel == null) {
            wirelessPanel = new WirelessPanel(new WirelessPanel.Host() {
                @Override public Font font() { return font; }
                @Override public void addPassBox(EditBox box) { addRenderableWidget(box); }
                @Override public void removePassBox(EditBox box) { removeWidget(box); }
                @Override public void focusPassBox(EditBox box) { setFocused(box); }
            }, menu.getPos(), wirelessLogo());
        }
        return wirelessPanel;
    }

    public void onWirelessInfo(WirelessInfoMessage m) {
        wirelessPanel().onInfo(m, leftPos, topPos);
    }

    protected void requestWirelessInfo() {
        wirelessPanel().requestInfo();
    }

    protected void drawWirelessPage(GuiGraphics gg, float partialTick) {
        wirelessPanel().render(gg, leftPos, topPos, partialTick);
    }

    protected void drawWirelessLabels(GuiGraphics gg) {
        wirelessPanel().renderText(gg, 0, 0);
    }

    public void setPage(int p) {
        if (p == page) return;
        page = p;
        menu.setSlotsActive(p == 0);
        onPageChanged(p);
        if (p == 1) {
            requestWirelessInfo();
        } else {
            wirelessPanel().clearPassBoxes();
        }
    }

    protected void onPageChanged(int p) {}

    private double lastMouseX, lastMouseY;

    @Override
    public boolean mouseClicked(double mx, double my, int button) {

        for (int i = 0; i < pageIconCount; i++) {
            if (isOverPageIcon(i, mx, my)) {
                setPage(i);
                return true;
            }
        }
        if (page == 1) {
            wirelessPanel().mouseClicked(mx, my, button, leftPos, topPos);
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    private static boolean inRect(double mx, double my, int x, int y, int w, int h) {
        return TechUIDraw.inRect(mx, my, x, y, w, h);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (page == 1 && wirelessPanel().keyPressed(key, scan, mods)) return true;
        return super.keyPressed(key, scan, mods);
    }

    protected boolean mouseInRect(int x, int y, int w, int h) {
        return inRect(lastMouseX, lastMouseY, x, y, w, h);
    }

    protected void drawHeader(GuiGraphics gg, int x, int y, String text) {
        gg.drawString(font, text, x, y, HEADER, false);
    }

    protected static String tr(String fullKey) {
        return Component.translatable(fullKey).getString();
    }
}
