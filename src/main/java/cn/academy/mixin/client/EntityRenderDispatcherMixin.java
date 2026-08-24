package cn.academy.mixin.client;

import cn.academy.gravity.ACGravity;
import cn.academy.gravity.RotationUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {

    @org.spongepowered.asm.mixin.injection.Redirect(
        method = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;render(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V")
    )
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void academy$dazeFreezePartialTick(
            net.minecraft.client.renderer.entity.EntityRenderer renderer, Entity entity,
            float rotationYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource buffers, int light) {
        renderer.render(entity, rotationYaw,
                cn.academy.ability.vanilla.mentalout.DazeState.renderFrozen(entity) ? 1.0F : partialTick,
                poseStack, buffers, light);
    }

    @org.spongepowered.asm.mixin.injection.Redirect(
        method = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;getRenderOffset(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/world/phys/Vec3;")
    )
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Vec3 academy$dazeFreezeRenderOffset(
            net.minecraft.client.renderer.entity.EntityRenderer renderer, Entity entity,
            float partialTick) {
        return renderer.getRenderOffset(entity,
                cn.academy.ability.vanilla.mentalout.DazeState.renderFrozen(entity) ? 1.0F : partialTick);
    }

    @Shadow @Final private static RenderType SHADOW_RENDER_TYPE;

    @Shadow private boolean shouldRenderShadow;

    @Shadow
    private static void shadowVertex(PoseStack.Pose entry, VertexConsumer vertices, float alpha,
                                     float x, float y, float z, float u, float v) {}

    @org.spongepowered.asm.mixin.Unique
    private static org.joml.Quaternionf academy$modelRotation(Entity entity, float tickDelta) {
        Direction g = ACGravity.getGravityDirection(entity);
        cn.academy.gravity.RotationAnimation anim = ACGravity.getRotationAnimation(entity);
        if (anim == null) return null;
        long timeMs = entity.level().getGameTime() * 50L + (long) (tickDelta * 50.0F);
        anim.update(timeMs);
        if (g == Direction.DOWN && !anim.isInAnimation()) return null;
        return new org.joml.Quaternionf(anim.getCurrentGravityRotation(g, timeMs)).conjugate();
    }

    @Inject(
        method = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(DDD)V", ordinal = 0, shift = At.Shift.AFTER)
    )
    private void academy$render_pushRotate(Entity entity, double x, double y, double z, float yaw, float tickDelta,
                                           PoseStack matrices, MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (!this.shouldRenderShadow) return;
        org.joml.Quaternionf rot = academy$modelRotation(entity, tickDelta);
        if (rot == null) return;
        matrices.pushPose();
        matrices.mulPose(rot);
    }

    @Inject(
        method = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(DDD)V", ordinal = 1)
    )
    private void academy$render_pop(Entity entity, double x, double y, double z, float yaw, float tickDelta,
                                    PoseStack matrices, MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (!this.shouldRenderShadow) return;
        if (academy$modelRotation(entity, tickDelta) == null) return;
        matrices.popPose();
    }

    @Inject(
        method = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(DDD)V", ordinal = 1, shift = At.Shift.AFTER)
    )
    private void academy$render_shadowRotate(Entity entity, double x, double y, double z, float yaw, float tickDelta,
                                             PoseStack matrices, MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (!this.shouldRenderShadow) return;
        org.joml.Quaternionf rot = academy$modelRotation(entity, tickDelta);
        if (rot == null) return;
        matrices.mulPose(rot);
    }

    @Inject(
        method = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;renderShadow(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/Entity;FFLnet/minecraft/world/level/LevelReader;F)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void academy$renderShadow(PoseStack matrices, MultiBufferSource buffers, Entity entity,
                                             float opacity, float tickDelta, LevelReader world, float radius, CallbackInfo ci) {
        Direction g = ACGravity.getGravityDirection(entity);
        if (g == Direction.DOWN) return;
        ci.cancel();

        double x = Mth.lerp(tickDelta, entity.xOld, entity.getX());
        double y = Mth.lerp(tickDelta, entity.yOld, entity.getY());
        double z = Mth.lerp(tickDelta, entity.zOld, entity.getZ());
        Vec3 minShadowPos = RotationUtil.vecPlayerToWorld((double) -radius, (double) -radius, (double) -radius, g).add(x, y, z);
        Vec3 maxShadowPos = RotationUtil.vecPlayerToWorld((double) radius, 0.0D, (double) radius, g).add(x, y, z);
        PoseStack.Pose entry = matrices.last();
        VertexConsumer vc = buffers.getBuffer(SHADOW_RENDER_TYPE);
        for (BlockPos bp : BlockPos.betweenClosed(BlockPos.containing(minShadowPos), BlockPos.containing(maxShadowPos))) {
            academy$renderShadowPart(entry, vc, world, bp, x, y, z, radius, opacity, g);
        }
    }

    private static void academy$renderShadowPart(PoseStack.Pose entry, VertexConsumer vertices, LevelReader world,
                                                 BlockPos pos, double x, double y, double z, float radius, float opacity, Direction g) {
        BlockPos posBelow = pos.relative(g);
        BlockState stateBelow = world.getBlockState(posBelow);
        if (stateBelow.getRenderShape() == RenderShape.INVISIBLE || world.getMaxLocalRawBrightness(pos) <= 3) return;
        if (!stateBelow.isCollisionShapeFullBlock(world, posBelow)) return;
        VoxelShape shape = stateBelow.getShape(world, posBelow);
        if (shape.isEmpty()) return;

        Vec3 playerPos = RotationUtil.vecWorldToPlayer(x, y, z, g);
        float alpha = (float) (((double) opacity
                - (playerPos.y - (RotationUtil.vecWorldToPlayer(Vec3.atCenterOf(pos), g).y - 0.5D)) / 2.0D)
                * 0.5D * (double) world.getLightLevelDependentMagicValue(pos));
        if (alpha < 0.0F) return;
        if (alpha > 1.0F) alpha = 1.0F;

        Vec3 centerPos = Vec3.atCenterOf(pos);
        Vec3 playerCenterPos = RotationUtil.vecWorldToPlayer(centerPos, g);
        Vec3 playerRelNN = playerCenterPos.add(-0.5D, -0.5D, -0.5D).subtract(playerPos);
        Vec3 playerRelPP = playerCenterPos.add(0.5D, -0.5D, 0.5D).subtract(playerPos);

        Vec3 relNN = RotationUtil.vecWorldToPlayer(centerPos.add(RotationUtil.vecPlayerToWorld(-0.5D, -0.5D, -0.5D, g)).subtract(x, y, z), g);
        Vec3 relNP = RotationUtil.vecWorldToPlayer(centerPos.add(RotationUtil.vecPlayerToWorld(-0.5D, -0.5D, 0.5D, g)).subtract(x, y, z), g);
        Vec3 relPN = RotationUtil.vecWorldToPlayer(centerPos.add(RotationUtil.vecPlayerToWorld(0.5D, -0.5D, -0.5D, g)).subtract(x, y, z), g);
        Vec3 relPP = RotationUtil.vecWorldToPlayer(centerPos.add(RotationUtil.vecPlayerToWorld(0.5D, -0.5D, 0.5D, g)).subtract(x, y, z), g);

        float minU = -(float) playerRelNN.x / 2.0F / radius + 0.5F;
        float maxU = -(float) playerRelPP.x / 2.0F / radius + 0.5F;
        float minV = -(float) playerRelNN.z / 2.0F / radius + 0.5F;
        float maxV = -(float) playerRelPP.z / 2.0F / radius + 0.5F;

        shadowVertex(entry, vertices, alpha, (float) relNN.x, (float) relNN.y, (float) relNN.z, minU, minV);
        shadowVertex(entry, vertices, alpha, (float) relNP.x, (float) relNP.y, (float) relNP.z, minU, maxV);
        shadowVertex(entry, vertices, alpha, (float) relPP.x, (float) relPP.y, (float) relPP.z, maxU, maxV);
        shadowVertex(entry, vertices, alpha, (float) relPN.x, (float) relPN.y, (float) relPN.z, maxU, minV);
    }
}
