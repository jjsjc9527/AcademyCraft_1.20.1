package cn.academy.client.auxgui;

import cn.academy.Resources;
import cn.academy.ability.Category;
import cn.academy.ability.Skill;
import cn.academy.datapart.AbilityData;
import cn.academy.datapart.CPData;
import cn.academy.util.ACKeyManager;
import cn.lambdalib2.auxgui.AuxGui;
import cn.lambdalib2.input.KeyHandler;
import cn.lambdalib2.render.font.IFont;
import cn.lambdalib2.render.font.IFont.FontOption;
import cn.lambdalib2.util.Color;
import cn.lambdalib2.util.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class DebugConsole extends AuxGui {

    private static class Text {
        final String text;
        final FontOption option;

        public Text(String _text, float _size, int _color) {
            text = _text;
            option = new FontOption(_size, Colors.fromRGBA32(_color));
        }

        public Text(String _text, float _size) {
            this(_text, _size, 0xffffffff);
        }

        public Text(String _text) {
            this(_text, 10);
        }
    }

    static DebugConsole INSTANCE;

    public static void init() {
        AuxGui.register(INSTANCE = new DebugConsole());

        ACKeyManager.instance.addKeyHandler("debug_console", "", GLFW.GLFW_KEY_F4, new KeyHandler() {
            @Override
            public void onKeyDown() {
                State[] states = State.values();
                INSTANCE.state = states[(INSTANCE.state.ordinal() + 1) % states.length];
            }
        });
    }

    enum State { NONE, NORMAL, SHOW_EXP }

    State state = State.NONE;

    private DebugConsole() {
        foreground = false;
    }

    @Override
    public void draw(GuiGraphics gg, float width, float height) {
        if (state == State.NONE)
            return;

        Player player = Minecraft.getInstance().player;
        if (player == null)
            return;

        if (!cn.lambdalib2.datapart.EntityData.isReady(player))
            return;

        List<Text> texts = new ArrayList<>();
        texts.add(new Text("AcademyCraft developer info"));

        AbilityData aData = AbilityData.get(player);
        CPData cpData = CPData.get(player);

        switch (state) {
            case NORMAL:
                if (!aData.hasCategory()) {
                    texts.add(new Text("Ability not acquired"));
                } else {
                    texts.add(new Text(aData.getCategory().getName()));

                    texts.add(new Text("Level " + aData.getLevel()));
                    texts.add(new Text(String.format("CP:       %.0f/%.0f(%.1f+%.1f)",
                            cpData.getCP(), cpData.getMaxCP(), cpData.getRawMaxCP(), cpData.getAddMaxCP())));
                    texts.add(new Text(String.format("Overload: %.0f/%.0f(%.1f+%.1f)",
                            cpData.getOverload(), cpData.getMaxOverload(), cpData.getRawMaxOverload(), cpData.getAddMaxOverload())));
                    texts.add(new Text("CPData.canUseAbility: " + cpData.canUseAbility()));
                    texts.add(new Text("CPData.activated: " + cpData.isActivated()));
                    texts.add(new Text("CPData.addMaxCP: " + cpData.getAddMaxCP()));
                    texts.add(new Text("CPData.interfering: " + cpData.isInterfering()));
                    texts.add(new Text(String.format(" AData.levelProgress: %.2f%%", aData.getLevelProgress() * 100)));
                }
                break;
            case SHOW_EXP:
                texts.add(new Text("Skill status"));
                if (aData.hasCategory()) {
                    Category cat = aData.getCategory();
                    for (Skill s : cat.getSkillList()) {
                        StringBuilder sb = new StringBuilder(s.getName());
                        for (int i = 0; i < 30 - s.getName().length(); ++i)
                            sb.append(' ');
                        if (aData.isSkillLearned(s)) {
                            sb.append(String.format("%.1f", aData.getSkillExp(s) * 100)).append('%');
                        } else {
                            sb.append("[not learned]");
                        }

                        texts.add(new Text(sb.toString()));
                    }
                }
                break;
            default:
                break;
        }

        iter(texts, 10.5f, 10.5f, 0.2);
        iter(texts, 10, 10, 1);
    }

    private void iter(List<Text> texts, float x, float y, double lumMul) {
        IFont font = Resources.font();
        for (Text text : texts) {

            Color crl = text.option.color;
            Color prev = new Color(crl);

            crl.r = (int) (crl.r * lumMul);
            crl.g = (int) (crl.g * lumMul);
            crl.b = (int) (crl.b * lumMul);

            font.draw(text.text, x, y, text.option);

            crl.r = prev.r;
            crl.g = prev.g;
            crl.b = prev.b;
            crl.a = prev.a;

            y += text.option.fontSize;
        }
    }
}
