package cn.academy.client.gui.developer;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class DeveloperInfoClient {

    @Nullable
    private static String nodeName;

    private DeveloperInfoClient() {}

    public static void acceptNodeName(@Nullable String name) {
        nodeName = name;
    }

    @Nullable
    public static String getNodeName() {
        return nodeName;
    }

    public static void clear() {
        nodeName = null;
    }
}
