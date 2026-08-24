package cn.academy.ability.vanilla.mentalout;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import cn.academy.util.ACDefense;

public final class Helpless {

    private Helpless() {}

    public static boolean isHelpless(Entity e) {
        return FaintState.isFainted(e) || SelfLossState.isActive(e)

                || DazeState.isDazed(e)

                || ControlState.playerHeldStill(e)

                || ProxyState.isProxyOwner(e);
    }

    public static boolean isPositionLocked(Entity e) {
        return isHelpless(e);
    }

    public static boolean isBlind(Entity e) {
        return SelfLossState.isActive(e) || ControlState.playerHeldStill(e);
    }

    @OnlyIn(Dist.CLIENT)
    public static boolean isLocalPlayerBlind() {
        net.minecraft.client.player.LocalPlayer p = net.minecraft.client.Minecraft.getInstance().player;
        return p != null && isBlind(p);
    }

    @OnlyIn(Dist.CLIENT)
    public static boolean isLocalPlayerHelpless() {
        net.minecraft.client.player.LocalPlayer p = net.minecraft.client.Minecraft.getInstance().player;
        return p != null && isHelpless(p);
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new HelplessEvents());
    }

    public static void tickAggro(Entity entity) {
        if (!(entity instanceof Mob mob) || mob.level().isClientSide || !isHelpless(mob)) {
            return;
        }
        if (mob.getTarget() != null) {
            mob.setTarget(null);
        }

        Brain<?> brain = mob.getBrain();
        if (brain.checkMemory(MemoryModuleType.ATTACK_TARGET, MemoryStatus.REGISTERED)) {
            brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
        }
    }

    public static class HelplessEvents {

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void onChangeTarget(LivingChangeTargetEvent event) {
            if (event.getNewTarget() != null && isHelpless(event.getEntity())) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void onHelplessAttack(LivingAttackEvent event) {
            if (event.getSource().getDirectEntity() instanceof LivingEntity attacker
                    && isHelpless(attacker)) {
                ACDefense.block(event);
            }
        }
    }
}
