package cn.lambdalib2.auxgui;

import cn.lambdalib2.util.GameTimer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AuxGui {

    public static void register(AuxGui gui) {
        AuxGuiHandler.register(gui);
    }

    boolean lastFrameActive = false;
    double lastActivateTime;

    public boolean requireTicking = false;

    public boolean consistent = true;

    public boolean foreground = false;

    public boolean disposed;

    public AuxGui() {}

    public void dispose() {
        disposed = true;
    }

    public double getTimeActive() {
        return GameTimer.getTime() - lastActivateTime;
    }

    public void onDisposed() {}

    public void onEnable() {}

    public void onTick() {}

    public abstract void draw(GuiGraphics gg, float width, float height);

}
