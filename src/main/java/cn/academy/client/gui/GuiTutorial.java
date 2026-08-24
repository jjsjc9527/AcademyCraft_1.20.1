package cn.academy.client.gui;

import cn.academy.Resources;
import cn.academy.tutorial.ACTutorial;
import cn.academy.tutorial.TutorialRegistry;
import cn.academy.tutorial.client.ACMarkdownRenderer;
import cn.lambdalib2.cgui.CGuiScreen;
import cn.lambdalib2.cgui.Widget;
import cn.lambdalib2.cgui.component.DragBar;
import cn.lambdalib2.cgui.component.DrawTexture;
import cn.lambdalib2.cgui.component.ElementList;
import cn.lambdalib2.cgui.component.TextBox;
import cn.lambdalib2.cgui.component.Tint;
import cn.lambdalib2.cgui.component.Transform.HeightAlign;
import cn.lambdalib2.cgui.component.Transform.WidthAlign;
import cn.lambdalib2.cgui.event.FrameEvent;
import cn.lambdalib2.cgui.event.LeftClickEvent;
import cn.lambdalib2.render.font.Fonts;
import cn.lambdalib2.render.font.IFont;
import cn.lambdalib2.render.font.IFont.FontOption;
import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.HudUtils;
import cn.lambdalib2.util.markdown.GLMarkdownRenderer;
import cn.lambdalib2.util.markdown.MarkdownParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.tuple.Pair;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class GuiTutorial extends CGuiScreen {

    private static final double REF_WIDTH = 480;

    private static final ResourceLocation TEX_WINDOW = Resources.getTexture("guis/window_tutorial_left");
    private static final ResourceLocation LOGO0 = Resources.getTexture("guis/tutorial/logo0");
    private static final ResourceLocation LOGO1 = Resources.getTexture("guis/tutorial/logo1");
    private static final ResourceLocation LOGO2 = Resources.getTexture("guis/tutorial/logo2");
    private static final ResourceLocation LOGO3 = Resources.getTexture("guis/tutorial/logo3");
    private static final ResourceLocation SCROLL1 = Resources.getTexture("guis/button/widget_scroll_1");
    private static final ResourceLocation SCROLL2 = Resources.getTexture("guis/button/widget_scroll_2");
    private static final ResourceLocation BTN_LEFT = Resources.getTexture("guis/button/button_left_2");
    private static final ResourceLocation BTN_RIGHT = Resources.getTexture("guis/button/button_right_2");

    private final IFont font = Resources.font();
    private final FontOption titleOption = new FontOption(10);

    private final List<ACTutorial> learned, unlearned;

    private Widget frame, listArea;
    private Widget centerPart, rightWindow;
    private Widget showWindow, previewArea, tagArea, btnLeft, btnRight;
    private Widget[] logos;

    private ACTutorial currentTut;
    private int previewIndex;
    private int viewIndex;
    private List<cn.academy.client.gui.tutorial.RecipeViews.PreviewView> currentViews = List.of();

    private final Map<ACTutorial, CachedRenderInfo> cached = new HashMap<>();
    private final Map<Widget, String> tabText = new HashMap<>();

    public GuiTutorial() {
        super();
        Player player = Minecraft.getInstance().player;
        Pair<List<ACTutorial>, List<ACTutorial>> p = TutorialRegistry.groupByLearned(player);
        learned = p.getLeft();
        unlearned = p.getRight();
        initUI();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {

        if (frame != null) {
            frame.transform.scale = (float) (width / REF_WIDTH);
            frame.dirty = true;
        }
        super.render(g, mx, my, partial);
    }

    private final class CachedRenderInfo {
        final String title, rawBrief, rawContent;
        private GLMarkdownRenderer brief_, content_;

        CachedRenderInfo(String title, String brief, String content) {
            this.title = title;
            this.rawBrief = brief;
            this.rawContent = content;
        }

        GLMarkdownRenderer getBrief() {
            if (brief_ == null) brief_ = build(rawBrief, 130);
            return brief_;
        }

        GLMarkdownRenderer getContent() {
            if (content_ == null) content_ = build(rawContent, 150);
            return content_;
        }

        private GLMarkdownRenderer build(String raw, float widthLimit) {
            ACMarkdownRenderer r = new ACMarkdownRenderer();
            r.setFonts(font, font, font);
            r.widthLimit = widthLimit;
            r.fontSize = 8;
            MarkdownParser.accept(raw, r);
            return r;
        }
    }

    private CachedRenderInfo renderInfo(ACTutorial tut) {
        return cached.computeIfAbsent(tut, t -> {
            String raw = t.getContent();
            int i1 = raw.indexOf("![title]"), i2 = raw.indexOf("![brief]"), i3 = raw.indexOf("![content]");
            if (i1 != -1 && i1 < i2 && i2 < i3) {
                return new CachedRenderInfo(

                        raw.substring(i1 + 8, i2).trim(),
                        trimHead(raw.substring(i2 + 8, i3)),
                        trimHead(raw.substring(i3 + 10)));
            }
            return new CachedRenderInfo(t.id, "", "");
        });
    }

    private static String trimHead(String str) {
        int idx = 0;
        while (idx < str.length() && (str.charAt(idx) == '\r' || str.charAt(idx) == '\n' || str.charAt(idx) == ' ')) {
            idx++;
        }
        return str.substring(idx);
    }

    private void initUI() {
        frame = newFrame();

        Widget leftPart = frame.getWidget("leftPart");
        listArea = leftPart.getWidget("list");

        Widget rightPart = frame.getWidget("rightPart");
        centerPart = rightPart.getWidget("centerPart");
        rightWindow = rightPart.getWidget("rightWindow");
        showWindow = rightPart.getWidget("showWindow");
        previewArea = showWindow.getWidget("area");
        tagArea = showWindow.getWidget("tag_area");
        btnLeft = showWindow.getWidget("btn_left");
        btnRight = showWindow.getWidget("btn_right");
        logos = new Widget[]{
                rightPart.getWidget("logo0"), rightPart.getWidget("logo1"),
                rightPart.getWidget("logo2"), rightPart.getWidget("logo3")
        };

        centerPart.transform.doesDraw = false;
        rightWindow.transform.doesDraw = false;
        showWindow.transform.doesDraw = false;
        btnLeft.transform.doesDraw = false;
        btnRight.transform.doesDraw = false;

        previewArea.listen(FrameEvent.class, (w, e) -> {
            if (currentViews.isEmpty()) return;
            int idx = Math.min(viewIndex, currentViews.size() - 1);
            currentViews.get(idx).render(w.transform.width, w.transform.height, e.mx, e.my, e.hovering);
        });

        btnLeft.listen(LeftClickEvent.class, (w, e) -> cycleView(-1));
        btnRight.listen(LeftClickEvent.class, (w, e) -> cycleView(1));

        tagArea.listen(FrameEvent.class, (w, e) -> {
            String txt = tabText.get(gui.getHoveringWidget());
            if (txt != null && !txt.isEmpty()) {
                font.draw(txt, 0, -8, new FontOption(10));
            }
        });

        centerPart.getWidget("text").listen(FrameEvent.class, (w, e) -> {
            if (currentTut == null) return;
            GLMarkdownRenderer r = renderInfo(currentTut).getContent();
            double ht = Math.max(0, r.getMaxHeight() - w.transform.height + 10);
            double delta = DragBar.get(centerPart.getWidget("scroll_2")).getProgress() * ht;
            drawMarkdown(r, 3, (float) (3 - delta));
        });

        rightWindow.getWidget("text").listen(FrameEvent.class, (w, e) -> {
            if (currentTut == null) return;
            CachedRenderInfo info = renderInfo(currentTut);
            font.draw(info.title, 3, 3, titleOption);
            drawMarkdown(info.getBrief(), 3, 15);
        });

        rebuildList();

        gui.addWidget(frame);
    }

    private void drawMarkdown(GLMarkdownRenderer r, float dx, float dy) {
        Matrix4f saved = new Matrix4f(HudUtils.getMatrix());
        HudUtils.setMatrix(new Matrix4f(saved).translate(dx, dy, 0));
        r.render();
        HudUtils.setMatrix(saved);
    }

    private void rebuildList() {
        listArea.removeComponent(ElementList.class);
        ElementList el = new ElementList();
        buildList(el, learned, true);
        buildList(el, unlearned, false);
        listArea.addComponent(el);
    }

    private void buildList(ElementList el, List<ACTutorial> list, boolean isLearned) {
        for (ACTutorial t : list) {
            Widget w = new Widget();
            w.transform.setSize(72, 12);
            w.addComponent(new Tint(Colors.whiteBlend(0.0f), Colors.whiteBlend(0.3f), false));

            TextBox box = new TextBox(new FontOption(10, isLearned ? Colors.white() : Colors.fromFloatMono(0.6f)));
            box.font = Fonts.get("AC_Normal");
            box.setContent(renderInfo(t).title);
            box.localized = false;
            box.heightAlign = HeightAlign.CENTER;
            w.addComponent(box);

            w.listen(LeftClickEvent.class, (ww, e) -> onSelect(t, isLearned));
            el.addWidget(w);
        }
    }

    private void onSelect(ACTutorial t, boolean isLearned) {
        currentTut = t;
        previewIndex = 0;

        for (Widget logo : logos) logo.transform.doesDraw = false;
        rightWindow.transform.doesDraw = true;
        centerPart.transform.doesDraw = isLearned;
        showWindow.transform.doesDraw = !t.getPreview().isEmpty();
        buildTabs(t);
        rebuildViews();
        DragBar.get(centerPart.getWidget("scroll_2")).setProgress(0.0f);
    }

    private void buildTabs(ACTutorial t) {
        tagArea.clear();
        tabText.clear();
        java.util.List<cn.academy.tutorial.ViewGroup> pv = t.getPreview();
        float sz = tagArea.transform.height;
        float step = sz - 1;
        float x = 0;
        for (int i = 0; i < pv.size(); i++) {
            final int idx = i;
            Widget tab = new Widget();
            tab.transform.setSize(sz, sz).setPos(x, 0);
            tab.addComponent(new DrawTexture(pv.get(i).getTag().icon, Colors.monoBlend(1, 0.7f)));
            tab.addComponent(new Tint(Colors.monoBlend(1, 0.7f), Colors.monoBlend(1, 1), true));
            tab.listen(LeftClickEvent.class, (w, e) -> { previewIndex = idx; rebuildViews(); });
            tagArea.addWidget(tab);
            tabText.put(tab, pv.get(i).getDisplayText());
            x += step;
        }
    }

    private void rebuildViews() {
        viewIndex = 0;
        java.util.List<cn.academy.tutorial.ViewGroup> pv =
                currentTut == null ? List.of() : currentTut.getPreview();
        currentViews = (pv.isEmpty() || previewIndex >= pv.size())
                ? List.of()
                : cn.academy.client.gui.tutorial.RecipeViews.buildFor(pv.get(previewIndex));
        boolean multi = currentViews.size() >= 2;
        btnLeft.transform.doesDraw = multi;
        btnRight.transform.doesDraw = multi;
    }

    private void cycleView(int delta) {
        int n = currentViews.size();
        if (n < 2) return;
        viewIndex = ((viewIndex + delta) % n + n) % n;
    }

    private Widget newFrame() {
        Widget frame = new Widget();
        frame.transform.setSize(427, 240).setPos(0, 0).setCenteredAlign();
        frame.transform.doesDraw = true;

        Widget leftPart = new Widget();
        leftPart.transform.setSize(85, 220.5f).setPos(7, 0).setAlign(WidthAlign.LEFT, HeightAlign.CENTER);
        leftPart.addComponent(new DrawTexture(TEX_WINDOW));
        Widget list = new Widget();
        list.transform.setSize(72, 207).setPos(6.6f, 7).setAlign(WidthAlign.LEFT, HeightAlign.TOP);
        leftPart.addWidget("list", list);
        frame.addWidget("leftPart", leftPart);

        Widget rightPart = new Widget();
        rightPart.transform.setSize(332, 220.5f).setPos(92, 0).setAlign(WidthAlign.LEFT, HeightAlign.CENTER);

        rightPart.addWidget("logo1", logo(899, 236, 0, 59, LOGO1));
        rightPart.addWidget("logo0", logo(899, 548, 0, -32.5f, LOGO0));

        Widget centerPart = new Widget();
        centerPart.transform.setSize(172, 220.5f).setPos(0, 0).setAlign(WidthAlign.LEFT, HeightAlign.CENTER);
        centerPart.transform.doesDraw = true;
        {
            Widget text = new Widget();
            text.transform.setSize(160, 210.5f).setPos(2, 0).setAlign(WidthAlign.LEFT, HeightAlign.CENTER);
            centerPart.addWidget("text", text);

            Widget scroll2 = new Widget();
            scroll2.transform.setSize(9.5f, 53).setPos(0, 2).setAlign(WidthAlign.RIGHT, HeightAlign.TOP);
            scroll2.addComponent(new DrawTexture(SCROLL2, new cn.lambdalib2.util.Color(255, 255, 255, 204)));
            scroll2.addComponent(new DragBar(DragBar.Axis.Y, 2, 165));
            scroll2.addComponent(new Tint(new cn.lambdalib2.util.Color(255, 255, 255, 204),
                    new cn.lambdalib2.util.Color(255, 255, 255, 255), true));
            centerPart.addWidget("scroll_2", scroll2);

            Widget scroll1 = new Widget();
            scroll1.transform.setSize(9.5f, 216.5f).setPos(0, 0).setAlign(WidthAlign.RIGHT, HeightAlign.CENTER);
            scroll1.transform.doesListenKey = false;
            scroll1.addComponent(new DrawTexture(SCROLL1));
            centerPart.addWidget("scroll_1", scroll1);
        }
        rightPart.addWidget("centerPart", centerPart);

        Widget showWindow = new Widget();
        showWindow.transform.setSize(158.5f, 136).setPos(0, 0).setAlign(WidthAlign.RIGHT, HeightAlign.TOP);
        showWindow.transform.doesDraw = true;
        {
            Widget area = new Widget();
            area.transform.setSize(134, 134).setPos(0, -2).setAlign(WidthAlign.CENTER, HeightAlign.CENTER);
            showWindow.addWidget("area", area);

            Widget tagAreaW = new Widget();
            tagAreaW.transform.setSize(133, 18).setPos(12, 120.75f).setAlign(WidthAlign.LEFT, HeightAlign.TOP);
            showWindow.addWidget("tag_area", tagAreaW);

            showWindow.addWidget("btn_left", arrowButton(5f, 41.75f, BTN_LEFT));
            showWindow.addWidget("btn_right", arrowButton(140f, 41.75f, BTN_RIGHT));
        }
        rightPart.addWidget("showWindow", showWindow);

        Widget rightWindow = new Widget();
        rightWindow.transform.setSize(158.5f, 82).setPos(0, 0).setAlign(WidthAlign.RIGHT, HeightAlign.BOTTOM);
        rightWindow.addComponent(new DrawTexture(TEX_WINDOW));
        {
            Widget text = new Widget();
            text.transform.setSize(146, 69).setPos(0, -0.25f).setAlign(WidthAlign.CENTER, HeightAlign.CENTER);
            rightWindow.addWidget("text", text);
        }
        rightPart.addWidget("rightWindow", rightWindow);

        rightPart.addWidget("logo3", logo(149, 149, 0, -36, LOGO3));
        rightPart.addWidget("logo2", logo(899, 236, 0, 59, LOGO2));

        frame.addWidget("rightPart", rightPart);
        return frame;
    }

    private Widget logo(float w, float h, float x, float y, ResourceLocation tex) {
        Widget lg = new Widget();
        lg.transform.setSize(w, h).setPos(x, y).setAlign(WidthAlign.CENTER, HeightAlign.CENTER);
        lg.transform.scale = 0.25f;
        lg.addComponent(new DrawTexture(tex));
        return lg;
    }

    private Widget arrowButton(float x, float y, ResourceLocation tex) {
        Widget btn = new Widget();
        btn.transform.setSize(30, 130).setPos(x, y).setAlign(WidthAlign.LEFT, HeightAlign.TOP);
        btn.transform.scale = 0.4f;
        btn.addComponent(new DrawTexture(tex, Colors.monoBlend(1, 0.7f)));
        btn.addComponent(new Tint(Colors.monoBlend(1, 0.7f), Colors.monoBlend(1, 1), true));
        return btn;
    }
}
