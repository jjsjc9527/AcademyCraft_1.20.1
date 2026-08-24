package cn.academy.client.render;

import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class BodyBones {

    private BodyBones() {}

    public interface Sink {

        void storeBone(Vec3 origin, Vec3 left, Vec3 up, Vec3 front, int frame);
    }

    private static final Map<UUID, List<Sink>> SINKS = new ConcurrentHashMap<>();

    public static void register(UUID player, Sink sink) {
        SINKS.computeIfAbsent(player, k -> new CopyOnWriteArrayList<>()).add(sink);
    }

    public static void unregister(UUID player, Sink sink) {
        List<Sink> list = SINKS.get(player);
        if (list == null) {
            return;
        }
        list.remove(sink);
        if (list.isEmpty()) {
            SINKS.remove(player, list);
        }
    }

    public static boolean has(UUID player) {
        List<Sink> list = SINKS.get(player);
        return list != null && !list.isEmpty();
    }

    public static void feed(UUID player, Vec3 origin, Vec3 left, Vec3 up, Vec3 front, int frame) {
        List<Sink> list = SINKS.get(player);
        if (list == null) {
            return;
        }
        for (Sink s : list) {
            s.storeBone(origin, left, up, front, frame);
        }
    }
}
