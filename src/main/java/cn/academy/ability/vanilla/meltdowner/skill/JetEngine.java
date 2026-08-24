package cn.academy.ability.vanilla.meltdowner.skill;

import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientContext;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.RegClientContext;
import cn.academy.client.render.entity.ACEffectEntities;
import cn.academy.config.AbilityConfig;
import cn.academy.entity.EntityDiamondShield;
import cn.academy.entity.EntityRippleMark;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;

import java.util.HashSet;
import java.util.Set;
import cn.academy.util.ACDefense;

public class JetEngine extends Skill {

    public static final JetEngine INSTANCE = new JetEngine();

    static final String MSG_TRIGGER = "trigger";

    static final String MSG_MARK_END = "mark_end";

    public static final int FLIGHT_TICKS = 8;

    public static final int EFFECT_TICKS = 15;

    private static final float FLIGHT_WALK_SPEED = 0.07f;

    private static final double PROBE_STEP = 0.5;

    private static final double LIFT_MAX = 2.0;

    private static final double MIN_VALID_DIST = 1.0;

    private JetEngine() {
        super("jet_engine", 4);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void activate(ClientRuntime rt, int keyID) {
        activateSingleKey2(rt, keyID, Ctx::new);
    }

    public static Vec3 aimPos(Player p, double dist) {
        Vec3 eye = p.getEyePosition(1.0f);
        Vec3 look = p.getViewVector(1.0f);
        BlockHitResult block = p.level().clip(new ClipContext(
                eye, eye.add(look.scale(dist)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        return block.getType() == HitResult.Type.BLOCK
                ? block.getLocation()
                : p.position().add(look.scale(dist));
    }

    private static Vec3 liftOutOfGround(Player p, Vec3 t) {
        Level w = p.level();
        Vec3 from = p.position();
        AABB box = p.getBoundingBox();
        Vec3 up = cn.academy.gravity.RotationUtil.vecPlayerToWorld(
                new Vec3(0, 1, 0), cn.academy.gravity.ACGravity.getGravityDirection(p));
        for (double h = 0; h <= LIFT_MAX + 1.0e-6; h += 0.25) {
            Vec3 at = t.add(up.scale(h));
            if (w.noCollision(p, box.move(at.x - from.x, at.y - from.y, at.z - from.z).deflate(0.05))) {
                return at;
            }
        }
        return t;
    }

    public static Vec3 destFeet(Player p, double dist) {
        Level w = p.level();
        Vec3 from = p.position();
        Vec3 seg = liftOutOfGround(p, aimPos(p, dist)).subtract(from);
        double len = seg.length();
        if (len < 1.0e-4) {
            return from;
        }
        Vec3 dir = seg.scale(1.0 / len);
        AABB box = p.getBoundingBox();
        Vec3 best = from;
        for (double d = PROBE_STEP; ; d += PROBE_STEP) {
            boolean last = d >= len;
            Vec3 at = from.add(dir.scale(last ? len : d));
            AABB probe = box.move(at.x - from.x, at.y - from.y, at.z - from.z);
            if (!w.noCollision(p, probe.deflate(0.05))) {
                break;
            }
            best = at;
            if (last) {
                break;
            }
        }
        return best;
    }

    public static class Ctx extends Context<JetEngine> {

        private final float exp = ctx.getSkillExp();

        private final float consumption = AbilityConfig.cp("jet_engine", exp);
        private final float overload = AbilityConfig.overload("jet_engine", exp);
        private final int cooldown = (int) AbilityConfig.cooldown("jet_engine", exp);
        private final float damage = AbilityConfig.stat("jet_engine", "damage", exp);

        private final double distance = AbilityConfig.stat("jet_engine", "distance", exp);

        private final double chargeSpeed = AbilityConfig.stat("jet_engine", "charge_speed", exp);

        private final int chargeMin = (int) AbilityConfig.stat("jet_engine", "charge_min", exp);
        private final int shieldTicks = (int) AbilityConfig.stat("jet_engine", "shield_ticks", exp);

        private int chargeTicks = 0;

        private int flightTicks = 0;
        private boolean triggering = false;

        double chargedDist() {
            return Math.min(distance, chargeTicks * chargeSpeed);
        }

        private Vec3 start, target, velocity;

        private final Set<Integer> hitOnce = new HashSet<>();

        public Ctx(Player player) {
            super(player, INSTANCE);
        }

        @Listener(channel = MSG_KEYUP, side = LogicalSide.CLIENT)
        private void l_keyUp() {
            sendToServer(MSG_MARK_END);
        }

        @Listener(channel = MSG_KEYABORT, side = LogicalSide.CLIENT)
        private void l_keyAbort() {

            terminate();
        }

        @Listener(channel = MSG_TICK, side = LogicalSide.SERVER)
        private void s_tick() {
            if (!triggering) {
                chargeTicks++;
                if (!ctx.canConsumeCP(consumption)) {
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.translatable("gui.academy.jet_engine.no_cp"), true);
                    terminate();
                }
                return;
            }
            flightTicks++;
            if (flightTicks > EFFECT_TICKS) {
                terminate();
                return;
            }
            if (flightTicks <= FLIGHT_TICKS) {
                sweepDamage();
            }
        }

        @Listener(channel = MSG_MARK_END, side = LogicalSide.SERVER)
        private void s_markEnd() {

            if (chargeTicks < chargeMin) {
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("gui.academy.jet_engine.too_short"), true);
                terminate();
                return;
            }

            Vec3 dest = destFeet(player, chargedDist());
            if (dest.distanceToSqr(player.position()) < MIN_VALID_DIST * MIN_VALID_DIST) {
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("gui.academy.jet_engine.no_space"), true);
                terminate();
                return;
            }
            if (!ctx.consume(overload, consumption)) {
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("gui.academy.jet_engine.no_cp"), true);
                terminate();
                return;
            }
            triggering = true;
            flightTicks = 0;
            if (player.isPassenger()) {
                player.stopRiding();
            }
            ctx.addSkillExp(0.004f);
            ctx.setCooldown(cooldown);
            if (shieldTicks > 0) {
                MinecraftForge.EVENT_BUS.register(this);
            }
            sendToClient(MSG_TRIGGER, dest);
        }

        @Listener(channel = MSG_TERMINATED, side = LogicalSide.SERVER)
        private void s_terminated() {
            MinecraftForge.EVENT_BUS.unregister(this);
        }

        @SubscribeEvent
        public void onAttacked(LivingAttackEvent event) {
            if (event.getEntity() != player || !triggering || flightTicks > shieldTicks) {
                return;
            }
            float amount = event.getAmount();

            if (amount <= 0 || !Float.isFinite(amount) || ACDefense.isInstakill(event.getSource())) {
                return;
            }
            ACDefense.block(event);
        }

        private void sweepDamage() {
            double dx = player.getX() - player.xOld;
            double dy = player.getY() - player.yOld;
            double dz = player.getZ() - player.zOld;
            AABB swept = player.getBoundingBox()
                    .move(-dx, -dy, -dz)
                    .expandTowards(dx, dy, dz);
            for (Entity e : player.level().getEntities(player, swept,
                    x -> x != player && x.isAlive() && x instanceof LivingEntity
                            && ctx.canTarget(x))) {
                if (hitOnce.add(e.getId())) {
                    MDDamageHelper.attack(ctx, e, damage);
                }
            }
        }

        @Listener(channel = MSG_TRIGGER, side = LogicalSide.CLIENT)
        private void c_trigger(Vec3 dest) {
            if (!isLocal()) {
                return;
            }
            triggering = true;
            flightTicks = 0;
            start = player.position();
            target = dest;
            velocity = target.subtract(start).scale(1.0 / FLIGHT_TICKS);
        }

        @Listener(channel = MSG_TICK, side = LogicalSide.CLIENT)
        private void c_tick() {
            if (!isLocal()) {
                return;
            }
            if (!triggering) {
                chargeTicks++;
                return;
            }
            flightTicks++;
            if (flightTicks <= FLIGHT_TICKS) {
                double lambda = Math.min(1.0, (double) flightTicks / FLIGHT_TICKS);
                Vec3 at = start.add(target.subtract(start).scale(lambda));
                player.setPos(at.x, at.y, at.z);
                player.setDeltaMovement(velocity);
            }

            player.fallDistance = 0.0f;
        }
    }

