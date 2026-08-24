package cn.academy.client.render;

import cn.academy.block.tileentity.WirelessMatrixBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;

public class WirelessMatrixRenderer implements BlockEntityRenderer<WirelessMatrixBlockEntity> {

    public static final ResourceLocation SHIELD_MODEL = new ResourceLocation("academy", "block/matrix_shield");

    public static final ResourceLocation BASE_MODEL = new ResourceLocation("academy", "block/matrix");

    private static final double ROT_CENTER = 1.0;

    private static final int PLATES = 3;
    private static final float ROT_SPEED = 2.5f;
    private static final float FLOAT_AMP = 0.1f;
    private static final float FLOAT_SPEED = 0.11f;

    public WirelessMatrixRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(WirelessMatrixBlockEntity be, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        ModelBlockRenderer mbr = mc.getBlockRenderer().getModelRenderer();
        VertexConsumer vc = buffers.getBuffer(RenderType.cutout());

        BakedModel base = ACClientRenderers.matrixBase;
        if (base != null) {
            mbr.renderModel(pose.last(), vc, null, base, 1f, 1f, 1f, light, overlay);
        }

        BakedModel model = ACClientRenderers.matrixShield;
        if (model == null) {
            return;
        }

        if (!be.isRenderShields()) {
            return;
        }

        float t = (float) (cn.lambdalib2.util.GameTimer.getPausableTime() * 20.0);
        float baseAngle = (t * ROT_SPEED) % 360f;

        for (int i = 0; i < PLATES; i++) {
            float floatY = FLOAT_AMP * (float) Math.sin(t * FLOAT_SPEED + i * 2.1f);
            float angle = baseAngle + i * (360f / PLATES);
            pose.pushPose();

            pose.translate(ROT_CENTER, floatY, ROT_CENTER);
            pose.mulPose(Axis.YP.rotationDegrees(angle));
            pose.translate(-ROT_CENTER, 0, -ROT_CENTER);
            mbr.renderModel(pose.last(), vc, null, model, 1f, 1f, 1f, light, overlay);
            pose.popPose();
        }
    }
}
