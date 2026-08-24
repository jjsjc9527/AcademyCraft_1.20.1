package cn.lambdalib2.cgui;

import cn.lambdalib2.util.GameTimer;
import cn.lambdalib2.util.MathUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class GuiOpenAnimation {

    private GuiOpenAnimation() {}

    public static final double DURATION = 0.22;

    public static final float OVERSHOOT = 1.1f;

    private static final float MIN_SCALE = 0.02f;

    public static float ease(float t) {
        if (t <= 0f) return 0f;
        if (t >= 1f) return 1f;
        final float c1 = OVERSHOOT;
        final float c3 = c1 + 1f;
        float x = t - 1f;
        return 1f + c3 * x * x * x + c1 * x * x;
    }

    public static float progress(double startSec) {
        if (startSec < 0) return 1f;
        double t = (GameTimer.getAbsTime() - startSec) / DURATION;
        return ease((float) MathUtils.clampd(0, 1, t));
    }

    public static boolean animating(double startSec) {
        return startSec >= 0 && (GameTimer.getAbsTime() - startSec) < DURATION;
    }

    public static void apply(PoseStack pose, float p) {
        pose.scale(1f, Math.max(MIN_SCALE, p), 1f);
    }
}
