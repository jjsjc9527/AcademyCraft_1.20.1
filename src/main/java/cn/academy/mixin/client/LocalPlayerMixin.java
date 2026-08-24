package cn.academy.mixin.client;

import cn.academy.gravity.ACGravity;
import cn.academy.gravity.RotationUtil;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin extends AbstractClientPlayer {

    protected LocalPlayerMixin(ClientLevel level, GameProfile profile) {
        super(level, profile);
    }

    @Unique
    private Direction academy$grav() {
        return ACGravity.getGravityDirection(this);
    }

    @Unique
    private boolean academy$suffocatesAt(BlockPos pos) {
        Direction g = academy$grav();
        AABB playerBox = this.getBoundingBox();
        AABB box;
        if (g == Direction.DOWN) {
            box = new AABB(pos.getX(), playerBox.minY, pos.getZ(),
                    pos.getX() + 1.0D, playerBox.maxY, pos.getZ() + 1.0D);
        } else {
            Vec3 playerMask = RotationUtil.maskPlayerToWorld(0.0D, 1.0D, 0.0D, g);
            AABB posBox = new AABB(pos);
            Vec3 posMask = RotationUtil.maskPlayerToWorld(1.0D, 0.0D, 1.0D, g);
            box = new AABB(
                    playerMask.multiply(playerBox.minX, playerBox.minY, playerBox.minZ)
                            .add(posMask.multiply(posBox.minX, posBox.minY, posBox.minZ)),
                    playerMask.multiply(playerBox.maxX, playerBox.maxY, playerBox.maxZ)
                            .add(posMask.multiply(posBox.maxX, posBox.maxY, posBox.maxZ)));
        }
        return this.level().collidesWithSuffocatingBlock(this, box.deflate(1.0E-7D));
    }

    @Inject(
        method = "Lnet/minecraft/client/player/LocalPlayer;moveTowardsClosestSpace(DD)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void academy$moveTowardsClosestSpace(double x, double z, CallbackInfo ci) {
        Direction g = academy$grav();
        if (g == Direction.DOWN) return;
        ci.cancel();

        Vec3 pos = RotationUtil.vecPlayerToWorld(x - this.getX(), 0.0D, z - this.getZ(), g).add(this.position());
        BlockPos blockPos = BlockPos.containing(pos);
        if (!academy$suffocatesAt(blockPos)) return;

        double dx = pos.x - blockPos.getX();
        double dy = pos.y - blockPos.getY();
        double dz = pos.z - blockPos.getZ();
        Direction escape = null;
        double minDistToEdge = Double.MAX_VALUE;
        for (Direction playerDir : new Direction[]{Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH}) {
            Direction worldDir = RotationUtil.dirPlayerToWorld(playerDir, g);
            double comp = worldDir.getAxis().choose(dx, dy, dz);
            double distToEdge = worldDir.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1.0D - comp : comp;
            if (distToEdge < minDistToEdge && !academy$suffocatesAt(blockPos.relative(worldDir))) {
                minDistToEdge = distToEdge;
                escape = playerDir;
            }
        }
        if (escape != null) {
            Vec3 v = this.getDeltaMovement();
            if (escape.getAxis() == Direction.Axis.X) {
                this.setDeltaMovement(0.1D * escape.getStepX(), v.y, v.z);
            } else if (escape.getAxis() == Direction.Axis.Z) {
                this.setDeltaMovement(v.x, v.y, 0.1D * escape.getStepZ());
            }
        }
    }
}
