package cn.academy.client.gui;

import cn.academy.network.WirelessActionMessage;
import cn.academy.network.WirelessInfoMessage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class WirelessPanel {

    public static final int PW = 176, PH = 187;

    public static final ResourceLocation ICON_TONODE = TechUIDraw.tex("icon_tonode");
    public static final ResourceLocation ICON_TOMATRIX = TechUIDraw.tex("icon_tomatrix");
    private static final ResourceLocation ICON_KEY = TechUIDraw.tex("icon_key");
    private static final ResourceLocation ICON_CONNECTED = TechUIDraw.tex("icon_connected");
    private static final ResourceLocation ICON_UNCONNECTED = TechUIDraw.tex("icon_unconnected");
    private static final ResourceLocation ICON_DEVICE = TechUIDraw.tex("icon_matrix");
    private static final ResourceLocation ELEM_BG = TechUIDraw.tex("element_background300x32");
    private static final ResourceLocation BTN_UP = TechUIDraw.tex("button_arrowupb");
    private static final ResourceLocation BTN_DOWN = TechUIDraw.tex("button_arrowdownb");

    private static final int WL_LOGO_X = 10, WL_LOGO_Y = 10;
    private static final int WL_CONNECTED_LABEL_Y = 34;
    private static final int WL_CONN_ROW_Y = 44;
    private static final int WL_AVAIL_LABEL_Y = 66;
    private static final int WL_LIST_Y = 76;
    private static final int WL_ROW_H = 18;
    public static final int WL_ROWS = 5;
    private static final int WL_ROW_X = 8, WL_ROW_W = 150;
    private static final int WL_ARROW = 14;

    private static final int VALUE = 0xFFFFFFFF;
    private static final int HEADER = 0xFF8A9299;

    public interface Host {
        Font font();

        void addPassBox(EditBox box);

        void removePassBox(EditBox box);

        void focusPassBox(EditBox box);
    }

    private final Host host;

    private final BlockPos pos;

    private final ResourceLocation logo;

    @Nullable
    private WirelessInfoMessage info;
    private int scroll = 0;
    private final EditBox[] passBoxes = new EditBox[WL_ROWS];

    public WirelessPanel(Host host, BlockPos pos, ResourceLocation logo) {
        this.host = host;
        this.pos = pos;
        this.logo = logo;
    }

    public void requestInfo() {
        WirelessActionMessage.send(pos, WirelessActionMessage.GATHER, null, "");
    }

    public void onInfo(WirelessInfoMessage m, int x, int y) {
        this.info = m;
        int max = Math.max(0, m.avail.size() - WL_ROWS);
        if (scroll > max) scroll = max;
        rebuildPassBoxes(x, y);
    }

    public void clearPassBoxes() {
        for (int i = 0; i < passBoxes.length; i++) {
            if (passBoxes[i] != null) {
                host.removePassBox(passBoxes[i]);
                passBoxes[i] = null;
            }
        }
    }

    public void rebuildPassBoxes(int x, int y) {
        clearPassBoxes();
        if (info == null) return;

        int n = Math.min(WL_ROWS, info.avail.size() - scroll);
        for (int i = 0; i < n; i++) {
            WirelessInfoMessage.Entry e = info.avail.get(scroll + i);
            if (!e.encrypted()) continue;
            int by = y + WL_LIST_Y + i * WL_ROW_H + 4;

            EditBox box = new EditBox(host.font(), x + WL_ROW_X + 72, by, 44, 9, Component.literal("pass"));
            box.setMaxLength(32);
            box.setBordered(false);
            box.setTextColor(0xFFFFFF);
            box.setFormatter((s, idx) -> FormattedCharSequence.forward("*".repeat(s.length()), Style.EMPTY));
            passBoxes[i] = box;
            host.addPassBox(box);
        }
    }

    private String passOf(int row) {
        return passBoxes[row] == null ? "" : passBoxes[row].getValue();
    }

    public void render(GuiGraphics gg, int x, int y, float partialTick) {
        TechUIDraw.panel(gg, x, y, PW, PH);

        TechUIDraw.icon(gg, logo, x + WL_LOGO_X, y + WL_LOGO_Y, 16, TechUIDraw.breathe(partialTick), 32);

        drawRow(gg, x, y + WL_CONN_ROW_Y,
                info != null && info.linked ? info.linkedNode.name() : tr("gui.academy.ui.not_connected"),
                info != null && info.linked);

        if (info != null) {
            int n = Math.min(WL_ROWS, info.avail.size() - scroll);
            for (int i = 0; i < n; i++) {
                WirelessInfoMessage.Entry e = info.avail.get(scroll + i);
                drawRow(gg, x, y + WL_LIST_Y + i * WL_ROW_H, e.name(), false);
                if (e.encrypted()) {
                    TechUIDraw.icon(gg, ICON_KEY, x + WL_ROW_X + 58, y + WL_LIST_Y + i * WL_ROW_H + 2, 12, 0.9f, 24);
                }
            }
        }

        TechUIDraw.icon(gg, BTN_UP, x + arrowX(), y + WL_LIST_Y, WL_ARROW, 0.9f, 32);
        TechUIDraw.icon(gg, BTN_DOWN, x + arrowX(), y + arrowDownY(), WL_ARROW, 0.9f, 32);
    }

    public void renderText(GuiGraphics gg, int ox, int oy) {
        Font font = host.font();
        gg.drawString(font, tr("gui.academy.ui.connected"), ox + WL_ROW_X, oy + WL_CONNECTED_LABEL_Y, HEADER, false);
        gg.drawString(font, tr("gui.academy.ui.available"), ox + WL_ROW_X, oy + WL_AVAIL_LABEL_Y, HEADER, false);

        String cn = info != null && info.linked ? info.linkedNode.name() : tr("gui.academy.ui.not_connected");
        gg.drawString(font, cn, ox + WL_ROW_X + 20, oy + WL_CONN_ROW_Y + 4, VALUE, false);
        if (info != null) {
            int n = Math.min(WL_ROWS, info.avail.size() - scroll);
            for (int i = 0; i < n; i++) {
                gg.drawString(font, info.avail.get(scroll + i).name(),
                        ox + WL_ROW_X + 20, oy + WL_LIST_Y + i * WL_ROW_H + 4, VALUE, false);
            }
        }
    }

    private void drawRow(GuiGraphics gg, int x, int y, String name, boolean connected) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        gg.setColor(1f, 1f, 1f, connected ? 1.0f : 0.6f);
        gg.blit(ELEM_BG, x + WL_ROW_X, y, WL_ROW_W, 16, 0f, 0f, 300, 32, 300, 32);
        gg.setColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
        TechUIDraw.icon(gg, ICON_DEVICE, x + WL_ROW_X + 4, y + 2, 12, connected ? 1.0f : 0.6f, 24);
        TechUIDraw.icon(gg, connected ? ICON_CONNECTED : ICON_UNCONNECTED,
                x + WL_ROW_X + WL_ROW_W - 18, y + 2, 12, connected ? 1.0f : 0.6f, 24);
    }

    private int arrowX() {
        return PW - WL_ARROW - 8;
    }

    private int arrowDownY() {
        return PH - WL_ARROW - 12;
    }

    public boolean mouseClicked(double mx, double my, int button, int x, int y) {

        for (EditBox b : passBoxes) {
            if (b != null && b.mouseClicked(mx, my, button)) {
                host.focusPassBox(b);
                return true;
            }
        }

        if (info != null && info.linked
                && TechUIDraw.inRect(mx, my, x + WL_ROW_X + WL_ROW_W - 18, y + WL_CONN_ROW_Y + 2, 12, 12)) {
            WirelessActionMessage.send(pos, WirelessActionMessage.DISCONNECT, null, "");
            return true;
        }
        if (info != null) {

            int n = Math.min(WL_ROWS, info.avail.size() - scroll);
            for (int i = 0; i < n; i++) {
                if (TechUIDraw.inRect(mx, my, x + WL_ROW_X + WL_ROW_W - 18,
                        y + WL_LIST_Y + i * WL_ROW_H + 2, 12, 12)) {
                    WirelessActionMessage.send(pos, WirelessActionMessage.CONNECT,
                            info.avail.get(scroll + i).pos(), passOf(i));
                    return true;
                }
            }

            if (TechUIDraw.inRect(mx, my, x + arrowX(), y + WL_LIST_Y, WL_ARROW, WL_ARROW)) {
                if (scroll > 0) { scroll--; rebuildPassBoxes(x, y); }
                return true;
            }
            if (TechUIDraw.inRect(mx, my, x + arrowX(), y + arrowDownY(), WL_ARROW, WL_ARROW)) {
                if (scroll + WL_ROWS < info.avail.size()) { scroll++; rebuildPassBoxes(x, y); }
                return true;
            }
        }
        return false;
    }

    public boolean keyPressed(int key, int scan, int mods) {
        for (int i = 0; i < passBoxes.length; i++) {
            EditBox b = passBoxes[i];
            if (b != null && b.isFocused()) {
                if (key == 257 || key == 335) {
                    if (info != null && scroll + i < info.avail.size()) {
                        WirelessActionMessage.send(pos, WirelessActionMessage.CONNECT,
                                info.avail.get(scroll + i).pos(), b.getValue());
                    }
                    return true;
                }
                if (b.keyPressed(key, scan, mods)) return true;
            }
        }
        return false;
    }

    public boolean isInside(double mx, double my, int x, int y) {
        return TechUIDraw.inRect(mx, my, x, y, PW, PH);
    }

    private static String tr(String fullKey) {
        return Component.translatable(fullKey).getString();
    }
}
