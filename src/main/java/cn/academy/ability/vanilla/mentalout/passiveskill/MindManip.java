package cn.academy.ability.vanilla.mentalout.passiveskill;

import cn.academy.ability.Skill;
import cn.academy.ability.vanilla.mentalout.skill.ForcedControl;
import cn.academy.config.AbilityConfig;
import cn.academy.datapart.AbilityData;
import cn.academy.event.ability.SkillExpAddedEvent;
import cn.lambdalib2.datapart.EntityData;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class MindManip extends Skill {

    public static final MindManip INSTANCE = new MindManip();

    private MindManip() {
        super("mind_manip", 4);
        canControl = false;
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new ExpRelay());
    }

    public static final String MSG_IMMUNE_ARC = "mm_immune_arc";

    private static final int ARC_COUNT = 5;

    public static void sendImmuneArc(net.minecraft.world.entity.Entity target) {
        cn.lambdalib2.s11n.network.NetworkMessage.sendToTracking(
                target, INSTANCE, MSG_IMMUNE_ARC, target);
    }

    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    @cn.lambdalib2.s11n.network.NetworkMessage.Listener(
            channel = MSG_IMMUNE_ARC, side = net.minecraftforge.fml.LogicalSide.CLIENT)
    private void c_immuneArc(net.minecraft.world.entity.Entity target) {
        if (target == null) {
            return;
        }
        net.minecraft.world.phys.Vec3 head = new net.minecraft.world.phys.Vec3(
                target.getX(), target.getEyeY(), target.getZ());
        net.minecraft.client.player.LocalPlayer self = net.minecraft.client.Minecraft.getInstance().player;
        if (self == null) {
            return;
        }
        for (int i = 0; i < ARC_COUNT; i++) {
            double y = cn.lambdalib2.util.RandUtils.ranged(-1, 1);
            double t = cn.lambdalib2.util.RandUtils.ranged(0, Math.PI * 2);
            double r = Math.sqrt(Math.max(0, 1 - y * y));
            double len = cn.lambdalib2.util.RandUtils.ranged(0.7, 1.3);

            cn.academy.entity.EntityArc arc = new cn.academy.entity.EntityArc(
                    self, cn.academy.client.render.util.ArcPatterns.weakArc);
            arc.texWiggle = 0.7;
            arc.showWiggle = 0.1;
            arc.hideWiggle = 0.4;
            arc.setLife(7);
            arc.lengthFixed = false;
            arc.viewOptimize = false;
            arc.setFromTo(head.x, head.y, head.z,
                    head.x + r * Math.cos(t) * len, head.y + y * len, head.z + r * Math.sin(t) * len);
            cn.academy.client.render.entity.ACEffectEntities.spawn(arc);
        }
    }

    public static boolean obeysAlways(Player player) {
        if (player == null || !EntityData.isReady(player)) {
            return false;
        }
        AbilityData data = AbilityData.get(player);
        return data != null && data.isSkillLearned(INSTANCE);
    }

    public static class ExpRelay {

        @SubscribeEvent
        public void onForcedControlExp(SkillExpAddedEvent event) {
            if (event.skill != ForcedControl.INSTANCE) {
                return;
            }
            Player player = event.player;
            if (player == null || player.level().isClientSide || !EntityData.isReady(player)) {
                return;
            }
            AbilityData data = AbilityData.get(player);

            if (data == null || !data.isSkillLearned(INSTANCE)) {
                return;
            }
            float ratio = AbilityConfig.stat("mind_manip", "exp_ratio", 0f);
            if (ratio <= 0f) {
                return;
            }

            data.addSkillExp(INSTANCE, event.amount * ratio);
        }
    }
}
