package cn.academy.client.render.entity;

import cn.academy.client.render.ACRenderTypes;
import cn.academy.entity.EntityMdShield;
import cn.lambdalib2.util.GameTimer;
import cn.lambdalib2.util.MathUtils;
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
public class MdShieldRenderer extends EntityRenderer<EntityMdShield> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("academy", "textures/effects/mdshield.png");

    private static final double SPIN_SCALE = 1.0;

    private static final double FADE_IN_S = 6 / 20.0, GROW_S = 15 / 20.0, SPINUP_S = 30 / 20.0;

    public MdShieldRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(EntityMdShield e, float yaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffers, int packedLight) {

    }

    public static void draw(EntityMdShield e, PoseStack pose, MultiBufferSource buffers) {
        if (e.getOwner() == null) return;
        e.updatePos();

        double age = e.age();
        double now = GameTimer.getPausableTime();

        double dt = e.lastRender == 0 ? 0 : now - e.lastRender;
        e.lastRender = now;
        float spin = MathUtils.lerpf(0.8f, 2f, (float) Math.min(age / SPINUP_S, 1));
        e.rotation += (float) (spin * dt * SPIN_SCALE);
        if (e.rotation >= 360f) e.rotation -= 360f;

        float size = EntityMdShield.SIZE * MathUtils.lerpf(0.2f, 1f, (float) Math.min(age / GROW_S, 1));
        int alpha = (int) (Math.min(age / FADE_IN_S, 1) * 255);
        if (alpha <= 0 || size <= 0) return;

        pose.pushPose();

        pose.mulPose(Axis.YP.rotationDegrees(-e.getYRot()));
        pose.mulPose(Axis.XP.rotationDegrees(e.getXRot()));
        pose.mulPose(Axis.ZP.rotationDegrees(e.rotation));
        pose.scale(size, size, 1);

        Matrix4f mat = pose.last().pose();
        VertexConsumer vc = buffers.getBuffer(ACRenderTypes.rayGlow(TEXTURE));
        vertex(vc, mat, -0.5f, -0.5f, 0, 1, alpha);
        vertex(vc, mat, 0.5f, -0.5f, 1, 1, alpha);
        vertex(vc, mat, 0.5f, 0.5f, 1, 0, alpha);
        vertex(vc, mat, -0.5f, 0.5f, 0, 0, alpha);

        pose.popPose();
    }

    private static void vertex(VertexConsumer vc, Matrix4f mat, float x, float y, float u, float v, int alpha) {
        vc.vertex(mat, x, y, 0).uv(u, v).color(255, 255, 255, alpha).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(EntityMdShield e) {
        return TEXTURE;
    }
}
