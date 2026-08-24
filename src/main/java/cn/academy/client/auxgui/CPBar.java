package cn.academy.client.auxgui;

import cn.academy.Resources;
import cn.academy.ability.Category;
import cn.academy.ability.context.ContextManager;
import cn.academy.ability.context.IConsumptionProvider;
import cn.academy.client.gui.developer.DevRender;
import cn.academy.client.render.ACEffectShaders;
import cn.academy.client.render.util.ACRenderingHelper;
import cn.academy.datapart.AbilityData;
import cn.academy.datapart.CPData;
import cn.academy.datapart.PresetData;
import cn.academy.event.ability.PresetSwitchEvent;
import cn.academy.client.gui.SvgShape;
import cn.lambdalib2.cgui.Widget;
import cn.lambdalib2.cgui.component.DrawTexture;
import cn.lambdalib2.cgui.component.Transform.WidthAlign;
import cn.lambdalib2.cgui.event.FrameEvent;
import cn.lambdalib2.render.font.IFont;
import cn.lambdalib2.render.font.IFont.FontAlign;
import cn.lambdalib2.render.font.IFont.FontOption;
import cn.lambdalib2.util.Color;
import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.GameTimer;
import cn.lambdalib2.util.HudUtils;
import cn.lambdalib2.util.MathUtils;
import cn.lambdalib2.util.RandUtils;
import cn.lambdalib2.vis.curve.CubicCurve;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@OnlyIn(Dist.CLIENT)
public class CPBar extends Widget {

    public static final CPBar instance = new CPBar();

    static final float WIDTH = 964, HEIGHT = 147;

    static final float SCALE = 0.2f;

    static final float CP_BALANCE_SPEED = 2.0f, O_BALANCE_SPEED = 2.0f;

    static final double sin41 = Math.sin(Math.toRadians(44.0));

    public static void init() {
        ACHud.instance.addElement(instance, () -> true, "cpbar",
                new Widget().size(WIDTH, HEIGHT)
                        .scale(SCALE)
                        .walign(WidthAlign.RIGHT)
                        .addComponent(new DrawTexture().setTex(Resources.getTexture("gui/edit_preview/cpbar"))));
    }

    public static ResourceLocation
            TEX_BACK_NORMAL = tex("back_normal"),
            TEX_BACK_OVERLOAD = tex("back_overload"),
            TEX_CP = tex("cp"),
            TEX_FRONT_OVERLOAD = tex("front_overload"),
            TEX_OVERLOAD_HIGHLIGHT = tex("highlight_overload"),
            TEX_MASK = tex("mask");

    private final List<ProgColor> cpColors = new ArrayList<>(), overrideColors = new ArrayList<>();

    private long presetChangeTime, lastPresetTime;

    private boolean lastFrameActive;
    private long lastDrawTime;
    private long showTime;

    private boolean showingNumbers;
    private long lastShowValueChange;

    private static final long KEEP_MS = 5_000L;

    private float lastCp, lastOverload;

    private int lastConsumeSeq;

    private boolean primed;

    private long keepUntil;

    private float mAlpha;

    private float bufferedCP;
    private float bufferedOverload;

    private ResourceLocation overlayTexture;

    private long maxtime;
    private final List<OffsetKeyframe> frames = new ArrayList<>();
    private final CubicCurve alphaCurve = new CubicCurve();

    private static class OffsetKeyframe {
        long time;
        double dirX, dirY;
    }

    {
        final double aspect = WIDTH / HEIGHT, offsetMax = 9;
        final int iteration = 60;

        alphaCurve.addPoint(0, RandUtils.ranged(0.2, 0.8));

        int sum = 0;
        for (int i = 0; i < iteration; ++i) {
            OffsetKeyframe frame = new OffsetKeyframe();
            int thistime = RandUtils.rangei(80, 400);
            float offsetNorm = RandUtils.rangef(0, 1);
            float theta = RandUtils.rangef(0, MathUtils.PI_F * 2);
            offsetNorm = offsetNorm * offsetNorm * offsetNorm;

            sum += thistime;

            frame.time = sum;
            frame.dirX = Math.sin(theta) * offsetNorm * offsetMax * aspect;
            frame.dirY = Math.cos(theta) * offsetNorm * offsetMax;
            frames.add(frame);

            alphaCurve.addPoint(sum, RandUtils.ranged(0.4, 0.7));
        }

        maxtime = sum;
    }

    private OffsetKeyframe int_get() {
        long timeInput = ((long) (GameTimer.getAbsTime() * 1000)) % maxtime;
        return frames.stream().filter(f -> f.time > timeInput).findFirst().get();
    }

