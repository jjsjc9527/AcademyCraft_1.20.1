package cn.academy.client.render.util;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Random;

@OnlyIn(Dist.CLIENT)
public class IronSandFactory {

    public int cell = 64;

    public float grain = 1.0f;

    public float coverage = 0.86f;

    public float hardness = 6.0f;

    public float clump = 0.22f;

    public float falloffStart = 0.35f;
    public float falloffPow = 1.0f;

    public int tone = 40;
    public int toneSpread = 75;

    public Sheet generate(String name, int variants, long seed) {
        NativeImage img = new NativeImage(NativeImage.Format.RGBA, cell * variants, cell, false);
        for (int v = 0; v < variants; v++) {
            paint(img, v * cell, new Random(seed * 31 + v));
        }
        return new Sheet(new ResourceLocation("academy", name), img, variants, cell);
    }

    private void paint(NativeImage img, int ox, Random rng) {
        int n = cell;

        final int CELLS = 6;
        float[][] g = new float[CELLS + 1][CELLS + 1];
        for (int y = 0; y <= CELLS; y++) {
            for (int x = 0; x <= CELLS; x++) {
                g[y][x] = rng.nextFloat();
            }
        }
        float[] lump = new float[n * n];
        double sum = 0;
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                float fx = x * (float) CELLS / n, fy = y * (float) CELLS / n;
                int x0 = (int) fx, y0 = (int) fy;
                float tx = fx - x0, ty = fy - y0;
                tx = tx * tx * (3 - 2 * tx);
                ty = ty * ty * (3 - 2 * ty);
                float a = g[y0][x0] * (1 - tx) + g[y0][x0 + 1] * tx;
                float b = g[y0 + 1][x0] * (1 - tx) + g[y0 + 1][x0 + 1] * tx;
                float val = a * (1 - ty) + b * ty;
                lump[y * n + x] = val;
                sum += val;
            }
        }
        float mean = (float) (sum / (n * n));
        double var = 0;
        for (float v : lump) {
            var += (v - mean) * (v - mean);
        }
        float sd = (float) Math.max(Math.sqrt(var / (n * n)), 1.0e-6);

        float[] field = new float[n * n];
        for (int i = 0; i < n * n; i++) {
            field[i] = rng.nextFloat() * (1.0f + clump * ((lump[i] - mean) / sd) * 0.55f);
        }

        if (grain < 1.0f) {
            float[] src = field.clone();
            for (int y = 0; y < n; y++) {
                for (int x = 0; x < n; x++) {
                    float b = (src[y * n + x]
                            + src[((y + 1) % n) * n + x] + src[((y - 1 + n) % n) * n + x]
                            + src[y * n + (x + 1) % n] + src[y * n + (x - 1 + n) % n]) / 5f;
                    field[y * n + x] = grain * src[y * n + x] + (1 - grain) * b;
                }
            }
        }

        float half = (n - 1) / 2f;
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                float r = (float) Math.hypot(x - half, y - half) / (n / 2f);
                float fall = r <= falloffStart ? 1f
                        : (float) Math.pow(Math.max(0, (1f - r) / (1f - falloffStart)), falloffPow);

                float thr = 1f - coverage * fall;
                float a = Math.min(1f, Math.max(0f, (field[y * n + x] - thr) * hardness));
                int gray = Math.min(255, Math.max(0, Math.round(tone + toneSpread * (rng.nextFloat() - 0.5f) * 2)));

                int abgr = (Math.round(a * 255) << 24) | (gray << 16) | (gray << 8) | gray;
                img.setPixelRGBA(ox + x, y, abgr);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static final class Sheet {

        private final ResourceLocation loc;
        private final NativeImage image;
        public final int variants;
        private final int cell;
        private boolean registered = false;
        private final ParticleRenderType renderType;

        Sheet(ResourceLocation loc, NativeImage image, int variants, int cell) {
            this.loc = loc;
            this.image = image;
            this.variants = variants;
            this.cell = cell;
            this.renderType = new ParticleRenderType() {
                @Override
                public void begin(BufferBuilder b, TextureManager tm) {
                    ensureRegistered();

                    RenderSystem.depthMask(false);
                    RenderSystem.setShader(GameRenderer::getParticleShader);
                    RenderSystem.setShaderTexture(0, loc);
                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                    b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
                }

                @Override
                public void end(Tesselator t) {
                    t.end();
                    RenderSystem.depthMask(true);
                }

                @Override
                public String toString() {
                    return "ac_iron_sand(" + loc + ")";
                }
            };
        }

        private void ensureRegistered() {
            if (registered) {
                return;
            }
            registered = true;
            Minecraft.getInstance().getTextureManager().register(loc, new DynamicTexture(image));
        }

        public ParticleRenderType renderType() {
            return renderType;
        }

        private float inset() {
            return 0.5f / (cell * variants);
        }

        public float u0(int i) {
            return (float) i / variants + inset();
        }

        public float u1(int i) {
            return (float) (i + 1) / variants - inset();
        }

        public float v0() {
            return 0.5f / cell;
        }

        public float v1() {
            return 1f - 0.5f / cell;
        }
    }
}
