package cn.academy.advancements;

import cn.academy.ACItems;
import cn.academy.datapart.AbilityData;
import cn.academy.event.ability.LevelChangeEvent;
import cn.academy.event.ability.OverloadEvent;
import cn.academy.event.ability.SkillExpAddedEvent;
import cn.academy.event.ability.SkillLearnEvent;
import cn.academy.event.ability.TransformCategoryEvent;
import cn.academy.item.InductionFactorItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "academy", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DispatcherAch {

    private DispatcherAch() {}

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        Item item = event.getCrafting().getItem();
        if (item == ACItems.PHASE_GEN.get()) {
            ACAdvancements.trigger(event.getEntity(), ACAdvancements.PHASE_GENERATOR);
        } else if (item == ACItems.NODE_BASIC.get()) {
            ACAdvancements.trigger(event.getEntity(), ACAdvancements.AC_NODE);
        } else if (item == ACItems.WIRELESS_MATRIX.get()) {
            ACAdvancements.trigger(event.getEntity(), ACAdvancements.AC_MATRIX);
        } else if (item == ACItems.DEVELOPER_PORTABLE.get()) {
            ACAdvancements.trigger(event.getEntity(), ACAdvancements.AC_DEVELOPER);
        }
    }

    @SubscribeEvent
    public static void onItemPickup(PlayerEvent.ItemPickupEvent event) {
        if (event.getStack().getItem() instanceof InductionFactorItem) {
            ACAdvancements.trigger(event.getEntity(), ACAdvancements.GETTING_FACTOR);
        }
    }

    @SubscribeEvent
    public static void onLevelChange(LevelChangeEvent event) {
        if (!cn.lambdalib2.datapart.EntityData.isReady(event.player)) {
            return;
        }
        AbilityData data = AbilityData.get(event.player);
        if (!data.hasCategory()) return;
        switch (data.getLevel()) {
            case 1 -> ACAdvancements.trigger(event.player, ACAdvancements.DEV_CATEGORY);
            case 3 -> ACAdvancements.trigger(event.player, ACAdvancements.AC_LEVEL_3);
            case 5 -> ACAdvancements.trigger(event.player, ACAdvancements.AC_LEVEL_5);
            default -> { }
        }
    }

    @SubscribeEvent
    public static void onSkillLearn(SkillLearnEvent event) {
        ACAdvancements.trigger(event.player, ACAdvancements.AC_LEARNING_SKILL);
    }

    @SubscribeEvent
    public static void onSkillExpAdded(SkillExpAddedEvent event) {

        if (!cn.lambdalib2.datapart.EntityData.isReady(event.player)) {
            return;
        }
        if (AbilityData.get(event.player).getSkillExp(event.skill) >= 1.0f) {
            ACAdvancements.trigger(event.player, ACAdvancements.AC_EXP_FULL);
        }
    }

    @SubscribeEvent
    public static void onOverload(OverloadEvent event) {
        ACAdvancements.trigger(event.player, ACAdvancements.AC_OVERLOAD);
    }

    @SubscribeEvent
    public static void onTransformCategory(TransformCategoryEvent event) {
        ACAdvancements.trigger(event.player, ACAdvancements.CONVERT_CATEGORY);
    }
}
