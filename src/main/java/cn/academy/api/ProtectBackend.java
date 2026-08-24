package cn.academy.api;

import net.minecraft.server.level.ServerPlayer;

public interface ProtectBackend {

    String id();

    default void onTakeOver(ServerPlayer player) {}

    default void onRelease(ServerPlayer player) {}

    default boolean preventsRemoval() {
        return false;
    }
}
