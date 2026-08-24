package cn.academy.client.render.block;

import cn.academy.AcademyCraft;
import cn.academy.block.tileentity.ImagPhaseBlockEntity;
import cn.academy.client.render.ACRenderTypes;
import cn.lambdalib2.util.GameTimer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class ImagPhaseRenderer implements BlockEntityRenderer<ImagPhaseBlockEntity> {

    private static final ResourceLocation[] LAYERS = {
            new ResourceLocation(AcademyCraft.MODID, "textures/effect/imag_proj_liquid/0.png"),
            new ResourceLocation(AcademyCraft.MODID, "textures/effect/imag_proj_liquid/1.png"),
            new ResourceLocation(AcademyCraft.MODID, "textures/effect/imag_proj_liquid/2.png"),
    };

    private static final float DENSITY = 0.7f;

    public ImagPhaseRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(ImagPhaseBlockEntity be, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        Level level = be.getLevel();
        if (level == null) return;

        BlockPos pos = be.getBlockPos();
        Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        double distSq = cam.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        float alpha = (float) (1.0 / (1.0 + 0.2 * Math.sqrt(distSq)));
        if (alpha < 0.1f) return;

        FluidState fs = level.getFluidState(pos);
        if (fs.isEmpty()) return;
        double ht = 1.2 * Math.sqrt(fs.getHeight(level, pos));

        drawLayer(pose, buffers, 0, -0.30 * ht, 0.30, 0.20, alpha);
        drawLayer(pose, buffers, 1, 0.35 * ht, 0.30, 0.05, alpha);
        if (ht > 0.5) {
            drawLayer(pose, buffers, 2, 0.70 * ht, 0.10, 0.25, alpha);
        }
    }

    private void drawLayer(PoseStack pose, MultiBufferSource buffers,
                           int layer, double height, double vx, double vz, float alpha) {
        double time = GameTimer.getTime();
        float du = (float) ((time * vx) % 1);
        float dv = (float) ((time * vz) % 1);

        VertexConsumer vc = buffers.getBuffer(ACRenderTypes.liquidFx(LAYERS[layer]));
        Matrix4f m = pose.last().pose();
        float y = (float) height;

        quad(vc, m, 0, y, 0, du, dv, alpha);
        quad(vc, m, 1, y, 0, du + DENSITY, dv, alpha);
        quad(vc, m, 1, y, 1, du + DENSITY, dv + DENSITY, alpha);
        quad(vc, m, 0, y, 1, du, dv + DENSITY, alpha);
    }

    private void quad(VertexConsumer vc, Matrix4f m, float x, float y, float z,
                      float u, float v, float alpha) {
        vc.vertex(m, x, y, z)
                .color(1f, 1f, 1f, alpha)
                .uv(u, v)
                .uv2(0xF000F0)
                .endVertex();
    }
}
