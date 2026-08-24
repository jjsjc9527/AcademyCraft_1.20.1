package cn.academy.client.render.entity;

import cn.academy.client.render.ACRenderTypes;
import cn.academy.entity.EntityStormWing;
import cn.academy.util.ImprovedNoise;
import cn.lambdalib2.util.Colors;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
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
public class StormWingRenderer extends EntityRenderer<EntityStormWing> {

    private static final double FLAP_SHAPE = 1.2;

    private static final int DIV = 20;
    private static final double U_STEP = 1.0 / DIV;

    private static final int DIV_FINE = 96;
    private static final double U_STEP_FINE = 1.0 / DIV_FINE;

    private static final double[] CIRCLE_SIN = new double[DIV];
    private static final double[] CIRCLE_COS = new double[DIV];
    private static final double[] FINE_SIN = new double[DIV_FINE];
    private static final double[] FINE_COS = new double[DIV_FINE];

    static {
        for (int i = 0; i < DIV; i++) {
            double rad = (double) i / DIV * Math.PI * 2;
            CIRCLE_SIN[i] = Math.sin(rad);
            CIRCLE_COS[i] = Math.cos(rad);
        }
        for (int i = 0; i < DIV_FINE; i++) {
            double rad = (double) i / DIV_FINE * Math.PI * 2;
            FINE_SIN[i] = Math.sin(rad);
            FINE_COS[i] = Math.cos(rad);
        }
    }

    public StormWingRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(EntityStormWing eff, float yaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffers, int packedLight) {

    }

    public static void draw(EntityStormWing eff, float pt, Vec3 cam,
                            PoseStack pose, MultiBufferSource buffers) {
        if (eff.alpha <= 0) {
            return;
        }
        Player owner = eff.getOwner();
        if (owner == null) {
            return;
        }

        if (eff.boneFrame != cn.academy.client.render.MagLimbBones.frame()) {
            return;
        }

        VertexConsumer vc = buffers.getBuffer(ACRenderTypes.tornado());
        int alpha = Colors.f2i((float) (eff.alpha * 0.7));

        pose.pushPose();

        Matrix4f m = pose.last().pose();

        m.set(cn.academy.client.render.ACEffectLateRender.cameraRotation());
        m.translate((float) (eff.boneOrigin.x - cam.x),
                (float) (eff.boneOrigin.y - cam.y),
                (float) (eff.boneOrigin.z - cam.z));

        m.mul(new Matrix4f().set(
                (float) eff.boneLeft.x, (float) eff.boneLeft.y, (float) eff.boneLeft.z, 0,
                (float) eff.boneUp.x, (float) eff.boneUp.y, (float) eff.boneUp.z, 0,
                (float) eff.boneFront.x, (float) eff.boneFront.y, (float) eff.boneFront.z, 0,
                0, 0, 0, 1));

        pose.translate(0, 1.6 - 1.407, 0);

        float pitch = Mth.lerp(pt, owner.xRotO, owner.getXRot());
        pose.mulPose(Axis.XP.rotationDegrees(pitch * 0.2f));
        pose.mulPose(Axis.XP.rotationDegrees(-70));
        pose.translate(0, 0.2, -0.5);

        for (EntityStormWing.Tornado t : eff.tornados) {
            pose.pushPose();

            pose.translate(t.tx, t.ty, t.tz);
            pose.mulPose(Axis.XP.rotationDegrees((float) t.rx));
            pose.mulPose(Axis.YP.rotationDegrees((float) t.ry));
            pose.mulPose(Axis.ZP.rotationDegrees((float) t.rz));
            drawTornado(t, pose.last().pose(), vc, alpha);
            pose.popPose();
        }

        pose.popPose();
    }

    public static void drawTornado(EntityStormWing.Tornado t, Matrix4f mat,
                                   VertexConsumer vc, int alpha) {
        drawTornado(t, mat, vc, alpha, 0);
    }

    public static void drawTornado(EntityStormWing.Tornado t, Matrix4f mat,
                                   VertexConsumer vc, int alpha, double density) {
        drawTornado(t, mat, vc, 255, 255, 255, alpha, density);
    }

    public static void drawTornado(EntityStormWing.Tornado t, Matrix4f mat, VertexConsumer vc,
                                   int red, int green, int blue, int alpha, double density) {
        drawTornado(t, mat, vc, red, green, blue, alpha, density, null);
    }

    public static final class RootBend {

        public final double bx, bz;

        public final double span;

        public RootBend(double bx, double bz, double span) {
            this.bx = bx;
            this.bz = bz;
            this.span = span;
        }

