package cn.academy.client.render.entity;

import cn.academy.entity.EntityShiftNeedle;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class StuckNeedles {

    private static final Map<Integer, List<EntityShiftNeedle>> BY_TARGET = new HashMap<>();

    private StuckNeedles() {}

    public static void add(int targetId, EntityShiftNeedle needle) {
        List<EntityShiftNeedle> list = BY_TARGET.computeIfAbsent(targetId, k -> new ArrayList<>());
        if (!list.contains(needle)) {
            list.add(needle);
        }
    }

    public static void remove(int targetId, EntityShiftNeedle needle) {
        List<EntityShiftNeedle> list = BY_TARGET.get(targetId);
        if (list != null) {
            list.remove(needle);
            if (list.isEmpty()) {
                BY_TARGET.remove(targetId);
            }
        }
    }

    public static List<EntityShiftNeedle> get(int targetId) {
        List<EntityShiftNeedle> list = BY_TARGET.get(targetId);
        if (list == null) {
            return List.of();
        }
        list.removeIf(n -> n.isRemoved() || !n.isStuck());
        if (list.isEmpty()) {
            BY_TARGET.remove(targetId);
            return List.of();
        }
        return list;
    }

    public static boolean hasAnyOwnedBy(net.minecraft.world.entity.player.Player p) {
        for (List<EntityShiftNeedle> list : BY_TARGET.values()) {
            for (EntityShiftNeedle n : list) {
                if (!n.isRemoved() && n.isStuck() && n.isOwnedBy(p)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void clear() {
        BY_TARGET.clear();
    }
}
