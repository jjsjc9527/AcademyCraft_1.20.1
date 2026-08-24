package cn.academy.energy.impl;

import cn.academy.energy.api.block.IWirelessGenerator;
import cn.academy.energy.api.block.IWirelessMatrix;
import cn.academy.energy.api.block.IWirelessNode;
import cn.academy.energy.api.block.IWirelessReceiver;
import cn.academy.energy.api.block.IWirelessTile;
import cn.lambdalib2.util.MathUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class VBlocks {

    static abstract class VBlock<T extends IWirelessTile> {

        protected final int x, y, z;
        protected final boolean ignoreChunk;

        public VBlock(BlockEntity te, boolean _ignoreChunk) {
            BlockPos p = te.getBlockPos();
            x = p.getX();
            y = p.getY();
            z = p.getZ();
            ignoreChunk = _ignoreChunk;
        }

        public VBlock(CompoundTag tag, boolean _ignoreChunk) {
            x = tag.getInt("x");
            y = tag.getInt("y");
            z = tag.getInt("z");
            ignoreChunk = _ignoreChunk;
        }

        public double distSq(VBlock<?> another) {
            return MathUtils.distanceSq(another.x, another.y, another.z, x, y, z);
        }

        public boolean isLoaded(Level world) {
            return world != null && world.hasChunk(x >> 4, z >> 4);
        }

        @SuppressWarnings("unchecked")
        public T get(Level world) {
            if (world == null) {
                return null;
            }
            if (!ignoreChunk && !isLoaded(world)) {
                return null;
            }
            BlockEntity te = world.getBlockEntity(new BlockPos(x, y, z));
            if (te == null || !isAcceptable(te)) {
                return null;
            }
            return (T) te;
        }

        public CompoundTag toNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("x", x);
            tag.putInt("y", y);
            tag.putInt("z", z);
            return tag;
        }

        @Override
        public int hashCode() {
            return x ^ y ^ z;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            VBlock<?> vb = (VBlock<?>) obj;
            return vb.x == x && vb.y == y && vb.z == z;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[" + x + ", " + y + ", " + z + "]";
        }

        protected abstract boolean isAcceptable(BlockEntity tile);
    }

    static class VWMatrix extends VBlock<IWirelessMatrix> {

        public VWMatrix(IWirelessMatrix te) {
            super((BlockEntity) te, true);
        }

        public VWMatrix(CompoundTag tag) {
            super(tag, true);
        }

        @Override
        protected boolean isAcceptable(BlockEntity tile) {
            return tile instanceof IWirelessMatrix;
        }
    }

    static class VWNode extends VBlock<IWirelessNode> {

        public VWNode(IWirelessNode te) {
            super((BlockEntity) te, false);
        }

        public VWNode(CompoundTag tag) {
            super(tag, false);
        }

        @Override
        protected boolean isAcceptable(BlockEntity tile) {
            return tile instanceof IWirelessNode;
        }
    }

    static class VNNode extends VBlock<IWirelessNode> {

        public VNNode(IWirelessNode te) {
            super((BlockEntity) te, true);
        }

        public VNNode(CompoundTag tag) {
            super(tag, true);
        }

        @Override
        protected boolean isAcceptable(BlockEntity tile) {
            return tile instanceof IWirelessNode;
        }
    }

    static class VNGenerator extends VBlock<IWirelessGenerator> {

        public VNGenerator(IWirelessGenerator te) {
            super((BlockEntity) te, true);
        }

        public VNGenerator(CompoundTag tag) {
            super(tag, true);
        }

        @Override
        protected boolean isAcceptable(BlockEntity tile) {
            return tile instanceof IWirelessGenerator;
        }
    }

    static class VNReceiver extends VBlock<IWirelessReceiver> {

        public VNReceiver(IWirelessReceiver te) {
            super((BlockEntity) te, true);
        }

        public VNReceiver(CompoundTag tag) {
            super(tag, true);
        }

        @Override
        protected boolean isAcceptable(BlockEntity tile) {
            return tile instanceof IWirelessReceiver;
        }
    }
}
