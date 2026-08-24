/*
 * 源自 LambdaLib2 (MIT),Copyright (c) Lambda Innovation, 2013-2016,作者 WeAthFolD。
 */
package cn.lambdalib2.util.markdown;

import cn.lambdalib2.render.font.IFont;
import cn.lambdalib2.render.font.IFont.FontOption;
import cn.lambdalib2.util.Color;
import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.Fragmentor;
import cn.lambdalib2.util.Fragmentor.IFontSizeProvider;
import cn.lambdalib2.util.HudUtils;
import cn.lambdalib2.util.markdown.MarkdownParser.Attribute;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GLMarkdownRenderer implements MarkdownRenderer {

    private static final float[] HEADER_PREFIXES = {1.8f, 1.6f, 1.4f, 1.2f, 1.0f};

    public IFont font = defaultFont();
    public IFont boldFont = defaultFont();
    public IFont italicFont = defaultFont();

    public float fontSize = 10.0f;
    public int lineSpacing = 4;
    public float widthLimit = Float.MAX_VALUE;

    public Color textColor = Colors.white();
    public Color refTextColor = Colors.fromHexColor(0xffe1c385);
    public Color refBackgroundColor = Colors.fromFloat(0.5f, 0.5f, 0.5f, 0.4f);

    private static IFont defaultFont() {

        return cn.lambdalib2.render.font.Fonts.get("AC_Normal");
    }

    public void setFonts(IFont font, IFont boldFont, IFont italicFont) {
        this.font = font;
        this.boldFont = boldFont;
        this.italicFont = italicFont;
    }

    private static final class Context {
        float x = 0, y = 0, lastSize = 0;
        boolean lineBegin = true;

        boolean lineHead() {
            return x == 0.0f;
        }
    }

    private final Context rc = new Context();

    private final List<Runnable> instructions = new ArrayList<>();

    private static final class TextInsr implements Runnable {
        float x, y;
        final IFont font;
        final FontOption option;
        final String txt;

        TextInsr(String txt, float x, float y, IFont font, FontOption option) {
            this.txt = txt;
            this.x = x;
            this.y = y;
            this.font = font;
            this.option = option;
        }

        @Override
        public void run() {
            font.draw(txt, x, y, option);
        }
    }

    private void insr(Runnable r) {
        instructions.add(r);
    }

    @Override
    public void onTextContent(String text, Set<Attribute> attr) {
        IFont usedFont = font;
        float usedSize = fontSize;
        boolean listElement = false;
        Color usedColor = textColor;

        for (Attribute a : attr) {
            switch (a.kind) {
                case LIST_ELEMENT:
                    if (rc.lineBegin) listElement = true;
                    break;
                case HEADER:
                    usedSize = fontSize * HEADER_PREFIXES[Math.min(a.level, HEADER_PREFIXES.length - 1)];
                    if (a.level <= 3) usedFont = boldFont;
                    break;
                case EMPHASIZE:
                    usedFont = italicFont;
                    break;
                case STRONG:
                    usedFont = boldFont;
                    break;
                case REFERENCE:
                    usedColor = refTextColor;
                    if (rc.lineHead()) rc.x = usedSize;
                    break;
                default:
                    break;
            }
        }

        final FontOption option = new FontOption(usedSize, usedColor);
        final IFont uFont = usedFont;
        final float uSize = usedSize;

        List<String> lines = Fragmentor.toMultiline(text, new IFontSizeProvider() {
            @Override
            public double getCharWidth(int chr) {
                return uFont.getCharWidth(chr, option);
            }

            @Override
            public double getTextWidth(String str) {
                return uFont.getTextWidth(str, option);
            }
        }, rc.x, widthLimit);

        if (listElement) {
            final float dotSize = usedSize * 0.2f;
            final float indent = usedSize * 1.2f;
            rc.x = indent;
            final float x = rc.x;
            final float y = rc.y;
            insr(() -> {
                RenderSystem.setShaderColor(1, 1, 1, 1);
                HudUtils.colorRect(x, y + uSize / 2 - dotSize / 2, dotSize, dotSize);
            });
            rc.x += dotSize * 2;
        }

        for (int i = 0; i < lines.size(); i++) {
            String ln = lines.get(i);
            float width = usedFont.getTextWidth(ln, option);

            if (i != 0 || width + rc.x > widthLimit * 1.2f) {
                newline(true);
            }

            rc.lastSize = option.fontSize;
            insr(new TextInsr(ln, rc.x, rc.y, usedFont, option));
            rc.x += width;
        }
    }

    private void newline(boolean continu) {
        rc.x = 0.0f;
        if (continu) {
            rc.y += rc.lastSize;
        } else {
            rc.y += rc.lastSize + lineSpacing;
            rc.lastSize = 0;
        }
        rc.lineBegin = !continu;
    }

    @Override
    public void onNewline() {
        newline(false);
    }

    @Override
    public void onTag(String name, Map<String, String> attr) {
        if (!name.equals("img")) return;

        final ResourceLocation src = new ResourceLocation(attr.get("src"));
        float scale = attr.containsKey("scale") ? Float.parseFloat(attr.get("scale")) : 1;

        float sw, sh;
        if (attr.containsKey("width") && attr.containsKey("height")) {
            sw = Float.parseFloat(attr.get("width")) * scale;
            sh = Float.parseFloat(attr.get("height")) * scale;
        } else {
            HudUtils.loadTexture(src);
            int[] size = HudUtils.boundTextureSize();
            sw = size[0] * scale;
            sh = size[1] * scale;
        }

        if (sw + rc.x > widthLimit) {
            float s = (widthLimit - rc.x) / sw;
            sw = sw * s;
            sh = sh * s;
        }

        final float x = rc.x;
        final float y = (sh >= rc.lastSize) ? rc.y : rc.y + rc.lastSize - sh;
        final float fw = sw, fh = sh;
        insr(() -> {
            HudUtils.loadTexture(src);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            HudUtils.rect(x, y, fw, fh);
        });

        float newY = rc.y + Math.max(0f, sh - fontSize);

        for (int i = instructions.size() - 2; i >= 0; i--) {
            if (instructions.get(i) instanceof TextInsr ti) {
                if (ti.y == rc.y) ti.y = newY;
            } else {
                break;
            }
        }

        rc.x += sw;
        rc.y = newY;
        rc.lastSize = fontSize;
    }

    public void render() {
        for (Runnable r : instructions) r.run();
    }

    public float getMaxHeight() {
        return rc.y + fontSize;
    }

}
