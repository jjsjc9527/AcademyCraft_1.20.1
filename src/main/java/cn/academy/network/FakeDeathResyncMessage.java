package cn.academy.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class FakeDeathResyncMessage {

    public FakeDeathResyncMessage() {}

    public static void encode(FakeDeathResyncMessage m, FriendlyByteBuf buf) {}

    public static FakeDeathResyncMessage decode(FriendlyByteBuf buf) {
        return new FakeDeathResyncMessage();
    }

    private static final Map<UUID, Long> LAST_AT = new HashMap<>();
    private static final long MIN_INTERVAL_MS = 1000L;
    private static final int MAX_ENTRIES = 128;

    public static void handle(FakeDeathResyncMessage m, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            ServerPlayer p = ctx.getSender();
            if (p == null) {
                return;
            }

            if (p.getHealth() <= 0.0F) {
                return;
            }
            long now = System.currentTimeMillis();
            Long last = LAST_AT.get(p.getUUID());
            if (last != null && now - last < MIN_INTERVAL_MS) {
                return;
            }
            if (LAST_AT.size() > MAX_ENTRIES) {
                LAST_AT.clear();
            }
            LAST_AT.put(p.getUUID(), now);

            p.resetSentInfo();
            p.connection.send(new ClientboundSetHealthPacket(
                    p.getHealth(), p.getFoodData().getFoodLevel(),
                    p.getFoodData().getSaturationLevel()));
        });
        ctx.setPacketHandled(true);
    }

    public static void send() {
        ACNetwork.CHANNEL.sendToServer(new FakeDeathResyncMessage());
    }
}
