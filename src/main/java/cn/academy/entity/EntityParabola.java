package cn.academy.entity;

import cn.academy.ACEntities;
import cn.academy.ability.context.Context;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;

@OnlyIn(Dist.CLIENT)
public class EntityParabola extends Entity implements cn.academy.client.render.ACEffect {

    private final Player owner;

    public final DoubleSupplier speed;

    public final BooleanSupplier canPerform;
    private final Context<?> ctx;

    public EntityParabola(Player owner, Context<?> ctx, DoubleSupplier speed, BooleanSupplier canPerform) {
        super(ACEntities.PARABOLA.get(), owner.level());
        this.owner = owner;
        this.ctx = ctx;
        this.speed = speed;
        this.canPerform = canPerform;
        noCulling = true;
        syncToPlayer();
    }

    public Player getOwner() {
        return owner;
    }

    @Override
    public void tick() {
        super.tick();
        syncToPlayer();
        if (ctx.getStatus() == Context.Status.TERMINATED) {
            discard();
        }
    }

    private void syncToPlayer() {
        setPos(owner.getX(), owner.getY(), owner.getZ());
        xOld = owner.xOld;
        yOld = owner.yOld;
        zOld = owner.zOld;
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
