package cn.academy.client.gui;

import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

public final class InfoArea {

    public static final int X = 176 + 7;
    public static final int Y = 5;

    public static final int W = 100;

    private static final int KEY_LENGTH = 40;

    public static final int HIST_TEX = 210;
    public static final float HIST_SCALE = 0.4f;

    private static final float SIZE_SEP = 6f;
    private static final float SIZE_PROP = 8f;

    private static final float FONT_BASE = 9f;

    private static final int LABEL = 0xFFFFFFFF;
    private static final int VALUE = 0xFFFFFFFF;

    private static final int HEADER = 0x99FFFFFF;

    public record HistElement(String label, int color, double frac, String desc) {}

    public static HistElement histEnergy(double energy, double max) {
        return new HistElement(tr("gui.academy.common.hist.energy"), 0xFF25C4FF, energy / max,
                String.format("%.0f IF", energy));
    }

    public static HistElement histBuffer(double energy, double max) {
        return new HistElement(tr("gui.academy.common.hist.buffer"), 0xFF25F7FF, energy / max,
                String.format("%.0f IF", energy));
    }

    public static HistElement histPhaseLiquid(double amt, double max) {
        return new HistElement(tr("gui.academy.common.hist.liquid"), 0xFF7680DE, amt / max,
                String.format("%.0f mB", amt));
    }

    public static HistElement histCapacity(int amt, int max) {
        return new HistElement(tr("gui.academy.common.hist.capacity"), 0xFFFF6C00,
                max == 0 ? 0 : (double) amt / max, amt + "/" + max);
    }

    private abstract static class Elem {
        final float y;

        Elem(float y) {
            this.y = y;
        }
    }

    private static final class Hist extends Elem {
        final List<HistElement> bars;

        Hist(float y, List<HistElement> bars) {
            super(y);
            this.bars = bars;
        }
    }

    private static final class Legend extends Elem {
        final HistElement e;

        Legend(float y, HistElement e) {
            super(y);
            this.e = e;
        }
    }

    private static final class Sepline extends Elem {
        final String text;

        Sepline(float y, String text) {
            super(y);
            this.text = text;
        }
    }

    private static final class Property extends Elem {
        final String key;

        final String value;

        Property(float y, String key, String value) {
            super(y);
            this.key = key;
            this.value = value;
        }
    }

    private static final class Btn extends Elem {
        Btn(float y) {
            super(y);
        }
    }

    private float elemY = 10;
    private float height = 50;
    private final List<Elem> elems = new ArrayList<>();

    public int getHeight() {
        return Math.round(height);
    }

    private void element(float h, Elem e) {
        elemY += h;
        height = Math.max(50f, elemY + 8);
        elems.add(e);
    }

    public InfoArea blank(float h) {
        elemY += h;
        return this;
    }

    public InfoArea histogram(HistElement... bars) {
        blank(-30);
        float y = elemY;
        element(HIST_TEX * HIST_SCALE, new Hist(y, List.of(bars)));
        for (HistElement b : bars) {
            float ly = elemY;
            element(8, new Legend(ly, b));
        }
        return this;
    }

    public InfoArea sepline(String langKey) {
        blank(3);
        float y = elemY;
        element(8, new Sepline(y, tr(langKey)));
        return this;
    }

    public InfoArea seplineInfo() {
        return sepline("gui.academy.common.sep.info");
    }

    public InfoArea property(String langKey, String value) {
        float y = elemY;
        element(8, new Property(y, tr(langKey), value));
        return this;
    }

    public InfoArea propertyEditable(String langKey) {
        float y = elemY;
        element(8, new Property(y, tr(langKey), null));
        return this;
    }

    public InfoArea button(String text) {
        float y = elemY;
        element(8, new Btn(y));
        return this;
    }

    public float lastElementY() {
        return elems.get(elems.size() - 1).y;
    }

    public static int valueX() {
        return X + 6 + KEY_LENGTH;
    }

    public void draw(GuiGraphics gg, TechUIContainerScreen<?> s, int leftPos, int topPos) {
        int px = leftPos + X, py = topPos + Y;
        s.drawPanel(gg, px, py, W, getHeight());
        for (Elem e : elems) {
            int ey = Math.round(py + e.y);
            if (e instanceof Hist h) {
                drawHist(gg, px, py + h.y, h);
            } else if (e instanceof Legend l) {
                drawLegend(gg, px, ey, l);
            } else if (e instanceof Sepline sp) {
                drawSized(gg, sp.text, px + 3, ey, HEADER, SIZE_SEP);
            } else if (e instanceof Property p) {
                drawSized(gg, p.key, px + 6, ey, LABEL, SIZE_PROP);
                if (p.value != null) {
                    drawSized(gg, p.value, px + 6 + KEY_LENGTH, ey, VALUE, SIZE_PROP);
                }
            }

        }
    }

    private static void drawSized(GuiGraphics gg, String txt, float x, float y, int color, float size) {
        float s = size / FONT_BASE;
        gg.pose().pushPose();
        gg.pose().translate(x, y, 0);
        gg.pose().scale(s, s, 1f);
        gg.drawString(font(), txt, 0, 0, color, false);
        gg.pose().popPose();
    }

    private void drawHist(GuiGraphics gg, int px, float pyf, Hist h) {
        int hx = px;
        int hy = Math.round(pyf);
        int size = Math.round(HIST_TEX * HIST_SCALE);
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        gg.blit(TechUIContainerScreen.TEX_HISTOGRAM, hx, hy, size, size,
                0f, 0f, HIST_TEX, HIST_TEX, HIST_TEX, HIST_TEX);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();

        for (int i = 0; i < h.bars.size(); i++) {
            HistElement b = h.bars.get(i);

            double f = Math.max(0.03, Math.min(1.0, b.frac()));
            int bx = hx + Math.round((56 + i * 40) * HIST_SCALE);
            int bw = Math.round(16 * HIST_SCALE);
            int bTop = hy + Math.round(78 * HIST_SCALE);
            int bBot = hy + Math.round(198 * HIST_SCALE);
            int bh = (int) Math.round((bBot - bTop) * f);
            gg.fill(bx, bBot - bh, bx + bw, bBot, b.color());
        }
    }

    private void drawLegend(GuiGraphics gg, int px, int y, Legend l) {
        gg.fill(px + 3, y + 1, px + 9, y + 7, l.e.color());
        drawSized(gg, l.e.label(), px + 10, y, LABEL, SIZE_PROP);
        drawSized(gg, l.e.desc(), px + 6 + KEY_LENGTH, y, VALUE, SIZE_PROP);
    }

    private static net.minecraft.client.gui.Font font() {
        return net.minecraft.client.Minecraft.getInstance().font;
    }

    private static String tr(String key) {
        return net.minecraft.network.chat.Component.translatable(key).getString();
    }
}
