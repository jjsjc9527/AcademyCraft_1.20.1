/*
 * 源自 LambdaLib2 (MIT),Copyright (c) Lambda Innovation, 2013-2016,作者 WeAthFolD。
 */
package cn.lambdalib2.util.markdown;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MarkdownParser {

    private MarkdownParser() {}

    public static final class Attribute {
        public enum Kind { LIST_ELEMENT, HEADER, EMPHASIZE, STRONG, REFERENCE }

        public final Kind kind;
        public final int level;

        private Attribute(Kind kind, int level) {
            this.kind = kind;
            this.level = level;
        }

        public static final Attribute LIST_ELEMENT = new Attribute(Kind.LIST_ELEMENT, 0);
        public static final Attribute EMPHASIZE = new Attribute(Kind.EMPHASIZE, 0);
        public static final Attribute STRONG = new Attribute(Kind.STRONG, 0);
        public static final Attribute REFERENCE = new Attribute(Kind.REFERENCE, 0);

        public static Attribute header(int level) {
            return new Attribute(Kind.HEADER, level);
        }
    }

    private interface Instruction {}

    private record TextI(String text, Set<Attribute> attrs) implements Instruction {}

    private record TagI(String name, Map<String, String> attrs) implements Instruction {}

    private static final Pattern STAR = Pattern.compile("(.*)\\*\\*(.*)");
    private static final Pattern UNDERSCORE = Pattern.compile("(.*)__(.*)");
    private static final Pattern IMAGE = Pattern.compile("(.*)!\\[([^\\[\\]]*)\\]\\(([^()]+)\\)(.*)");
    private static final Pattern INLINE_TAG = Pattern.compile("(.*)!\\[([^\\[\\]]+)\\](.*)");
    private static final Pattern STRING_PROPERTY = Pattern.compile(" *([^=]+)= *\"([^\"]+)\"(.*)");
    private static final Pattern PROPERTY = Pattern.compile(" *([^=]+)= *([^ =]+)(.*)");

    public static void accept(String content, MarkdownRenderer target) {
        content.lines().forEach(ln -> parseLine(ln, target));
    }

    private static void parseLine(String ln, MarkdownRenderer target) {
        Set<Attribute> attributes = new LinkedHashSet<>();
        if (ln.startsWith("#")) {
            int sharps = 0;
            while (sharps < ln.length() && ln.charAt(sharps) == '#') sharps++;
            int level = Math.min(6, sharps);
            attributes.add(Attribute.header(level));
            processSpan(dropWhile(ln.substring(sharps), ' '), attributes, target);
            target.onNewline();
        } else if (ln.startsWith("* ")) {
            attributes.add(Attribute.LIST_ELEMENT);
            processSpan(ln.substring(2), attributes, target);
            target.onNewline();
        } else if (ln.startsWith("> ")) {
            attributes.add(Attribute.REFERENCE);
            processSpan(ln.substring(2), attributes, target);

        } else {
            processSpan(ln, attributes, target);
        }
    }

    private static void processSpan(String ln, Set<Attribute> attrs, MarkdownRenderer target) {
        List<Instruction> list = parseSpan(ln, attrs);
        if (list.isEmpty()) {
            target.onNewline();
        } else {
            for (Instruction ins : list) {
                if (ins instanceof TextI t) {
                    target.onTextContent(t.text(), t.attrs());
                } else if (ins instanceof TagI t) {
                    target.onTag(t.name(), t.attrs());
                }
            }
        }
    }

    private static List<Instruction> parseSpan(String line, Set<Attribute> baseattr) {
        Matcher m;

        m = IMAGE.matcher(line);
        if (m.matches()) {
            List<Instruction> r = new ArrayList<>(parseSpan(m.group(1), baseattr));
            Map<String, String> a = new LinkedHashMap<>();
            a.put("hover", m.group(2));
            a.put("src", m.group(3));
            r.add(new TagI("img", a));
            r.addAll(parseSpan(m.group(4), baseattr));
            return r;
        }

        m = INLINE_TAG.matcher(line);
        if (m.matches()) {
            List<Instruction> r = new ArrayList<>(parseSpan(m.group(1), baseattr));
            r.add(parseTag(m.group(2)));
            r.addAll(parseSpan(m.group(3), baseattr));
            return r;
        }

        m = STAR.matcher(line);
        if (m.matches()) {
            Matcher inner = STAR.matcher(m.group(1));
            if (inner.matches()) {
                List<Instruction> r = new ArrayList<>(parseSpan(inner.group(1), baseattr));
                r.add(new TextI(inner.group(2), plus(baseattr, Attribute.EMPHASIZE)));
                r.addAll(parseSpan(m.group(2), baseattr));
                return r;
            }
        }

        m = UNDERSCORE.matcher(line);
        if (m.matches()) {
            Matcher inner = UNDERSCORE.matcher(m.group(1));
            if (inner.matches()) {
                List<Instruction> r = new ArrayList<>(parseSpan(inner.group(1), baseattr));
                r.add(new TextI(inner.group(2), plus(baseattr, Attribute.STRONG)));
                r.addAll(parseSpan(m.group(2), baseattr));
                return r;
            }
        }

        List<Instruction> r = new ArrayList<>();
        if (!line.isEmpty()) {
            r.add(new TextI(line, baseattr));
        }
        return r;
    }

    private static TagI parseTag(String content) {
        int rwind = content.indexOf(' ');
        int ind = rwind == -1 ? content.length() : rwind;
        String name = content.substring(0, ind);
        Map<String, String> attrs = parseProperties(content.substring(ind), new LinkedHashMap<>());
        return new TagI(name, attrs);
    }

    private static Map<String, String> parseProperties(String rest, Map<String, String> prev) {
        Matcher sm = STRING_PROPERTY.matcher(rest);
        if (sm.matches()) {
            prev.put(sm.group(1).trim(), sm.group(2).trim());
            return parseProperties(sm.group(3), prev);
        }
        Matcher pm = PROPERTY.matcher(rest);
        if (pm.matches()) {
            prev.put(pm.group(1).trim(), pm.group(2).trim());
            return parseProperties(pm.group(3), prev);
        }
        return prev;
    }

    private static Set<Attribute> plus(Set<Attribute> base, Attribute add) {
        Set<Attribute> ret = new LinkedHashSet<>(base);
        ret.add(add);
        return ret;
    }

    private static String dropWhile(String s, char c) {
        int i = 0;
        while (i < s.length() && s.charAt(i) == c) i++;
        return s.substring(i);
    }

}
