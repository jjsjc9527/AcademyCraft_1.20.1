package cn.academy.client.render.util;

import cn.lambdalib2.util.RandUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@OnlyIn(Dist.CLIENT)
public class ArcFactory {

    public static final ResourceLocation TEXTURE =
            new ResourceLocation("academy", "textures/effects/arc/line_segment.png");

    private static final double NO_LIMIT = 23333333;

    static Random rand = new Random();

    public double width = 0.1;
    public double lengthShrink = 0.7;
    public double alphaShrink = 0.9;
    public int passes = 6;
    public double maxOffset = 1.5;
    public double branchFactor = 0.4;
    public double widthShrink = 0.7;
    public Vec3 normal = new Vec3(0, 0, 1);

    public double thickness = 0.0;

    public double bow = 0.0;

    private static final int BOW_SEGMENTS = 6;

    private final List<List<Segment>> listAll = new ArrayList<>();
    private final List<List<Segment>> bufferAll = new ArrayList<>();

    private void handleSingle(List<Segment> list, List<Segment> buffer, double offset) {
        buffer.clear();

        for (Segment s : list) {
            Point ave = average(s.start, s.end);
            float theta = (float) (rand.nextFloat() * Math.PI * 2);
            double sin = Mth.sin(theta), cos = Mth.cos(theta);
            double off = rand.nextFloat() * offset;
            double x = ave.pt.x, y = ave.pt.y, z = ave.pt.z;
            y += off * sin;
            z += off * cos;
            ave.pt = new Vec3(x, y, z);

            Segment s1 = s, s2 = new Segment(ave, s.end, s.alpha);
            s1.end = ave;
            buffer.add(s1);
            buffer.add(s2);

            if (rand.nextDouble() < branchFactor) {
                Vec3 dir = ave.pt.subtract(s.start.pt).scale(lengthShrink);
                dir = randomRotate(10, dir);

                double w2 = ave.width * widthShrink;
                Point p1 = new Point(ave.pt, w2);
                Point p2 = new Point(ave.pt.add(dir), w2);
                List<Segment> toAdd = new ArrayList<>();
                toAdd.add(new Segment(p1, p2, s.alpha * alphaShrink));
                bufferAll.add(toAdd);
                listAll.add(new ArrayList<>());
            }
        }
    }

    public Arc[] generateList(int count, double lengthFrom, double lengthTo) {
        Arc[] arr = new Arc[count];
        for (int i = 0; i < count; ++i) {
            arr[i] = generate(RandUtils.ranged(lengthFrom, lengthTo));
        }
        return arr;
    }

    public Arc generate(double length) {
        listAll.clear();
        bufferAll.clear();

        List<Segment> init = new ArrayList<>();
        if (bow > 0) {

            double phi = RandUtils.ranged(0, Math.PI * 2);
            double by = Math.sin(phi), bz = Math.cos(phi);
            double h = bow * length;
            Point prev = new Point(new Vec3(0, 0, 0), width);
            for (int i = 1; i <= BOW_SEGMENTS; ++i) {
                double t = (double) i / BOW_SEGMENTS;
                double lift = 4.0 * h * t * (1.0 - t);
                Point cur = new Point(new Vec3(length * t, by * lift, bz * lift), width);
                init.add(new Segment(prev, cur, 1.0));
                prev = cur;
            }
        } else {
            Vec3 v0 = new Vec3(0, 0, 0), v1 = new Vec3(length, 0, 0);
            init.add(new Segment(new Point(v0, width), new Point(v1, width), 1.0));
        }
        listAll.add(init);
        bufferAll.add(new ArrayList<>());

        boolean flip = false;
        double offset = maxOffset;
        int realPasses = passes;
        for (int i = 0; i < realPasses; ++i) {
            if (flip) {
                for (int j = 0; j < listAll.size(); ++j) {
                    handleSingle(bufferAll.get(j), listAll.get(j), offset);
                }
            } else {
                for (int j = 0; j < listAll.size(); ++j) {
                    handleSingle(listAll.get(j), bufferAll.get(j), offset);
                }
            }

            flip = !flip;
            offset /= 2;
        }

        return new Arc(flip ? bufferAll : listAll, normal, length, thickness);
    }

    private static Vec3 randomRotate(float range, Vec3 dir) {
        float a = (float) (RandUtils.rangef(-range, range) / 180 * Math.PI);
        Vec3 ret = dir;
        ret = ret.xRot(RandUtils.rangef(-a, a));
        ret = ret.yRot(RandUtils.rangef(-a, a));
        ret = ret.zRot(RandUtils.rangef(-a, a));
        return ret;
    }

    private Point average(Point pa, Point pb) {
        Vec3 v = pa.pt.lerp(pb.pt, 0.5);
        return new Point(v, (pa.width + pb.width) / 2);
    }

    static class Point {
        Vec3 pt;
        double width;

        Point(Vec3 pt, double w) {
            this.pt = pt;
            this.width = w;
        }
    }

