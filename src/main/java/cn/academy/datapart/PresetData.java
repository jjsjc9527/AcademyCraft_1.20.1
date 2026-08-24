package cn.academy.datapart;

import cn.academy.ability.Category;
import cn.academy.ability.CategoryManager;
import cn.academy.ability.Controllable;
import cn.academy.event.ability.CategoryChangeEvent;
import cn.academy.event.ability.PresetUpdateEvent;
import cn.lambdalib2.datapart.DataPart;
import cn.lambdalib2.datapart.EntityData;
import cn.lambdalib2.datapart.RegDataPart;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.util.SideUtils;
import com.google.common.base.Preconditions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;

import java.util.Arrays;

@RegDataPart(Player.class)
public class PresetData extends DataPart<Player> {

    public static final int MAX_KEYS = 4;

    public static final int MAX_PRESETS = 4;

    private static final String
            MSG_SYNC_SWITCH = "switch",
            MSG_SYNC_UPDATE = "update";

    int presetID = 0;
    Preset[] presets = new Preset[MAX_PRESETS];

    public PresetData() {
        for (int i = 0; i < MAX_PRESETS; ++i) {
            presets[i] = new Preset();
        }

        setNBTStorage();
        setClientNeedSync();
    }

    public void clear() {
        checkSide(LogicalSide.SERVER);

        for (int i = 0; i < MAX_PRESETS; ++i)
            presets[i] = new Preset();

        presetID = 0;

        sync();
    }

    public void setPreset(int id, Preset p) {
        checkSide(LogicalSide.SERVER);

        presets[id] = p;
        sync();
    }

    public void switchCurrent(int nid) {
        Preconditions.checkElementIndex(nid, MAX_PRESETS);
        checkSide(LogicalSide.SERVER);

        presetID = nid;
        sync();
    }

    public void switchFromClient(int id) {
        Preconditions.checkElementIndex(id, MAX_PRESETS);
        checkSide(LogicalSide.CLIENT);

        presetID = id;
        sendMessage(MSG_SYNC_SWITCH, id);
    }

    public void setPresetFromClient(int id, Preset p) {
        checkSide(LogicalSide.CLIENT);

        presets[id] = p;
        sendMessage(MSG_SYNC_UPDATE, id, p);
        firePresetUpdate();
    }

    public Preset getPreset(int id) {
        return presets[id];
    }

    public int getCurrentID() {
        return presetID;
    }

    public Preset getCurrentPreset() {
        return presets[presetID];
    }

    @Override
    public void toNBT(CompoundTag tag) {
        tag.putByte("cur", (byte) presetID);
        for (int i = 0; i < MAX_PRESETS; ++i) {
            tag.put("p" + i, writePresetNBT(presets[i]));
        }
    }

    @Override
    public void fromNBT(CompoundTag tag) {
        presetID = tag.getByte("cur");
        if (presetID < 0 || presetID >= MAX_PRESETS) presetID = 0;
        for (int i = 0; i < MAX_PRESETS; ++i) {
            presets[i] = tag.contains("p" + i) ? readPresetNBT(tag.getCompound("p" + i)) : new Preset();
        }
    }

    private static CompoundTag writePresetNBT(Preset p) {
        CompoundTag tag = new CompoundTag();
        for (int i = 0; i < MAX_KEYS; ++i) {
            Controllable c = p.data[i];
            if (c != null) {
                tag.putString(i + "n", c.getCategory().getName());
                tag.putByte(i + "c", (byte) c.getControlID());

                tag.putByteArray(String.valueOf(i),
                        new byte[]{(byte) c.getCategory().getCategoryID(), (byte) c.getControlID()});
            }
        }
        return tag;
    }

