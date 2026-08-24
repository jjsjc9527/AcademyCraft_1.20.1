package cn.academy.mixin.client;

import cn.academy.gravity.ACGravity;
import cn.academy.gravity.RotationAnimation;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Camera.class, priority = 1001)
public abstract class CameraMixin {

    @Shadow protected abstract void setPosition(double x, double y, double z);
    @Shadow private Entity entity;
    @Shadow @Final private Quaternionf rotation;
    @Shadow private float eyeHeightOld;
    @Shadow private float eyeHeight;

    @Redirect(
        method = "setup",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(DDD)V", ordinal = 0),
        require = 0, expect = 1
    )
    private void academy$setup_setPos(Camera camera, double x, double y, double z,
                                      BlockGetter area, Entity focusedEntity, boolean thirdPerson,
                                      boolean inverseView, float tickDelta) {
        Direction g = ACGravity.getGravityDirection(focusedEntity);
        RotationAnimation anim = ACGravity.getRotationAnimation(focusedEntity);
        if (anim == null) {
            this.setPosition(x, y, z);
            return;
        }
        long timeMs = academy$timeMs(focusedEntity, tickDelta);
        anim.update(timeMs);
        if (g == Direction.DOWN && !anim.isInAnimation()) {
            this.setPosition(x, y, z);
            return;
        }
        Quaternionf gravityRotation = anim.getCurrentGravityRotation(g, timeMs);
        double ex = Mth.lerp((double) tickDelta, focusedEntity.xo, focusedEntity.getX());
        double ey = Mth.lerp((double) tickDelta, focusedEntity.yo, focusedEntity.getY());
        double ez = Mth.lerp((double) tickDelta, focusedEntity.zo, focusedEntity.getZ());
        double camY = Mth.lerp(tickDelta, this.eyeHeightOld, this.eyeHeight);
        Vec3 off = anim.getEyeOffset(gravityRotation, new Vec3(0.0D, camY, 0.0D), g);
        this.setPosition(ex + off.x, ey + off.y, ez + off.z);
    }

    @Inject(
        method = "Lnet/minecraft/client/Camera;setRotation(FF)V",
        at = @At(value = "INVOKE", target = "Lorg/joml/Quaternionf;rotationYXZ(FFF)Lorg/joml/Quaternionf;",
                shift = At.Shift.AFTER, remap = false)
    )
    private void academy$setRotation(CallbackInfo ci) {
        if (this.entity == null) return;
        Direction g = ACGravity.getGravityDirection(this.entity);
        RotationAnimation anim = ACGravity.getRotationAnimation(this.entity);
        if (anim == null) return;
        long timeMs = academy$timeMs(this.entity, Minecraft.getInstance().getFrameTime());
        anim.update(timeMs);
        if (g == Direction.DOWN && !anim.isInAnimation()) return;
        Quaternionf grav = new Quaternionf(anim.getCurrentGravityRotation(g, timeMs));
        grav.conjugate();
        grav.mul(this.rotation);
        this.rotation.set(grav.x(), grav.y(), grav.z(), grav.w());
    }

    @org.spongepowered.asm.mixin.Unique
    private static long academy$timeMs(Entity e, float partialTick) {
        return e.level().getGameTime() * 50L + (long) (partialTick * 50.0F);
    }
}
