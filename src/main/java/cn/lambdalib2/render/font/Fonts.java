package cn.lambdalib2.render.font;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class Fonts {

    public static final IFont def = MCFont.instance;

    private static final Map<String, IFont> registry = new HashMap<>();

    private Fonts() {}

    public static void register(String name, IFont font) {
        registry.put(name, font);
    }

    public static IFont get(String name) {
        return registry.getOrDefault(name, def);
    }

    public static IFont getDefault() {
        return def;
    }
}
