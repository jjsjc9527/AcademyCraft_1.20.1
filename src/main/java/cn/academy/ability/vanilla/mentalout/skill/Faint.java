package cn.academy.ability.vanilla.mentalout.skill;

import cn.academy.ability.AbilityContext;
import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientContext;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.RegClientContext;
import cn.academy.ability.vanilla.mentalout.FaintState;
import cn.academy.ability.vanilla.mentalout.WideCastFx;
import cn.academy.ability.vanilla.mentalout.WideCastable;
import cn.academy.client.render.entity.ACEffectEntities;
import cn.academy.config.AbilityConfig;
import cn.academy.entity.EntityMarker;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.util.RandUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.LogicalSide;

public class Faint extends Skill implements WideCastable {

    public static final Faint INSTANCE = new Faint();

    public static final String MSG_SYNC = "faint_sync";

    private Faint() {
        super("faint", 2);
    }

    @Listener(channel = MSG_SYNC, side = LogicalSide.CLIENT)
    private void c_sync(Entity target, Integer ticks, Float roll) {
        if (target == null || ticks == null) {
            return;
        }
        FaintState.setTicks(target, ticks);
        if (roll != null) {
            FaintState.setRoll(target, roll);
        }
    }

    static float rangeOf(float exp) {
        return AbilityConfig.stat("faint", "range", exp);
    }

    @Override
    public boolean wideApply(WideCastable.Call call, LivingEntity target) {
        float exp = call.exp;
        FaintState.apply(target,
                (int) AbilityConfig.stat("faint", "duration", exp),
                (int) AbilityConfig.stat("faint", "interval", exp),

                    AbilityContext.finalSkillDamage(call.caster, INSTANCE, target,
                            AbilityConfig.stat("faint", "damage", exp)),
                (int) AbilityConfig.stat("faint", "darkness", exp),
                call.caster);
        WideCastFx.at(target, ParticleTypes.SMOKE, 18, 0.02);
        return true;
    }

    @Override
    public boolean releaseFrom(Player caster, LivingEntity target) {
        if (!FaintState.isFainted(target) || FaintState.ownerOf(target) != caster) {
            return false;
        }
        FaintState.clear(target);
        return true;
    }

    @Override
    public float wideExp() {
        return 0.004f;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void activate(ClientRuntime rt, int keyID) {
        activateSingleKey2(rt, keyID, FaintContext::new);
    }

    public static class FaintContext extends Context<Faint> {

        static final String MSG_EXECUTE = "execute";

        private final float exp = ctx.getSkillExp();

        public FaintContext(Player player) {
            super(player, INSTANCE);
        }

        @Listener(channel = MSG_KEYUP, side = LogicalSide.CLIENT)
        private void l_onKeyUp() {
            sendToServer(MSG_EXECUTE);
        }

        @Listener(channel = MSG_KEYABORT, side = LogicalSide.CLIENT)
        private void l_onKeyAbort() {
            terminate();
        }

        @Listener(channel = MSG_EXECUTE, side = LogicalSide.SERVER)
        private void s_execute() {
            boolean done = false;
            LivingEntity target = Daze.trace(player, rangeOf(exp));
            if (target != null && ctx.consume(
                    AbilityConfig.overload("faint", exp),
                    (int) AbilityConfig.cp("faint", exp))

                    && !cn.academy.ability.vanilla.mentalout.MentalImmune.blocked(ctx, target)) {
                FaintState.apply(target,
                        (int) AbilityConfig.stat("faint", "duration", exp),
                        (int) AbilityConfig.stat("faint", "interval", exp),

                        AbilityContext.finalSkillDamage(player, INSTANCE, target,
                                AbilityConfig.stat("faint", "damage", exp)),
                        (int) AbilityConfig.stat("faint", "darkness", exp),
                        player);
                done = true;
                ctx.addSkillExp(0.004f);
                ctx.setCooldown((int) AbilityConfig.cooldown("faint", exp));
            }
            sendToClient(MSG_EXECUTE, done);
            terminate();
        }
    }

    @OnlyIn(Dist.CLIENT)
    @RegClientContext(FaintContext.class)
    public static class FaintContextC extends ClientContext {

        private static final int[] COLOR_IDLE = {0xba, 0xba, 0xba};
        private static final int[] COLOR_LOCK = {0xff, 0xcd, 0x46};

        private final FaintContext par;
        private EntityMarker marker = null;

        public FaintContextC(FaintContext par) {
            super(par);
            this.par = par;
        }

        @Listener(channel = MSG_MADEALIVE, side = LogicalSide.CLIENT)
        private void l_start() {
            if (isLocal()) {
                marker = new EntityMarker(player.level());
                marker.boxWidth = 0.6f;
                marker.boxHeight = 0.6f;
                marker.moveTo2(player.getX(), player.getY(), player.getZ());
                ACEffectEntities.spawn(marker);
            }
        }

        @Listener(channel = MSG_TICK, side = LogicalSide.CLIENT)
        private void l_tick() {
            if (!isLocal() || marker == null) {
                return;
            }
            LivingEntity t = Daze.trace(player, rangeOf(par.exp));
            if (t != null) {
                marker.moveTo2(t.getX(), t.getY(), t.getZ());
            } else {
                Vec3 end = player.getEyePosition(1.0f)
                        .add(player.getViewVector(1.0f).scale(rangeOf(par.exp)));
                marker.moveTo2(end.x, end.y, end.z);
            }
            marker.target = t;
            int[] c = t != null ? COLOR_LOCK : COLOR_IDLE;
            marker.color.set(c[0], c[1], c[2], 255);
            marker.touch();
        }

        @Listener(channel = MSG_TERMINATED, side = LogicalSide.CLIENT)
        private void l_terminated() {
            dropMarker();
        }

        @Listener(channel = FaintContext.MSG_EXECUTE, side = LogicalSide.CLIENT)
        private void c_end(boolean done) {
            dropMarker();
            if (!isLocal() || !done) {
                return;
            }

            Entity t = Daze.trace(player, rangeOf(par.exp));
            double x = t != null ? t.getX() : player.getX();
            double y = t != null ? t.getY() + t.getBbHeight() * 0.5 : player.getY() + 1;
            double z = t != null ? t.getZ() : player.getZ();
            double r = t != null ? t.getBbWidth() * 0.8 : 0.5;
            for (int i = 0; i < 24; ++i) {
                player.level().addParticle(ParticleTypes.SMOKE,
                        x + RandUtils.ranged(-r, r),
                        y + RandUtils.ranged(-r, r),
                        z + RandUtils.ranged(-r, r), 0, 0.02, 0);
            }
            player.level().playLocalSound(x, y, z,

                    cn.academy.ACSounds.V_PLAYER_BREATH.get(), SoundSource.PLAYERS, 1.0f, 0.6f, false);
        }

        private void dropMarker() {
            if (marker != null) {
                marker.discard();
                marker = null;
            }
        }
    }
}
