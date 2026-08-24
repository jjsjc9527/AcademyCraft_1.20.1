package cn.academy.ability.vanilla.mentalout;

import cn.academy.ability.AbilityContext;
import cn.academy.ability.Skill;
import cn.academy.ability.vanilla.electromaster.CatElectromaster;
import cn.academy.ability.vanilla.mentalout.passiveskill.MindManip;
import cn.academy.datapart.AbilityData;
import cn.academy.util.RayReflect;
import cn.lambdalib2.datapart.EntityData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class MentalImmune {

    private MentalImmune() {}

    private static final float REFLECT_DIFFICULTY = 0.5f;

    public static boolean blocked(AbilityContext ctx, LivingEntity target) {
        if (ctx == null || target == null) {
            return false;
        }

        if (isImmuneCategory(target)) {
            sparkFx(target);
            return true;
        }
        return deflectedByVecManip(ctx, target);
    }

    public static boolean blocked(Player caster, Skill skill, LivingEntity target) {
        if (caster == null || skill == null || target == null) {
            return false;
        }
        if (isImmuneCategory(target)) {
            sparkFx(target);
            return true;
        }
        return deflectedByVecManip(AbilityContext.of(caster, skill), target);
    }

    private static long lastSoundTick = Long.MIN_VALUE;

    private static void sparkFx(LivingEntity target) {
        if (!(target.level() instanceof net.minecraft.server.level.ServerLevel level)) {
            return;
        }
        MindManip.sendImmuneArc(target);

        long now = level.getGameTime();
        if (now == lastSoundTick) {
            return;
        }
        lastSoundTick = now;

        level.playSound(null, target.getX(), target.getEyeY(), target.getZ(),
                cn.academy.ACSounds.EM_ARC_WEAK.get(),
                net.minecraft.sounds.SoundSource.PLAYERS, 0.35f, 1.0f);
    }

    private static boolean isImmuneCategory(LivingEntity target) {
        if (!(target instanceof Player p) || !EntityData.isReady(p)) {
            return false;
        }
        AbilityData aData = AbilityData.get(p);
        return aData != null && aData.getCategoryNullable() == CatElectromaster.CATEGORY;
    }

    private static boolean deflectedByVecManip(AbilityContext ctx, LivingEntity target) {
        Vec3 from = ctx.player.getEyePosition(1.0f);
        Vec3 to = target.getEyePosition(1.0f).subtract(from);
        final Vec3 dir = to.lengthSqr() < 1.0e-6 ? ctx.player.getViewVector(1.0f) : to.normalize();
        return ctx.tryReflect(target, ev -> {
            RayReflect.fill(ev, from, dir, target, 0);
            ev.difficulty = REFLECT_DIFFICULTY;

            ev.deflectable = false;
        });
    }
}
