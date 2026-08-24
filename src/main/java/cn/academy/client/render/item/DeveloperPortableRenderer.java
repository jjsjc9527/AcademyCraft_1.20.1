package cn.academy.client.render.item;

import cn.academy.client.render.ACClientRenderers;
import cn.academy.client.render.ACRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class DeveloperPortableRenderer extends BlockEntityWithoutLevelRenderer {

    private static DeveloperPortableRenderer instance;

    public static DeveloperPortableRenderer getInstance() {
        if (instance == null) {
            Minecraft mc = Minecraft.getInstance();
            instance = new DeveloperPortableRenderer(
                    mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
        }
        return instance;
    }

    public DeveloperPortableRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet models) {
        super(dispatcher, models);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack pose,
                             MultiBufferSource buffers, int light, int overlay) {

        if (ctx == ItemDisplayContext.GUI) {
            int damage = stack.getDamageValue();
            BakedModel icon = damage < 3 ? ACClientRenderers.devPortableIconFull
                    : damage > 10 ? ACClientRenderers.devPortableIconEmpty
                    : ACClientRenderers.devPortableIconHalf;
            if (icon != null) {
                VertexConsumer vc = net.minecraft.client.renderer.entity.ItemRenderer.getFoilBufferDirect(
                        buffers, net.minecraft.client.renderer.ItemBlockRenderTypes.getRenderType(stack, true),
                        true, stack.hasFoil());
                Minecraft.getInstance().getItemRenderer()
                        .renderModelLists(icon, stack, light, overlay, pose, vc);
            }
            return;
        }

        BakedModel model = ACClientRenderers.developerPortable;
        if (model == null) return;

        pose.pushPose();
        applyTransform(ctx, pose);
        VertexConsumer vc = buffers.getBuffer(ACRenderTypes.blockNoCull());
        Minecraft.getInstance().getBlockRenderer().getModelRenderer()
                .renderModel(pose.last(), vc, null, model, 1f, 1f, 1f, light, overlay);
        pose.popPose();
    }

    private static final float CX = 0.005f, CY = -0.571f, CZ = 0.479f;

    private static void applyTransform(ItemDisplayContext ctx, PoseStack pose) {
        pose.translate(0.5, 0.5, 0.5);

        switch (ctx) {
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> {
                pose.translate(0.0, 0.5, 0.0);
                pose.scale(0.22f, 0.22f, 0.22f);
                pose.mulPose(Axis.XP.rotationDegrees(180f));
            }
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> {
                pose.translate(0.0, 0.15, 0.0);
                pose.scale(0.18f, 0.18f, 0.18f);
                pose.mulPose(Axis.XP.rotationDegrees(180f));
            }
            case GROUND -> {
                pose.scale(0.16f, 0.16f, 0.16f);
                pose.mulPose(Axis.XP.rotationDegrees(180f));
            }

            default -> {
                pose.scale(0.2f, 0.2f, 0.2f);
                pose.mulPose(Axis.XP.rotationDegrees(180f));
            }
        }
        pose.translate(-CX, -CY, -CZ);
    }
}
