package cn.academy.client.gui.config;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public final class ACConfigNode {

    public enum Kind {
        BOOLEAN,
        INT,
        DOUBLE,
        LIST_INT,
        LIST_DOUBLE,
        LIST_STRING;

        public boolean isList() {
            return this == LIST_INT || this == LIST_DOUBLE || this == LIST_STRING;
        }
    }

    public interface Entry {
        Kind kind();

        Object get();

        void set(Object value);

        Object defaultValue();

        boolean accepts(Object value);

        List<String> comment();

        default String rangeHint() {
            return null;
        }

        default boolean isKeyBinding() {
            return false;
        }
    }

    private final String name;
    private final String path;
    private final List<ACConfigNode> children = new ArrayList<>();
    private final Entry entry;

    private String display;
    private ResourceLocation icon;

    private Entry second;

    public static ACConfigNode category(String name, String path) {
        return new ACConfigNode(name, path, null);
    }

    public static ACConfigNode leaf(String name, String path, Entry entry) {
        return new ACConfigNode(name, path, entry);
    }

    public static ACConfigNode pair(String name, String path, Entry lo, Entry hi) {
        ACConfigNode n = new ACConfigNode(name, path, lo);
        n.second = hi;
        return n;
    }

    private ACConfigNode(String name, String path, Entry entry) {
        this.name = name;
        this.path = path;
        this.entry = entry;
    }

    public String name() {
        return name;
    }

    public String display() {
        return display != null ? display : name;
    }

    public ResourceLocation icon() {
        return icon;
    }

    public ACConfigNode display(String d, ResourceLocation i) {
        this.display = d;
        this.icon = i;
        return this;
    }

    public String path() {
        return path;
    }

    public boolean isCategory() {
        return entry == null;
    }

    public Entry entry() {
        return entry;
    }

    public Entry second() {
        return second;
    }

    public List<ACConfigNode> children() {
        return children;
    }

    public ACConfigNode add(ACConfigNode child) {
        children.add(child);
        return this;
    }

    public int countLeaves() {
        if (!isCategory()) {
            return second != null ? 2 : 1;
        }
        int n = 0;
        for (ACConfigNode c : children) {
            n += c.countLeaves();
        }
        return n;
    }
}
