package cn.academy.client.render.entity;

import cn.academy.client.render.ACRenderTypes;
import cn.academy.entity.EntityRailgunHand;
import cn.academy.gravity.ACGravity;
import cn.academy.gravity.RotationUtil;
import net.minecraft.core.Direction;
import cn.lambdalib2.util.GameTimer;
import cn.lambdalib2.util.MathUtils;
import cn.lambdalib2.util.ViewOptimize;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class RailgunHandRenderer extends EntityRenderer<EntityRailgunHand> {

    private static final float[][] VERTS = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};
    private static final float[][] UVS = {{0, 0}, {1, 0}, {1, 1}, {0, 1}};

    public RailgunHandRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(EntityRailgunHand ent, float yaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffers, int packedLight) {

    }

    public static void draw(EntityRailgunHand ent, float partialTick, PoseStack pose, MultiBufferSource buffers) {
        Player p = ent.getPlayer();
        if (p == null) {
            return;
        }

        double dt = GameTimer.getPausableTime() - ent.getCreateTime();
        if (dt >= EntityRailgunHand.PER_FRAME * EntityRailgunHand.COUNT) {
            return;
        }
        int frame = (int) (dt / EntityRailgunHand.PER_FRAME);
        if (frame < 0) frame = 0;
        if (frame >= EntityRailgunHand.COUNT) frame = EntityRailgunHand.COUNT - 1;

        boolean fp = ViewOptimize.isFirstPerson(ent);
        float pitch = Mth.lerp(partialTick, p.xRotO, p.getXRot());

        pose.pushPose();

        Direction grav = ACGravity.getGravityDirection(p);
        if (grav != Direction.DOWN) {
            pose.mulPose(RotationUtil.getCameraRotationQuaternion(grav));
        }

        float headYaw = fp
                ? Mth.rotLerp(partialTick, p.yHeadRotO, p.yHeadRot)
                : Mth.rotLerp(partialTick, p.yBodyRotO, p.yBodyRot);
        pose.mulPose(Axis.YP.rotationDegrees(180 - headYaw));
        if (fp) {
            pose.mulPose(Axis.XP.rotationDegrees(-pitch));
        } else {
            ViewOptimize.fixThirdPerson(pose);
        }

        if (fp) {
            double pitchRad = MathUtils.toRadians(pitch);
            double eyeHeight = p.getEyeHeight();
            pose.translate(0, Math.cos(pitchRad) * eyeHeight, Math.sin(pitchRad) * eyeHeight);

            double xOff = ent.getArm() == net.minecraft.world.entity.HumanoidArm.RIGHT ? .26 : -.26;
            pose.translate(xOff, -.15, -.24);
            pose.scale(.4f, .4f, 1f);
        } else {
            pose.translate(0, 1.8, -1);
            pose.mulPose(Axis.XP.rotationDegrees(-pitch));
        }

        VertexConsumer vc = buffers.getBuffer(ACRenderTypes.handEffect(frameTex(frame)));
        Matrix4f mat = pose.last().pose();
        for (int i = 0; i < 4; ++i) {
            vc.vertex(mat, VERTS[i][0], VERTS[i][1], 0)
                    .uv(UVS[i][0], UVS[i][1])
                    .color(255, 255, 255, 255)
                    .endVertex();
        }

        pose.popPose();
    }

    private static ResourceLocation frameTex(int frame) {
        return new ResourceLocation("academy", "textures/effects/arc_burst/" + frame + ".png");
    }

    @Override
    public ResourceLocation getTextureLocation(EntityRailgunHand entity) {
        return frameTex(0);
    }
}
