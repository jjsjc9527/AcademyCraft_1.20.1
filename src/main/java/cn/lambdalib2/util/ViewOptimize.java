package cn.lambdalib2.util;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ViewOptimize {

    public interface IAssociatePlayer {
        Player getPlayer();

        default HumanoidArm getArm() {
            return HumanoidArm.RIGHT;
        }
    }

    private static final double
            fpOffsetX = -0.05,
            fpOffsetY = -0.25,
            fpOffsetZ = 0.2;

    private static final double
            tpOffsetX = 0.15,
            tpOffsetY = -0.8,
            tpOffsetZ = 0.23;

    public static void fixFirstPerson(PoseStack pose) {
        pose.translate(fpOffsetX, fpOffsetY, fpOffsetZ);
    }

    public static void fixThirdPerson(PoseStack pose) {
        pose.translate(tpOffsetX, tpOffsetY, tpOffsetZ);
    }

    public static void fix(PoseStack pose, IAssociatePlayer entity) {
        Vec3 v = getFixVector(entity);
        pose.translate(v.x, v.y, v.z);
    }

    public static Vec3 getFixVector(IAssociatePlayer entity) {

        double mir = entity.getArm() == HumanoidArm.LEFT ? -1 : 1;
        return isFirstPerson(entity)
                ? new Vec3(fpOffsetX, fpOffsetY, fpOffsetZ * mir)
                : new Vec3(tpOffsetX, tpOffsetY, tpOffsetZ * mir);
    }

    public static boolean isFirstPerson(IAssociatePlayer entity) {
        Minecraft mc = Minecraft.getInstance();
        return mc.options.getCameraType().isFirstPerson() && mc.player == entity.getPlayer();
    }
}
