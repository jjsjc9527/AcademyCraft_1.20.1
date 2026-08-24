package cn.academy.ability.vanilla.mentalout.skill;

import cn.academy.ability.AbilityPipeline;
import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientContext;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.RegClientContext;
import cn.academy.ability.vanilla.mentalout.DazeState;
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
import cn.academy.util.AimTrace;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.LogicalSide;

public class Daze extends Skill implements WideCastable {

    public static final Daze INSTANCE = new Daze();

    public static final String MSG_SYNC = "daze_sync";

    private Daze() {
        super("daze", 1);
    }

    @Listener(channel = MSG_SYNC, side = LogicalSide.CLIENT)
    private void c_sync(Entity target, Integer ticks) {
        if (target == null || ticks == null) {
            return;
        }

        boolean wasDazed = DazeState.isDazed(target);
        DazeState.setTicks(target, ticks);
        if (!wasDazed && ticks > 0
                && target == net.minecraft.client.Minecraft.getInstance().player) {
            DazeState.beginVisionSnapshot();
        }
    }

    static float rangeOf(float exp) {
        return AbilityConfig.stat("daze", "range", exp);
    }

    static LivingEntity trace(Player player, float range) {
        Vec3 eye = player.getEyePosition(1.0f);
        Vec3 look = player.getViewVector(1.0f);
        Vec3 rayEnd = eye.add(look.scale(range));

        BlockHitResult block = player.level().clip(new ClipContext(
                eye, rayEnd, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        Vec3 clipEnd = block.getType() == HitResult.Type.BLOCK ? block.getLocation() : rayEnd;

        EntityHitResult ent = AimTrace.firstResult(player.level(), player, eye, clipEnd,
                e -> e != player && e.isAlive() && e instanceof LivingEntity
                        && AbilityPipeline.canTarget(player, e));
        return ent != null && ent.getEntity() instanceof LivingEntity le ? le : null;
    }

    @Override
    public boolean wideApply(WideCastable.Call call, LivingEntity target) {
        DazeState.apply(target, (int) AbilityConfig.stat("daze", "duration", call.exp), call.caster);
        WideCastFx.at(target, ParticleTypes.END_ROD, 20, 0);
        return true;
    }

    @Override
    public boolean releaseFrom(Player caster, LivingEntity target) {
        if (!DazeState.isDazed(target) || DazeState.ownerOf(target) != caster) {
            return false;
        }
        DazeState.clear(target);
        return true;
    }

    @Override
    public float wideExp() {
        return 0.003f;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void activate(ClientRuntime rt, int keyID) {
        activateSingleKey2(rt, keyID, DazeContext::new);
    }

    public static class DazeContext extends Context<Daze> {

        static final String MSG_EXECUTE = "execute";

        private final float exp = ctx.getSkillExp();

        public DazeContext(Player player) {
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
            LivingEntity target = trace(player, rangeOf(exp));
            if (target != null && ctx.consume(
                    AbilityConfig.overload("daze", exp),
                    (int) AbilityConfig.cp("daze", exp))

                    && !cn.academy.ability.vanilla.mentalout.MentalImmune.blocked(ctx, target)) {
                DazeState.apply(target, (int) AbilityConfig.stat("daze", "duration", exp), player);
                done = true;
                ctx.addSkillExp(0.003f);
                ctx.setCooldown((int) AbilityConfig.cooldown("daze", exp));
            }
            sendToClient(MSG_EXECUTE, done);
            terminate();
        }
    }

    @OnlyIn(Dist.CLIENT)
    @RegClientContext(DazeContext.class)
    public static class DazeContextC extends ClientContext {

        private static final int[] COLOR_IDLE = {0xba, 0xba, 0xba};
        private static final int[] COLOR_LOCK = {0xff, 0xcd, 0x46};

        private final DazeContext par;
        private EntityMarker marker = null;

        public DazeContextC(DazeContext par) {
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
            LivingEntity t = trace(player, rangeOf(par.exp));
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

        @Listener(channel = DazeContext.MSG_EXECUTE, side = LogicalSide.CLIENT)
        private void c_end(boolean done) {
            dropMarker();
            if (!isLocal() || !done) {
                return;
            }

            Entity t = trace(player, rangeOf(par.exp));
            double x = t != null ? t.getX() : player.getX();
            double y = t != null ? t.getY() + t.getBbHeight() * 0.5 : player.getY() + 1;
            double z = t != null ? t.getZ() : player.getZ();
            double r = t != null ? t.getBbWidth() * 0.8 : 0.5;
            for (int i = 0; i < 28; ++i) {

                player.level().addParticle(ParticleTypes.END_ROD,
                        x + RandUtils.ranged(-r, r),
                        y + RandUtils.ranged(-r, r),
                        z + RandUtils.ranged(-r, r), 0, 0, 0);
            }
            player.level().playLocalSound(x, y, z,

                    cn.academy.ACSounds.V_BELL_RESONATE.get(), SoundSource.PLAYERS, 0.8f, 0.6f, false);
        }

        private void dropMarker() {
            if (marker != null) {
                marker.discard();
                marker = null;
            }
        }
    }
}
