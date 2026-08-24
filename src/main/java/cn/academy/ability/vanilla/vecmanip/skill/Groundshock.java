package cn.academy.ability.vanilla.vecmanip.skill;

import cn.academy.ACParticles;
import cn.academy.ACSounds;
import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientContext;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.ContextManager;
import cn.academy.ability.context.DelegateState;
import cn.academy.ability.context.IConsumptionProvider;
import cn.academy.ability.context.IStateProvider;
import cn.academy.ability.context.RegClientContext;
import cn.academy.ability.vanilla.util.ClientTicker;
import cn.academy.config.AbilityConfig;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.util.RandUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.LogicalSide;

import java.util.LinkedHashMap;
import java.util.Map;

import static cn.lambdalib2.util.MathUtils.lerpf;

public class Groundshock extends Skill {

    public static final Groundshock INSTANCE = new Groundshock();

    public Groundshock() {
        super("ground_shock", 1);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void activate(ClientRuntime rt, int keyID) {
        activateSingleKey2(rt, keyID, GroundshockContext::new);
    }

    public static class GroundshockContext extends Context<Groundshock>
            implements IConsumptionProvider, IStateProvider {

        static final String MSG_PERFORM = "perform";

        private static GroundshockContext pendingAuto;

        private static final int MIN_TICKS = 5;

        private static final double GROUND_BREAK_PROB = 0.3;

        private final float exp = ctx.getSkillExp();
        private final double initEnergy = lerpf(60, 120, exp);
        private final float damage = AbilityConfig.stat("ground_shock", "damage", exp);
        private final float consumption = AbilityConfig.cp("ground_shock", exp);
        private final float overload = AbilityConfig.overload("ground_shock", exp);
        private final int maxIter = (int) lerpf(10, 25, exp);
        private final float dropRate = lerpf(0.3f, 1.0f, exp);

        private final float ySpeed = RandUtils.rangef(0.6f, 0.9f) * lerpf(0.8f, 1.3f, exp);

        private int localTick = 0;
        private double energy;

        private Boolean autoMode;

        private boolean autoSent = false;

        private int groundedTicks = 0;

        public GroundshockContext(Player player) {
            super(player, INSTANCE);
        }

        private boolean autoMode() {
            if (autoMode == null) {
                autoMode = ContextManager.instance
                        .findLocal(cn.academy.ability.vanilla.vecmanip.skill.StormWing.StormWingContext.class)
                        .isPresent();
            }
            return autoMode;
        }

        @Override
        public float getConsumptionHint() {
            return consumption;
        }

        @Override
        public DelegateState getState() {
            return localTick < MIN_TICKS ? DelegateState.CHARGE : DelegateState.ACTIVE;
        }

        @Listener(channel = MSG_KEYDOWN, side = LogicalSide.CLIENT)
        private void l_keyDown() {
            if (!isLocal()) return;
            if (pendingAuto != null && pendingAuto != this
                    && pendingAuto.getStatus() != Status.TERMINATED) {
                pendingAuto.terminate();
                pendingAuto = null;
                player.displayClientMessage(
                        Component.translatable("gui.academy.ground_shock.auto_cancel"), true);
                terminate();
            }
        }

        @Listener(channel = MSG_TICK, side = LogicalSide.CLIENT)
        private void l_tick() {
            if (!isLocal()) return;
            localTick++;

            if (autoMode()) {
                if (player.onGround()) {
                    groundedTicks++;
                } else {
                    groundedTicks = 0;
                }
                if (!autoSent && groundedTicks >= 2 && localTick >= MIN_TICKS) {
                    autoSent = true;
                    sendToServer(MSG_PERFORM);
                }
                return;
            }

            float env;
            if (localTick < 4) env = localTick / 4.0f;
            else if (localTick <= 20) env = 1.0f;
            else if (localTick <= 25) env = 1.0f - (localTick - 20) / 5.0f;
            else env = 0.0f;

            player.setXRot(player.getXRot() - env * 0.2f);
        }

        @Listener(channel = MSG_KEYUP, side = LogicalSide.CLIENT)
        private void l_keyUp() {

            if (autoMode()) {
                pendingAuto = this;
                player.displayClientMessage(
                        Component.translatable("gui.academy.ground_shock.auto_armed"), true);
                return;
            }
            if (localTick >= MIN_TICKS) {
                sendToServer(MSG_PERFORM);
            } else {
                player.displayClientMessage(Component.translatable("gui.academy.ground_shock.too_short"), true);
                terminate();
            }
        }

        @Listener(channel = MSG_TERMINATED, side = LogicalSide.CLIENT)
        private void l_terminated() {
            if (pendingAuto == this) {
                pendingAuto = null;
            }
        }

        @Listener(channel = MSG_KEYABORT, side = LogicalSide.CLIENT)
        private void l_keyAbort() {
            terminate();
        }

        @Listener(channel = MSG_PERFORM, side = LogicalSide.SERVER)
        private void s_perform() {
            if (player.onGround() && ctx.consume(overload, consumption)) {
                shockwave();
                ctx.addSkillExp(0.001f);
                ctx.setCooldown((int) AbilityConfig.cooldown("ground_shock", exp));
                sendToClient(MSG_PERFORM);
            } else if (!player.onGround()) {
                player.displayClientMessage(Component.translatable("gui.academy.ground_shock.not_grounded"), true);
            }
            terminate();
        }

        private void shockwave() {
            ServerLevel level = (ServerLevel) player.level();

            boolean standingOnHeaved = GroundHeave.isHeaved(level, player.blockPosition().below());
            BlockPos origin = player.blockPosition().below();
            double maxRadius = maxIter;

            energy = initEnergy;

            java.util.List<BlockPos> heavePos = new java.util.ArrayList<>();
            java.util.List<Double> heaveHeight = new java.util.ArrayList<>();

            Map<BlockPos, BlockState> seenBlocks = new LinkedHashMap<>();

            double reached = 0;

            for (int[] off : ringOffsets((int) Math.ceil(maxRadius))) {
                if (energy <= 0) {
                    break;
                }
                double d = Math.sqrt(off[0] * off[0] + off[1] * off[1]);
                if (d > maxRadius) {
                    break;
                }

                double edge = d / maxRadius;
                if (edge > 0.75 && RandUtils.nextDouble() >= 1.0 - 2.0 * (edge - 0.75)) {
                    continue;
                }

                BlockPos pos = null;
                BlockState state = null;
                for (int dy : new int[]{0, -1, 1}) {
                    BlockPos p = origin.offset(off[0], dy, off[1]);
                    BlockState s = level.getBlockState(p);
                    if (!s.isAir()) {
                        pos = p;
                        state = s;
                        break;
                    }
                }
                if (pos == null || seenBlocks.containsKey(pos)) {
                    continue;
                }
                seenBlocks.put(pos, state);
                reached = d;

                if (GroundHeave.isHeaved(level, pos)) {
                    convertGround(level, pos, state);
                    if (RandUtils.nextDouble() < GROUND_BREAK_PROB) {
                        breakWithForce(level, pos, false);
                    }
                    GroundHeave.clear(level, pos);

                    for (int k = 1; k <= 3; k++) {
                        breakWithForce(level, pos.above(k), false);
                    }
                } else {
                    energy -= groundCost(state);
                    GroundHeave.add(level, pos);
                    heavePos.add(pos);
                    heaveHeight.add(GroundHeave.heightAt(d, maxRadius));
                }
            }

            double hitR = Math.max(1.0, reached) + 0.5;
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class,
                    new AABB(origin).inflate(hitR, 3.0, hitR),
                    e -> e != player && e.isAlive()
                            && ctx.canTarget(e))) {
                if (e.distanceToSqr(player.getX(), e.getY(), player.getZ()) > hitR * hitR) {
                    continue;
                }
                ctx.attack(e, damage);
                e.setDeltaMovement(e.getDeltaMovement().x, ySpeed, e.getDeltaMovement().z);
                e.hurtMarked = true;
                ctx.addSkillExp(0.002f);
            }

            if (exp >= 1.0f && standingOnHeaved) {
                energy = Double.MAX_VALUE;
                int x0 = (int) player.getX(), y0 = (int) player.getY(), z0 = (int) player.getZ();
                for (int x = x0 - 5; x < x0 + 5; x++) {
                    for (int y = y0 - 1; y < y0 + 1; y++) {
                        for (int z = z0 - 5; z < z0 + 5; z++) {
                            BlockPos pos = new BlockPos(x, y, z);
                            BlockState st = level.getBlockState(pos);
                            if (st.isAir()) continue;
                            if (st.getDestroySpeed(level, pos) <= 0.6f) {
                                breakWithForce(level, pos, true);
                            }
                        }
                    }
                }
            }

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ACSounds.VM_VEC_EXPLOSION.get(), SoundSource.AMBIENT, 2.0f, 1.0f);
            spawnDust(level, seenBlocks);

