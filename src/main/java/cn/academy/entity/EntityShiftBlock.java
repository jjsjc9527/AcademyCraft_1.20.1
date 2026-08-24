package cn.academy.entity;

import cn.academy.ACEntities;
import cn.academy.ACParticles;
import cn.academy.ability.AbilityContext;
import cn.academy.ability.AbilityPipeline;
import cn.academy.ability.vanilla.teleporter.skill.ShiftTeleport;
import cn.lambdalib2.util.RandUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class EntityShiftBlock extends Entity {

    private static final double SPEED = 1.2;

    private static final int MAX_LIFE = 200;

    private static final int BEAT = 2;

    private float poseX, poseY, poseZ;

    private BlockState blockState = Blocks.STONE.defaultBlockState();

    private UUID ownerUUID = null;

    private UUID immuneUUID = null;
    private int homingTargetId = -1;
    private float damage = 0;
    private double maxDist = 30;
    private Vec3 dir = Vec3.ZERO;
    private double traveled = 0;
    private final Set<Integer> hitEntities = new HashSet<>();

    public EntityShiftBlock(EntityType<? extends EntityShiftBlock> type, Level level) {
        super(type, level);
        rollPose();
    }

    private void rollPose() {
        poseX = random.nextFloat() * 360f;
        poseY = random.nextFloat() * 360f;
        poseZ = random.nextFloat() * 360f;
    }

    public float getPoseX() {
        return poseX;
    }

    public float getPoseY() {
        return poseY;
    }

    public float getPoseZ() {
        return poseZ;
    }

    public EntityShiftBlock(Player owner, BlockState state, Vec3 bottomPos, Vec3 dir,
                            double maxDist, float damage) {
        this(ACEntities.SHIFT_BLOCK.get(), owner.level());
        this.ownerUUID = owner.getUUID();
        this.immuneUUID = owner.getUUID();
        this.blockState = state;
        this.dir = dir.normalize();
        this.maxDist = maxDist;
        this.damage = damage;
        setPos(bottomPos.x, bottomPos.y, bottomPos.z);
        setDeltaMovement(this.dir.scale(SPEED));
    }

    public BlockState getBlockState() {
        return blockState;
    }

    public void setHomingTarget(int entityId) {
        this.homingTargetId = entityId;
    }

    @javax.annotation.Nullable
    public Player getOwnerPlayer() {
        return ownerUUID != null ? level().getPlayerByUUID(ownerUUID) : null;
    }

    public void deflect(Vec3 newDir, Player reflector, @javax.annotation.Nullable Entity homingTarget) {
        this.dir = newDir.normalize();
        setDeltaMovement(this.dir.scale(SPEED));
        this.immuneUUID = reflector.getUUID();
        this.homingTargetId = homingTarget != null ? homingTarget.getId() : -1;
        this.traveled = 0;
        this.hitEntities.clear();
        this.hurtMarked = true;
    }

    @Override
    public void tick() {
        super.tick();

        if (tickCount % BEAT == 0) {
            rollPose();
            if (!level().isClientSide) {
                level().playSound(null, getX(), getY() + getBbHeight() / 2.0, getZ(),
                        cn.academy.ACSounds.TP_MOVE_BLOCK_SPEED.get(),
                        SoundSource.AMBIENT, 1.0f, 1.0f);
            }
        }

        if (level().isClientSide) {

            setPos(getX() + getDeltaMovement().x, getY() + getDeltaMovement().y, getZ() + getDeltaMovement().z);
            if (RandUtils.ranged(0, 1) < 0.6) {
                level().addParticle(ACParticles.TP.get(),
                        getX() + RandUtils.ranged(-.4, .4),
                        getY() + RandUtils.ranged(0, 1),
                        getZ() + RandUtils.ranged(-.4, .4),
                        RandUtils.ranged(-.02, .02), RandUtils.ranged(0, .03), RandUtils.ranged(-.02, .02));
            }
            return;
        }

        if (tickCount > MAX_LIFE) {
            finish(position());
            return;
        }

        if (homingTargetId >= 0) {
            Entity t = level().getEntity(homingTargetId);
            if (t != null && t.isAlive()) {
                Vec3 c = t.getBoundingBox().getCenter().subtract(0, getBbHeight() / 2.0, 0);
                Vec3 nd = c.subtract(position());
                if (nd.lengthSqr() > 1e-6) {
                    dir = nd.normalize();
                }
            } else {
                homingTargetId = -1;
            }
        }

        double step = Math.min(SPEED, maxDist - traveled);
        Vec3 from = position();
        Vec3 to = from.add(dir.scale(step));

        Player owner = ownerUUID != null ? level().getPlayerByUUID(ownerUUID) : null;

        Entity immune = immuneUUID != null ? level().getPlayerByUUID(immuneUUID) : owner;
        AABB sweep = getBoundingBox().expandTowards(to.subtract(from)).inflate(0.25);
        for (LivingEntity le : level().getEntitiesOfClass(LivingEntity.class, sweep,
                e -> e.isAlive() && e != immune && !hitEntities.contains(e.getId())

                        && (owner == null || AbilityPipeline.canTarget(owner, e)))) {
            hitEntities.add(le.getId());

            AbilityContext actx = owner == null ? null
                    : AbilityContext.ofIfReady(owner, ShiftTeleport.INSTANCE);
            if (actx != null) {

                cn.academy.ability.vanilla.teleporter.util.TPSkillHelper.attack(actx, le, damage);
                actx.addSkillExp(0.002f);
            }
            if (le.getId() == homingTargetId) {
                finish(position());
                return;
            }
        }

        Vec3 half = new Vec3(0, getBbHeight() / 2.0, 0);
        BlockHitResult hit = level().clip(new ClipContext(from.add(half), to.add(half),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (hit.getType() == HitResult.Type.BLOCK) {
            finishAt(hit.getBlockPos().relative(hit.getDirection()), hit.getLocation(), hit);
            return;
        }

        setPos(to.x, to.y, to.z);
        setDeltaMovement(dir.scale(SPEED));
        traveled += step;
        if (traveled >= maxDist - 1e-6) {
            finish(to);
        }
    }

    private void finish(Vec3 at) {
        finishAt(BlockPos.containing(at.x, at.y + getBbHeight() / 2.0, at.z), at, null);
    }

    private void finishAt(BlockPos bp, Vec3 fallback, BlockHitResult hit) {
        if (!level().isClientSide) {
            BlockState partner = getPartnerState();
            boolean pair = !partner.isAir();
            BlockPos pp = pair ? bp.relative(getPartnerDir()) : null;

            boolean canPlace = level().getBlockState(bp).canBeReplaced()
                    && blockState.canSurvive(level(), bp)
                    && (!pair || level().getBlockState(pp).canBeReplaced());
            if (canPlace) {
                level().setBlock(bp, blockState, 3);
                if (pair) {
                    level().setBlock(pp, partner, 3);
                }
                level().playSound(null, bp, blockState.getSoundType().getPlaceSound(),
                        SoundSource.BLOCKS, 1.0f, 0.9f);
            } else if (!pair && hit != null && placeLikePlayer(hit)) {

            } else {

                ItemStack drop = blockState.getBlock().getCloneItemStack(level(), bp, blockState);
                if (!drop.isEmpty()) {
                    level().addFreshEntity(new ItemEntity(level(),
                            fallback.x, fallback.y + getBbHeight() / 2.0, fallback.z, drop));
                }
            }
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(ACParticles.TP.get(),
                        fallback.x, fallback.y + 0.5, fallback.z, 12, 0.4, 0.4, 0.4, 0.02);
            }
        }
        discard();
    }

    private boolean placeLikePlayer(BlockHitResult hit) {
        ItemStack stack = blockState.getBlock().getCloneItemStack(level(), hit.getBlockPos(), blockState);
        if (!(stack.getItem() instanceof BlockItem bi)) {
            return false;
        }
        Player owner = ownerUUID != null ? level().getPlayerByUUID(ownerUUID) : null;
        BlockPlaceContext ctx = new BlockPlaceContext(
                new UseOnContext(level(), owner, InteractionHand.MAIN_HAND, stack, hit));
        return bi.place(ctx).consumesAction();
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this, Block.getId(blockState));
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket pkt) {
        super.recreateFromPacket(pkt);
        blockState = Block.stateById(pkt.getData());
    }

    private static final EntityDataAccessor<Integer> DATA_PARTNER_STATE =
            SynchedEntityData.defineId(EntityShiftBlock.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> DATA_PARTNER_DIR =
            SynchedEntityData.defineId(EntityShiftBlock.class, EntityDataSerializers.INT);

    public void setPartner(BlockState state, Direction dir) {
        entityData.set(DATA_PARTNER_STATE, Block.getId(state));
        entityData.set(DATA_PARTNER_DIR, dir.ordinal());
    }

    public BlockState getPartnerState() {
        return Block.stateById(entityData.get(DATA_PARTNER_STATE));
    }

    public Direction getPartnerDir() {
        return Direction.values()[entityData.get(DATA_PARTNER_DIR) % Direction.values().length];
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_PARTNER_STATE, 0);
        entityData.define(DATA_PARTNER_DIR, Direction.UP.ordinal());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}
}
