package cn.academy.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.function.Predicate;

public final class AimTrace {

    private AimTrace() {}

    public static final float INFLATE = 0.3f;

    public static Entity first(Level level, Entity except, Vec3 from, Vec3 to, Predicate<Entity> filter) {
        AABB range = new AABB(from, to).inflate(1.0 + INFLATE);
        Entity best = null;
        double bestSq = Double.MAX_VALUE;

        for (Entity e : level.getEntities(except, range, filter::test)) {
            AABB box = e.getBoundingBox().inflate(INFLATE);

            if (box.contains(from)) {
                return e;
            }
            Optional<Vec3> hit = box.clip(from, to);
            if (hit.isPresent()) {
                double d = from.distanceToSqr(hit.get());
                if (d < bestSq) {
                    bestSq = d;
                    best = e;
                }
            }
        }
        return best;
    }

    public static java.util.List<Entity> nearest(Level level, Entity except, Vec3 from, Vec3 to,
                                                 double extra, int n, Predicate<Entity> filter) {
        if (n <= 0) {
            return java.util.Collections.emptyList();
        }
        double pad = INFLATE + Math.max(0, extra);
        AABB range = new AABB(from, to).inflate(1.0 + pad);
        java.util.List<Entity> found = new java.util.ArrayList<>();
        java.util.Map<Entity, Double> dist = new java.util.IdentityHashMap<>();

        for (Entity e : level.getEntities(except, range, filter::test)) {
            AABB box = e.getBoundingBox().inflate(pad);
            double d;
            if (box.contains(from)) {
                d = 0;
            } else {
                Optional<Vec3> hit = box.clip(from, to);
                if (hit.isEmpty()) {
                    continue;
                }
                d = from.distanceToSqr(hit.get());
            }
            found.add(e);
            dist.put(e, d);
        }
        found.sort(java.util.Comparator.comparingDouble(dist::get));
        return found.size() <= n ? found : new java.util.ArrayList<>(found.subList(0, n));
    }

    public static java.util.List<LivingEntity> nearestLiving(Level level, Entity except,
                                                             Vec3 from, Vec3 to,
                                                             double extra, int n,
                                                             Predicate<Entity> filter) {
        java.util.List<LivingEntity> out = new java.util.ArrayList<>();
        for (Entity e : nearest(level, except, from, to, extra, n,
                x -> x.isAlive() && x instanceof LivingEntity && filter.test(x))) {
            out.add((LivingEntity) e);
        }
        return out;
    }

    public record Hit(Entity entity, Vec3 pos) {}

    public static Hit firstHitOf(Iterable<? extends Entity> candidates, Vec3 from, Vec3 to) {
        Hit best = null;
        double bestSq = Double.MAX_VALUE;
        for (Entity e : candidates) {
            AABB b = e.getBoundingBox().inflate(INFLATE);
            if (b.contains(from)) {
                return new Hit(e, from);
            }
            Optional<Vec3> hit = b.clip(from, to);
            if (hit.isPresent()) {
                double d = from.distanceToSqr(hit.get());
                if (d < bestSq) {
                    bestSq = d;
                    best = new Hit(e, hit.get());
                }
            }
        }
        return best;
    }

    public static Entity firstOf(Iterable<? extends Entity> candidates, Vec3 from, Vec3 to) {
        Hit h = firstHitOf(candidates, from, to);
        return h == null ? null : h.entity();
    }

    public static EntityHitResult firstResult(Level level, Entity except, Vec3 from, Vec3 to,
                                              Predicate<Entity> filter) {
        Entity e = first(level, except, from, to, filter);
        if (e == null) {
            return null;
        }
        AABB b = e.getBoundingBox().inflate(INFLATE);

        return new EntityHitResult(e, b.clip(from, to).orElse(from));
    }

    public static LivingEntity firstLiving(Level level, Entity except, Vec3 from, Vec3 to,
                                           Predicate<Entity> filter) {
        Entity e = first(level, except, from, to,
                x -> x.isAlive() && x instanceof LivingEntity && filter.test(x));
        return (LivingEntity) e;
    }
}
