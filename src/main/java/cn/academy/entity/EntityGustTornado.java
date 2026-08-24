package cn.academy.entity;

import cn.academy.ACEntities;
import cn.lambdalib2.util.GameTimer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EntityGustTornado extends Entity implements cn.academy.client.render.ACEffect {

    private static final int FLY_TICK = 5;

    private static final int FADE_TICK = 8;

    private static final double MAX_ALPHA = 0.86;

    public static final double HT = 4.0;

    private static final double SZ = 1.5;

    private static final double DSCALE = 1.6;

    private static final EntityStormWing.Shape SHAPE =
            new EntityStormWing.Shape(2.0, 1.5, 4.0, 0.9, 0.30, 3.5, 0.30, 0.25, 1.8,
                    0.5, 2.0, 0.1, 2.77, 0, 0, 0, 0, 0, 1.8, 96, 6, 0.35, 0);

    public final EntityStormWing.Tornado tornado =
            new EntityStormWing.Tornado(HT, SZ, DSCALE, 0, 0, 0, 0, 0, 0, 0, SHAPE);

    public final EntityDualWing wing;

    public final int side;

    public final boolean press;

    public final Vec3 origin;

    public final Entity target;

    private int flyTick = 0;
    private boolean ending = false;
    private int endTick = 0;

    public double extend = 0;

    public double alpha = 0;

    public boolean white = false;

    public double lastFeedTime = GameTimer.getPausableTime();

    public EntityGustTornado(Level level, EntityDualWing wing, int side, Vec3 origin, Entity target) {
        this(level, wing, side, origin, target, false);
    }

    public EntityGustTornado(Level level, EntityDualWing wing, int side, Vec3 origin, Entity target,
                             boolean press) {
        super(ACEntities.GUST_TORNADO.get(), level);
        this.wing = wing;
        this.side = side == 0 ? 0 : 1;
        this.press = press;
        this.origin = origin;
        this.target = target;
        noCulling = true;
        setPos(origin.x, origin.y, origin.z);

        xOld = origin.x;
        yOld = origin.y;
        zOld = origin.z;
    }

    public void touch(boolean white) {
        this.lastFeedTime = GameTimer.getPausableTime();
        this.white = white;
    }

    public void end() {
        ending = true;
    }

    public Vec3 footOf(float pt) {
        if (target == null) {
            return null;
        }

        double y = net.minecraft.util.Mth.lerp(pt, target.yOld, target.getY());
        return new Vec3(
                net.minecraft.util.Mth.lerp(pt, target.xOld, target.getX()),
                press ? y + target.getBbHeight() * 0.5 : y - 0.2,
                net.minecraft.util.Mth.lerp(pt, target.zOld, target.getZ()));
    }

    public Vec3 rootNow() {
        if (wing != null && !wing.isRemoved()) {
            Vec3 tip = cn.academy.client.render.entity.DualWingRenderer.wingTipWorld(wing, side);
            if (tip != null) {
                return tip;
            }
        }
        return origin;
    }

    @Override
    public void tick() {
        super.tick();

        if (target == null || !target.isAlive() || target.isRemoved()
                || target.level() != level()) {
            ending = true;
        }

        if (!ending) {

            Vec3 root = rootNow();
            setPos(root.x, root.y, root.z);
            if (flyTick < FLY_TICK) {
                flyTick++;

                double t = flyTick / (double) FLY_TICK;
                extend = t * t * (3 - 2 * t);
            } else {
                extend = 1;
            }
        }

        tornado.flowTick();

        if (wing != null && !wing.isRemoved()) {
            Vec3 aim = ending ? null : footOf(1f);
            wing.pressFoot[side] = aim;
            wing.pressExtend[side] = extend;
        }

        if (ending) {
            if (++endTick > FADE_TICK) {
                discard();
                return;
            }
            alpha = MAX_ALPHA * (1 - endTick / (double) FADE_TICK);
        } else {

            alpha = MAX_ALPHA * Math.min(1, (flyTick + 1) / (double) FLY_TICK);
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
