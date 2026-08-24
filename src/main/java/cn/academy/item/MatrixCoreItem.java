package cn.academy.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class MatrixCoreItem extends Item {

    private final int level;

    public MatrixCoreItem(int level, Properties props) {
        super(props);
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    public static int levelOf(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof MatrixCoreItem core ? core.getLevel() : 0;
    }
}
