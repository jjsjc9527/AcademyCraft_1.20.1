package cn.academy.ability.vanilla.electromaster;

import cn.academy.ability.AbilityPipeline;
import cn.academy.util.AimTrace;
import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientContext;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.RegClientContext;
import cn.academy.client.render.entity.ACEffectEntities;
import cn.academy.client.render.util.ThunderArcs;
import cn.academy.entity.EntityArc;
import cn.academy.entity.EntityRippleMark;
import cn.academy.entity.EntitySurroundArc;
import cn.academy.entity.EntityThunderStrike;
import cn.academy.util.RayReflect;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.util.RandUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.LogicalSide;

import cn.academy.config.AbilityConfig;
import static cn.lambdalib2.util.MathUtils.lerpf;

public class ThunderClap extends Skill {

    public static final ThunderClap INSTANCE = new ThunderClap();

    public static final int MIN_TICKS = 40;
    public static final int MAX_TICKS = 60;

    static final float REFLECT_DIFFICULTY = 0.8f;

    private static final double STRIKE_HEIGHT = 20.0;

    public ThunderClap() {
        super("thunder_clap", 5);
    }

    public static float getDamage(float exp, int ticks) {
        return AbilityConfig.stat("thunder_clap", "damage", exp)
                * lerpf(1.0f, 1.2f, (ticks - 40.0f) / 60.0f);
    }

    public static float getRange(float exp) {
        return lerpf(15, 30, exp);
    }

    public static int getCooldown(float exp, int ticks) {
        return (int) (ticks * AbilityConfig.cooldown("thunder_clap", exp));
    }

    @Override
    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    public void activate(cn.academy.ability.context.ClientRuntime rt, int keyID) {
        activateSingleKey2(rt, keyID, ClapContext::new);
    }

