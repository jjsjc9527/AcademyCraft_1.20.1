package cn.academy.client.render;

import cn.academy.ability.develop.DeveloperType;
import cn.academy.block.block.DeveloperBlock;
import cn.academy.block.tileentity.DeveloperBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

public class DeveloperRenderer implements BlockEntityRenderer<DeveloperBlockEntity> {

    public static final ResourceLocation MODEL_NORMAL = new ResourceLocation("academy", "block/developer_normal");
    public static final ResourceLocation MODEL_ADVANCED = new ResourceLocation("academy", "block/developer_advanced");

    private static final float SCALE = 0.5f;

    public DeveloperRenderer(BlockEntityRendererProvider.Context ctx) {}

    private static float angleOf(Direction dir) {
        return switch (dir) {
            case SOUTH -> 180f;
            case WEST -> 90f;
            case EAST -> 270f;
            default -> 0f;
        };
    }

    @Override
    public void render(DeveloperBlockEntity be, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        BlockState st = be.getBlockState();
        if (!(st.getBlock() instanceof DeveloperBlock devBlock)) return;

        BakedModel model = devBlock.type == DeveloperType.ADVANCED
                ? ACClientRenderers.developerAdvanced
                : ACClientRenderers.developerNormal;
        if (model == null) return;

        if (st.getValue(DeveloperBlock.SUB) != 0) return;

        Minecraft mc = Minecraft.getInstance();
        ModelBlockRenderer mbr = mc.getBlockRenderer().getModelRenderer();

        VertexConsumer vc = buffers.getBuffer(ACRenderTypes.blockNoCull());

        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(angleOf(st.getValue(DeveloperBlock.FACING))));
        pose.scale(SCALE, SCALE, SCALE);
        mbr.renderModel(pose.last(), vc, null, model, 1f, 1f, 1f, light, overlay);
        pose.popPose();
    }
}
