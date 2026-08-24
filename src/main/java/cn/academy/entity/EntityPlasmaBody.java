package cn.academy.entity;

import cn.academy.ACEntities;
import cn.academy.ability.context.Context;
import cn.lambdalib2.util.GameTimer;
import cn.lambdalib2.util.MathUtils;
import cn.lambdalib2.util.RandUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EntityPlasmaBody extends Entity implements cn.academy.client.render.ACEffect {

    public static final int MAX_BALLS = 10;

    public static final class TrigPar {
        public final float amp, speed, dphase;

        TrigPar(float amp, float speed, float dphase) {
            this.amp = amp;
            this.speed = speed;
            this.dphase = dphase;
        }

        public float phase(float time) {
            return speed * time - dphase;
        }
    }

    public static final class BallInst {
        public final float size;
        public final float cx, cy, cz;
        public final TrigPar hmove, vmove;

        BallInst(float size, float cx, float cy, float cz, TrigPar hmove, TrigPar vmove) {
            this.size = size;
            this.cx = cx;
            this.cy = cy;
            this.cz = cz;
            this.hmove = hmove;
            this.vmove = vmove;
        }
    }

    public final BallInst[] balls;

    private final Context<?> ctx;

    private final double bornTime = GameTimer.getPausableTime();

    private double lastAlphaTime = GameTimer.getPausableTime();

    public double alpha = 0;

    public float scale = 1f;

    public double lastFeedTime = GameTimer.getPausableTime();

    public EntityPlasmaBody(net.minecraft.world.level.Level level, Context<?> ctx) {
        super(ACEntities.PLASMA_BODY.get(), level);
        this.ctx = ctx;
        noCulling = true;

        java.util.List<BallInst> out = new java.util.ArrayList<>();
        for (int i = 0; i < 4; i++) {
            out.add(new BallInst(RandUtils.rangef(2.5f, 3.5f),
                    RandUtils.rangef(-1.1f, 1.1f), RandUtils.rangef(-1.1f, 1.1f), RandUtils.rangef(-1.1f, 1.1f),
                    nextTrigPar(1.0f), nextTrigPar(1.0f)));
        }
        int small = RandUtils.rangei(4, 6);
        for (int i = 0; i < small; i++) {
            out.add(new BallInst(RandUtils.rangef(0.23f, 0.7f),
                    RandUtils.rangef(-2.2f, 2.2f), RandUtils.rangef(-2.2f, 2.2f), RandUtils.rangef(-2.2f, 2.2f),
                    nextTrigPar(2.5f), nextTrigPar(2.5f)));
        }
        balls = out.toArray(new BallInst[0]);
    }

    private static TrigPar nextTrigPar(float size) {
        return new TrigPar(RandUtils.rangef(1.4f, 2f) * size,
                RandUtils.rangef(0.5f, 0.7f),
                RandUtils.rangef(0, MathUtils.PI_F * 2));
    }

    public void touch() {
        lastFeedTime = GameTimer.getPausableTime();
    }

    public float animTime() {
        return (float) (GameTimer.getPausableTime() - bornTime);
    }

    public net.minecraft.world.phys.Vec3 ballOffset(int i) {
        BallInst b = balls[i];
        float t = animTime();
        float hr = b.hmove.phase(t);
        float vt = b.vmove.phase(t);
        return new net.minecraft.world.phys.Vec3(
                b.cx + b.hmove.amp * net.minecraft.util.Mth.sin(hr),
                b.cy + b.vmove.amp * net.minecraft.util.Mth.sin(vt),
                b.cz + b.hmove.amp * net.minecraft.util.Mth.cos(hr));
    }

    public int firstSmallBall() {
        return Math.min(4, balls.length);
    }

    public void updateAlpha() {
        double now = GameTimer.getPausableTime();
        double dt = now - lastAlphaTime;
        lastAlphaTime = now;

        boolean terminated = ctx.getStatus() == Context.Status.TERMINATED;
        double desired = terminated ? 0 : 1;
        double max = dt * (terminated ? 1.0 : 0.3);
        double delta = desired - alpha;
        alpha += Math.min(Math.abs(delta), max) * Math.signum(delta);
    }

    @Override
    public void tick() {
        super.tick();

        if (ctx.getStatus() == Context.Status.TERMINATED && Math.abs(alpha) <= 1.0e-3) {
            discard();
        }
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
