package cn.academy.client.render;

import cn.academy.client.render.entity.ArcRenderer;
import cn.academy.client.render.entity.RailgunFXRenderer;
import cn.academy.client.render.entity.RailgunHandRenderer;
import cn.academy.client.render.entity.RippleMarkRender;
import cn.academy.client.render.entity.SurroundArcRenderer;
import cn.academy.client.render.entity.ThunderStrikeRenderer;
import cn.academy.entity.EntityArc;
import cn.academy.entity.EntityRailgunFX;
import cn.academy.entity.EntityRailgunHand;
import cn.academy.entity.EntityRippleMark;
import cn.academy.entity.EntitySurroundArc;
import cn.academy.entity.EntityThunderStrike;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "academy", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ACEffectLateRender {

    private ACEffectLateRender() {}

    private static MultiBufferSource.BufferSource ownBuffers;

    private static final org.joml.Matrix4f CAM_ROTATION = new org.joml.Matrix4f();

    public static org.joml.Matrix4f cameraRotation() {
        return CAM_ROTATION;
    }

    private static MultiBufferSource.BufferSource effectBuffers() {
        if (ownBuffers == null) {

            ownBuffers = MultiBufferSource.immediate(
                    new com.mojang.blaze3d.vertex.BufferBuilder(2048));
        }
        return ownBuffers;
    }

    private static int orderOf(Entity e, Vec3 cam) {

        return e instanceof ACEffect fx ? fx.effectOrder(cam) : 0;
    }

    @SubscribeEvent
    public static void onLevelUnload(net.minecraftforge.event.level.LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            cn.academy.client.render.misc.IronSandParticle.clearAll();
            cn.academy.client.render.misc.SonicWaveParticle.clearAll();

            cn.academy.ability.vanilla.vecmanip.advanced.CrushFieldFx.clear();

            AllyMarkRender.clear();

            cn.academy.ability.vanilla.vecmanip.advanced.PlatinumFeatherFx.clearAll();
        }
    }

    @SubscribeEvent
    public static void onRenderStage(RenderLevelStageEvent event) {

        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            MagLimbBones.newFrame();

            CAM_ROTATION.set(event.getPoseStack().last().pose());
            return;
        }
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        final boolean dazed = cn.academy.ability.vanilla.mentalout.DazeState.isLocalPlayerDazed();
        float pt = dazed ? 1.0f : event.getPartialTick();
        Vec3 cam = event.getCamera().getPosition();

        PoseStack pose = new PoseStack();
        pose.mulPoseMatrix(CAM_ROTATION);
        MultiBufferSource.BufferSource buffers = effectBuffers();

        if (cn.academy.client.render.misc.SonicWaveParticle.hasAlive()
                || cn.academy.ability.vanilla.vecmanip.advanced.CrushFieldFx.hasAny()) {
            ScreenCopy.capture();
        }

        cn.academy.client.render.misc.IronSandParticle.drawAll(event.getCamera(), pt);

        cn.academy.client.render.misc.SonicWaveParticle.drawAll(event.getCamera(), pt);

        cn.academy.ability.vanilla.vecmanip.advanced.CrushFieldFx.drawAll(CAM_ROTATION, cam);

        java.util.List<Entity> expired = null;
        double now = cn.lambdalib2.util.GameTimer.getPausableTime();

        java.util.List<Entity> effects = new java.util.ArrayList<>();

        for (Entity e : mc.level.entitiesForRendering()) {

            boolean zombie = e instanceof ACEffect fx
                    ? fx.effectExpired(now)
                    : (e instanceof cn.academy.entity.EntityCoinThrowing c && c.clientSpawnTime >= 0
                            && (now - c.clientSpawnTime) * 20.0 > 140);
            if (zombie) {
                if (expired == null) expired = new java.util.ArrayList<>();
                expired.add(e);
                continue;
            }

            boolean isEffect = e instanceof ACEffect;
            if (!isEffect) {
                continue;
            }
            effects.add(e);
        }

        if (effects.size() > 1) {
            effects.sort(java.util.Comparator.comparingInt(x -> orderOf(x, cam)));
        }

        float dim = nightDim(mc.level, cam, pt);
        if (dim < 1.0f) {
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(dim, dim, dim, 1.0f);
        }

        boolean any = false;
        for (Entity e : effects) {

            double sx = Mth.lerp(pt, e.xOld, e.getX());
            double sy = Mth.lerp(pt, e.yOld, e.getY());
            double sz = Mth.lerp(pt, e.zOld, e.getZ());
            if (e instanceof EntityArc ba && ba.boneIndex >= 0 && ba.getPlayer() != null) {
                Vec3 bone = MagLimbBones.get(ba.getPlayer().getUUID(), ba.boneIndex);
                if (bone != null) {
                    sx = bone.x; sy = bone.y; sz = bone.z;
                    ba.aimFrom(sx, sy, sz);
                }
            }

            pose.pushPose();
            try {
            pose.translate(sx - cam.x, sy - cam.y, sz - cam.z);

            EffectDrawers.draw(e, new EffectDrawCtx(pt, cam, pose, buffers));
            } finally {
                pose.popPose();
            }
            any = true;
        }

        if (any) {
            buffers.endBatch();
        }
        if (dim < 1.0f) {
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }

        if (AllyMarkRender.drawAll(pose, buffers, event.getCamera(), cam, pt)) {
            buffers.endBatch();
        }
        if (expired != null) {
            for (Entity a : expired) a.discard();
        }
    }

    private static final double NIGHT_MIN = 0.40;

    private static float nightDim(net.minecraft.client.multiplayer.ClientLevel level,
                                  Vec3 cam, float pt) {
        if (NIGHT_MIN >= 1.0) {
            return 1.0f;
        }
        net.minecraft.core.BlockPos bp = net.minecraft.core.BlockPos.containing(cam);
        float skyF = level.getSkyDarken(pt);
        float sky = level.getBrightness(net.minecraft.world.level.LightLayer.SKY, bp) / 15.0f * skyF;
        float block = level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, bp) / 15.0f;
        float env = Math.max(sky, block);
        return (float) (NIGHT_MIN + (1.0 - NIGHT_MIN) * env);
    }
}
