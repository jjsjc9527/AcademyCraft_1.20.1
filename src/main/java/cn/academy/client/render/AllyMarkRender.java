package cn.academy.client.render;

import cn.academy.Resources;
import cn.academy.datapart.RemoteData;
import cn.lambdalib2.util.GameTimer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public final class AllyMarkRender {

    private AllyMarkRender() {}

    private static final ResourceLocation TEX_ANIM = Resources.getTexture("effects/cognition_mark_anim");

    private static final ResourceLocation TEX_STILL = Resources.getTexture("effects/cognition_mark");

    private static final ResourceLocation TEX_ENRAGED = Resources.getTexture("effects/enraged_mark");

    private static final float MARK_GAP = 0.62f;

    private static final int FRAMES = 5;

    private static final double FRAME_TIME = 0.09;

    private static final float SIZE = 0.55f;

    private static final float HEAD_GAP = 0.42f;

    private static final Map<UUID, Double> ANIM = new HashMap<>();

    public static void begin(UUID id) {
        if (id != null) {
            ANIM.put(id, GameTimer.getPausableTime());
        }
    }

    public static void clear() {
        ANIM.clear();
    }

    public static boolean drawAll(PoseStack pose, MultiBufferSource buffers,
                                  Camera camera, Vec3 cam, float pt) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer self = mc.player;
        if (self == null || mc.level == null) {
            return false;
        }

        RemoteData rd = RemoteData.get(self);

        if (rd == null || (rd.getAllies().isEmpty() && rd.getEnraged().isEmpty())) {
            return false;
        }

        Set<UUID> ids = new HashSet<>();
        for (RemoteData.Ally a : rd.getAllies()) {
            ids.add(a.id);
        }
        Set<UUID> angry = rd.getEnraged();

        double now = GameTimer.getPausableTime();

        List<Mark> marks = new ArrayList<>();

        for (Entity e : mc.level.entitiesForRendering()) {
            if (e == self) {
                continue;
            }
            boolean hasAlly = ids.contains(e.getUUID());
            boolean hasAngry = angry.contains(e.getUUID());
            if (!hasAlly && !hasAngry) {
                continue;
            }

            double x = Mth.lerp(pt, e.xOld, e.getX());
            double y = Mth.lerp(pt, e.yOld, e.getY()) + e.getBbHeight() + HEAD_GAP;
            double z = Mth.lerp(pt, e.zOld, e.getZ());

            int markCount = (hasAlly ? 1 : 0) + (hasAngry ? 1 : 0);
            float dx0 = markCount == 2 ? -MARK_GAP * 0.5f : 0f;

            Double start = hasAlly ? ANIM.get(e.getUUID()) : null;
            int frame = start == null ? -1 : (int) ((now - start) / FRAME_TIME);
            boolean playing = frame >= 0 && frame < FRAMES;
            if (frame >= FRAMES) {

                ANIM.remove(e.getUUID());
            }

            pose.pushPose();
            pose.translate(x - cam.x, y - cam.y, z - cam.z);

            pose.mulPose(camera.rotation());

            pose.scale(-1f, 1f, 1f);

            Matrix4f m = new Matrix4f(pose.last().pose());
            pose.popPose();

            float dx = dx0;
            if (hasAlly) {
                marks.add(playing
                        ? new Mark(m, dx, (float) frame / FRAMES, (float) (frame + 1) / FRAMES, KIND_ANIM)
                        : new Mark(m, dx, 0f, 1f, KIND_STILL));
                dx += MARK_GAP;
            }
            if (hasAngry) {
                marks.add(new Mark(m, dx, 0f, 1f, KIND_ENRAGED));
            }
        }

        if (marks.isEmpty()) {
            return false;
        }

        drawKind(buffers, marks, KIND_ANIM, TEX_ANIM);
        drawKind(buffers, marks, KIND_STILL, TEX_STILL);
        drawKind(buffers, marks, KIND_ENRAGED, TEX_ENRAGED);
        return true;
    }

    private static final int KIND_ANIM = 0, KIND_STILL = 1, KIND_ENRAGED = 2;

    private record Mark(Matrix4f pose, float dx, float u0, float u1, int kind) {}

    private static void drawKind(MultiBufferSource buffers, List<Mark> marks, int kind,
                                 ResourceLocation tex) {
        VertexConsumer vc = null;
        for (Mark k : marks) {
            if (k.kind() != kind) {
                continue;
            }
            if (vc == null) {
                vc = buffers.getBuffer(ACRenderTypes.allyMark(tex));
            }
            emit(vc, k.pose(), k.dx(), k.u0(), k.u1());
        }
    }

    private static void emit(VertexConsumer vc, Matrix4f m, float dx, float u0, float u1) {
        float h = SIZE * 0.5f;
        vc.vertex(m, dx - h, -h, 0f).uv(u0, 1f).color(255, 255, 255, 255).endVertex();
        vc.vertex(m, dx + h, -h, 0f).uv(u1, 1f).color(255, 255, 255, 255).endVertex();
        vc.vertex(m, dx + h, h, 0f).uv(u1, 0f).color(255, 255, 255, 255).endVertex();
        vc.vertex(m, dx - h, h, 0f).uv(u0, 0f).color(255, 255, 255, 255).endVertex();
    }
}
