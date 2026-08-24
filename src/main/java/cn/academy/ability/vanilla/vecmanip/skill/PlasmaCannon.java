package cn.academy.ability.vanilla.vecmanip.skill;

import cn.academy.ability.AbilityPipeline;
import cn.academy.util.AimTrace;
import cn.academy.ACSounds;
import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientContext;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.DelegateState;
import cn.academy.ability.context.IConsumptionProvider;
import cn.academy.ability.context.IStateProvider;
import cn.academy.ability.context.RegClientContext;
import cn.academy.client.render.entity.ACEffectEntities;
import cn.academy.client.sound.FollowEntitySound;
import cn.academy.config.AbilityConfig;
import cn.academy.entity.EntityPlasmaBody;
import cn.academy.entity.EntityPlasmaTornado;
import cn.academy.util.RayReflect;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;

public class PlasmaCannon extends Skill {

    public static final PlasmaCannon INSTANCE = new PlasmaCannon();

    static final float REFLECT_DIFFICULTY = 1.2f;

    private static Explosion pendingExplosion = null;
    private static Player pendingCaster = null;

    public PlasmaCannon() {
        super("plasma_cannon", 5);

        MinecraftForge.EVENT_BUS.register(PlasmaCannon.class);
    }

    @SubscribeEvent
    public static void onDetonate(net.minecraftforge.event.level.ExplosionEvent.Detonate ev) {
        if (pendingExplosion != null && pendingCaster != null
                && ev.getExplosion() == pendingExplosion) {
            ev.getAffectedEntities().remove(pendingCaster);
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void activate(ClientRuntime rt, int keyID) {
        activateSingleKey2(rt, keyID, PlasmaCannonContext::new);
    }

    static Vec3 aimPos(Player player, double range) {
        Vec3 eye = player.getEyePosition(1.0f);
        Vec3 look = player.getViewVector(1.0f);
        Vec3 rayEnd = eye.add(look.scale(range));

        BlockHitResult block = player.level().clip(new ClipContext(
                eye, rayEnd, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        Vec3 clipEnd = block.getType() == HitResult.Type.BLOCK ? block.getLocation() : rayEnd;

        EntityHitResult ent = AimTrace.firstResult(player.level(), player, eye, clipEnd,
                e -> e != player && e.isAlive() && e instanceof LivingEntity
                        && AbilityPipeline.canTarget(player, e));
        return ent != null ? ent.getEntity().position() : clipEnd;
    }

    public static class PlasmaCannonContext extends Context<PlasmaCannon>
            implements IConsumptionProvider, IStateProvider {

        static final String MSG_PERFORM = "perform";
        static final String MSG_STATECHG = "state_change";
        static final String MSG_SYNCPOS = "sync_pos";

        public static final int STATE_CHARGING = 0, STATE_GO = 1;

        private static final double CHARGE_HEIGHT = 15;

        private static final double AIM_RANGE = 100;

        private static final int MAX_FLY_TICKS = 240;

        private static final double ARRIVE_DIST = 1.5;

        private static final double MAX_RISE = 6;

        private static final int READY_HINT_INTERVAL = 40;

        private final float exp = ctx.getSkillExp();
        private final float consumption = AbilityConfig.cp("plasma_cannon", exp);
        private final float overloadToKeep = AbilityConfig.overload("plasma_cannon", exp);
        private final int chargeTime = (int) AbilityConfig.stat("plasma_cannon", "charge_time", exp);
        private final double speed = AbilityConfig.stat("plasma_cannon", "speed", exp);
        private final float damage = AbilityConfig.stat("plasma_cannon", "damage", exp);

        private final double radius = AbilityConfig.stat("plasma_cannon", "radius", exp);

        private final float powerMax = AbilityConfig.stat("plasma_cannon", "power_max", exp);

        private final float explosionRadiusCap =
                AbilityConfig.stat("plasma_cannon", "explosion_radius_cap", exp);

        private float lockedMult = -1;

        private int localTicker = 0;

        private int syncTicker = 0;
        private int state = STATE_CHARGING;

        private Vec3 chargePosition;

        private Vec3 baseChargePos;

        private Vec3 destination;

        private float overloadKeep = 0;

        public PlasmaCannonContext(Player player) {
            super(player, INSTANCE);
            this.baseChargePos = player.position().add(0, CHARGE_HEIGHT, 0);
            this.chargePosition = baseChargePos;
        }

        public int getPhase() {
            return state;
        }

        public Vec3 getChargePosition() {
            return chargePosition;
        }

        private void updateChargeRise() {
            if (state == STATE_CHARGING && baseChargePos != null) {
                chargePosition = baseChargePos.add(0, powerProgress() * MAX_RISE, 0);
            }
        }

        public float powerMult() {
            if (lockedMult > 0) {
                return lockedMult;
            }
            if (chargeTime <= 0) {
                return 1f;
            }
            return net.minecraft.util.Mth.clamp(localTicker / (float) chargeTime, 1f, powerMax);
        }

        public float powerProgress() {
            if (powerMax <= 1f) {
                return 0f;
            }
            return net.minecraft.util.Mth.clamp((powerMult() - 1f) / (powerMax - 1f), 0f, 1f);
        }

        public boolean atMaxPower() {
            return powerMult() >= powerMax - 1.0e-4f;
        }

        private boolean isGrowing() {
            return lockedMult < 0 && !atMaxPower();
        }

        @Override
        public float getConsumptionHint() {
            return consumption * chargeTime;
        }

        @Override
        public DelegateState getState() {
            return state == STATE_CHARGING && localTicker < chargeTime
                    ? DelegateState.CHARGE : DelegateState.ACTIVE;
        }

        @Listener(channel = MSG_MADEALIVE, side = LogicalSide.SERVER)
        private void s_madeAlive() {
            ctx.consume(overloadToKeep, 0);
            overloadKeep = ctx.cpData.getOverload();

        }

        @Listener(channel = MSG_TICK, side = LogicalSide.SERVER)
        private void s_tick() {
            if (ctx.cpData.getOverload() < overloadKeep) {
                ctx.cpData.setOverload(overloadKeep);
            }
            localTicker++;

            if (state == STATE_CHARGING) {

                if (localTicker == 1) {
                    sendToClient(MSG_SYNCPOS, baseChargePos);
                }
                updateChargeRise();

                if (isGrowing() && !ctx.consume(0, consumption)) {

                    if (localTicker < chargeTime) {
                        player.displayClientMessage(
                                Component.translatable("gui.academy.plasma_cannon.no_cp"), true);
                        terminate();
                    } else {

                        lockedMult = powerMult();
                    }
                }
                return;
            }

            Vec3 lastPos = chargePosition;
            tryMove();
            if (hitSomething(lastPos, chargePosition)) {
                explode();
                return;
            }
            if (localTicker >= MAX_FLY_TICKS || chargePosition.distanceTo(destination) < arriveDist()) {
                explode();
                return;
            }

            if (syncTicker == 0) {
                syncTicker = 5;
                sendToClient(MSG_SYNCPOS, chargePosition);
            } else {
                syncTicker--;
            }
        }

        @Listener(channel = MSG_PERFORM, side = LogicalSide.SERVER)
        private void s_perform() {
            ctx.addSkillExp(0.008f);

            lockedMult = powerMult();

            destination = PlasmaCannon.aimPos(player, AIM_RANGE);
            state = STATE_GO;
            localTicker = 0;
            ctx.setCooldown((int) AbilityConfig.cooldown("plasma_cannon", exp));
            sendToClient(MSG_STATECHG, destination, (double) lockedMult);
        }

        private boolean hitSomething(Vec3 from, Vec3 to) {
            if (from.distanceToSqr(to) < 1.0e-8) {
                return false;
            }
            BlockHitResult block = player.level().clip(new ClipContext(
                    from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (block.getType() == HitResult.Type.BLOCK) {
                return true;
            }
            EntityHitResult ent = AimTrace.firstResult(player.level(), player, from, to,
                    e -> e != player && e.isAlive() && e instanceof LivingEntity
                            && AbilityPipeline.canTarget(player, e));
            return ent != null;
        }

        private void explode() {
            ServerLevel level = (ServerLevel) player.level();
            Vec3 at = destination;

            float mult = powerMult();
            float dmg = damage * mult;
            double range = radius * mult;

            float boom = (float) Math.min(range, explosionRadiusCap);

            for (Entity e : level.getEntities(player, new AABB(at, at).inflate(range))) {
                if (e.position().distanceToSqr(at) > range * range) {
                    continue;
                }
                if (!ctx.canTarget(e)) {
                    continue;
                }
                final Entity target = e;
                ctx.attackReflect(target, dmg,
                        ev -> {

                            Vec3 dir = target.getEyePosition(1.0f).subtract(at);
                            if (dir.lengthSqr() < 1.0e-6) {
                                dir = new Vec3(0, 1, 0);
                            }
                            RayReflect.fill(ev, at, dir, target, 0);
                            ev.difficulty = REFLECT_DIFFICULTY;
                            ev.deflectable = false;
                        },
                        ev -> {  });
                target.invulnerableTime = -1;
            }

            Explosion explosion = new Explosion(level, player, at.x, at.y, at.z,
                    boom, false, Explosion.BlockInteraction.DESTROY_WITH_DECAY);

            if (ctx.canBreakBlock(level)) {
                pendingExplosion = explosion;
                pendingCaster = player;
                try {
                    explosion.explode();
                } finally {
                    pendingExplosion = null;
                    pendingCaster = null;
                }
            }
            explosion.finalizeExplosion(true);

            level.playSound(null, at.x, at.y, at.z,

                    cn.academy.ACSounds.V_GENERIC_EXPLODE.get(), SoundSource.BLOCKS,
                    4.0f, (1.0f + (level.random.nextFloat() - level.random.nextFloat()) * 0.2f) * 0.7f);
            spawnBoomFx(level, at, range);

            terminate();
        }

        private void spawnBoomFx(ServerLevel level, Vec3 at, double r) {

            int cores = (int) net.minecraft.util.Mth.clamp(r / 3.0, 1, 10);
            farParticles(level, net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,
                    at, cores, r * 0.28, r * 0.22, r * 0.28, 0);

            int puffs = (int) net.minecraft.util.Mth.clamp(r * 5, 30, 220);
            farParticles(level, net.minecraft.core.particles.ParticleTypes.EXPLOSION,
                    at, puffs, r * 0.42, r * 0.35, r * 0.42, 0);

            int smoke = (int) net.minecraft.util.Mth.clamp(r * 6, 40, 260);
            farParticles(level, net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                    at, smoke, r * 0.55, r * 0.4, r * 0.55, 0.08);

            int ring = (int) net.minecraft.util.Mth.clamp(r * 1.5, 12, 48);
            for (int i = 0; i < ring; i++) {
                double a = i * Math.PI * 2 / ring;
                double rr = r * cn.lambdalib2.util.RandUtils.ranged(0.75, 1.0);
                double cos = Math.cos(a), sin = Math.sin(a);
                farParticles(level, net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                        new Vec3(at.x + cos * rr,
                                at.y + cn.lambdalib2.util.RandUtils.ranged(-1, 2),
                                at.z + sin * rr),
                        0, cos * 0.35, 0.12, sin * 0.35, 1.0);
            }
        }

        private void farParticles(ServerLevel level,
                                  net.minecraft.core.particles.ParticleOptions type, Vec3 at,
                                  int count, double dx, double dy, double dz, double speed) {
            for (net.minecraft.server.level.ServerPlayer sp : level.players()) {
                if (sp.distanceToSqr(at) <= 256 * 256) {
                    level.sendParticles(sp, type, true, at.x, at.y, at.z, count, dx, dy, dz, speed);
                }
            }
        }

        @Listener(channel = MSG_TICK, side = LogicalSide.CLIENT)
        private void c_tick() {

            if (state == STATE_GO) {
                tryMove();
                return;
            }

            localTicker++;
            updateChargeRise();
            if (!isLocal()) {
                return;
            }
            boolean wasMax = atMaxPower();
            if (isGrowing()) {

                ctx.consume(0, consumption);
            }

            if (localTicker < chargeTime) {
                return;
            }
            if (localTicker == chargeTime) {

                player.level().playLocalSound(player.getX(), player.getY(), player.getZ(),
                        ACSounds.VM_PLASMA_CANNON_T.get(), SoundSource.AMBIENT, 0.5f, 1.0f, false);
                readyHint();
            } else if (!wasMax && atMaxPower()) {

                player.level().playLocalSound(player.getX(), player.getY(), player.getZ(),
                        ACSounds.VM_PLASMA_CANNON_T.get(), SoundSource.AMBIENT, 0.7f, 1.4f, false);
                readyHint();
            } else if ((localTicker - chargeTime) % READY_HINT_INTERVAL == 0) {

                readyHint();
            }
        }

        private void readyHint() {
            String mult = String.format("%.1f", powerMult());
            player.displayClientMessage(Component.translatable(
                    atMaxPower() ? "gui.academy.plasma_cannon.ready_max"
                                 : "gui.academy.plasma_cannon.ready", mult), true);
        }

        @Listener(channel = MSG_KEYUP, side = LogicalSide.CLIENT)
        private void l_keyUp() {
            if (localTicker >= chargeTime) {
                sendToServer(MSG_PERFORM);
            } else {
                player.displayClientMessage(
                        Component.translatable("gui.academy.plasma_cannon.too_short"), true);
                terminate();
            }
        }

        @Listener(channel = MSG_KEYABORT, side = LogicalSide.CLIENT)
        private void l_keyAbort() {
            terminate();
        }

        @Listener(channel = MSG_STATECHG, side = LogicalSide.CLIENT)
        private void c_stateChange(Vec3 dest, double mult) {
            state = STATE_GO;
            destination = dest;
            lockedMult = (float) mult;
        }

        @Listener(channel = MSG_SYNCPOS, side = LogicalSide.CLIENT)
        private void c_syncPos(Vec3 pos) {

            if (state == STATE_CHARGING) {
                baseChargePos = pos;
                updateChargeRise();
            } else {
                chargePosition = pos;
            }
        }

        private void tryMove() {
            if (destination == null) {
                return;
            }
            Vec3 raw = destination.subtract(chargePosition);
            if (raw.length() < 1) {
                return;
            }
            chargePosition = chargePosition.add(raw.normalize().scale(moveStep()));
        }

        private double moveStep() {
            return speed * powerMult();
        }

        private double arriveDist() {
            return Math.max(ARRIVE_DIST, moveStep());
        }
    }

    @OnlyIn(Dist.CLIENT)
    @RegClientContext(PlasmaCannonContext.class)
    public static class PlasmaCannonContextC extends ClientContext {

        private static final double ARC_MIN_SPAN = 1.5;

        private static final double ARC_MAX_SPAN = 9.5;

        private static final double ARC_SURFACE_PUSH = 0.6;

        private static final int ARC_PICK_TRIES = 8;

        private static final int ARC_GAP_MIN = 6, ARC_GAP_MAX = 14;

        private static final int ARC_LIFE = 3;

        private static final float ARC_R = 0.85f, ARC_G = 0.42f, ARC_B = 1.0f;

        private final PlasmaCannonContext par;
        private FollowEntitySound loopSound;
        private EntityPlasmaBody body;
        private EntityPlasmaTornado tornado;
        private int arcCooldown = cn.lambdalib2.util.RandUtils.rangei(4, 12);

        public PlasmaCannonContextC(PlasmaCannonContext par) {
            super(par);
            this.par = par;
        }

        @Listener(channel = MSG_MADEALIVE, side = LogicalSide.CLIENT)
        private void c_makeAlive() {
            Level level = player.level();
            Vec3 at = par.getChargePosition();

            tornado = new EntityPlasmaTornado(level, par, at);
            ACEffectEntities.spawn(tornado);

            body = new EntityPlasmaBody(level, par);
            body.setPos(at.x, at.y, at.z);
            body.xOld = at.x;
            body.yOld = at.y;
            body.zOld = at.z;
            ACEffectEntities.spawn(body);

            loopSound = new FollowEntitySound(ACSounds.VM_PLASMA_CANNON.get(), player, 1.0f);
            Minecraft.getInstance().getSoundManager().play(loopSound);
        }

        @Listener(channel = MSG_TICK, side = LogicalSide.CLIENT)
        private void c_tick() {

            float vis = (float) Math.sqrt(par.powerMult());
            if (body != null) {
                body.scale = vis;
            }
            if (tornado != null) {
                tornado.scale = vis;

                tornado.density = par.powerProgress();
            }

            if (body != null) {
                body.touch();
                Vec3 at = par.getChargePosition();

                body.xOld = body.getX();
                body.yOld = body.getY();
                body.zOld = body.getZ();
                body.setPos(at.x, at.y, at.z);
            }
            if (tornado != null) {
                tornado.touch();

                if (par.getPhase() == PlasmaCannonContext.STATE_GO) {
                    tornado.setDying();
                }
            }

            if (--arcCooldown <= 0) {
                arcCooldown = cn.lambdalib2.util.RandUtils.rangei(ARC_GAP_MIN, ARC_GAP_MAX);
                spawnSurfaceArc(par.getChargePosition());
            }
        }

        private void spawnSurfaceArc(Vec3 center) {
            if (body == null) {
                return;
            }
            int first = body.firstSmallBall();
            int count = body.balls.length - first;
            if (count < 2) {
                return;
            }

            Vec3 o0 = null, o1 = null;
            for (int t = 0; t < ARC_PICK_TRIES; t++) {
                int i = first + cn.lambdalib2.util.RandUtils.rangei(0, count);
                int j = first + cn.lambdalib2.util.RandUtils.rangei(0, count);
                if (i == j) {
                    continue;
                }
                Vec3 a = body.ballOffset(i);
                Vec3 b = body.ballOffset(j);
                double d2 = a.distanceToSqr(b);
                if (d2 >= ARC_MIN_SPAN * ARC_MIN_SPAN && d2 <= ARC_MAX_SPAN * ARC_MAX_SPAN) {
                    o0 = a;
                    o1 = b;
                    break;
                }
            }
            if (o0 == null) {
                return;
            }

            Vec3 dir = o1.subtract(o0).normalize();
            Vec3 p0 = center.add(o0).subtract(dir.scale(ARC_SURFACE_PUSH));
            Vec3 p1 = center.add(o1).add(dir.scale(ARC_SURFACE_PUSH));

            cn.academy.entity.EntityArc arc =
                    new cn.academy.entity.EntityArc(player, cn.academy.client.render.util.ArcPatterns.plasmaArc);
            arc.viewOptimize = false;
            arc.lengthFixed = false;
            arc.setLife(ARC_LIFE);
            arc.setColor(ARC_R, ARC_G, ARC_B);
            arc.setFromTo(p0.x, p0.y, p0.z, p1.x, p1.y, p1.z);
            arc.xOld = arc.getX();
            arc.yOld = arc.getY();
            arc.zOld = arc.getZ();
            ACEffectEntities.spawn(arc);
        }

        @Listener(channel = MSG_TERMINATED, side = LogicalSide.CLIENT)
        private void c_terminated() {
            if (loopSound != null) {
                loopSound.requestStop();
                loopSound = null;
            }

            body = null;
            tornado = null;
        }
    }
}
