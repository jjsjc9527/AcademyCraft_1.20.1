package cn.academy.client.render.entity;

import cn.academy.Resources;
import cn.academy.client.render.ACRenderTypes;
import cn.academy.entity.EntityThunderStrike;
import cn.lambdalib2.util.GameTimer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class ThunderStrikeRenderer extends EntityRenderer<EntityThunderStrike> {

    private static final ResourceLocation TEXTURE = Resources.getTexture("effects/thunder/bolt");

    private static final float HEIGHT = (float) cn.academy.client.render.util.ThunderArcs.SKY_HEIGHT;
    private static final float WIDTH = 8f;

    private static final int PLANES = 2;

    public ThunderStrikeRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(EntityThunderStrike e, float yaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffers, int packedLight) {

    }

    public static void draw(EntityThunderStrike e, PoseStack pose, MultiBufferSource buffers) {
        double age = GameTimer.getPausableTime() - e.spawnTime;
        if (age >= EntityThunderStrike.LIFESPAN) return;
        float alpha = alphaAt((float) age);
        if (alpha <= 0f) return;

        float u0 = e.mirror ? 1f : 0f;
        float u1 = e.mirror ? 0f : 1f;
        int a = Math.min(255, (int) (alpha * 255f));
        float hw = WIDTH / 2f;
        VertexConsumer vc = buffers.getBuffer(ACRenderTypes.thunder(TEXTURE));

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(e.yawOffset));

        for (int i = 0; i < PLANES; i++) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(180f / PLANES * i));
            Matrix4f m = pose.last().pose();

            vertex(vc, m, -hw, 0f,     u0, 1f, a);
            vertex(vc, m,  hw, 0f,     u1, 1f, a);
            vertex(vc, m,  hw, HEIGHT, u1, 0f, a);
            vertex(vc, m, -hw, HEIGHT, u0, 0f, a);
            pose.popPose();
        }
        pose.popPose();
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, float x, float y, float u, float v, int a) {
        vc.vertex(m, x, y, 0f).uv(u, v).color(255, 255, 255, a).endVertex();
    }

    private static float alphaAt(float age) {
        float t = age / (float) EntityThunderStrike.LIFESPAN;
        float env = 1f - t;

        float flick = 0.55f + 0.45f * (float) (0.5 + 0.5 * Math.cos(age * (Math.PI * 2) * 6));
        float a = env * flick;
        return a < 0f ? 0f : a;
    }

    @Override
    public ResourceLocation getTextureLocation(EntityThunderStrike e) {
        return TEXTURE;
    }
}
