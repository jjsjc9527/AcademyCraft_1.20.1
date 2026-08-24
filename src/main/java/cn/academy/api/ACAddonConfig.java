package cn.academy.api;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class ACAddonConfig {

    private static final Logger LOG = LoggerFactory.getLogger("AcademyCraft/AddonConfig");

    public interface Value<T> {
        T get();
    }

    public interface BoolValue extends Value<Boolean> {
        boolean getAsBoolean();
    }

    public interface IntValue extends Value<Integer> {
        int getAsInt();
    }

    public interface DoubleValue extends Value<Double> {
        double getAsDouble();
    }

    private static final class Cached<T> implements Value<T> {
        private final ForgeConfigSpec.ConfigValue<T> cv;
        private final T def;
        private volatile T cache;

        Cached(ForgeConfigSpec.ConfigValue<T> cv, T def) {
            this.cv = cv;
            this.def = def;
            this.cache = def;
        }

        @Override
        public T get() {
            return cache;
        }

        void refresh() {
            try {
                T v = cv.get();
                cache = v == null ? def : v;
            } catch (Throwable t) {

                cache = cache == null ? def : cache;
            }
        }
    }

    private static final class CachedBool extends CachedBase<Boolean> implements BoolValue {
        CachedBool(ForgeConfigSpec.ConfigValue<Boolean> cv, Boolean def) {
            super(cv, def);
        }

        @Override
        public boolean getAsBoolean() {
            return inner.get();
        }
    }

    private static final class CachedInt extends CachedBase<Integer> implements IntValue {
        CachedInt(ForgeConfigSpec.ConfigValue<Integer> cv, Integer def) {
            super(cv, def);
        }

        @Override
        public int getAsInt() {
            return inner.get();
        }
    }

    private static final class CachedDouble extends CachedBase<Double> implements DoubleValue {
        CachedDouble(ForgeConfigSpec.ConfigValue<Double> cv, Double def) {
            super(cv, def);
        }

        @Override
        public double getAsDouble() {
            return inner.get();
        }
    }

    private abstract static class CachedBase<T> implements Value<T> {
        final Cached<T> inner;

        CachedBase(ForgeConfigSpec.ConfigValue<T> cv, T def) {
            this.inner = new Cached<>(cv, def);
        }

        @Override
        public T get() {
            return inner.get();
        }
    }

    public record Addon(String modId, String displayName, ForgeConfigSpec spec, String fileName) {}

    private static final List<Addon> ADDONS = new java.util.concurrent.CopyOnWriteArrayList<>();

    public static List<Addon> registered() {
        return java.util.Collections.unmodifiableList(ADDONS);
    }

    private final String modId;
    private final String displayName;
    private final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
    private final List<Cached<?>> values = new ArrayList<>();
    private int depth;
    private boolean built;

    private ACAddonConfig(String modId, String displayName) {
        this.modId = modId;
        this.displayName = displayName;
        builder.comment(" 本文件由 AcademyCraft 的附属 mod 「" + displayName + "」(" + modId + ")提供。",
                " 改完保存即可生效(热重载),不必重启游戏;",
                " 也可以在主菜单的「AcademyCraft 配置 → 附属包」里改。").push(modId);
        depth++;
    }

    public static ACAddonConfig of(String modId) {
        return of(modId, modId);
    }

    public static ACAddonConfig of(String modId, String displayName) {
        java.util.Objects.requireNonNull(modId, "modId");
        return new ACAddonConfig(modId, displayName == null ? modId : displayName);
    }

    public ACAddonConfig push(String name, String... comment) {
        checkOpen();
        if (comment.length > 0) {
            builder.comment(comment);
        }
        builder.push(name);
        depth++;
        return this;
    }

    public ACAddonConfig pop() {
        checkOpen();
        if (depth <= 1) {
            throw new IllegalStateException("unbalanced pop, already at the outermost level (" + modId + ")");
        }
        builder.pop();
        depth--;
        return this;
    }

    public BoolValue bool(String key, boolean def, String... comment) {
        checkOpen();
        if (comment.length > 0) {
            builder.comment(comment);
        }
        CachedBool v = new CachedBool(builder.define(key, def), def);
        values.add(v.inner);
        return v;
    }

    public IntValue integer(String key, int def, int min, int max, String... comment) {
        checkOpen();
        if (comment.length > 0) {
            builder.comment(comment);
        }
        CachedInt v = new CachedInt(builder.defineInRange(key, def, min, max), def);
        values.add(v.inner);
        return v;
    }

    public DoubleValue decimal(String key, double def, double min, double max, String... comment) {
        checkOpen();
        if (comment.length > 0) {
            builder.comment(comment);
        }
        CachedDouble v = new CachedDouble(builder.defineInRange(key, def, min, max), def);
        values.add(v.inner);
        return v;
    }

    public Value<String> text(String key, String def, String... comment) {
        checkOpen();
        if (comment.length > 0) {
            builder.comment(comment);
        }
        Cached<String> v = new Cached<>(builder.define(key, def), def);
        values.add(v);
        return v;
    }

    @SuppressWarnings("unchecked")
    public Value<List<? extends String>> strings(String key, List<String> def, String... comment) {
        checkOpen();
        if (comment.length > 0) {
            builder.comment(comment);
        }
        Predicate<Object> isStr = o -> o instanceof String;
        ForgeConfigSpec.ConfigValue<List<? extends String>> cv =
                (ForgeConfigSpec.ConfigValue<List<? extends String>>)
                        (ForgeConfigSpec.ConfigValue<?>) builder.defineList(key, def, isStr);
        Cached<List<? extends String>> v = new Cached<>(cv, def);
        values.add(v);
        return v;
    }

    public void build(Runnable onReload) {
        checkOpen();
        while (depth > 0) {
            builder.pop();
            depth--;
        }
        ForgeConfigSpec spec = builder.build();
        built = true;

        String file = "academy-addon-" + modId + ".toml";

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, spec, file);

        ADDONS.add(new Addon(modId, displayName, spec, file));

        Runnable refresh = () -> {
            for (Cached<?> v : values) {
                v.refresh();
            }
            if (onReload != null) {
                try {
                    onReload.run();
                } catch (Throwable t) {
                    LOG.error("[addon-config] reload callback of {} threw", modId, t);
                }
            }
        };

        try {
            var bus = FMLJavaModLoadingContext.get().getModEventBus();
            bus.addListener((net.minecraftforge.fml.event.config.ModConfigEvent.Loading e) -> {
                if (e.getConfig().getSpec() == spec) {
                    refresh.run();
                }
            });
            bus.addListener((net.minecraftforge.fml.event.config.ModConfigEvent.Reloading e) -> {
                if (e.getConfig().getSpec() == spec) {
                    refresh.run();
                }
            });
            LOG.info("[addon-config] registered config for addon {}: config/{}", modId, file);
        } catch (Throwable t) {
            LOG.error("[addon-config] {} cannot obtain the mod event bus, its config will stay at defaults -- "
                    + "make sure ACAddonConfig is called from your own @Mod constructor", modId, t);
        }
    }

    public void build() {
        build(null);
    }

    private void checkOpen() {
        if (built) {
            throw new IllegalStateException("this config (" + modId + ") has already been built and can no longer be modified");
        }
    }
}
