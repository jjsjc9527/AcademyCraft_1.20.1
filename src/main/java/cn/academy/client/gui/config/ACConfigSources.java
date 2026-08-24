package cn.academy.client.gui.config;

import cn.academy.AcademyCraft;
import cn.academy.ability.Category;
import cn.academy.ability.CategoryManager;
import cn.academy.ability.Skill;
import cn.academy.client.gui.config.ACConfigNode.Entry;
import cn.academy.client.gui.config.ACConfigNode.Kind;
import cn.academy.config.AbilityConfig;
import cn.academy.config.ConfigCodec;
import cn.academy.config.InterfererConfig;
import cn.academy.config.Property;
import cn.academy.config.ServerConfigGate;
import cn.academy.terminal.app.settings.PropertyElements;
import cn.academy.terminal.app.settings.SettingsUI;
import cn.academy.terminal.app.settings.UIProperty;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public final class ACConfigSources {

    private ACConfigSources() {}

    public static final class Source {
        public final Component title;
        public final String fileName;
        public final ACConfigNode root;
        private final Runnable saver;

        Source(Component title, String fileName, ACConfigNode root, Runnable saver) {
            this.title = title;
            this.fileName = fileName;
            this.root = root;
            this.saver = saver;
        }

        public void save() {
            saver.run();
        }
    }

    public static List<Source> all() {
        List<Source> out = new ArrayList<>();
        out.add(fromSpec(Component.translatable("gui.academy.config.ability"),
                "academy-ability.toml", AbilityConfig.SPEC, true));
        out.add(fromSpec(Component.translatable("gui.academy.config.interferer"),
                "academy-ability-interferer.toml", InterfererConfig.SPEC, false));
        out.add(fromSettings());

        Source addons = fromAddons();
        if (addons != null) {
            out.add(addons);
        }
        return out;
    }

    private static Source fromAddons() {
        List<cn.academy.api.ACAddonConfig.Addon> addons = cn.academy.api.ACAddonConfig.registered();
        if (addons.isEmpty()) {
            return null;
        }
        ACConfigNode root = ACConfigNode.category("addons", "");
        List<ForgeConfigSpec> savers = new ArrayList<>();
        for (cn.academy.api.ACAddonConfig.Addon a : addons) {
            try {
                ACConfigNode raw = ACConfigNode.category(a.modId(), "");
                walk(a.spec().getSpec(), a.spec().getValues(), raw, "");
                pairUp(raw);

                ACConfigNode one = ACConfigNode.category(a.modId(), a.modId());
                List<ACConfigNode> top = raw.children();
                if (top.size() == 1 && top.get(0).name().equals(a.modId())) {
                    top = top.get(0).children();
                }
                for (ACConfigNode c : top) {
                    one.add(c);
                }
                if (one.children().isEmpty() || !loaded(one)) {
                    AcademyCraft.LOGGER.warn("config of addon {} is not loaded yet, hidden for now", a.modId());
                    continue;
                }

                one.display(a.displayName(), null);
                root.add(one);
                savers.add(a.spec());
            } catch (RuntimeException e) {
                AcademyCraft.LOGGER.error("failed to read the config of addon {}, skipped", a.modId(), e);
            }
        }
        if (root.children().isEmpty()) {
            return null;
        }
        return new Source(Component.translatable("gui.academy.config.addons"),
                "academy-addon-*.toml", root, () -> {

            for (ForgeConfigSpec s : savers) {
                try {
                    s.save();
                } catch (RuntimeException e) {
                    AcademyCraft.LOGGER.error("failed to save addon config", e);
                }
            }
        });
    }

    private static ACConfigNode regroupBySkill(ACConfigNode raw) {
        Map<String, ACConfigNode> byName = new HashMap<>();
        index(raw, byName);

        Set<ACConfigNode> claimed = new HashSet<>();
        ACConfigNode root = ACConfigNode.category("", "");

        for (Category cat : CategoryManager.INSTANCE.getCategories()) {
            ACConfigNode catNode = ACConfigNode.category(cat.getName(), cat.getName())
                    .display(cat.getDisplayName(), cat.getIcon());
            for (Skill s : cat.getSkillList()) {
                ACConfigNode n = byName.get(s.getName());
                if (n != null && claimed.add(n)) {
                    n.display(s.getDisplayName(), s.getHintIcon());
                    catNode.add(n);
                }
            }
            if (!catNode.children().isEmpty()) {
                root.add(catNode);
            }
        }

        ACConfigNode rest = ACConfigNode.category("other", "")
                .display(I18n.get("gui.academy.config.other"), null);
        collectUnclaimed(raw, claimed, rest);
        if (!rest.children().isEmpty()) {
            root.add(rest);
        }

        if (root.countLeaves() != raw.countLeaves()) {
            AcademyCraft.LOGGER.error("config screen rebuild lost entries ({} -> {}), players will not be able to change them",
                    raw.countLeaves(), root.countLeaves());
            return raw;
        }
        return root;
    }

    private static void index(ACConfigNode n, Map<String, ACConfigNode> out) {
        if (!n.isCategory() || n.children().isEmpty()) {
            return;
        }
        boolean allLeaves = true;
        for (ACConfigNode c : n.children()) {
            if (c.isCategory()) {
                allLeaves = false;
                break;
            }
        }
        if (allLeaves) {
            out.putIfAbsent(n.name(), n);
        } else {
            for (ACConfigNode c : n.children()) {
                index(c, out);
            }
        }
    }

    private static void collectUnclaimed(ACConfigNode node, Set<ACConfigNode> claimed, ACConfigNode out) {
        for (ACConfigNode c : node.children()) {
            if (claimed.contains(c)) {
                continue;
            }
            if (c.isCategory()) {
                ACConfigNode sub = ACConfigNode.category(c.name(), c.path()).display(c.display(), c.icon());
                collectUnclaimed(c, claimed, sub);
                if (!sub.children().isEmpty()) {
                    out.add(sub);
                }
            } else {
                out.add(c);
            }
        }
    }

    private static String statName(String key) {
        String k = "config.academy.stat." + key;
        String s = I18n.get(k);
        return s.equals(k) ? key : s;
    }

    private static String statName(String path, String key) {
        if (path != null && !path.isEmpty()) {
            String k = "config.academy.stat." + path;
            String s = I18n.get(k);
            if (!s.equals(k)) {
                return s;
            }
        }
        return statName(key);
    }

    private static final String SUF_LO = "_lv1";
    private static final String SUF_HI = "_lvmax";

    private static void pairUp(ACConfigNode node) {
        if (!node.isCategory()) {
            return;
        }
        for (ACConfigNode c : node.children()) {
            pairUp(c);
        }

        Map<String, ACConfigNode> hiByBase = new HashMap<>();
        for (ACConfigNode c : node.children()) {
            if (!c.isCategory() && c.name().endsWith(SUF_HI)) {
                hiByBase.put(c.name().substring(0, c.name().length() - SUF_HI.length()), c);
            }
        }
        if (hiByBase.isEmpty()) {
            return;
        }

        Set<ACConfigNode> consumed = new HashSet<>();
        for (ACConfigNode c : node.children()) {
            if (!c.isCategory() && c.name().endsWith(SUF_LO)) {
                ACConfigNode hi = hiByBase.get(c.name().substring(0, c.name().length() - SUF_LO.length()));
                if (hi != null) {
                    consumed.add(hi);
                }
            }
        }

        List<ACConfigNode> merged = new ArrayList<>();
        for (ACConfigNode c : node.children()) {
            if (consumed.contains(c)) {
                continue;
            }
            if (!c.isCategory() && c.name().endsWith(SUF_LO)) {
                String base = c.name().substring(0, c.name().length() - SUF_LO.length());
                ACConfigNode hi = hiByBase.get(base);
                if (hi != null) {
                    merged.add(ACConfigNode.pair(base, c.path(), c.entry(), hi.entry())
                            .display(statName(base), null));
                    continue;
                }
            }
            merged.add(c);
        }
        node.children().clear();
        node.children().addAll(merged);
    }

    private static Source fromSpec(Component title, String fileName, ForgeConfigSpec spec,
                                   boolean regroup) {
        ACConfigNode root = ACConfigNode.category(fileName, "");
        walk(spec.getSpec(), spec.getValues(), root, "");
        pairUp(root);

        if (!loaded(root)) {
            AcademyCraft.LOGGER.warn("config not loaded yet, cannot be edited from the main menu: {}", fileName);
            return new Source(title, fileName, ACConfigNode.category(fileName, ""), () -> {});
        }
        return new Source(title, fileName, regroup ? regroupBySkill(root) : root, () -> {
            try {
                spec.save();
            } catch (RuntimeException e) {
                AcademyCraft.LOGGER.error("failed to save config: " + fileName, e);
            }
        });
    }

    private static boolean loaded(ACConfigNode root) {
        ACConfigNode leaf = firstLeaf(root);
        if (leaf == null) {
            return true;
        }
        try {
            leaf.entry().get();
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static ACConfigNode firstLeaf(ACConfigNode n) {
        if (!n.isCategory()) {
            return n;
        }
        for (ACConfigNode c : n.children()) {
            ACConfigNode r = firstLeaf(c);
            if (r != null) {
                return r;
            }
        }
        return null;
    }

    private static void walk(UnmodifiableConfig specCfg, UnmodifiableConfig valCfg,
                             ACConfigNode parent, String prefix) {
        Map<String, Object> vals = valCfg.valueMap();
        for (Map.Entry<String, Object> e : specCfg.valueMap().entrySet()) {
            String key = e.getKey();
            Object sv = e.getValue();
            Object vv = vals.get(key);
            String path = ConfigCodec.childPath(prefix, key);

            if (sv instanceof UnmodifiableConfig childSpec && vv instanceof UnmodifiableConfig childVal) {

                ACConfigNode cat = ACConfigNode.category(key, path).display(statName(path, key), null);
                walk(childSpec, childVal, cat, path);

                if (!cat.children().isEmpty()) {
                    parent.add(cat);
                }
            } else if (sv instanceof ForgeConfigSpec.ValueSpec vs
                    && vv instanceof ForgeConfigSpec.ConfigValue<?> cv) {
                parent.add(ACConfigNode.leaf(key, path, new SpecEntry(vs, cv))
                        .display(statName(path, key), null));
            }
        }
    }

    private static final class SpecEntry implements Entry {
        private final ForgeConfigSpec.ValueSpec spec;
        private final ForgeConfigSpec.ConfigValue<?> value;

        SpecEntry(ForgeConfigSpec.ValueSpec spec, ForgeConfigSpec.ConfigValue<?> value) {
            this.spec = spec;
            this.value = value;
        }

        @Override
        public Kind kind() {
            Class<?> c = spec.getClazz();
            if (c == Boolean.class) return Kind.BOOLEAN;
            if (c == Integer.class || c == Long.class) return Kind.INT;
            if (c == Double.class || c == Float.class) return Kind.DOUBLE;

            Object cur = value.get();
            if (cur == null) {
                cur = spec.getDefault();
            }
            if (cur instanceof Boolean) return Kind.BOOLEAN;
            if (cur instanceof Integer || cur instanceof Long) return Kind.INT;
            if (cur instanceof Double || cur instanceof Float) return Kind.DOUBLE;

            Object probe = firstOf(cur);
            if (probe == null) {
                probe = firstOf(spec.getDefault());
            }
            if (probe instanceof Integer || probe instanceof Long) return Kind.LIST_INT;
            if (probe instanceof Double || probe instanceof Float) return Kind.LIST_DOUBLE;
            return Kind.LIST_STRING;
        }

        private static Object firstOf(Object o) {
            return (o instanceof List<?> l && !l.isEmpty()) ? l.get(0) : null;
        }

        @Override
        public Object get() {
            return value.get();
        }

        @SuppressWarnings("unchecked")
        @Override
        public void set(Object v) {
            ((ForgeConfigSpec.ConfigValue<Object>) value).set(v);
        }

        @Override
        public Object defaultValue() {
            return spec.getDefault();
        }

        @Override
        public boolean accepts(Object v) {
            return spec.test(v);
        }

        @Override
        public String rangeHint() {
            ForgeConfigSpec.Range<?> r = spec.getRange();
            if (r == null) {
                return null;
            }
            return r.getMin() + " ~ " + r.getMax();
        }

        @Override
        public List<String> comment() {
            String c = spec.getComment();
            if (c == null || c.isBlank()) {
                return Collections.emptyList();
            }
            List<String> out = new ArrayList<>();
            for (String line : c.split("\n")) {
                String s = line.strip();
                if (!s.isEmpty()) {
                    out.add(s);
                }
            }
            return out;
        }
    }

    private static Source fromSettings() {
        ACConfigNode root = ACConfigNode.category("academy-craft.toml", "");
        for (Map.Entry<String, List<UIProperty>> cat : SettingsUI.registeredProperties().entrySet()) {
            ACConfigNode node = ACConfigNode.category(cat.getKey(), cat.getKey());
            for (UIProperty p : cat.getValue()) {
                if (!(p instanceof UIProperty.Config cfg)) {
                    continue;
                }
                Property prop = claim(cfg);
                if (prop == null) {
                    continue;
                }
                boolean isKey = cfg.element == PropertyElements.KEY;
                node.add(ACConfigNode.leaf(cfg.id, ConfigCodec.childPath(cat.getKey(), cfg.id),
                        new LegacyEntry(prop, cfg.defValue, isKey)));
            }
            if (!node.children().isEmpty()) {
                root.add(node);
            }
        }
        return new Source(Component.translatable("gui.academy.config.general"),
                "academy-craft.toml", root, () -> AcademyCraft.config.save());
    }

    private static Property claim(UIProperty.Config cfg) {
        String cat = cfg.category;
        Object d = cfg.defValue;
        if (d instanceof Boolean b) {
            return AcademyCraft.config.get(cat, cfg.id, b);
        }
        if (d instanceof Number n) {
            return AcademyCraft.config.get(cat, cfg.id, n.intValue());
        }
        if (d instanceof String[] s) {
            return AcademyCraft.config.get(cat, cfg.id, s);
        }
        if (d instanceof double[] s) {
            return AcademyCraft.config.get(cat, cfg.id, s);
        }
        return null;
    }

    private static final class LegacyEntry implements Entry {
        private final Property prop;
        private final Object def;
        private final boolean keyBinding;

        LegacyEntry(Property prop, Object def, boolean keyBinding) {
            this.prop = prop;
            this.def = def;
            this.keyBinding = keyBinding;
        }

        @Override
        public boolean isKeyBinding() {
            return keyBinding;
        }

        @Override
        public Kind kind() {
            return switch (prop.getType()) {
                case BOOLEAN -> Kind.BOOLEAN;
                case INTEGER -> Kind.INT;
                case DOUBLE_LIST -> Kind.LIST_DOUBLE;
                case STRING_LIST -> Kind.LIST_STRING;
            };
        }

        @Override
        public Object get() {
            return ConfigCodec.valueOf(prop);
        }

        private static List<Double> boxed(double[] a) {
            List<Double> out = new ArrayList<>(a.length);
            for (double d : a) {
                out.add(d);
            }
            return out;
        }

        @Override
        public void set(Object v) {
            Object before = get();
            write(v);

            if (!ConfigCodec.same(before, get())) {
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                        new cn.academy.event.ConfigModifyEvent(prop));
            }
        }

        private void write(Object v) {
            switch (prop.getType()) {
                case BOOLEAN -> prop.set((Boolean) v);
                case INTEGER -> prop.set(((Number) v).intValue());
                case DOUBLE_LIST -> {
                    List<?> l = (List<?>) v;
                    double[] a = new double[l.size()];
                    for (int i = 0; i < a.length; i++) {
                        a[i] = ((Number) l.get(i)).doubleValue();
                    }
                    prop.set(a);
                }
                case STRING_LIST -> {
                    List<?> l = (List<?>) v;
                    String[] a = new String[l.size()];
                    for (int i = 0; i < a.length; i++) {
                        a[i] = String.valueOf(l.get(i));
                    }
                    prop.set(a);
                }
            }
        }

        @Override
        public Object defaultValue() {
            if (def instanceof double[] a) {
                return boxed(a);
            }
            if (def instanceof String[] a) {
                return Arrays.asList(a);
            }
            if (def instanceof Number n && prop.getType() == Property.Type.INTEGER) {
                return n.intValue();
            }
            return def;
        }

        @Override
        public boolean accepts(Object v) {
            return v != null;
        }

        @Override
        public List<String> comment() {
            return Collections.emptyList();
        }
    }

    public record Change(String file, String path, String value) {}

    public record PushResult(int applied, int rejected, String badPath, byte reason) {}

    private interface Visitor {
        void accept(String file, String path, Object value);
    }

    private static final Map<String, Object> SESSION = new HashMap<>();

    private static PushResult lastResult;

    private static boolean pushPending;

    private static String sessionKey(String file, String path) {
        return file + "|" + path;
    }

    public static void beginSession() {
        SESSION.clear();
        lastResult = null;
        pushPending = false;
        forEachPushable((file, path, value) -> SESSION.put(sessionKey(file, path), value));
    }

    public static List<Change> pendingChanges() {
        List<Change> out = new ArrayList<>();
        forEachPushable((file, path, value) -> {
            String k = sessionKey(file, path);

            if (SESSION.containsKey(k) && !ConfigCodec.same(SESSION.get(k), value)) {
                out.add(new Change(file, path, ConfigCodec.format(value)));
            }
        });
        return out;
    }

    public static int pushToServer() {
        List<Change> changes = pendingChanges();
        if (changes.isEmpty()) {
            return 0;
        }

        Map<String, List<Change>> byFile = new LinkedHashMap<>();
        for (Change c : changes) {
            byFile.computeIfAbsent(c.file(), f -> new ArrayList<>()).add(c);
        }

        for (Map.Entry<String, List<Change>> e : byFile.entrySet()) {
            List<String> paths = new ArrayList<>(e.getValue().size());
            List<String> values = new ArrayList<>(e.getValue().size());
            for (Change c : e.getValue()) {
                paths.add(c.path());
                values.add(c.value());
            }
            cn.academy.network.ConfigPushMessage.send(e.getKey(), paths, values);
        }

        lastResult = null;
        pushPending = true;
        forEachPushable((file, path, value) -> SESSION.put(sessionKey(file, path), value));
        return changes.size();
    }

    public static void acceptPushResult(String file, int applied, int rejected,
                                        String badPath, byte reason) {
        pushPending = false;
        if (lastResult == null) {
            lastResult = new PushResult(applied, rejected, badPath, reason);
            return;
        }
        boolean keepOld = lastResult.reason() != ServerConfigGate.OK;
        lastResult = new PushResult(
                lastResult.applied() + applied,
                lastResult.rejected() + rejected,
                keepOld ? lastResult.badPath() : badPath,
                keepOld ? lastResult.reason() : reason);
    }

    public static PushResult lastPushResult() {
        return lastResult;
    }

    public static boolean pushPending() {
        return pushPending;
    }

    private static void forEachPushable(Visitor v) {
        visitSpec(ServerConfigGate.F_ABILITY, AbilityConfig.SPEC, v);
        visitSpec(ServerConfigGate.F_INTERFERER, InterfererConfig.SPEC, v);
        visitGeneral(v);
    }

    private static void visitSpec(String file, ForgeConfigSpec spec, Visitor v) {
        ConfigCodec.forEachValue(spec, (path, cv) -> {
            if (!ServerConfigGate.isPushable(file, path)) {
                return;
            }
            try {
                v.accept(file, path, cv.get());
            } catch (RuntimeException e) {

                AcademyCraft.LOGGER.debug("config entry {} is not readable yet, skipped this round", path);
            }
        });
    }

    private static void visitGeneral(Visitor v) {
        for (Map.Entry<String, List<UIProperty>> cat : SettingsUI.registeredProperties().entrySet()) {
            for (UIProperty p : cat.getValue()) {
                if (!(p instanceof UIProperty.Config cfg)) {
                    continue;
                }
                String path = ConfigCodec.childPath(cat.getKey(), cfg.id);
                if (!ServerConfigGate.isPushable(ServerConfigGate.F_GENERAL, path)) {
                    continue;
                }
                Property prop = claim(cfg);
                if (prop != null) {
                    v.accept(ServerConfigGate.F_GENERAL, path, ConfigCodec.valueOf(prop));
                }
            }
        }
    }
}
