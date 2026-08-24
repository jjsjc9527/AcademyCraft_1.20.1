package cn.academy.mixin.client;

import cn.academy.client.render.util.HandRenderOverride;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {

    @Unique private boolean academy$pushed = false;

    @Inject(method = "renderHandsWithItems", at = @At("HEAD"))
    private void academy$applyOverride(float partialTicks, PoseStack ps, MultiBufferSource.BufferSource buffers,
                                       LocalPlayer player, int light, CallbackInfo ci) {
        HandRenderOverride.IHandRenderer r = HandRenderOverride.get();
        if (r != null) {
            ps.pushPose();
            r.applyTransform(ps, partialTicks);
            academy$pushed = true;
        }
    }

    @Inject(method = "renderHandsWithItems", at = @At("RETURN"))
    private void academy$popOverride(float partialTicks, PoseStack ps, MultiBufferSource.BufferSource buffers,
                                     LocalPlayer player, int light, CallbackInfo ci) {
        if (academy$pushed) {
            ps.popPose();
            academy$pushed = false;
        }
    }
}
