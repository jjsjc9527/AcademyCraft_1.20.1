package cn.academy.client.render;

import cn.lambdalib2.util.GameTimer;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "academy", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class AllyCastEyelid {

    private static final ResourceLocation TEX =
            new ResourceLocation("academy", "textures/effects/cast_eyelid.png");
    private static final int FRAMES = 20, FRAME_W = 512, FRAME_H = 288;
    private static final int TEX_W = FRAME_W, TEX_H = FRAME_H * FRAMES;

    private static final double OPEN_TIME = 0.45;

    private static final double CLOSE_TIME = 0.40;

    private static final double FADE_FROM = 0.5;

    private static final double CLOSE_MIN = 0.08;

    private static final float Z = -1f;

    private static final int OFF = 0, ACTIVE = 1, CLOSING = 2;

    private static int mode = OFF;

    private static double t0;

    private static double closeFrom;

    private static Boolean texOk;

    private static final ResourceLocation NOISE_TEX =
            new ResourceLocation("academy", "textures/effects/cast_noise.png");
    private static final int NOISE_FRAMES = 8, NOISE_FRAME_W = 512, NOISE_FRAME_H = 288;
    private static final int NOISE_TEX_W = NOISE_FRAME_W,
            NOISE_TEX_H = NOISE_FRAME_H * NOISE_FRAMES;

    private static final double NOISE_FRAME_TIME = 0.055;

    private static final float NOISE_ALPHA = 0.85f;

    private static boolean noisy;
    private static Boolean noiseTexOk;

    private AllyCastEyelid() {}

    public static void armNoise() {
        noisy = true;
        noiseTexOk = null;
    }

    public static void onCastBegin() {
        texOk = null;
        noisy = false;
        closeFrom = 1.0;
        mode = ACTIVE;
        t0 = GameTimer.getPausableTime();
    }

    public static void onCastEnd() {
        if (mode != ACTIVE) {
            return;
        }
        closeFrom = openness(GameTimer.getPausableTime());
        mode = CLOSING;
        t0 = GameTimer.getPausableTime();
    }

    private static double closeDur() {
        return Math.max(CLOSE_MIN, CLOSE_TIME * closeFrom);
    }

    private static double openness(double now) {
        switch (mode) {
            case ACTIVE:

                return clamp01((now - t0) / OPEN_TIME);
            case CLOSING:
                return closeFrom * (1.0 - clamp01((now - t0) / closeDur()));
            default:
                return 0.0;
        }
    }

    private static double fade(double now) {
        if (mode != CLOSING) {
            return 1.0;
        }
        double s = clamp01((now - t0) / closeDur());
        return s < FADE_FROM ? 1.0 : (1.0 - s) / (1.0 - FADE_FROM);
    }

    private static double clamp01(double v) {
        return v < 0.0 ? 0.0 : (v > 1.0 ? 1.0 : v);
    }

    @SubscribeEvent(priority = EventPriority.HIGH, receiveCanceled = true)
    public static void onRenderEyelid(RenderGuiEvent.Pre event) {
        if (mode == OFF) {
            return;
        }
        double now = GameTimer.getPausableTime();

        if (mode == CLOSING && now - t0 >= closeDur()) {
            mode = OFF;
            noisy = false;
            return;
        }
        float a = (float) fade(now);
        if (a <= 0.004f) {
            return;
        }
        if (!hasTex()) {
            return;
        }

        GuiGraphics gg = event.getGuiGraphics();

        gg.pose().pushPose();
        gg.pose().translate(0f, 0f, Z);

        if (noisy && mode == CLOSING) {
            blitNoise(gg, a);
        }
        blitEyelid(gg, openness(now), a);
        gg.pose().popPose();
    }

    private static void blitNoise(GuiGraphics gg, float alpha) {
        if (!hasNoiseTex()) {
            return;
        }
        double t = GameTimer.getPausableTime();
        int frame = (int) (t / NOISE_FRAME_TIME) % NOISE_FRAMES;
        if (frame < 0) {
            frame += NOISE_FRAMES;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        gg.setColor(1f, 1f, 1f, alpha * NOISE_ALPHA);
        gg.blit(NOISE_TEX, 0, 0, gg.guiWidth(), gg.guiHeight(),
                0f, (float) (frame * NOISE_FRAME_H), NOISE_FRAME_W, NOISE_FRAME_H,
                NOISE_TEX_W, NOISE_TEX_H);
        gg.setColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    private static boolean hasNoiseTex() {
        if (noiseTexOk == null) {
            noiseTexOk = Minecraft.getInstance().getResourceManager()
                    .getResource(NOISE_TEX).isPresent();
        }
        return noiseTexOk;
    }

    public static void blitEyelid(GuiGraphics gg, double openness, float alpha) {
        int frame = (int) Math.round(openness * (FRAMES - 1));
        frame = frame < 0 ? 0 : (frame >= FRAMES ? FRAMES - 1 : frame);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        gg.setColor(1f, 1f, 1f, alpha);
        gg.blit(TEX, 0, 0, gg.guiWidth(), gg.guiHeight(),
                0f, (float) (frame * FRAME_H), FRAME_W, FRAME_H, TEX_W, TEX_H);
        gg.setColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    static boolean hasTex() {
        if (texOk == null) {
            texOk = Minecraft.getInstance().getResourceManager().getResource(TEX).isPresent();
        }
        return texOk;
    }

    static void forgetTex() {
        texOk = null;
    }

    private static final float[] HOLE_A_TOP = {
            0.51480f, 0.51379f, 0.51147f, 0.50765f, 0.50271f, 0.49696f, 0.49056f, 0.48394f,
            0.47730f, 0.47081f, 0.46480f, 0.45935f, 0.45471f, 0.45099f, 0.44836f, 0.44674f,
            0.44623f, 0.44605f, 0.44667f, 0.44821f, 0.45074f, 0.45424f, 0.45873f, 0.46418f,
            0.47070f, 0.47814f, 0.48642f, 0.49565f, 0.50525f, 0.51508f, 0.52409f, 0.52972f,
            0.52972f
    };
    private static final float[] HOLE_A_BOT = {
            0.51505f, 0.51656f, 0.52051f, 0.52689f, 0.53466f, 0.54295f, 0.55154f, 0.55970f,
            0.56730f, 0.57418f, 0.58013f, 0.58520f, 0.58926f, 0.59240f, 0.59453f, 0.59580f,
            0.59630f, 0.59550f, 0.59310f, 0.58988f, 0.58615f, 0.58202f, 0.57755f, 0.57286f,
            0.56792f, 0.56284f, 0.55764f, 0.55224f, 0.54680f, 0.54118f, 0.53523f, 0.52972f,
            0.52972f
    };
    private static final float[] HOLE_B_TOP = {
            0.51431f, 0.50770f, 0.49177f, 0.46543f, 0.43164f, 0.39287f, 0.34974f, 0.30546f,
            0.26123f, 0.21828f, 0.17892f, 0.14352f, 0.11378f, 0.09030f, 0.07395f, 0.06489f,
            0.06364f, 0.07077f, 0.08497f, 0.10565f, 0.13243f, 0.16406f, 0.20006f, 0.23875f,
            0.28004f, 0.32216f, 0.36433f, 0.40604f, 0.44475f, 0.48058f, 0.51125f, 0.52945f,
            0.52945f
    };
    private static final float[] HOLE_B_BOT = {
            0.51706f, 0.52894f, 0.54990f, 0.57930f, 0.61389f, 0.65139f, 0.69153f, 0.73147f,
            0.77042f, 0.80737f, 0.84056f, 0.86978f, 0.89383f, 0.91229f, 0.92458f, 0.93072f,
            0.93099f, 0.92695f, 0.91863f, 0.90576f, 0.88798f, 0.86543f, 0.83802f, 0.80645f,
            0.77053f, 0.73158f, 0.69039f, 0.64787f, 0.60747f, 0.57069f, 0.54240f, 0.52945f,
            0.52945f
    };

    private static final float[] HOLE_X = new float[HOLE_A_TOP.length];

    static {
        for (int i = 0; i < HOLE_X.length; ++i) {
            HOLE_X[i] = (float) ((1.0 - Math.cos(Math.PI * i / (HOLE_X.length - 1.0))) / 2.0);
        }
    }

    public static boolean inHole(float u, float v, double openness, float margin, float shrink) {
        float t = lerpTable(HOLE_A_TOP, HOLE_B_TOP, u, openness);
        float b = lerpTable(HOLE_A_BOT, HOLE_B_BOT, u, openness);
        if (shrink > 0f) {
            float mid = (t + b) * 0.5f;
            float k = 1f - (shrink > 1f ? 1f : shrink);
            t = mid + (t - mid) * k;
            b = mid + (b - mid) * k;
        }
        return v > t + margin && v < b - margin;
    }

    private static float lerpTable(float[] a, float[] b, float u, double openness) {
        u = u < 0f ? 0f : (u > 1f ? 1f : u);
        int hi = 1;
        while (hi < HOLE_X.length - 1 && HOLE_X[hi] < u) {
            ++hi;
        }
        int lo = hi - 1;
        float span = HOLE_X[hi] - HOLE_X[lo];
        float k = span <= 1e-9f ? 0f : (u - HOLE_X[lo]) / span;
        float va = a[lo] + (a[hi] - a[lo]) * k;
        float vb = b[lo] + (b[hi] - b[lo]) * k;
        return (float) (va + (vb - va) * openness);
    }
}
