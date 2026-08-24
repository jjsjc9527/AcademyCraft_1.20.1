package cn.academy.ability.vanilla.teleporter.skill;

import cn.academy.util.AimTrace;
import cn.academy.ACSounds;
import cn.academy.Resources;
import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientContext;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.ContextManager;
import cn.academy.ability.context.KeyDelegate;
import cn.academy.ability.context.RegClientContext;
import cn.academy.ability.vanilla.teleporter.skill.PenetrateTeleport.Dest;
import cn.academy.ability.vanilla.teleporter.util.GravityCancellor;
import cn.academy.client.render.entity.ACEffectEntities;
import cn.academy.config.AbilityConfig;
import cn.academy.entity.EntityTPMarking;
import cn.academy.event.ability.FlushControlEvent;
import cn.academy.gravity.ACGravity;
import cn.academy.gravity.RotationUtil;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.LogicalSide;

import java.util.Optional;

public class Flashing extends Skill {

    public static final Flashing INSTANCE = new Flashing();

    static final String MSG_PERFORM = "perform";

    static final String KEY_GROUP = "TP_Flashing";

    static final String[] KEY_ICONS = {null, "a", "d", "w", "s"};

    public Flashing() {
        super("flashing", 5);
    }

    @Override
    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    public void activate(ClientRuntime rt, int keyID) {
        rt.addKey(keyID, new KeyDelegate() {
            @Override
            public void onKeyDown() {

                Optional<MainContext> opt = ContextManager.instance.findLocal(MainContext.class);
                if (opt.isPresent()) {
                    opt.get().terminate();
                } else {
                    ContextManager.instance.activate(new MainContext(getPlayer()));
                }
                MinecraftForge.EVENT_BUS.post(new FlushControlEvent());
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
        });
    }

    public static class MainContext extends Context<Flashing> {

        final float exp = ctx.getSkillExp();

        final float consumption = AbilityConfig.cp("flashing", exp);

        final float overloadStart = AbilityConfig.overload("flashing", exp);
        final float cpStart = AbilityConfig.stat("flashing", "cp_start", exp);
        final int cooldownTime = (int) AbilityConfig.cooldown("flashing", exp);
        final double flashDist = AbilityConfig.stat("flashing", "distance", exp);

        private float overloadKeep;

        public MainContext(Player player) {
            super(player, INSTANCE);
        }

        @Listener(channel = MSG_MADEALIVE, side = LogicalSide.SERVER)
        private void s_madeAlive() {
            if (!ctx.consume(overloadStart, cpStart)) {
                terminate();
            } else {
                overloadKeep = ctx.cpData.getOverload();
            }
        }

        @Listener(channel = MSG_TICK, side = LogicalSide.SERVER)
        private void s_tick() {
            if (ctx.cpData.getOverload() < overloadKeep) {
                ctx.cpData.setOverload(overloadKeep);
            }
            if (!ctx.cpData.canUseAbility() || !ctx.canConsumeCP(consumption)) {
                terminate();
            }
        }

        @Listener(channel = MSG_PERFORM, side = LogicalSide.SERVER)
        private void s_perform(int keyid) {
            if (keyid < 1 || keyid > 4) {
                return;
            }
            Dest dest = getDest(keyid);
            if (!dest.available || !ctx.consume(0, consumption)) {
                return;
            }
            if (player.isPassenger()) {
                player.stopRiding();
            }

            ((ServerPlayer) player).connection.teleport(
                    dest.pos.x, dest.pos.y, dest.pos.z, player.getYRot(), player.getXRot());
            player.fallDistance = 0.0f;
            ctx.addSkillExp(0.002f);

            player.level().playSound(player, dest.pos.x, dest.pos.y, dest.pos.z,
                    ACSounds.TP_MOVE_PLAYER.get(), SoundSource.AMBIENT, 1.0f, 1.0f);
            sendToClient(MSG_PERFORM);
        }

        @Listener(channel = MSG_TERMINATED, side = LogicalSide.SERVER)
        private void s_terminated() {
            ctx.setCooldown(cooldownTime);
        }

        boolean hasPlace(Level world, Vec3 feet) {
            AABB bb = player.getBoundingBox();
            AABB at = bb.move(feet.x - player.getX(), feet.y - player.getY(), feet.z - player.getZ());
            return world.noCollision(player, at.deflate(0.05));
        }

        Vec3 dirOf(int keyid) {
            Vec3 look = player.getLookAngle().normalize();
            Direction g = ACGravity.getGravityDirection(player);
            Vec3 up = RotationUtil.vecPlayerToWorld(new Vec3(0, 1, 0), g);
            Vec3 right = look.cross(up);
            if (right.lengthSqr() < 1.0e-6) {
                right = RotationUtil.vecPlayerToWorld(
                        RotationUtil.rotToVec(player.getYRot(), 0), g).cross(up);
            }
            right = right.normalize();
            switch (keyid) {
                case 1: return right.scale(-1);
                case 2: return right;
                case 3: return look;
                default: return look.scale(-1);
            }
        }

        public Dest getDest(int keyid) {
            Level world = player.level();
            Vec3 eye = player.getEyePosition(1.0f);
            Vec3 dir = dirOf(keyid);
            Vec3 end = eye.add(dir.scale(flashDist));

            BlockHitResult block = world.clip(new ClipContext(
                    eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            Vec3 clipEnd = block.getType() == HitResult.Type.BLOCK ? block.getLocation() : end;

            EntityHitResult ent = AimTrace.firstResult(world, player, eye, clipEnd,
                    e -> e != player && e.isAlive() && e instanceof LivingEntity);

            Vec3 feet = null;
            if (ent != null) {
                Entity t = ent.getEntity();
                feet = new Vec3(t.getX(), t.getBoundingBox().maxY, t.getZ());
            } else if (block.getType() == HitResult.Type.BLOCK) {
                BlockPos hp = block.getBlockPos();
                Vec3 onTop = new Vec3(hp.getX() + 0.5, hp.getY() + 1, hp.getZ() + 0.5);
                if (block.getDirection() == Direction.UP && hasPlace(world, clipEnd)) {
                    feet = clipEnd;
                } else if (hasPlace(world, onTop)) {
                    feet = onTop;
                }
            }
            if (feet == null) {
                double travel = clipEnd.distanceTo(eye);
                if (block.getType() == HitResult.Type.BLOCK) {
                    travel = Math.max(0, travel - 0.35);
                }
                feet = eye.add(dir.scale(travel)).subtract(0, player.getEyeHeight(), 0);
            }

            Vec3 pos = feet;
            int steps = 0;
            while (!hasPlace(world, pos)) {
                if (++steps > 8) {
                    return new Dest(feet, false);
                }
                pos = pos.add(dir.scale(-0.5));
            }
            return new Dest(pos, true);
        }
    }

    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    @RegClientContext(MainContext.class)
    public static class FlashingContextC extends ClientContext {

        private final MainContext par;

        private EntityTPMarking marking;
        private GravityCancellor cancellor;
        private ClientRuntime.IActivateHandler activateHandler;

        private int performingKey = -1;

        public FlashingContextC(MainContext par) {
            super(par);
            this.par = par;
        }

        @Listener(channel = MSG_MADEALIVE, side = LogicalSide.CLIENT)
        private void l_madeAlive() {
            if (!isLocal()) {
                return;
            }

            if (!ClientRuntime.available()) return;
            ClientRuntime rt = ClientRuntime.instance();

            activateHandler = new ClientRuntime.IActivateHandler() {
                @Override
                public boolean handles(Player p) {
                    return par.getStatus() == Context.Status.ALIVE;
                }

                @Override
                public void onKeyDown(Player p) {
                    par.terminate();

                    cn.academy.datapart.CPData.get(p).setActivateState(false,
                            cn.academy.datapart.AbilityToggleSource.SKILL_KEY);
                }

                @Override
                public String getHint() {
                    return "deactivate";
                }
            };
            rt.addActivateHandler(activateHandler);

            net.minecraft.client.Options opts = net.minecraft.client.Minecraft.getInstance().options;
            int[] keys = {
                    -1,
                    opts.keyLeft.getKey().getValue(),
                    opts.keyRight.getKey().getValue(),
                    opts.keyUp.getKey().getValue(),
                    opts.keyDown.getKey().getValue()
            };
            for (int i = 1; i <= 4; i++) {
                final int localid = i;
                rt.addKey(KEY_GROUP, keys[localid], new KeyDelegate() {
                    @Override
                    public void onKeyDown() {
                        localStart(localid);
                    }

                    @Override
                    public void onKeyUp() {
                        localEnd(localid);
                    }

                    @Override
                    public void onKeyAbort() {
                        localAbort(localid);
                    }

                    @Override
                    public ResourceLocation getIcon() {
                        return Resources.getTexture("abilities/teleporter/flashing/" + KEY_ICONS[localid]);
                    }

                    @Override
                    public int createID() {
                        return localid;
                    }

                    @Override
                    public Skill getSkill() {
                        return INSTANCE;
                    }
                });
            }
        }

        private void localStart(int keyid) {
            performingKey = keyid;
            startEffects();
        }

        private void localEnd(int keyid) {
            if (keyid != performingKey) {
                return;
            }
            endEffects();
            if (par.getDest(keyid).available) {
                par.sendToServer(MSG_PERFORM, keyid);
            }
            performingKey = -1;
        }

        private void localAbort(int keyid) {
            if (performingKey == keyid) {
                performingKey = -1;
                endEffects();
            }
        }

        @Listener(channel = MSG_TICK, side = LogicalSide.CLIENT)
        private void l_tick() {
            if (!isLocal()) {
                return;
            }
            if (performingKey != -1 && !ctx.canConsumeCP(par.consumption)) {
                performingKey = -1;
                endEffects();
            } else if (marking != null) {
                Dest dest = par.getDest(performingKey);
                marking.available = dest.available;
                marking.moveTo2(dest.pos.x, dest.pos.y + player.getEyeHeight(), dest.pos.z);
                marking.yaw = player.getYRot();
                marking.touch();
            }
            if (cancellor != null && cancellor.isDead()) {
                cancellor = null;
            }
        }

        @Listener(channel = MSG_PERFORM, side = LogicalSide.CLIENT)
        private void c_perform() {
            if (isLocal()) {
                net.minecraft.client.Minecraft.getInstance().getSoundManager().play(
                        net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                                ACSounds.TP_MOVE_PLAYER.get(), 1.0f, 1.0f));
                if (cancellor != null) {
                    cancellor.setDead();
                }
                cancellor = new GravityCancellor(player, 40);
            }
        }

        @Listener(channel = MSG_TERMINATED, side = LogicalSide.CLIENT)
        private void c_terminated() {
            if (!isLocal()) {
                return;
            }

            if (!ClientRuntime.available()) return;
            ClientRuntime rt = ClientRuntime.instance();
            if (activateHandler != null) {
                rt.removeActiveHandler(activateHandler);
                activateHandler = null;
            }
            rt.clearKeys(KEY_GROUP);
            endEffects();
            if (cancellor != null) {
                cancellor.setDead();
                cancellor = null;
            }
        }

        private void startEffects() {
            endEffects();
            marking = new EntityTPMarking(player.level());
            Dest dest = par.getDest(performingKey);
            marking.available = dest.available;
            marking.moveTo2(dest.pos.x, dest.pos.y + player.getEyeHeight(), dest.pos.z);
            marking.yaw = player.getYRot();

            if (player instanceof net.minecraft.client.player.AbstractClientPlayer acp) {
                marking.skin = acp.getSkinTextureLocation();
                marking.slimArms = "slim".equals(acp.getModelName());
            }
            ACEffectEntities.spawn(marking);
        }

        private void endEffects() {
            if (marking != null) {
                marking.discard();
                marking = null;
            }
        }
    }
}
