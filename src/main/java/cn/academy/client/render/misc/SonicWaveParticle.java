package cn.academy.client.render.misc;

import cn.academy.client.render.ACEffectShaders;
import cn.academy.client.render.ScreenCopy;
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
public class SonicWaveParticle extends TextureSheetParticle {

    private static final int FRAMES = 16;

    private static final int LIFE = 10;

    public static final float SIZE = 3.6f;

    private final SpriteSet sprites;

    private final int delay;

    protected SonicWaveParticle(ClientLevel level, double x, double y, double z,
                                double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z, vx, vy, vz);
        this.sprites = sprites;

        delay = (int) Math.max(0, Math.round(vx));
        xd = 0;
        yd = 0;
        zd = 0;

        quadSize = vy > 0 ? (float) vy : SIZE;

        lifetime = LIFE;
        gravity = 0;
        hasPhysics = false;

        age = -delay;

        applySprite();

        ALIVE.add(this);
    }

    @Override
    public void tick() {
        super.tick();
        applySprite();
    }

    private void applySprite() {

        int t = age < 0 ? 0 : Math.min(age, LIFE - 1);
        setSprite(sprites.get(t, LIFE));
        alpha = age < 0 ? 0f : 1f;
    }

    @Override
    protected int getLightColor(float pt) {
        return 0xF000F0;
    }

    private static final float DISTORT = 1200f;

    private static float distortScale = 1.0f;

    private static final java.util.List<SonicWaveParticle> ALIVE = new java.util.ArrayList<>();

    public static void clearAll() {
        ALIVE.clear();
    }

    public static boolean hasAlive() {
        return !ALIVE.isEmpty();
    }

    private static boolean refractReady() {
        return ACEffectShaders.sonicWave() != null && ScreenCopy.ready();
    }

    private static final ParticleRenderType REFRACT_TYPE = new ParticleRenderType() {
        @Override
        public void begin(com.mojang.blaze3d.vertex.BufferBuilder bb,
                          net.minecraft.client.renderer.texture.TextureManager tm) {
            com.mojang.blaze3d.systems.RenderSystem.depthMask(false);
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
            com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0,
                    net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_PARTICLES);

            com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(1, ScreenCopy.textureId());

            net.minecraft.client.renderer.ShaderInstance sh = ACEffectShaders.sonicWave();
            com.mojang.blaze3d.systems.RenderSystem.setShader(ACEffectShaders::sonicWave);
            if (sh != null) {

                sh.safeGetUniform("ScreenSize")
                        .set((float) ScreenCopy.width(), (float) ScreenCopy.height());
                sh.safeGetUniform("Distort").set(DISTORT * distortScale);
            }
            bb.begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS,
                    com.mojang.blaze3d.vertex.DefaultVertexFormat.PARTICLE);
        }

        @Override
        public void end(com.mojang.blaze3d.vertex.Tesselator tess) {
            tess.end();
            com.mojang.blaze3d.systems.RenderSystem.depthMask(true);
        }

        @Override
        public String toString() {
            return "ACADEMY_SONIC_WAVE";
        }
    };

    public static void drawAll(net.minecraft.client.Camera camera, float partialTick) {
        ALIVE.removeIf(p -> !p.isAlive());
        if (ALIVE.isEmpty() || !refractReady()) {
            return;
        }
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();

        com.mojang.blaze3d.vertex.PoseStack mv =
                com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();
        mv.pushPose();
        mv.mulPoseMatrix(cn.academy.client.render.ACEffectLateRender.cameraRotation());
        com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();

        com.mojang.blaze3d.vertex.Tesselator tess =
                com.mojang.blaze3d.vertex.Tesselator.getInstance();
        com.mojang.blaze3d.vertex.BufferBuilder bb = tess.getBuilder();

        ALIVE.sort(java.util.Comparator.comparingDouble(p -> p.quadSize));
        int i = 0;
        while (i < ALIVE.size()) {
            float qs = ALIVE.get(i).quadSize;
            int j = i;
            while (j < ALIVE.size() && ALIVE.get(j).quadSize == qs) {
                j++;
            }
            distortScale = qs / SIZE;
            REFRACT_TYPE.begin(bb, mc.getTextureManager());
            for (int k = i; k < j; k++) {
                ALIVE.get(k).render(bb, camera, partialTick);
            }
            REFRACT_TYPE.end(tess);
            i = j;
        }

        mv.popPose();
        com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
    }

    @Override
    public ParticleRenderType getRenderType() {
        return refractReady() ? ParticleRenderType.NO_RENDER
                : ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
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
            return new SonicWaveParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}
