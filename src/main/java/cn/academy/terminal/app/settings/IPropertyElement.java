package cn.academy.terminal.app.settings;

import cn.academy.AcademyCraft;
import cn.academy.config.Configuration;
import cn.lambdalib2.cgui.Widget;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class IPropertyElement<T extends UIProperty> {

    public abstract Widget getWidget(T prop);

    public Configuration getConfig() {
        return AcademyCraft.config;
    }

}
