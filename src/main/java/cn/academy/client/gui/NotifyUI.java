package cn.academy.client.gui;

import cn.academy.Resources;
import cn.academy.client.auxgui.ACHud;
import cn.academy.client.gui.developer.DevRender;
import cn.lambdalib2.cgui.Widget;
import cn.lambdalib2.cgui.event.FrameEvent;
import cn.lambdalib2.render.font.IFont;
import cn.lambdalib2.render.font.IFont.FontOption;
import cn.lambdalib2.util.Color;
import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.GameTimer;
import cn.lambdalib2.util.HudUtils;
import cn.lambdalib2.util.MathUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class NotifyUI extends Widget {

    public static NotifyUI instance;

    public static void init() {
        final ResourceLocation texture = Resources.getTexture("gui/edit_preview/notify_logo");

        final INotification dummy = new INotification() {
            @Override
            public ResourceLocation getIcon() {
                return texture;
            }

            @Override
            public String getTitle() {
                return "Some Notification";
            }

            @Override
            public String getContent() {
                return "blablabla";
            }
        };
        instance = new NotifyUI();
        ACHud.instance.addElement(instance, () -> true, "notification",
                new Widget().size(517, 170).scale(0.25f).listen(FrameEvent.class, (w, e) -> {
                    drawBack(1);
                    drawIcon(dummy, end, 1);
                    drawText(dummy, 1);
                }));
    }

    static final double KEEP_TIME = 6;
    static final double BLEND_IN_TIME = 0.5, SCAN_TIME = 0.5, BLEND_OUT_TIME = 0.3;

    static final ResourceLocation texture = Resources.getTexture("gui/notification/back");

    static final Vec3
            start = new Vec3(420, 42, 0),
            end = new Vec3(34, 42, 0);

    INotification lastNotify;
    double lastReceiveTime;

    public NotifyUI() {
        addDrawing();

        transform.scale = 0.25f;
        transform.setPos(0, 15);

    }

    public void addDrawing() {
        listen(FrameEvent.class, (w, e) -> {
            if (lastNotify != null) {
                double dt = GameTimer.getTime() - lastReceiveTime;
                RenderSystem.enableBlend();

                if (dt < BLEND_IN_TIME) {

                    drawBack(Math.min(dt / 300.0, 1));

                    double iconAlpha = Math.max(0, Math.min(1, (dt - 200) / 300.0));
                    drawIcon(lastNotify, start, iconAlpha);

                } else if (dt < SCAN_TIME + BLEND_IN_TIME) {
                    float scanProgress = (float) ((dt - BLEND_IN_TIME) / SCAN_TIME);

                    scanProgress = Mth.sin(scanProgress * MathUtils.PI_F / 2);

                    drawBack(1);
                    drawIcon(lastNotify, start.lerp(end, scanProgress), 1);
                    drawText(lastNotify, scanProgress);

                } else if (dt < KEEP_TIME - BLEND_OUT_TIME) {
                    drawBack(1);
                    drawIcon(lastNotify, end, 1);
                    drawText(lastNotify, 1);

                } else if (dt < KEEP_TIME) {
                    float alpha = 1 - (float) ((dt - (KEEP_TIME - BLEND_OUT_TIME)) / BLEND_OUT_TIME);
                    drawBack(alpha);
                    drawIcon(lastNotify, end, alpha);
                    drawText(lastNotify, alpha);

                } else {
                    lastNotify = null;
                }

                RenderSystem.setShaderColor(1, 1, 1, 1);
            }
        });
    }

    private static void drawText(INotification notif, float alpha) {

        if (alpha < 1E-1f) alpha = 1E-1f;
        Color color = Colors.fromFloat(1, 1, 1, alpha);

        FontOption optTitle = new FontOption(38, color);
        FontOption optContent = new FontOption(54, color);

        IFont font = Resources.font();

        font.draw(notif.getTitle(), 137, 32, optTitle);
        font.draw(notif.getContent(), 137, 81, optContent);
    }

    private static void drawBack(double alpha) {
        RenderSystem.setShaderColor(1, 1, 1, (float) alpha);
        HudUtils.loadTexture(texture);
        HudUtils.rect(517, 170);
    }

    private static void drawIcon(INotification notf, Vec3 p, double alpha) {
        RenderSystem.setShaderColor(1, 1, 1, (float) alpha);
        Matrix4f saved = DevRender.save();
        DevRender.translate(p.x, p.y, p.z);
        HudUtils.loadTexture(notf.getIcon());
        HudUtils.rect(83, 83);
        DevRender.restore(saved);
    }

    public void notify(INotification n) {
        lastNotify = n;
        lastReceiveTime = GameTimer.getTime();
    }
}
