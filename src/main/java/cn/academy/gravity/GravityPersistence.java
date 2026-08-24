package cn.academy.gravity;

import cn.academy.AcademyCraft;
import cn.academy.network.GravitySyncMessage;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AcademyCraft.MODID)
public final class GravityPersistence {

    private GravityPersistence() {}

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        Direction g = ACGravity.getGravityDirection(sp);
        if (g != Direction.DOWN) {

            GravitySyncMessage.sync(sp, g, false, true);
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking e) {
        if (!(e.getEntity() instanceof ServerPlayer viewer)) return;
        Entity target = e.getTarget();
        Direction g = ACGravity.getGravityDirection(target);
        if (g != Direction.DOWN) {
            GravitySyncMessage.syncTo(viewer, target, g);
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone e) {
        if (e.isWasDeath() && !cn.academy.util.ACRespawn.isRebuilding()) {
            return;
        }
        Direction g = ACGravity.getGravityDirection(e.getOriginal());
        if (g != Direction.DOWN) {
            ACGravity.setGravityDirectionRaw(e.getEntity(), g, false);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        Direction g = ACGravity.getGravityDirection(sp);
        if (g != Direction.DOWN) {
            ACGravity.initGravityDirection(sp, g);
            GravitySyncMessage.sync(sp, g, false, true);
        }
    }
}
