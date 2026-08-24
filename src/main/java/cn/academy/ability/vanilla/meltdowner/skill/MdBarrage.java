package cn.academy.ability.vanilla.meltdowner.skill;

import cn.academy.ability.AbilityContext;
import cn.academy.ability.AbilityPipeline;
import cn.academy.ability.vanilla.meltdowner.passiveskill.RayBarrage;
import cn.academy.config.AbilityConfig;
import cn.academy.entity.EntityMdRayBarrage;
import cn.academy.entity.EntitySilbarn;
import cn.academy.util.AimTrace;
import cn.academy.util.RayBeam;
import cn.academy.util.RayReflect;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.util.RandUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class MdBarrage {

    private MdBarrage() {}

    private static final float EXP_PER_BURST = 0.005f;

    public static final float SPREAD_FROM = 50, SPREAD_TO = 60;

    public static final int COUNT_FROM = 25, COUNT_TO = 30;

    private static float hash01(int seed, int pass, int idx, int salt) {
        int h = seed * 0x9E3779B1 + pass * 0x85EBCA6B + idx * 0xC2B2AE35 + salt * 0x27D4EB2F;
        h ^= h >>> 15;
        h *= 0x2C1B3C6D;
        h ^= h >>> 12;
        h ^= h >>> 16;
        return (h >>> 8) / (float) (1 << 24);
    }

    public static float spreadFor(int seed, int pass) {
        return SPREAD_FROM + hash01(seed, pass, 0, 1) * (SPREAD_TO - SPREAD_FROM);
    }

    public static float yawOffFor(int seed, int pass, int i, float spread) {
        return (hash01(seed, pass, i, 2) * 2 - 1) * spread;
    }

    public static float pitchOffFor(int seed, int pass, int i, float spread) {
        return (hash01(seed, pass, i, 3) * 2 - 1) * spread / 2;
    }

    public static Vec3[] basisOf(Vec3 axis) {
        Vec3 ref = Math.abs(axis.y) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 right = axis.cross(ref).normalize();
        return new Vec3[]{right, right.cross(axis).normalize()};
    }

    public static Vec3 subRayDir(Vec3 axis, Vec3 right, Vec3 up, float yawOff, float pitchOff) {
        return axis.add(right.scale(Math.tan(Math.toRadians(yawOff))))
                .add(up.scale(Math.tan(Math.toRadians(pitchOff))))
                .normalize();
    }

    public record RayHit(double reach, Entity target) {}

    public static RayHit traceRay(Level level, Entity except, Vec3 from, Vec3 dir,
                                  double maxLen, Iterable<? extends Entity> candidates) {
        Vec3 d = dir.normalize();
        Vec3 end = from.add(d.scale(maxLen));

        BlockHitResult b = level.clip(new ClipContext(
                from, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, except));
        Vec3 segEnd = b.getType() == HitResult.Type.BLOCK ? b.getLocation() : end;

        AimTrace.Hit hit = AimTrace.firstHitOf(candidates, from, segEnd);
        return hit == null
                ? new RayHit(from.distanceTo(segEnd), null)
                : new RayHit(from.distanceTo(hit.pos()), hit.entity());
    }

    public static java.util.List<Entity> candidatesAround(Level level, Entity except, Vec3 at, double range) {
        return level.getEntities(except, new AABB(at, at).inflate(range + AimTrace.INFLATE),
                x -> x != except && x.isAlive() && x instanceof LivingEntity

                        && (!(except instanceof net.minecraft.world.entity.player.Player ep)
                                || AbilityPipeline.canTarget(ep, x)));
    }

    private static final float REFLECT_DIFFICULTY = 0.1f;

    private static final long EXTEND_MS = 0;

    public static final RayBeam.Medium HOOK = new RayBeam.Medium() {
        @Override
        public Entity find(AbilityContext ctx, Vec3 from, Vec3 dir, double dist) {
            return MdBarrage.find(ctx, from, dir, dist);
        }

        @Override
        public int burst(AbilityContext ctx, Entity medium, Vec3 dir) {
            return MdBarrage.burst(ctx, medium, dir);
        }
    };

    public static EntitySilbarn find(AbilityContext ctx, Vec3 from, Vec3 dir, double dist) {
        Player player = ctx.player;
        if (player.level().isClientSide || !RayBarrage.isLearned(player)) {
            return null;
        }
        return find(player.level(), from, from.add(dir.normalize().scale(dist)));
    }

    public static EntitySilbarn find(Level level, Vec3 from, Vec3 to) {

        double radius = AbilityConfig.stat("ray_barrage", "hit_radius", 0);
        Vec3 seg = to.subtract(from);
        double segLen = seg.length();
        if (segLen < 1.0e-6) {
            return null;
        }
        Vec3 axis = seg.scale(1 / segLen);

        AABB box = new AABB(from, to).inflate(radius + 0.5);
        EntitySilbarn best = null;
        double bestAlong = Double.MAX_VALUE;

        for (Entity e : level.getEntities((Entity) null, box,
                x -> x instanceof EntitySilbarn s && s.isIntact())) {
            Vec3 c = e.getBoundingBox().getCenter();
            Vec3 v = c.subtract(from);
            double along = v.dot(axis);
            if (along < 0 || along > segLen) {
                continue;
            }
            if (c.distanceTo(from.add(axis.scale(along))) > radius) {
                continue;
            }
            if (along < bestAlong) {
                bestAlong = along;
                best = (EntitySilbarn) e;
            }
        }
        return best;
    }

    public static java.util.List<EntitySilbarn> nearby(Player player, double radius) {
        if (player.level().isClientSide || !RayBarrage.isLearned(player)) {
            return java.util.List.of();
        }
        Vec3 eye = player.getEyePosition(1.0f);
        java.util.List<EntitySilbarn> out = new java.util.ArrayList<>();
        for (Entity e : player.level().getEntities(player,
                new AABB(eye, eye).inflate(radius),
                x -> x instanceof EntitySilbarn s && s.isIntact())) {
            out.add((EntitySilbarn) e);
        }
        out.sort(java.util.Comparator.comparingDouble(
                s -> s.getBoundingBox().getCenter().distanceToSqr(eye)));
        return out;
    }

    public static Vec3 tryScatter(AbilityContext ctx, Vec3 from, Vec3 dir, double dist) {
        EntitySilbarn medium = find(ctx, from, dir, dist);
        if (medium == null) {
            return null;
        }
        Vec3 at = medium.getBoundingBox().getCenter();
        burst(ctx, medium, dir);
        return at;
    }

    public static int burst(AbilityContext ctx, Entity medium, Vec3 dir) {

        EntitySilbarn s = medium instanceof EntitySilbarn e ? e : null;
        scatter(AbilityContext.of(ctx.player, RayBarrage.INSTANCE),
                medium.getBoundingBox().getCenter(), dir, s);
        return holdTicks();
    }

    private static final class Active {
        final Player player;

        final Level level;
        final Vec3 at, axis, right, up;
        final float damage;

        final int seed;

        final int count;

        final EntitySilbarn medium;

        final int totalPasses;

        int passIndex = 0;
        final double range;
        int ticksLeft;
        int untilNext;

        Active(Player player, Vec3 at, Vec3 axis, float damage, int seed, int count,
               double range, int ticksLeft, EntitySilbarn medium, int totalPasses) {
            this.player = player;
            this.level = player.level();
            this.at = at;
            this.axis = axis.normalize();
            Vec3[] basis = basisOf(this.axis);
            this.right = basis[0];
            this.up = basis[1];
            this.damage = damage;
            this.seed = seed;
            this.count = count;
            this.range = range;
            this.ticksLeft = ticksLeft;
            this.medium = medium;
            this.totalPasses = Math.max(1, totalPasses);
        }

        void finish() {
            if (medium != null && !medium.isRemoved()) {
                medium.shatter(true);
            }
        }
    }

    private static final java.util.List<Active> ACTIVE = new java.util.ArrayList<>();

    private static final java.util.List<Active> PENDING = new java.util.ArrayList<>();

    public static void serverTick() {
        if (!PENDING.isEmpty()) {
            ACTIVE.addAll(PENDING);
            PENDING.clear();
        }
        if (ACTIVE.isEmpty()) {
            return;
        }
        java.util.Iterator<Active> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Active a = it.next();
            if (a.player.isRemoved() || --a.ticksLeft <= 0) {
                a.finish();
                it.remove();
                continue;
            }
            if (--a.untilNext > 0) {
                continue;
            }
            a.untilNext = flickerInterval();
            damagePass(a);
        }
    }

    public static int holdTicks() {
        return Math.max(1, (int) AbilityConfig.stat("ray_barrage", "flicker_ticks", 0));
    }

    private static int flickerInterval() {
        return Math.max(1, (int) AbilityConfig.stat("ray_barrage", "flicker_interval", 0));
    }

    private static void scatter(AbilityContext ctx, Vec3 at, Vec3 dir, EntitySilbarn medium) {
        Player player = ctx.player;
        float exp = ctx.getSkillExp();
        float damage = AbilityConfig.stat("ray_barrage", "ray_damage", exp);
        double range = AbilityConfig.stat("ray_barrage", "range", exp);
        int interval = flickerInterval();
        int ticks = Math.max(1, (int) AbilityConfig.stat("ray_barrage", "flicker_ticks", 0));

        int seed = RandUtils.RNG.nextInt();
        int count = RandUtils.rangei(COUNT_FROM, COUNT_TO);

        int totalPasses = Math.max(1, (ticks + interval - 1) / interval);
        Active a = new Active(player, at, dir.normalize(), damage, seed, count, range, ticks,
                medium, totalPasses);
        a.untilNext = interval;
        PENDING.add(a);
        damagePass(a);

        NetworkMessage.sendToTracking(player, RayBarrage.INSTANCE, RayBarrage.MSG_BURST, player,
                (float) at.x, (float) at.y, (float) at.z,
                (float) a.axis.x, (float) a.axis.y, (float) a.axis.z,
                (float) range, seed, count, interval, ticks);

        ctx.addSkillExp(EXP_PER_BURST);
    }

    private static void damagePass(Active a) {
        Player player = a.player;

        AbilityContext ctx = AbilityContext.ofIfReady(player, RayBarrage.INSTANCE);
        if (ctx == null) {
            return;
        }

        int pass = a.passIndex++;
        float spread = spreadFor(a.seed, pass);

        boolean canTrace = player.level() == a.level;

        if (a.medium != null && !a.medium.isRemoved()) {
            a.medium.setCrack(100 * (pass + 1) / a.totalPasses);
        }

        java.util.List<Entity> candidates = candidatesAround(a.level, player, a.at, a.range);
        if (candidates.isEmpty()) {
            return;
        }

        for (int i = 0; i < a.count; i++) {
            Vec3 dir = subRayDir(a.axis, a.right, a.up,
                    yawOffFor(a.seed, pass, i, spread),
                    pitchOffFor(a.seed, pass, i, spread));

            Entity hit = traceRay(a.level, player, a.at, dir, a.range, candidates).target();
            if (hit == null) {
                continue;
            }

            hit.invulnerableTime = -1;
            hitOne(ctx, a, hit, dir, canTrace);
        }
    }

    private static void hitOne(AbilityContext ctx, Active a, Entity target, Vec3 rayDir, boolean canTrace) {

        final Vec3 dir = rayDir.normalize();

        final boolean[] blocked = {false};
        final boolean[] bend = {false};
        final Vec3[] refDir = {null}, refAt = {null};
        final double[] hitDist = {0};

        MDDamageHelper.attackReflect(ctx, target, a.damage,
                ev -> {
                    RayReflect.fill(ev, a.at, dir, target, EXTEND_MS);
                    ev.beamLength = a.range;
                    ev.difficulty = REFLECT_DIFFICULTY;
                },
                ev -> {
                    blocked[0] = true;
                    bend[0] = ev.bend;
                    hitDist[0] = ev.hitDist;
                    refDir[0] = ev.reflectDir;
                    refAt[0] = ev.hitPos;
                });

        if (!blocked[0]) {
            return;
        }

        if (!canTrace || refDir[0] == null || refAt[0] == null || refDir[0].lengthSqr() < 1.0e-6) {
            return;
        }

        double remaining = a.range - Math.max(0, hitDist[0]);
        if (remaining <= RayReflect.MIN_BEND_STEP) {
            return;
        }

        Vec3 dOut = refDir[0].normalize();
        MdBeam.Shot shot = MdBeam.solve(ctx, a.player, refAt[0], dOut, remaining, a.range,
                a.damage, true, bend[0] ? a.player : null, target,
                bend[0] ? (secPath, secHold) -> sendDeflected(a.player, secPath) : null);
        if (shot.path.size() < 2) {
            return;
        }

        java.util.List<Vec3> path;
        if (bend[0]) {

            path = new java.util.ArrayList<>();
            path.add(a.at);

            double firstSeg = shot.path.get(1).distanceTo(shot.path.get(0));
            double r = RayReflect.bendRadiusFor(Math.max(0, hitDist[0]), firstSeg);
            if (r > 0) {
                Vec3 corner = refAt[0];
                Vec3 arcA = corner.subtract(dir.scale(r));
                Vec3 arcB = corner.add(dOut.scale(r));
                path.add(arcA);
                path.addAll(RayReflect.bezierArc(arcA, corner, arcB, RayReflect.bendDivFor(dir, dOut)));

                path.addAll(shot.path.subList(1, shot.path.size()));
            } else {
                path.addAll(shot.path);
            }
        } else {

            path = shot.path;
        }

        sendDeflected(a.player, path);

    }

    private static void sendDeflected(Player player, java.util.List<Vec3> path) {
        NetworkMessage.sendToTracking(player, RayBarrage.INSTANCE, RayBarrage.MSG_DEFLECTED,
                player, RayReflect.encodePath(path), 0);
    }
}
