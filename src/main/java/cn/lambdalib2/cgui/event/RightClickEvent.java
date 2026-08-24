package cn.lambdalib2.cgui.event;

public class RightClickEvent implements GuiEvent {
    public final double x, y;

    public RightClickEvent(double _x, double _y) {
        x = _x;
        y = _y;
    }
}
