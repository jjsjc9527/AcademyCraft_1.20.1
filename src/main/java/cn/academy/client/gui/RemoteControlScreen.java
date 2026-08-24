package cn.academy.client.gui;

import cn.academy.ability.vanilla.mentalout.advanced.MentalMastery;
import cn.academy.ability.Skill;
import cn.academy.ability.vanilla.mentalout.WideCastable;
import cn.academy.ability.vanilla.mentalout.passiveskill.WideCast;
import cn.academy.datapart.AbilityData;
import cn.academy.datapart.RemoteData;
import cn.lambdalib2.datapart.EntityData;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class RemoteControlScreen extends Screen {

    private static final int GOLD = 0xFFFFCD46;
    private static final int GREY = 0xFFB0B8C0;
    private static final int DIM = 0xFF5A6068;

    private static final int CELL = 24, GAP = 4;

    private static final int LABEL_W = 46;
    private static final int GRID_W = RemoteData.MAX_SLOTS * CELL + (RemoteData.MAX_SLOTS - 1) * GAP;
    private static final int GRID_H = RemoteData.MAX_PROGRAMS * CELL + (RemoteData.MAX_PROGRAMS - 1) * GAP;
    private static final int PAD = 10;

    private static final int HEAD_H = 32;

    private static final int FOOT_H = 0;

    private static final int ENT_GAP = 10;

    private static final float PREVIEW_YAW_DEG = 45f;

    private static final float PREVIEW_PITCH_DEG = 15f;

    private static final int CARD_W = 46, CARD_NAME_H = 11, CARD_MODEL_H = 49;
    private static final int CARD_H = CARD_NAME_H + CARD_MODEL_H;

    private static final int ENT_PER_ROW = 4;
    private static final int ALLY_PANEL_W =
            PAD + ENT_PER_ROW * CARD_W + (ENT_PER_ROW - 1) * ENT_GAP + PAD;

    private static final int ALLY_TITLE_Y = 5;

    private static final int ALLY_TOP = ALLY_TITLE_Y + 9 + 6;

    private static final int PANEL_GAP = 8;

    private static final int ALLY_BAR_W = 4;

    private static final int ALLY_BAR_GAP = 3;

    private static final int ALLY_BAR_MIN = 12;

    private static final int ALLY_SCROLL_STEP = (CARD_H + ENT_GAP) / 2;

    private static final ResourceLocation NOISE_TEX = TechUIDraw.tex("cam_noise");
    private static final int NOISE_TEX_W = 256, NOISE_FRAME = 32,
            NOISE_FRAMES = NOISE_TEX_W / NOISE_FRAME;

    private static final double NOISE_FRAME_TIME = 0.055;

    private static final int JAM_TINT = 0x386A2AB4;

    private static final int JAM_LINE = 0x66C8A0FF;

    private static final double JAM_SPIN_DEG = 100.0;

    private static final float JAM_STRETCH = 1.59f;

    private static final int JAM_HALF = 56;

    private static final int JAM_STEP = 6;

    private static Boolean noiseTexOk;

    private static final float Z_OVERLAY = 100f, Z_DISTORT = 125f, Z_EYES = 150f;

    private static final int[][] NAME_OUTLINE = {{1, 1}};

    private static final ResourceLocation EYE_TEX = TechUIDraw.tex("watch_eye");
    private static final int EYE_TEX_W = 288, EYE_FRAME = 32, EYE_FRAMES = EYE_TEX_W / EYE_FRAME;

    private static final int EYES_PER_SIDE = 6;

    private static final float EYE_SIZE = 12f;

    private static final float EYE_TILT_DEG = 35f;

    private static final float EYE_JITTER_DEG = 30f;

    private static final float EYE_INSET = 1f;

    private static final double EYE_FRAME_TIME = 0.11;

    private static final int EYE_PHASE_STEP = 2;

    private static final int LEVER_W = 26, LEVER_H = 12, LEVER_KNOB = 10;

    private static final int LEVER_COL_W = LEVER_W + 4;

    private static final int INNER_W_LEVER = LABEL_W + 6 + GRID_W + 6 + LEVER_COL_W;
    private static final int INNER_W_BARE  = LABEL_W + 6 + GRID_W;
    private static final int PANEL_H = PAD + HEAD_H + GRID_H + FOOT_H + PAD;

    private static final int PICK_CELL = 22, PICK_PER_ROW = 4, PICK_MARGIN = 4;

    private static final int SHIFT_STEP = 10;

    private final Player player;

    private int px, py, panelW, x0, gridX, gridY, spinY, leverX;

    private boolean showLever;

    private int innerW() {
        return showLever ? INNER_W_LEVER : INNER_W_BARE;
    }

    private int allyPx;

    private float uiScale = 1f;

    private static final int SCREEN_MARGIN = 4;

    private static final float MIN_UI_SCALE = 0.35f;

    private int vw() {
        return (int) (width / uiScale);
    }

    private int vh() {
        return (int) (height / uiScale);
    }

    private int vpos(double screen) {
        return (int) (screen / uiScale);
    }

    private boolean showAllies;

    private int allyScroll;

    private boolean allyDragging;

    private int allyDragGrab;

    private java.util.UUID denyId;
    private long denyAt;

    private static final long DENY_MS = 1500L;

    private int pickSlot = -1;

    private int pickStage = 0;

    private Skill pendingSkill = null;

    private int pickX, pickY;

    private RemoteControlScreen() {
        super(Component.translatable("gui.academy.remote_control.title"));
        player = Minecraft.getInstance().player;
    }

    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        Player p = mc.player;
        if (p == null || !EntityData.isReady(p) || mc.screen != null) {
            return;
        }
        if (RemoteData.get(p) == null || AbilityData.get(p) == null) {
            return;
        }
        mc.setScreen(new RemoteControlScreen());
    }

    public static boolean tryOpenFromKey(Player p) {
        if (!cn.academy.item.RemoteControlItem.readyInMainHand(p)) {
            return false;
        }
        open();
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private RemoteData data() {
        return RemoteData.get(player);
    }

    private float wideExp() {
        AbilityData ad = AbilityData.get(player);
        return ad == null ? 0f : ad.getSkillExp(WideCast.INSTANCE);
    }

    private boolean hasElsewhereInRow(RemoteData.Program prog, Skill skill) {
        for (int i = 0; i < RemoteData.MAX_SLOTS; ++i) {
            if (i != pickSlot && prog.getSkill(i) == skill) {
                return true;
            }
        }
        return false;
    }

    private List<Skill> pickable() {
        List<Skill> out = new ArrayList<>();
        AbilityData ad = AbilityData.get(player);
        if (ad == null || !ad.hasCategory()) {
            return out;
        }
        RemoteData rd = data();
        RemoteData.Program cur = rd == null ? null : rd.book().getCurrent();
        for (Skill s : ad.getLearnedSkillList()) {
            if (!(s instanceof WideCastable wc)) {
                continue;
            }

            if (wc.wideUniquePerProgram() && cur != null && hasElsewhereInRow(cur, s)) {
                continue;
            }
            out.add(s);
        }
        return out;
    }

    @Override
    protected void init() {
        super.init();
        noiseTexOk = null;
    }

    private void layout() {

        showLever = cn.academy.ability.vanilla.mentalout.advanced.CognitionTamper.isLearned(player);
        panelW = PAD + innerW() + PAD;

        RemoteData rd = data();
        showAllies = rd != null && !rd.getAllies().isEmpty()
                && cn.academy.ability.vanilla.mentalout.advanced.FreeManip.isLearned(player);
        int leftW = showAllies ? ALLY_PANEL_W + PANEL_GAP : 0;

        int needW = leftW + panelW + SCREEN_MARGIN * 2;
        int needH = PANEL_H + SCREEN_MARGIN * 2;
        uiScale = Math.max(MIN_UI_SCALE,
                Math.min(1f, Math.min(width / (float) needW, height / (float) needH)));

        allyPx = (vw() - (leftW + panelW)) / 2;
        px = allyPx + leftW;
        py = (vh() - PANEL_H) / 2;
        x0 = px + PAD;
        gridX = x0 + LABEL_W + 6;

        leverX = showLever ? gridX + GRID_W + 6 : -9999;
        gridY = py + PAD + HEAD_H;
        spinY = py + 20;

        allyScroll = clamp(allyScroll, 0, allyMaxScroll(rd));
    }

    private int allyViewH() {
        return PANEL_H - ALLY_TOP - PAD;
    }

    private int allyViewTop() {
        return py + ALLY_TOP;
    }

    private static void drawLock(GuiGraphics gg, int cx, int ry) {
        final int C = 0xFF6A6A78;
        int x = cx + CELL / 2 - 4, y = ry + CELL / 2 - 5;
        gg.fill(x + 1, y, x + 7, y + 1, C);
        gg.fill(x + 1, y + 1, x + 2, y + 4, C);
        gg.fill(x + 6, y + 1, x + 7, y + 4, C);
        gg.fill(x, y + 4, x + 8, y + 9, C);
    }

    private int allyContentH(RemoteData rd) {
        int n = rd == null ? 0 : rd.getAllies().size();
        if (n <= 0) {
            return 0;
        }
        int rows = (n + ENT_PER_ROW - 1) / ENT_PER_ROW;
        return rows * (CARD_H + ENT_GAP) - ENT_GAP;
    }

    private int allyMaxScroll(RemoteData rd) {
        return Math.max(0, allyContentH(rd) - allyViewH());
    }

    private boolean allyBarVisible(RemoteData rd) {
        return showAllies && allyMaxScroll(rd) > 0;
    }

    private int[] allyBarRect() {
        return new int[] {
                allyPx + ALLY_PANEL_W - PAD + ALLY_BAR_GAP,
                allyViewTop(),
                ALLY_BAR_W,
                allyViewH()
        };
    }

    private int[] allyThumbRect(RemoteData rd) {
        int[] slot = allyBarRect();
        int content = Math.max(1, allyContentH(rd));
        int len = Math.max(ALLY_BAR_MIN, slot[3] * allyViewH() / content);
        len = Math.min(len, slot[3]);
        int max = allyMaxScroll(rd);
        int travel = slot[3] - len;
        int off = max <= 0 ? 0 : travel * allyScroll / max;
        return new int[] {slot[0], slot[1] + off, slot[2], len};
    }

    @Override
    public void render(GuiGraphics gg, int mx, int my, float partialTick) {
        RemoteData rd = data();
        if (rd == null) {
            onClose();
            return;
        }

        if (Minecraft.getInstance().player != player
                || !(player.getMainHandItem().getItem() instanceof cn.academy.item.RemoteControlItem)) {
            onClose();
            return;
        }
        layout();

        gg.fill(0, 0, width, height, 0xB3000000);

        int vmx = vpos(mx), vmy = vpos(my);
        gg.pose().pushPose();
        gg.pose().scale(uiScale, uiScale, 1f);
        try {
            renderPanels(gg, rd, vmx, vmy);
        } finally {
            gg.pose().popPose();
        }

        if (pickSlot < 0) {
            drawTooltip(gg, vmx, vmy, mx, my, rd);
        }
    }

    private void renderPanels(GuiGraphics gg, RemoteData rd, int mx, int my) {
        TechUIDraw.panel(gg, px, py, panelW, PANEL_H);

        RemoteData.Book book = rd.book();
        int cur = book.getCurrentID();
        float exp = wideExp();
        RemoteData.Program prog = book.getProgram(cur);

        gg.drawCenteredString(font, this.title, px + panelW / 2, py + 6, GOLD);

        drawSpinner(gg, x0, spinY, Component.translatable("gui.academy.remote_control.range"),
                effRange(prog, exp), RemoteData.MIN_RANGE, RemoteData.maxRange(exp), mx, my);
        drawSpinner(gg, px + panelW / 2 + 4, spinY,
                Component.translatable("gui.academy.remote_control.count"),
                effCount(prog, exp), RemoteData.MIN_COUNT, RemoteData.maxCount(exp), mx, my);

        int gx = gridX;
        int gy = gridY;
        for (int r = 0; r < RemoteData.MAX_PROGRAMS; ++r) {
            int ry = gy + r * (CELL + GAP);
            boolean on = r == cur;

            gg.drawString(font,
                    Component.translatable("gui.academy.remote_control.no", r + 1),
                    x0 + 4, ry + (CELL - 8) / 2, on ? GOLD : GREY, false);
            if (on) {
                gg.fill(x0 - 2, ry, x0, ry + CELL, GOLD);
            }

            RemoteData.Program p = book.getProgram(r);

            int usable = MentalMastery.usableSlots(minecraft == null ? null : minecraft.player);
            for (int c = 0; c < RemoteData.MAX_SLOTS; ++c) {
                int cx = gx + c * (CELL + GAP);
                boolean locked = c >= usable;
                boolean hover = !locked && TechUIDraw.inRect(mx, my, cx, ry, CELL, CELL);

                int border = locked ? 0xFF3A3A42 : (on ? (hover ? GOLD : 0xFFCFD6DD) : DIM);
                gg.fill(cx, ry, cx + CELL, ry + CELL,
                        locked ? 0x70000000 : (on ? 0x50000000 : 0x30000000));
                frame(gg, cx, ry, CELL, CELL, border);
                if (locked) {
                    drawLock(gg, cx, ry);
                    continue;
                }

                Skill s = p.getSkill(c);
                if (s != null) {
                    TechUIDraw.icon(gg, s.getHintIcon(), cx + 2, ry + 2, CELL - 4,
                            on ? 1.0f : 0.45f, 32);

                    if (s instanceof WideCastable wc && wc.wideNeedsCommand()) {
                        net.minecraft.resources.ResourceLocation oi =
                                wc.wideOptionIcon(p.getCommand(c));
                        if (oi != null) {
                            TechUIDraw.icon(gg, oi,
                                    cx + CELL - 11, ry + CELL - 11, 10, on ? 1.0f : 0.45f, 32);
                        }
                    }
                }
            }

            if (showLever) {
                drawLever(gg, leverX, ry + (CELL - LEVER_H) / 2, p.isSyncMind(), on, mx, my);
            }
        }

        drawAllies(gg, rd, mx, my);

        if (pickSlot >= 0) {
            drawPicker(gg, mx, my);
        }
    }

    private void drawLever(GuiGraphics gg, int lx, int ly, boolean on, boolean cur, int mx, int my) {
        boolean hover = TechUIDraw.inRect(mx, my, lx, ly, LEVER_W, LEVER_H);
        int frame = cur ? (hover ? GOLD : 0xFFCFD6DD) : DIM;
        gg.fill(lx, ly, lx + LEVER_W, ly + LEVER_H, cur ? 0x50000000 : 0x30000000);
        frame(gg, lx, ly, LEVER_W, LEVER_H, frame);

        int kx = on ? lx + 1 : lx + LEVER_W - LEVER_KNOB - 1;
        int knob = !cur ? DIM : (on ? GOLD : GREY);
        gg.fill(kx, ly + 1, kx + LEVER_KNOB, ly + LEVER_H - 1, knob);
    }

    private boolean leverHit(int mx, int my, int row) {
        if (!showLever) {
            return false;
        }
        int ry = gridY + row * (CELL + GAP);
        return TechUIDraw.inRect(mx, my, leverX, ry + (CELL - LEVER_H) / 2, LEVER_W, LEVER_H);
    }

    private void drawAllies(GuiGraphics gg, RemoteData rd, int mx, int my) {
        if (!showAllies) {
            return;
        }
        TechUIDraw.panel(gg, allyPx, py, ALLY_PANEL_W, PANEL_H);

        gg.drawCenteredString(font,
                Component.translatable("gui.academy.remote_control.ally_title")
                        .withStyle(net.minecraft.ChatFormatting.BOLD),
                allyPx + ALLY_PANEL_W / 2, py + ALLY_TITLE_Y, GOLD);
        java.util.List<RemoteData.Ally> list = rd.getAllies();

        gg.enableScissor(sx(allyPx), sx(allyViewTop()),
                sx(allyPx + ALLY_PANEL_W), sx(allyViewTop() + allyViewH()));
        for (int i = 0; i < list.size(); ++i) {
            int[] p = allySlot(i);

            if (!allyVisible(p)) {
                continue;
            }

            boolean hover = allyInView(my) && TechUIDraw.inRect(mx, my, p[0], p[1], CARD_W, CARD_H);

            gg.fill(p[0], p[1], p[0] + CARD_W, p[1] + CARD_NAME_H, hover ? 0xFF3A424C : 0xFF2A3038);
            gg.fill(p[0], p[1] + CARD_NAME_H, p[0] + CARD_W, p[1] + CARD_H, 0xFF12161C);
            Player pa = playerAlly(list.get(i));
            Component name = allyName(list.get(i));
            drawAllyName(gg, p, name);

            cn.academy.client.render.AllyCamFeed.want(list.get(i).id, hover);
            if (drawAllyCam(gg, p, list.get(i).id)) {
                continue;
            }

            if (isPlayerAlly(list.get(i))) {
                drawPlayerAlly(gg, p, pa, list.get(i).id);
                drawLostSignal(gg, p, list.get(i).id);
                continue;
            }

            LivingEntity le = renderStub(list.get(i).type);
            if (le == null) {

                drawLostSignal(gg, p, list.get(i).id);
                continue;
            }
            idleTick(le);

            float sh = (CARD_MODEL_H - 6) / Math.max(0.1f, le.getBbHeight());
            float sw = (CARD_W - 8) / Math.max(0.1f, le.getBbWidth());
            int scale = (int) Mth.clamp(Math.min(sh, sw), 3, 40);
            try {

                org.joml.Quaternionf cam = new org.joml.Quaternionf()
                        .rotateX((float) Math.toRadians(-PREVIEW_PITCH_DEG));
                org.joml.Quaternionf pose = new org.joml.Quaternionf()
                        .rotateZ((float) Math.PI).mul(cam);
                net.minecraft.client.gui.screens.inventory.InventoryScreen
                        .renderEntityInInventory(gg,
                                p[0] + CARD_W / 2, p[1] + CARD_H - 3, scale, pose, cam, le);
            } catch (Exception ignored) {

            }
            drawLostSignal(gg, p, list.get(i).id);
        }

        for (int i = 0; i < list.size(); ++i) {
            int[] p = allySlot(i);
            if (!allyVisible(p)) {
                continue;
            }

            drawWatchEyes(gg, p, list.get(i).id.hashCode());
        }
        gg.disableScissor();

        drawAllyBar(gg, rd, mx, my);
    }

    private int sx(int virtual) {
        return Math.round(virtual * uiScale);
    }

    private void drawAllyBar(GuiGraphics gg, RemoteData rd, int mx, int my) {
        if (!allyBarVisible(rd)) {
            return;
        }
        int[] slot = allyBarRect();
        int[] th = allyThumbRect(rd);
        boolean hot = allyDragging
                || TechUIDraw.inRect(mx, my, th[0] - 2, th[1], th[2] + 4, th[3]);

        gg.fill(slot[0], slot[1], slot[0] + slot[2], slot[1] + slot[3], 0x30FFFFFF);
        gg.fill(th[0], th[1], th[0] + th[2], th[1] + th[3], hot ? GOLD : 0x90FFFFFF);
    }

    private void drawWatchEyes(GuiGraphics gg, int[] p, int seed) {
        double t = cn.lambdalib2.util.GameTimer.getTime();
        int base = (int) (t / EYE_FRAME_TIME);
        float top = p[1] + CARD_NAME_H;
        float step = (float) CARD_MODEL_H / EYES_PER_SIDE;
        int half = (int) (EYE_SIZE / 2f);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        gg.pose().pushPose();
        gg.pose().translate(0f, 0f, Z_EYES);
        for (int i = 0; i < EYES_PER_SIDE; ++i) {
            float cy = top + (i + 0.5f) * step;

            int frame = ((base + i * EYE_PHASE_STEP) % EYE_FRAMES + EYE_FRAMES) % EYE_FRAMES;
            for (int side = 0; side < 2; ++side) {

                int h = eyeHash((long) seed * 31 + i * 2 + side);
                float jitter = ((h & 0xFFFF) / 65535f * 2f - 1f) * EYE_JITTER_DEG;
                boolean mirror = ((h >>> 16) & 1) != 0;
                float tilt = (side == 0 ? -EYE_TILT_DEG : EYE_TILT_DEG) + jitter;

                float u0 = mirror ? (frame + 1) * EYE_FRAME : frame * EYE_FRAME;
                int uw = mirror ? -EYE_FRAME : EYE_FRAME;

                gg.pose().pushPose();

                gg.pose().translate(cx(p, side), cy, 0f);
                gg.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(tilt));
                gg.blit(EYE_TEX, -half, -half, (int) EYE_SIZE, (int) EYE_SIZE,
                        u0, 0f, uw, EYE_FRAME, EYE_TEX_W, EYE_FRAME);
                gg.pose().popPose();
            }
        }
        gg.pose().popPose();
        RenderSystem.disableBlend();
    }

    private static float cx(int[] p, int side) {
        return side == 0 ? p[0] + EYE_INSET : p[0] + CARD_W - EYE_INSET;
    }

    private static int eyeHash(long s) {
        s ^= s >>> 33;
        s *= 0xff51afd7ed558ccdL;
        s ^= s >>> 33;
        s *= 0xc4ceb9fe1a85ec53L;
        s ^= s >>> 33;
        return (int) s;
    }

    private boolean drawAllyCam(GuiGraphics gg, int[] p, java.util.UUID id) {
        int[] t = cn.academy.client.render.AllyCamFeed.texture(id);
        if (t == null) {
            return false;
        }
        int x = p[0] + 1, y = p[1] + CARD_NAME_H + 1;
        int w = CARD_W - 2, h = CARD_MODEL_H - 2;

        float ta = (float) t[1] / (float) t[2];
        float ca = (float) w / (float) h;
        float u0 = 0f, u1 = 1f, v0 = 0f, v1 = 1f;
        if (ta > ca) {
            float keep = ca / ta;
            u0 = (1f - keep) / 2f;
            u1 = 1f - u0;
        } else if (ta < ca) {
            float keep = ta / ca;
            v0 = (1f - keep) / 2f;
            v1 = 1f - v0;
        }

        com.mojang.blaze3d.systems.RenderSystem.setShader(
                net.minecraft.client.renderer.GameRenderer::getPositionTexShader);
        com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, t[0]);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        org.joml.Matrix4f m = gg.pose().last().pose();
        com.mojang.blaze3d.vertex.BufferBuilder bb =
                com.mojang.blaze3d.vertex.Tesselator.getInstance().getBuilder();
        bb.begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS,
                com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX);

        bb.vertex(m, x, y + h, 0f).uv(u0, v0).endVertex();
        bb.vertex(m, x + w, y + h, 0f).uv(u1, v0).endVertex();
        bb.vertex(m, x + w, y, 0f).uv(u1, v1).endVertex();
        bb.vertex(m, x, y, 0f).uv(u0, v1).endVertex();
        com.mojang.blaze3d.vertex.BufferUploader.drawWithShader(bb.end());
        return true;
    }

    private void drawAllyName(GuiGraphics gg, int[] p, Component name) {
        Component styled = Component.literal(name.getString())
                .withStyle(net.minecraft.ChatFormatting.BOLD);
        net.minecraft.util.FormattedCharSequence seq =
                net.minecraft.locale.Language.getInstance()
                        .getVisualOrder(font.substrByWidth(styled, CARD_W - 6));
        int x = p[0] + CARD_W / 2 - font.width(seq) / 2;
        int y = p[1] + 2;
        for (int[] o : NAME_OUTLINE) {
            gg.drawString(font, seq, x + o[0], y + o[1], GOLD, false);
        }
        gg.drawString(font, seq, x, y, 0xFFFFFFFF, false);
    }

    private void drawLostSignal(GuiGraphics gg, int[] p, java.util.UUID id) {

        gg.pose().pushPose();
        gg.pose().translate(0f, 0f, Z_OVERLAY);
        drawCamNoise(gg, p);
        drawNoSignal(gg, p, id);
        gg.pose().popPose();

        drawJamDistort(gg, p, id);
    }

    private void drawJamDistort(GuiGraphics gg, int[] p, java.util.UUID id) {
        if (id == null || !cn.academy.client.render.AllyCamFeed.jammed(id)) {
            return;
        }

        int x = p[0] + 1, y = p[1] + CARD_NAME_H + 1;
        int w = CARD_W - 2, h = CARD_MODEL_H - 2;
        gg.enableScissor(sx(x), sx(y), sx(x + w), sx(y + h));
        gg.pose().pushPose();
        gg.pose().translate(0f, 0f, Z_DISTORT);

        gg.fill(x, y, x + w, y + h, JAM_TINT);

        float cx = x + w * 0.5f, cy = y + h * 0.5f;
        float ang = (float) (cn.lambdalib2.util.GameTimer.getTime() * JAM_SPIN_DEG);

        gg.pose().translate(cx, cy, 0f);
        gg.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(ang));
        gg.pose().scale(JAM_STRETCH, 1f, 1f);
        gg.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-ang));
        gg.pose().translate(-cx, -cy, 0f);

        int x0 = (int) (cx - JAM_HALF), x1 = (int) (cx + JAM_HALF);
        int top = (int) (cy - JAM_HALF), bottom = (int) (cy + JAM_HALF);
        for (int i = 0, ly = top; ly < bottom; ++i, ly += JAM_STEP) {
            int bar = 1 + (eyeHash(id.hashCode() * 31L + i) & 3);
            gg.fill(x0, ly, x1, ly + bar, JAM_LINE);
        }
        gg.pose().popPose();
        gg.disableScissor();
    }

    private void drawCamNoise(GuiGraphics gg, int[] p) {
        if (!hasNoiseTex()) {
            return;
        }
        int frame = (int) (cn.lambdalib2.util.GameTimer.getTime() / NOISE_FRAME_TIME)
                % NOISE_FRAMES;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        gg.blit(NOISE_TEX, p[0], p[1] + CARD_NAME_H, CARD_W, CARD_MODEL_H,
                frame * NOISE_FRAME, 0f, NOISE_FRAME, NOISE_FRAME, NOISE_TEX_W, NOISE_FRAME);
        RenderSystem.disableBlend();
    }

    private static boolean hasNoiseTex() {
        if (noiseTexOk == null) {
            noiseTexOk = Minecraft.getInstance().getResourceManager()
                    .getResource(NOISE_TEX).isPresent();
        }
        return noiseTexOk;
    }

    private void drawNoSignal(GuiGraphics gg, int[] p, java.util.UUID id) {
        boolean denied = id != null && id.equals(denyId)
                && net.minecraft.Util.getMillis() - denyAt < DENY_MS;

        Component msg = Component.translatable(
                denied ? "gui.academy.remote_control.cast_deny"
                        : cn.academy.client.render.AllyCamFeed.shaderBlocked()
                                ? "gui.academy.remote_control.cam_shader"
                                : "gui.academy.remote_control.cam_nosignal")
                .withStyle(net.minecraft.ChatFormatting.BOLD);

        java.util.List<net.minecraft.util.FormattedCharSequence> lines =
                font.split(msg, CARD_W - 4);
        int total = lines.size() * font.lineHeight;

        int y0 = p[1] + CARD_NAME_H + (CARD_MODEL_H - total) / 2;

        gg.fill(p[0] + 1, y0 - 2, p[0] + CARD_W - 1, y0 + total + 1, 0xB0000000);
        int color = denied ? 0xFFFF5555 : 0xFFFFFFFF;
        for (int i = 0; i < lines.size(); ++i) {
            gg.drawCenteredString(font, lines.get(i),
                    p[0] + CARD_W / 2, y0 + i * font.lineHeight, color);
        }
    }

    private Component allyName(RemoteData.Ally a) {
        Player pa = playerAlly(a);
        if (pa != null) {
            return pa.getName();
        }
        net.minecraft.world.entity.EntityType<?> et =
                net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(a.type);
        return et == null ? Component.literal(a.type.getPath()) : et.getDescription();
    }

    private static boolean isPlayerAlly(RemoteData.Ally a) {
        return net.minecraft.world.entity.EntityType.PLAYER.equals(
                net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(a.type));
    }

    @Nullable
    private Player playerAlly(RemoteData.Ally a) {
        if (!isPlayerAlly(a)) {
            return null;
        }
        net.minecraft.world.level.Level lv = Minecraft.getInstance().level;
        return lv == null ? null : lv.getPlayerByUUID(a.id);
    }

    private void drawPlayerAlly(GuiGraphics gg, int[] p, @Nullable Player who,
                                java.util.UUID id) {
        net.minecraft.resources.ResourceLocation skin =
                who instanceof net.minecraft.client.player.AbstractClientPlayer acp
                        ? acp.getSkinTextureLocation()
                        : net.minecraft.client.resources.DefaultPlayerSkin.getDefaultSkin(id);
        int size = Math.min(CARD_W - 8, CARD_MODEL_H - 8);
        net.minecraft.client.gui.components.PlayerFaceRenderer.draw(gg, skin,
                p[0] + (CARD_W - size) / 2,
                p[1] + CARD_NAME_H + (CARD_MODEL_H - size) / 2, size);
    }

    private void idleTick(LivingEntity le) {
        net.minecraft.world.level.Level lv = Minecraft.getInstance().level;
        if (lv == null) {
            return;
        }
        le.tickCount = (int) (lv.getGameTime() & 0x7FFFFFFF);
        float yaw = 180f + PREVIEW_YAW_DEG;
        le.yBodyRotO = le.yBodyRot = yaw;
        le.yHeadRotO = le.yHeadRot = yaw;
        le.setYRot(yaw);
        le.yRotO = yaw;

        le.setXRot(PREVIEW_PITCH_DEG);
        le.xRotO = PREVIEW_PITCH_DEG;

        le.walkAnimation.setSpeed(0f);
    }

    private int[] allySlot(int i) {
        int col = i % ENT_PER_ROW, row = i / ENT_PER_ROW;
        return new int[] {
                allyPx + PAD + col * (CARD_W + ENT_GAP),
                py + ALLY_TOP + row * (CARD_H + ENT_GAP) - allyScroll
        };
    }

    private boolean allyVisible(int[] p) {
        return p[1] + CARD_H > allyViewTop() && p[1] < allyViewTop() + allyViewH();
    }

    private boolean allyInView(int my) {
        return my >= allyViewTop() && my < allyViewTop() + allyViewH();
    }

    private final java.util.Map<net.minecraft.resources.ResourceLocation, LivingEntity> stubs =
            new java.util.HashMap<>();

    @Nullable
    private LivingEntity renderStub(net.minecraft.resources.ResourceLocation type) {
        if (stubs.containsKey(type)) {
            return stubs.get(type);
        }
        LivingEntity made = null;
        try {
            net.minecraft.world.level.Level lv = Minecraft.getInstance().level;
            net.minecraft.world.entity.EntityType<?> et =
                    net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(type);
            if (lv != null && et != null && et.create(lv) instanceof LivingEntity le) {
                made = le;
            }
        } catch (Exception ignored) {

        }
        stubs.put(type, made);
        return made;
    }

    private void drawTooltip(GuiGraphics gg, int mx, int my, int sx, int sy, RemoteData rd) {
        int gx = gridX, gy = gridY;

        for (int r = 0; r < RemoteData.MAX_PROGRAMS; ++r) {
            if (leverHit(mx, my, r)) {
                List<Component> lines = new ArrayList<>();
                lines.add(Component.translatable("gui.academy.remote_control.sync_mind"));
                lines.add(Component.translatable(rd.book().getProgram(r).isSyncMind()
                        ? "gui.academy.remote_control.sync_on"
                        : "gui.academy.remote_control.sync_off"));
                gg.renderComponentTooltip(font, lines, sx, sy);
                return;
            }
        }

        java.util.List<RemoteData.Ally> allies = rd.getAllies();
        for (int i = 0; i < allies.size() && allyInView(my); ++i) {
            int[] sp = allySlot(i);
            if (!TechUIDraw.inRect(mx, my, sp[0], sp[1], CARD_W, CARD_H)) {
                continue;
            }

            Player pa = playerAlly(allies.get(i));
            net.minecraft.world.entity.EntityType<?> et =
                    net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(allies.get(i).type);
            gg.renderComponentTooltip(font, List.of(pa != null ? pa.getName()
                    : et == null
                        ? Component.literal(allies.get(i).type.toString())
                        : et.getDescription()), sx, sy);
            return;
        }
        for (int r = 0; r < RemoteData.MAX_PROGRAMS; ++r) {
            for (int c = 0; c < RemoteData.MAX_SLOTS; ++c) {
                int cx = gx + c * (CELL + GAP), ry = gy + r * (CELL + GAP);
                if (!TechUIDraw.inRect(mx, my, cx, ry, CELL, CELL)) {
                    continue;
                }

                if (c >= MentalMastery.usableSlots(minecraft == null ? null : minecraft.player)) {
                    gg.renderTooltip(font, Component.translatable(
                            "gui.academy.remote_control.slot_locked",
                            MentalMastery.INSTANCE.getDisplayName()), sx, sy);
                    return;
                }
                RemoteData.Program p = rd.book().getProgram(r);
                Skill s = p.getSkill(c);
                if (s == null) {
                    return;
                }
                List<Component> lines = new ArrayList<>();
                lines.add(Component.literal(s.getDisplayName()));
                if (s instanceof WideCastable wc && wc.wideNeedsCommand()) {
                    lines.add(wc.wideOptionName(p.getCommand(c)));
                }
                gg.renderComponentTooltip(font, lines, sx, sy);
                return;
            }
        }
    }

    private int numberX(int x, Component label) {
        return x + font.width(label) + 6;
    }

    private int arrowX(int x, Component label, int value) {
        return numberX(x, label) + Math.max(14, font.width(String.valueOf(value)) + 4) + 4;
    }

    private static boolean arrowHit(int ax, int y, int dir, int mx, int my) {
        return TechUIDraw.inRect(mx, my, ax - 5, dir > 0 ? y - 1 : y + 9, 12, 8);
    }

    private void drawSpinner(GuiGraphics gg, int x, int y, Component label,
                             int value, int min, int max, int mx, int my) {
        gg.drawString(font, label, x, y + 5, GREY, false);
        gg.drawString(font, String.valueOf(value), numberX(x, label), y + 5, GOLD, false);

        int ax = arrowX(x, label, value);
        boolean upOK = value < max, downOK = value > min;
        boolean upHover = upOK && arrowHit(ax, y, 1, mx, my);
        boolean dnHover = downOK && arrowHit(ax, y, -1, mx, my);
        triUp(gg, ax, y + 1, 5, !upOK ? DIM : (upHover ? GOLD : GREY));
        triDown(gg, ax, y + 10, 5, !downOK ? DIM : (dnHover ? GOLD : GREY));
    }

    private int[] pickerBox(List<Component> names) {
        int n = pickerCount(names);
        int rows = (n + PICK_PER_ROW - 1) / PICK_PER_ROW;
        int w = PICK_MARGIN * 2 + Math.min(n, PICK_PER_ROW) * PICK_CELL;
        int h = PICK_MARGIN * 2 + rows * PICK_CELL;

        return new int[]{Math.min(pickX, vw() - w - 2), Math.min(pickY, vh() - h - 2), w, h, n};
    }

    private static int pickerCellX(int[] box, int i) {
        return box[0] + PICK_MARGIN + (i % PICK_PER_ROW) * PICK_CELL;
    }

    private static int pickerCellY(int[] box, int i) {
        return box[1] + PICK_MARGIN + (i / PICK_PER_ROW) * PICK_CELL;
    }

    @Override
    public boolean mouseScrolled(double mxd, double myd, double delta) {
        RemoteData rd = data();
        if (rd != null) {
            layout();
            int mx = vpos(mxd), my = vpos(myd);
            if (allyBarVisible(rd)
                    && TechUIDraw.inRect(mx, my, allyPx, py, ALLY_PANEL_W, PANEL_H)) {
                allyScroll = clamp(allyScroll - (int) Math.signum(delta) * ALLY_SCROLL_STEP,
                        0, allyMaxScroll(rd));
                return true;
            }
        }
        return super.mouseScrolled(mxd, myd, delta);
    }

    @Override
    public boolean mouseDragged(double mxd, double myd, int button, double dx, double dy) {
        if (allyDragging && button == 0) {
            RemoteData rd = data();
            if (rd == null) {
                allyDragging = false;
            } else {
                layout();
                dragAllyTo(rd, vpos(myd));
                return true;
            }
        }
        return super.mouseDragged(mxd, myd, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mxd, double myd, int button) {
        if (button == 0) {
            allyDragging = false;
        }
        return super.mouseReleased(mxd, myd, button);
    }

    private void dragAllyTo(RemoteData rd, int my) {
        int[] slot = allyBarRect();
        int[] th = allyThumbRect(rd);
        int travel = slot[3] - th[3];
        if (travel <= 0) {
            return;
        }
        int want = clamp(my - allyDragGrab - slot[1], 0, travel);
        allyScroll = clamp(want * allyMaxScroll(rd) / travel, 0, allyMaxScroll(rd));
    }

    private void drawPicker(GuiGraphics gg, int mx, int my) {
        List<Component> names = new ArrayList<>();
        int[] box = pickerBox(names);

        gg.fill(box[0], box[1], box[0] + box[2], box[1] + box[3], 0xE0101418);
        frame(gg, box[0], box[1], box[2], box[3], GOLD);

        String hover = null;
        for (int i = 0; i < box[4]; ++i) {
            int cx = pickerCellX(box, i), cy = pickerCellY(box, i);
            if (TechUIDraw.inRect(mx, my, cx, cy, PICK_CELL, PICK_CELL)) {
                gg.fill(cx, cy, cx + PICK_CELL, cy + PICK_CELL, 0x40FFCD46);
                hover = names.get(i).getString();
            }
            TechUIDraw.icon(gg, pickerIcon(i), cx + 2, cy + 2, PICK_CELL - 4, 1.0f, 32);
        }
        if (hover != null) {
            gg.drawString(font, hover, box[0], box[1] - 10, GOLD, false);
        }
    }

    private int pickerCount(List<Component> out) {

        if (pickStage == 1) {
            if (pendingSkill instanceof WideCastable wc) {
                for (int i = 0; i < wc.wideOptionCount(); ++i) {
                    out.add(wc.wideOptionName(i));
                }
            }
            return out.size();
        }
        out.add(Component.translatable("gui.academy.remote_control.clear"));
        for (Skill s : pickable()) {
            out.add(Component.literal(s.getDisplayName()));
        }
        return out.size();
    }

    private net.minecraft.resources.ResourceLocation pickerIcon(int i) {
        if (pickStage == 1) {

            net.minecraft.resources.ResourceLocation oi =
                    pendingSkill instanceof WideCastable wc ? wc.wideOptionIcon(i) : null;
            return oi != null ? oi : pendingSkill.getHintIcon();
        }
        if (i == 0) {
            return cn.academy.Resources.getTexture("gui/preset_settings/cancel");
        }
        return pickable().get(i - 1).getHintIcon();
    }

    @Override
    public boolean mouseClicked(double mxd, double myd, int button) {
        if (button != 0) {
            return super.mouseClicked(mxd, myd, button);
        }
        RemoteData rd = data();
        if (rd == null) {
            onClose();
            return true;
        }

        layout();

        int mx = vpos(mxd), my = vpos(myd);

        if (pickSlot >= 0) {
            clickPicker(rd, mx, my);
            return true;
        }

        if (allyBarVisible(rd)) {
            int[] th = allyThumbRect(rd);
            if (TechUIDraw.inRect(mx, my, th[0] - 2, th[1], th[2] + 4, th[3])) {
                allyDragging = true;
                allyDragGrab = my - th[1];
                return true;
            }
            int[] slot = allyBarRect();
            if (TechUIDraw.inRect(mx, my, slot[0] - 2, slot[1], slot[2] + 4, slot[3])) {

                allyDragging = true;
                allyDragGrab = th[3] / 2;
                dragAllyTo(rd, my);
                return true;
            }
        }

        if (clickAllyCard(rd, mx, my)) {
            return true;
        }

        RemoteData.Book book = rd.book();
        int cur = book.getCurrentID();
        float exp = wideExp();
        RemoteData.Program prog = book.getProgram(cur);

        int dr = spinnerHit(x0, spinY,
                Component.translatable("gui.academy.remote_control.range"),
                effRange(prog, exp), mx, my);
        int dc = spinnerHit(px + panelW / 2 + 4, spinY,
                Component.translatable("gui.academy.remote_control.count"),
                effCount(prog, exp), mx, my);
        if (dr != 0 || dc != 0) {

            int step = hasShiftDown() ? SHIFT_STEP : 1;
            int nr = clamp(effRange(prog, exp) + dr * step, RemoteData.MIN_RANGE, RemoteData.maxRange(exp));
            int nc = clamp(effCount(prog, exp) + dc * step, RemoteData.MIN_COUNT, RemoteData.maxCount(exp));
            rd.setNumbersFromClient(cur, nr, nc);
            return true;
        }

        int gx = gridX, gy = gridY;
        for (int r = 0; r < RemoteData.MAX_PROGRAMS; ++r) {
            int ry = gy + r * (CELL + GAP);

            if (TechUIDraw.inRect(mx, my, x0 - 2, ry, LABEL_W + 6, CELL)) {
                rd.switchFromClient(r);
                return true;
            }

            if (leverHit(mx, my, r)) {
                rd.setSyncFromClient(r, !book.getProgram(r).isSyncMind());
                return true;
            }
            int usableC = MentalMastery.usableSlots(minecraft == null ? null : minecraft.player);
            for (int c = 0; c < RemoteData.MAX_SLOTS; ++c) {
                int cx = gx + c * (CELL + GAP);
                if (!TechUIDraw.inRect(mx, my, cx, ry, CELL, CELL)) {
                    continue;
                }
                if (c >= usableC) {
                    return true;
                }
                if (r != cur) {
                    rd.switchFromClient(r);
                    return true;
                }
                pickSlot = c;
                pickStage = 0;
                pendingSkill = null;
                pickX = mx + 6;
                pickY = my + 6;
                return true;
            }
        }
        return super.mouseClicked(mxd, myd, button);
    }

    private boolean clickAllyCard(RemoteData rd, int mx, int my) {
        if (!showAllies || !allyInView(my)) {
            return false;
        }
        java.util.List<RemoteData.Ally> list = rd.getAllies();
        for (int i = 0; i < list.size(); ++i) {
            int[] p = allySlot(i);
            if (!TechUIDraw.inRect(mx, my, p[0], p[1], CARD_W, CARD_H)) {
                continue;
            }
            RemoteData.Ally a = list.get(i);
            if (cn.academy.client.render.AllyCastView.begin(a.id, allyName(a))) {
                onClose();
            } else {
                denyId = a.id;
                denyAt = net.minecraft.Util.getMillis();
            }
            return true;
        }
        return false;
    }

    private void clickPicker(RemoteData rd, int mx, int my) {
        List<Component> names = new ArrayList<>();
        int[] box = pickerBox(names);

        for (int i = 0; i < box[4]; ++i) {
            int cx = pickerCellX(box, i), cy = pickerCellY(box, i);
            if (!TechUIDraw.inRect(mx, my, cx, cy, PICK_CELL, PICK_CELL)) {
                continue;
            }
            if (pickStage == 1) {
                commit(rd, pendingSkill, i);
                return;
            }
            if (i == 0) {
                commit(rd, null, 0);
                return;
            }
            Skill s = pickable().get(i - 1);

            if (s instanceof WideCastable wc && wc.wideNeedsCommand()) {
                pendingSkill = s;
                pickStage = 1;
                return;
            }
            commit(rd, s, 0);
            return;
        }
        closePicker();
    }

    private void commit(RemoteData rd, Skill skill, int cmd) {
        rd.setSlotFromClient(rd.book().getCurrentID(), pickSlot, skill == null ? -1 : skill.getID(), cmd);
        closePicker();
    }

    private void closePicker() {
        pickSlot = -1;
        pickStage = 0;
        pendingSkill = null;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE && pickSlot >= 0) {
            closePicker();
            return true;
        }
        if (key == activateKey()) {
            onClose();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    private static int activateKey() {
        int id = cn.academy.util.ACKeyManager.instance.getKeyID(
                cn.academy.ability.ctrl.ClientHandler.keyActivate);
        return id > 0 ? id : Integer.MIN_VALUE;
    }

    private int spinnerHit(int x, int y, Component label, int value, int mx, int my) {
        int ax = arrowX(x, label, value);
        if (arrowHit(ax, y, 1, mx, my)) return 1;
        if (arrowHit(ax, y, -1, mx, my)) return -1;
        return 0;
    }

    private static int effRange(RemoteData.Program p, float exp) {
        return p.effectiveRange(exp);
    }

    private static int effCount(RemoteData.Program p, float exp) {
        return p.effectiveCount(exp);
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static void frame(GuiGraphics gg, int x, int y, int w, int h, int color) {
        gg.fill(x, y, x + w, y + 1, color);
        gg.fill(x, y + h - 1, x + w, y + h, color);
        gg.fill(x, y + 1, x + 1, y + h - 1, color);
        gg.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    private static void triUp(GuiGraphics gg, int cx, int cy, int size, int color) {
        for (int i = 0; i < size; ++i) {
            gg.fill(cx - i, cy + i, cx + i + 1, cy + i + 1, color);
        }
    }

    private static void triDown(GuiGraphics gg, int cx, int cy, int size, int color) {
        for (int i = 0; i < size; ++i) {
            int hw = size - 1 - i;
            gg.fill(cx - hw, cy + i, cx + hw + 1, cy + i + 1, color);
        }
    }

    @Override
    public void removed() {
        cn.academy.client.render.AllyCamFeed.release();
        super.removed();
    }
}
