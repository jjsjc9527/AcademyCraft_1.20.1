package cn.academy.ability.develop.action;

import cn.academy.ability.AwakenedCategories;
import cn.academy.ability.Category;
import cn.academy.ability.CategoryManager;
import cn.academy.ability.develop.IDeveloper;
import cn.academy.ability.develop.LearningHelper;
import cn.academy.datapart.AbilityData;
import cn.academy.item.InductionFactorItem;
import cn.lambdalib2.util.RandUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class DevelopActionLevel implements IDevelopAction {

    @Override
    public int getStimulations(Player player) {
        return 5 * (AbilityData.get(player).getLevel() + 1);
    }

    @Override
    public boolean validate(Player player, IDeveloper developer) {
        return LearningHelper.canLevelUp(developer.getDeveloperType(), AbilityData.get(player));
    }

    @Override
    public void onLearned(Player player) {
        AbilityData aData = AbilityData.get(player);
        if (!aData.hasCategory()) {
            Category c = chooseCategory(player);
            if (c != null) {
                aData.setCategory(c);
            }
        } else {
            aData.setLevel(aData.getLevel() + 1);
        }
    }

    private Category chooseCategory(Player player) {
        Optional<ItemStack> inducted = DevelopActionReset.getFactor(player);
        if (inducted.isPresent()) {

            ItemStack factor = inducted.get();
            Category c = InductionFactorItem.getCategory(factor);

            int idx = player.getInventory().items.indexOf(factor);
            if (idx >= 0) {
                factor.shrink(1);
                if (factor.isEmpty()) {
                    player.getInventory().items.set(idx, ItemStack.EMPTY);
                }
            }
            return c;
        }

        List<Category> all = CategoryManager.INSTANCE.getCategories();
        if (all.isEmpty()) return null;

        List<Category> pool = all;
        AwakenedCategories reg = AwakenedCategories.of(player);
        if (reg != null) {
            Set<String> taken = reg.takenExcept(player.getUUID());
            List<Category> free = new ArrayList<>(all.size());
            for (Category c : all) {
                if (!taken.contains(c.getName())) {
                    free.add(c);
                }
            }

            if (!free.isEmpty()) {
                pool = free;
            }
        }
        return pool.get(RandUtils.nextInt(pool.size()));
    }
}
