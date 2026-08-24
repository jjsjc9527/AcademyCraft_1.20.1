package cn.academy.ability.vanilla.mentalout.skill;

import cn.academy.ability.AbilityPipeline;
import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientContext;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.RegClientContext;
import cn.academy.ability.vanilla.mentalout.MentalCharm;
import cn.academy.ability.vanilla.mentalout.WideCastFx;
import cn.academy.ability.vanilla.mentalout.WideCastable;
import cn.academy.client.render.entity.ACEffectEntities;
import cn.academy.config.AbilityConfig;
import cn.academy.entity.EntityMarker;
import cn.academy.util.AimTrace;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.util.RandUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.LogicalSide;

public class Impression extends Skill implements WideCastable {

    public static final Impression INSTANCE = new Impression();

    private Impression() {
        super("impression", 1);
    }

    static Mob trace(Player player, float range) {
        Vec3 eye = player.getEyePosition(1.0f);
        Vec3 look = player.getViewVector(1.0f);
        Vec3 rayEnd = eye.add(look.scale(range));

        BlockHitResult block = player.level().clip(new ClipContext(
                eye, rayEnd, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        Vec3 clipEnd = block.getType() == HitResult.Type.BLOCK ? block.getLocation() : rayEnd;

        EntityHitResult ent = AimTrace.firstResult(player.level(), player, eye, clipEnd,
                e -> e != player && e.isAlive() && e instanceof Mob
                        && AbilityPipeline.canTarget(player, e));
        return ent != null && ent.getEntity() instanceof Mob m ? m : null;
    }

    static float rangeOf(float exp) {
        return AbilityConfig.stat("impression", "range", exp);
    }

    @Override
    public boolean wideAccepts(WideCastable.Call call, net.minecraft.world.entity.LivingEntity target) {
        return target instanceof Mob;
    }

    @Override
    public boolean wideApply(WideCastable.Call call, net.minecraft.world.entity.LivingEntity target) {
        if (!(target instanceof Mob mob)) {
            return false;
        }
        boolean toFriendly = MentalCharm.apply(call.caster, mob,
                (int) AbilityConfig.stat("impression", "duration", call.exp));
        WideCastFx.at(mob, toFriendly ? ParticleTypes.ENCHANT : ParticleTypes.WITCH, 16, 0.05);
        return true;
    }

    @Override
    public boolean releaseFrom(Player caster, net.minecraft.world.entity.LivingEntity target) {
        if (!MentalCharm.isActive(target) || MentalCharm.getOwner(target) != caster) {
            return false;
        }
        MentalCharm.clear(target);
        return true;
    }

    @Override
    public float wideExp() {
        return 0.003f;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void activate(ClientRuntime rt, int keyID) {
        activateSingleKey2(rt, keyID, ImpContext::new);
    }

    public static class ImpContext extends Context<Impression> {

        static final String MSG_EXECUTE = "execute";

        private final float exp = ctx.getSkillExp();

        public ImpContext(Player player) {
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
            int result = 0;
            Mob target = trace(player, rangeOf(exp));
            if (target != null && ctx.consume(
                    AbilityConfig.overload("impression", exp),
                    (int) AbilityConfig.cp("impression", exp))

                    && !cn.academy.ability.vanilla.mentalout.MentalImmune.blocked(ctx, target)) {
                int ticks = (int) AbilityConfig.stat("impression", "duration", exp);
                boolean toFriendly = MentalCharm.apply(player, target, ticks);
                result = toFriendly ? 1 : 2;
                ctx.addSkillExp(0.003f);
                ctx.setCooldown((int) AbilityConfig.cooldown("impression", exp));
            }
            sendToClient(MSG_EXECUTE, result);
            terminate();
        }
    }

    @OnlyIn(Dist.CLIENT)
    @RegClientContext(ImpContext.class)
    public static class ImpContextC extends ClientContext {

        private static final int[] COLOR_IDLE = {0xba, 0xba, 0xba};
        private static final int[] COLOR_LOCK = {0xff, 0xcd, 0x46};

        private final ImpContext par;
        private EntityMarker marker = null;

        public ImpContextC(ImpContext par) {
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
            Mob t = trace(player, rangeOf(par.exp));

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

        @Listener(channel = ImpContext.MSG_EXECUTE, side = LogicalSide.CLIENT)
        private void c_end(int result) {
            dropMarker();
            if (!isLocal() || result == 0) {
                return;
            }
            Entity t = trace(player, rangeOf(par.exp));

            double x = t != null ? t.getX() : player.getX();
            double y = t != null ? t.getY() + t.getBbHeight() * 0.75 : player.getY() + 1;
            double z = t != null ? t.getZ() : player.getZ();
            for (int i = 0; i < 24; ++i) {
                player.level().addParticle(
                        result == 1 ? ParticleTypes.ENCHANT : ParticleTypes.WITCH,
                        x + RandUtils.ranged(-0.4, 0.4), y + RandUtils.ranged(-0.4, 0.4),
                        z + RandUtils.ranged(-0.4, 0.4),
                        RandUtils.ranged(-0.05, 0.05), RandUtils.ranged(0.0, 0.08),
                        RandUtils.ranged(-0.05, 0.05));
            }
            player.level().playLocalSound(x, y, z,

                    cn.academy.ACSounds.V_AMETHYST_CHIME.get(), SoundSource.PLAYERS, 0.7f,
                    result == 1 ? 1.2f : 0.8f, false);
        }

        private void dropMarker() {
            if (marker != null) {
                marker.discard();
                marker = null;
            }
        }
    }
}
