package cn.academy.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

@OnlyIn(Dist.CLIENT)
public final class ScreenCopy {

    private static TextureTarget copy;

    private ScreenCopy() {}

    public static void capture() {
        Minecraft mc = Minecraft.getInstance();
        RenderTarget main = mc.getMainRenderTarget();
        if (main == null) {
            return;
        }
        int w = main.width;
        int h = main.height;
        if (w <= 0 || h <= 0) {
            return;
        }

        if (copy == null || copy.width != w || copy.height != h) {
            if (copy != null) {
                copy.destroyBuffers();
            }

            copy = new TextureTarget(w, h, false, Minecraft.ON_OSX);
            copy.setFilterMode(GL11.GL_LINEAR);
        }

        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, copy.frameBufferId);
        GlStateManager._glBlitFrameBuffer(0, 0, w, h, 0, 0, w, h,
                GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);

        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, main.frameBufferId);
    }

    public static boolean ready() {
        return copy != null;
    }

    public static int textureId() {
        return copy == null ? 0 : copy.getColorTextureId();
    }

    public static int width() {
        return copy == null ? 0 : copy.width;
    }

    public static int height() {
        return copy == null ? 0 : copy.height;
    }
}
