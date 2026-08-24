package cn.lambdalib2.render.font;

import cn.lambdalib2.util.HudUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class MCFont implements IFont {

    public static final MCFont instance = new MCFont();

    private static final float BASE = 12.375f;

    private static Font mc() {
        return Minecraft.getInstance().font;
    }

    @Override
    public float getCharWidth(int chr, FontOption o) {
        return mc().width(String.valueOf((char) chr)) * (o.fontSize / BASE);
    }

    @Override
    public float getTextWidth(String str, FontOption o) {
        return mc().width(str) * (o.fontSize / BASE);
    }

    @Override
    public void draw(String str, float x, float y, FontOption o) {
        float scale = o.fontSize / BASE;
        float w = getTextWidth(str, o);
        float dx = x - w * o.align.lenOffset;

        Matrix4f m = new Matrix4f(HudUtils.getMatrix());
        m.translate(dx, y, 0);
        m.scale(scale, scale, 1);

        int argb = o.color == null ? 0xFFFFFFFF : o.color.toARGB();

        MultiBufferSource.BufferSource buffer =
                Minecraft.getInstance().renderBuffers().bufferSource();
        mc().drawInBatch(str, 0, 0, argb, false, m, buffer,
                Font.DisplayMode.NORMAL, 0, 0xF000F0);
        buffer.endBatch();
    }
}
