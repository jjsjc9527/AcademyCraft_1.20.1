package cn.academy.entity;

import cn.lambdalib2.util.GameTimer;
import cn.lambdalib2.util.ViewOptimize;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EntityRailgunHand extends net.minecraft.world.entity.Entity implements ViewOptimize.IAssociatePlayer, cn.academy.client.render.ACEffect {

    public static final double PER_FRAME = 40 / 1000.0;
    public static final int COUNT = 40;

    private Player player;
    private final double createTime;

    private final InteractionHand hand;

    public EntityRailgunHand(Player player, InteractionHand hand) {
        super(cn.academy.ACEntities.RAILGUN_HAND.get(), player.level());
        this.player = player;
        this.hand = hand;
        this.createTime = GameTimer.getPausableTime();
        setPos(player.getX(), player.getY(), player.getZ());
        noCulling = true;
    }

    public double getCreateTime() {
        return createTime;
    }

    public HumanoidArm getArm() {
        HumanoidArm main = player.getMainArm();
        return hand == InteractionHand.MAIN_HAND ? main : main.getOpposite();
    }

    @Override
    public void tick() {
        super.tick();
        if (player == null || player.isRemoved()) {
            discard();
            return;
        }

        setPos(player.getX(), player.getY(), player.getZ());

        if (GameTimer.getPausableTime() - createTime >= PER_FRAME * COUNT) {
            discard();
        }
    }

    @Override
    public Player getPlayer() {
        return player;
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
        return now - getCreateTime() > PER_FRAME * COUNT + 0.25;
    }
}
