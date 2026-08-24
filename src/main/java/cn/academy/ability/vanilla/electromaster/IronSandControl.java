package cn.academy.ability.vanilla.electromaster;

import cn.academy.ability.AbilityPipeline;
import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientContext;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.ContextManager;
import cn.academy.ability.context.KeyDelegate;
import cn.academy.ability.context.RegClientContext;
import cn.academy.client.render.entity.ACEffectEntities;
import cn.academy.client.render.util.ArcPatterns;
import cn.academy.config.AbilityConfig;
import cn.academy.ACSounds;
import cn.academy.client.sound.FollowEntitySound;
import cn.academy.entity.EntityArc;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.util.RandUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import cn.academy.util.ACDefense;

public class IronSandControl extends Skill {

    public static final IronSandControl INSTANCE = new IronSandControl();

    private IronSandControl() {
        super("iron_sand", 4);
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new Events());
    }

    static Vec3 upOf(net.minecraft.world.entity.Entity e) {
        net.minecraft.core.Direction g = cn.academy.gravity.ACGravity.getGravityDirection(e);
        return new Vec3(-g.getStepX(), -g.getStepY(), -g.getStepZ());
    }

    static Vec3[] planeOf(Vec3 up) {
        Vec3 a = Math.abs(up.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 r = up.cross(a).normalize();
        return new Vec3[]{r, r.cross(up).normalize()};
    }

    public enum Mode {
        ASSAULT, GUARD;

        public Mode next() {
            return this == ASSAULT ? GUARD : ASSAULT;
        }
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
                MinecraftForge.EVENT_BUS.post(new cn.academy.event.ability.FlushControlEvent());
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

    public static class Ctx extends Context<IronSandControl> {

        static final String MSG_MODE = "mode";
        static final String MSG_WHIP = "whip";
        static final String MSG_HIT = "hit";
        static final String MSG_DEFLECT = "defl";
        static final String MSG_RETURN = "wret";

        private final float exp = ctx.getSkillExp();

        private final float startCp = AbilityConfig.cp("iron_sand", exp);
        private final float startOverload = AbilityConfig.overload("iron_sand", exp);

        private final float cpTick = AbilityConfig.stat("iron_sand", "cp_tick", exp);
        private final float overloadTick = AbilityConfig.stat("iron_sand", "overload_tick", exp);
        private final int cooldown = (int) AbilityConfig.cooldown("iron_sand", exp);
        private final int windup = Math.max(1, (int) AbilityConfig.stat("iron_sand", "windup", 0f));

        private final float whipDamage = AbilityConfig.stat("iron_sand", "whip_damage", exp);
        private final int whipCd = Math.max(1, (int) AbilityConfig.stat("iron_sand", "whip_cd", exp));
        private final double whipRange = AbilityConfig.stat("iron_sand", "whip_range", exp);

        private final double whipRelease = AbilityConfig.stat("iron_sand", "whip_release", exp);

        private final double whipSpin = AbilityConfig.stat("iron_sand", "whip_spin", 0f);

        private final double whipChase = AbilityConfig.stat("iron_sand", "whip_chase", 0f);

        private final double whipTouch = AbilityConfig.stat("iron_sand", "whip_touch", 0f);

        private final double[] orbitBox = {
                AbilityConfig.stat("iron_sand", "orbit_near", 0f),
                AbilityConfig.stat("iron_sand", "orbit_far", 0f),
                AbilityConfig.stat("iron_sand", "orbit_low", 0f),
                AbilityConfig.stat("iron_sand", "orbit_high", 0f)};

        private final double orbitBlend = AbilityConfig.stat("iron_sand", "orbit_blend", 0f);

        private final int whipPierce = Math.max(2, (int) AbilityConfig.stat("iron_sand", "whip_pierce", 0f));

        private final int deflectTicks = Math.max(1, (int) AbilityConfig.stat("iron_sand", "deflect_ticks", 0f));

        private final double deflectDist = AbilityConfig.stat("iron_sand", "deflect_dist", 0f);

        private final double cloudHeight = AbilityConfig.stat("iron_sand", "cloud_height", 0f);

        final float guardReduce = AbilityConfig.stat("iron_sand", "guard_reduce", exp);

        private int ticks = 0;
        Mode mode = Mode.ASSAULT;

        private static final class SWhip {

            Vec3 tip;

            int deflectTicks = 0;

            Vec3 deflectDir = Vec3.ZERO;

            Vec3 deflectFrom = Vec3.ZERO;

            long orbitSeed;
            long prevOrbitSeed;

            Vec3 prevTip;

            Vec3 anchor;

            long lastHit;

            SWhip(Vec3 from, long now) {
                this.tip = from;
                this.prevTip = from;
                this.anchor = from;
                this.lastHit = now;
                this.orbitSeed = now;
                this.prevOrbitSeed = now;
            }
        }

        private static final class RWhip {
            Vec3 tip;
            Vec3 prevTip;
            final float damage;
            int life;

            RWhip(Vec3 at, float dmg, int life) {
                this.tip = at;
                this.prevTip = at;
                this.damage = dmg;
                this.life = life;
            }
        }

        private final List<RWhip> returning = new ArrayList<>();

        private final java.util.Map<Integer, SWhip> whips = new java.util.LinkedHashMap<>();

        private java.util.Set<Integer> lastSent = new java.util.HashSet<>();
        private int lastSentAt = Integer.MIN_VALUE / 2;

        public Ctx(Player player) {
            super(player, INSTANCE);
        }

        boolean formed() {
            return ticks >= windup;
        }

        @Listener(channel = MSG_MODE, side = LogicalSide.SERVER)
        private void s_setMode(int ordinal) {
            Mode[] values = Mode.values();
            if (ordinal >= 0 && ordinal < values.length) {
                mode = values[ordinal];

                sendToClient(MSG_MODE, ordinal, formed());
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

            if (player.isRemoved()) {
                terminate();
                return;
            }

            if (!ctx.cpData.canUseAbility()) {
                terminate();
                return;
            }

            boolean ok = (ticks == 0)
                    ? ctx.consume(startOverload + overloadTick, startCp + cpTick)
                    : ctx.consume(overloadTick, cpTick);
            if (!ok) {

                player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("gui.academy.iron_sand.no_cp"), true);
                terminate();
                return;
            }

            if (ticks % 20 == 0) {
                sendToClient(MSG_MODE, mode.ordinal(), formed());
            }
            if (formed()) {
                if (mode == Mode.ASSAULT) {
                    tickWhips();
                }

                if (ticks % 20 == 0) {
                    ctx.addSkillExp(0.0006f);
                }
            }
            ticks++;
        }

        private void tickWhips() {
            Vec3 from = cloudCenter();

            double rel2 = whipRelease * whipRelease;
            whips.keySet().removeIf(id -> {
                net.minecraft.world.entity.Entity e = player.level().getEntity(id);
                if (!(e instanceof LivingEntity le) || !le.isAlive()) {
                    return true;
                }

                if (le instanceof Player && !isHostile(le)) {
                    return true;
                }
                return le.position().distanceToSqr(player.position()) > rel2;
            });

            long spawnAt = player.level().getGameTime();
            for (LivingEntity t : allHostiles()) {
                whips.computeIfAbsent(t.getId(), i -> new SWhip(from, spawnAt));
            }
            List<Integer> stolen = new ArrayList<>();

            for (java.util.Map.Entry<Integer, SWhip> en : whips.entrySet()) {
                if (!(player.level().getEntity(en.getKey()) instanceof LivingEntity t)) {
                    continue;
                }
                SWhip w = en.getValue();
                long now = player.level().getGameTime();

                w.anchor = w.anchor.lerp(t.getBoundingBox().getCenter(), whipChase);

                Vec3 off = tipOffset(en.getKey(), now, w.lastHit, whipCd, orbitBox, whipPierce,
                        t.getBoundingBox().getCenter().subtract(w.anchor),
                        w.orbitSeed, w.prevOrbitSeed, orbitBlend);
                w.prevTip = w.tip;
                w.tip = w.anchor.add(off);

                if (w.deflectTicks > 0) {
                    w.deflectTicks--;

                    w.lastHit = now - whipCd;

                    double u = 1.0 - w.deflectTicks / (double) deflectTicks;
                    double push = u < DEFLECT_RISE ? u / DEFLECT_RISE
                            : Math.pow(1.0 - (u - DEFLECT_RISE) / (1.0 - DEFLECT_RISE), 1.5);
                    w.tip = w.deflectFrom.lerp(w.tip, u)
                            .add(w.deflectDir.scale(push * deflectDist));
                    continue;
                }
                if (now - w.lastHit < whipCd) {
                    continue;
                }
                if (!sweptHit(t.getBoundingBox().inflate(whipTouch), w.prevTip, w.tip)) {
                    continue;
                }
                w.lastHit = now;
                w.prevOrbitSeed = w.orbitSeed;
                w.orbitSeed = now;
                if (strike(t, w.tip, from, w)) {
                    stolen.add(en.getKey());
                }
            }
            whips.keySet().removeAll(stolen);
            tickReturning();

            if (!whips.keySet().equals(lastSent) || ticks - lastSentAt >= 20) {

                sendToClient(MSG_WHIP, encodeIds(whips.keySet()), whipCd);
                lastSent = new java.util.HashSet<>(whips.keySet());
                lastSentAt = ticks;
            }
        }

        private void tickReturning() {
            if (returning.isEmpty()) {
                return;
            }
            AABB me = player.getBoundingBox().inflate(whipTouch);
            Vec3 heart = player.getBoundingBox().getCenter();
            java.util.Iterator<RWhip> it = returning.iterator();
            while (it.hasNext()) {
                RWhip r = it.next();
                r.prevTip = r.tip;
                r.tip = r.tip.lerp(heart, RETURN_CHASE);
                if (sweptHit(me, r.prevTip, r.tip)) {
                    player.invulnerableTime = -1;
                    player.hurt(player.damageSources().playerAttack(player), r.damage);
                    it.remove();
                    continue;
                }
                if (--r.life <= 0) {
                    it.remove();
                }
            }
        }

        private boolean strike(LivingEntity t, Vec3 tip, Vec3 from, SWhip w) {

            boolean[] stolenFlag = {false};
            t.invulnerableTime = -1;

            Vec3 d = tip.subtract(w.prevTip);
            double len = d.length();
            Vec3 nd = len < 1.0e-4 ? tip.subtract(from).normalize() : d.scale(1.0 / len);
            boolean reflected = EMDamageHelper.attackReflect(ctx, t, whipDamage,
                    ev -> {
                        ev.incomingFrom = w.prevTip;
                        ev.incomingDir = nd;
                        ev.hitPos = tip;
                        ev.hitDist = len;
                        ev.difficulty = 0.35f;
                    },
                    ev -> {

                        if (ev.returnToCaster) {
                            stolenFlag[0] = true;
                            returning.add(new RWhip(tip, whipDamage, RETURN_LIFE));
                            sendToClient(MSG_RETURN, t.getId(),
                                    (float) tip.x, (float) tip.y, (float) tip.z);
                        } else {

                            Vec3 bounce = ev.reflectDir != null && ev.reflectDir.lengthSqr() > 1.0e-6
                                    ? ev.reflectDir.normalize()
                                    : tip.subtract(from).normalize().reverse();
                            w.deflectTicks = deflectTicks;
                            w.deflectDir = bounce;
                            w.deflectFrom = tip;
                            sendToClient(MSG_DEFLECT, t.getId(),
                                    (float) bounce.x, (float) bounce.y, (float) bounce.z);
                        }
                    });

            sendToClient(MSG_HIT, t.getId(), reflected);
            return stolenFlag[0];
        }

        private Vec3 cloudCenter() {
            return player.getEyePosition(1.0f).add(upOf(player).scale(cloudHeight));
        }

        private List<LivingEntity> allHostiles() {
            Vec3 c = player.getEyePosition(1.0f);
            AABB box = new AABB(c, c).inflate(whipRange);
            double r2 = whipRange * whipRange;
            List<LivingEntity> out = new ArrayList<>();
            for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, box,
                    x -> x != player && x.isAlive() && isHostile(x))) {

                if (e.position().distanceToSqr(player.position()) <= r2 && ctx.canAttack(e)) {
                    out.add(e);
                }
            }
            out.sort(java.util.Comparator.comparingDouble(e -> e.position().distanceToSqr(player.position())));
            return out;
        }

        private boolean isHostile(LivingEntity e) {
            if (e instanceof Player) {
                return AbilityPipeline.canTarget(player, e);
            }
            return e instanceof Enemy || (e instanceof Mob m && m.getTarget() == player);
        }

        @Listener(channel = MSG_TERMINATED, side = LogicalSide.SERVER)
        private void s_terminated() {
            ctx.setCooldown(cooldown);
        }
    }

    static byte[] encodeIds(java.util.Collection<Integer> ids) {
        java.nio.ByteBuffer buf = java.nio.ByteBuffer.allocate(ids.size() * 4);
        for (int id : ids) {
            buf.putInt(id);
        }
        return buf.array();
    }

    static java.util.Set<Integer> decodeIds(byte[] raw) {
        java.util.Set<Integer> out = new java.util.LinkedHashSet<>();
        if (raw == null || raw.length % 4 != 0) {
            return out;
        }
        java.nio.ByteBuffer buf = java.nio.ByteBuffer.wrap(raw);
        while (buf.remaining() >= 4) {
            out.add(buf.getInt());
        }
        return out;
    }

    public static class Events {

        @SubscribeEvent
        public void onLivingHurt(LivingHurtEvent event) {
            if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof Player p)) {
                return;
            }
            ContextManager.instance.find(Ctx.class, p).ifPresent(c -> {
                if (c.mode == Mode.GUARD && c.formed() && c.guardReduce > 0) {
                    ACDefense.reduce(event, event.getAmount() * Math.max(0f, 1f - c.guardReduce));
                }
            });
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static final class Strand {
        final Vec3 anchor;
        final double phase;
        final double ampScale;
        double u = 0;

        Strand(Vec3 anchor) {
            this.anchor = anchor;
            this.phase = RandUtils.ranged(0, Math.PI * 2);
            this.ampScale = RandUtils.ranged(0.7, 1.35);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static final class Lash {
        final int targetId;
        final double phase0;
        final double ampScale;

        Vec3 tip;

        double phase;
        double spin = 0.25;

        boolean retracting = false;

        Vec3 anchor;

        Vec3 prevTip;

        int deflectTicks = 0;
        Vec3 deflectDir = Vec3.ZERO;
        Vec3 deflectFrom = Vec3.ZERO;

        long orbitSeed;
        long prevOrbitSeed;

        long lastHit;

        Lash(int targetId, Vec3 from, long now) {
            this.lastHit = now;
            this.orbitSeed = now;
            this.prevOrbitSeed = now;
            this.targetId = targetId;
            this.phase0 = RandUtils.ranged(0, Math.PI * 2);
            this.phase = phase0;
            this.ampScale = RandUtils.ranged(0.75, 1.3);
            this.tip = from;
            this.prevTip = from;
            this.anchor = from;
        }
    }

    private static double wander(double u, double t, double seed) {

        return (1.00 * vnoise2(u * 6.5 + seed, t * 0.35)
                + 0.55 * vnoise2(u * 13.0 + seed + 31.0, t * 0.55)
                + 0.30 * vnoise2(u * 26.0 + seed + 71.0, t * 0.85)) / 1.85;
    }

    private static double vnoise2(double x, double y) {
        int xi = Mth.floor(x), yi = Mth.floor(y);
        double fx = x - xi, fy = y - yi;
        fx = fx * fx * (3 - 2 * fx);
        fy = fy * fy * (3 - 2 * fy);
        double a = hash2(xi, yi), b = hash2(xi + 1, yi);
        double c = hash2(xi, yi + 1), d = hash2(xi + 1, yi + 1);
        return (a + (b - a) * fx) * (1 - fy) + (c + (d - c) * fx) * fy;
    }

    private static double hash2(int x, int y) {
        int n = x * 374761393 + y * 668265263;
        n = (n ^ (n >>> 13)) * 1274126177;
        n = n ^ (n >>> 16);
        return ((n & 0xffffff) / 8388608.0) - 1.0;
    }

    private static final double ORBIT_SPEED = 0.25;

    private static final double ORBIT_BASE_TURN = 0.72;

    private static final double ORBIT_WOBBLE = 1.2;

    private static final double ORBIT_WOBBLE_SLOW = 0.35;

    private static final double RECOVER_BULGE = 0.65;

    private static final double ORBIT_TILT = 1.0;

    private static final double DEFLECT_RISE = 0.15;

    private static final double RETURN_CHASE = 0.34;

    private static final int RETURN_LIFE = 60;

    private static boolean sweptHit(net.minecraft.world.phys.AABB box, Vec3 from, Vec3 to) {
        return box.contains(to) || box.contains(from) || box.clip(from, to).isPresent();
    }

    private static Vec3 tipOffset(int targetId, long now, long lastHit, int cd,
                                  double[] orbit, int pierceTicks, Vec3 aim,
                                  long seed, long prevSeed, double blendRatio) {
        long since = now - lastHit;

        long recover = Math.max(2L, (long) (cd * blendRatio));
        if (since < recover) {
            Vec3 orb = orbitOffset(targetId, now, seed, prevSeed, orbit, cd, blendRatio);
            Vec3 a = orbitAt(targetId, lastHit, prevSeed, orbit);
            Vec3 exit = new Vec3(-a.x, a.y, -a.z);
            double s = (double) since / recover;
            Vec3 ctrl = aim.add(exit.subtract(aim).scale(RECOVER_BULGE));
            double v0 = (1 - s) * (1 - s), v1 = 2 * s * (1 - s), v2 = s * s;
            return aim.scale(v0).add(ctrl.scale(v1)).add(orb.scale(v2));
        }
        if (since >= cd && since < cd + pierceTicks) {
            Vec3 a = orbitOffset(targetId, lastHit + cd, seed, prevSeed, orbit, cd, blendRatio);
            double s = (double) (since - cd) / Math.max(1, pierceTicks - 1);

            Vec3 b = new Vec3(-a.x, a.y, -a.z);
            Vec3 c = aim.scale(2.0).subtract(new Vec3(0, a.y, 0));
            double w0 = (1 - s) * (1 - s), w1 = 2 * s * (1 - s), w2 = s * s;
            return a.scale(w0).add(c.scale(w1)).add(b.scale(w2));
        }
        return orbitOffset(targetId, now, seed, prevSeed, orbit, cd, blendRatio);
    }

    private static Vec3 orbitOffset(int targetId, long t, long seed, long prevSeed,
                                    double[] orbit, int cd, double blendRatio) {
        Vec3 cur = orbitAt(targetId, t, seed, orbit);
        long since = t - seed;
        double blend = Math.max(2.0, cd * blendRatio);
        if (seed == prevSeed || since >= blend || since < 0) {
            return cur;
        }
        double x = since / blend;
        return orbitAt(targetId, t, prevSeed, orbit).lerp(cur, x * x * (3 - 2 * x));
    }

    private static Vec3 orbitAt(int targetId, long t, long seed, double[] orbit) {
        double rH = orbit[0] + hash01(targetId, seed, 7) * (orbit[1] - orbit[0]);
        double h = orbit[2] + hash01(targetId, seed, 11) * (orbit[3] - orbit[2]);
        double rate = ORBIT_SPEED * (rH / Math.max(0.01, orbit[0]));
        double sd = targetId * 13.37;
        double ang = t * rate * ORBIT_BASE_TURN
                + wander(0.0, t * rate * ORBIT_WOBBLE_SLOW, sd) * ORBIT_WOBBLE;

        double phi = hash01(targetId, seed, 17) * Math.PI * 2;
        double cMax = Math.min(1.0, h / Math.max(0.01, rH)) * ORBIT_TILT;
        double cy = cMax * hash01(targetId, seed, 13);
        double sxz = Math.sqrt(Math.max(0.0, 1.0 - cy * cy));
        double cosA = Math.cos(ang) * rH, sinA = Math.sin(ang) * rH;
        double cf = Math.cos(phi), sf = Math.sin(phi);
        return new Vec3(cosA * cf - sinA * sf * sxz,
                        h + sinA * cy,
                        cosA * sf + sinA * cf * sxz);
    }

    private static double hash01(int a, long b, int salt) {
        return (hash2(a * 31 + salt, (int) (b & 0x7fffffffL)) + 1.0) * 0.5;
    }

    @RegClientContext(Ctx.class)
    public static class CtxC extends ClientContext {

        private static final float E = 0f;
        private final double radius = AbilityConfig.stat("iron_sand", "radius", E);
        private final int windup = Math.max(1, (int) AbilityConfig.stat("iron_sand", "windup", E));
        private final int maxStreams = Math.max(1, (int) AbilityConfig.stat("iron_sand", "streams", E));
        private final int streamInterval = Math.max(1, (int) AbilityConfig.stat("iron_sand", "stream_interval", E));
        private final int streamTicks = Math.max(2, (int) AbilityConfig.stat("iron_sand", "stream_ticks", E));
        private final double streamDensity = AbilityConfig.stat("iron_sand", "stream_density", E);
        private final double wobble = AbilityConfig.stat("iron_sand", "wobble", E);
        private final double turns = AbilityConfig.stat("iron_sand", "turns", E);
        private final double cloudHeight = AbilityConfig.stat("iron_sand", "cloud_height", E);
        private final double cloudRadius = AbilityConfig.stat("iron_sand", "cloud_radius", E);
        private final double cloudThick = AbilityConfig.stat("iron_sand", "cloud_thick", E);
        private final double cloudDensity = AbilityConfig.stat("iron_sand", "cloud_density", E);
        private final double whipDensity = AbilityConfig.stat("iron_sand", "whip_density", E);
        private final int whipBudget = Math.max(8, (int) AbilityConfig.stat("iron_sand", "whip_budget", E));

        private final double whipChase = AbilityConfig.stat("iron_sand", "whip_chase", E);

        private final double whipSag = AbilityConfig.stat("iron_sand", "whip_sag", E);

        private final int whipHit = Math.max(0, (int) AbilityConfig.stat("iron_sand", "whip_hit", E));

        private final int arcInterval = Math.max(1, (int) AbilityConfig.stat("iron_sand", "arc_interval", E));
        private final int arcWhipInterval = Math.max(1, (int) AbilityConfig.stat("iron_sand", "arc_whip_interval", E));
        private final int arcLife = Math.max(1, (int) AbilityConfig.stat("iron_sand", "arc_life", E));
        private final double arcLen = AbilityConfig.stat("iron_sand", "arc_len", E);
        private final double guardRadius = AbilityConfig.stat("iron_sand", "guard_radius", E);
        private final double guardHalfH = AbilityConfig.stat("iron_sand", "guard_half_h", E);
        private final double guardDensity = AbilityConfig.stat("iron_sand", "guard_density", E);

        private final double whipTurns = AbilityConfig.stat("iron_sand", "whip_turns", E) / 360.0;

        private final double whipCoil = AbilityConfig.stat("iron_sand", "whip_coil", E);

        private final double whipWobble = AbilityConfig.stat("iron_sand", "whip_wobble", E);

        private final double[] orbitBox = {
                AbilityConfig.stat("iron_sand", "orbit_near", E),
                AbilityConfig.stat("iron_sand", "orbit_far", E),
                AbilityConfig.stat("iron_sand", "orbit_low", E),
                AbilityConfig.stat("iron_sand", "orbit_high", E)};

        private final double orbitBlend = AbilityConfig.stat("iron_sand", "orbit_blend", E);
        private final int whipPierce = Math.max(2, (int) AbilityConfig.stat("iron_sand", "whip_pierce", E));

        private final double whipTailBias = Math.max(1.0, AbilityConfig.stat("iron_sand", "whip_tail_bias", E));

        private final double whipTrail = Math.max(0.0, Math.min(1.0, AbilityConfig.stat("iron_sand", "whip_trail", E)));

        private final double whipTipSpread = Math.max(0.02, AbilityConfig.stat("iron_sand", "whip_tip_spread", E));
        private final double whipSpin = AbilityConfig.stat("iron_sand", "whip_spin", E);

        private int whipCdC = 12;

        private final int deflectTicksC = Math.max(1, (int) AbilityConfig.stat("iron_sand", "deflect_ticks", E));
        private final double deflectDistC = AbilityConfig.stat("iron_sand", "deflect_dist", E);

        private static final int STREAM_BUDGET = 400;

        private static final double WHIP_LIFE = 14.0;

        private static final double RETRACT = 0.30, RETRACT_DONE = 0.45;

        private static final int ARC_WHIP_CAP = 3;

        private final List<Strand> streams = new ArrayList<>();

        private final java.util.Map<Integer, Lash> lashes = new java.util.LinkedHashMap<>();

        private static final class RLash {
            Vec3 tip;
            Vec3 prevTip;
            int life = RETURN_LIFE;

            RLash(Vec3 at) {
                this.tip = at;
                this.prevTip = at;
            }
        }

        private final List<RLash> rlashes = new ArrayList<>();
        private int ticks = 0;
        private Mode mode = Mode.ASSAULT;

        private static final int ATTACK_SOUND_CD = 4;

        private FollowEntitySound loopSound;

        private long lastAttackSound = -ATTACK_SOUND_CD;

        public CtxC(Ctx par) {
            super(par);
        }

        @Listener(channel = MSG_MADEALIVE, side = LogicalSide.CLIENT)
        private void c_makeAlive() {
            loopSound = new FollowEntitySound(ACSounds.EM_IRONSAND_CYCLE.get(), player, 1.0f);
            Minecraft.getInstance().getSoundManager().play(loopSound);
        }

        @Listener(channel = Ctx.MSG_MODE, side = LogicalSide.CLIENT)
        private void c_setMode(int ordinal, boolean formed) {
            Mode[] values = Mode.values();
            if (ordinal >= 0 && ordinal < values.length) {
                mode = values[ordinal];
            }
            if (formed && ticks < windup) {
                ticks = windup;
            }
        }

        @Listener(channel = Ctx.MSG_WHIP, side = LogicalSide.CLIENT)
        private void c_whip(byte[] raw, int cd) {
            java.util.Set<Integer> want = decodeIds(raw);
            whipCdC = Math.max(1, cd);
            for (int id : want) {

                lashes.computeIfAbsent(id, i -> new Lash(i, cloudCenter(), player.level().getGameTime()));
            }
            for (Lash l : lashes.values()) {

                l.spin = whipSpin * 20.0 / whipCdC;
                l.retracting = !want.contains(l.targetId);
            }
        }

        @Listener(channel = Ctx.MSG_DEFLECT, side = LogicalSide.CLIENT)
        private void c_deflect(int id, float dx, float dy, float dz) {
            Lash l = lashes.get(id);
            if (l != null) {
                l.deflectTicks = deflectTicksC;
                l.deflectDir = new Vec3(dx, dy, dz);
                l.deflectFrom = l.tip;
            }
        }

        @Listener(channel = Ctx.MSG_RETURN, side = LogicalSide.CLIENT)
        private void c_return(int id, float x, float y, float z) {
            lashes.remove(id);
            rlashes.add(new RLash(new Vec3(x, y, z)));
        }

        @Listener(channel = Ctx.MSG_HIT, side = LogicalSide.CLIENT)
        private void c_hit(int id, boolean reflected) {
            Lash l = lashes.get(id);
            if (l != null) {
                long now = player.level().getGameTime();
                l.lastHit = now;
                l.prevOrbitSeed = l.orbitSeed;
                l.orbitSeed = now;
            }
            if (reflected) {
                return;
            }
            if (player.level().getEntity(id) instanceof net.minecraft.world.entity.LivingEntity le) {
                burstAt(le);

                long now = player.level().getGameTime();
                if (now - lastAttackSound >= ATTACK_SOUND_CD) {
                    lastAttackSound = now;
                    player.level().playLocalSound(
                            le.getX(), le.getY() + le.getBbHeight() * 0.5, le.getZ(),
                            ACSounds.EM_IRONSAND_ATTACK.get(), SoundSource.AMBIENT, 0.7f, 1.0f, false);
                }
            }
        }

        private void spawnArc(Vec3 a, Vec3 b) {
            EntityArc arc = new EntityArc(player, ArcPatterns.weakArc);
            arc.viewOptimize = false;
            arc.lengthFixed = false;
            arc.texWiggle = 0.8;
            arc.showWiggle = 0.15;
            arc.hideWiggle = 0.5;
            arc.setLife(arcLife);
            arc.setFromTo(a.x, a.y, a.z, b.x, b.y, b.z);
            ACEffectEntities.spawn(arc);
        }

        private void spawnBodyArc() {
            boolean guard = mode == Mode.GUARD;
            Vec3 c = guard ? bodyCenter() : cloudCenter();
            Vec3 a = guard ? guardPoint(c) : cloudPoint(c);
            double t = RandUtils.ranged(0, Math.PI * 2);
            double y = RandUtils.ranged(-1, 1);
            double rr = Math.sqrt(Math.max(0, 1 - y * y));
            Vec3 dir = new Vec3(rr * Math.cos(t), y, rr * Math.sin(t));
            spawnArc(a, a.add(dir.scale(arcLen * RandUtils.ranged(0.6, 1.0))));
        }

        private void spawnWhipArc(Vec3 from, Lash l, double len) {
            double du = Math.min(0.45, arcLen / Math.max(len, 0.001));
            double u0 = RandUtils.ranged(0.1, 1.0 - du);
            spawnArc(whipPoint(from, l, u0), whipPoint(from, l, u0 + du));
        }

        private void burstAt(net.minecraft.world.entity.LivingEntity t) {
            Vec3 c = t.getBoundingBox().getCenter();
            Vec3 up = upOf(player);
            double r = Math.max(0.35, t.getBbWidth() * 0.5);
            for (int i = 0; i < whipHit; i++) {
                double th = RandUtils.ranged(0, Math.PI * 2);
                double y = RandUtils.ranged(-1, 1);
                double rr = Math.sqrt(Math.max(0, 1 - y * y));
                Vec3 dir = new Vec3(rr * Math.cos(th), y, rr * Math.sin(th));
                Vec3 p = c.add(dir.scale(r * RandUtils.ranged(0.4, 1.0)));
                Vec3 v = dir.scale(RandUtils.ranged(0.05, 0.16)).add(up.scale(-0.02));
                player.level().addParticle(cn.academy.ACParticles.IRON_SAND_WHIP.get(),
                        p.x, p.y, p.z, v.x, v.y, v.z);
            }
        }

        @Listener(channel = MSG_TICK, side = LogicalSide.CLIENT)
        private void c_tick() {
            float fill = Math.min(1f, ticks / (float) windup);

            if (ticks % streamInterval == 0 && streams.size() < maxStreams) {
                Vec3 src = pickGroundSpot();
                if (src != null) {
                    streams.add(new Strand(src));
                }
            }
            advance(streams, 1.0 / streamTicks, streamDensity, STREAM_BUDGET);

            tickLashes();
            tickRLashes();

            if (mode == Mode.GUARD) {
                spawnGuard(fill);
            } else {
                spawnCloud(fill);
            }

            if (fill >= 1f && ticks % arcInterval == 0) {
                spawnBodyArc();
            }
            ticks++;
        }

        @Listener(channel = MSG_TERMINATED, side = LogicalSide.CLIENT)
        private void c_terminated() {

            if (loopSound != null) {
                loopSound.requestStop();
                loopSound = null;
            }
            Vec3 up = upOf(player);
            Vec3[] pl = planeOf(up);
            for (int i = 0; i < 22; i++) {
                Vec3 p = mode == Mode.GUARD ? guardPoint(bodyCenter()) : cloudPoint(cloudCenter());

                Vec3 v = up.scale(RandUtils.ranged(-0.14, -0.04))
                        .add(pl[0].scale(RandUtils.ranged(-0.03, 0.03)))
                        .add(pl[1].scale(RandUtils.ranged(-0.03, 0.03)));
                player.level().addParticle(cn.academy.ACParticles.IRON_SAND.get(),
                        p.x, p.y, p.z, v.x, v.y, v.z);
            }
            streams.clear();
            lashes.clear();
        }

        private void tickLashes() {
            if (lashes.isEmpty()) {
                return;
            }
            Vec3 from = cloudCenter();
            int perLash = Math.max(1, whipBudget / lashes.size());
            int arcs = 0, idx = -1;
            java.util.Iterator<java.util.Map.Entry<Integer, Lash>> it = lashes.entrySet().iterator();
            while (it.hasNext()) {
                Lash l = it.next().getValue();
                idx++;
                net.minecraft.world.entity.Entity e = player.level().getEntity(l.targetId);

                l.prevTip = l.tip;
                if (l.retracting || e == null || !e.isAlive()) {
                    l.tip = l.tip.lerp(from, RETRACT);
                    if (l.tip.distanceToSqr(from) < RETRACT_DONE * RETRACT_DONE) {
                        it.remove();
                        continue;
                    }
                } else {

                    l.anchor = l.anchor.lerp(e.getBoundingBox().getCenter(), whipChase);

                    Vec3 off = tipOffset(l.targetId, player.level().getGameTime(),
                            l.lastHit, whipCdC, orbitBox, whipPierce,
                            e.getBoundingBox().getCenter().subtract(l.anchor),
                            l.orbitSeed, l.prevOrbitSeed, orbitBlend);
                    l.tip = l.anchor.add(off);
                }
                if (l.deflectTicks > 0) {
                    l.deflectTicks--;

                    l.lastHit = player.level().getGameTime() - whipCdC;
                    double du = 1.0 - l.deflectTicks / (double) deflectTicksC;
                    double dpush = du < DEFLECT_RISE ? du / DEFLECT_RISE
                            : Math.pow(1.0 - (du - DEFLECT_RISE) / (1.0 - DEFLECT_RISE), 1.5);
                    l.tip = l.deflectFrom.lerp(l.tip, du)
                            .add(l.deflectDir.scale(dpush * deflectDistC));
                }
                l.phase += l.spin;

                Vec3 axis = l.tip.subtract(from);
                double len = axis.length();
                if (len < 0.35) {
                    continue;
                }

                if (!l.retracting && arcs < ARC_WHIP_CAP && (ticks + idx) % arcWhipInterval == 0) {
                    spawnWhipArc(from, l, len);
                    arcs++;
                }

                int n = Mth.clamp((int) Math.ceil(lashArcLen(from, l) * whipDensity / WHIP_LIFE), 1, perLash);
                int nTrail = (int) (n * whipTrail);
                for (int k = 0; k < nTrail; k++) {
                    double r = RandUtils.ranged(0, 1);
                    Vec3 p = whipPoint(from, l, 1.0 - Math.pow(1.0 - r, whipTailBias));
                    spawnSand(p, 0.04);
                }

                for (int k = n - nTrail; k > 0; k--) {

                    spawnSand(l.prevTip.lerp(l.tip, RandUtils.ranged(0, 1)), whipTipSpread);
                }
            }
        }

        private void spawnSand(Vec3 c, double spread) {
            player.level().addParticle(cn.academy.ACParticles.IRON_SAND_WHIP.get(),
                    c.x + RandUtils.ranged(-spread, spread),
                    c.y + RandUtils.ranged(-spread, spread),
                    c.z + RandUtils.ranged(-spread, spread),
                    0, 0, 0);
        }

        private void tickRLashes() {
            if (rlashes.isEmpty()) {
                return;
            }
            Vec3 heart = player.getBoundingBox().getCenter();
            java.util.Iterator<RLash> it = rlashes.iterator();
            while (it.hasNext()) {
                RLash r = it.next();
                r.prevTip = r.tip;
                r.tip = r.tip.lerp(heart, RETURN_CHASE);
                int n = Math.max(6, (int) (whipDensity * 2));
                for (int k = 0; k < n; k++) {
                    spawnSand(r.prevTip.lerp(r.tip, RandUtils.ranged(0, 1)), whipTipSpread);
                }
                if (--r.life <= 0 || r.tip.distanceToSqr(heart) < 0.36) {
                    it.remove();
                }
            }
        }

        private double lashArcLen(Vec3 from, Lash l) {
            final int seg = 16;
            double s = 0;
            Vec3 prev = whipPoint(from, l, 0);
            for (int i = 1; i <= seg; i++) {
                Vec3 cur = whipPoint(from, l, (double) i / seg);
                s += cur.distanceTo(prev);
                prev = cur;
            }
            return s;
        }

        private Vec3 whipPoint(Vec3 from, Lash l, double u) {
            Vec3 axis = l.tip.subtract(from);

            double drop = Math.max(0, from.subtract(l.tip).dot(upOf(player)));
            Vec3 base = from.add(axis.scale(u))
                    .add(upOf(player).scale(-whipSag * drop * Math.sin(Math.PI * u)));
            Vec3 dir = axis.normalize();
            Vec3 up = upOf(player);
            Vec3 n1 = dir.cross(up);
            if (n1.lengthSqr() < 1.0e-6) {
                n1 = dir.cross(planeOf(up)[0]);
            }
            n1 = n1.normalize();
            Vec3 n2 = dir.cross(n1).normalize();

            double u2 = u * u;
            double env = Math.sin(Math.PI * u2 * u2);
            double amp = whipWobble * l.ampScale * env
                    * (1 + 0.35 * Math.sin(3.7 * 2 * Math.PI * u + l.phase0));
            double th = l.phase + whipTurns * 2 * Math.PI * u;
            double o1 = whipCoil * Math.cos(th) + wander(u, l.phase, l.phase0);
            double o2 = whipCoil * Math.sin(th) + wander(u, l.phase, l.phase0 + 17.31);
            return base.add(n1.scale(o1 * amp)).add(n2.scale(o2 * amp));
        }

        private Vec3 cloudCenter() {
            return player.getEyePosition(1.0f).add(upOf(player).scale(cloudHeight));
        }

        private Vec3 bodyCenter() {
            return player.getBoundingBox().getCenter();
        }

        private Vec3 hub() {
            return mode == Mode.GUARD ? bodyCenter() : cloudCenter();
        }

        private Vec3 pointAt(Vec3 from, Vec3 to, Strand s, double u, double amp0, double turn0) {
            Vec3 axis = to.subtract(from);
            Vec3 base = from.add(axis.scale(u));

            Vec3 dir = axis.normalize();

            Vec3 up = upOf(player);
            Vec3 n1 = dir.cross(up);
            if (n1.lengthSqr() < 1.0e-6) {
                n1 = dir.cross(planeOf(up)[0]);
            }
            n1 = n1.normalize();
            Vec3 n2 = dir.cross(n1).normalize();

            double env = Math.sin(Math.PI * u);
            double amp = amp0 * s.ampScale * env * (1 + 0.35 * Math.sin(3.7 * 2 * Math.PI * u + s.phase));
            double th = s.phase + turn0 * 2 * Math.PI * u;
            return base.add(n1.scale(Math.cos(th) * amp)).add(n2.scale(Math.sin(th) * amp));
        }

        private void advance(List<Strand> list, double du, double density, int budget) {
            if (list.isEmpty()) {
                return;
            }
            int perStrand = Math.max(1, budget / list.size());
            for (int i = list.size() - 1; i >= 0; i--) {
                Strand s = list.get(i);
                double u0 = s.u;
                s.u = Math.min(1.0, s.u + du);
                Vec3 from = s.anchor, to = hub();

                double chord = pointAt(from, to, s, u0, wobble, turns)
                        .distanceTo(pointAt(from, to, s, s.u, wobble, turns));
                int grain = Mth.clamp((int) Math.ceil(chord * density), 1, perStrand);
                for (int k = 1; k <= grain; k++) {
                    double u = u0 + (s.u - u0) * k / grain;
                    Vec3 p = pointAt(from, to, s, u, wobble, turns);

                    player.level().addParticle(cn.academy.ACParticles.IRON_SAND_FINE.get(),
                            p.x + RandUtils.ranged(-0.05, 0.05),
                            p.y + RandUtils.ranged(-0.05, 0.05),
                            p.z + RandUtils.ranged(-0.05, 0.05),
                            RandUtils.ranged(-0.004, 0.004),
                            RandUtils.ranged(0.002, 0.012),
                            RandUtils.ranged(-0.004, 0.004));
                }
                if (s.u >= 1.0) {
                    list.remove(i);
                }
            }
        }

        private Vec3 cloudPoint(Vec3 c) {
            Vec3 up = upOf(player);
            Vec3[] pl = planeOf(up);
            double t = RandUtils.ranged(0, Math.PI * 2);
            double rr = Math.sqrt(RandUtils.ranged(0, 1)) * cloudRadius;
            double k = rr / cloudRadius;
            double halfH = cloudThick * Math.sqrt(Math.max(0, 1 - k * k));
            return c.add(pl[0].scale(Math.cos(t) * rr))
                    .add(pl[1].scale(Math.sin(t) * rr))
                    .add(up.scale(RandUtils.ranged(-halfH, halfH)));
        }

        private void spawnCloud(float fill) {
            if (fill <= 0) {
                return;
            }
            Vec3 c = cloudCenter();
            Vec3 up = upOf(player);
            int n = (int) Math.ceil(cloudDensity * fill);
            for (int i = 0; i < n; i++) {
                Vec3 p = cloudPoint(c);

                final double W = 0.05;
                Vec3 v = up.cross(p.subtract(c)).scale(W).add(up.scale(RandUtils.ranged(-0.004, 0.004)));
                player.level().addParticle(cn.academy.ACParticles.IRON_SAND.get(),
                        p.x, p.y, p.z, v.x, v.y, v.z);
            }
        }

        private Vec3 guardPoint(Vec3 c) {
            Vec3 up = upOf(player);
            Vec3[] pl = planeOf(up);
            double t = RandUtils.ranged(0, Math.PI * 2);
            if (RandUtils.ranged(0, 1) < 0.4) {
                double rr = Math.sqrt(RandUtils.ranged(0, 1)) * guardRadius;
                double sign = RandUtils.ranged(0, 1) < 0.5 ? 1 : -1;
                return c.add(pl[0].scale(Math.cos(t) * rr))
                        .add(pl[1].scale(Math.sin(t) * rr))
                        .add(up.scale(sign * guardHalfH * RandUtils.ranged(0.88, 1.0)));
            }
            double rr = guardRadius * RandUtils.ranged(0.9, 1.0);
            return c.add(pl[0].scale(Math.cos(t) * rr))
                    .add(pl[1].scale(Math.sin(t) * rr))
                    .add(up.scale(RandUtils.ranged(-guardHalfH, guardHalfH)));
        }

        private void spawnGuard(float fill) {
            if (fill <= 0) {
                return;
            }
            Vec3 c = bodyCenter();
            Vec3 up = upOf(player);
            int n = (int) Math.ceil(guardDensity * fill);
            for (int i = 0; i < n; i++) {
                Vec3 p = guardPoint(c);

                final double W = 0.06;
                Vec3 v = up.cross(p.subtract(c)).scale(W).add(up.scale(RandUtils.ranged(-0.006, 0.006)));
                player.level().addParticle(cn.academy.ACParticles.IRON_SAND.get(),
                        p.x, p.y, p.z, v.x, v.y, v.z);
            }
        }

        private Vec3 pickGroundSpot() {
            Level lv = player.level();
            Vec3 up = upOf(player);
            Vec3[] pl = planeOf(up);
            Vec3 foot = player.position();
            for (int tries = 0; tries < 8; tries++) {
                double ang = RandUtils.ranged(0, Math.PI * 2);
                double r = radius * Math.sqrt(RandUtils.ranged(0.06, 1.0));
                Vec3 base = foot.add(pl[0].scale(Math.cos(ang) * r)).add(pl[1].scale(Math.sin(ang) * r));

                for (int d = 3; d >= -4; d--) {
                    Vec3 probe = base.add(up.scale(d));
                    BlockPos pos = BlockPos.containing(probe);
                    if (!lv.hasChunkAt(pos)) {
                        break;
                    }
                    BlockState here = lv.getBlockState(pos);
                    if (here.getCollisionShape(lv, pos).isEmpty()) {
                        continue;
                    }
                    BlockPos above = BlockPos.containing(probe.add(up));
                    if (!lv.getBlockState(above).getCollisionShape(lv, above).isEmpty()) {
                        continue;
                    }

                    return Vec3.atCenterOf(pos)
                            .add(up.scale(0.52))
                            .add(pl[0].scale(RandUtils.ranged(-0.35, 0.35)))
                            .add(pl[1].scale(RandUtils.ranged(-0.35, 0.35)));
                }
            }
            return null;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static final class ModeKeyHandler {

        private static final String KEY_GROUP = "EM_IronSandMode";

        private static Mode clientMode = Mode.ASSAULT;

        public static Mode mode() {
            return clientMode;
        }

        public static void init() {
            MinecraftForge.EVENT_BUS.register(new ModeKeyHandler());
        }

        @SubscribeEvent
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
                            clientMode == Mode.GUARD
                                    ? "gui.academy.iron_sand.mode_guard"
                                    : "gui.academy.iron_sand.mode_assault"), true);
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
