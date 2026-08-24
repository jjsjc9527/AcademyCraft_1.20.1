package cn.academy.network;

import cn.academy.ability.Category;
import cn.academy.ability.CategoryManager;
import cn.academy.ability.Skill;
import cn.academy.ability.develop.DevelopData;
import cn.academy.ability.develop.action.DevelopActionLevel;
import cn.academy.ability.develop.action.DevelopActionReset;
import cn.academy.ability.develop.action.DevelopActionSkill;
import cn.academy.block.tileentity.DeveloperBlockEntity;
import cn.academy.energy.api.WirelessHelper;
import cn.academy.energy.impl.NodeConn;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DeveloperActionMessage {

    public static final int START_LEVEL = 0;
    public static final int START_SKILL = 1;
    public static final int RESET = 2;
    public static final int GET_NODE = 3;

    public static final int UNUSE = 4;

    private final BlockPos pos;

    private final boolean portable;
    private final int action;

    private final int catId;
    private final int skillId;

    public DeveloperActionMessage(BlockPos pos, boolean portable, int action, int catId, int skillId) {
        this.pos = pos;
        this.portable = portable;
        this.action = action;
        this.catId = catId;
        this.skillId = skillId;
    }

    public static void encode(DeveloperActionMessage m, FriendlyByteBuf buf) {
        buf.writeBlockPos(m.pos);
        buf.writeBoolean(m.portable);
        buf.writeByte(m.action);
        buf.writeByte(m.catId);
        buf.writeByte(m.skillId);
    }

    public static DeveloperActionMessage decode(FriendlyByteBuf buf) {
        return new DeveloperActionMessage(
                buf.readBlockPos(), buf.readBoolean(), buf.readByte(), buf.readByte(), buf.readByte());
    }

    public static void handle(DeveloperActionMessage m, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            if (m.portable) {
                cn.academy.ability.develop.PortableDevData dev =
                        cn.academy.ability.develop.PortableDevData.get(player);
                DevelopData data = DevelopData.get(player);
                switch (m.action) {
                    case START_LEVEL -> data.startDeveloping(dev, new DevelopActionLevel());
                    case START_SKILL -> {
                        Skill skill = lookupSkill(m.catId, m.skillId);
                        if (skill != null) data.startDeveloping(dev, new DevelopActionSkill(skill));
                    }
                    case RESET -> data.startDeveloping(dev, new DevelopActionReset());

                    default -> { }
                }
                return;
            }

            if (player.distanceToSqr(m.pos.getX() + 0.5, m.pos.getY() + 0.5, m.pos.getZ() + 0.5) > 64.0) return;
            if (!(player.level().getBlockEntity(m.pos) instanceof DeveloperBlockEntity dev)) return;

            DevelopData data = DevelopData.get(player);
            switch (m.action) {

                case START_LEVEL -> data.startDeveloping(dev, new DevelopActionLevel());

                case START_SKILL -> {
                    Skill skill = lookupSkill(m.catId, m.skillId);
                    if (skill != null) {
                        data.startDeveloping(dev, new DevelopActionSkill(skill));
                    }
                }

                case RESET -> data.startDeveloping(dev, new DevelopActionReset());

                case GET_NODE -> {
                    NodeConn conn = WirelessHelper.getNodeConn(dev);
                    DeveloperInfoMessage.send(player, conn == null ? null : conn.getNode().getNodeName());
                }

                case UNUSE -> dev.unuse(player);
                default -> { }
            }
        });
        ctx.setPacketHandled(true);
    }

    private static Skill lookupSkill(int catId, int skillId) {
        Category c = CategoryManager.INSTANCE.getCategory(catId);
        return c == null ? null : c.getSkill(skillId);
    }

    private static BlockPos posOrZero(boolean portable, BlockPos pos) {
        return portable ? BlockPos.ZERO : pos;
    }

    public static void sendStartLevel(BlockPos pos, boolean portable) {
        ACNetwork.CHANNEL.sendToServer(
                new DeveloperActionMessage(posOrZero(portable, pos), portable, START_LEVEL, 0, 0));
    }

    public static void sendStartSkill(BlockPos pos, boolean portable, Skill skill) {
        ACNetwork.CHANNEL.sendToServer(new DeveloperActionMessage(
                posOrZero(portable, pos), portable, START_SKILL,
                skill.getCategory().getCategoryID(), skill.getID()));
    }

    public static void sendReset(BlockPos pos, boolean portable) {
        ACNetwork.CHANNEL.sendToServer(
                new DeveloperActionMessage(posOrZero(portable, pos), portable, RESET, 0, 0));
    }

    public static void sendGetNode(BlockPos pos) {
        ACNetwork.CHANNEL.sendToServer(new DeveloperActionMessage(pos, false, GET_NODE, 0, 0));
    }

    public static void sendUnuse(BlockPos pos) {
        ACNetwork.CHANNEL.sendToServer(new DeveloperActionMessage(pos, false, UNUSE, 0, 0));
    }
}
