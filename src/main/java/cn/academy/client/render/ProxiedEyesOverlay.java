package cn.academy.client.render;

import cn.academy.ability.vanilla.mentalout.ProxyClientDrive;
import cn.lambdalib2.util.GameTimer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "academy", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ProxiedEyesOverlay {

    private static final ResourceLocation OPEN_TEX =
            new ResourceLocation("academy", "textures/effects/proxy_eye_open.png");
    private static final ResourceLocation IDLE_TEX =
            new ResourceLocation("academy", "textures/effects/proxy_eye_idle.png");
    private static final int OPEN_FRAMES = 22, IDLE_FRAMES = 8, EYE_FRAME = 128;
    private static final int OPEN_TEX_W = EYE_FRAME * OPEN_FRAMES;
    private static final int IDLE_TEX_W = EYE_FRAME * IDLE_FRAMES;
    private static final int EYE_TEX_H = EYE_FRAME;

    private static final int GRID_X = 12, GRID_Y = 8;

    private static final float EYE_SIZE = 0.13f;

    private static final float EYE_SIZE_VAR = 0.18f;

    private static final float HOLE_MARGIN = 0.30f;

    private static final double OPEN_FRAME_TIME = 0.10, IDLE_FRAME_TIME = 0.10;

    private static final double BORN_JITTER = 0.45;

    private static final int BORN_STEPS = 128;

    private static final double CLOSE_TIME = 0.28;

    private static final double OPEN_TIME = 0.55;

    private static final double BLACK_FRAC = 0.30;

    private static final double EYELID_FADE_FROM = 0.75;

    private static final double VANISH_TIME = 0.12;

    private static final float Z = 400f;

    private static final int OFF = 0, ENTER = 1, EXIT = 2;
    private static int mode = OFF;

    private static double t0;

    private static double tEnter;

    private static boolean lastDriven;

    private static float[] eyeX, eyeY, eyeAngle, eyeScale, eyeVanish, eyeSpeed;

    private static float[] eyeBornP;

    private static float[] eyeDelay;

    private static Boolean eyeTexOk;

    private ProxiedEyesOverlay() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        boolean now = ProxyClientDrive.beingDriven();
        if (now == lastDriven) {
            return;
        }
        lastDriven = now;
        if (now) {
            begin();
        } else if (mode == ENTER) {
            mode = EXIT;
            t0 = GameTimer.getPausableTime();
        }
    }

    private static void begin() {
        eyeTexOk = null;
        AllyCastEyelid.forgetTex();
        spawnEyes((long) (GameTimer.getPausableTime() * 1000.0));
        mode = ENTER;
        t0 = tEnter = GameTimer.getPausableTime();
    }

    private static void spawnEyes(long seed) {
        int n = GRID_X * GRID_Y;
        eyeX = new float[n];
        eyeY = new float[n];
        eyeAngle = new float[n];
        eyeScale = new float[n];
        eyeVanish = new float[n];
        eyeSpeed = new float[n];
        eyeBornP = new float[n];
        eyeDelay = new float[n];
        for (int gy = 0; gy < GRID_Y; ++gy) {
            for (int gx = 0; gx < GRID_X; ++gx) {
                int i = gy * GRID_X + gx;
                eyeX[i] = (gx + rnd(seed, i, 0)) / GRID_X;
                eyeY[i] = (gy + rnd(seed, i, 1)) / GRID_Y;
                eyeAngle[i] = rnd(seed, i, 2) * 360f;
                eyeScale[i] = 1f + (rnd(seed, i, 3) * 2f - 1f) * EYE_SIZE_VAR;

                eyeVanish[i] = rnd(seed, i, 4) * (float) (1.0 - VANISH_TIME);
                eyeSpeed[i] = 0.8f + rnd(seed, i, 5) * 0.5f;
                eyeDelay[i] = rnd(seed, i, 6) * (float) BORN_JITTER;

                eyeBornP[i] = solveBornP(i);
            }
        }
    }

    private static float solveBornP(int i) {
        float margin = EYE_SIZE * eyeScale[i] * HOLE_MARGIN;
        for (int s = 0; s <= BORN_STEPS; ++s) {
            double p = (double) s / BORN_STEPS;
            if (!AllyCastEyelid.inHole(eyeX[i], eyeY[i], opennessAt(ENTER, p),
                    margin, (float) blacknessAt(ENTER, p))) {
                return (float) p;
            }
        }

        return 1f;
    }

    private static float rnd(long seed, int i, int k) {
        long h = seed * 0x9E3779B97F4A7C15L + (i * 8L + k) * 0xBF58476D1CE4E5B9L;
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= h >>> 33;
        return (int) (h >>> 40) / (float) (1 << 24);
    }

    private static double phase(double now) {
        switch (mode) {
            case ENTER:
                return clamp01((now - t0) / CLOSE_TIME);
            case EXIT:
                return clamp01((now - t0) / OPEN_TIME);
            default:
                return 0.0;
        }
    }

    private static double opennessAt(int m, double p) {
        if (m == ENTER) {
            return clamp01(1.0 - p / (1.0 - BLACK_FRAC));
        }
        if (m == EXIT) {
            return clamp01((p - BLACK_FRAC) / (1.0 - BLACK_FRAC));
        }
        return 0.0;
    }

    private static double blacknessAt(int m, double p) {
        if (m == ENTER) {
            return clamp01((p - (1.0 - BLACK_FRAC)) / BLACK_FRAC);
        }
        if (m == EXIT) {
            return clamp01(1.0 - p / BLACK_FRAC);
        }
        return 0.0;
    }

    private static double clamp01(double v) {
        return v < 0.0 ? 0.0 : (v > 1.0 ? 1.0 : v);
    }

    @SubscribeEvent(priority = EventPriority.HIGH, receiveCanceled = true)
    public static void onRenderEyes(RenderGuiEvent.Post event) {
        if (mode == OFF) {
            return;
        }
        double now = GameTimer.getPausableTime();
        double p = phase(now);

        if (mode == EXIT && p >= 1.0) {
            mode = OFF;
            return;
        }
        double op = opennessAt(mode, p);
        double black = blacknessAt(mode, p);
        double exitP = mode == EXIT ? p : 0.0;
        GuiGraphics gg = event.getGuiGraphics();
        gg.pose().pushPose();
        gg.pose().translate(0f, 0f, Z);

        if (AllyCastEyelid.hasTex()) {
            float a = exitP < EYELID_FADE_FROM ? 1f
                    : (float) ((1.0 - exitP) / (1.0 - EYELID_FADE_FROM));
            AllyCastEyelid.blitEyelid(gg, op, a);
        }

        if (black > 0.0) {
            int alpha = (int) (Math.min(1.0, black) * 255.0);
            gg.fill(0, 0, gg.guiWidth(), gg.guiHeight(), alpha << 24);
        }
        if (hasEyeTex()) {
            drawEyes(gg, op, (float) black, exitP, now);
        }
        gg.pose().popPose();
    }

    private static void drawEyes(GuiGraphics gg, double openness, float shrink,
                                 double exitP, double now) {
        int w = gg.guiWidth(), h = gg.guiHeight();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        float curA = 1f;
        double openDur = OPEN_FRAMES * OPEN_FRAME_TIME;
        for (int i = 0; i < eyeX.length; ++i) {

            double ac = (now - (tEnter + eyeBornP[i] * CLOSE_TIME + eyeDelay[i])) * eyeSpeed[i];
            if (ac < 0.0) {
                continue;
            }
            float a = 1f;
            if (mode == EXIT) {

                a = 1f - (float) clamp01((exitP - eyeVanish[i]) / VANISH_TIME);
                if (a <= 0.004f) {
                    continue;
                }
            }
            float scale = eyeScale[i];

            if (AllyCastEyelid.inHole(eyeX[i], eyeY[i], openness,
                    EYE_SIZE * scale * HOLE_MARGIN, shrink)) {
                continue;
            }
            if (a != curA) {
                gg.setColor(1f, 1f, 1f, a);
                curA = a;
            }

            ResourceLocation tex;
            int frame, texW;
            if (ac < openDur) {
                tex = OPEN_TEX;
                texW = OPEN_TEX_W;

                frame = Math.min(OPEN_FRAMES - 1, (int) (ac / OPEN_FRAME_TIME));
            } else {
                tex = IDLE_TEX;
                texW = IDLE_TEX_W;
                frame = (int) ((ac - openDur) / IDLE_FRAME_TIME) % IDLE_FRAMES;
            }
            gg.pose().pushPose();
            gg.pose().translate(eyeX[i] * w, eyeY[i] * h, 0f);
            gg.pose().mulPose(Axis.ZP.rotationDegrees(eyeAngle[i]));

            float k = h * EYE_SIZE * scale / EYE_FRAME;
            gg.pose().scale(k, k, 1f);
            gg.blit(tex, -EYE_FRAME / 2, -EYE_FRAME / 2, EYE_FRAME, EYE_FRAME,
                    (float) (frame * EYE_FRAME), 0f, EYE_FRAME, EYE_FRAME, texW, EYE_TEX_H);
            gg.pose().popPose();
        }
        if (curA != 1f) {
            gg.setColor(1f, 1f, 1f, 1f);
        }
        RenderSystem.disableBlend();
    }

    private static boolean hasEyeTex() {
        if (eyeTexOk == null) {
            var rm = Minecraft.getInstance().getResourceManager();
            eyeTexOk = rm.getResource(OPEN_TEX).isPresent() && rm.getResource(IDLE_TEX).isPresent();
        }
        return eyeTexOk;
    }
}
