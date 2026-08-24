package cn.lambdalib2.cgui.component;

import cn.lambdalib2.cgui.annotation.CGuiEditorComponent;
import cn.lambdalib2.cgui.event.FrameEvent;
import cn.lambdalib2.util.Color;
import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.HudUtils;
import com.mojang.blaze3d.systems.RenderSystem;

@CGuiEditorComponent
public class Tint extends Component {

    public Color idleColor, hoverColor;

    public boolean affectTexture = false;

    public double zLevel = 0.0;

    public Tint() {
        this(Colors.fromFloat(1, 1, 1, 0.6f), Colors.fromFloat(1, 1, 1, 1));
    }

    public Tint(Color idle, Color hover, boolean _affectTexture) {
        this(idle, hover);
        affectTexture = _affectTexture;
    }

    public Tint(Color idle, Color hover) {
        super("Tint");

        idleColor = idle;
        hoverColor = hover;

        listen(FrameEvent.class, (w, event) -> {
            if (affectTexture) {
                DrawTexture dt = w.getComponent(DrawTexture.class);
                if (dt != null) {
                    dt.color = event.hovering ? hoverColor : idleColor;
                }
            } else {
                Colors.bindToGL(event.hovering ? hoverColor : idleColor);

                double prevZ = HudUtils.zLevel;
                HudUtils.zLevel = zLevel;
                HudUtils.colorRect(0, 0, w.transform.width, w.transform.height);
                HudUtils.zLevel = prevZ;

                RenderSystem.setShaderColor(1, 1, 1, 1);
            }
        });
    }

    public Tint setAffectTexture() {
        affectTexture = true;
        return this;
    }
}
