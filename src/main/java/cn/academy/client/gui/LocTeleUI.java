package cn.academy.client.gui;

import cn.academy.ACSounds;
import cn.academy.ability.vanilla.teleporter.LocTeleportData;
import cn.academy.ability.vanilla.teleporter.Location;
import cn.academy.ability.vanilla.teleporter.skill.LocationTeleport;
import cn.lambdalib2.auxgui.AuxGui;
import cn.lambdalib2.cgui.CGui;
import cn.lambdalib2.cgui.Widget;
import cn.lambdalib2.cgui.component.Component;
import cn.lambdalib2.cgui.component.DrawTexture;
import cn.lambdalib2.cgui.component.ElementList;
import cn.lambdalib2.cgui.component.Outline;
import cn.lambdalib2.cgui.component.TextBox;
import cn.lambdalib2.cgui.component.Tint;
import cn.lambdalib2.cgui.component.Transform.HeightAlign;
import cn.lambdalib2.cgui.component.Transform.WidthAlign;
import cn.lambdalib2.cgui.event.FrameEvent;
import cn.lambdalib2.datapart.EntityData;
import cn.lambdalib2.render.font.Fonts;
import cn.lambdalib2.render.font.IFont;
import cn.lambdalib2.render.font.IFont.FontAlign;
import cn.lambdalib2.render.font.IFont.FontOption;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.util.Color;
import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.GameTimer;
import cn.lambdalib2.util.HudUtils;
import cn.lambdalib2.util.MathUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class LocTeleUI extends AuxGui {

    private static WeakReference<LocTeleUI> CURRENT = new WeakReference<>(null);

    private static final double TYPING_GRACE = 1.0;

    public static void open() {
        LocTeleUI cur = CURRENT.get();
        if (cur != null && !cur.disposed) {
            return;
        }
        if (!EntityData.isLocalPlayerReady()) {
            return;
        }
        AuxGui.register(new LocTeleUI());
    }

    public static void refreshCurrent() {
        LocTeleUI ui = CURRENT.get();
        if (ui != null && !ui.disposed && ui.attachedToCurrentPlayer()
                && EntityData.isReady(ui.player)) {
            ui.updateList();
        }
    }

    private static final double ELEM_TIME_STEP = 0.06;
    private static final int[] TEXT_NORMAL = {0xc1, 0xcf, 0xd5};
    private static final int[] TEXT_DISABLED = {0xa2, 0xa2, 0xa2};
    private static final int[] TEXT_INPUT = {0xa4, 0xd4, 0xe9};
    private static final Color ROW_IDLE = new Color(255, 255, 255, 25);
    private static final Color ROW_SELECTED = new Color(255, 255, 255, 102);

    private final CGui gui = new CGui();
    private final Player player;
    private final LocTeleportData data;

    private final Widget root, menu, info;
    private final MessageTab tab;

    private List<Location> locs = List.of();
    private List<List<String>> rowLines = List.of();
    private int sel = 0;
    private boolean firstBuild = true;

    private final StringBuilder pending = new StringBuilder();

    private double addEnterTime = -1;

    private boolean selOnAdd = false;

    private boolean wasTyping = false;

    private static final class Blend {
        final double init = GameTimer.getTime();
        final double offset, length;

        Blend(double offset, double length) {
            this.offset = offset;
            this.length = length;
        }

        float alpha() {
            return (float) MathUtils.clampd(0, 1, (GameTimer.getTime() - init - offset) / length);
        }
    }

    private LocTeleUI() {
        foreground = true;

        player = Minecraft.getInstance().player;
        data = LocTeleportData.of(player);

        root = new Widget();
        root.transform.setSize(0, 0).setCenteredAlign();
        root.transform.scale = 0.24f;
        root.transform.doesDraw = true;

        menu = new Widget();
        menu.transform.setSize(442, 530).setPos(20, -179.17f);
        menu.addComponent(new Outline(new Color(122, 146, 156, 158)));
        menu.addComponent(new Tint(new Color(53, 53, 53, 128), new Color(53, 53, 53, 128), false));
        Blend menuBlend = new Blend(0, 0.4);
        menu.listen(FrameEvent.class, (w, e) -> w.transform.height = menuBlend.alpha() * 530);
        root.addWidget("menu", menu);

        info = new Widget();
        info.transform.setSize(335.83f, 191.94f).setPos(-23.33f, -179.44f)
                .setAlign(WidthAlign.RIGHT, HeightAlign.TOP);
        info.addComponent(new Outline(new Color(122, 146, 156, 158)));
        info.addComponent(new Tint(new Color(53, 53, 53, 128), new Color(53, 53, 53, 128), false));
        tab = new MessageTab();
        info.addComponent(tab);
        info.transform.doesDraw = false;
        root.addWidget("info", info);

        gui.addWidget("root", root);
        updateList();

        MinecraftForge.EVENT_BUS.register(this);
        CURRENT = new WeakReference<>(this);
    }

    @Override
    public void onDisposed() {
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    private boolean attachedToCurrentPlayer() {
        Player cur = Minecraft.getInstance().player;
        return cur != null && cur == player;
    }

    @Override
    public void draw(GuiGraphics gg, float width, float height) {
        if (!attachedToCurrentPlayer()) {
            dispose();
            return;
        }

        boolean typing = typingActive();
        if (typing && !wasTyping) {
            swallowAllVanillaKeys();
        }
        wasTyping = typing;

        gui.resize(width, height);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        gui.draw(gg.pose(), -1, -1);
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    private boolean typingActive() {
        return addSelected() && addEnterTime >= 0
                && GameTimer.getTime() - addEnterTime >= TYPING_GRACE;
    }

    private static void swallowVanillaKey(int key, int scanCode) {
        for (net.minecraft.client.KeyMapping km : Minecraft.getInstance().options.keyMappings) {
            if (km.matches(key, scanCode)) {
                km.setDown(false);
                while (km.consumeClick()) {  }
            }
        }
    }

    private static void swallowAllVanillaKeys() {
        for (net.minecraft.client.KeyMapping km : Minecraft.getInstance().options.keyMappings) {
            km.setDown(false);
            while (km.consumeClick()) {  }
        }
    }

    private boolean addSelected() {
        return sel >= locs.size();
    }

    private boolean inputActive() {
        Minecraft mc = Minecraft.getInstance();
        if (disposed || mc.player == null || mc.screen != null) {
            return false;
        }
        if (!attachedToCurrentPlayer()) {
            dispose();
            return false;
        }
        return EntityData.isReady(player);
    }

    @SubscribeEvent(receiveCanceled = true)
    public void onScroll(InputEvent.MouseScrollingEvent e) {
        if (!inputActive()) return;
        setSel(sel + (e.getScrollDelta() > 0 ? -1 : 1));
        e.setCanceled(true);
    }

    @SubscribeEvent(receiveCanceled = true)
    public void onMouseButton(InputEvent.MouseButton.Pre e) {
        if (!inputActive() || e.getAction() != GLFW.GLFW_PRESS) return;
        if (addSelected()) return;
        Location loc = locs.get(sel);
        if (e.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (LocationTeleport.getPerformStat(player, loc) == null) {
                Minecraft.getInstance().getSoundManager().play(
                        SimpleSoundInstance.forUI(ACSounds.TP_MOVE_PLAYER.get(), 1.0f, 1.0f));
                NetworkMessage.sendToServer(LocationTeleport.Net.INSTANCE,
                        LocationTeleport.Net.MSG_PERFORM, player, loc.id);
                dispose();
            }
            e.setCanceled(true);
        } else if (e.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            NetworkMessage.sendToServer(LocationTeleport.Net.INSTANCE,
                    LocationTeleport.Net.MSG_REMOVE, player, loc.id);
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onKey(InputEvent.Key e) {
        if (!inputActive()) return;
        if (e.getAction() != GLFW.GLFW_PRESS && e.getAction() != GLFW.GLFW_REPEAT) return;
        int key = e.getKey();

        if (typingActive()) {

            handleTyping(key, (e.getModifiers() & GLFW.GLFW_MOD_SHIFT) != 0);
            swallowVanillaKey(key, e.getScanCode());
            return;
        }
        if (addSelected()) {
            return;
        }

        if (Minecraft.getInstance().options.keyInventory.matches(key, e.getScanCode())) {
            swallowVanillaKey(key, e.getScanCode());
            dispose();
        } else if (key == GLFW.GLFW_KEY_ESCAPE) {
            dispose();
        }
    }

    private void handleTyping(int key, boolean shift) {
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            confirmAdd();
        } else if (key == GLFW.GLFW_KEY_BACKSPACE) {
            if (pending.length() > 0) pending.setLength(pending.length() - 1);
        } else if (pending.length() < 16) {
            if (key >= GLFW.GLFW_KEY_A && key <= GLFW.GLFW_KEY_Z) {
                char c = (char) ('a' + (key - GLFW.GLFW_KEY_A));
                pending.append(shift ? Character.toUpperCase(c) : c);
            } else if (key >= GLFW.GLFW_KEY_0 && key <= GLFW.GLFW_KEY_9) {
                pending.append((char) ('0' + (key - GLFW.GLFW_KEY_0)));
            } else if (key >= GLFW.GLFW_KEY_KP_0 && key <= GLFW.GLFW_KEY_KP_9) {
                pending.append((char) ('0' + (key - GLFW.GLFW_KEY_KP_0)));
            } else if (key == GLFW.GLFW_KEY_SPACE) {
                pending.append(' ');
            } else if (key == GLFW.GLFW_KEY_MINUS) {
                pending.append(shift ? '_' : '-');
            }
        }
    }

    private void confirmAdd() {
        String name = pending.toString().trim();
        if (name.isEmpty()) return;
        NetworkMessage.sendToServer(LocationTeleport.Net.INSTANCE,
                LocationTeleport.Net.MSG_ADD, player, name);
        pending.setLength(0);
    }

    private void updateList() {
        boolean wasAdd = !firstBuild && selOnAdd;

        Widget old = menu.getWidget("list");
        if (old != null) {
            old.dispose();
        }

        locs = data.locations();
        rowLines = new ArrayList<>();

        Widget list = new Widget();
        list.transform.setSize(442, 509.78f).setPos(0, 18);

        ElementList el = new ElementList();
        el.spacing = 2;
        int n = 0;
        for (Location l : locs) {
            el.addWidget(newElem(l, n++));
        }
        el.addWidget(newAdd(n));
        list.addComponent(el);

        menu.addWidget("list", list);

        setSel(firstBuild ? 0 : (wasAdd ? locs.size() : Math.min(sel, locs.size())));
        firstBuild = false;
    }

    private void setSel(int to) {
        sel = MathUtils.clampi(0, locs.size(), to);
        boolean nowAdd = addSelected();

        if (nowAdd && !selOnAdd) {
            addEnterTime = GameTimer.getTime();
        } else if (!nowAdd) {
            addEnterTime = -1;
        }

        selOnAdd = nowAdd;

        tab.updateText(sel < rowLines.size() ? rowLines.get(sel) : List.of());
    }

    private Widget newElem(Location loc, int idx) {
        Widget ret = new Widget();
        ret.transform.setSize(442, 80);
        ret.transform.doesDraw = true;

        String stat = LocationTeleport.getPerformStat(player, loc);
        float cp = LocationTeleport.getConsumption(player, loc)[1];

        List<String> lines = new ArrayList<>();
        lines.add(loc.dim);
        lines.add(String.format("(%.0f, %.0f, %.0f)", loc.x, loc.y, loc.z));
        lines.add(String.format("%.0f CP", cp));
        if (stat != null) {
            lines.add(I18n.get(stat));
        }
        rowLines.add(lines);
        hookRowBack(ret, idx);

        Widget text = new Widget();
        text.transform.setSize(377.78f, 55.56f).setPos(45.28f, 16.13f);
        text.transform.doesListenKey = false;
        int[] c = stat == null ? TEXT_NORMAL : TEXT_DISABLED;
        TextBox tb = textBox(43, new Color(c[0], c[1], c[2], 0));
        tb.setContent(loc.name);
        text.addComponent(tb);
        Blend textBlend = new Blend(idx * ELEM_TIME_STEP + 0.1, 0.1);
        text.listen(FrameEvent.class, (w, e) -> tb.option.color.a = (int) (textBlend.alpha() * 255));
        ret.addWidget("text", text);

        if (stat == null) {
            ret.addWidget("icon_teleport", passiveIcon("gui/icon_location_on", 335, idx, 0.03));
        }
        ret.addWidget("icon_remove", passiveIcon("gui/icon_clear", 375, idx, 0.05));

        return ret;
    }

    private Widget newAdd(int idx) {
        Widget ret = new Widget();
        ret.transform.setSize(442, 80);
        ret.transform.doesDraw = true;

        rowLines.add(List.of(
                player.level().dimension().location().toString(),
                String.format("(%.0f, %.0f, %.0f)", player.getX(), player.getY(), player.getZ())));
        hookRowBack(ret, idx);

        Widget input = new Widget();
        input.transform.setSize(280.56f, 55.56f).setPos(45.28f, 19.70f);
        TextBox tb = textBox(43, new Color(TEXT_INPUT[0], TEXT_INPUT[1], TEXT_INPUT[2], 0));
        input.addComponent(tb);
        Blend blend = new Blend(idx * ELEM_TIME_STEP, 0.2);
        input.listen(FrameEvent.class, (w, e) -> {
            boolean editing = typingActive();
            tb.option.color.a = (int) (blend.alpha() * (editing ? 0.8 : 0.4) * 255);
            if (editing) {
                boolean blink = GameTimer.getTime() % 1.0 < 0.5;
                tb.setContent(pending + (blink ? "_" : ""));
            } else {
                tb.setContent(pending.length() == 0 ? "Add..." : pending.toString());
            }
        });
        ret.addWidget("input_text", input);

        ret.addWidget("icon_confirm", passiveIcon("gui/check", 375.83f, idx, 0.0));

        return ret;
    }

    private void hookRowBack(Widget row, int idx) {
        row.listen(FrameEvent.class, (w, e) -> {
            Colors.bindToGL(idx == sel ? ROW_SELECTED : ROW_IDLE);
            HudUtils.colorRect(0, 0, w.transform.width, w.transform.height);
        });
    }

    private static TextBox textBox(float size, Color color) {
        TextBox tb = new TextBox(new FontOption(size, FontAlign.LEFT, color));
        tb.font = Fonts.get("AC_Normal");
        tb.heightAlign = HeightAlign.CENTER;
        tb.localized = false;
        return tb;
    }

    private Widget passiveIcon(String tex, float x, int idx, double timeOffset) {
        Widget icon = new Widget();
        icon.transform.setSize(33.33f, 33.33f).setPos(x, 29.92f);
        icon.transform.doesListenKey = false;
        DrawTexture dt = new DrawTexture(cn.academy.Resources.getTexture(tex));
        dt.color = new Color(TEXT_NORMAL[0], TEXT_NORMAL[1], TEXT_NORMAL[2], 0);
        icon.addComponent(dt);
        Blend blend = new Blend(idx * ELEM_TIME_STEP + timeOffset, 0.1);
        icon.listen(FrameEvent.class, (w, e) -> dt.color.a = (int) (0.7 * blend.alpha() * 255));
        return icon;
    }

    private final class MessageTab extends Component {

        private static final int LINE_HEIGHT = 42, MARGIN_X = 20, MARGIN_Y = 20;

        private final IFont font = Fonts.get("AC_Normal");
        private final FontOption fo = new FontOption(40, FontAlign.RIGHT,
                new Color(TEXT_NORMAL[0], TEXT_NORMAL[1], TEXT_NORMAL[2], 255));
        private List<String> text = List.of();

        MessageTab() {
            super("MessageTab");
            listen(FrameEvent.class, (w, e) -> {
                for (int i = 0; i < text.size(); i++) {
                    font.draw(text.get(i), w.transform.width - MARGIN_X,
                            MARGIN_Y + LINE_HEIGHT * i, fo);
                }
            });
        }

        void updateText(List<String> t) {
            text = t;
            if (t.isEmpty()) {
                info.transform.doesDraw = false;
            } else {
                info.transform.doesDraw = true;
                float max = 0;
                for (String s : t) {
                    max = Math.max(max, font.getTextWidth(s, fo));
                }
                info.transform.setSize(max + MARGIN_X * 2, t.size() * LINE_HEIGHT + MARGIN_Y * 2);
                info.dirty = true;
            }
        }
    }
}
