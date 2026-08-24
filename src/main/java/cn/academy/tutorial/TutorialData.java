package cn.academy.tutorial;

import cn.academy.ACItems;
import cn.academy.event.TutorialActivatedEvent;
import cn.lambdalib2.datapart.DataPart;
import cn.lambdalib2.datapart.EntityData;
import cn.lambdalib2.datapart.RegDataPart;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.util.RandUtils;
import cn.lambdalib2.util.TickScheduler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.LogicalSide;

import java.util.BitSet;
import java.util.HashSet;

@RegDataPart(Player.class)
public class TutorialData extends DataPart<Player> {

    private static final boolean GIVE_ON_FIRST_SPAWN = true;

    private static final String MSG_ACTIVATE = "activate";

    public static TutorialData get(Player player) {
        return EntityData.get(player).getPart(TutorialData.class);
    }

    private BitSet savedConditions = new BitSet();
    private HashSet<String> activatedTuts = new HashSet<>();
    private boolean tutorialAcquired = false;
    private int misakaID = -1;

    private final TickScheduler scheduler = new TickScheduler();
    private boolean dirty;

    public TutorialData() {
        setTick(true);
        setClientNeedSync();
        setNBTStorage();

        misakaID = RandUtils.rangei(1000, 19000);

        scheduler.every(3).atOnly(LogicalSide.SERVER).run(() -> {
            if (dirty) {
                dirty = false;
                TutorialRegistry.enumeration().forEach(tut -> {
                    if (!activatedTuts.contains(tut.id) &&
                            tut.isActivated(getEntity()) &&
                            !tut.isDefaultInstalled()) {
                        activatedTuts.add(tut.id);

                        postActivate(tut.id);
                        sendToLocal(MSG_ACTIVATE, tut.id);
                    }
                });
                sync();
            }
        });

        if (GIVE_ON_FIRST_SPAWN) {
            scheduler.every(10).atOnly(LogicalSide.SERVER).run(() -> {
                if (!tutorialAcquired) {
                    Player player = getEntity();
                    ItemEntity ent = new ItemEntity(player.level(),
                            player.getX(), player.getY() + 1.0, player.getZ(),
                            new ItemStack(ACItems.TUTORIAL.get()));
                    player.level().addFreshEntity(ent);

                    tutorialAcquired = true;
                }
            });
        }
    }

    @Override
    public void tick() {
        scheduler.runTick();
    }

    public int getMisakaID() {
        return misakaID;
    }

    boolean isCondActivate(int index) {
        return savedConditions.get(index);
    }

    void setCondActivate(int index) {
        checkSide(LogicalSide.SERVER);

        if (!savedConditions.get(index)) {
            savedConditions.set(index);
            dirty = true;
        }
    }

    private void postActivate(String tutName) {
        ACTutorial tut = TutorialRegistry.getTutorial(tutName);
        MinecraftForge.EVENT_BUS.post(new TutorialActivatedEvent(getEntity(), tut));
    }

    @Listener(channel = MSG_ACTIVATE, side = LogicalSide.CLIENT)
    private void onActivateClient(String tutName) {
        postActivate(tutName);
    }

    @Override
    public void toNBT(CompoundTag tag) {
        tag.putByteArray("conds", savedConditions.toByteArray());
        ListTag list = new ListTag();
        for (String s : activatedTuts) list.add(StringTag.valueOf(s));
        tag.put("activated", list);
        tag.putBoolean("acquired", tutorialAcquired);
        tag.putInt("misakaID", misakaID);
    }

    @Override
    public void fromNBT(CompoundTag tag) {
        savedConditions = BitSet.valueOf(tag.getByteArray("conds"));
        activatedTuts = new HashSet<>();
        ListTag list = tag.getList("activated", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) activatedTuts.add(list.getString(i));
        tutorialAcquired = tag.getBoolean("acquired");
        misakaID = tag.getInt("misakaID");
    }

    @Override
    protected void writeSyncData(FriendlyByteBuf buf) {
        byte[] c = savedConditions.toByteArray();
        buf.writeVarInt(c.length);
        buf.writeBytes(c);
        buf.writeVarInt(activatedTuts.size());
        for (String s : activatedTuts) buf.writeUtf(s);
        buf.writeBoolean(tutorialAcquired);
        buf.writeInt(misakaID);
    }

    @Override
    protected void readSyncData(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        byte[] c = new byte[n];
        buf.readBytes(c);
        savedConditions = BitSet.valueOf(c);
        int m = buf.readVarInt();
        activatedTuts = new HashSet<>();
        for (int i = 0; i < m; i++) activatedTuts.add(buf.readUtf());
        tutorialAcquired = buf.readBoolean();
        misakaID = buf.readInt();
    }

}
