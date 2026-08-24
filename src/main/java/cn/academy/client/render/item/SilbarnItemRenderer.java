package cn.academy.client.render.item;

import cn.academy.client.render.ACClientRenderers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class SilbarnItemRenderer extends BlockEntityWithoutLevelRenderer {

    private static SilbarnItemRenderer instance;

    public static SilbarnItemRenderer getInstance() {
        if (instance == null) {
            Minecraft mc = Minecraft.getInstance();
            instance = new SilbarnItemRenderer(
                    mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
        }
        return instance;
    }

    public SilbarnItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet models) {
        super(dispatcher, models);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack pose,
                             MultiBufferSource buffers, int light, int overlay) {

        if (ctx == ItemDisplayContext.GUI) {
            BakedModel icon = ACClientRenderers.silbarnIcon;
            if (icon != null) {
                VertexConsumer vc = ItemRenderer.getFoilBufferDirect(
                        buffers, ItemBlockRenderTypes.getRenderType(stack, true),
                        true, stack.hasFoil());
                Minecraft.getInstance().getItemRenderer()
                        .renderModelLists(icon, stack, light, overlay, pose, vc);
            }
            return;
        }

        BakedModel model = ACClientRenderers.silbarnCrystal;
        if (model == null) {
            return;
        }
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);

        Minecraft.getInstance().getItemRenderer().render(
                stack, ctx, isLeftHand(ctx), pose, buffers, light, overlay, model);
        pose.popPose();
    }

    private static boolean isLeftHand(ItemDisplayContext ctx) {
        return ctx == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || ctx == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
    }
}
