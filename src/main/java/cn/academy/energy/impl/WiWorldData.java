package cn.academy.energy.impl;

import cn.academy.AcademyCraft;
import cn.academy.energy.api.block.IWirelessGenerator;
import cn.academy.energy.api.block.IWirelessMatrix;
import cn.academy.energy.api.block.IWirelessNode;
import cn.academy.energy.api.block.IWirelessReceiver;
import cn.academy.energy.api.block.IWirelessUser;
import cn.academy.energy.impl.VBlocks.VNGenerator;
import cn.academy.energy.impl.VBlocks.VNNode;
import cn.academy.energy.impl.VBlocks.VNReceiver;
import cn.academy.energy.impl.VBlocks.VWMatrix;
import cn.academy.energy.impl.VBlocks.VWNode;
import cn.lambdalib2.util.IBlockSelector;
import cn.lambdalib2.util.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WiWorldData extends SavedData {

    public static final String ID = "ac_wen";

    Level world;

    private final IBlockSelector filterWirelessBlocks = (world, x, y, z, block) -> {
        BlockEntity te = world.getBlockEntity(new BlockPos(x, y, z));
        return te instanceof IWirelessMatrix || te instanceof IWirelessNode;
    };

    Map<Object, WirelessNet> netLookup = new HashMap<>();
    Set<WirelessNet> netList = new HashSet<>();
    private List<WirelessNet> toRemove = new ArrayList<>();

    Map<Object, NodeConn> nodeLookup = new HashMap<>();
    Set<NodeConn> nodeList = new HashSet<>();
    List<NodeConn> nToRemove = new ArrayList<>();

    private final List<BlockPos> _bufferBlockPos = new ArrayList<>();

    public WiWorldData() {}

    public static WiWorldData load(CompoundTag tag) {
        WiWorldData d = new WiWorldData();
        d.readFromNBT(tag);
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        writeToNBT(tag);
        return tag;
    }

    public static WiWorldData get(Level world) {
        if (world.isClientSide()) {
            throw new RuntimeException("Not allowed to create WiWorldData in client");
        }
        ServerLevel sl = (ServerLevel) world;
        WiWorldData ret = sl.getDataStorage().computeIfAbsent(WiWorldData::load, WiWorldData::new, ID);
        ret.world = world;
        return ret;
    }

    public static WiWorldData getNonCreate(Level world) {
        if (world.isClientSide()) {
            return null;
        }
        ServerLevel sl = (ServerLevel) world;
        WiWorldData data = sl.getDataStorage().get(WiWorldData::load, ID);
        if (data != null) data.world = world;
        return data;
    }

    private void tickNetwork() {
        for (WirelessNet net : toRemove) {
            this.doRemoveNetwork(net);
        }
        toRemove.clear();

        Iterator<WirelessNet> iter = netList.iterator();
        while (iter.hasNext()) {
            WirelessNet net = iter.next();
            if (net.isDisposed()) {
                toRemove.add(net);
            } else {
                net.world = world;
                net.tick();
            }
        }
    }

    boolean createNetwork(IWirelessMatrix matrix, String ssid, String password) {
        VWMatrix vm = new VWMatrix(matrix);
        if (netLookup.containsKey(vm)) {
            WirelessNet old = netLookup.get(vm);
            doRemoveNetwork(old);
        }

        WirelessNet net = new WirelessNet(this, vm, ssid, password);
        doAddNetwork(net);

        return true;
    }

    public Collection<WirelessNet> rangeSearch(int x, int y, int z, double range, int max) {
        WorldUtils.getBlocksWithin(_bufferBlockPos, world, x, y, z, range, max, filterWirelessBlocks);

        Set<WirelessNet> set = new HashSet<>();
        for (BlockPos bp : _bufferBlockPos) {
            BlockEntity te = world.getBlockEntity(bp);
            WirelessNet net;
            if (te instanceof IWirelessMatrix) {
                net = getNetwork((IWirelessMatrix) te);
            } else if (te instanceof IWirelessNode) {
                net = getNetwork((IWirelessNode) te);
            } else {
                throw new RuntimeException("Invalid BlockEntity");
            }
            if (net != null && net.isInRange(x, y, z) && net.getLoad() < net.getCapacity()) {
                set.add(net);
                if (set.size() >= max)
                    return set;
            }
        }

        return set;
    }

    public WirelessNet getNetwork(IWirelessMatrix matrix) {
        return privateGetNetwork(new VWMatrix(matrix));
    }

    public WirelessNet getNetwork(IWirelessNode node) {
        return privateGetNetwork(new VWNode(node));
    }

    private WirelessNet privateGetNetwork(Object key) {
        WirelessNet ret = netLookup.get(key);
        if (ret != null) {

            ret.world = world;
            if (ret.validate()) {
                return ret;
            }
        }
        return null;
    }

    private void doRemoveNetwork(WirelessNet net) {
        netList.remove(net);
        net.onCleanup(this);
    }

    private void doAddNetwork(WirelessNet net) {
        netList.add(net);
        net.onCreate(this);
    }

    private void loadNetwork(CompoundTag tag) {
        ListTag list = tag.getList("networks", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); ++i) {
            CompoundTag tag2 = list.getCompound(i);
            WirelessNet net = new WirelessNet(this, tag2);
            doAddNetwork(net);
        }
    }

    private void saveNetwork(CompoundTag tag) {
        ListTag list = new ListTag();
        for (WirelessNet net : netList) {
            if (!net.isDisposed()) {
                list.add(net.toNBT());
            }
        }
        tag.put("networks", list);
    }

    public NodeConn getNodeConnection(IWirelessNode node) {
        VNNode vnn = new VNNode(node);
        NodeConn ret = privateGetNodeConn(vnn);
        if (ret == null) {
            doAddNode(ret = new NodeConn(this, vnn));
        }
        return ret;
    }

    public NodeConn getNodeConnectionNonCreate(IWirelessNode node) {
        return privateGetNodeConn(new VNNode(node));
    }

    public NodeConn getNodeConnection(IWirelessUser user) {
        if (user instanceof IWirelessGenerator) {
            return privateGetNodeConn(new VNGenerator((IWirelessGenerator) user));
        } else if (user instanceof IWirelessReceiver) {
            return privateGetNodeConn(new VNReceiver((IWirelessReceiver) user));
        } else if (user == null) {
            return null;
        } else {
            throw new IllegalArgumentException("Invalid user type");
        }
    }

    private NodeConn privateGetNodeConn(Object key) {
        NodeConn ret = nodeLookup.get(key);
        if (ret != null && ret.validate()) {
            return ret;
        } else {
            return null;
        }
    }

    private void tickNode() {
        for (NodeConn nc : nToRemove) {
            doRemoveNode(nc);
        }
        nToRemove.clear();

        Iterator<NodeConn> iter = nodeList.iterator();
        while (iter.hasNext()) {
            NodeConn conn = iter.next();
            if (conn.isDisposed()) {
                nToRemove.add(conn);
            } else {
                conn.tick();
            }
        }
    }

    private void doAddNode(NodeConn conn) {
        nodeList.add(conn);
        conn.onAdded(this);
    }

    private void doRemoveNode(NodeConn conn) {
        nodeList.remove(conn);
        conn.onCleanup(this);
    }

    private void loadNode(CompoundTag tag) {
        ListTag list = tag.getList("list", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); ++i) {
            doAddNode(new NodeConn(this, list.getCompound(i)));
        }
    }

    private void saveNode(CompoundTag tag) {
        ListTag list = new ListTag();
        for (NodeConn c : nodeList) {
            if (!c.isDisposed()) {
                list.add(c.toNBT());
            }
        }
        tag.put("list", list);
    }

    public void tick() {
        tickNetwork();
        tickNode();
        setDirty();
    }

    void readFromNBT(CompoundTag tag) {
        if (tag.contains("net")) {
            loadNetwork(tag.getCompound("net"));
        }
        if (tag.contains("node")) {
            loadNode(tag.getCompound("node"));
        }
    }

    void writeToNBT(CompoundTag tag) {
        CompoundTag tag1 = new CompoundTag();
        saveNetwork(tag1);
        tag.put("net", tag1);

        CompoundTag tag2 = new CompoundTag();
        saveNode(tag2);
        tag.put("node", tag2);
    }

    private void debug(Object msg) {
        if (AcademyCraft.DEBUG_MODE)
            AcademyCraft.LOGGER.info("WiWorldData: " + msg);
    }
}
