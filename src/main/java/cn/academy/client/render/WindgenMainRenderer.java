package cn.academy.client.render;

import cn.academy.block.block.ACMultiBlock;
import cn.academy.block.block.WindgenMainBlock;
import cn.academy.block.tileentity.WindgenMainBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;

public class WindgenMainRenderer implements BlockEntityRenderer<WindgenMainBlockEntity> {

    public static final ResourceLocation FAN_MODEL = new ResourceLocation("academy", "block/windgen_fan");

    private static final float MOUNT_X = 0.5f, MOUNT_Y = 0.496f, MOUNT_Z = 1.32f;
    private static final float SPIN_SPEED = 4.0f;

    private static final float PILLARS_FOR_NATIVE = 8f;
    private static final float FAN_MIN = 0.4f, FAN_MAX = 1.4f;

    public WindgenMainRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(WindgenMainBlockEntity be, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        if (!be.isFanInstalled()) {
            return;
        }
        BakedModel model = ACClientRenderers.windgenFan;
        if (model == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ModelBlockRenderer mbr = mc.getBlockRenderer().getModelRenderer();
        VertexConsumer vc = buffers.getBuffer(RenderType.cutout());

        float angle = 0f;
        if (be.isWorking()) {

            float t = (float) (cn.lambdalib2.util.GameTimer.getPausableTime() * 20.0);
            angle = (t * SPIN_SPEED) % 360f;
        }

        float fanScale = Math.max(FAN_MIN, Math.min(FAN_MAX, be.getPillars() / PILLARS_FOR_NATIVE));

        pose.pushPose();

        Direction dir = be.getBlockState().getBlock() instanceof ACMultiBlock mb
                ? mb.facingOf(be.getBlockState()) : Direction.SOUTH;
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(WindgenMainBlock.drMapOf(dir)));
        pose.translate(-0.5, 0, -0.5);

        pose.translate(MOUNT_X, MOUNT_Y, MOUNT_Z);
        pose.scale(fanScale, fanScale, fanScale);
        pose.mulPose(Axis.ZP.rotationDegrees(angle));
        mbr.renderModel(pose.last(), vc, null, model, 1f, 1f, 1f, light, overlay);
        pose.popPose();
    }
}
