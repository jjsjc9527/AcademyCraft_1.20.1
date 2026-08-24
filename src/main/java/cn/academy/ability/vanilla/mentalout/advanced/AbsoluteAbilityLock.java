package cn.academy.ability.vanilla.mentalout.advanced;

import cn.academy.datapart.AbilityData;
import cn.lambdalib2.datapart.EntityData;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "academy", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AbsoluteAbilityLock {

    private static final int PERIOD = 40;

    private AbsoluteAbilityLock() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        if (player.level().isClientSide || player.tickCount % PERIOD != 0) {
            return;
        }

        if (!EntityData.isReady(player)) {
            return;
        }
        AbilityData data = AbilityData.get(player);

        if (data == null || !data.isSkillLearned(AbsoluteAbility.INSTANCE)) {
            return;
        }
        data.setSkillLearnState(AbsoluteAbility.INSTANCE, false);
    }
}
