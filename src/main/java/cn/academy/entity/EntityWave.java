package cn.academy.entity;

import cn.academy.ACEntities;
import cn.lambdalib2.util.GameTimer;
import cn.lambdalib2.util.RandUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EntityWave extends Entity implements cn.academy.client.render.ACEffect {

    public static final int LIFE = 15;

    public static final class Ring {
        public final int life;
        public final double offset;
        public final double size;
        public final int timeOffset;

        Ring(int life, double offset, double size, int timeOffset) {
            this.life = life;
            this.offset = offset;
            this.size = size;
            this.timeOffset = timeOffset;
        }
    }

    public final Ring[] rings;

    public float yaw, pitch;

    public final double spawnTime = GameTimer.getPausableTime();

    public EntityWave(Level level) {
        this(level, RandUtils.rangei(2, 3), 1.0);
    }

    public EntityWave(Level level, int ringCount, double size) {
        super(ACEntities.WAVE.get(), level);
        noCulling = true;

        rings = new Ring[Math.max(1, ringCount)];
        for (int i = 0; i < rings.length; i++) {
            rings[i] = new Ring(
                    RandUtils.rangei(8, 12),
                    i * 1.5 + RandUtils.ranged(-.3, .3),
                    size * RandUtils.ranged(0.8, 1.2),
                    i * 2 + RandUtils.rangei(-1, 1));
        }
    }

    @Override
    public void tick() {

        super.tick();
        if (tickCount >= LIFE) {
            discard();
        }
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public boolean effectExpired(double now) {
        return (now - spawnTime) * 20.0 > LIFE + 5;
    }
}
