package cn.academy.client.gui;

import cn.academy.ability.vanilla.mentalout.ControlState;
import cn.academy.ability.vanilla.mentalout.skill.ForcedControl;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class CommandWheelScreen extends Screen {

    private static final ControlState.Command[] CMDS = ControlState.Command.values();

    private static final float R_IN = 42f, R_OUT = 96f;

    private static final float R_DEAD = 26f;

    private static final int SEGS = 12;

    private static final float GAP = 0.035f;

    private int hover = -1;

    private float uiScale = 1f;

    private static final int WHEEL_MARGIN = 6;
    private static final float MIN_UI_SCALE = 0.35f;

    private void updateScale() {
        float need = R_OUT * 2 + WHEEL_MARGIN * 2;
        uiScale = Math.max(MIN_UI_SCALE, Math.min(1f, Math.min(width / need, height / need)));
    }

    private int sectorAtScreen(double sx, double sy) {
        return sectorAt((sx - width / 2.0) / uiScale, (sy - height / 2.0) / uiScale);
    }

    private CommandWheelScreen() {
        super(Component.translatable("gui.academy.forced_control.wheel"));
    }

    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null) {
            mc.setScreen(new CommandWheelScreen());
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void render(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        updateScale();
        hover = sectorAtScreen(mouseX, mouseY);

        gg.fill(0, 0, this.width, this.height, 0x50000000);

        gg.pose().pushPose();
        gg.pose().scale(uiScale, uiScale, 1f);
        try {
            drawWheel(gg);
        } finally {
            gg.pose().popPose();
        }
    }

    private void drawWheel(GuiGraphics gg) {
        float cx = this.width / 2f / uiScale, cy = this.height / 2f / uiScale;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionColorShader);
        Matrix4f m = gg.pose().last().pose();
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        float step = (float) (Math.PI * 2 / CMDS.length);
        for (int i = 0; i < CMDS.length; i++) {

            float mid = -(float) (Math.PI / 2) + i * step;
            float a0 = mid - step / 2 + GAP, a1 = mid + step / 2 - GAP;
            boolean on = i == hover;
            int argb = on ? 0xE0FFCD46 : 0x99202830;
            sector(bb, m, cx, cy, R_IN, R_OUT, a0, a1, argb);
        }
        BufferUploader.drawWithShader(bb.end());
        RenderSystem.disableBlend();

        for (int i = 0; i < CMDS.length; i++) {
            float mid = -(float) (Math.PI / 2) + i * step;
            float r = (R_IN + R_OUT) / 2f;
            int ix = (int) (cx + Math.cos(mid) * r);
            int iy = (int) (cy + Math.sin(mid) * r);
            TechUIDraw.icon(gg, ForcedControl.iconOf(CMDS[i]), ix - 12, iy - 18, 24, 1.0f, 32);
            Component name = ForcedControl.nameOf(CMDS[i]);
            gg.drawCenteredString(this.font, name, ix, iy + 10, i == hover ? 0xFFFFFFFF : 0xFFB0B8C0);
        }

        Component cur = ForcedControl.nameOf(ForcedControl.MiddleKey.clientCommand());
        gg.drawCenteredString(this.font, cur, (int) cx, (int) cy - 4, 0xFFFFCD46);
    }

    private int sectorAt(double dx, double dy) {
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < R_DEAD) {
            return -1;
        }
        double step = Math.PI * 2 / CMDS.length;
        double a = Math.atan2(dy, dx) + Math.PI / 2 + step / 2;
        double norm = (a % (Math.PI * 2) + Math.PI * 2) % (Math.PI * 2);
        return (int) (norm / step) % CMDS.length;
    }

    private static void sector(BufferBuilder bb, Matrix4f m, float cx, float cy,
                               float rIn, float rOut, float a0, float a1, int argb) {
        float alpha = (argb >>> 24) / 255f;
        float red = ((argb >> 16) & 0xFF) / 255f;
        float green = ((argb >> 8) & 0xFF) / 255f;
        float blue = (argb & 0xFF) / 255f;
        for (int s = 0; s < SEGS; s++) {
            float t0 = a0 + (a1 - a0) * s / SEGS;
            float t1 = a0 + (a1 - a0) * (s + 1) / SEGS;
            float c0 = (float) Math.cos(t0), s0 = (float) Math.sin(t0);
            float c1 = (float) Math.cos(t1), s1 = (float) Math.sin(t1);

            bb.vertex(m, cx + c0 * rIn, cy + s0 * rIn, 0).color(red, green, blue, alpha).endVertex();
            bb.vertex(m, cx + c0 * rOut, cy + s0 * rOut, 0).color(red, green, blue, alpha).endVertex();
            bb.vertex(m, cx + c1 * rOut, cy + s1 * rOut, 0).color(red, green, blue, alpha).endVertex();
            bb.vertex(m, cx + c1 * rIn, cy + s1 * rIn, 0).color(red, green, blue, alpha).endVertex();
        }
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == 2) {
            commit();
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            updateScale();
            hover = sectorAtScreen(mx, my);
            commit();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    private void commit() {
        if (hover >= 0 && hover < CMDS.length) {
            ForcedControl.MiddleKey.setClientCommand(CMDS[hover]);
        }
        this.onClose();
    }
}
