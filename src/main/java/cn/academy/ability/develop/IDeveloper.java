package cn.academy.ability.develop;

public interface IDeveloper {

    DeveloperType getDeveloperType();

    boolean tryPullEnergy(double amount);

    double getEnergy();

    double getMaxEnergy();

    default void onGuiClosed() {}
}
