package cn.academy.ability.develop;

import cn.academy.energy.IFConstants;
import net.minecraft.resources.ResourceLocation;

public enum DeveloperType {

    PORTABLE(IFConstants.LATENCY_MK1, 0.3, 10000, 25, 750, "item/developer_portable_empty"),
    NORMAL(IFConstants.LATENCY_MK2, 0.7, 50000, 20, 700, "item/dev_normal"),
    ADVANCED(IFConstants.LATENCY_MK3, 1.0, 200000, 15, 600, "item/dev_advanced");

    private final double bandwidth;

    public final double syncRate;
    public final ResourceLocation texture;
    public final double energy;

    public final double cps;

    public final int tps;

    DeveloperType(double bandwidth, double syncRate, double energy, int tps, double cps, String tex) {
        this.bandwidth = bandwidth;
        this.syncRate = syncRate;
        this.energy = energy;
        this.tps = tps;
        this.cps = cps;
        this.texture = new ResourceLocation("academy", "textures/" + tex + ".png");
    }

    public double getEnergy() {
        return energy;
    }

    public double getCPS() {
        return cps;
    }

    public double getBandwidth() {
        return bandwidth;
    }

    public int getTPS() {
        return tps;
    }
}
