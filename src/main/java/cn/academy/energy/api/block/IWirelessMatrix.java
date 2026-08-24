package cn.academy.energy.api.block;

public interface IWirelessMatrix extends IWirelessTile {

    int getCapacity();

    double getBandwidth();

    double getRange();
}
