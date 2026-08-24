package cn.academy.ability.vanilla.electromaster;

import cn.academy.ability.AbilityPipeline;
import cn.academy.util.AimTrace;
import cn.academy.ACSounds;
import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientContext;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.RegClientContext;
import cn.academy.client.render.entity.ACEffectEntities;
import cn.academy.client.render.util.ArcPatterns;
import cn.academy.entity.EntityArc;
import cn.academy.util.RayReflect;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.util.RandUtils;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.LogicalSide;

import java.util.ArrayList;
import java.util.List;

import cn.academy.config.AbilityConfig;
import static cn.lambdalib2.util.MathUtils.lerp;
import static cn.lambdalib2.util.MathUtils.lerpf;

public class ThunderBolt extends Skill {

    public static final ThunderBolt INSTANCE = new ThunderBolt();

    public static final double RANGE = 20.0;
    public static final double AOE_RANGE = 8.0;

    public static final float REFLECT_DIFFICULTY = 0.5f;

    public ThunderBolt() {
        super("thunder_bolt", 4);
    }

    @Override
    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    public void activate(cn.academy.ability.context.ClientRuntime rt, int keyID) {
        activateSingleKey2(rt, keyID, BoltContext::new);
    }

    public static Object[] trace(Player player) {
        return trace(player, player.getEyePosition(1.0f), player.getViewVector(1.0f), player);
    }

    public static Object[] trace(Player player, Vec3 from, Vec3 dir, Entity except) {
        return trace(player, from, dir, except, RANGE);
    }

    public static Object[] trace(Player player, Vec3 from, Vec3 dir, Entity except, double maxDist) {
        Vec3 look = dir.normalize();
        Vec3 rayEnd = from.add(look.scale(maxDist));

        BlockHitResult block = player.level().clip(new ClipContext(
                from, rayEnd, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, except));
        Vec3 clipEnd = block.getType() == HitResult.Type.BLOCK ? block.getLocation() : rayEnd;

        EntityHitResult ent = AimTrace.firstResult(player.level(), except, from, clipEnd,
                e -> e != except && e.isAlive() && e instanceof LivingEntity
                        && AbilityPipeline.canTarget(player, e));

        if (ent != null) {
            LivingEntity target = (LivingEntity) ent.getEntity();
            Vec3 end = ent.getLocation().add(0, target.getEyeHeight(), 0);
            return new Object[]{end, target};
        }
        return new Object[]{clipEnd, null};
    }

