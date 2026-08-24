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
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.LogicalSide;

import cn.academy.config.AbilityConfig;
import static cn.lambdalib2.util.MathUtils.lerpf;

public class ElectricArc extends Skill {

    public static final ElectricArc INSTANCE = new ElectricArc();

    static final float REFLECT_DIFFICULTY = 0.1f;

    public ElectricArc() {

        super("arc_gen", 1);
    }

    @Override
    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    public void activate(cn.academy.ability.context.ClientRuntime rt, int keyID) {
        activateSingleKey2(rt, keyID, ArcContext::new);
    }

    public static class ArcContext extends Context<ElectricArc> {

        static final String MSG_PERFORM = "perform";

        public ArcContext(Player player) {
            super(player, INSTANCE);
        }

        private final float damage = AbilityConfig.stat("electric_arc", "damage", ctx.getSkillExp());
        private final float range = lerpf(8, 16, ctx.getSkillExp());
        private final float cpCost = AbilityConfig.cp("electric_arc", ctx.getSkillExp());

        private boolean consume() {
            float overload = AbilityConfig.overload("electric_arc", ctx.getSkillExp());
            return ctx.consume(overload, cpCost);
        }

        @Listener(channel = MSG_KEYDOWN, side = LogicalSide.CLIENT)
        private void c_keyDown() {
            sendToServer(MSG_PERFORM);
        }

        @Listener(channel = MSG_PERFORM, side = LogicalSide.SERVER)
        private void s_perform() {
            if (consume()) {
                Vec3 eye = player.getEyePosition(1.0f);
                java.util.List<Vec3> path = new java.util.ArrayList<>();
                path.add(eye);

                Vec3 from = eye, dir = player.getViewVector(1.0f);
                double remaining = range;
                Entity except = player;
                boolean bent = false;
                float beamLen = range;
                boolean hitAny = false;

                while (true) {
                    Entity target = raytrace(from, dir, remaining, except);
                    if (target == null) {
                        path.add(from.add(dir.scale(remaining)));
                        break;
                    }
                    hitAny = true;

                    final Vec3 fFrom = from, fDir = dir;
                    final Entity fTarget = target;
                    final boolean[] reflected = {false}, bendFlag = {false};
                    final Vec3[] refDir = {null}, refAt = {null};
                    final double[] hitDist = {remaining};

                    ctx.attackReflect(target, damage,
                            ev -> {
                                RayReflect.fill(ev, fFrom, fDir, fTarget, 0);
                                ev.difficulty = REFLECT_DIFFICULTY;
                                ev.beamLength = range;
                            },
                            ev -> {
                                reflected[0] = true;
                                hitDist[0] = ev.hitDist;
                                refDir[0] = ev.reflectDir;
                                refAt[0] = ev.hitPos;
                                if (ev.bend && ev.reflectDir != null
                                        && ev.reflectDir.lengthSqr() > 1.0e-6 && ev.hitPos != null) {
                                    bendFlag[0] = true;
                                }
                            });

                    if (!reflected[0]) {

                        path.add(from.add(dir.scale(remaining)));
                        break;
                    }

                    if (!bendFlag[0]) {
                        beamLen = (float) hitDist[0];
                        Vec3 at = refAt[0] != null ? refAt[0] : from.add(dir.scale(hitDist[0]));
                        reflectLegacy(at, refDir[0], fTarget);
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
                        path.add(arcA);
                        path.addAll(arcPts);
                        remaining += r - RayReflect.polyLength(arcA, arcPts);
                        from = arcB;
                    } else {
                        path.add(corner);
                        from = corner;
                    }
                    if (remaining <= RayReflect.MIN_BEND_STEP) {
                        break;
                    }
                    dir = dOut;
                    except = target;
                }

                ctx.addSkillExp(hitAny ? 0.006f : 0.002f);

                if (bent && path.size() >= 3) {
                    sendToClient(MSG_EFFECT_PATH, RayReflect.encodePath(path));
                } else {
                    sendToClient(MSG_EFFECT, beamLen);
                }
                ctx.setCooldown((int) AbilityConfig.cooldown("electric_arc", ctx.getSkillExp()));
            }
            terminate();
        }

