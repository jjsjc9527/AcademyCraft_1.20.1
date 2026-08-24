package cn.lambdalib2.cgui.event;

import cn.lambdalib2.cgui.Widget;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@FunctionalInterface
@OnlyIn(Dist.CLIENT)
public interface IGuiEventHandler<T extends GuiEvent> {
    void handleEvent(Widget w, T event);
}
