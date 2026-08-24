package cn.academy.config;

import cn.academy.AcademyCraft;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Configuration {

    private final Path file;

    private final Map<String, LinkedHashMap<String, Property>> categories = new LinkedHashMap<>();

    private final Map<String, LinkedHashMap<String, String>> unclaimed = new LinkedHashMap<>();

    private boolean dirty;

    public Configuration(Path file) {
        this.file = file;
        load();
    }

    void markDirty() {
        dirty = true;
    }

    public Property get(String category, String key, boolean def) {
        return get(category, key, def, null);
    }

    public Property get(String category, String key, boolean def, String comment) {
        return obtain(category, key, Property.Type.BOOLEAN, String.valueOf(def), null, comment);
    }

    public Property get(String category, String key, int def) {
        return get(category, key, def, null);
    }

    public Property get(String category, String key, int def, String comment) {
        return obtain(category, key, Property.Type.INTEGER, String.valueOf(def), null, comment);
    }

    public Property get(String category, String key, String[] def) {
        return get(category, key, def, null);
    }

    public Property get(String category, String key, String[] def, String comment) {
        return obtain(category, key, Property.Type.STRING_LIST, null, def == null ? new String[0] : def, comment);
    }

    public Property get(String category, String key, double[] def) {
        String[] s = new String[def == null ? 0 : def.length];
        for (int i = 0; i < s.length; i++) s[i] = String.valueOf(def[i]);
        return obtain(category, key, Property.Type.DOUBLE_LIST, null, s, null);
    }

    public boolean getBoolean(String key, String category, boolean def, String comment) {
        return get(category, key, def, comment).getBoolean();
    }

    public int getInt(String key, String category, int def, String comment) {
        return get(category, key, def, comment).getInt();
    }

    public Property find(String category, String key) {
        LinkedHashMap<String, Property> cat = categories.get(category);
        return cat == null ? null : cat.get(key);
    }

    private Property obtain(String category, String key, Property.Type type,
                            String scalarDef, String[] listDef, String comment) {
        LinkedHashMap<String, Property> cat = categories.computeIfAbsent(category, c -> new LinkedHashMap<>());
        Property existing = cat.get(key);
        if (existing != null) {
            return existing;
        }

        Property p = new Property(this, category, key, type);
        p.comment = comment;

        String raw = takeUnclaimed(category, key);
        boolean ok = false;
        if (raw != null) {
            switch (type) {
                case BOOLEAN -> {
                    if ("true".equalsIgnoreCase(raw) || "false".equalsIgnoreCase(raw)) {
                        p.initValue(raw.toLowerCase());
                        ok = true;
                    }
                }
                case INTEGER -> {
                    try {
                        p.initValue(String.valueOf(Integer.parseInt(raw.trim())));
                        ok = true;
                    } catch (NumberFormatException ignored) {
                    }
                }
                case STRING_LIST, DOUBLE_LIST -> {

                    String[] parsed = parseList(raw);
                    if (parsed != null) {
                        p.initValues(parsed);
                        ok = true;
                    }
                }
            }
        }
        if (!ok) {

            if (type == Property.Type.STRING_LIST || type == Property.Type.DOUBLE_LIST) {
                p.initValues(listDef);
            } else {
                p.initValue(scalarDef);
            }
            dirty = true;
        }

        cat.put(key, p);
        return p;
    }

    private String takeUnclaimed(String category, String key) {
        LinkedHashMap<String, String> cat = unclaimed.get(category);
        return cat == null ? null : cat.remove(key);
    }

    private void load() {
        if (!Files.exists(file)) {
            dirty = true;
            return;
        }
        try {
            String current = "general";
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String s = line.trim();
                if (s.isEmpty() || s.startsWith("#")) continue;

                if (s.startsWith("[") && s.endsWith("]")) {
                    current = s.substring(1, s.length() - 1).trim();
                    unclaimed.computeIfAbsent(current, c -> new LinkedHashMap<>());
                    continue;
                }
                int eq = s.indexOf('=');
                if (eq <= 0) continue;
                String key = s.substring(0, eq).trim();
                String val = s.substring(eq + 1).trim();
                unclaimed.computeIfAbsent(current, c -> new LinkedHashMap<>()).put(key, val);
            }
        } catch (IOException e) {
            AcademyCraft.LOGGER.error("failed to read config, defaults will be used for this run: " + file, e);
            unclaimed.clear();
            dirty = true;
        }
    }

    private static String[] parseList(String raw) {
        String s = raw.trim();
        if (!s.startsWith("[") || !s.endsWith("]")) return null;
        s = s.substring(1, s.length() - 1).trim();
        if (s.isEmpty()) return new String[0];

        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuote = false, escape = false;
        for (char c : s.toCharArray()) {
            if (escape) {
                cur.append(c);
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else if (c == '"') {
                inQuote = !inQuote;
            } else if (c == ',' && !inQuote) {
                out.add(cur.toString().trim());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString().trim());
        return out.toArray(new String[0]);
    }

    public void save() {
        if (!dirty) return;
        try {
            Path parent = file.getParent();
            if (parent != null) Files.createDirectories(parent);

            try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                w.write("# AcademyCraft —— 设置(数据终端 → 设置 App 会写这个文件)");
                w.newLine();
                w.write("# 可手工编辑。格式:[分区] / 键 = 值。改完重进游戏生效。");
                w.newLine();
                w.write("# 技能数值不在这里 —— 那是另一套配置(config/academy-craft-data.conf)。");
                w.newLine();

                Set<String> order = new LinkedHashSet<>(unclaimed.keySet());
                order.addAll(categories.keySet());

                for (String cat : order) {
                    Map<String, Property> claimed = categories.get(cat);
                    Map<String, String> rest = unclaimed.get(cat);
                    boolean empty = (claimed == null || claimed.isEmpty()) && (rest == null || rest.isEmpty());
                    if (empty) continue;

                    w.newLine();
                    w.write("[" + cat + "]");
                    w.newLine();

                    if (claimed != null) {
                        for (Property p : claimed.values()) {
                            if (p.comment != null && !p.comment.isEmpty()) {
                                w.write("# " + p.comment);
                                w.newLine();
                            }
                            w.write(p.getName() + " = " + p.serialize());
                            w.newLine();
                        }
                    }
                    if (rest != null && !rest.isEmpty()) {

                        for (Map.Entry<String, String> e : rest.entrySet()) {
                            w.write(e.getKey() + " = " + e.getValue());
                            w.newLine();
                        }
                    }
                }
            }
            dirty = false;
        } catch (IOException e) {
            AcademyCraft.LOGGER.error("failed to save config: " + file, e);
        }
    }
}