        double weight(double ny) {
            if (span <= 0 || ny >= span) {
                return 0;
            }
            double u = 1 - ny / span;
            return u * u * (3 - 2 * u);
        }
    }

    public static final class Flap {

        public final double flexAmp;

        public final double phase;

        public final double lag;

        public final double churn;

        public final double spin;

        public static final Flap FROZEN = new Flap(0, 0, 0, 0, 0);

        public Flap(double flexAmp, double phase, double lag, double churn, double spin) {
            this.flexAmp = flexAmp;
            this.phase = phase;
            this.lag = lag;
            this.churn = churn;
            this.spin = spin;
        }
    }

    public static void drawTornado(EntityStormWing.Tornado t, Matrix4f mat, VertexConsumer vc,
                                   int red, int green, int blue, int alpha, double density,
                                   Flap flap) {
        drawTornado(t, mat, vc, red, green, blue, alpha, density, flap, null);
    }

    public static void drawTornado(EntityStormWing.Tornado t, Matrix4f mat, VertexConsumer vc,
                                   int red, int green, int blue, int alpha, double density,
                                   Flap flap, RootBend bend) {
        drawTornado(t, mat, vc, red, green, blue, alpha, density, flap, bend, false);
    }

    public record ColorSweep(int[] from, int[] to, double front, double band) {}

    private static ColorSweep pendingSweep;

    public static void colorSweep(ColorSweep sweep) {
        pendingSweep = sweep;
    }

    public static void drawTornado(EntityStormWing.Tornado t, Matrix4f mat, VertexConsumer vc,
                                   int red, int green, int blue, int alpha, double density,
                                   Flap flap, RootBend bend, boolean extrudeOnly) {
        ColorSweep sweep = pendingSweep;
        pendingSweep = null;
        double time = t.time();
        double ht = t.height(), sz = t.size(), dscale = t.dscale();

        double nowSec = cn.lambdalib2.util.GameTimer.getPausableTime();

        double flapSide = Math.signum(t.tx);

        for (EntityStormWing.Ring ring : t.rings) {

            int ringAlpha = alpha;
            if (ring.threshold >= 0) {
                double t01 = (density - ring.threshold) / 0.2;
                if (t01 <= 0) {
                    continue;
                }
                if (t01 < 1) {
                    ringAlpha = (int) (alpha * t01);
                    if (ringAlpha <= 0) {
                        continue;
                    }
                }
            }
            double rh = t.ringHeight(ring, nowSec);

            if (extrudeOnly && rh <= ht) {
                continue;
            }

            int cr = red, cg = green, cb = blue;
            if (sweep != null) {
                double sPos = ht > 1.0e-6 ? rh / ht : 0.0;
                double f = (sweep.front() - sPos) / sweep.band();
                f = f < 0 ? 0 : (f > 1 ? 1 : f);
                cr = (int) (sweep.from()[0] + (sweep.to()[0] - sweep.from()[0]) * f);
                cg = (int) (sweep.from()[1] + (sweep.to()[1] - sweep.from()[1]) * f);
                cb = (int) (sweep.from()[2] + (sweep.to()[2] - sweep.from()[2]) * f);
            }
            drawOneRing(ring, rh, mat, vc,
                    cr, cg, cb, ringAlpha, time, ht, sz, dscale, t.shape(), flap,
                    flapSide, nowSec, bend, t.extrude());
        }
    }

    public static double flapWave(double theta) {
        return Math.sin(theta + 0.35 * Math.sin(theta));
    }

