package cn.academy.client.gui;

import cn.academy.block.container.WirelessNodeMenu;
import cn.academy.block.tileentity.WirelessNodeBlockEntity;
import cn.academy.network.NodeActionMessage;
import cn.academy.network.NodeInfoMessage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;

public class WirelessNodeScreen extends TechUIContainerScreen<WirelessNodeMenu> {

    private static final ResourceLocation TEX_NODE = tex("ui_node");
    private static final ResourceLocation TEX_EFFECT = tex("effect_node");

    private static final int EFF_W = 186, EFF_H = 75, EFF_FRAMES = 10;
    private static final int EFF_X = 42, EFF_Y = 36;
    private static final int EFF_DRAW_W = 93, EFF_DRAW_H = 37;

    private static final int LINKED_BEGIN = 0, LINKED_FRAMES = 8, LINKED_MS = 800;
    private static final int UNLINKED_BEGIN = 8, UNLINKED_FRAMES = 2, UNLINKED_MS = 3000;

    private static final int BOX_W = 40;

    private NodeInfoMessage info;
    private EditBox nameBox;
    private EditBox passBox;

    private int nameY, passY;

    public WirelessNodeScreen(WirelessNodeMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, 332, 190);
    }

    @Override
    protected ResourceLocation wirelessLogo() {
        return ICON_TOMATRIX;
    }

    @Override
    protected void init() {
        super.init();
        buildInfoWidgets();
        NodeActionMessage.send(menu.getPos(), NodeActionMessage.GATHER, "");
    }

    public void onNodeInfo(NodeInfoMessage m) {
        this.info = m;
        buildInfoWidgets();
    }

    private boolean isPlacer() {
        return info != null && minecraft != null && minecraft.player != null
                && info.owner.equals(minecraft.player.getName().getString());
    }

    private InfoArea buildInfo() {
        InfoArea a = new InfoArea()
                .histogram(
                        InfoArea.histEnergy(menu.getEnergy(), menu.getNodeType().maxEnergy),
                        InfoArea.histCapacity(menu.getLoad(), menu.getNodeType().capacity))
                .seplineInfo()

                .property("gui.academy.common.prop.range",
                        String.format("%.1f", (double) menu.getNodeType().range))
                .property("gui.academy.common.prop.owner",
                        info != null && !info.owner.isEmpty() ? info.owner : "-");

        a.propertyEditable("gui.academy.common.prop.node_name");
        nameY = Math.round(a.lastElementY());
        if (isPlacer()) {
            a.propertyEditable("gui.academy.common.prop.password");
            passY = Math.round(a.lastElementY());
        }
        return a;
    }

    private void buildInfoWidgets() {
        if (nameBox != null) removeWidget(nameBox);
        if (passBox != null) removeWidget(passBox);
        nameBox = null; passBox = null;
        if (info == null || page != 0) return;

        buildInfo();
        boolean placer = isPlacer();
        nameBox = new EditBox(font, leftPos + InfoArea.valueX(), topPos + InfoArea.Y + nameY,
                BOX_W, 8, Component.literal("name"));
        nameBox.setMaxLength(32);
        nameBox.setBordered(false);
        nameBox.setEditable(placer);
        nameBox.setTextColor(0xFFFFFF);
        nameBox.setValue(info.nodeName);
        resetView(nameBox);
        addRenderableWidget(nameBox);

        if (placer) {
            passBox = new EditBox(font, leftPos + InfoArea.valueX(), topPos + InfoArea.Y + passY,
                    BOX_W, 8, Component.literal("pass"));
            passBox.setMaxLength(32);
            passBox.setBordered(false);
            passBox.setTextColor(0xFFFFFF);
            passBox.setValue(info.password);
            passBox.setFormatter((s, idx) -> FormattedCharSequence.forward("*".repeat(s.length()), Style.EMPTY));
            resetView(passBox);
            addRenderableWidget(passBox);
        }
    }

    private static void resetView(EditBox box) {
        box.setCursorPosition(0);
        box.setHighlightPos(0);
    }

    @Override
    protected void onPageChanged(int p) {
        buildInfoWidgets();
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (page == 0 && isPlacer() && (key == 257 || key == 335)) {
            if (nameBox != null && nameBox.isFocused()) {
                NodeActionMessage.send(menu.getPos(), NodeActionMessage.RENAME, nameBox.getValue());
                return true;
            }
            if (passBox != null && passBox.isFocused()) {
                NodeActionMessage.send(menu.getPos(), NodeActionMessage.CHANGE_PASS, passBox.getValue());
                return true;
            }
        }
        if (page == 0) {
            if (nameBox != null && nameBox.isFocused() && nameBox.keyPressed(key, scan, mods)) return true;
            if (passBox != null && passBox.isFocused() && passBox.keyPressed(key, scan, mods)) return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    protected void renderBg(GuiGraphics gg, float partialTick, int mouseX, int mouseY) {
        if (page == 1) {
            drawWirelessPage(gg, partialTick);
            drawPageIcons(gg, ICON_INV, ICON_WIRELESS);
            return;
        }
        drawLeftWindow(gg, TEX_NODE, partialTick);
        drawPageIcons(gg, ICON_INV, ICON_WIRELESS);
        drawNodeAnim(gg, partialTick);

        buildInfo().draw(gg, this, leftPos, topPos);
    }

    private void drawNodeAnim(GuiGraphics gg, float partialTick) {
        boolean linked = menu.isLinked();
        int begin = linked ? LINKED_BEGIN : UNLINKED_BEGIN;
        int count = linked ? LINKED_FRAMES : UNLINKED_FRAMES;
        int ms = linked ? LINKED_MS : UNLINKED_MS;

        long timeMs = minecraft != null && minecraft.level != null
                ? (long) ((minecraft.level.getGameTime() + partialTick) * 50f) : 0L;
        int frame = begin + (int) ((timeMs / ms) % count);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        gg.setColor(1f, 1f, 1f, breatheAlpha(partialTick));
        gg.blit(TEX_EFFECT, leftPos + EFF_X, topPos + EFF_Y, EFF_DRAW_W, EFF_DRAW_H,
                0f, frame * EFF_H, EFF_W, EFF_H, EFF_W, EFF_H * EFF_FRAMES);
        gg.setColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    @Override
    protected void renderLabels(GuiGraphics gg, int mouseX, int mouseY) {

        if (page == 1) {
            drawWirelessLabels(gg);
            return;
        }

        if (info != null) {
            drawBrackets(gg, InfoArea.Y + nameY);
            if (isPlacer()) {
                drawBrackets(gg, InfoArea.Y + passY);
            }
        }
    }

    private void drawBrackets(GuiGraphics gg, int y) {
        gg.drawString(font, "[", InfoArea.valueX() - 4, y, LABEL, false);
        gg.drawString(font, "]", InfoArea.valueX() + BOX_W + 2, y, LABEL, false);
    }
}
