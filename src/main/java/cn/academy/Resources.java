package cn.academy;

import cn.lambdalib2.render.font.Fonts;
import cn.lambdalib2.render.font.IFont;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public final class Resources {

    private Resources() {}

    public static final ResourceLocation TEX_EMPTY = getTexture("null");

    public static ResourceLocation res(String loc) {
        return new ResourceLocation("academy", loc);
    }

    public static ResourceLocation getTexture(String loc) {
        return res("textures/" + loc + ".png");
    }

    @OnlyIn(Dist.CLIENT)
    public static IFont font() {
        return Fonts.get("AC_Normal");
    }
}
