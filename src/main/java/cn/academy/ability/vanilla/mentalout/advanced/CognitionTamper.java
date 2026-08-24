package cn.academy.ability.vanilla.mentalout.advanced;

import cn.academy.ability.AbilityPipeline;
import cn.academy.ability.vanilla.mentalout.WideCastFx;
import cn.academy.ability.vanilla.mentalout.WideCastable;
import cn.academy.ability.vanilla.mentalout.skill.Impression;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public class CognitionTamper extends MentalAdvSkill implements WideCastable {

    public static final CognitionTamper INSTANCE = new CognitionTamper();

    public static boolean isLearned(Player player) {
        return INSTANCE.isLearnedBy(player);
    }

    private CognitionTamper() {
        super("cognition_tamper", Impression.INSTANCE, true);
    }

    @Override
    public boolean wideAimOnly() {
        return true;
    }

    @Override
    public boolean wideAimFallbackToCrowd() {
        return true;
    }

    @Override
    public boolean wideApply(Call call, LivingEntity target) {
        if (call.crowdCount == 1 || call.aimEntity == null) {
            return applySingle(call, target);
        }
        return applyCrowd(call, target);
    }

    private boolean applySingle(Call call, LivingEntity target) {
        CognitionRewrite.Result r = CognitionRewrite.set(call.caster, target, call.caster, false);
        if (r == CognitionRewrite.Result.NONE) {
            return false;
        }

        boolean applied = r == CognitionRewrite.Result.APPLIED;
        cn.academy.datapart.RemoteData rd = cn.academy.datapart.RemoteData.get(call.caster);

        if (rd != null && (applied ? rd.addAlly(target.getUUID(), target.getType())
                                   : rd.removeAlly(target.getUUID()))) {
            rd.sync();
        }

        WideCastFx.at(target, applied ? ParticleTypes.ENCHANT : ParticleTypes.SMOKE, 16, 0.05);

        if (applied) {
            cn.academy.network.CogMarkMessage.send(call.caster, target);
        }
        return true;
    }

    private boolean applyCrowd(Call call, LivingEntity target) {
        double r = call.crowdRange;
        double r2 = r * r;
        List<Mob> around = target.level().getEntitiesOfClass(Mob.class,
                target.getBoundingBox().inflate(r),

                e -> e != target && e.isAlive() && e.distanceToSqr(target) <= r2
                        && AbilityPipeline.canTarget(call.caster, e));

        around.sort(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(target)));
        if (around.size() > call.crowdCount) {
            around = around.subList(0, call.crowdCount);
        }

        boolean undo = false;
        for (Mob m : around) {
            if (CognitionRewrite.has(call.caster, m, target, true)) {
                undo = true;
                break;
            }
        }
        int n = 0;
        for (Mob m : around) {
            boolean had = CognitionRewrite.has(call.caster, m, target, true);
            if (undo != had) {
                continue;
            }
            if (CognitionRewrite.set(call.caster, m, target, true) != CognitionRewrite.Result.NONE) {
                WideCastFx.at(m, undo ? ParticleTypes.SMOKE : ParticleTypes.ANGRY_VILLAGER, 8, 0.03);

                cn.academy.datapart.RemoteData rd2 = cn.academy.datapart.RemoteData.get(call.caster);
                if (rd2 != null && (undo ? rd2.removeEnraged(m.getUUID())
                                         : rd2.addEnraged(m.getUUID()))) {
                    rd2.sync();
                }
                n++;
            }
        }
        if (n > 0) {
            WideCastFx.at(target, undo ? ParticleTypes.SMOKE : ParticleTypes.WITCH, 16, 0.05);
        }

        return n > 0;
    }

    @Override
    public boolean releaseFrom(Player caster, LivingEntity target) {
        boolean any = CognitionRewrite.clearFrom(caster, target);

        cn.academy.datapart.RemoteData rd = cn.academy.datapart.RemoteData.get(caster);
        if (rd != null && (rd.removeAlly(target.getUUID()) | rd.removeEnraged(target.getUUID()))) {
            rd.sync();
        }
        return any;
    }

    @Override
    public float wideExp() {
        return 0.003f;
    }
}
