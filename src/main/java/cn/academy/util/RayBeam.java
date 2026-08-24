package cn.academy.util;

import cn.academy.ability.AbilityContext;
import cn.academy.ability.Skill;
import cn.academy.event.ability.ReflectEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public final class RayBeam {

    private static final int HIT_CD = 5;

    private static final double MOVE_EPS_SQ = 0.05 * 0.05;

    private static final float TURN_EPS = 1.0f;

    public record Shape(double length, List<Vec3> path, int holdTicks) {
        public Shape(double length, List<Vec3> path) {
            this(length, path, 0);
        }

        public boolean isStraight() {
            return path == null || path.size() < 3;
        }
    }

    @FunctionalInterface
    public interface ReflectHandler {

        void handle(Player caster, Entity reflector, Vec3 reflectDir, Vec3 hitPoint);
    }

    public interface Medium {

        Entity find(AbilityContext ctx, Vec3 from, Vec3 dir, double dist);

        int burst(AbilityContext ctx, Entity medium, Vec3 dir);
    }

    private final Skill skill;
    private final int damageTicks;
    private final long extendMs;
    private final ReflectHandler onTruncateReflect;

    private final Medium medium;

    private final List<BeamHit> activeBeams = new ArrayList<>();

    private final RayReflect.Schedule pendingReflects = new RayReflect.Schedule();

    public RayBeam(Skill skill, int damageTicks, long extendMs, ReflectHandler onTruncateReflect) {
        this(skill, damageTicks, extendMs, onTruncateReflect, null);
    }

    public RayBeam(Skill skill, int damageTicks, long extendMs,
                   ReflectHandler onTruncateReflect, Medium medium) {
        this.skill = skill;
        this.damageTicks = damageTicks;
        this.extendMs = extendMs;
        this.onTruncateReflect = onTruncateReflect;
        this.medium = medium;
    }

    private static final class BeamHit {
        final Player player;
        final Vec3 pos, dir;

        final Vec3 eye;
        final float dmg;

        final double range;

        final Map<UUID, HitState> states;

        final Set<UUID> blockers;

        Predicate<Entity> selector = e -> true;

        double scanDepth = -1;

        double minAlong = Double.NEGATIVE_INFINITY;
        int ticksLeft;

        BeamHit(Player player, Vec3 pos, Vec3 dir, Vec3 eye, float dmg, double range, int ticksLeft,
                Map<UUID, HitState> states, Set<UUID> blockers) {
            this.player = player;
            this.pos = pos;
            this.dir = dir;
            this.eye = eye;
            this.dmg = dmg;
            this.range = range;
            this.ticksLeft = ticksLeft;
            this.states = states;
            this.blockers = blockers;
        }
    }

    private static final class HitState {
        long tick;
        Vec3 pos;
        float yaw, pitch;
    }

    private static boolean shouldHitEntity(BeamHit b, Entity e, long now) {
        HitState st = b.states.get(e.getUUID());
        if (st == null) return true;
        if (now - st.tick < HIT_CD) return false;
        boolean moved = e.position().distanceToSqr(st.pos) > MOVE_EPS_SQ;
        boolean turned = angleDiff(e.getYRot(), st.yaw) > TURN_EPS
                || angleDiff(e.getXRot(), st.pitch) > TURN_EPS;
        return moved || turned;
    }

    private static void recordHit(BeamHit b, Entity e, long now) {
        HitState st = b.states.computeIfAbsent(e.getUUID(), k -> new HitState());
        st.tick = now;
        st.pos = e.position();
        st.yaw = e.getYRot();
        st.pitch = e.getXRot();
    }

    private static float angleDiff(float a, float c) {
        float d = Math.abs(a - c) % 360f;
        return d > 180f ? 360f - d : d;
    }

    public void serverTick() {

        pendingReflects.tick();

        if (activeBeams.isEmpty()) {
            return;
        }
        Iterator<BeamHit> it = activeBeams.iterator();
        while (it.hasNext()) {
            BeamHit b = it.next();
            if (b.player.isRemoved() || --b.ticksLeft <= 0) {
                it.remove();
                continue;
            }

            AbilityContext ctx = AbilityContext.ofIfReady(b.player, skill);
            if (ctx == null) {
                continue;
            }
            long now = b.player.level().getGameTime();

            RangedRayDamage.Reflectible dmg = new RangedRayDamage.Reflectible(ctx, b.range, 0,
                    (reflector, ev) -> recordHit(b, reflector, now));
            dmg.beamOrigin = b.eye;
            dmg.extendMs = extendMs;
            dmg.pos = b.pos;
            dmg.dir = b.dir;
            dmg.carveBlocks = false;
            dmg.startDamage = b.dmg;
            dmg.entitySelector = b.selector;

            dmg.scanDepth = b.scanDepth;
            dmg.minAlong = b.minAlong;

            dmg.shouldHit = e -> shouldHitEntity(b, e, now);

            dmg.blocksBeam = e -> b.blockers.contains(e.getUUID());
            dmg.onHit = e -> recordHit(b, e, now);
            dmg.perform();
        }
    }

    public Shape fire(Player caster, float dmg, float energy, double maxLen, double range,
                      Predicate<Entity> selector,
                      BiConsumer<Entity, ReflectEvent> onReflected) {
        AbilityContext ctx = AbilityContext.of(caster, skill);
        final Player player = caster;
        final long now = player.level().getGameTime();

        List<Vec3> path = new ArrayList<>();
        Vec3 eye = player.getEyePosition(1.0f);
        path.add(eye);

        Vec3 segPos = player.position();
        Vec3 segEye = eye;
        Vec3 segDir = player.getLookAngle();

        final Map<UUID, HitState> shared = new HashMap<>();

        final Set<UUID> blockers = new HashSet<>();

        double remaining = maxLen;
        float energyLeft = energy;
        int hold = 0;
        Entity lastBender = null;
        boolean firstSeg = true;

        while (true) {

            final Entity bender = lastBender;
            final Predicate<Entity> segSelector =
                    bender == null ? selector : selector.and(e -> e != bender);

            Entity mediumHit = null;
            double mediumDist = -1;
            if (medium != null) {
                mediumHit = medium.find(ctx, segEye, segDir, remaining);
                if (mediumHit != null) {
                    mediumDist = mediumHit.getBoundingBox().getCenter().subtract(segEye).length();
                }
            }
            final double scanDist = mediumDist >= 0 ? Math.min(remaining, mediumDist) : remaining;

            final BeamHit beam = new BeamHit(player, segPos, segDir, segEye, dmg, range,
                    damageTicks, shared, blockers);
            beam.selector = segSelector;
            beam.scanDepth = scanDist;
            beam.minAlong = firstSeg ? Double.NEGATIVE_INFINITY : 0;

            final Entity[] bentBy = {null};
            final Vec3[] bentAt = {null}, bentDir = {null};
            final double segRemaining = scanDist;
            final double[] cut = {segRemaining};

            RangedRayDamage.Reflectible damage = new RangedRayDamage.Reflectible(ctx, range, energyLeft,
                    (reflector, event) -> {

                        recordHit(beam, reflector, now);

                        blockers.add(reflector.getUUID());

                        if (event.bend && event.reflectDir != null && event.hitPos != null
                                && event.reflectDir.lengthSqr() > 1.0e-6) {

                            bentBy[0] = reflector;
                            bentAt[0] = event.hitPos;
                            bentDir[0] = event.reflectDir.normalize();
                            cut[0] = Math.min(segRemaining, event.hitDist);
                        } else {

                            cut[0] = Math.min(cut[0], event.hitDist);
                            Vec3 rDir = event.reflectDir;
                            Vec3 rHitPos = event.hitPos;
                            pendingReflects.after(event.arriveDelay,
                                    () -> !player.isRemoved() && !reflector.isRemoved(),
                                    () -> onTruncateReflect.handle(player, reflector, rDir, rHitPos));
                        }

                        if (onReflected != null) {
                            onReflected.accept(reflector, event);
                        }
                    });
            damage.beamOrigin = segEye;
            damage.extendMs = extendMs;
            damage.beamLength = maxLen;
            damage.pos = segPos;
            damage.dir = segDir;
            damage.scanDepth = scanDist;
            damage.minAlong = beam.minAlong;
            damage.startDamage = dmg;
            damage.entitySelector = segSelector;
            damage.shouldHit = e -> shouldHitEntity(beam, e, now);
            damage.blocksBeam = e -> blockers.contains(e.getUUID());
            damage.onHit = e -> recordHit(beam, e, now);
            damage.perform();

            energyLeft = damage.energyLeft;
            activeBeams.add(beam);

            double walked = Math.max(0, Math.min(cut[0], scanDist));

            if (bentBy[0] == null) {

                if (mediumHit != null && walked >= mediumDist - 1.0e-6) {
                    hold = medium.burst(ctx, mediumHit, segDir);
                    path.add(mediumHit.getBoundingBox().getCenter());
                } else {
                    path.add(segEye.add(segDir.scale(walked)));
                }
                break;
            }

            Vec3 corner = bentAt[0];
            Vec3 dIn = segDir, dOut = bentDir[0];
            remaining -= walked;

            beam.scanDepth = walked;

            double r = RayReflect.bendRadiusFor(walked, remaining);
            if (r > 0) {
                Vec3 arcA = corner.subtract(dIn.scale(r));
                Vec3 arcB = corner.add(dOut.scale(r));
                List<Vec3> arc = RayReflect.bezierArc(arcA, corner, arcB, RayReflect.bendDivFor(dIn, dOut));
                double arcLen = RayReflect.polyLength(arcA, arc);

                path.add(arcA);
                path.addAll(arc);

                Vec3 chord = arcB.subtract(arcA);
                if (chord.lengthSqr() > 1.0e-8) {

                    final Entity justBent = bentBy[0];
                    scanCorner(ctx, player, arcA, chord.normalize(), chord.length(), dmg, range,
                            segSelector.and(e -> e != justBent), shared, blockers, now);
                }

                remaining += r - arcLen;
                segEye = arcB;
                segPos = arcB;
            } else {
                path.add(corner);
                segEye = corner;
                segPos = corner;
            }

            if (remaining <= RayReflect.MIN_BEND_STEP) {
                break;
            }
            segDir = dOut;
            lastBender = bentBy[0];
            firstSeg = false;
        }

        double total = 0;
        for (int i = 1; i < path.size(); i++) {
            total += path.get(i).distanceTo(path.get(i - 1));
        }
        return new Shape(total, path.size() >= 3 ? path : null, hold);
    }

    private void scanCorner(AbilityContext ctx, Player player, Vec3 from, Vec3 dir, double len,
                            float dmg, double range, Predicate<Entity> selector,
                            Map<UUID, HitState> shared, Set<UUID> blockers, long now) {
        BeamHit beam = new BeamHit(player, from, dir, from, dmg, range, damageTicks, shared, blockers);
        beam.selector = selector;
        beam.scanDepth = len;
        beam.minAlong = 0;

        RangedRayDamage.Reflectible d = new RangedRayDamage.Reflectible(ctx, range, 0,
                (reflector, ev) -> {
                    recordHit(beam, reflector, now);
                    blockers.add(reflector.getUUID());
                });
        d.beamOrigin = from;
        d.extendMs = extendMs;
        d.pos = from;
        d.dir = dir;
        d.scanDepth = len;
        d.minAlong = 0;
        d.startDamage = dmg;
        d.entitySelector = selector;
        d.shouldHit = e -> shouldHitEntity(beam, e, now);
        d.blocksBeam = e -> blockers.contains(e.getUUID());
        d.onHit = e -> recordHit(beam, e, now);
        d.perform();

        activeBeams.add(beam);
    }
}
