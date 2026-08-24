package cn.academy.client.auxgui;

import cn.academy.ability.vanilla.mentalout.advanced.MentalMastery;
import cn.academy.ability.Skill;
import cn.academy.ability.vanilla.mentalout.WideCastable;
import cn.academy.ability.vanilla.mentalout.skill.ForcedControl;
import cn.academy.config.AbilityConfig;
import cn.academy.datapart.RemoteData;
import cn.academy.item.RemoteControlItem;
import cn.lambdalib2.auxgui.AuxGui;
import cn.lambdalib2.input.KeyHandler;
import cn.lambdalib2.input.KeyManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@OnlyIn(Dist.CLIENT)
public final class QuickPlanSelector extends AuxGui {

    private static final String KEY_NAME = "mo_remote_quick_plan";

    private static final int GOLD = 0xFFFFCD46;
    private static final int GREY = 0xFFB0B8C0;
    private static final int DIM = 0xFF5A6068;

    private static final int CELL = 22, GAP = 3;

    private static final int PREV_BOX = 20, PREV_ICON = 16, PREV_SUB = 8, PREV_GAP = 2;

    private static final int ACTION_BAR_Y = 68;

    private static QuickPlanSelector showing;

    private int pending;

    private QuickPlanSelector(int start) {
        pending = start;
        consistent = false;
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new QuickPlanEvents());
    }

    public static boolean isShowing() {
        return showing != null && !showing.disposed;
    }

    public static boolean holdingRemote(Player p) {
        return RemoteControlItem.readyInMainHand(p);
    }

    public static boolean holdingRemoteEitherHand(Player p) {
        return p != null && (RemoteControlItem.readyInMainHand(p)
                || (p.getOffhandItem().getItem() instanceof RemoteControlItem
                    && cn.academy.ability.vanilla.mentalout.passiveskill.WideCast.unlocked(p)));
    }

    private static void show() {
        if (isShowing()) {
            return;
        }
        RemoteData rd = RemoteData.get(Minecraft.getInstance().player);
        if (rd == null) {
            return;
        }
        showing = new QuickPlanSelector(rd.book().getCurrentID());
        AuxGui.register(showing);
    }

    private static void confirm() {
        if (!isShowing()) {
            return;
        }
        RemoteData rd = RemoteData.get(Minecraft.getInstance().player);
        if (rd != null && rd.book().getCurrentID() != showing.pending) {
            rd.switchFromClient(showing.pending);
        }
        close();
    }

    private static void close() {
        if (showing != null) {
            showing.dispose();
            showing = null;
        }
    }

    private static void scroll(double delta) {
        if (!isShowing()) {
            return;
        }

        int step = delta > 0 ? -1 : 1;
        showing.pending = Math.floorMod(showing.pending + step, RemoteData.MAX_PROGRAMS);
    }

    @Override
    public void draw(GuiGraphics gg, float width, float height) {
        Player p = Minecraft.getInstance().player;
        RemoteData rd = RemoteData.get(p);
        if (rd == null) {
            return;
        }
        int stripW = RemoteData.MAX_PROGRAMS * CELL + (RemoteData.MAX_PROGRAMS - 1) * GAP;
        int x0 = (int) (width / 2) - stripW / 2;
        int y = (int) height - ACTION_BAR_Y - CELL / 2;

        net.minecraft.client.gui.Font font = Minecraft.getInstance().font;

        RemoteData.Book book = rd.book();
        RemoteData.Program sel = book.getProgram(pending);

        int usable = MentalMastery.usableSlots(Minecraft.getInstance().player);
        int n = 0;
        for (int i = 0; i < usable; ++i) {
            if (sel.getSkill(i) != null) {
                n++;
            }
        }
        if (n > 0) {
            int iw = n * PREV_BOX + (n - 1) * PREV_GAP;
            int ix = (int) (width / 2) - iw / 2;
            int iy = y - PREV_BOX - 6;
            int k = 0;
            for (int i = 0; i < usable; ++i) {
                Skill s = sel.getSkill(i);
                if (s == null) {
                    continue;
                }
                int bx = ix + k * (PREV_BOX + PREV_GAP);
                gg.fill(bx, iy, bx + PREV_BOX, iy + PREV_BOX, 0xC0000000);
                frame(gg, bx, iy, PREV_BOX, PREV_BOX, GREY);
                int inset = (PREV_BOX - PREV_ICON) / 2;
                cn.academy.client.gui.TechUIDraw.icon(
                        gg, s.getHintIcon(), bx + inset, iy + inset, PREV_ICON, 1.0f, 32);

                if (s instanceof WideCastable wc && wc.wideNeedsCommand()) {
                    net.minecraft.resources.ResourceLocation oi =
                            wc.wideOptionIcon(sel.getCommand(i));
                    if (oi != null) {
                        cn.academy.client.gui.TechUIDraw.icon(gg, oi,
                                bx + PREV_BOX - PREV_SUB - 1, iy + PREV_BOX - PREV_SUB - 1,
                                PREV_SUB, 1.0f, 32);
                    }
                }
                k++;
            }
        }

        for (int i = 0; i < RemoteData.MAX_PROGRAMS; ++i) {
            int cx = x0 + i * (CELL + GAP);
            boolean on = i == pending;
            boolean cur = i == book.getCurrentID();
            gg.fill(cx, y, cx + CELL, y + CELL, on ? 0xC0000000 : 0x80000000);
            frame(gg, cx, y, CELL, CELL, on ? GOLD : (cur ? GREY : DIM));
            gg.drawCenteredString(font, String.valueOf(i + 1),
                    cx + CELL / 2, y + (CELL - 8) / 2, on ? GOLD : (cur ? GREY : DIM));
        }
    }

    private static void frame(GuiGraphics gg, int x, int y, int w, int h, int color) {
        gg.fill(x, y, x + w, y + 1, color);
        gg.fill(x, y + h - 1, x + w, y + h, color);
        gg.fill(x, y + 1, x + 1, y + h - 1, color);
        gg.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    private static final MiddleKey HANDLER = new MiddleKey();

    public static final class QuickPlanEvents {

        @SubscribeEvent
        public void onRemoteTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            boolean want = RemoteControlItem.readyInMainHand(mc.player);

            boolean has = KeyManager.dynamic.getKeyID(HANDLER) != -1;
            if (want != has) {
                if (want) {
                    KeyManager.dynamic.addKeyHandler(KEY_NAME, KeyManager.MOUSE_MIDDLE, HANDLER);
                } else {
                    KeyManager.dynamic.removeKeyHandler(KEY_NAME);
                }
            }

            if (isShowing() && (!want || mc.screen != null)) {
                close();
            }
        }

        @SubscribeEvent
        public void onRemoteMouseBtn(InputEvent.MouseButton.Pre event) {
            if (event.getButton() != org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen != null || mc.player == null || !holdingRemoteEitherHand(mc.player)) {
                return;
            }
            event.setCanceled(true);
        }

        @SubscribeEvent
        public void onRemoteScroll(InputEvent.MouseScrollingEvent event) {
            if (!isShowing()) {
                return;
            }
            scroll(event.getScrollDelta());
            event.setCanceled(true);
        }
    }

    private static final class MiddleKey extends KeyHandler {

        @Override
        public void onKeyDown() {
            if (isShowing()) {
                confirm();
            } else {
                show();
            }
        }
    }
}
