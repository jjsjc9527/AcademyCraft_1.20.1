package cn.academy.ability.vanilla.mentalout;

import cn.academy.ability.vanilla.mentalout.advanced.AbsoluteAbility;
import cn.academy.ability.Category;
import cn.academy.ability.CategoryManager;
import cn.academy.ability.Skill;
import cn.academy.ability.vanilla.VanillaCategories;
import cn.academy.ability.vanilla.mentalout.advanced.MentalAdvSkill;
import cn.academy.ability.vanilla.mentalout.passiveskill.MindManip;
import cn.academy.ability.vanilla.mentalout.passiveskill.PainCutoff;
import cn.academy.ability.vanilla.mentalout.passiveskill.WideCast;
import cn.academy.ability.vanilla.mentalout.skill.Daze;
import cn.academy.ability.vanilla.mentalout.skill.Faint;
import cn.academy.ability.vanilla.mentalout.skill.ForcedControl;
import cn.academy.ability.vanilla.mentalout.skill.Impression;

public final class CatMentalOut {

    public static final Category CATEGORY = new Category("mentalout");

    private CatMentalOut() {}

    public static void register() {

        CATEGORY.setColorStyle(255, 205, 70, 100);

        Impression.INSTANCE.setPosition(14, 63);
        CATEGORY.addSkill(Impression.INSTANCE);

        VanillaCategories.addGenericSkills(CATEGORY);

        PainCutoff.INSTANCE.setPosition(52, 10);
        CATEGORY.addSkill(PainCutoff.INSTANCE);

        PainCutoff.INSTANCE.setParent(Impression.INSTANCE, 0.1f);

        Daze.INSTANCE.setPosition(52, 108);
        CATEGORY.addSkill(Daze.INSTANCE);

        Daze.INSTANCE.setParent(Impression.INSTANCE, 0.5f);

        Faint.INSTANCE.setPosition(90, 63);
        CATEGORY.addSkill(Faint.INSTANCE);

        Faint.INSTANCE.setParent(Daze.INSTANCE, 0.3f);
        Faint.INSTANCE.addTreeParent(PainCutoff.INSTANCE, 0.5f);

        ForcedControl.INSTANCE.setPosition(128, 63);
        CATEGORY.addSkill(ForcedControl.INSTANCE);

        ForcedControl.INSTANCE.setParent(Faint.INSTANCE, 0.5f);

        MindManip.INSTANCE.setPosition(166, 63);
        CATEGORY.addSkill(MindManip.INSTANCE);

        MindManip.INSTANCE.setParent(ForcedControl.INSTANCE, 1.0f);

        WideCast.INSTANCE.setPosition(204, 63);
        CATEGORY.addSkill(WideCast.INSTANCE);

        WideCast.INSTANCE.setParent(MindManip.INSTANCE, 1.0f);

        for (MentalAdvSkill adv : MentalAdvSkill.ALL) {
            Skill base = adv.getBase();
            adv.setPosition(base.guiX, base.guiY);
            CATEGORY.addSkill(adv);
            adv.addSkillDep(base, MentalAdvSkill.REQ_BASE);

            if (base != WideCast.INSTANCE) {
                adv.addSkillDep(WideCast.INSTANCE, MentalAdvSkill.REQ_WIDE_CAST);
            }
        }

        AbsoluteAbility.INSTANCE.setPosition(218, 63);

        MentalCharm.init();

        PainCutoff.init();

        DazeState.init();

        FaintState.init();

        ControlState.init();

        DragonControl.register();

        MindManip.init();

        WideCast.init();

        cn.academy.ability.vanilla.mentalout.advanced.CognitionRewrite.init();

        Helpless.init();

        CategoryManager.INSTANCE.register(CATEGORY);

        VanillaCategories.addLateGenericSkills(CATEGORY);
    }
}
