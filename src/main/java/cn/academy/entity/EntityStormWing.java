package cn.academy.entity;

import cn.academy.ACEntities;
import cn.academy.ability.context.Context;
import cn.lambdalib2.util.GameTimer;
import cn.lambdalib2.util.RandUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class EntityStormWing extends Entity implements cn.academy.client.render.BodyBones.Sink, cn.academy.client.render.ACEffect {

    public static final int TERMINATE_TICK = 15;

    private static final double HT = 2.0, SZ = 0.16, DSCALE = 2.0;

    private static final int DIVIDE = 40;

    private static final double SEP = 45;

    public static final class Shape {

        public final double divideMul;

        public final double widthMin, widthMax;

        public final double yJitter;

        public final double flare;

        public final double wobbleFreq;

        public final double wobbleAmp;

        public final double flowInterval;

        public final double flowLife;

        public final double sizeMin, sizeMax;

        public final double spikeMin, spikeMax;

        public final double swayPeriod;

        public final double swayAmp;

        public final double swayLag;

        public final double flapAmp;

        public final double curveAmp;

        public final double spikePow;

        public final double ringDiv;

        public final double spikeSmooth;

        public final double spikeRootRise;

        public final double rootTaper;

        public Shape(double divideMul, double widthMin, double widthMax,
                     double yJitter, double flare, double wobbleFreq, double wobbleAmp,
                     double flowInterval, double flowLife, double sizeMin, double sizeMax,
                     double spikeMin, double spikeMax,
                     double swayPeriod, double swayAmp, double swayLag, double flapAmp,
                     double curveAmp, double spikePow, double ringDiv, double spikeSmooth,
                     double spikeRootRise, double rootTaper) {
            this.spikeRootRise = spikeRootRise;
            this.rootTaper = rootTaper;
            this.flapAmp = flapAmp;
            this.curveAmp = curveAmp;
            this.spikePow = spikePow;
            this.ringDiv = ringDiv;
            this.spikeSmooth = spikeSmooth;
            this.sizeMin = sizeMin;
            this.sizeMax = sizeMax;
            this.spikeMin = spikeMin;
            this.spikeMax = spikeMax;
            this.swayPeriod = swayPeriod;
            this.swayAmp = swayAmp;
            this.swayLag = swayLag;
            this.divideMul = divideMul;
            this.widthMin = widthMin;
            this.widthMax = widthMax;
            this.yJitter = yJitter;
            this.flare = flare;
            this.wobbleFreq = wobbleFreq;
            this.wobbleAmp = wobbleAmp;
            this.flowInterval = flowInterval;
            this.flowLife = flowLife;
        }

        public boolean flowing() {
            return flowInterval > 0 && flowLife > 0;
        }
    }

    public static final class Ring {

        public final double y;

        public double bornTime;
        public final double width;

        public final double phase;
        public final double sizeScale;

        public final double threshold;

        public final float[] spikeU, spikeD;

        Ring(double y, double width, double phase, double sizeScale) {
            this(y, width, phase, sizeScale, -1);
        }

        Ring(double y, double width, double phase, double sizeScale, double threshold) {
            this(y, -1, width, phase, sizeScale, threshold, null);
        }

        Ring(double y, double bornTime, double width, double phase, double sizeScale,
             double threshold, float[][] spikes) {
            this.y = y;
            this.bornTime = bornTime;
            this.width = width;
            this.phase = phase;
            this.sizeScale = sizeScale;
            this.threshold = threshold;
            this.spikeU = spikes == null ? null : spikes[0];
            this.spikeD = spikes == null ? null : spikes[1];
        }
    }

    public static final class Tornado {

        public final List<Ring> rings = new ArrayList<>();

        private boolean spawnStopped = false;

        private double flowSpeed = 1.0;

        public void setFlowSpeed(double s) {
            flowSpeed = Math.max(0.05, s);
        }

        public void setSpawning(boolean on) {
            if (on && spawnStopped) {
                lastSpawn = GameTimer.getPausableTime();
            }
            spawnStopped = !on;
        }

        public final double timeOffset = RandUtils.RNG.nextDouble() * 20;
        public final double tx, ty, tz;
        public final double rx, ry, rz;

        private double ht;
        private final double sz, dscale;

        private final Shape shape;

        Tornado(double tx, double ty, double tz, double rx, double ry, double rz) {
            this(HT, SZ, DSCALE, tx, ty, tz, rx, ry, rz);
        }

        public Tornado(double ht, double sz, double dscale,
                       double tx, double ty, double tz, double rx, double ry, double rz) {
            this(ht, sz, dscale, 0, tx, ty, tz, rx, ry, rz);
        }

        public Tornado(double ht, double sz, double dscale, double extraDensity,
                       double tx, double ty, double tz, double rx, double ry, double rz) {
            this(ht, sz, dscale, extraDensity, tx, ty, tz, rx, ry, rz, null);
        }

        public Tornado(double ht, double sz, double dscale, double extraDensity,
                       double tx, double ty, double tz, double rx, double ry, double rz,
                       Shape shape) {
            this.ht = ht; this.sz = sz; this.dscale = dscale; this.shape = shape;
            this.tx = tx; this.ty = ty; this.tz = tz;
            this.rx = rx; this.ry = ry; this.rz = rz;

            if (shape != null && shape.flowing()) {
                return;
            }

            List<Ring> out = new ArrayList<>();
            double accum = 0;

            double stdstep = ht / (DIVIDE * (shape == null ? 1.0 : shape.divideMul));
            while (accum < ht) {
                accum += stdstep * (1.0 + RandUtils.RNG.nextGaussian() * 0.2);
                out.add(new Ring(ringY(accum, stdstep), ringW(stdstep, 1.8, 2.2),
                        RandUtils.RNG.nextDouble() * 360, ringSize(0.9, 1.2)));
                if (RandUtils.RNG.nextDouble() < 0.35) {
                    out.add(new Ring(ringY(accum, stdstep), ringW(stdstep, 1.8, 2.2),
                            RandUtils.RNG.nextDouble() * 360, ringSize(1.2, 1.7)));
                }

                for (int k = 0; k < (int) extraDensity; k++) {

                    out.add(new Ring(
                            ringY(accum + stdstep * (RandUtils.RNG.nextDouble() - 0.5) * 2, stdstep),
                            ringW(stdstep, 1.6, 2.6),
                            RandUtils.RNG.nextDouble() * 360, ringSize(0.6, 1.9),
                            RandUtils.RNG.nextDouble()));
                }
            }
            rings.addAll(out);
        }

        private double lastSpawn = -1;

        @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
        public void flowTick() {
            if (shape == null || !shape.flowing()) {
                return;
            }
            double now = GameTimer.getPausableTime();
            if (lastSpawn < 0) {
                seedFlow(now);
                return;
            }

            double life = effLife();
            rings.removeIf(r -> r.bornTime >= 0 && now - r.bornTime >= life);

            if (spawnStopped) {
                return;
            }

            if (now - lastSpawn > shape.flowLife) {
                rings.clear();
                seedFlow(now);
                return;
            }
            double iv = effInterval();
            while (now - lastSpawn >= iv) {
                lastSpawn += iv;
                spawnBatch(lastSpawn);
            }
        }

        private void seedFlow(double now) {
            int batches = (int) Math.ceil(effLife() / effInterval());
            for (int b = batches; b >= 0; b--) {
                spawnBatch(now - b * shape.flowInterval);
            }
            lastSpawn = now;
        }

        private void spawnBatch(double bornTime) {
            double stdstep = ht / (DIVIDE * shape.divideMul);

            double target = DIVIDE * shape.divideMul * 1.35;
            int n = (int) Math.max(1, Math.round(target * shape.flowInterval / shape.flowLife));
            for (int i = 0; i < n; i++) {

                double t = bornTime + shape.flowInterval * (i + RandUtils.RNG.nextDouble()) / n;
                boolean fringe = RandUtils.RNG.nextDouble() < 0.35;
                rings.add(new Ring(0, t, ringW(stdstep, 1.8, 2.2),
                        RandUtils.RNG.nextDouble() * 360,
                        fringe ? ringSize(1.2, 1.7) : ringSize(0.9, 1.2), -1, makeSpikes()));
            }
        }

        @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
        public double ringHeight(Ring r, double now) {
            if (r.bornTime < 0 || shape == null || !shape.flowing()) {
                return r.y;
            }

            return (now - r.bornTime) / effLife() * totalSpan();
        }

        private double ringW(double stdstep, double defMin, double defMax) {
            return stdstep * (shape == null ? RandUtils.ranged(defMin, defMax)
                    : RandUtils.ranged(shape.widthMin, shape.widthMax));
        }

        private float[][] makeSpikes() {
            if (shape == null || shape.spikeMax <= 0) {
                return null;
            }
            int div = (int) Math.max(20, shape.ringDiv);
            int sm = Math.max(1, (int) shape.spikeSmooth);
            int groups = Math.max(1, div / sm);
            double lo = shape.spikeMin, hi = shape.spikeMax;
            double pw = shape.spikePow > 0 ? shape.spikePow : 1.0;
            float[] u = new float[groups], d = new float[groups];
            for (int g = 0; g < groups; g++) {
                u[g] = (float) (lo + (hi - lo) * Math.pow(RandUtils.RNG.nextDouble(), pw));
                d[g] = (float) (lo + (hi - lo) * Math.pow(RandUtils.RNG.nextDouble(), pw));
            }
            return new float[][]{u, d};
        }

        private double ringSize(double defMin, double defMax) {
            return (shape == null || shape.sizeMax <= 0)
                    ? RandUtils.ranged(defMin, defMax)
                    : RandUtils.ranged(shape.sizeMin, shape.sizeMax);
        }

        private double ringY(double y, double stdstep) {
            if (shape == null || shape.yJitter <= 0) {
                return y;
            }
            return y + stdstep * RandUtils.ranged(-shape.yJitter, shape.yJitter);
        }

        public double time() {
            return GameTimer.getPausableTime() * 4.0 - timeOffset;
        }

        public double height() {
            return ht;
        }

        public void setHeight(double h) {
            if (h > 0) {
                ht = h;
            }
        }

        public static final class Extrude {

            public final org.joml.Vector3f dir;

            public final double len;

            public final double bend;

            public final double speed;

            public final org.joml.Vector3f endDir;

            public final double endLen;

            public final double endBend;

            public final double wrapR;

            public final double wrapTurns;

            public Extrude(org.joml.Vector3f dir, double len, double bend, double speed) {
                this(dir, len, bend, speed, null, 0, 0);
            }

            public Extrude(org.joml.Vector3f dir, double len, double bend, double speed,
                           org.joml.Vector3f endDir, double endLen, double endBend) {
                this(dir, len, bend, speed, endDir, endLen, endBend, 0, 0);
            }

            public Extrude(org.joml.Vector3f dir, double len, double bend, double speed,
                           org.joml.Vector3f endDir, double endLen, double endBend,
                           double wrapR, double wrapTurns) {
                this.dir = dir;
                this.len = len;
                this.bend = bend;
                this.speed = Math.max(0.05, speed);
                this.wrapR = Math.max(0, wrapR);
                this.wrapTurns = Math.max(0.1, wrapTurns);

                this.endDir = endDir;
                this.endLen = (endDir == null && this.wrapR <= 0) ? 0 : Math.max(0, endLen);
                this.endBend = endBend;
            }

            public double span() {
                return Math.max(0, len) + endLen;
            }
        }

        private Extrude extrude;

        private double totalSpan() {
            return ht + (extrude == null ? 0 : Math.max(0, extrude.span()));
        }

        public void setExtrude(Extrude e) {
            double oldLife = effLife(), oldSpan = totalSpan();
            extrude = e;
            double newLife = effLife(), newSpan = totalSpan();
            if (oldLife <= 0 || newLife <= 0 || oldSpan <= 0 || newSpan <= 0
                    || (oldLife == newLife && oldSpan == newSpan)) {
                return;
            }
            double k = (newLife / oldLife) * (oldSpan / newSpan);
            double now = GameTimer.getPausableTime();
            for (Ring r : rings) {
                if (r.bornTime >= 0) {
                    r.bornTime = now - (now - r.bornTime) * k;
                }
            }
        }

        public Extrude extrude() {
            return extrude;
        }

        public boolean extrudeReachedMain() {
            if (extrude == null || extrude.len <= 0 || rings.isEmpty()) {
                return false;
            }
            double now = GameTimer.getPausableTime();
            double need = ht + extrude.len;
            for (Ring r : rings) {
                if (ringHeight(r, now) >= need) {
                    return true;
                }
            }
            return false;
        }

        private double effLife() {
            if (extrude == null) {
                return shape.flowLife / flowSpeed;
            }

            double sp = extrude.span();
            double base = (sp <= 0 || ht <= 0)
                    ? shape.flowLife
                    : shape.flowLife * (1 + sp / ht);
            return base / extrude.speed / flowSpeed;
        }

        private double effInterval() {
            double base = extrude == null ? shape.flowInterval : shape.flowInterval / extrude.speed;
            return base / flowSpeed;
        }

        public double size() {
            return sz;
        }

        public double dscale() {
            return dscale;
        }

        public Shape shape() {
            return shape;
        }
    }

    public final Tornado[] tornados;

    private final Context<?> ctx;

    private Player owner() {
        return ctx.player;
    }

    public Vec3 boneOrigin = Vec3.ZERO;

    public Vec3 boneLeft = new Vec3(1, 0, 0), boneUp = new Vec3(0, 1, 0), boneFront = new Vec3(0, 0, 1);

    public int boneFrame = -1;

    public void storeBone(Vec3 origin, Vec3 left, Vec3 up, Vec3 front, int frame) {
        boneOrigin = origin;
        boneLeft = left;
        boneUp = up;
        boneFront = front;
        boneFrame = frame;
    }

    public double alpha = 0;

    public double lastFeedTime = GameTimer.getPausableTime();

    private boolean terminated = false;
    private int terminateTick = 0;

    public EntityStormWing(Player owner, Context<?> ctx) {
        super(ACEntities.STORM_WING.get(), owner.level());
        this.ctx = ctx;

        noCulling = true;

        tornados = new Tornado[]{
                new Tornado(-0.1, -0.3, 0.1, 0, SEP, SEP),
                new Tornado(0.1, -0.3, 0.1, 0, -SEP, -SEP),
                new Tornado(-0.1, -0.5, -0.1, 0, -SEP, SEP),
                new Tornado(0.1, -0.5, -0.1, 0, SEP, -SEP)
        };

        syncToPlayer();
        if (level().isClientSide) {
            cn.academy.client.render.BodyBones.register(owner.getUUID(), this);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (level().isClientSide) {
            cn.academy.client.render.BodyBones.unregister(owner().getUUID(), this);
        }
        super.remove(reason);
    }

    public Player getOwner() {
        return owner();
    }

    public void touch() {
        lastFeedTime = GameTimer.getPausableTime();
    }

    @Override
    public void tick() {
        super.tick();
        syncToPlayer();

        if (ctx.getStatus() == Context.Status.TERMINATED) {
            terminated = true;
        }
        if (terminated && ++terminateTick > TERMINATE_TICK) {
            discard();
            return;
        }
        alpha = currentAlpha();
    }

    private double currentAlpha() {
        if (terminated) {
            return 0.7 * (1 - terminateTick / (double) TERMINATE_TICK);
        }
        if (ctx instanceof cn.academy.ability.vanilla.vecmanip.skill.StormWing.StormWingContext sw) {
            if (sw.getState() == cn.academy.ability.vanilla.vecmanip.skill.StormWing.StormWingContext.STATE_CHARGE) {
                return Math.min(0.7, sw.getStateTick() / (double) sw.chargeTime() * 0.7);
            }
        }
        return 0.7;
    }

    private void syncToPlayer() {
        Player o = owner();
        setPos(o.getX(), o.getY(), o.getZ());
        xOld = o.xOld;
        yOld = o.yOld;
        zOld = o.zOld;
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public boolean effectExpired(double now) {
        return now - lastFeedTime > 2.0;
    }
}
