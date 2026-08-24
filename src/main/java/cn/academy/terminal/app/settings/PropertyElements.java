package cn.academy.terminal.app.settings;

import cn.academy.Resources;
import cn.academy.config.Configuration;
import cn.academy.config.Property;
import cn.academy.event.ConfigModifyEvent;
import cn.lambdalib2.cgui.Widget;
import cn.lambdalib2.cgui.component.Component;
import cn.lambdalib2.cgui.component.DrawTexture;
import cn.lambdalib2.cgui.component.TextBox;
import cn.lambdalib2.cgui.event.GainFocusEvent;
import cn.lambdalib2.cgui.event.IGuiEventHandler;
import cn.lambdalib2.cgui.event.KeyEvent;
import cn.lambdalib2.cgui.event.LeftClickEvent;
import cn.lambdalib2.cgui.event.MouseClickEvent;
import cn.lambdalib2.input.KeyManager;
import cn.lambdalib2.util.Color;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public class PropertyElements {

    public static IPropertyElement CHECKBOX = new IPropertyElement<UIProperty.Config>() {

        final ResourceLocation
                CHECK_TRUE = Resources.getTexture("gui/check_true"),
                CHECK_FALSE = Resources.getTexture("gui/check_false");

        @Override
        public Widget getWidget(UIProperty.Config prop) {
            Configuration cfg = getConfig();
            Property p = cfg.get(prop.category, prop.id, (boolean) prop.defValue);

            Widget ret = SettingsUI.newCheckbox();
            TextBox.get(ret.getWidget("text")).setContent(prop.getDisplayID());

            Widget check = ret.getWidget("box");
            DrawTexture.get(check).setTex(p.getBoolean() ? CHECK_TRUE : CHECK_FALSE);

            check.listen(LeftClickEvent.class, (w, e) -> {
                boolean b = !p.getBoolean();
                p.set(b);
                DrawTexture.get(check).setTex(b ? CHECK_TRUE : CHECK_FALSE);
                MinecraftForge.EVENT_BUS.post(new ConfigModifyEvent(p));
            });

            return ret;
        }

    };

    public static IPropertyElement SLIDER = new IPropertyElement<UIProperty.Config>() {

        @Override
        public Widget getWidget(UIProperty.Config prop) {
            Configuration cfg = getConfig();
            Property p = cfg.get(prop.category, prop.id, (int) prop.defValue);

            Widget ret = SettingsUI.newSliderRow();
            TextBox.get(ret.getWidget("text")).setContent(prop.getDisplayID());

            Widget handle = ret.getWidget("track").getWidget("handle");
            TextBox valText = TextBox.get(ret.getWidget("value"));
            valText.localized = false;

            int init = Math.max(0, Math.min(100, p.getInt()));
            cn.lambdalib2.cgui.component.DragBar.get(handle).setProgress(init / 100f);
            valText.setContent(init + "%");

            handle.listen(cn.lambdalib2.cgui.component.DragBar.DraggedEvent.class, (w, e) -> {
                int v = Math.round(cn.lambdalib2.cgui.component.DragBar.get(w).getProgress() * 100);
                p.set(v);
                valText.setContent(v + "%");
                MinecraftForge.EVENT_BUS.post(new ConfigModifyEvent(p));
            });

            return ret;
        }

    };

    public static IPropertyElement KEY = new IPropertyElement<UIProperty.Config>() {

        @Override
        public Widget getWidget(UIProperty.Config prop) {
            Configuration cfg = getConfig();
            Property p = cfg.get(prop.category, prop.id, (int) prop.defValue);

            Widget ret = SettingsUI.newKeyRow();
            TextBox.get(ret.getWidget("text")).setContent(prop.getDisplayID());

            Widget key = ret.getWidget("key");
            key.addComponent(new EditKey(p));

            return ret;
        }

    };

    public static IPropertyElement CALLBACK = new IPropertyElement<UIProperty.Callback>() {
        @Override
        public Widget getWidget(UIProperty.Callback prop) {
            Widget ret = SettingsUI.newCallbackRow();
            TextBox.get(ret.getWidget("text")).setContent(prop.getDisplayID());
            ret.getWidget("button").listen(LeftClickEvent.class, (w, e) -> prop.action.run());
            return ret;
        }
    };

    private static class EditKey extends Component {

        static final Color
                CRL_NORMAL = new Color(200, 200, 200, 200),
                CRL_EDIT = new Color(251, 133, 37, 200);

        IGuiEventHandler<MouseClickEvent> gMouseHandler;

        final Property prop;

        public boolean editing;

        TextBox textBox;

        public EditKey(Property _prop) {
            super("EditKey");

            prop = _prop;

            listen(KeyEvent.class, (w, event) ->
            {
                if (editing) {
                    endEditing(event.keyCode);
                }
            });

            listen(GainFocusEvent.class, (w, e) ->
            {
                startEditing();
            });
        }

        @Override
        public void onAdded() {
            super.onAdded();

            textBox = TextBox.get(widget);
            widget.transform.doesListenKey = true;
            updateKeyName();
        }

        private void updateKeyName() {
            textBox.setContent(KeyManager.getKeyName(prop.getInt()));
        }

        private void startEditing() {
            editing = true;
            textBox.setContent("PRESS");
            textBox.option.color = CRL_EDIT;

            widget.getGui().listen(MouseClickEvent.class,
                    gMouseHandler = (w, event) -> {
                        endEditing(event.button - 100);
                    });
        }

        private void endEditing(int key) {
            editing = false;
            textBox.option.color = CRL_NORMAL;
            widget.getGui().removeFocus();

            if (key == GLFW.GLFW_KEY_ESCAPE) {
                ;
            } else {
                prop.set(key);
            }

            updateKeyName();
            widget.getGui().unlisten(MouseClickEvent.class, gMouseHandler);
            MinecraftForge.EVENT_BUS.post(new ConfigModifyEvent(prop));
        }

    }

}
