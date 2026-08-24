package cn.academy.entity;

import cn.academy.ACEntities;
import cn.academy.ACSounds;
import cn.lambdalib2.util.RandUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class EntitySilbarn extends Entity {

    private static final double DRAG = 0.8;

    private static final double GRAVITY = 0.12;

    private static final int GRAVITY_DELAY = 50;

    private static final int LINGER_AFTER_HIT = 10;

    private static final int FRAG_FROM = 18, FRAG_TO = 27;

    private static final int TRICKLE_FROM = 2, TRICKLE_TO = 5;

    private static final EntityDataAccessor<Boolean> HIT =
            SynchedEntityData.defineId(EntitySilbarn.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Byte> CRACK =
            SynchedEntityData.defineId(EntitySilbarn.class, EntityDataSerializers.BYTE);

    private static final int BREAKING_MAX_TICKS = 200;

    private int lingerTicks = 0;

    private boolean fragSpawned = false;

    private int lastCrackSeen = 0;

    private long lastCrackMs = 0;

    private int breakingTicks = 0;

    public EntitySilbarn(EntityType<? extends EntitySilbarn> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public EntitySilbarn(Level level) {
        this(ACEntities.SILBARN.get(), level);
    }

    public static EntitySilbarn thrownBy(Player player) {
        EntitySilbarn e = new EntitySilbarn(player.level());
        Vec3 eye = player.getEyePosition();
        e.setPos(eye.x, eye.y, eye.z);

        e.setDeltaMovement(player.getViewVector(1.0f));

        e.setYRot(player.getYHeadRot());
        e.setXRot(player.getXRot());
        return e;
    }

    public static EntitySilbarn thrownBy(Player player, Vec3 dir) {
        EntitySilbarn e = new EntitySilbarn(player.level());
        Vec3 eye = player.getEyePosition();
        e.setPos(eye.x, eye.y, eye.z);
        Vec3 d = dir.lengthSqr() < 1.0e-8 ? player.getViewVector(1.0f) : dir.normalize();
        e.setDeltaMovement(d);
        e.setYRot((float) Math.toDegrees(Math.atan2(-d.x, d.z)));
        e.setXRot((float) -Math.toDegrees(Math.asin(Math.max(-1, Math.min(1, d.y)))));
        return e;
    }

    private static final int TURN_INTERVAL = 10;

    private static final float TURN_RANGE = 180f;

    private float turnAngleAt(int seg) {
        int h = getId() * 0x9E3779B1 + seg * 0x85EBCA6B;
        h ^= h >>> 15;
        h *= 0x2C1B3C6D;
        h ^= h >>> 12;
        h ^= h >>> 16;
        return ((h >>> 8) / (float) (1 << 24) * 2f - 1f) * TURN_RANGE;
    }

    public float turnRoll() {
        if (getCrack() == 0 || breakStartTick < 0) {
            return 0f;
        }
        return turnAngleAt((tickCount - breakStartTick) / TURN_INTERVAL);
    }

    private int breakStartTick = -1;

    @Override
    protected void defineSynchedData() {
        entityData.define(HIT, false);
        entityData.define(CRACK, (byte) 0);
    }

    public boolean isHit() {
        return entityData.get(HIT);
    }

    public long lastCrackMs() {
        return lastCrackMs;
    }

    public int getCrack() {
        return entityData.get(CRACK);
    }

    public boolean isIntact() {
        return !isHit() && getCrack() == 0;
    }

    public void setCrack(int percent) {
        if (level().isClientSide || isHit()) {
            return;
        }
        entityData.set(CRACK, (byte) Math.max(1, Math.min(100, percent)));
        setDeltaMovement(Vec3.ZERO);
    }

    public void shatter(boolean heavy) {
        if (level().isClientSide || isHit()) {
            return;
        }
        entityData.set(HIT, true);
        setDeltaMovement(Vec3.ZERO);
        level().playSound(null, getX(), getY(), getZ(),
                (heavy ? ACSounds.SILBARN_HEAVY : ACSounds.SILBARN_LIGHT).get(),
                SoundSource.NEUTRAL, 0.5f, 1.0f);
    }

    @Override
    public void tick() {
        super.tick();

        if (isHit()) {
            if (level().isClientSide) {
                if (!fragSpawned) {
                    fragSpawned = true;
                    spawnFragments(FRAG_FROM, FRAG_TO);
                }
            } else if (++lingerTicks > LINGER_AFTER_HIT) {
                discard();
            }
            return;
        }

        if (getCrack() > 0) {
            if (level().isClientSide) {
                int c = getCrack();
                if (c > lastCrackSeen) {
                    if (lastCrackSeen == 0) {

                        breakStartTick = tickCount;
                    }
                    lastCrackSeen = c;
                    lastCrackMs = System.currentTimeMillis();
                    spawnFragments(TRICKLE_FROM, TRICKLE_TO);
                }
            } else if (++breakingTicks > BREAKING_MAX_TICKS) {
                shatter(true);
            }
            return;
        }

        Vec3 from = position();
        Vec3 to = from.add(getDeltaMovement());
        BlockHitResult hit = level().clip(new ClipContext(
                from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (hit.getType() == HitResult.Type.BLOCK) {
            Vec3 at = hit.getLocation();
            setPos(at.x, at.y, at.z);
            shatter(false);
            return;
        }
        setPos(to.x, to.y, to.z);

        Vec3 v = getDeltaMovement();
        if (tickCount > GRAVITY_DELAY) {
            v = v.subtract(0, GRAVITY, 0);
        }
        setDeltaMovement(v.scale(DRAG));
    }

    private void spawnFragments(int from, int to) {
        int n = RandUtils.rangei(from, to);
        for (int i = 0; i < n; i++) {
            double vel = RandUtils.ranged(0.08, 0.18);
            double vsq = vel * vel;
            double vx = random.nextDouble() * vel;
            double vy = random.nextDouble() * Math.sqrt(Math.max(0, vsq - vx * vx));
            double vz = Math.sqrt(Math.max(0, vsq - vx * vx - vy * vy));
            if (random.nextBoolean()) vx = -vx;
            if (random.nextBoolean()) vy = -vy;
            if (random.nextBoolean()) vz = -vz;
            level().addParticle(cn.academy.ACParticles.SILBARN_FRAG.get(),
                    getX(), getY(), getZ(), vx, vy + 0.2, vz);
        }
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        discard();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}
}
