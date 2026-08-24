package cn.lambdalib2.cgui.event;

public class KeyEvent implements GuiEvent {
    public final char inputChar;
    public final int keyCode;

    public KeyEvent(char _ch, int _key) {
        inputChar = _ch;
        keyCode = _key;
    }
}
