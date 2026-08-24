package cn.academy.ability.vanilla.vecmanip.skill;

import cn.academy.ability.AbilityPipeline;
import cn.academy.util.AimTrace;
import cn.academy.ACSounds;
import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientContext;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.RegClientContext;
import cn.academy.client.render.util.AnimPresets;
import cn.academy.client.render.util.HandAnim;
import cn.academy.client.render.util.HandRenderOverride;
import cn.academy.config.AbilityConfig;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.util.GameTimer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.LogicalSide;

import static cn.lambdalib2.util.MathUtils.lerpf;

public class DirectedShock extends Skill {

    public static final DirectedShock INSTANCE = new DirectedShock();

    static final double RANGE = 3;

    public DirectedShock() {
        super("dir_shock", 1);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void activate(ClientRuntime rt, int keyID) {
        activateSingleKey2(rt, keyID, ShockContext::new);
    }

    static LivingEntity traceTarget(Player player) {
        Vec3 eye = player.getEyePosition(1.0f);
        Vec3 look = player.getViewVector(1.0f);
        Vec3 rayEnd = eye.add(look.scale(RANGE));

        BlockHitResult block = player.level().clip(new ClipContext(
                eye, rayEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 clipEnd = block.getType() == HitResult.Type.BLOCK ? block.getLocation() : rayEnd;

        EntityHitResult hit = AimTrace.firstResult(player.level(), player, eye, clipEnd,
                e -> e != player && e.isAlive() && e instanceof LivingEntity
                        && AbilityPipeline.canTarget(player, e));

        return hit == null ? null : (LivingEntity) hit.getEntity();
    }

    public static class ShockContext extends Context<DirectedShock> {

        static final String MSG_PERFORM = "perform";
        static final String MSG_GENERATE_EFFECT = "gen_eff";

        private static final int MIN_TICKS = 6;

        private static final int MAX_ACCEPTED_TICKS = 50;

        private static final int MAX_TOLERANT_TICKS = 200;

        private final float exp = ctx.getSkillExp();
        private final float damage = AbilityConfig.stat("dir_shock", "damage", exp);

        private int ticker = 0;

        public ShockContext(Player player) {
            super(player, INSTANCE);
        }

        @Listener(channel = MSG_TICK, side = LogicalSide.CLIENT)
        private void l_tick() {
            if (!isLocal()) return;
            ticker++;
            if (ticker >= MAX_TOLERANT_TICKS) {
                player.displayClientMessage(Component.translatable("gui.academy.dir_shock.too_long"), true);
                terminate();
            }
        }

        @Listener(channel = MSG_KEYUP, side = LogicalSide.CLIENT)
        private void l_keyUp() {
            if (ticker > MIN_TICKS && ticker < MAX_ACCEPTED_TICKS) {
                sendToServer(MSG_PERFORM);
            } else {

                player.displayClientMessage(Component.translatable(ticker <= MIN_TICKS
                        ? "gui.academy.dir_shock.too_short" : "gui.academy.dir_shock.too_long"), true);
                terminate();
            }
        }

        @Listener(channel = MSG_KEYABORT, side = LogicalSide.CLIENT)
        private void l_keyAbort() {
            terminate();
        }

        @Listener(channel = MSG_PERFORM, side = LogicalSide.SERVER)
        private void s_perform() {
            if (ctx.consume(AbilityConfig.overload("dir_shock", exp), AbilityConfig.cp("dir_shock", exp))) {
                LivingEntity target = traceTarget(player);
                if (target != null) {
                    ctx.attack(target, damage);
                    knockback(target);

                    Vec3 push = target.position().subtract(player.position()).normalize().scale(0.24);
                    target.setDeltaMovement(target.getDeltaMovement().add(push));
                    target.hurtMarked = true;

                    ctx.setCooldown((int) AbilityConfig.cooldown("dir_shock", exp));
                    ctx.addSkillExp(0.0035f);
                    sendToClient(MSG_GENERATE_EFFECT);
                } else {
                    ctx.addSkillExp(0.0010f);
                }
            }
            terminate();
        }

        private void knockback(Entity target) {
            if (exp < 0.25f) return;

            Vec3 delta = player.getEyePosition(1.0f).subtract(target.getEyePosition(1.0f)).normalize();
            delta = new Vec3(delta.x, delta.y - 0.6, delta.z).normalize();

            target.setPos(target.getX(), target.getY() + 0.1, target.getZ());
            target.setDeltaMovement(delta.x * -0.7, delta.y * -0.7, delta.z * -0.7);
            target.hurtMarked = true;
        }
    }

    @OnlyIn(Dist.CLIENT)
    @RegClientContext(ShockContext.class)
    public static class ShockContextC extends ClientContext {

        private static final double PREPARE_SCALE = 0.15, PREPARE_MAX_T = 2.0;

        private static final double PUNCH_SCALE = 0.3;

        private HandFx prepareFx;

        public ShockContextC(ShockContext par) {
            super(par);
        }

        @Listener(channel = MSG_MADEALIVE, side = LogicalSide.CLIENT)
        private void l_handEffectStart() {
            if (!isLocal()) return;
            prepareFx = new HandFx(AnimPresets.createPrepareAnim(), PREPARE_SCALE, PREPARE_MAX_T, false);
            HandRenderOverride.addInterrupt(prepareFx);
        }

        @Listener(channel = ShockContext.MSG_GENERATE_EFFECT, side = LogicalSide.CLIENT)
        private void c_effect() {
            player.level().playLocalSound(player.getX(), player.getY(), player.getZ(),
                    ACSounds.VM_DIRECTED_SHOCK.get(), SoundSource.AMBIENT, 0.5f, 1.0f, false);

            if (isLocal()) {

                HandRenderOverride.addInterrupt(new HandFx(AnimPresets.createPunchAnim(), PUNCH_SCALE, 1.0, true));
            }
        }

        @Listener(channel = MSG_TERMINATED, side = LogicalSide.CLIENT)
        private void l_handEffectTerminate() {
            if (prepareFx != null) {

                HandRenderOverride.stopInterrupt(prepareFx);
                prepareFx = null;
            }
        }

        private static final class HandFx implements HandRenderOverride.IHandRenderer {
            private final HandAnim anim;
            private final double scale, maxT;
            private final boolean selfStop;
            private final double start = GameTimer.getPausableTime();

            HandFx(HandAnim anim, double scale, double maxT, boolean selfStop) {
                this.anim = anim;
                this.scale = scale;
                this.maxT = maxT;
                this.selfStop = selfStop;
            }

            @Override
            public void applyTransform(PoseStack ps, float partialTicks) {
                double t = (GameTimer.getPausableTime() - start) / scale;
                if (t >= maxT) {
                    if (selfStop) {
                        HandRenderOverride.stopInterrupt(this);
                        return;
                    }
                    t = maxT;
                }
                anim.apply(ps, t);
            }
        }
    }
}
