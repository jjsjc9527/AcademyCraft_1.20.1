package cn.academy.ability.vanilla.mentalout;

import cn.academy.ability.AbilityContext;
import cn.academy.ability.vanilla.mentalout.skill.Faint;
import cn.academy.util.ACPierce;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.util.RandUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class FaintState {

    private static final String TICKS = "mo_faint_ticks";

    private static final String NEXT = "mo_faint_next";

    private static final String INTERVAL = "mo_faint_interval";

    private static final String DAMAGE = "mo_faint_damage";

    private static final String OWNER = "mo_faint_owner";

    private static final String GRAV = "mo_faint_grav";

    private static final String NOGRAV = "mo_faint_nograv";

    private static final String ROLL = "mo_faint_roll";

    private static final int SYNC_INTERVAL = 10;

    public static final ResourceKey<DamageType> ASPHYXIATION = ResourceKey.create(
            Registries.DAMAGE_TYPE, new ResourceLocation("academy", "asphyxiation"));

    private FaintState() {}

    public static void init() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new FaintEvents());
    }

    public static int getTicks(Entity e) {
        return e.getPersistentData().getInt(TICKS);
    }

    public static boolean isFainted(Entity e) {
        return getTicks(e) > 0;
    }

    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    public static boolean isLocalPlayerFainted() {
        net.minecraft.client.player.LocalPlayer p = net.minecraft.client.Minecraft.getInstance().player;
        return p != null && isFainted(p);
    }

    public static void setTicks(Entity e, int ticks) {
        e.getPersistentData().putInt(TICKS, Math.max(0, ticks));
    }

    public static final float LIE_DOWN_DEGREES = -90.0f;

    public static boolean liesViaGravity(Entity e) {
        return e instanceof Player;
    }

    public static float rollOf(Entity e) {
        return e.getPersistentData().getFloat(ROLL);
    }

    public static void setRoll(Entity e, float deg) {
        e.getPersistentData().putFloat(ROLL, deg);
    }

    private static final double LIE_CLEARANCE = 0.22;

    public static net.minecraft.world.phys.Vec3 bodyToWorld(
            LivingEntity e, double side, double up, double fwd) {

        if (isFainted(e) && liesViaGravity(e)) {
            return e.position().add(cn.academy.gravity.RotationUtil.vecPlayerToWorld(
                    new net.minecraft.world.phys.Vec3(side, up, fwd),
                    cn.academy.gravity.ACGravity.getGravityDirection(e)));
        }

        float yaw = e.yBodyRot * ((float) Math.PI / 180f);
        double sin = net.minecraft.util.Mth.sin(yaw), cos = net.minecraft.util.Mth.cos(yaw);

        net.minecraft.world.phys.Vec3 f = new net.minecraft.world.phys.Vec3(-sin, 0, cos);
        net.minecraft.world.phys.Vec3 s = new net.minecraft.world.phys.Vec3(-cos, 0, -sin);

        net.minecraft.world.phys.Vec3 base = e.position().add(s.scale(side));
        if (isFainted(e)) {
            return base.add(f.scale(up)).add(0, LIE_CLEARANCE - fwd, 0);
        }
        return base.add(0, up, 0).add(f.scale(fwd));
    }

    public static void apply(LivingEntity target, int ticks, int interval, float damage,
                             int darknessTicks, Player owner) {
        CompoundTag d = target.getPersistentData();
        d.putInt(TICKS, Math.max(d.getInt(TICKS), ticks));
        d.putInt(INTERVAL, Math.max(1, interval));
        d.putFloat(DAMAGE, damage);
        if (owner != null) {
            d.putUUID(OWNER, owner.getUUID());
        }

        d.putInt(NEXT, Math.max(1, interval));

        applyDarkness(target, darknessTicks);

        if (target instanceof net.minecraft.server.level.ServerPlayer sp) {
            lieDownByGravity(sp);
        }

        target.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        target.hasImpulse = true;
        sync(target);
    }

    private static void lieDownByGravity(net.minecraft.server.level.ServerPlayer sp) {
        net.minecraft.core.Direction face = net.minecraft.core.Direction.fromYRot(sp.yBodyRot);
        net.minecraft.core.Direction grav = face.getOpposite();

        net.minecraft.nbt.CompoundTag d = sp.getPersistentData();

        if (!d.contains(GRAV)) {
            d.putBoolean(NOGRAV, sp.isNoGravity());
        }
        d.putInt(GRAV, grav.get3DDataValue());
        d.putFloat(ROLL, net.minecraft.util.Mth.wrapDegrees(sp.yBodyRot - face.toYRot()));

        cn.academy.gravity.ACGravity.setGravityDirection(sp, grav, true);
        cn.academy.network.GravitySyncMessage.sync(sp, grav, true);
        sp.setNoGravity(true);
    }

    private static void standUpFromGravity(LivingEntity e) {
        if (!(e instanceof net.minecraft.server.level.ServerPlayer sp)) {
            return;
        }
        net.minecraft.nbt.CompoundTag d = sp.getPersistentData();
        if (!d.contains(GRAV)) {
            return;
        }
        cn.academy.gravity.ACGravity.setGravityDirection(sp, net.minecraft.core.Direction.DOWN, true);
        cn.academy.network.GravitySyncMessage.sync(sp, net.minecraft.core.Direction.DOWN, true);
        sp.setNoGravity(d.getBoolean(NOGRAV));
        d.remove(GRAV);
        d.remove(NOGRAV);
        d.putFloat(ROLL, 0f);
    }

    private static boolean applyingOwnDarkness = false;

    public static boolean isApplyingOwnDarkness() {
        return applyingOwnDarkness;
    }

    public static void applyDarkness(LivingEntity target, int ticks) {
        if (ticks <= 0) {
            return;
        }
        applyingOwnDarkness = true;
        try {
            target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, ticks, 0, false, false));
        } finally {
            applyingOwnDarkness = false;
        }
    }

    private static void sync(Entity target) {
        NetworkMessage.sendToTracking(target, Faint.INSTANCE, Faint.MSG_SYNC,
                target, getTicks(target), rollOf(target));
    }

    public static void tick(Entity entity) {
        if (!(entity instanceof LivingEntity e)) {
            return;
        }
        int ticks = getTicks(e);
        if (ticks <= 0) {
            return;
        }
        if (!e.isAlive()) {
            clear(e);
            return;
        }

        setTicks(e, ticks - 1);

        pinDown(e);

        if (e.level().isClientSide) {
            if (ticks % 3 == 0) {
                spawnParticles(e);
            }
            return;
        }

        CompoundTag d = e.getPersistentData();
        int next = d.getInt(NEXT) - 1;
        if (next <= 0) {
            suffocate(e);
            next = Math.max(1, d.getInt(INTERVAL));
        }
        d.putInt(NEXT, next);

        if (ticks % SYNC_INTERVAL == 0) {
            sync(e);
        }
        if (ticks == 1) {
            clear(e);
        }
    }

    private static void pinDown(LivingEntity e) {

        if (e.isNoGravity()) {
            e.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        } else {
            net.minecraft.world.phys.Vec3 v = e.getDeltaMovement();
            if (v.x == 0.0 && v.z == 0.0) {
                return;
            }
            e.setDeltaMovement(0.0, v.y, 0.0);
        }
        e.hasImpulse = true;
    }

    private static void suffocate(LivingEntity e) {
        float dmg = e.getPersistentData().getFloat(DAMAGE);
        if (dmg <= 0f) {
            return;
        }
        dmg = AbilityContext.calcSkillDamage(ownerOf(e), Faint.INSTANCE, e, dmg);

        ACPierce.hurtOrPierce(e, asphyxiation(e), ACPierce.ASPHYXIATION_PIERCE, dmg);
    }

    private static DamageSource asphyxiation(LivingEntity target) {
        net.minecraft.core.Holder<DamageType> type = target.level().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ASPHYXIATION);
        Player owner = ownerOf(target);
        return owner != null ? new DamageSource(type, owner) : new DamageSource(type);
    }

    public static Player ownerOf(LivingEntity target) {
        CompoundTag d = target.getPersistentData();
        return d.hasUUID(OWNER) ? target.level().getPlayerByUUID(d.getUUID(OWNER)) : null;
    }

    private static void spawnParticles(LivingEntity e) {
        double w = e.getBbWidth();
        double eye = e.getEyeY() - e.getY();
        for (int i = 0; i < 2; ++i) {
            net.minecraft.world.phys.Vec3 p = bodyToWorld(e,
                    RandUtils.ranged(-w * 0.4, w * 0.4),
                    eye + RandUtils.ranged(-0.1, 0.25),
                    RandUtils.ranged(-w * 0.4, w * 0.4));
            e.level().addParticle(ParticleTypes.SMOKE, p.x, p.y, p.z, 0, 0.01, 0);
        }
    }

    public static void clear(LivingEntity e) {

        standUpFromGravity(e);

        CompoundTag d = e.getPersistentData();
        d.putInt(TICKS, 0);
        d.remove(NEXT);
        d.remove(INTERVAL);
        d.remove(DAMAGE);
        d.remove(OWNER);

        if (!e.level().isClientSide) {
            sync(e);
        }
    }

    public static class FaintEvents {

        @net.minecraftforge.eventbus.api.SubscribeEvent
        public void onKnockBack(net.minecraftforge.event.entity.living.LivingKnockBackEvent event) {
            if (isFainted(event.getEntity())) {
                event.setCanceled(true);
            }
        }

        @net.minecraftforge.eventbus.api.SubscribeEvent
        public void onLogin(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer sp
                    && sp.getPersistentData().contains(GRAV) && !isFainted(sp)) {
                standUpFromGravity(sp);
            }
        }
    }
}
