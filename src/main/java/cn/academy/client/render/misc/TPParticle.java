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
public class TPParticle extends TextureSheetParticle {

    private static final int FADE_START = 20;
    private static final int FADE_TICKS = 20;

    private final float baseAlpha;

    protected TPParticle(ClientLevel level, double x, double y, double z,
                         double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z, vx, vy, vz);

        xd = vx;
        yd = vy;
        zd = vz;
        quadSize = RandUtils.rangef(0.05f, 0.1f);
        baseAlpha = RandUtils.rangef(0.6f, 0.8f);
        alpha = baseAlpha;
        lifetime = FADE_START + FADE_TICKS;
        gravity = 0;
        hasPhysics = false;
        pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (age > FADE_START) {
            alpha = baseAlpha * Math.max(0, 1 - (age - FADE_START) / (float) FADE_TICKS);
        }
    }

    @Override
    protected int getLightColor(float pt) {
        return 0xF000F0;
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
            return new TPParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}
