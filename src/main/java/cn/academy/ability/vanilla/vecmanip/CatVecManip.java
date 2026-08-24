package cn.academy.ability.vanilla.vecmanip;

import cn.academy.ability.Category;
import cn.academy.ability.CategoryManager;
import cn.academy.ability.vanilla.VanillaCategories;
import cn.academy.ability.vanilla.vecmanip.skill.DirectedShock;

public final class CatVecManip {

    public static final Category CATEGORY = new Category("vecmanip");

    private CatVecManip() {}

    public static void register() {

        CATEGORY.setColorStyle(0, 0, 0);

        DirectedShock.INSTANCE.setPosition(16, 45);

        CATEGORY.addSkill(DirectedShock.INSTANCE);

        VanillaCategories.addGenericSkills(CATEGORY);

        cn.academy.ability.vanilla.vecmanip.skill.Groundshock.INSTANCE.setPosition(64, 85);
        CATEGORY.addSkill(cn.academy.ability.vanilla.vecmanip.skill.Groundshock.INSTANCE);
        cn.academy.ability.vanilla.vecmanip.skill.Groundshock.INSTANCE
                .setParent(DirectedShock.INSTANCE);

        cn.academy.ability.vanilla.vecmanip.skill.DirectedBlastwave.INSTANCE.setPosition(136, 80);
        CATEGORY.addSkill(cn.academy.ability.vanilla.vecmanip.skill.DirectedBlastwave.INSTANCE);
        cn.academy.ability.vanilla.vecmanip.skill.DirectedBlastwave.INSTANCE
                .setParent(cn.academy.ability.vanilla.vecmanip.skill.Groundshock.INSTANCE);

        cn.academy.ability.vanilla.vecmanip.skill.VecAccel.INSTANCE.setPosition(76, 40);
        CATEGORY.addSkill(cn.academy.ability.vanilla.vecmanip.skill.VecAccel.INSTANCE);
        cn.academy.ability.vanilla.vecmanip.skill.VecAccel.INSTANCE
                .setParent(DirectedShock.INSTANCE);

        cn.academy.ability.vanilla.vecmanip.skill.VecDeviation.INSTANCE.setPosition(210, 50);
        CATEGORY.addSkill(cn.academy.ability.vanilla.vecmanip.skill.VecDeviation.INSTANCE);
        cn.academy.ability.vanilla.vecmanip.skill.VecDeviation.INSTANCE
                .setParent(cn.academy.ability.vanilla.vecmanip.skill.VecAccel.INSTANCE);

        cn.academy.ability.vanilla.vecmanip.skill.VecDeviation.init();

        cn.academy.ability.vanilla.vecmanip.skill.StormWing.INSTANCE.setPosition(130, 20);
        CATEGORY.addSkill(cn.academy.ability.vanilla.vecmanip.skill.StormWing.INSTANCE);
        cn.academy.ability.vanilla.vecmanip.skill.StormWing.INSTANCE
                .setParent(cn.academy.ability.vanilla.vecmanip.skill.VecAccel.INSTANCE);

        cn.academy.ability.vanilla.vecmanip.skill.PlasmaCannon.INSTANCE.setPosition(175, 14);
        CATEGORY.addSkill(cn.academy.ability.vanilla.vecmanip.skill.PlasmaCannon.INSTANCE);

        cn.academy.ability.vanilla.vecmanip.skill.PlasmaCannon.INSTANCE
                .setParent(cn.academy.ability.vanilla.vecmanip.skill.StormWing.INSTANCE, 0.8f);

        CategoryManager.INSTANCE.register(CATEGORY);

        VanillaCategories.addLateGenericSkills(CATEGORY);

        cn.academy.ability.vanilla.vecmanip.advanced.DualWing.INSTANCE.setPosition(10, 62);
        CATEGORY.addSkill(cn.academy.ability.vanilla.vecmanip.advanced.DualWing.INSTANCE);

        cn.academy.ability.vanilla.vecmanip.advanced.DualWing.INSTANCE
                .addSkillDep(cn.academy.ability.vanilla.vecmanip.skill.StormWing.INSTANCE, 1.0f);

        cn.academy.ability.vanilla.vecmanip.advanced.DualWing.init();

        cn.academy.ability.vanilla.vecmanip.advanced.AbyssStride.INSTANCE.setPosition(80, 62);
        CATEGORY.addSkill(cn.academy.ability.vanilla.vecmanip.advanced.AbyssStride.INSTANCE);
        cn.academy.ability.vanilla.vecmanip.advanced.AbyssStride.INSTANCE
                .setParent(cn.academy.ability.vanilla.vecmanip.advanced.DualWing.INSTANCE, 1.0f);

        cn.academy.ability.vanilla.vecmanip.advanced.AbsoluteAbility.INSTANCE.setPosition(218, 63);
        CATEGORY.addSkill(cn.academy.ability.vanilla.vecmanip.advanced.AbsoluteAbility.INSTANCE);
    }
}
