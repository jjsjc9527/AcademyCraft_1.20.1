package cn.academy.ability.vanilla.meltdowner.skill;

import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientContext;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.ContextManager;
import cn.academy.ability.context.RegClientContext;
import cn.academy.client.render.entity.ACEffectEntities;
import cn.academy.client.sound.FollowEntitySound;
import cn.academy.config.AbilityConfig;
import cn.academy.entity.EntityMdShield;
import cn.academy.event.ability.ReflectEvent;
import cn.academy.util.RayReflect;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.util.RandUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;

import java.util.List;
import cn.academy.util.ACDefense;

public class LightShield extends Skill {

    public static final LightShield INSTANCE = new LightShield();

    private static final int ACTION_INTERVAL = 18;

    private static final double FRONT_ANGLE = 60;

    private static final double TOUCH_RANGE = 3;

    private static final double MIN_UP = 0.0;

    public static Vec3 shieldNormal(LivingEntity p) {
        return Vec3.directionFromRotation(p.getXRot(), p.getYHeadRot());
    }

    public static final double SHIELD_FORWARD = 1.0;

    public static final double SHIELD_LIFT = 1.1;

    public static Vec3 shieldCenter(LivingEntity p) {
        Vec3 lift = cn.academy.gravity.RotationUtil.vecPlayerToWorld(
                new Vec3(0, SHIELD_LIFT, 0), cn.academy.gravity.ACGravity.getGravityDirection(p));
        return p.position().add(shieldNormal(p).scale(SHIELD_FORWARD)).add(lift);
    }

    public static boolean isFacing(LivingEntity defender, Vec3 incoming) {
        if (incoming == null || incoming.lengthSqr() < 1.0e-8) {
            return true;
        }

        double cos = -incoming.normalize().dot(shieldNormal(defender));
        return cos > Math.cos(Math.toRadians(FRONT_ANGLE));
    }

    public static void applyBend(ReflectEvent event, LivingEntity defender) {
        double d = Math.max(0, shieldCenter(defender).subtract(event.incomingFrom).dot(event.incomingDir));
        event.hitDist = d;
        event.hitPos = event.incomingFrom.add(event.incomingDir.scale(d));
        event.reflectDir = noDownward(RayReflect.mirror(event.incomingDir, shieldNormal(defender)), defender);
        event.bend = true;
    }

    public static Vec3 noDownward(Vec3 r, LivingEntity defender) {
        Vec3 up = cn.academy.gravity.RotationUtil.vecPlayerToWorld(
                new Vec3(0, 1, 0), cn.academy.gravity.ACGravity.getGravityDirection(defender));
        double vert = r.dot(up);
        if (vert >= MIN_UP) {
            return r;
        }
        Vec3 horiz = r.subtract(up.scale(vert));
        if (horiz.lengthSqr() < 1.0e-6) {

            Vec3 look = shieldNormal(defender);
            horiz = look.subtract(up.scale(look.dot(up)));
            if (horiz.lengthSqr() < 1.0e-6) {
                return r;
            }
        }
        return horiz.normalize().scale(Math.sqrt(Math.max(0, 1 - MIN_UP * MIN_UP)))
                .add(up.scale(MIN_UP));
    }

    private LightShield() {
        super("light_shield", 2);
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new Events());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void activate(ClientRuntime rt, int keyID) {
        activateSingleKey2(rt, keyID, Ctx::new);
    }

    public static class Events {

        @SubscribeEvent
        public void onPlayerAttacked(LivingAttackEvent event) {
            if (!(event.getEntity() instanceof Player p)) return;
            ContextManager.instance.find(Ctx.class, p).ifPresent(ctx -> {
                if (ctx.handleAttacked(event.getSource(), event.getAmount())) {
                    ACDefense.block(event);
                }
            });
        }
    }

    public static class Ctx extends Context<LightShield> {

        private final float exp = ctx.getSkillExp();

        private final float startOverload = AbilityConfig.overload("light_shield", exp);
        private final float cpPerTick = AbilityConfig.cp("light_shield", exp);
        private final float maxTime = AbilityConfig.stat("light_shield", "max_time", exp);

        private int ticks = 0;

        private int lastAbsorb = -1;

        private float overloadKeep = 0f;

        private long lastNoCpWarn = Long.MIN_VALUE / 2;

        public Ctx(Player player) {
            super(player, INSTANCE);
        }

        @Listener(channel = MSG_KEYUP, side = LogicalSide.CLIENT)
        private void l_keyUp() {
            terminate();
        }

        @Listener(channel = MSG_KEYABORT, side = LogicalSide.CLIENT)
        private void l_keyAbort() {
            terminate();
        }

        @Listener(channel = MSG_MADEALIVE, side = LogicalSide.SERVER)
        private void s_madeAlive() {
            ctx.consume(startOverload, 0);
            overloadKeep = ctx.cpData.getOverload();
            MinecraftForge.EVENT_BUS.register(this);
        }

