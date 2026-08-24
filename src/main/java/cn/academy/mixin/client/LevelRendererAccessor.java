package cn.academy.mixin.client;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.PostChain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelRenderer.class)
public interface LevelRendererAccessor {

    @Accessor("transparencyChain")
    PostChain academy$getTransparencyChain();

    @Accessor("transparencyChain")
    void academy$setTransparencyChain(PostChain chain);
}
