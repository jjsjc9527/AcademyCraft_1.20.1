package cn.academy.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = "academy", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class GroundHeaveClient {

    private static final double RENDER_DIST = 48.0;

    private static final float RISE_TICKS = 6.0f;

    private static final float SIDE_EPS = 0.01f;

    private static final class Heave {
        final float height;

        final long bornAt;
        final long expireAt;

        Heave(float height, long bornAt, long expireAt) {
            this.height = height;
            this.bornAt = bornAt;
            this.expireAt = expireAt;
        }
    }

    private static final Map<BlockPos, Heave> HEAVES = new HashMap<>();

    private GroundHeaveClient() {}

    public static void accept(int lifetime, List<BlockPos> positions, List<Integer> heights) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        long now = (long) (cn.lambdalib2.util.GameTimer.getPausableTime() * 20.0);
        for (int i = 0; i < positions.size(); i++) {

            HEAVES.put(positions.get(i).immutable(),
                    new Heave(heights.get(i) / 100.0f, now, now + lifetime));
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(net.minecraftforge.event.level.LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            HEAVES.clear();
        }
    }

    @SubscribeEvent
    public static void onRenderStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || HEAVES.isEmpty()) {
            return;
        }

        long now = (long) (cn.lambdalib2.util.GameTimer.getPausableTime() * 20.0);

        float pt = cn.academy.ability.vanilla.mentalout.DazeState.isLocalPlayerDazed()
                ? 1.0f : event.getPartialTick();
        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        List<BlockPos> expired = null;
        boolean any = false;

        for (Map.Entry<BlockPos, Heave> en : HEAVES.entrySet()) {
            BlockPos pos = en.getKey();
            Heave h = en.getValue();

            if (now >= h.expireAt) {
                if (expired == null) expired = new ArrayList<>();
                expired.add(pos);
                continue;
            }

            BlockState state = mc.level.getBlockState(pos);
            if (state.isAir()) {
                if (expired == null) expired = new ArrayList<>();
                expired.add(pos);
                continue;
            }
            if (pos.distToCenterSqr(cam.x, cam.y, cam.z) > RENDER_DIST * RENDER_DIST) {
                continue;
            }

            BlockPos up = pos.above();
            if (mc.level.getBlockState(up).canOcclude()) {
                continue;
            }

            float rise = Math.min(1.0f, (now - h.bornAt + pt) / RISE_TICKS);
            if (rise <= 0) {
                continue;
            }

            if (h.height <= 0) {
                continue;
            }

            pose.pushPose();
            pose.translate(pos.getX() - cam.x, pos.getY() - cam.y + h.height * rise, pos.getZ() - cam.z);

            pose.translate(0.5, 0.0, 0.5);
            pose.scale(1.0f + 2 * SIDE_EPS, 1.0f, 1.0f + 2 * SIDE_EPS);
            pose.translate(-0.5, 0.0, -0.5);

            mc.getBlockRenderer().renderSingleBlock(state, pose, buffers,
                    LevelRenderer.getLightColor(mc.level, up), OverlayTexture.NO_OVERLAY);
            pose.popPose();
            any = true;
        }

        if (any) {
            buffers.endBatch();
        }
        if (expired != null) {
            for (BlockPos p : expired) {
                HEAVES.remove(p);
            }
        }
    }
}
