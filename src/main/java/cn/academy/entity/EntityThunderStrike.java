package cn.academy.entity;

import cn.academy.ACEntities;
import cn.lambdalib2.util.GameTimer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EntityThunderStrike extends Entity implements cn.academy.client.render.ACEffect {

    public static final double LIFESPAN = 0.5;

    public final double spawnTime = GameTimer.getPausableTime();

    public final boolean mirror;

    public final float yawOffset;

    public EntityThunderStrike(Level level) {
        super(ACEntities.THUNDER_STRIKE.get(), level);
        noCulling = true;
        mirror = level.random.nextBoolean();
        yawOffset = level.random.nextFloat() * 360f;
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public boolean effectExpired(double now) {
        return now - spawnTime > LIFESPAN + 0.25;
    }
}
