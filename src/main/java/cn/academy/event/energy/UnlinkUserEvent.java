package cn.academy.event.energy;

import cn.academy.energy.api.block.IWirelessTile;
import cn.academy.event.WirelessUserEvent;

public class UnlinkUserEvent extends WirelessUserEvent {

    public UnlinkUserEvent(IWirelessTile _tile) {
        super(_tile);
    }
}
