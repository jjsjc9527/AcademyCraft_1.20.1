package cn.academy.ability.vanilla.teleporter.util;

import cn.academy.ACParticles;
import cn.academy.ability.AbilityContext;
import cn.academy.ability.vanilla.teleporter.passiveskill.DimFoldingTheorem;
import cn.academy.ability.vanilla.teleporter.passiveskill.SpaceFluctuation;
import cn.academy.datapart.AbilityData;
import cn.academy.event.ability.AbilityEvent;
import cn.lambdalib2.util.RandUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;

public final class TPSkillHelper {

    private static final float[] RATES = {1.3f, 1.6f, 2.6f};

    private TPSkillHelper() {}

    public static void attack(AbilityContext ctx, Entity target, float damage) {
        ctx.attack(target, rollCrit(ctx, target, damage));
    }

    public static void attackIgnoreArmor(AbilityContext ctx, Entity target, float damage) {
        ctx.attackIgnoreArmor(target, rollCrit(ctx, target, damage));
    }

    private static float rollCrit(AbilityContext ctx, Entity target, float damage) {
        AbilityData aData = ctx.aData;
        Player player = ctx.player;

        for (int i = 0; i < 3; i++) {
            if (RandUtils.nextFloat() >= prob(aData, i)) {
                continue;
            }
            damage *= RATES[i];

            aData.addSkillExp(DimFoldingTheorem.INSTANCE, (i + 1) * 0.005f);
            aData.addSkillExp(SpaceFluctuation.INSTANCE, 0.0001f);

            MinecraftForge.EVENT_BUS.post(new TPCritHitEvent(player, target, i));
            spawnCritEffect(target);
            break;
        }
        return damage;
    }

    private static float prob(AbilityData data, int level) {
        float dimFolding = data.isSkillLearned(DimFoldingTheorem.INSTANCE)
                ? data.getSkillExp(DimFoldingTheorem.INSTANCE) : -1;
        float spaceFluct = data.isSkillLearned(SpaceFluctuation.INSTANCE)
                ? data.getSkillExp(SpaceFluctuation.INSTANCE) : -1;
        switch (level) {
            case 0:
                return tryLerp(0.10f, 0.20f, dimFolding) + tryLerp(0.18f, 0.25f, spaceFluct);
            case 1:
                return tryLerp(0.10f, 0.15f, spaceFluct);
            case 2:
                return tryLerp(0.01f, 0.03f, spaceFluct);
            default:
                throw new IllegalArgumentException("crit level " + level);
        }
    }

    private static float tryLerp(float a, float b, float l) {
        return l == -1 ? 0 : a + l * (b - a);
    }

    private static void spawnCritEffect(Entity t) {
        if (!(t.level() instanceof ServerLevel sl)) {
            return;
        }
        int count = RandUtils.rangei(5, 8);
        while (count-- > 0) {
            double angle = RandUtils.ranged(0, Math.PI * 2);
            double r = RandUtils.ranged(t.getBbWidth() * 0.5, t.getBbWidth() * 0.7);
            double h = RandUtils.ranged(0, 1) * t.getBbHeight();
            sl.sendParticles(ACParticles.FORMULA.get(),
                    t.getX() + r * Math.sin(angle), t.getY() + h, t.getZ() + r * Math.cos(angle),
                    1, 0.03, 0.03, 0.03, 0.01);
        }
    }

    public static class TPCritHitEvent extends AbilityEvent {
        public final int level;
        public final Entity target;

        public TPCritHitEvent(Player player, Entity target, int level) {
            super(player);
            this.target = target;
            this.level = level;
        }
    }

}
