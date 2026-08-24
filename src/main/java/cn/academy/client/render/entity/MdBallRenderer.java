package cn.academy.client.render.entity;

import cn.academy.client.render.ACRenderTypes;
import cn.academy.entity.EntityMdBall;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class MdBallRenderer extends EntityRenderer<EntityMdBall> {

    private static final ResourceLocation GLOW = tex("glow");
    private static final ResourceLocation[] CORE = new ResourceLocation[EntityMdBall.MAX_TEXTURES];

    static {
        for (int i = 0; i < CORE.length; ++i) {
            CORE[i] = tex(String.valueOf(i));
        }
    }

    private static ResourceLocation tex(String name) {
        return new ResourceLocation("academy", "textures/effects/mdball/" + name + ".png");
    }

    public MdBallRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(EntityMdBall ball, float yaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffers, int packedLight) {

    }

    public static void draw(EntityMdBall ball, PoseStack pose, MultiBufferSource buffers) {
        if (!ball.updateRenderTick()) return;
        if (ball.expired()) return;

        float alpha = ball.getAlpha();
        float size = ball.getSize();
        if (alpha <= 0 || size <= 0) return;

        pose.pushPose();

        pose.translate(ball.offsetX, ball.offsetY, ball.offsetZ);

        pose.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());

        Matrix4f mat = pose.last().pose();

        int glowAlpha = clamp255(alpha * (0.3f + ball.alphaWiggle * 0.7f));
        quad(buffers.getBuffer(ACRenderTypes.rayGlow(GLOW)), mat, 0.7f * size, glowAlpha);

        int coreAlpha = clamp255(alpha * (0.8f + 0.2f * ball.alphaWiggle));
        int id = Math.floorMod(ball.texID, CORE.length);
        quad(buffers.getBuffer(ACRenderTypes.rayGlow(CORE[id])), mat, 0.5f * size, coreAlpha);

        pose.popPose();
    }

    private static void quad(VertexConsumer vc, Matrix4f mat, float size, int alpha) {
        float x0 = -0.5f * size, x1 = 0.5f * size;
        float y0 = -0.25f * size, y1 = 0.75f * size;
        vertex(vc, mat, x0, y0, 0, 1, alpha);
        vertex(vc, mat, x1, y0, 1, 1, alpha);
        vertex(vc, mat, x1, y1, 1, 0, alpha);
        vertex(vc, mat, x0, y1, 0, 0, alpha);
    }

    private static void vertex(VertexConsumer vc, Matrix4f mat, float x, float y, float u, float v, int alpha) {
        vc.vertex(mat, x, y, 0).uv(u, v).color(255, 255, 255, alpha).endVertex();
    }

    private static int clamp255(double v) {
        int i = (int) (v * 255);
        return i < 0 ? 0 : Math.min(i, 255);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityMdBall e) {
        return GLOW;
    }
}
