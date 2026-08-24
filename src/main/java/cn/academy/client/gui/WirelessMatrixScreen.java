package cn.academy.client.gui;

import cn.academy.block.container.WirelessMatrixMenu;
import cn.academy.network.MatrixActionMessage;
import cn.academy.network.MatrixInfoMessage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;

public class WirelessMatrixScreen extends TechUIContainerScreen<WirelessMatrixMenu> {

    private static final ResourceLocation TEX_MATRIX = tex("ui_matrix");

    private static final int BOX_W = 40;

    private static final int BTN_W = 50;

    private MatrixInfoMessage info;
    private EditBox ssidBox;
    private EditBox passBox;
    private PlainTextButton initButton;

    private int ssidY = -1, passY = -1, initY = -1;

    public WirelessMatrixScreen(WirelessMatrixMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, 332, 190);
    }

    @Override
    protected void init() {
        super.init();
        buildDynamicWidgets();
        MatrixActionMessage.send(menu.getPos(), MatrixActionMessage.GATHER, "", "");
    }

    public void onInfo(MatrixInfoMessage m) {
        this.info = m;
        buildDynamicWidgets();
    }

    private boolean isPlacer() {
        return info != null && minecraft != null && minecraft.player != null
                && info.owner.equals(minecraft.player.getName().getString());
    }

    private boolean isInit() {
        return info != null && info.initialized;
    }

    private InfoArea buildInfo() {
        ssidY = passY = initY = -1;

        InfoArea a = new InfoArea()
                .histogram(InfoArea.histCapacity(
                        info != null ? info.load : 0,
                        info != null ? info.capacity : 0))
                .seplineInfo()
                .property("gui.academy.common.prop.owner",
                        info != null && !info.owner.isEmpty() ? info.owner : "-")

                .property("gui.academy.common.prop.range",
                        info != null ? String.format("%.0f", info.range) : "0")

                .property("gui.academy.common.prop.bandwidth",
                        info != null ? String.format("%.1f IF/T", info.bandwidth) : "0.0 IF/T");

        boolean placer = isPlacer();
        if (isInit()) {
            a.sepline("gui.academy.common.sep.wireless_info");
            if (placer) {
                a.propertyEditable("gui.academy.common.prop.ssid");
                ssidY = Math.round(a.lastElementY());
                a.sepline("gui.academy.common.sep.change_pass");
                a.propertyEditable("gui.academy.common.prop.password");
                passY = Math.round(a.lastElementY());
            } else {
                a.propertyEditable("gui.academy.common.prop.ssid");
                ssidY = Math.round(a.lastElementY());
                a.propertyEditable("gui.academy.common.prop.password");
                passY = Math.round(a.lastElementY());
            }
        } else if (placer) {
            a.sepline("gui.academy.common.sep.wireless_init");
            a.propertyEditable("gui.academy.common.prop.ssid");
            ssidY = Math.round(a.lastElementY());
            a.propertyEditable("gui.academy.common.prop.password");
            passY = Math.round(a.lastElementY());
            a.blank(1);
            a.button("INIT");
            initY = Math.round(a.lastElementY());
        } else {

            a.sepline("gui.academy.common.sep.wireless_noinit");
        }
        return a;
    }

    private void buildDynamicWidgets() {
        if (ssidBox != null) removeWidget(ssidBox);
        if (passBox != null) removeWidget(passBox);
        if (initButton != null) removeWidget(initButton);
        ssidBox = null; passBox = null; initButton = null;

        if (info == null) return;

        buildInfo();
        boolean placer = isPlacer();
        int bx = leftPos + InfoArea.valueX();

        if (ssidY >= 0) {
            ssidBox = new EditBox(font, bx, topPos + InfoArea.Y + ssidY, BOX_W, 8, Component.literal("ssid"));
            ssidBox.setMaxLength(32);
            ssidBox.setBordered(false);
            ssidBox.setEditable(placer);
            ssidBox.setTextColor(0xFFFFFF);
            ssidBox.setValue(isInit() ? info.ssid : "");
            resetView(ssidBox);
            addRenderableWidget(ssidBox);
        }
        if (passY >= 0) {
            passBox = new EditBox(font, bx, topPos + InfoArea.Y + passY, BOX_W, 8, Component.literal("pass"));
            passBox.setMaxLength(32);
            passBox.setBordered(false);
            passBox.setEditable(placer);
            passBox.setTextColor(0xFFFFFF);
            passBox.setValue(isInit() ? info.pass : "");
            passBox.setFormatter((s, idx) -> FormattedCharSequence.forward("*".repeat(s.length()), Style.EMPTY));
            resetView(passBox);
            addRenderableWidget(passBox);
        }
        if (initY >= 0) {

            int btnX = leftPos + InfoArea.X + (InfoArea.W - BTN_W) / 2;
            initButton = new PlainTextButton(btnX, topPos + InfoArea.Y + initY, BTN_W, 9,
                    Component.translatable("gui.academy.matrix.init"), () ->
                    MatrixActionMessage.send(menu.getPos(), MatrixActionMessage.INIT,
                            ssidBox.getValue(), passBox.getValue()));
            addRenderableWidget(initButton);
        }
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (isInit() && isPlacer() && (key == 257 || key == 335)) {
            if (ssidBox != null && ssidBox.isFocused()) {
                MatrixActionMessage.send(menu.getPos(), MatrixActionMessage.CHANGE_SSID, ssidBox.getValue(), "");
                return true;
            }
            if (passBox != null && passBox.isFocused()) {
                MatrixActionMessage.send(menu.getPos(), MatrixActionMessage.CHANGE_PASS, "", passBox.getValue());
                return true;
            }
        }

        if ((ssidBox != null && ssidBox.isFocused()) || (passBox != null && passBox.isFocused())) {
            if (ssidBox != null && ssidBox.keyPressed(key, scan, mods)) return true;
            if (passBox != null && passBox.keyPressed(key, scan, mods)) return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    protected void renderBg(GuiGraphics gg, float partialTick, int mouseX, int mouseY) {
        drawLeftWindow(gg, TEX_MATRIX, partialTick);
        drawPageIcons(gg, ICON_INV);
        buildInfo().draw(gg, this, leftPos, topPos);
    }

    @Override
    protected void renderLabels(GuiGraphics gg, int mouseX, int mouseY) {

        if (ssidY >= 0) drawBrackets(gg, InfoArea.Y + ssidY);
        if (passY >= 0) drawBrackets(gg, InfoArea.Y + passY);
    }

    private static void resetView(EditBox box) {
        box.setCursorPosition(0);
        box.setHighlightPos(0);
    }

    private void drawBrackets(GuiGraphics gg, int y) {
        gg.drawString(font, "[", InfoArea.valueX() - 4, y, LABEL, false);
        gg.drawString(font, "]", InfoArea.valueX() + BOX_W + 2, y, LABEL, false);
    }
}
