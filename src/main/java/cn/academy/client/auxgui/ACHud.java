package cn.academy.client.auxgui;

import cn.academy.AcademyCraft;
import cn.academy.config.Configuration;
import cn.academy.config.Property;
import cn.lambdalib2.auxgui.AuxGui;
import cn.lambdalib2.cgui.CGui;
import cn.lambdalib2.cgui.Widget;
import com.google.common.collect.ImmutableList;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class ACHud extends AuxGui {

    public static ACHud instance = new ACHud();

    public static void init() {
        AuxGui.register(instance);
    }

    private final List<Node> nodes = new ArrayList<>();

    private final CGui gui = new CGui();

    private ACHud() {
        foreground = false;
    }

    @Override
    public void draw(GuiGraphics gg, float width, float height) {
        gui.resize(width, height);

        for (Node n : nodes) {
            n.w.transform.doesDraw = n.cond.shows();
        }

        gui.draw(gg.pose());
    }

    public void addElement(Widget w, Condition showCondition, String name, Widget preview) {
        Node node = new Node(w, showCondition, name, preview);
        nodes.add(node);
        node.updatePosition();

        gui.addWidget(w);
    }

    public List<Node> getNodes() {
        return ImmutableList.copyOf(nodes);
    }

    public interface Condition {
        boolean shows();
    }

    public class Node {
        final Widget w;
        final Condition cond;
        final String name;

        final float defaultX, defaultY;
        final Widget preview;

        public Node(Widget _w, Condition _cond, String _name, Widget _preview) {
            w = _w;
            cond = _cond;
            name = _name;
            defaultX = w.transform.x;
            defaultY = w.transform.y;
            preview = _preview;
        }

        public double[] getPosition() {
            double[] pos = prop().getDoubleList();
            return pos.length >= 2 ? pos : new double[]{defaultX, defaultY};
        }

        public Widget getPreview() {
            return preview;
        }

        public String getName() {
            return name;
        }

        void updatePosition() {
            double[] pos = getPosition();
            w.pos((float) pos[0], (float) pos[1]);
            w.dirty = true;
        }

        public void setPosition(float newX, float newY) {
            Property prop = prop();
            prop.set(new double[]{newX, newY});
            updatePosition();
        }

        private Property prop() {
            return conf().get("gui", name, new double[]{defaultX, defaultY});
        }

        private Configuration conf() {
            return AcademyCraft.config;
        }
    }

}
