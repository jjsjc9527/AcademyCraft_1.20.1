package cn.academy.client.render.entity;

import cn.academy.entity.EntityShiftBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ShiftBlockRenderer extends EntityRenderer<EntityShiftBlock> {

    private final BlockRenderDispatcher blockRenderer;

    public ShiftBlockRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.blockRenderer = ctx.getBlockRenderDispatcher();
    }

    @Override
    public void render(EntityShiftBlock e, float entityYaw, float partialTick,
                       PoseStack ps, MultiBufferSource buffers, int light) {
        BlockState state = e.getBlockState();
        if (state.getRenderShape() == RenderShape.MODEL) {
            BlockState partner = e.getPartnerState();
            boolean pair = !partner.isAir();
            Direction pd = pair ? e.getPartnerDir() : null;

            double hx = pair ? pd.getStepX() * 0.5 : 0;
            double hy = pair ? pd.getStepY() * 0.5 : 0;
            double hz = pair ? pd.getStepZ() * 0.5 : 0;

            ps.pushPose();
            ps.translate(hx, e.getBbHeight() / 2.0 + hy, hz);

            ps.mulPose(Axis.YP.rotationDegrees(e.getPoseY()));
            ps.mulPose(Axis.XP.rotationDegrees(e.getPoseX()));
            ps.mulPose(Axis.ZP.rotationDegrees(e.getPoseZ()));
            ps.translate(-hx, -hy, -hz);
            ps.translate(-0.5, -0.5, -0.5);
            blockRenderer.renderSingleBlock(state, ps, buffers, light, OverlayTexture.NO_OVERLAY);
            if (pair) {
                ps.translate(pd.getStepX(), pd.getStepY(), pd.getStepZ());
                blockRenderer.renderSingleBlock(partner, ps, buffers, light, OverlayTexture.NO_OVERLAY);
            }
            ps.popPose();
        }
        super.render(e, entityYaw, partialTick, ps, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityShiftBlock e) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
