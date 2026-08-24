package cn.academy.event;

import cn.academy.energy.api.block.IWirelessTile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.eventbus.api.Event;

public class WirelessEvent extends Event {

    public final IWirelessTile tile;

    public WirelessEvent(IWirelessTile _tile) {
        tile = _tile;
    }

    public BlockEntity getBlockEntity() {
        return (BlockEntity) tile;
    }

    public Level getWorld() {
        return getBlockEntity().getLevel();
    }
}
