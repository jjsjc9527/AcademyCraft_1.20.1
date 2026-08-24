package cn.academy.network;

import cn.academy.AcademyCraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ACNetwork {

    private static final String VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(AcademyCraft.MODID, "main"),
            () -> VERSION,
            VERSION::equals,
            VERSION::equals);

    private static int nextId = 0;

    private ACNetwork() {}

    public static void register() {

        CHANNEL.registerMessage(nextId++, MsgRpc.class,
                MsgRpc::encode, MsgRpc::decode, MsgRpc::handle);

        toServer(SafeSpotMessage.class,
                SafeSpotMessage::encode, SafeSpotMessage::decode, SafeSpotMessage::handle);

        toServer(MatrixActionMessage.class,
                MatrixActionMessage::encode, MatrixActionMessage::decode, MatrixActionMessage::handle);

        toClient(MatrixInfoMessage.class,
                MatrixInfoMessage::encode, MatrixInfoMessage::decode, MatrixInfoMessage::handle);

        toServer(WirelessActionMessage.class,
                WirelessActionMessage::encode, WirelessActionMessage::decode, WirelessActionMessage::handle);

        toClient(WirelessInfoMessage.class,
                WirelessInfoMessage::encode, WirelessInfoMessage::decode, WirelessInfoMessage::handle);

        toServer(NodeActionMessage.class,
                NodeActionMessage::encode, NodeActionMessage::decode, NodeActionMessage::handle);

        toClient(NodeInfoMessage.class,
                NodeInfoMessage::encode, NodeInfoMessage::decode, NodeInfoMessage::handle);

        toServer(MetalFormerActionMessage.class,
                MetalFormerActionMessage::encode, MetalFormerActionMessage::decode,
                MetalFormerActionMessage::handle);

        toServer(DeveloperActionMessage.class,
                DeveloperActionMessage::encode, DeveloperActionMessage::decode,
                DeveloperActionMessage::handle);

        toClient(DeveloperInfoMessage.class,
                DeveloperInfoMessage::encode, DeveloperInfoMessage::decode,
                DeveloperInfoMessage::handle);

        toClient(DeveloperOpenMessage.class,
                DeveloperOpenMessage::encode, DeveloperOpenMessage::decode,
                DeveloperOpenMessage::handle);

        toClient(TerminalInstallMessage.class,
                TerminalInstallMessage::encode, TerminalInstallMessage::decode,
                TerminalInstallMessage::handle);

        toClient(GravitySyncMessage.class,
                GravitySyncMessage::encode, GravitySyncMessage::decode,
                GravitySyncMessage::handle);

        toServer(FreqTransmitterActionMessage.class,
                FreqTransmitterActionMessage::encode, FreqTransmitterActionMessage::decode,
                FreqTransmitterActionMessage::handle);

        toClient(FreqTransmitterResultMessage.class,
                FreqTransmitterResultMessage::encode, FreqTransmitterResultMessage::decode,
                FreqTransmitterResultMessage::handle);

        toServer(InterfererActionMessage.class,
                InterfererActionMessage::encode, InterfererActionMessage::decode,
                InterfererActionMessage::handle);

        toClient(InterfererInfoMessage.class,
                InterfererInfoMessage::encode, InterfererInfoMessage::decode,
                InterfererInfoMessage::handle);

        toClient(CoinThrowMessage.class,
                CoinThrowMessage::encode, CoinThrowMessage::decode,
                CoinThrowMessage::handle);

        toClient(GroundHeaveMessage.class,
                GroundHeaveMessage::encode, GroundHeaveMessage::decode,
                GroundHeaveMessage::handle);

        toServer(ConfigPushMessage.class,
                ConfigPushMessage::encode, ConfigPushMessage::decode,
                ConfigPushMessage::handle);
        toClient(ConfigPushResultMessage.class,
                ConfigPushResultMessage::encode, ConfigPushResultMessage::decode,
                ConfigPushResultMessage::handle);

        toClient(CogMarkMessage.class,
                CogMarkMessage::encode, CogMarkMessage::decode,
                CogMarkMessage::handle);

        toClient(DeathScreenReleaseMessage.class,
                DeathScreenReleaseMessage::encode, DeathScreenReleaseMessage::decode,
                DeathScreenReleaseMessage::handle);

        toServer(FakeDeathResyncMessage.class,
                FakeDeathResyncMessage::encode, FakeDeathResyncMessage::decode,
                FakeDeathResyncMessage::handle);
    }

    private static <MSG> void toServer(Class<MSG> type,
                                       BiConsumer<MSG, FriendlyByteBuf> encoder,
                                       Function<FriendlyByteBuf, MSG> decoder,
                                       BiConsumer<MSG, Supplier<NetworkEvent.Context>> handler) {
        CHANNEL.registerMessage(nextId++, type, encoder, decoder, handler,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    private static <MSG> void toClient(Class<MSG> type,
                                       BiConsumer<MSG, FriendlyByteBuf> encoder,
                                       Function<FriendlyByteBuf, MSG> decoder,
                                       BiConsumer<MSG, Supplier<NetworkEvent.Context>> handler) {
        CHANNEL.registerMessage(nextId++, type, encoder, decoder, handler,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void sendRpcToServer(FriendlyByteBuf payload) {
        CHANNEL.sendToServer(new MsgRpc(toBytes(payload)));
    }

    public static void sendRpcToPlayer(FriendlyByteBuf payload, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new MsgRpc(toBytes(payload)));
    }

    public static void sendRpcToTracking(FriendlyByteBuf payload, net.minecraft.world.entity.Entity entity) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), new MsgRpc(toBytes(payload)));
    }

    private static byte[] toBytes(FriendlyByteBuf buf) {
        byte[] arr = new byte[buf.readableBytes()];
        buf.readBytes(arr);
        return arr;
    }
}
