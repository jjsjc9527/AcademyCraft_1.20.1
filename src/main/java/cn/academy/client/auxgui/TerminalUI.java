package cn.academy.client.auxgui;

import cn.academy.ACSounds;
import cn.academy.Resources;
import cn.academy.terminal.App;
import cn.academy.terminal.AppEnvironment;
import cn.academy.terminal.TerminalData;
import cn.academy.event.AppInstalledEvent;
import cn.lambdalib2.auxgui.AuxGui;
import cn.lambdalib2.auxgui.AuxGuiHandler;
import cn.lambdalib2.cgui.CGui;
import cn.lambdalib2.cgui.Widget;
import cn.lambdalib2.cgui.component.Component;
import cn.lambdalib2.cgui.component.DrawTexture;
import cn.lambdalib2.cgui.component.TextBox;
import cn.lambdalib2.cgui.component.Transform.HeightAlign;
import cn.lambdalib2.cgui.component.Transform.WidthAlign;
import cn.lambdalib2.cgui.event.FrameEvent;
import cn.lambdalib2.input.KeyHandler;
import cn.academy.util.ACKeyManager;
import cn.lambdalib2.input.KeyManager;
import cn.lambdalib2.render.font.IFont.FontAlign;
import cn.lambdalib2.render.font.IFont.FontOption;
import cn.lambdalib2.util.Color;
import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.ControlOverrider;
import cn.lambdalib2.util.GameTimer;
import cn.lambdalib2.util.HudUtils;
import cn.lambdalib2.util.MathUtils;
import com.google.common.base.Preconditions;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class TerminalUI extends AuxGui {

    private static final String OVERRIDE_GROUP = "AC_Terminal";

    private static AuxGui current = null;

    private static final double BALANCE_SPEED = 3000;
    public static final int MAX_MX = 605, MAX_MY = 740;

    static final ResourceLocation
            APP_BACK = tex("app_back"),
            APP_BACK_HDR = tex("app_back_highlight"),
            CURSOR = tex("cursor"),
            BACK = tex("back"),
            LOGO = tex("logo"),
            ARROW_UP = tex("arrow_up"),
            ARROW_DOWN = tex("arrow_down");

    final double SENSITIVITY = 0.7;

    CGui gui;
    Widget root;

    LeftClickHandler clickHandler;

    float mouseX, mouseY;

    float buffX, buffY;

    double createTime;
    double lastFrameTime;

    int selection = 0;
    int scroll = 0;
    List<Widget> apps = new ArrayList<>();

    private boolean hijacking;
    private double lastCursorX, lastCursorY;
    private boolean cursorTracked;

    public TerminalUI() {
        gui = new CGui();
        gui.addWidget(root = createRoot());

        buffX = buffY = mouseX = mouseY = 150;

        consistent = false;

        MinecraftForge.EVENT_BUS.register(this);

        initGui();
    }

    @Override
    public void onEnable() {
        KeyManager.dynamic.addKeyHandler("terminal_click", KeyManager.MOUSE_LEFT, clickHandler = new LeftClickHandler());
        ControlOverrider.override(OVERRIDE_GROUP, KeyManager.MOUSE_LEFT);

        updateHijack();
    }

    @Override
    public void onDisposed() {
        endHijack();

        KeyManager.dynamic.removeKeyHandler("terminal_click");
        ControlOverrider.endOverride(OVERRIDE_GROUP);
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    private static long window() {
        return Minecraft.getInstance().getWindow().getWindow();
    }

    private void updateHijack() {
        Minecraft mc = Minecraft.getInstance();
        boolean want = !disposed && mc.screen == null && mc.player != null;
        if (want) {
            if (mc.mouseHandler.isMouseGrabbed()) {
                startHijack();
            }
        } else if (hijacking) {
            endHijack();
        }
    }

    private void startHijack() {
        Minecraft mc = Minecraft.getInstance();

        mc.mouseHandler.releaseMouse();

        InputConstants.grabOrReleaseMouse(window(), 212995 ,
                mc.getWindow().getScreenWidth() / 2.0, mc.getWindow().getScreenHeight() / 2.0);
        hijacking = true;
        cursorTracked = false;
    }

    private void endHijack() {
        if (!hijacking) return;
        hijacking = false;
        cursorTracked = false;

        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null) {

            mc.mouseHandler.grabMouse();
        } else {

            InputConstants.grabOrReleaseMouse(window(), 212993 ,
                    mc.getWindow().getScreenWidth() / 2.0, mc.getWindow().getScreenHeight() / 2.0);
        }
    }

    private double[] pollMouseDelta() {
        Minecraft mc = Minecraft.getInstance();
        double x = mc.mouseHandler.xpos();
        double y = mc.mouseHandler.ypos();
        if (!cursorTracked) {
            lastCursorX = x;
            lastCursorY = y;
            cursorTracked = true;
            return new double[]{0, 0};
        }
        double dx = x - lastCursorX;
        double dy = y - lastCursorY;
        lastCursorX = x;
        lastCursorY = y;
        return new double[]{dx, dy};
    }

    @Override
    public void draw(GuiGraphics gg, float width, float height) {
        Minecraft mc = Minecraft.getInstance();

        updateHijack();

        selection = (int) ((mouseY - 0.01) / MAX_MY * 3) * 3 + (int) ((mouseX - 0.01) / MAX_MX * 3);

        if (mouseY == 0) {
            mouseY = 1;
            if (scroll > 0) scroll--;
        }
        if (mouseY == MAX_MY) {
            mouseY -= 1;
            if (scroll < getMaxScroll()) scroll++;
        }

        double time = GameTimer.getTime();
        if (lastFrameTime == 0) lastFrameTime = time;
        double dt = time - lastFrameTime;

        double[] d = hijacking ? pollMouseDelta() : new double[]{0, 0};
        mouseX += d[0] * SENSITIVITY;
        mouseY += d[1] * SENSITIVITY;
        mouseX = Math.max(0, Math.min(MAX_MX, mouseX));
        mouseY = Math.max(0, Math.min(MAX_MY, mouseY));

        buffX = balance(dt, buffX, mouseX);
        buffY = balance(dt, buffY, mouseY);

        float aspect = (float) mc.getWindow().getWidth() / mc.getWindow().getHeight();

        Matrix4f oldProj = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorting oldSorting = RenderSystem.getVertexSorting();
        Matrix4f persp = new Matrix4f().perspective((float) Math.toRadians(50), aspect, 1f, 100f);
        RenderSystem.setProjectionMatrix(persp, VertexSorting.DISTANCE_TO_ORIGIN);

        PoseStack mv = RenderSystem.getModelViewStack();
        mv.pushPose();
        mv.setIdentity();

        final double scale = 1.0 / 310;
        mv.translate(.35 * aspect, 1.2, -4);

        mv.translate(1, -1.8, 0);
        mv.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-1.6f));
        mv.mulPose(com.mojang.math.Axis.YP.rotationDegrees(
                (float) (-18 - 4 * (buffX / MAX_MX - 0.5) + 1 * Math.sin(time / 1000.0))));
        mv.mulPose(com.mojang.math.Axis.XP.rotationDegrees(
                (float) (7 + 4 * (buffY / MAX_MY - 0.5))));
        mv.translate(-1, 1.8, 0);

        mv.scale((float) scale, (float) -scale, (float) scale);
        RenderSystem.applyModelViewMatrix();

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1, 1, 1, 1);

        RenderSystem.disableCull();

        gui.draw(new PoseStack(), mouseX, mouseY);

        {
            double csize = (getSelectedApp() == null ? 1 : 1.3) * (20 + Math.sin(time / 300.0) * 2);
            HudUtils.setMatrix(new Matrix4f().translate(0, 0, -2));
            HudUtils.loadTexture(CURSOR);
            RenderSystem.setShaderColor(1, 1, 1, .4f);
            RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            HudUtils.rect(-csize / 2 + buffX, -csize / 2 + buffY + 120, csize, csize);
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1, 1, 1, 1);
        }

        mv.popPose();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setProjectionMatrix(oldProj, oldSorting);
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
    }

    @SubscribeEvent
    public void _onAppInstalled(AppInstalledEvent evt) {
        if (Minecraft.getInstance().player != null) {
            updateAppList(TerminalData.get(Minecraft.getInstance().player));
        }
    }

    private float balance(double dt, float from, float to) {
        double d = to - from;
        return (float) (from + Math.min(BALANCE_SPEED * dt, Math.abs(d)) * Math.signum(d));
    }

    private static Widget textBox(float w, float h, float x, float y,
                                  WidthAlign wa, float fontSize, FontAlign align, Color color,
                                  HeightAlign heightAlign, float zLevel, String content) {
        Widget ret = new Widget();
        ret.transform.setSize(w, h).setPos(x, y);
        ret.transform.alignWidth = wa;
        TextBox tb = new TextBox(new FontOption(fontSize, align, color));
        tb.heightAlign = heightAlign;
        tb.zLevel = zLevel;
        tb.content = content;
        ret.addComponent(tb);
        return ret;
    }

    private Widget createRoot() {

        Widget back = new Widget();
        back.transform.setSize(640, 785).setPos(0, 0);
        back.addComponent(new DrawTexture(BACK));

        back.addWidget("text_appcount", textBox(300, 30, -40, 84, WidthAlign.RIGHT,
                30, FontAlign.RIGHT, new Color(255, 255, 255, 153), HeightAlign.CENTER, 5, ""));

        Widget icon = new Widget();
        icon.transform.setSize(50, 50).setPos(40, 50);
        icon.addComponent(new DrawTexture(LOGO));
        back.addWidget("icon", icon);

        Widget arrowUp = new Widget();
        arrowUp.transform.setSize(100, 25).setPos(0, 133);
        arrowUp.transform.scale = 0.8f;
        arrowUp.transform.alignWidth = WidthAlign.CENTER;
        arrowUp.addComponent(new DrawTexture(ARROW_UP));
        back.addWidget("arrow_up", arrowUp);

        back.addWidget("text_username", textBox(200, 30, -40, 41.666668f, WidthAlign.RIGHT,
                40, FontAlign.RIGHT, new Color(255, 255, 255, 204), HeightAlign.TOP, 15, "[username]"));

        back.addWidget("text_static_1", textBox(160, 30, 98, 74, WidthAlign.LEFT,
                40, FontAlign.LEFT, new Color(255, 255, 255, 170), HeightAlign.CENTER, 15, "TERMINAL"));

        back.addWidget("text_static_0", textBox(100, 30, 98, 42, WidthAlign.LEFT,
                40, FontAlign.LEFT, new Color(255, 255, 255, 170), HeightAlign.CENTER, 15, "DATA"));

        Widget arrowDown = new Widget();
        arrowDown.transform.setSize(100, 25).setPos(0, -40);
        arrowDown.transform.scale = 0.8f;
        arrowDown.transform.alignWidth = WidthAlign.CENTER;
        arrowDown.transform.alignHeight = HeightAlign.BOTTOM;
        arrowDown.addComponent(new DrawTexture(ARROW_DOWN));
        back.addWidget("arrow_down", arrowDown);

        return back;
    }

    private void initGui() {
        Player player = Minecraft.getInstance().player;

        final TerminalData data = TerminalData.get(player);

        createTime = GameTimer.getTime();

        {
            Widget widget = root.getWidget("text_appcount");
            TextBox textBox = widget.getComponent(TextBox.class);
            widget.listen(FrameEvent.class, (w, e) -> {
                int currentTime = (int) (player.level().getDayTime() % 24000);
                int hour = currentTime / 1000;
                int minutes = (currentTime % 1000) * 60 / 1000;

                String countText = net.minecraft.client.resources.language.I18n
                        .get("gui.academy.terminal.appcount", apps.size());
                String timeText = wrapTime(hour) + ":" + wrapTime(minutes);
                textBox.content = countText + ", " + timeText;
            });
        }

        root.getWidget("text_username").getComponent(TextBox.class)
                .setContent(player.getGameProfile().getName());

        updateAppList(data);

        createTime = GameTimer.getTime();

        root.getWidget("arrow_up").listen(FrameEvent.class,
                (w, e) -> w.getComponent(DrawTexture.class).enabled = scroll > 0);

        root.getWidget("arrow_down").listen(FrameEvent.class,
                (w, e) -> w.getComponent(DrawTexture.class).enabled = scroll < getMaxScroll());

    }

    private String wrapTime(int val) {
        return val < 10 ? ("0" + val) : (String.valueOf(val));
    }

    private void updateAppList(TerminalData data) {
        for (Widget w : apps)
            w.dispose();
        apps.clear();
        for (App app : data.getInstalledApps()) {
            Widget w = createAppWidget(apps.size(), app);
            root.addWidget(w);
            apps.add(w);
        }

        root.getWidget("text_appcount").getComponent(TextBox.class).content =
                net.minecraft.client.resources.language.I18n.get("gui.academy.terminal.appcount", apps.size());
        updatePosition();
    }

    private void updatePosition() {
        final float START_X = 65, START_Y = 155, STEP_X = 180, STEP_Y = 180;

        int max = getMaxScroll();
        if (scroll > max) scroll = max;

        for (Widget w : apps) {
            w.transform.doesDraw = false;
        }

        for (int i = scroll * 3; i < scroll * 3 + 9 && i < apps.size(); ++i) {
            int order = i - scroll * 3;
            Widget app = apps.get(i);
            app.transform.doesDraw = true;
            app.transform.x = START_X + STEP_X * (order % 3);
            app.transform.y = START_Y + STEP_Y * (order / 3);
            app.dirty = true;
        }
    }

    private int getMaxScroll() {
        int r;
        if (apps.size() % 3 == 0)
            r = apps.size() / 3;
        else r = apps.size() / 3 + 1;
        return Math.max(0, r - 3);
    }

    private Widget getSelectedApp() {
        int lookup = scroll + selection;
        return apps.size() <= lookup ? null : apps.get(lookup);
    }

    private double getLifetime() {
        return GameTimer.getTime() - createTime;
    }

    private Widget createAppWidget(int id, App app) {
        Widget ret = new Widget();
        ret.transform.setSize(151, 151).setPos(30, 155);
        ret.transform.doesDraw = true;
        ret.addComponent(new DrawTexture(APP_BACK));

        Widget icon = new Widget();
        icon.transform.setSize(110, 110).setPos(9, 32);
        DrawTexture iconTex = new DrawTexture(app.getIcon());
        iconTex.color = new Color(255, 255, 255, 160);
        icon.addComponent(iconTex);
        ret.addWidget("icon", icon);

        ret.addWidget("text", textBox(151, 21, 0, 148, WidthAlign.LEFT,
                32, FontAlign.CENTER, new Color(255, 255, 255, 255), HeightAlign.CENTER, 0,
                app.getDisplayName()));

        ret.addComponent(new AppHandler(id, app));

        return ret;
    }

    private static ResourceLocation tex(String name) {
        return Resources.getTexture("gui/data_terminal/" + name);
    }

    public static void passOn(AuxGui newGui) {
        Preconditions.checkNotNull(current);
        current.dispose();
        current = newGui;
        AuxGuiHandler.register(current);
    }

    public static final KeyHandler keyHandler = new KeyHandler() {

        @Override
        public void onKeyUp() {
            Player player = getPlayer();
            if (player == null) return;
            TerminalData tData = TerminalData.get(player);

            if (tData.isTerminalInstalled()) {
                if (current == null || current.disposed) {
                    current = new TerminalUI();
                    AuxGuiHandler.register(current);
                } else if (current instanceof TerminalUI) {
                    current.dispose();
                    current = null;
                }
            } else {
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("terminal.academy.notinstalled"), false);
            }
        }

    };

    public static void registerKeyHandler() {
        ACKeyManager.instance.addKeyHandler("open_data_terminal", "", GLFW.GLFW_KEY_LEFT_ALT, keyHandler);
    }

    private class AppHandler extends Component {

        final int id;
        final App app;

        DrawTexture drawer;
        TextBox text;
        DrawTexture icon;

        boolean lastSelected = true;

        public AppHandler(int _id, App _app) {
            super("AppHandler");
            id = _id;
            app = _app;

            listen(FrameEvent.class, (w, e) -> {
                float mAlpha = MathUtils.clampf(0.0f, 1.0f, (float) (getLifetime() - ((id + 1) * 0.1f)) / 0.40f);
                boolean selected = getSelectedApp() == w;

                if (selected) {
                    if (!lastSelected) {
                        playSelectSound();
                    }
                    drawer.texture = APP_BACK_HDR;

                    icon.zLevel = 40;
                    drawer.zLevel = text.zLevel = (float) icon.zLevel;

                    drawer.color.setAlpha(Colors.f2i(mAlpha));
                    icon.color.setAlpha(Colors.f2i(0.8f * mAlpha));
                    text.option.color.setAlpha(Colors.f2i(0.1f + 0.72f * mAlpha));
                } else {
                    drawer.texture = APP_BACK;

                    icon.zLevel = 10;
                    drawer.zLevel = text.zLevel = (float) icon.zLevel;

                    drawer.color.setAlpha(Colors.f2i(mAlpha));
                    icon.color.setAlpha(Colors.f2i(0.6f * mAlpha));
                    text.option.color.setAlpha(Colors.f2i(0.10f + 0.1f * mAlpha));
                }

                lastSelected = selected;
            });

        }

        @Override
        public void onAdded() {
            super.onAdded();

            drawer = DrawTexture.get(widget);
            text = TextBox.get(widget.getWidget("text"));
            icon = DrawTexture.get(widget.getWidget("icon"));
            icon.color.setAlpha(0);
            drawer.color.setAlpha(0);
            text.option.color.setAlpha(Colors.f2i(0.1f));
        }
    }

    private static void playSelectSound() {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(ACSounds.TERMINAL_SELECT.get(), 1.0f, 0.2f));
    }

    static AppHandler getHandler(Widget w) {
        return w.getComponent(AppHandler.class);
    }

    private class LeftClickHandler extends KeyHandler {

        @Override
        public void onKeyUp() {
            Widget app = getSelectedApp();
            if (app != null) {
                AppHandler handler = getHandler(app);
                AppEnvironment env = handler.app.createEnvironment();

                env.app = handler.app;
                env.terminal = TerminalUI.this;

                env.onStart();
            }
        }

    }

}