    static List<LivingEntity> aoeTargets(Player player, Vec3 end, Entity exclude) {
        AABB box = new AABB(end.x - AOE_RANGE, end.y - AOE_RANGE, end.z - AOE_RANGE,
                end.x + AOE_RANGE, end.y + AOE_RANGE, end.z + AOE_RANGE);
        List<LivingEntity> out = new ArrayList<>();
        double r2 = AOE_RANGE * AOE_RANGE;
        for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e != exclude && e.isAlive()
                        && AbilityPipeline.canTarget(player, e))) {
            if (e.position().distanceToSqr(end) <= r2) out.add(e);
        }
        return out;
    }

    public static class BoltContext extends Context<ThunderBolt> {

        static final String MSG_PERFORM = "perform";
        static final String MSG_REFLECT = "reflect";

        static final String MSG_PERFORM_PATH = "perform_path";

        private final float exp = ctx.getSkillExp();
        private final float damage = AbilityConfig.stat("thunder_bolt", "damage", exp);
        private final float aoeDamage = AbilityConfig.stat("thunder_bolt", "aoe_damage", exp);

        public BoltContext(Player player) {
            super(player, INSTANCE);
        }

        private boolean consume() {
            float overload = AbilityConfig.overload("thunder_bolt", exp);
            int cp = (int) AbilityConfig.cp("thunder_bolt", exp);
            return ctx.consume(overload, cp);
        }

        @Listener(channel = MSG_KEYDOWN, side = LogicalSide.CLIENT)
        private void l_onKeyDown() {
            sendToServer(MSG_PERFORM);
        }

        @Listener(channel = MSG_PERFORM, side = LogicalSide.SERVER)
        private void s_perform() {
            if (consume()) {
                Vec3 eye = player.getEyePosition(1.0f);
                java.util.List<Vec3> path = new java.util.ArrayList<>();
                path.add(eye);

                Vec3 from = eye, dir = player.getViewVector(1.0f);
                double remaining = RANGE;
                Entity except = player;
                Vec3 end = null;
                LivingEntity aoeExclude = null;
                boolean effective = false;
                boolean bent = false;
                double mainLen = RANGE;

                while (true) {
                    Object[] tr = trace(player, from, dir, except, remaining);
                    Vec3 segEnd = (Vec3) tr[0];
                    LivingEntity target = (LivingEntity) tr[1];

                    if (target == null) {
                        path.add(segEnd);
                        end = segEnd;
                        break;
                    }

                    effective = true;
                    final Vec3 fFrom = from, fDir = dir;
                    final LivingEntity fTarget = target;
                    final boolean[] bendFlag = {false};
                    final Vec3[] refDir = {null}, refAt = {null};
                    final double[] hitDist = {segEnd.distanceTo(from)};

                    boolean reflected = EMDamageHelper.attackReflect(ctx, target, damage,
                            ev -> {
                                RayReflect.fill(ev, fFrom, fDir, fTarget, 0);
                                ev.difficulty = REFLECT_DIFFICULTY;
                                ev.beamLength = RANGE;
                            },
                            ev -> {
                                hitDist[0] = ev.hitDist;
                                refDir[0] = ev.reflectDir;
                                refAt[0] = ev.hitPos;
                                if (ev.bend && ev.reflectDir != null
                                        && ev.reflectDir.lengthSqr() > 1.0e-6 && ev.hitPos != null) {
                                    bendFlag[0] = true;
                                }
                            });

                    if (!reflected) {
                        maybeSlow(target, 40);
                        path.add(segEnd);
                        end = segEnd;
                        aoeExclude = target;
                        break;
                    }

                    if (!bendFlag[0]) {

                        mainLen = hitDist[0];
                        Vec3 at = refAt[0] != null ? refAt[0] : from.add(dir.scale(hitDist[0]));
                        end = reflectLegacy(at, refDir[0], target);
                        if (bent) {
                            path.add(at);
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
                        java.util.List<Vec3> arcPts = RayReflect.bezierArc(
                                arcA, corner, arcB, RayReflect.bendDivFor(dir, dOut));
                        double arcLen = RayReflect.polyLength(arcA, arcPts);
                        path.add(arcA);
                        path.addAll(arcPts);
                        remaining += r - arcLen;
                        from = arcB;
                    } else {
                        path.add(corner);
                        from = corner;
                    }
                    end = from;
                    aoeExclude = target;
                    if (remaining <= RayReflect.MIN_BEND_STEP) {
                        break;
                    }
                    dir = dOut;
                    except = target;
                }

                for (LivingEntity e : aoeTargets(player, end, aoeExclude)) {
                    effective = true;
                    EMDamageHelper.attack(ctx, e, aoeDamage);

                    maybeSlow(e, 20);
                }

                if (bent && path.size() >= 3) {
                    sendToClient(MSG_PERFORM_PATH, end.x, end.y, end.z, RayReflect.encodePath(path));
                } else {
                    sendToClient(MSG_PERFORM, end.x, end.y, end.z, mainLen);
                }
                ctx.addSkillExp(effective ? 0.005f : 0.003f);
                ctx.setCooldown((int) AbilityConfig.cooldown("thunder_bolt", exp));
            }
            terminate();
        }

        private Vec3 reflectLegacy(Vec3 at, Vec3 refDir, LivingEntity blocker) {
            Vec3 dir = refDir != null && refDir.lengthSqr() > 1.0e-6
                    ? refDir.normalize() : blocker.getLookAngle();

            Object[] tr = trace(player, at, dir, blocker);
            Vec3 end = (Vec3) tr[0];
            LivingEntity hit = (LivingEntity) tr[1];
            if (hit != null) {
                EMDamageHelper.attack(ctx, hit, damage);
                maybeSlow(hit, 40);
            }

            sendToClient(MSG_REFLECT, at.x, at.y, at.z, end.x, end.y, end.z);
            return end;
        }

        private void maybeSlow(LivingEntity e, int ticks) {
            if (exp > 0.2f && RandUtils.ranged(0, 1) < 0.8) {
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 3));
            }
        }
    }

    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    @RegClientContext(BoltContext.class)
    public static class BoltContextC extends ClientContext {

        public BoltContextC(BoltContext par) {
            super(par);
        }

        @Listener(channel = BoltContext.MSG_PERFORM_PATH, side = LogicalSide.CLIENT)
        private void c_spawnPathEffect(double px, double py, double pz, byte[] raw) {
            java.util.List<Vec3> path = cn.academy.util.RayReflect.decodePath(raw);
            if (path == null) {
                return;
            }
            for (int i = 0; i < 3; i++) {
                EntityArc arc = new EntityArc(player, ArcPatterns.strongArc);
                arc.setPath(path);
                arc.setLife(20);
                ACEffectEntities.spawn(arc);
            }
            spawnAoeArcs(px, py, pz);
            player.level().playLocalSound(player.getX(), player.getY(), player.getZ(),
                    ACSounds.EM_ARC_STRONG.get(), SoundSource.AMBIENT, 0.6f, 1.0f, false);
        }

        private void spawnAoeArcs(double px, double py, double pz) {
            Vec3 end = new Vec3(px, py, pz);
            for (LivingEntity e : aoeTargets(player, end, null)) {
                EntityArc aoeArc = new EntityArc(player, ArcPatterns.aoeArc);
                aoeArc.lengthFixed = false;

                Vec3 te = e.getEyePosition();
                aoeArc.setFromTo(px, py, pz, te.x, te.y, te.z);
                aoeArc.setLife(RandUtils.rangei(15, 25));
                ACEffectEntities.spawn(aoeArc);
            }
        }

        @Listener(channel = BoltContext.MSG_PERFORM, side = LogicalSide.CLIENT)
        private void c_spawnEffect(double px, double py, double pz, double mainLen) {

            for (int i = 0; i < 3; i++) {
                EntityArc mainArc = new EntityArc(player, ArcPatterns.strongArc);
                mainArc.length = mainLen;

                if (mainLen < RANGE - 1.0e-6) {
                    mainArc.lengthFixed = false;
                }
                mainArc.setLife(20);
                ACEffectEntities.spawn(mainArc);
            }
            spawnAoeArcs(px, py, pz);
            player.level().playLocalSound(player.getX(), player.getY(), player.getZ(),
                    ACSounds.EM_ARC_STRONG.get(), SoundSource.AMBIENT, 0.6f, 1.0f, false);
        }

        @Listener(channel = BoltContext.MSG_REFLECT, side = LogicalSide.CLIENT)
        private void c_spawnReflect(double px, double py, double pz,
                                    double ex, double ey, double ez) {
            for (int i = 0; i < 3; i++) {
                EntityArc arc = new EntityArc(player, ArcPatterns.strongArc);
                arc.lengthFixed = false;
                arc.viewOptimize = false;
                arc.setFromTo(px, py, pz, ex, ey, ez);
                arc.setLife(20);
                ACEffectEntities.spawn(arc);
            }
            player.level().playLocalSound(px, py, pz,
                    ACSounds.EM_ARC_STRONG.get(), SoundSource.AMBIENT, 0.6f, 1.0f, false);
        }
    }
}
