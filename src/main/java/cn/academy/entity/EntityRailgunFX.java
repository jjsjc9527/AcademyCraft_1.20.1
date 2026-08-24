package cn.academy.entity;

import cn.academy.client.render.util.ArcFactory;
import cn.academy.client.render.util.ArcFactory.Arc;
import cn.academy.client.render.util.SubArcHandler;
import cn.lambdalib2.util.RandUtils;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EntityRailgunFX extends EntityRayBase {

    static final int ARC_SIZE = 24;

    private static final double ARC_DENSITY = 12.0;
    private static final double RADIUS_MIN = 0.35, RADIUS_MAX = 0.6;
    private static final double SCALE_MIN = 0.1, SCALE_MAX = 0.6;

    private static final Arc[] TEMPLATES = new Arc[ARC_SIZE];

    static {
        ArcFactory factory = new ArcFactory();
        factory.widthShrink = 0.9;
        factory.maxOffset = 0.8;
        factory.passes = 3;
        factory.width = 0.3;
        factory.branchFactor = 0.7;
        factory.thickness = 0.5;

        for (int i = 0; i < ARC_SIZE; ++i) {

            TEMPLATES[i] = factory.generate(RandUtils.ranged(2.5, 3.5));
        }
    }

    public final SubArcHandler arcHandler = new SubArcHandler(TEMPLATES);

    private final InteractionHand hand;

    public net.minecraft.core.Direction gravAtFire = net.minecraft.core.Direction.DOWN;
    public float localYawAtFire = 0;

    public EntityRailgunFX(Player player, double length, InteractionHand hand) {
        super(player);
        this.hand = hand;

        this.gravAtFire = cn.academy.gravity.ACGravity.getGravityDirection(player);
        this.localYawAtFire = player.getYRot();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0f);
        setPos(eye.x, eye.y, eye.z);
        double dxz = Math.sqrt(look.x * look.x + look.z * look.z);
        setYRot((float) (-Math.atan2(look.x, look.z) * 180 / Math.PI));
        setXRot((float) (-Math.atan2(look.y, dxz) * 180 / Math.PI));

        this.life = 50;
        this.blendInTime = 150;

        this.widthShrinkTime = 300;
        this.widthWiggleRadius = 0.3;
        this.maxWiggleSpeed = 0.8;
        this.blendOutTime = 400;
        this.length = length;

        noCulling = true;

        spawnArcs();
    }

    public void aimAt(Vec3 from, Vec3 direction, double newLength) {
        Vec3 d = direction.normalize();

        this.viewOptimize = false;
        clearPath();
        setPos(from.x, from.y, from.z);

        setYRot((float) (-Math.atan2(d.x, d.z) * 180 / Math.PI));
        setXRot((float) (-Math.atan2(d.y, d.horizontalDistance()) * 180 / Math.PI));
        this.length = newLength;

        arcHandler.clear();
        spawnArcs();
    }

    public void bendAlong(java.util.List<Vec3> worldPoints) {
        setPath(worldPoints);
        arcHandler.clear();
        spawnArcs();
    }

    private void spawnArcs() {
        double len = this.length;
        int count = (int) Math.max(1, len * ARC_DENSITY);
        for (int i = 0; i < count; ++i) {

            double cur = RandUtils.ranged(1.0, Math.max(1.5, len));
            float theta = RandUtils.rangef(0, (float) (Math.PI * 2));
            double r = RandUtils.ranged(RADIUS_MIN, RADIUS_MAX);

            Vec3 base = pointAt(cur);
            Vec3 dir = dirAt(cur);

            Vec3 ref = Math.abs(dir.y) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
            Vec3 u = dir.cross(ref).normalize();
            Vec3 v = dir.cross(u).normalize();
            Vec3 p = base.add(u.scale(Math.cos(theta) * r)).add(v.scale(Math.sin(theta) * r));

            var sa = arcHandler.generateAt(p);
            sa.scale = RandUtils.ranged(SCALE_MIN, SCALE_MAX);
            sa.life = this.life;
        }
    }

    private Vec3 pointAt(double s) {
        Vec3[] path = getPath();
        double[] cum = getPathCum();
        if (path == null || cum == null) {
            return lookVec(getYRot(), getXRot()).scale(s);
        }
        for (int i = 0; i + 1 < path.length; i++) {
            if (s <= cum[i + 1] || i + 2 == path.length) {
                Vec3 seg = path[i + 1].subtract(path[i]);
                double segLen = seg.length();
                double t = segLen < 1e-8 ? 0 : Math.min(s - cum[i], segLen);
                return path[i].add(seg.normalize().scale(t));
            }
        }
        return path[path.length - 1];
    }

    private Vec3 dirAt(double s) {
        Vec3[] path = getPath();
        double[] cum = getPathCum();
        if (path == null || cum == null) {
            return lookVec(getYRot(), getXRot());
        }
        for (int i = 0; i + 1 < path.length; i++) {
            if (s <= cum[i + 1] || i + 2 == path.length) {
                Vec3 seg = path[i + 1].subtract(path[i]);
                return seg.lengthSqr() < 1e-8 ? lookVec(getYRot(), getXRot()) : seg.normalize();
            }
        }
        return lookVec(getYRot(), getXRot());
    }

    private static Vec3 lookVec(float yaw, float pitch) {
        float f = pitch * Mth.DEG_TO_RAD;
        float f1 = -yaw * Mth.DEG_TO_RAD;
        float cosF = Mth.cos(f);
        return new Vec3(Mth.sin(f1) * cosF, -Mth.sin(f), Mth.cos(f1) * cosF);
    }

    @Override
    public HumanoidArm getArm() {
        HumanoidArm main = getPlayer().getMainArm();
        return hand == InteractionHand.MAIN_HAND ? main : main.getOpposite();
    }

    @Override
    public void tick() {
        super.tick();

        arcHandler.tick();
    }
}
