package cn.lambdalib2.util;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public final class GameTimer {

    private GameTimer() {}

    public static double getAbsTime() {
        return Util.getMillis() / 1000.0;
    }

    public static double getTime() {
        return getAbsTime();
    }

    private static double pausableSeconds = 0.0;
    private static long pausableLastMillis = -1L;

    public static java.util.function.BooleanSupplier extraFreeze = () -> false;

    private static double pendingSkip = 0.0;

    @OnlyIn(Dist.CLIENT)
    public static double getPausableTime() {
        long now = Util.getMillis();
        if (pausableLastMillis < 0L) {
            pausableLastMillis = now;
        }
        double dt = (now - pausableLastMillis) / 1000.0;
        pausableLastMillis = now;

        if (!Minecraft.getInstance().isPaused()) {
            if (extraFreeze.getAsBoolean()) {
                pendingSkip += dt;
            } else {
                pausableSeconds += dt + pendingSkip;
                pendingSkip = 0.0;
            }
        }
        return pausableSeconds;
    }
}
