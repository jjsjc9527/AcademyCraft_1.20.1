package cn.academy.entity;

import cn.academy.ACEntities;
import cn.academy.client.render.util.ArcFactory;
import cn.academy.client.render.util.ArcFactory.Arc;
import cn.lambdalib2.util.MathUtils;
import cn.lambdalib2.util.ViewOptimize;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EntityArc extends net.minecraft.world.entity.Entity
        implements ViewOptimize.IAssociatePlayer, cn.academy.client.render.ACEffect {

    static final int GEN = 20;

    static Arc[] defaultPatterns = new Arc[GEN];

    static {
        ArcFactory fac = new ArcFactory();
        for (int i = 0; i < GEN; ++i) {
            defaultPatterns[i] = fac.generate(20);
        }
    }

    public final Arc[] patterns;

    int[] iid;
    int n = 1;
    boolean show = true;

    public double
            showWiggle = .2,
            hideWiggle = .2,
            texWiggle = .5;

    public double length = 20.0;
    public boolean lengthFixed = true;

    public boolean viewOptimize = true;

    public int life = -1;

    public final double spawnTime = cn.lambdalib2.util.GameTimer.getPausableTime();

    @Override
    public boolean effectExpired(double now) {
        return life >= 0 && (now - spawnTime) * 20.0 > life + 5;
    }

    public boolean fade = false;
    public int fadeIn = 2;
    public int fadeOut = 4;

    public float alphaAt(float partialTick) {
        if (!fade || life < 0) return 1f;
        float age = tickCount + partialTick;
        float a = 1f;
        if (fadeIn > 0 && age < fadeIn) a = age / fadeIn;
        float remain = life - age;
        if (fadeOut > 0 && remain < fadeOut) a = Math.min(a, remain / fadeOut);
        return a < 0f ? 0f : (a > 1f ? 1f : a);
    }

    public int boneIndex = -1;

    public double endX, endY, endZ;

    public float colorR = 1f, colorG = 1f, colorB = 1f;

    public EntityArc setColor(float r, float g, float b) {
        colorR = r;
        colorG = g;
        colorB = b;
        return this;
    }

    private final Player player;

    public EntityArc(Player player, Arc[] patterns) {
        super(ACEntities.ARC.get(), player.level());
        this.player = player;

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0f);
        setPos(eye.x, eye.y, eye.z);
        double dxz = Math.sqrt(look.x * look.x + look.z * look.z);
        setYRot((float) (-Math.atan2(look.x, look.z) * 180 / Math.PI));
        setXRot((float) (-Math.atan2(look.y, dxz) * 180 / Math.PI));
        noCulling = true;
        iid = new int[n];

        this.patterns = patterns;
    }

    public EntityArc(Player player) {
        this(player, defaultPatterns);
    }

    public EntityArc setLife(int ticks) {
        this.life = ticks;
        return this;
    }

    @Override
    public void tick() {
        super.tick();
        for (int i = 0; i < iid.length; ++i) {
            if (random.nextDouble() < texWiggle) {
                iid[i] = random.nextInt(patterns.length);
            }
        }

        if (show && random.nextDouble() < showWiggle) {
            show = !show;
        } else if (!show && random.nextDouble() < hideWiggle) {
            show = !show;
        }

        if (life >= 0 && tickCount >= life) {
            discard();
        }
    }

    private Vec3[] path;

    private double[] pathCum;

    public Vec3[] getPath() {
        return path;
    }

    public double[] getPathCum() {
        return pathCum;
    }

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

        Vec3 origin = pts.get(0);
        path = new Vec3[pts.size()];
        pathCum = new double[pts.size()];
        path[0] = Vec3.ZERO;
        for (int i = 1; i < pts.size(); i++) {
            path[i] = pts.get(i).subtract(origin);
            pathCum[i] = pathCum[i - 1] + pts.get(i).distanceTo(pts.get(i - 1));
        }

        setPos(origin.x, origin.y, origin.z);
        Vec3 last = pts.get(pts.size() - 1);
        endX = last.x;
        endY = last.y;
        endZ = last.z;

        Vec3 d = pts.get(1).subtract(origin);
        setYRot((float) (-Math.atan2(d.x, d.z) * 180 / Math.PI));
        setXRot((float) (-Math.atan2(d.y, d.horizontalDistance()) * 180 / Math.PI));

        length = pathCum[pathCum.length - 1];
        lengthFixed = false;
    }

    public void setFromTo(double x0, double y0, double z0, double x1, double y1, double z1) {
        setPos(x0, y0, z0);
        endX = x1; endY = y1; endZ = z1;

        double dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        double dxzsq = dx * dx + dz * dz;
        setYRot((float) (-Math.atan2(dx, dz) * 180 / Math.PI));
        setXRot((float) (-Math.atan2(dy, Math.sqrt(dxzsq)) * 180 / Math.PI));

        length = MathUtils.distance(x0, y0, z0, x1, y1, z1);
    }

    public void aimFrom(double x0, double y0, double z0) {
        double dx = endX - x0, dy = endY - y0, dz = endZ - z0;
        double dxzsq = dx * dx + dz * dz;
        setYRot((float) (-Math.atan2(dx, dz) * 180 / Math.PI));
        setXRot((float) (-Math.atan2(dy, Math.sqrt(dxzsq)) * 180 / Math.PI));
        length = MathUtils.distance(x0, y0, z0, endX, endY, endZ);
    }

    public boolean isShown() {
        return show;
    }

    public int[] patternIds() {
        return iid;
    }

    public int subArcCount() {
        return n;
    }

    @Override
    public Player getPlayer() {
        return player;
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}
}
