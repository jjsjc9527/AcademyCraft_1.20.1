package cn.lambdalib2.s11n.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public final class NetS11nContext {

    private static final ThreadLocal<Level> CURRENT_LEVEL = new ThreadLocal<>();
    private static final ThreadLocal<ServerPlayer> CURRENT_SENDER = new ThreadLocal<>();

    private NetS11nContext() {}

    public static void setLevel(Level level) {
        CURRENT_LEVEL.set(level);
    }

    public static Level getLevel() {
        return CURRENT_LEVEL.get();
    }

    public static void setSender(ServerPlayer sender) {
        CURRENT_SENDER.set(sender);
    }

    public static ServerPlayer getSender() {
        return CURRENT_SENDER.get();
    }

    public static void clear() {
        CURRENT_LEVEL.remove();
        CURRENT_SENDER.remove();
    }
}