        @SubscribeEvent
        public void onReflect(ReflectEvent event) {
            if (event.target != player || event.player == player) {
                return;
            }

            if (!isFacing(player, event.incomingDir)) {
                return;
            }
            if (!pay(event.damage)) {
                warnNoCp();
                return;
            }

            if (event.isRay() && event.deflectable) {
                applyBend(event, player);
            }
            event.setCanceled(true);
            ctx.addSkillExp(0.002f);
        }

        @Listener(channel = MSG_TICK, side = LogicalSide.SERVER)
        private void s_tick() {
            if (ctx.cpData.getOverload() < overloadKeep) {
                ctx.cpData.setOverload(overloadKeep);
            }
            ticks++;
            if (ticks > maxTime) {
                terminate();
                return;
            }
            if (!ctx.consume(0, cpPerTick)) {
                terminate();
                return;
            }
            ctx.addSkillExp(1e-6f);

            float touch = AbilityConfig.stat("light_shield", "touch_damage", exp);
            for (Entity e : frontTargets()) {

                if (e.invulnerableTime <= 0 && pay(touch)) {
                    MDDamageHelper.attack(ctx, e, touch);
                    ctx.addSkillExp(0.001f);
                }
            }
        }

        @Listener(channel = MSG_TERMINATED, side = LogicalSide.SERVER)
        private void s_terminated() {
            MinecraftForge.EVENT_BUS.unregister(this);

            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));

            ctx.setCooldown((int) (ticks * AbilityConfig.stat("light_shield", "cooldown_mult", exp)));
        }

        boolean handleAttacked(DamageSource src, float damage) {

            if (damage <= 0 || !Float.isFinite(damage) || ACDefense.isInstakill(src)) {
                return false;
            }

            if (lastAbsorb != -1 && ticks - lastAbsorb <= ACTION_INTERVAL) {
                return false;
            }

            Entity direct = src.getDirectEntity();
            if (direct != null && !isInFront(direct)) {
                return false;
            }

            if (!pay(damage)) {
                warnNoCp();
                return false;
            }
            lastAbsorb = ticks;
            ctx.addSkillExp(0.001f);
            return true;
        }

        private boolean pay(float damage) {
            float d = Math.max(1, damage);
            return ctx.consume(d * AbilityConfig.stat("light_shield", "block_overload", exp),
                    d * AbilityConfig.stat("light_shield", "block_cp", exp));
        }

        private void warnNoCp() {
            long now = player.level().getGameTime();
            if (now - lastNoCpWarn < 40) {
                return;
            }
            lastNoCpWarn = now;
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable("gui.academy.light_shield.no_cp"), true);
        }

        private List<Entity> frontTargets() {
            AABB box = player.getBoundingBox().inflate(TOUCH_RANGE);
            return player.level().getEntities(player, box, e ->
                    e != player && e.isAlive() && e instanceof LivingEntity
                            && e.distanceToSqr(player) <= TOUCH_RANGE * TOUCH_RANGE
                            && ctx.canTarget(e)
                            && isInFront(e));
        }

        private boolean isInFront(Entity e) {
            double dx = e.getX() - player.getX();
            double dz = e.getZ() - player.getZ();
            double yawToTarget = -Math.toDegrees(Math.atan2(dx, dz));
            return Math.abs(Mth.wrapDegrees(yawToTarget - player.getYHeadRot())) < FRONT_ANGLE;
        }
    }

    @OnlyIn(Dist.CLIENT)
    @RegClientContext(Ctx.class)
    public static class CtxC extends ClientContext {

        private EntityMdShield shield;
        private FollowEntitySound loopSound;

        public CtxC(Ctx par) {
            super(par);
        }

        @Listener(channel = MSG_MADEALIVE, side = LogicalSide.CLIENT)
        private void c_spawn() {
            shield = new EntityMdShield(player.level());
            shield.init(player);
            ACEffectEntities.spawn(shield);

            player.level().playLocalSound(player.getX(), player.getY(), player.getZ(),
                    cn.academy.ACSounds.MD_SHIELD_STARTUP.get(), SoundSource.AMBIENT, 0.5f, 1.0f, false);

            loopSound = new FollowEntitySound(cn.academy.ACSounds.MD_SHIELD_LOOP.get(), player, 0.5f);
            Minecraft.getInstance().getSoundManager().play(loopSound);
        }

        @Listener(channel = MSG_TICK, side = LogicalSide.CLIENT)
        private void c_tick() {
            if (shield != null) {
                shield.touch();
            }

            if (RandUtils.nextFloat() < 0.3f) {
                double s = 0.5;

                Vec3 at = shieldCenter(player)
                        .add(RandUtils.ranged(-s, s), RandUtils.ranged(-s, s), RandUtils.ranged(-s, s));
                player.level().addParticle(cn.academy.ACParticles.MD.get(),
                        at.x, at.y, at.z,
                        RandUtils.ranged(-.02, .02), RandUtils.ranged(-.01, .05), RandUtils.ranged(-.02, .02));
            }
        }

        @Listener(channel = MSG_TERMINATED, side = LogicalSide.CLIENT)
        private void c_terminated() {
            if (shield != null) {
                shield.discard();
                shield = null;
            }
            if (loopSound != null) {
                loopSound.requestStop();
                loopSound = null;
            }
        }
    }
}
