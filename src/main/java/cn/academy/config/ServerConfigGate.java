package cn.academy.config;

import cn.academy.AcademyCraft;
import cn.academy.command.ACCommands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;
import java.util.Set;

public final class ServerConfigGate {

    private ServerConfigGate() {}

    public static final String F_ABILITY = "academy-ability.toml";
    public static final String F_INTERFERER = "academy-ability-interferer.toml";
    public static final String F_GENERAL = "academy-craft.toml";

    public static final byte OK = 0;

    public static final byte DENIED = 1;

    public static final byte NOT_ALLOWED = 2;

    public static final byte BAD_VALUE = 3;

    public static final byte SAVE_FAILED = 4;

    private static final Set<String> GENERAL_PUSHABLE = Set.of(
            "generic.attackPlayer",
            "generic.destroyBlocks");

    public static boolean canPush(ServerPlayer player) {
        return player != null && player.hasPermissions(ACCommands.PERM_LEVEL);
    }

    private static ForgeConfigSpec specOf(String file) {
        if (F_ABILITY.equals(file)) {
            return AbilityConfig.SPEC;
        }
        if (F_INTERFERER.equals(file)) {
            return InterfererConfig.SPEC;
        }
        return null;
    }

    public static boolean isPushable(String file, String path) {
        ForgeConfigSpec spec = specOf(file);
        if (spec != null) {
            return ConfigCodec.index(spec).containsKey(path);
        }
        if (F_GENERAL.equals(file)) {
            return GENERAL_PUSHABLE.contains(path) && findProperty(path) != null;
        }
        return false;
    }

    private static Property findProperty(String path) {
        int dot = path.indexOf('.');
        if (dot <= 0 || dot == path.length() - 1 || AcademyCraft.config == null) {
            return null;
        }
        return AcademyCraft.config.find(path.substring(0, dot), path.substring(dot + 1));
    }

    public static byte apply(String file, String path, String raw) {
        if (!isPushable(file, path)) {
            return NOT_ALLOWED;
        }
        try {
            ForgeConfigSpec spec = specOf(file);
            return spec != null ? applySpec(spec, path, raw) : applyProperty(path, raw);
        } catch (RuntimeException e) {
            AcademyCraft.LOGGER.warn("failed to apply pushed config: {} / {}", file, path, e);
            return BAD_VALUE;
        }
    }

    @SuppressWarnings("unchecked")
    private static byte applySpec(ForgeConfigSpec spec, String path, String raw) {
        ForgeConfigSpec.ConfigValue<?> cv = ConfigCodec.index(spec).get(path);
        ForgeConfigSpec.ValueSpec vs = ConfigCodec.specAt(spec, path);
        if (cv == null || vs == null) {
            return NOT_ALLOWED;
        }

        Object parsed = ConfigCodec.parseLike(cv.get(), vs.getDefault(), raw);
        if (parsed == null || !vs.test(parsed)) {
            return BAD_VALUE;
        }

        ((ForgeConfigSpec.ConfigValue<Object>) cv).set(parsed);
        return OK;
    }

    private static byte applyProperty(String path, String raw) {
        Property p = findProperty(path);
        if (p == null) {
            return NOT_ALLOWED;
        }

        Object current = ConfigCodec.valueOf(p);
        Object parsed = ConfigCodec.parseLike(current, current, raw);
        if (parsed == null) {
            return BAD_VALUE;
        }

        switch (p.getType()) {
            case BOOLEAN -> {
                if (!(parsed instanceof Boolean b)) {
                    return BAD_VALUE;
                }
                p.set(b.booleanValue());
            }
            case INTEGER -> {
                if (!(parsed instanceof Number n)) {
                    return BAD_VALUE;
                }
                p.set(n.intValue());
            }
            case STRING_LIST -> {
                if (!(parsed instanceof List<?> l)) {
                    return BAD_VALUE;
                }
                String[] a = new String[l.size()];
                for (int i = 0; i < a.length; i++) {
                    a[i] = String.valueOf(l.get(i));
                }
                p.set(a);
            }
            case DOUBLE_LIST -> {
                if (!(parsed instanceof List<?> l)) {
                    return BAD_VALUE;
                }
                double[] a = new double[l.size()];
                for (int i = 0; i < a.length; i++) {
                    if (!(l.get(i) instanceof Number n)) {
                        return BAD_VALUE;
                    }
                    a[i] = n.doubleValue();
                }
                p.set(a);
            }
        }

        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
                new cn.academy.event.ConfigModifyEvent(p));
        return OK;
    }

    public static boolean save(String file) {
        try {
            ForgeConfigSpec spec = specOf(file);
            if (spec != null) {
                spec.save();
                return true;
            }
            if (F_GENERAL.equals(file) && AcademyCraft.config != null) {
                AcademyCraft.config.save();
                return true;
            }

            AcademyCraft.LOGGER.error("failed to save config: unknown file name {}", file);
            return false;
        } catch (RuntimeException e) {
            AcademyCraft.LOGGER.error("failed to save config: " + file, e);
            return false;
        }
    }
}
