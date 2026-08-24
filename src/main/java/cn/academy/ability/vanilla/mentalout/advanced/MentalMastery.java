package cn.academy.ability.vanilla.mentalout.advanced;

import cn.academy.ability.vanilla.mentalout.WideCastExecutor;
import cn.academy.ability.vanilla.mentalout.passiveskill.MindManip;
import cn.academy.datapart.AbilityData;
import cn.academy.datapart.RemoteData;
import cn.lambdalib2.datapart.EntityData;
import net.minecraft.world.entity.player.Player;

public class MentalMastery extends MentalAdvSkill {

    public static final MentalMastery INSTANCE = new MentalMastery();

    public static final int LOCKED_SLOTS = 1;

    private MentalMastery() {
        super("mental_mastery", MindManip.INSTANCE, true);
    }

    public static boolean isLearned(Player player) {
        return INSTANCE.isLearnedBy(player);
    }

    public static int usableSlots(Player player) {
        if (player == null) {
            return RemoteData.MAX_SLOTS;
        }
        return isLearned(player) ? RemoteData.MAX_SLOTS : LOCKED_SLOTS;
    }

    public static boolean slotUnlocked(Player player, int slot) {
        return slot >= 0 && slot < usableSlots(player);
    }
}
