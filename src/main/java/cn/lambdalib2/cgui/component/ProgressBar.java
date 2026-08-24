package cn.lambdalib2.cgui.component;

import cn.lambdalib2.cgui.Widget;
import cn.lambdalib2.cgui.annotation.CGuiEditorComponent;
import cn.lambdalib2.cgui.event.FrameEvent;
import cn.lambdalib2.util.Color;
import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.GameTimer;
import cn.lambdalib2.util.HudUtils;
import cn.lambdalib2.util.MathUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.resources.ResourceLocation;

@CGuiEditorComponent
public class ProgressBar extends Component {

    public enum Direction { RIGHT, LEFT, UP, DOWN }

    public boolean illustrating = false;

    public ResourceLocation texture;

    public Direction dir = Direction.RIGHT;

    public double progress;

    public Color color = Colors.white();

    public ProgressBar() {
        super("ProgressBar");
        listen(FrameEvent.class, (wi, e) -> {
            if (illustrating) {
                progress = 0.5 * (1 + Math.sin(GameTimer.getAbsTime()));
            }

            double disp = MathUtils.clampd(0, 1, progress);

            double x, y, u, v, w, h, tw, th;
            double width = wi.transform.width, height = wi.transform.height;
            switch (dir) {
                case RIGHT -> {
                    w = width * disp; h = height; x = 0; y = 0;
                    u = 0; v = 0; tw = disp; th = 1;
                }
                case LEFT -> {
                    w = width * disp; h = height; x = width - w; y = 0;
                    u = 1 - disp; v = 0; tw = disp; th = 1;
                }
                case UP -> {
                    w = width; h = height * disp; x = 0; y = height * (1 - disp);
                    u = 0; v = 1 - disp; tw = 1; th = disp;
                }
                case DOWN -> {
                    w = width; h = height * disp; x = 0; y = 0;
                    u = 0; v = 0; tw = 1; th = disp;
                }
                default -> throw new RuntimeException("unreachable");
            }

            Colors.bindToGL(color);
            if (texture != null && !texture.getPath().equals("<null>")) {
                HudUtils.loadTexture(texture);
                HudUtils.rawRect(x, y, u, v, w, h, tw, th);
            } else {

                HudUtils.colorRect(x, y, w, h);
            }
            RenderSystem.setShaderColor(1, 1, 1, 1);
        });
    }

    public ProgressBar setDirection(Direction dir) {
        this.dir = dir;
        return this;
    }

    public ProgressBar setColor(Color c) {
        this.color = c;
        return this;
    }

    public static ProgressBar get(Widget w) {
        return w.getComponent(ProgressBar.class);
    }
}
