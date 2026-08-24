package cn.academy.gravity;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class ACGravity {

    private ACGravity() {}

    public static final long ROTATION_TIME_MS = 500L;

    public static Direction getGravityDirection(Entity entity) {
        return entity instanceof GravityEntity g ? g.academy_getGravityDirection() : Direction.DOWN;
    }

    public static RotationAnimation getRotationAnimation(Entity entity) {
        return entity instanceof GravityEntity g ? g.academy_getRotationAnimation() : null;
    }

    public static void setGravityDirection(Entity entity, Direction newDir, boolean animate) {
        if (!(entity instanceof GravityEntity ge)) return;
        Direction oldDir = ge.academy_getGravityDirection();
        ge.academy_setGravityDirection(newDir, animate);
        if (oldDir == newDir || !canChangeGravity(entity)) return;
        applyChange(entity, oldDir, newDir, animate);
    }

    public static void setGravityDirectionRaw(Entity entity, Direction dir, boolean animate) {
        if (entity instanceof GravityEntity g) g.academy_setGravityDirection(dir, animate);
    }

    public static void initGravityDirection(Entity entity, Direction dir) {
        if (!(entity instanceof GravityEntity ge)) return;
        ge.academy_setGravityDirection(dir, false);
        entity.setPos(entity.getX(), entity.getY(), entity.getZ());
    }

    private static void applyChange(Entity entity, Direction oldG, Direction newG, boolean animate) {
        entity.fallDistance = 0f;

        Vec3 relCenter = getLocalRotationCenter(entity, oldG, newG);
        Vec3 oldPos = entity.position();
        Vec3 oldLastTick = new Vec3(entity.xOld, entity.yOld, entity.zOld);
        Vec3 rotationCenter = oldPos.add(RotationUtil.vecPlayerToWorld(relCenter, oldG));
        Vec3 newPos = rotationCenter.subtract(RotationUtil.vecPlayerToWorld(relCenter, newG));
        Vec3 posTranslation = newPos.subtract(oldPos);
        Vec3 newLastTick = oldLastTick.add(posTranslation);

        entity.setPos(newPos);
        entity.xo = newLastTick.x; entity.yo = newLastTick.y; entity.zo = newLastTick.z;
        entity.xOld = newLastTick.x; entity.yOld = newLastTick.y; entity.zOld = newLastTick.z;

        adjustEntityPosition(entity, oldG, entity.getBoundingBox());
        settleOntoSurface(entity, newG);

        if (entity.level().isClientSide() && entity instanceof GravityEntity ge) {
            long timeMs = entity.level().getGameTime() * 50L;
            ge.academy_getRotationAnimation().startRotationAnimation(
                    newG, oldG, animate ? ROTATION_TIME_MS : 0L,
                    entity, timeMs, true, relCenter);
        }

        Vec3 realWorldVel = getRealWorldVelocity(entity, oldG);
        entity.setDeltaMovement(RotationUtil.vecWorldToPlayer(realWorldVel, newG));
    }

    private static Vec3 getLocalRotationCenter(Entity entity, Direction oldG, Direction newG) {
        if (newG.getOpposite() == oldG) {
            EntityDimensions dim = entity.getDimensions(entity.getPose());
            return new Vec3(0, dim.height / 2.0, 0);
        }
        return Vec3.ZERO;
    }

    private static Vec3 getRealWorldVelocity(Entity entity, Direction oldG) {
        if (entity.isControlledByLocalInstance()) {
            return new Vec3(entity.getX() - entity.xo, entity.getY() - entity.yo, entity.getZ() - entity.zo);
        }
        return RotationUtil.vecPlayerToWorld(entity.getDeltaMovement(), oldG);
    }

    private static void adjustEntityPosition(Entity entity, Direction oldG, AABB box) {
        Direction movingDir = oldG.getOpposite();
        Iterable<VoxelShape> collisions = entity.level().getCollisions(entity, box.inflate(-0.01));
        AABB total = null;
        for (VoxelShape s : collisions) {
            if (!s.isEmpty()) {
                AABB b = s.bounds();
                total = (total == null) ? b : total.minmax(b);
            }
        }
        if (total != null) {
            entity.setPos(entity.position().add(getPositionAdjustmentOffset(box, total, movingDir)));
        }
    }

    private static void settleOntoSurface(Entity entity, Direction newG) {
        AABB box = entity.getBoundingBox();
        Direction.Axis axis = newG.getAxis();
        double push = 0;
        for (VoxelShape s : entity.level().getBlockCollisions(entity, box)) {
            if (s.isEmpty()) continue;
            AABB b = s.bounds();

            double overlap = (newG.getAxisDirection() == Direction.AxisDirection.POSITIVE)
                    ? box.max(axis) - b.min(axis)
                    : b.max(axis) - box.min(axis);
            if (overlap > 0 && overlap <= 0.5) push = Math.max(push, overlap);
        }
        if (push > 0) {

            Vec3 off = new Vec3(newG.getOpposite().step()).scale(push);
            entity.setPos(entity.position().add(off));
            entity.xo += off.x; entity.yo += off.y; entity.zo += off.z;
            entity.xOld += off.x; entity.yOld += off.y; entity.zOld += off.z;
        }
    }

    private static Vec3 getPositionAdjustmentOffset(AABB entityBox, AABB nearbyUnion, Direction movingDir) {
        Direction.Axis axis = movingDir.getAxis();
        double offset = 0;
        if (movingDir.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
            double pushing = nearbyUnion.max(axis);
            double pushed = entityBox.min(axis);
            if (pushing > pushed) offset = pushing - pushed;
        } else {
            double pushing = nearbyUnion.min(axis);
            double pushed = entityBox.max(axis);
            if (pushing < pushed) offset = pushed - pushing;
        }
        return new Vec3(movingDir.step()).scale(offset);
    }

    public static boolean canChangeGravity(Entity entity) {
        return entity instanceof Player;
    }

    public static Vec3 getWorldVelocity(Entity entity) {
        return RotationUtil.vecPlayerToWorld(entity.getDeltaMovement(), getGravityDirection(entity));
    }

    public static void setWorldVelocity(Entity entity, Vec3 worldVelocity) {
        entity.setDeltaMovement(RotationUtil.vecWorldToPlayer(worldVelocity, getGravityDirection(entity)));
    }
}
