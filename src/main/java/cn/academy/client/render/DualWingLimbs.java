package cn.academy.client.render;

import cn.academy.ability.vanilla.vecmanip.advanced.DualWingAnim;
import cn.academy.ability.vanilla.vecmanip.advanced.DualWingAnimData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class DualWingLimbs {

    private DualWingLimbs() {}

    public static final float ELBOW = 4.0f;

    public static final float KNEE = 6.0f;

    private static final float LAYER_GROW = 0.25f;

    private static boolean frameActive;
    private static final float[] FORE_R = new float[3];
    private static final float[] FORE_L = new float[3];
    private static final float[] SHIN_R = new float[3];
    private static final float[] SHIN_L = new float[3];

    private static boolean frameSlim;

    private static boolean bodyVisible;
    private static final boolean[] LAYER_VIS = new boolean[4];

    public static void beginFrame(float[] pose, PlayerModel<?> pm, boolean slim) {
        frameActive = pose != null;
        if (pose == null || pm == null) {
            return;
        }
        frameSlim = slim;
        bodyVisible = pm.rightArm.visible;
        LAYER_VIS[0] = pm.rightSleeve.visible;
        LAYER_VIS[1] = pm.leftSleeve.visible;
        LAYER_VIS[2] = pm.rightPants.visible;
        LAYER_VIS[3] = pm.leftPants.visible;
        fill(FORE_R, pose, DualWingAnimData.B_FOREARM_RIGHT);
        fill(FORE_L, pose, DualWingAnimData.B_FOREARM_LEFT);
        fill(SHIN_R, pose, DualWingAnimData.B_SHIN_RIGHT);
        fill(SHIN_L, pose, DualWingAnimData.B_SHIN_LEFT);
    }

    private static void fill(float[] out, float[] pose, int bone) {
        out[0] = DualWingAnim.bb2mRotX(pose[DualWingAnim.idx(bone, 0)]);
        out[1] = DualWingAnim.bb2mRotY(pose[DualWingAnim.idx(bone, 1)]);
        out[2] = DualWingAnim.bb2mRotZ(pose[DualWingAnim.idx(bone, 2)]);
    }

    public static boolean frameActive() {
        return frameActive;
    }

    public static boolean isSlim(Player player) {
        return player instanceof net.minecraft.client.player.AbstractClientPlayer acp
                && "slim".equals(acp.getModelName());
    }

    private static final int SKIN_W = 64, SKIN_H = 64;
    private static final float[] ROT = new float[3];

    public static void render(PoseStack ps, VertexConsumer vc, int light, int overlay,
                              PlayerModel<?> pm) {
        if (!bodyVisible) {
            return;
        }
        int aw = frameSlim ? 3 : 4;
        float ax = frameSlim ? -2.0f : -3.0f;

        arm(ps, vc, light, overlay, pm.rightArm, FORE_R, 0, 40, 16, 40, 32, ax, aw, false);

        arm(ps, vc, light, overlay, pm.leftArm, FORE_L, 1, 32, 48, 48, 48, -1.0f, aw, false);

        leg(ps, vc, light, overlay, pm, pm.rightLeg, SHIN_R, 2, 0, 16, 0, 32, false);

        leg(ps, vc, light, overlay, pm, pm.leftLeg, SHIN_L, 3, 16, 48, 0, 48, true);
    }

    private static void arm(PoseStack ps, VertexConsumer vc, int light, int overlay,
                            ModelPart part, float[] lowRot, int layerSlot,
                            int u1, int v1, int u2, int v2, float x0, int w, boolean mirror) {
        ROT[0] = part.xRot;
        ROT[1] = part.yRot;
        ROT[2] = part.zRot;
        DualWingSkin.noSeam = false;
        ps.pushPose();
        ps.translate(part.x / 16.0f, part.y / 16.0f, part.z / 16.0f);
        DualWingSkin.limb(ps, vc, light, overlay, ELBOW, ROT, lowRot, SKIN_W, SKIN_H,
                boxes(u1, v1, u2, v2, x0, -2.0f, w, layerSlot, mirror));
        ps.popPose();
    }

    private static void leg(PoseStack ps, VertexConsumer vc, int light, int overlay,
                            PlayerModel<?> pm, ModelPart part, float[] lowRot, int layerSlot,
                            int u1, int v1, int u2, int v2, boolean left) {

        float baseX = left ? LEG_X : -LEG_X;
        setupLeg(pm.body, part, baseX, lowRot);
        ps.pushPose();

        ps.translate(baseX / 16.0f, LEG_Y / 16.0f, 0.0f);
        DualWingSkin.noSeam = false;
        DualWingSkin.limbMulti(ps, vc, light, overlay, LEG_CLIPS, BODY_ROT, ROOT_PIVOT, TWO_ROT,
                TWO_OFF, SKIN_W, SKIN_H,
                boxes(u1, v1, u2, v2, -2.0f, 0.0f, 4, layerSlot, false));
        ps.popPose();
    }

    private static final float LEG_X = 1.9f, LEG_Y = 12.0f;

    private static void setupLeg(ModelPart body, ModelPart part, float baseX, float[] lowRot) {
        BODY_ROT[0] = body.xRot;
        BODY_ROT[1] = body.yRot;
        BODY_ROT[2] = body.zRot;
        HIP_ROT[0] = part.xRot - body.xRot;
        HIP_ROT[1] = part.yRot - body.yRot;
        HIP_ROT[2] = part.zRot - body.zRot;

        ROOT_PIVOT[0] = baseX - body.x;
        ROOT_PIVOT[1] = LEG_Y - body.y;
        ROOT_PIVOT[2] = -body.z;

        HIP_OFF[0] = 0.0f;
        HIP_OFF[1] = 0.0f;
        HIP_OFF[2] = 0.0f;
        TWO_ROT[0] = HIP_ROT;
        TWO_ROT[1] = lowRot;
        TWO_OFF[0] = HIP_OFF;
        TWO_OFF[1] = null;
    }

    private static final float HIP = 2.0f;
    private static final float[] LEG_CLIPS = {HIP, KNEE};
    private static final float[] BODY_ROT = new float[3];
    private static final float[] HIP_ROT = new float[3];
    private static final float[] ROOT_PIVOT = new float[3];
    private static final float[][] TWO_ROT = new float[2][];
    private static final float[] HIP_OFF = new float[3];
    private static final float[][] TWO_OFF = new float[2][];

    private static DualWingSkin.Box[] boxes(int u1, int v1, int u2, int v2,
                                            float x0, float y0, int w, int layerSlot, boolean mirror) {
        DualWingSkin.Box body =
                new DualWingSkin.Box(u1, v1, x0, y0, -2.0f, w, 12, 4, 0.0f, mirror);
        if (!LAYER_VIS[layerSlot]) {
            return new DualWingSkin.Box[]{body};
        }
        return new DualWingSkin.Box[]{body,
                new DualWingSkin.Box(u2, v2, x0, y0, -2.0f, w, 12, 4, LAYER_GROW, mirror)};
    }

    private static final int ARMOR_W = 64, ARMOR_H = 32;

    private static boolean innerArmor;

    public static void setInnerArmor(boolean inner) {
        innerArmor = inner;
    }

    public static boolean isInnerArmor() {
        return innerArmor;
    }

    public static void renderArmor(PoseStack ps, VertexConsumer vc, int light, int overlay,
                                   HumanoidModel<?> model, boolean innerModel) {
        if (!frameActive) {
            return;
        }
        float g = innerModel ? 0.5f : 1.0f;
        float legG = g - 0.1f;
        armorArm(ps, vc, light, overlay, model.rightArm, FORE_R, -3.0f, g, false);
        armorArm(ps, vc, light, overlay, model.leftArm, FORE_L, -1.0f, g, true);
        armorLeg(ps, vc, light, overlay, model, model.rightLeg, SHIN_R, legG, false, innerModel);
        armorLeg(ps, vc, light, overlay, model, model.leftLeg, SHIN_L, legG, true, innerModel);
    }

    private static void armorArm(PoseStack ps, VertexConsumer vc, int light, int overlay,
                                 ModelPart part, float[] lowRot, float x0, float grow, boolean mirror) {
        if (!part.visible) {
            return;
        }
        ROT[0] = part.xRot;
        ROT[1] = part.yRot;
        ROT[2] = part.zRot;
        DualWingSkin.noSeam = false;
        ps.pushPose();
        ps.translate(part.x / 16.0f, part.y / 16.0f, part.z / 16.0f);
        DualWingSkin.limb(ps, vc, light, overlay, ELBOW, ROT, lowRot, ARMOR_W, ARMOR_H,
                new DualWingSkin.Box(40, 16, x0, -2.0f, -2.0f, 4, 12, 4, grow, mirror));
        ps.popPose();
    }

    private static void armorLeg(PoseStack ps, VertexConsumer vc, int light, int overlay,
                                 HumanoidModel<?> model, ModelPart part, float[] lowRot,
                                 float grow, boolean mirror, boolean innerModel) {
        if (!part.visible) {
            return;
        }
        float baseX = mirror ? LEG_X : -LEG_X;
        setupLeg(model.body, part, baseX, lowRot);

        DualWingSkin.noSeam = !innerModel;
        ps.pushPose();
        ps.translate(baseX / 16.0f, LEG_Y / 16.0f, 0.0f);
        DualWingSkin.limbMulti(ps, vc, light, overlay, LEG_CLIPS, BODY_ROT, ROOT_PIVOT, TWO_ROT,
                TWO_OFF, ARMOR_W, ARMOR_H,
                new DualWingSkin.Box(0, 16, -2.0f, 0.0f, -2.0f, 4, 12, 4, grow, mirror));
        ps.popPose();
    }

    public static void applyForearm(HumanoidArm side, PoseStack ps) {
        if (!frameActive) {
            return;
        }
        float[] r = side == HumanoidArm.RIGHT ? FORE_R : FORE_L;
        ps.translate(0.0F, ELBOW / 16.0F, 0.0F);
        if (r[2] != 0.0F) {
            ps.mulPose(com.mojang.math.Axis.ZP.rotation(r[2]));
        }
        if (r[1] != 0.0F) {
            ps.mulPose(com.mojang.math.Axis.YP.rotation(r[1]));
        }
        if (r[0] != 0.0F) {
            ps.mulPose(com.mojang.math.Axis.XP.rotation(r[0]));
        }
        ps.translate(0.0F, -ELBOW / 16.0F, 0.0F);
    }
}
