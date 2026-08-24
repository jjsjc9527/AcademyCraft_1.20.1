package cn.academy.client.jei;

import cn.academy.ACItems;
import cn.academy.crafting.ImagFusorRecipes;
import cn.academy.crafting.MetalFormerRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

@JeiPlugin
public class ACJeiPlugin implements IModPlugin {

    private static final ResourceLocation UID = new ResourceLocation("academy", "jei");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new FusorCategory(gui),
                new MetalFormerCategory(gui));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(FusorCategory.TYPE, ImagFusorRecipes.INSTANCE.getAllRecipe());

        registration.addRecipes(MetalFormerCategory.TYPE,
                MetalFormerRecipes.INSTANCE.getAllRecipes().stream()
                        .filter(r -> !r.displayInputs().isEmpty() && !r.getOutput().isEmpty())
                        .toList());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {

        registration.addRecipeCatalyst(new ItemStack(ACItems.IMAG_FUSOR.get()), FusorCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(ACItems.METAL_FORMER.get()), MetalFormerCategory.TYPE);
    }
}
