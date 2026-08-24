package cn.academy.ability.vanilla.meltdowner.skill;

import cn.academy.ability.AbilityContext;
import cn.academy.ability.vanilla.meltdowner.passiveskill.RadiationIntensify;
import cn.academy.datapart.AbilityData;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.util.RandUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import cn.academy.util.ACDefense;

public final class MDDamageHelper {

    private MDDamageHelper() {}

    private static final String MARK_TICK = "md_marktick", MARK_RATE = "md_markrate";

    private static final int MARK_MIN_TICKS = 60;

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new Events());
    }

    public static void attack(AbilityContext ctx, Entity target, float damage) {
        ctx.attack(target, damage);
        mark(ctx, target);
    }

    public static void attackReflect(AbilityContext ctx, Entity target, float damage,
                                     java.util.function.Consumer<cn.academy.event.ability.ReflectEvent> prefill,
                                     java.util.function.Consumer<cn.academy.event.ability.ReflectEvent> onReflected) {
        boolean[] blocked = {false};
        ctx.attackReflect(target, damage, prefill, ev -> {
            blocked[0] = true;
            onReflected.accept(ev);
        });
        if (!blocked[0]) {
            mark(ctx, target);
        }
    }

    private static void mark(AbilityContext ctx, Entity target) {
        if (target.level().isClientSide) {
            return;
        }
        AbilityData aData = AbilityData.get(ctx.player);
        if (!aData.isSkillLearned(RadiationIntensify.INSTANCE)) {
            return;
        }
        int ticks = Math.max(MARK_MIN_TICKS, getMarkTick(target));
        setMarkTick(target, ticks);
        target.getPersistentData().putFloat(MARK_RATE, RadiationIntensify.getRate(aData));

        NetworkMessage.sendToTracking(target, RadiationIntensify.INSTANCE,
                RadiationIntensify.MSG_MARK, target, ticks);
    }

    public static int getMarkTick(Entity e) {
        return e.getPersistentData().getInt(MARK_TICK);
    }

    public static void setMarkTick(Entity e, int ticks) {
        e.getPersistentData().putInt(MARK_TICK, ticks);
    }

    private static float getMarkRate(Entity e) {
        return e.getPersistentData().getFloat(MARK_RATE);
    }

    public static class Events {

        @SubscribeEvent
        public void onLivingTick(LivingEvent.LivingTickEvent event) {
            LivingEntity e = event.getEntity();
            int tick = getMarkTick(e);
            if (tick > 0) {
                setMarkTick(e, tick - 1);
                if (e.level().isClientSide) {
                    spawnParticles(e);
                }
            }
        }

        @SubscribeEvent
        public void onLivingHurt(LivingHurtEvent event) {
            if (getMarkTick(event.getEntity()) <= 0) {
                return;
            }
            float rate = getMarkRate(event.getEntity());
            if (rate > 0) {
                ACDefense.reduce(event, event.getAmount() * rate);
            }
        }

        private static void spawnParticles(LivingEntity e) {
            int times = RandUtils.rangei(0, 3);
            while (times-- > 0) {
                double r = RandUtils.ranged(.6, .7) * e.getBbWidth();
                double theta = RandUtils.nextDouble() * 2 * Math.PI;
                double h = RandUtils.ranged(0, e.getBbHeight());
                Vec3 at = new Vec3(e.getX() + r * Math.sin(theta), e.getY() + h, e.getZ() + r * Math.cos(theta));
                Vec3 vel = new Vec3(RandUtils.ranged(-1, 1), RandUtils.ranged(-1, 1), RandUtils.ranged(-1, 1))
                        .normalize().scale(0.02);
                e.level().addParticle(cn.academy.ACParticles.MD.get(),
                        at.x, at.y, at.z, vel.x, vel.y, vel.z);
            }
        }
    }
}
