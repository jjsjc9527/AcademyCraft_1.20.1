package cn.academy.client.render.misc;

import com.mojang.blaze3d.vertex.VertexConsumer;
import cn.lambdalib2.util.RandUtils;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class SilbarnFragParticle extends TextureSheetParticle {

    private static final float QUAD_SIZE = 0.05f;

    private static final float GRAVITY = 0.03f / 0.04f;

    private static final double SPIN_RATE = 25;

    private static final int FADE_TICKS = 10;

    private static final int SETTLE_TICKS = 3;

    private static final float GROUND_LIFT = 0.02f;

    private float yaw, pitch, prevYaw, prevPitch;

    private final float spinYaw, spinPitch;

    private int settleAge = -1;

    private float settleFrom, settleTo;

    protected SilbarnFragParticle(ClientLevel level, double x, double y, double z,
                                  double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z, vx, vy, vz);

        xd = vx;
        yd = vy;
        zd = vz;

        quadSize = QUAD_SIZE;
        gravity = GRAVITY;
        hasPhysics = true;
        lifetime = RandUtils.rangei(40, 61);

        yaw = prevYaw = RandUtils.nextFloat() * 360f;
        pitch = prevPitch = RandUtils.rangef(-90, 90);

        double phi = RandUtils.nextDouble() * Math.PI * 2;
        spinYaw = (float) (Math.sin(phi) * SPIN_RATE);
        spinPitch = (float) (Math.cos(phi) * SPIN_RATE);

        pickSprite(sprites);
    }

    @Override
    public void tick() {
        prevYaw = yaw;
        prevPitch = pitch;
        super.tick();
        if (removed) {
            return;
        }

        if (settleAge < 0) {
            if (onGround) {

                settleAge = 0;
                settleFrom = pitch;
                settleTo = nearestFlatPitch(pitch);
            } else {
                yaw += spinYaw;
                pitch += spinPitch;
            }
        } else if (settleAge < SETTLE_TICKS) {
            settleAge++;

            pitch = Mth.lerp(settleAge / (float) SETTLE_TICKS, settleFrom, settleTo);
        }

        int left = lifetime - age;
        if (left < FADE_TICKS) {
            alpha = Math.max(0, left / (float) FADE_TICKS);
        }
    }

    private static float nearestFlatPitch(float p) {
        return 90f + 180f * Math.round((p - 90f) / 180f);
    }

    @Override
    public void render(VertexConsumer buf, Camera camera, float pt) {
        Vec3 cam = camera.getPosition();
        float px = (float) (Mth.lerp(pt, xo, x) - cam.x());

        float lift = settleAge >= 0 ? GROUND_LIFT : 0f;
        float py = (float) (Mth.lerp(pt, yo, y) - cam.y()) + lift;
        float pz = (float) (Mth.lerp(pt, zo, z) - cam.z());

        Quaternionf q = new Quaternionf().rotationYXZ(
                (float) Math.toRadians(Mth.lerp(pt, prevYaw, yaw)),
                (float) Math.toRadians(Mth.lerp(pt, prevPitch, pitch)),
                0f);

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
        int light = getLightColor(pt);
        emit(buf, quad[0], u1, v1, light);
        emit(buf, quad[1], u1, v0, light);
        emit(buf, quad[2], u0, v0, light);
        emit(buf, quad[3], u0, v1, light);
    }

    private void emit(VertexConsumer buf, Vector3f v, float u, float vt, int light) {
        buf.vertex(v.x(), v.y(), v.z()).uv(u, vt)
                .color(rCol, gCol, bCol, alpha).uv2(light).endVertex();
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z, double vx, double vy, double vz) {
            return new SilbarnFragParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}
