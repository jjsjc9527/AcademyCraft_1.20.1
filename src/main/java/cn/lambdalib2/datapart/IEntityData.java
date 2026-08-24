package cn.lambdalib2.datapart;

import net.minecraft.nbt.CompoundTag;

public interface IEntityData {
    void writeNBT(CompoundTag tag);

    void readNBT(CompoundTag tag);
}
