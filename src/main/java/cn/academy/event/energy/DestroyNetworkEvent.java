package cn.academy.event.energy;

import cn.academy.energy.api.block.IWirelessMatrix;
import cn.academy.event.WirelessEvent;

public class DestroyNetworkEvent extends WirelessEvent {

    public final IWirelessMatrix mat;

    public DestroyNetworkEvent(IWirelessMatrix _mat) {
        super(_mat);
        mat = _mat;
    }
}
