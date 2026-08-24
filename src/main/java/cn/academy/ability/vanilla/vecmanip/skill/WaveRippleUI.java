package cn.academy.ability.vanilla.vecmanip.skill;

import cn.academy.AcademyCraft;
import cn.academy.Resources;
import cn.academy.config.Property;
import cn.lambdalib2.auxgui.AuxGui;
import cn.lambdalib2.util.GameTimer;
import cn.lambdalib2.util.HudUtils;
import cn.lambdalib2.util.RandUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class WaveRippleUI extends AuxGui {

    private static final WaveRippleUI INSTANCE = new WaveRippleUI();

    private static final ResourceLocation TEXTURE = Resources.getTexture("effects/glow_circle");

    private static final float MAX_ALPHA = 0.4f;
    private static final float AVG_SIZE = 110;
    private static final float INTENSITY = 1.6f;

    public static final String CATEGORY = "generic", KEY = "vecDeviationHud";

    private static Property prop;

    private final List<Ripple> ripples = new ArrayList<>();
    private double lastFrameTime = GameTimer.getTime();

    public static void init() {
        prop = AcademyCraft.config.get(CATEGORY, KEY, false,
                "Show the rippling screen overlay while your own Vector Deviation is active. "
                        + "Off by default.");
        AuxGui.register(INSTANCE);
    }

    private static boolean enabled() {
        return prop != null && prop.getBoolean();
    }

    private static boolean isActive() {
        return cn.academy.ability.context.ContextManager.instance
                .findLocal(VecDeviation.DeviationContext.class).isPresent();
    }

    private WaveRippleUI() {
        consistent = true;
        foreground = false;
    }

    @Override
    public void draw(GuiGraphics gg, float width, float height) {
        double now = GameTimer.getTime();
        double dt = now - lastFrameTime;
        lastFrameTime = now;

        boolean active = enabled() && isActive();

        if (!active && ripples.isEmpty()) {
            return;
        }

        for (Iterator<Ripple> it = ripples.iterator(); it.hasNext(); ) {
            Ripple r = it.next();
            r.timeAlive += dt;
            if (r.timeAlive >= r.life) {
                it.remove();
            }
        }

        if (active && RandUtils.nextFloat() < dt * INTENSITY) {
            ripples.add(new Ripple(
                    RandUtils.rangef(1.5f, 2.5f),
                    RandUtils.nextFloat() * width,
                    RandUtils.nextFloat() * height,
                    RandUtils.rangef(0.8f, 1.2f) * AVG_SIZE));
        }

        if (ripples.isEmpty()) {
            return;
        }

        HudUtils.setPose(gg.pose());
        HudUtils.loadTexture(TEXTURE);
        for (Ripple r : ripples) {
            float a = MAX_ALPHA * r.alpha();
            if (a <= 0) continue;
            double size = r.realSize();
            RenderSystem.setShaderColor(1, 1, 1, a);
            HudUtils.rect(r.x - size / 2, r.y - size / 2, size, size);
        }
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    private static final class Ripple {
        final float life;
        final double x, y;
        final float size;
        double timeAlive = 0;

        Ripple(float life, double x, double y, float size) {
            this.life = life;
            this.x = x;
            this.y = y;
            this.size = size;
        }

        float alpha() {
            double prog = timeAlive / life;
            if (prog < 0.2) return (float) (prog / 0.2);
            if (prog < 0.5) return 1;
            return (float) (1 - (prog - 0.5) / 0.5);
        }

        double realSize() {
            return size + timeAlive * 20;
        }
    }
}
