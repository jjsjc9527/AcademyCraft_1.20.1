package cn.academy.entity;

import cn.academy.ACEntities;
import cn.academy.ability.context.Context;
import cn.academy.client.render.BodyBones;
import cn.lambdalib2.util.GameTimer;
import cn.lambdalib2.util.RandUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class EntityDualWing extends Entity implements BodyBones.Sink, cn.academy.client.render.ACEffect {

    public static final double ROOT_Y = -0.20;

    public static final int TERMINATE_TICK = 12;

    public static final int GROW_TICK = 18;

    public static final int PER_SIDE = 1;

    public static final double FLATTEN = 1.00;

    private static final double MAX_ALPHA = 0.86;

    public static final boolean FLAP_ON = false;

    public static final double FLAP_PERIOD = 0.45;

    public static final double FLAP_AMP = 13.0;

    public static final double FLAP_RX = 5.0;

    public static final double FLAP_FLEX = 0.45;

    public static final double FLAP_LAG = 0.35;

    public static final double FLAP_CHURN = 15.0;

    public static final double FLAP_SPIN = 3.0;

    private static final double HT_MIN = 4.6, HT_MAX = 5.6;

    private static final double SZ_MIN = 0.48, SZ_MAX = 0.62;

    private static final double DS_MIN = 2.2, DS_MAX = 2.8;

    public static final double FLOW_INTERVAL = 0.25, FLOW_LIFE = 1.8;

    private static final EntityStormWing.Shape SHAPE =
            new EntityStormWing.Shape(3.0, 1.5, 4.0, 0.9, 0.30, 3.5, 0.30, FLOW_INTERVAL, FLOW_LIFE,
                    0.5, 2.4, 0.1, 2.77, 0.5, 0.6, 2.0, 2.1, 0.9, 1.8, 96, 6, 0.35, 0.30);

    public static final double GUST_SPEED = 5.0;

    public static final double GUST_END_LEN = 4.0;

    public static final double PRESS_WRAP_R = 1.3;

    public static int gustArriveTicks(double dist) {
        double htMid = (HT_MIN + HT_MAX) / 2;

        double span = Math.max(0, dist);
        double perSec = htMid * GUST_SPEED / FLOW_LIFE;
        double sec = span / perSec;

        return Math.max(3, Math.min(40, (int) Math.ceil(sec * 20) + 1));
    }

    public final EntityStormWing.Tornado[][] wings = new EntityStormWing.Tornado[2][];

    private final Context<?> ctx;

    private Player owner() {
        return ctx.player;
    }

    public Vec3 boneOrigin = Vec3.ZERO;
    public Vec3 boneLeft = new Vec3(1, 0, 0), boneUp = new Vec3(0, 1, 0), boneFront = new Vec3(0, 0, 1);
    public int boneFrame = -1;

    @Override
    public void storeBone(Vec3 origin, Vec3 left, Vec3 up, Vec3 front, int frame) {
        boneOrigin = origin;
        boneLeft = left;
        boneUp = up;
        boneFront = front;
        boneFrame = frame;
    }

    public double alpha = 0;

    public double grow = 0;

    public double sharp = 0;

    public boolean white = false;

    private int morphTick = 0;

    public static final int MORPH_TICK = 22;

    public double morph = 0;

    public static final double MORPH_SPLIT = 0.30;

    public static final double MORPH_FLOW_SPEED = 3.2;

    private static final int MOTE_PER_TICK = 3;

    public final net.minecraft.world.phys.Vec3[] pressFoot = new net.minecraft.world.phys.Vec3[2];
    public final double[] pressExtend = new double[2];

    public final EntityStormWing.Tornado[] pressWing = new EntityStormWing.Tornado[2];

    private void tickPressWings() {
        for (int side = 0; side < 2; side++) {
            if (pressFoot[side] == null) {
                pressWing[side] = null;
                continue;
            }
            if (pressWing[side] == null) {

                EntityStormWing.Tornado src = wings[side][0];
                pressWing[side] = new EntityStormWing.Tornado(
                        src.height(), src.size(), src.dscale(), 0,
                        src.tx, src.ty, src.tz, src.rx, src.ry, src.rz, src.shape());
            }
            pressWing[side].flowTick();
        }
    }

    public boolean pressWrapped() {
        for (EntityStormWing.Tornado t : pressWing) {
            if (t != null && t.extrudeReachedMain()) {
                return true;
            }
        }
        return false;
    }

    public boolean gusting() {
        return pressFoot[0] != null || pressFoot[1] != null;
    }

    public double lastFeedTime = GameTimer.getPausableTime();

    private boolean terminated = false;
    private int terminateTick = 0;
    private int growTick = 0;

    public EntityDualWing(Player owner, Context<?> ctx) {
        super(ACEntities.DUAL_WING.get(), owner.level());
        this.ctx = ctx;

        noCulling = true;

        wings[0] = buildSide(+1);
        wings[1] = buildSide(-1);

        syncToPlayer();
        if (level().isClientSide) {
            BodyBones.register(owner.getUUID(), this);
        }
    }

    private static EntityStormWing.Tornado[] buildSide(int sx) {
        EntityStormWing.Tornado[] out = new EntityStormWing.Tornado[PER_SIDE];
        for (int i = 0; i < PER_SIDE; i++) {

            double t = PER_SIDE > 1 ? i / (double) (PER_SIDE - 1) : 0.28;

            double ht = RandUtils.ranged(HT_MIN, HT_MAX);
            double sz = RandUtils.ranged(SZ_MIN, SZ_MAX);
            double dscale = RandUtils.ranged(DS_MIN, DS_MAX);

            double tx = (0.12 + RandUtils.ranged(-0.05, 0.10)) * sx;
            double ty = 0.07 + RandUtils.ranged(-0.10, 0.10);
            double tz = -0.35 + RandUtils.ranged(-0.05, 0.05);

            double rz = -(30 + 72 * t + RandUtils.ranged(-6, 6)) * sx;

            double rx = RandUtils.ranged(-20, -10);
            double ry = RandUtils.ranged(-6, 6);

            out[i] = new EntityStormWing.Tornado(ht, sz, dscale, 0, tx, ty, tz, rx, ry, rz, SHAPE);
        }
        return out;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (level().isClientSide) {
            BodyBones.unregister(owner().getUUID(), this);

            cn.academy.client.render.misc.WingWind.remove(owner().getUUID());
        }
        super.remove(reason);
    }

    public Player getOwner() {
        return owner();
    }

    public void touch() {
        lastFeedTime = GameTimer.getPausableTime();
    }

    public void snapTo(boolean whiteNow, boolean sharpOn) {
        this.white = whiteNow;
        this.sharp = sharpOn ? 1.0 : 0.0;
        this.morphTick = whiteNow ? MORPH_TICK : 0;
        this.morph = whiteNow ? 1.0 : 0.0;
        this.growTick = GROW_TICK;
        this.grow = 1.0;
    }

    public void setForm(boolean white, boolean sharpOn) {
        this.white = white;

        double target = sharpOn ? 1.0 : 0.0;
        this.sharp = sharp + Math.max(-0.18, Math.min(0.18, target - sharp));
    }

    @Override
    public void tick() {
        super.tick();
        syncToPlayer();

        if (level().isClientSide) {

            boolean spawning = morph <= MORPH_SPLIT;

            double speed = 1.0 + (MORPH_FLOW_SPEED - 1.0) * morph;
            for (EntityStormWing.Tornado[] side : wings) {
                for (EntityStormWing.Tornado t : side) {
                    t.setSpawning(spawning);
                    t.setFlowSpeed(speed);
                    t.flowTick();
                }
            }
            publishWingWind();
            spawnFeatherMotes();
            tickFlapSound();
            tickPressWings();
        }

        if (ctx.getStatus() == Context.Status.TERMINATED) {
            terminated = true;
        }
        if (terminated && ++terminateTick > TERMINATE_TICK) {
            discard();
            return;
        }
        if (!terminated && growTick < GROW_TICK) {
            growTick++;
        }

        double g = growTick / (double) GROW_TICK;
        grow = 1 - (1 - g) * (1 - g);

        int want = white ? 1 : -1;
        morphTick = Math.max(0, Math.min(MORPH_TICK, morphTick + want));
        morph = morphTick / (double) MORPH_TICK;

        alpha = terminated
                ? MAX_ALPHA * (1 - terminateTick / (double) TERMINATE_TICK)
                : MAX_ALPHA * Math.min(1, g * 1.6);
    }

    private final double[] moteTmp = new double[3];

    private static final int BONE_STALE_FRAMES = 20;

    private long lastFlapCycle = Long.MIN_VALUE;

    private void tickFlapSound() {
        if (morph <= MORPH_SPLIT || grow < 0.35 || terminated) {
            lastFlapCycle = Long.MIN_VALUE;
            return;
        }
        long c = cn.academy.client.render.entity.FeatherWing.flapCycle();
        if (c == lastFlapCycle) {
            return;
        }
        boolean first = lastFlapCycle == Long.MIN_VALUE;
        lastFlapCycle = c;
        if (first) {

            return;
        }
        level().playLocalSound(getX(), getY(), getZ(),
                cn.academy.ACSounds.VM_WING_FLAP.get(),
                net.minecraft.sounds.SoundSource.MASTER, FLAP_VOLUME, 1.0f, false);
    }

    private static final float FLAP_VOLUME = 0.55f;

    private boolean wingIdle() {
        if (morph < 1.0 || grow < 0.35 || terminated || boneFrame < 0
                || cn.academy.client.render.MagLimbBones.frame() - boneFrame > BONE_STALE_FRAMES) {
            return true;
        }
        return cn.academy.client.render.LocalVisibility.hiddenFromLocalPlayer(owner());
    }

    private void publishWingWind() {
        if (wingIdle()) {
            return;
        }

        cn.academy.client.render.misc.WingWind.publish(
                owner().getUUID(), grow, level().getGameTime(),
                boneOrigin, boneLeft, boneUp, boneFront, ROOT_Y);
    }

    private void spawnFeatherMotes() {

        if (wingIdle()) {
            return;
        }

        int tips = cn.academy.client.render.entity.FeatherWing.primaryCount(grow);
        for (int i = 0; i < MOTE_PER_TICK; i++) {
            int side = RandUtils.nextInt(2);
            cn.academy.client.render.entity.FeatherWing.primaryTip(
                    side, RandUtils.nextInt(tips), grow, moteTmp);

            double lx = moteTmp[0];
            double ly = moteTmp[1] + ROOT_Y;
            double lz = moteTmp[2];

            Vec3 p = boneOrigin
                    .add(boneLeft.scale(lx))
                    .add(boneUp.scale(ly))
                    .add(boneFront.scale(lz));

            double vx = RandUtils.rangef(-0.045f, 0.045f);
            double vy = RandUtils.rangef(-0.020f, 0.010f);
            double vz = RandUtils.rangef(-0.045f, 0.045f);
            level().addParticle(cn.academy.ACParticles.FEATHER.get(),
                    p.x, p.y, p.z, vx, vy, vz);
        }
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
