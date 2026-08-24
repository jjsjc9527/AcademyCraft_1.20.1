package cn.academy.gravity;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class GravityClientHandler {

    private GravityClientHandler() {}

    private static int[] pendingSelf = null;

    public static void apply(int entityId, int dir3d, boolean animate, boolean init) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Entity e = mc.level.getEntity(entityId);
        if (e == null) return;

        if (!init && e == mc.player
                && cn.academy.ability.vanilla.mentalout.DazeState.isLocalPlayerDazed()) {
            pendingSelf = new int[]{entityId, dir3d, animate ? 1 : 0};
            return;
        }

        applyNow(e, dir3d, animate, init);
    }

    private static void applyNow(Entity e, int dir3d, boolean animate, boolean init) {
        Direction dir = Direction.from3DDataValue(dir3d);
        if (init) {
            ACGravity.initGravityDirection(e, dir);
        } else {
            ACGravity.setGravityDirection(e, dir, animate);
        }
    }

    public static void flushPending() {
        int[] p = pendingSelf;
        pendingSelf = null;
        if (p == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Entity e = mc.level.getEntity(p[0]);
        if (e == null) return;
        applyNow(e, p[1], p[2] != 0, false);
    }
}
