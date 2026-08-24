package cn.academy.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

@OnlyIn(Dist.CLIENT)
public final class DualWingSkin {

    private DualWingSkin() {}

    public record Box(int u, int v, float x, float y, float z,
                      float w, float h, float d, float grow, boolean mirror) {}

    private static final class V {
        float x, y, z, u, v;

        V(float x, float y, float z, float u, float v) {
            this.x = x; this.y = y; this.z = z; this.u = u; this.v = v;
        }

        static V clip(V a, V b, float clipY) {
            float t = (clipY - a.y) / (b.y - a.y);
            return new V(a.x + (b.x - a.x) * t, clipY, a.z + (b.z - a.z) * t,
                    a.u + (b.u - a.u) * t, a.v + (b.v - a.v) * t);
        }
    }

    private static final int MAX_SEG = 4;
    private static final Matrix4f[] SEG = new Matrix4f[MAX_SEG];
    private static final Matrix4f[] MID = new Matrix4f[MAX_SEG];
    private static final Matrix3f[] SEG_N = new Matrix3f[MAX_SEG];
    private static final Matrix3f[] MID_N = new Matrix3f[MAX_SEG];

    static {
        for (int i = 0; i < MAX_SEG; i++) {
            SEG[i] = new Matrix4f();
            MID[i] = new Matrix4f();
            SEG_N[i] = new Matrix3f();
            MID_N[i] = new Matrix3f();
        }
    }

    private static float[] clips = new float[0];
    private static int segCount = 1;

    private static final Vector4f TMP4 = new Vector4f();
    private static final Vector3f TMP3 = new Vector3f();

    public static void limb(PoseStack ps, VertexConsumer vc, int light, int overlay,
                            float clipY, float[] upRot, float[] lowRot,
                            int texW, int texH, Box... boxes) {
        begin(new float[]{clipY});
        segOffsets = null;
        seg(0).identity().rotateZYX(upRot[2], upRot[1], upRot[0]);
        joint(0, lowRot, clipY);
        finish();
        emit(ps, vc, light, overlay, texW, texH, boxes);
    }

    public static void limbMulti(PoseStack ps, VertexConsumer vc, int light, int overlay,
                                 float[] clipList, float[] rootRot, float[] rootPivot,
                                 float[][] rots, int texW, int texH, Box... boxes) {
        limbMulti(ps, vc, light, overlay, clipList, rootRot, rootPivot, rots, null, texW, texH, boxes);
    }

    public static void limbMulti(PoseStack ps, VertexConsumer vc, int light, int overlay,
                                 float[] clipList, float[] rootRot, float[] rootPivot,
                                 float[][] rots, float[][] offsets,
                                 int texW, int texH, Box... boxes) {
        begin(clipList);
        segOffsets = offsets;

        seg(0).identity()
                .translate(-rootPivot[0], -rootPivot[1], -rootPivot[2])
                .rotateZYX(rootRot[2], rootRot[1], rootRot[0])
                .translate(rootPivot[0], rootPivot[1], rootPivot[2]);
        for (int i = 0; i < clipList.length; i++) {
            joint(i, rots[i], clipList[i]);
        }
        finish();
        emit(ps, vc, light, overlay, texW, texH, boxes);
    }

    private static void begin(float[] clipList) {
        clips = clipList;
        segCount = clipList.length + 1;
    }

    public static boolean noSeam;

    private static Matrix4f seg(int i) {
        return SEG[i];
    }

    private static void joint(int i, float[] rot, float clipY) {
        MID[i].set(SEG[i]).translate(0.0f, clipY, 0.0f)
                .rotateZYX(rot[2] * 0.5f, rot[1] * 0.5f, rot[0] * 0.5f)
                .translate(0.0f, -clipY, 0.0f);
        SEG[i + 1].set(SEG[i]).translate(0.0f, clipY, 0.0f)
                .rotateZYX(rot[2], rot[1], rot[0])
                .translate(0.0f, -clipY, 0.0f);
        float[] off = segOffsets == null ? null : segOffsets[i];
        if (off != null && (off[0] != 0.0f || off[1] != 0.0f || off[2] != 0.0f)) {

            TMP_M.translation(off[0] * 0.5f, off[1] * 0.5f, off[2] * 0.5f);
            MID[i].set(TMP_M.mul(MID[i], TMP_M2));
            TMP_M.translation(off[0], off[1], off[2]);
            SEG[i + 1].set(TMP_M.mul(SEG[i + 1], TMP_M2));
        }
    }

