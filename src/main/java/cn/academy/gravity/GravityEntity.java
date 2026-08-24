package cn.academy.gravity;

import net.minecraft.core.Direction;

public interface GravityEntity {

    Direction academy_getGravityDirection();

    void academy_setGravityDirection(Direction dir, boolean animate);

    RotationAnimation academy_getRotationAnimation();
}