            cn.academy.network.GroundHeaveMessage.broadcast(
                    level, player.position(), heavePos, heaveHeight);
        }

        private static final Map<Integer, int[][]> RING_CACHE = new java.util.HashMap<>();

        private static int[][] ringOffsets(int r) {
            return RING_CACHE.computeIfAbsent(r, radius -> {
                java.util.List<int[]> out = new java.util.ArrayList<>();
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (dx * dx + dz * dz <= radius * radius) {
                            out.add(new int[]{dx, dz});
                        }
                    }
                }
                out.sort(java.util.Comparator.comparingInt(o -> o[0] * o[0] + o[1] * o[1]));
                return out.toArray(new int[0][]);
            });
        }

        private static double groundCost(BlockState state) {
            Block block = state.getBlock();
            if (block == Blocks.STONE) return 0.4;
            if (block == Blocks.GRASS_BLOCK) return 0.2;
            if (block == Blocks.FARMLAND) return 0.1;
            return 0.5;
        }

        private void convertGround(ServerLevel level, BlockPos pos, BlockState state) {
            energy -= groundCost(state);
            if (!ctx.canBreakBlock(level, pos)) return;
            Block block = state.getBlock();
            if (block == Blocks.STONE) {
                level.setBlockAndUpdate(pos, Blocks.COBBLESTONE.defaultBlockState());
            } else if (block == Blocks.GRASS_BLOCK) {
                level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
            }
        }

        private void breakWithForce(ServerLevel level, BlockPos pos, boolean drop) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) return;
            if (!ctx.canBreakBlock(level, pos)) return;

            float hardness = state.getDestroySpeed(level, pos);
            if (hardness < 0) return;
            if (energy < hardness) return;
            if (state.is(Blocks.FARMLAND) || !state.getFluidState().isEmpty()) return;

            energy -= hardness;
            if (drop && RandUtils.nextFloat() < dropRate) {
                Block.dropResources(state, level, pos);
            }
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);

        }

        private void spawnDust(ServerLevel level, Map<BlockPos, BlockState> blocks) {
            for (Map.Entry<BlockPos, BlockState> en : blocks.entrySet()) {
                BlockPos pos = en.getKey();
                BlockState state = en.getValue();

                int count = RandUtils.rangei(4, 8);
                for (int i = 0; i < count; i++) {
                    level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                            pos.getX() + RandUtils.nextDouble(),
                            pos.getY() + 1 + RandUtils.nextDouble() * 0.5 + 0.2,
                            pos.getZ() + RandUtils.nextDouble(),
                            0, RandUtils.ranged(-0.2, 0.2), 0.1 + RandUtils.nextDouble() * 0.2,
                            RandUtils.ranged(-0.2, 0.2), 1.0);
                }

                if (RandUtils.nextFloat() < 0.5f) {
                    level.sendParticles(ACParticles.SMOKE.get(),
                            pos.getX() + 0.5 + RandUtils.ranged(-.3, .3),
                            pos.getY() + 1 + RandUtils.ranged(0, 0.2),
                            pos.getZ() + 0.5 + RandUtils.ranged(-.3, .3),
                            0, RandUtils.ranged(-.03, .03), RandUtils.ranged(.03, .06),
                            RandUtils.ranged(-.03, .03), 1.0);
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    @RegClientContext(GroundshockContext.class)
    public static class GroundshockContextC extends ClientContext {

        public GroundshockContextC(GroundshockContext par) {
            super(par);
        }

        @Listener(channel = GroundshockContext.MSG_PERFORM, side = LogicalSide.CLIENT)
        private void c_perform() {
            if (!isLocal()) return;
            Player p = player;
            ClientTicker.run(4, () -> p.setXRot(p.getXRot() + 3.4f));
        }
    }
}