    private static void drawOneRing(EntityStormWing.Ring ring, double ringY,
                                    Matrix4f mat, VertexConsumer vc,
                                    int red, int green, int blue,
                                    int alpha, double time, double ht, double sz, double dscale,
                                    EntityStormWing.Shape shape, Flap flap, double flapSide,
                                    double nowSec, RootBend bend,
                                    EntityStormWing.Tornado.Extrude extrude) {
        double flare = shape == null ? 1.0 : shape.flare;
        double ny = ringY / ht;

        boolean coarse = false;
        double exX = 0, exZ = 0, exFade = 1;
        if (extrude != null && ny > 1) {
            double s = (ny - 1) * ht;
            coarse = s > extrude.bend;

            double sm = Math.min(s, Math.max(0, extrude.len));
            double w = extrude.bend <= 0 ? 1 : Math.min(1, sm / extrude.bend);
            w = w * w * (3 - 2 * w);
            double vx = extrude.dir.x * w;

            double vy = 1.0 + (extrude.dir.y - 1.0) * w;
            double vz = extrude.dir.z * w;
            double l = Math.sqrt(vx * vx + vy * vy + vz * vz);
            double ux = 0, uy = 1, uz = 0;
            if (l > 1.0e-6) {
                ux = vx / l;
                uy = vy / l;
                uz = vz / l;
                exX = ux * sm;
                exZ = uz * sm;
                ringY = ht + uy * sm;
            }

            if (extrude.wrapR > 0 && extrude.endLen > 0 && s > sm) {

                double se = Math.min(s - sm, extrude.endLen);
                double p = se / extrude.endLen;
                double phi = p * Math.PI;
                double th = p * extrude.wrapTurns * Math.PI * 2;
                double rr = extrude.wrapR * Math.sin(phi);

                double along = extrude.wrapR * (1 - Math.cos(phi));

                double ax = Math.abs(ux) < 0.9 ? 1 : 0, ay = Math.abs(ux) < 0.9 ? 0 : 1, az = 0;
                double d1 = ax * ux + ay * uy + az * uz;
                double e1x = ax - ux * d1, e1y = ay - uy * d1, e1z = az - uz * d1;
                double l1 = Math.sqrt(e1x * e1x + e1y * e1y + e1z * e1z);
                if (l1 > 1.0e-6) {
                    e1x /= l1; e1y /= l1; e1z /= l1;

                    double e2x = uy * e1z - uz * e1y;
                    double e2y = uz * e1x - ux * e1z;
                    double e2z = ux * e1y - uy * e1x;
                    double c = Math.cos(th), sn = Math.sin(th);
                    exX += ux * along + (e1x * c + e2x * sn) * rr;
                    ringY += uy * along + (e1y * c + e2y * sn) * rr;
                    exZ += uz * along + (e1z * c + e2z * sn) * rr;
                }
            } else if (extrude.endDir != null && extrude.endLen > 0 && s > sm) {
                double se = Math.min(s - sm, extrude.endLen);
                double we = extrude.endBend <= 0 ? 1 : Math.min(1, se / extrude.endBend);
                we = we * we * (3 - 2 * we);
                double ex = ux + (extrude.endDir.x - ux) * we;
                double ey = uy + (extrude.endDir.y - uy) * we;
                double ez = uz + (extrude.endDir.z - uz) * we;
                double el = Math.sqrt(ex * ex + ey * ey + ez * ez);
                if (el > 1.0e-6) {
                    exX += ex / el * se;
                    exZ += ez / el * se;
                    ringY += ey / el * se;
                }
            }

            exFade = 1 - w;
            ny = 1;
        }

        double amp = (0.3 + Math.pow(ny * 2, 1.4)) * (shape == null ? 1.0 : shape.wobbleAmp);

        double ct = time * (flap == null ? 1 : flap.churn);

        double nw = ny * (shape == null ? 1.0 : shape.wobbleFreq);
        double dx = ImprovedNoise.noise(nw, ct * 0.1) * amp * sz * dscale;
        double dz = ImprovedNoise.noise(nw, ct * 0.1, 1) * amp * sz * dscale;

        if (shape != null && shape.swayPeriod > 0 && shape.flapAmp > 0) {

            double fp = nowSec / shape.swayPeriod * (Math.PI * 2) - shape.swayLag * ny;

            dx += shape.flapAmp * Math.pow(ny, FLAP_SHAPE) * Math.sin(fp) * flapSide;
        }

        if (shape != null && shape.curveAmp != 0) {
            dx += shape.curveAmp * Math.sin(ny * Math.PI * 2) * flapSide;
        }

        if (flap != null) {
            dx += flap.flexAmp * ny * ny * flapWave(flap.phase - flap.lag * ny);
        }

        if (bend != null) {
            double w = bend.weight(ny);
            if (w > 0) {
                dx += bend.bx * w;
                dz += bend.bz * w;
            }
        }

        dx = dx * exFade + exX;
        dz = dz * exFade + exZ;

        double vr = ((0.5 + 0.3 * ImprovedNoise.noise(ny, 0.2 * ct))
                + 0.5 * Math.pow(1.5 * ny, 2) * flare
                + ImprovedNoise.noise(ny)) * sz * ring.sizeScale;

        if (shape != null && shape.rootTaper > 0) {
            double g = Math.min(1.0, ny / shape.rootTaper);
            vr *= g * g * (3 - 2 * g);
        }

        double rot = 0.1 * (1 + 0.5 * ny) * time * (flap == null ? 1 : flap.spin) + ring.phase;

        drawRing(mat, vc, red, green, blue, alpha, ringY, ring.width, dx, dz, vr, rot,
                ring, shape, ny, nowSec, coarse);
    }

