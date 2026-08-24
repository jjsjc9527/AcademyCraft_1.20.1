package cn.academy.event.ability;

import cn.academy.ability.Category;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.Cancelable;

@Cancelable
public class TransformCategoryEvent extends AbilityEvent {

    public Category category;

    public int level;

    public TransformCategoryEvent(Player player, Category cat, int level) {
        super(player);
        this.category = cat;
        this.level = level;
    }
}
