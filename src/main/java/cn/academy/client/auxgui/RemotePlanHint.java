package cn.academy.client.auxgui;

import cn.academy.ability.vanilla.mentalout.advanced.MentalMastery;
import cn.academy.ability.Skill;
import cn.academy.ability.vanilla.mentalout.WideCastable;
import cn.academy.client.gui.TechUIDraw;
import cn.academy.datapart.RemoteData;
import cn.academy.item.RemoteControlItem;
import cn.lambdalib2.auxgui.AuxGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RemotePlanHint extends AuxGui {

    private static final RemotePlanHint INSTANCE = new RemotePlanHint();

    public static void init() {
        AuxGui.register(INSTANCE);
    }

    private static final int ICON = 16, GAP = 2;

    private static final int NAME_Y = 59, NO_HEALTH_BAR_OFFSET = 14;

    private static final int LIFT = 4;

    private RemotePlanHint() {
        foreground = false;
    }

    @Override
    public void draw(GuiGraphics gg, float width, float height) {
        Minecraft mc = Minecraft.getInstance();
        Player p = mc.player;
        if (p == null || mc.options.hideGui || mc.screen != null) {
            return;
        }

        if (!(p.getMainHandItem().getItem() instanceof RemoteControlItem)) {
            return;
        }
        RemoteData rd = RemoteData.get(p);
        if (rd == null) {
            return;
        }
        RemoteData.Program prog = rd.book().getCurrent();

        Skill[] skills = new Skill[RemoteData.MAX_SLOTS];
        int[] cmds = new int[RemoteData.MAX_SLOTS];
        int n = 0;

        for (int i = 0; i < MentalMastery.usableSlots(Minecraft.getInstance().player); ++i) {
            Skill s = prog.getSkill(i);
            if (s != null) {
                skills[n] = s;
                cmds[n] = prog.getCommand(i);
                ++n;
            }
        }
        if (n == 0) {
            return;
        }

        int total = n * ICON + (n - 1) * GAP;
        int x = (int) ((width - total) / 2);
        int y = (int) height - NAME_Y - ICON - LIFT
                + (mc.gameMode != null && !mc.gameMode.canHurtPlayer() ? NO_HEALTH_BAR_OFFSET : 0);

        for (int i = 0; i < n; ++i) {
            int ix = x + i * (ICON + GAP);

            gg.fill(ix - 1, y - 1, ix + ICON + 1, y + ICON + 1, 0x70000000);
            TechUIDraw.icon(gg, skills[i].getHintIcon(), ix, y, ICON, 1.0f, 32);

            if (skills[i] instanceof WideCastable wc && wc.wideNeedsCommand()) {
                net.minecraft.resources.ResourceLocation oi = wc.wideOptionIcon(cmds[i]);
                if (oi != null) {
                    TechUIDraw.icon(gg, oi, ix + ICON - 8, y + ICON - 8, 8, 1.0f, 32);
                }
            }
        }
    }
}