    private static double lerpSpike(float[] tab, int groups, int sm, int idx) {
        if (sm <= 1) {
            return tab[idx % groups];
        }
        int g0 = (idx / sm) % groups;
        int g1 = (g0 + 1) % groups;
        double t = (idx % sm) / (double) sm;
        t = t * t * (3 - 2 * t);
        double a = tab[g0];
        return a + (tab[g1] - a) * t;
    }

    private static double lerpSpikeU(float[] tab, int groups, double u) {
        if (groups <= 1) {
            return tab[0];
        }
        double p = u * groups;
        double fg = Math.floor(p);
        int g0 = ((int) fg % groups + groups) % groups;
        int g1 = (g0 + 1) % groups;
        double t = p - fg;
        t = t * t * (3 - 2 * t);
        double a = tab[g0];
        return a + (tab[g1] - a) * t;
    }

    private static void drawRing(Matrix4f mat, VertexConsumer vc,
                                 int red, int green, int blue, int alpha,
                                 double y, double w, double dx, double dz, double r, double rot,
                                 EntityStormWing.Ring ring, EntityStormWing.Shape shape,
                                 double ny, double nowSec, boolean coarse) {

        float[] su = ring.spikeU, sd = ring.spikeD;
        boolean spiky = su != null && su.length > 0;
        int groups = spiky ? su.length : 1;
        int sm = spiky && shape != null && shape.spikeSmooth > 1
                ? (int) shape.spikeSmooth : 1;
        double hw = w / 2;

        boolean fine = !coarse && shape != null && shape.ringDiv >= DIV_FINE;
        int div = fine ? DIV_FINE : DIV;
        double uStep = fine ? U_STEP_FINE : U_STEP;
        double[] csin = fine ? FINE_SIN : CIRCLE_SIN;
        double[] ccos = fine ? FINE_COS : CIRCLE_COS;

        double sway = 0;
        if (shape != null && shape.swayPeriod > 0) {

            double ph = nowSec / shape.swayPeriod * (Math.PI * 2) - shape.swayLag * ny;
            sway = Math.sin(ph) * shape.swayAmp;
        }
        double upMul = 1 + sway;
        double dnMul = 1 - sway;

        double rootGrow = 1.0;
        if (shape != null && shape.spikeRootRise > 0) {
            double g = Math.min(1.0, ny / shape.spikeRootRise);
            rootGrow = g * g * (3 - 2 * g);
        }

        for (int idx = 0; idx < div; idx++) {
            int next = (idx + 1) % div;
            float x0 = (float) (csin[idx] * r + dx);
            float z0 = (float) (ccos[idx] * r + dz);
            float x1 = (float) (csin[next] * r + dx);
            float z1 = (float) (ccos[next] * r + dz);

            double u0n = idx / (double) div, u1n = next == 0 ? 1.0 : next / (double) div;
            double eu0 = spiky ? 1 + (lerpSpikeU(su, groups, u0n) - 1) * rootGrow : 1.0;
            double ed0 = spiky ? 1 + (lerpSpikeU(sd, groups, u0n) - 1) * rootGrow : 1.0;
            double eu1 = spiky ? 1 + (lerpSpikeU(su, groups, u1n) - 1) * rootGrow : 1.0;
            double ed1 = spiky ? 1 + (lerpSpikeU(sd, groups, u1n) - 1) * rootGrow : 1.0;
            float y0a = (float) (y + hw * eu0 * upMul);
            float y1a = (float) (y - hw * ed0 * dnMul);
            float y0b = (float) (y + hw * eu1 * upMul);
            float y1b = (float) (y - hw * ed1 * dnMul);

            float u0 = (float) (uStep * idx - rot);
            float u1 = (float) (u0 + uStep);

            vc.vertex(mat, x0, y0a, z0).uv(u0, 0).color(red, green, blue, alpha).endVertex();
            vc.vertex(mat, x0, y1a, z0).uv(u0, 1).color(red, green, blue, alpha).endVertex();
            vc.vertex(mat, x1, y1b, z1).uv(u1, 1).color(red, green, blue, alpha).endVertex();
            vc.vertex(mat, x1, y0b, z1).uv(u1, 0).color(red, green, blue, alpha).endVertex();
        }
    }

    @Override
    public ResourceLocation getTextureLocation(EntityStormWing entity) {
        return cn.academy.Resources.getTexture("effects/tornado_ring");
    }
}
