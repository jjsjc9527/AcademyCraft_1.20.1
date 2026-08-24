package cn.academy.client.render.misc;

import cn.academy.client.render.util.IronSandFactory;
import cn.academy.client.render.util.IronSandPatterns;
import cn.lambdalib2.util.RandUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class IronSandParticle extends SingleQuadParticle {

    private static final float FADE_AT = 0.55f;

    private static final java.util.List<IronSandParticle> ALIVE = new java.util.ArrayList<>();

    private final IronSandFactory.Sheet sheet;
    private final int variant;
    private final float baseAlpha;

    protected IronSandParticle(ClientLevel level, double x, double y, double z,
                               double vx, double vy, double vz,
                               IronSandFactory.Sheet sheet, float sizeFrom, float sizeTo,
                               int lifeFrom, int lifeTo, float alphaFrom, float alphaTo) {
        super(level, x, y, z, vx, vy, vz);

        xd = vx;
        yd = vy;
        zd = vz;
        this.sheet = sheet;
        this.variant = RandUtils.rangei(0, sheet.variants);

        quadSize = RandUtils.rangef(sizeFrom, sizeTo) * 0.5f;
        lifetime = RandUtils.rangei(lifeFrom, lifeTo);
        baseAlpha = alpha = RandUtils.rangef(alphaFrom, alphaTo);
        gravity = 0;
        hasPhysics = false;
        friction = 0.97f;
        roll = oRoll = (float) RandUtils.ranged(0, Math.PI * 2);
        ALIVE.add(this);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.NO_RENDER;
    }

    public static void clearAll() {
        ALIVE.clear();
    }

    public static void drawAll(net.minecraft.client.Camera camera, float partialTick) {
        ALIVE.removeIf(p -> !p.isAlive());
        if (ALIVE.isEmpty()) {
            return;
        }
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();

        java.util.Map<IronSandFactory.Sheet, java.util.List<IronSandParticle>> groups =
                new java.util.LinkedHashMap<>();
        for (IronSandParticle p : ALIVE) {
            groups.computeIfAbsent(p.sheet, k -> new java.util.ArrayList<>()).add(p);
        }

        net.minecraft.client.renderer.LightTexture lightTexture = mc.gameRenderer.lightTexture();
        lightTexture.turnOnLightLayer();
        com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
        com.mojang.blaze3d.systems.RenderSystem.activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE2);
        com.mojang.blaze3d.systems.RenderSystem.activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);

        com.mojang.blaze3d.vertex.PoseStack mv =
                com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();
        mv.pushPose();
        mv.mulPoseMatrix(cn.academy.client.render.ACEffectLateRender.cameraRotation());
        com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();

        com.mojang.blaze3d.vertex.Tesselator tess =
                com.mojang.blaze3d.vertex.Tesselator.getInstance();
        com.mojang.blaze3d.vertex.BufferBuilder bb = tess.getBuilder();
        for (java.util.Map.Entry<IronSandFactory.Sheet, java.util.List<IronSandParticle>> en
                : groups.entrySet()) {
            ParticleRenderType type = en.getKey().renderType();
            type.begin(bb, mc.getTextureManager());
            for (IronSandParticle p : en.getValue()) {
                p.render(bb, camera, partialTick);
            }
            type.end(tess);
        }

        mv.popPose();
        com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
        lightTexture.turnOffLightLayer();
    }

    @Override
    public void tick() {
        super.tick();
        float u = age / (float) lifetime;
        if (u > FADE_AT) {
            alpha = baseAlpha * Math.max(0f, 1f - (u - FADE_AT) / (1f - FADE_AT));
        }
    }

    @Override
    protected float getU0() {
        return sheet.u0(variant);
    }

    @Override
    protected float getU1() {
        return sheet.u1(variant);
    }

    @Override
    protected float getV0() {
        return sheet.v0();
    }

    @Override
    protected float getV1() {
        return sheet.v1();
    }

    @Override
    public float getQuadSize(float partialTick) {
        return quadSize;
    }

    @OnlyIn(Dist.CLIENT)
    public static class PuffProvider implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z, double vx, double vy, double vz) {
            return new IronSandParticle(level, x, y, z, vx, vy, vz,
                    IronSandPatterns.PUFF, 0.42f, 0.68f, 22, 40, 0.80f, 1.00f);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class WhipProvider implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z, double vx, double vy, double vz) {
            return new IronSandParticle(level, x, y, z, vx, vy, vz,
                    IronSandPatterns.FINE, 0.22f, 0.36f, 10, 18, 0.55f, 0.9f);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class FineProvider implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z, double vx, double vy, double vz) {
            return new IronSandParticle(level, x, y, z, vx, vy, vz,
                    IronSandPatterns.FINE, 0.20f, 0.34f, 16, 28, 0.45f, 0.75f);
        }
    }
}
