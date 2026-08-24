package cn.academy.ability.vanilla.teleporter.skill;

import cn.academy.ACParticles;
import cn.academy.ACSounds;
import cn.academy.ability.AbilityPipeline;
import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientContext;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.RegClientContext;
import cn.academy.client.render.entity.ACEffectEntities;
import cn.academy.config.AbilityConfig;
import cn.academy.entity.EntityTPMarking;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.util.RandUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;

import static cn.lambdalib2.util.MathUtils.lerpf;

public class PenetrateTeleport extends Skill {

    public static final PenetrateTeleport INSTANCE = new PenetrateTeleport();

    public PenetrateTeleport() {
        super("penetrate_teleport", 2);
    }

    @Override
    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    public void activate(cn.academy.ability.context.ClientRuntime rt, int keyID) {
        activateSingleKey2(rt, keyID, PTContext::new);
    }

    static float getMaxDistance(float exp) {
        return lerpf(10, 35, exp);
    }

    public static class PTContext extends Context<PenetrateTeleport> {

        static final String MSG_EXECUTE = "execute";

        final float exp = ctx.getSkillExp();
        final float maxDist = getMaxDistance(exp);

        final float minDist = -getMaxDistance(exp);
        float curDist = maxDist;

        public PTContext(Player player) {
            super(player, INSTANCE);
        }

        @Listener(channel = MSG_KEYABORT, side = LogicalSide.CLIENT)
        private void l_onKeyAbort() {
            terminate();
        }

        @Listener(channel = MSG_KEYUP, side = LogicalSide.CLIENT)
        private void l_onKeyUp() {
            Dest d = getDest();
            if (!d.available) {
                terminate();
                return;
            }
            sendToServer(MSG_EXECUTE, curDist);
        }

        @Listener(channel = MSG_EXECUTE, side = LogicalSide.SERVER)
        private void s_execute(float dist) {
            curDist = Mth.clamp(dist, minDist, maxDist);
            Dest dest = getDest();
            if (!dest.available) {
                terminate();
                return;
            }

            double distance = player.position().distanceTo(dest.pos);
            ctx.consumeWithForce(AbilityConfig.overload("penetrate_teleport", exp),
                    (float) (distance * AbilityConfig.cp("penetrate_teleport", exp)));
            ctx.addSkillExp((float) (0.00014f * distance));
            ctx.setCooldown((int) AbilityConfig.cooldown("penetrate_teleport", exp));
            if (player.isPassenger()) {
                player.stopRiding();
            }

            ((ServerPlayer) player).connection.teleport(
                    dest.pos.x, dest.pos.y, dest.pos.z, player.getYRot(), player.getXRot());
            player.fallDistance = 0;

            player.level().playSound(player, dest.pos.x, dest.pos.y, dest.pos.z,
                    ACSounds.TP_MOVE_PLAYER.get(), SoundSource.AMBIENT, 1.0f, 1.0f);

            terminate();
        }

        boolean hasPlace(Level world, Vec3 feet) {
            AABB bb = player.getBoundingBox();
            AABB at = bb.move(feet.x - player.getX(), feet.y - player.getY(), feet.z - player.getZ());
            return world.noCollision(player, at.deflate(0.05));
        }

        public Dest getDest() {
            Level world = player.level();
            double cplim = ctx.cpData.getCP() / AbilityConfig.cp("penetrate_teleport", exp);

            double dist = Math.min(Math.abs(curDist), cplim);
            final double STEP = 0.8;

            Vec3 pos = player.position();
            Vec3 dir = player.getLookAngle().normalize();
            if (curDist < 0) {
                dir = dir.scale(-1);
            }
            Vec3 lastFree = hasPlace(world, pos) ? pos : null;
            int stage = 0;
            int counter = 0;

            double total = 0.0;
            while (total <= dist) {
                boolean b = hasPlace(world, pos);
                if (b) lastFree = pos;
                if (stage == 0) {
                    if (!b) stage = 1;
                } else if (stage == 1) {
                    if (b) stage = 2;
                } else {
                    counter += 1;
                    if (!b || counter > 4) break;
                }
                total += STEP;
                pos = pos.add(dir.scale(STEP));
            }

            if (!hasPlace(world, pos)) {
                if (stage == 2 && lastFree != null) {
                    pos = lastFree;
                } else {
                    return new Dest(pos, false);
                }
            }
            return new Dest(pos, true);
        }

        public void updateDistance(float delta) {
            if (delta + curDist >= minDist && delta + curDist <= maxDist) {
                curDist += delta;
            }
        }
    }

    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    @RegClientContext(PTContext.class)
    public static class PTContextC extends ClientContext {

        private final PTContext par;
        private EntityTPMarking mark = null;

        public PTContextC(PTContext par) {
            super(par);
            this.par = par;
        }

        @Listener(channel = MSG_MADEALIVE, side = LogicalSide.CLIENT)
        private void l_spawnMark() {
            if (isLocal()) {
                mark = new EntityTPMarking(player.level());
                mark.moveTo2(player.getX(), player.getY() + player.getEyeHeight(), player.getZ());
                mark.yaw = player.getYRot();

                if (player instanceof net.minecraft.client.player.AbstractClientPlayer acp) {
                    mark.skin = acp.getSkinTextureLocation();
                    mark.slimArms = "slim".equals(acp.getModelName());
                }
                ACEffectEntities.spawn(mark);
                MinecraftForge.EVENT_BUS.register(this);
            }
        }

        @SubscribeEvent
        public void onMouseScroll(InputEvent.MouseScrollingEvent event) {
            if (AbilityPipeline.canUseMouseWheel()) {
                float perNotch = lerpf(1, par.maxDist, AbilityPipeline.tpWheelSensitivity());
                par.updateDistance((float) event.getScrollDelta() * perNotch);
                event.setCanceled(true);
            }
        }

        @Listener(channel = MSG_TICK, side = LogicalSide.CLIENT)
        private void l_updateMark() {
            if (isLocal() && mark != null) {
                Dest dest = par.getDest();
                mark.available = dest.available;
                mark.moveTo2(dest.pos.x, dest.pos.y + player.getEyeHeight(), dest.pos.z);
                mark.yaw = player.getYRot();
                mark.touch();

                if (dest.available && RandUtils.ranged(0, 1) < 0.4) {
                    player.level().addParticle(ACParticles.TP.get(),
                            mark.getX() + RandUtils.ranged(-1, 1),
                            mark.getY() + RandUtils.ranged(0.2, 1.6) - 1.6,
                            mark.getZ() + RandUtils.ranged(-1, 1),
                            RandUtils.ranged(-.03, .03), RandUtils.ranged(0, .05), RandUtils.ranged(-.03, .03));
                }
            }
        }

        @Listener(channel = MSG_KEYUP, side = LogicalSide.CLIENT)
        private void l_playCastSound() {
            if (isLocal() && par.getDest().available) {
                net.minecraft.client.Minecraft.getInstance().getSoundManager().play(
                        net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                                ACSounds.TP_MOVE_PLAYER.get(), 1.0f, 1.0f));
            }
        }

        @Listener(channel = MSG_TERMINATED, side = LogicalSide.CLIENT)
        private void c_endEffect() {
            if (isLocal()) {
                MinecraftForge.EVENT_BUS.unregister(this);
                if (mark != null) {
                    mark.discard();
                    mark = null;
                }
            }
        }
    }

    public static class Dest {
        public final Vec3 pos;
        public final boolean available;

        public Dest(Vec3 pos, boolean available) {
            this.pos = pos;
            this.available = available;
        }
    }
}
