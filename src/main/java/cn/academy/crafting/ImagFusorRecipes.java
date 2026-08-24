package cn.academy.crafting;

import cn.academy.ACItems;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class ImagFusorRecipes {

    public static final ImagFusorRecipes INSTANCE = new ImagFusorRecipes();

    private final List<IFRecipe> recipeList = new ArrayList<>();

    private ImagFusorRecipes() {}

    public static void register() {
        if (!INSTANCE.recipeList.isEmpty()) return;
        INSTANCE.addRecipe(new ItemStack(ACItems.CRYSTAL_LOW.get()), 3000,
                new ItemStack(ACItems.CRYSTAL_NORMAL.get()));
        INSTANCE.addRecipe(new ItemStack(ACItems.CRYSTAL_NORMAL.get()), 8000,
                new ItemStack(ACItems.CRYSTAL_PURE.get()));
    }

    public void addRecipe(ItemStack consume, int liquid, ItemStack output) {
        addRecipe(new IFRecipe(consume, liquid, output));
    }

    public void addRecipe(IFRecipe recipe) {
        for (IFRecipe r : recipeList) {
            if (r.matches(recipe.consumeType)) {
                throw new RuntimeException("Can't register multiple recipes for same item "
                        + recipe.consumeType.getItem());
            }
        }
        recipeList.add(recipe);
        recipe.id = recipeList.size() - 1;
    }

    public IFRecipe getRecipe(ItemStack input) {
        for (IFRecipe r : recipeList) {
            if (r.matches(input)) return r;
        }
        return null;
    }

    public IFRecipe byId(int id) {
        return id >= 0 && id < recipeList.size() ? recipeList.get(id) : null;
    }

    public List<IFRecipe> getAllRecipe() {
        return recipeList;
    }

    public static class IFRecipe {

        int id;
        public final ItemStack consumeType;

        public final int consumeLiquid;
        public final ItemStack output;

        public IFRecipe(ItemStack consume, int liquid, ItemStack output) {
            this.consumeType = consume;
            this.consumeLiquid = liquid;
            this.output = output;
        }

        public boolean matches(ItemStack input) {
            return consumeType.getItem() == input.getItem();
        }

        public int getID() {
            return id;
        }
    }
}
