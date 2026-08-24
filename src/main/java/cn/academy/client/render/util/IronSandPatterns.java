package cn.academy.client.render.util;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class IronSandPatterns {

    private static final int GEN = 8;

    public static final IronSandFactory.Sheet PUFF;

    public static final IronSandFactory.Sheet FINE;

    static {
        {
            IronSandFactory f = new IronSandFactory();

            f.falloffStart = 0.72f;
            f.falloffPow = 1.6f;
            f.coverage = 0.96f;
            f.hardness = 9.0f;
            f.clump = 0.14f;
            PUFF = f.generate("iron_sand_puff", GEN, 20260729L);
        }

        {
            IronSandFactory f = new IronSandFactory();
            f.coverage = 0.78f;

            f.clump = 0.16f;
            f.falloffStart = 0.16f;
            f.falloffPow = 1.25f;
            f.hardness = 7.0f;
            FINE = f.generate("iron_sand_fine", GEN, 20260730L);
        }
    }

    private IronSandPatterns() {}
}
