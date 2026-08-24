package cn.academy.ability.vanilla.meltdowner.skill;

import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientContext;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.RegClientContext;
import cn.academy.client.render.entity.ACEffectEntities;
import cn.academy.config.AbilityConfig;
import cn.academy.entity.EntityMdBall;
import cn.academy.entity.EntityMdRaySmall;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.util.RandUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.LogicalSide;

public class ScatterBomb extends Skill {

    public static final ScatterBomb INSTANCE = new ScatterBomb();

    private ScatterBomb() {
        super("scatter_bomb", 2);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void activate(ClientRuntime rt, int keyID) {
        activateSingleKey2(rt, keyID, Ctx::new);
    }

    public static final double RANGE = 15;

    private static final double BALL_FROM = EntityMdBall.RANGE_FROM, BALL_TO = EntityMdBall.RANGE_TO;

    private static final String MSG_REFLECT = "sb_reflect";

    private static void sendReflect(Player player, java.util.List<Vec3> path, int hold) {
        cn.lambdalib2.s11n.network.NetworkMessage.sendToTracking(player, INSTANCE, MSG_REFLECT,
                player, cn.academy.util.RayReflect.encodePath(path), hold);
    }

    @OnlyIn(Dist.CLIENT)
    @Listener(channel = MSG_REFLECT, side = LogicalSide.CLIENT)
    private void hReflectClient(Player player, byte[] raw, Integer hold) {
        MdBeam.Fx.spawnRay(player, raw, hold, false);
    }

    public static class Ctx extends Context<ScatterBomb> {

        static final String MSG_BALL = "ball";
        static final String MSG_RELEASE = "release";
        static final String MSG_FIRE = "fire";
        static final String MSG_RAY = "ray";
        static final String MSG_RAY_PATH = "ray_path";

        private final float exp = ctx.getSkillExp();

        private final float cpPer = AbilityConfig.cp("scatter_bomb", exp);
        private final float overload = AbilityConfig.overload("scatter_bomb", exp);
        private final int cooldown = (int) AbilityConfig.cooldown("scatter_bomb", exp);
        private final float damage = AbilityConfig.stat("scatter_bomb", "damage", exp);
        private final float cpStep = AbilityConfig.stat("scatter_bomb", "cp_step", exp);
        private final int ballDelay = (int) AbilityConfig.stat("scatter_bomb", "ball_delay", exp);

        private final int ballInterval =
                Math.max(1, (int) AbilityConfig.stat("scatter_bomb", "ball_interval", exp));
        private final int chargeMax = (int) AbilityConfig.stat("scatter_bomb", "charge_max", exp);
        private final double spreadDeg = AbilityConfig.stat("scatter_bomb", "spread", exp);
        private final double aimRange = AbilityConfig.stat("scatter_bomb", "aim_range", exp);
        private final double aimRadius = AbilityConfig.stat("scatter_bomb", "aim_radius", exp);
        private final int holdTicks = (int) AbilityConfig.stat("scatter_bomb", "hold_ticks", exp);
        private final float holdDamage = AbilityConfig.stat("scatter_bomb", "hold_damage", exp);

        private final java.util.List<Vec3> subs = new java.util.ArrayList<>();

        private int ticks = 0;

        private float overloadKeep = 0;

        private boolean fired = false;

        public Ctx(Player player) {
            super(player, INSTANCE);
        }

        @Listener(channel = MSG_MADEALIVE, side = LogicalSide.SERVER)
        private void s_start() {
            ctx.consume(overload, 0);
            overloadKeep = ctx.cpData.getOverload();

        }

        @Listener(channel = MSG_TICK, side = LogicalSide.SERVER)
        private void s_tick() {
            if (ctx.cpData.getOverload() < overloadKeep) {
                ctx.cpData.setOverload(overloadKeep);
            }
            ticks++;

            if (ticks <= chargeMax && ticks >= ballDelay && (ticks - ballDelay) % ballInterval == 0) {
                Vec3 sub = randomSub();
                subs.add(sub);

                sendToClient(MSG_BALL, sub, holdTicks + 40);
            }

            if (holdTicks > 0 && ticks >= holdTicks) {
                if (holdDamage > 0) {
                    player.hurt(player.damageSources().playerAttack(player), holdDamage);
                }
                fireAll();
                terminate();
            }
        }

        @Listener(channel = MSG_KEYUP, side = LogicalSide.CLIENT)
        private void l_keyUp() {
            sendToServer(MSG_RELEASE);
        }

        @Listener(channel = MSG_KEYABORT, side = LogicalSide.CLIENT)
        private void l_keyAbort() {
            sendToServer(MSG_RELEASE);
        }

        @Listener(channel = MSG_RELEASE, side = LogicalSide.SERVER)
        private void s_release() {
            fireAll();
            terminate();
        }

        private void fireAll() {
            if (fired) {
                return;
            }
            fired = true;

            int n = 0;
            while (n < subs.size()) {
                float price = cpPer * (1 + cpStep * n);
                if (price > 0 && !ctx.consume(0, price)) {
                    break;
                }
                n++;
            }

            sendToClient(MSG_FIRE, n);

            if (n > 0) {
                Vec3 eye = player.getEyePosition(1.0f);
                LivingEntity target = findAimTarget();
                Vec3[] forced = assignMediums(eye, n);
                for (int i = 0; i < n; i++) {
                    fireOne(eye, subs.get(i), target, forced[i]);
                }
                ctx.addSkillExp(0.001f * n);
            }

            if (n < subs.size()) {

                player.displayClientMessage(
                        Component.translatable("gui.academy.scatter_bomb.no_cp", subs.size() - n), true);
            }

            if (cooldown > 0) {
                ctx.setCooldown(cooldown);
            }
        }

        private Vec3[] assignMediums(Vec3 eye, int n) {
            Vec3[] forced = new Vec3[n];
            for (cn.academy.entity.EntitySilbarn s : MdBarrage.nearby(player, RANGE)) {
                Vec3 mp = s.getBoundingBox().getCenter();
                Vec3 want = mp.subtract(eye);
                if (want.lengthSqr() < 1.0e-6) {
                    continue;
                }
                want = want.normalize();

                int best = -1;
                double bestDot = -2;
                for (int i = 0; i < n; i++) {
                    if (forced[i] != null) {
                        continue;
                    }
                    Vec3 sub = subs.get(i);
                    Vec3 d = sub.lengthSqr() < 1.0e-6 ? want : sub.normalize();
                    double dot = d.dot(want);
                    if (dot > bestDot) {
                        bestDot = dot;
                        best = i;
                    }
                }
                if (best < 0) {
                    break;
                }
                forced[best] = mp;
            }
            return forced;
        }

        private void fireOne(Vec3 eye, Vec3 sub, LivingEntity target, Vec3 forcedAt) {
            Vec3 from = eye.add(sub);
            Vec3 dir;

            if (forcedAt != null) {

                Vec3 to = forcedAt.subtract(from);
                dir = to.lengthSqr() < 1.0e-6 ? player.getViewVector(1.0f) : to.normalize();
            } else if (target == null) {

                dir = sub.lengthSqr() < 1.0e-6 ? player.getViewVector(1.0f) : sub.normalize();
            } else {

                Vec3 to = target.getEyePosition(1.0f).subtract(from);
                dir = to.lengthSqr() < 1.0e-6 ? player.getViewVector(1.0f) : to.normalize();
                dir = scatter(dir, spreadDeg);
            }

            MdBeam.Shot shot = MdBeam.solve(ctx, player, from, dir, RANGE, RANGE, damage, true,
                    (secPath, secHold) -> sendReflect(player, secPath, secHold));
            if (shot.bent && shot.path.size() >= 3) {
                sendToClient(MSG_RAY_PATH, cn.academy.util.RayReflect.encodePath(shot.path), shot.holdTicks);
            } else {

                sendToClient(MSG_RAY, from, shot.end(), shot.holdTicks);
            }
        }

        private static Vec3 randomSub() {
            double y = RandUtils.ranged(-1, 1);
            double t = RandUtils.ranged(0, Math.PI * 2);
            double r = Math.sqrt(Math.max(0, 1 - y * y));
            double range = RandUtils.ranged(BALL_FROM, BALL_TO);
            return new Vec3(r * Math.cos(t), y, r * Math.sin(t)).scale(range);
        }

        private static Vec3 scatter(Vec3 dir, double maxDeg) {
            if (maxDeg <= 0) {
                return dir;
            }

            Vec3 ref = Math.abs(dir.y) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
            Vec3 right = dir.cross(ref).normalize();
            Vec3 up = right.cross(dir).normalize();
            double a = Math.tan(Math.toRadians(RandUtils.ranged(-maxDeg, maxDeg)));
            double b = Math.tan(Math.toRadians(RandUtils.ranged(-maxDeg, maxDeg)));
            return dir.add(right.scale(a)).add(up.scale(b)).normalize();
        }

        private LivingEntity findAimTarget() {
            Vec3 eye = player.getEyePosition(1.0f);
            Vec3 look = player.getViewVector(1.0f);
            Vec3 far = eye.add(look.scale(aimRange));

            BlockHitResult b = player.level().clip(new ClipContext(
                    eye, far, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
            Vec3 clipEnd = b.getType() == HitResult.Type.BLOCK ? b.getLocation() : far;
            double depth = clipEnd.subtract(eye).length();

            AABB box = new AABB(eye, clipEnd).inflate(aimRadius);
            LivingEntity best = null;
            double bestDist = Double.MAX_VALUE;

            for (Entity e : player.level().getEntities(player, box,
                    x -> x != player && x.isAlive() && x instanceof LivingEntity
                            && ctx.canTarget(x))) {
                Vec3 c = e.getBoundingBox().getCenter();
                double along = c.subtract(eye).dot(look);
                if (along < 0 || along > depth) {
                    continue;
                }
                double perp = c.distanceTo(eye.add(look.scale(along)));
                if (perp > aimRadius || perp >= bestDist) {
                    continue;
                }

                BlockHitResult los = player.level().clip(new ClipContext(
                        eye, c, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
                if (los.getType() == HitResult.Type.BLOCK) {
                    continue;
                }
                bestDist = perp;
                best = (LivingEntity) e;
            }
            return best;
        }
    }

    @OnlyIn(Dist.CLIENT)
    @RegClientContext(Ctx.class)
    public static class CtxC extends ClientContext {

        private final java.util.List<EntityMdBall> balls = new java.util.ArrayList<>();

        public CtxC(Ctx par) {
            super(par);
        }

        @Listener(channel = Ctx.MSG_BALL, side = LogicalSide.CLIENT)
        private void c_ball(Vec3 sub, int life) {
            EntityMdBall ball = new EntityMdBall(player.level());
            ball.init(player, sub, life);
            ACEffectEntities.spawn(ball);
            balls.add(ball);
        }

        @Listener(channel = Ctx.MSG_FIRE, side = LogicalSide.CLIENT)
        private void c_fire(int count) {
            clearBalls();
            if (count > 0) {
                Vec3 at = player.getEyePosition(1.0f);
                player.level().playLocalSound(at.x, at.y, at.z,
                        cn.academy.ACSounds.MD_RAY_SMALL.get(), SoundSource.AMBIENT, 1.0f, 1.0f, false);
            }
        }

        @Listener(channel = Ctx.MSG_RAY, side = LogicalSide.CLIENT)
        private void c_ray(Vec3 from, Vec3 to, Integer hold) {
            EntityMdRaySmall ray = new EntityMdRaySmall(player.level());

            ray.viewOptimize = false;
            ray.setFromTo(from, to);

            if (hold > 0) {
                ray.life = Math.max(ray.life, hold);
            }
            ACEffectEntities.spawn(ray);
        }

        @Listener(channel = Ctx.MSG_RAY_PATH, side = LogicalSide.CLIENT)
        private void c_rayPath(byte[] raw, Integer hold) {
            java.util.List<Vec3> path = cn.academy.util.RayReflect.decodePath(raw);
            if (path == null) {
                return;
            }
            EntityMdRaySmall ray = new EntityMdRaySmall(player.level());
            ray.viewOptimize = false;
            ray.setPath(path);

            if (hold > 0) {
                ray.life = Math.max(ray.life, hold);
            }
            ACEffectEntities.spawn(ray);
        }

        @Listener(channel = MSG_TERMINATED, side = LogicalSide.CLIENT)
        private void c_terminated() {
            clearBalls();
        }

        private void clearBalls() {
            for (EntityMdBall ball : balls) {
                ball.discard();
            }
            balls.clear();
        }
    }
}
