package cn.academy.ability.develop;

import cn.academy.ability.develop.action.IDevelopAction;
import cn.lambdalib2.datapart.DataPart;
import cn.lambdalib2.datapart.EntityData;
import cn.lambdalib2.datapart.RegDataPart;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.LogicalSide;
import org.jetbrains.annotations.Nullable;

@RegDataPart(Player.class)
public class DevelopData extends DataPart<Player> {

    public static DevelopData get(Player player) {
        return EntityData.get(player).getPart(DevelopData.class);
    }

    public enum DevState {
        IDLE, FAILED, DEVELOPING, DONE;

        static DevState byOrdinal(int i) {
            DevState[] v = values();
            return (i >= 0 && i < v.length) ? v[i] : IDLE;
        }
    }

    private boolean dirty = false;

    @Nullable
    private IDeveloper developer;

    @Nullable
    private DeveloperType devType;

    @Nullable
    private IDevelopAction action;

    private int stim;
    private int maxStim;
    private DevState state = DevState.IDLE;

    private int tickThisStim;
    private int tickSync;

    public DevelopData() {
        setTick(true);
        setClientNeedSync();
    }

    public void startDeveloping(IDeveloper developer, IDevelopAction action) {
        checkSide(LogicalSide.SERVER);

        resetProgress(false);
        this.developer = developer;
        this.devType = developer.getDeveloperType();
        this.action = action;
        this.state = DevState.DEVELOPING;
        this.maxStim = action.getStimulations(getEntity());
        this.dirty = true;
    }

    public boolean isDeveloping() {
        return devType != null;
    }

    public double getDevelopProgress() {
        if (isDeveloping()) {
            return (double) stim / maxStim + (double) tickThisStim / devType.getTPS();
        }
        return 0;
    }

    @Nullable
    public IDevelopAction getDevelopType() {
        return action;
    }

    public int getStim() {
        return stim;
    }

    public DevState getState() {
        return state;
    }

    public int getMaxStim() {
        return maxStim;
    }

    public void abort() {
        checkSide(LogicalSide.SERVER);
        if (state == DevState.DEVELOPING) {
            resetProgress(true);
        }
    }

    public void reset() {
        resetProgress(false);
    }

    private void resetProgress(boolean failed) {
        developer = null;
        devType = null;
        action = null;
        tickSync = 5;
        stim = maxStim = tickThisStim = 0;
        state = failed ? DevState.FAILED : DevState.IDLE;
        dirty = true;
    }

    @Override
    public void tick() {
        if (isClient()) return;

        Player player = getEntity();

        if (dirty) {
            dirty = false;
            sync();
        }

        if (!isDeveloping()) return;

        if (tickSync-- == 0) {
            tickSync = 5;
            sync();
        }

        double consume = devType.getCPS() / devType.getTPS();
        if (!developer.tryPullEnergy(consume)) {
            resetProgress(true);
            return;
        }

        if (++tickThisStim > devType.getTPS()) {
            tickThisStim = 0;
            ++stim;

            if (stim >= maxStim) {

                boolean success = action.validate(player, developer);
                if (success) {
                    action.onLearned(player);
                }
                resetProgress(!success);
                if (success) {
                    state = DevState.DONE;
                }
            }
        }
    }

    @Override
    protected void writeSyncData(FriendlyByteBuf buf) {
        buf.writeByte(devType == null ? -1 : devType.ordinal());
        buf.writeVarInt(stim);
        buf.writeVarInt(maxStim);
        buf.writeByte(state.ordinal());
    }

    @Override
    protected void readSyncData(FriendlyByteBuf buf) {
        int t = buf.readByte();
        devType = t < 0 ? null : DeveloperType.values()[t];
        stim = buf.readVarInt();
        maxStim = buf.readVarInt();
        state = DevState.byOrdinal(buf.readByte());
    }
}
