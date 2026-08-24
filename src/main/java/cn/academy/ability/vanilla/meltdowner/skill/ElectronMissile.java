package cn.academy.ability.vanilla.meltdowner.skill;

import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientContext;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.ContextManager;
import cn.academy.ability.context.KeyDelegate;
import cn.academy.ability.context.RegClientContext;
import cn.academy.client.render.entity.ACEffectEntities;
import cn.academy.config.AbilityConfig;
import cn.academy.entity.EntityMdBall;
import cn.academy.entity.EntityMdRaySmall;
import cn.academy.entity.EntitySilbarn;
import cn.academy.event.ability.FlushControlEvent;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.util.RandUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.LogicalSide;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ElectronMissile extends Skill {

    public static final ElectronMissile INSTANCE = new ElectronMissile();

    private static final int BALL_LIFE = 400;

    private static final double BALL_FROM = 0.8, BALL_TO = 1.3;

    private static final double MEDIUM_GAP = 0.15;

    private static final double MEDIUM_MIN = 0.5;

    private static final String MSG_REFLECT = "em_reflect";

    private static void sendReflect(Player player, List<Vec3> path, int hold) {
        cn.lambdalib2.s11n.network.NetworkMessage.sendToTracking(player, INSTANCE, MSG_REFLECT,
                player, cn.academy.util.RayReflect.encodePath(path), hold);
    }

    @OnlyIn(Dist.CLIENT)
    @Listener(channel = MSG_REFLECT, side = LogicalSide.CLIENT)
    private void hReflectClient(Player player, byte[] raw, Integer hold) {
        MdBeam.Fx.spawnRay(player, raw, hold, true);
    }

    public enum Mode {

        HOSTILE,

        ALL;

        public Mode next() {
            return this == HOSTILE ? ALL : HOSTILE;
        }
    }

    private ElectronMissile() {
        super("electron_missile", 5);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void activate(ClientRuntime rt, int keyID) {
        rt.addKey(keyID, new KeyDelegate() {
            @Override
            public void onKeyDown() {
                Optional<Ctx> opt = ContextManager.instance.findLocal(Ctx.class);
                if (opt.isPresent()) {
                    opt.get().terminate();
                } else {
                    ContextManager.instance.activate(new Ctx(getPlayer()));
                }
                MinecraftForge.EVENT_BUS.post(new FlushControlEvent());
            }

            @Override
            public ResourceLocation getIcon() {
                return INSTANCE.getHintIcon();
            }

            @Override
            public int createID() {
                return 0;
            }

            @Override
            public Skill getSkill() {
                return INSTANCE;
            }
        });
    }

    private static Vec3 randomSub() {
        double y = RandUtils.ranged(-1, 1);
        double t = RandUtils.ranged(0, Math.PI * 2);
        double r = Math.sqrt(Math.max(0, 1 - y * y));
        return new Vec3(r * Math.cos(t), y, r * Math.sin(t)).scale(RandUtils.ranged(BALL_FROM, BALL_TO));
    }

    public static class Ctx extends Context<ElectronMissile> {

        static final String MSG_MODE = "mode";
        static final String MSG_BALL = "ball";
        static final String MSG_FIRE = "fire";
        static final String MSG_RAY = "ray";
        static final String MSG_RAY_PATH = "ray_path";

        private final float exp = ctx.getSkillExp();

        private final float genCp = AbilityConfig.cp("electron_missile", exp);

        private final float fireCp = AbilityConfig.stat("electron_missile", "fire_cp", exp);

        private final float cpStep = AbilityConfig.stat("electron_missile", "cp_step", exp);

        private final float fireOverload = AbilityConfig.overload("electron_missile", exp);

        private final float overloadExp = AbilityConfig.stat("electron_missile", "overload_exp", exp);
        private final int cooldown = (int) AbilityConfig.cooldown("electron_missile", exp);
        private final float damage = AbilityConfig.stat("electron_missile", "damage", exp);
        private final double range = AbilityConfig.stat("electron_missile", "range", exp);

        private final int genInterval = Math.max(1, (int) AbilityConfig.stat("electron_missile", "gen_interval", exp));
        private final int fireCd = (int) AbilityConfig.stat("electron_missile", "fire_cd", exp);

        private final int genMax = (int) AbilityConfig.stat("electron_missile", "gen_max", exp)
                + (int) Math.floor(exp / Math.max(1.0e-4,
                        AbilityConfig.stat("electron_missile", "gen_max_step", exp)));

        private final double throwAhead = AbilityConfig.stat("electron_missile", "throw_ahead", exp);

        private int ticks = 0;

        private int genCount = 0, fireCount = 0;

        private int lastFire = Integer.MIN_VALUE / 2;

        private final List<Vec3> subs = new ArrayList<>();

        private Mode mode = Mode.HOSTILE;

        public Ctx(Player player) {
            super(player, INSTANCE);
        }

        @Listener(channel = MSG_MODE, side = LogicalSide.SERVER)
        private void s_setMode(int ordinal) {
            Mode[] values = Mode.values();
            if (ordinal >= 0 && ordinal < values.length) {
                mode = values[ordinal];
            }
        }

        @Listener(channel = MSG_MADEALIVE, side = LogicalSide.CLIENT)
        private void l_madeAlive() {
            if (isLocal()) {
                sendToServer(MSG_MODE, ModeKeyHandler.mode().ordinal());
            }
        }

        @Listener(channel = MSG_TICK, side = LogicalSide.SERVER)
        private void s_tick() {

            if (!ctx.cpData.canUseAbility()) {
                terminate();
                return;
            }
            if (!tryGenerate() || !tryFire()) {
                terminate();
                return;
            }

            ticks++;
        }

        private boolean tryGenerate() {
            if (ticks % genInterval != 0 || subs.size() >= genMax) {
                return true;
            }

            if (!ctx.consume(0, priceOf(genCp, genCount))) {
                return false;
            }
            genCount++;
            Vec3 sub = randomSub();
            subs.add(sub);
            sendToClient(MSG_BALL, sub, BALL_LIFE);
            return true;
        }

        private boolean tryFire() {
            if (subs.isEmpty() || ticks - lastFire < Math.max(1, fireCd)) {
                return true;
            }
            LivingEntity target = findTarget();
            if (target == null) {
                return true;
            }

            float overload = exp > overloadExp ? 0f : fireOverload;
            if (!ctx.consume(overload, priceOf(fireCp, fireCount))) {
                return false;
            }
            fireCount++;
            lastFire = ticks;

            Vec3 sub = subs.remove(RandUtils.rangei(0, subs.size()));
            sendToClient(MSG_FIRE, sub);

            Vec3 from = player.getEyePosition(1.0f).add(sub);
            Vec3 to = target.getEyePosition(1.0f);
            Vec3 dir = to.subtract(from);
            dir = dir.lengthSqr() < 1.0e-8 ? player.getViewVector(1.0f) : dir.normalize();

            Vec3 spot = mediumSpot(from, dir, target);
            if (spot != null) {
                placeSilbarn(spot, dir);
            }

            double len = from.distanceTo(to) + 1.0;

            MdBeam.Shot shot = MdBeam.solve(ctx, player, from, dir, len, len, damage, true,
                    (secPath, secHold) -> sendReflect(player, secPath, secHold));
            if (shot.bent) {
                sendToClient(MSG_RAY_PATH, cn.academy.util.RayReflect.encodePath(shot.path), shot.holdTicks);
            } else {
                sendToClient(MSG_RAY, from, shot.end(), shot.holdTicks);
            }
            ctx.addSkillExp(0.001f);
            return true;
        }

        private float priceOf(float base, int n) {
            return (float) (base * Math.pow(1 + cpStep, n));
        }

        private LivingEntity findTarget() {
            AABB box = player.getBoundingBox().inflate(range);
            LivingEntity best = null;
            double min = Double.MAX_VALUE;
            for (Entity e : player.level().getEntities(player, box,
                    x -> x != player && x.isAlive() && x instanceof LivingEntity
                            && ctx.canTarget(x))) {
                double d = e.distanceToSqr(player);
                if (d > range * range) {
                    continue;
                }
                if (mode == Mode.HOSTILE && !isHostile(e)) {
                    continue;
                }
                if (d < min) {
                    min = d;
                    best = (LivingEntity) e;
                }
            }
            return best;
        }

        private boolean isHostile(Entity e) {
            return e instanceof Enemy || (e instanceof Mob m && m.getTarget() == player);
        }

        private Vec3 mediumSpot(Vec3 from, Vec3 dir, LivingEntity target) {
            AABB box = target.getBoundingBox().inflate(cn.academy.util.AimTrace.INFLATE);
            if (box.contains(from)) {
                return null;
            }
            java.util.Optional<Vec3> enter = box.clip(from, from.add(dir.scale(range + 4)));
            double d = throwAhead;
            if (enter.isPresent()) {
                d = Math.min(d, enter.get().distanceTo(from) - MEDIUM_GAP);
            }
            return d < MEDIUM_MIN ? null : from.add(dir.scale(d));
        }

        private void placeSilbarn(Vec3 at, Vec3 dir) {
            net.minecraft.world.entity.player.Inventory inv = player.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack st = inv.getItem(i);
                if (!st.isEmpty() && st.is(cn.academy.ACItems.SILBARN.get())) {
                    if (!player.getAbilities().instabuild) {
                        st.shrink(1);
                    }
                    EntitySilbarn e = EntitySilbarn.thrownBy(player, dir);
                    e.setPos(at.x, at.y, at.z);
                    player.level().addFreshEntity(e);
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),

                            cn.academy.ACSounds.V_EGG_THROW.get(), SoundSource.PLAYERS, 0.5F,
                            0.4F / (player.level().getRandom().nextFloat() * 0.4F + 0.8F));
                    return;
                }
            }
        }

        @Listener(channel = MSG_TERMINATED, side = LogicalSide.SERVER)
        private void s_terminated() {

            ctx.setCooldown(cooldown);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @RegClientContext(Ctx.class)
    public static class CtxC extends ClientContext {

        private final List<Vec3> subs = new ArrayList<>();
        private final List<EntityMdBall> balls = new ArrayList<>();

        public CtxC(Ctx par) {
            super(par);
        }

        @Listener(channel = Ctx.MSG_BALL, side = LogicalSide.CLIENT)
        private void c_ball(Vec3 sub, int life) {
            EntityMdBall ball = new EntityMdBall(player.level());
            ball.init(player, sub, life);
            ACEffectEntities.spawn(ball);
            subs.add(sub);
            balls.add(ball);
        }

        @Listener(channel = Ctx.MSG_FIRE, side = LogicalSide.CLIENT)
        private void c_fire(Vec3 sub) {
            int i = subs.indexOf(sub);
            if (i < 0) {
                return;
            }
            subs.remove(i);
            EntityMdBall ball = balls.remove(i);
            if (ball != null) {
                ball.discard();
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
            player.level().playLocalSound(from.x, from.y, from.z,
                    cn.academy.ACSounds.MD_RAY_SMALL.get(), SoundSource.AMBIENT, 0.8f, 1.0f, false);
        }

        @Listener(channel = Ctx.MSG_RAY_PATH, side = LogicalSide.CLIENT)
        private void c_rayPath(byte[] raw, Integer hold) {
            List<Vec3> path = cn.academy.util.RayReflect.decodePath(raw);
            if (path == null || path.size() < 2) {
                return;
            }
            EntityMdRaySmall ray = new EntityMdRaySmall(player.level());
            ray.viewOptimize = false;
            ray.setPath(path);
            if (hold > 0) {
                ray.life = Math.max(ray.life, hold);
            }
            ACEffectEntities.spawn(ray);
            player.level().playLocalSound(path.get(0).x, path.get(0).y, path.get(0).z,
                    cn.academy.ACSounds.MD_RAY_SMALL.get(), SoundSource.AMBIENT, 0.8f, 1.0f, false);
        }

        @Listener(channel = MSG_TICK, side = LogicalSide.CLIENT)
        private void c_tick() {
            Vec3 eye = player.getEyePosition(1.0f);
            int n = RandUtils.rangei(1, 3);
            while (n-- > 0) {
                double r = RandUtils.ranged(0.5, 1.0);
                double theta = RandUtils.ranged(0, Math.PI * 2);
                double h = RandUtils.ranged(-1.2, 0);
                player.level().addParticle(cn.academy.ACParticles.MD.get(),
                        eye.x + r * Math.sin(theta), eye.y + h, eye.z + r * Math.cos(theta),
                        RandUtils.ranged(-.02, .02), RandUtils.ranged(.01, .05), RandUtils.ranged(-.02, .02));
            }
        }

        @Listener(channel = MSG_TERMINATED, side = LogicalSide.CLIENT)
        private void c_terminated() {
            for (EntityMdBall ball : balls) {
                if (ball != null) {
                    ball.discard();
                }
            }
            balls.clear();
            subs.clear();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static final class ModeKeyHandler {

        private static final String KEY_GROUP = "MD_MissileMode";

        private static Mode clientMode = Mode.HOSTILE;

        public static Mode mode() {
            return clientMode;
        }

        public static void init() {
            MinecraftForge.EVENT_BUS.register(new ModeKeyHandler());
        }

        @net.minecraftforge.eventbus.api.SubscribeEvent
        public void onClientTick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
            if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) {
                return;
            }
            Player p = net.minecraft.client.Minecraft.getInstance().player;

            if (p == null) {
                return;
            }

            boolean want = ContextManager.instance.findLocal(Ctx.class).isPresent();

            if (!ClientRuntime.available()) {
                return;
            }
            ClientRuntime rt = ClientRuntime.instance();

            boolean has = !rt.getDelegates(KEY_GROUP).isEmpty();
            if (want == has) {
                return;
            }
            if (want) {
                rt.addKey(KEY_GROUP, cn.lambdalib2.input.KeyManager.MOUSE_MIDDLE, new ModeDelegate());
            } else {
                rt.clearKeys(KEY_GROUP);
            }
        }

        private static final class ModeDelegate extends KeyDelegate {
            @Override
            public void onKeyDown() {
                clientMode = clientMode.next();

                ContextManager.instance.findLocal(Ctx.class)
                        .ifPresent(c -> c.sendToServer(Ctx.MSG_MODE, clientMode.ordinal()));

                Player p = net.minecraft.client.Minecraft.getInstance().player;
                if (p != null) {
                    p.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                            clientMode == Mode.ALL
                                    ? "gui.academy.electron_missile.mode_all"
                                    : "gui.academy.electron_missile.mode_hostile"), true);
                }
            }

            @Override
            public ResourceLocation getIcon() {
                return INSTANCE.getHintIcon();
            }

            @Override
            public int createID() {
                return 1;
            }

            @Override
            public Skill getSkill() {
                return INSTANCE;
            }
        }
    }
}
