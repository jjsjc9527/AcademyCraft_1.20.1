package cn.academy.ability.vanilla.mentalout.advanced;

import cn.academy.ability.vanilla.mentalout.SelfLossState;
import cn.academy.ability.vanilla.mentalout.WideCastFx;
import cn.academy.ability.vanilla.mentalout.WideCastable;
import cn.academy.ability.vanilla.mentalout.skill.Daze;
import cn.academy.config.AbilityConfig;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.LogicalSide;

public class SelfLoss extends MentalAdvSkill implements WideCastable {

    public static final SelfLoss INSTANCE = new SelfLoss();

    public static final String MSG_SYNC = "self_loss_sync";

    private SelfLoss() {
        super("self_loss", Daze.INSTANCE, true);
    }

    @Listener(channel = MSG_SYNC, side = LogicalSide.CLIENT)
    private void c_sync(Entity target, Integer ticks) {
        if (target == null || ticks == null) {
            return;
        }
        SelfLossState.setTicks(target, ticks);
    }

    @Override
    public boolean wideApply(Call call, LivingEntity target) {
        SelfLossState.apply(target,
                (int) AbilityConfig.stat("self_loss", "duration", call.exp), call.caster);
        WideCastFx.at(target, ParticleTypes.SMOKE, 22, 0.02);
        return true;
    }

    @Override
    public boolean releaseFrom(Player caster, LivingEntity target) {
        if (!SelfLossState.isActive(target) || SelfLossState.ownerOf(target) != caster) {
            return false;
        }
        SelfLossState.clear(target);
        return true;
    }

    @Override
    public float wideExp() {
        return 0.003f;
    }
}