    private static float[][] segOffsets;
    private static final Matrix4f TMP_M = new Matrix4f();
    private static final Matrix4f TMP_M2 = new Matrix4f();

    private static void finish() {

        for (int i = 0; i < segCount; i++) {
            SEG[i].get3x3(SEG_N[i]);
        }
        for (int i = 0; i < segCount - 1; i++) {
            MID[i].get3x3(MID_N[i]);
        }
    }

    private static void emit(PoseStack ps, VertexConsumer vc, int light, int overlay,
                             int texW, int texH, Box... boxes) {
        Matrix4f pose = ps.last().pose();
        Matrix3f normal = ps.last().normal();

        for (Box b : boxes) {
            float g = b.grow();
            float x0 = b.x() - g, y0 = b.y() - g, z0 = b.z() - g;
            float x1 = b.x() + b.w() + g, y1 = b.y() + b.h() + g, z1 = b.z() + b.d() + g;

            float u = b.u(), v = b.v(), w = b.w(), h = b.h(), d = b.d();
            float f4 = u, f5 = u + d, f6 = u + d + w, f7 = u + d + w + w;
            float f8 = u + d + w + d, f9 = u + d + w + d + w;
            float f10 = v, f11 = v + d, f12 = v + d + h;

            V v7 = new V(x0, y0, z0, 0, 0), vA = new V(x1, y0, z0, 0, 0);
            V v1 = new V(x1, y1, z0, 0, 0), v2 = new V(x0, y1, z0, 0, 0);
            V v3 = new V(x0, y0, z1, 0, 0), v4 = new V(x1, y0, z1, 0, 0);
            V v5 = new V(x1, y1, z1, 0, 0), v6 = new V(x0, y1, z1, 0, 0);

            face(vc, pose, normal, light, overlay, b, texW, texH,
                    v4, v3, v7, vA, f5, f10, f6, f11, Direction.DOWN);
            face(vc, pose, normal, light, overlay, b, texW, texH,
                    v1, v2, v6, v5, f6, f11, f7, f10, Direction.UP);
            face(vc, pose, normal, light, overlay, b, texW, texH,
                    v7, v3, v6, v2, f4, f11, f5, f12, Direction.WEST);
            face(vc, pose, normal, light, overlay, b, texW, texH,
                    vA, v7, v2, v1, f5, f11, f6, f12, Direction.NORTH);
            face(vc, pose, normal, light, overlay, b, texW, texH,
                    v4, vA, v1, v5, f6, f11, f8, f12, Direction.EAST);
            face(vc, pose, normal, light, overlay, b, texW, texH,
                    v3, v4, v5, v6, f8, f11, f9, f12, Direction.SOUTH);
        }
    }

    private static void face(VertexConsumer vc, Matrix4f pose, Matrix3f normal,
                             int light, int overlay, Box b, int texW, int texH,
                             V p0, V p1, V p2, V p3,
                             float u0, float v0, float u1, float v1, Direction dir) {
        p0.u = u1 / texW; p0.v = v0 / texH;
        p1.u = u0 / texW; p1.v = v0 / texH;
        p2.u = u0 / texW; p2.v = v1 / texH;
        p3.u = u1 / texW; p3.v = v1 / texH;

        float gnx = dir.getStepX(), gny = dir.getStepY(), gnz = dir.getStepZ();
        float nx = gnx, ny = gny, nz = gnz;
        if (b.mirror()) {
            V t = p0; p0 = p3; p3 = t;
            t = p1; p1 = p2; p2 = t;
            nx = -nx;
        }

        boolean downward = p3.y > p0.y;
        V a0 = p0, a1 = p1;
        for (int k = 0; k < clips.length; k++) {
            int idx = downward ? k : clips.length - 1 - k;
            float cy = clips[idx];

            if ((a0.y > cy) == (p3.y > cy)) {
                continue;
            }
            V c1 = V.clip(a1, p2, cy);
            V c2 = V.clip(a0, p3, cy);
            int segA = downward ? idx : idx + 1;
            int segB = downward ? idx + 1 : idx;
            quad(vc, pose, normal, light, overlay, nx, ny, nz,
                    a0, segA, a1, segA, c1, segA, c2, segA);

            if (!noSeam && seamOpen(c1, c2, idx)) {
                seamQuad(vc, pose, normal, light, overlay, nx, ny, nz, gnx, gny, gnz,
                        c2, MID_ID + idx, c1, MID_ID + idx, c1, segA, c2, segA);
                seamQuad(vc, pose, normal, light, overlay, nx, ny, nz, gnx, gny, gnz,
                        c2, segB, c1, segB, c1, MID_ID + idx, c2, MID_ID + idx);
            }
            a0 = c2;
            a1 = c1;
        }

        int last = segOf(a0.y, p3.y);
        quad(vc, pose, normal, light, overlay, nx, ny, nz, a0, last, a1, last, p2, last, p3, last);
    }

