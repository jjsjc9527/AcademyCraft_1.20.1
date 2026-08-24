package cn.academy.ability.vanilla.vecmanip.skill;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Mod.EventBusSubscriber(modid = "academy", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GroundHeave {

    public static final int LIFETIME = 20 * 60 * 5;

    public static final double HEAVE_MAX = 0.5;

    public static final double HEAVE_STEP = 0.01;

    private static final int SWEEP_INTERVAL = 100;

    private static final Map<ResourceKey<Level>, Map<BlockPos, Long>> FIELDS = new HashMap<>();

    private GroundHeave() {}

    private static Map<BlockPos, Long> of(Level level) {
        return FIELDS.computeIfAbsent(level.dimension(), k -> new HashMap<>());
    }

    public static void add(ServerLevel level, BlockPos pos) {
        of(level).put(pos.immutable(), level.getGameTime() + LIFETIME);
    }

    public static boolean isHeaved(ServerLevel level, BlockPos pos) {
        Long until = of(level).get(pos);
        return until != null && until > level.getGameTime();
    }

    public static void clear(ServerLevel level, BlockPos pos) {
        of(level).remove(pos);
    }

    public static double heightAt(double dist, double maxDist) {
        double t = maxDist <= 1.0e-4 ? 0 : Math.min(1.0, dist / maxDist);
        return Math.round(HEAVE_MAX * t / HEAVE_STEP) * HEAVE_STEP;
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side != LogicalSide.SERVER) {
            return;
        }
        if (event.level.getGameTime() % SWEEP_INTERVAL != 0) {
            return;
        }
        Map<BlockPos, Long> map = FIELDS.get(event.level.dimension());
        if (map == null || map.isEmpty()) {
            return;
        }
        long now = event.level.getGameTime();
        for (Iterator<Map.Entry<BlockPos, Long>> it = map.entrySet().iterator(); it.hasNext(); ) {
            if (it.next().getValue() <= now) {
                it.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(net.minecraftforge.event.server.ServerStoppedEvent event) {
        FIELDS.clear();
    }
}
