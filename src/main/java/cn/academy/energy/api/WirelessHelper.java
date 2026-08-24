package cn.academy.energy.api;

import cn.academy.energy.api.block.IWirelessGenerator;
import cn.academy.energy.api.block.IWirelessMatrix;
import cn.academy.energy.api.block.IWirelessNode;
import cn.academy.energy.api.block.IWirelessReceiver;
import cn.academy.energy.api.block.IWirelessUser;
import cn.academy.energy.impl.NodeConn;
import cn.academy.energy.impl.WiWorldData;
import cn.academy.energy.impl.WirelessNet;
import cn.lambdalib2.util.IBlockSelector;
import cn.lambdalib2.util.MathUtils;
import cn.lambdalib2.util.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class WirelessHelper {

    public static WirelessNet getWirelessNet(IWirelessMatrix matrix) {
        BlockEntity tile = (BlockEntity) matrix;
        return WiWorldData.get(tile.getLevel()).getNetwork(matrix);
    }

    public static WirelessNet getWirelessNet(IWirelessNode node) {
        BlockEntity tile = (BlockEntity) node;
        return WiWorldData.get(tile.getLevel()).getNetwork(node);
    }

    public static boolean isNodeLinked(IWirelessNode node) {
        return getWirelessNet(node) != null;
    }

    public static boolean isMatrixActive(IWirelessMatrix matrix) {
        return getWirelessNet(matrix) != null;
    }

    public static Collection<WirelessNet> getNetInRange(Level world, int x, int y, int z, double range, int max) {
        WiWorldData data = WiWorldData.get(world);
        return data.rangeSearch(x, y, z, range, max);
    }

    public static NodeConn getNodeConn(IWirelessNode node) {
        BlockEntity tile = (BlockEntity) node;
        return WiWorldData.get(tile.getLevel()).getNodeConnection(node);
    }

    public static NodeConn getNodeConnNonCreate(IWirelessNode node) {
        BlockEntity tile = (BlockEntity) node;
        return WiWorldData.get(tile.getLevel()).getNodeConnectionNonCreate(node);
    }

    public static NodeConn getNodeConn(IWirelessUser gen) {
        BlockEntity tile = (BlockEntity) gen;
        return WiWorldData.get(tile.getLevel()).getNodeConnection(gen);
    }

    public static boolean isReceiverLinked(IWirelessReceiver rec) {
        return getNodeConn(rec) != null;
    }

    public static boolean isGeneratorLinked(IWirelessGenerator gen) {
        return getNodeConn(gen) != null;
    }

    private static final List<BlockPos> _blockPosBuffer = new ArrayList<>();

    public static List<IWirelessNode> getNodesInRange(Level world, BlockPos pos) {
        double range = 20.0;
        IBlockSelector selector = (w, x2, y2, z2, block) -> {
            BlockEntity te = w.getBlockEntity(new BlockPos(x2, y2, z2));
            if (te instanceof IWirelessNode) {
                IWirelessNode node = (IWirelessNode) te;
                NodeConn conn = getNodeConn(node);

                double distSq = MathUtils.distanceSq(pos.getX(), pos.getY(), pos.getZ(), x2, y2, z2);
                double nrange = node.getRange();

                return nrange * nrange >= distSq && conn.getLoad() < conn.getCapacity();
            } else {
                return false;
            }
        };
        WorldUtils.getBlocksWithin(_blockPosBuffer, world, pos.getX(), pos.getY(), pos.getZ(), range, 100, selector);

        List<IWirelessNode> ret = new ArrayList<>();
        for (BlockPos bp : _blockPosBuffer) {
            ret.add((IWirelessNode) world.getBlockEntity(bp));
        }

        return ret;
    }
}
