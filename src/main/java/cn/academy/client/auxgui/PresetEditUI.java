package cn.academy.client.auxgui;

import cn.academy.Resources;
import cn.academy.ability.Controllable;
import cn.academy.ability.Skill;
import cn.academy.client.render.util.ACRenderingHelper;
import cn.academy.datapart.AbilityData;
import cn.academy.datapart.PresetData;
import cn.academy.datapart.PresetData.Preset;
import cn.lambdalib2.cgui.CGui;
import cn.lambdalib2.cgui.GuiOpenAnimation;
import cn.lambdalib2.cgui.Widget;
import cn.lambdalib2.cgui.component.Component;
import cn.lambdalib2.cgui.component.DrawTexture;
import cn.lambdalib2.cgui.component.TextBox;
import cn.lambdalib2.cgui.component.Tint;
import cn.lambdalib2.cgui.component.Transform.HeightAlign;
import cn.lambdalib2.cgui.component.Transform.WidthAlign;
import cn.lambdalib2.cgui.event.FrameEvent;
import cn.lambdalib2.cgui.event.IGuiEventHandler;
import cn.lambdalib2.cgui.event.LeftClickEvent;
import cn.lambdalib2.render.font.IFont;
import cn.lambdalib2.render.font.IFont.FontAlign;
import cn.lambdalib2.render.font.IFont.FontOption;
import cn.lambdalib2.util.Color;
import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.GameTimer;
import cn.lambdalib2.util.HudUtils;
import cn.lambdalib2.util.MathUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class PresetEditUI extends Screen {

    static final Color
            CRL_BACK = new Color(49, 49, 49, 200),
            CRL_WHITE = Colors.fromFloat(1, 1, 1, 0.6f),
            CRL_GLOW = Colors.fromFloat(1, 1, 1, 0.2f);

    static final float STEP = 125;
    static final double TRANSIT_TIME = 0.35;
    static final int MAX_ALPHA = Colors.f2i(1f), MIN_ALPHA = Colors.f2i(0.3f);
    static final float MAX_SCALE = 1, MIN_SCALE = 0.8f;

    static class SelectionProvider {
        public final int id;
        public final ResourceLocation texture;
        public final String hint;

        public SelectionProvider(int _id, ResourceLocation _texture, String _hint) {
            id = _id;
            texture = _texture;
            hint = _hint;
        }
    }

    CGui foreground = new CGui();

    CGui transitor = new CGui();

    final Player player;
    final PresetData data;
    final AbilityData aData;

    final IFont font = Resources.font();

    int lastActive, active;

    boolean transiting;
    double transitStartTime;
    double deltaTime;
    double transitProgress;

    private double openAnimStart = -1;

    Widget selector;

    public PresetEditUI() {
        super(net.minecraft.network.chat.Component.empty());
        player = Minecraft.getInstance().player;
        data = PresetData.get(player);
        aData = AbilityData.get(player);

        initPages();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private String local(String key) {
        return I18n.get("gui.academy.preset_edit." + key);
    }

    private void initPages() {
        foreground.addWidget(newBackground());
        transitor.addWidget(newBackground());

        for (int i = 0; i < PresetData.MAX_PRESETS; ++i) {
            Widget normal = createPage();
            TextBox.get(normal.getWidget("title")).setContent(local("tag") + (i + 1));

            for (int j = 0; j < PresetData.MAX_KEYS; ++j) {
                normal.getWidget(String.valueOf(j)).addComponent(new HintHandler(j));
            }
            normal.addComponent(new ForegroundPage(i));
            add(i, foreground, normal);
        }

        for (int i = 0; i < PresetData.MAX_PRESETS; ++i) {
            Widget back = createPage();
            TextBox.get(back.getWidget("title")).setContent(local("tag") + (i + 1));

            back.addComponent(new TransitPage(i));
            add(i, transitor, back);
        }

        resetAll();
    }

    private void resetAll() {
        updateInfo(foreground);
        updateInfo(transitor);

        updatePosForeground();
    }

    private Widget createPage() {
        Widget ret = newTemplate();
        for (Widget w : ret.getDrawList()) {
            for (Widget w2 : w.getDrawList())
                w2.listen(FrameEvent.class, 1, new AlphaAssign());
        }
        return ret;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {

            if (!transiting) {
                foreground.mouseClicked((int) mx, (int) my, button);
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void render(GuiGraphics gg, int mx, int my, float partialTicks) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        gg.fill(0, 0, width, height, 0xB3000000);

        if (openAnimStart < 0) openAnimStart = GameTimer.getAbsTime();
        float p = GuiOpenAnimation.progress(openAnimStart);
        gg.pose().pushPose();
        GuiOpenAnimation.apply(gg.pose(), p);

        if (transiting) {
            updateTransit();
            transitor.resize(width, height);

            transitor.draw(gg.pose(), -1, -1);
        } else {
            updatePosForeground();
            foreground.resize(width, height);
            foreground.draw(gg.pose(), mx, my);
        }

        RenderSystem.setShaderColor(1, 1, 1, 1);
        super.render(gg, mx, my, partialTicks);
        gg.pose().popPose();
    }

    private float getXFor(int i, int active) {
        if (i == active) {
            return 0;
        }
        return STEP * (i - active);
    }

    private float getXFor(int i) {
        return getXFor(i, active);
    }

    private void add(int i, CGui gui, Widget w) {
        gui.addWidget("" + i, w);
    }

    private Widget get(CGui gui, int i) {
        return gui.getWidget("" + i);
    }

    private void startTransit(int to) {
        updateInfo(transitor);

        lastActive = active;
        active = to;
        transiting = true;
        transitStartTime = GameTimer.getAbsTime();
    }

    private void finishTransit() {
        updatePosForeground();
    }

    private void onEdit(int keyID, Controllable controllable) {
        Preset last = data.getPreset(active);
        Controllable[] arr = last.copyData();
        arr[keyID] = controllable;

        Preset newPreset = new Preset(arr);
        data.setPresetFromClient(active, newPreset);
        getPage(get(foreground, active)).updateInfo(newPreset);
    }

    private void updateTransit() {
        deltaTime = GameTimer.getAbsTime() - transitStartTime;

        transitProgress = deltaTime / TRANSIT_TIME;
        if (transitProgress > 1) {
            transitProgress = 1;
        }

        for (int i = 0; i < PresetData.MAX_PRESETS; ++i) {
            Widget page = get(transitor, i);
            getPage(page).updatePosition();
        }

        if (transitProgress == 1) {
            transiting = false;
            finishTransit();
        }
    }

    private void updateInfo(CGui gui) {
        for (int i = 0; i < PresetData.MAX_PRESETS; ++i) {
            Widget page = get(gui, i);
            getPage(page).updateInfo(data.getPreset(i));
        }
    }

    private void updatePosForeground() {
        for (int i = 0; i < PresetData.MAX_PRESETS; ++i) {
            Widget page = get(foreground, i);
            getPage(page).updatePosition();
        }
    }

    private abstract class Page extends Component {

        protected int alpha;

        final int id;

        public Page(int _id) {
            super("Page");
            id = _id;
        }

        public void updateInfo(Preset preset) {

            for (int i = 0; i < PresetData.MAX_PRESETS; ++i) {
                Controllable c = preset.getControllable(i);
                Widget main = widget.getWidget("" + i);
                DrawTexture.get(main.getWidget("icon")).texture = c == null ? Resources.TEX_EMPTY : c.getHintIcon();
                TextBox.get(main.getWidget("text")).content = c == null ? "" : c.getHintText();
            }
        }

        public void updatePosition() {
            widget.transform.x = getXFor(id);
            widget.dirty = true;

            alpha = id == active ? MAX_ALPHA : MIN_ALPHA;
            widget.transform.scale = id == active ? MAX_SCALE : MIN_SCALE;
            DrawTexture.get(widget).color.setAlpha(alpha);
        }
    }

    static Page getPage(Widget w) {
        return w.getComponent(Page.class);
    }

    private class HintHandler extends Component {

        final int keyid;

        public HintHandler(int _keyid) {
            super("Hint");
            keyid = _keyid;

            listen(FrameEvent.class, (w, event) ->
            {
                Page page = getPage(w.getWidgetParent());
                DrawTexture dt = DrawTexture.get(w);

                dt.enabled = page.id == active && event.hovering;
                dt.color.setAlpha(page.alpha);
            });

            listen(LeftClickEvent.class, (w, e) -> {
                Page page = getPage(w.getWidgetParent());
                if (selector != null && !selector.disposed) {

                    selector.dispose();
                    selector = null;
                } else if (page.id == active) {
                    selector = new Selector(keyid);
                    selector.transform.setPos(foreground.getMouseX(), foreground.getMouseY());
                    foreground.addWidget(selector);
                } else {
                    startTransit(page.id);
                }
            });
        }

    }

    private class ForegroundPage extends Page {

        public ForegroundPage(int _id) {
            super(_id);
        }

    }

    private class TransitPage extends Page {

        public TransitPage(int _id) {
            super(_id);

            listen(FrameEvent.class, (w, e) ->
            {
                DrawTexture.get(w).color.setAlpha(alpha);
            });
        }

        @Override
        public void updatePosition() {
            double x0 = getXFor(id, lastActive), x1 = getXFor(id, active);
            float dx = (float) MathUtils.lerp(x0, x1, transitProgress);
            float scale;

            if (isFrom()) {
                alpha = (int) MathUtils.lerp(MAX_ALPHA, MIN_ALPHA, transitProgress);
                scale = (float) MathUtils.lerp(MAX_SCALE, MIN_SCALE, transitProgress);
            } else if (isTo()) {
                alpha = (int) MathUtils.lerp(MIN_ALPHA, MAX_ALPHA, transitProgress);
                scale = (float) MathUtils.lerp(MIN_SCALE, MAX_SCALE, transitProgress);
            } else {
                alpha = MIN_ALPHA;
                scale = MIN_SCALE;
            }

            widget.transform.x = dx;
            widget.transform.scale = scale;

            DrawTexture.get(widget).color.setAlpha(alpha);
            widget.dirty = true;
        }

        private boolean isFrom() {
            return id == lastActive;
        }

        private boolean isTo() {
            return id == active;
        }

    }

    private class AlphaAssign implements IGuiEventHandler<FrameEvent> {

        @Override
        public void handleEvent(Widget w, FrameEvent event) {
            int masterAlpha = getPage(w.getWidgetParent().getWidgetParent()).alpha;
            DrawTexture dt = DrawTexture.get(w);
            if (dt != null) {
                dt.color.setAlpha(masterAlpha);
            } else {
                TextBox.get(w).option.color.setAlpha(masterAlpha);
            }
        }

    }

    private class Selector extends Widget {
        final int MAX_PER_ROW = 4;
        final float MARGIN = 2.5f, SIZE = 15, STEP = SIZE + 3;

        List<Skill> available = new ArrayList<>();
        final int keyid;

        float width, height;

        public Selector(int _keyid) {
            keyid = _keyid;

            AbilityData aData = AbilityData.get(player);

            for (Skill s : aData.getControllableSkillList()) {
                if (!data.getPreset(active).hasControllable(s)) {
                    available.add(s);
                }
            }

            List<SelectionProvider> providers = new ArrayList<>();

            providers.add(new SelectionProvider(-1, Resources.getTexture("gui/preset_settings/cancel"),
                    local("skill_remove")));
            for (Skill s : available) {
                providers.add(new SelectionProvider(s.getControlID(), s.getHintIcon(), s.getDisplayName()));
            }

            height = MARGIN * 2 + SIZE + STEP * (ldiv(providers.size(), MAX_PER_ROW) - 1);

            width = available.size() < MAX_PER_ROW
                    ? MARGIN * 2 + SIZE + STEP * (providers.size() - 1)
                    : MARGIN * 2 + SIZE + STEP * (MAX_PER_ROW - 1);

            transform.setSize(width, height);

            listen(FrameEvent.class, (w, e) -> {
                Colors.bindToGL(CRL_WHITE);
                ACRenderingHelper.drawGlow(0, 0, width, height, 1, CRL_WHITE);

                Colors.bindToGL(CRL_BACK);
                HudUtils.colorRect(0, 0, width, height);

                String str;
                Widget hovering = foreground.getHoveringWidget();
                if (hovering != null && hovering.getName().contains("_sel")) {
                    SelHandler sh = hovering.getComponent(SelHandler.class);
                    str = sh.selection.hint;
                } else {
                    str = local("skill_select");
                }

                FontOption opt = new FontOption(9, Colors.fromHexColor(0xffbbbbbb));
                double len = font.getTextWidth(str, opt);

                Colors.bindToGL(CRL_BACK);
                HudUtils.colorRect(0, -13.5, len + 6, 11.5);

                ACRenderingHelper.drawGlow(0, -13.5, len + 6, 11.5, 1, CRL_GLOW);

                font.draw(str, 3, -12, opt);

                RenderSystem.setShaderColor(1, 1, 1, 1);
            });

            for (int i = 0; i < providers.size(); ++i) {
                int row = i / MAX_PER_ROW, col = i % MAX_PER_ROW;
                SelectionProvider selection = providers.get(i);
                Widget single = new Widget();
                single.transform.setPos(MARGIN + col * STEP, MARGIN + row * STEP);
                single.transform.setSize(SIZE, SIZE);

                DrawTexture tex = new DrawTexture().setTex(selection.texture);
                single.addComponent(tex);
                single.addComponent(new Tint(Colors.monoBlend(1, 0), Colors.monoBlend(1, 0.2f), false));
                single.addComponent(new SelHandler(selection));
                addWidget("_sel" + i, single);
            }
        }

        private class SelHandler extends Component {

            final SelectionProvider selection;

            public SelHandler(SelectionProvider _selection) {
                super("_sel");
                selection = _selection;
                listen(LeftClickEvent.class, (w, e) -> {
                    onEdit(keyid, aData.getCategory().getControllable(selection.id));
                    Selector.this.dispose();
                });
            }

        }
    }

    private int ldiv(int a, int b) {
        return a % b == 0 ? a / b : a / b + 1;
    }

    private static Widget newBackground() {
        Widget w = new Widget();
        w.transform.setSize(80, 18).setPos(5, 5);
        w.transform.doesListenKey = false;
        TextBox tb = new TextBox(new FontOption(12, FontAlign.LEFT, new Color(255, 255, 255, 255)));
        tb.font = Resources.font();
        tb.heightAlign = HeightAlign.TOP;
        tb.localized = true;
        tb.setContent("gui.academy.preset_edit.name");
        w.addComponent(tb);
        return w;
    }

    private static Widget newTemplate() {
        Widget ret = new Widget();
        ret.transform.setSize(116.25f, 141.5f).setPos(0, 0).setCenteredAlign();
        ret.addComponent(new DrawTexture(Resources.getTexture("gui/preset_settings/back")));

        float[] ys = {1.5f, 36.25f, 71.0f, 105.75f};
        for (int i = 0; i < 4; i++) {
            Widget slot = new Widget();
            slot.transform.setSize(110, 34.75f).setPos(0.5f, ys[i])
                    .setAlign(WidthAlign.CENTER, HeightAlign.TOP);
            slot.addComponent(new DrawTexture(Resources.getTexture("gui/preset_settings/selected")));

            Widget icon = new Widget();
            icon.transform.setSize(26.5f, 26.5f).setPos(2.25f, 3.75f);
            icon.transform.doesListenKey = false;
            icon.addComponent(new DrawTexture(Resources.TEX_EMPTY));
            slot.addWidget("icon", icon);

            Widget text = new Widget();
            text.transform.setSize(62.5f, 15).setPos(36.25f, 10);
            text.transform.doesListenKey = false;
            TextBox tb = new TextBox(new FontOption(10, FontAlign.LEFT, new Color(255, 255, 255, 255)));
            tb.font = Resources.font();
            tb.heightAlign = HeightAlign.CENTER;
            tb.localized = false;
            slot.addWidget("text", text);
            text.addComponent(tb);

            ret.addWidget(String.valueOf(i), slot);
        }

        Widget title = new Widget();
        title.transform.setSize(35, 15).setPos(0.5f, -15)
                .setAlign(WidthAlign.CENTER, HeightAlign.TOP);
        title.transform.doesListenKey = false;
        TextBox tt = new TextBox(new FontOption(10, FontAlign.LEFT, new Color(255, 255, 255, 255)));
        tt.font = Resources.font();
        tt.heightAlign = HeightAlign.CENTER;
        tt.localized = false;
        title.addComponent(tt);
        ret.addWidget("title", title);

        return ret;
    }

}
