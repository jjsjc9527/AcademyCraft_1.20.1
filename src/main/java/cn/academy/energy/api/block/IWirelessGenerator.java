package cn.academy.energy.api.block;

public interface IWirelessGenerator extends IWirelessUser {

    double getProvidedEnergy(double req);

    double getBandwidth();
}
