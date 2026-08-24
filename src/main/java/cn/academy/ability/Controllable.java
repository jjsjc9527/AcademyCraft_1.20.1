package cn.academy.ability;

import cn.academy.ability.context.ClientRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public abstract class Controllable {

    private Category category;
    private int id;

    public Controllable() {}

    final void addedControllable(Category _category, int _id) {
        category = _category;
        id = _id;
    }

    public final Category getCategory() {
        return category;
    }

    public final int getControlID() {
        return id;
    }

    @OnlyIn(Dist.CLIENT)
    public void activate(ClientRuntime rt, int keyID) {}

    public abstract ResourceLocation getHintIcon();

    public abstract String getHintText();

    public boolean shouldOverrideKey() {
        return true;
    }
}