    @OnlyIn(Dist.CLIENT)
    @RegClientContext(Ctx.class)
    public static class CtxC extends ClientContext {

        private final Ctx par;

        private EntityRippleMark mark;
        private EntityDiamondShield shield;

        private float walkSpeedBefore;
        private boolean walkSpeedTouched = false;
        private boolean triggering = false;

        public CtxC(Ctx par) {
            super(par);
            this.par = par;
        }

        @Listener(channel = MSG_MADEALIVE, side = LogicalSide.CLIENT)
        private void c_spawnMark() {
            if (!isLocal()) {
                return;
            }
            mark = new EntityRippleMark(player.level());
            ACEffectEntities.spawn(mark);
            mark.color.set(51, 255, 51, 179);
            Vec3 at = JetEngine.destFeet(player, par.chargedDist());
            mark.setPos(at.x, at.y, at.z);
        }

        @Listener(channel = MSG_TICK, side = LogicalSide.CLIENT)
        private void c_tick() {
            if (mark != null) {
                Vec3 at = JetEngine.destFeet(player, par.chargedDist());
                mark.setPos(at.x, at.y, at.z);
            }
            if (!triggering) {
                chargeParticles();
                return;
            }
            if (shield != null) {
                shield.touch();
            }

            for (int i = 0; i < 10; i++) {
                player.level().addParticle(cn.academy.ACParticles.MD.get(),
                        player.getX() + RandUtils.ranged(-.3, .3),
                        player.getY() + player.getBbHeight() * 0.5 + RandUtils.ranged(-.3, .3),
                        player.getZ() + RandUtils.ranged(-.3, .3),
                        RandUtils.ranged(-.02, .02), RandUtils.ranged(-.02, .02), RandUtils.ranged(-.02, .02));
            }
        }

