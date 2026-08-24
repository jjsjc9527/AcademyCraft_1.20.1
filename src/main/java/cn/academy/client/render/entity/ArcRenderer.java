package cn.academy.client.render.entity;

import cn.academy.client.render.ACRenderTypes;
import cn.academy.client.render.util.ArcFactory;
import cn.academy.entity.EntityArc;
import cn.lambdalib2.util.ViewOptimize;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ArcRenderer extends EntityRenderer<EntityArc> {

    public ArcRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(EntityArc arc, float yaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffers, int packedLight) {

    }

    public static void draw(EntityArc arc, float partialTick, PoseStack pose, MultiBufferSource buffers) {
        if (!arc.isShown()) {
            return;
        }
        float alphaMul = arc.alphaAt(partialTick);
        if (alphaMul <= 0f) {
            return;
        }

        pose.pushPose();

        boolean voFallback = false;
        if (arc.viewOptimize) {
            double a = -Math.toRadians(arc.getYRot());
            double b = -Math.toRadians(arc.getXRot());
            net.minecraft.world.phys.Vec3 f = new net.minecraft.world.phys.Vec3(
                    Math.sin(a) * Math.cos(b), Math.sin(b), Math.cos(a) * Math.cos(b));
            net.minecraft.core.Direction g =
                    cn.academy.gravity.ACGravity.getGravityDirection(arc.getPlayer());
            net.minecraft.world.phys.Vec3 up =
                    new net.minecraft.world.phys.Vec3(-g.getStepX(), -g.getStepY(), -g.getStepZ());
            net.minecraft.world.phys.Vec3 r = f.cross(up);
            if (r.lengthSqr() < 1.0E-6) {
                voFallback = true;
            } else {
                r = r.normalize();
                net.minecraft.world.phys.Vec3 u = r.cross(f);
                net.minecraft.world.phys.Vec3 v = ViewOptimize.getFixVector(arc);
                net.minecraft.world.phys.Vec3 world =
                        f.scale(v.x).add(u.scale(v.y)).add(r.scale(v.z));
                pose.translate(world.x, world.y, world.z);
            }
        }

        if (arc.getPath() != null) {
            drawPath(arc, alphaMul, pose, buffers);
            pose.popPose();
            return;
        }

        pose.mulPose(Axis.YN.rotationDegrees(arc.getYRot() + 90));
        pose.mulPose(Axis.ZN.rotationDegrees(arc.getXRot()));

        if (voFallback) {
            ViewOptimize.fix(pose, arc);
        }

        VertexConsumer vc = buffers.getBuffer(ACRenderTypes.arc(ArcFactory.TEXTURE, false));

        int[] iid = arc.patternIds();
        if (arc.lengthFixed) {
            for (int i = 0; i < arc.subArcCount(); ++i) {
                arc.patterns[iid[i]].draw(pose, vc, alphaMul, arc.colorR, arc.colorG, arc.colorB);
            }
        } else {
            for (int i = 0; i < arc.subArcCount(); ++i) {

                arc.patterns[iid[i]].draw(pose, vc, arc.length, alphaMul,
                        arc.colorR, arc.colorG, arc.colorB);
            }
        }

        pose.popPose();
    }

    private static void drawPath(EntityArc arc, float alphaMul, PoseStack pose, MultiBufferSource buffers) {
        net.minecraft.world.phys.Vec3[] path = arc.getPath();
        double[] cum = arc.getPathCum();
        if (path == null || cum == null || path.length < 2) {
            return;
        }
        VertexConsumer vc = buffers.getBuffer(ACRenderTypes.arc(ArcFactory.TEXTURE, false));
        int[] iid = arc.patternIds();

        for (int i = 0; i + 1 < path.length; i++) {
            net.minecraft.world.phys.Vec3 seg = path[i + 1].subtract(path[i]);
            if (seg.lengthSqr() < 1.0e-8) {
                continue;
            }
            double from = cum[i], to = cum[i + 1];

            pose.pushPose();
            pose.translate(path[i].x, path[i].y, path[i].z);
            pose.mulPose(Axis.YN.rotationDegrees(
                    (float) (-Math.atan2(seg.x, seg.z) * 180 / Math.PI) + 90));
            pose.mulPose(Axis.ZN.rotationDegrees(
                    (float) (-Math.atan2(seg.y, seg.horizontalDistance()) * 180 / Math.PI)));
            pose.translate(-from, 0, 0);

            for (int k = 0; k < arc.subArcCount(); ++k) {
                arc.patterns[iid[k]].drawRange(pose, vc, from, to, alphaMul,
                        arc.colorR, arc.colorG, arc.colorB);
            }
            pose.popPose();
        }
    }

    @Override
    public ResourceLocation getTextureLocation(EntityArc entity) {
        return ArcFactory.TEXTURE;
    }
}
