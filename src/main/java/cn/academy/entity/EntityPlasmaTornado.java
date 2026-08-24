package cn.academy.entity;

import cn.academy.ACEntities;
import cn.academy.ability.context.Context;
import cn.lambdalib2.util.GameTimer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EntityPlasmaTornado extends Entity implements cn.academy.client.render.ACEffect {

    private static final double HT = 12, SZ = 8, DSCALE = 0.3;

    private static final double GROUND_SEARCH = 20;

    private static final int DEAD_TICK = 30, FADE_TICK = 20;

    private static final int RISE_TICK = 20;

    private static final double EXTRA_DENSITY = 3.0;

    public final EntityStormWing.Tornado tornado =
            new EntityStormWing.Tornado(HT, SZ, DSCALE, EXTRA_DENSITY, 0, 0, 0, 0, 0, 0);

    private final Context<?> ctx;

    private boolean dying = false;
    private int deadTick = 0;

    public double alpha = 0;

    public float scale = 1f;

    public float density = 0f;

    private static final double INSIDE_RADIUS_FACTOR = 0.9;

    private double radiusAt(double ny) {
        double t = Math.max(0, Math.min(1, ny));
        return (0.5 + 1.125 * t * t) * SZ * INSIDE_RADIUS_FACTOR * scale;
    }

    public boolean isCameraInside(Vec3 cam) {
        double dx = cam.x - getX();
        double dz = cam.z - getZ();
        double ny = (cam.y - getY()) / (HT * scale);
        double r = radiusAt(ny);
        return dx * dx + dz * dz <= r * r;
    }

    public int renderOrder(Vec3 cam) {
        return isCameraInside(cam) ? -1 : 1;
    }

    public double lastFeedTime = GameTimer.getPausableTime();

    private final double bornTime = GameTimer.getPausableTime();

    public EntityPlasmaTornado(Level level, Context<?> ctx, Vec3 chargePos) {
        super(ACEntities.PLASMA_TORNADO.get(), level);
        this.ctx = ctx;
        noCulling = true;

        Vec3 down = new Vec3(chargePos.x, chargePos.y - GROUND_SEARCH, chargePos.z);

        HitResult hit = level.clip(new ClipContext(chargePos, down,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null));
        Vec3 at = hit.getType() != HitResult.Type.MISS ? hit.getLocation() : down;
        setPos(at.x, at.y, at.z);
        xOld = at.x;
        yOld = at.y;
        zOld = at.z;
    }

    public void touch() {
        lastFeedTime = GameTimer.getPausableTime();
    }

    public void setDying() {
        dying = true;
    }

    @Override
    public void tick() {
        super.tick();
        if (ctx.getStatus() == Context.Status.TERMINATED) {
            dying = true;
        }
        if (dying && ++deadTick >= DEAD_TICK) {
            discard();
            return;
        }
        alpha = currentAlpha() * 0.5;
    }

    private double currentAlpha() {
        if (dying) {
            return Math.max(0, 1 - deadTick / (double) FADE_TICK);
        }
        double ticks = (GameTimer.getPausableTime() - bornTime) * 20.0;
        return ticks < RISE_TICK ? ticks / RISE_TICK : 1.0;
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

    @Override
    public int effectOrder(Vec3 camera) {
        return renderOrder(camera);
    }
}
