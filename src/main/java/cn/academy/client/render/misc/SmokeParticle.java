package cn.academy.client.render.misc;

import cn.lambdalib2.util.RandUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SmokeParticle extends TextureSheetParticle {

    private final float lifeSeconds;

    protected SmokeParticle(ClientLevel level, double x, double y, double z,
                            double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z, vx, vy, vz);

        xd = vx;
        yd = vy;
        zd = vz;
        lifeSeconds = RandUtils.rangef(0.5f, 0.7f);
        quadSize = 1.0f;
        gravity = 0;
        friction = 1.0f;
        hasPhysics = false;
        alpha = 0;

        lifetime = Math.max(1, (int) (2.0f * lifeSeconds * 20));
        pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        float dt = age / (lifeSeconds * 20f);
        if (dt <= 0.3f) {
            alpha = dt / 0.3f;
        } else if (dt <= 1.5f) {
            alpha = 1.0f;
        } else if (dt <= 2.0f) {
            alpha = 1 - (dt - 1.5f) / 0.5f;
        } else {
            alpha = 0;
        }
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
            return new SmokeParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}
