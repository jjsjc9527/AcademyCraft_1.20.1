package cn.academy.client.auxgui;

import cn.academy.Resources;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.ClientRuntime.DelegateNode;
import cn.academy.ability.context.DelegateState;
import cn.academy.ability.context.KeyDelegate;
import cn.academy.client.gui.developer.DevRender;
import cn.academy.client.render.util.ACRenderingHelper;
import cn.academy.datapart.CPData;
import cn.academy.datapart.CooldownData;
import cn.academy.datapart.CooldownData.SkillCooldown;
import cn.lambdalib2.cgui.Widget;
import cn.lambdalib2.cgui.component.DrawTexture;
import cn.lambdalib2.cgui.component.Transform.HeightAlign;
import cn.lambdalib2.cgui.component.Transform.WidthAlign;
import cn.lambdalib2.cgui.event.FrameEvent;
import cn.lambdalib2.input.KeyManager;
import cn.lambdalib2.render.font.IFont;
import cn.lambdalib2.render.font.IFont.FontAlign;
import cn.lambdalib2.render.font.IFont.FontOption;
import cn.lambdalib2.util.Color;
import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.GameTimer;
import com.google.common.collect.Multimap;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class KeyHintUI extends Widget {

    static final float SCALE = 0.23f;

    public static void init() {

        Widget child = new Widget()
                .size(128, 193)
                .addComponent(new DrawTexture()
                        .setTex(Resources.getTexture("gui/edit_preview/key_hint")));
        Widget display = new Widget()
                .size(140, 210)
                .scale(SCALE * 2)
                .walign(WidthAlign.RIGHT)
                .halign(HeightAlign.CENTER);
        display.addWidget(child);

        ACHud.instance.addElement(new KeyHintUI(),
                () -> CPData.get(Minecraft.getInstance().player).isActivated(), "keyhint", display);
    }

    private final ResourceLocation
            TEX_BACK = tex("back"),
            TEX_ICON_BACK = tex("icon_back"),
            TEX_KEY_LONG = tex("key_long"),
            TEX_KEY_SHORT = tex("key_short"),
            TEX_MOUSE_L = tex("mouse_left"),
            TEX_MOUSE_R = tex("mouse_right"),

            TEX_MOUSE_CENTER = tex("mouse_center"),
            TEX_MOUSE_GENERIC = tex("mouse_generic");

    private double lastFrameTime, showTime;

    private double mAlpha;

    private float sinAlpha;
    private boolean canUseAbility;

    private final FontOption option = new FontOption(32, FontAlign.CENTER, Colors.fromHexColor(0xff194246));

    private KeyHintUI() {
        walign(WidthAlign.RIGHT);
        halign(HeightAlign.CENTER);
        size(140, 210);
        pos(0, 30);
        scale(SCALE);

        addDrawing();
    }

    private void addDrawing() {
        listen(FrameEvent.class, (w, e) -> {
            Player player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }

            CPData cpData = CPData.get(player);

            canUseAbility = cpData.canUseAbility();

            double time = GameTimer.getTime();

            if (time - lastFrameTime > 0.3) {
                showTime = time;
            }

            if ((time - showTime) < 0.3) {
                mAlpha = (time - showTime) / 0.3;
            } else {
                mAlpha = 1.0;
            }

            sinAlpha = 0.6f + (1 + Mth.sin(((float) time % 100) / 50.0f)) * 0.2f;

            if (cpData.isActivated()) {
                ClientRuntime rt = ClientRuntime.instance();
                CooldownData cd = CooldownData.of(rt.getEntity());

                Multimap<String, DelegateNode> map = rt.getDelegateRawData();
                List<String> groups = new ArrayList<>(map.keySet());

                groups.sort((s1, s2) -> {
                    if (s1.equals(ClientRuntime.DEFAULT_GROUP)) return -1;
                    else if (s2.equals(ClientRuntime.DEFAULT_GROUP)) return 1;
                    else return s1.compareTo(s2);
                });

                int availIdx = 0;
                for (String group : groups) {
                    Collection<DelegateNode> nodes = map.get(group);
                    if (!nodes.isEmpty()) {

                        final double x = -200 - availIdx * 200;
                        double y = 0;
                        for (DelegateNode node : nodes) {
                            Matrix4f saved = DevRender.save();
                            DevRender.translate(x, y, 0);
                            drawSingle(node.keyID, node.delegate,
                                    cd.getSub(node.delegate.getSkill(), node.delegate.getIdentifier()));
                            DevRender.restore(saved);
                            y += 92;
                        }
                        availIdx++;
                    }
                }
            }

            lastFrameTime = time;
            DevRender.color(1, 1, 1, 1);
        });
    }

    private void drawSingle(int keyCode, KeyDelegate c, SkillCooldown data) {
        ResourceLocation icon = c.getIcon();

        color4d(1, 1, 1, 1);
        DevRender.rect(TEX_BACK, 122, 0, 185, 83);

        IFont font = Resources.font();

        {
            float wx = 180, wy = 27;

            boolean mono = !canUseAbility || data.getTickLeft() > 0;
            if (mono) {
                color4d(0.7, 0.7, 0.7, 1);
            }

            if (keyCode >= 0) {
                String name = KeyManager.getKeyName(keyCode);

                drawBack(name.length() <= 2 ? TEX_KEY_SHORT : TEX_KEY_LONG, mono);
                font.draw(name, wx, wy, option);
            } else {
                if (keyCode == KeyManager.MOUSE_LEFT) {
                    drawBack(TEX_MOUSE_L, mono);
                } else if (keyCode == KeyManager.MOUSE_RIGHT) {
                    drawBack(TEX_MOUSE_R, mono);
                } else if (keyCode == KeyManager.MOUSE_MIDDLE) {

                    drawBack(TEX_MOUSE_CENTER, mono);
                } else {
                    drawBack(TEX_MOUSE_GENERIC, mono);

                    font.draw("" + (keyCode + 100), wx, wy, option);
                }
            }

            color4d(1, 1, 1, 1);
        }

        color4d(1, 1, 1, 1);
        DevRender.rect(TEX_ICON_BACK, 216, 5, 72, 72);

        DelegateState state = c.getState();
        float prog = (float) data.getTickLeft() / data.getMaxTick();

        float thisSinAlpha = (state.sinEffect ? sinAlpha : 1);

        float alpha;
        if (prog == 0.0f) {
            alpha = state.alpha * (0.4f + thisSinAlpha * 0.6f);
        } else {
            alpha = 0.4f;
        }

        final double ICON_SIZE = 62;
        color4d(1, 1, 1, alpha);
        DevRender.rect(icon, 221, 10, ICON_SIZE, ICON_SIZE);

        Color glow = new Color(state.glowColor);
        glow.setAlpha((int) (glow.getAlpha() * thisSinAlpha * mAlpha));
        ACRenderingHelper.drawGlow(221, 10, ICON_SIZE, ICON_SIZE, 5, glow);

        if (prog != 0) {
            color4d(0.6, 0.6, 0.6, .3);
            cn.lambdalib2.util.HudUtils.colorRect(221, 10 + ICON_SIZE * (1 - prog), ICON_SIZE, ICON_SIZE * prog);
        }

        DevRender.color(1, 1, 1, 1);
    }

    private void drawBack(ResourceLocation tex, boolean mono) {
        color4d(1, 1, 1, 1);
        if (mono) {
            DevRender.rectMono(tex, 146, 10, 70, 70);
        } else {
            DevRender.rect(tex, 146, 10, 70, 70);
        }
    }

    private void color4d(double r, double g, double b, double a) {
        DevRender.color(r, g, b, a * mAlpha);
    }

    private static ResourceLocation tex(String name) {
        return Resources.getTexture("gui/key_hint/" + name);
    }
}
