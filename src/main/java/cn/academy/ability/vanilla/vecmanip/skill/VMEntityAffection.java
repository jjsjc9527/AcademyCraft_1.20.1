package cn.academy.ability.vanilla.vecmanip.skill;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.ThrownExperienceBottle;
import net.minecraft.world.entity.projectile.ThrownPotion;

import java.util.UUID;

public final class VMEntityAffection {

    private static final String TAG_MARK = "ac_vm_deviated";

    private static final String TAG_REFLECTOR = "ac_vm_reflector";

    private VMEntityAffection() {}

    public static boolean canAffect(Entity entity) {
        if (entity instanceof cn.academy.entity.EntityShiftBlock) {
            return true;
        }

        if (entity instanceof cn.academy.entity.EntityShiftNeedle needle) {
            return !needle.isStuck();
        }
        if (!(entity instanceof Projectile)) {
            return false;
        }
        return !(entity instanceof ThrownExperienceBottle);
    }

    public static boolean shouldTransferOwner(Projectile proj) {
        return !(proj instanceof net.minecraft.world.entity.projectile.FishingHook)
                && !(proj instanceof net.minecraft.world.entity.projectile.ThrownEnderpearl);
    }

    public static Entity ownerOf(Entity entity) {
        if (entity instanceof Projectile p) {
            return p.getOwner();
        }
        if (entity instanceof cn.academy.entity.EntityShiftBlock b) {
            return b.getOwnerPlayer();
        }
        if (entity instanceof cn.academy.entity.EntityShiftNeedle n) {
            return n.getOwnerPlayer();
        }
        return null;
    }

    public static float difficulty(Entity entity) {

        if (entity instanceof cn.academy.entity.EntityShiftBlock) return 1.5f;

        if (entity instanceof cn.academy.entity.EntityShiftNeedle) return 0.1f;
        if (entity instanceof ThrownPotion) return 1.4f;
        if (entity instanceof Snowball) return 0.1f;
        return 1.0f;
    }

    public static void mark(Entity entity, UUID reflector) {
        entity.getPersistentData().putBoolean(TAG_MARK, true);
        entity.getPersistentData().putUUID(TAG_REFLECTOR, reflector);
    }

    public static boolean isMarked(Entity entity) {
        return entity.getPersistentData().getBoolean(TAG_MARK);
    }

    public static UUID getReflector(Entity entity) {
        if (!entity.getPersistentData().hasUUID(TAG_REFLECTOR)) {
            return null;
        }
        return entity.getPersistentData().getUUID(TAG_REFLECTOR);
    }
}
