package cn.academy.client.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public final class MachineSounds {

    private static final Map<BlockPos, MachineWorkSound> ACTIVE = new HashMap<>();

    private MachineSounds() {}

    public static void updateWorkSound(BlockEntity be, boolean working, SoundEvent event,
                                       Predicate<BlockEntity> stillWorking) {
        BlockPos pos = be.getBlockPos();
        MachineWorkSound cur = ACTIVE.get(pos);
        if (cur != null && cur.isStopped()) {
            ACTIVE.remove(pos);
            cur = null;
        }
        if (!working) return;
        if (cur != null) return;

        MachineWorkSound s = new MachineWorkSound(event, be, stillWorking);
        ACTIVE.put(pos, s);
        Minecraft.getInstance().getSoundManager().play(s);
    }
}
