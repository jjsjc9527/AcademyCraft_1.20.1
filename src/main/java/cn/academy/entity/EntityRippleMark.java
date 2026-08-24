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
public class EntityRippleMark extends Entity implements cn.academy.client.render.ACEffect {

    public final Color color = Colors.white();

    public final double creationTime = GameTimer.getPausableTime();

    public EntityRippleMark(Level level) {
        super(ACEntities.RIPPLE_MARK.get(), level);
        noCulling = true;
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public boolean effectExpired(double now) {
        return false;
    }
}
