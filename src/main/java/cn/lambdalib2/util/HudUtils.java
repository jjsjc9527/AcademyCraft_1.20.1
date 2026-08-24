package cn.lambdalib2.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

@OnlyIn(Dist.CLIENT)
public final class HudUtils {

    public static double zLevel = 0;
    private static double stack = Double.NEGATIVE_INFINITY;

    private static Matrix4f matrix = new Matrix4f();

    private HudUtils() {}

    public static void setPose(PoseStack pose) {
        matrix = pose.last().pose();
    }

    public static void setMatrix(Matrix4f m) {
        matrix = m;
    }

    public static Matrix4f getMatrix() {
        return matrix;
    }

    public static void pushZLevel() {
        if (stack != Double.NEGATIVE_INFINITY)
            throw new RuntimeException("Stack overflow");
        stack = zLevel;
    }

    public static void popZLevel() {
        if (stack == Double.NEGATIVE_INFINITY)
            throw new RuntimeException("Stack underflow");
        zLevel = stack;
        stack = Double.NEGATIVE_INFINITY;
    }

    public static void loadTexture(ResourceLocation res) {
        RenderSystem.setShaderTexture(0, res);
    }

    public static void rect(double width, double height) {
        rect(0, 0, width, height);
    }

    public static void rect(double x, double y, double width, double height) {
        rawRect(x, y, 0, 0, width, height, 1, 1);
    }

    public static void rect(double x, double y, double u, double v, double width, double height) {
        rect(x, y, u, v, width, height, width, height);
    }

    public static void rect(double x, double y, double u, double v, double width, double height,
                            double texWidth, double texHeight) {
        int[] size = boundTextureSize();
        double f = 1.0 / size[0], f1 = 1.0 / size[1];
        drawTexQuad(x, y, width, height, u * f, v * f1, (u + texWidth) * f, (v + texHeight) * f1);
    }

    public static void rawRect(double x, double y, double u, double v, double width, double height,
                               double texWidth, double texHeight) {
        drawTexQuad(x, y, width, height, u, v, u + texWidth, v + texHeight);
    }

    public static void drawRectOutline(double x, double y, double w, double h, float lineWidth) {
        double lw = lineWidth * 0.2;
        x -= lw;
        y -= lw;
        w += 2 * lw;
        h += 2 * lw;

        colorRect(x, y, w, lineWidth);
        colorRect(x, y + h - lineWidth, w, lineWidth);
        colorRect(x, y, lineWidth, h);
        colorRect(x + w - lineWidth, y, lineWidth, h);
    }

    public static void colorRect(double x, double y, double width, double height) {
        RenderSystem.setShader(GameRenderer::getPositionShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        float z = (float) zLevel;
        Tesselator t = Tesselator.getInstance();
        BufferBuilder bb = t.getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        bb.vertex(matrix, (float) x, (float) (y + height), z).endVertex();
        bb.vertex(matrix, (float) (x + width), (float) (y + height), z).endVertex();
        bb.vertex(matrix, (float) (x + width), (float) y, z).endVertex();
        bb.vertex(matrix, (float) x, (float) y, z).endVertex();
        t.end();
    }

    private static void drawTexQuad(double x, double y, double w, double h,
                                    double u0, double v0, double u1, double v1) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        float z = (float) zLevel;
        Tesselator t = Tesselator.getInstance();
        BufferBuilder bb = t.getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bb.vertex(matrix, (float) x, (float) (y + h), z).uv((float) u0, (float) v1).endVertex();
        bb.vertex(matrix, (float) (x + w), (float) (y + h), z).uv((float) u1, (float) v1).endVertex();
        bb.vertex(matrix, (float) (x + w), (float) y, z).uv((float) u1, (float) v0).endVertex();
        bb.vertex(matrix, (float) x, (float) y, z).uv((float) u0, (float) v0).endVertex();
        t.end();
    }

    public static int[] boundTextureSize() {
        int id = RenderSystem.getShaderTexture(0);
        RenderSystem.bindTexture(id);
        int w = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
        int h = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
        if (w <= 0) w = 256;
        if (h <= 0) h = 256;
        return new int[]{w, h};
    }
}
