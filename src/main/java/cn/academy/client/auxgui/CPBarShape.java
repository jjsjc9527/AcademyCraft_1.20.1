package cn.academy.client.auxgui;

import cn.academy.client.gui.SvgShape;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public final class CPBarShape {

    private CPBarShape() {}

    public static final ResourceLocation UCP = shape("ucp");
    public static final ResourceLocation CP = shape("cp");
    public static final ResourceLocation OV = shape("ov");
    public static final ResourceLocation AVATAR = shape("avatar");

    private static ResourceLocation shape(String name) {
        return new ResourceLocation("academy", "shapes/cpbar/" + name + ".svg");
    }

    private static final float STROKE = 2.0f;

    public static void drawStatic(Matrix4f mat, ResourceLocation loc, float sx, float sy,
                                  int rgb, float alpha, boolean stroke) {
        SvgShape s = SvgShape.get(loc);
        if (s == null) {
            return;
        }
        if (stroke) {
            float[][] e = expand(s.xs, s.ys, STROKE);
            fillRaw(mat, e[0], e[1], sx, sy, 0x000000, alpha);
        }
        fillRaw(mat, s.xs, s.ys, sx, sy, rgb, alpha);
    }

    public static void drawBar(Matrix4f mat, ResourceLocation loc, float sx, float sy,
                               int rgb, float alpha, float prog, boolean stroke) {
        SvgShape s = SvgShape.get(loc);
        SvgShape base = SvgShape.get(UCP);
        if (s == null || base == null) {
            return;
        }
        float[][] q = barQuad(s, base, prog);
        if (q == null) {
            return;
        }
        if (stroke) {
            float[][] e = expand(q[0], q[1], STROKE);
            fillRaw(mat, e[0], e[1], sx, sy, 0x000000, alpha);
        }
        fillRaw(mat, q[0], q[1], sx, sy, rgb, alpha);
    }

    private static float[][] barQuad(SvgShape s, SvgShape base, float prog) {

        float yT = s.minY(), yB = s.maxY();
        float xLT = Float.MAX_VALUE, xLB = Float.MAX_VALUE, xR = -Float.MAX_VALUE;
        for (int i = 0; i < s.size(); i++) {
            boolean top = Math.abs(s.ys[i] - yT) < 0.5f;
            if (top) {
                xLT = Math.min(xLT, s.xs[i]);
            } else {
                xLB = Math.min(xLB, s.xs[i]);
            }
            xR = Math.max(xR, s.xs[i]);
        }
        if (xLT == Float.MAX_VALUE || xLB == Float.MAX_VALUE) {
            return null;
        }

        float travel = base.maxX() - baseLowerLeft(base);
        float off = (1.0f - Math.max(0.0f, Math.min(1.0f, prog))) * travel;
        float aLT = xLT + off;
        float aLB = xLB + off;
        if (aLT >= xR) {
            return null;
        }
        if (aLB <= xR) {
            return new float[][]{{aLT, xR, xR, aLB}, {yT, yT, yB, yB}};
        }

        float t = (xR - aLT) / (aLB - aLT);
        return new float[][]{{aLT, xR, xR}, {yT, yT, yT + t * (yB - yT)}};
    }

    private static float baseLowerLeft(SvgShape base) {
        float yB = base.maxY();
        float v = Float.MAX_VALUE;
        for (int i = 0; i < base.size(); i++) {
            if (Math.abs(base.ys[i] - yB) < 0.5f) {
                v = Math.min(v, base.xs[i]);
            }
        }
        return v == Float.MAX_VALUE ? base.minX() : v;
    }

    private static float[][] expand(float[] xs, float[] ys, float d) {
        int n = xs.length;
        float cx = 0, cy = 0;
        for (int i = 0; i < n; i++) {
            cx += xs[i];
            cy += ys[i];
        }
        cx /= n;
        cy /= n;
        float[] ox = new float[n], oy = new float[n];
        for (int i = 0; i < n; i++) {
            float dx = xs[i] - cx, dy = ys[i] - cy;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len < 1e-5f) {
                ox[i] = xs[i];
                oy[i] = ys[i];
            } else {
                ox[i] = xs[i] + dx / len * d;
                oy[i] = ys[i] + dy / len * d;
            }
        }
        return new float[][]{ox, oy};
    }

    private static void fillRaw(Matrix4f mat, float[] xs, float[] ys, float sx, float sy,
                                int rgb, float alpha) {
        if (xs.length < 3 || alpha <= 0.001f) {
            return;
        }
        float r = ((rgb >> 16) & 0xFF) / 255.0f;
        float g = ((rgb >> 8) & 0xFF) / 255.0f;
        float b = (rgb & 0xFF) / 255.0f;

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        Tesselator t = Tesselator.getInstance();
        BufferBuilder bb = t.getBuilder();
        bb.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);

        float z = (float) cn.lambdalib2.util.HudUtils.zLevel;

        boolean rev = shoelace(xs, ys) > 0;
        for (int k = 0; k < xs.length; k++) {
            int i = rev ? xs.length - 1 - k : k;
            bb.vertex(mat, xs[i] * sx, ys[i] * sy, z).color(r, g, b, alpha).endVertex();
        }
        t.end();
    }

    private static float shoelace(float[] xs, float[] ys) {
        float s = 0;
        int n = xs.length;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            s += xs[i] * ys[j] - xs[j] * ys[i];
        }
        return s;
    }

    public static final int C_UCP = 0xF9F9F9;

    public static final int C_CP = 0xDAEFF1;

    public static final int C_AVATAR = 0x90D5E6;

    public static final int C_OV = 0xE11F32;
}
