package cn.academy.client.gui;

import cn.academy.AcademyCraft;
import cn.academy.Resources;
import cn.academy.client.auxgui.ACHud;
import cn.academy.terminal.app.settings.SettingsUI;
import cn.lambdalib2.cgui.CGuiScreen;
import cn.lambdalib2.cgui.Widget;
import cn.lambdalib2.cgui.component.DrawTexture;
import cn.lambdalib2.cgui.component.ElementList;
import cn.lambdalib2.cgui.component.Outline;
import cn.lambdalib2.cgui.component.TextBox;
import cn.lambdalib2.cgui.component.TextBox.ConfirmInputEvent;
import cn.lambdalib2.cgui.component.Tint;
import cn.lambdalib2.cgui.component.Transform.HeightAlign;
import cn.lambdalib2.cgui.component.Transform.WidthAlign;
import cn.lambdalib2.cgui.event.LeftClickEvent;
import cn.lambdalib2.render.font.Fonts;
import cn.lambdalib2.render.font.IFont.FontAlign;
import cn.lambdalib2.render.font.IFont.FontOption;
import cn.lambdalib2.util.Color;
import cn.lambdalib2.util.Colors;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class CustomizeUI extends CGuiScreen {

    public static void register() {
        SettingsUI.addCallback("edit_ui", "misc", () ->
                Minecraft.getInstance().setScreen(new CustomizeUI()), false);
    }

    private final Widget main;
    private final Widget body;

    public CustomizeUI() {
        main = newMain();
        body = main.getWidget("body");

        ElementList list = new ElementList();
        for (ACHud.Node n : ACHud.instance.getNodes()) {
            double[] pos = n.getPosition();
            n.getPreview().pos((float) pos[0], (float) pos[1]);
            gui.addWidget(n.getPreview());
            n.getPreview().removeComponent(Outline.class);

            Widget elem = newTemplate();

            elem.transform.doesDraw = true;
            TextBox textBox = elem.getComponent(TextBox.class);
            textBox.localized = true;
            textBox.setContent("gui.academy.uiedit.elm." + n.getName());
            elem.listen(LeftClickEvent.class, (w, evt) -> changeEditFocus(w, n));

            list.addWidget(elem);
        }
        body.addComponent(list);

        gui.addWidget("main", main);
    }

    @Override
    public void removed() {
        AcademyCraft.config.save();
        super.removed();
    }

    private ACHud.Node prevFocus;
    private Widget edit;

    private void changeEditFocus(Widget button, ACHud.Node node) {
        if (node == prevFocus) {
            return;
        }

        if (prevFocus != null) {
            prevFocus.getPreview().removeComponent(Outline.class);
            edit.dispose();
        }

        prevFocus = node;
        node.getPreview().addComponent(new Outline());

        edit = newEditBox();
        double[] prevPos = node.getPosition();
        wrapEdit(edit.getWidget("edit_x"), (value_) -> {
            double[] pos = node.getPosition();
            float value = (float) (double) value_;
            node.setPosition(value, (float) pos[1]);
            node.getPreview().pos(value, ((float) pos[1]));
            node.getPreview().dirty = true;
        }, prevPos[0]);
        wrapEdit(edit.getWidget("edit_y"), (value_) -> {
            double[] pos = node.getPosition();
            float value = (float) (double) value_;
            node.setPosition((float) pos[0], value);
            node.getPreview().pos((float) pos[0], value);
            node.getPreview().dirty = true;
        }, prevPos[1]);

        edit.pos(button.x + button.transform.width * button.scale + 5,
                button.y + button.transform.height * button.scale / 2 - edit.transform.height / 2);

        gui.addWidget(edit);
    }

    private void wrapEdit(Widget w, Consumer<Double> action, double defaultValue) {
        TextBox box = w.getComponent(TextBox.class);
        DrawTexture tex = w.getComponent(DrawTexture.class);
        box.content = String.valueOf(defaultValue);
        w.listen(ConfirmInputEvent.class, (w2, evt) -> {
            try {
                double x = Double.parseDouble(box.content);
                checkCoord(x);

                action.accept(x);
                tex.color = Colors.fromRGBA32(0x333333ff);
            } catch (NumberFormatException e) {
                tex.color = Colors.fromRGBA32(0xbb3333ff);
            }
        });
    }

    private void checkCoord(double val) {
        if (val < -512 || val > 512) {
            throw new NumberFormatException();
        }
    }

    private static Widget newMain() {
        Widget main = new Widget();
        main.transform.setSize(144, 203).setPos(94, 104);
        main.transform.scale = 0.5f;
        main.transform.doesDraw = true;
        main.addComponent(new DrawTexture(Resources.getTexture("gui/window_ui_resize")));

        Widget header = new Widget();
        header.transform.setSize(128, 28).setPos(0, 8).setAlign(WidthAlign.CENTER, HeightAlign.TOP);
        TextBox headText = textBox(18, new Color(255, 255, 255, 187));
        headText.localized = true;
        headText.xOffset = 5;
        headText.setContent("gui.academy.uiedit.elements");
        header.addComponent(headText);
        main.addWidget("header", header);

        Widget body = new Widget();
        body.transform.setSize(128, 160).setPos(0, 36).setAlign(WidthAlign.CENTER, HeightAlign.TOP);

        body.transform.doesDraw = true;
        main.addWidget("body", body);

        return main;
    }

    private static Widget newTemplate() {
        Widget ret = new Widget();
        ret.transform.setSize(128, 24);
        ret.transform.doesDraw = false;

        ret.addComponent(new Tint(new Color(255, 255, 255, 25), new Color(255, 255, 255, 127), false));

        TextBox tb = textBox(18, new Color(255, 255, 255, 255));
        tb.xOffset = 10;
        tb.setContent("CP Indicator");
        ret.addComponent(tb);

        return ret;
    }

    private static Widget newEditBox() {
        Widget ret = new Widget();
        ret.transform.setSize(90, 16);
        ret.transform.doesDraw = true;

        ret.addComponent(new DrawTexture(null, new Color(34, 34, 34, 187)));
        ret.addComponent(new Outline(new Color(255, 255, 255, 255)));

        ret.addWidget("text_x", newLabel("X", 2));
        ret.addWidget("edit_x", newInput(10));
        ret.addWidget("text_y", newLabel("Y", 46));
        ret.addWidget("edit_y", newInput(53));

        return ret;
    }

    private static Widget newLabel(String text, float x) {
        Widget ret = new Widget();
        ret.transform.setSize(10, 10).setPos(x, 3);
        TextBox tb = textBox(10, new Color(255, 255, 255, 255));
        tb.setContent(text);
        ret.addComponent(tb);
        return ret;
    }

    private static Widget newInput(float x) {
        Widget ret = new Widget();
        ret.transform.setSize(34, 10).setPos(x, 3);
        ret.addComponent(new DrawTexture(null, new Color(51, 51, 51, 255)));
        ret.addComponent(textBox(10, new Color(255, 255, 255, 255)).allowEdit());
        return ret;
    }

    private static TextBox textBox(float fontSize, Color color) {
        TextBox tb = new TextBox(new FontOption(fontSize, FontAlign.LEFT, color));
        tb.font = Fonts.get("AC_Normal");
        tb.heightAlign = HeightAlign.CENTER;
        return tb;
    }
}
