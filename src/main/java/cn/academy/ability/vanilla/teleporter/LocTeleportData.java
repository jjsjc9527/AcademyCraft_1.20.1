package cn.academy.ability.vanilla.teleporter;

import cn.lambdalib2.datapart.DataPart;
import cn.lambdalib2.datapart.EntityData;
import cn.lambdalib2.datapart.RegDataPart;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.LogicalSide;

import java.util.ArrayList;
import java.util.List;

@RegDataPart(Player.class)
public class LocTeleportData extends DataPart<Player> {

    public static LocTeleportData of(Player player) {
        return EntityData.get(player).getPart(LocTeleportData.class);
    }

    private final List<Location> locationList = new ArrayList<>();

    {
        setNBTStorage();
        setClientNeedSync();
    }

    public List<Location> locations() {
        return new ArrayList<>(locationList);
    }

    public void add(String name, String dim, float x, float y, float z) {
        checkSide(LogicalSide.SERVER);
        locationList.add(new Location(name, dim, x, y, z, locationList.size()));
        sync();
    }

    public void remove(int id) {
        checkSide(LogicalSide.SERVER);
        if (id < 0 || id >= locationList.size()) return;
        locationList.remove(id);
        for (int i = 0; i < locationList.size(); i++) {
            locationList.get(i).id = i;
        }
        sync();
    }

    public Location get(int id) {
        return id >= 0 && id < locationList.size() ? locationList.get(id) : null;
    }

    @Override
    protected void writeSyncData(net.minecraft.network.FriendlyByteBuf buf) {
        buf.writeVarInt(locationList.size());
        for (Location l : locationList) {
            buf.writeUtf(l.name);
            buf.writeUtf(l.dim);
            buf.writeFloat(l.x);
            buf.writeFloat(l.y);
            buf.writeFloat(l.z);
        }
    }

    @Override
    protected void readSyncData(net.minecraft.network.FriendlyByteBuf buf) {
        locationList.clear();
        int n = buf.readVarInt();
        for (int i = 0; i < n; i++) {
            locationList.add(new Location(buf.readUtf(), buf.readUtf(),
                    buf.readFloat(), buf.readFloat(), buf.readFloat(), i));
        }
    }

    @Override
    public void toNBT(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Location l : locationList) {
            list.add(l.toNBT());
        }
        tag.put("locations", list);
    }

    @Override
    public void fromNBT(CompoundTag tag) {
        locationList.clear();
        ListTag list = tag.getList("locations", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            locationList.add(Location.fromNBT(list.getCompound(i), i));
        }
    }
}
