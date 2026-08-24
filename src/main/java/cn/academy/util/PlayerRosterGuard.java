package cn.academy.util;

import cn.academy.AcademyCraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod.EventBusSubscriber(modid = AcademyCraft.MODID)
public final class PlayerRosterGuard {

    private static final Logger LOG = LoggerFactory.getLogger("ACRoster");

    private PlayerRosterGuard() {}

    private static final int PERIOD = 20;

    private static int cooldown;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (--cooldown > 0) {
            return;
        }
        cooldown = PERIOD;

        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }

        for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
            try {
                repairIfMissing(sp);
            } catch (Throwable t) {
                LOG.error("[ac-roster] failed while checking {}", sp.getGameProfile().getName(), t);
            }
        }
    }

    private static void repairIfMissing(ServerPlayer sp) {
        if (!(sp.level() instanceof ServerLevel lvl)) {
            return;
        }
        if (lvl.players().contains(sp)) {
            return;
        }

        if (cn.academy.util.ACLife.trueLife(sp) <= 0.0F) {
            return;
        }

        lvl.players().add(sp);

        String tracking;
        try {
            lvl.getChunkSource().addEntity(sp);
            tracking = "tracking restored as well";
        } catch (IllegalStateException alreadyTracked) {
            tracking = "tracking was still in place";
        }

        LOG.warn("[ac-roster] {} is alive but fell out of ServerLevel.players (removed by a third party), restored -- {}."
                        + "players missing from this list stop receiving entity visibility updates (symptom: models linger after others log out)",
                sp.getGameProfile().getName(), tracking);
    }
}
