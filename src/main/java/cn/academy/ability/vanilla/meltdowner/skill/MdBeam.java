package cn.academy.ability.vanilla.meltdowner.skill;

import cn.academy.ability.AbilityContext;
import cn.academy.entity.EntitySilbarn;
import cn.academy.util.AimTrace;
import cn.academy.util.RayReflect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public final class MdBeam {

    private MdBeam() {}

    static final long EXTEND_MS = 200;

    private static final RayReflect.Schedule PENDING = new RayReflect.Schedule();

    public static final class Ticker {
        @SubscribeEvent
        public void onServerTick(TickEvent.ServerTickEvent evt) {
            if (evt.phase == TickEvent.Phase.END) {
                PENDING.tick();
            }
        }
    }

    private static final Ticker TICKER = new Ticker();
    private static boolean inited = false;

    public static void init() {
        if (inited) {
            return;
        }
        inited = true;
        MinecraftForge.EVENT_BUS.register(TICKER);
    }

    static final class Shot {

        final List<Vec3> path;

        final boolean bent;

        final int holdTicks;

        final boolean truncated;

        Shot(List<Vec3> path, boolean bent, int holdTicks, boolean truncated) {
            this.path = path;
            this.bent = bent;
            this.holdTicks = holdTicks;
            this.truncated = truncated;
        }

        Vec3 end() {
            return path.get(path.size() - 1);
        }
    }

    static Shot solve(AbilityContext ctx, Player player, Vec3 from, Vec3 dir,
                      double dist, double beamLength, float damage, boolean clearIFrames,
                      BiConsumer<List<Vec3>, Integer> onSecondary) {
        return solve(ctx, player, from, dir, dist, beamLength, damage, clearIFrames,
                player, null, onSecondary);
    }

    static Shot solve(AbilityContext ctx, Player player, Vec3 from, Vec3 dir,
                      double dist, double beamLength, float damage, boolean clearIFrames,
                      Entity immune, Entity firstExcept,
                      BiConsumer<List<Vec3>, Integer> onSecondary) {
        List<Vec3> path = new ArrayList<>();
        path.add(from);

        Vec3 cur = from;
        double remaining = dist;
        Entity except = firstExcept != null ? firstExcept : player;
        boolean bent = false;
        int hold = 0;
        boolean truncated = false;

        double traveled = 0;

        while (true) {
            Vec3 segEnd = blockEnd(player, cur, dir, remaining);

            EntitySilbarn mediumHit = MdBarrage.find(ctx, cur, dir, cur.distanceTo(segEnd));
            Vec3 mediumAt = mediumHit == null ? null : mediumHit.getBoundingBox().getCenter();
            if (mediumAt != null) {
                segEnd = mediumAt;
            }

            LivingEntity target = findTarget(player, cur, segEnd, except, immune);
            if (target == null) {
                if (mediumAt != null) {

                    hold = MdBarrage.burst(ctx, mediumHit, dir);

                    truncated = true;
                }
                path.add(segEnd);
                break;
            }

            final Vec3 fFrom = cur, fDir = dir;
            final LivingEntity fTarget = target;
            final double fTraveled = traveled;
            final boolean[] reflected = {false}, bendFlag = {false};
            final Vec3[] refDir = {null}, refAt = {null};
            final double[] hitDist = {remaining};
            final int[] delay = {0};

            if (clearIFrames) {
                target.invulnerableTime = -1;
            }

            MDDamageHelper.attackReflect(ctx, target, damage,
                    ev -> {

                        RayReflect.fill(ev, fFrom, fDir, fTarget,
                                RayReflect.DEFAULT_STANDOFF, EXTEND_MS, fTraveled);
                        ev.beamLength = beamLength;
                    },
                    ev -> {
                        reflected[0] = true;
                        hitDist[0] = ev.hitDist;
                        refDir[0] = ev.reflectDir;
                        refAt[0] = ev.hitPos;
                        delay[0] = ev.arriveDelay;
                        if (ev.bend && ev.reflectDir != null
                                && ev.reflectDir.lengthSqr() > 1.0e-6 && ev.hitPos != null) {
                            bendFlag[0] = true;
                        }
                    });

            if (!reflected[0] || !bendFlag[0]) {

                path.add(reflected[0] && refAt[0] != null ? refAt[0] : segEnd);
                truncated = reflected[0];

                if (reflected[0] && onSecondary != null && refAt[0] != null && refDir[0] != null
                        && refDir[0].lengthSqr() > 1.0e-6) {
                    double left = remaining - Math.min(Math.max(0, hitDist[0]), remaining);
                    if (left > RayReflect.MIN_BEND_STEP) {
                        final Vec3 secFrom = refAt[0], secDir = refDir[0].normalize();
                        final double secLeft = left;
                        final LivingEntity reflector = target;
                        PENDING.after(delay[0],
                                () -> !player.isRemoved() && !reflector.isRemoved(),
                                () -> {

                                    Shot sec = solve(ctx, player, secFrom, secDir, secLeft,
                                            beamLength, damage, clearIFrames, null, reflector, null);
                                    onSecondary.accept(sec.path, sec.holdTicks);
                                });
                    }
                }
                break;
            }

            bent = true;
            remaining -= Math.min(hitDist[0], remaining);
            Vec3 corner = refAt[0], dOut = refDir[0].normalize();

            double r = RayReflect.bendRadiusFor(hitDist[0], remaining);
            if (r > 0) {
                Vec3 arcA = corner.subtract(dir.scale(r));
                Vec3 arcB = corner.add(dOut.scale(r));
                List<Vec3> arcPts = RayReflect.bezierArc(arcA, corner, arcB, RayReflect.bendDivFor(dir, dOut));
                path.add(arcA);
                path.addAll(arcPts);

                remaining += r - RayReflect.polyLength(arcA, arcPts);
                cur = arcB;
            } else {
                path.add(corner);
                cur = corner;
            }

            traveled = RayReflect.polyLength(from, path);

            if (remaining <= RayReflect.MIN_BEND_STEP) {
                break;
            }
            dir = dOut;
            except = target;
        }

        return new Shot(path, bent, hold, truncated);
    }

    private static Vec3 blockEnd(Player player, Vec3 from, Vec3 dir, double dist) {
        Vec3 end = from.add(dir.scale(dist));
        BlockHitResult b = player.level().clip(new ClipContext(
                from, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        return b.getType() == HitResult.Type.BLOCK ? b.getLocation() : end;
    }

    private static LivingEntity findTarget(Player player, Vec3 from, Vec3 clipEnd,
                                           Entity except, Entity immune) {
        return AimTrace.firstLiving(player.level(), except, from, clipEnd,
                e -> e != except && e != immune);
    }

    @OnlyIn(Dist.CLIENT)
    static final class Fx {

        private Fx() {}

        static void spawnRay(Player player, byte[] raw, int hold, boolean sound) {
            List<Vec3> path = RayReflect.decodePath(raw);
            if (path == null) {
                return;
            }
            cn.academy.entity.EntityMdRaySmall ray =
                    new cn.academy.entity.EntityMdRaySmall(player.level());

            ray.viewOptimize = false;
            ray.setPath(path);
            if (hold > 0) {
                ray.life = Math.max(ray.life, hold);
            }
            cn.academy.client.render.entity.ACEffectEntities.spawn(ray);

            if (sound) {
                Vec3 at = path.get(0);
                player.level().playLocalSound(at.x, at.y, at.z,
                        cn.academy.ACSounds.MD_RAY_SMALL.get(),
                        net.minecraft.sounds.SoundSource.AMBIENT, 0.8f, 1.0f, false);
            }
        }
    }
}
