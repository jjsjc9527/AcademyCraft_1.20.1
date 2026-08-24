package cn.academy.network;

import cn.academy.AcademyCraft;
import cn.academy.config.ServerConfigGate;
import cn.academy.datapart.CPData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ConfigPushMessage {

    private static final int MAX_ITEMS = 1024;

    private final String file;
    private final List<String> paths;
    private final List<String> values;

    public ConfigPushMessage(String file, List<String> paths, List<String> values) {
        this.file = file;
        this.paths = paths;
        this.values = values;
    }

    public static void encode(ConfigPushMessage m, FriendlyByteBuf buf) {
        buf.writeUtf(m.file, 64);
        buf.writeVarInt(m.paths.size());
        for (int i = 0; i < m.paths.size(); i++) {
            buf.writeUtf(m.paths.get(i), 128);
            buf.writeUtf(m.values.get(i), 512);
        }
    }

    public static ConfigPushMessage decode(FriendlyByteBuf buf) {
        String file = buf.readUtf(64);
        int n = buf.readVarInt();
        if (n < 0 || n > MAX_ITEMS) {
            throw new IllegalArgumentException("illegal entry count in config push: " + n);
        }
        List<String> paths = new ArrayList<>(n);
        List<String> values = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            paths.add(buf.readUtf(128));
            values.add(buf.readUtf(512));
        }
        return new ConfigPushMessage(file, paths, values);
    }

    public static void handle(ConfigPushMessage m, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) {
                return;
            }

            if (!ServerConfigGate.canPush(player)) {
                AcademyCraft.LOGGER.warn("player {} tried to change server config {} without operator permission, denied.",
                        player.getGameProfile().getName(), m.file);
                ConfigPushResultMessage.send(player, m.file, 0, m.paths.size(), "",
                        ServerConfigGate.DENIED);
                return;
            }

            int applied = 0;
            int rejected = 0;
            String badPath = "";
            byte reason = ServerConfigGate.OK;

            for (int i = 0; i < m.paths.size(); i++) {
                byte r = ServerConfigGate.apply(m.file, m.paths.get(i), m.values.get(i));
                if (r == ServerConfigGate.OK) {
                    applied++;
                } else {
                    rejected++;
                    if (reason == ServerConfigGate.OK) {
                        reason = r;
                        badPath = m.paths.get(i);
                    }
                }
            }

            boolean saved = true;
            if (applied > 0) {
                saved = ServerConfigGate.save(m.file);
                if (!saved && reason == ServerConfigGate.OK) {
                    reason = ServerConfigGate.SAVE_FAILED;
                }
            }

            if (applied > 0 && ServerConfigGate.F_ABILITY.equals(m.file)) {
                for (ServerPlayer sp : player.server.getPlayerList().getPlayers()) {

                    if (!cn.lambdalib2.datapart.EntityData.isReady(sp)) {
                        continue;
                    }
                    CPData.get(sp).refreshMaxFromConfig();
                }
            }

            AcademyCraft.LOGGER.info("operator {} changed server config {}: {} entries applied ({}), {} rejected.",
                    player.getGameProfile().getName(), m.file, applied,
                    saved ? "written to file" : "write failed, changes will revert after restart", rejected);

            ConfigPushResultMessage.send(player, m.file, applied, rejected, badPath, reason);
        });
        ctx.setPacketHandled(true);
    }

    public static void send(String file, List<String> paths, List<String> values) {
        ACNetwork.CHANNEL.sendToServer(new ConfigPushMessage(file, paths, values));
    }
}
