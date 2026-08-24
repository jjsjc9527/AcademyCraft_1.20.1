package cn.academy.client.render.entity;

import cn.academy.client.render.util.ArcFactory;
import cn.academy.entity.EntitySurroundArc;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SurroundArcRenderer extends EntityRenderer<EntitySurroundArc> {

    public SurroundArcRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(EntitySurroundArc esa, float yaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffers, int packedLight) {

    }

    public static void draw(EntitySurroundArc esa, PoseStack pose, MultiBufferSource buffers) {
        if (esa.draw && esa.getArcHandler() != null) {
            pose.pushPose();

            net.minecraft.world.entity.Entity follow = esa.getFollowEntity();
            if (follow != null) {
                net.minecraft.core.Direction g = cn.academy.gravity.ACGravity.getGravityDirection(follow);
                if (g != net.minecraft.core.Direction.DOWN) {
                    pose.mulPose(cn.academy.gravity.RotationUtil.getCameraRotationQuaternion(g));
                }
            }
            pose.mulPose(Axis.YP.rotationDegrees(-esa.getYRot()));
            esa.getArcHandler().drawAll(pose, buffers);

            pose.popPose();
        }
    }

    @Override
    public ResourceLocation getTextureLocation(EntitySurroundArc entity) {
        return ArcFactory.TEXTURE;
    }
}
