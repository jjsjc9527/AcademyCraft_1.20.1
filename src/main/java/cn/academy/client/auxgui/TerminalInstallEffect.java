package cn.academy.client.auxgui;

import cn.lambdalib2.auxgui.AuxGui;
import cn.lambdalib2.cgui.CGui;
import cn.lambdalib2.cgui.Widget;
import cn.lambdalib2.cgui.component.DrawTexture;
import cn.lambdalib2.cgui.component.ProgressBar;
import cn.lambdalib2.cgui.component.TextBox;
import cn.lambdalib2.cgui.component.Transform.HeightAlign;
import cn.lambdalib2.cgui.component.Transform.WidthAlign;
import cn.lambdalib2.cgui.event.FrameEvent;
import cn.lambdalib2.cgui.event.IGuiEventHandler;
import cn.lambdalib2.input.KeyManager;
import cn.lambdalib2.render.font.IFont.FontAlign;
import cn.lambdalib2.render.font.IFont.FontOption;
import cn.lambdalib2.util.Color;
import cn.lambdalib2.util.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TerminalInstallEffect extends AuxGui {

    private static final double ANIM_LENGTH = 4;
    private static final double WAIT = 0.7;
    private static final double BLEND_IN = 0.2, BLEND_OUT = 0.2;

    private final CGui gui = new CGui();

    public TerminalInstallEffect() {
        gui.addWidget("main", createMain());

        gui.getWidget("main/progbar").listen(FrameEvent.class, (w, e) -> {
            double prog = this.getTimeActive() / ANIM_LENGTH;
            if (this.getTimeActive() >= ANIM_LENGTH + WAIT) {
                dispose();

                TerminalUI.keyHandler.onKeyUp();
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    mc.player.displayClientMessage(Component.translatable("terminal.academy.key_hint",
                            KeyManager.getKeyName(KeyManager.dynamic.getKeyID(TerminalUI.keyHandler))), false);
                }
            }

            if (prog > 1.0) {
                prog = 1.0;
            }
            ProgressBar.get(w).progress = prog;
        });

        Widget main = gui.getWidget("main");
        initBlender(main);
        for (Widget w : main.getDrawList())
            initBlender(w);
    }

    private Widget createMain() {
        Widget main = new Widget();
        main.transform.setSize(150, 9).setPos(164, 203);
        main.addComponent(new DrawTexture((ResourceLocation) null, new Color(60, 60, 60, 120)));

        Widget outline = new Widget();
        outline.transform.setSize(147, 6).setPos(0, 0);
        outline.transform.setAlign(WidthAlign.CENTER, HeightAlign.CENTER);
        outline.addComponent(new DrawTexture((ResourceLocation) null, new Color(255, 255, 255, 150)));
        main.addWidget("outline", outline);

        Widget cover = new Widget();
        cover.transform.setSize(146, 5).setPos(0, 0);
        cover.transform.setAlign(WidthAlign.CENTER, HeightAlign.CENTER);
        cover.addComponent(new DrawTexture((ResourceLocation) null, new Color(30, 30, 30, 200)));
        main.addWidget("cover", cover);

        Widget tag = new Widget();
        tag.transform.setSize(40, 8).setPos(0, -8);
        tag.addComponent(new DrawTexture((ResourceLocation) null, new Color(60, 60, 60, 120)));
        TextBox tb = new TextBox(new FontOption(10, FontAlign.LEFT, new Color(255, 255, 255, 255)));
        tb.heightAlign = HeightAlign.CENTER;
        tb.localized = true;
        tb.content = "gui.academy.terminal.installing";
        tag.addComponent(tb);
        main.addWidget("tag", tag);

        Widget progbar = new Widget();
        progbar.transform.setSize(145, 4).setPos(0, 0);
        progbar.transform.setAlign(WidthAlign.CENTER, HeightAlign.CENTER);
        ProgressBar bar = new ProgressBar();
        bar.texture = null;
        bar.dir = ProgressBar.Direction.RIGHT;
        bar.progress = 0;
        bar.color = new Color(255, 255, 255, 200);
        progbar.addComponent(bar);
        main.addWidget("progbar", progbar);

        return main;
    }

    @Override
    public void draw(GuiGraphics gg, float width, float height) {
        gui.resize(width, height);
        gui.draw(gg.pose());
    }

    private void initBlender(Widget w) {
        w.listen(FrameEvent.class, new IGuiEventHandler<FrameEvent>() {
            final int texA, textA, barA;

            {
                DrawTexture tex = DrawTexture.get(w);
                TextBox text = TextBox.get(w);
                ProgressBar bar = ProgressBar.get(w);
                texA = tex != null ? tex.color.getAlpha() : 0;
                textA = text != null ? text.option.color.getAlpha() : 0;
                barA = bar != null ? bar.color.getAlpha() : 0;
            }

            @Override
            public void handleEvent(Widget w, FrameEvent event) {
                double alpha;
                double dt = getTimeActive();
                if (dt < BLEND_IN) {
                    alpha = (dt) / BLEND_IN;
                } else if (dt > ANIM_LENGTH) {
                    alpha = Math.max(0, 1 - (dt - ANIM_LENGTH) / BLEND_OUT);
                } else {
                    alpha = 1;
                }

                DrawTexture tex = DrawTexture.get(w);
                TextBox text = TextBox.get(w);
                ProgressBar bar = ProgressBar.get(w);
                if (tex != null) tex.color.setAlpha((int) (texA * alpha));
                if (text != null) text.option.color.setAlpha((int) (Colors.f2i(0.1f) + 0.9 * textA * alpha));
                if (bar != null) bar.color.setAlpha((int) (barA * alpha));
            }
        });
    }

}
