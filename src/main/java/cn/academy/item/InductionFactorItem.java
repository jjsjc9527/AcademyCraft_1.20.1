package cn.academy.item;

import cn.academy.ability.Category;
import cn.academy.ability.CategoryManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class InductionFactorItem extends Item {

    private static final String NBT_CATEGORY = "category";

    public InductionFactorItem(Properties p) {
        super(p.stacksTo(1));
    }

    @Nullable
    public static Category getCategory(ItemStack stack) {
        if (!(stack.getItem() instanceof InductionFactorItem)) return null;
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(NBT_CATEGORY)) return null;
        return CategoryManager.INSTANCE.getCategory(tag.getString(NBT_CATEGORY));
    }

    public static void setCategory(ItemStack stack, Category cat) {
        stack.getOrCreateTag().putString(NBT_CATEGORY, cat.getName());
    }

    public ItemStack create(Category cat) {
        ItemStack stack = new ItemStack(this);
        setCategory(stack, cat);
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Category c = getCategory(stack);
        if (c != null) {
            tooltip.add(Component.literal(c.getDisplayName()));
        }
    }

    private static final java.util.Map<String, Integer> MODEL_INDEX = java.util.Map.of(
            "electromaster", 0, "meltdowner", 1, "teleporter", 2, "vecmanip", 3, "mentalout", 4);

    public static void registerProperties(Item item) {
        net.minecraft.client.renderer.item.ItemProperties.register(item,
                new net.minecraft.resources.ResourceLocation("category"),
                (stack, lvl, entity, seed) -> {
                    Category c = getCategory(stack);
                    if (c == null) return 0;
                    Integer idx = MODEL_INDEX.get(c.getName());
                    return idx != null ? idx : c.getCategoryID();
                });
    }
}
