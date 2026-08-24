package cn.academy.ability.vanilla.teleporter;

import net.minecraft.nbt.CompoundTag;

public class Location {

    public String name;

    public String dim;
    public float x, y, z;

    public int id;

    public Location() {}

    public Location(String name, String dim, float x, float y, float z, int id) {
        this.name = name;
        this.dim = dim;
        this.x = x;
        this.y = y;
        this.z = z;
        this.id = id;
    }

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("name", name);
        tag.putString("dim", dim);
        tag.putFloat("x", x);
        tag.putFloat("y", y);
        tag.putFloat("z", z);
        return tag;
    }

    public static Location fromNBT(CompoundTag tag, int id) {
        return new Location(tag.getString("name"), tag.getString("dim"),
                tag.getFloat("x"), tag.getFloat("y"), tag.getFloat("z"), id);
    }
}
