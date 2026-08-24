package cn.academy.client.render.util;

import cn.academy.client.render.util.ArcFactory.Arc;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ThunderArcs {

    public static final double SKY_HEIGHT = 40.0;

    public static Arc[] skyFork;

    public static Arc[] groundBurst;

    static {
        {
            ArcFactory fac = new ArcFactory();
            fac.width = 0.3;
            fac.thickness = 0.6;
            fac.maxOffset = 1.8;
            fac.branchFactor = 0.4;
            fac.passes = 5;
            skyFork = fac.generateList(12, 16.0, 16.0);
        }
        {
            ArcFactory fac = new ArcFactory();
            fac.width = 0.35;
            fac.thickness = 0.6;
            fac.maxOffset = 4.0;
            fac.branchFactor = 0.35;
            fac.passes = 6;
            groundBurst = fac.generateList(16, 32.0, 32.0);
        }
    }

    private ThunderArcs() {}
}
