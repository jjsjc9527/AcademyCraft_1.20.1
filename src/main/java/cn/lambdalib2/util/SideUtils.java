package cn.lambdalib2.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.util.thread.EffectiveSide;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.Collections;
import java.util.List;

public final class SideUtils {

    private SideUtils() {}

    public static LogicalSide getRuntimeSide() {
        return EffectiveSide.get();
    }

    public static boolean isClient() {
        return getRuntimeSide() == LogicalSide.CLIENT;
    }

    public static boolean isPlayerInGame() {
        if (getRuntimeSide() != LogicalSide.CLIENT) return false;
        Boolean r = DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> ClientSideAccess::isPlayerInGame);
        return r != null && r;
    }

    public static boolean isGamePaused() {
        if (getRuntimeSide() != LogicalSide.CLIENT) return false;
        Boolean r = DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> ClientSideAccess::isGamePaused);
        return r != null && r;
    }

    public static Player getThePlayer() {
        return DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> ClientSideAccess::thePlayer);
    }

    public static Player findPlayerOnServer(String name) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? null : server.getPlayerList().getPlayerByName(name);
    }

    public static List<ServerPlayer> getPlayerListOnServer() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? Collections.emptyList() : server.getPlayerList().getPlayers();
    }
}
