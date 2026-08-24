package cn.academy.config;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class ConfigChoices {

    private ConfigChoices() {}

    private static final Map<String, Source> TABLE = new ConcurrentHashMap<>();

    public record Source(Supplier<List<String>> choices,
                         String title,
                         Supplier<List<String>> preset,
                         String presetLabel,
                         String iconType) {

        public Source(Supplier<List<String>> choices, String title,
                      Supplier<List<String>> preset, String presetLabel) {
            this(choices, title, preset, presetLabel, null);
        }
    }

    public static final class IconType {
        private IconType() {}

        public static final String MOB_EFFECT = "mob_effect";
    }

    public static void register(String fullPath, Source source) {
        if (fullPath != null && source != null) {
            TABLE.put(fullPath, source);
        }
    }

    public static Source get(String fullPath) {
        return fullPath == null ? null : TABLE.get(fullPath);
    }
}
