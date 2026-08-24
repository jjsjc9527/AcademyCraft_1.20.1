package cn.academy.ability.vanilla.electromaster;

import cn.academy.ability.AbilityPipeline;
import cn.academy.util.AimTrace;
import cn.academy.ACSounds;
import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientContext;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.RegClientContext;
import cn.academy.client.render.entity.ACEffectEntities;
import cn.academy.client.render.util.ArcPatterns;
import cn.academy.client.sound.FollowEntitySound;
import cn.academy.config.AbilityConfig;
import cn.academy.energy.api.IFItemManager;
import cn.academy.energy.api.block.IWirelessNode;
import cn.academy.energy.api.block.IWirelessReceiver;
import cn.academy.entity.EntityArc;
import cn.academy.entity.EntitySurroundArc;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.LogicalSide;

import static cn.lambdalib2.util.MathUtils.lerpf;

public class CurrentCharging extends Skill {

    public static final CurrentCharging INSTANCE = new CurrentCharging();

    public static final double DISTANCE = 15.0;

    public CurrentCharging() {
        super("charging", 1);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void activate(ClientRuntime rt, int keyID) {
        activateSingleKey2(rt, keyID, ChargingContext::new);
    }

    static float chargingSpeed(float exp) {
        return (float) Math.floor(lerpf(15, 35, exp));
    }

    static float expIncr(boolean effective) {
        return effective ? 0.0001f : 0.00003f;
    }

    static boolean isChargeableBlock(BlockEntity be) {
        return be instanceof IWirelessNode || be instanceof IWirelessReceiver;
    }

    static void chargeBlock(BlockEntity be, double amt) {
        if (be instanceof IWirelessNode node) {
            node.setEnergy(Math.min(node.getMaxEnergy(), node.getEnergy() + amt));
        } else if (be instanceof IWirelessReceiver receiver) {
            receiver.injectEnergy(amt);
        }
    }

    static HitResult trace(Player player, double dist) {
        Vec3 eye = player.getEyePosition(1.0f);
        Vec3 look = player.getViewVector(1.0f);
        Vec3 rayEnd = eye.add(look.scale(dist));
        BlockHitResult block = player.level().clip(new ClipContext(
                eye, rayEnd, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        Vec3 clipEnd = block.getType() == HitResult.Type.BLOCK ? block.getLocation() : rayEnd;
        EntityHitResult ent = AimTrace.firstResult(player.level(), player, eye, clipEnd,
                e -> e != player && e.isAlive() && e instanceof LivingEntity

                        && AbilityPipeline.canTarget(player, e));
        if (ent != null) return ent;
        return block.getType() == HitResult.Type.BLOCK ? block : null;
    }

    public static class ChargingContext extends Context<CurrentCharging> {

        static final String MSG_EFFECT_START = "effect_start";
        static final String MSG_EFFECT_END = "effect_end";

        private final float exp = ctx.getSkillExp();
        private final float consumption = AbilityConfig.cp("charging", exp);
        private float overload = 0f;

        private final boolean isItem;

        public ChargingContext(Player player) {
            super(player, INSTANCE);
            isItem = !player.getMainHandItem().isEmpty();
        }

        @Listener(channel = MSG_MADEALIVE, side = LogicalSide.SERVER)
        private void s_onStart() {
            ctx.consume(AbilityConfig.overload("charging", exp), 0);
            overload = ctx.cpData.getOverload();
        }

        @Listener(channel = MSG_KEYDOWN, side = LogicalSide.CLIENT)
        private void l_onStart() {
            sendToServer(MSG_EFFECT_START, isItem);
        }

        @Listener(channel = MSG_EFFECT_START, side = LogicalSide.SERVER)
        private void s_onEffectStart(boolean item) {
            sendToClient(MSG_EFFECT_START, item);
        }

        @Listener(channel = MSG_EFFECT_END, side = LogicalSide.SERVER)
        private void s_onEffectEnd(boolean item) {
            sendToClient(MSG_EFFECT_END, item);
            terminate();
        }

        @Listener(channel = MSG_TICK, side = LogicalSide.SERVER)
        private void s_onTick() {
            if (ctx.cpData.getOverload() < overload) ctx.cpData.setOverload(overload);
            if (!isItem) {

                HitResult hit = trace(player, DISTANCE);
                boolean good = false;
                if (hit instanceof BlockHitResult bhr && hit.getType() == HitResult.Type.BLOCK) {
                    BlockEntity be = player.level().getBlockEntity(bhr.getBlockPos());
                    if (isChargeableBlock(be)) {
                        good = true;
                        chargeBlock(be, chargingSpeed(exp));
                    }
                }
                ctx.addSkillExp(expIncr(good));
                if (!ctx.consume(0, consumption)) {
                    sendToClient(MSG_EFFECT_END, isItem);
                    terminate();
                }
            } else {

                ItemStack stack = player.getMainHandItem();
                if (!stack.isEmpty() && ctx.consume(0, consumption)) {
                    boolean good = IFItemManager.instance.isSupported(stack);
                    if (good) {
                        IFItemManager.instance.charge(stack, chargingSpeed(exp));
                    }
                    ctx.addSkillExp(expIncr(good));
                } else {
                    sendToClient(MSG_EFFECT_END, isItem);
                    terminate();
                }
            }
        }

        @Listener(channel = MSG_KEYUP, side = LogicalSide.CLIENT)
        private void l_onEnd() {
            sendToServer(MSG_EFFECT_END, isItem);
        }

        @Listener(channel = MSG_KEYABORT, side = LogicalSide.CLIENT)
        private void l_onAbort() {
            sendToServer(MSG_EFFECT_END, isItem);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @RegClientContext(ChargingContext.class)
    public static class ChargingContextC extends ClientContext {

        private EntityArc arc;
        private EntitySurroundArc surround;
        private FollowEntitySound sound;
        private boolean isItem = false;

        public ChargingContextC(ChargingContext par) {
            super(par);
        }

        @Listener(channel = ChargingContext.MSG_EFFECT_START, side = LogicalSide.CLIENT)
        private void c_startEffects(boolean item) {
            if (!item) {

                arc = new EntityArc(player, ArcPatterns.chargingArc);
                arc.lengthFixed = false;
                arc.hideWiggle = 0.8;
                arc.showWiggle = 0.2;
                arc.texWiggle = 0.8;
                ACEffectEntities.spawn(arc);
                surround = new EntitySurroundArc(player.level(),
                        player.getX(), player.getY(), player.getZ(), 1, 1)
                        .setArcType(EntitySurroundArc.ArcType.NORMAL).setLife(100000);
                surround.draw = false;
                ACEffectEntities.spawn(surround);
            } else {

                surround = new EntitySurroundArc(player)
                        .setArcType(EntitySurroundArc.ArcType.THIN).setLife(100000);
                ACEffectEntities.spawn(surround);
            }
            sound = new FollowEntitySound(ACSounds.EM_CHARGE_LOOP.get(), player, 0.3f);
            Minecraft.getInstance().getSoundManager().play(sound);
            this.isItem = item;
        }

        @Listener(channel = MSG_TICK, side = LogicalSide.CLIENT)
        private void c_updateEffects() {
            if (isItem) return;
            HitResult hit = trace(player, DISTANCE);

            double x, y, z;
            boolean good = false;
            BlockPos goodPos = null;
            if (hit instanceof EntityHitResult ehr) {
                Vec3 p = ehr.getEntity().position();
                x = p.x; y = p.y + ehr.getEntity().getEyeHeight(); z = p.z;
            } else if (hit instanceof BlockHitResult bhr && hit.getType() == HitResult.Type.BLOCK) {
                Vec3 p = bhr.getLocation();
                x = p.x; y = p.y; z = p.z;
                BlockEntity be = player.level().getBlockEntity(bhr.getBlockPos());
                if (isChargeableBlock(be)) {
                    good = true;
                    goodPos = bhr.getBlockPos();
                }
            } else {
                Vec3 miss = player.position().add(player.getViewVector(1.0f).scale(DISTANCE));
                x = miss.x; y = miss.y; z = miss.z;
            }

            if (arc != null) {

                Vec3 eye = player.getEyePosition(1.0f);
                arc.setFromTo(eye.x, eye.y, eye.z, x, y, z);
            }
            if (surround != null) {
                if (good) {
                    surround.updatePos(goodPos.getX() + 0.5, goodPos.getY(), goodPos.getZ() + 0.5);
                    surround.draw = true;
                } else {
                    surround.draw = false;
                }
            }
        }

        @Listener(channel = ChargingContext.MSG_EFFECT_END, side = LogicalSide.CLIENT)
        private void c_endEffects(boolean item) {
            cleanup();
        }

        @Listener(channel = MSG_TERMINATED, side = LogicalSide.CLIENT)
        private void c_terminated() {
            cleanup();
        }

        private void cleanup() {
            if (arc != null) { arc.discard(); arc = null; }
            if (surround != null) { surround.discard(); surround = null; }
            if (sound != null) { sound.requestStop(); sound = null; }
        }
    }
}
