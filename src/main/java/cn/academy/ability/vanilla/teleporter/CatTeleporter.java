package cn.academy.ability.vanilla.teleporter;

import cn.academy.ability.Category;
import cn.academy.ability.CategoryManager;
import cn.academy.ability.vanilla.VanillaCategories;
import cn.academy.ability.vanilla.teleporter.skill.ThreateningTeleport;

public final class CatTeleporter {

    public static final Category CATEGORY = new Category("teleporter");

    private CatTeleporter() {}

    public static void register() {

        CATEGORY.setColorStyle(164, 164, 164, 145);

        ThreateningTeleport.INSTANCE.setPosition(14, 42);
        cn.academy.ability.vanilla.teleporter.skill.PenetrateTeleport.INSTANCE.setPosition(60, 46);

        CATEGORY.addSkill(ThreateningTeleport.INSTANCE);

        VanillaCategories.addGenericSkills(CATEGORY);

        CATEGORY.addSkill(cn.academy.ability.vanilla.teleporter.skill.PenetrateTeleport.INSTANCE);
        cn.academy.ability.vanilla.teleporter.skill.PenetrateTeleport.INSTANCE
                .setParent(ThreateningTeleport.INSTANCE, 0.5f);

        cn.academy.ability.vanilla.teleporter.skill.LocationTeleport.INSTANCE.setPosition(118, 50);
        CATEGORY.addSkill(cn.academy.ability.vanilla.teleporter.skill.LocationTeleport.INSTANCE);
        cn.academy.ability.vanilla.teleporter.skill.LocationTeleport.INSTANCE
                .setParent(cn.academy.ability.vanilla.teleporter.skill.PenetrateTeleport.INSTANCE, 0.8f);

        cn.academy.ability.vanilla.teleporter.skill.MarkTeleport.INSTANCE.setPosition(70, 16);
        CATEGORY.addSkill(cn.academy.ability.vanilla.teleporter.skill.MarkTeleport.INSTANCE);
        cn.academy.ability.vanilla.teleporter.skill.MarkTeleport.INSTANCE
                .setParent(ThreateningTeleport.INSTANCE, 0.4f);

        cn.academy.ability.vanilla.teleporter.skill.LocationTeleport.INSTANCE
                .addSkillDep(cn.academy.ability.vanilla.teleporter.skill.MarkTeleport.INSTANCE, 0.8f);

        cn.academy.ability.vanilla.teleporter.skill.ShiftTeleport.INSTANCE.setPosition(175, 47);
        CATEGORY.addSkill(cn.academy.ability.vanilla.teleporter.skill.ShiftTeleport.INSTANCE);
        cn.academy.ability.vanilla.teleporter.skill.ShiftTeleport.INSTANCE
                .setParent(cn.academy.ability.vanilla.teleporter.skill.LocationTeleport.INSTANCE, 0.5f);

        cn.academy.ability.vanilla.teleporter.skill.Flashing.INSTANCE.setPosition(220, 20);
        CATEGORY.addSkill(cn.academy.ability.vanilla.teleporter.skill.Flashing.INSTANCE);
        cn.academy.ability.vanilla.teleporter.skill.Flashing.INSTANCE
                .setParent(cn.academy.ability.vanilla.teleporter.skill.ShiftTeleport.INSTANCE, 0.8f);

        cn.academy.ability.vanilla.teleporter.passiveskill.DimFoldingTheorem.INSTANCE.setPosition(50, 75);
        CATEGORY.addSkill(cn.academy.ability.vanilla.teleporter.passiveskill.DimFoldingTheorem.INSTANCE);
        cn.academy.ability.vanilla.teleporter.passiveskill.DimFoldingTheorem.INSTANCE
                .setParent(ThreateningTeleport.INSTANCE, 0.2f);

        cn.academy.ability.vanilla.teleporter.passiveskill.SpaceFluctuation.INSTANCE.setPosition(160, 80);
        CATEGORY.addSkill(cn.academy.ability.vanilla.teleporter.passiveskill.SpaceFluctuation.INSTANCE);
        cn.academy.ability.vanilla.teleporter.passiveskill.SpaceFluctuation.INSTANCE
                .setParent(cn.academy.ability.vanilla.teleporter.skill.ShiftTeleport.INSTANCE, 0.0f);

        cn.academy.ability.vanilla.teleporter.skill.LocationTeleport.Net.init();

        CategoryManager.INSTANCE.register(CATEGORY);

        VanillaCategories.addLateGenericSkills(CATEGORY);
    }
}
