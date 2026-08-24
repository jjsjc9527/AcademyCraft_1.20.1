package cn.academy.ability.vanilla;

import cn.academy.ability.Category;
import cn.academy.ability.Skill;
import cn.academy.ability.develop.condition.DevConditionAnySkillOfLevel;
import cn.academy.ability.vanilla.generic.skill.SkillBrainCourse;
import cn.academy.ability.vanilla.generic.skill.SkillBrainCourseAdvanced;
import cn.academy.ability.vanilla.generic.skill.SkillEsperCultivation;
import cn.academy.ability.vanilla.generic.skill.SkillMindCalcCourse;
import cn.academy.ability.vanilla.generic.skill.SkillMindCourse;

public final class VanillaCategories {

    private VanillaCategories() {}

    public static void addGenericSkills(Category category) {
        Skill bc = new SkillBrainCourse(),
                bca = new SkillBrainCourseAdvanced(),
                mc = new SkillMindCourse();

        bc.setPosition(42, 10);
        bca.setPosition(17, 42);
        mc.setPosition(98, 12);

        category.addSkill(bc);
        category.addSkill(bca);
        category.addSkill(mc);

        bc.addDevCondition(new DevConditionAnySkillOfLevel(3));

        bca.setParent(bc);
        bca.addDevCondition(new DevConditionAnySkillOfLevel(4));

        mc.addDevCondition(new DevConditionAnySkillOfLevel(4));
    }

    public static void addLateGenericSkills(Category category) {
        Skill mcc = new SkillMindCalcCourse(),
                ec = new SkillEsperCultivation();

        mcc.setPosition(75, 42);
        ec.setPosition(121, 42);

        category.addSkill(mcc);
        category.addSkill(ec);

        Skill mc = category.getSkill("mind_course");
        if (mc != null) {
            mcc.setParent(mc);
            ec.setParent(mc);
        }
    }
}
