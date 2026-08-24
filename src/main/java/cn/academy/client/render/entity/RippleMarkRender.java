package cn.academy.client.render.entity;

import cn.academy.Resources;
import cn.academy.client.render.ACRenderTypes;
import cn.academy.entity.EntityRippleMark;
import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.GameTimer;
import cn.lambdalib2.util.MathUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class RippleMarkRender extends EntityRenderer<EntityRippleMark> {

    private static final ResourceLocation TEXTURE = Resources.getTexture("effects/ripple");

    private static final double CYCLE = 3.6;
    private static final double[] timeOffsets = {0, -1.2, -2.4};

    private static final double[][] VERTS = {
            {-.5, 0, -.5},
            {.5, 0, -.5},
            {.5, 0, .5},
            {-.5, 0, .5}
    };
    private static final double[][] UVS = {
            {0, 0},
            {0, 1},
            {1, 1},
            {1, 0}
    };

    public RippleMarkRender(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(EntityRippleMark mark, float yaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffers, int packedLight) {

    }

    public static void draw(EntityRippleMark mark, PoseStack pose, MultiBufferSource buffers) {
        double dt = GameTimer.getPausableTime() - mark.creationTime;

        VertexConsumer vc = buffers.getBuffer(ACRenderTypes.rippleMark(TEXTURE));

        pose.pushPose();

        for (double timeOffset : timeOffsets) {
            pose.pushPose();

            double mod = (dt - timeOffset) % CYCLE;
            float size = getSize(mod);

            pose.translate(0, getHeight(mod), 0);
            pose.scale(size, 1, size);

            int alpha = Colors.f2i(getAlpha(mod));
            Matrix4f mat = pose.last().pose();
            for (int i = 0; i < 4; ++i) {
                vc.vertex(mat, (float) VERTS[i][0], (float) VERTS[i][1], (float) VERTS[i][2])
                        .uv((float) UVS[i][0], (float) UVS[i][1])
                        .color(mark.color.r, mark.color.g, mark.color.b, alpha)
                        .endVertex();
            }

            pose.popPose();
        }

        pose.popPose();
    }

    private static float getHeight(double mod) {
        return (float) mod * 3e-1f;
    }

    private static float getAlpha(double mod) {
        final float BIN = 1.6f, BOUT = 1.6f;
        if (mod < BIN) {
            return (float) mod / BIN;
        }
        if (mod > CYCLE - BOUT) {
            return (float) (1 - ((float) mod - (CYCLE - BOUT)) / BOUT);
        }
        return 1.0f;
    }

    private static float getSize(double mod) {
        return MathUtils.lerpf(1.9f, 1.4f, (float) (mod / CYCLE));
    }

    @Override
    public ResourceLocation getTextureLocation(EntityRippleMark entity) {
        return TEXTURE;
    }
}
