package cn.academy.client.render.entity;

import cn.academy.entity.EntityMdRayBarrage;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MdRayBarrageRenderer extends EntityRenderer<EntityMdRayBarrage> {

    public MdRayBarrageRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(EntityMdRayBarrage ray, float yaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffers, int packedLight) {

    }

    public static void draw(EntityMdRayBarrage ray, float partialTick, Vec3 camPos,
                            PoseStack pose, MultiBufferSource buffers) {
        ray.onRenderTick();

        Vec3 axis = RailgunFXRenderer.lookVec(ray.getYRot(), ray.getXRot());

        ray.updateFlicker(axis);
        Vec3[] basis = cn.academy.ability.vanilla.meltdowner.skill.MdBarrage.basisOf(axis);

        pose.pushPose();
        for (EntityMdRayBarrage.SubRay sr : ray.getSubrays()) {
            Vec3 dir = cn.academy.ability.vanilla.meltdowner.skill.MdBarrage.subRayDir(
                    axis, basis[0], basis[1], sr.yawOff(), sr.pitchOff());

            MdRaySmallRenderer.drawOne(new Clipped(ray, sr.reach()), dir, camPos, pose, buffers);
        }
        pose.popPose();
    }

    private record Clipped(EntityMdRayBarrage src, double reach) implements cn.academy.entity.IRay {
        @Override public void onRenderTick() { }
        @Override public Vec3 getRayPosition() { return src.getRayPosition(); }
        @Override public boolean needsViewOptimize() { return src.needsViewOptimize(); }
        @Override public double getLength() { return Math.min(src.getLength(), reach); }
        @Override public double getAlpha() { return src.getAlpha(); }
        @Override public double getGlowAlpha() { return src.getGlowAlpha(); }
        @Override public double getStartFix() { return src.getStartFix(); }
        @Override public double getWidth() { return src.getWidth(); }
        @Override public net.minecraft.world.entity.player.Player getPlayer() { return src.getPlayer(); }
    }

    @Override
    public ResourceLocation getTextureLocation(EntityMdRayBarrage e) {
        return MdRaySmallRenderer.glowTile();
    }
}
