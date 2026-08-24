package cn.academy.client.render.entity;

import cn.academy.entity.EntityCoinThrowing;
import cn.academy.gravity.RotationUtil;
import cn.lambdalib2.util.GameTimer;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CoinThrowingRenderer extends EntityRenderer<EntityCoinThrowing> {

    private static final ResourceLocation TEX_FRONT = new ResourceLocation("academy", "textures/item/coin_front.png");

    private final net.minecraft.client.renderer.entity.ItemRenderer itemRenderer;

    public CoinThrowingRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.itemRenderer = ctx.getItemRenderer();
    }

    @Override
    public void render(EntityCoinThrowing coin, float entityYaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffers, int light) {
        Player player = coin.player;
        if (player == null) return;
        if (coin.clientSpawnTime < 0) coin.clientSpawnTime = GameTimer.getPausableTime();

        Minecraft mc = Minecraft.getInstance();

        if (coin.isSync && player == mc.player) return;

        Direction g = coin.getGrav();

        if (RotationUtil.vecWorldToPlayer(coin.position(), g).y
                < RotationUtil.vecWorldToPlayer(player.position(), g).y) return;

        boolean fp = player == mc.player && mc.options.getCameraType().isFirstPerson();

        double dt = (GameTimer.getPausableTime() * 1000) % 150;

        pose.pushPose();
        {

            Vec3 coinLerp = new Vec3(
                    coin.xOld + (coin.getX() - coin.xOld) * partialTick,
                    coin.yOld + (coin.getY() - coin.yOld) * partialTick,
                    coin.zOld + (coin.getZ() - coin.zOld) * partialTick);
            double dx = 0, dy = 0, dz = 0;
            if (player != mc.player) {
                Vec3 playerLerp = new Vec3(
                        player.xOld + (player.getX() - player.xOld) * partialTick,
                        player.yOld + (player.getY() - player.yOld) * partialTick,
                        player.zOld + (player.getZ() - player.zOld) * partialTick);
                Vec3 lp = RotationUtil.vecWorldToPlayer(playerLerp, g);
                Vec3 lc = RotationUtil.vecWorldToPlayer(coinLerp, g);
                Vec3 target = RotationUtil.vecPlayerToWorld(lp.x, lc.y, lp.z, g);
                dx = target.x - coinLerp.x; dy = target.y - coinLerp.y; dz = target.z - coinLerp.z;
            }
            pose.translate(dx, dy, dz);

            if (g != Direction.DOWN) {
                pose.mulPose(RotationUtil.getCameraRotationQuaternion(g));
            }
            float yaw = fp ? player.getYRot() : player.yBodyRot;
            pose.mulPose(Axis.YN.rotationDegrees(yaw));

            double xMir = coin.getArm() == net.minecraft.world.entity.HumanoidArm.LEFT ? -1 : 1;
            pose.translate(-0.63 * xMir, 1, 0.30);
            float scale = 0.3F;
            pose.scale(scale, scale, scale);

            pose.translate(0.5, 0.5, 0);
            pose.mulPose(new org.joml.Quaternionf().rotateAxis(
                    (float) Math.toRadians(dt * 360.0 / 300.0),
                    (float) coin.axis.x, (float) coin.axis.y, (float) coin.axis.z));

            drawCoin(pose, buffers, light, coin.level());
        }
        pose.popPose();
    }

    private static final float THICKNESS_MUL = 2.0f;

    private void drawCoin(PoseStack pose, MultiBufferSource buffers, int light,
                          net.minecraft.world.level.Level level) {
        pose.pushPose();
        pose.scale(1f, 1f, THICKNESS_MUL);
        itemRenderer.renderStatic(
                new net.minecraft.world.item.ItemStack(cn.academy.ACItems.COIN.get()),
                net.minecraft.world.item.ItemDisplayContext.NONE,
                light, OverlayTexture.NO_OVERLAY, pose, buffers, level, 0);
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(EntityCoinThrowing e) {
        return TEX_FRONT;
    }
}
