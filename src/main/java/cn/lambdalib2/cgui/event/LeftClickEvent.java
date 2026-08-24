package cn.lambdalib2.cgui.event;

public class LeftClickEvent implements GuiEvent {
    public final float x, y;

    public LeftClickEvent(float _x, float _y) {
        x = _x;
        y = _y;
    }
}
