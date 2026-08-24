package cn.academy.ability.vanilla.mentalout.advanced;

import cn.academy.ability.vanilla.mentalout.skill.ForcedControl;
import cn.academy.datapart.AbilityData;
import cn.lambdalib2.datapart.EntityData;
import net.minecraft.world.entity.player.Player;

public class FreeManip extends MentalAdvSkill {

    public static final FreeManip INSTANCE = new FreeManip();

    private FreeManip() {
        super("free_manip", ForcedControl.INSTANCE, true);
    }

    public static boolean isLearned(Player player) {
        return INSTANCE.isLearnedBy(player);
    }
}
