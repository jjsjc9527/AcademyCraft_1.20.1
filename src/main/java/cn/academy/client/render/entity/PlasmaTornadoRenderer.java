package cn.academy.client.render.entity;

import cn.academy.client.render.ACRenderTypes;
import cn.academy.entity.EntityPlasmaTornado;
import cn.lambdalib2.util.Colors;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PlasmaTornadoRenderer extends EntityRenderer<EntityPlasmaTornado> {

    public PlasmaTornadoRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(EntityPlasmaTornado eff, float yaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffers, int packedLight) {

    }

    public static void draw(EntityPlasmaTornado eff, PoseStack pose, MultiBufferSource buffers) {
        if (eff.alpha <= 0) {
            return;
        }
        VertexConsumer vc = buffers.getBuffer(ACRenderTypes.tornado());

        int alpha = Colors.f2i((float) (eff.alpha * 0.7));

        pose.pushPose();
        pose.scale(eff.scale, eff.scale, eff.scale);
        StormWingRenderer.drawTornado(eff.tornado, pose.last().pose(), vc, alpha, eff.density);
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(EntityPlasmaTornado entity) {
        return cn.academy.Resources.getTexture("effects/tornado_ring");
    }
}
