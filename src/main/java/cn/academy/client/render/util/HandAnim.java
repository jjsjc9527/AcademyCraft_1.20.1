package cn.academy.client.render.util;

import cn.lambdalib2.vis.curve.CubicCurve;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class HandAnim {

    public CubicCurve tx, ty, tz;

    public CubicCurve rx, ry, rz;

    public void apply(PoseStack ps, double t) {
        ps.translate(at(tx, t), at(ty, t), at(tz, t));

        float ax = (float) at(rx, t), ay = (float) at(ry, t), az = (float) at(rz, t);
        if (ax != 0) ps.mulPose(Axis.XP.rotationDegrees(ax));
        if (ay != 0) ps.mulPose(Axis.YP.rotationDegrees(ay));
        if (az != 0) ps.mulPose(Axis.ZP.rotationDegrees(az));
    }

    private static double at(CubicCurve c, double t) {
        return c == null ? 0 : c.valueAt(t);
    }

    public static CubicCurve curve(double... xy) {
        CubicCurve c = new CubicCurve();
        for (int i = 0; i + 1 < xy.length; i += 2) {
            c.addPoint(xy[i], xy[i + 1]);
        }
        return c;
    }
}
