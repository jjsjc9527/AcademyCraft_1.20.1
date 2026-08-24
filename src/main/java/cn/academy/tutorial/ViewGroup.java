package cn.academy.tutorial;

import cn.academy.tutorial.ACTutorial.Tag;

public interface ViewGroup {

    Tag getTag();

    default String getDisplayText() {
        return "";
    }

    default net.minecraft.world.item.ItemStack previewStack() {
        return net.minecraft.world.item.ItemStack.EMPTY;
    }

    default net.minecraft.resources.ResourceLocation previewIcon() {
        return null;
    }

    default net.minecraft.world.item.ItemStack recipeTarget() {
        return net.minecraft.world.item.ItemStack.EMPTY;
    }

}
