package cn.academy.client.render;

import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
public final class MagLimbBones {

    private static final class Entry {
        final Vec3[] pos = new Vec3[4];
        int frame = -1;
    }

    private static final Map<UUID, Entry> MAP = new ConcurrentHashMap<>();

    private static volatile int frameId = 0;

    private MagLimbBones() {}

    public static void newFrame() {
        frameId++;
    }

    public static int frame() {
        return frameId;
    }

    public static void setActive(UUID id) {
        MAP.computeIfAbsent(id, k -> new Entry());
    }

    public static void clearActive(UUID id) {
        MAP.remove(id);
    }

    public static boolean isActive(UUID id) {
        return MAP.containsKey(id);
    }

    public static void store(UUID id, Vec3[] tips) {
        Entry e = MAP.get(id);
        if (e == null) return;
        System.arraycopy(tips, 0, e.pos, 0, 4);
        e.frame = frameId;
    }

    public static Vec3 get(UUID id, int i) {
        Entry e = MAP.get(id);
        if (e == null || i < 0 || i >= 4 || e.pos[i] == null) return null;
        if (e.frame != frameId) return null;
        return e.pos[i];
    }
}