    static Vec3 aimPos(Player player) {

        final double DISTANCE = 256.0;
        Vec3 eye = player.getEyePosition(1.0f);
        Vec3 look = player.getViewVector(1.0f);
        Vec3 rayEnd = eye.add(look.scale(DISTANCE));

        BlockHitResult block = player.level().clip(new ClipContext(
                eye, rayEnd, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        Vec3 clipEnd = block.getType() == HitResult.Type.BLOCK ? block.getLocation() : rayEnd;

        EntityHitResult ent = AimTrace.firstResult(player.level(), player, eye, clipEnd,
                e -> e != player && e.isAlive() && e instanceof LivingEntity
                        && AbilityPipeline.canTarget(player, e));
        return ent != null ? ent.getEntity().position() : clipEnd;
    }

    public static class ClapContext extends Context<ThunderClap> {

        static final String MSG_START = "start";
        static final String MSG_END = "end";
        static final String MSG_EFFECT_START = "effect_start";
        static final String MSG_STRIKE = "strike";

        private final float exp = ctx.getSkillExp();
        private int ticks = 0;
        private double hitX, hitY, hitZ;

        public ClapContext(Player player) {
            super(player, INSTANCE);
        }

        @Listener(channel = MSG_KEYDOWN, side = LogicalSide.CLIENT)
        private void l_onKeyDown() {
            sendToServer(MSG_START);
        }

        @Listener(channel = MSG_START, side = LogicalSide.SERVER)
        private void s_onStart() {
            sendToClient(MSG_EFFECT_START);

            float overload = AbilityConfig.overload("thunder_clap", exp);
            ctx.consume(overload, 0);
        }

        @Listener(channel = MSG_TICK, side = LogicalSide.SERVER)
        private void s_onTick() {
            Vec3 pos = aimPos(player);
            hitX = pos.x;
            hitY = pos.y;
            hitZ = pos.z;

            ticks += 1;

            float consumption = AbilityConfig.cp("thunder_clap", exp);

            if ((ticks <= MIN_TICKS && !ctx.consume(0, consumption)) || ticks >= MAX_TICKS) {
                sendToSelf(MSG_END);
            }
        }

        @Listener(channel = MSG_END, side = LogicalSide.SERVER)
        private void s_onEnd() {
            if (ticks < MIN_TICKS) {
                terminate();
                return;
            }

            sendToClient(MSG_STRIKE, hitX, hitY, hitZ, (double) ThunderClap.getRange(exp));

            player.level().playSound(null, hitX, hitY, hitZ,
                    cn.academy.ACSounds.EM_THUNDER_CLAP.get(), net.minecraft.sounds.SoundSource.AMBIENT,
                    4.0f, 0.9f + player.getRandom().nextFloat() * 0.2f);

            final float fullDamage = ThunderClap.getDamage(exp, ticks);
            ctx.attackRange(hitX, hitY, hitZ, ThunderClap.getRange(exp), fullDamage,
                    e -> e != player && e.isAlive() && e instanceof LivingEntity
                            && ctx.canTarget(e),
                    (target, dmg) -> ctx.attackReflect(target, dmg,
                            ev -> {

                                Vec3 eye = target.getEyePosition(1.0f);
                                RayReflect.fill(ev, eye.add(0, STRIKE_HEIGHT, 0),
                                        new Vec3(0, -1, 0), target, 0);

                                ev.difficulty = REFLECT_DIFFICULTY
                                        * (fullDamage > 0 ? dmg / fullDamage : 1f);
                                ev.deflectable = false;
                            },
                            ev -> {  }));

            ctx.setCooldown(ThunderClap.getCooldown(exp, ticks));
            ctx.addSkillExp(0.003f);
            terminate();
        }

        @Listener(channel = MSG_KEYUP, side = LogicalSide.CLIENT)
        private void l_onEnd() {
            terminate();
        }

        @Listener(channel = MSG_KEYABORT, side = LogicalSide.CLIENT)
        private void l_onAbort() {
            terminate();
        }
    }

    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    @RegClientContext(ClapContext.class)
    public static class ClapContextC extends ClientContext {

        private EntitySurroundArc surroundArc;
        private EntityRippleMark mark;
        private int ticks = 0;
        private double hitX, hitY, hitZ;
        private boolean canTicking = false;

        public ClapContextC(ClapContext par) {
            super(par);
        }

        @Listener(channel = ClapContext.MSG_EFFECT_START, side = LogicalSide.CLIENT)
        private void c_spawnEffect() {
            canTicking = true;
            surroundArc = new EntitySurroundArc(player).setArcType(EntitySurroundArc.ArcType.BOLD);
            ACEffectEntities.spawn(surroundArc);

            if (isLocal()) {
                mark = new EntityRippleMark(player.level());
                ACEffectEntities.spawn(mark);
                mark.color.set(204, 204, 204, 179);
                mark.setPos(hitX, hitY, hitZ);
            }
        }

        @Listener(channel = MSG_TICK, side = LogicalSide.CLIENT)
        private void c_updateEffect() {
            if (canTicking) {
                Vec3 pos = ThunderClap.aimPos(player);
                hitX = pos.x;
                hitY = pos.y;
                hitZ = pos.z;

                ticks += 1;
                if (isLocal()) {

                    final float max = 0.1f;
                    final float min = 0.001f;
                    player.getAbilities().setWalkingSpeed(Math.max(min, max - (max - min) / 60 * ticks));
                    if (mark != null) {
                        mark.setPos(hitX, hitY, hitZ);
                    }
                }
            }
        }

        @Listener(channel = ClapContext.MSG_STRIKE, side = LogicalSide.CLIENT)
        private void c_strike(double x, double y, double z, double range) {
            double skyY = y + ThunderArcs.SKY_HEIGHT;

            EntityThunderStrike bolt = new EntityThunderStrike(player.level());
            bolt.setPos(x, y, z);
            ACEffectEntities.spawn(bolt);

            bolt.xOld = bolt.getX(); bolt.yOld = bolt.getY(); bolt.zOld = bolt.getZ();

            for (int i = 0; i < 16; i++) {
                double ox = x + RandUtils.rangef(-7f, 7f);
                double oz = z + RandUtils.rangef(-7f, 7f);
                double oy = skyY - RandUtils.rangef(1f, 11f);
                EntityArc fork = new EntityArc(player, ThunderArcs.skyFork);
                fork.viewOptimize = false;
                fork.lengthFixed = false;
                fork.fade = true; fork.fadeIn = 1; fork.fadeOut = 4;
                fork.setLife(RandUtils.rangei(8, 14));
                fork.setFromTo(x, skyY, z, ox, oy, oz);
                spawnArc(fork);
            }

            for (int i = 0; i < 20; i++) {
                double dx = RandUtils.rangef(-1f, 1f);
                double dy = RandUtils.rangef(-1f, 1f);
                double dz = RandUtils.rangef(-1f, 1f);
                double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (len < 1e-4) { dx = 1; dy = 0; dz = 0; len = 1; }
                double rr = range * RandUtils.rangef(0.4f, 1.0f) / len;
                EntityArc burst = new EntityArc(player, ThunderArcs.groundBurst);
                burst.viewOptimize = false;
                burst.lengthFixed = false;
                burst.fade = true; burst.fadeIn = 1; burst.fadeOut = 4;
                burst.setLife(RandUtils.rangei(8, 14));
                burst.setFromTo(x, y, z, x + dx * rr, y + dy * rr, z + dz * rr);
                spawnArc(burst);
            }
        }

        private void spawnArc(EntityArc arc) {
            ACEffectEntities.spawn(arc);
            arc.xOld = arc.getX();
            arc.yOld = arc.getY();
            arc.zOld = arc.getZ();
        }

        @Listener(channel = MSG_TERMINATED, side = LogicalSide.CLIENT)
        private void c_terminated() {
            canTicking = false;
            player.getAbilities().setWalkingSpeed(0.1f);
            if (surroundArc != null) {

                surroundArc.discardAfter(10);
            }

            if (isLocal() && mark != null) {
                mark.discard();
            }
        }
    }
}
