package cn.academy.client.gui;

import cn.academy.block.container.AbilityInterfererMenu;
import cn.academy.config.InterfererConfig;
import cn.academy.network.InterfererActionMessage;
import cn.academy.network.InterfererInfoMessage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class AbilityInterfererScreen extends TechUIContainerScreen<AbilityInterfererMenu> {

    private static final ResourceLocation TEX_INTERFERE = tex("ui_interfere");
    private static final ResourceLocation BTN_SWITCH_ON = tex("button_switch_on");
    private static final ResourceLocation BTN_SWITCH_OFF = tex("button_switch_off");
    private static final ResourceLocation BTN_LEFT = tex("button_arrowlefta");
    private static final ResourceLocation BTN_RIGHT = tex("button_arrowrighta");
    private static final ResourceLocation BTN_UP = tex("button_arrowupb");
    private static final ResourceLocation BTN_DOWN = tex("button_arrowdownb");
    private static final ResourceLocation BTN_ADD = tex("button_add");
    private static final ResourceLocation BTN_REMOVE = tex("button_remove");
    private static final ResourceLocation ICON_WL = tex("icon_whitelist_single");

    private static final int SWITCH_X = 48, SWITCH_Y = 25, SWITCH_SIZE = 16;
    private static final int RANGE_L_X = 48, RANGE_R_X = 108, RANGE_Y = 42, RANGE_BTN = 16;
    private static final int RANGE_VAL_CX = 86, RANGE_VAL_Y = 46;
    private static final int LABEL_X = 12;

    private static final int WL_ADD_X = 12, WL_REMOVE_X = 32, WL_BTN_Y = 82, WL_SMALL = 12;
    private static final int WL_UP_X = 152, WL_UP_Y = 95, WL_DOWN_X = 152, WL_DOWN_Y = 147, WL_ARROW = 16;
    private static final int WL_ZONE_X = 8, WL_ZONE_Y = 101, WL_ZONE_W = 140;
    private static final int WL_ROW_H = 15, WL_VISIBLE = 4;

    private static final int RANGE_STEP = 10;

    private boolean enabled = false;
    private double range = InterfererConfig.rangeMin();
    private final List<String> whitelist = new ArrayList<>();
    private int scroll = 0;
    private int selected = -1;

    private EditBox addBox;

    public AbilityInterfererScreen(AbilityInterfererMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, 332, 190);
        this.hasWirelessPage = true;
    }

    @Override
    protected void init() {
        super.init();

        InterfererActionMessage.send(menu.getPos(), InterfererActionMessage.GATHER, false, 0, null);
    }

    public void onInfo(InterfererInfoMessage m) {
        if (!m.getPos().equals(menu.getPos())) return;
        enabled = m.isEnabled();
        range = m.getRange();
        whitelist.clear();
        for (String s : m.getNames()) whitelist.add(s);
        clampScroll();
        if (selected >= whitelist.size()) selected = -1;
    }

    @Override
    protected void drawLeftWindow(GuiGraphics gg, ResourceLocation machineTex, float partialTick) {
        drawPanel(gg, leftPos, topPos, LW, WH);
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        gg.setColor(1f, 1f, 1f, breatheAlpha(partialTick));
        if (machineTex != null) {
            gg.blit(machineTex, leftPos, topPos, LW, WH, 0f, 0f, TEX_W, TEX_H, TEX_W, TEX_H);
        }
        gg.setColor(1f, 1f, 1f, 1f);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }

    @Override
    protected void renderBg(GuiGraphics gg, float partialTick, int mouseX, int mouseY) {
        if (page == 1) {
            drawWirelessPage(gg, partialTick);
            drawPageIcons(gg, ICON_INV, ICON_WIRELESS);
            return;
        }
        drawLeftWindow(gg, TEX_INTERFERE, partialTick);
        drawPageIcons(gg, ICON_INV, ICON_WIRELESS);

        drawConfig(gg);
        drawWhitelist(gg);

        new InfoArea()
                .histogram(InfoArea.histEnergy(menu.getEnergy(), InterfererConfig.maxEnergy()))
                .draw(gg, this, leftPos, topPos);
    }

    private void drawConfig(GuiGraphics gg) {

        drawIcon(gg, enabled ? BTN_SWITCH_ON : BTN_SWITCH_OFF,
                leftPos + SWITCH_X, topPos + SWITCH_Y, SWITCH_SIZE, enabled ? 1.0f : 0.7f, 32);

        drawIcon(gg, BTN_LEFT, leftPos + RANGE_L_X, topPos + RANGE_Y, RANGE_BTN,
                mouseInRect(leftPos + RANGE_L_X, topPos + RANGE_Y, RANGE_BTN, RANGE_BTN) ? 1.0f : 0.8f, 32);
        drawIcon(gg, BTN_RIGHT, leftPos + RANGE_R_X, topPos + RANGE_Y, RANGE_BTN,
                mouseInRect(leftPos + RANGE_R_X, topPos + RANGE_Y, RANGE_BTN, RANGE_BTN) ? 1.0f : 0.8f, 32);
    }

    private void drawWhitelist(GuiGraphics gg) {

        drawIcon(gg, BTN_ADD, leftPos + WL_ADD_X, topPos + WL_BTN_Y, WL_SMALL,
                mouseInRect(leftPos + WL_ADD_X, topPos + WL_BTN_Y, WL_SMALL, WL_SMALL) ? 1.0f : 0.7f, 32);
        drawIcon(gg, BTN_REMOVE, leftPos + WL_REMOVE_X, topPos + WL_BTN_Y, WL_SMALL,
                mouseInRect(leftPos + WL_REMOVE_X, topPos + WL_BTN_Y, WL_SMALL, WL_SMALL) ? 1.0f : 0.7f, 32);
        drawIcon(gg, BTN_UP, leftPos + WL_UP_X, topPos + WL_UP_Y, WL_ARROW,
                (scroll > 0 ? 0.9f : 0.4f), 32);
        drawIcon(gg, BTN_DOWN, leftPos + WL_DOWN_X, topPos + WL_DOWN_Y, WL_ARROW,
                (scroll < maxScroll() ? 0.9f : 0.4f), 32);

        for (int i = 0; i < WL_VISIBLE; i++) {
            int idx = scroll + i;
            if (idx >= whitelist.size()) break;
            int rowY = topPos + WL_ZONE_Y + i * WL_ROW_H;
            int rowX = leftPos + WL_ZONE_X;
            if (idx == selected) {
                gg.fill(rowX, rowY, rowX + WL_ZONE_W, rowY + WL_ROW_H - 1, 0x40FFFFFF);
            }
            drawIcon(gg, ICON_WL, rowX + 2, rowY + 2, 10, 1.0f, 24);
            gg.drawString(font, whitelist.get(idx), rowX + 16, rowY + 3, VALUE, false);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics gg, int mouseX, int mouseY) {
        if (page == 1) {
            drawWirelessLabels(gg);
            return;
        }

        gg.drawString(font, tr("gui.academy.ability_interferer.switch"), LABEL_X, SWITCH_Y + 4, LABEL, false);
        gg.drawString(font, tr("gui.academy.ability_interferer.range"), LABEL_X, RANGE_Y + 4, LABEL, false);

        String rangeStr = String.format(java.util.Locale.ROOT, "%.1f", range);
        int tw = font.width(rangeStr);
        gg.drawString(font, rangeStr, RANGE_VAL_CX - tw / 2, RANGE_VAL_Y, VALUE, false);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {

        if (addBox != null && !addBox.isMouseOver(mx, my)) {
            removeAddBox();
        }

        if (page == 0) {

            if (inBox(mx, my, leftPos + SWITCH_X, topPos + SWITCH_Y, SWITCH_SIZE, SWITCH_SIZE)) {
                enabled = !enabled;
                InterfererActionMessage.send(menu.getPos(), InterfererActionMessage.SET_ENABLED, enabled, 0, null);
                return true;
            }

            if (inBox(mx, my, leftPos + RANGE_L_X, topPos + RANGE_Y, RANGE_BTN, RANGE_BTN)) {
                changeRange(-RANGE_STEP);
                return true;
            }
            if (inBox(mx, my, leftPos + RANGE_R_X, topPos + RANGE_Y, RANGE_BTN, RANGE_BTN)) {
                changeRange(RANGE_STEP);
                return true;
            }

            if (inBox(mx, my, leftPos + WL_ADD_X, topPos + WL_BTN_Y, WL_SMALL, WL_SMALL)) {
                openAddBox();
                return true;
            }
            if (inBox(mx, my, leftPos + WL_REMOVE_X, topPos + WL_BTN_Y, WL_SMALL, WL_SMALL)) {
                removeSelected();
                return true;
            }
            if (inBox(mx, my, leftPos + WL_UP_X, topPos + WL_UP_Y, WL_ARROW, WL_ARROW)) {
                if (scroll > 0) scroll--;
                return true;
            }
            if (inBox(mx, my, leftPos + WL_DOWN_X, topPos + WL_DOWN_Y, WL_ARROW, WL_ARROW)) {
                if (scroll < maxScroll()) scroll++;
                return true;
            }

            for (int i = 0; i < WL_VISIBLE; i++) {
                int idx = scroll + i;
                if (idx >= whitelist.size()) break;
                int rowY = topPos + WL_ZONE_Y + i * WL_ROW_H;
                if (inBox(mx, my, leftPos + WL_ZONE_X, rowY, WL_ZONE_W, WL_ROW_H)) {
                    selected = idx;
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {

        if (addBox != null) {
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                confirmAdd();
                return true;
            }
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                removeAddBox();
                return true;
            }
            addBox.keyPressed(key, scan, mods);
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    private void changeRange(int delta) {
        double nv = Mth.clamp(range + delta, InterfererConfig.rangeMin(), InterfererConfig.rangeMax());
        if (nv == range) return;
        range = nv;
        InterfererActionMessage.send(menu.getPos(), InterfererActionMessage.SET_RANGE, false, nv, null);
    }

    private void removeSelected() {
        if (selected < 0 || selected >= whitelist.size()) return;
        List<String> next = new ArrayList<>(whitelist);
        next.remove(selected);
        selected = -1;
        sendWhitelist(next);
    }

    private void openAddBox() {
        if (addBox != null) return;
        addBox = new EditBox(font, leftPos + WL_ZONE_X + 14, topPos + WL_BTN_Y - 1,
                WL_ZONE_W - 20, 12, Component.empty());
        addBox.setMaxLength(40);
        addBox.setBordered(true);
        addRenderableWidget(addBox);
        setFocused(addBox);
        addBox.setFocused(true);
    }

    private void confirmAdd() {
        String name = addBox.getValue().trim();
        if (!name.isEmpty()) {
            List<String> next = new ArrayList<>(whitelist);
            if (!next.contains(name)) next.add(name);
            sendWhitelist(next);
        }
        removeAddBox();
    }

    private void removeAddBox() {
        if (addBox == null) return;
        removeWidget(addBox);
        setFocused(null);
        addBox = null;
    }

    private void sendWhitelist(List<String> names) {
        whitelist.clear();
        whitelist.addAll(names);
        clampScroll();
        InterfererActionMessage.send(menu.getPos(), InterfererActionMessage.SET_WHITELIST,
                false, 0, names.toArray(new String[0]));
    }

    private int maxScroll() {
        return Math.max(0, whitelist.size() - WL_VISIBLE);
    }

    private void clampScroll() {
        if (scroll > maxScroll()) scroll = maxScroll();
        if (scroll < 0) scroll = 0;
    }

    @Override
    protected void onPageChanged(int p) {
        if (p != 0) removeAddBox();
    }

    private static boolean inBox(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
