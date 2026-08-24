package cn.academy.client.render.entity;

import cn.academy.Resources;
import cn.academy.client.render.ACRenderTypes;
import cn.academy.entity.EntityParabola;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class ParabolaRenderer extends EntityRenderer<EntityParabola> {

    private static final ResourceLocation TEXTURE = Resources.getTexture("effects/glow_line");

    private static final int STEPS = 100;
    private static final double DT = 0.02;
    private static final double DAMPING = 0.98;
    private static final double GRAVITY = 1.9;
    private static final float HALF_H = 0.02f;

    public ParabolaRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(EntityParabola e, float yaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffers, int packedLight) {

    }

    public static void draw(EntityParabola e, float pt, PoseStack pose, MultiBufferSource buffers) {
        Minecraft mc = Minecraft.getInstance();

        if (!mc.options.getCameraType().isFirstPerson()) return;

        Player p = e.getOwner();
        float yaw = Mth.lerp(pt, p.yRotO, p.getYRot());
        float pitch = Mth.lerp(pt, p.xRotO, p.getXRot());
        Vec3 look = Vec3.directionFromRotation(pitch, yaw);

        Vec3 side = new Vec3(look.x, 0, look.z);
        if (side.lengthSqr() < 1.0e-6) {
            side = new Vec3(0, 0, 1);
        }
        side = side.yRot((float) (Math.PI / 2)).normalize().scale(-0.08);
        Vec3 pos = new Vec3(side.x, 1.56, side.z).subtract(look.scale(0.12));

        Vec3 speed = Vec3.directionFromRotation(pitch - 10, yaw).scale(e.speed.getAsDouble());
        boolean ok = e.canPerform.getAsBoolean();

        VertexConsumer vc = buffers.getBuffer(ACRenderTypes.thunder(TEXTURE));
        Matrix4f mat = pose.last().pose();

        Vec3 prev = pos;
        for (int idx = 1; idx < STEPS; idx++) {
            speed = speed.scale(DAMPING);
            Vec3 cur = prev.add(speed.scale(DT));
            speed = new Vec3(speed.x, speed.y - DT * GRAVITY, speed.z);

            float alpha = 0.7f * (1 - idx * 0.03f);
            if (alpha <= 0) break;

            int a = (int) (alpha * 255);
            int r = 255, g = ok ? 255 : 51, b = ok ? 255 : 51;

            vc.vertex(mat, (float) prev.x, (float) prev.y + HALF_H, (float) prev.z)
                    .uv(0, 0).color(r, g, b, a).endVertex();
            vc.vertex(mat, (float) prev.x, (float) prev.y - HALF_H, (float) prev.z)
                    .uv(0, 1).color(r, g, b, a).endVertex();
            vc.vertex(mat, (float) cur.x, (float) cur.y - HALF_H, (float) cur.z)
                    .uv(1, 1).color(r, g, b, a).endVertex();
            vc.vertex(mat, (float) cur.x, (float) cur.y + HALF_H, (float) cur.z)
                    .uv(1, 0).color(r, g, b, a).endVertex();

            prev = cur;
        }
    }

    @Override
    public ResourceLocation getTextureLocation(EntityParabola entity) {
        return TEXTURE;
    }
}
