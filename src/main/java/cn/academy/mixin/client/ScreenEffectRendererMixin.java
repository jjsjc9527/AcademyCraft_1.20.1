package cn.academy.mixin.client;

import cn.academy.gravity.ACGravity;
import cn.academy.gravity.RotationUtil;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Pair;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererMixin {

    @Inject(
        method = "getOverlayBlock(Lnet/minecraft/world/entity/player/Player;)Lorg/apache/commons/lang3/tuple/Pair;",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private static void academy$getOverlayBlock(Player player, CallbackInfoReturnable<Pair<BlockState, BlockPos>> cir) {
        Direction g = ACGravity.getGravityDirection(player);
        if (g == Direction.DOWN) return;

        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        Vec3 eyePos = player.getEyePosition();
        Vector3f mul = RotationUtil.vecPlayerToWorld(
                player.getBbWidth() * 0.8F, 0.1F, player.getBbWidth() * 0.8F, g);
        for (int i = 0; i < 8; ++i) {
            double x = eyePos.x + (double) (((float) ((i >> 0) % 2) - 0.5F) * mul.x());
            double y = eyePos.y + (double) (((float) ((i >> 1) % 2) - 0.5F) * mul.y());
            double z = eyePos.z + (double) (((float) ((i >> 2) % 2) - 0.5F) * mul.z());
            mutable.set(x, y, z);
            BlockState state = player.level().getBlockState(mutable);
            if (state.getRenderShape() != RenderShape.INVISIBLE && state.isViewBlocking(player.level(), mutable)) {
                cir.setReturnValue(Pair.of(state, mutable.immutable()));
                return;
            }
        }
        cir.setReturnValue(null);
    }
}
