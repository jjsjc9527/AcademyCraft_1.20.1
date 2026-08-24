package cn.academy.client.render.entity;

import cn.academy.client.render.ACRenderTypes;
import cn.academy.entity.EntityGustTornado;
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
public class GustTornadoRenderer extends EntityRenderer<EntityGustTornado> {

    public GustTornadoRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(EntityGustTornado eff, float yaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffers, int packedLight) {

    }

    public static void draw(EntityGustTornado eff, float pt, net.minecraft.world.phys.Vec3 cam,
                            PoseStack pose, MultiBufferSource buffers) {

        if (true) {
            return;
        }
        if (!eff.press || eff.alpha <= 0 || eff.extend <= 0) {
            return;
        }
        cn.academy.entity.EntityDualWing w = eff.wing;

        if (w == null || w.isRemoved()
                || w.boneFrame != cn.academy.client.render.MagLimbBones.frame()) {
            return;
        }
        net.minecraft.world.phys.Vec3 root = DualWingRenderer.wingTipWorld(w, eff.side);
        net.minecraft.world.phys.Vec3 foot = eff.footOf(pt);
        if (root == null || foot == null) {
            return;
        }

        net.minecraft.world.phys.Vec3 dw = foot.subtract(root).scale(eff.extend);
        double len = dw.length();
        if (len < 0.05) {
            return;
        }
        net.minecraft.world.phys.Vec3 dir = new net.minecraft.world.phys.Vec3(
                dw.dot(w.boneLeft), dw.dot(w.boneUp), dw.dot(w.boneFront));

        VertexConsumer vc = buffers.getBuffer(ACRenderTypes.tornado());

        int[] rgb = eff.white ? DualWingRenderer.WHITE : DualWingRenderer.BLACK;
        int alpha = Colors.f2i((float) (eff.alpha * (eff.white ? 0.62 : 1.0)));

        org.joml.Vector3f axis = new org.joml.Vector3f(
                (float) (dir.x / len), (float) (dir.y / len), (float) (dir.z / len));
        org.joml.Quaternionf turn = new org.joml.Quaternionf()
                .rotateTo(new org.joml.Vector3f(0f, 1f, 0f), axis);

        pose.pushPose();

        DualWingRenderer.enterBoneSpace(w, cam, pose);
        DualWingRenderer.enterWingTip(w, eff.side, pose);

        pose.mulPose(new org.joml.Quaternionf().rotateTo(wingDirLocal(w, eff.side), axis));

        eff.tornado.setHeight(len);

        StormWingRenderer.drawTornado(eff.tornado, pose.last().pose(), vc,
                rgb[0], rgb[1], rgb[2], alpha, 0, null, rootBend(w, eff.side, axis, turn, len));
        pose.popPose();
    }

    private static org.joml.Vector3f wingDirLocal(cn.academy.entity.EntityDualWing w, int side) {
        cn.academy.entity.EntityStormWing.Tornado t = w.wings[side][0];

        double[] d = cn.academy.client.render.entity.DualWingRenderer.flapDelta(w, side);
        org.joml.Quaternionf q = new org.joml.Quaternionf()
                .rotateX((float) Math.toRadians(t.rx + d[0]))
                .rotateY((float) Math.toRadians(t.ry))
                .rotateZ((float) Math.toRadians(t.rz + d[1]));
        return q.transform(new org.joml.Vector3f(0f, 1f, 0f));
    }

    private static StormWingRenderer.RootBend rootBend(cn.academy.entity.EntityDualWing w, int side,
                                                       org.joml.Vector3f axis,
                                                       org.joml.Quaternionf turn, double len) {

        org.joml.Vector3f wd = wingDirLocal(w, side);

        float dot = wd.dot(axis);

        org.joml.Vector3f lateral = new org.joml.Vector3f(
                wd.x - axis.x * dot, wd.y - axis.y * dot, wd.z - axis.z * dot);
        if (lateral.lengthSquared() < 1.0e-6f) {
            return null;
        }

        turn.conjugate(new org.joml.Quaternionf()).transform(lateral);
        double k = len * BEND_SPAN * BEND_GAIN;
        return new StormWingRenderer.RootBend(lateral.x * k, lateral.z * k, BEND_SPAN);
    }

    private static final double BEND_SPAN = 0.3;

    private static final double BEND_GAIN = 0.55;

    @Override
    public ResourceLocation getTextureLocation(EntityGustTornado entity) {
        return cn.academy.Resources.getTexture("effects/tornado_ring");
    }
}