        @Listener(channel = MSG_TRIGGER, side = LogicalSide.CLIENT)
        private void c_trigger(Vec3 dest) {
            triggering = true;
            endMark();

            shield = new EntityDiamondShield(player.level());
            shield.lifespan = EFFECT_TICKS / 20.0;
            shield.init(player);
            ACEffectEntities.spawn(shield);

            if (isLocal()) {

                walkSpeedBefore = player.getAbilities().getWalkingSpeed();
                walkSpeedTouched = true;
                player.getAbilities().setWalkingSpeed(FLIGHT_WALK_SPEED);
            }
        }

        @Listener(channel = MSG_TERMINATED, side = LogicalSide.CLIENT)
        private void c_terminated() {
            endMark();
            if (shield != null) {
                shield.discard();
                shield = null;
            }

            if (walkSpeedTouched) {
                player.getAbilities().setWalkingSpeed(walkSpeedBefore);
                walkSpeedTouched = false;
            }
            triggering = false;
        }

        private void endMark() {
            if (mark != null) {
                mark.discard();
                mark = null;
            }
        }

        private void chargeParticles() {
            float f = (float) Math.min(1.0, par.chargedDist() / Math.max(1.0e-6, par.distance));
            double r = 1.3 - 1.0 * f;
            for (int i = 0; i < 2; i++) {
                double theta = RandUtils.ranged(0, Math.PI * 2);
                double sx = Math.sin(theta) * r, sz = Math.cos(theta) * r;
                player.level().addParticle(cn.academy.ACParticles.MD.get(),
                        player.getX() + sx, player.getY() + RandUtils.ranged(0.1, 0.9), player.getZ() + sz,
                        -sx * 0.06, RandUtils.ranged(0, .02), -sz * 0.06);
            }
        }
    }
}
