package cn.academy.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BooleanSupplier;

public final class RayReflect {

    private RayReflect() {}

    public static final double DEFAULT_STANDOFF = 1.0;

    public static final long DEFAULT_EXTEND_MS = 150;

    private static final double ARRIVE_EPS = 0.0;

    public static double hitDist(Vec3 from, Vec3 dir, Entity reflector, double standoff) {
        double proj = reflector.getEyePosition(1.0f).subtract(from).dot(dir.normalize());
        return Math.max(0, proj - standoff);
    }

    public static Vec3 hitPoint(Vec3 from, Vec3 dir, Entity reflector, double standoff) {
        Vec3 d = dir.normalize();
        return from.add(d.scale(hitDist(from, d, reflector, standoff)));
    }

    public static int arriveDelay(double hitDist, long extendMs) {
        if (extendMs <= 0) {
            return 0;
        }
        if (hitDist <= ARRIVE_EPS) {
            return 1;
        }
        double ms = extendMs * (hitDist - ARRIVE_EPS) / hitDist;
        return Math.max(1, (int) Math.ceil(ms / 50.0));
    }

    public static final double MIN_BEND_STEP = 0.05;

    public static int bendArriveDelay(double hitDist, double beamLength, long extendMs) {
        if (extendMs <= 0) {
            return 0;
        }
        if (beamLength <= 1.0e-6) {
            return arriveDelay(hitDist, extendMs);
        }
        double ms = extendMs * Math.max(0, hitDist - ARRIVE_EPS) / beamLength;
        return Math.max(1, (int) Math.ceil(ms / 50.0));
    }

    public static final double BEND_RADIUS = 1.6;

    private static final double BEND_MAX_EAT = 0.45;

    private static final double BEND_DEG_PER_DIV = 12.0;

    public static double bendRadiusFor(double inLen, double outLen) {
        double r = Math.min(BEND_RADIUS, Math.min(inLen, outLen) * BEND_MAX_EAT);
        return r < 0.05 ? 0 : r;
    }

    public static int bendDivFor(Vec3 d1, Vec3 d2) {
        double cos = Math.max(-1, Math.min(1, d1.normalize().dot(d2.normalize())));
        double deg = Math.toDegrees(Math.acos(cos));
        return Math.max(2, (int) Math.ceil(deg / BEND_DEG_PER_DIV));
    }

    public static java.util.List<Vec3> bezierArc(Vec3 a, Vec3 p, Vec3 b, int div) {
        java.util.List<Vec3> out = new ArrayList<>(div);
        for (int k = 1; k <= div; k++) {
            double t = (double) k / div, mt = 1 - t;
            out.add(a.scale(mt * mt).add(p.scale(2 * mt * t)).add(b.scale(t * t)));
        }
        return out;
    }

    public static byte[] encodePath(java.util.List<Vec3> path) {
        java.nio.ByteBuffer buf = java.nio.ByteBuffer.allocate(path.size() * 12);
        for (Vec3 p : path) {
            buf.putFloat((float) p.x).putFloat((float) p.y).putFloat((float) p.z);
        }
        return buf.array();
    }

    public static java.util.List<Vec3> decodePath(byte[] raw) {
        if (raw == null || raw.length < 24 || raw.length % 12 != 0) {
            return null;
        }
        java.nio.ByteBuffer buf = java.nio.ByteBuffer.wrap(raw);
        java.util.List<Vec3> path = new ArrayList<>(raw.length / 12);
        while (buf.remaining() >= 12) {
            path.add(new Vec3(buf.getFloat(), buf.getFloat(), buf.getFloat()));
        }
        return path;
    }

    public static double polyLength(Vec3 from, java.util.List<Vec3> pts) {
        double sum = 0;
        Vec3 prev = from;
        for (Vec3 q : pts) {
            sum += q.distanceTo(prev);
            prev = q;
        }
        return sum;
    }

    public static Vec3 mirror(Vec3 incoming, Vec3 normal) {
        Vec3 d = incoming.normalize();
        Vec3 n = normal.normalize();
        return d.subtract(n.scale(2 * d.dot(n)));
    }

    public static void fill(cn.academy.event.ability.ReflectEvent event,
                            Vec3 from, Vec3 dir, Entity reflector, long extendMs) {
        fill(event, from, dir, reflector, DEFAULT_STANDOFF, extendMs);
    }

    public static void fill(cn.academy.event.ability.ReflectEvent event,
                            Vec3 from, Vec3 dir, Entity reflector, double standoff, long extendMs) {
        Vec3 d = dir.normalize();
        double hd = hitDist(from, d, reflector, standoff);
        event.incomingFrom = from;
        event.incomingDir = d;
        event.hitDist = hd;
        event.hitPos = from.add(d.scale(hd));
        event.arriveDelay = arriveDelay(hd, extendMs);
    }

    public static void fill(cn.academy.event.ability.ReflectEvent event,
                            Vec3 from, Vec3 dir, Entity reflector,
                            double standoff, long extendMs, double traveled) {
        fill(event, from, dir, reflector, standoff, extendMs);
        event.arriveDelay = arriveDelay(traveled + event.hitDist, extendMs);
    }

    public static net.minecraft.world.entity.LivingEntity traceLiving(
            net.minecraft.world.level.Level level, Entity except, Vec3 from, Vec3 dir, double dist) {
        Vec3 end = from.add(dir.normalize().scale(dist));
        net.minecraft.world.phys.BlockHitResult block = level.clip(
                new net.minecraft.world.level.ClipContext(from, end,
                        net.minecraft.world.level.ClipContext.Block.COLLIDER,
                        net.minecraft.world.level.ClipContext.Fluid.NONE, except));
        Vec3 clipEnd = block.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK
                ? block.getLocation() : end;

        return AimTrace.firstLiving(level, except, from, clipEnd, e -> e != except);
    }

    public static final class Schedule {

        private static final class Item {
            int ticksLeft;
            final BooleanSupplier stillValid;
            final Runnable action;

            Item(int ticksLeft, BooleanSupplier stillValid, Runnable action) {
                this.ticksLeft = ticksLeft;
                this.stillValid = stillValid;
                this.action = action;
            }
        }

        private final List<Item> items = new ArrayList<>();

        public void after(int ticks, Runnable action) {
            after(ticks, () -> true, action);
        }

        public void after(int ticks, BooleanSupplier stillValid, Runnable action) {
            items.add(new Item(Math.max(1, ticks), stillValid, action));
        }

        public void tick() {
            if (items.isEmpty()) {
                return;
            }
            for (Iterator<Item> it = items.iterator(); it.hasNext(); ) {
                Item item = it.next();
                if (!item.stillValid.getAsBoolean()) {
                    it.remove();
                    continue;
                }
                if (--item.ticksLeft <= 0) {
                    it.remove();
                    item.action.run();
                }
            }
        }

        public boolean isEmpty() {
            return items.isEmpty();
        }

        public void clear() {
            items.clear();
        }
    }
}
