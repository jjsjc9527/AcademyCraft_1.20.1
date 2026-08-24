package cn.academy.entity;

import cn.academy.ACEntities;
import cn.academy.ACParticles;
import cn.academy.ability.AbilityContext;
import cn.academy.ability.AbilityPipeline;
import cn.academy.ability.vanilla.teleporter.skill.ShiftTeleport;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class EntityShiftNeedle extends Entity {

    private static final double SPEED = 0.6;

    public static final int HOVER_TICKS = 4;

    private static final double EMBED = 0.25;

    private static final int MAX_LIFE = 120;

    private static final double DEFLECT_SPEED = 1.5;

    private static final double DEFLECT_RANGE = 16.0;

    private static final EntityDataAccessor<Integer> DATA_TARGET =
            SynchedEntityData.defineId(EntityShiftNeedle.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> DATA_OWNER =
            SynchedEntityData.defineId(EntityShiftNeedle.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Boolean> DATA_DEFLECTED =
            SynchedEntityData.defineId(EntityShiftNeedle.class, EntityDataSerializers.BOOLEAN);

    private UUID ownerUUID = null;

    private UUID immuneUUID = null;

    private double deflectTraveled = 0;
    private float damage = 0;

    public EntityShiftNeedle(EntityType<? extends EntityShiftNeedle> type, Level level) {
        super(type, level);
    }

    public EntityShiftNeedle(Player owner, LivingEntity target, float damage, Vec3 center) {
        this(ACEntities.SHIFT_NEEDLE.get(), owner.level());
        this.ownerUUID = owner.getUUID();
        this.damage = damage;
        setPos(center.x, center.y - getBbHeight() / 2.0, center.z);
        entityData.set(DATA_TARGET, target.getId());
        entityData.set(DATA_OWNER, owner.getId());
    }

    public boolean isOwnedBy(Player p) {
        return p != null && entityData.get(DATA_OWNER) == p.getId();
    }

    @javax.annotation.Nullable
    public Player getOwnerPlayer() {
        return ownerUUID != null ? level().getPlayerByUUID(ownerUUID) : null;
    }

    public boolean isDeflected() {
        return entityData.get(DATA_DEFLECTED);
    }

    private double speed() {
        return isDeflected() ? DEFLECT_SPEED : SPEED;
    }

    public void deflect(Vec3 newDir, Player reflector, @javax.annotation.Nullable Entity newTarget) {
        entityData.set(DATA_DEFLECTED, true);
        entityData.set(DATA_TARGET, newTarget != null ? newTarget.getId() : -1);
        this.immuneUUID = reflector.getUUID();
        this.deflectTraveled = 0;
        Vec3 d = newDir.lengthSqr() > 1.0e-8 ? newDir.normalize() : new Vec3(1, 0, 0);
        setDeltaMovement(d.scale(DEFLECT_SPEED));
        this.hurtMarked = true;
    }

    public boolean isStuck() {
        return stuck;
    }

    public int getTargetId() {
        return entityData.get(DATA_TARGET);
    }

    private void followTarget(Entity t) {
        if (t instanceof LivingEntity le) {
            Vec3 off = rotY(stuckLocalPos, le.yBodyRot);
            setPos(t.getX() + off.x, t.getY() + off.y - getBbHeight() / 2.0, t.getZ() + off.z);
        }
    }

    private Vec3 center() {
        return new Vec3(getX(), getY() + getBbHeight() / 2.0, getZ());
    }

    private float[] aimFractions() {
        if (aimFrac == null) {
            RandomSource rnd = RandomSource.create(getId());
            aimFrac = new float[]{
                    0.25f + rnd.nextFloat() * 0.5f,
                    0.10f + rnd.nextFloat() * 0.8f,
                    0.25f + rnd.nextFloat() * 0.5f};
        }
        return aimFrac;
    }

    private float[] aimFrac = null;

    public float aimHeightFraction() {
        return aimFractions()[1];
    }

    private Vec3 aimPointOn(Entity t) {
        float[] f = aimFractions();
        AABB bb = t.getBoundingBox();
        return new Vec3(Mth.lerp(f[0], bb.minX, bb.maxX),
                Mth.lerp(f[1], bb.minY, bb.maxY),
                Mth.lerp(f[2], bb.minZ, bb.maxZ));
    }

    private boolean stuck = false;

    private boolean settled = false;

    private Vec3 stuckLocalPos = Vec3.ZERO;
    private Vec3 stuckLocalDir = new Vec3(1, 0, 0);

    private static Vec3 rotY(Vec3 v, float yawDeg) {
        double r = Math.toRadians(yawDeg);
        double cos = Math.cos(r), sin = Math.sin(r);
        return new Vec3(v.x * cos - v.z * sin, v.y, v.x * sin + v.z * cos);
    }

    public Vec3 getRenderDir() {
        Entity t = getTarget();
        if (stuck && t instanceof LivingEntity le) {
            return rotY(stuckLocalDir, le.yBodyRot);
        }
        Vec3 dm = getDeltaMovement();
        if (dm.lengthSqr() > 1.0e-6) {
            return dm;
        }
        return t != null ? t.getBoundingBox().getCenter().subtract(center()) : new Vec3(1, 0, 0);
    }

    public Entity getTarget() {
        int id = entityData.get(DATA_TARGET);
        return id >= 0 ? level().getEntity(id) : null;
    }

    @Override
    public void tick() {
        super.tick();

        Entity t = getTarget();

        if (t == null && isDeflected() && !stuck) {
            tickDeflectedFlight();
            return;
        }
        if (t == null) {

            if (!level().isClientSide) {
                if (stuck) {
                    dropHere();
                } else {
                    settle(true);
                }
            }
            return;
        }

        if (!stuck) {
            spawnFlyParticle();
        }

        if (stuck) {

            if (tickCount % 10 == 0) {
                followTarget(t);
            }
            setDeltaMovement(Vec3.ZERO);

            if (!level().isClientSide && !t.isAlive()) {
                followTarget(t);
                dropHere();
            }
            return;
        }

        if (tickCount < HOVER_TICKS) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }

        double sp = speed();
        Vec3 to = aimPointOn(t);
        Vec3 d = to.subtract(center());
        double dist = d.length();

        if (tickCount == HOVER_TICKS) {
            setDeltaMovement(dist > 1.0e-4 ? d.scale(sp / dist) : Vec3.ZERO);
            return;
        }

        Vec3 step = dist > 1.0e-4 ? d.scale(Math.min(sp, dist) / dist) : Vec3.ZERO;

        if (isDeflected() && step.lengthSqr() > 1.0e-8) {
            BlockHitResult bh = level().clip(new ClipContext(center(), center().add(step),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (bh.getType() == HitResult.Type.BLOCK) {
                Vec3 at = bh.getLocation();
                setPos(at.x, at.y - getBbHeight() / 2.0, at.z);
                setDeltaMovement(Vec3.ZERO);
                if (!level().isClientSide) {
                    dropHere();
                }
                return;
            }
        }

        setPos(getX() + step.x, getY() + step.y, getZ() + step.z);
        setDeltaMovement(step);

        if (dist <= sp) {
            Vec3 dir = step.lengthSqr() > 1.0e-8 ? step.normalize() : new Vec3(1, 0, 0);
            float yaw = t instanceof LivingEntity le ? le.yBodyRot : 0;

            Vec3 hit = center().add(dir.scale(EMBED));
            stuckLocalPos = rotY(hit.subtract(t.position()), -yaw);
            stuckLocalDir = rotY(dir, -yaw);
            stuck = true;
            if (level().isClientSide) {

                cn.academy.client.render.entity.StuckNeedles.add(getTargetId(), this);
            }
            if (!level().isClientSide) {
                Player owner = ownerUUID != null ? level().getPlayerByUUID(ownerUUID) : null;

                AbilityContext octx = owner == null ? null
                        : AbilityContext.ofIfReady(owner, ShiftTeleport.INSTANCE);
                if (octx != null) {

                    t.invulnerableTime = -1;

                    cn.academy.ability.vanilla.teleporter.util.TPSkillHelper.attack(
                            octx, t, damage);
                }
            }
            return;
        }

        if (!level().isClientSide && (!t.isAlive() || tickCount > MAX_LIFE)) {
            settle(true);
        }
    }

    private void spawnFlyParticle() {
        if (level().isClientSide && random.nextFloat() < 0.35f) {
            level().addParticle(ACParticles.TP.get(),
                    getX() + (random.nextDouble() - 0.5) * 0.3,
                    center().y + (random.nextDouble() - 0.5) * 0.3,
                    getZ() + (random.nextDouble() - 0.5) * 0.3,
                    0, 0.01, 0);
        }
    }

    private void tickDeflectedFlight() {
        Vec3 dm = getDeltaMovement();

        if (level().isClientSide) {
            setPos(getX() + dm.x, getY() + dm.y, getZ() + dm.z);
            spawnFlyParticle();
            return;
        }

        if (dm.lengthSqr() < 1.0e-8 || tickCount > MAX_LIFE) {
            dropHere();
            return;
        }

        Vec3 from = center();
        Vec3 to = from.add(dm);

        Entity immune = immuneUUID != null ? level().getPlayerByUUID(immuneUUID) : null;
        Player needleOwner = getOwnerPlayer();
        for (LivingEntity le : level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().expandTowards(dm),
                e -> e.isAlive() && e != immune

                        && (needleOwner == null || AbilityPipeline.canTarget(needleOwner, e)))) {
            entityData.set(DATA_TARGET, le.getId());
            return;
        }

        BlockHitResult hit = level().clip(new ClipContext(from, to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (hit.getType() == HitResult.Type.BLOCK) {
            Vec3 at = hit.getLocation();
            setPos(at.x, at.y - getBbHeight() / 2.0, at.z);
            dropHere();
            return;
        }

        setPos(getX() + dm.x, getY() + dm.y, getZ() + dm.z);
        deflectTraveled += dm.length();
        if (deflectTraveled >= DEFLECT_RANGE) {
            dropHere();
        }
    }

    public void retrieveToOwner() {
        if (!level().isClientSide) {
            settle(true);
        }
    }

    private void dropHere() {
        settle(false);
    }

    private void settle(boolean toOwner) {
        if (!level().isClientSide) {
            settled = true;
            ItemStack back = new ItemStack(cn.academy.ACItems.NEEDLE.get());
            Player owner = ownerUUID != null ? level().getPlayerByUUID(ownerUUID) : null;
            if (toOwner && owner != null && owner.getInventory().add(back)) {

            } else {
                Vec3 at = toOwner && owner != null ? owner.position() : center();
                level().addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(
                        level(), at.x, at.y, at.z, back));
            }
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(ACParticles.TP.get(), getX(), center().y, getZ(),
                        6, 0.15, 0.15, 0.15, 0.02);
            }
        }
        discard();
    }

    @Override
    public void remove(RemovalReason reason) {
        if (level().isClientSide) {
            cn.academy.client.render.entity.StuckNeedles.remove(getTargetId(), this);
        }
        if (!level().isClientSide && !settled) {
            settled = true;
            ItemStack back = new ItemStack(cn.academy.ACItems.NEEDLE.get());
            Player owner = ownerUUID != null ? level().getPlayerByUUID(ownerUUID) : null;
            if (owner == null || !owner.getInventory().add(back)) {
                Vec3 at = owner != null ? owner.position() : center();
                level().addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(
                        level(), at.x, at.y, at.z, back));
            }
        }
        super.remove(reason);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_TARGET, -1);
        entityData.define(DATA_OWNER, -1);
        entityData.define(DATA_DEFLECTED, false);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}
}
