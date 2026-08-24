package cn.academy.energy.impl;

import cn.academy.energy.api.block.IWirelessGenerator;
import cn.academy.energy.api.block.IWirelessReceiver;
import cn.academy.event.WirelessUserEvent.UserType;
import cn.academy.event.energy.ChangePassEvent;
import cn.academy.event.energy.CreateNetworkEvent;
import cn.academy.event.energy.DestroyNetworkEvent;
import cn.academy.event.energy.LinkNodeEvent;
import cn.academy.event.energy.LinkUserEvent;
import cn.academy.event.energy.UnlinkNodeEvent;
import cn.academy.event.energy.UnlinkUserEvent;
import cn.academy.energy.impl.VBlocks.VNGenerator;
import cn.academy.energy.impl.VBlocks.VNReceiver;
import cn.academy.energy.impl.VBlocks.VWNode;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

public class WirelessSystem {

    public static final WirelessSystem INSTANCE = new WirelessSystem();

    private WirelessSystem() {}

    public static void bootstrap() {
        MinecraftForge.EVENT_BUS.register(INSTANCE);
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent event) {
        if (event.phase == Phase.START)
            return;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null)
            return;

        for (ServerLevel ws : server.getAllLevels()) {
            WiWorldData data = WiWorldData.getNonCreate(ws);
            if (data != null) {
                data.tick();
            }
        }
    }

    @SubscribeEvent
    public void onCreateNet(CreateNetworkEvent event) {
        WiWorldData data = WiWorldData.get(event.getWorld());
        if (!data.createNetwork(event.mat, event.ssid, event.pwd)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onDestroyNet(DestroyNetworkEvent event) {
        WiWorldData data = WiWorldData.get(event.getWorld());
        WirelessNet net = data.getNetwork(event.mat);
        if (net != null) net.dispose();
    }

    @SubscribeEvent
    public void changePass(ChangePassEvent event) {
        WiWorldData data = WiWorldData.get(event.getWorld());
        WirelessNet net = data.getNetwork(event.mat);
        if (net == null || !net.resetPassword(event.pwd)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void linkNode(LinkNodeEvent event) {
        WiWorldData data = WiWorldData.get(event.getWorld());
        WirelessNet net = data.getNetwork(event.matrix);

        if (net == null || !net.addNode(new VWNode(event.node), event.pwd))
            event.setCanceled(true);
    }

    @SubscribeEvent
    public void unlinkNode(UnlinkNodeEvent event) {
        WiWorldData data = WiWorldData.get(event.getWorld());
        WirelessNet net = data.getNetwork(event.node);

        if (net != null)
            net.removeNode(new VWNode(event.node));
    }

    @SubscribeEvent
    public void linkUser(LinkUserEvent event) {
        WiWorldData data = WiWorldData.get(event.getWorld());
        NodeConn conn = data.getNodeConnection(event.node);

        if (event.needAuth) {
            if (!event.node.getPassword().equals(event.password)) {
                event.setCanceled(true);
                return;
            }
        }

        if (event.type == UserType.GENERATOR) {
            if (!conn.addGenerator(new VNGenerator(event.getAsGenerator())))
                event.setCanceled(true);
        } else {
            if (!conn.addReceiver(new VNReceiver(event.getAsReceiver())))
                event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void unlinkUser(UnlinkUserEvent event) {
        WiWorldData data = WiWorldData.get(event.getWorld());

        if (event.type == UserType.GENERATOR) {
            IWirelessGenerator gen = event.getAsGenerator();
            NodeConn conn = data.getNodeConnection(gen);
            if (conn != null) conn.removeGenerator(new VNGenerator(gen));
        } else {
            IWirelessReceiver rec = event.getAsReceiver();
            NodeConn conn = data.getNodeConnection(rec);
            if (conn != null) conn.removeReceiver(new VNReceiver(rec));
        }
    }
}
