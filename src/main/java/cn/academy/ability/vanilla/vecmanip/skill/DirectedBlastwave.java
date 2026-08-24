package cn.academy.ability.vanilla.vecmanip.skill;

import cn.academy.ability.AbilityPipeline;
import cn.academy.util.AimTrace;
import cn.academy.ACSounds;
import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientContext;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.IConsumptionProvider;
import cn.academy.ability.context.RegClientContext;
import cn.academy.client.render.entity.ACEffectEntities;
import cn.academy.client.render.util.AnimPresets;
import cn.academy.client.render.util.HandAnim;
import cn.academy.client.render.util.HandRenderOverride;
import cn.academy.config.AbilityConfig;
import cn.academy.entity.EntityWave;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.util.GameTimer;
import cn.lambdalib2.util.RandUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.LogicalSide;

import static cn.lambdalib2.util.MathUtils.lerpf;

public class DirectedBlastwave extends Skill {

    public static final DirectedBlastwave INSTANCE = new DirectedBlastwave();

    static final double RANGE = 4;

    static final double BLAST_RADIUS = 3;

    public DirectedBlastwave() {
        super("dir_blast", 3);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void activate(ClientRuntime rt, int keyID) {
        activateSingleKey2(rt, keyID, BlastwaveContext::new);
    }

    public static class BlastwaveContext extends Context<DirectedBlastwave> implements IConsumptionProvider {

        static final String MSG_PERFORM = "perform";

        private static final int MIN_TICKS = 6;
        private static final int MAX_ACCEPTED_TICKS = 50;
        private static final int MAX_TOLERANT_TICKS = 200;

        private final float exp = ctx.getSkillExp();
        private final float damage = AbilityConfig.stat("dir_blast", "damage", exp);
        private final float consumption = AbilityConfig.cp("dir_blast", exp);
        private final float overload = AbilityConfig.overload("dir_blast", exp);
        private final float breakProb = lerpf(0.5f, 0.8f, exp);
        private final float dropRate = lerpf(0.4f, 0.9f, exp);

        private final float breakHardness = exp < 0.25f ? 2.9f : (exp < 0.5f ? 25f : 55f);

        private int ticker = 0;

        public BlastwaveContext(Player player) {
            super(player, INSTANCE);
        }

        @Override
        public float getConsumptionHint() {
            return consumption;
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
            if (!ctx.consume(overload, consumption)) {
                terminate();
                return;
            }

            Vec3 position = aimPos();
            ctx.setCooldown((int) AbilityConfig.cooldown("dir_blast", exp));
            sendToClient(MSG_PERFORM, position);

            boolean effective = blastEntities(position);
            breakBlocks((ServerLevel) player.level(), position);

            ctx.addSkillExp(effective ? 0.0025f : 0.0012f);
            terminate();
        }

        private Vec3 aimPos() {
            Vec3 eye = player.getEyePosition(1.0f);
            Vec3 look = player.getViewVector(1.0f);
            Vec3 rayEnd = eye.add(look.scale(RANGE));

            BlockHitResult block = player.level().clip(new ClipContext(
                    eye, rayEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            Vec3 clipEnd = block.getType() == HitResult.Type.BLOCK ? block.getLocation() : rayEnd;

            EntityHitResult hit = AimTrace.firstResult(player.level(), player, eye, clipEnd,
                    e -> e != player && e.isAlive() && e instanceof LivingEntity
                            && AbilityPipeline.canTarget(player, e));

            if (hit != null) {
                return hit.getEntity().getEyePosition(1.0f);
            }
            if (block.getType() == HitResult.Type.BLOCK) {
                BlockPos bp = block.getBlockPos();
                return new Vec3(bp.getX(), bp.getY(), bp.getZ());
            }
            return player.position().add(look.scale(RANGE));
        }

        private boolean blastEntities(Vec3 position) {
            AABB box = new AABB(position.x - BLAST_RADIUS, position.y - BLAST_RADIUS, position.z - BLAST_RADIUS,
                    position.x + BLAST_RADIUS, position.y + BLAST_RADIUS, position.z + BLAST_RADIUS);
            boolean effective = false;

            for (Entity e : player.level().getEntitiesOfClass(Entity.class, box,
                    e -> e != player && e.isAlive()
                            && ctx.canTarget(e))) {

                if (e.position().distanceToSqr(position) > BLAST_RADIUS * BLAST_RADIUS) continue;

                if (e instanceof LivingEntity) {
                    ctx.attack(e, damage);
                    effective = true;
                }
                knockback(e);
            }
            return effective;
        }

        private void knockback(Entity target) {
            Vec3 delta = player.getEyePosition(1.0f).subtract(target.getEyePosition(1.0f)).normalize();
            delta = new Vec3(delta.x, delta.y - 0.4, delta.z).normalize();

            target.setPos(target.getX(), target.getY() + 0.1, target.getZ());
            Vec3 motion = delta.scale(-1.2);

            Vec3 push = target.position().subtract(player.position()).normalize().scale(0.24);
            target.setDeltaMovement(motion.add(push));
            target.hurtMarked = true;
        }

        private void breakBlocks(ServerLevel level, Vec3 position) {
            int x0 = (int) Math.round(position.x), y0 = (int) Math.round(position.y), z0 = (int) Math.round(position.z);

            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = -2; dy <= 2; dy++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        int distSq = dx * dx + dy * dy + dz * dz;
                        if (distSq > 6) continue;
                        if (distSq != 0 && RandUtils.nextFloat() >= breakProb) continue;

                        BlockPos pos = new BlockPos(x0 + dx, y0 + dy, z0 + dz);
                        BlockState state = level.getBlockState(pos);
                        if (state.isAir()) continue;

                        float hardness = state.getDestroySpeed(level, pos);
                        if (hardness < 0 || hardness > breakHardness) continue;
                        if (!ctx.canBreakBlock(level, pos)) continue;

                        float p = exp >= 1.0f ? 1.0f : dropRate;
                        if (RandUtils.nextFloat() < p) {
                            Block.dropResources(state, level, pos);
                        }
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    @RegClientContext(BlastwaveContext.class)
    public static class BlastwaveContextC extends ClientContext {

        private static final double PREPARE_SCALE = 0.15, PREPARE_MAX_T = 2.0;
        private static final double PUNCH_SCALE = 0.3;

        private HandFx prepareFx;

        public BlastwaveContextC(BlastwaveContext par) {
            super(par);
        }

        @Listener(channel = MSG_MADEALIVE, side = LogicalSide.CLIENT)
        private void l_handEffectStart() {
            if (!isLocal()) return;
            prepareFx = new HandFx(AnimPresets.createPrepareAnim(), PREPARE_SCALE, PREPARE_MAX_T, false);
            HandRenderOverride.addInterrupt(prepareFx);
        }

        @Listener(channel = BlastwaveContext.MSG_PERFORM, side = LogicalSide.CLIENT)
        private void c_perform(Vec3 pos) {
            player.level().playLocalSound(pos.x, pos.y, pos.z,
                    ACSounds.VM_DIRECTED_BLAST.get(), SoundSource.AMBIENT, 0.5f, 1.0f, false);

            EntityWave wave = new EntityWave(player.level(), RandUtils.rangei(2, 3), 1.0);
            Vec3 head = player.getEyePosition(1.0f);
            wave.setPos(head.x + (pos.x - head.x) * 0.7,
                    head.y + (pos.y - head.y) * 0.7,
                    head.z + (pos.z - head.z) * 0.7);
            wave.yaw = player.getYHeadRot() + RandUtils.rangef(-20, 20);
            wave.pitch = player.getXRot() + RandUtils.rangef(-10, 10);
            ACEffectEntities.spawn(wave);

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
