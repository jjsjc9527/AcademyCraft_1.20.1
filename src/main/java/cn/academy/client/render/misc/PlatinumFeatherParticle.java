package cn.academy.client.render.misc;

import cn.academy.ability.vanilla.vecmanip.advanced.PlatinumFeatherFx;
import cn.academy.ability.vanilla.vecmanip.advanced.WhiteFeatherField;
import cn.lambdalib2.util.RandUtils;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class PlatinumFeatherParticle extends TextureSheetParticle {

    private static final int FADE_TICKS = 16;

    private static final int SETTLE_TICKS = 4;
    private static final float GROUND_LIFT = 0.012f;

    private static final double SWAY_TILT = 0.55;

    private double landY;

    private final int ownerId;

    private final int featherId;

    private final boolean empowered;

    private double baseX, baseZ, phase;

    private final float rollSpeed;

    private final float baseRoll;

    private int settleAge = -1;
    private float settleYaw;

    private final float settleTilt;

    private int swayBase;

    private boolean launching;

    private int targetId = -1;

    private boolean recoiling;

    private int recoilBase;

    private double rx, ry, rz;

    private double bx, by, bz;

    private double ax, ay, az;

    private boolean anchored;

    private static final double[] DIR = new double[3];

    public PlatinumFeatherParticle(ClientLevel level, double x, double y, double z,
                                   int ownerId, int featherId, double landY,
                                   boolean empowered, SpriteSet sprites) {
        super(level, x, y, z, 0.0, 0.0, 0.0);
        this.landY = landY;
        this.ownerId = ownerId;
        this.featherId = featherId;
        this.empowered = empowered;
        this.baseX = x;
        this.baseZ = z;
        this.phase = WhiteFeatherField.phaseOf(x, y, z);

        xd = 0.0;
        yd = -WhiteFeatherField.fallSpeedOf(empowered, PlatinumFeatherFx.empowerFallMul());
        zd = 0.0;
        gravity = 0.0f;
        friction = 1.0f;
        hasPhysics = false;

        quadSize = (float) WhiteFeatherField.detRange(featherId, 1, 0.085, 0.17);

        settleYaw = (float) (WhiteFeatherField.detOf(featherId, 2) * Math.PI * 2);
        baseRoll = (float) (WhiteFeatherField.detOf(featherId, 3) * Math.PI * 2);
        roll = oRoll = baseRoll;

        rollSpeed = (float) WhiteFeatherField.detRange(featherId, 4, -0.025, 0.025);

        settleTilt = (float) WhiteFeatherField.detRange(featherId, 5, -0.16, 0.16);

        setColor(1.0f, 1.0f, 1.0f);
        pickSprite(sprites);

        lifetime = PlatinumFeatherFx.lifetime();
    }

    public void launch(int tid) {

        if (!launching) {
            lifetime -= PlatinumFeatherFx.shotCost();
        }
        targetId = tid;
        recoiling = false;
        launching = true;
        settleAge = -1;

        bx = x;
        by = y;
        bz = z;
        anchored = false;
        rx = 0.0;
        ry = 0.0;
        rz = 0.0;
    }

    private void advanceAnchor(double cx, double cy, double cz) {
        if (!anchored) {
            anchored = true;
            ax = cx;
            ay = cy;
            az = cz;
            return;
        }
        ax += (cx - ax) * WhiteFeatherField.FEATHER_CHASE;
        ay += (cy - ay) * WhiteFeatherField.FEATHER_CHASE;
        az += (cz - az) * WhiteFeatherField.FEATHER_CHASE;
    }

    private void applySpiral() {

        double dx = bx - ax, dy = by - ay, dz = bz - az;
        double fade = WhiteFeatherField.spiralFadeAt(Math.sqrt(dx * dx + dy * dy + dz * dz));
        if (fade <= 0.0) {
            setPos(bx, by, bz);
            return;
        }
        if (rx == 0.0 && ry == 0.0 && rz == 0.0) {
            WhiteFeatherField.recoilDirOf(featherId, dx, dy, dz, DIR);
            rx = DIR[0];
            ry = DIR[1];
            rz = DIR[2];
        }
        WhiteFeatherField.spiralOffset(rx, ry, rz, age * WhiteFeatherField.SPIRAL_OMEGA, DIR);
        setPos(bx + DIR[0] * fade, by + DIR[1] * fade, bz + DIR[2] * fade);
    }

    public void dropFrom(double newLandY) {
        targetId = -1;
        recoiling = false;
        launching = false;
        landY = newLandY;
        baseX = x;
        baseZ = z;
        phase = WhiteFeatherField.phaseOf(x, y, z);
        swayBase = age;
        yd = -WhiteFeatherField.fallSpeedOf(empowered, PlatinumFeatherFx.empowerFallMul());
        settleAge = y <= landY + 1.0e-6 ? 0 : -1;

    }

    @Override
    public void tick() {
        oRoll = roll;

        if (!PlatinumFeatherFx.isActive(ownerId)) {
            remove();
            return;
        }

        if (launching) {
            tickLaunch();
            return;
        }

        if (settleAge < 0) {

            double next = y + yd;
            if (next <= landY) {
                yd = landY - y;
            }
        }

        super.tick();
        if (removed) {
            return;
        }

        if (settleAge < 0) {

            int sa = age - swayBase;
            double damp = WhiteFeatherField.swayDamp(y, landY, sa);
            setPos(baseX + WhiteFeatherField.swayX(phase, sa, damp), y,
                    baseZ + WhiteFeatherField.swayZ(phase, sa, damp));

            if (y <= landY + 1.0e-6) {
                settleAge = 0;
                settleYaw = roll;
                yd = 0.0;
            } else {

                roll = baseRoll
                        + (float) (Math.sin(WhiteFeatherField.swayAngle(phase, sa)) * SWAY_TILT)
                        + rollSpeed * sa;
            }
        } else if (settleAge < SETTLE_TICKS) {
            settleAge++;
        }

        int left = lifetime - age;
        if (left < FADE_TICKS) {
            alpha = Math.max(0.0f, left / (float) FADE_TICKS);
        }
    }

    private void tickLaunch() {
        xo = x;
        yo = y;
        zo = z;
        if (age++ >= lifetime) {
            remove();
            return;
        }

        net.minecraft.world.entity.Entity te =
                targetId < 0 || level == null ? null : level.getEntity(targetId);
        if (!(te instanceof net.minecraft.world.entity.LivingEntity tgt)
                || WhiteFeatherField.isDown(tgt)) {
            roll += rollSpeed;
        } else {

            net.minecraft.world.phys.AABB tb = tgt.getBoundingBox();
            advanceAnchor((tb.minX + tb.maxX) * 0.5,
                    (tb.minY + tb.maxY) * 0.5, (tb.minZ + tb.maxZ) * 0.5);
            if (recoiling) {

                int t = age - recoilBase;
                if (t >= WhiteFeatherField.RECOIL_TICKS) {
                    recoiling = false;
                } else {
                    double d = WhiteFeatherField.recoilDistAt(t);
                    bx = ax + rx * d;
                    by = ay + ry * d;
                    bz = az + rz * d;
                    applySpiral();
                    roll += rollSpeed * 3.0f;
                }
            } else {

                double dx = ax - bx, dy = ay - by, dz = az - bz;
                double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                double sp = PlatinumFeatherFx.launchSpeed();
                double inv = Math.min(1.0, sp / Math.max(dist, 1.0e-4));
                bx += dx * inv;
                by += dy * inv;
                bz += dz * inv;
                applySpiral();
                roll += rollSpeed * 6.0f;
                if (dist <= sp) {

                    WhiteFeatherField.recoilDirOf(featherId, dx, dy, dz, DIR);
                    rx = DIR[0];
                    ry = DIR[1];
                    rz = DIR[2];
                    recoiling = true;
                    recoilBase = age;
                }
            }
        }

        int left = lifetime - age;
        if (left < FADE_TICKS) {
            alpha = Math.max(0.0f, left / (float) FADE_TICKS);
        }
    }

    @Override
    public void remove() {
        super.remove();
        PlatinumFeatherFx.unregister(featherId);
    }

    @Override
    public void render(VertexConsumer buf, Camera camera, float pt) {
        net.minecraft.world.phys.Vec3 cam = camera.getPosition();
        float px = (float) (Mth.lerp(pt, xo, x) - cam.x());
        float lift = settleAge >= 0
                ? GROUND_LIFT * Math.min(1f, (settleAge + pt) / SETTLE_TICKS) : 0f;
        float py = (float) (Mth.lerp(pt, yo, y) - cam.y()) + lift;
        float pz = (float) (Mth.lerp(pt, zo, z) - cam.z());

        Quaternionf q;
        if (settleAge < 0) {
            q = new Quaternionf(camera.rotation());
            q.rotateZ(Mth.lerp(pt, oRoll, roll));
        } else {
            float f = Math.min(1f, (settleAge + pt) / SETTLE_TICKS);
            Quaternionf flying = new Quaternionf(camera.rotation()).rotateZ(Mth.lerp(pt, oRoll, roll));

            Quaternionf lying = new Quaternionf()
                    .rotationYXZ(settleYaw, (float) (Math.PI / 2.0) + settleTilt, 0f);
            q = flying.slerp(lying, f);
        }

        Vector3f[] quad = {
                new Vector3f(-1, -1, 0), new Vector3f(-1, 1, 0),
                new Vector3f(1, 1, 0), new Vector3f(1, -1, 0)};
        float size = getQuadSize(pt);
        for (Vector3f v : quad) {
            v.rotate(q);
            v.mul(size);
            v.add(px, py, pz);
        }

        float u0 = getU0(), u1 = getU1(), v0 = getV0(), v1 = getV1();
        emit(buf, quad[0], u1, v1);
        emit(buf, quad[1], u1, v0);
        emit(buf, quad[2], u0, v0);
        emit(buf, quad[3], u0, v1);
    }

    private static final int LIGHT = 0xF000F0;

    private void emit(VertexConsumer buf, Vector3f v, float u, float vt) {
        buf.vertex(v.x(), v.y(), v.z()).uv(u, vt)
                .color(rCol, gCol, bCol, alpha).uv2(LIGHT).endVertex();
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        public Provider(SpriteSet sprites) {
            PlatinumFeatherFx.setSprites(sprites);
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double vx, double vy, double vz) {
            return null;
        }
    }
}
