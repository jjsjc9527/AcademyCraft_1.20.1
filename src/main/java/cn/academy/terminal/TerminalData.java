package cn.academy.terminal;

import cn.academy.event.AppInstalledEvent;
import cn.academy.event.TerminalInstalledEvent;
import cn.lambdalib2.datapart.DataPart;
import cn.lambdalib2.datapart.EntityData;
import cn.lambdalib2.datapart.RegDataPart;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.LogicalSide;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RegDataPart(Player.class)
public class TerminalData extends DataPart<Player> {

    private static final String MSG_TERMINAL_INST = "terminal_inst";
    private static final String MSG_APP_INST = "app_inst";

    public static TerminalData get(Player player) {
        return EntityData.get(player).getPart(TerminalData.class);
    }

    private List<Integer> installedNameHashes = new ArrayList<>();
    private boolean isInstalled;

    public TerminalData() {
        setClientNeedSync();
        setNBTStorage();
    }

    public List<App> getInstalledApps() {
        return AppRegistry.enumeration().stream().filter(this::isInstalled).collect(Collectors.toList());
    }

    public boolean isInstalled(App app) {
        return app.isPreInstalled() || installedNameHashes.contains(app.getName().hashCode());
    }

    public boolean isTerminalInstalled() {
        return isInstalled;
    }

    public void install() {
        checkSide(LogicalSide.SERVER);

        if (!isInstalled) {
            isInstalled = true;

            sync();

            informTerminalInstall();
            sendToLocal(MSG_TERMINAL_INST);
        }
    }

    public void installApp(App app) {
        checkSide(LogicalSide.SERVER);

        if (!isInstalled(app)) {
            installedNameHashes.add(app.getName().hashCode());

            sync();

            informAppInstall(app.getName());
            sendToLocal(MSG_APP_INST, app.getName());
        }
    }

    @Override
    public void toNBT(CompoundTag tag) {
        tag.putBoolean("installed", isInstalled);
        int[] hashes = new int[installedNameHashes.size()];
        for (int i = 0; i < hashes.length; i++) hashes[i] = installedNameHashes.get(i);
        tag.putIntArray("apps", hashes);
    }

    @Override
    public void fromNBT(CompoundTag tag) {
        isInstalled = tag.getBoolean("installed");
        installedNameHashes = new ArrayList<>();
        for (int h : tag.getIntArray("apps")) installedNameHashes.add(h);
    }

    @Override
    protected void writeSyncData(FriendlyByteBuf buf) {
        buf.writeBoolean(isInstalled);
        buf.writeVarInt(installedNameHashes.size());
        for (int h : installedNameHashes) buf.writeInt(h);
    }

    @Override
    protected void readSyncData(FriendlyByteBuf buf) {
        isInstalled = buf.readBoolean();
        int n = buf.readVarInt();
        installedNameHashes = new ArrayList<>(n);
        for (int i = 0; i < n; i++) installedNameHashes.add(buf.readInt());
    }

    @Listener(channel = MSG_TERMINAL_INST, side = LogicalSide.CLIENT)
    private void informTerminalInstall() {
        MinecraftForge.EVENT_BUS.post(new TerminalInstalledEvent(getEntity()));
    }

    @Listener(channel = MSG_APP_INST, side = LogicalSide.CLIENT)
    private void informAppInstall(String appName) {
        MinecraftForge.EVENT_BUS.post(new AppInstalledEvent(getEntity(), AppRegistry.getByName(appName)));
    }

}
