package cn.lambdalib2.cgui.event;

public class FrameEvent implements GuiEvent {
    public final double mx, my;
    public final boolean hovering;
    public final float deltaTime;

    public FrameEvent(double _mx, double _my, boolean hov, float deltaTime) {
        mx = _mx;
        my = _my;
        hovering = hov;
        this.deltaTime = deltaTime;
    }
}
