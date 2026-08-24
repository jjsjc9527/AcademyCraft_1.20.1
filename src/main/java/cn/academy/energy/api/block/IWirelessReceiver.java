package cn.academy.energy.api.block;

public interface IWirelessReceiver extends IWirelessUser {

    double getRequiredEnergy();

    double injectEnergy(double amt);

    double pullEnergy(double amt);

    double getBandwidth();
}
