package cn.lambdalib2.cgui.event;

import cn.lambdalib2.cgui.Widget;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LostFocusEvent implements GuiEvent {
    public Widget newFocus;

    public LostFocusEvent(Widget _newFocus) {
        newFocus = _newFocus;
    }
}
