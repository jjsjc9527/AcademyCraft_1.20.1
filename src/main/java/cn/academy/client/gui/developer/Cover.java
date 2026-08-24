package cn.academy.client.gui.developer;

import cn.lambdalib2.cgui.component.Component;
import cn.lambdalib2.cgui.event.FrameEvent;
import cn.lambdalib2.util.GameTimer;
import cn.lambdalib2.util.HudUtils;
import cn.lambdalib2.util.MathUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11;

public class Cover extends Component {

    private double lastTransit = GameTimer.getTime();
    private boolean ended = false;

    public Cover() {
        super("cover");
        listen(FrameEvent.class, (w, e) -> {
            double time = GameTimer.getTime();
            double dt = time - lastTransit;

            w.transform.width = w.getGui().getWidth();
            w.transform.height = w.getGui().getHeight();

            double src = MathUtils.clampd(0, 1, dt / 0.2);
            double alpha = ended ? 1 - src : src;

            GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);

            RenderSystem.setShaderColor(0, 0, 0, (float) (alpha * 0.7));
            HudUtils.colorRect(0, 0, w.transform.width, w.transform.height);
            RenderSystem.setShaderColor(1, 1, 1, 1);

            if (ended && alpha == 0) {
                w.post(new CloseEvent());
                w.dispose();
            }

            w.dirty = true;
        });
    }

    public void end() {
        ended = true;
        lastTransit = GameTimer.getTime();
    }

    public boolean isEnded() {
        return ended;
    }
}
