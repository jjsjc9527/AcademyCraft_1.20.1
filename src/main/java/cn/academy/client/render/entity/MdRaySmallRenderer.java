package cn.academy.client.render.entity;

import cn.academy.entity.EntityMdRaySmall;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MdRaySmallRenderer extends EntityRenderer<EntityMdRaySmall> {

    private static ResourceLocation ray(String name) {
        return new ResourceLocation("academy", "textures/effects/mdray_small/" + name + ".png");
    }

    private static final RailgunFXRenderer.GlowStyle GLOW = new RailgunFXRenderer.GlowStyle(
            ray("blend_in"), ray("tile"), ray("blend_out"), 0.3, 0, 0);

    private static final RailgunFXRenderer.Cylinder CYL_IN =
            new RailgunFXRenderer.Cylinder(216, 248, 216, 230, 0.03, 0.98);
    private static final RailgunFXRenderer.Cylinder CYL_OUT =
            new RailgunFXRenderer.Cylinder(106, 242, 106, 50, 0.045, 1.00);

    public MdRaySmallRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(EntityMdRaySmall ray, float yaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffers, int packedLight) {

    }

    public static void draw(EntityMdRaySmall ray, float partialTick, Vec3 camPos,
                            PoseStack pose, MultiBufferSource buffers) {
        ray.onRenderTick();

        Vec3 dir = RailgunFXRenderer.lookVec(ray.getYRot(), ray.getXRot());

        pose.pushPose();

        if (ray.getPath() != null) {

            RailgunFXRenderer.drawGlowPath(ray, camPos, pose, buffers, GLOW, Vec3.ZERO, null);
            RailgunFXRenderer.drawCylinderPath(ray, CYL_IN, pose, buffers, Vec3.ZERO);
            RailgunFXRenderer.drawCylinderPath(ray, CYL_OUT, pose, buffers, Vec3.ZERO);
        } else {
            drawOne(ray, dir, camPos, pose, buffers);
        }
        pose.popPose();
    }

    public static void drawOne(cn.academy.entity.IRay ray, Vec3 dir, Vec3 camPos,
                               PoseStack pose, MultiBufferSource buffers) {
        RailgunFXRenderer.drawGlow(ray, dir, camPos, pose, buffers, GLOW, Vec3.ZERO, null);
        RailgunFXRenderer.drawCylinder(ray, dir, CYL_IN, pose, buffers, Vec3.ZERO);
        RailgunFXRenderer.drawCylinder(ray, dir, CYL_OUT, pose, buffers, Vec3.ZERO);
    }

    public static ResourceLocation glowTile() {
        return GLOW.tile();
    }

    @Override
    public ResourceLocation getTextureLocation(EntityMdRaySmall e) {
        return GLOW.tile();
    }
}
