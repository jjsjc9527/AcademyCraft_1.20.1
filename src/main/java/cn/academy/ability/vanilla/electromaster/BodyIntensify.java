package cn.academy.ability.vanilla.electromaster;

import cn.academy.ACSounds;
import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientContext;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.RegClientContext;
import cn.academy.client.render.entity.ACEffectEntities;
import cn.academy.client.sound.FollowEntitySound;
import cn.academy.entity.EntityIntensifyEffect;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.util.RandUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.LogicalSide;

import cn.academy.config.AbilityConfig;
import static cn.lambdalib2.util.MathUtils.lerp;
import static cn.lambdalib2.util.MathUtils.lerpf;

public class BodyIntensify extends Skill {

    public static final BodyIntensify INSTANCE = new BodyIntensify();

    public static final int MIN_TIME = 10, MAX_TIME = 40, MAX_TOLERANT_TIME = 100;

    private static final MobEffect[] EFFECTS = {
            MobEffects.MOVEMENT_SPEED, MobEffects.JUMP, MobEffects.REGENERATION,
            MobEffects.DAMAGE_BOOST, MobEffects.DAMAGE_RESISTANCE
    };
    private static final int[] MAX_AMP = {3, 1, 1, 1, 1};

    public BodyIntensify() {
        super("body_intensify", 3);
    }

    @Override
    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    public void activate(cn.academy.ability.context.ClientRuntime rt, int keyID) {
        activateSingleKey2(rt, keyID, IntensifyContext::new);
    }

    public static class IntensifyContext extends Context<BodyIntensify> {

        static final String MSG_END = "end";
        static final String MSG_EFFECT_END = "effect_end";

        private int tick = 0;
        private float overload = 0f;
        private final float consumption = AbilityConfig.cp("body_intensify", ctx.getSkillExp());

        public IntensifyContext(Player player) {
            super(player, INSTANCE);
        }

        private double getProbability(int ct) { return (ct - 10.0) / 18.0; }
        private int getBuffLevel(int ct) { return (int) Math.floor(getProbability(ct)); }
        private int getBuffTime(int ct) {
            return (int) (RandUtils.ranged(1, 2) * ct * lerp(1.5, 2.5, ctx.getSkillExp()));
        }
        private int getHungerBuffTime(int ct) { return (int) (1.25f * ct); }

        @Listener(channel = MSG_MADEALIVE, side = LogicalSide.SERVER)
        private void s_consume() {
            float ol = AbilityConfig.overload("body_intensify", ctx.getSkillExp());
            ctx.consume(ol, 0);
            this.overload = ctx.cpData.getOverload();
        }

        @Listener(channel = MSG_TICK, side = LogicalSide.SERVER)
        private void s_onTick() {
            if (ctx.cpData.getOverload() < overload) ctx.cpData.setOverload(overload);
            tick += 1;
            if ((tick <= MAX_TIME && !ctx.consume(0, consumption)) || tick >= MAX_TOLERANT_TIME) {
                sendToClient(MSG_EFFECT_END, false);
                terminate();
            }
        }

        @Listener(channel = MSG_END, side = LogicalSide.SERVER)
        private void s_onEnd() {
            if (tick < MIN_TIME) {
                sendToClient(MSG_EFFECT_END, false);
                terminate();
                return;
            }
            if (tick >= MAX_TIME) tick = MAX_TIME;

            double p = getProbability(tick);
            int i = 0;
            int time = getBuffTime(tick);
            while (p > 0) {
                if (RandUtils.ranged(0, 1) < p) {
                    int level = getBuffLevel(tick);
                    i += 1;
                    player.addEffect(new MobEffectInstance(
                            EFFECTS[i], time, Math.min(level, MAX_AMP[i]), false, true));
                }
                p -= 1.0;
            }

            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, getHungerBuffTime(tick), 2));

            ctx.addSkillExp(0.01f);
            ctx.setCooldown((int) AbilityConfig.cooldown("body_intensify", ctx.getSkillExp()));
            sendToClient(MSG_EFFECT_END, true);
            terminate();
        }

        @Listener(channel = MSG_KEYUP, side = LogicalSide.CLIENT)
        private void l_onEnd() {
            sendToServer(MSG_END);
        }

        @Listener(channel = MSG_KEYABORT, side = LogicalSide.CLIENT)
        private void l_onAbort() {
            sendToSelf(MSG_EFFECT_END, false);
            terminate();
        }
    }

    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    @RegClientContext(IntensifyContext.class)
    public static class IntensifyContextC extends ClientContext {

        private FollowEntitySound loopSound;

        public IntensifyContextC(IntensifyContext par) {
            super(par);
        }

        @Listener(channel = MSG_MADEALIVE, side = LogicalSide.CLIENT)
        private void c_startEffect() {
            if (isLocal()) {
                loopSound = new FollowEntitySound(ACSounds.EM_INTENSIFY_LOOP.get(), player, 1.0f);
                Minecraft.getInstance().getSoundManager().play(loopSound);
            }
        }

        @Listener(channel = IntensifyContext.MSG_EFFECT_END, side = LogicalSide.CLIENT)
        private void c_endEffect(boolean performed) {
            if (isLocal() && loopSound != null) {
                loopSound.requestStop();
            }
            if (performed) {
                player.level().playLocalSound(player.getX(), player.getY(), player.getZ(),
                        ACSounds.EM_INTENSIFY_ACTIVATE.get(), SoundSource.AMBIENT, 0.5f, 1.0f, false);
                ACEffectEntities.spawn(new EntityIntensifyEffect(player));
            }
        }

        @Listener(channel = MSG_TERMINATED, side = LogicalSide.CLIENT)
        private void c_terminated() {
            if (loopSound != null) loopSound.requestStop();
        }
    }
}
