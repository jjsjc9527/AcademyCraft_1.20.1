package cn.academy.client.jei;

import cn.academy.ACBlocks;
import cn.academy.ACItems;
import cn.academy.crafting.ImagFusorRecipes.IFRecipe;
import com.mojang.blaze3d.systems.RenderSystem;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class FusorCategory implements IRecipeCategory<IFRecipe> {

    public static final RecipeType<IFRecipe> TYPE =
            new RecipeType<>(new ResourceLocation("academy", "imag_fusor"), IFRecipe.class);

    private static final int W = 115, H = 66;
    private static final ResourceLocation BG = new ResourceLocation("academy", "textures/guis/nei_fusor.png");

    private static final ResourceLocation LIQUID = new ResourceLocation("academy", "textures/gui/progress_fusor.png");
    private static final int TANK_X = 27, TANK_Y = 11, TANK_W = 61, TANK_H = 15;

    private static final long LOOP_MS = 2000L;

    private final IDrawable background;
    private final IDrawable icon;

    public FusorCategory(IGuiHelper gui) {
        this.background = gui.drawableBuilder(BG, 0, 0, W, H).setTextureSize(W, H).build();
        this.icon = gui.createDrawableItemStack(new ItemStack(ACItems.IMAG_FUSOR.get()));
    }

    @Override public RecipeType<IFRecipe> getRecipeType() { return TYPE; }
    @Override public Component getTitle() { return ACBlocks.IMAG_FUSOR.get().getName(); }
    @SuppressWarnings("removal")
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, IFRecipe recipe, IFocusGroup focuses) {

        builder.addSlot(RecipeIngredientRole.INPUT, 6, 37).addItemStack(recipe.consumeType);
        builder.addSlot(RecipeIngredientRole.OUTPUT, 94, 37).addItemStack(recipe.output);
    }

    @Override
    public void draw(IFRecipe recipe, IRecipeSlotsView view, GuiGraphics g, double mouseX, double mouseY) {

        drawLiquid(g);

        Font font = Minecraft.getInstance().font;
        String s = recipe.consumeLiquid + " mB";
        g.drawString(font, s, 58 - font.width(s) / 2, 2, 0xFF3050C0, false);
    }

    private void drawLiquid(GuiGraphics g) {
        double p = (System.currentTimeMillis() % LOOP_MS) / (double) LOOP_MS;
        int w = (int) Math.round(TANK_W * p);
        if (w <= 0) return;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.blit(LIQUID, TANK_X, TANK_Y, w, TANK_H, 0f, 0f, (int) Math.round(126 * p), 30, 126, 30);
        RenderSystem.disableBlend();
    }
}
