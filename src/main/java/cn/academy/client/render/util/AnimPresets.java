package cn.academy.client.render.util;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import static cn.academy.client.render.util.HandAnim.curve;

@OnlyIn(Dist.CLIENT)
public final class AnimPresets {

    private AnimPresets() {}

    public static HandAnim createPrepareAnim() {
        HandAnim anim = new HandAnim();
        anim.ty = curve(0, 0, 0.5, 0.2, 1, 0.4);
        anim.tx = curve(0, 0, 1, -0.02);
        anim.tz = curve(0, 0, 1, -0.05);
        anim.rx = curve(0, 0, 1, -20);
        return anim;
    }

    public static HandAnim createPunchAnim() {
        HandAnim anim = new HandAnim();
        anim.ty = curve(0, 0.8, 0.5, 0.75, 1, 0);
        anim.tx = curve(0, -0.04, 0.5, -0.04, 1, 0);
        anim.tz = curve(0, -0.0, 0.3, -0.4, 1, 0);
        anim.rx = curve(0, -40, 0.5, -45, 1, 0);
        anim.ry = curve(0, 0, 0.3, 10, 1, 0);
        return anim;
    }
}
