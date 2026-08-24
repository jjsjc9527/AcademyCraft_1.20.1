package cn.academy.client.render.entity;

import cn.academy.client.render.ACRenderTypes;
import cn.academy.entity.EntityRailgunFX;
import cn.academy.entity.IRay;
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
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class RailgunFXRenderer extends EntityRenderer<EntityRailgunFX> {

    private static final ResourceLocation TEX_BLEND_IN = ray("blend_in");
    private static final ResourceLocation TEX_TILE = ray("tile");
    private static final ResourceLocation TEX_BLEND_OUT = ray("blend_out");

    private static ResourceLocation ray(String name) {
        return new ResourceLocation("academy", "textures/effects/railgun/" + name + ".png");
    }

    private static final int DIV = 12;
    private static final double[][] HEAD_V;
    private static final int[] HEAD_Q;
    private static final double[][] CYL_V;
    private static final int[] CYL_Q;

    static {
        double drad = Math.PI * 2 / DIV;
        double[] sins = new double[DIV], cosines = new double[DIV];
        double cur = 0;
        for (int i = 0; i < DIV; ++i) {
            sins[i] = Math.sin(cur);
            cosines[i] = Math.cos(cur);
            cur += drad;
        }

        {
            int D = 4;
            double dlen = 1.0 / D;
            List<double[]> verts = new ArrayList<>();
            List<Integer> faces = new ArrayList<>();
            double x = 0.0;
            for (int i = 0; i <= D; ++i) {
                double y = Math.sqrt(x);
                for (int j = 0; j < DIV; ++j) {
                    verts.add(new double[]{x, y * sins[j], y * cosines[j]});
                }
                x += dlen;
            }
            for (int i = 0; i < D; ++i) {
                int offset = DIV * i;
                for (int j = 0; j < DIV - 1; ++j) {
                    faces.add(offset);
                    faces.add(offset + DIV);
                    faces.add(offset + DIV + 1);
                    faces.add(offset + 1);
                    offset++;
                }
                faces.add(DIV * i);
                faces.add(offset);
                faces.add(offset + DIV);
                faces.add(DIV * i + DIV);
            }
            HEAD_V = verts.toArray(new double[0][]);
            HEAD_Q = toIntArray(faces);
        }

        {
            List<double[]> verts = new ArrayList<>();
            List<Integer> faces = new ArrayList<>();
            for (int j = 0; j < DIV; ++j) {
                verts.add(new double[]{0, sins[j], cosines[j]});
                verts.add(new double[]{1, sins[j], cosines[j]});
            }
            for (int i = 0; i < DIV - 1; ++i) {
                faces.add(i * 2);
                faces.add(i * 2 + 1);
                faces.add(i * 2 + 3);
                faces.add(i * 2 + 2);
            }
            int n = (DIV - 1) * 2;
            faces.add(n);
            faces.add(n + 1);
            faces.add(1);
            faces.add(0);
            CYL_V = verts.toArray(new double[0][]);
            CYL_Q = toIntArray(faces);
        }
    }

    private static final double[][] BALL_V;
    private static final int[] BALL_Q;

    static {
        int LAT = 8, LON = 12;
        List<double[]> verts = new ArrayList<>();
        List<Integer> faces = new ArrayList<>();
        for (int i = 0; i <= LAT; ++i) {
            double theta = Math.PI * i / LAT;
            double sin = Math.sin(theta), cos = Math.cos(theta);
            for (int j = 0; j < LON; ++j) {
                double phi = Math.PI * 2 * j / LON;
                verts.add(new double[]{sin * Math.cos(phi), cos, sin * Math.sin(phi)});
            }
        }
        for (int i = 0; i < LAT; ++i) {
            for (int j = 0; j < LON; ++j) {
                int j2 = (j + 1) % LON;
                faces.add(i * LON + j);
                faces.add(i * LON + j2);
                faces.add((i + 1) * LON + j2);
                faces.add((i + 1) * LON + j);
            }
        }
        BALL_V = verts.toArray(new double[0][]);
        BALL_Q = toIntArray(faces);
    }

    private static int[] toIntArray(List<Integer> l) {
        int[] a = new int[l.size()];
        for (int i = 0; i < a.length; ++i) a[i] = l.get(i);
        return a;
    }

    public record Cylinder(float r, float g, float b, float a, double width, double headFix) {}

    public record GlowStyle(ResourceLocation blendIn, ResourceLocation tile, ResourceLocation blendOut,
                            double width, double startFix, double endFix) {}

    private static final Cylinder CYL_IN = new Cylinder(241, 240, 222, 200, 0.09, 0.98);
    private static final Cylinder CYL_OUT = new Cylinder(236, 170, 93, 60, 0.13, 1.00);

    private static final GlowStyle GLOW = new GlowStyle(TEX_BLEND_IN, TEX_TILE, TEX_BLEND_OUT, 1.1, -0.3, 0.3);

    public RailgunFXRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(EntityRailgunFX fx, float yaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffers, int packedLight) {

    }

    public static void draw(EntityRailgunFX fx, float partialTick, Vec3 camPos,
                            PoseStack pose, MultiBufferSource buffers) {
        fx.onRenderTick();

        Vec3 dir = lookVec(fx.getYRot(), fx.getXRot());

        pose.pushPose();
        if (fx.needsViewOptimize()) {
            Vec3 vo = handOffset(fx);
            pose.translate(vo.x, vo.y, vo.z);
        }

        fx.arcHandler.drawAll(pose, buffers, 1.0, (float) fx.getAlpha());
        pose.popPose();

        Vec3 handOff = fx.needsViewOptimize() ? handOffset(fx) : Vec3.ZERO;
        Vec3 fpUp = ViewOptimize.isFirstPerson(fx)

                ? cn.academy.gravity.RotationUtil.vecPlayerToWorld(new Vec3(0, 1, -0.5), fx.gravAtFire)
                : null;

        pose.pushPose();
        if (fx.getPath() != null) {

            drawGlowPath(fx, camPos, pose, buffers, GLOW, handOff, fpUp);
            drawCylinderPath(fx, CYL_IN, pose, buffers, handOff);
            drawCylinderPath(fx, CYL_OUT, pose, buffers, handOff);
        } else {
            drawGlow(fx, dir, camPos, pose, buffers, GLOW, handOff, fpUp);
            drawCylinder(fx, dir, CYL_IN, pose, buffers, handOff);
            drawCylinder(fx, dir, CYL_OUT, pose, buffers, handOff);
        }
        pose.popPose();
    }

    private static int frontSegment(double[] cum, double shown) {
        int last = -1;
        for (int i = 0; i + 1 < cum.length; i++) {
            if (shown > cum[i]) last = i;
        }
        return last;
    }

    public static void drawGlowPath(IRay ray, Vec3 camPos, PoseStack pose, MultiBufferSource buffers,
                                    GlowStyle style, Vec3 handOff, Vec3 fpUp) {
        Vec3[] path = ray.getPath();
        double[] cum = ray.getPathCum();
        if (path == null || cum == null || path.length < 2) {
            return;
        }
        double shown = ray.getLength();
        int lastSeg = frontSegment(cum, shown);
        if (lastSeg < 0) {
            return;
        }

        Vec3[] dir = new Vec3[path.length - 1];
        for (int i = 0; i < dir.length; i++) {
            Vec3 sv = path[i + 1].subtract(path[i]);
            dir[i] = sv.lengthSqr() < 1e-8 ? null : sv.normalize();
        }
        if (dir[0] == null) {
            return;
        }

        double s0 = ray.getStartFix() + style.startFix();
        double s1 = shown + style.endFix();
        if (s1 - s0 < 1.0e-4) {
            return;
        }

        double w = Math.min(style.width(), (s1 - s0) / 2);
        double tin = s0 + w, tout = s1 - w;

        Vec3[] nodeUp = new Vec3[path.length];
        Vec3 origin = ray.getRayPosition();
        for (int j = 0; j < path.length; j++) {
            Vec3 tangent;
            if (j == 0) {
                tangent = dir[0];
            } else if (j >= dir.length) {
                tangent = dir[dir.length - 1];
            } else if (dir[j - 1] == null || dir[j] == null) {
                tangent = dir[j - 1] != null ? dir[j - 1] : dir[j];
            } else {
                tangent = dir[j - 1].add(dir[j]);
                tangent = tangent.lengthSqr() < 1.0e-8 ? dir[j] : tangent.normalize();
            }

            Vec3 up;
            if (fpUp != null) {
                up = fpUp;
            } else {
                up = tangent.cross(origin.add(path[j]).subtract(camPos));
                if (up.lengthSqr() < 1e-6) up = new Vec3(0, 1, 0);
            }
            up = up.normalize();

            if (j > 0 && up.dot(nodeUp[j - 1]) < 0) {
                up = up.scale(-1);
            }
            nodeUp[j] = up;
        }

        int alpha = clamp255(255 * ray.getAlpha() * ray.getGlowAlpha());
        double boardWidth = style.width() * ray.getWidth();
        Matrix4f mat = pose.last().pose();

        for (int i = 0; i <= lastSeg; i++) {
            if (dir[i] == null) continue;
            double segA = i == 0 ? s0 : cum[i];
            double segB = i == lastSeg ? s1 : Math.min(cum[i + 1], s1);
            if (segB - segA < 1.0e-6) continue;

            glowPart(buffers, mat, style.blendIn(), segA, segB, s0, tin, path, cum, dir, handOff, nodeUp, boardWidth, alpha);
            glowPart(buffers, mat, style.tile(), segA, segB, tin, tout, path, cum, dir, handOff, nodeUp, boardWidth, alpha);
            glowPart(buffers, mat, style.blendOut(), segA, segB, tout, s1, path, cum, dir, handOff, nodeUp, boardWidth, alpha);
        }
    }

    private static void glowPart(MultiBufferSource buffers, Matrix4f mat, ResourceLocation tex,
                                 double segA, double segB, double rangeA, double rangeB,
                                 Vec3[] path, double[] cum, Vec3[] dir, Vec3 handOff,
                                 Vec3[] nodeUp, double width, int alpha) {
        double a = Math.max(segA, rangeA), b = Math.min(segB, rangeB);
        if (b - a < 1.0e-6 || rangeB - rangeA < 1.0e-9) {
            return;
        }
        float u0 = (float) ((a - rangeA) / (rangeB - rangeA));
        float u1 = (float) ((b - rangeA) / (rangeB - rangeA));

        drawBoard(buffers.getBuffer(ACRenderTypes.rayGlow(tex)), mat,
                arcPoint(path, cum, dir, handOff, a), arcPoint(path, cum, dir, handOff, b),
                upAtArc(cum, nodeUp, a), upAtArc(cum, nodeUp, b), width, alpha, u0, u1);
    }

    private static Vec3 upAtArc(double[] cum, Vec3[] nodeUp, double s) {
        for (int i = 0; i + 1 < nodeUp.length; i++) {
            if (s <= cum[i + 1] || i + 2 == nodeUp.length) {
                double span = cum[i + 1] - cum[i];
                double t = span < 1.0e-9 ? 0 : Math.max(0, Math.min(1, (s - cum[i]) / span));
                Vec3 r = nodeUp[i].scale(1 - t).add(nodeUp[i + 1].scale(t));
                return r.lengthSqr() < 1.0e-8 ? nodeUp[i] : r.normalize();
            }
        }
        return nodeUp[nodeUp.length - 1];
    }

    private static Vec3 arcPoint(Vec3[] path, double[] cum, Vec3[] dir, Vec3 handOff, double s) {
        for (int i = 0; i + 1 < path.length; i++) {
            if (dir[i] == null) continue;
            if (s <= cum[i + 1] || i + 2 == path.length) {
                Vec3 base = i == 0 ? handOff : path[i];
                return base.add(dir[i].scale(s - cum[i]));
            }
        }
        return handOff;
    }

    public static void drawCylinderPath(IRay ray, Cylinder cyl, PoseStack pose,
                                        MultiBufferSource buffers, Vec3 vo) {
        Vec3[] path = ray.getPath();
        double[] cum = ray.getPathCum();
        if (path == null || cum == null || path.length < 2) {
            return;
        }
        double shown = ray.getLength();
        int lastSeg = frontSegment(cum, shown);
        if (lastSeg < 0) {
            return;
        }

        int alpha = clamp255(cyl.a * ray.getAlpha());
        double width = cyl.width * ray.getWidth();
        double nose = width * cyl.headFix;
        VertexConsumer vc = buffers.getBuffer(ACRenderTypes.rayCylinder());

        Vec3[] dir = new Vec3[path.length - 1];
        for (int i = 0; i < dir.length; i++) {
            Vec3 sv = path[i + 1].subtract(path[i]);
            dir[i] = sv.lengthSqr() < 1e-8 ? null : sv.normalize();
        }

        Vec3 carryU = null;
        for (int i = 0; i <= lastSeg; i++) {
            if (dir[i] == null) continue;
            boolean first = i == 0, last = i == lastSeg;

            Vec3 segStart = first ? vo.add(dir[i].scale(ray.getStartFix())) : path[i];
            Vec3 segEnd = path[i].add(dir[i].scale(Math.min(shown, cum[i + 1]) - cum[i]));

            Vec3 bodyA = first ? segStart.add(dir[i].scale(width)) : segStart;
            if (segEnd.subtract(bodyA).dot(dir[i]) <= 1.0e-6) continue;

            Vec3 nA = first ? null : miterNormal(dir[i - 1], dir[i]);
            Vec3 nB = last ? null : miterNormal(dir[i], dir[i + 1]);
            carryU = emitTube(vc, pose.last().pose(), bodyA, segEnd, nA, nB, width, cyl, alpha, carryU);

            if (first) drawNose(vc, pose, bodyA, dir[i], false, nose, width, cyl, alpha);
            if (last) drawNose(vc, pose, segEnd, dir[i], true, nose, width, cyl, alpha);
        }

        for (int j = 1; j <= lastSeg; j++) {
            if (dir[j - 1] != null && dir[j] != null && miterNormal(dir[j - 1], dir[j]) == null) {
                drawJointBall(ray, cyl, path[j], pose, buffers);
            }
        }
    }

    private static Vec3 miterNormal(Vec3 d1, Vec3 d2) {
        Vec3 sum = d1.add(d2);
        if (sum.lengthSqr() < 1.0e-6) {
            return null;
        }
        Vec3 n = sum.normalize();
        return d1.dot(n) < 0.5 ? null : n;
    }

    private static double slide(Vec3 radial, Vec3 n, Vec3 axis) {
        if (n == null) {
            return 0;
        }
        double denom = axis.dot(n);
        return Math.abs(denom) < 1.0e-3 ? 0 : -radial.dot(n) / denom;
    }

    private static Vec3 emitTube(VertexConsumer vc, Matrix4f mat, Vec3 a, Vec3 b,
                                 Vec3 nA, Vec3 nB, double radius, Cylinder cyl, int alpha, Vec3 prevU) {
        Vec3 axis = b.subtract(a);
        if (axis.lengthSqr() < 1.0e-10) {
            return prevU;
        }
        axis = axis.normalize();
        Vec3 u = prevU == null ? null : prevU.subtract(axis.scale(prevU.dot(axis)));
        if (u == null || u.lengthSqr() < 1.0e-6) {
            Vec3 ref = Math.abs(axis.y) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
            u = axis.cross(ref);
        }
        u = u.normalize();
        Vec3 v = axis.cross(u).normalize();

        Vec3[] rad = new Vec3[DIV];
        for (int i = 0; i < DIV; i++) {
            double th = Math.PI * 2 * i / DIV;
            rad[i] = u.scale(Math.cos(th) * radius).add(v.scale(Math.sin(th) * radius));
        }
        for (int i = 0; i < DIV; i++) {
            Vec3 r1 = rad[i], r2 = rad[(i + 1) % DIV];
            emitQuad(vc, mat, cyl, alpha,
                    a.add(r1).add(axis.scale(slide(r1, nA, axis))),
                    b.add(r1).add(axis.scale(slide(r1, nB, axis))),
                    b.add(r2).add(axis.scale(slide(r2, nB, axis))),
                    a.add(r2).add(axis.scale(slide(r2, nA, axis))));
        }
        return u;
    }

    private static void emitQuad(VertexConsumer vc, Matrix4f mat, Cylinder cyl, int alpha, Vec3... p) {
        for (Vec3 q : p) {
            vc.vertex(mat, (float) q.x, (float) q.y, (float) q.z)
                    .color((int) cyl.r, (int) cyl.g, (int) cyl.b, alpha)
                    .endVertex();
        }
    }

    private static void drawNose(VertexConsumer vc, PoseStack pose, Vec3 at, Vec3 dir, boolean outward,
                                 double len, double width, Cylinder cyl, int alpha) {
        double dxzsq = dir.x * dir.x + dir.z * dir.z;
        pose.pushPose();
        pose.translate(at.x, at.y, at.z);
        pose.mulPose(Axis.YP.rotationDegrees((float) (-90 + MathUtils.toDegrees(Math.atan2(dir.x, dir.z)))));
        pose.mulPose(Axis.ZP.rotationDegrees((float) MathUtils.toDegrees(Math.atan2(dir.y, Math.sqrt(dxzsq)))));

        pose.translate(outward ? len : -len, 0, 0);
        pose.scale((float) (outward ? -len : len), (float) width, (float) width);
        emitMesh(vc, pose.last().pose(), HEAD_V, HEAD_Q, cyl, alpha);
        pose.popPose();
    }

    public static void drawGlow(IRay ray, Vec3 dir, Vec3 camPos, PoseStack pose, MultiBufferSource buffers,
                                GlowStyle style, Vec3 handOff, Vec3 fpUp) {
        double startFix = ray.getStartFix();
        Vec3 start = dir.scale(startFix);
        Vec3 end = start.add(dir.scale(ray.getLength() - startFix));

        Vec3 upDir;
        if (fpUp != null) {
            upDir = fpUp;
        } else {
            Vec3 camToEnt = ray.getRayPosition().subtract(camPos);
            upDir = dir.cross(camToEnt);
            if (upDir.lengthSqr() < 1e-6) upDir = new Vec3(0, 1, 0);
        }
        upDir = upDir.normalize();

        start = start.add(handOff);

        if (end.subtract(start).lengthSqr() < 1e-8) {
            return;
        }

        Vec3 look = end.subtract(start).normalize();
        end = end.add(look.scale(style.endFix()));
        start = start.add(look.scale(style.startFix()));
        Vec3 mid1 = start.add(look.scale(style.width()));
        Vec3 mid2 = end.add(look.scale(-style.width()));

        int alpha = clamp255(255 * ray.getAlpha() * ray.getGlowAlpha());
        double boardWidth = style.width() * ray.getWidth();

        Matrix4f mat = pose.last().pose();
        drawBoard(buffers.getBuffer(ACRenderTypes.rayGlow(style.blendIn())), mat, start, mid1, upDir, boardWidth, alpha);
        drawBoard(buffers.getBuffer(ACRenderTypes.rayGlow(style.tile())), mat, mid1, mid2, upDir, boardWidth, alpha);
        drawBoard(buffers.getBuffer(ACRenderTypes.rayGlow(style.blendOut())), mat, mid2, end, upDir, boardWidth, alpha);
    }

    private static void drawBoard(VertexConsumer vc, Matrix4f mat, Vec3 s, Vec3 e, Vec3 up, double width, int alpha) {
        drawBoard(vc, mat, s, e, up, up, width, alpha, 0, 1);
    }

    private static void drawBoard(VertexConsumer vc, Matrix4f mat, Vec3 s, Vec3 e, Vec3 upS, Vec3 upE,
                                  double width, int alpha, float u0, float u1) {
        double w = width / 2;
        Vec3 v1 = s.add(upS.scale(w));
        Vec3 v2 = s.add(upS.scale(-w));
        Vec3 v3 = e.add(upE.scale(-w));
        Vec3 v4 = e.add(upE.scale(w));
        vertex(vc, mat, v1, u0, 1, alpha);
        vertex(vc, mat, v2, u0, 0, alpha);
        vertex(vc, mat, v3, u1, 0, alpha);
        vertex(vc, mat, v4, u1, 1, alpha);
    }

    private static void vertex(VertexConsumer vc, Matrix4f mat, Vec3 p, float u, float v, int alpha) {
        vc.vertex(mat, (float) p.x, (float) p.y, (float) p.z)
                .uv(u, v)
                .color(255, 255, 255, alpha)
                .endVertex();
    }

    private static Vec3 handOffset(EntityRailgunFX fx) {
        Vec3 vo = ViewOptimize.getFixVector(fx).yRot((float) MathUtils.toRadians(270 - fx.localYawAtFire));
        return cn.academy.gravity.RotationUtil.vecPlayerToWorld(vo, fx.gravAtFire);
    }

    public static void drawCylinder(IRay ray, Vec3 dir, Cylinder cyl, PoseStack pose, MultiBufferSource buffers, Vec3 vo) {
        double length = ray.getLength();
        double fix = ray.getStartFix();

        Vec3 end = dir.scale(length);
        Vec3 delta = end.subtract(vo);
        double dxzsq = delta.x * delta.x + delta.z * delta.z;
        double npitch = MathUtils.toDegrees(Math.atan2(delta.y, Math.sqrt(dxzsq)));
        double nyaw = MathUtils.toDegrees(Math.atan2(delta.x, delta.z));

        if (delta.lengthSqr() < 1e-8) {
            return;
        }

        pose.pushPose();
        pose.translate(vo.x, vo.y, vo.z);
        pose.mulPose(Axis.YP.rotationDegrees((float) (-90 + nyaw)));
        pose.mulPose(Axis.ZP.rotationDegrees((float) npitch));
        pose.translate(fix, 0, 0);

        drawCylinderMesh(ray, cyl, length - fix, pose, buffers);

        pose.popPose();
    }

    private static void drawCylinderMesh(IRay ray, Cylinder cyl, double len, PoseStack pose, MultiBufferSource buffers) {
        int alpha = clamp255(cyl.a * ray.getAlpha());
        double width = cyl.width * ray.getWidth();
        double offset = width * (1 - cyl.headFix);

        VertexConsumer vc = buffers.getBuffer(ACRenderTypes.rayCylinder());

        pose.pushPose();
        pose.translate(offset, 0, 0);
        pose.scale((float) (width * cyl.headFix), (float) width, (float) width);
        emitMesh(vc, pose.last().pose(), HEAD_V, HEAD_Q, cyl, alpha);
        pose.popPose();

        pose.pushPose();
        pose.translate(width, 0, 0);
        pose.scale((float) (len - width), (float) width, (float) width);
        emitMesh(vc, pose.last().pose(), CYL_V, CYL_Q, cyl, alpha);
        pose.popPose();

        pose.pushPose();
        pose.translate(len + width - offset, 0, 0);
        pose.scale((float) (-width * cyl.headFix), (float) width, (float) (-width));
        emitMesh(vc, pose.last().pose(), HEAD_V, HEAD_Q, cyl, alpha);
        pose.popPose();
    }

    private static void drawJointBall(IRay ray, Cylinder cyl, Vec3 at, PoseStack pose, MultiBufferSource buffers) {
        int alpha = clamp255(cyl.a * ray.getAlpha());
        float width = (float) (cyl.width * ray.getWidth());
        pose.pushPose();
        pose.translate(at.x, at.y, at.z);
        pose.scale(width, width, width);
        emitMesh(buffers.getBuffer(ACRenderTypes.rayCylinder()), pose.last().pose(), BALL_V, BALL_Q, cyl, alpha);
        pose.popPose();
    }

    private static void emitMesh(VertexConsumer vc, Matrix4f mat, double[][] verts, int[] quads, Cylinder cyl, int alpha) {
        for (int i = 0; i + 3 < quads.length; i += 4) {
            for (int k = 0; k < 4; ++k) {
                double[] p = verts[quads[i + k]];
                vc.vertex(mat, (float) p[0], (float) p[1], (float) p[2])
                        .color((int) cyl.r, (int) cyl.g, (int) cyl.b, alpha)
                        .endVertex();
            }
        }
    }

    public static Vec3 lookVec(float yaw, float pitch) {
        float f = pitch * Mth.DEG_TO_RAD;
        float f1 = -yaw * Mth.DEG_TO_RAD;
        float cosF = Mth.cos(f);
        return new Vec3(Mth.sin(f1) * cosF, -Mth.sin(f), Mth.cos(f1) * cosF);
    }

    private static int clamp255(double v) {
        int i = (int) Math.round(v);
        return i < 0 ? 0 : Math.min(i, 255);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityRailgunFX entity) {
        return TEX_TILE;
    }
}
