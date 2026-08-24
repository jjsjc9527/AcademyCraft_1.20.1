package cn.academy.client.render.entity;

import cn.academy.entity.EntityMDRay;
import cn.lambdalib2.util.MathUtils;
import cn.lambdalib2.util.ViewOptimize;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MDRayRenderer extends EntityRenderer<EntityMDRay> {

    private static ResourceLocation ray(String name) {
        return new ResourceLocation("academy", "textures/effects/mdray/" + name + ".png");
    }

    private static final RailgunFXRenderer.GlowStyle GLOW = new RailgunFXRenderer.GlowStyle(
            ray("blend_in"), ray("tile"), ray("blend_out"), 1.5, 0, 0);

    private static final RailgunFXRenderer.Cylinder CYL_IN =
            new RailgunFXRenderer.Cylinder(216, 248, 216, 230, 0.17, 0.98);
    private static final RailgunFXRenderer.Cylinder CYL_OUT =
            new RailgunFXRenderer.Cylinder(106, 242, 106, 50, 0.22, 1.00);

    public MDRayRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(EntityMDRay ray, float yaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffers, int packedLight) {

    }

    public static void draw(EntityMDRay ray, float partialTick, Vec3 camPos,
                            PoseStack pose, MultiBufferSource buffers) {
        ray.onRenderTick();

        Vec3 dir = RailgunFXRenderer.lookVec(ray.getYRot(), ray.getXRot());

        Vec3 handOff = ray.needsViewOptimize() ? handOffset(ray) : Vec3.ZERO;
        Vec3 fpUp = ViewOptimize.isFirstPerson(ray)

                ? cn.academy.gravity.RotationUtil.vecPlayerToWorld(new Vec3(0, 1, -0.5), ray.gravAtFire)
                : null;

        pose.pushPose();
        if (ray.getPath() != null) {

            RailgunFXRenderer.drawGlowPath(ray, camPos, pose, buffers, GLOW, handOff, fpUp);
            RailgunFXRenderer.drawCylinderPath(ray, CYL_IN, pose, buffers, handOff);
            RailgunFXRenderer.drawCylinderPath(ray, CYL_OUT, pose, buffers, handOff);
        } else {
            RailgunFXRenderer.drawGlow(ray, dir, camPos, pose, buffers, GLOW, handOff, fpUp);
            RailgunFXRenderer.drawCylinder(ray, dir, CYL_IN, pose, buffers, handOff);
            RailgunFXRenderer.drawCylinder(ray, dir, CYL_OUT, pose, buffers, handOff);
        }
        pose.popPose();
    }

    private static Vec3 handOffset(EntityMDRay ray) {
        Vec3 vo = ViewOptimize.getFixVector(ray)
                .yRot((float) MathUtils.toRadians(270 - ray.localYawAtFire));
        return cn.academy.gravity.RotationUtil.vecPlayerToWorld(vo, ray.gravAtFire);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityMDRay e) {
        return GLOW.tile();
    }
}
