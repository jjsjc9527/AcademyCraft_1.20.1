package cn.academy.client.sound;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

public class FollowEntitySound extends AbstractTickableSoundInstance {

    private final Entity follow;
    private boolean manualStop = false;

    private final float target;
    private final int fadeTicks;
    private int fadeInAge = 0;
    private int fadeOutAge = 0;

    public FollowEntitySound(SoundEvent event, Entity follow, float volume) {
        this(event, follow, volume, 0);
    }

    public FollowEntitySound(SoundEvent event, Entity follow, float volume, int fadeTicks) {
        super(event, SoundSource.AMBIENT, RandomSource.create());
        this.follow = follow;
        this.looping = true;
        this.delay = 0;
        this.target = volume;
        this.fadeTicks = Math.max(0, fadeTicks);

        this.volume = this.fadeTicks > 0 ? 0.0f : volume;
        updatePos();
    }

    @Override
    public boolean canStartSilent() {
        return fadeTicks > 0;
    }

    public void requestStop() {
        manualStop = true;
    }

    private void updatePos() {
        this.x = follow.getX();
        this.y = follow.getY();
        this.z = follow.getZ();
    }

    @Override
    public void tick() {

        if (follow.isRemoved() || !follow.isAlive()) {
            stop();
            return;
        }
        updatePos();

        if (manualStop) {
            if (fadeTicks <= 0) {
                stop();
                return;
            }
            fadeOutAge++;
            float k = 1.0f - fadeOutAge / (float) fadeTicks;
            if (k <= 0.0f) {
                this.volume = 0.0f;
                stop();
                return;
            }
            this.volume = target * k;
            return;
        }
        if (fadeTicks > 0 && fadeInAge < fadeTicks) {
            fadeInAge++;
            this.volume = target * (fadeInAge / (float) fadeTicks);
        }
    }
}