    private static Preset readPresetNBT(CompoundTag tag) {
        Controllable[] data = new Controllable[MAX_KEYS];
        for (int i = 0; i < MAX_KEYS; ++i) {
            if (tag.contains(i + "n")) {
                Category cat = CategoryManager.INSTANCE.getCategory(tag.getString(i + "n"));
                data[i] = cat == null ? null : cat.getControllable(tag.getByte(i + "c"));
                continue;
            }
            String k = String.valueOf(i);
            if (tag.contains(k)) {
                byte[] b = tag.getByteArray(k);
                if (b.length >= 2) {

                    Category cat = CategoryManager.INSTANCE.getCategory(b[0]);
                    data[i] = cat == null ? null : cat.getControllable(b[1]);
                }
            }
        }
        return new Preset(data);
    }

    @Override
    protected void writeSyncData(FriendlyByteBuf buf) {
        buf.writeByte(presetID);
        for (int i = 0; i < MAX_PRESETS; ++i) {
            writePresetBuf(buf, presets[i]);
        }
    }

    @Override
    protected void readSyncData(FriendlyByteBuf buf) {
        presetID = buf.readByte();
        for (int i = 0; i < MAX_PRESETS; ++i) {
            presets[i] = readPresetBuf(buf);
        }
    }

    public static void writePresetBuf(FriendlyByteBuf buf, Preset p) {
        int count = 0;
        for (int i = 0; i < MAX_KEYS; ++i) if (p.hasMapping(i)) count++;
        buf.writeByte(count);

        for (int i = 0; i < MAX_KEYS; ++i) {
            if (p.hasMapping(i)) {
                Controllable c = p.getControllable(i);
                buf.writeByte(i);
                buf.writeByte(c.getCategory().getCategoryID());
                buf.writeByte(c.getControlID());
            }
        }
    }

    public static Preset readPresetBuf(FriendlyByteBuf buf) {
        Preset ret = new Preset();
        int count = buf.readByte();
        while (count-- > 0) {
            int id = buf.readByte();
            int catID = buf.readByte();
            int ctrlID = buf.readByte();
            Category cat = CategoryManager.INSTANCE.getCategory(catID);
            if (cat != null && id >= 0 && id < MAX_KEYS) {
                ret.data[id] = cat.getControllable(ctrlID);
            }
        }
        return ret;
    }

    @Listener(channel = MSG_SYNC_SWITCH, side = LogicalSide.SERVER)
    private void handleSwitch(int idx) {
        switchCurrent(idx);
    }

    @Listener(channel = MSG_SYNC_UPDATE, side = LogicalSide.SERVER)
    private void handleSet(int idx, Preset mapping) {
        setPreset(idx, mapping);
        firePresetUpdate();
    }

    @Override
    protected void onSynchronized() {
        debug("OnSynchronized " + isClient() + " " + getCurrentPreset());
        firePresetUpdate();
    }

    private void firePresetUpdate() {
        MinecraftForge.EVENT_BUS.post(new PresetUpdateEvent(getEntity()));
    }

    public static PresetData get(Player player) {
        return EntityData.get(player).getPart(PresetData.class);
    }

    public static class Preset {

        final Controllable[] data;

        public Preset(Controllable[] _data) {
            data = Arrays.copyOf(_data, MAX_KEYS);
        }

        public Preset() {
            data = new Controllable[MAX_KEYS];
            Arrays.fill(data, null);
        }

        public boolean hasMapping(int key) {
            return getControllable(key) != null;
        }

        public Controllable getControllable(int key) {
            return key >= data.length ? null : data[key];
        }

        public boolean hasControllable(Controllable c) {
            for (Controllable cc : data) {
                if (cc == c) {
                    return true;
                }
            }
            return false;
        }

        public Controllable[] copyData() {
            return Arrays.copyOf(data, MAX_KEYS);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("Preset{");
            for (int i = 0; i < data.length; ++i) {
                if (i > 0) sb.append(", ");
                sb.append('#').append(i).append('=').append(data[i]);
            }
            return sb.append('}').toString();
        }

    }

    public static class Events {

        @SubscribeEvent
        public void onCategoryChanged(CategoryChangeEvent event) {
            if (!SideUtils.isClient()) {
                PresetData.get(event.player).clear();
            }
        }

    }

}
