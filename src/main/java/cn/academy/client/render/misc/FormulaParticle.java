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
public class FormulaParticle extends TextureSheetParticle {

    private static final int FADE_IN = 2;
    private static final int FADE_TICKS = 20;

    private final int fadeStart;
    private final float baseAlpha;

    protected FormulaParticle(ClientLevel level, double x, double y, double z,
                              double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z, vx, vy, vz);

        xd = vx;
        yd = vy;
        zd = vz;
        rCol = 220 / 255f;
        gCol = 220 / 255f;
        bCol = 220 / 255f;
        quadSize = RandUtils.rangef(1.0f, 1.7f) * 0.5f;
        baseAlpha = Math.min(255, RandUtils.rangei(152, 384)) / 255f;
        alpha = 0;
        fadeStart = RandUtils.rangei(10, 15);
        lifetime = fadeStart + FADE_TICKS;
        gravity = 0;
        hasPhysics = false;
        pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (age < FADE_IN) {
            alpha = baseAlpha * ((age + 1) / (float) FADE_IN);
        } else if (age > fadeStart) {
            alpha = baseAlpha * Math.max(0, 1 - (age - fadeStart) / (float) FADE_TICKS);
        } else {
            alpha = baseAlpha;
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
            return new FormulaParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}
