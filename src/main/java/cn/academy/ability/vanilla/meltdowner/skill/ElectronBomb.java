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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.LogicalSide;

public class ElectronBomb extends Skill {

    public static final ElectronBomb INSTANCE = new ElectronBomb();

    private ElectronBomb() {
        super("electron_bomb", 1);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void activate(ClientRuntime rt, int keyID) {
        activateSingleKey2(rt, keyID, Ctx::new);
    }

    public static final double DISTANCE = 15;

    public static final int LIFE = 20, LIFE_IMPROVED = 5;

    private static final float FAST_EXP = 0.8f;

    private static final int FIRE_BEFORE_END = 2;

    private static final String MSG_REFLECT = "eb_reflect";

    private static void sendReflect(Player player, java.util.List<net.minecraft.world.phys.Vec3> path,
                                    int hold) {
        cn.lambdalib2.s11n.network.NetworkMessage.sendToTracking(player, INSTANCE, MSG_REFLECT,
                player, cn.academy.util.RayReflect.encodePath(path), hold);
    }

    @OnlyIn(Dist.CLIENT)
    @Listener(channel = MSG_REFLECT, side = LogicalSide.CLIENT)
    private void hReflectClient(Player player, byte[] raw, Integer hold) {
        MdBeam.Fx.spawnRay(player, raw, hold, true);
    }

    private static final java.util.Map<java.util.UUID, Burst> BURSTS = new java.util.concurrent.ConcurrentHashMap<>();

    private static final class Burst {

        int shots;

        int ticksLeft;
    }

    public static class Ctx extends Context<ElectronBomb> {

        static final String MSG_BALL = "ball";
        static final String MSG_RAY = "ray";

        static final String MSG_RAY_PATH = "ray_path";

        private final float exp = ctx.getSkillExp();
        private final int life = exp > FAST_EXP ? LIFE_IMPROVED : LIFE;
        private final float cp = AbilityConfig.cp("electron_bomb", exp);
        private final float overload = AbilityConfig.overload("electron_bomb", exp);

        private Vec3 sub = Vec3.ZERO;
        private int ticker = 0;
        private boolean fired = false;

        private boolean burstOwner = false;

        public Ctx(Player player) {
            super(player, INSTANCE);
        }

        @Listener(channel = MSG_MADEALIVE, side = LogicalSide.SERVER)
        private void s_start() {
            java.util.UUID id = player.getUUID();
            Burst b = BURSTS.get(id);

            int index = (b == null) ? 0 : b.shots;
            float mult = 1 + AbilityConfig.stat("electron_bomb", "cp_step", exp) * index;

            if ((cp > 0 || overload > 0) && !ctx.consume(overload * mult, cp * mult)) {

                player.displayClientMessage(
                        Component.translatable("gui.academy.electron_bomb.no_cp"), true);
                terminate();
                return;
            }

            if (b == null) {
                b = new Burst();
                b.shots = 1;
                b.ticksLeft = (int) AbilityConfig.stat("electron_bomb", "burst_window", exp);
                BURSTS.put(id, b);
                burstOwner = true;
            } else {
                b.shots++;
            }

            sub = randomSub(player);

        }

        @Listener(channel = MSG_TERMINATED, side = LogicalSide.SERVER)
        private void s_terminated() {
            if (burstOwner) {
                BURSTS.remove(player.getUUID());
            }
        }

        private void closeBurst() {
            BURSTS.remove(player.getUUID());
            ctx.setCooldown((int) AbilityConfig.cooldown("electron_bomb", exp));
        }

        @Listener(channel = MSG_TICK, side = LogicalSide.SERVER)
        private void s_tick() {
            ticker++;

            if (ticker == 1) {
                sendToClient(MSG_BALL, sub, life);
            }

            if (!fired && ticker >= life - FIRE_BEFORE_END) {
                fired = true;

                Vec3 from = ballPos(player, sub);
                Vec3 aim = aimPos(player);
                float dmg = AbilityConfig.stat("electron_bomb", "damage", exp);

                Vec3 toAim = aim.subtract(from);
                double dist = toAim.length();
                Vec3 dir = dist < 1.0e-6 ? player.getViewVector(1.0f) : toAim.normalize();

                MdBeam.Shot shot = MdBeam.solve(ctx, player, from, dir, dist, DISTANCE, dmg, false,
                        (secPath, secHold) -> sendReflect(player, secPath, secHold));

                if (shot.bent && shot.path.size() >= 3) {
                    sendToClient(MSG_RAY_PATH, cn.academy.util.RayReflect.encodePath(shot.path), shot.holdTicks);
                } else {

                    sendToClient(MSG_RAY, from, shot.truncated ? shot.end() : aim, shot.holdTicks);
                }
                ctx.addSkillExp(0.005f);
            }

            if (!burstOwner) {
                if (fired) terminate();
                return;
            }

            Burst b = BURSTS.get(player.getUUID());
            if (b == null) {
                terminate();
                return;
            }
            if (--b.ticksLeft <= 0) {
                closeBurst();
                terminate();
            }
        }
    }

    public static Vec3 ballPos(Player player, Vec3 sub) {
        return player.getEyePosition().add(sub);
    }

    private static Vec3 randomSub(Player player) {
        float theta = (float) (-player.getYRot() / 180 * Math.PI
                + RandUtils.ranged(-EntityMdBall.YAW_SPREAD, EntityMdBall.YAW_SPREAD));
        double range = RandUtils.ranged(EntityMdBall.RANGE_FROM, EntityMdBall.RANGE_TO);
        return new Vec3(
                Mth.sin(theta) * range,
                RandUtils.ranged(EntityMdBall.SUB_Y_FROM, EntityMdBall.SUB_Y_TO),
                Mth.cos(theta) * range);
    }

    private static Vec3 aimPos(Player player) {
        Vec3 eye = player.getEyePosition(1.0f);
        Vec3 end = eye.add(player.getViewVector(1.0f).scale(DISTANCE));
        BlockHitResult b = player.level().clip(new ClipContext(
                eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        return b.getType() == HitResult.Type.BLOCK ? b.getLocation() : end;
    }

    @OnlyIn(Dist.CLIENT)
    @RegClientContext(Ctx.class)
    public static class CtxC extends ClientContext {

        private EntityMdBall ball;

        public CtxC(Ctx par) {
            super(par);
        }

        @Listener(channel = Ctx.MSG_BALL, side = LogicalSide.CLIENT)
        private void c_ball(Vec3 sub, int life) {
            ball = new EntityMdBall(player.level());
            ball.init(player, sub, life);
            ACEffectEntities.spawn(ball);
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

            player.level().playLocalSound(from.x, from.y, from.z,
                    cn.academy.ACSounds.MD_RAY_SMALL.get(), SoundSource.AMBIENT, 0.8f, 1.0f, false);
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

            Vec3 from = path.get(0);
            player.level().playLocalSound(from.x, from.y, from.z,
                    cn.academy.ACSounds.MD_RAY_SMALL.get(), SoundSource.AMBIENT, 0.8f, 1.0f, false);
        }

        @Listener(channel = MSG_TERMINATED, side = LogicalSide.CLIENT)
        private void c_terminated() {

            ball = null;
        }
    }
}
