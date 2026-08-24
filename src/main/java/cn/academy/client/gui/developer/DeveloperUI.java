package cn.academy.client.gui.developer;

import cn.academy.ACItems;
import cn.academy.Resources;
import cn.academy.ability.AbilityLocalization;
import cn.academy.ability.Category;
import cn.academy.ability.Skill;
import cn.academy.ability.SkillTab;
import cn.academy.ability.develop.DevelopData;
import cn.academy.ability.develop.DevelopData.DevState;
import cn.academy.ability.develop.DeveloperType;
import cn.academy.ability.develop.IDeveloper;
import cn.academy.ability.develop.LearningHelper;
import cn.academy.ability.develop.action.DevelopActionLevel;
import cn.academy.ability.develop.action.DevelopActionReset;
import cn.academy.ability.develop.action.DevelopActionSkill;
import cn.academy.ability.develop.condition.IDevCondition;
import cn.academy.block.tileentity.DeveloperBlockEntity;
import cn.academy.client.gui.WirelessPanel;
import cn.academy.client.gui.developer.Console.Command;
import cn.academy.client.gui.developer.Console.Task;
import cn.academy.datapart.AbilityData;
import cn.academy.network.DeveloperActionMessage;
import cn.lambdalib2.cgui.CGui;
import cn.lambdalib2.cgui.CGuiScreen;
import cn.lambdalib2.cgui.Widget;
import cn.lambdalib2.cgui.component.Component;
import cn.lambdalib2.cgui.component.DrawTexture;
import cn.lambdalib2.cgui.component.ProgressBar;
import cn.lambdalib2.cgui.component.TextBox;
import cn.lambdalib2.cgui.component.Tint;
import cn.lambdalib2.cgui.component.Transform.HeightAlign;
import cn.lambdalib2.cgui.component.Transform.WidthAlign;
import cn.lambdalib2.cgui.event.FrameEvent;
import cn.lambdalib2.cgui.event.LeftClickEvent;
import cn.lambdalib2.render.font.Fonts;
import cn.lambdalib2.render.font.IFont;
import cn.lambdalib2.render.font.IFont.FontAlign;
import cn.lambdalib2.render.font.IFont.FontOption;
import cn.lambdalib2.util.Color;
import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.GameTimer;
import cn.lambdalib2.util.HudUtils;
import cn.lambdalib2.util.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class DeveloperUI {

    private static final ResourceLocation TEX_AREA_BACK        = Resources.getTexture("gui/effect_developer_background");
    private static final ResourceLocation TEX_SKILL_BACK       = Resources.getTexture("gui/dev_skill_back");
    private static final ResourceLocation TEX_SKILL_MASK       = Resources.getTexture("gui/dev_skill_radial_mask");
    private static final ResourceLocation TEX_SKILL_OUTLINE    = Resources.getTexture("gui/dev_skill_outline");
    private static final ResourceLocation TEX_LINE             = Resources.getTexture("gui/dev_line");
    private static final ResourceLocation TEX_VIEW_OUTLINE     = Resources.getTexture("gui/dev_skill_view_outline");
    private static final ResourceLocation TEX_VIEW_OUTLINE_GLOW= Resources.getTexture("gui/dev_skill_view_outline_glow");
    private static final ResourceLocation TEX_BUTTON           = Resources.getTexture("gui/dev_button");
    private static final ResourceLocation TEX_BUTTON_LEARN     = Resources.getTexture("gui/button_learn");
    private static final ResourceLocation TEX_PARENT_RIGHT     = Resources.getTexture("gui/parent_background_developerright");
    private static final ResourceLocation TEX_PARENT_LEFT      = Resources.getTexture("gui/parent_background_developerleft");
    private static final ResourceLocation TEX_PARENT_MACHINE   = Resources.getTexture("gui/parent_background_developermachine");
    private static final ResourceLocation TEX_UI_RIGHT         = Resources.getTexture("gui/ui_developerright");
    private static final ResourceLocation TEX_UI_LEFT          = Resources.getTexture("gui/ui_developerleft");
    private static final ResourceLocation TEX_UI_LEFT_TREE     = Resources.getTexture("gui/ui_developerleft_skilltree");
    private static final ResourceLocation TEX_ELEMENT_BACK     = Resources.getTexture("gui/element_background300x32");
    private static final ResourceLocation TEX_ICON_NODE        = Resources.getTexture("gui/icon_node");

    private static final ResourceLocation TEX_SKILL_AURA       = Resources.getTexture("gui/skill_aura");
    private static final ResourceLocation TEX_ICON_NOCATEGORY  = Resources.getTexture("gui/icon_nocategory");

    private static final FontOption FO_SKILL_TITLE      = new FontOption(12, FontAlign.CENTER);
    private static final FontOption FO_SKILL_DESC       = new FontOption(9,  FontAlign.CENTER);
    private static final FontOption FO_SKILL_PROG       = new FontOption(8,  FontAlign.CENTER, Colors.fromHexColor(0xffa1e1ff));
    private static final FontOption FO_SKILL_UNLEARNED  = new FontOption(10, FontAlign.CENTER, Colors.fromHexColor(0xffff5555));
    private static final FontOption FO_SKILL_UNLEARNED2 = new FontOption(10, FontAlign.CENTER, Colors.fromHexColor(0xaaffffff));
    private static final FontOption FO_SKILL_REQ        = new FontOption(9,  FontAlign.RIGHT,  Colors.fromHexColor(0xaaffffff));
    private static final FontOption FO_SKILL_REQ_DETAIL = new FontOption(9,  FontAlign.LEFT,   Colors.fromHexColor(0xeeffffff));
    private static final FontOption FO_SKILL_REQ_DETAIL2= new FontOption(9,  FontAlign.LEFT,   Colors.fromHexColor(0xffee5858));

    private static final FontOption FO_TAB              = new FontOption(12, FontAlign.CENTER, Colors.fromHexColor(0xff555555));
    private static final FontOption FO_TAB_SELECTED     = new FontOption(12, FontAlign.CENTER, Colors.fromHexColor(0xff262626));
    private static final FontOption FO_LEVEL_TITLE      = new FontOption(12, FontAlign.CENTER);
    private static final FontOption FO_LEVEL_REQ        = new FontOption(9,  FontAlign.CENTER);

    private static IFont font() {
        return Fonts.getDefault();
    }

    private static IFont fontBold() {
        return Fonts.getDefault();
    }

    private static String local(String key, Object... args) {
        return AbilityLocalization.instance.local(key, args);
    }

    private static Player player() {
        return Minecraft.getInstance().player;
    }

    private DeveloperUI() {}

    public static CGuiScreen open(DeveloperBlockEntity be) {
        return new DeveloperScreen(be, be.getBlockPos());
    }

    public static CGuiScreen openSkillTreeOnly() {
        return new DeveloperScreen(null, null);
    }

    public static CGuiScreen openPortable(cn.academy.ability.develop.PortableDevData data) {
        return new DeveloperScreen(data, null);
    }

    private static boolean isPortable(@Nullable IDeveloper developer) {
        return developer != null
                && developer.getDeveloperType() == cn.academy.ability.develop.DeveloperType.PORTABLE;
    }

    public static class DeveloperScreen extends CGuiScreen implements WirelessPanel.Host {

        @Nullable
        private final IDeveloper developer;
        @Nullable
        private final BlockPos pos;

        @Nullable
        private WirelessPanel wirelessPanel;
        @Nullable
        private Widget wirelessCover;

        public DeveloperScreen(@Nullable IDeveloper developer, @Nullable BlockPos pos) {
            this.developer = developer;
            this.pos = pos;

            gui.listen(RebuildEvent.class, (w, e) -> build());

            DeveloperInfoClient.clear();
            build();
        }

        private void build() {
            closeWirelessPage();
            gui.clear();
            gui.addWidget("main", initialize(gui, developer, pos, this));
        }

        private int wpX() { return (width - WirelessPanel.PW) / 2; }

        private int wpY() { return (height - WirelessPanel.PH) / 2; }

        void openWirelessPage() {
            if (pos == null || wirelessPanel != null) return;
            wirelessPanel = new WirelessPanel(this, pos, WirelessPanel.ICON_TONODE);
            wirelessPanel.requestInfo();

            Widget cover = blackCover(gui);
            cover.listen(CloseEvent.class, () -> {
                wirelessPanel = null;
                wirelessCover = null;
                DeveloperInfoClient.clear();
                if (pos != null) DeveloperActionMessage.sendGetNode(pos);
                gui.postEvent(new RebuildEvent());
            });
            wirelessCover = cover;
            gui.addWidget("link_page", cover);
        }

        private void closeWirelessPage() {
            if (wirelessPanel != null) {
                wirelessPanel.clearPassBoxes();
                wirelessPanel = null;
            }
            wirelessCover = null;
        }

        public void onWirelessInfo(cn.academy.network.WirelessInfoMessage m) {
            if (wirelessPanel != null) {
                wirelessPanel.onInfo(m, wpX(), wpY());
            }
        }

        @Override public Font font() { return font; }
        @Override public void addPassBox(EditBox box) { addRenderableWidget(box); }
        @Override public void removePassBox(EditBox box) { removeWidget(box); }
        @Override public void focusPassBox(EditBox box) { setFocused(box); }

        @Override
        public void render(GuiGraphics g, int mx, int my, float partialTick) {
            super.render(g, mx, my, partialTick);
            if (wirelessPanel != null) {
                wirelessPanel.render(g, wpX(), wpY(), partialTick);

                wirelessPanel.renderText(g, wpX(), wpY());
            }
        }

        @Override
        public boolean mouseClicked(double mx, double my, int btn) {
            if (wirelessPanel != null) {
                if (wirelessPanel.mouseClicked(mx, my, btn, wpX(), wpY())) return true;

                if (wirelessPanel.isInside(mx, my, wpX(), wpY())) return true;
                endWirelessCover();
                return true;
            }
            return super.mouseClicked(mx, my, btn);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (wirelessPanel != null) {
                if (wirelessPanel.keyPressed(keyCode, scanCode, modifiers)) return true;
                if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                    endWirelessCover();
                    return true;
                }
                return true;
            }

            if (minecraft != null && minecraft.options.keyInventory.matches(keyCode, scanCode)) {
                Widget f = gui.getFocus();
                if (f == null || f.getComponent(Console.class) == null) {
                    onClose();
                    return true;
                }
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public boolean charTyped(char ch, int modifiers) {
            if (wirelessPanel != null) {

                if (getFocused() instanceof EditBox box) return box.charTyped(ch, modifiers);
                return true;
            }
            return super.charTyped(ch, modifiers);
        }

        private void endWirelessCover() {
            if (wirelessCover != null) {
                Cover c = wirelessCover.getComponent(Cover.class);
                if (c != null && !c.isEnded()) c.end();
            }

            if (wirelessPanel != null) wirelessPanel.clearPassBoxes();
        }

        @Override
        public void removed() {
            if (developer != null) {
                developer.onGuiClosed();
            }
            super.removed();
        }

    }

    private static Widget initialize(CGui gui, @Nullable IDeveloper developer, @Nullable BlockPos pos,
                                     @Nullable DeveloperScreen screen) {
        Player player = player();
        AbilityData aData = AbilityData.get(player);
        DevelopData devData = DevelopData.get(player);

        Widget main = new Widget().size(400, 187).centered();

        Widget parentRight = new Widget().size(278, 187).pos(-4, 0)
                .walign(WidthAlign.RIGHT).halign(HeightAlign.CENTER)
                .addComponents(new DrawTexture(TEX_PARENT_RIGHT));

        Widget uiRight = new Widget().size(278, 187)
                .walign(WidthAlign.RIGHT).halign(HeightAlign.CENTER)
                .addComponents(new DrawTexture(TEX_UI_RIGHT));

        Widget area = new Widget().size(257, 139).pos(10, 18);
        parentRight.addWidget("ui_right", uiRight);
        parentRight.addWidget("area", area);
        main.addWidget("parent_right", parentRight);

        boolean holdingCoil = !player.getMainHandItem().isEmpty()
                && player.getMainHandItem().getItem() == ACItems.MAGNETIC_COIL.get();

        if (!aData.hasCategory()) {
            initConsole(area, devData, developer, pos);
        } else if (holdingCoil) {
            initReset(area, devData, developer, pos);
        } else {
            initSkillTree(gui, parentRight, area, aData, developer, pos);
        }

        Widget parentLeft = new Widget().size(108.5f, 187).pos(4, 0)
                .walign(WidthAlign.LEFT).halign(HeightAlign.CENTER)
                .addComponents(new DrawTexture(TEX_PARENT_LEFT));

        Widget uiLeft = new Widget().size(108.5f, 187)
                .walign(WidthAlign.RIGHT).halign(HeightAlign.CENTER)
                .addComponents(new DrawTexture(TEX_UI_LEFT));
        parentLeft.addWidget("ui_left", uiLeft);

        parentLeft.addWidget("panel_ability", initAbilityPanel(gui, aData, developer, pos));

        Widget panelMachine = initMachinePanel(developer, pos, screen);
        parentLeft.addWidget("panel_machine", panelMachine);

        if (developer == null) {

            uiLeft.getComponent(DrawTexture.class).setTex(TEX_UI_LEFT_TREE);
            panelMachine.transform.doesDraw = false;
        }

        main.addWidget("parent_left", parentLeft);
        return main;
    }

    private static Widget initAbilityPanel(CGui gui, AbilityData aData,
                                           @Nullable IDeveloper developer, @Nullable BlockPos pos) {

        Widget panel = new Widget().size(104, 32).pos(2, -10)
                .walign(WidthAlign.LEFT).halign(HeightAlign.CENTER);

        Category cat = aData.getCategoryNullable();
        ResourceLocation icon = cat != null ? cat.getDeveloperIcon() : TEX_ICON_NOCATEGORY;
        String name = cat != null ? cat.getDisplayName() : "N/A";
        float prog = cat != null ? Math.max(0.02f, aData.getLevelProgress()) : 0.0f;

        panel.addWidget("logo_ability", new Widget().size(32, 32)
                .walign(WidthAlign.LEFT).halign(HeightAlign.CENTER)
                .addComponents(new DrawTexture(icon)));

        panel.addWidget("text_abilityname", new Widget().size(70, 12).pos(14, -8)
                .centered()
                .addComponents(textBox(name, 13, FontAlign.LEFT, Colors.white(), HeightAlign.TOP)));

        panel.addWidget("logo_progress_back", new Widget().size(70, 1.5f).pos(31, -2)
                .walign(WidthAlign.LEFT).halign(HeightAlign.CENTER)
                .addComponents(progressBar(1.0, new Color(102, 102, 102, 76))));

        panel.addWidget("logo_progress", new Widget().size(70, 1.5f).pos(31, -2)
                .walign(WidthAlign.LEFT).halign(HeightAlign.CENTER)
                .addComponents(progressBar(prog, Colors.white())));

        panel.addWidget("text_exp", new Widget().size(42, 10).pos(30, 15.5f)
                .walign(WidthAlign.LEFT).halign(HeightAlign.TOP)
                .addComponents(textBox("EXP " + (int) (aData.getLevelProgress() * 100) + "%",
                        8, FontAlign.LEFT, Colors.white(), HeightAlign.CENTER)));

        Widget textLevel = new Widget().size(70, 12).pos(30, 13.5f)
                .walign(WidthAlign.LEFT).halign(HeightAlign.CENTER)
                .addComponents(textBox(AbilityLocalization.instance.levelDesc(aData.getLevel()),
                        9, FontAlign.LEFT, new Color(17, 119, 214, 255), HeightAlign.TOP));
        textLevel.transform.doesListenKey = false;
        panel.addWidget("text_level", textLevel);

        if (developer != null && aData.hasCategory()
                && LearningHelper.canLevelUp(developer.getDeveloperType(), aData)) {
            Widget btn = new Widget().size(186.01398601398603f, 59.52447552447553f).pos(60, 14.5f)
                    .scale(0.26f)
                    .walign(WidthAlign.LEFT).halign(HeightAlign.TOP)
                    .addComponents(new DrawTexture(TEX_BUTTON_LEARN, Colors.whiteBlend(178 / 255f)),
                            new Tint(Colors.whiteBlend(178 / 255f), Colors.white(), true));
            btn.transform.doesDraw = true;
            btn.listen(LeftClickEvent.class, () -> gui.addWidget(levelUpArea(gui, aData, developer, pos)));
            panel.addWidget("btn_upgrade", btn);

            panel.removeWidget("text_level");
        }

        return panel;
    }

    private static Widget initMachinePanel(@Nullable IDeveloper developer, @Nullable BlockPos pos,
                                           @Nullable DeveloperScreen screen) {

        Widget panel = new Widget().size(108.5f, 187)
                .walign(WidthAlign.LEFT).halign(HeightAlign.TOP)
                .addComponents(new DrawTexture(TEX_PARENT_MACHINE));
        panel.transform.doesListenKey = false;

        Widget textWireless = new Widget().size(100, 12).pos(0, 17).centered()
                .addComponents(textBox("Current Node:", 12, FontAlign.LEFT, Colors.white(), HeightAlign.TOP));
        panel.addWidget("text_wireless", textWireless);

        panel.addWidget("text_power", new Widget().size(100, 12).pos(0, BAR_POWER_Y - BAR_GAP).centered()
                .addComponents(textBox("Power:", 12, FontAlign.LEFT, Colors.white(), HeightAlign.TOP)));

        panel.addWidget("text_syncrate", new Widget().size(100, 12).pos(0, BAR_SYNC_Y - BAR_GAP).centered()
                .addComponents(textBox("Sync Rate:", 12, FontAlign.LEFT, Colors.white(), HeightAlign.TOP)));

        Widget buttonWireless = new Widget().size(100, 16).pos(0, 29).centered()
                .addComponents(new DrawTexture(TEX_ELEMENT_BACK, Colors.whiteBlend(178 / 255f)),
                        new Tint(Colors.whiteBlend(178 / 255f), Colors.white(), true));

        TextBox nodeName = textBox("N/A", 12, FontAlign.LEFT, Colors.white(), HeightAlign.TOP);
        Widget wNodeName = new Widget().size(70, 12).pos(11, NODE_NAME_DY).centered().addComponents(nodeName);
        wNodeName.transform.doesListenKey = false;
        buttonWireless.addWidget("text_nodename", wNodeName);

        Widget logoNode = new Widget().size(12, 12).pos(-37, 0).centered()
                .addComponents(new DrawTexture(TEX_ICON_NODE));
        logoNode.transform.doesListenKey = false;
        buttonWireless.addWidget("logo_node", logoNode);
        panel.addWidget("button_wireless", buttonWireless);

        ProgressBar progPower = progressBar(0.5, new Color(252, 197, 50, 255));
        Widget wProgPower = new Widget().size(97, 8).pos(0, BAR_POWER_Y).centered().addComponents(progPower);
        panel.addWidget("progress_power", wProgPower);

        ProgressBar progRate = progressBar(0.6, new Color(50, 164, 252, 255));
        Widget wProgRate = new Widget().size(97, 8).pos(0, BAR_SYNC_Y).centered().addComponents(progRate);
        panel.addWidget("progress_syncrate", wProgRate);

        if (developer != null) {
            wProgPower.listen(FrameEvent.class, () ->
                    progPower.progress = developer.getEnergy() / developer.getMaxEnergy());

            progRate.progress = developer.getDeveloperType().syncRate;

            if (pos != null) {

                DeveloperActionMessage.sendGetNode(pos);
                wNodeName.listen(FrameEvent.class, () -> {
                    String n = DeveloperInfoClient.getNodeName();
                    nodeName.setContent(n != null ? n : "N/A");
                });

                if (screen != null) {
                    buttonWireless.listen(LeftClickEvent.class, screen::openWirelessPage);
                }
            }
        } else {

            buttonWireless.transform.doesDraw = false;
            textWireless.transform.doesDraw = false;
        }

        if (pos == null) {
            buttonWireless.transform.doesDraw = false;
            buttonWireless.transform.doesListenKey = false;
            textWireless.transform.doesDraw = false;
        }

        return panel;
    }

    private static void initConsole(Widget area, DevelopData data,
                                    @Nullable IDeveloper developer, @Nullable BlockPos pos) {
        Console console = new Console(false, developer != null);

        if (developer != null && (pos != null || isPortable(developer))) {
            console.addCommand(new Command("learn", () -> {
                console.enqueue(console.printTask(Console.localized("dev_begin")));
                console.enqueue(console.printTask(Console.localized("progress", fmt(0))));
                DeveloperActionMessage.sendStartLevel(pos, isPortable(developer));
                data.reset();
                console.enqueue(progressTask(console, data, "dev_succ", "dev_fail"));
            }));
        }

        area.addComponent(console);
    }

    private static void initReset(Widget area, DevelopData data,
                                  @Nullable IDeveloper developer, @Nullable BlockPos pos) {
        Console console = new Console(true, true);

        console.addCommand(new Command("reset", () -> {

            if (developer != null && (pos != null || isPortable(developer))
                    && DevelopActionReset.canReset(player(), developer)) {
                console.enqueue(console.printTask(Console.localized("reset_begin")));
                console.enqueue(console.printTask(Console.localized("progress", fmt(0))));
                DeveloperActionMessage.sendReset(pos, isPortable(developer));
                data.reset();
                console.enqueue(progressTask(console, data, "reset_succ", "reset_fail"));
            } else {

                if (developer == null || developer.getDeveloperType() != DeveloperType.ADVANCED) {
                    console.enqueue(console.printTask(Console.localized("reset_fail_dev")));
                } else {
                    console.enqueue(console.printTask(Console.localized("reset_fail_other")));
                }
            }
        }));

        area.addComponent(console);
    }

    private static Task progressTask(Console console, DevelopData data, String succKey, String failKey) {
        return new Task() {
            @Override
            public boolean isFinished() {
                return data.getState() == DevState.FAILED || data.getState() == DevState.DONE;
            }

            @Override
            public void update() {

                console.output("\b\b\b" + fmt((int) (data.getDevelopProgress() * 100)) + "%");
            }

            @Override
            public void finish() {
                console.outputln();
                console.output(Console.localized(data.getState() == DevState.DONE ? succKey : failKey));
                console.pause(0.5);
                console.enqueueRebuild();
            }
        };
    }

    private static String fmt(int x) {
        return x < 10 ? "0" + x : String.valueOf(x);
    }

    private static void initSkillTree(CGui gui, Widget parentRight, Widget area, AbilityData aData,
                                      @Nullable IDeveloper developer, @Nullable BlockPos pos) {
        final double backScale = 1.01;
        final double backScaleInv = 1 / backScale;
        final double maxDu = backScale - 1;
        final double maxDuSkills = 10;

        final float[] d = new float[2];

        area.listen(FrameEvent.class, (w, e) -> {
            CGui g = w.getGui();
            d[0] = MathUtils.clampf(0, 1, g.getMouseX() / g.getWidth()) - 0.5f;
            d[1] = MathUtils.clampf(0, 1, g.getMouseY() / g.getHeight()) - 0.5f;

            HudUtils.loadTexture(TEX_AREA_BACK);
            HudUtils.rawRect(0, 0,
                    parallaxUV(d[0] * maxDu, backScaleInv), parallaxUV(d[1] * maxDu, backScaleInv),
                    w.transform.width, w.transform.height,
                    backScaleInv, backScaleInv);
        });

        List<Skill> skills = new ArrayList<>();
        for (Skill s : aData.getCategory().getSkillList()) {
            if (LearningHelper.canBePotentiallyLearned(aData, s) && s.isEnabled()) {
                skills.add(s);
            }
        }

        Widget pageNormal = new Widget().size(area.transform.width, area.transform.height);
        Widget pageAdvanced = new Widget().size(area.transform.width, area.transform.height);
        Widget pageGeneric = new Widget().size(area.transform.width, area.transform.height);

        final Widget[] pages = new Widget[SkillTab.values().length];
        pages[SkillTab.NORMAL.ordinal()]   = pageNormal;
        pages[SkillTab.ADVANCED.ordinal()] = pageAdvanced;
        pages[SkillTab.GENERIC.ordinal()]  = pageGeneric;

        for (Skill s : skills) {
            pages[s.getTab().ordinal()].addWidget(skillWidget(gui, aData, s, d, maxDuSkills, developer, pos));
        }
        area.addWidget("page_normal", pageNormal);
        area.addWidget("page_advanced", pageAdvanced);
        area.addWidget("page_generic", pageGeneric);

        pageAdvanced.listen(FrameEvent.class, (w, e) -> {
            if (w.widgetCount() == 0) {
                font().draw(local("advanced_empty"), w.transform.width / 2,
                        (w.transform.height - font().getTextWidth("进", FO_SKILL_DESC)) / 2, FO_SKILL_DESC);
            }
        });

        Widget tabAdvanced = tabButton(local("tab_advanced"), TAB_X0 + TAB_STEP, SkillTab.ADVANCED, pages);
        Widget tabGeneric = tabButton(local("tab_generic"), TAB_X0 + 2 * TAB_STEP, SkillTab.GENERIC, pages);
        parentRight.addWidget("tab_normal", tabButton(local("tab_normal"), TAB_X0, SkillTab.NORMAL, pages));
        parentRight.addWidget("tab_advanced", tabAdvanced);
        parentRight.addWidget("tab_generic", tabGeneric);

        area.listen(FrameEvent.class, (w, e) -> refreshTabs(aData, developer, tabAdvanced, tabGeneric, pages));
        refreshTabs(aData, developer, tabAdvanced, tabGeneric, pages);
    }

    private static SkillTab currentTab = SkillTab.NORMAL;

    private static void refreshTabs(AbilityData aData, @Nullable IDeveloper developer,
                                    Widget tabAdvanced, Widget tabGeneric, Widget[] pages) {
        boolean adv = LearningHelper.canUseAdvancedTree(developer, aData);
        tabAdvanced.transform.doesDraw = adv;
        tabGeneric.transform.x = TAB_X0 + (adv ? 2 : 1) * TAB_STEP;

        if (!adv && currentTab == SkillTab.ADVANCED) {
            currentTab = SkillTab.NORMAL;
        }
        applyTab(pages);
    }

    private static final float TAB_W = 28, TAB_H = 14;

    private static final float TAB_X0 = 12, TAB_STEP = TAB_W + 2, TAB_Y = 3;

    private static final float TAB_CUT = 5;

    private static void tabShape(float w, float h, float cut, float alpha) {
        Colors.bindToGL(Colors.whiteBlend(Math.min(1f, alpha)));
        for (int i = 0; i < (int) h; i++) {
            float l = i < cut ? cut - i : 0;
            float r = i >= h - cut ? w - (cut - (h - 1 - i)) : w;
            if (r > l) {
                HudUtils.colorRect(l, i, r - l, 1);
            }
        }

        Colors.bindToGL(Colors.whiteBlend(Math.min(1f, alpha) * 0.45f));
        for (int i = 0; i < (int) h; i++) {
            float l = i < cut ? cut - i : 0;
            float r = i >= h - cut ? w - (cut - (h - 1 - i)) : w;
            if (l > 0) {
                HudUtils.colorRect(l - 1, i, 1, 1);
            }
            if (r < w) {
                HudUtils.colorRect(r, i, 1, 1);
            }
        }
    }

    private static Widget tabButton(String text, float x, SkillTab tab, Widget[] pages) {
        Widget w = new Widget().size(TAB_W, TAB_H).pos(x, TAB_Y);
        w.listen(FrameEvent.class, (wd, e) -> {
            boolean sel = currentTab == tab;

            float a = (sel ? 0.78f : 0.55f) + (e.hovering ? 0.22f : 0f);
            tabShape(TAB_W, TAB_H, TAB_CUT, a);
            Colors.bindToGL(Colors.white());

            float th = font().getTextWidth("普", FO_TAB);
            font().draw(text, TAB_W / 2, (TAB_H - th) / 2, sel ? FO_TAB_SELECTED : FO_TAB);
        });
        w.listen(LeftClickEvent.class, () -> {
            currentTab = tab;
            applyTab(pages);
        });
        return w;
    }

    private static void applyTab(Widget[] pages) {
        for (int i = 0; i < pages.length; i++) {
            pages[i].transform.doesDraw = (currentTab.ordinal() == i);
        }
    }

    private static double parallaxUV(double x, double backScaleInv) {
        return (x - 0.5) * backScaleInv + 0.5;
    }

    private static final double CASCADE_BASE = 0.1;

    private static final double NODE_APPEAR = 0.1;

    private static final double LINE_GROW = 0.2;

    private static int treeDepth(Skill s) {
        return treeDepth(s, new java.util.HashSet<>());
    }

    private static int treeDepth(Skill s, java.util.Set<Skill> visited) {
        if (s == null || !visited.add(s)) {
            return 0;
        }
        int best = 0;
        for (Skill p : s.getTreeParents()) {
            best = Math.max(best, 1 + treeDepth(p, visited));
        }
        return best;
    }

    private static final double AURA_PERIOD = 3.2;

    private static final float AURA_MIN = 100f, AURA_MAX = 140f;

    private static final float AURA_ALPHA_MIN = 0.30f, AURA_ALPHA_MAX = 0.62f;

    private static Widget skillWidget(CGui gui, AbilityData aData, Skill skill,
                                      float[] d, double maxDuSkills,
                                      @Nullable IDeveloper developer, @Nullable BlockPos pos) {
        final int STATE_IDLE = 0, STATE_HOVER = 1;
        final double TRANSIT_TIME = 0.1;

        final float WIDGET_SIZE = 16.0f;
        final float PROG_SIZE = 31.0f;
        final float TOTAL_SIZE = 23.0f;
        final float ICON_SIZE = 14.0f;
        final float PROG_ALIGN = (TOTAL_SIZE - PROG_SIZE) / 2;
        final float ALIGN = (TOTAL_SIZE - ICON_SIZE) / 2;
        final float DRAW_ALIGN = (WIDGET_SIZE - TOTAL_SIZE) / 2;

        final boolean learned = aData.isSkillLearned(skill);
        final Widget widget = new Widget();
        final float sx = skill.guiX, sy = skill.guiY;

        final double[] lastTransit = { GameTimer.getTime() - 2 };
        final int[] state = { STATE_IDLE };
        final double creationTime = GameTimer.getTime();

        final double blendOffset = CASCADE_BASE + treeDepth(skill) * (NODE_APPEAR + LINE_GROW);

        boolean allParentsOk = true;
        for (Skill p : skill.getTreeParents()) {
            if (!aData.isSkillLearned(p)) {
                allParentsOk = false;
                break;
            }
        }
        final boolean parentOk = allParentsOk;
        final double mAlpha = learned ? 1.0 : (parentOk ? 0.7 : 0.25);

        final java.util.List<LineDrawer> lineDrawers =
                makeLineDrawers(skill, WIDGET_SIZE, mAlpha, learned);

        widget.pos(sx, sy).size(WIDGET_SIZE, WIDGET_SIZE);
        widget.listen(FrameEvent.class, (w, e) -> {
            double time = GameTimer.getTime();

            widget.pos((float) (sx - d[0] * maxDuSkills), (float) (sy - d[1] * maxDuSkills));
            widget.dirty = true;

            double transitProgress = MathUtils.clampd(0, 1, (time - lastTransit[0]) / TRANSIT_TIME);
            double scale = state[0] == STATE_IDLE
                    ? MathUtils.lerp(1.2, 1, MathUtils.clampd(0, 1, transitProgress))
                    : MathUtils.lerp(1, 1.2, MathUtils.clampd(0, 1, transitProgress));

            if (transitProgress == 1) {
                if (state[0] == STATE_IDLE && e.hovering) {
                    state[0] = STATE_HOVER;
                    lastTransit[0] = GameTimer.getTime();
                } else if (state[0] == STATE_HOVER && !e.hovering) {
                    state[0] = STATE_IDLE;
                    lastTransit[0] = GameTimer.getTime();
                }
            }

            double dt = Math.max(0, time - creationTime - blendOffset);
            double backAlpha = mAlpha * MathUtils.clampd(0, 1, dt * 10.0);
            double iconAlpha = mAlpha * MathUtils.clampd(0, 1, (dt - 0.08) * 10.0);
            float progressBlend = (float) MathUtils.clampd(0, 1, (dt - 0.12) * 2.0);
            double lineBlend = MathUtils.clampd(0, 1,
                    (time - creationTime - blendOffset + LINE_GROW) / LINE_GROW);

            DevRender.enableDepth();
            DevRender.beginNoCull();
            Matrix4f outer = DevRender.save();

            DevRender.translate(DRAW_ALIGN, DRAW_ALIGN, 10);

            DevRender.translate(TOTAL_SIZE / 2, TOTAL_SIZE / 2, 0);
            DevRender.scale(scale, scale);
            DevRender.translate(-TOTAL_SIZE / 2, -TOTAL_SIZE / 2, 0);

            if (skill.hasAura()) {
                double breath = 0.5 - 0.5 * Math.cos(time / AURA_PERIOD * Math.PI * 2);
                float size = AURA_MIN + (AURA_MAX - AURA_MIN) * (float) breath;
                double a = AURA_ALPHA_MIN + (AURA_ALPHA_MAX - AURA_ALPHA_MIN) * breath;
                DevRender.disableDepth();
                DevRender.color(1, 1, 1, backAlpha * a);
                DevRender.rect(TEX_SKILL_AURA, (TOTAL_SIZE - size) / 2, (TOTAL_SIZE - size) / 2,
                        size, size);
                DevRender.enableDepth();
            }

            DevRender.color(1, 1, 1, backAlpha);
            DevRender.depthMask(false);
            DevRender.rect(TEX_SKILL_BACK, 0, 0, TOTAL_SIZE, TOTAL_SIZE);

            DevRender.color(0.2, 0.2, 0.2, backAlpha * 0.6);
            DevRender.rect(TEX_SKILL_OUTLINE, PROG_ALIGN, PROG_ALIGN, PROG_SIZE, PROG_SIZE);
            DevRender.color(1, 1, 1, 1);

            DevRender.depthMask(true);
            DevRender.colorMask(false);
            DevRender.rectCutout(TEX_SKILL_BACK, 0.3f, 0, 0, TOTAL_SIZE, TOTAL_SIZE);

            Matrix4f beforeOutline = DevRender.save();
            DevRender.translate(0, 0, 1);
            DevRender.rectCutout(TEX_SKILL_OUTLINE, 0.5f, PROG_ALIGN, PROG_ALIGN, PROG_SIZE, PROG_SIZE);
            DevRender.restore(beforeOutline);

            DevRender.colorMask(true);
            DevRender.depthMask(false);

            DevRender.color(1, 1, 1, iconAlpha);
            DevRender.depthFunc(GL11.GL_EQUAL);
            if (learned) {
                DevRender.rect(skill.getHintIcon(), ALIGN, ALIGN, ICON_SIZE, ICON_SIZE);
            } else {
                DevRender.rectMono(skill.getHintIcon(), ALIGN, ALIGN, ICON_SIZE, ICON_SIZE);
            }
            DevRender.depthFunc(GL11.GL_LEQUAL);

            DevRender.color(1, 1, 1, 1);
            if (learned) {
                DevRender.disableDepth();
                DevRender.rectProgBar(TEX_SKILL_OUTLINE, TEX_SKILL_MASK,
                        progressBlend * aData.getSkillExp(skill),
                        PROG_ALIGN, PROG_ALIGN, PROG_SIZE, PROG_SIZE);
                DevRender.enableDepth();
            }

            DevRender.restore(outer);

            DevRender.depthFunc(GL11.GL_NOTEQUAL);
            Matrix4f beforeLine = DevRender.save();
            DevRender.translate(0, 0, 11);
            for (LineDrawer ld : lineDrawers) {
                ld.draw(lineBlend);
            }
            DevRender.restore(beforeLine);

            DevRender.resetState();
        });

        widget.listen(LeftClickEvent.class, () ->
                gui.addWidget(skillViewArea(gui, aData, skill, developer, pos)));

        return widget;
    }

    private interface LineDrawer {
        void draw(double progress);
    }

    private static java.util.List<LineDrawer> makeLineDrawers(Skill skill, float widgetSize,
                                                              double mAlpha, boolean learned) {
        java.util.List<Skill> parents = skill.getTreeParents();
        if (parents.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        java.util.List<LineDrawer> out = new java.util.ArrayList<>(parents.size());
        for (Skill p : parents) {
            LineDrawer ld = makeLineDrawer(skill, p, widgetSize, mAlpha, learned);
            if (ld != null) {
                out.add(ld);
            }
        }
        return out;
    }

    @Nullable
    private static LineDrawer makeLineDrawer(Skill skill, Skill parent, float widgetSize,
                                             double mAlpha, boolean learned) {
        if (parent == null) return null;

        double cx = skill.guiX + widgetSize / 2, cy = skill.guiY + widgetSize / 2;
        double pcx = parent.guiX + widgetSize / 2, pcy = parent.guiY + widgetSize / 2;
        double px = pcx - cx, py = pcy - cy;
        double norm = Math.sqrt(px * px + py * py);
        if (norm == 0) return null;
        double dx = px / norm * 12.2, dy = py / norm * 12.2;

        return drawLine(px + widgetSize / 2 - dx, py + widgetSize / 2 - dy,
                widgetSize / 2 + dx, widgetSize / 2 + dy,
                5.5, mAlpha * (learned ? 1.0 : 0.4));
    }

    private static LineDrawer drawLine(double x0, double y0, double x1, double y1,
                                       double width, double alpha) {
        double dx = x1 - x0, dy = y1 - y0;
        double norm = Math.sqrt(dx * dx + dy * dy);
        double nx = -dy / norm / 2 * width, ny = dx / norm / 2 * width;

        return progress -> {
            double xx = MathUtils.lerp(x0, x1, progress);
            double yy = MathUtils.lerp(y0, y1, progress);
            DevRender.color(1, 1, 1, alpha);
            DevRender.texQuad(TEX_LINE,
                    x0 - nx, y0 - ny,
                    x0 + nx, y0 + ny,
                    xx + nx, yy + ny,
                    xx - nx, yy - ny);
            DevRender.color(1, 1, 1, 1);
        };
    }

    private static Widget blackCover(CGui gui) {
        Widget ret = new Widget();
        ret.addComponent(new Cover());
        ret.size(gui.getWidth(), gui.getHeight());
        return ret;
    }

    private static final float BAR_POWER_Y = 55.5f, BAR_SYNC_Y = 77.5f;

    private static final float NODE_NAME_DY = 2f;

    private static final float BAR_GAP = 10f;

    private static Widget newButton() {
        return new Widget().size(64, 32).scale(.5f)
                .addComponents(new DrawTexture(TEX_BUTTON),
                        new Tint(Colors.monoBlend(1, .6f), Colors.monoBlend(1, 1), true));
    }

    private static void drawActionIcon(ResourceLocation icon, double progress, boolean glow) {
        final int BACK_SIZE = 50;
        final int ICON_SIZE = 27;
        final int ICON_ALIGN = (BACK_SIZE - ICON_SIZE) / 2;

        Matrix4f saved = DevRender.save();
        DevRender.translate(0, 0, 11);
        DevRender.color(1, 1, 1, 1);

        DevRender.rect(TEX_SKILL_BACK, 0, 0, BACK_SIZE, BACK_SIZE);
        DevRender.rect(icon, ICON_ALIGN, ICON_ALIGN, ICON_SIZE, ICON_SIZE);
        DevRender.rectProgBar(glow ? TEX_VIEW_OUTLINE_GLOW : TEX_VIEW_OUTLINE, TEX_SKILL_MASK,
                (float) progress, 0, 0, BACK_SIZE, BACK_SIZE);

        DevRender.restore(saved);
    }

    private static Widget levelUpArea(CGui gui, AbilityData data, IDeveloper developer, @Nullable BlockPos pos) {
        Widget ret = blackCover(gui);

        Widget wid = new Widget().centered().size(50, 50);

        DevelopActionLevel action = new DevelopActionLevel();
        double estmCons = LearningHelper.getEstimatedConsumption(player(), developer.getDeveloperType(), action);

        Widget textArea = new Widget().size(0, 10).centered().pos(0, 25);

        final String[] hint = { local("level_question") };
        final double[] progress = { 0 };
        final boolean[] canClose = { true };
        final boolean[] shouldRebuild = { false };
        final double[] doneTime = { -1 };

        ResourceLocation icon = Resources.getTexture("abilities/condition/any" + (data.getLevel() + 1));

        wid.listen(FrameEvent.class, () -> drawActionIcon(icon, progress[0], progress[0] == 1));

        String lvltext = local("uplevel", AbilityLocalization.instance.levelDesc(data.getLevel() + 1));
        String reqtext = local("req") + String.format(" %.0f", estmCons);
        textArea.listen(FrameEvent.class, () -> {
            font().draw(lvltext, 0, 3, FO_LEVEL_TITLE);
            font().draw(reqtext, 0, 16, FO_LEVEL_REQ);
            font().draw(hint[0], 0, 26, FO_LEVEL_REQ);
        });

        Widget button = newButton().centered().pos(0, 40);
        button.listen(LeftClickEvent.class, () -> {
            if (developer.getEnergy() < estmCons) {
                hint[0] = local("noenergy");
            } else if (pos != null || isPortable(developer)) {

                DevelopData devData = DevelopData.get(player());
                devData.reset();
                canClose[0] = false;

                DeveloperActionMessage.sendStartLevel(pos, isPortable(developer));
                ret.listen(FrameEvent.class, () -> {
                    switch (devData.getState()) {
                        case IDLE -> { }
                        case DEVELOPING -> {
                            hint[0] = local("dev_developing");
                            progress[0] = devData.getDevelopProgress();
                        }
                        case DONE -> {
                            hint[0] = local("dev_successful");
                            progress[0] = 1;
                            canClose[0] = true;
                            shouldRebuild[0] = true;

                            if (doneTime[0] < 0) {
                                doneTime[0] = cn.lambdalib2.util.GameTimer.getAbsTime();
                            } else if (cn.lambdalib2.util.GameTimer.getAbsTime() - doneTime[0]
                                    >= DEV_DONE_BACK_DELAY) {
                                gui.postEvent(new RebuildEvent());
                            }
                        }
                        case FAILED -> {
                            hint[0] = local("dev_failed");
                            canClose[0] = true;
                        }
                    }
                });
            }
            button.dispose();
        });

        textArea.addWidget(button);
        ret.addWidget(textArea);
        ret.listen(LeftClickEvent.class, () -> {
            if (canClose[0]) {
                if (shouldRebuild[0]) {
                    gui.postEvent(new RebuildEvent());
                } else {
                    ret.getComponent(Cover.class).end();
                }
            }
        });
        ret.addWidget(wid);

        return ret;
    }

    private static final double DEV_DONE_BACK_DELAY = 0.8;

    private static Widget skillViewArea(CGui gui, AbilityData data, Skill skill,
                                        @Nullable IDeveloper developer, @Nullable BlockPos pos) {
        Widget ret = blackCover(gui);

        Widget skillWid = new Widget().centered().size(50, 50);
        boolean learned = data.isSkillLearned(skill);
        final boolean[] canClose = { true };
        final boolean[] shouldRebuild = { false };
        final double[] doneTime = { -1 };

        Widget textArea = new Widget().size(0, 10).centered().pos(0, 25);

        if (learned) {
            skillWid.listen(FrameEvent.class, () -> drawActionIcon(skill.getHintIcon(), 0, false));
            textArea.listen(FrameEvent.class, () -> {
                fontBold().draw(skill.getDisplayName(), 0, 3, FO_SKILL_TITLE);
                font().draw(local("skill_exp") + (int) (data.getSkillExp(skill) * 100) + "%", 0, 15, FO_SKILL_PROG);
                font().drawSeperated(skill.getDescription(), 0, 24, 200, FO_SKILL_DESC);
            });
        } else {
            final double[] progress = { 0 };
            final String[] message = { null };

            skillWid.listen(FrameEvent.class, () -> drawActionIcon(skill.getHintIcon(), progress[0], progress[0] == 1));

            String skillNameText = skill.getDisplayName() + " (LV " + skill.getLevel() + ")";
            textArea.listen(FrameEvent.class, () -> {
                fontBold().draw(skillNameText, 0, 3, FO_SKILL_TITLE);
                font().draw(local("skill_not_learned"), 0, 15, FO_SKILL_UNLEARNED);
            });

            if (developer != null) {
                DevelopActionSkill action = new DevelopActionSkill(skill);
                double estmCons = LearningHelper.getEstimatedConsumption(player(), developer.getDeveloperType(), action);

                List<IDevCondition> conditions = new ArrayList<>();
                for (IDevCondition c : skill.getDevConditions()) {
                    if (c.shouldDisplay()) conditions.add(c);
                }
                final int CondIconSize = 14;
                final int CondIconStep = 16;
                final int len = CondIconStep * conditions.size();

                textArea.listen(FrameEvent.class, () -> font().draw(local("req"), -len / 2f - 2, 28, FO_SKILL_REQ));

                for (int i = 0; i < conditions.size(); i++) {
                    IDevCondition cond = conditions.get(i);
                    boolean accepted = cond.accepts(data, developer, skill);
                    Widget w = new Widget().size(CondIconSize, CondIconSize)
                            .pos(-len / 2f + CondIconStep * i, 25);
                    ResourceLocation ci = cond.getIcon();
                    if (ci != null) {

                        w.listen(FrameEvent.class, () -> {
                            DevRender.color(1, 1, 1, 1);
                            if (accepted) {
                                DevRender.rect(ci, 0, 0, CondIconSize, CondIconSize);
                            } else {
                                DevRender.rectMono(ci, 0, 0, CondIconSize, CondIconSize);
                            }
                        });
                    }
                    w.addComponent(new CondTag(cond, accepted));
                    textArea.addWidget(w);
                }

                textArea.listen(FrameEvent.class, () -> {
                    Widget hovering = gui.getHoveringWidget();
                    if (hovering == null) return;
                    CondTag tag = hovering.getComponent(CondTag.class);
                    if (tag == null) return;
                    font().draw("(" + tag.cond.getHintText() + ")", len / 2f + 3, 27,
                            tag.accepted ? FO_SKILL_REQ_DETAIL : FO_SKILL_REQ_DETAIL2);
                });

                textArea.listen(FrameEvent.class, () -> {
                    if (message[0] != null) {
                        font().draw(message[0], 0, 40, FO_SKILL_UNLEARNED2);
                    } else {
                        font().draw(local("learn_question", String.format("%.0f", estmCons)), 0, 40, FO_SKILL_UNLEARNED2);
                    }
                });

                Widget button = newButton().centered().pos(0, 55);
                button.listen(LeftClickEvent.class, () -> {
                    if (developer.getEnergy() < estmCons) {
                        message[0] = local("noenergy");
                    } else if (skill.getLevel() > data.getLevel()) {
                        message[0] = local("level_fail", skill.getLevel());
                    } else if (!action.validate(player(), developer)) {
                        message[0] = local("condition_fail");
                    } else if (pos != null || isPortable(developer)) {

                        DevelopData devData = DevelopData.get(player());
                        devData.reset();

                        DeveloperActionMessage.sendStartSkill(pos, isPortable(developer), skill);
                        canClose[0] = false;
                        ret.listen(FrameEvent.class, () -> {
                            switch (devData.getState()) {
                                case IDLE -> { }
                                case DEVELOPING -> {
                                    message[0] = local("progress")
                                            + String.format(" %.0f%%", devData.getDevelopProgress() * 100);
                                    progress[0] = devData.getDevelopProgress();
                                }
                                case DONE -> {
                                    message[0] = local("dev_successful");
                                    shouldRebuild[0] = true;
                                    progress[0] = 1.0;
                                    canClose[0] = true;

                                    if (doneTime[0] < 0) {
                                        doneTime[0] = cn.lambdalib2.util.GameTimer.getAbsTime();
                                    } else if (cn.lambdalib2.util.GameTimer.getAbsTime() - doneTime[0]
                                            >= DEV_DONE_BACK_DELAY) {
                                        gui.postEvent(new RebuildEvent());
                                    }
                                }
                                case FAILED -> {
                                    canClose[0] = true;
                                    message[0] = local("dev_failed");
                                }
                            }
                        });
                    }
                    button.dispose();
                });

                textArea.addWidget(button);
            }
        }

        ret.addWidget(textArea);
        ret.addWidget(skillWid);

        ret.listen(LeftClickEvent.class, () -> {
            if (canClose[0]) {
                if (shouldRebuild[0]) {
                    gui.postEvent(new RebuildEvent());
                } else {
                    ret.getComponent(Cover.class).end();
                }
            }
        });

        return ret;
    }

    private static class CondTag extends Component {
        final IDevCondition cond;
        final boolean accepted;

        CondTag(IDevCondition cond, boolean accepted) {
            super("CondTag");
            this.cond = cond;
            this.accepted = accepted;
        }
    }

    private static TextBox textBox(String content, float size, FontAlign align, Color color, HeightAlign halign) {
        TextBox box = new TextBox(new FontOption(size, align, color));
        box.heightAlign = halign;
        box.setContent(content);
        return box;
    }

    private static ProgressBar progressBar(double progress, Color color) {
        ProgressBar bar = new ProgressBar();
        bar.dir = ProgressBar.Direction.RIGHT;
        bar.progress = progress;
        bar.color = color;
        bar.texture = null;
        return bar;
    }
}
