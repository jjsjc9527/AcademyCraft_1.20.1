package cn.academy.mixin;

import cn.academy.gravity.ACGravity;
import cn.academy.gravity.GravityEntity;
import cn.academy.gravity.RotationUtil;
import com.google.common.collect.ImmutableList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(Entity.class)
public abstract class EntityMixin implements GravityEntity {

    @Inject(method = "Lnet/minecraft/world/entity/Entity;rideTick()V",
            at = @At("HEAD"), cancellable = true)
    private void academy$controlTickWhileRiding(CallbackInfo ci) {
        if (cn.academy.ability.vanilla.mentalout.ControlTick.tick((Entity) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(method = "Lnet/minecraft/world/entity/Entity;rideTick()V", at = @At("RETURN"))
    private void academy$dazeUntickWhileRiding(CallbackInfo ci) {
        cn.academy.ability.vanilla.mentalout.ControlTick.untick((Entity) (Object) this);
    }

    @Inject(method = "Lnet/minecraft/world/entity/Entity;push(DDD)V",
            at = @At("HEAD"), cancellable = true)
    private void academy$dualWingNoPush(double dx, double dy, double dz, CallbackInfo ci) {
        if (cn.academy.ability.vanilla.vecmanip.advanced.DualWing.isWingOn((Entity) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(method = "Lnet/minecraft/world/entity/Entity;setRemoved(Lnet/minecraft/world/entity/Entity$RemovalReason;)V",
            at = @At("HEAD"), cancellable = true)
    private void academy$guardBlocksRemoval(Entity.RemovalReason reason, CallbackInfo ci) {
        if (cn.academy.ability.vanilla.vecmanip.advanced.WhiteWingGuard
                .shouldBlockRemoval((Entity) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(method = "Lnet/minecraft/world/entity/Entity;isEffectiveAi()Z",
            at = @At("HEAD"), cancellable = true)
    private void academy$proxyStopsAi(CallbackInfoReturnable<Boolean> cir) {
        if (cn.academy.ability.vanilla.mentalout.ProxyState.isProxied((Entity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "Lnet/minecraft/world/entity/Entity;isControlledByLocalInstance()Z",
            at = @At("HEAD"), cancellable = true)
    private void academy$proxyKeepsMoveAuthority(CallbackInfoReturnable<Boolean> cir) {
        if (cn.academy.ability.vanilla.mentalout.ProxyState
                .linkDrivingMob((Entity) (Object) this) != null) {
            cir.setReturnValue(true);
        }
    }

    @Unique
    private Direction academy$gravityDirection = Direction.DOWN;

    @Override
    public Direction academy_getGravityDirection() {
        return academy$gravityDirection;
    }

    @Override
    public void academy_setGravityDirection(Direction dir, boolean animate) {

        this.academy$gravityDirection = dir;
    }

    @Unique
    private cn.academy.gravity.RotationAnimation academy$rotationAnimation;

    @Override
    public cn.academy.gravity.RotationAnimation academy_getRotationAnimation() {
        if (this.academy$rotationAnimation == null) {
            this.academy$rotationAnimation = new cn.academy.gravity.RotationAnimation();
        }
        return this.academy$rotationAnimation;
    }

    @Inject(method = "Lnet/minecraft/world/entity/Entity;saveWithoutId(Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/nbt/CompoundTag;", at = @At("TAIL"))
    private void academy$saveGravity(CompoundTag tag, CallbackInfoReturnable<CompoundTag> cir) {
        if (this.academy$gravityDirection != Direction.DOWN) {
            tag.putString("AcademyGravity", this.academy$gravityDirection.getSerializedName());
        }
    }

    @Inject(method = "Lnet/minecraft/world/entity/Entity;load(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("TAIL"))
    private void academy$loadGravity(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains("AcademyGravity", 8)) {
            Direction dir = Direction.byName(tag.getString("AcademyGravity"));
            if (dir != null && dir != Direction.DOWN) {
                cn.academy.gravity.ACGravity.initGravityDirection((Entity) (Object) this, dir);
            }
        }
    }

    @Unique
    private Direction academy$grav() {
        return this.academy$gravityDirection;
    }

    @Shadow private Vec3 position;
    @Shadow private float eyeHeight;
    @Shadow public double xo;
    @Shadow public double yo;
    @Shadow public double zo;
    @Shadow private EntityDimensions dimensions;
    @Shadow protected RandomSource random;
    @Shadow public abstract double getX();
    @Shadow public abstract double getY();
    @Shadow public abstract double getZ();
    @Shadow public abstract Level level();
    @Shadow public abstract Vec3 getDeltaMovement();

    @Inject(
        method = "Lnet/minecraft/world/entity/Entity;getBoundingBoxForPose(Lnet/minecraft/world/entity/Pose;)Lnet/minecraft/world/phys/AABB;",
        at = @At("RETURN"),
        cancellable = true
    )
    private void academy$getBoundingBoxForPose(Pose pose, CallbackInfoReturnable<AABB> cir) {
        Direction g = academy$grav();
        if (g == Direction.DOWN) return;
        cir.setReturnValue(RotationUtil.makeBoxFromDimensions(
                ((Entity) (Object) this).getDimensions(pose), g, this.position));
    }

    @Inject(
        method = "Lnet/minecraft/world/entity/Entity;makeBoundingBox()Lnet/minecraft/world/phys/AABB;",
        at = @At("RETURN"),
        cancellable = true
    )
    private void academy$makeBoundingBox(CallbackInfoReturnable<AABB> cir) {
        if (((Entity) (Object) this) instanceof Projectile) return;
        Direction g = academy$grav();
        if (g == Direction.DOWN) return;

        AABB box = cir.getReturnValue().move(this.position.reverse());
        if (g.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
            box = box.move(0.0D, -1.0E-6D, 0.0D);
        }
        cir.setReturnValue(RotationUtil.boxPlayerToWorld(box, g).move(this.position));
    }

    @Inject(
        method = "Lnet/minecraft/world/entity/Entity;calculateViewVector(FF)Lnet/minecraft/world/phys/Vec3;",
        at = @At("RETURN"),
        cancellable = true
    )
    private void academy$calculateViewVector(CallbackInfoReturnable<Vec3> cir) {
        Direction g = academy$grav();
        if (g == Direction.DOWN) return;
        cir.setReturnValue(RotationUtil.vecPlayerToWorld(cir.getReturnValue(), g));
    }

    @Inject(
        method = "Lnet/minecraft/world/entity/Entity;getBlockPosBelowThatAffectsMyMovement()Lnet/minecraft/core/BlockPos;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void academy$getBlockPosBelow(CallbackInfoReturnable<BlockPos> cir) {
        Direction g = academy$grav();
        if (g == Direction.DOWN) return;
        cir.setReturnValue(BlockPos.containing(
                this.position.add(Vec3.atLowerCornerOf(g.getNormal()).scale(0.5000001D))));
    }

    @Inject(
        method = "Lnet/minecraft/world/entity/Entity;getEyePosition()Lnet/minecraft/world/phys/Vec3;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void academy$getEyePosition(CallbackInfoReturnable<Vec3> cir) {
        Direction g = academy$grav();
        if (g == Direction.DOWN) return;
        cir.setReturnValue(RotationUtil.vecPlayerToWorld(0.0D, this.eyeHeight, 0.0D, g).add(this.position));
    }

    @Inject(
        method = "Lnet/minecraft/world/entity/Entity;getEyePosition(F)Lnet/minecraft/world/phys/Vec3;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void academy$getEyePositionPartial(float tickDelta, CallbackInfoReturnable<Vec3> cir) {
        Direction g = academy$grav();
        if (g == Direction.DOWN) return;
        Vec3 off = RotationUtil.vecPlayerToWorld(0.0D, this.eyeHeight, 0.0D, g);
        double d = Mth.lerp((double) tickDelta, this.xo, this.getX()) + off.x;
        double e = Mth.lerp((double) tickDelta, this.yo, this.getY()) + off.y;
        double f = Mth.lerp((double) tickDelta, this.zo, this.getZ()) + off.z;
        cir.setReturnValue(new Vec3(d, e, f));
    }

    @ModifyVariable(
        method = "Lnet/minecraft/world/entity/Entity;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V",
        at = @At("HEAD"),
        ordinal = 0,
        argsOnly = true
    )
    private Vec3 academy$move_in(Vec3 vec3d) {
        Direction g = academy$grav();
        return g == Direction.DOWN ? vec3d : RotationUtil.vecPlayerToWorld(vec3d, g);
    }

    @ModifyVariable(
        method = "Lnet/minecraft/world/entity/Entity;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;pop()V", ordinal = 0),
        ordinal = 0,
        argsOnly = true
    )
    private Vec3 academy$move_argBack(Vec3 vec3d) {
        Direction g = academy$grav();
        return g == Direction.DOWN ? vec3d : RotationUtil.vecWorldToPlayer(vec3d, g);
    }

    @ModifyVariable(
        method = "Lnet/minecraft/world/entity/Entity;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;pop()V", ordinal = 0),
        ordinal = 1
    )
    private Vec3 academy$move_collideResultBack(Vec3 vec3d) {
        Direction g = academy$grav();
        return g == Direction.DOWN ? vec3d : RotationUtil.vecWorldToPlayer(vec3d, g);
    }

    @Inject(
        method = "Lnet/minecraft/world/entity/Entity;getOnPos(F)Lnet/minecraft/core/BlockPos;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void academy$getOnPos(float offset, CallbackInfoReturnable<BlockPos> cir) {
        Direction g = academy$grav();
        if (g == Direction.DOWN) return;
        cir.setReturnValue(BlockPos.containing(
                RotationUtil.vecPlayerToWorld(0.0D, -(double) offset, 0.0D, g).add(this.position)));
    }

    @Inject(
        method = "Lnet/minecraft/world/entity/Entity;spawnSprintParticle()V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void academy$spawnSprintParticle(CallbackInfo ci) {
        Direction g = academy$grav();
        if (g == Direction.DOWN) return;
        ci.cancel();

        Vec3 floorPos = this.position.subtract(RotationUtil.vecPlayerToWorld(0.0D, 0.20000000298023224D, 0.0D, g));
        BlockPos floorBlock = BlockPos.containing(floorPos);
        BlockState floorState = this.level().getBlockState(floorBlock);
        if (floorState.getRenderShape() == RenderShape.INVISIBLE) return;

        Vec3 off = RotationUtil.vecPlayerToWorld(
                (this.random.nextDouble() - 0.5D) * (double) this.dimensions.width, 0.1D,
                (this.random.nextDouble() - 0.5D) * (double) this.dimensions.width, g);
        Vec3 pos = this.position.add(off);
        Vec3 vel = this.getDeltaMovement();
        Vec3 pv = RotationUtil.vecPlayerToWorld(vel.x * -4.0D, 1.5D, vel.z * -4.0D, g);
        this.level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, floorState),
                pos.x, pos.y, pos.z, pv.x, pv.y, pv.z);
    }

    @ModifyVariable(
        method = "collide",
        at = @At(value = "INVOKE_ASSIGN",
                target = "Lnet/minecraft/world/level/Level;getEntityCollisions(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;",
                ordinal = 0),
        ordinal = 0
    )
    private Vec3 academy$collide_toLocal(Vec3 vec3d) {
        Direction g = academy$grav();
        return g == Direction.DOWN ? vec3d : RotationUtil.vecWorldToPlayer(vec3d, g);
    }

    @Inject(
        method = "Lnet/minecraft/world/entity/Entity;collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;",
        at = @At("RETURN"),
        cancellable = true
    )
    private void academy$collide_toWorld(CallbackInfoReturnable<Vec3> cir) {
        Direction g = academy$grav();
        if (g == Direction.DOWN) return;
        cir.setReturnValue(RotationUtil.vecPlayerToWorld(cir.getReturnValue(), g));
    }

    @Redirect(
        method = "collide",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;expandTowards(DDD)Lnet/minecraft/world/phys/AABB;"),
        require = 0, expect = 1
    )
    private AABB academy$collide_stretch(AABB box, double x, double y, double z) {
        Direction g = academy$grav();
        if (g == Direction.DOWN) return box.expandTowards(x, y, z);
        Vec3 r = RotationUtil.vecPlayerToWorld(new Vec3(x, y, z), g);
        return box.expandTowards(r.x, r.y, r.z);
    }

    @ModifyArg(
        method = "collide",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;move(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/AABB;"),
        index = 0,
        require = 0, expect = 1
    )
    private Vec3 academy$collide_offset(Vec3 offset) {
        Direction g = academy$grav();
        return g == Direction.DOWN ? offset : RotationUtil.vecPlayerToWorld(offset, g);
    }

    @Inject(
        method = "Lnet/minecraft/world/entity/Entity;collideBoundingBox(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Lnet/minecraft/world/level/Level;Ljava/util/List;)Lnet/minecraft/world/phys/Vec3;",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void academy$cbb_takeOver(Entity entity, Vec3 movement, AABB box, Level level,
                                             List<VoxelShape> potentialHits,
                                             CallbackInfoReturnable<Vec3> cir) {
        if (entity == null) {
            return;
        }
        Direction g = ACGravity.getGravityDirection(entity);
        if (g == Direction.DOWN) {
            return;
        }

        Vec3 world = RotationUtil.vecPlayerToWorld(movement, g);

        ImmutableList.Builder<VoxelShape> shapes =
                ImmutableList.builderWithExpectedSize(potentialHits.size() + 1);
        if (!potentialHits.isEmpty()) {
            shapes.addAll(potentialHits);
        }

        AABB swept = box.expandTowards(world);
        WorldBorder border = level.getWorldBorder();
        if (border.isInsideCloseToBorder(entity, swept)) {
            shapes.add(border.getCollisionShape());
        }
        shapes.addAll(level.getBlockCollisions(entity, swept));

        cir.setReturnValue(RotationUtil.vecWorldToPlayer(
                academy$solveRotated(world, box, shapes.build(), g), g));
    }

    @Unique
    private static Vec3 academy$solveRotated(Vec3 movement, AABB box,
                                             List<VoxelShape> collisions, Direction g) {
        Vec3 pm = RotationUtil.vecWorldToPlayer(movement, g);
        double px = pm.x, py = pm.y, pz = pm.z;
        Direction dx = RotationUtil.dirPlayerToWorld(Direction.EAST, g);
        Direction dy = RotationUtil.dirPlayerToWorld(Direction.UP, g);
        Direction dz = RotationUtil.dirPlayerToWorld(Direction.SOUTH, g);

        if (py != 0.0D) {
            py = Shapes.collide(dy.getAxis(), box, collisions, py * dy.getAxisDirection().getStep())
                    * dy.getAxisDirection().getStep();
            if (py != 0.0D) box = box.move(RotationUtil.vecPlayerToWorld(0.0D, py, 0.0D, g));
        }

        boolean zLargerThanX = Math.abs(px) < Math.abs(pz);
        if (zLargerThanX && pz != 0.0D) {
            pz = Shapes.collide(dz.getAxis(), box, collisions, pz * dz.getAxisDirection().getStep())
                    * dz.getAxisDirection().getStep();
            if (pz != 0.0D) box = box.move(RotationUtil.vecPlayerToWorld(0.0D, 0.0D, pz, g));
        }

        if (px != 0.0D) {
            px = Shapes.collide(dx.getAxis(), box, collisions, px * dx.getAxisDirection().getStep())
                    * dx.getAxisDirection().getStep();
            if (!zLargerThanX && px != 0.0D) box = box.move(RotationUtil.vecPlayerToWorld(px, 0.0D, 0.0D, g));
        }

        if (!zLargerThanX && pz != 0.0D) {
            pz = Shapes.collide(dz.getAxis(), box, collisions, pz * dz.getAxisDirection().getStep())
                    * dz.getAxisDirection().getStep();
        }

        return RotationUtil.vecPlayerToWorld(px, py, pz, g);
    }

    @Redirect(
        method = "isInWall",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;ofSize(Lnet/minecraft/world/phys/Vec3;DDD)Lnet/minecraft/world/phys/AABB;", ordinal = 0),
        require = 0, expect = 1
    )
    private AABB academy$isInWall(Vec3 center, double x, double y, double z) {
        Direction g = academy$grav();
        if (g == Direction.DOWN) return AABB.ofSize(center, x, y, z);
        Vec3 r = RotationUtil.vecPlayerToWorld(new Vec3(x, y, z), g);
        return AABB.ofSize(center, r.x, r.y, r.z);
    }

    @Inject(method = "setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V",
            at = @At("HEAD"), cancellable = true)
    private void academy$immortalHoldMotion(Vec3 next, CallbackInfo ci) {
        if (cn.academy.util.MotionProbe.shouldBlock((Entity) (Object) this, next)) {
            ci.cancel();
        }
    }

    @Inject(method = "setPos(DDD)V", at = @At("HEAD"))
    private void academy$probePosWriter(double x, double y, double z, CallbackInfo ci) {
        cn.academy.util.MotionProbe.notePos((Entity) (Object) this, x, y, z);
    }
}
