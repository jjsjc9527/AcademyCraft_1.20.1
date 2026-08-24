package cn.lambdalib2.cgui.event;

import cn.lambdalib2.cgui.Widget;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AddWidgetEvent implements GuiEvent {
    public final Widget widget;

    public AddWidgetEvent(Widget w) {
        widget = w;
    }
}
