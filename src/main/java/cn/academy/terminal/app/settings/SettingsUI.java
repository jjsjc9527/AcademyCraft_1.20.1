package cn.academy.terminal.app.settings;

import cn.academy.AcademyCraft;
import cn.academy.Resources;
import cn.lambdalib2.cgui.CGuiScreen;
import cn.lambdalib2.cgui.Widget;
import cn.lambdalib2.cgui.component.DragBar;
import cn.lambdalib2.cgui.component.DrawTexture;
import cn.lambdalib2.cgui.component.ElementList;
import cn.lambdalib2.cgui.component.TextBox;
import cn.lambdalib2.cgui.component.Transform.HeightAlign;
import cn.lambdalib2.cgui.component.Transform.WidthAlign;
import cn.lambdalib2.cgui.component.Tint;
import cn.lambdalib2.cgui.event.DragEvent;
import cn.lambdalib2.render.font.IFont.FontAlign;
import cn.lambdalib2.render.font.IFont.FontOption;
import cn.lambdalib2.util.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public class SettingsUI extends CGuiScreen {

    private static final Map<String, List<UIProperty>> properties = new LinkedHashMap<>();

    private static final String[] CATEGORY_ORDER = { "keys", "generic", "misc" };

    static {

        addProperty(PropertyElements.CHECKBOX, "generic", "attackPlayer", true, true);
        addProperty(PropertyElements.CHECKBOX, "generic", "destroyBlocks", true, true);
        addProperty(PropertyElements.CHECKBOX, "generic", "headsOrTails", false, false);
        addProperty(PropertyElements.CHECKBOX, "generic", "useMouseWheel", false, false);

        addProperty(PropertyElements.SLIDER, "generic", "tpWheelSensitivity", 25, false);

        addProperty(PropertyElements.SLIDER, "generic", "soundVolume", 100, false);

        addProperty(PropertyElements.CHECKBOX, "generic", "vecDeviationHud", false, false);

        addProperty(PropertyElements.CHECKBOX, "generic", "dualWingCrushFx", true, false);

        addProperty(PropertyElements.CHECKBOX, "generic", "showCpValue", true, false);
        addProperty(PropertyElements.CHECKBOX, "generic", "showOverloadValue", true, false);
    }

    public static void addProperty(IPropertyElement elem, String cat, String id, Object defValue, boolean singlePlayer) {
        add(cat, new UIProperty.Config(elem, cat, id, defValue, singlePlayer));
    }

    public static Map<String, List<UIProperty>> registeredProperties() {
        Map<String, List<UIProperty>> out = new LinkedHashMap<>();
        for (String cat : orderedCategories()) {
            List<UIProperty> list = properties.get(cat);
            if (list != null && !list.isEmpty()) {
                out.put(cat, java.util.Collections.unmodifiableList(list));
            }
        }
        return out;
    }

    public static void addCallback(String id, String cat, Runnable callback, boolean singlePlayer) {
        add(cat, new UIProperty.Callback(PropertyElements.CALLBACK, id, callback, singlePlayer));
    }

    private static void add(String cat, UIProperty prop) {
        properties.computeIfAbsent(cat, c -> new ArrayList<>()).add(prop);
    }

    private static Set<String> orderedCategories() {
        Set<String> ret = new LinkedHashSet<>();
        for (String c : CATEGORY_ORDER) {
            if (properties.containsKey(c)) ret.add(c);
        }
        ret.addAll(properties.keySet());
        return ret;
    }

    public SettingsUI() {
        initPages();
    }

    @Override
    public void removed() {
        AcademyCraft.config.save();
        super.removed();
    }

    private void initPages() {
        Widget main = newMain();

        Widget area = main.getWidget("area");

        boolean singlePlayer = Minecraft.getInstance().hasSingleplayerServer();

        ElementList list = new ElementList();
        {
            for (String cat : orderedCategories()) {
                Widget head = newCatHead();
                TextBox.get(head.getWidget("text")).setContent(local("cat." + cat));
                list.addWidget(head);

                for (UIProperty prop : properties.get(cat)) {

                    if (!prop.singlePlayer || singlePlayer)
                        list.addWidget(prop.element.getWidget(prop));
                }

                Widget placeholder = new Widget();
                placeholder.transform.setSize(10, 20);
                list.addWidget(placeholder);
            }
        }
        area.addComponent(list);

        Widget bar = main.getWidget("scrollbar");
        bar.listen(DragEvent.class, (w, e) ->
        {
            list.setProgress((int) (list.getMaxProgress() * DragBar.get(w).getProgress()));
        });

        gui.addWidget(main);
    }

    private String local(String id) {
        return I18n.get("settings.academy." + id);
    }

    private static Widget newMain() {
        Widget main = new Widget();
        main.transform.setSize(742, 923).setPos(0, 0).setCenteredAlign();
        main.transform.scale = 0.2f;
        main.transform.doesDraw = true;
        main.addComponent(new DrawTexture(Resources.getTexture("gui/settings")));

        Widget scrollbar = new Widget();
        scrollbar.transform.setSize(9, 96).setPos(673, 119);
        scrollbar.addComponent(new DrawTexture(Resources.getTexture("gui/life_record/rollbar")));
        scrollbar.addComponent(new DragBar(DragBar.Axis.Y, 119, 760));
        scrollbar.addComponent(new Tint(new Color(0, 0, 0, 0), new Color(255, 255, 255, 80), false));
        main.addWidget("scrollbar", scrollbar);

        Widget area = new Widget();
        area.transform.setSize(614, 720).setPos(60, 120);

        area.transform.doesDraw = true;
        area.transform.doesListenKey = false;
        main.addWidget("area", area);

        return main;
    }

    static Widget newCatHead() {
        Widget ret = new Widget();
        ret.transform.setSize(611, 60);
        ret.transform.doesDraw = true;
        ret.transform.doesListenKey = false;

        Widget line = new Widget();
        line.transform.setSize(611, 4).setPos(0, 0).setAlign(WidthAlign.LEFT, HeightAlign.BOTTOM);
        line.transform.doesListenKey = false;

        line.addComponent(new DrawTexture(null, new Color(255, 255, 255, 150)));
        ret.addWidget("line", line);

        Widget text = new Widget();
        text.transform.setSize(611, 60).setPos(8, 0);
        text.transform.doesListenKey = false;
        text.addComponent(textBox(42, FontAlign.LEFT, new Color(255, 255, 255, 170), HeightAlign.BOTTOM));
        ret.addWidget("text", text);

        return ret;
    }

    static Widget newCheckbox() {
        Widget ret = new Widget();
        ret.transform.setSize(611, 60);
        ret.transform.doesDraw = true;

        Widget text = new Widget();
        text.transform.setSize(300, 40).setPos(15, 0).setAlign(WidthAlign.LEFT, HeightAlign.CENTER);
        text.transform.doesListenKey = false;
        text.addComponent(textBox(40, FontAlign.LEFT, new Color(255, 255, 255, 255), HeightAlign.CENTER));
        ret.addWidget("text", text);

        Widget box = new Widget();
        box.transform.setSize(35, 35).setPos(550, 0).setAlign(WidthAlign.LEFT, HeightAlign.CENTER);
        box.addComponent(new DrawTexture(DrawTexture.MISSING));
        ret.addWidget("box", box);

        return ret;
    }

    static Widget newSliderRow() {
        Widget ret = new Widget();
        ret.transform.setSize(611, 60);
        ret.transform.doesDraw = true;

        Widget text = new Widget();
        text.transform.setSize(300, 40).setPos(15, 0).setAlign(WidthAlign.LEFT, HeightAlign.CENTER);
        text.addComponent(textBox(40, FontAlign.LEFT, new Color(255, 255, 255, 255), HeightAlign.CENTER));
        ret.addWidget("text", text);

        Widget track = new Widget();
        track.transform.setSize(170, 8).setPos(330, 0).setAlign(WidthAlign.LEFT, HeightAlign.CENTER);
        track.addComponent(new Tint(new Color(255, 255, 255, 60), new Color(255, 255, 255, 60), false));

        Widget handle = new Widget();
        handle.transform.setSize(12, 28).setPos(0, -10);
        handle.addComponent(new Tint(new Color(255, 255, 255, 170), new Color(255, 255, 255, 235), false));
        handle.addComponent(new DragBar(DragBar.Axis.X, 0, 158));
        track.addWidget("handle", handle);
        ret.addWidget("track", track);

        Widget value = new Widget();
        value.transform.setSize(90, 40).setPos(515, 0).setAlign(WidthAlign.LEFT, HeightAlign.CENTER);
        TextBox valText = textBox(35, FontAlign.LEFT, new Color(255, 255, 255, 255), HeightAlign.CENTER);
        valText.localized = false;
        valText.setContent("0%");
        value.addComponent(valText);
        ret.addWidget("value", value);

        return ret;
    }

    static Widget newKeyRow() {
        Widget ret = new Widget();
        ret.transform.setSize(611, 60);
        ret.transform.doesDraw = true;

        Widget text = new Widget();
        text.transform.setSize(300, 40).setPos(15, 0).setAlign(WidthAlign.LEFT, HeightAlign.CENTER);
        text.addComponent(textBox(40, FontAlign.LEFT, new Color(255, 255, 255, 255), HeightAlign.CENTER));
        ret.addWidget("text", text);

        Widget key = new Widget();
        key.transform.setSize(160, 40).setPos(440, 0).setAlign(WidthAlign.LEFT, HeightAlign.CENTER);
        TextBox keyText = textBox(40, FontAlign.LEFT, new Color(255, 255, 255, 255), HeightAlign.TOP);
        keyText.localized = false;
        keyText.setContent("MOUSE1");
        key.addComponent(keyText);
        key.addComponent(new Tint(new Color(255, 255, 255, 0), new Color(255, 255, 255, 51), false));
        ret.addWidget("key", key);

        return ret;
    }

    static Widget newCallbackRow() {
        Widget ret = new Widget();
        ret.transform.setSize(611, 60);
        ret.transform.doesDraw = true;

        Widget text = new Widget();
        text.transform.setSize(300, 40).setPos(15, 0).setAlign(WidthAlign.LEFT, HeightAlign.CENTER);
        text.addComponent(textBox(40, FontAlign.LEFT, new Color(255, 255, 255, 255), HeightAlign.CENTER));
        ret.addWidget("text", text);

        Widget button = new Widget();
        button.transform.setSize(160, 40).setPos(440, 0).setAlign(WidthAlign.LEFT, HeightAlign.CENTER);
        button.addComponent(new Tint(new Color(51, 51, 51, 255), new Color(85, 85, 85, 255), false));
        TextBox btnText = textBox(40, FontAlign.CENTER, new Color(255, 255, 255, 255), HeightAlign.CENTER);
        btnText.localized = false;
        btnText.setContent("OK");
        button.addComponent(btnText);
        ret.addWidget("button", button);

        return ret;
    }

    private static TextBox textBox(float fontSize, FontAlign align, Color color, HeightAlign heightAlign) {
        TextBox tb = new TextBox(new FontOption(fontSize, align, color));
        tb.font = cn.lambdalib2.render.font.Fonts.get("AC_Normal");
        tb.heightAlign = heightAlign;
        tb.localized = true;
        return tb;
    }

}
