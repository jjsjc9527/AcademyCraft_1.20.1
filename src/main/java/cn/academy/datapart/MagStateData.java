package cn.academy.datapart;

import cn.lambdalib2.datapart.DataPart;
import cn.lambdalib2.datapart.EntityData;
import cn.lambdalib2.datapart.RegDataPart;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

@RegDataPart(Player.class)
public class MagStateData extends DataPart<Player> {

    public static final int MODE_NONE = 0, MODE_ANCHOR = 1, MODE_GRAVITY = 2;

    public static MagStateData of(Player player) {
        return EntityData.get(player).getPart(MagStateData.class);
    }

    private int mode = MODE_NONE;
    private double tx, ty, tz;

    private int aux;

    private int syncTimer = 0;

    {
        setTick(true);
        setNBTStorage();
    }

    @Override
    public void tick() {
        if (!isClient() && ++syncTimer >= 20) {
            syncTimer = 0;
            sync();
        }
    }

    @net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "academy")
    public static final class LoginPush {
        @net.minecraftforge.eventbus.api.SubscribeEvent
        public static void onLogin(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e) {
            if (e.getEntity().level().isClientSide) return;
            MagStateData st = MagStateData.of(e.getEntity());
            if (st.getMode() != MODE_NONE) st.sync();
        }
    }

    @Override
    public void onPlayerDead() {
        mode = MODE_NONE;
    }

    public void declare(int mode, double tx, double ty, double tz, int aux) {
        this.mode = mode;
        this.tx = tx; this.ty = ty; this.tz = tz;
        this.aux = aux;
        if (!isClient()) sync();
    }

    public void clearState() {
        if (mode != MODE_NONE) {
            mode = MODE_NONE;
            if (!isClient()) sync();
        }
    }

    public int getMode() {
        return mode;
    }

    public Vec3 getTarget() {
        return new Vec3(tx, ty, tz);
    }

    public int getAux() {
        return aux;
    }

    @Override
    public void toNBT(CompoundTag tag) {
        tag.putInt("mode", mode);
        tag.putDouble("tx", tx);
        tag.putDouble("ty", ty);
        tag.putDouble("tz", tz);
        tag.putInt("aux", aux);
    }

    @Override
    public void fromNBT(CompoundTag tag) {
        mode = tag.getInt("mode");
        tx = tag.getDouble("tx");
        ty = tag.getDouble("ty");
        tz = tag.getDouble("tz");
        aux = tag.getInt("aux");
    }

    @Override
    protected void writeSyncData(FriendlyByteBuf buf) {
        buf.writeVarInt(mode);
        buf.writeDouble(tx);
        buf.writeDouble(ty);
        buf.writeDouble(tz);
        buf.writeVarInt(aux);
    }

    @Override
    protected void readSyncData(FriendlyByteBuf buf) {
        mode = buf.readVarInt();
        tx = buf.readDouble();
        ty = buf.readDouble();
        tz = buf.readDouble();
        aux = buf.readVarInt();
    }
}
