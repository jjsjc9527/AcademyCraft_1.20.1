package cn.lambdalib2.cgui.component;

import cn.lambdalib2.cgui.Widget;
import cn.lambdalib2.cgui.annotation.CGuiEditorComponent;
import cn.lambdalib2.cgui.event.FrameEvent;
import cn.lambdalib2.util.Color;
import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.HudUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.resources.ResourceLocation;

@CGuiEditorComponent
public class DrawTexture extends Component {

    public static final ResourceLocation MISSING =
            new ResourceLocation("academy", "textures/block/machine_frame.png");

    public enum DepthTestMode { Default, Equals }

    public ResourceLocation texture;

    public Color color;

    public double zLevel = 0;

    public boolean writeDepth = true;

    public boolean doesUseUV;

    public double u = 0, v = 0, texWidth = 0, texHeight = 0;

    public DepthTestMode depthTestMode = DepthTestMode.Default;

    public DrawTexture() {
        this(MISSING);
    }

    public DrawTexture(ResourceLocation texture) {
        this(texture, Colors.white());
    }

    public DrawTexture(String name, ResourceLocation _texture, Color _color) {
        super(name);
        this.texture = _texture;
        this.color = _color;

        listen(FrameEvent.class, (w, e) -> {
            Colors.bindToGL(color);

            double prevZ = HudUtils.zLevel;
            HudUtils.zLevel = zLevel;

            if (texture != null && !texture.getPath().equals("<null>")) {
                HudUtils.loadTexture(texture);
                if (doesUseUV) {
                    HudUtils.rect(0, 0, u, v, w.transform.width, w.transform.height, texWidth, texHeight);
                } else {
                    HudUtils.rect(0, 0, w.transform.width, w.transform.height);
                }
            } else {
                HudUtils.colorRect(0, 0, w.transform.width, w.transform.height);
            }

            HudUtils.zLevel = prevZ;
            RenderSystem.setShaderColor(1, 1, 1, 1);
        });
    }

    public DrawTexture(ResourceLocation _texture, Color _color) {
        this("DrawTexture", _texture, _color);
    }

    public DrawTexture setTex(ResourceLocation t) {
        texture = t;
        return this;
    }

    public DrawTexture setUVRect(double u, double v, double texWidth, double texHeight) {
        doesUseUV = true;
        this.u = u;
        this.v = v;
        this.texWidth = texWidth;
        this.texHeight = texHeight;
        return this;
    }

    public DrawTexture setColor(Color c) {
        this.color.setColor(c);
        return this;
    }

    public static DrawTexture get(Widget w) {
        return w.getComponent(DrawTexture.class);
    }
}
