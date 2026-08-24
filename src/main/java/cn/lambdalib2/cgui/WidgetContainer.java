package cn.lambdalib2.cgui;

import java.util.*;

import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WidgetContainer implements Iterable<Widget> {

    HashBiMap<String, Widget> widgets = HashBiMap.create();
    LinkedList<Widget> widgetList = new LinkedList<>();

    private static final String UNNAMED_PRE = "Unnamed ";

    public void addAll(WidgetContainer container) {
        for (Widget w : container.getDrawList()) {
            addWidget(w.getName(), w.copy());
        }
    }

    protected void update() {
        Iterator<Widget> iter = widgetList.iterator();
        while (iter.hasNext()) {
            Widget w = iter.next();
            if (w.disposed) {
                iter.remove();
                widgets.inverse().remove(w);
            }
        }
    }

    public void renameWidget(String name, String newName) {
        Widget w = widgets.remove(name);
        if (w == null)
            throw new NullPointerException();
        widgets.put(newName, w);
    }

    public Set<Map.Entry<String, Widget>> getEntries() {
        return widgets.entrySet();
    }

    public boolean addWidget(Widget add) {
        return addWidget(getNextName(), add);
    }

    public boolean addWidget(String name, Widget add) {
        return addWidget(name, add, false);
    }

    public boolean addWidget(Widget add, boolean begin) {
        return addWidget(getNextName(), add, begin);
    }

    public boolean addWidget(String name, Widget add, boolean begin) {
        if (!checkInit(name, add))
            return false;

        if (begin)
            widgetList.addFirst(add);
        else
            widgetList.add(add);

        checkAdded(name, add);
        return true;
    }

    public boolean addWidgetAfter(Widget add, Widget pivot) {
        return addWidgetAfter(getNextName(), add, pivot);
    }

    public boolean addWidgetAfter(String name, Widget add, Widget pivot) {
        int index = widgetList.indexOf(pivot);
        if (index == -1)
            return false;
        if (!checkInit(name, add))
            return false;

        widgetList.add(index + 1, add);
        checkAdded(name, add);
        return true;
    }

    public boolean addWidgetBefore(Widget add, Widget pivot) {
        return addWidgetBefore(getNextName(), add, pivot);
    }

    public boolean addWidgetBefore(String name, Widget add, Widget pivot) {
        int index = widgetList.indexOf(pivot);
        if (index == -1)
            index = 0;
        if (!checkInit(name, add))
            return false;

        widgetList.add(index, add);
        checkAdded(name, add);
        return true;
    }

    private boolean checkInit(String name, Widget add) {
        if (widgets.containsKey(name)) {
            Widget w = widgets.get(name);
            if (!w.disposed) {
                return false;
            }
            widgets.remove(name);
        }

        if (widgets.containsValue(add)) {
            widgets.inverse().remove(add);
        }

        add.disposed = false;
        add.dirty = true;
        widgets.put(name, add);
        return true;
    }

    private void checkAdded(String name, Widget add) {
        onWidgetAdded(name, add);
        add.abstractParent = this;
        add.onAdded();
    }

    public void clear() {
        widgets.clear();
        widgetList.clear();
    }

    public Widget getWidget(int i) {
        return widgetList.get(i);
    }

    public int locate(Widget w) {
        return widgetList.indexOf(w);
    }

    protected void onWidgetAdded(String name, Widget w) {}

    public Widget getWidget(String name) {
        int ind = name.indexOf('/');
        if (ind == -1) {
            return widgets.get(name);
        } else if (ind != name.length() - 1) {
            String cp = name.substring(0, ind);
            String ep = name.substring(ind + 1);
            Widget w = widgets.get(cp);
            return w == null ? null : w.getWidget(ep);
        } else {
            return null;
        }
    }

    public boolean hasWidget(String name) {
        Widget w = getWidget(name);
        return w != null && !w.disposed;
    }

    public void removeWidget(String name) {
        Widget w = widgets.get(name);
        if (w != null) {
            removeWidget(w);
        }
    }

    public void removeWidget(Widget w) {
        w.dispose();
        w.parent = null;
    }

    public void forceRemoveWidget(Widget w) {
        if (w.getAbstractParent() != this)
            return;
        widgets.remove(w.getName());
        widgetList.remove(w);
        w.gui = null;
        w.parent = null;
    }

    public String getWidgetName(Widget w) {
        return widgets.inverse().get(w);
    }

    public void changeWidgetName(Widget w, String newName) {
        widgets.inverse().put(w, newName);
    }

    public List<Widget> getDrawList() {
        return ImmutableList.copyOf(widgetList);
    }

    public int widgetCount() {
        return widgetList.size();
    }

    public void reorder(Widget target, Widget pivot) {
        widgetList.remove(target);
        ListIterator<Widget> litr = widgetList.listIterator();
        if (pivot == null) {
            litr.add(target);
        } else {
            while (litr.hasNext()) {
                Widget w = litr.next();
                if (w == pivot) {
                    litr.add(target);
                    break;
                }
            }
        }
    }

    public void reorder(Widget target, int newIndex) {
        int prevIndex = widgetList.indexOf(target);
        widgetList.remove(prevIndex);
        if (newIndex > prevIndex) {
            widgetList.add(newIndex - 1, target);
        } else {
            widgetList.add(newIndex, target);
        }
    }

    public Iterator<Widget> iterator() {
        return getDrawList().iterator();
    }

    public String getNextName() {
        String res;
        int nameCount = 0;
        do {
            res = UNNAMED_PRE + (nameCount++);
        } while (hasWidget(res));
        return res;
    }
}
