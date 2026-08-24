package cn.academy.client.render;

import net.minecraft.world.phys.Vec3;

public interface ACEffect {

    boolean effectExpired(double now);

    default int effectOrder(Vec3 camera) {
        return 0;
    }
}
