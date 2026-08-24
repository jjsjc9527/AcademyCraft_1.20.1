package cn.academy.config;

public final class Property {

    public enum Type { BOOLEAN, INTEGER, STRING_LIST, DOUBLE_LIST }

    private final Configuration owner;
    private final String category;
    private final String name;
    private final Type type;

    private String value;

    private String[] values;

    String comment;

    Property(Configuration owner, String category, String name, Type type) {
        this.owner = owner;
        this.category = category;
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public Type getType() {
        return type;
    }

    public boolean isIntValue() {
        return type == Type.INTEGER;
    }

    public boolean isBooleanValue() {
        return type == Type.BOOLEAN;
    }

    public boolean getBoolean() {
        return "true".equalsIgnoreCase(value);
    }

    public int getInt() {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public String getString() {
        return value;
    }

    public String[] getStringList() {
        return values == null ? new String[0] : values.clone();
    }

    public int[] getIntList() {
        String[] src = values == null ? new String[0] : values;
        int[] ret = new int[src.length];
        for (int i = 0; i < src.length; i++) {
            try {
                ret[i] = Integer.parseInt(src[i].trim());
            } catch (NumberFormatException e) {
                ret[i] = 0;
            }
        }
        return ret;
    }

    public double[] getDoubleList() {
        String[] src = values == null ? new String[0] : values;
        double[] ret = new double[src.length];
        for (int i = 0; i < src.length; i++) {
            try {
                ret[i] = Double.parseDouble(src[i].trim());
            } catch (NumberFormatException e) {
                ret[i] = 0;
            }
        }
        return ret;
    }

    public void set(boolean b) {
        setRaw(String.valueOf(b));
    }

    public void set(int i) {
        setRaw(String.valueOf(i));
    }

    public void set(String s) {
        setRaw(s);
    }

    public void set(String[] list) {
        values = list == null ? new String[0] : list.clone();
        owner.markDirty();
    }

    public void set(double[] list) {
        String[] s = new String[list == null ? 0 : list.length];
        for (int i = 0; i < s.length; i++) s[i] = String.valueOf(list[i]);
        set(s);
    }

    private void setRaw(String s) {
        if (!java.util.Objects.equals(value, s)) {
            value = s;
            owner.markDirty();
        }
    }

    void initValue(String v) {
        value = v;
    }

    void initValues(String[] v) {
        values = v;
    }

    String serialize() {
        if (type == Type.STRING_LIST || type == Type.DOUBLE_LIST) {
            if (values == null || values.length == 0) return "[]";

            boolean quote = type == Type.STRING_LIST;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < values.length; i++) {
                if (i > 0) sb.append(", ");
                if (quote) {
                    sb.append('"').append(values[i].replace("\\", "\\\\").replace("\"", "\\\"")).append('"');
                } else {
                    sb.append(values[i]);
                }
            }
            return sb.append(']').toString();
        }
        return value;
    }
}
