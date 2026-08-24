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
public class MdParticle extends TextureSheetParticle {

    private static final int FADE_TICKS = 20;

    private final float baseAlpha;

    private final int fadeStart;

    protected MdParticle(ClientLevel level, double x, double y, double z,
                         double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z, vx, vy, vz);

        xd = vx;
        yd = vy;
        zd = vz;
        quadSize = RandUtils.rangef(0.025f, 0.035f);
        baseAlpha = RandUtils.rangei(76, 152) / 255f;
        alpha = baseAlpha;
        fadeStart = RandUtils.rangei(25, 55);
        lifetime = fadeStart + FADE_TICKS;
        gravity = 0;
        hasPhysics = false;
        pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (age > fadeStart) {
            alpha = baseAlpha * Math.max(0, 1 - (age - fadeStart) / (float) FADE_TICKS);
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
            return new MdParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}
