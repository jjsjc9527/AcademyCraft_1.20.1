package cn.academy.client;

import cn.academy.AcademyCraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = AcademyCraft.MODID, value = Dist.CLIENT)
public final class ACGhostSweeper {

    private static final Logger LOG = LoggerFactory.getLogger("ACGhost");

    private ACGhostSweeper() {}

    private static final int PERIOD = 20;

    private static final int STRIKES = 3;

    private static int cooldown;
    private static final Map<Integer, Integer> strikes = new HashMap<>();

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (--cooldown > 0) {
            return;
        }
        cooldown = PERIOD;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer self = mc.player;
        ClientLevel lvl = mc.level;
        var conn = mc.getConnection();
        if (self == null || lvl == null || conn == null) {
            strikes.clear();
            return;
        }
        try {
            Set<UUID> online = new HashSet<>();
            for (var info : conn.getOnlinePlayers()) {
                online.add(info.getProfile().getId());
            }

            if (online.isEmpty()) {
                strikes.clear();
                return;
            }

            for (AbstractClientPlayer other : lvl.players()) {
                if (other == self) {
                    continue;
                }
                int id = other.getId();
                if (online.contains(other.getUUID())) {
                    strikes.remove(id);
                    continue;
                }
                int n = strikes.merge(id, 1, Integer::sum);
                if (n < STRIKES) {
                    continue;
                }
                strikes.remove(id);

                LOG.warn("[ac-ghost] removing ghost player {} (id={}) -- absent from the player list for {} seconds, "
                                + "yet still present in the world | markedRemoved={} distance={} blocks",
                        other.getGameProfile().getName(), id, STRIKES,
                        other.isRemoved(), (int) Math.sqrt(other.distanceToSqr(self)));

                lvl.removeEntity(id, Entity.RemovalReason.DISCARDED);
            }

            strikes.keySet().removeIf(id -> lvl.getEntity(id) == null);
        } catch (Throwable t) {
            LOG.error("[ac-ghost] scan failed", t);
        }
    }
}