    private CPBar() {
        transform.setSize(WIDTH, HEIGHT);
        transform.scale = SCALE;
        transform.alignWidth = WidthAlign.RIGHT;
        transform.setPos(-12, 12);

        initEvents();

        cpColors.add(new ProgColor(0.0, Colors.fromHexColor(0xfff06767)));
        cpColors.add(new ProgColor(0.35, Colors.fromHexColor(0xffffae44)));
        cpColors.add(new ProgColor(1.0, Colors.fromHexColor(0xffffffff)));

        overrideColors.add(new ProgColor(0.0, Colors.fromHexColor(0x0Adfdfdf)));
        overrideColors.add(new ProgColor(0.55, Colors.fromHexColor(0x23f0d49d)));
        overrideColors.add(new ProgColor(1.0, Colors.fromHexColor(0x50f56464)));

        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onSwitchPreset(PresetSwitchEvent event) {
        lastPresetTime = presetChangeTime;
        presetChangeTime = (long) (GameTimer.getTime() * 1000);
    }

    public void startDisplayNumbers() {
        showingNumbers = true;
        lastShowValueChange = (long) (GameTimer.getTime() * 1000);
    }

    public void stopDisplayNumbers() {
        showingNumbers = false;
        long time = (long) (GameTimer.getTime() * 1000);
        if (time - lastShowValueChange > 400) {
            lastShowValueChange = time;
        } else {
            lastShowValueChange = 0;
        }
    }

    private void initEvents() {
        listen(FrameEvent.class, (w, e) -> {
            Player player = Minecraft.getInstance().player;
            if (player == null) return;
            CPData cpData = CPData.get(player);
            AbilityData aData = AbilityData.get(player);
            if (!aData.hasCategory()) return;

            Category c = aData.getCategory();
            overlayTexture = c.getOverlayIcon();

            long time = (long) (GameTimer.getTime() * 1000);

            float cpNow = cpData.getCP(), olNow = cpData.getOverload();
            int seqNow = cpData.getConsumeSeq();
            if (primed && (seqNow != lastConsumeSeq
                    || cpNow < lastCp - 1e-3f || olNow > lastOverload + 1e-3f)) {
                keepUntil = time + KEEP_MS;
            }
            lastCp = cpNow;
            lastOverload = olNow;
            lastConsumeSeq = seqNow;
            primed = true;

            boolean active = cpData.isActivated() || time < keepUntil;
            if (!lastFrameActive && active) {
                showTime = time;
            }

            long deltaTime = Math.min(100L, time - lastDrawTime);

            final long BLENDIN_TIME = 200L;
            mAlpha = (time - showTime < BLENDIN_TIME) ? (float) (time - showTime) / BLENDIN_TIME
                    : (active ? 1.0f : Math.max(0.0f, 1 - (time - lastDrawTime) / 200.0f));

            boolean interf = cpData.isInterfering();
            boolean overloadRecovering = cpData.isOverloadRecovering();

            Matrix4f saved = DevRender.save();
            if (interf) {
                OffsetKeyframe frame = int_get();
                DevRender.translate(frame.dirX, frame.dirY, 0);
                long timeInput = (long) (GameTimer.getAbsTime() * 1000) % maxtime;
                timeInput = (timeInput / 10) * 10;
                mAlpha *= alphaCurve.valueAt(timeInput);
            }

            float poverload = mAlpha > 0 ? cpData.getOverload() / cpData.getMaxOverload() : 0;
            bufferedOverload = balance(bufferedOverload, poverload, deltaTime * 1E-3f * O_BALANCE_SPEED);

            float pcp = mAlpha > 0 ? cpData.getCP() / cpData.getMaxCP() : 0;
            bufferedCP = balance(bufferedCP, pcp, deltaTime * 1E-3f * CP_BALANCE_SPEED);

            if (mAlpha > 0) {

                {
                    if (!cpData.isOverloaded()) {
                        drawNormal(bufferedOverload);
                    } else {
                        drawOverload();
                    }

                    float estmCons = getConsumptionHint();
                    boolean low = interf || overloadRecovering;

                    if (estmCons != 0) {

                        float ncp = Math.max(0, cpData.getCP() - estmCons);

                        float oldAlpha = mAlpha;
                        mAlpha *= 0.2f + 0.1f * (1 + Math.sin(time / 80.0f));
                        drawCPBar(pcp, low);
                        mAlpha = oldAlpha;

                        drawCPBar(ncp / cpData.getMaxCP(), low);
                    } else {
                        drawCPBar(bufferedCP, low);
                    }
                }

                {
                    final long preset_wait = 2000L;
                    if (time - presetChangeTime < preset_wait) {
                        drawPresetHint((double) (time - presetChangeTime) / preset_wait, time - lastPresetTime);
                    }
                }

                {
                    float alpha;
                    long dt = lastShowValueChange == 0 ? Long.MAX_VALUE : time - lastShowValueChange;

                    if (cpData.isOverloaded()) {
                        alpha = 0.0f;
                    } else if (CPBarSettings.showCp() || CPBarSettings.showOverload()) {
                        alpha = 1.0f;
                    } else if (showingNumbers) {
                        alpha = MathUtils.clampf(0, 1, (dt - 200) / 400f);
                    } else if (dt < 300f) {
                        alpha = 1 - dt / 300f;
                    } else {
                        alpha = 0.0f;
                    }

                    if (alpha > 0) {
                        drawNumbers(cpData, alpha);
                    }
                }

            }

            if (active) {
                lastDrawTime = time;
            }

            lastFrameActive = active;

            DevRender.color(1, 1, 1, 1);
            DevRender.restore(saved);
        });
    }

    private void drawNumbers(CPData cpData, float alpha) {
        final float x0 = 110;

        IFont font = Resources.font();
        FontOption option = new FontOption(40);
        option.color.setAlpha(Colors.f2i(0.6f * mAlpha * alpha));

        String str10 = "CP ";
        String str11 = String.format("%.0f", cpData.getCP());
        String str12 = String.format("/%.0f", cpData.getMaxCP());

        String str20 = "OL ";
        String str21 = String.format("%.0f", cpData.getOverload());
        String str22 = String.format("/%.0f", cpData.getMaxOverload());

        float len10 = font.getTextWidth(str10, option), len11 = font.getTextWidth(str11, option),
                len20 = font.getTextWidth(str20, option), len21 = font.getTextWidth(str21, option);

        float len0 = Math.max(len10, len20);
        float len1 = len0 + Math.max(len11, len21);

        boolean showCp = CPBarSettings.showCp();
        boolean showOl = CPBarSettings.showOverload();

        if (showCp) {
            font.draw(str10, x0, 55, option);
            font.draw(str12, x0 + len1, 55, option);
        }
        if (showOl) {
            font.draw(str20, x0, 85, option);
            font.draw(str22, x0 + len1, 85, option);
        }

        option.align = FontAlign.RIGHT;
        if (showCp) {
            font.draw(str11, x0 + len1, 55, option);
        }
        if (showOl) {
            font.draw(str21, x0 + len1, 85, option);
        }
    }

    private void drawOverload() {
        setColor(1, 1, 1, 0.8);
        DevRender.rect(TEX_BACK_OVERLOAD, 0, 0, WIDTH, HEIGHT);

        final double x0 = 30, width2 = WIDTH - x0 - 20;

        ShaderInstance sh = ACEffectShaders.cpbarOverload();
        if (DevRender.useTwoSampler(sh, TEX_FRONT_OVERLOAD, TEX_MASK)) {

            sh.safeGetUniform("TexOffset").set(((float) (GameTimer.getTime() % 10L)) / 10000.0f);
            DevRender.cpbarQuad(width2, HEIGHT, 1, 1, 1, mAlpha,
                    x0, 0, x0, HEIGHT, x0 + width2, HEIGHT, x0 + width2, 0);
        } else {

            setColor(1, 1, 1, 1);
            DevRender.rect(TEX_FRONT_OVERLOAD, x0, 0, width2, HEIGHT);
        }

        setColor(1, 1, 1, 0.3 + 0.35 * (Math.sin(GameTimer.getTime() / 200.0) + 1));
        DevRender.rect(TEX_OVERLOAD_HIGHLIGHT, 0, 0, WIDTH, HEIGHT);
    }

    private void drawNormal(float overload) {
        setColor(1, 1, 1, .8);
        DevRender.rect(TEX_BACK_NORMAL, 0, 0, WIDTH, HEIGHT);

        final double X0 = 0, Y0 = 21, WIDTH = 943, HEIGHT = 104;

        Color col = autoLerp(overrideColors, overload);
        double len = overload * WIDTH;

        DevRender.useTwoSampler(ACEffectShaders.simple(), TEX_MASK, TEX_MASK);
        double x = X0 + WIDTH - len;
        DevRender.cpbarQuad(CPBar.WIDTH, CPBar.HEIGHT,
                Colors.i2f(col.getRed()), Colors.i2f(col.getGreen()), Colors.i2f(col.getBlue()),
                Colors.i2f(col.getAlpha()) * mAlpha,
                x, Y0, x, Y0 + HEIGHT, x + len, Y0 + HEIGHT, x + len, Y0);
    }

    private float getConsumptionHint() {
        Optional<IConsumptionProvider> provider = ContextManager.instance.findLocal(IConsumptionProvider.class);
        return provider.map(IConsumptionProvider::getConsumptionHint).orElse(0f);
    }

    private void drawCPBar(float prog, boolean cantuse) {
        float pre_mAlpha = mAlpha;
        if (cantuse) {
            mAlpha *= 0.3f;
        }

        Color col = autoLerp(cpColors, prog);

        prog = 0.16f + prog * 0.8f;

        final double OFF = 103 * sin41, X0 = 47, Y0 = 30, WIDTH = 883, HEIGHT = 84;
        double len = WIDTH * prog, len2 = len - OFF;

        if (!DevRender.useTwoSampler(ACEffectShaders.cpbarCp(), TEX_CP, overlayTexture)) {
            DevRender.useTwoSampler(ACEffectShaders.simple(), TEX_CP, TEX_CP);
        }

        DevRender.cpbarQuad(CPBar.WIDTH, CPBar.HEIGHT,
                Colors.i2f(col.getRed()), Colors.i2f(col.getGreen()), Colors.i2f(col.getBlue()),
                Colors.i2f(col.getAlpha()) * mAlpha,
                X0 + (WIDTH - len), Y0,
                X0 + (WIDTH - len2), Y0 + HEIGHT,
                X0 + WIDTH, Y0 + HEIGHT,
                X0 + WIDTH, Y0);

        mAlpha = pre_mAlpha;
    }

    private final Color CRL_P_BACK = new Color(48, 48, 48, 160),
            CRL_P_FORE = new Color(255, 255, 255, 200);
    private final Color CRL_P_TEXT = new Color(255, 255, 255, 255);

    private final FontOption fo_PresetHint = new FontOption(46, FontAlign.CENTER);

    private void drawPresetHint(double progress, long untilLast) {
        final double x0 = 580, y0 = 136;
        final double size = 52, step = size + 10;

        double x = x0, y = y0;

        int cur = PresetData.get(Minecraft.getInstance().player).getCurrentID();

        double alpha;
        if (untilLast > 3000 && progress < 0.2) {
            alpha = progress / 0.2;
        } else if (progress > 0.8) {
            alpha = (1 - progress) / 0.2;
        } else {
            alpha = 1;
        }
        alpha *= 0.75;

        for (int i = 0; i < 4; ++i) {
            CRL_P_BACK.setAlpha(Colors.f2i((float) alpha));
            Colors.bindToGL(CRL_P_BACK);
            HudUtils.colorRect(x, y, size, size);

            CRL_P_TEXT.setAlpha(Colors.f2i((float) Math.max(0.05, alpha * 0.8)));

            fo_PresetHint.color = CRL_P_TEXT;

            Resources.font().draw(String.valueOf(i + 1), (float) (x + size / 2), (float) (y + 5), fo_PresetHint);

            Colors.bindToGL(CRL_P_TEXT);
            if (i == cur) {
                ACRenderingHelper.drawGlow(x, y, size, size, 5, CRL_P_FORE);
            }

            x += step;
        }
    }

    private void setColor(double r, double g, double b, double a) {
        DevRender.color(r, g, b, mAlpha * a);
    }

    private Color autoLerp(List<ProgColor> list, double prog) {
        for (int i = 0; i < list.size(); ++i) {
            ProgColor cur = list.get(i);
            if (cur.prog >= prog) {
                if (i == 0) {
                    return cur.color;
                }
                ProgColor last = list.get(i - 1);
                return lerpColor(last.color, cur.color, (prog - last.prog) / (cur.prog - last.prog));
            }
        }
        throw new RuntimeException("bad progress: " + prog);
    }

    private Color lerpColor(Color a, Color b, double f) {
        return new Color(
                (int) lerp(a.getRed(), b.getRed(), f),
                (int) lerp(a.getGreen(), b.getGreen(), f),
                (int) lerp(a.getBlue(), b.getBlue(), f),
                (int) lerp(a.getAlpha(), b.getAlpha(), f));
    }

    private double lerp(double a, double b, double factor) {
        return a * (1 - factor) + b * factor;
    }

    private float balance(float from, float to, float max) {
        float delta = to - from;
        delta = Math.signum(delta) * Math.min(max, Math.abs(delta));
        return from + delta;
    }

    private static ResourceLocation tex(String name) {
        return Resources.getTexture("gui/cpbar/" + name);
    }

    private static class ProgColor {
        final double prog;
        final Color color;

        ProgColor(double prog, Color color) {
            this.prog = prog;
            this.color = color;
        }
    }
}
