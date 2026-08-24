package cn.lambdalib2.util;

public class Color {

    public int r, g, b, a;

    public Color() {
        this(255, 255, 255, 255);
    }

    public Color(int r, int g, int b, int a) {
        set(r, g, b, a);
    }

    public Color(int r, int g, int b) {
        this(r, g, b, 255);
    }

    public Color(Color other) {
        this(other.r, other.g, other.b, other.a);
    }

    public void set(int r, int g, int b, int a) {
        this.r = clamp(r);
        this.g = clamp(g);
        this.b = clamp(b);
        this.a = clamp(a);
    }

    public void set(int r, int g, int b) {
        set(r, g, b, this.a);
    }

    public void setColor(Color other) {
        set(other.r, other.g, other.b, other.a);
    }

    public int getRed() { return r; }
    public int getGreen() { return g; }
    public int getBlue() { return b; }
    public int getAlpha() { return a; }

    public void setRed(int v) { r = clamp(v); }
    public void setGreen(int v) { g = clamp(v); }
    public void setBlue(int v) { b = clamp(v); }
    public void setAlpha(int v) { a = clamp(v); }

    public byte getRedByte() { return (byte) r; }
    public byte getGreenByte() { return (byte) g; }
    public byte getBlueByte() { return (byte) b; }
    public byte getAlphaByte() { return (byte) a; }

    public float getRedF() { return r / 255f; }
    public float getGreenF() { return g / 255f; }
    public float getBlueF() { return b / 255f; }
    public float getAlphaF() { return a / 255f; }

    public Color copy() {
        return new Color(r, g, b, a);
    }

    public int toARGB() {
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int clamp(int v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }

    @Override
    public String toString() {
        return "Color(" + r + ", " + g + ", " + b + ", " + a + ")";
    }
}
