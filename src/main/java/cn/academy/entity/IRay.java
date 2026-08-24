package cn.academy.entity;

import cn.lambdalib2.util.ViewOptimize;
import net.minecraft.world.phys.Vec3;

public interface IRay extends ViewOptimize.IAssociatePlayer {

    void onRenderTick();

    Vec3 getRayPosition();

    boolean needsViewOptimize();

    double getLength();

    double getAlpha();

    double getGlowAlpha();

    double getStartFix();

    double getWidth();

    default Vec3[] getPath() {
        return null;
    }

    default double[] getPathCum() {
        return null;
    }
}
