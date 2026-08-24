package cn.academy.client.sound;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.function.Predicate;

public class MachineWorkSound extends AbstractTickableSoundInstance {

    private final BlockEntity target;
    private final Predicate<BlockEntity> stillWorking;

    public MachineWorkSound(SoundEvent event, BlockEntity be, Predicate<BlockEntity> stillWorking) {

        super(event, SoundSource.MASTER, RandomSource.create());
        this.target = be;
        this.stillWorking = stillWorking;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.6f;
        this.x = be.getBlockPos().getX() + 0.5;
        this.y = be.getBlockPos().getY() + 0.5;
        this.z = be.getBlockPos().getZ() + 0.5;
    }

    @Override
    public void tick() {
        if (target.isRemoved() || !stillWorking.test(target)) {
            stop();
        }
    }
}
