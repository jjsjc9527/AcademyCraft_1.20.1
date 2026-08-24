package cn.academy.client.render.util;

import cn.academy.client.render.ACRenderTypes;
import cn.academy.client.render.util.ArcFactory.Arc;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class SubArcHandler {

    public final Arc[] arcs;

    private final List<SubArc> list = new LinkedList<>();

    public double frameRate = 1.0, switchRate = 1.0;

    public SubArcHandler(Arc[] arcs) {
        this.arcs = arcs;
    }

    public SubArc generateAt(Vec3 pos) {
        SubArc sa = new SubArc(pos, arcs.length);
        sa.frameRate = frameRate;
        sa.switchRate = switchRate;
        list.add(sa);

        return sa;
    }

    public void tick() {
        Iterator<SubArc> iter = list.iterator();
        while (iter.hasNext()) {
            SubArc sa = iter.next();
            if (sa.dead) {
                iter.remove();
            } else {
                sa.tick();
            }
        }
    }

    public void clear() {
        list.clear();
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    public void drawAll(PoseStack pose, MultiBufferSource buffers) {
        drawAll(pose, buffers, 1.0, 1.0f);
    }

    public void drawAll(PoseStack pose, MultiBufferSource buffers, double scaleMul, float alphaMul) {

        var vc = buffers.getBuffer(ACRenderTypes.arc(ArcFactory.TEXTURE, false));

        for (SubArc arc : list) {
            if (!arc.dead && arc.draw) {
                pose.pushPose();

                pose.translate(arc.pos.x, arc.pos.y, arc.pos.z);
                pose.mulPose(Axis.ZP.rotationDegrees((float) arc.rotZ));
                pose.mulPose(Axis.YP.rotationDegrees((float) arc.rotY));
                pose.mulPose(Axis.XP.rotationDegrees((float) arc.rotX));

                final double scale = arc.scale * scaleMul;
                pose.scale((float) scale, (float) scale, (float) scale);
                pose.translate(-arcs[arc.texID].length / 2, 0, 0);

                arcs[arc.texID].draw(pose, vc, alphaMul);

                pose.popPose();
            }
        }
    }
}
