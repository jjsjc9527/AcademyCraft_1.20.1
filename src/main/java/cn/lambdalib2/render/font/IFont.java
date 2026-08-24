package cn.lambdalib2.render.font;

import cn.lambdalib2.render.font.TextSplitter.IFontSizeProvider;
import cn.lambdalib2.s11n.SerializeType;
import cn.lambdalib2.util.Color;
import cn.lambdalib2.util.Colors;

import java.util.List;

public interface IFont {

    enum FontAlign {
        LEFT(0), CENTER(0.5f), RIGHT(1);

        public final float lenOffset;

        FontAlign(float _lenOffset) {
            lenOffset = _lenOffset;
        }
    }

    class Extent {
        public int linesDrawn;
        public float width;
        public float height;

        public Extent(int _lines, float _width, float _height) {
            linesDrawn = _lines;
            width = _width;
            height = _height;
        }
    }

    @SerializeType
    class FontOption {

        public float fontSize;
        public FontAlign align;
        public Color color;

        public FontOption() {
            this(10);
        }

        public FontOption(float _fontsz) {
            this(_fontsz, FontAlign.LEFT);
        }

        public FontOption(float _fontsz, Color _color) {
            this(_fontsz, FontAlign.LEFT, _color);
        }

        public FontOption(float _fontsz, int hex) {
            this(_fontsz, Colors.fromRGBA32(hex));
        }

        public FontOption(float _fontsz, FontAlign _align) {
            this(_fontsz, _align, Colors.white());
        }

        public FontOption(float _fontsz, FontAlign _align, Color _color) {
            fontSize = _fontsz;
            align = _align;
            color = _color;
        }

        public FontOption(float _fontsz, FontAlign _align, int hex) {
            this(_fontsz, _align, Colors.fromRGBA32(hex));
        }

        @Override
        public FontOption clone() {
            FontOption ret = new FontOption();
            ret.fontSize = fontSize;
            ret.align = align;
            ret.color = color == null ? Colors.white() : color.copy();
            return ret;
        }
    }

    void draw(String str, float x, float y, FontOption option);

    float getCharWidth(int chr, FontOption option);

    float getTextWidth(String str, FontOption option);

    default void drawSeperated(String str, final float x, float y, float limit, FontOption option) {
        List<String> lines = TextSplitter.split(str, provider(option), limit);
        for (int i = 0; i < lines.size(); ++i) {
            draw(lines.get(i), x, y + i * option.fontSize, option);
        }
    }

    default Extent drawSeperated_Sim(String str, float limit, FontOption option) {
        List<String> lines = TextSplitter.split(str, provider(option), limit);
        return new Extent(lines.size(),
                lines.size() == 1 ? getTextWidth(lines.get(0), option) : limit,
                lines.size() * option.fontSize);
    }

    default IFontSizeProvider provider(FontOption option) {
        return new IFontSizeProvider() {
            @Override
            public float getCharWidth(int chr) {
                return IFont.this.getCharWidth(chr, option);
            }

            @Override
            public float getTextWidth(String str) {
                return IFont.this.getTextWidth(str, option);
            }
        };
    }
}
