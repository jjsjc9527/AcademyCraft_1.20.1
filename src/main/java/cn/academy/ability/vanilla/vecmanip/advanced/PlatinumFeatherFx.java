package cn.academy.ability.vanilla.vecmanip.advanced;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cn.academy.client.render.misc.PlatinumFeatherParticle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class PlatinumFeatherFx {

    private PlatinumFeatherFx() {}

    private static final Set<Integer> ACTIVE = new HashSet<>();

    private static final Map<Integer, PlatinumFeatherParticle> BY_ID = new HashMap<>();

    private static SpriteSet sprites;

    public static void setSprites(SpriteSet s) {
        sprites = s;
    }

    private static int lifetime = 600;
    private static double empowerFallMul = 2.0;
    private static double launchSpeed = 1.2;
    private static int shotCost = 200;

    public static int lifetime() {
        return lifetime;
    }

    public static double empowerFallMul() {
        return empowerFallMul;
    }

    public static double launchSpeed() {
        return launchSpeed;
    }

    public static int shotCost() {
        return shotCost;
    }

    public static void begin(int ownerId, double[] cfg) {
        ACTIVE.add(ownerId);
        if (cfg != null && cfg.length >= 4) {
            lifetime = (int) cfg[0];
            empowerFallMul = cfg[1];
            launchSpeed = cfg[2];
            shotCost = (int) cfg[3];
        }
    }

    public static void end(int ownerId) {
        ACTIVE.remove(ownerId);
    }

    public static boolean isActive(int ownerId) {
        return ACTIVE.contains(ownerId);
    }

    public static void unregister(int featherId) {
        BY_ID.remove(featherId);
    }

    public static void clearAll() {
        ACTIVE.clear();
        BY_ID.clear();
    }

    public static void spawnBatch(int ownerId, double[] flat, boolean empowered) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || flat == null || sprites == null) {
            return;
        }
        for (int i = 0; i + 4 < flat.length; i += 5) {
            int id = (int) flat[i];
            PlatinumFeatherParticle p = new PlatinumFeatherParticle(
                    level, flat[i + 1], flat[i + 2], flat[i + 4],
                    ownerId, id, flat[i + 3], empowered, sprites);
            BY_ID.put(id, p);
            Minecraft.getInstance().particleEngine.add(p);
        }
    }

    public static void launchBatch(double[] flat) {
        if (flat == null) {
            return;
        }
        for (int i = 0; i + 2 < flat.length; i += 3) {
            PlatinumFeatherParticle p = BY_ID.get((int) flat[i]);
            if (p == null) {
                continue;
            }
            int tid = (int) flat[i + 1];
            if (tid >= 0) {
                p.launch(tid);
            } else {
                p.dropFrom(flat[i + 2]);
            }
        }
    }
}
