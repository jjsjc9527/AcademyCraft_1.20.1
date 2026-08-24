package cn.academy.client.auxgui;

import cn.academy.Resources;
import cn.academy.datapart.AbilityData;
import cn.academy.datapart.CPData;
import cn.lambdalib2.auxgui.AuxGui;
import cn.lambdalib2.util.Color;
import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.GameTimer;
import cn.lambdalib2.util.HudUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BackgroundMask extends AuxGui {

    public static final BackgroundMask instance = new BackgroundMask();

    public static void init() {
        AuxGui.register(instance);
    }

    private final ResourceLocation MASK = Resources.getTexture("effects/screen_mask");

    private final Color CRL_OVERRIDE = new Color(208, 20, 20, 170);

    private static final double CHANGE_PER_SEC = 1;

    private double r, g, b, a;

    private long lastFrame;

    private BackgroundMask() {}

    @Override
    public void draw(GuiGraphics gg, float width, float height) {
        double time = GameTimer.getTime();

        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        if (!cn.lambdalib2.datapart.EntityData.isReady(player)) return;
        AbilityData aData = AbilityData.get(player);
        CPData cpData = CPData.get(player);

        double cr, cg, cb, ca;

        Color color = null;
        if (cpData.isOverloaded()) {
            color = CRL_OVERRIDE;
        } else if (cpData.isActivated() && aData.hasCategory()) {

            color = aData.getCategory().getColorStyle();
        }

        if (color == null) {

            cr = r;
            cg = g;
            cb = b;
            ca = 0;
        } else {
            cr = Colors.i2f(color.getRed());
            cg = Colors.i2f(color.getGreen());
            cb = Colors.i2f(color.getBlue());
            ca = Colors.i2f(color.getAlpha());
        }

        if (ca != 0 || a != 0) {
            long dt = lastFrame == 0 ? 0 : (long) (time * 1000) - lastFrame;
            r = balanceTo(r, cr, dt);
            g = balanceTo(g, cg, dt);
            b = balanceTo(b, cb, dt);
            a = balanceTo(a, ca, dt);

            HudUtils.setPose(gg.pose());
            RenderSystem.setShaderColor((float) r, (float) g, (float) b, (float) a);
            HudUtils.loadTexture(MASK);
            HudUtils.rect(0, 0, width, height);
            RenderSystem.setShaderColor(1, 1, 1, 1);
        } else {

            r = cr;
            g = cg;
            b = cb;
        }

        lastFrame = (long) (time * 1000);
    }

    private double balanceTo(double from, double to, long dt) {
        double delta = to - from;
        delta = Math.signum(delta) * Math.min(Math.abs(delta), dt / 1000.0 * CHANGE_PER_SEC);
        return from + delta;
    }
}
