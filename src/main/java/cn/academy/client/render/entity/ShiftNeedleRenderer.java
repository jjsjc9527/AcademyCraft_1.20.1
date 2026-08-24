package cn.academy.client.render.entity;

import cn.academy.ACItems;
import cn.academy.entity.EntityShiftNeedle;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ShiftNeedleRenderer extends EntityRenderer<EntityShiftNeedle> {

    private final ItemRenderer itemRenderer;

    public ShiftNeedleRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.itemRenderer = ctx.getItemRenderer();
    }

    @Override
    public void render(EntityShiftNeedle needle, float entityYaw, float partialTick,
                       PoseStack ps, MultiBufferSource buffers, int light) {

        if (needle.isStuck()) {
            super.render(needle, entityYaw, partialTick, ps, buffers, light);
            return;
        }

        Vec3 dir = needle.getRenderDir();
        if (dir.lengthSqr() < 1.0e-6) {
            dir = new Vec3(1, 0, 0);
        }
        dir = dir.normalize();

        float pitch = (float) Math.toDegrees(Math.asin(Mth.clamp(dir.y, -1.0, 1.0)));
        float yaw = (float) Math.toDegrees(Math.atan2(-dir.z, dir.x));

        ps.pushPose();
        ps.translate(0, needle.getBbHeight() / 2.0, 0);
        ps.mulPose(Axis.YP.rotationDegrees(yaw));
        ps.mulPose(Axis.ZP.rotationDegrees(pitch - 45.0f));
        ps.scale(0.9f, 0.9f, 0.9f);
        itemRenderer.renderStatic(new ItemStack(ACItems.NEEDLE.get()), ItemDisplayContext.NONE,
                light, OverlayTexture.NO_OVERLAY, ps, buffers, needle.level(), 0);
        ps.popPose();

        super.render(needle, entityYaw, partialTick, ps, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityShiftNeedle e) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
