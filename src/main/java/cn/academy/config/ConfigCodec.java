package cn.academy.config;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;

public final class ConfigCodec {

    private ConfigCodec() {}

    public static String format(Object v) {
        if (v == null) {
            return "";
        }
        if (v instanceof List<?> list) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(list.get(i));
            }
            return sb.toString();
        }
        return String.valueOf(v);
    }

    public static Object parseLike(Object current, Object fallback, String raw) {
        Object sample = current != null ? current : fallback;
        String s = raw == null ? "" : raw.trim();

        try {
            if (sample instanceof Boolean) {
                if (s.equalsIgnoreCase("true")) return Boolean.TRUE;
                if (s.equalsIgnoreCase("false")) return Boolean.FALSE;
                return null;
            }
            if (sample instanceof Integer) {
                return s.isEmpty() ? null : Integer.valueOf(Integer.parseInt(s));
            }
            if (sample instanceof Long) {
                return s.isEmpty() ? null : Long.valueOf(Long.parseLong(s));
            }
            if (sample instanceof Double || sample instanceof Float) {
                if (s.isEmpty()) return null;
                double d = Double.parseDouble(s);
                return Double.isFinite(d) ? Double.valueOf(d) : null;
            }
            if (sample instanceof List<?> list) {

                Object probe = firstOf(list);
                if (probe == null) {
                    probe = firstOf(fallback);
                }
                return parseList(probe, s);
            }
        } catch (NumberFormatException e) {
            return null;
        }

        return s;
    }

    private static Object parseList(Object probe, String s) {
        List<Object> out = new ArrayList<>();
        if (s.isEmpty()) {
            return out;
        }
        for (String part : s.split(",", -1)) {
            String p = part.trim();
            if (probe instanceof Integer) {
                out.add(Integer.valueOf(Integer.parseInt(p)));
            } else if (probe instanceof Long) {
                out.add(Long.valueOf(Long.parseLong(p)));
            } else if (probe instanceof Double || probe instanceof Float) {
                double d = Double.parseDouble(p);
                if (!Double.isFinite(d)) {
                    return null;
                }
                out.add(Double.valueOf(d));
            } else {
                out.add(p);
            }
        }
        return out;
    }

    private static Object firstOf(Object o) {
        return (o instanceof List<?> l && !l.isEmpty()) ? l.get(0) : null;
    }

    public static Object valueOf(Property p) {
        return switch (p.getType()) {
            case BOOLEAN -> Boolean.valueOf(p.getBoolean());
            case INTEGER -> Integer.valueOf(p.getInt());
            case STRING_LIST -> java.util.Arrays.asList(p.getStringList());
            case DOUBLE_LIST -> boxed(p.getDoubleList());
        };
    }

    private static List<Double> boxed(double[] a) {
        List<Double> out = new ArrayList<>(a.length);
        for (double d : a) {
            out.add(Double.valueOf(d));
        }
        return out;
    }

    public static boolean same(Object a, Object b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (a instanceof Number na && b instanceof Number nb) {
            return Double.compare(na.doubleValue(), nb.doubleValue()) == 0;
        }
        if (a instanceof List<?> la && b instanceof List<?> lb) {
            if (la.size() != lb.size()) {
                return false;
            }
            for (int i = 0; i < la.size(); i++) {
                if (!same(la.get(i), lb.get(i))) {
                    return false;
                }
            }
            return true;
        }
        return a.equals(b);
    }

    private static final Map<ForgeConfigSpec, Map<String, ForgeConfigSpec.ConfigValue<?>>> INDEX =
            Collections.synchronizedMap(new WeakHashMap<>());

    public static Map<String, ForgeConfigSpec.ConfigValue<?>> index(ForgeConfigSpec spec) {
        return INDEX.computeIfAbsent(spec, s -> {
            Map<String, ForgeConfigSpec.ConfigValue<?>> out = new LinkedHashMap<>();
            forEachValue(s, out::put);
            return Collections.unmodifiableMap(out);
        });
    }

    public static String childPath(String prefix, String key) {
        return prefix == null || prefix.isEmpty() ? key : prefix + "." + key;
    }

    public static void forEachValue(ForgeConfigSpec spec,
                                    BiConsumer<String, ForgeConfigSpec.ConfigValue<?>> fn) {
        walk(spec.getValues(), "", fn);
    }

    private static void walk(UnmodifiableConfig cfg, String prefix,
                             BiConsumer<String, ForgeConfigSpec.ConfigValue<?>> fn) {
        for (Map.Entry<String, Object> e : cfg.valueMap().entrySet()) {
            String path = childPath(prefix, e.getKey());
            Object v = e.getValue();
            if (v instanceof UnmodifiableConfig child) {
                walk(child, path, fn);
            } else if (v instanceof ForgeConfigSpec.ConfigValue<?> cv) {
                fn.accept(path, cv);
            }
        }
    }

    public static ForgeConfigSpec.ValueSpec specAt(ForgeConfigSpec spec, String path) {
        Object o = spec.getSpec().get(java.util.Arrays.asList(path.split("\\.")));
        return o instanceof ForgeConfigSpec.ValueSpec vs ? vs : null;
    }
}
