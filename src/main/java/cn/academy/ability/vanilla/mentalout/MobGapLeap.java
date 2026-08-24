package cn.academy.ability.vanilla.mentalout;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class MobGapLeap {

    private static final int MAX_GAP = 4;

    private static final int AIRBORNE_TICKS = 12;

    private static final double SPEED_DIVISOR = 6.5;

    private static final double MIN_SPEED = 0.25;
    private static final double MAX_SPEED = 0.62;

    private static final String LEAP_AT = "acGapLeapAt";

    private MobGapLeap() {}

    public static boolean tryLeap(Mob mob, Vec3 goal) {
        CompoundTag d = mob.getPersistentData();
        long now = mob.level().getGameTime();
        if (now - d.getLong(LEAP_AT) < AIRBORNE_TICKS) {
            return true;
        }

        if (!mob.onGround() || mob.isInWater() || mob.isPassenger() || mob.isNoGravity()) {
            return false;
        }
        Vec3 self = mob.position();
        double dx = goal.x - self.x, dz = goal.z - self.z;
        double flat = Math.sqrt(dx * dx + dz * dz);
        if (flat < 1.0e-4) {
            return false;
        }
        Vec3 dir = new Vec3(dx / flat, 0.0, dz / flat);

        Vec3 ahead = self.add(dir);
        if (!passable(mob, ahead) || standable(mob, ahead)) {
            return false;
        }

        for (int n = 2; n <= MAX_GAP; n++) {
            Vec3 land = self.add(dir.scale(n));
            if (!passable(mob, land)) {
                return false;
            }
            if (!standable(mob, land)) {
                continue;
            }

            if (land.distanceToSqr(goal) >= self.distanceToSqr(goal)) {
                return false;
            }
            leap(mob, dir, n, d, now);
            return true;
        }
        return false;
    }

    private static void leap(Mob mob, Vec3 dir, double dist, CompoundTag d, long now) {

        mob.getNavigation().stop();
        mob.getJumpControl().jump();

        double v = Mth.clamp(dist / SPEED_DIVISOR, MIN_SPEED, MAX_SPEED);
        Vec3 m = mob.getDeltaMovement();

        mob.setDeltaMovement(dir.x * v, m.y, dir.z * v);
        mob.hasImpulse = true;

        float yaw = (float) (Mth.atan2(dir.z, dir.x) * (180.0 / Math.PI)) - 90.0f;
        mob.setYRot(yaw);
        mob.yBodyRot = yaw;
        d.putLong(LEAP_AT, now);
    }

    private static boolean standable(Mob mob, Vec3 pos) {
        AABB box = box(mob, pos);
        return mob.level().noCollision(mob, box)
                && !mob.level().noCollision(mob, box.move(0.0, -0.2, 0.0));
    }

    private static boolean passable(Mob mob, Vec3 pos) {
        return mob.level().noCollision(mob, box(mob, pos));
    }

    private static AABB box(Mob mob, Vec3 pos) {
        Vec3 self = mob.position();
        return mob.getBoundingBox().move(pos.x - self.x, pos.y - self.y, pos.z - self.z);
    }
}
