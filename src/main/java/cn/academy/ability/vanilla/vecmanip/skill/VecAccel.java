package cn.academy.ability.vanilla.vecmanip.skill;

import cn.academy.ACSounds;
import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientContext;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.IConsumptionProvider;
import cn.academy.ability.context.RegClientContext;
import cn.academy.client.render.entity.ACEffectEntities;
import cn.academy.config.AbilityConfig;
import cn.academy.entity.EntityParabola;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.LogicalSide;

import static cn.lambdalib2.util.MathUtils.lerpf;

public class VecAccel extends Skill {

    public static final VecAccel INSTANCE = new VecAccel();

    public VecAccel() {
        super("vec_accel", 2);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void activate(ClientRuntime rt, int keyID) {
        activateSingleKey2(rt, keyID, VecAccelContext::new);
    }

    public static class VecAccelContext extends Context<VecAccel> implements IConsumptionProvider {

        static final String MSG_PERFORM = "perform";

        private static final int MAX_CHARGE = 20;

        private static final double MAX_VELOCITY = 2.5;

        private final float exp = ctx.getSkillExp();
        private final float consumption = AbilityConfig.cp("vec_accel", exp);
        private final float overload = AbilityConfig.overload("vec_accel", exp);

        private final boolean ignoreGroundChecking = exp > 0.5f;

        private int ticker = 0;
        private boolean canPerform = true;

        public VecAccelContext(Player player) {
            super(player, INSTANCE);
        }

        @Override
        public float getConsumptionHint() {
            return consumption;
        }

        public double speed() {
            double prog = 0.4 + (1 - 0.4) * Math.max(0, Math.min(1, ticker / (double) MAX_CHARGE));
            return Math.sin(prog) * MAX_VELOCITY;
        }

        public boolean canPerform() {
            return canPerform;
        }

        @Listener(channel = MSG_TICK, side = LogicalSide.CLIENT)
        private void l_tick() {
            if (!isLocal()) return;
            ticker++;
            canPerform = ignoreGroundChecking || hasGroundBelow();
        }

        @Listener(channel = MSG_KEYUP, side = LogicalSide.CLIENT)
        private void l_keyUp() {
            if (!isLocal()) return;

            if (!canPerform) {
                player.displayClientMessage(Component.translatable("gui.academy.vec_accel.no_ground"), true);
                terminate();
                return;
            }
            if (!ctx.consume(overload, consumption)) {
                terminate();
                return;
            }

            if (player.isPassenger()) {
                player.stopRiding();
            }
            player.setDeltaMovement(Vec3.directionFromRotation(player.getXRot() - 10, player.getYRot())
                    .scale(speed()));
            player.hurtMarked = true;
            ctx.setCooldown((int) AbilityConfig.cooldown("vec_accel", exp));

            sendToServer(MSG_PERFORM);
        }

        @Listener(channel = MSG_KEYABORT, side = LogicalSide.CLIENT)
        private void l_keyAbort() {
            terminate();
        }

        private boolean hasGroundBelow() {
            BlockPos foot = player.blockPosition();
            for (int i = 0; i <= 2; i++) {
                if (!player.level().getBlockState(foot.below(i)).isAir()) {
                    return true;
                }
            }
            return false;
        }

        @Listener(channel = MSG_PERFORM, side = LogicalSide.SERVER)
        private void s_perform() {
            ctx.consume(overload, consumption);
            player.fallDistance = 0;
            ctx.addSkillExp(0.002f);

            sendToClient(MSG_PERFORM);
            terminate();
        }
    }

    @OnlyIn(Dist.CLIENT)
    @RegClientContext(VecAccelContext.class)
    public static class VecAccelContextC extends ClientContext {

        private final VecAccelContext par;
        private EntityParabola parabola;

        public VecAccelContextC(VecAccelContext par) {
            super(par);
            this.par = par;
        }

        @Listener(channel = MSG_MADEALIVE, side = LogicalSide.CLIENT)
        private void l_start() {
            if (!isLocal()) return;
            parabola = new EntityParabola(player, par, par::speed, par::canPerform);
            ACEffectEntities.spawn(parabola);
        }

        @Listener(channel = MSG_TERMINATED, side = LogicalSide.CLIENT)
        private void l_terminated() {
            if (parabola != null) {
                parabola.discard();
                parabola = null;
            }
        }

        @Listener(channel = VecAccelContext.MSG_PERFORM, side = LogicalSide.CLIENT)
        private void c_perform() {
            player.level().playLocalSound(player.getX(), player.getY(), player.getZ(),
                    ACSounds.VM_VEC_ACCEL.get(), SoundSource.AMBIENT, 0.35f, 1.0f, false);
        }
    }
}
