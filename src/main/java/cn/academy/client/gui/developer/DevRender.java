package cn.academy.client.gui.developer;

import cn.academy.client.render.ACGuiShaders;
import cn.lambdalib2.util.HudUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

@OnlyIn(Dist.CLIENT)
public final class DevRender {

    private DevRender() {}

    public static Matrix4f save() {
        return new Matrix4f(HudUtils.getMatrix());
    }

    public static void restore(Matrix4f saved) {
        HudUtils.setMatrix(saved);
    }

    public static void translate(double x, double y, double z) {
        HudUtils.setMatrix(new Matrix4f(HudUtils.getMatrix()).translate((float) x, (float) y, (float) z));
    }

    public static void scale(double sx, double sy) {
        HudUtils.setMatrix(new Matrix4f(HudUtils.getMatrix()).scale((float) sx, (float) sy, 1f));
    }

    public static void enableDepth()  { RenderSystem.enableDepthTest(); }
    public static void disableDepth() { RenderSystem.disableDepthTest(); }
    public static void depthMask(boolean flag) { RenderSystem.depthMask(flag); }
    public static void colorMask(boolean on)   { RenderSystem.colorMask(on, on, on, on); }

    public static void depthFunc(int func) { RenderSystem.depthFunc(func); }

    public static void beginNoCull() { RenderSystem.disableCull(); }
    public static void endNoCull()   { RenderSystem.enableCull(); }

    public static void resetState() {
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(true);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.disableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    public static void color(double r, double g, double b, double a) {
        RenderSystem.setShaderColor((float) r, (float) g, (float) b, (float) a);
    }

    public static void rect(ResourceLocation tex, double x, double y, double w, double h) {
        HudUtils.loadTexture(tex);
        HudUtils.rect(x, y, w, h);
    }

    public static void rectMono(ResourceLocation tex, double x, double y, double w, double h) {
        ShaderInstance sh = ACGuiShaders.mono();
        if (sh == null) { rect(tex, x, y, w, h); return; }
        RenderSystem.setShader(() -> sh);
        RenderSystem.setShaderTexture(0, tex);
        quad(x, y, w, h);
    }

    public static void rectCutout(ResourceLocation tex, float threshold, double x, double y, double w, double h) {
        ShaderInstance sh = ACGuiShaders.guiCutout();
        if (sh == null) { rect(tex, x, y, w, h); return; }
        RenderSystem.setShader(() -> sh);
        RenderSystem.setShaderTexture(0, tex);
        sh.safeGetUniform("Threshold").set(threshold);
        quad(x, y, w, h);
    }

    public static void rectProgBar(ResourceLocation circle, ResourceLocation mask, float progress,
                                   double x, double y, double w, double h) {
        ShaderInstance sh = ACGuiShaders.skillProgBar();
        if (sh == null) { rect(circle, x, y, w, h); return; }
        RenderSystem.setShader(() -> sh);
        RenderSystem.setShaderTexture(0, circle);
        RenderSystem.setShaderTexture(1, mask);
        sh.safeGetUniform("Progress").set(progress);
        quad(x, y, w, h);

        RenderSystem.setShaderTexture(1, 0);
    }

    public static void texQuad(ResourceLocation tex,
                               double x0, double y0, double x1, double y1,
                               double x2, double y2, double x3, double y3) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, tex);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        Matrix4f m = HudUtils.getMatrix();
        float z = (float) HudUtils.zLevel;
        Tesselator t = Tesselator.getInstance();
        BufferBuilder bb = t.getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bb.vertex(m, (float) x0, (float) y0, z).uv(0f, 0f).endVertex();
        bb.vertex(m, (float) x1, (float) y1, z).uv(0f, 1f).endVertex();
        bb.vertex(m, (float) x2, (float) y2, z).uv(1f, 1f).endVertex();
        bb.vertex(m, (float) x3, (float) y3, z).uv(1f, 0f).endVertex();
        t.end();
    }

    public static void cpbarQuad(double uvW, double uvH, float r, float g, float b, float a, double... verts) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        Matrix4f m = HudUtils.getMatrix();

        final float z = -90f;
        Tesselator t = Tesselator.getInstance();
        BufferBuilder bb = t.getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (int i = 0; i < 4; ++i) {
            double x = verts[i * 2], y = verts[i * 2 + 1];
            bb.vertex(m, (float) x, (float) y, z)
                    .uv((float) (x / uvW), (float) (y / uvH))
                    .color(r, g, b, a)
                    .endVertex();
        }
        t.end();
    }

    public static boolean useTwoSampler(ShaderInstance sh, ResourceLocation tex0, ResourceLocation tex1) {
        if (sh == null) {
            return false;
        }
        RenderSystem.setShader(() -> sh);
        RenderSystem.setShaderTexture(0, tex0);
        RenderSystem.setShaderTexture(1, tex1);
        return true;
    }

    private static void quad(double x, double y, double w, double h) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        Matrix4f m = HudUtils.getMatrix();
        float z = (float) HudUtils.zLevel;
        Tesselator t = Tesselator.getInstance();
        BufferBuilder bb = t.getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bb.vertex(m, (float) x,       (float) (y + h), z).uv(0f, 1f).endVertex();
        bb.vertex(m, (float) (x + w), (float) (y + h), z).uv(1f, 1f).endVertex();
        bb.vertex(m, (float) (x + w), (float) y,       z).uv(1f, 0f).endVertex();
        bb.vertex(m, (float) x,       (float) y,       z).uv(0f, 0f).endVertex();
        t.end();
    }

    public static double guiScale() {
        return Minecraft.getInstance().getWindow().getGuiScale();
    }
}
