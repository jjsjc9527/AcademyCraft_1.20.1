package cn.academy.entity;

import cn.academy.ACEntities;
import cn.lambdalib2.util.GameTimer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EntityDiamondShield extends Entity implements cn.academy.client.render.ACEffect {

    public static final float RENDER_SCALE = 1.5f;

    private LivingEntity owner;

    public final double spawnTime = GameTimer.getPausableTime();

    public double lifespan = 15 / 20.0;

    public double lastFeedTime = GameTimer.getPausableTime();

    public void touch() {
        lastFeedTime = GameTimer.getPausableTime();
    }

    public EntityDiamondShield(Level level) {
        super(ACEntities.DIAMOND_SHIELD.get(), level);
        noCulling = true;
    }

    public void init(LivingEntity owner) {
        this.owner = owner;
        updatePos();
        xOld = getX();
        yOld = getY();
        zOld = getZ();
    }

    public LivingEntity getOwner() {
        return owner;
    }

    public double age() {
        return GameTimer.getPausableTime() - spawnTime;
    }

    @Override
    public void tick() {
        super.tick();
        updatePos();
    }

    public void updatePos() {
        if (owner == null) return;
        Vec3 p = cn.academy.ability.vanilla.meltdowner.skill.LightShield.shieldCenter(owner);
        setPos(p.x, p.y, p.z);
        setYRot(owner.getYHeadRot());
        setXRot(owner.getXRot());
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        discard();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public boolean effectExpired(double now) {
        return now - lastFeedTime > 2.0;
    }
}
