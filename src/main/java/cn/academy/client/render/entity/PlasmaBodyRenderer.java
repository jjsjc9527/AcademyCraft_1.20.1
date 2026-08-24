package cn.academy.client.render.entity;

import cn.academy.client.render.ACEffectShaders;
import cn.academy.client.render.ACRenderTypes;
import cn.academy.entity.EntityPlasmaBody;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class PlasmaBodyRenderer extends EntityRenderer<EntityPlasmaBody> {

    private static final float SIZE = 22;

    public PlasmaBodyRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(EntityPlasmaBody eff, float yaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffers, int packedLight) {

    }

    public static void draw(EntityPlasmaBody eff, float pt, Vec3 cam,
                            PoseStack pose, MultiBufferSource.BufferSource buffers) {
        ShaderInstance sh = ACEffectShaders.plasmaBody();
        if (sh == null) {
            return;
        }
        eff.updateAlpha();
        float alpha = (float) (eff.alpha * eff.alpha);
        if (alpha <= 1.0e-4f) {
            return;
        }

        double px = Mth.lerp(pt, eff.xOld, eff.getX()) - cam.x;
        double py = Mth.lerp(pt, eff.yOld, eff.getY()) - cam.y;
        double pz = Mth.lerp(pt, eff.zOld, eff.getZ()) - cam.z;

        final org.joml.Matrix4f camRot = cn.academy.client.render.ACEffectLateRender.cameraRotation();
        final org.joml.Vector4f tmp = new org.joml.Vector4f();

        float time = eff.animTime();
        for (int i = 0; i < EntityPlasmaBody.MAX_BALLS; i++) {
            if (i >= eff.balls.length) {

                sh.safeGetUniform("Ball" + i).set(0f, 0f, 0f, 0f);
                continue;
            }

            Vec3 off = eff.ballOffset(i).scale(eff.scale);

            camRot.transform(tmp.set((float) (px + off.x), (float) (py + off.y),
                    (float) (pz + off.z), 1.0f));
            sh.safeGetUniform("Ball" + i).set(
                    tmp.x(), tmp.y(), tmp.z(),
                    eff.balls[i].size * eff.scale);
        }
        sh.safeGetUniform("Alpha").set(alpha);

        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vector3f up = camera.getUpVector();
        Vector3f left = camera.getLeftVector();

        float h = SIZE * eff.scale / 2;
        float ux = up.x() * h, uy = up.y() * h, uz = up.z() * h;
        float lx = left.x() * h, ly = left.y() * h, lz = left.z() * h;

        VertexConsumer vc = buffers.getBuffer(ACRenderTypes.plasmaBody());
        Matrix4f mat = pose.last().pose();
        vc.vertex(mat, -lx - ux, -ly - uy, -lz - uz).uv(0f, 0f).endVertex();
        vc.vertex(mat, lx - ux, ly - uy, lz - uz).uv(1f, 0f).endVertex();
        vc.vertex(mat, lx + ux, ly + uy, lz + uz).uv(1f, 1f).endVertex();
        vc.vertex(mat, -lx + ux, -ly + uy, -lz + uz).uv(0f, 1f).endVertex();

        buffers.endBatch(ACRenderTypes.plasmaBody());
    }

    @Override
    public ResourceLocation getTextureLocation(EntityPlasmaBody entity) {
        return null;
    }
}
