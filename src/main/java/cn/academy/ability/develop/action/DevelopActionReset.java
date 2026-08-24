package cn.academy.ability.develop.action;

import cn.academy.ACItems;
import cn.academy.ability.Category;
import cn.academy.ability.develop.DeveloperType;
import cn.academy.ability.develop.IDeveloper;
import cn.academy.datapart.AbilityData;
import cn.academy.event.ability.TransformCategoryEvent;
import cn.academy.item.InductionFactorItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;

import java.util.Optional;

public class DevelopActionReset implements IDevelopAction {

    public static boolean canReset(Player player, IDeveloper developer) {
        AbilityData data = AbilityData.get(player);
        ItemStack equip = player.getMainHandItem();
        Optional<ItemStack> factor = getFactor(player);

        return data.getLevel() >= 3
                && developer.getDeveloperType() == DeveloperType.ADVANCED
                && !equip.isEmpty() && equip.getItem() == ACItems.MAGNETIC_COIL.get()
                && factor.isPresent()
                && InductionFactorItem.getCategory(factor.get()) != data.getCategory();
    }

    static Optional<ItemStack> getFactor(Player player) {
        Category playerCategory = AbilityData.get(player).getCategoryNullable();
        for (ItemStack stack : player.getInventory().items) {
            if (stack.isEmpty() || !(stack.getItem() instanceof InductionFactorItem)) continue;
            Category c = InductionFactorItem.getCategory(stack);
            if (c != null && c != playerCategory) {
                return Optional.of(stack);
            }
        }
        return Optional.empty();
    }

    @Override
    public int getStimulations(Player player) {
        return AbilityData.get(player).getLevel() * 10;
    }

    @Override
    public boolean validate(Player player, IDeveloper developer) {
        return canReset(player, developer);
    }

    @Override
    public void onLearned(Player player) {
        AbilityData data = AbilityData.get(player);
        ItemStack factor = getFactor(player).orElse(ItemStack.EMPTY);
        if (factor.isEmpty()) return;

        Category newCat = InductionFactorItem.getCategory(factor);
        if (newCat == null) return;

        int prevLevel = data.getLevel() - 1;

        if (!MinecraftForge.EVENT_BUS.post(new TransformCategoryEvent(player, newCat, prevLevel))) {
            data.setCategory(newCat);
            data.setLevel(prevLevel);

            ItemStack coil = player.getMainHandItem();
            coil.shrink(1);
            if (coil.isEmpty()) {
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            }

            int idx = player.getInventory().items.indexOf(factor);
            if (idx >= 0) {
                factor.shrink(1);
                if (factor.isEmpty()) {
                    player.getInventory().items.set(idx, ItemStack.EMPTY);
                }
            }
        }
    }
}
