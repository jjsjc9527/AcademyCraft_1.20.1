package cn.academy.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;
import org.jetbrains.annotations.Nullable;

@Cancelable
public final class BlockDestroyEvent extends Event {

    public final Level world;
    @Nullable
    public final Player player;
    public final BlockPos pos;

    public BlockDestroyEvent(Level world_, @Nullable Player player_, BlockPos pos_) {
        world = world_;
        player = player_;
        pos = pos_;
    }

    public BlockDestroyEvent(Level world_, BlockPos pos) {
        this(world_, null, pos);
    }

    public BlockDestroyEvent(Player player_, BlockPos pos) {
        this(player_.level(), player_, pos);
    }
}
