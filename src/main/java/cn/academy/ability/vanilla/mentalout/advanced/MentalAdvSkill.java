package cn.academy.ability.vanilla.mentalout.advanced;

import net.minecraft.world.entity.player.Player;
import cn.lambdalib2.datapart.EntityData;
import cn.academy.datapart.AbilityData;
import cn.academy.ability.Skill;
import cn.academy.ability.SkillTab;
import cn.academy.ability.develop.LearningHelper;
import cn.academy.ability.vanilla.mentalout.passiveskill.MindManip;
import cn.academy.ability.vanilla.mentalout.passiveskill.WideCast;
import com.google.common.collect.ImmutableList;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class MentalAdvSkill extends Skill {

    public static final float REQ_BASE = 1.0f;

    public static final float REQ_WIDE_CAST = 0.5f;

    public static final List<MentalAdvSkill> ALL = ImmutableList.of(
            CognitionTamper.INSTANCE,
            PainNumb.INSTANCE,
            SelfLoss.INSTANCE,

            BrainPressure.INSTANCE,

            FreeManip.INSTANCE,

            MentalMastery.INSTANCE,

            AbsoluteAbility.INSTANCE);

    private final Skill base;

    private final boolean ownIcon;

    private MentalAdvSkill(Skill base) {
        this(base.getName() + "_adv", base);
    }

    protected MentalAdvSkill(String name, Skill base) {
        this(name, base, false);
    }

    protected MentalAdvSkill(String name, Skill base, boolean ownIcon) {

        super(name, LearningHelper.ADVANCED_TREE_LEVEL);
        this.base = base;
        this.ownIcon = ownIcon;
        this.tab = SkillTab.ADVANCED;

        this.canControl = false;
    }

    public boolean isLearnedBy(Player player) {
        if (player == null || !EntityData.isReady(player)) {
            return false;
        }
        AbilityData data = AbilityData.get(player);
        return data != null && data.isSkillLearned(this);
    }

    public Skill getBase() {
        return base;
    }

    @Override
    protected ResourceLocation initIcon() {
        return ownIcon ? super.initIcon() : base.getHintIcon();
    }

    @Override
    public boolean isEnabled() {
        return super.isEnabled() && base.isEnabled();
    }
}
