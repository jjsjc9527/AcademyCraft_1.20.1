package cn.academy.client.render.entity;

import cn.academy.entity.EntitySilbarn;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SilbarnRenderer extends EntityRenderer<EntitySilbarn> {

    public static final ModelResourceLocation MODEL =
            new ModelResourceLocation("academy", "silbarn_crystal", "inventory");

    public static final int MAX_FRAMES = 9;

    public static ModelResourceLocation frameModel(int n) {
        return new ModelResourceLocation("academy", "silbarn_crystal_" + n, "inventory");
    }

    public static boolean frameExists(int n) {
        return Minecraft.getInstance().getResourceManager()
                .getResource(new ResourceLocation("academy", "models/item/silbarn_crystal_" + n + ".json"))
                .isPresent();
    }

    public static int extraFrames = 0;

    private static final float PULSE_MS = 220f;

    private static final float SHAKE_DEG = 6f;

    private static final int DESTROY_STAGES = 10;

    private static final ItemStack STACK_HOLDER = new ItemStack(net.minecraft.world.item.Items.STONE);

    public SilbarnRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(EntitySilbarn e, float yaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffers, int packedLight) {
        if (e.isHit()) {
            return;
        }
        var model = cn.academy.client.render.ACClientRenderers.silbarnCrystal;
        if (model == null) {
            return;
        }

        pose.pushPose();
        pose.translate(0, e.getBbHeight() * 0.5, 0);

        pose.mulPose(Axis.YP.rotationDegrees(180 - yaw));
        pose.mulPose(Axis.XP.rotationDegrees(-e.getXRot()));

        pose.mulPose(Axis.ZP.rotationDegrees(e.turnRoll()));

        int crack = e.getCrack();
        if (crack > 0) {
            float t = Math.min(1f, crack / 100f);
            long now = System.currentTimeMillis();

            float pulse = Math.max(0f, 1f - (now - e.lastCrackMs()) / PULSE_MS);

            double w = now * (0.05 + 0.09 * t);
            float amp = SHAKE_DEG * t;
            pose.mulPose(Axis.ZP.rotationDegrees((float) Math.sin(w) * amp));
            pose.mulPose(Axis.XP.rotationDegrees((float) Math.cos(w * 1.37) * amp));

            float squash = 1f - 0.06f * pulse;
            pose.scale(squash, squash, squash);
        }

        int frames = extraFrames;
        if (crack > 0 && frames > 0) {
            int idx = Math.min(frames, 1 + (crack - 1) * frames / 100);
            var f = cn.academy.client.render.ACClientRenderers.silbarnFrames[idx - 1];
            if (f != null) {
                model = f;
            }
        }

        var itemRenderer = Minecraft.getInstance().getItemRenderer();
        itemRenderer.render(STACK_HOLDER, ItemDisplayContext.NONE, false, pose, buffers,
                packedLight, OverlayTexture.NO_OVERLAY, model);

        if (crack > 0 && extraFrames == 0) {
            int stage = Math.min(DESTROY_STAGES - 1, crack * DESTROY_STAGES / 100);
            pose.pushPose();
            pose.translate(-0.5F, -0.5F, -0.5F);
            PoseStack.Pose last = pose.last();
            VertexConsumer decal = new SheetedDecalTextureGenerator(
                    buffers.getBuffer(ModelBakery.DESTROY_TYPES.get(stage)),
                    last.pose(), last.normal(), 1.0F);
            itemRenderer.renderModelLists(model, STACK_HOLDER,
                    packedLight, OverlayTexture.NO_OVERLAY, pose, decal);
            pose.popPose();
        }
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(EntitySilbarn e) {
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
    }
}
