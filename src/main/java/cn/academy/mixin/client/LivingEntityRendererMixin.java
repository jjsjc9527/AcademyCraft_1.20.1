package cn.academy.mixin.client;

import cn.academy.client.render.MagLimbBones;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.WalkAnimationState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

    @Inject(method = "getBob(Lnet/minecraft/world/entity/LivingEntity;F)F",
            at = @At("HEAD"), cancellable = true)
    private void academy$dazeBob(LivingEntity entity, float partialTick,
                                 CallbackInfoReturnable<Float> cir) {
        if (cn.academy.ability.vanilla.mentalout.DazeState.renderFrozen(entity)) {
            cir.setReturnValue((float) entity.tickCount);
        }
    }

    @Redirect(
        method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/WalkAnimationState;position(F)F"),
        require = 0, expect = 1
    )
    private float academy$dazeLimbSwing(WalkAnimationState state, float partialTick,
                                        LivingEntity entity, float yaw, float pt,
                                        PoseStack poseStack, MultiBufferSource buffers, int light) {
        return cn.academy.ability.vanilla.mentalout.DazeState.renderFrozen(entity)
                ? state.position() : state.position(partialTick);
    }

    @Redirect(
        method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/WalkAnimationState;speed(F)F"),
        require = 0, expect = 1
    )
    private float academy$dazeLimbAmount(WalkAnimationState state, float partialTick,
                                         LivingEntity entity, float yaw, float pt,
                                         PoseStack poseStack, MultiBufferSource buffers, int light) {
        return cn.academy.ability.vanilla.mentalout.DazeState.renderFrozen(entity)
                ? state.speed() : state.speed(partialTick);
    }

    private static final float[][] academy$LIMB_TIP = {
            {-1.0f, 10.0f, 0.0f},
            { 1.0f, 10.0f, 0.0f},
            { 0.0f, 12.0f, 0.0f},
            { 0.0f, 12.0f, 0.0f},
    };

    private void academy$dualWingPose(LivingEntity entity, float partialTick) {
        Object model = ((LivingEntityRenderer<?, ?>) (Object) this).getModel();

        if (!(entity instanceof Player player) || !(model instanceof PlayerModel<?> pm)) {
            cn.academy.client.render.DualWingLimbs.beginFrame(null, null, false);
            return;
        }
        float[] pose =
                cn.academy.ability.vanilla.vecmanip.advanced.DualWingAnim.pose(player, partialTick);

        cn.academy.client.render.DualWingLimbs.beginFrame(pose, pm,
                cn.academy.client.render.DualWingLimbs.isSlim(player));
        if (pose == null) {

            cn.academy.ability.vanilla.vecmanip.advanced.DualWingAnim.resetIfDirty(pm);
            return;
        }
        cn.academy.ability.vanilla.vecmanip.advanced.DualWingAnim.applyToModel(pm, pose);
        pm.rightArm.visible = false;
        pm.leftArm.visible = false;
        pm.rightLeg.visible = false;
        pm.leftLeg.visible = false;

        pm.rightSleeve.visible = false;
        pm.leftSleeve.visible = false;
        pm.rightPants.visible = false;
        pm.leftPants.visible = false;
    }

    @Inject(
        method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/client/model/EntityModel;setupAnim(Lnet/minecraft/world/entity/Entity;FFFFF)V",
                 shift = At.Shift.AFTER)
    )
    private void academy$captureLimbBones(LivingEntity entity, float yaw, float partialTick,
                                          PoseStack poseStack, MultiBufferSource buffers, int light, CallbackInfo ci) {

        academy$dualWingPose(entity, partialTick);

        if (!(entity instanceof Player player)) return;

        boolean wantBody = cn.academy.client.render.BodyBones.has(player.getUUID());
        if (!MagLimbBones.isActive(player.getUUID()) && !wantBody) return;

        Object model = ((LivingEntityRenderer<?, ?>) (Object) this).getModel();
        if (!(model instanceof PlayerModel<?> pm)) return;

        Matrix4f base = poseStack.last().pose();
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 cam = camera.getPosition();
        Vector3f look = camera.getLookVector();
        Vector3f up = camera.getUpVector();
        Vector3f left = camera.getLeftVector();
        if (MagLimbBones.isActive(player.getUUID())) {
            ModelPart[] bones = { pm.rightArm, pm.leftArm, pm.rightLeg, pm.leftLeg };

            Vec3[] tips = new Vec3[4];
            for (int i = 0; i < 4; i++) {

                PoseStack ps = new PoseStack();
                ps.last().pose().set(base);
                bones[i].translateAndRotate(ps);
                float[] t = academy$LIMB_TIP[i];
                Vector3f v = new Vector3f(t[0] / 16.0f, t[1] / 16.0f, t[2] / 16.0f);
                v.mulPosition(ps.last().pose());
                tips[i] = academy$toWorld(v, cam, left, up, look);
            }
            MagLimbBones.store(player.getUUID(), tips);
        }

        if (wantBody) {
            academy$captureBodyFrame(player.getUUID(), pm, base, cam, left, up, look);
        }
    }

    private static void academy$captureBodyFrame(java.util.UUID player, PlayerModel<?> pm,
                                                 Matrix4f base, Vec3 cam,
                                                 Vector3f left, Vector3f up, Vector3f look) {
        PoseStack ps = new PoseStack();
        ps.last().pose().set(base);
        pm.body.translateAndRotate(ps);
        Matrix4f m = ps.last().pose();

        Vector3f o = new Vector3f(0, 0, 0).mulPosition(m);
        Vector3f ex = new Vector3f(1, 0, 0).mulPosition(m).sub(o);
        Vector3f ey = new Vector3f(0, 1, 0).mulPosition(m).sub(o);
        Vector3f ez = new Vector3f(0, 0, 1).mulPosition(m).sub(o);

        cn.academy.client.render.BodyBones.feed(
                player,
                academy$toWorld(o, cam, left, up, look),
                academy$toWorldDir(ex, left, up, look).normalize(),
                academy$toWorldDir(ey, left, up, look).normalize().scale(-1),
                academy$toWorldDir(ez, left, up, look).normalize().scale(-1),
                MagLimbBones.frame());
    }

    private static Vec3 academy$toWorld(Vector3f v, Vec3 cam, Vector3f left, Vector3f up, Vector3f look) {
        return cam.add(academy$toWorldDir(v, left, up, look));
    }

    private static Vec3 academy$toWorldDir(Vector3f v, Vector3f left, Vector3f up, Vector3f look) {
        return new Vec3(-v.x * left.x + v.y * up.x - v.z * look.x,
                        -v.x * left.y + v.y * up.y - v.z * look.y,
                        -v.x * left.z + v.y * up.z - v.z * look.z);
    }

    @Inject(
        method = "setupRotations(Lnet/minecraft/world/entity/LivingEntity;Lcom/mojang/blaze3d/vertex/PoseStack;FFF)V",
        at = @At("TAIL")
    )
    private void academy$faintLieDown(LivingEntity entity, PoseStack poseStack, float ageInTicks,
                                      float rotationYaw, float partialTicks, CallbackInfo ci) {

        if (cn.academy.ability.vanilla.mentalout.FaintState.isFainted(entity)
                && !cn.academy.ability.vanilla.mentalout.FaintState.liesViaGravity(entity)) {
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(
                    cn.academy.ability.vanilla.mentalout.FaintState.LIE_DOWN_DEGREES));
        }
    }
}
