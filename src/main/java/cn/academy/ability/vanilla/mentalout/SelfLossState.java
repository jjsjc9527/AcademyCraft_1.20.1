package cn.academy.ability.vanilla.mentalout;

import cn.academy.ability.vanilla.mentalout.advanced.SelfLoss;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.util.RandUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class SelfLossState {

    private static final String TICKS = "mo_sloss_ticks";

    private static final String OWNER = "mo_sloss_owner";

    private static final int SYNC_INTERVAL = 10;

    private SelfLossState() {}

    public static int getTicks(Entity e) {
        return e.getPersistentData().getInt(TICKS);
    }

    public static boolean isActive(Entity e) {
        return getTicks(e) > 0;
    }

    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    public static boolean isLocalPlayerLost() {
        net.minecraft.client.player.LocalPlayer p = net.minecraft.client.Minecraft.getInstance().player;
        return p != null && isActive(p);
    }

    public static void setTicks(Entity e, int ticks) {
        e.getPersistentData().putInt(TICKS, Math.max(0, ticks));
    }

    public static void apply(LivingEntity target, int ticks, Player owner) {
        setTicks(target, Math.max(getTicks(target), ticks));
        if (owner != null) {
            target.getPersistentData().putUUID(OWNER, owner.getUUID());
        }
        target.setDeltaMovement(Vec3.ZERO);
        target.hasImpulse = true;
        sync(target);
    }

    public static Player ownerOf(Entity e) {
        CompoundTag d = e.getPersistentData();
        return d.hasUUID(OWNER) ? e.level().getPlayerByUUID(d.getUUID(OWNER)) : null;
    }

    public static void clear(LivingEntity e) {
        setTicks(e, 0);
        e.getPersistentData().remove(OWNER);

        if (!e.level().isClientSide) {
            sync(e);
        }
    }

    private static void sync(Entity target) {
        NetworkMessage.sendToTracking(target, SelfLoss.INSTANCE, SelfLoss.MSG_SYNC,
                target, getTicks(target));
    }

    public static void tick(Entity entity) {
        if (!(entity instanceof LivingEntity e)) {
            return;
        }
        int ticks = getTicks(e);
        if (ticks <= 0) {
            return;
        }
        if (!e.isAlive()) {
            clear(e);
            return;
        }

        setTicks(e, ticks - 1);

        if (e.level().isClientSide) {
            if (ticks % 5 == 0) {
                spawnParticles(e);
            }
        } else {
            if (ticks % SYNC_INTERVAL == 0) {
                sync(e);
            }
            if (ticks == 1) {
                clear(e);
            }
        }
    }

    private static void spawnParticles(LivingEntity e) {
        double w = e.getBbWidth(), h = e.getBbHeight();
        for (int i = 0; i < 2; ++i) {
            Vec3 p = FaintState.bodyToWorld(e,
                    RandUtils.ranged(-w * 0.6, w * 0.6),
                    RandUtils.ranged(h * 0.3, h),
                    RandUtils.ranged(-w * 0.6, w * 0.6));
            e.level().addParticle(ParticleTypes.SMOKE, p.x, p.y, p.z, 0, -0.02, 0);
        }
    }
}
