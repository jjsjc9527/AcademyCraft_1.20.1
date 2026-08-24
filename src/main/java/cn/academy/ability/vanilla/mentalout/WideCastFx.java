package cn.academy.ability.vanilla.mentalout;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

public final class WideCastFx {

    private WideCastFx() {}

    public static void at(Entity target, ParticleOptions type, int count, double speed) {
        if (!(target.level() instanceof ServerLevel level)) {
            return;
        }
        double r = Math.max(0.3, target.getBbWidth() * 0.6);
        double h = Math.max(0.3, target.getBbHeight() * 0.4);
        level.sendParticles(type,
                target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                count, r, h, r, speed);
    }

    public static void atHead(Entity target, ParticleOptions type, int count, double speed) {
        if (!(target.level() instanceof ServerLevel level)) {
            return;
        }
        double r = Math.max(0.15, target.getBbWidth() * 0.3);
        level.sendParticles(type,
                target.getX(), target.getEyeY(), target.getZ(),
                count, r, 0.12, r, speed);
    }
}
