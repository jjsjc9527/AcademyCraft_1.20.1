package cn.academy.client.render.entity;

import cn.academy.entity.EntityDualWing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public final class FeatherWing {

    private FeatherWing() {}

    private static final double SCALE = 1.35;

    private static final double ROOT_OUT = 0.28;

    private static final double SPAN = 2.05;

    private static final double RISE = 1.05;

    private static final double BONE_ARC = 0.62;

    private static final double DROOP_MAX = 0.42;

    private static final double BACK = 0.45;

    private static final float ATLAS = 256f;

    private static final int[] SLOT_X = {0, 32, 64, 96, 128};

    private static final int[] SLOT_H = {104, 72, 48, 28, 252};
    private static final int SLOT_W = 32;

    private static final float HALF_TEXEL = 0.5f / ATLAS;

    private static final int[] LAYER_COUNT = {14, 11, 9, 7, 7};

    private static final double[] LAYER_LEN = {1.00, 0.72, 0.46, 0.26, 1.55};

    private static final double[] LAYER_FROM = {0.05, 0.03, 0.01, 0.00, 0.42};

    private static final double[] LAYER_ALPHA = {1.00, 0.92, 0.85, 0.78, 1.00};

    private static final double[] LAYER_LIFT = {0.000, 0.040, 0.075, 0.105, -0.055};

    private static final double LEN_MAX = 1.30;

    private static final double W_ROOT = 0.34;

    private static final double W_TIP_RATIO = 0.86;

    private static final double TIP_ALPHA = 0.14;

    private static final double SWAY = 0.30;
    private static final double SWAY_LAG = 2.30;

    public static final boolean FLAP_ON = true;

    private static final double PERIOD = 2.10;

    private static final double AMP_Z = 24.0;

    private static final double AMP_X = 7.5;

    private static final double BONE_WAVE = 0.40;
    private static final double BONE_LAG = 2.05;

    private static final double BONE_WAVE_Z = 0.38;

    private static final double SPREAD = 0.16;

    private static final double CURL = 0.42;

    private static final int SEG = 3;

    private static final double CURL_LAG = 0.35;

    private static final int FINGER_LAYER = 4;

    private static final double FAN_SEP = 0.62;

    private static final double FAN_ANGLE = 0.85;

    public static void draw(EntityDualWing eff, int side, PoseStack pose, VertexConsumer vc,
                            int[] rgb, int alpha, double grow) {
        if (grow <= 0.01 || alpha <= 0) {
            return;
        }
        int sx = side == 0 ? +1 : -1;

        double theta = FLAP_ON
                ? cn.lambdalib2.util.GameTimer.getPausableTime() / PERIOD * (Math.PI * 2) : 0;

        double fa = FLAP_ON ? grow : 0;

        pose.pushPose();

        double wz = Math.sin(theta);
        double wx = Math.sin(theta + Math.PI / 2);
        pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees((float) (AMP_X * wx * fa)));
        pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees((float) (AMP_Z * wz * fa * sx)));

        double droop = DROOP_MAX * (0.5 - 0.5 * Math.sin(theta)) * fa;

        pose.scale((float) SCALE, (float) SCALE, (float) SCALE);

        Matrix4f mat = pose.last().pose();
        double span = SPAN * grow;
        double lenScale = LEN_MAX * (0.45 + 0.55 * grow);

        for (int layer = 0; layer < LAYER_COUNT.length; layer++) {
            int n = (int) Math.ceil(LAYER_COUNT[layer] * Math.min(1.0, grow * 1.15));
            for (int i = 0; i < n; i++) {

                double t = tOf(layer, i);
                if (t > 1.0) {
                    continue;
                }
                feather(mat, vc, sx, layer, t, span, lenScale, theta, fa, droop, rgb, alpha);
            }
        }
        pose.popPose();
    }

    private static double tOf(int layer, int i) {
        return LAYER_FROM[layer]
                + (1.0 - LAYER_FROM[layer]) * (i / (double) Math.max(1, LAYER_COUNT[layer] - 1));
    }

    private static final double FLAP_SOUND_AT = 0.30;

    public static long flapCycle() {
        if (!FLAP_ON) {
            return 0L;
        }
        return (long) Math.floor(
                cn.lambdalib2.util.GameTimer.getPausableTime() / PERIOD - FLAP_SOUND_AT);
    }

    public static int primaryCount(double grow) {
        return (int) Math.ceil(LAYER_COUNT[0] * Math.min(1.0, grow * 1.15));
    }

    public static void primaryTip(int side, int i, double grow, double[] out) {
        surfacePoint(side, tOf(0, i), 1.0, grow, out);
    }

    public static boolean surfacePoint(int side, double t, double f, double grow, double[] out) {
        int sx = side == 0 ? +1 : -1;
        double theta = FLAP_ON
                ? cn.lambdalib2.util.GameTimer.getPausableTime() / PERIOD * (Math.PI * 2) : 0;
        double fa = FLAP_ON ? grow : 0;
        double droop = DROOP_MAX * (0.5 - 0.5 * Math.sin(theta)) * fa;
        out[0] = out[1] = out[2] = 0;
        if (t > 1.0 || !axisOf(sx, 0, t, SPAN * grow,
                LEN_MAX * (0.45 + 0.55 * grow), theta, fa, droop)) {
            return false;
        }
        double nx = AX[6], ny = AX[7], nz = AX[8];
        pointOn(f, out);

        double wz = Math.sin(theta);
        double wxr = Math.sin(theta + Math.PI / 2);
        RIG.identity()
                .rotate(com.mojang.math.Axis.XP.rotationDegrees((float) (AMP_X * wxr * fa)))
                .rotate(com.mojang.math.Axis.ZP.rotationDegrees((float) (AMP_Z * wz * fa * sx)))
                .scale((float) SCALE);
        RIGV.set((float) out[0], (float) out[1], (float) out[2]);
        RIG.transformPosition(RIGV);
        out[0] = RIGV.x;
        out[1] = RIGV.y;
        out[2] = RIGV.z;

        if (out.length >= 6) {
            RIGV.set((float) nx, (float) ny, (float) nz);
            RIG.transformDirection(RIGV);
            RIGV.normalize();
            out[3] = RIGV.x;
            out[4] = RIGV.y;
            out[5] = RIGV.z;
        }
        return true;
    }

    private static final Matrix4f RIG = new Matrix4f();
    private static final org.joml.Vector3f RIGV = new org.joml.Vector3f();

    private static void boneAt(double t, double span, double droop, double[] out) {
        out[0] = ROOT_OUT + span * t;

        out[1] = RISE * Math.sin(t * Math.PI * BONE_ARC) - droop * t * t;
        out[2] = -BACK * t;
    }

    private static final double[] B = new double[3];
    private static final double[] D = new double[3];

    private static final double[] AX = new double[13];

    private static final double[] Q = new double[3];

    private static boolean axisOf(int sx, int layer, double t, double span, double lenScale,
                                  double theta, double fa, double droop) {
        boneAt(t, span, droop, B);

        double phase = theta - t * BONE_LAG;
        double tw = Math.pow(t, 1.25) * fa;
        B[1] += BONE_WAVE * Math.sin(phase) * tw;
        B[2] += BONE_WAVE * BONE_WAVE_Z * Math.cos(phase) * tw;

        B[0] *= sx;

        boolean isFinger = layer == FINGER_LAYER;
        double fan = 0.5 + 0.5 * Math.sin(theta - t * CURL_LAG);
        double fg = fan * fa;

        double dx = 0.42 + (0.46 + FAN_ANGLE * fg) * t;
        double dy = -0.86 + 0.30 * t;
        double dz = -0.44 - 0.16 * t;
        double dl = Math.sqrt(dx * dx + dy * dy + dz * dz);
        D[0] = dx / dl * sx;
        D[1] = dy / dl;
        D[2] = dz / dl;

        double shape = 0.32 + 0.68 * Math.sin(Math.pow(t, 0.75) * Math.PI);
        double spread = 1.0 + SPREAD * Math.sin(phase) * fa;
        double len = lenScale * shape * LAYER_LEN[layer] * spread;
        if (isFinger) {
            len *= 0.35 + 0.65 * fan;
        }

        double sway = SWAY * Math.sin(theta - t * SWAY_LAG) * (0.25 + 0.75 * t) * fa;
        if (isFinger) {
            sway *= 0.3;
        }

        double nx = 0.0;
        double ny = D[2];
        double nz = -D[1];
        double nl = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (nl < 1e-6) {
            return false;
        }
        nx /= nl; ny /= nl; nz /= nl;

        double lift = -LAYER_LIFT[layer];
        double bx = B[0] + nx * lift;
        double by = B[1] + ny * lift;
        double bz = B[2] + nz * lift;

        double curlPhase = theta - t * CURL_LAG;
        double curl = isFinger ? 0.0
                : CURL * (0.5 - 0.5 * Math.sin(curlPhase)) * fa * LAYER_LEN[layer];

        AX[0] = bx;   AX[1] = by;   AX[2] = bz;
        AX[3] = D[0]; AX[4] = D[1]; AX[5] = D[2];
        AX[6] = nx;   AX[7] = ny;   AX[8] = nz;
        AX[9] = len;  AX[10] = curl; AX[11] = sway; AX[12] = fan;
        return true;
    }

    private static void pointOn(double f, double[] out) {
        double c = AX[10] * f * f;
        double sw = AX[11] * f;
        out[0] = AX[0] + AX[3] * AX[9] * f - AX[6] * c + AX[6] * sw;
        out[1] = AX[1] + AX[4] * AX[9] * f - AX[7] * c + AX[7] * sw;
        out[2] = AX[2] + AX[5] * AX[9] * f - AX[8] * c + AX[8] * sw;
    }

    private static void feather(Matrix4f mat, VertexConsumer vc, int sx, int layer, double t,
                                double span, double lenScale, double theta, double fa, double droop,
                                int[] rgb, int alpha) {
        if (!axisOf(sx, layer, t, span, lenScale, theta, fa, droop)) {
            return;
        }
        boolean isFinger = layer == FINGER_LAYER;
        double bx = AX[0], by = AX[1], bz = AX[2];
        double dx0 = AX[3], dy0 = AX[4], dz0 = AX[5];
        double nx = AX[6], ny = AX[7], nz = AX[8];
        double fan = AX[12];
        double fg = fan * fa;

        double wx = dy0 * nz - dz0 * ny;
        double wy = dz0 * nx - dx0 * nz;
        double wz = dx0 * ny - dy0 * nx;
        double wl = Math.sqrt(wx * wx + wy * wy + wz * wz);
        if (wl < 1e-6) {
            return;
        }
        wx /= wl; wy /= wl; wz /= wl;

        double w0 = W_ROOT * (0.75 + 0.45 * Math.sin(t * Math.PI)) * 0.5;
        if (isFinger) {
            w0 *= 0.62;
        }
        double w1 = w0 * W_TIP_RATIO * (1.0 - FAN_SEP * fg);

        int aRoot = (int) (alpha * LAYER_ALPHA[layer] * (isFinger ? fan : 1.0));
        int aTip = (int) (aRoot * TIP_ALPHA);
        int r = rgb[0], g = rgb[1], b = rgb[2];

        float u0 = SLOT_X[layer] / ATLAS + HALF_TEXEL;
        float u1 = (SLOT_X[layer] + SLOT_W) / ATLAS - HALF_TEXEL;
        float v0 = HALF_TEXEL;
        float v1 = SLOT_H[layer] / ATLAS - HALF_TEXEL;

        double px = bx, py = by, pz = bz;
        double pw = w0;
        int pa = aRoot;
        float pv = 0.0f;
        for (int s = 1; s <= SEG; s++) {
            float f = s / (float) SEG;

            pointOn(f, Q);
            double qx = Q[0], qy = Q[1], qz = Q[2];
            double qw = w0 + (w1 - w0) * f;
            int qa = (int) (aRoot + (aTip - aRoot) * f);
            float vp = v0 + (v1 - v0) * pv;
            float vq = v0 + (v1 - v0) * f;

            vertex(vc, mat, px + wx * pw, py + wy * pw, pz + wz * pw, u0, vp, r, g, b, pa);
            vertex(vc, mat, px - wx * pw, py - wy * pw, pz - wz * pw, u1, vp, r, g, b, pa);
            vertex(vc, mat, qx - wx * qw, qy - wy * qw, qz - wz * qw, u1, vq, r, g, b, qa);
            vertex(vc, mat, qx + wx * qw, qy + wy * qw, qz + wz * qw, u0, vq, r, g, b, qa);

            px = qx; py = qy; pz = qz;
            pw = qw; pa = qa; pv = f;
        }
    }

    private static void vertex(VertexConsumer vc, Matrix4f mat, double x, double y, double z,
                               float u, float v, int r, int g, int b, int a) {
        vc.vertex(mat, (float) x, (float) y, (float) z).uv(u, v).color(r, g, b, a).endVertex();
    }
}
