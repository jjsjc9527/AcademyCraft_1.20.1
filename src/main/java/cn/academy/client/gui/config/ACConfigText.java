package cn.academy.client.gui.config;

import cn.academy.client.gui.config.ACConfigNode.Kind;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class ACConfigText {

    private ACConfigText() {}

    @SuppressWarnings("unused")
    public static String format(Kind kind, Object v) {
        return cn.academy.config.ConfigCodec.format(v);
    }

    public static Object parse(Kind kind, String raw) {
        String s = raw == null ? "" : raw.trim();
        try {
            switch (kind) {
                case BOOLEAN: {
                    if (s.equalsIgnoreCase("true")) return Boolean.TRUE;
                    if (s.equalsIgnoreCase("false")) return Boolean.FALSE;
                    return null;
                }
                case INT:
                    return s.isEmpty() ? null : Integer.valueOf(Integer.parseInt(s));
                case DOUBLE: {
                    if (s.isEmpty()) return null;
                    double d = Double.parseDouble(s);

                    return Double.isFinite(d) ? Double.valueOf(d) : null;
                }
                case LIST_INT:
                case LIST_DOUBLE:
                case LIST_STRING: {
                    List<Object> out = new ArrayList<>();
                    if (s.isEmpty()) {
                        return out;
                    }
                    for (String part : s.split(",", -1)) {
                        String p = part.trim();
                        if (kind == Kind.LIST_INT) {
                            out.add(Integer.valueOf(Integer.parseInt(p)));
                        } else if (kind == Kind.LIST_DOUBLE) {
                            double d = Double.parseDouble(p);
                            if (!Double.isFinite(d)) return null;
                            out.add(Double.valueOf(d));
                        } else {
                            out.add(p);
                        }
                    }
                    return out;
                }
                default:
                    return null;
            }
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static boolean same(Object a, Object b) {
        return cn.academy.config.ConfigCodec.same(a, b);
    }
}
