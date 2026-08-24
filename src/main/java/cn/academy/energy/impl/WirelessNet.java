package cn.academy.energy.impl;

import cn.academy.AcademyCraft;
import cn.academy.energy.api.block.IWirelessMatrix;
import cn.academy.energy.api.block.IWirelessNode;
import cn.academy.energy.impl.VBlocks.VWMatrix;
import cn.academy.energy.impl.VBlocks.VWNode;
import cn.lambdalib2.util.MathUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class WirelessNet {

    private static final int UPDATE_INTERVAL = 40;
    private static final double BUFFER_MAX = 2000;

    private final WiWorldData data;
    Level world;

    private List<VWNode> nodes = new LinkedList<>();
    private List<VWNode> toRemoveNodes = new ArrayList<>();

    private VWMatrix matrix;

    private String ssid;
    private String password;

    private double buffer;

    private int aliveUpdateCounter = UPDATE_INTERVAL;

    private boolean disposed = false;

    WirelessNet(WiWorldData data, VWMatrix matrix, String ssid, String pass) {
        this.data = data;
        this.matrix = matrix;
        this.ssid = ssid;
        this.password = pass;
    }

    WirelessNet(WiWorldData data, CompoundTag tag) {
        this.data = data;

        matrix = new VWMatrix(tag.getCompound("matrix"));

        ssid = tag.getString("ssid");
        password = tag.getString("password");
        buffer = tag.getDouble("buffer");

        ListTag list = tag.getList("list", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); ++i) {
            doAddNode(new VWNode(list.getCompound(i)));
        }

        debug("Loading " + ssid + " from NBT, " + list.size() + " nodes.");
    }

    CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.put("matrix", matrix.toNBT());
        tag.putString("ssid", ssid);
        tag.putString("password", password);
        tag.putDouble("buffer", buffer);

        ListTag list = new ListTag();
        for (VWNode vn : nodes) {
            if (!vn.isLoaded(world) || vn.get(world) != null) {
                list.add(vn.toNBT());
            }
        }
        tag.put("list", list);

        return tag;
    }

    public String getSSID() {
        return ssid;
    }

    public void setSSID(String ssid) {
        this.ssid = ssid;
    }

    public String getPassword() {
        return password;
    }

    public boolean resetPassword(String np) {
        password = np;
        return true;
    }

    public boolean isDisposed() {
        return disposed;
    }

    public int getLoad() {
        return nodes.size();
    }

    public int getCapacity() {
        Level world = data.world;
        IWirelessMatrix imat = matrix.get(world);
        return imat == null ? 0 : imat.getCapacity();
    }

    public IWirelessMatrix getMatrix() {
        return matrix.get(world);
    }

    void dispose() {
        disposed = true;
    }

    boolean addNode(VWNode node, String password) {
        if (!password.equals(this.password))
            return false;
        if (getLoad() >= getCapacity())
            return false;

        IWirelessMatrix imat = matrix.get(world);
        if (imat == null) {
            return false;
        }

        double r = imat.getRange();
        if (node.distSq(matrix) > r * r)
            return false;

        WiWorldData data = getWorldData();

        WirelessNet other = data.getNetwork(node.get(world));
        if (other != null) {
            other.removeNode(node);
        }

        doAddNode(node);

        return true;
    }

    boolean validate() {
        if (matrix.isLoaded(world)) {
            IWirelessMatrix mat = matrix.get(world);
            if (mat == null) {
                disposed = true;
            }
        }

        return !disposed;
    }

    boolean isInRange(int x, int y, int z) {
        IWirelessMatrix imat = matrix.get(world);
        if (imat == null) {
            return false;
        }

        double r = imat.getRange();
        return MathUtils.distanceSq(x, y, z, matrix.x, matrix.y, matrix.z) <= r * r;
    }

    private void doAddNode(VWNode node) {
        WiWorldData data = getWorldData();
        nodes.add(node);
        data.netLookup.put(node, this);
    }

    void removeNode(VWNode node) {
        debug("Removing " + node + " from " + ssid);
        toRemoveNodes.add(node);
    }

    void onCreate(WiWorldData data) {
        data.netLookup.put(matrix, this);
    }

    void onCleanup(WiWorldData data) {

        data.netLookup.remove(matrix, this);

        for (VWNode n : nodes) {
            data.netLookup.remove(n, this);
        }
    }

    private WiWorldData getWorldData() {
        return data;
    }

    void tick() {
        validate();

        if (matrix.isLoaded(world)) {
            IWirelessMatrix imat = matrix.get(world);
            if (imat == null) {
                debug("WirelessNet with SSID " + ssid + " matrix destroyed, removing");
                dispose();
            } else {

                Collections.shuffle(nodes);

                double sum = 0, maxSum = 0;
                for (VWNode vn : nodes) {
                    if (vn.isLoaded(world)) {
                        IWirelessNode node = vn.get(world);
                        if (node == null) {
                            removeNode(vn);
                        } else {
                            sum += node.getEnergy();
                            maxSum += node.getMaxEnergy();
                        }
                    }
                }

                data.netLookup.keySet().removeAll(toRemoveNodes);
                nodes.removeAll(toRemoveNodes);
                toRemoveNodes.clear();

                double percent = sum / maxSum;
                double transferLeft = imat.getBandwidth();
                for (VWNode vn : nodes) {
                    if (vn.isLoaded(world)) {
                        IWirelessNode node = vn.get(world);

                        double cur = node.getEnergy();
                        double targ = node.getMaxEnergy() * percent;

                        double delta = targ - cur;
                        delta = Math.signum(delta) * Math.min(Math.abs(delta), Math.min(transferLeft, node.getBandwidth()));

                        if (buffer + delta > BUFFER_MAX) {
                            delta = BUFFER_MAX - buffer;
                        } else if (buffer + delta < 0) {
                            delta = -buffer;
                        }

                        transferLeft -= Math.abs(delta);
                        buffer += delta;
                        node.setEnergy(cur + delta);

                        if (transferLeft == 0)
                            break;
                    }
                }
            }
        }
    }

    private void debug(Object msg) {
        if (AcademyCraft.DEBUG_MODE)
            AcademyCraft.LOGGER.info("WN:" + msg);
    }
}