    static class Segment {
        Point start, end;
        double alpha;

        Segment(Point s, Point e, double a) {
            start = s;
            end = e;
            alpha = a;
        }
    }

    private interface VertSink {
        void put(Vec3 pos, double u, double v, double alpha);
    }

    @OnlyIn(Dist.CLIENT)
    public static class Arc {

        private final List<List<Segment>> segmentList;
        private final Vec3 normal;
        private final double thickness;

        public final double length;

        private final float[] baked;

        Arc(List<List<Segment>> list, Vec3 normal, double len, double thickness) {
            segmentList = new ArrayList<>(list);
            this.normal = normal;
            this.thickness = thickness;
            length = len;
            baked = bake();
        }

        public void draw(PoseStack pose, VertexConsumer vc) {
            draw(pose, vc, 1f);
        }

        public void draw(PoseStack pose, VertexConsumer vc, float alphaMul) {
            draw(pose, vc, alphaMul, 1f, 1f, 1f);
        }

        public void draw(PoseStack pose, VertexConsumer vc, float alphaMul, float r, float g, float b) {
            Matrix4f mat = pose.last().pose();
            for (int i = 0; i < baked.length; i += 6) {
                vc.vertex(mat, baked[i], baked[i + 1], baked[i + 2])
                        .uv(baked[i + 3], baked[i + 4])
                        .color(r, g, b, baked[i + 5] * alphaMul)
                        .endVertex();
            }
        }

        public void draw(PoseStack pose, VertexConsumer vc, double length) {
            draw(pose, vc, length, 1f);
        }

        public void draw(PoseStack pose, VertexConsumer vc, double length, float alphaMul) {
            draw(pose, vc, length, alphaMul, 1f, 1f, 1f);
        }

        public void draw(PoseStack pose, VertexConsumer vc, double length, float alphaMul,
                         float r, float g, float b) {
            drawRange(pose, vc, 0, length, alphaMul, r, g, b);
        }

        public void drawRange(PoseStack pose, VertexConsumer vc, double from, double to,
                              float alphaMul, float r, float g, float b) {
            Matrix4f mat = pose.last().pose();
            emit((pos, u, v, alpha) ->
                            vc.vertex(mat, (float) pos.x, (float) pos.y, (float) pos.z)
                                    .uv((float) u, (float) v)
                                    .color(r, g, b, (float) alpha * alphaMul)
                                    .endVertex(),
                    from, to);
        }

        private float[] bake() {
            List<Float> out = new ArrayList<>();
            emit((pos, u, v, alpha) -> {
                out.add((float) pos.x);
                out.add((float) pos.y);
                out.add((float) pos.z);
                out.add((float) u);
                out.add((float) v);
                out.add((float) alpha);
            }, NO_LIMIT);

            float[] arr = new float[out.size()];
            for (int i = 0; i < arr.length; ++i) {
                arr[i] = out.get(i);
            }
            return arr;
        }

        private void emit(VertSink sink, double len) {
            emit(sink, 0, len);
        }

        private void emit(VertSink sink, double from, double to) {
            for (List<Segment> l : segmentList) {
                handleSegment(sink, l, normal, from, to);
            }
        }

        private void handleSegment(VertSink sink, List<Segment> list, Vec3 normal, double from, double to) {
            Vec3 lastDir = null, lastPerp = null;
            for (Segment s : list) {
                if (s.start.pt.x > to) {
                    break;
                }

                Vec3 axis = s.end.pt.subtract(s.start.pt);
                Vec3 dir = randomRotate(15, axis.cross(normal)).normalize();
                if (lastDir == null) {
                    lastDir = dir;
                }
                Vec3 perp = thickness > 0 ? axis.cross(dir).normalize() : null;
                if (perp != null && lastPerp == null) {
                    lastPerp = perp;
                }

                if (s.end.pt.x >= from) {
                    emitQuad(sink, s, lastDir, dir, s.start.width, s.end.width);

                    if (perp != null) {
                        emitQuad(sink, s, lastPerp, perp,
                                s.start.width * thickness, s.end.width * thickness);
                    }
                }

                if (perp != null) {
                    lastPerp = perp;
                }
                lastDir = dir;
            }
        }

        private static void emitQuad(VertSink sink, Segment s, Vec3 startDir, Vec3 endDir,
                                     double startW, double endW) {
            Vec3 p1 = s.start.pt.add(startDir.scale(startW));
            Vec3 p2 = s.start.pt.add(startDir.scale(-startW));
            Vec3 p3 = s.end.pt.add(endDir.scale(endW));
            Vec3 p4 = s.end.pt.add(endDir.scale(-endW));

            sink.put(p1, 0, 0, s.alpha);
            sink.put(p2, 0, 1, s.alpha);
            sink.put(p4, 1, 1, s.alpha);
            sink.put(p3, 1, 0, s.alpha);
        }
    }
}
