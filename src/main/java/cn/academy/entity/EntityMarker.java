package cn.academy.entity;

import cn.academy.ACEntities;
import cn.lambdalib2.util.Color;
import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.GameTimer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EntityMarker extends Entity implements cn.academy.client.render.ACEffect {

    public Entity target = null;

    public float boxWidth = 0.5f, boxHeight = 0.5f;
    public final Color color = Colors.white();

    public double lastFeedTime = GameTimer.getPausableTime();

    public EntityMarker(Level level) {
        super(ACEntities.MARKER.get(), level);
        noCulling = true;
    }

    public void touch() {
        lastFeedTime = GameTimer.getPausableTime();
    }

    public void moveTo2(double x, double y, double z) {
        xOld = getX();
        yOld = getY();
        zOld = getZ();
        setPos(x, y, z);
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public boolean effectExpired(double now) {
        return now - lastFeedTime > 2.0;
    }
}