        private void reflectLegacy(Vec3 at, Vec3 refDir, Entity blocker) {
            Vec3 dir = refDir != null && refDir.lengthSqr() > 1.0e-6
                    ? refDir.normalize() : blocker.getLookAngle();

            LivingEntity hit = RayReflect.traceLiving(player.level(), blocker, at, dir, range);
            if (hit != null) {
                ctx.attack(hit, damage);
            }

            sendToClient(MSG_REFLECT, at.x, at.y, at.z, dir.x, dir.y, dir.z, (double) range);
        }

        static final String MSG_EFFECT = "effect";
        static final String MSG_REFLECT = "reflect";

        static final String MSG_EFFECT_PATH = "effect_path";

        private Entity raytrace(Vec3 from, Vec3 dir, double dist, Entity except) {
            Vec3 end = from.add(dir.scale(dist));
            EntityHitResult res = AimTrace.firstResult(player.level(), except, from, end,
                    e -> e != except && e.isAlive() && e instanceof LivingEntity
                            && AbilityPipeline.canTarget(player, e));
            return res == null ? null : res.getEntity();
        }

    }

    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    @RegClientContext(ArcContext.class)
    public static class ArcContextC extends ClientContext {

        public ArcContextC(ArcContext par) {
            super(par);
        }

        @Listener(channel = ArcContext.MSG_EFFECT, side = LogicalSide.CLIENT)
        private void c_spawnEffects(float range) {
            EntityArc arc = new EntityArc(player, ArcPatterns.weakArc);
            arc.texWiggle = 0.7;
            arc.showWiggle = 0.1;
            arc.hideWiggle = 0.4;
            arc.setLife(10);
            arc.lengthFixed = false;
            arc.length = range;

            ACEffectEntities.spawn(arc);

            player.level().playLocalSound(player.getX(), player.getY(), player.getZ(),
                    ACSounds.EM_ARC_WEAK.get(), SoundSource.AMBIENT, 0.5f, 1.0f, false);
        }

        @Listener(channel = ArcContext.MSG_EFFECT_PATH, side = LogicalSide.CLIENT)
        private void c_spawnPathEffect(byte[] raw) {
            java.util.List<Vec3> path = cn.academy.util.RayReflect.decodePath(raw);
            if (path == null) {
                return;
            }
            EntityArc arc = new EntityArc(player, ArcPatterns.weakArc);
            arc.texWiggle = 0.7;
            arc.showWiggle = 0.1;
            arc.hideWiggle = 0.4;
            arc.setLife(10);
            arc.setPath(path);

            ACEffectEntities.spawn(arc);

            player.level().playLocalSound(player.getX(), player.getY(), player.getZ(),
                    ACSounds.EM_ARC_WEAK.get(), SoundSource.AMBIENT, 0.5f, 1.0f, false);
        }

        @Listener(channel = ArcContext.MSG_REFLECT, side = LogicalSide.CLIENT)
        private void c_spawnReflect(double px, double py, double pz,
                                    double dx, double dy, double dz, double len) {
            EntityArc arc = new EntityArc(player, ArcPatterns.weakArc);
            arc.texWiggle = 0.7;
            arc.showWiggle = 0.1;
            arc.hideWiggle = 0.4;
            arc.setLife(10);
            arc.lengthFixed = false;
            arc.viewOptimize = false;
            arc.setFromTo(px, py, pz, px + dx * len, py + dy * len, pz + dz * len);

            ACEffectEntities.spawn(arc);

            player.level().playLocalSound(px, py, pz,
                    ACSounds.EM_ARC_WEAK.get(), SoundSource.AMBIENT, 0.5f, 1.0f, false);
        }
    }
}
