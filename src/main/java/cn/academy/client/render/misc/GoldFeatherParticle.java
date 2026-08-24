package cn.academy.client.render.misc;

import cn.academy.ability.vanilla.vecmanip.advanced.WhiteFeatherField;
import cn.academy.mixin.client.ParticleAccessor;
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
public class GoldFeatherParticle extends TextureSheetParticle {

    private static final int HOVER_TICKS = 20;

    private static final int RELEASE_TICKS = 8;

    public static final double OMEGA = 0.20;

    private static final float FALL_GRAVITY = 0.10f;

    private static final float FALL_FRICTION = 0.94f;

    private static final int FADE_TICKS = 20;

    private final double swayFreq;
    private final double swayAmp;
    private final double swayPhase;

    private final float rollSpeed;

    private int settleAge = -1;

    private boolean landedOnce;

    private float settleYaw;
    private static final int SETTLE_TICKS = 4;

    private static final float GROUND_LIFT = 0.012f;

    protected GoldFeatherParticle(ClientLevel level, double x, double y, double z,
                                  double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z, 0.0, 0.0, 0.0);

        xd = vx;
        yd = vy;
        zd = vz;

        quadSize = (float) RandUtils.rangef(0.085f, 0.135f);

        gravity = 0.0f;
        friction = 1.0f;

        hasPhysics = true;

        lifetime = HOVER_TICKS + RELEASE_TICKS + RandUtils.rangei(55, 85);

        settleYaw = (float) (RandUtils.nextDouble() * Math.PI * 2);
        roll = oRoll = (float) (RandUtils.nextDouble() * Math.PI * 2);

        rollSpeed = (float) RandUtils.rangef(-0.06f, 0.06f);

        swayFreq = RandUtils.rangef(0.14f, 0.32f);
        swayAmp = RandUtils.rangef(0.0016f, 0.0034f);
        swayPhase = RandUtils.nextDouble() * Math.PI * 2;

        setColor(1.0f, 1.0f, 1.0f);
        pickSprite(sprites);
    }

    @Override
    public void tick() {
        oRoll = roll;

        if (settleAge < 0) {

            if (!landedOnce && age < HOVER_TICKS + RELEASE_TICKS) {

                float t = age < HOVER_TICKS
                        ? 0.0f
                        : (age - HOVER_TICKS) / (float) RELEASE_TICKS;

                double w = OMEGA * (1.0 - t);
                double cos = Math.cos(w);
                double sin = Math.sin(w);
                double nx = xd * cos - zd * sin;
                double nz = xd * sin + zd * cos;
                xd = nx;
                zd = nz;

                gravity = FALL_GRAVITY * t;
                friction = Mth.lerp(t, 1.0f, FALL_FRICTION);
            } else {

                double a = (age + swayPhase) * swayFreq;
                xd += Math.cos(a) * swayAmp;
                zd += Math.sin(a) * swayAmp;
            }
        }

        super.tick();
        if (removed) {
            return;
        }

        if (settleAge < 0) {
            if (onGround) {

                settleAge = 0;
                settleYaw = roll;
                xd = 0.0;
                yd = 0.0;
                zd = 0.0;
                landedOnce = true;
            } else {
                roll += rollSpeed;
            }
        } else {
            if (settleAge < SETTLE_TICKS) {
                settleAge++;
            }

            if (WhiteFeatherField.groundCheckDue(age, hashCode())
                    && !WhiteFeatherField.hasGroundUnder(level, x, y, z)) {
                unstick();
            }
        }

        int left = lifetime - age;
        if (left < FADE_TICKS) {
            alpha = Math.max(0.0f, left / (float) FADE_TICKS);
        }
    }

    private void unstick() {
        ((ParticleAccessor) this).academy$setStoppedByCollision(false);
        xd = 0.0;
        yd = 0.0;
        zd = 0.0;
        settleAge = -1;
        gravity = FALL_GRAVITY;
        friction = FALL_FRICTION;
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
            Quaternionf lying = new Quaternionf().rotationYXZ(settleYaw, (float) (Math.PI / 2.0), 0f);
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
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new GoldFeatherParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}
