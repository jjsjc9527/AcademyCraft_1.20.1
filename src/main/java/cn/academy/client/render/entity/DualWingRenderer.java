package cn.academy.client.render.entity;

import cn.academy.client.render.ACEffectLateRender;
import cn.academy.client.render.ACRenderTypes;
import cn.academy.client.render.MagLimbBones;
import cn.academy.entity.EntityDualWing;
import cn.academy.entity.EntityStormWing;
import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.GameTimer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class DualWingRenderer extends EntityRenderer<EntityDualWing> {

    static final int[] BLACK = {16, 17, 23};

    static final int[] WHITE = {255, 255, 255};

    private static final double MORPH_SPLIT = EntityDualWing.MORPH_SPLIT;

    private static final double FADE_FROM = 0.60;

    private static final double WHITEN_BAND = 0.38;

    private static void sweepIfMorphing(double m, double front) {
        if (m > 0.0 && m < 1.0) {
            StormWingRenderer.colorSweep(
                    new StormWingRenderer.ColorSweep(BLACK, WHITE, front, WHITEN_BAND));
        }
    }

    private static final int[] LERP_RGB = new int[3];

    private static int[] lerpRgb(int[] a, int[] b, double f) {
        for (int i = 0; i < 3; i++) {
            LERP_RGB[i] = (int) (a[i] + (b[i] - a[i]) * f);
        }
        return LERP_RGB;
    }

    public static Vec3 wingTipWorld(EntityDualWing eff, int side) {
        if (eff.boneFrame < 0) {
            return null;
        }
        EntityStormWing.Tornado t = eff.wings[side][0];

        double[] d = flapDelta(eff, side);
        Matrix4f m = new Matrix4f();
        m.translate((float) t.tx, (float) (EntityDualWing.ROOT_Y + t.ty), (float) t.tz);
        m.rotate(Axis.XP.rotationDegrees((float) (t.rx + d[0])));
        m.rotate(Axis.YP.rotationDegrees((float) t.ry));
        m.rotate(Axis.ZP.rotationDegrees((float) (t.rz + d[1])));

        float g = (float) Math.max(0.05, eff.grow);
        float stretch = (float) (1.0 + 0.45 * eff.sharp) * g;
        org.joml.Vector4f tip =
                m.transform(new org.joml.Vector4f(0, (float) (t.height() * stretch), 0, 1));

        return eff.boneOrigin
                .add(eff.boneLeft.scale(tip.x))
                .add(eff.boneUp.scale(tip.y))
                .add(eff.boneFront.scale(tip.z));
    }

    static void enterBoneSpace(EntityDualWing eff, Vec3 cam, PoseStack pose) {
        Matrix4f m = pose.last().pose();
        m.set(ACEffectLateRender.cameraRotation());
        m.translate((float) (eff.boneOrigin.x - cam.x),
                (float) (eff.boneOrigin.y - cam.y),
                (float) (eff.boneOrigin.z - cam.z));
        m.mul(new Matrix4f().set(
                (float) eff.boneLeft.x, (float) eff.boneLeft.y, (float) eff.boneLeft.z, 0,
                (float) eff.boneUp.x, (float) eff.boneUp.y, (float) eff.boneUp.z, 0,
                (float) eff.boneFront.x, (float) eff.boneFront.y, (float) eff.boneFront.z, 0,
                0, 0, 0, 1));
        pose.translate(0, EntityDualWing.ROOT_Y, 0);
    }

    static void enterWingTip(EntityDualWing eff, int side, PoseStack pose) {
        EntityStormWing.Tornado t = eff.wings[side][0];

        double[] d = flapDelta(eff, side);
        pose.translate((float) t.tx, (float) t.ty, (float) t.tz);
        pose.mulPose(Axis.XP.rotationDegrees((float) (t.rx + d[0])));
        pose.mulPose(Axis.YP.rotationDegrees((float) t.ry));
        pose.mulPose(Axis.ZP.rotationDegrees((float) (t.rz + d[1])));
        float g = (float) Math.max(0.05, eff.grow);
        float stretch = (float) (1.0 + 0.45 * eff.sharp) * g;
        pose.translate(0, (float) (t.height() * stretch * TIP_OVERLAP), 0);
    }

    static double[] flapDelta(EntityDualWing eff, int side) {
        boolean on = EntityDualWing.FLAP_ON;
        double theta = on
                ? GameTimer.getPausableTime() / EntityDualWing.FLAP_PERIOD * (Math.PI * 2) : 0;
        double wave = on ? StormWingRenderer.flapWave(theta) : 0;
        double waveQ = on ? StormWingRenderer.flapWave(theta + Math.PI / 2) : 0;
        double fa = eff.grow;

        if (eff.gusting()) {
            fa *= GUST_FLAP_SCALE;
        }
        int sx = side == 0 ? +1 : -1;
        return new double[]{EntityDualWing.FLAP_RX * waveQ * fa,
                            EntityDualWing.FLAP_AMP * wave * fa * sx};
    }

    private static final double GUST_FLAP_SCALE = 0.3;

    private static final double GUST_BEND = 2.5;

    private static final double GUST_END_LEN = EntityDualWing.GUST_END_LEN;

    private static final double GUST_END_BEND = 2.0;

    private static final double GUST_END_FROM = 0.75;

    private static final double PRESS_WRAP_R = EntityDualWing.PRESS_WRAP_R;

    private static final double PRESS_WRAP_TURNS = 3.0;

    private static final double PRESS_WRAP_SPAN =
            2 * Math.PI * PRESS_WRAP_R * PRESS_WRAP_TURNS * 0.55;

    private static final double GUST_SPEED = EntityDualWing.GUST_SPEED;

    private static EntityStormWing.Tornado.Extrude pressExtrude(EntityDualWing eff, int side,
                                                                double drx, double drz) {
        return channelExtrude(eff, side, drx, drz,
                eff.pressFoot[side], eff.pressExtend[side], eff.pressWing[side],
                PRESS_WRAP_R, PRESS_WRAP_TURNS, PRESS_WRAP_SPAN);
    }

    private static EntityStormWing.Tornado.Extrude channelExtrude(EntityDualWing eff, int side,
                                                                  double drx, double drz,
                                                                  Vec3 foot, double extend,
                                                                  EntityStormWing.Tornado t,
                                                                  double wrapR, double wrapTurns,
                                                                  double wrapSpan) {
        if (foot == null || extend <= 0 || t == null) {
            return null;
        }
        float g = (float) Math.max(0.05, eff.grow);
        float stretch = (float) (1.0 + 0.45 * eff.sharp) * g;
        Vec3 tip = wingTipWorld(eff, side);
        if (tip == null) {
            return null;
        }
        Vec3 dw = foot.subtract(tip);
        double len = dw.length() * extend;

        if (wrapR > 0) {
            len = Math.max(0.05, len - wrapR);
        }
        if (len < 0.05) {
            return null;
        }

        org.joml.Quaternionf inv = new org.joml.Quaternionf()
                .rotateX((float) Math.toRadians(t.rx + drx))
                .rotateY((float) Math.toRadians(t.ry))
                .rotateZ((float) Math.toRadians(t.rz + drz))
                .conjugate();
        java.util.function.Function<Vec3, org.joml.Vector3f> toLocal = world -> {
            org.joml.Vector3f o = new org.joml.Vector3f(
                    (float) world.dot(eff.boneLeft), (float) world.dot(eff.boneUp),
                    (float) world.dot(eff.boneFront));
            inv.transform(o);
            return o;
        };

        org.joml.Vector3f v = toLocal.apply(dw);
        if (v.lengthSquared() < 1.0e-8f) {
            return null;
        }

        org.joml.Vector3f up = toLocal.apply(new Vec3(0, 1, 0));
        double endGrow = (extend - GUST_END_FROM) / Math.max(1.0e-6, 1 - GUST_END_FROM);
        endGrow = Math.min(1, Math.max(0, endGrow));
        endGrow = endGrow * endGrow * (3 - 2 * endGrow);

        float thin = (float) (1.0 - 0.35 * eff.sharp) * g;
        double sX = Math.max(1.0e-4, thin);
        double sY = Math.max(1.0e-4, stretch);
        double sZ = Math.max(1.0e-4, thin * EntityDualWing.FLATTEN);

        v.set((float) (v.x / sX), (float) (v.y / sY), (float) (v.z / sZ));
        if (v.lengthSquared() < 1.0e-12f) {
            return null;
        }
        v.normalize();

        double k = Math.sqrt((v.x * sX) * (v.x * sX)
                + (v.y * sY) * (v.y * sY)
                + (v.z * sZ) * (v.z * sZ));
        double inv2 = 1 / Math.max(1.0e-4, k);

        org.joml.Vector3f endDir = null;
        double endK = inv2;
        up.set((float) (up.x / sX), (float) (up.y / sY), (float) (up.z / sZ));
        if (up.lengthSquared() > 1.0e-12f) {
            up.normalize();
            endDir = up;
            double ku = Math.sqrt((up.x * sX) * (up.x * sX)
                    + (up.y * sY) * (up.y * sY)
                    + (up.z * sZ) * (up.z * sZ));
            endK = 1 / Math.max(1.0e-4, ku);
        }
        if (wrapR > 0) {

            double avg = (sX + sY + sZ) / 3;
            double wk = 1 / Math.max(1.0e-4, avg);
            return new EntityStormWing.Tornado.Extrude(
                    v, len * inv2, GUST_BEND * inv2, GUST_SPEED,
                    null, wrapSpan * wk * endGrow, 0,
                    wrapR * wk, wrapTurns);
        }
        return new EntityStormWing.Tornado.Extrude(
                v, len * inv2, GUST_BEND * inv2, GUST_SPEED,
                endDir, GUST_END_LEN * endGrow * endK, GUST_END_BEND * endK);
    }

    private static boolean feedFirstPersonBones(EntityDualWing eff, Player owner, float pt) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (owner != mc.player
                || !mc.options.getCameraType().isFirstPerson()) {
            return false;
        }

        double yaw = net.minecraft.util.Mth.rotLerp(pt, owner.yBodyRotO, owner.yBodyRot)
                * net.minecraft.util.Mth.DEG_TO_RAD;
        Vec3 front = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        Vec3 up = new Vec3(0, 1, 0);

        Vec3 left = up.cross(front);

        double y = net.minecraft.util.Mth.lerp(pt, owner.yo, owner.getY()) + 1.501;
        Vec3 origin = new Vec3(
                net.minecraft.util.Mth.lerp(pt, owner.xo, owner.getX()), y,
                net.minecraft.util.Mth.lerp(pt, owner.zo, owner.getZ()));

        eff.storeBone(origin, left, up, front, MagLimbBones.frame());
        return true;
    }

    private static final double TIP_OVERLAP = 0.84;

    public DualWingRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(EntityDualWing eff, float yaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffers, int packedLight) {

    }

    public static void draw(EntityDualWing eff, float pt, Vec3 cam,
                            PoseStack pose, MultiBufferSource buffers) {
        if (eff.alpha <= 0) {
            return;
        }
        Player owner = eff.getOwner();
        if (owner == null) {
            return;
        }

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player != null && owner.isInvisibleTo(mc.player)) {
            return;
        }

        boolean extrudeOnly = false;
        if (eff.boneFrame != MagLimbBones.frame()) {
            if (!feedFirstPersonBones(eff, owner, pt)) {
                return;
            }
            extrudeOnly = true;
        }

        double m = eff.morph;
        double whiten = Math.min(1.0, m / MORPH_SPLIT);

        double sweepFront = whiten * (1.0 + WHITEN_BAND);
        int[] rgb = lerpRgb(BLACK, WHITE, whiten);

        double glowMix = Math.max(0.0, Math.min(1.0, (whiten - 0.35) / 0.50));

        if (m > MORPH_SPLIT) {
            double fg = (m - MORPH_SPLIT) / (1.0 - MORPH_SPLIT);
            drawFeather(eff, cam, pose, buffers, WHITE, fg);
            drawHalo(eff, owner, pt, cam, pose, buffers, fg);
        }

        boolean glow = glowMix >= 0.5;
        VertexConsumer vc = buffers.getBuffer(
                glow ? ACRenderTypes.tornadoGlow() : ACRenderTypes.tornado());

        double whiteFade = 1.0 - 0.10 * whiten;

        double vanish = m <= FADE_FROM
                ? 1.0
                : Math.max(0.0, 1.0 - (m - FADE_FROM) / (1.0 - FADE_FROM));
        int alpha = Colors.f2i((float) (eff.alpha * whiteFade * vanish));
        if (alpha <= 0) {
            return;
        }

        pose.pushPose();

        enterBoneSpace(eff, cam, pose);

        boolean flapOn = EntityDualWing.FLAP_ON;
        double theta = flapOn
                ? GameTimer.getPausableTime() / EntityDualWing.FLAP_PERIOD * (Math.PI * 2) : 0;
        double wave = flapOn ? StormWingRenderer.flapWave(theta) : 0;
        double waveQ = flapOn ? StormWingRenderer.flapWave(theta + Math.PI / 2) : 0;

        double fa = eff.grow;

        for (int side = 0; side < 2; side++) {

            int sx = side == 0 ? +1 : -1;

            double[] fd = flapDelta(eff, side);
            double drx = fd[0];
            double drz = fd[1];

            double flexAmp = -EntityDualWing.FLAP_FLEX * sx;
            double flexLag = EntityDualWing.FLAP_LAG * Math.PI * 2;

            eff.wings[side][0].setExtrude(null);

            EntityStormWing.Tornado pw = eff.pressWing[side];
            if (pw != null) {
                pw.setExtrude(pressExtrude(eff, side, drx, drz));
            }

            java.util.List<EntityStormWing.Tornado> queue = new java.util.ArrayList<>(3);
            for (EntityStormWing.Tornado t : eff.wings[side]) {
                queue.add(t);
            }
            if (pw != null) {
                queue.add(pw);
            }
            for (EntityStormWing.Tornado t : queue) {
                boolean shadow = t == pw;
                pose.pushPose();

                pose.translate(t.tx, t.ty, t.tz);

                pose.mulPose(Axis.XP.rotationDegrees((float) (t.rx + drx)));
                pose.mulPose(Axis.YP.rotationDegrees((float) t.ry));
                pose.mulPose(Axis.ZP.rotationDegrees((float) (t.rz + drz)));

                float g = (float) Math.max(0.05, eff.grow);
                float stretch = (float) (1.0 + 0.45 * eff.sharp) * g;
                float thin = (float) (1.0 - 0.35 * eff.sharp) * g;
                pose.scale(thin, stretch, (float) (thin * EntityDualWing.FLATTEN));

                StormWingRenderer.Flap flap = flapOn
                        ? new StormWingRenderer.Flap(flexAmp, theta, flexLag,
                                EntityDualWing.FLAP_CHURN, EntityDualWing.FLAP_SPIN)
                        : StormWingRenderer.Flap.FROZEN;
                Matrix4f tm = pose.last().pose();

                boolean only = extrudeOnly || shadow;

                sweepIfMorphing(m, sweepFront);
                StormWingRenderer.drawTornado(t, tm, vc, rgb[0], rgb[1], rgb[2],
                        alpha, 0, flap, null, only);
                pose.popPose();
            }
        }

        pose.popPose();
    }

    private static final double HALO_Y = 0.67;

    private static final double HALO_R = 0.48;

    private static final double HALO_BOB = 0.025;
    private static final double HALO_BOB_PERIOD = 3.4;

    private static final double HALO_THICK = 0.05;
    private static final double[] HALO_LAYER_W = {0.30, 0.55, 0.30};

    private static final double HALO_PULSE_MIN = 0.82;
    private static final double HALO_PULSE_PERIOD = 2.6;

    private static void drawHalo(EntityDualWing eff, Player owner, float pt, Vec3 cam,
                                 PoseStack pose, MultiBufferSource buffers, double morphGrow) {

        double fade = Math.min(1.0, morphGrow * 1.7) * eff.grow;
        if (fade <= 0.01) {
            return;
        }
        double now = GameTimer.getPausableTime();
        double bob = HALO_BOB * Math.sin(now / HALO_BOB_PERIOD * (Math.PI * 2));
        double pulse = HALO_PULSE_MIN + (1.0 - HALO_PULSE_MIN)
                * (0.5 + 0.5 * Math.sin(now / HALO_PULSE_PERIOD * (Math.PI * 2)));
        int alpha = Colors.f2i((float) (eff.alpha * fade * pulse));
        if (alpha <= 0) {
            return;
        }

        float hYaw = net.minecraft.util.Mth.rotLerp(pt, owner.yHeadRotO, owner.yHeadRot);
        float hPitch = net.minecraft.util.Mth.lerp(pt, owner.xRotO, owner.getXRot());
        Vec3 up = Vec3.directionFromRotation(hPitch - 90.0f, hYaw);
        Vec3 look = Vec3.directionFromRotation(hPitch, hYaw);
        Vec3 right = look.cross(up);

        Vec3 c = eff.boneOrigin.add(up.scale(HALO_Y + bob));
        double r = HALO_R;

        pose.pushPose();
        pose.last().pose().set(ACEffectLateRender.cameraRotation());

        VertexConsumer vc = buffers.getBuffer(ACRenderTypes.halo());
        Matrix4f mat = pose.last().pose();

        int n = HALO_LAYER_W.length;
        for (int i = 0; i < n; i++) {
            double off = n == 1 ? 0.0 : (i / (double) (n - 1) - 0.5) * HALO_THICK;
            int a = (int) (alpha * HALO_LAYER_W[i]);
            if (a <= 0) {
                continue;
            }
            Vec3 lc = c.add(up.scale(off));
            haloVertex(vc, mat, lc, right, look, cam, -r, -r, 0, 0, a);
            haloVertex(vc, mat, lc, right, look, cam, -r, +r, 0, 1, a);
            haloVertex(vc, mat, lc, right, look, cam, +r, +r, 1, 1, a);
            haloVertex(vc, mat, lc, right, look, cam, +r, -r, 1, 0, a);
        }
        pose.popPose();
    }

    private static void haloVertex(VertexConsumer vc, Matrix4f mat, Vec3 c, Vec3 right, Vec3 look,
                                   Vec3 cam, double a, double b, float u, float v, int alpha) {
        vc.vertex(mat,
                        (float) (c.x + right.x * a + look.x * b - cam.x),
                        (float) (c.y + right.y * a + look.y * b - cam.y),
                        (float) (c.z + right.z * a + look.z * b - cam.z))
                .uv(u, v).color(255, 255, 255, alpha).endVertex();
    }

    private static void drawFeather(EntityDualWing eff, Vec3 cam, PoseStack pose,
                                    MultiBufferSource buffers, int[] rgb, double morphGrow) {
        VertexConsumer vc = buffers.getBuffer(ACRenderTypes.featherWing());

        int alpha = Colors.f2i((float) (eff.alpha * Math.min(1.0, morphGrow * 1.7)));

        pose.pushPose();
        enterBoneSpace(eff, cam, pose);
        for (int side = 0; side < 2; side++) {

            FeatherWing.draw(eff, side, pose, vc, rgb, alpha, eff.grow * morphGrow);
        }
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(EntityDualWing entity) {
        return cn.academy.Resources.getTexture("effects/tornado_ring");
    }
}
