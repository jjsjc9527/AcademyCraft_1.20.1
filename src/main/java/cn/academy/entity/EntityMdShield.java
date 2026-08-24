package cn.academy.entity;

import cn.academy.ACEntities;
import cn.academy.gravity.ACGravity;
import cn.academy.gravity.RotationUtil;
import cn.lambdalib2.util.GameTimer;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EntityMdShield extends net.minecraft.world.entity.Entity implements cn.academy.client.render.ACEffect {

    public static final float SIZE = 1.8f;

    private Player owner;

    public final double spawnTime = GameTimer.getPausableTime();

    public double lastFeedTime = GameTimer.getPausableTime();

    public void touch() {
        lastFeedTime = GameTimer.getPausableTime();
    }

    public float rotation = 0;

    public double lastRender = 0;

    public EntityMdShield(Level level) {
        super(ACEntities.MD_SHIELD.get(), level);
        noCulling = true;
    }

    public void init(Player owner) {
        this.owner = owner;
        updatePos();
        xOld = getX();
        yOld = getY();
        zOld = getZ();
    }

    public Player getOwner() {
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
        setYRot(owner.yHeadRot);
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
