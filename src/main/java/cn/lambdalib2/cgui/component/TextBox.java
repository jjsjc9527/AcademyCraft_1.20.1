package cn.lambdalib2.cgui.component;

import cn.lambdalib2.cgui.Widget;
import cn.lambdalib2.cgui.annotation.CGuiEditorComponent;
import cn.lambdalib2.cgui.component.Transform.HeightAlign;
import cn.lambdalib2.cgui.event.FrameEvent;
import cn.lambdalib2.cgui.event.GuiEvent;
import cn.lambdalib2.cgui.event.KeyEvent;
import cn.lambdalib2.cgui.event.LeftClickEvent;
import cn.lambdalib2.render.font.Fonts;
import cn.lambdalib2.render.font.IFont;
import cn.lambdalib2.render.font.IFont.FontOption;
import cn.lambdalib2.s11n.SerializeIncluded;
import cn.lambdalib2.util.ClientUtils;
import cn.lambdalib2.util.GameTimer;
import cn.lambdalib2.util.HudUtils;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.glfw.GLFW;

@CGuiEditorComponent
public class TextBox extends Component {

    public static class ChangeContentEvent implements GuiEvent {}

    public static class ConfirmInputEvent implements GuiEvent {}

    public String content = "";

    @SerializeIncluded
    public IFont font = Fonts.getDefault();

    public FontOption option;

    public HeightAlign heightAlign = HeightAlign.CENTER;

    public boolean localized = false;

    public boolean allowEdit = false;

    public boolean emit = true;

    public boolean doesEcho = false;
    public char echoChar = '*';

    public float zLevel = 0;

    public float xOffset, yOffset;

    private int caretPos = 0;

    private int displayOffset = 0;

    public TextBox() {
        this(new FontOption());
    }

    public TextBox(FontOption _option) {
        super("TextBox");
        this.option = _option;

        listen(FrameEvent.class, (w, e) -> {
            validate();

            final float originX = w.transform.width * option.align.lenOffset + xOffset;
            final float originY = Math.max(0, w.transform.height - option.fontSize) * heightAlign.factor + yOffset;

            final float widthLimit = w.transform.width - xOffset;
            final String processed = processedContent().substring(displayOffset);
            final int localCaret = caretPos - displayOffset;

            int i = processed.length();
            if (emit) {
                float acc = 0.0f;
                for (i = 0; i < processed.length() && acc < widthLimit; ++i) {
                    acc += font.getCharWidth(processed.codePointAt(i), option);
                }
            }
            final String display = processed.substring(0, i);

            double prevZ = HudUtils.zLevel;
            HudUtils.zLevel = zLevel;

            font.draw(display, originX, originY, option);

            if (w.isFocused() && allowEdit && GameTimer.getAbsTime() % 2 < 1) {
                font.draw("|", originX + sumLength(display, 0, localCaret), originY - 1, option);
            }

            HudUtils.zLevel = prevZ;
        });

        listen(KeyEvent.class, (__, evt) -> {
            if (!allowEdit) {
                return;
            }

            final char input = evt.inputChar;
            final int keyCode = evt.keyCode;

            if (keyCode == GLFW.GLFW_KEY_RIGHT) {
                caretPos = Math.min(content.length(), caretPos + 1);
                checkCaretRegion();
            } else if (keyCode == GLFW.GLFW_KEY_LEFT) {
                caretPos = Math.max(0, caretPos - 1);
                if (caretPos < displayOffset) {
                    displayOffset = caretPos;
                }
            } else if (keyCode == GLFW.GLFW_KEY_V && Screen.hasControlDown()) {
                setContent(content.substring(0, caretPos) + ClientUtils.getClipboardContent() + content.substring(caretPos));
                validate();

                widget.post(new ChangeContentEvent());
            } else if (keyCode == GLFW.GLFW_KEY_C && Screen.hasControlDown()) {
                ClientUtils.setClipboardContent(content);
            } else if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (caretPos != 0) {
                    content = content.substring(0, caretPos - 1) + content.substring(caretPos);
                    --caretPos;
                    if (displayOffset != 0) {
                        --displayOffset;
                    }
                    widget.post(new ChangeContentEvent());

                    checkCaretRegion();
                    validate();
                }
            } else if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                widget.post(new ConfirmInputEvent());
            } else if (keyCode == GLFW.GLFW_KEY_DELETE) {

                content = "";
                widget.post(new ChangeContentEvent());

                validate();
            } else if (SharedConstants.isAllowedChatCharacter(input)) {
                content = content.substring(0, caretPos) + input + content.substring(caretPos);
                caretPos = Math.min(content.length(), caretPos + 1);
                widget.post(new ChangeContentEvent());

                checkCaretRegion();
            }
        });

        listen(LeftClickEvent.class, (w, evt) -> {
            if (!allowEdit) {
                return;
            }

            final float originX = w.transform.width * option.align.lenOffset + xOffset;
            final String display = processedContent().substring(displayOffset);
            final float rel_x = originX - font.getTextWidth(display, option) * option.align.lenOffset + evt.x;

            float acc = 0.0f;
            int ind = 0;
            for (; acc < rel_x && ind < display.length(); ++ind) {
                acc += font.getCharWidth(display.codePointAt(ind), option);
            }

            if (ind > 0 && rel_x < acc - font.getCharWidth(display.codePointAt(ind - 1), option) * 0.5) {
                ind--;
            }

            caretPos = displayOffset + ind;
            checkCaretRegion();
        });
    }

    public TextBox allowEdit() {
        allowEdit = true;
        return this;
    }

    public TextBox setContent(String s) {
        content = s;
        return this;
    }

    public String getContent() {
        return content;
    }

    public TextBox setFont(IFont f) {
        this.font = f;
        return this;
    }

    public TextBox setHeightAlign(HeightAlign align) {
        heightAlign = align;
        return this;
    }

    private void validate() {
        if (!allowEdit) {
            displayOffset = caretPos = 0;
            return;
        }

        if (displayOffset >= content.length() || caretPos > content.length()) {
            displayOffset = caretPos = 0;
        }
    }

    private boolean shouldLocalize() {
        return !allowEdit && localized;
    }

    private void checkCaretRegion() {
        final float widthLimit = widthLimit();
        final String local = processedContent().substring(displayOffset);
        final int localCaret = caretPos - displayOffset;
        final float distance = sumLength(local, 0, localCaret);
        if (distance > widthLimit) {
            float acc = 0.0f;
            int mini = 0;
            for (; mini < localCaret && distance - acc > widthLimit; ++mini) {
                acc += font.getCharWidth(local.codePointAt(mini), option);
            }
            displayOffset += mini;
        }

        if (displayOffset >= caretPos) {
            displayOffset = Math.max(0, caretPos - 1);
        }
    }

    private float widthLimit() {
        return widget.transform.width - xOffset;
    }

    private String processedContent() {
        String ret = content;
        if (shouldLocalize()) {
            ret = I18n.get(ret);
        }
        if (doesEcho) {
            ret = StringUtils.repeat(echoChar, ret.length());
        }

        return ret;
    }

    private float sumLength(String str, int begin, int end) {
        return font.getTextWidth(str.substring(begin, end), option);
    }

    public static TextBox get(Widget w) {
        return w.getComponent(TextBox.class);
    }
}
