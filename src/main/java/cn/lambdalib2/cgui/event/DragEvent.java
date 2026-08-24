package cn.lambdalib2.cgui.event;

public class DragEvent implements GuiEvent {
    public final float offsetX, offsetY;

    public DragEvent(float _offsetX, float _offsetY) {
        offsetX = _offsetX;
        offsetY = _offsetY;
    }
}
