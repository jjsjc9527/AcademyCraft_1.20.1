package cn.academy.entity;

import cn.lambdalib2.util.GameTimer;
import cn.lambdalib2.util.RandUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EntityRayBase extends net.minecraft.world.entity.Entity implements IRay, cn.academy.client.render.ACEffect {

    private Player spawner;

    public int life = 30;

    public final double spawnTime = cn.lambdalib2.util.GameTimer.getPausableTime();

    public long blendInTime = 100;
    public long blendOutTime = 300;
    public long widthShrinkTime = 300;

    public double length = 15.0;

    public double widthWiggleRadius = 0.1;
    public double maxWiggleSpeed = 0.4;
    public double widthWiggle = 0.0;

    public double glowWiggleRadius = 0.1;
    public double maxGlowWiggleSpeed = 0.4;
    public double glowWiggle = 0.0;

    public boolean viewOptimize = true;

    private double lastFrame = 0;
    private final double creationTime;

    public EntityRayBase(Player player) {
        this(player.level());
        spawner = player;
    }

    public EntityRayBase(Level level) {
        this(cn.academy.ACEntities.RAILGUN_FX.get(), level);
    }

    protected EntityRayBase(net.minecraft.world.entity.EntityType<? extends EntityRayBase> type, Level level) {
        super(type, level);
        creationTime = GameTimer.getPausableTime();
        noCulling = true;
    }

    public void setFromTo(Vec3 from, Vec3 to) {
        setFromTo(from.x, from.y, from.z, to.x, to.y, to.z);
    }

    public void setFromTo(double x0, double y0, double z0, double x1, double y1, double z1) {
        setPos(x0, y0, z0);
        xOld = x0;
        yOld = y0;
        zOld = z0;

        double dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        double dxzsq = dx * dx + dz * dz;
        setYRot((float) (-Math.atan2(dx, dz) * 180 / Math.PI));
        setXRot((float) (-Math.atan2(dy, Math.sqrt(dxzsq)) * 180 / Math.PI));

        length = Math.sqrt(dxzsq + dy * dy);
    }

    private Vec3[] path;

    private double[] pathCum;

    public double bendRadius = 0;

    private static final double BEND_MAX_EAT = 0.45;

    private static final double BEND_DEG_PER_DIV = 12.0;

    public void setPath(java.util.List<Vec3> worldPoints) {
        if (worldPoints == null || worldPoints.size() < 2) {
            return;
        }

        java.util.List<Vec3> pts = new java.util.ArrayList<>();
        for (Vec3 p : worldPoints) {
            if (pts.isEmpty() || p.distanceToSqr(pts.get(pts.size() - 1)) > 1.0e-8) {
                pts.add(p);
            }
        }
        if (pts.size() < 2) {
            return;
        }

        if (bendRadius > 0) {
            pts = roundCorners(pts, bendRadius);
        }

        Vec3 origin = pts.get(0);
        path = new Vec3[pts.size()];
        pathCum = new double[pts.size()];
        path[0] = Vec3.ZERO;
        pathCum[0] = 0;
        for (int i = 1; i < pts.size(); i++) {
            path[i] = pts.get(i).subtract(origin);
            pathCum[i] = pathCum[i - 1] + pts.get(i).distanceTo(pts.get(i - 1));
        }

        setPos(origin.x, origin.y, origin.z);
        xOld = origin.x;
        yOld = origin.y;
        zOld = origin.z;

        Vec3 d = pts.get(1).subtract(origin);
        setYRot((float) (-Math.atan2(d.x, d.z) * 180 / Math.PI));
        setXRot((float) (-Math.atan2(d.y, d.horizontalDistance()) * 180 / Math.PI));

        this.length = pathCum[pathCum.length - 1];
    }

    private static java.util.List<Vec3> roundCorners(java.util.List<Vec3> pts, double radius) {
        if (pts.size() < 3) {
            return pts;
        }
        java.util.List<Vec3> out = new java.util.ArrayList<>();
        out.add(pts.get(0));
        for (int i = 1; i + 1 < pts.size(); i++) {
            Vec3 p = pts.get(i);
            Vec3 in = p.subtract(pts.get(i - 1));
            Vec3 outv = pts.get(i + 1).subtract(p);
            double l1 = in.length(), l2 = outv.length();
            if (l1 < 1.0e-6 || l2 < 1.0e-6) {
                out.add(p);
                continue;
            }
            Vec3 d1 = in.scale(1 / l1), d2 = outv.scale(1 / l2);

            double r = Math.min(radius, Math.min(l1, l2) * BEND_MAX_EAT);
            if (r < 1.0e-3) {
                out.add(p);
                continue;
            }

            double cos = Math.max(-1, Math.min(1, d1.dot(d2)));
            double deg = Math.toDegrees(Math.acos(cos));
            if (deg < 1.0) {
                out.add(p);
                continue;
            }
            int div = Math.max(2, (int) Math.ceil(deg / BEND_DEG_PER_DIV));

            Vec3 a = p.subtract(d1.scale(r));
            Vec3 b = p.add(d2.scale(r));
            for (int k = 0; k <= div; k++) {
                double t = (double) k / div, mt = 1 - t;
                out.add(a.scale(mt * mt).add(p.scale(2 * mt * t)).add(b.scale(t * t)));
            }
        }
        out.add(pts.get(pts.size() - 1));
        return out;
    }

    public void clearPath() {
        path = null;
        pathCum = null;
    }

    @Override
    public Vec3[] getPath() {
        return path;
    }

    @Override
    public double[] getPathCum() {
        return pathCum;
    }

    protected long getDeltaTime() {
        return (long) ((GameTimer.getPausableTime() - creationTime) * 1000);
    }

    @Override
    public void tick() {
        super.tick();

        if (tickCount >= life) {
            discard();
        }
    }

    @Override
    public double getLength() {
        long dt = getDeltaTime();
        return (dt < blendInTime ? (double) dt / blendInTime : 1) * length;
    }

    public long getLifeMS() {
        return life * 50L;
    }

    @Override
    public double getAlpha() {
        long dt = getDeltaTime();
        long lifeMS = getLifeMS();
        return dt > lifeMS - blendOutTime ? 1 - (double) (dt + blendOutTime - lifeMS) / blendOutTime : 1.0;
    }

    @Override
    public double getWidth() {
        long dt = getDeltaTime();
        long lifeMS = getLifeMS();
        return widthWiggle +
                (dt > lifeMS - widthShrinkTime ? 1 - (double) (dt + widthShrinkTime - lifeMS) / widthShrinkTime : 1.0);
    }

    @Override
    public boolean needsViewOptimize() {
        return viewOptimize;
    }

    @Override
    public double getStartFix() {
        return 0.0;
    }

    @Override
    public void onRenderTick() {
        double time = GameTimer.getPausableTime();
        if (lastFrame != 0) {
            long dt = (long) ((time - lastFrame) * 1000);
            widthWiggle += dt * RandUtils.ranged(-maxWiggleSpeed, maxWiggleSpeed) / 1000.0;
            if (widthWiggle > widthWiggleRadius) widthWiggle = widthWiggleRadius;
            if (widthWiggle < 0) widthWiggle = 0;

            glowWiggle += dt * RandUtils.ranged(-maxGlowWiggleSpeed, maxGlowWiggleSpeed) / 1000.0;
            if (glowWiggle > glowWiggleRadius) glowWiggle = glowWiggleRadius;
            if (glowWiggle < 0) glowWiggle = 0;
        }
        lastFrame = time;
    }

    @Override
    public Vec3 getRayPosition() {
        return position();
    }

    @Override
    public double getGlowAlpha() {
        return (1 - glowWiggleRadius + glowWiggle) * getAlpha();
    }

    @Override
    public Player getPlayer() {
        return spawner;
    }

    public void setSpawner(Player player) {
        this.spawner = player;
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        discard();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public boolean effectExpired(double now) {
        return life >= 0 && (now - spawnTime) * 20.0 > life + 5;
    }
}
