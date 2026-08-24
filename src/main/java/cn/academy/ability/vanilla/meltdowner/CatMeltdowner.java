package cn.academy.ability.vanilla.meltdowner;

import cn.academy.ability.Category;
import cn.academy.ability.CategoryManager;
import cn.academy.ability.vanilla.VanillaCategories;
import cn.academy.ability.vanilla.meltdowner.skill.ElectronBomb;

public final class CatMeltdowner {

    public static final Category CATEGORY = new Category("meltdowner");

    private CatMeltdowner() {}

    public static void register() {

        CATEGORY.setColorStyle(126, 255, 132, 80);

        ElectronBomb.INSTANCE.setPosition(15, 45);

        CATEGORY.addSkill(ElectronBomb.INSTANCE);

        VanillaCategories.addGenericSkills(CATEGORY);

        cn.academy.ability.vanilla.meltdowner.skill.LightShield.INSTANCE.setPosition(55, 15);
        CATEGORY.addSkill(cn.academy.ability.vanilla.meltdowner.skill.LightShield.INSTANCE);

        cn.academy.ability.vanilla.meltdowner.skill.LightShield.INSTANCE
                .setParent(ElectronBomb.INSTANCE, 1.0f);

        cn.academy.ability.vanilla.meltdowner.skill.LightShield.init();

        cn.academy.ability.vanilla.meltdowner.passiveskill.RadiationIntensify.INSTANCE.setPosition(35, 75);
        CATEGORY.addSkill(cn.academy.ability.vanilla.meltdowner.passiveskill.RadiationIntensify.INSTANCE);
        cn.academy.ability.vanilla.meltdowner.passiveskill.RadiationIntensify.INSTANCE
                .setParent(ElectronBomb.INSTANCE, 0.5f);

        cn.academy.ability.vanilla.meltdowner.skill.MDDamageHelper.init();

        cn.academy.ability.vanilla.meltdowner.skill.MdBeam.init();

        cn.academy.ability.vanilla.meltdowner.skill.ScatterBomb.INSTANCE.setPosition(70, 50);
        CATEGORY.addSkill(cn.academy.ability.vanilla.meltdowner.skill.ScatterBomb.INSTANCE);
        cn.academy.ability.vanilla.meltdowner.skill.ScatterBomb.INSTANCE
                .setParent(ElectronBomb.INSTANCE, 0.8f);

        cn.academy.ability.vanilla.meltdowner.skill.Meltdowner.INSTANCE.setPosition(115, 40);
        CATEGORY.addSkill(cn.academy.ability.vanilla.meltdowner.skill.Meltdowner.INSTANCE);
        cn.academy.ability.vanilla.meltdowner.skill.Meltdowner.INSTANCE
                .setParent(cn.academy.ability.vanilla.meltdowner.skill.ScatterBomb.INSTANCE, 0.8f);
        cn.academy.ability.vanilla.meltdowner.skill.Meltdowner.INSTANCE
                .addSkillDep(cn.academy.ability.vanilla.meltdowner.skill.LightShield.INSTANCE, 0.8f);

        cn.academy.ability.vanilla.meltdowner.skill.Meltdowner.init();

        cn.academy.ability.vanilla.meltdowner.passiveskill.RayBarrage.INSTANCE.setPosition(140, 10);
        CATEGORY.addSkill(cn.academy.ability.vanilla.meltdowner.passiveskill.RayBarrage.INSTANCE);
        cn.academy.ability.vanilla.meltdowner.passiveskill.RayBarrage.INSTANCE
                .setParent(cn.academy.ability.vanilla.meltdowner.skill.Meltdowner.INSTANCE, 0.5f);

        cn.academy.ability.vanilla.meltdowner.passiveskill.RayBarrage.init();

        cn.academy.ability.vanilla.meltdowner.skill.JetEngine.INSTANCE.setPosition(170, 32);
        CATEGORY.addSkill(cn.academy.ability.vanilla.meltdowner.skill.JetEngine.INSTANCE);

        cn.academy.ability.vanilla.meltdowner.skill.JetEngine.INSTANCE
                .setParent(cn.academy.ability.vanilla.meltdowner.skill.Meltdowner.INSTANCE, 1.0f);

        cn.academy.ability.vanilla.meltdowner.skill.ElectronMissile.INSTANCE.setPosition(210, 35);
        CATEGORY.addSkill(cn.academy.ability.vanilla.meltdowner.skill.ElectronMissile.INSTANCE);

        cn.academy.ability.vanilla.meltdowner.skill.ElectronMissile.INSTANCE
                .setParent(cn.academy.ability.vanilla.meltdowner.skill.JetEngine.INSTANCE, 0.3f);

        CategoryManager.INSTANCE.register(CATEGORY);

        VanillaCategories.addLateGenericSkills(CATEGORY);
    }
}
