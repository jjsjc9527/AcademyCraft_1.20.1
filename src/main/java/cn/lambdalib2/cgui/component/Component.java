package cn.lambdalib2.cgui.component;

import java.util.ArrayList;
import java.util.List;

import cn.lambdalib2.cgui.Widget;
import cn.lambdalib2.cgui.event.GuiEvent;
import cn.lambdalib2.cgui.event.IGuiEventHandler;
import cn.lambdalib2.s11n.SerializeExcluded;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class Component {

    public final String name;

    public boolean enabled = true;

    public boolean canEdit = true;

    @SerializeExcluded
    public Widget widget;

    public Component(String _name) {
        name = _name;
    }

    public <T extends GuiEvent> void listen(Class<? extends T> type, IGuiEventHandler<T> handler) {
        listen(type, handler, 0);
    }

    public <T extends GuiEvent> void listen(Class<? extends T> type, IGuiEventHandler<T> handler, int prio) {
        if (widget != null)
            throw new RuntimeException("Can only add event handlers before component is added into widget");
        Node n = new Node();
        n.type = type;
        n.handler = new EHWrapper<>(handler);
        n.prio = prio;
        addedHandlers.add(n);
    }

    public void onAdded() {
        for (Node n : addedHandlers) {
            widget.listen(n.type, n.prio, false, n.handler);
        }
    }

    public void onRemoved() {
        for (Node n : addedHandlers) {
            widget.unlisten(n.type, n.handler);
        }
    }

    public Component copy() {
        throw new UnsupportedOperationException("cgui Component.copy is not ported yet (CopyHelper, #7)");
    }

    private List<Node> addedHandlers = new ArrayList<>();

    @OnlyIn(Dist.CLIENT)
    private final class EHWrapper<T extends GuiEvent> implements IGuiEventHandler<T> {

        final IGuiEventHandler<T> wrapped;

        public EHWrapper(IGuiEventHandler<T> _wrapped) {
            wrapped = _wrapped;
        }

        @Override
        public void handleEvent(Widget w, T event) {
            if (enabled)
                wrapped.handleEvent(w, event);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static class Node {
        Class<? extends GuiEvent> type;
        IGuiEventHandler handler;
        int prio;
    }
}
