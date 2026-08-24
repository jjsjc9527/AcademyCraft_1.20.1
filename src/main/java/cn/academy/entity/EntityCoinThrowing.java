package cn.academy.entity;

import cn.academy.ACEntities;
import cn.academy.ACItems;
import cn.academy.AcademyCraft;
import cn.academy.event.ConfigModifyEvent;
import cn.lambdalib2.util.RandUtils;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nullable;

public class EntityCoinThrowing extends Entity {

    public static boolean PLAY_HEADS_OR_TAILS;

    private static final int MAXLIFE = 120;
    private static final double INITVEL = 0.92;

    private static final double GRAVITY = 0.06;

    private Direction grav = Direction.DOWN;

    private double relH = 0, relVh = 0;

    private double maxRelH = 0;

    @Nullable
    public Player player;

    public double clientSpawnTime = -1;

    public ItemStack stack = ItemStack.EMPTY;

    public Vec3 axis = new Vec3(0.5, 0.5, 0.5);

    public boolean isSync = false;

    private InteractionHand hand = InteractionHand.MAIN_HAND;

    public EntityCoinThrowing(EntityType<? extends EntityCoinThrowing> type, Level level) {
        super(type, level);
        isSync = true;
        setup();
    }

    public EntityCoinThrowing(Player player, ItemStack is, InteractionHand hand) {
        super(ACEntities.COIN_THROWING.get(), player.level());
        this.stack = is;
        this.player = player;
        this.hand = hand;
        this.grav = cn.academy.gravity.ACGravity.getGravityDirection(player);
        setPos(player.getX(), player.getY(), player.getZ());
        setup();
        this.noCulling = true;
    }

    private void setup() {

        relVh += INITVEL;
        axis = new Vec3(0.1 + random.nextDouble(), random.nextDouble(), random.nextDouble());
    }

    @Override
    public void tick() {

        if (level().isClientSide && isSync) {
            discard();
            return;
        }
        super.tick();

        relVh -= GRAVITY;
        relH += relVh;
        maxRelH = Math.max(maxRelH, relH);
        if (player != null) {
            Vec3 lp = cn.academy.gravity.RotationUtil.vecWorldToPlayer(player.position(), grav);
            Vec3 world = cn.academy.gravity.RotationUtil.vecPlayerToWorld(lp.x, lp.y + relH, lp.z, grav);
            setPos(world.x, world.y, world.z);

            if ((relH < 0 && relVh < 0) || tickCount > MAXLIFE) {
                finishThrowing();
                return;
            }
        } else if (tickCount > MAXLIFE) {
            discard();
        }
    }

    void finishThrowing() {
        if (!level().isClientSide && player != null && !player.getAbilities().instabuild) {
            ItemStack equipped = player.getMainHandItem();
            ItemStack coin = new ItemStack(ACItems.COIN.get());
            if (equipped.isEmpty()) {
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, coin);
            } else if (equipped.getItem() == ACItems.COIN.get() && equipped.getCount() < equipped.getMaxStackSize()) {
                equipped.setCount(equipped.getCount() + 1);
                player.getInventory().setChanged();
            } else if (!player.getInventory().add(coin)) {

                player.drop(coin, false);
            }
        }
        if (level().isClientSide && PLAY_HEADS_OR_TAILS && player != null) {
            player.sendSystemMessage(Component.translatable("headsOrTails.academy." + RandUtils.nextInt(2)));
        }
        discard();
    }

    public double getProgress() {

        if (relVh > 0) {
            return (INITVEL - relVh) / INITVEL * 0.5;
        } else {
            return Math.min(1.0, 0.5 + ((maxRelH - relH) / Math.max(1.0E-6, maxRelH)) * 0.5);
        }
    }

    public Direction getGrav() {
        return grav;
    }

    public InteractionHand getHand() {
        return hand;
    }

    public HumanoidArm getArm() {
        HumanoidArm main = player != null ? player.getMainArm() : HumanoidArm.RIGHT;
        return hand == InteractionHand.MAIN_HAND ? main : main.getOpposite();
    }

    @Override protected void defineSynchedData() {}
    @Override protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {}

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket() {
        return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);
    }

    public static class Events {

        public static void init() {
            reload();
            MinecraftForge.EVENT_BUS.register(new Events());
        }

        private static void reload() {
            PLAY_HEADS_OR_TAILS = AcademyCraft.config
                    .get("generic", "headsOrTails", false).getBoolean();
        }

        @SubscribeEvent
        public void onConfigModified(ConfigModifyEvent e) {
            reload();
        }
    }
}
