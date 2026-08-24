package cn.academy.ability.vanilla.teleporter.skill;

import cn.academy.ability.AbilityPipeline;
import cn.academy.util.AimTrace;
import cn.academy.ACParticles;
import cn.academy.ACSounds;
import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientContext;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.RegClientContext;
import cn.academy.ability.vanilla.teleporter.util.TPSkillHelper;
import cn.academy.client.render.entity.ACEffectEntities;
import cn.academy.config.AbilityConfig;
import cn.academy.entity.EntityMarker;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.util.RandUtils;
import net.minecraft.sounds.SoundSource;
import cn.academy.util.RayReflect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.LogicalSide;

import static cn.lambdalib2.util.MathUtils.lerpf;

public class ThreateningTeleport extends Skill {

    public static final ThreateningTeleport INSTANCE = new ThreateningTeleport();

    public static final float REFLECT_DIFFICULTY = 0.15f;

    public ThreateningTeleport() {
        super("threatening_teleport", 1);
    }

    @Override
    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    public void activate(cn.academy.ability.context.ClientRuntime rt, int keyID) {
        activateSingleKey2(rt, keyID, TTContext::new);
    }

    static float rangeOf(float exp) {
        return lerpf(8, 15, exp);
    }

    static TraceResult trace(Player player, float range) {
        Vec3 eye = player.getEyePosition(1.0f);
        Vec3 look = player.getViewVector(1.0f);
        Vec3 rayEnd = eye.add(look.scale(range));

        BlockHitResult block = player.level().clip(new ClipContext(
                eye, rayEnd, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        Vec3 clipEnd = block.getType() == HitResult.Type.BLOCK ? block.getLocation() : rayEnd;

        EntityHitResult ent = AimTrace.firstResult(player.level(), player, eye, clipEnd,
                e -> e != player && e.isAlive() && e instanceof LivingEntity
                        && AbilityPipeline.canTarget(player, e));

        TraceResult ret = new TraceResult();
        if (ent != null) {
            Entity t = ent.getEntity();
            ret.setPos(t.getX(), t.getY() + t.getBbHeight(), t.getZ());
            ret.target = t;
        } else {
            ret.setPos(clipEnd.x, clipEnd.y, clipEnd.z);
        }
        return ret;
    }

    public static class TTContext extends Context<ThreateningTeleport> {

        static final String MSG_EXECUTE = "execute";

        private final float exp = ctx.getSkillExp();

        public TTContext(Player player) {
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

        @Listener(channel = MSG_MADEALIVE, side = LogicalSide.SERVER)
        private void s_madeAlive() {
            if (player.getMainHandItem().isEmpty()) terminate();
        }

        @Listener(channel = MSG_TICK, side = LogicalSide.SERVER)
        private void s_tick() {
            if (player.getMainHandItem().isEmpty()) terminate();
        }

        @Listener(channel = MSG_EXECUTE, side = LogicalSide.SERVER)
        private void s_execute() {
            boolean executed = false;
            ItemStack cur = player.getMainHandItem();
            if (!cur.isEmpty() && ctx.consume(
                    AbilityConfig.overload("threatening_teleport", exp),
                    (int) AbilityConfig.cp("threatening_teleport", exp))) {
                executed = true;
                TraceResult result = trace(player, rangeOf(exp));
                double dropProb = 1.0;
                boolean hit = false;
                Vec3 deflectDir = null;
                if (result.target != null) {
                    hit = true;

                    Vec3 eye = player.getEyePosition(1.0f);
                    Vec3 look = player.getViewVector(1.0f);
                    final Entity victim = result.target;
                    Vec3[] rd = {null};
                    boolean blocked = ctx.tryReflect(victim,
                            ev -> {
                                RayReflect.fill(ev, eye, look, victim, 0);
                                ev.difficulty = REFLECT_DIFFICULTY;
                            },
                            ev -> rd[0] = ev.reflectDir != null && ev.reflectDir.lengthSqr() > 1e-6
                                    ? ev.reflectDir.normalize() : look.scale(-1));
                    if (blocked) {
                        deflectDir = rd[0];
                        dropProb = 1.0;
                    } else {
                        TPSkillHelper.attackIgnoreArmor(ctx, victim, getDamage(cur));
                        dropProb = 0.3;
                    }
                }

                ItemStack drop = cur.copy();
                drop.setCount(1);
                if (!player.getAbilities().instabuild) {
                    cur.shrink(1);
                }
                if (RandUtils.ranged(0, 1) < dropProb) {
                    ItemEntity ie = new ItemEntity(
                            player.level(), result.x, result.y, result.z, drop);
                    if (deflectDir != null) {

                        ie.setDeltaMovement(deflectDir.scale(0.55).add(0, 0.15, 0));
                        ie.hurtMarked = true;
                        ie.setPickUpDelay(20);
                    }
                    player.level().addFreshEntity(ie);
                }
                ctx.addSkillExp((hit ? 1 : 0.2f) * 0.003f);
                ctx.setCooldown((int) AbilityConfig.cooldown("threatening_teleport", exp));
            }
            sendToClient(MSG_EXECUTE, executed);
            terminate();
        }

        private float getDamage(ItemStack stack) {
            float dmg = AbilityConfig.stat("threatening_teleport", "damage", exp);
            if (stack.is(cn.academy.ACItems.NEEDLE.get())) {
                dmg *= 1.5f;
            }
            return dmg;
        }
    }

    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    @RegClientContext(TTContext.class)
    public static class TTContextC extends ClientContext {

        private static final int[] COLOR_NORMAL = {0xba, 0xba, 0xba};
        private static final int[] COLOR_THREATENING = {0xba, 0xb2, 0x23};

        private final TTContext par;
        private EntityMarker marker = null;

        public TTContextC(TTContext par) {
            super(par);
            this.par = par;
        }

        @Listener(channel = MSG_MADEALIVE, side = LogicalSide.CLIENT)
        private void l_start() {
            if (isLocal()) {
                marker = new EntityMarker(player.level());
                marker.boxWidth = 0.5f;
                marker.boxHeight = 0.5f;
                marker.moveTo2(player.getX(), player.getY(), player.getZ());
                ACEffectEntities.spawn(marker);
            }
        }

        @Listener(channel = MSG_TICK, side = LogicalSide.CLIENT)
        private void l_tick() {
            if (isLocal() && marker != null) {
                TraceResult res = trace(player, rangeOf(par.exp));

                double y = res.target != null ? res.y - res.target.getBbHeight() : res.y;
                marker.moveTo2(res.x, y, res.z);
                marker.target = res.target;
                int[] c = res.target != null ? COLOR_THREATENING : COLOR_NORMAL;
                marker.color.set(c[0], c[1], c[2], 255);
                marker.touch();
            }
        }

        @Listener(channel = MSG_TERMINATED, side = LogicalSide.CLIENT)
        private void l_terminated() {
            if (isLocal() && marker != null) {
                marker.discard();
                marker = null;
            }
        }

        @Listener(channel = TTContext.MSG_EXECUTE, side = LogicalSide.CLIENT)
        private void c_end(boolean executed) {
            if (isLocal() && marker != null) {
                marker.discard();
                marker = null;
            }
            if (executed) {
                player.level().playLocalSound(player.getX(), player.getY(), player.getZ(),
                        ACSounds.TP_TP.get(), SoundSource.AMBIENT, 0.5f, 1.0f, false);

                TraceResult dropPos = trace(player, rangeOf(par.exp));
                double dx = dropPos.x + .5 - player.getX();
                double dy = dropPos.y + .5 - (player.getY() - 0.5);
                double dz = dropPos.z + .5 - player.getZ();
                double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                double posX = player.getX();
                double posY = player.getY() - 0.5;
                double posZ = player.getZ();
                Vec3 dir = new Vec3(dx, dy, dz).normalize();

                double move = 1;
                double x = move;
                while (x <= dist) {
                    posX += move * dir.x;
                    posY += move * dir.y;
                    posZ += move * dir.z;
                    player.level().addParticle(ACParticles.TP.get(), posX, posY, posZ,
                            RandUtils.ranged(-.02, .02), RandUtils.ranged(-.02, .05), RandUtils.ranged(-.02, .02));
                    move = RandUtils.ranged(1, 2);
                    x += move;
                }
            }
        }
    }

    public static class TraceResult {
        public double x, y, z;
        public Entity target = null;

        public void setPos(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
