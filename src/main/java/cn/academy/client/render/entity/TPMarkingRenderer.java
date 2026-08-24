package cn.academy.client.render.entity;

import cn.academy.Resources;
import cn.academy.client.render.ACRenderTypes;
import cn.academy.entity.EntityTPMarking;
import cn.lambdalib2.util.GameTimer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class TPMarkingRenderer extends EntityRenderer<EntityTPMarking> {

    private static final int FRAMES = 7;
    private static final RenderType[] TYPES = new RenderType[FRAMES];

    static {
        for (int i = 0; i < FRAMES; i++) {
            TYPES[i] = ACRenderTypes.tpMark(Resources.getTexture("effects/tp_mark/" + i));
        }
    }

    private static final ModelPart ROOT = bakeLegacyBiped();

    private static ModelPart bakeLegacyBiped() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4, -8, -4, 8, 8, 8), PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create()
                .texOffs(32, 0).addBox(-4, -8, -4, 8, 8, 8, new CubeDeformation(0.5f)), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(16, 16).addBox(-4, 0, -2, 8, 12, 4), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(40, 16).addBox(-3, -2, -2, 4, 12, 4), PartPose.offset(-5, 2, 0));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(40, 16).mirror().addBox(-1, -2, -2, 4, 12, 4), PartPose.offset(5, 2, 0));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-2, 0, -2, 4, 12, 4), PartPose.offset(-1.9f, 12, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(0, 16).mirror().addBox(-2, 0, -2, 4, 12, 4), PartPose.offset(1.9f, 12, 0));
        return LayerDefinition.create(mesh, 64, 32).bakeRoot();
    }

    private static net.minecraft.client.model.PlayerModel<net.minecraft.client.player.AbstractClientPlayer> MODEL_WIDE;
    private static net.minecraft.client.model.PlayerModel<net.minecraft.client.player.AbstractClientPlayer> MODEL_SLIM;

    private static final java.util.Map<ResourceLocation, RenderType> SKIN_TYPES = new java.util.HashMap<>();

    public TPMarkingRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);

        MODEL_WIDE = new net.minecraft.client.model.PlayerModel<>(
                ctx.bakeLayer(net.minecraft.client.model.geom.ModelLayers.PLAYER), false);
        MODEL_SLIM = new net.minecraft.client.model.PlayerModel<>(
                ctx.bakeLayer(net.minecraft.client.model.geom.ModelLayers.PLAYER_SLIM), true);

        for (var m : java.util.List.of(MODEL_WIDE, MODEL_SLIM)) {
            m.young = false;
            m.riding = false;
            m.crouching = false;
        }
    }

    @Override
    public void render(EntityTPMarking mk, float yaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffers, int packedLight) {

    }

    @Override
    public ResourceLocation getTextureLocation(EntityTPMarking entity) {
        return Resources.getTexture("effects/tp_mark/0");
    }

    public static void draw(EntityTPMarking mk, PoseStack pose, MultiBufferSource.BufferSource buffers) {

        boolean useSkin = mk.skin != null && MODEL_WIDE != null;
        RenderType type = useSkin
                ? SKIN_TYPES.computeIfAbsent(mk.skin, ACRenderTypes::tpMark)
                : TYPES[(int) ((GameTimer.getPausableTime() * 20 / 2.5) % FRAMES)];
        VertexConsumer vc = buffers.getBuffer(type);

        float r = 1, g = 1, b = 1;
        if (!mk.available) {
            g = 0.2f;
            b = 0.2f;
        }

        float alpha = useSkin ? 0.30f : 1.0f;

        pose.pushPose();

        pose.mulPose(Axis.YP.rotationDegrees(useSkin ? 180 - mk.yaw : -mk.yaw));
        pose.scale(-1, -1, 1);
        if (useSkin) {
            var model = mk.slimArms ? MODEL_SLIM : MODEL_WIDE;
            model.renderToBuffer(pose, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, r, g, b, alpha);
        } else {
            ROOT.render(pose, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, r, g, b, alpha);
        }
        pose.popPose();

        RenderSystem.disableDepthTest();
        buffers.endBatch(type);
        RenderSystem.enableDepthTest();
    }
}
