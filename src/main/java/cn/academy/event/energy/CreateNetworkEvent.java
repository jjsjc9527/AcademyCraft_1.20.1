package cn.academy.event.energy;

import cn.academy.energy.api.block.IWirelessMatrix;
import cn.academy.event.WirelessEvent;
import net.minecraftforge.eventbus.api.Cancelable;

@Cancelable
public class CreateNetworkEvent extends WirelessEvent {

    public final IWirelessMatrix mat;
    public final String ssid;
    public final String pwd;

    public CreateNetworkEvent(IWirelessMatrix _mat, String _ssid, String _pwd) {
        super(_mat);
        mat = _mat;
        ssid = _ssid;
        pwd = _pwd;
    }
}
