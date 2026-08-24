package cn.academy.client.render.entity;

import cn.academy.client.render.ACRenderTypes;
import cn.academy.entity.EntityDiamondShield;
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
public class DiamondShieldRenderer extends EntityRenderer<EntityDiamondShield> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("academy", "textures/effects/diamond_shield.png");

    private static final double FADE_IN_S = 2 / 20.0;

    private static final double FADE_OUT_S = 5 / 20.0;

    private static final float[][] V = {
            {-1, 0, 0}, {0, -1, 0}, {1, 0, 0}, {0, 1, 0}, {0, 0, 1}
    };
    private static final float[][] UV = {
            {0, 0}, {1, 1}, {0, 0}, {1, 1}, {0, 1}
    };
    private static final int[] TRI = {0, 1, 4, 1, 2, 4, 2, 3, 4, 3, 0, 4};

    public DiamondShieldRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(EntityDiamondShield e, float yaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffers, int packedLight) {

    }

    public static void draw(EntityDiamondShield e, PoseStack pose, MultiBufferSource buffers) {
        if (e.getOwner() == null) return;
        e.updatePos();

        double age = e.age();
        double alphaF = Math.min(age / Math.max(1e-6, FADE_IN_S), 1.0);
        if (FADE_OUT_S > 0) {
            double left = e.lifespan - age;
            alphaF = Math.min(alphaF, Math.max(0, left) / FADE_OUT_S);
        }
        int alpha = (int) (Math.min(1.0, alphaF) * 255);
        if (alpha <= 0) return;

        pose.pushPose();

        pose.mulPose(Axis.YP.rotationDegrees(-e.getYRot()));
        pose.mulPose(Axis.XP.rotationDegrees(e.getXRot()));
        pose.scale(EntityDiamondShield.RENDER_SCALE,
                EntityDiamondShield.RENDER_SCALE, EntityDiamondShield.RENDER_SCALE);

        Matrix4f mat = pose.last().pose();
        VertexConsumer vc = buffers.getBuffer(ACRenderTypes.rayGlow(TEXTURE));
        for (int i = 0; i < TRI.length; i += 3) {
            int a = TRI[i], b = TRI[i + 1], c = TRI[i + 2];
            vertex(vc, mat, a, alpha);
            vertex(vc, mat, b, alpha);
            vertex(vc, mat, c, alpha);
            vertex(vc, mat, c, alpha);
        }

        pose.popPose();
    }

    private static void vertex(VertexConsumer vc, Matrix4f mat, int idx, int alpha) {
        vc.vertex(mat, V[idx][0], V[idx][1], V[idx][2])
                .uv(UV[idx][0], UV[idx][1])
                .color(255, 255, 255, alpha)
                .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(EntityDiamondShield e) {
        return TEXTURE;
    }
}
