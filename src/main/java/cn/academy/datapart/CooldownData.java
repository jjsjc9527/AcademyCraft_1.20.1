package cn.academy.datapart;

import cn.academy.ability.Controllable;
import cn.lambdalib2.datapart.DataPart;
import cn.lambdalib2.datapart.EntityData;
import cn.lambdalib2.datapart.RegDataPart;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.util.TickScheduler;
import com.google.common.base.Preconditions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.LogicalSide;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;

@RegDataPart(Player.class)
public class CooldownData extends DataPart<Player> {

    public static CooldownData of(Player player) {
        return EntityData.get(player).getPart(CooldownData.class);
    }

    private static final SkillCooldown EMPTY_COOLDOWN = new SkillCooldown(100, 0);

    private final Map<Integer, SkillCooldown> cooldownMap = new HashMap<>();
    private final TickScheduler scheduler = new TickScheduler();

    private boolean noCooldown = false;

    {
        setTick(true);

        setNBTStorage();

        scheduler.everyTick().run(() -> {
            for (Iterator<SkillCooldown> itr = cooldownMap.values().iterator(); itr.hasNext(); ) {
                SkillCooldown cd = itr.next();
                --cd.tickLeft;
                if (cd.tickLeft <= 0) {
                    itr.remove();
                }
            }
        });

        scheduler.every(15).atOnly(LogicalSide.SERVER).run(this::sync);
    }

    @Override
    public void tick() {
        scheduler.runTick();
    }

    @Override
    public void onPlayerDead() {
        cooldownMap.clear();
    }

    @Override
    public void toNBT(net.minecraft.nbt.CompoundTag tag) {
        int[] arr = new int[cooldownMap.size() * 3];
        int i = 0;
        for (Map.Entry<Integer, SkillCooldown> e : cooldownMap.entrySet()) {
            arr[i++] = e.getKey();
            arr[i++] = e.getValue().maxTick;
            arr[i++] = e.getValue().tickLeft;
        }
        tag.putIntArray("cds", arr);
    }

    @Override
    public void fromNBT(net.minecraft.nbt.CompoundTag tag) {
        cooldownMap.clear();
        int[] arr = tag.getIntArray("cds");
        for (int i = 0; i + 2 < arr.length; i += 3) {
            if (arr[i + 2] > 0) {
                cooldownMap.put(arr[i], new SkillCooldown(arr[i + 1], arr[i + 2]));
            }
        }
    }

    @Override
    protected void writeSyncData(FriendlyByteBuf buf) {
        buf.writeVarInt(cooldownMap.size());
        for (Map.Entry<Integer, SkillCooldown> e : cooldownMap.entrySet()) {
            buf.writeInt(e.getKey());
            buf.writeShort(e.getValue().maxTick);
            buf.writeShort(e.getValue().tickLeft);
        }
    }

    @Override
    protected void readSyncData(FriendlyByteBuf buf) {
        cooldownMap.clear();
        int n = buf.readVarInt();
        for (int i = 0; i < n; i++) {
            int key = buf.readInt();
            int maxTick = buf.readShort();
            int tickLeft = buf.readShort();
            cooldownMap.put(key, new SkillCooldown(maxTick, tickLeft));
        }
    }

    public void set(Controllable ctrl, int cd) {
        setSub(ctrl, 0, cd);
    }

    public void setSub(Controllable ctrl, int id, int cd) {
        Preconditions.checkArgument(id >= 0);
        if (noCooldown) return;
        int controlId = ctrl.getControlID();
        doSet(controlId, id, cd);

        if (isClient()) {
            sendMessage("cross", controlId, id, cd);
        } else {
            sendToLocal("cross", controlId, id, cd);
        }
    }

    public boolean isInCooldown(Controllable ctrl) {
        return isInCooldown(ctrl, 0);
    }

    public boolean isInCooldown(Controllable ctrl, int id) {
        return getSub(ctrl, id) != EMPTY_COOLDOWN;
    }

    public SkillCooldown get(Controllable ctrl) {
        return getSub(ctrl, 0);
    }

    public SkillCooldown getSub(Controllable ctrl, int id) {
        return getSubById(ctrl.getControlID(), id);
    }

    public void clear() {
        cooldownMap.clear();
    }

    public boolean isNoCooldown() {
        return noCooldown;
    }

    public void setNoCooldown(boolean on) {
        this.noCooldown = on;
        if (on) {
            clear();
        }
    }

    private SkillCooldown getSubById(int controlId, int id) {
        int sid = toID(controlId, id);
        return cooldownMap.getOrDefault(sid, EMPTY_COOLDOWN);
    }

    private void doSet(int controlId, int id, int cd) {
        SkillCooldown data = getSubById(controlId, id);
        if (data == EMPTY_COOLDOWN) {
            cooldownMap.put(toID(controlId, id), new SkillCooldown(cd, cd));
        } else {
            data.maxTick = Math.max(cd, data.maxTick);
            data.tickLeft = Math.max(cd, data.tickLeft);
        }
    }

    private int toID(int controlId, int id) {
        return controlId << 2 + id;
    }

    @Listener(channel = "cross", side = {LogicalSide.CLIENT, LogicalSide.SERVER})
    private void hCrossSet(int controlId, int id, int cd) {
        doSet(controlId, id, cd);
    }

    public static class SkillCooldown {
        private int tickLeft;
        private int maxTick;

        private SkillCooldown(int maxTick, int tickLeft) {
            checkArgument(maxTick >= 0);
            this.maxTick = maxTick;
            this.tickLeft = tickLeft;
        }

        public int getTickLeft() {
            return tickLeft;
        }

        public int getMaxTick() {
            return maxTick;
        }
    }

    public static class Events {

        @net.minecraftforge.eventbus.api.SubscribeEvent
        public void onCategoryChange(cn.academy.event.ability.CategoryChangeEvent evt) {
            if (!evt.player.level().isClientSide) {
                CooldownData.of(evt.player).clear();
            }
        }

    }
}
