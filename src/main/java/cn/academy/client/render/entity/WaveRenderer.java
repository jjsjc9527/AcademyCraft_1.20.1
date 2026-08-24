package cn.academy.client.render.entity;

import cn.academy.Resources;
import cn.academy.client.render.ACRenderTypes;
import cn.academy.entity.EntityWave;
import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.GameTimer;
import cn.lambdalib2.vis.curve.CubicCurve;
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
public class WaveRenderer extends EntityRenderer<EntityWave> {

    private static final ResourceLocation TEXTURE = Resources.getTexture("effects/glow_circle");

    private static final CubicCurve ALPHA_CURVE = new CubicCurve();
    private static final CubicCurve SIZE_CURVE = new CubicCurve();

    static {
        ALPHA_CURVE.addPoint(0, 0);
        ALPHA_CURVE.addPoint(0.2, 1);
        ALPHA_CURVE.addPoint(0.5, 1);
        ALPHA_CURVE.addPoint(0.8, 1);
        ALPHA_CURVE.addPoint(1, 0);

        SIZE_CURVE.addPoint(0, 0.4);
        SIZE_CURVE.addPoint(0.2, 0.8);
        SIZE_CURVE.addPoint(2.5, 1.5);
    }

    private static final float[][] VERTS = {
            {-.5f, -.5f, 0},
            {-.5f, .5f, 0},
            {.5f, .5f, 0},
            {.5f, -.5f, 0}
    };
    private static final float[][] UVS = {
            {0, 0}, {0, 1}, {1, 1}, {1, 0}
    };

    public WaveRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(EntityWave wave, float yaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffers, int packedLight) {

    }

    public static void draw(EntityWave wave, PoseStack pose, MultiBufferSource buffers) {

        double t = (GameTimer.getPausableTime() - wave.spawnTime) * 20.0;
        double maxAlpha = clamp01(ALPHA_CURVE.valueAt(t / EntityWave.LIFE));
        if (maxAlpha <= 0) return;

        VertexConsumer vc = buffers.getBuffer(ACRenderTypes.rippleMark(TEXTURE));

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-wave.yaw));
        pose.mulPose(Axis.XP.rotationDegrees(wave.pitch));
        pose.translate(0, 0, t / 40.0);

        double sizeScale = SIZE_CURVE.valueAt(Math.max(0, Math.min(1.62, t / 20.0)));

        for (EntityWave.Ring ring : wave.rings) {
            double a = clamp01(ALPHA_CURVE.valueAt((t - ring.timeOffset) / ring.life));
            float realAlpha = (float) Math.min(maxAlpha, a);
            if (realAlpha <= 0) continue;

            pose.pushPose();
            pose.translate(0, 0, ring.offset);
            float s = (float) (ring.size * sizeScale);
            pose.scale(s, s, 1);

            int alpha = Colors.f2i(realAlpha * 0.7f);
            Matrix4f mat = pose.last().pose();
            for (int i = 0; i < 4; i++) {
                vc.vertex(mat, VERTS[i][0], VERTS[i][1], VERTS[i][2])
                        .uv(UVS[i][0], UVS[i][1])
                        .color(255, 255, 255, alpha)
                        .endVertex();
            }

            pose.popPose();
        }

        pose.popPose();
    }

    private static double clamp01(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityWave entity) {
        return TEXTURE;
    }
}
