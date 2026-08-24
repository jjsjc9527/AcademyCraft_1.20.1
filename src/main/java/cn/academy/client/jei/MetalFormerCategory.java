package cn.academy.client.jei;

import cn.academy.ACBlocks;
import cn.academy.ACItems;
import cn.academy.crafting.MetalFormerRecipes.RecipeObject;
import com.mojang.blaze3d.systems.RenderSystem;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class MetalFormerCategory implements IRecipeCategory<RecipeObject> {

    public static final RecipeType<RecipeObject> TYPE =
            new RecipeType<>(new ResourceLocation("academy", "metal_former"), RecipeObject.class);

    private static final int W = 94, H = 57;
    private static final ResourceLocation BG = new ResourceLocation("academy", "textures/guis/nei_metalformer.png");

    private static final ResourceLocation[] MODE_ICONS = {
            modeIcon("plate"), modeIcon("incise"), modeIcon("etch"), modeIcon("refine")
    };

    private static ResourceLocation modeIcon(String m) {
        return new ResourceLocation("academy", "textures/gui/icon_former_" + m + "_dark.png");
    }

    private final IDrawable background;
    private final IDrawable icon;

    public MetalFormerCategory(IGuiHelper gui) {
        this.background = gui.drawableBuilder(BG, 0, 0, W, H).setTextureSize(W, H).build();
        this.icon = gui.createDrawableItemStack(new ItemStack(ACItems.METAL_FORMER.get()));
    }

    @Override public RecipeType<RecipeObject> getRecipeType() { return TYPE; }
    @Override public Component getTitle() { return ACBlocks.METAL_FORMER.get().getName(); }
    @SuppressWarnings("removal")
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeObject recipe, IFocusGroup focuses) {

        builder.addSlot(RecipeIngredientRole.INPUT, 6, 24).addItemStacks(recipe.displayInputs());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 72, 24).addItemStack(recipe.getOutput());
    }

    @Override
    public void draw(RecipeObject recipe, IRecipeSlotsView view, GuiGraphics g, double mouseX, double mouseY) {

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.blit(MODE_ICONS[recipe.mode.ordinal()], 39, 24, 16, 16, 0, 0, 48, 48, 48, 48);
    }
}
