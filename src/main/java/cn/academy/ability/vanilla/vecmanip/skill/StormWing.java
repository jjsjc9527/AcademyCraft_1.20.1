package cn.academy.ability.vanilla.vecmanip.skill;

import cn.academy.ACSounds;
import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientContext;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.ContextManager;
import cn.academy.ability.context.DelegateState;
import cn.academy.ability.context.KeyDelegate;
import cn.academy.ability.context.RegClientContext;
import cn.academy.client.render.entity.ACEffectEntities;
import cn.academy.client.sound.FollowEntitySound;
import cn.academy.config.AbilityConfig;
import cn.academy.entity.EntityStormWing;

import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.util.RandUtils;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.LogicalSide;

import java.util.Optional;

public class StormWing extends Skill {

    public static final StormWing INSTANCE = new StormWing();

    public StormWing() {
        super("storm_wing", 3);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void activate(ClientRuntime rt, int keyID) {
        rt.addKey(keyID, new ToggleDelegate());
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ToggleDelegate extends KeyDelegate {

        @Override
        public void onKeyDown() {
            Optional<StormWingContext> cur = ContextManager.instance.findLocal(StormWingContext.class);
            if (cur.isPresent()) {
                cur.get().terminate();
            } else {
                ContextManager.instance.activate(new StormWingContext(getPlayer()));
            }
        }

        @Override
        public DelegateState getState() {
            Optional<StormWingContext> cur = ContextManager.instance.findLocal(StormWingContext.class);
            if (cur.isEmpty()) {
                return DelegateState.IDLE;
            }
            return cur.get().getState() == StormWingContext.STATE_ACTIVE
                    ? DelegateState.ACTIVE : DelegateState.CHARGE;
        }

        @Override
        public ResourceLocation getIcon() {
            return INSTANCE.getHintIcon();
        }

        @Override
        public int createID() {
            return 0;
        }

        @Override
        public Skill getSkill() {
            return INSTANCE;
        }
    }

    public static class StormWingContext extends Context<StormWing> {

        static final String MSG_SYNC_STATE = "sync_state";

        static final String MSG_UPDSTATE = "upd_state";

        static final String KEY_GROUP = "vm_storm_wing";

        public static final int STATE_CHARGE = 0, STATE_ACTIVE = 1;

        private static final double ACCEL = 0.16;

        private static final float BREAK_BLOCK_EXP = 0.15f;

        private static final int BREAK_TRIES = 40, BREAK_AREA = 10;
        private static final float BREAK_MAX_HARDNESS = 0.3f;

        private static final double BLAST_RANGE = 6;

        private static final float EXP_INCR = 0.00005f;

        private static final double HOVER_RISE = 0.078;

        private static final double HOVER_LIFTOFF = 0.1;

        private static final float SPEED_GATE_EXP = 0.45f;
        private static final double SPEED_LOW = 0.7, SPEED_HIGH = 1.2;

        private static final int PRONE_DELAY = 3;

        private final float exp = ctx.getSkillExp();
        private final float consumption = AbilityConfig.cp("storm_wing", exp);

        private final float overload = AbilityConfig.overload("storm_wing", exp);
        private final double speed = (exp < SPEED_GATE_EXP ? SPEED_LOW : SPEED_HIGH)
                * AbilityConfig.stat("storm_wing", "speed", exp);
        private final int chargeTime = (int) AbilityConfig.stat("storm_wing", "charge_time", exp);

        private int state = STATE_CHARGE;

        private int stateTick = 0;

        private final boolean[] dirHeld = new boolean[4];

        private boolean applying = false;

        private int proneWantTicks = 0;

        private boolean prevAllowFlying;

        private boolean proneForced = false;

        public StormWingContext(Player player) {
            super(player, INSTANCE);
        }

        public int getState() {
            return state;
        }

        public int getStateTick() {
            return stateTick;
        }

        public int chargeTime() {
            return chargeTime;
        }

        @Listener(channel = MSG_MADEALIVE, side = LogicalSide.SERVER)
        private void s_makeAlive() {
            Abilities ab = player.getAbilities();
            prevAllowFlying = ab.mayfly;
            ab.mayfly = true;
            player.onUpdateAbilities();
        }

        @Listener(channel = MSG_UPDSTATE, side = LogicalSide.SERVER)
        private void s_updState(boolean flying) {
            setProne(flying);
            sendToClient(MSG_UPDSTATE, flying);
        }

        @Listener(channel = MSG_UPDSTATE, side = LogicalSide.CLIENT)
        private void c_updState(boolean flying) {
            if (isLocal()) {
                return;
            }
            setProne(flying);
        }

        private void setProne(boolean prone) {
            if (prone) {
                proneForced = true;
                player.setForcedPose(net.minecraft.world.entity.Pose.SWIMMING);

                cn.academy.util.ACPose.set(player, cn.academy.util.ACPose.Lean.FOLLOW_PITCH);
            } else if (proneForced) {
                proneForced = false;
                player.setForcedPose(null);
                cn.academy.util.ACPose.clear(player);
            }
        }

        @Listener(channel = MSG_TERMINATED, side = LogicalSide.CLIENT)
        private void l_terminated() {
            setProne(false);
        }

        @Listener(channel = MSG_TERMINATED, side = LogicalSide.SERVER)
        private void s_terminated() {
            setProne(false);
            Abilities ab = player.getAbilities();
            ab.mayfly = prevAllowFlying;
            if (!prevAllowFlying) {

                ab.flying = false;
            }
            player.onUpdateAbilities();
            ctx.setCooldown((int) AbilityConfig.cooldown("storm_wing", exp));
        }

        @Listener(channel = MSG_TICK, side = LogicalSide.SERVER)
        private void s_tick() {
            player.fallDistance = 0;

            if (!ctx.cpData.canUseAbility()) {
                terminate();
                return;
            }

            if (exp < BREAK_BLOCK_EXP) {
                scratchBlocks();
            }

            if (!doConsume()) {
                terminate();
            }
        }

        private void scratchBlocks() {
            Level level = player.level();
            for (int i = 0; i < BREAK_TRIES; i++) {

                BlockPos pos = new BlockPos(
                        Mth.floor(player.getX() + RandUtils.ranged(-BREAK_AREA, BREAK_AREA)),
                        Mth.floor(player.getY() + RandUtils.ranged(-BREAK_AREA, BREAK_AREA)),
                        Mth.floor(player.getZ() + RandUtils.ranged(-BREAK_AREA, BREAK_AREA)));
                BlockState st = level.getBlockState(pos);
                if (st.isAir()) {
                    continue;
                }
                float hardness = st.getDestroySpeed(level, pos);
                if (hardness < 0 || hardness > BREAK_MAX_HARDNESS) {
                    continue;
                }
                if (!ctx.canBreakBlock(level, pos)) {
                    continue;
                }
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        st.getSoundType().getBreakSound(), SoundSource.BLOCKS, 0.5f, 1.0f);
            }
        }

        private boolean doConsume() {
            if (state != STATE_ACTIVE) {
                return true;
            }
            ctx.addSkillExp(EXP_INCR);
            if (consumption <= 0 && overload <= 0) {
                return true;
            }
            return ctx.consume(overload, consumption);
        }

        @Listener(channel = MSG_TICK, side = LogicalSide.CLIENT)
        private void c_tick() {
            stateTick++;
            if (!isLocal()) {
                return;
            }

            Vec3 dir = moveDir();
            boolean grounded = nearGround();
            if (dir != null) {
                Vec3 expected = dir.scale(speed);
                if (player.isPassenger()) {
                    player.stopRiding();
                }
                Vec3 mo = player.getDeltaMovement();
                player.setDeltaMovement(
                        approach(mo.x, expected.x),
                        approach(mo.y, expected.y),
                        approach(mo.z, expected.z));
            } else {

                Vec3 mo = player.getDeltaMovement();
                if (grounded) {
                    player.setDeltaMovement(mo.x, HOVER_LIFTOFF, mo.z);
                } else {
                    player.setDeltaMovement(mo.x, mo.y + HOVER_RISE, mo.z);
                }
            }

            boolean wantProne = dir != null && dirHeld[0] && !grounded;
            if (wantProne) {
                if (proneWantTicks < PRONE_DELAY) {
                    proneWantTicks++;
                }
            } else {
                proneWantTicks = 0;
            }
            boolean prone = proneWantTicks >= PRONE_DELAY;
            if (prone != applying) {
                applying = prone;
                setProne(prone);
                sendToServer(MSG_UPDSTATE, prone);
            }

            player.fallDistance = 0;
            doConsume();

            if (state == STATE_CHARGE && stateTick > chargeTime) {
                state = STATE_ACTIVE;
                stateTick = 0;
                initKeys();
                sendToServer(MSG_SYNC_STATE);
            }
        }

        private static double approach(double from, double to) {
            double delta = to - from;
            return from + Math.min(Math.abs(delta), ACCEL) * Math.signum(delta);
        }

        private Vec3 moveDir() {
            double x = 0, z = 0;
            if (dirHeld[0]) z += 1;
            if (dirHeld[1]) z -= 1;
            if (dirHeld[2]) x += 1;
            if (dirHeld[3]) x -= 1;
            if (x == 0 && z == 0) {
                return null;
            }
            Vec3 base = new Vec3(x, 0, z).normalize();
            float yaw = (float) Math.toRadians(player.getYHeadRot());
            float pitch = (float) Math.toRadians(player.getXRot());
            return base.xRot(-pitch).yRot(-yaw);
        }

        private boolean nearGround() {
            Vec3 from = player.position().add(0, 0.5, 0);
            Vec3 to = player.position().add(0, -0.3, 0);
            return player.level().clip(new ClipContext(from, to,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player))
                    .getType() != HitResult.Type.MISS;
        }

        @OnlyIn(Dist.CLIENT)
        private void initKeys() {

            if (!ClientRuntime.available()) return;
            ClientRuntime rt = ClientRuntime.instance();
            Minecraft mc = Minecraft.getInstance();
            defKey(rt, 1, mc.options.keyUp, 0);
            defKey(rt, 2, mc.options.keyDown, 1);
            defKey(rt, 3, mc.options.keyLeft, 2);
            defKey(rt, 4, mc.options.keyRight, 3);
        }

        @OnlyIn(Dist.CLIENT)
        private void defKey(ClientRuntime rt, int id, KeyMapping mapping, int dir) {
            InputConstants.Key key = mapping.getKey();
            if (key.getType() != InputConstants.Type.KEYSYM) {
                return;
            }
            int code = key.getValue();
            rt.addKey(KEY_GROUP, code, new KeyDelegate() {

                @Override
                public void onKeyDown() {
                    dirHeld[dir] = true;
                }

                @Override
                public void onKeyUp() {
                    dirHeld[dir] = false;
                }

                @Override
                public void onKeyAbort() {
                    onKeyUp();
                }

                @Override
                public DelegateState getState() {
                    return dirHeld[dir] ? DelegateState.ACTIVE : DelegateState.IDLE;
                }

                @Override
                public ResourceLocation getIcon() {
                    return INSTANCE.getHintIcon();
                }

                @Override
                public int createID() {
                    return id;
                }

                @Override
                public Skill getSkill() {
                    return INSTANCE;
                }
            });
        }

        @Listener(channel = MSG_SYNC_STATE, side = {LogicalSide.CLIENT, LogicalSide.SERVER})
        private void syncState() {
            state = STATE_ACTIVE;
            stateTick = 0;

            if (!isRemote()) {
                if (exp >= 1.0f) {
                    blastNearby();
                }
                sendToExceptLocal(MSG_SYNC_STATE);
            }
        }

        private void blastNearby() {
            Vec3 center = player.position();
            AABB box = player.getBoundingBox().inflate(BLAST_RANGE);
            for (Entity e : player.level().getEntities(player, box)) {
                if (e.position().distanceToSqr(center) > BLAST_RANGE * BLAST_RANGE) {
                    continue;
                }
                Vec3 delta = e.getEyePosition(1.0f).subtract(center).scale(RandUtils.ranged(0.9, 1.2));
                if (delta.lengthSqr() < 1.0e-6) {
                    continue;
                }
                e.setDeltaMovement(delta.normalize().scale(RandUtils.ranged(0.5, 1.0)));
                e.hurtMarked = true;
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    @RegClientContext(StormWingContext.class)
    public static class StormWingContextC extends ClientContext {

        private final StormWingContext par;
        private ClientRuntime.IActivateHandler activateHandler;
        private FollowEntitySound loopSound;
        private EntityStormWing effect;

        public StormWingContextC(StormWingContext par) {
            super(par);
            this.par = par;
        }

        @Listener(channel = MSG_MADEALIVE, side = LogicalSide.CLIENT)
        private void c_makeAlive() {
            effect = new EntityStormWing(player, par);
            ACEffectEntities.spawn(effect);

            loopSound = new FollowEntitySound(ACSounds.VM_STORM_WING.get(), player, 1.0f);
            Minecraft.getInstance().getSoundManager().play(loopSound);
        }

        @Listener(channel = MSG_TICK, side = LogicalSide.CLIENT)
        private void c_tick() {
            healEffectIfLost();
            if (effect != null) {
                effect.touch();
            }
        }

        private static final double LOST_DIST_SQR = 64.0 * 64.0;

        private int healCooldown;

        private void healEffectIfLost() {
            if (effect == null) {
                return;
            }
            if (--healCooldown > 0) {
                return;
            }
            healCooldown = 20;
            if (effect.isRemoved() || effect.distanceToSqr(player) > LOST_DIST_SQR) {
                effect.discard();
                effect = new EntityStormWing(player, par);
                ACEffectEntities.spawn(effect);
            }
        }

        @Listener(channel = MSG_MADEALIVE, side = LogicalSide.CLIENT)
        private void l_makeAlive() {
            if (!isLocal()) {
                return;
            }
            activateHandler = new ClientRuntime.IActivateHandler() {
                @Override
                public boolean handles(Player p) {
                    return par.getStatus() == Context.Status.ALIVE;
                }

                @Override
                public void onKeyDown(Player p) {
                    par.terminate();
                }

                @Override
                public String getHint() {
                    return ClientRuntime.IActivateHandler.ENDSPECIAL;
                }
            };
            ClientRuntime.instance().addActivateHandler(activateHandler);
        }

        @Listener(channel = MSG_TERMINATED, side = LogicalSide.CLIENT)
        private void c_terminated() {
            if (loopSound != null) {
                loopSound.requestStop();
                loopSound = null;
            }

            effect = null;

            if (!isLocal()) {
                return;
            }

            if (!ClientRuntime.available()) return;
            ClientRuntime rt = ClientRuntime.instance();
            rt.clearKeys(StormWingContext.KEY_GROUP);
            if (activateHandler != null) {
                rt.removeActiveHandler(activateHandler);
                activateHandler = null;
            }
        }
    }
}
