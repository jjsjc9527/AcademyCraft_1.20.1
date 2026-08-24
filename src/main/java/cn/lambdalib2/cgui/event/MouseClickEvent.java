package cn.lambdalib2.cgui.event;

public class MouseClickEvent implements GuiEvent {
    public final double mx, my;
    public final int button;

    public MouseClickEvent(double _mx, double _my, int bid) {
        mx = _mx;
        my = _my;
        button = bid;
    }
}
