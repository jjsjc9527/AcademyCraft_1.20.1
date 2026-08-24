package cn.academy.client.auxgui;

import cn.academy.AcademyCraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import cn.academy.config.Property;

@OnlyIn(Dist.CLIENT)
public final class CPBarSettings {

    public static final String CATEGORY = "generic";
    public static final String KEY_CP = "showCpValue";
    public static final String KEY_OVERLOAD = "showOverloadValue";

    private static Property cpProp;
    private static Property olProp;

    private CPBarSettings() {}

    public static void init() {
        cpProp = AcademyCraft.config.get(CATEGORY, KEY_CP, true,
                "Show the numeric CP value on the CP bar.");
        olProp = AcademyCraft.config.get(CATEGORY, KEY_OVERLOAD, true,
                "Show the numeric overload value on the CP bar.");
    }

    public static boolean showCp() {
        return cpProp == null || cpProp.getBoolean();
    }

    public static boolean showOverload() {
        return olProp == null || olProp.getBoolean();
    }
}
