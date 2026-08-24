package cn.academy.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@OnlyIn(Dist.CLIENT)
public final class SvgShape {

    public final float[] xs;
    public final float[] ys;

    public final float vbW;
    public final float vbH;

    private SvgShape(float[] xs, float[] ys, float vbW, float vbH) {
        this.xs = xs;
        this.ys = ys;
        this.vbW = vbW;
        this.vbH = vbH;
    }

    public int size() {
        return xs.length;
    }

    public float minX() {
        float v = Float.MAX_VALUE;
        for (float x : xs) {
            v = Math.min(v, x);
        }
        return v;
    }

    public float maxX() {
        float v = -Float.MAX_VALUE;
        for (float x : xs) {
            v = Math.max(v, x);
        }
        return v;
    }

    public float minY() {
        float v = Float.MAX_VALUE;
        for (float y : ys) {
            v = Math.min(v, y);
        }
        return v;
    }

    public float maxY() {
        float v = -Float.MAX_VALUE;
        for (float y : ys) {
            v = Math.max(v, y);
        }
        return v;
    }

    private static final Map<ResourceLocation, SvgShape> CACHE = new ConcurrentHashMap<>();

    public static SvgShape get(ResourceLocation loc) {
        SvgShape s = CACHE.get(loc);
        if (s != null) {
            return s;
        }
        if (FAILED.contains(loc)) {
            return null;
        }
        try {
            Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(loc);
            if (res.isEmpty()) {
                throw new IOException("resource not found");
            }
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = res.get().openAsReader()) {
                String line;
                while ((line = r.readLine()) != null) {
                    sb.append(line).append('\n');
                }
            }
            s = parse(sb.toString());
            CACHE.put(loc, s);
            return s;
        } catch (Exception e) {
            FAILED.add(loc);
            cn.academy.AcademyCraft.LOGGER.warn(
                    "[AC] failed to read SVG shape: {} -- {} (this widget will no longer be drawn)", loc, e.toString());
            return null;
        }
    }

    private static final java.util.Set<ResourceLocation> FAILED =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    public static void clearCache() {
        CACHE.clear();
        FAILED.clear();
    }

    private static final Pattern VIEW_BOX =
            Pattern.compile("viewBox\\s*=\\s*\"\\s*([-\\d.]+)\\s+([-\\d.]+)\\s+([\\d.]+)\\s+([\\d.]+)");
    private static final Pattern POLYGON =
            Pattern.compile("<polygon[^>]*points\\s*=\\s*\"([^\"]+)\"");

    private static SvgShape parse(String svg) {
        Matcher vb = VIEW_BOX.matcher(svg);
        if (!vb.find()) {
            throw new IllegalArgumentException("no viewBox");
        }
        float w = Float.parseFloat(vb.group(3));
        float h = Float.parseFloat(vb.group(4));

        Matcher pg = POLYGON.matcher(svg);
        if (!pg.find()) {
            throw new IllegalArgumentException("no <polygon points=...>");
        }
        String[] tok = pg.group(1).trim().replace(",", " ").split("\\s+");
        if (tok.length < 6 || (tok.length & 1) != 0) {
            throw new IllegalArgumentException("wrong number of points: " + tok.length);
        }
        List<Float> px = new ArrayList<>(), py = new ArrayList<>();
        for (int i = 0; i < tok.length; i += 2) {
            px.add(Float.parseFloat(tok[i]));
            py.add(Float.parseFloat(tok[i + 1]));
        }

        int n = px.size();
        if (n > 3 && Math.abs(px.get(0) - px.get(n - 1)) < 1e-4f
                && Math.abs(py.get(0) - py.get(n - 1)) < 1e-4f) {
            px.remove(n - 1);
            py.remove(n - 1);
        }
        float[] ax = new float[px.size()], ay = new float[py.size()];
        for (int i = 0; i < ax.length; i++) {
            ax[i] = px.get(i);
            ay[i] = py.get(i);
        }
        return new SvgShape(ax, ay, w, h);
    }
}
