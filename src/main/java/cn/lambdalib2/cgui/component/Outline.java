package cn.lambdalib2.cgui.component;

import cn.lambdalib2.cgui.annotation.CGuiEditorComponent;
import cn.lambdalib2.cgui.event.FrameEvent;
import cn.lambdalib2.util.Color;
import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.HudUtils;
import com.mojang.blaze3d.systems.RenderSystem;

@CGuiEditorComponent
public class Outline extends Component {

    public Color color;
    public float lineWidth = 2;

    public Outline() {
        this(Colors.white());
    }

    public Outline(Color _color) {
        super("Outline");

        color = _color;

        listen(FrameEvent.class, (w, e) -> {
            Colors.bindToGL(color);
            HudUtils.drawRectOutline(0, 0, w.transform.width, w.transform.height, lineWidth);
            RenderSystem.setShaderColor(1, 1, 1, 1);
        });
    }

}
