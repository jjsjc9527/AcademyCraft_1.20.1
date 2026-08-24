package cn.academy.ability.vanilla.meltdowner.skill;

import cn.academy.ACSounds;
import cn.academy.ability.AbilityContext;
import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientContext;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.RegClientContext;
import cn.academy.client.render.entity.ACEffectEntities;
import cn.academy.client.sound.FollowEntitySound;
import cn.academy.config.AbilityConfig;
import cn.academy.entity.EntityMDRay;
import cn.academy.util.RayBeam;
import cn.academy.util.RayReflect;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.util.MathUtils;
import cn.lambdalib2.util.RandUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;

public class Meltdowner extends Skill {

    public static final Meltdowner INSTANCE = new Meltdowner();

    private Meltdowner() {
        super("meltdowner", 3);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void activate(ClientRuntime rt, int keyID) {
        activateSingleKey2(rt, keyID, Ctx::new);
    }

    private static final int REFLECT_DISTANCE = 10;

    private static final int DAMAGE_TICKS = 44;

    private static final long EXTEND_MS = 200;

    private static final String MSG_REFLECT = "md_reflect";

    private final RayBeam beam = new RayBeam(this, DAMAGE_TICKS, EXTEND_MS,
            this::reflectServer, cn.academy.ability.vanilla.meltdowner.skill.MdBarrage.HOOK);

    public static void init() {
        MinecraftForge.EVENT_BUS.register(INSTANCE);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent evt) {
        if (evt.phase == TickEvent.Phase.END) {
            beam.serverTick();
        }
    }

    private void reflectServer(Player player, Entity reflector, Vec3 reflectDir, Vec3 hitPoint) {

        AbilityContext ctx = AbilityContext.ofIfReady(player, this);
        if (ctx == null) {
            return;
        }
        float exp = ctx.getSkillExp();

        Vec3 dir = reflectDir != null && reflectDir.lengthSqr() > 1.0e-6
                ? reflectDir.normalize() : reflector.getLookAngle();

        LivingEntity hit = RayReflect.traceLiving(
                player.level(), reflector, hitPoint, dir, REFLECT_DISTANCE);
        if (hit != null) {
            ctx.attack(hit, AbilityConfig.stat("meltdowner", "reflect_damage", exp));
        }

        NetworkMessage.sendToTracking(player, this, MSG_REFLECT, player,
                (float) dir.x, (float) dir.y, (float) dir.z,
                (float) hitPoint.x, (float) hitPoint.y, (float) hitPoint.z);
    }

    @OnlyIn(Dist.CLIENT)
    @Listener(channel = MSG_REFLECT, side = LogicalSide.CLIENT)
    private void hReflectClient(Player player, Float dx, Float dy, Float dz,
                                Float hx, Float hy, Float hz) {
        EntityMDRay ray = new EntityMDRay(player.level());

        ray.viewOptimize = false;
        ray.aimAt(new Vec3(hx, hy, hz), new Vec3(dx, dy, dz), REFLECT_DISTANCE);
        ACEffectEntities.spawn(ray);
    }

    public static class Ctx extends Context<Meltdowner> {

        static final String MSG_PERFORM = "perform";

        static final String MSG_PERFORM_PATH = "perform_path";

        private static final int TICKS_MIN = 20;

        private static final int TICKS_MAX = 40;

        private static final int TICKS_TOLE = 100;

        private final float exp = ctx.getSkillExp();
        private final float tickCp = AbilityConfig.cp("meltdowner", exp);
        private final float overload = AbilityConfig.overload("meltdowner", exp);
        private final double length = AbilityConfig.stat("meltdowner", "length", exp);

        private int ticks = 0;

        private float overloadKeep = 0;

        public Ctx(Player player) {
            super(player, INSTANCE);
        }

        private float timeRate(int ct) {
            return MathUtils.lerpf(0.8f, 1.2f, (ct - (float) TICKS_MIN) / (TICKS_MAX - TICKS_MIN));
        }

        private int chargeTicks() {
            return Math.min(ticks, TICKS_MAX);
        }

        @Listener(channel = MSG_MADEALIVE, side = LogicalSide.SERVER)
        private void s_start() {
            ctx.consume(overload, 0);
            overloadKeep = ctx.cpData.getOverload();
        }

        @Listener(channel = MSG_TICK, side = LogicalSide.SERVER)
        private void s_tick() {
            ticks++;
            if (ctx.cpData.getOverload() < overloadKeep) {
                ctx.cpData.setOverload(overloadKeep);
            }

            if (!ctx.consume(0, tickCp)) {
                player.displayClientMessage(
                        Component.translatable("gui.academy.meltdowner.no_cp"), true);
                terminate();
                return;
            }
            if (ticks > TICKS_TOLE) {
                terminate();
            }
        }

        @Listener(channel = MSG_TICK, side = LogicalSide.CLIENT)
        private void c_tick() {
            ticks++;
        }

        @Listener(channel = MSG_KEYUP, side = LogicalSide.CLIENT)
        private void l_keyUp() {
            if (ticks >= TICKS_MIN) {
                sendToServer(MSG_PERFORM);

            } else {

                player.displayClientMessage(
                        Component.translatable("gui.academy.meltdowner.too_short"), true);
                terminate();
            }
        }

        @Listener(channel = MSG_KEYABORT, side = LogicalSide.CLIENT)
        private void l_keyAbort() {
            terminate();
        }

        @Listener(channel = MSG_PERFORM, side = LogicalSide.SERVER)
        private void s_perform() {
            int ct = chargeTicks();
            float rate = timeRate(ct);

            float dmg = rate * AbilityConfig.stat("meltdowner", "damage", exp);
            float energy = rate * AbilityConfig.stat("meltdowner", "energy", exp);
            double range = AbilityConfig.stat("meltdowner", "range", exp);

            RayBeam.Shape shape = INSTANCE.beam.fire(player, dmg, energy, length, range, e -> true, null);

            ctx.addSkillExp(rate * 0.002f);
            ctx.setCooldown((int) (rate * AbilityConfig.cooldown("meltdowner", exp)));

            if (shape.isStraight()) {
                sendToClient(MSG_PERFORM, shape.length(), shape.holdTicks());
            } else {

                sendToClient(MSG_PERFORM_PATH, RayReflect.encodePath(shape.path()), shape.holdTicks());
            }
            terminate();
        }
    }

    @OnlyIn(Dist.CLIENT)
    @RegClientContext(Ctx.class)
    public static class CtxC extends ClientContext {

        private static final double R_FROM = 0.7, R_TO = 1.0;
        private static final double H_FROM = -1.2, H_TO = 0;

        private FollowEntitySound chargeSound;

        private float walkSpeedBefore = 0.1f;
        private boolean walkSpeedTouched = false;
        private int ticks = 0;

        public CtxC(Ctx par) {
            super(par);
        }

        @Listener(channel = MSG_MADEALIVE, side = LogicalSide.CLIENT)
        private void c_start() {

            chargeSound = new FollowEntitySound(ACSounds.MD_CHARGE.get(), player, 1.0f);
            Minecraft.getInstance().getSoundManager().play(chargeSound);

            if (isLocal()) {
                walkSpeedBefore = player.getAbilities().getWalkingSpeed();
                walkSpeedTouched = true;
            }
        }

        @Listener(channel = MSG_TICK, side = LogicalSide.CLIENT)
        private void c_tick() {
            ticks++;

            if (walkSpeedTouched) {
                player.getAbilities().setWalkingSpeed(
                        Math.max(0, walkSpeedBefore - ticks * 0.001f));
            }

            Vec3 eye = player.getEyePosition();
            int count = RandUtils.rangei(2, 4);
            while (count-- > 0) {
                double r = RandUtils.ranged(R_FROM, R_TO);
                double theta = RandUtils.ranged(0, Math.PI * 2);
                double h = RandUtils.ranged(H_FROM, H_TO);
                player.level().addParticle(cn.academy.ACParticles.MD.get(),
                        eye.x + r * Math.sin(theta), eye.y + h, eye.z + r * Math.cos(theta),
                        RandUtils.ranged(-.03, .03), RandUtils.ranged(.01, .05), RandUtils.ranged(-.03, .03));
            }
        }

        @Listener(channel = Ctx.MSG_PERFORM, side = LogicalSide.CLIENT)

        private void c_perform(Double length, Integer hold) {
            player.level().playLocalSound(player.getX(), player.getY(), player.getZ(),
                    ACSounds.MD_MELTDOWNER.get(), SoundSource.PLAYERS, 0.5f, 1.0f, false);

            EntityMDRay ray = new EntityMDRay(player.level());
            ray.aimFromPlayer(player, length);

            if (hold > 0) {
                ray.life = Math.max(ray.life, hold);
            }
            ACEffectEntities.spawn(ray);
        }

        @Listener(channel = Ctx.MSG_PERFORM_PATH, side = LogicalSide.CLIENT)
        private void c_performPath(byte[] raw, Integer hold) {
            java.util.List<Vec3> path = RayReflect.decodePath(raw);
            if (path == null) {
                return;
            }
            player.level().playLocalSound(player.getX(), player.getY(), player.getZ(),
                    ACSounds.MD_MELTDOWNER.get(), SoundSource.PLAYERS, 0.5f, 1.0f, false);

            EntityMDRay ray = new EntityMDRay(player.level());

            ray.bendAlong(player, path);

            if (hold > 0) {
                ray.life = Math.max(ray.life, hold);
            }
            ACEffectEntities.spawn(ray);
        }

        @Listener(channel = MSG_TERMINATED, side = LogicalSide.CLIENT)
        private void c_terminated() {
            if (chargeSound != null) {
                chargeSound.requestStop();
                chargeSound = null;
            }

            if (walkSpeedTouched) {
                player.getAbilities().setWalkingSpeed(walkSpeedBefore);
                walkSpeedTouched = false;
            }
        }
    }
}
