package cn.academy.api;

import cn.academy.ability.Category;
import cn.academy.ability.CategoryManager;
import net.minecraftforge.eventbus.api.Event;

public class ACRegisterCategoriesEvent extends Event {

    public void register(Category category) {
        java.util.Objects.requireNonNull(category, "category");
        CategoryManager.INSTANCE.register(category);
    }
}
