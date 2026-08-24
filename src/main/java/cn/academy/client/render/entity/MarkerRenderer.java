package cn.academy.client.render.entity;

import cn.academy.entity.EntityMarker;
import cn.lambdalib2.util.GameTimer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public final class MarkerRenderer extends EntityRenderer<EntityMarker> {

    public MarkerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(EntityMarker mk, float yaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffers, int packedLight) {

    }

    @Override
    public ResourceLocation getTextureLocation(EntityMarker entity) {
        return cn.academy.Resources.getTexture("effects/screen_mask");
    }

    private static final double[][] MUL = {
            {0, 0, 0}, {1, 0, 0}, {1, 0, 1}, {0, 0, 1},
            {0, 1, 0}, {1, 1, 0}, {1, 1, 1}, {0, 1, 1},
    };

    private static final float[] ROT = {0, -90, -180, -270, 0, -90, -180, -270};

    public static void draw(EntityMarker marker, float pt, PoseStack pose, MultiBufferSource buffers) {
        float width, height;
        Entity targ = marker.target;
        double ox = 0, oy = 0, oz = 0;
        if (targ != null) {
            width = targ.getBbWidth();
            height = targ.getBbHeight();

            double mx = Mth.lerp(pt, marker.xOld, marker.getX());
            double my = Mth.lerp(pt, marker.yOld, marker.getY());
            double mz = Mth.lerp(pt, marker.zOld, marker.getZ());
            ox = Mth.lerp(pt, targ.xOld, targ.getX()) - mx;
            oy = Mth.lerp(pt, targ.yOld, targ.getY()) - my;
            oz = Mth.lerp(pt, targ.zOld, targ.getZ()) - mz;
        } else {
            width = marker.boxWidth;
            height = marker.boxHeight;
        }

        double bob = 0.05 * Math.sin(GameTimer.getPausableTime() * 2.5);
        VertexConsumer vc = buffers.getBuffer(RenderType.lines());
        int r = marker.color.r, g = marker.color.g, b = marker.color.b;
        float len = 0.2f * width;

        pose.pushPose();
        pose.translate(ox - width / 2.0, oy + bob, oz - width / 2.0);
        for (int i = 0; i < 8; i++) {
            boolean rev = i < 4;
            pose.pushPose();
            pose.translate(width * MUL[i][0], height * MUL[i][1], width * MUL[i][2]);
            pose.mulPose(Axis.YP.rotationDegrees(ROT[i]));
            Matrix4f mat = pose.last().pose();
            Matrix3f nrm = pose.last().normal();
            line(vc, mat, nrm, r, g, b, 0, 0, 0, 0, rev ? len : -len, 0);
            line(vc, mat, nrm, r, g, b, 0, 0, 0, len, 0, 0);
            line(vc, mat, nrm, r, g, b, 0, 0, 0, 0, 0, len);
            pose.popPose();
        }
        pose.popPose();
    }

    private static void line(VertexConsumer vc, Matrix4f mat, Matrix3f nrm, int r, int g, int b,
                             float x1, float y1, float z1, float x2, float y2, float z2) {
        float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        float inv = 1.0f / (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        dx *= inv;
        dy *= inv;
        dz *= inv;
        vc.vertex(mat, x1, y1, z1).color(r, g, b, 255).normal(nrm, dx, dy, dz).endVertex();
        vc.vertex(mat, x2, y2, z2).color(r, g, b, 255).normal(nrm, dx, dy, dz).endVertex();
    }
}
