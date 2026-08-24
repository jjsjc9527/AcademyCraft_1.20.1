package cn.academy.client.compat;

import cn.academy.ability.vanilla.vecmanip.advanced.DualWingAnim;
import cn.academy.client.render.BodyBones;
import cn.academy.client.render.MagLimbBones;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.ModList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;
import java.util.function.Consumer;

public final class EpicFightCompat {

    private EpicFightCompat() {}

    private static final Logger LOG = LogManager.getLogger("AcademyCraft/EpicFightCompat");

    private static final String MOD_ID = "epicfight";
    private static final String EVENT_CLASS = "yesman.epicfight.api.client.forgeevent.RenderEpicFightPlayerEvent";

    private static Method getPlayerPatch;
    private static Method setShouldRender;
    private static Method getOriginal;

    @SuppressWarnings("unchecked")
    public static void init() {
        if (!ModList.get().isLoaded(MOD_ID)) {
            return;
        }
        Class<?> eventClass;
        try {
            eventClass = Class.forName(EVENT_CLASS);
            getPlayerPatch = eventClass.getMethod("getPlayerPatch");
            setShouldRender = eventClass.getMethod("setShouldRender", boolean.class);

            getOriginal = getPlayerPatch.getReturnType().getMethod("getOriginal");
        } catch (Throwable t) {

            LOG.warn("Epic Fight detected but its render hook did not match (version change?), flight animation compatibility disabled", t);
            return;
        }
        if (!Event.class.isAssignableFrom(eventClass)) {
            LOG.warn("Epic Fight {} is not a Forge event, compatibility disabled", EVENT_CLASS);
            return;
        }
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false,
                (Class<Event>) eventClass, (Consumer<Event>) EpicFightCompat::onRenderEpicFightPlayer);
        LOG.info("hooked into Epic Fight RenderEpicFightPlayerEvent: this mod takes over player rendering during dual wing flight");
    }

    private static void onRenderEpicFightPlayer(Event event) {
        if (disabled) {
            return;
        }
        try {
            Object patch = getPlayerPatch.invoke(event);
            if (patch == null) {
                return;
            }
            if (getOriginal.invoke(patch) instanceof Player player && wantsOwnRender(player)) {
                setShouldRender.invoke(event, false);
            }
        } catch (Throwable t) {

            disabled = true;
            LOG.warn("Epic Fight compatibility hook failed, disabled (no retry this session)", t);
        }
    }

    private static volatile boolean disabled;

    private static boolean wantsOwnRender(Player player) {
        return DualWingAnim.isPlaying(player)
                || BodyBones.has(player.getUUID())
                || MagLimbBones.isActive(player.getUUID());
    }
}
