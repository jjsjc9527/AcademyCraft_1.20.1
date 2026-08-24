package cn.academy.client.render.misc;

import cn.academy.client.render.entity.FeatherWing;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
public final class WingWind {

    private WingWind() {}

    private static final double[] SAMPLE_T = {0.35, 0.65, 0.95};
    private static final double[] SAMPLE_F = {0.45, 1.00};
    private static final int PER_SIDE = SAMPLE_T.length * SAMPLE_F.length;
    private static final int POINTS = PER_SIDE * 2;

    private static final int STRIDE = 7;

    private static final double RADIUS = 0.85;

    private static final double STRENGTH = 0.12;

    private static final double VN_CAP = 0.5;

    private static final int STALE_TICKS = 4;

    private static final class Source {
        final double[] data = new double[POINTS * STRIDE];
        final double[] prev = new double[POINTS * 3];
        boolean hasPrev = false;
        long tick = Long.MIN_VALUE;
        boolean valid = false;
    }

    private static final Map<UUID, Source> MAP = new ConcurrentHashMap<>();

    private static final double[] TMP = new double[6];

    public static void publish(UUID id, double grow, long tick,
                               Vec3 origin, Vec3 left, Vec3 up, Vec3 front, double rootY) {
        Source s = MAP.computeIfAbsent(id, k -> new Source());
        if (s.tick == tick) {
            return;
        }
        boolean fresh = s.hasPrev && tick - s.tick == 1;
        s.tick = tick;

        int k = 0;
        boolean any = false;
        for (int side = 0; side < 2; side++) {
            for (double t : SAMPLE_T) {
                for (double f : SAMPLE_F) {
                    int base = k * STRIDE;
                    int pb = k * 3;
                    k++;
                    if (!FeatherWing.surfacePoint(side, t, f, grow, TMP)) {
                        s.data[base + 6] = 0;
                        continue;
                    }

                    double lx = TMP[0], ly = TMP[1] + rootY, lz = TMP[2];
                    double wx = origin.x + left.x * lx + up.x * ly + front.x * lz;
                    double wy = origin.y + left.y * lx + up.y * ly + front.y * lz;
                    double wz = origin.z + left.z * lx + up.z * ly + front.z * lz;
                    double nx = left.x * TMP[3] + up.x * TMP[4] + front.x * TMP[5];
                    double ny = left.y * TMP[3] + up.y * TMP[4] + front.y * TMP[5];
                    double nz = left.z * TMP[3] + up.z * TMP[4] + front.z * TMP[5];

                    double vn = 0;
                    if (fresh) {
                        vn = (wx - s.prev[pb]) * nx
                                + (wy - s.prev[pb + 1]) * ny
                                + (wz - s.prev[pb + 2]) * nz;
                        vn = Math.max(-VN_CAP, Math.min(VN_CAP, vn));
                    }
                    s.prev[pb] = wx;
                    s.prev[pb + 1] = wy;
                    s.prev[pb + 2] = wz;

                    s.data[base] = wx;
                    s.data[base + 1] = wy;
                    s.data[base + 2] = wz;
                    s.data[base + 3] = nx;
                    s.data[base + 4] = ny;
                    s.data[base + 5] = nz;
                    s.data[base + 6] = vn;
                    any = true;
                }
            }
        }
        s.hasPrev = true;
        s.valid = any;
    }

    public static void remove(UUID id) {
        MAP.remove(id);
    }

    public static boolean push(double px, double py, double pz, long tick, double[] out) {
        if (MAP.isEmpty()) {
            return false;
        }
        boolean hit = false;
        for (Source s : MAP.values()) {
            if (!s.valid || tick - s.tick > STALE_TICKS) {
                continue;
            }
            for (int i = 0; i < POINTS; i++) {
                int b = i * STRIDE;
                double vn = s.data[b + 6];
                if (vn == 0) {
                    continue;
                }
                double dx = px - s.data[b];
                double dy = py - s.data[b + 1];
                double dz = pz - s.data[b + 2];
                double d2 = dx * dx + dy * dy + dz * dz;
                if (d2 > RADIUS * RADIUS) {
                    continue;
                }
                double nx = s.data[b + 3], ny = s.data[b + 4], nz = s.data[b + 5];
                double toF = dx * nx + dy * ny + dz * nz;

                if (toF * vn <= 0) {
                    continue;
                }

                double w = 1.0 - Math.sqrt(d2) / RADIUS;
                double k = vn * w * w * STRENGTH;
                out[0] += nx * k;
                out[1] += ny * k;
                out[2] += nz * k;
                hit = true;
            }
        }
        return hit;
    }
}