    private static final int MID_ID = 100;

    private static final Vector3f S_DOWN = new Vector3f();
    private static final Vector3f S_UP_POS = new Vector3f();
    private static final Vector3f S_GAP = new Vector3f();

    private static boolean seamOpen(V c1, V c2, int idx) {
        S_DOWN.set(0.0f, 1.0f, 0.0f).mul(SEG_N[idx]);
        return openAt(c1, idx) > 1.0e-5f || openAt(c2, idx) > 1.0e-5f;
    }

    private static float openAt(V q, int idx) {
        S_UP_POS.set(q.x, q.y, q.z).mulPosition(SEG[idx]);
        S_GAP.set(q.x, q.y, q.z).mulPosition(SEG[idx + 1]).sub(S_UP_POS);
        return S_GAP.dot(S_DOWN);
    }

    private static final float SEAM_PUSH = 0.02f;

    private static void seamQuad(VertexConsumer vc, Matrix4f pose, Matrix3f normal,
                                 int light, int overlay, float nx, float ny, float nz,
                                 float gnx, float gny, float gnz,
                                 V a, int ja, V b, int jb, V c, int jc, V d, int jd) {
        pushed(a, gnx, gny, gnz);
        vertex(vc, pose, normal, light, overlay, nx, ny, nz, PUSHED, ja);
        pushed(b, gnx, gny, gnz);
        vertex(vc, pose, normal, light, overlay, nx, ny, nz, PUSHED, jb);
        pushed(c, gnx, gny, gnz);
        vertex(vc, pose, normal, light, overlay, nx, ny, nz, PUSHED, jc);
        pushed(d, gnx, gny, gnz);
        vertex(vc, pose, normal, light, overlay, nx, ny, nz, PUSHED, jd);
    }

    private static final V PUSHED = new V(0, 0, 0, 0, 0);

    private static void pushed(V p, float nx, float ny, float nz) {
        PUSHED.x = p.x + nx * SEAM_PUSH;
        PUSHED.y = p.y + ny * SEAM_PUSH;
        PUSHED.z = p.z + nz * SEAM_PUSH;
        PUSHED.u = p.u;
        PUSHED.v = p.v;
    }

    private static int segOf(float ya, float yb) {
        float mid = (ya + yb) * 0.5f;
        int i = 0;
        while (i < clips.length && mid > clips[i]) {
            i++;
        }
        return i;
    }

    private static void quad(VertexConsumer vc, Matrix4f pose, Matrix3f normal,
                             int light, int overlay, float nx, float ny, float nz,
                             V a, int ja, V b, int jb, V c, int jc, V d, int jd) {
        vertex(vc, pose, normal, light, overlay, nx, ny, nz, a, ja);
        vertex(vc, pose, normal, light, overlay, nx, ny, nz, b, jb);
        vertex(vc, pose, normal, light, overlay, nx, ny, nz, c, jc);
        vertex(vc, pose, normal, light, overlay, nx, ny, nz, d, jd);
    }

    private static void vertex(VertexConsumer vc, Matrix4f pose, Matrix3f normal,
                               int light, int overlay, float nx, float ny, float nz,
                               V p, int joint) {
        boolean isMid = joint >= MID_ID;
        int i = isMid ? joint - MID_ID : joint;
        Matrix4f m = isMid ? MID[i] : SEG[i];
        Matrix3f n = isMid ? MID_N[i] : SEG_N[i];

        TMP4.set(p.x, p.y, p.z, 1.0f).mul(m);
        TMP4.set(TMP4.x() / 16.0f, TMP4.y() / 16.0f, TMP4.z() / 16.0f, 1.0f).mul(pose);
        TMP3.set(nx, ny, nz).mul(n).mul(normal);
        vc.vertex(TMP4.x(), TMP4.y(), TMP4.z(), 1.0f, 1.0f, 1.0f, 1.0f,
                p.u, p.v, overlay, light, TMP3.x(), TMP3.y(), TMP3.z());
    }
}
