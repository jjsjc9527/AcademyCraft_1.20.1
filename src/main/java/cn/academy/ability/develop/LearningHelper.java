package cn.academy.ability.develop;

import cn.academy.ability.Skill;
import cn.academy.ability.develop.action.IDevelopAction;
import cn.academy.ability.develop.condition.IDevCondition;
import cn.academy.datapart.AbilityData;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public final class LearningHelper {

    private LearningHelper() {}

    public static boolean canLevelUp(DeveloperType type, AbilityData aData) {
        return !aData.hasCategory() || aData.canLevelUp();
    }

    public static boolean canBePotentiallyLearned(AbilityData data, Skill skill) {
        return data.getLevel() >= skill.getLevel()
                || data.isSkillLearned(skill)
                || allParentsLearned(data, skill);
    }

    private static boolean allParentsLearned(AbilityData data, Skill skill) {
        for (Skill p : skill.getTreeParents()) {
            if (!data.isSkillLearned(p)) {
                return false;
            }
        }
        return true;
    }

    public static boolean canLearn(AbilityData data, IDeveloper dev, Skill skill) {
        for (IDevCondition cond : skill.getDevConditions()) {
            if (!cond.accepts(data, dev, skill)) {
                return false;
            }
        }
        return true;
    }

    public static double getEstimatedConsumption(Player player, DeveloperType blktype, IDevelopAction type) {
        return blktype.getCPS() * type.getStimulations(player);
    }

    public static final int ADVANCED_TREE_LEVEL = 5;

    public static boolean canUseAdvancedTree(@Nullable IDeveloper developer, AbilityData aData) {
        return developer != null
                && developer.getDeveloperType() == DeveloperType.ADVANCED
                && aData.hasCategory()
                && aData.getLevel() >= ADVANCED_TREE_LEVEL;
    }
}
