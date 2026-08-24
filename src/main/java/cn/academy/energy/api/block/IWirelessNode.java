package cn.academy.energy.api.block;

public interface IWirelessNode extends IWirelessTile {

    double getMaxEnergy();

    double getEnergy();

    void setEnergy(double value);

    double getBandwidth();

    int getCapacity();

    double getRange();

    String getNodeName();

    String getPassword();
}
