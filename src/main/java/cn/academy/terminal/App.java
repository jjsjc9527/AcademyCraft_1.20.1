package cn.academy.terminal;

import cn.academy.Resources;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public abstract class App {

    int appid;
    private final String name;
    protected ResourceLocation icon;

    private boolean preInstalled = false;

    public App(String _name) {
        name = _name;
        icon = getTexture("icon");
    }

    protected ResourceLocation getTexture(String texname) {
        return Resources.getTexture("gui/apps/" + name + "/" + texname);
    }

    private String localKey(String key) {
        return "app.academy." + name + "." + key;
    }

    @OnlyIn(Dist.CLIENT)
    protected String local(String key) {
        return I18n.get(localKey(key));
    }

    @OnlyIn(Dist.CLIENT)
    public ResourceLocation getIcon() {
        return icon;
    }

    public App setPreInstalled() {
        preInstalled = true;
        return this;
    }

    public int getID() {
        return appid;
    }

    public String getName() {
        return name;
    }

    @OnlyIn(Dist.CLIENT)
    public String getDisplayName() {
        return local("name");
    }

    public String getDisplayKey() {
        return localKey("name");
    }

    public final boolean isPreInstalled() {
        return preInstalled;
    }

    @OnlyIn(Dist.CLIENT)
    public abstract AppEnvironment createEnvironment();

}
