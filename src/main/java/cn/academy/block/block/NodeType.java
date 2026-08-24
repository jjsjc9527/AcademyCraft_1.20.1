package cn.academy.block.block;

import net.minecraft.util.StringRepresentable;

public enum NodeType implements StringRepresentable {

    BASIC("basic", 15000, 150, 9, 5),
    STANDARD("standard", 50000, 300, 12, 10),
    ADVANCED("advanced", 200000, 900, 19, 20);

    public final String id;

    public final int maxEnergy;

    public final int bandwidth;

    public final int range;

    public final int capacity;

    NodeType(String id, int maxEnergy, int bandwidth, int range, int capacity) {
        this.id = id;
        this.maxEnergy = maxEnergy;
        this.bandwidth = bandwidth;
        this.range = range;
        this.capacity = capacity;
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
