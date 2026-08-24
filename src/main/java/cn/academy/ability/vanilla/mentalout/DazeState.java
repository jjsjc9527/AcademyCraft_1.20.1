package cn.academy.ability.vanilla.mentalout;

import cn.academy.ability.vanilla.mentalout.skill.Daze;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.util.RandUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class DazeState {

    private static final String TICKS = "mo_daze_ticks";

    private static final int SYNC_INTERVAL = 10;

    private DazeState() {}

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new DazeEvents());
    }

    public static int getTicks(Entity e) {
        return e.getPersistentData().getInt(TICKS);
    }

    public static boolean isDazed(Entity e) {
        return getTicks(e) > 0;
    }

    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    public static boolean isLocalPlayerDazed() {
        net.minecraft.client.player.LocalPlayer p = net.minecraft.client.Minecraft.getInstance().player;
        return p != null && isDazed(p);
    }

    public static boolean frozenAsScenery(Entity e) {
        return e.level().isClientSide && isLocalPlayerDazed();
    }

    public static boolean renderFrozen(Entity e) {
        return isDazed(e) || frozenAsScenery(e);
    }

    private static java.util.Set<Integer> visionSnapshot = null;

    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    public static void beginVisionSnapshot() {
        java.util.Set<Integer> ids = new java.util.HashSet<>();
        net.minecraft.client.multiplayer.ClientLevel lv =
                net.minecraft.client.Minecraft.getInstance().level;
        if (lv != null) {
            for (Entity e : lv.entitiesForRendering()) {
                ids.add(e.getId());
            }
        }
        visionSnapshot = ids;
    }

    public static void endVisionSnapshot() {
        visionSnapshot = null;
        cn.academy.gravity.GravityClientHandler.flushPending();
    }

    public static boolean hiddenFromDazedVision(Entity e) {
        java.util.Set<Integer> s = visionSnapshot;
        if (s == null) {
            return false;
        }
        if (!e.level().isClientSide || !isLocalPlayerDazed()) {
            visionSnapshot = null;
            return false;
        }
        return !s.contains(e.getId());
    }

    public static void setTicks(Entity e, int ticks) {
        e.getPersistentData().putInt(TICKS, Math.max(0, ticks));
    }

    public static void apply(LivingEntity target, int ticks, net.minecraft.world.entity.player.Player source) {
        boolean fresh = getTicks(target) <= 0;
        setTicks(target, Math.max(getTicks(target), ticks));
        if (source != null) {
            target.getPersistentData().putUUID(OWNER, source.getUUID());
        }

        if (fresh && !target.level().isClientSide) {
            target.getPersistentData().putBoolean(NOGRAV, target.isNoGravity());
        }
        target.setDeltaMovement(Vec3.ZERO);
        target.hasImpulse = true;
        sync(target);
    }

    private static final String NOGRAV = "mo_daze_nograv";

    private static final String OWNER = "mo_daze_owner";

    public static net.minecraft.world.entity.player.Player ownerOf(Entity e) {
        net.minecraft.nbt.CompoundTag d = e.getPersistentData();
        return d.hasUUID(OWNER) ? e.level().getPlayerByUUID(d.getUUID(OWNER)) : null;
    }

    public static void clear(Entity e) {
        setTicks(e, 0);
        e.getPersistentData().remove(OWNER);
        if (!e.level().isClientSide) {
            sync(e);
        }
    }

    private static void sync(Entity target) {
        NetworkMessage.sendToTracking(target, Daze.INSTANCE, Daze.MSG_SYNC, target, getTicks(target));
    }

    private static void freezeInterpolation(LivingEntity e) {
        e.setOldPosAndRot();
        e.yBodyRotO = e.yBodyRot;
        e.yHeadRotO = e.yHeadRot;
        e.oAttackAnim = e.attackAnim;
        alignPairedFields(e);
    }

    private static final java.util.Map<Class<?>, java.lang.reflect.Field[][]> PAIRS =
            new java.util.concurrent.ConcurrentHashMap<>();

    private static java.util.List<String> currentNamesOf(String old) {
        java.util.List<String> out = new java.util.ArrayList<>(3);
        int n = old.length();
        if (n > 1 && old.charAt(0) == 'o' && Character.isUpperCase(old.charAt(1))) {
            out.add(Character.toLowerCase(old.charAt(1)) + old.substring(2));
        }
        if (n > 3 && old.startsWith("old") && Character.isUpperCase(old.charAt(3))) {
            out.add(Character.toLowerCase(old.charAt(3)) + old.substring(4));
        }
        if (n > 1 && old.endsWith("O")) {
            out.add(old.substring(0, n - 1));
        }
        if (n > 3 && old.endsWith("Old")) {
            out.add(old.substring(0, n - 3));
        }
        if (n > 1 && old.endsWith("0")) {
            out.add(old.substring(0, n - 1));
        }
        int i = old.indexOf('O', 1);
        if (i > 0 && i + 1 < n && Character.isUpperCase(old.charAt(i + 1))) {
            out.add(old.substring(0, i) + old.substring(i + 1));
        }
        return out;
    }

    private static java.lang.reflect.Field[][] pairsOf(Class<?> cls) {
        return PAIRS.computeIfAbsent(cls, c -> {
            java.util.Map<String, java.lang.reflect.Field> all = new java.util.HashMap<>();
            for (Class<?> k = c; k != null && k != Object.class; k = k.getSuperclass()) {
                for (java.lang.reflect.Field f : k.getDeclaredFields()) {
                    int mod = f.getModifiers();
                    if (java.lang.reflect.Modifier.isStatic(mod)
                            || java.lang.reflect.Modifier.isFinal(mod)) {
                        continue;
                    }
                    all.putIfAbsent(f.getName(), f);
                }
            }
            java.util.List<java.lang.reflect.Field[]> out = new java.util.ArrayList<>();
            for (java.util.Map.Entry<String, java.lang.reflect.Field> en : all.entrySet()) {
                for (String cur : currentNamesOf(en.getKey())) {
                    java.lang.reflect.Field cf = all.get(cur);

                    if (cf == null || cf.getType() != en.getValue().getType()) {
                        continue;
                    }
                    try {
                        en.getValue().setAccessible(true);
                        cf.setAccessible(true);
                        out.add(new java.lang.reflect.Field[]{en.getValue(), cf});
                    } catch (Throwable ignored) {

                    }
                    break;
                }
            }
            return out.toArray(new java.lang.reflect.Field[0][]);
        });
    }

    private static void alignPairedFields(LivingEntity e) {
        for (java.lang.reflect.Field[] p : pairsOf(e.getClass())) {
            try {
                Object cur = p[1].get(e);
                if (cur != null && cur.getClass().isArray()) {

                    Object old = p[0].get(e);
                    int n = java.lang.reflect.Array.getLength(cur);
                    if (old != null && java.lang.reflect.Array.getLength(old) == n) {
                        System.arraycopy(cur, 0, old, 0, n);
                    }
                } else {
                    p[0].set(e, cur);
                }
            } catch (Throwable ignored) {

            }
        }
    }

    public static boolean frozenTick(Entity entity) {

        if (!(entity instanceof LivingEntity e)) {
            return false;
        }
        int ticks = getTicks(e);
        if (ticks <= 0) {
            return false;
        }

        if (!e.isAlive()) {
            release(e);
            return false;
        }

        setTicks(e, ticks - 1);

        freezeInterpolation(e);

        if (!e.level().isClientSide) {

            e.setDeltaMovement(Vec3.ZERO);
            e.hasImpulse = false;
            e.setNoGravity(true);
        }

        if (e.level().isClientSide) {
            if (ticks % 4 == 0) {
                spawnParticles(e);
            }

            if (ticks == 1) {
                endVisionSnapshot();
            }
        } else {
            syncHealthIfChanged(e);
            if (ticks % SYNC_INTERVAL == 0) {
                sync(e);
            }
            if (ticks == 1) {
                release(e);
            }
        }
        return true;
    }

    private static final String LAST_HP = "mo_daze_lasthp";

    private static void syncHealthIfChanged(LivingEntity e) {
        if (!(e instanceof net.minecraft.server.level.ServerPlayer sp)) {
            return;
        }
        float hp = sp.getHealth();
        net.minecraft.nbt.CompoundTag d = sp.getPersistentData();
        if (Math.abs(hp - d.getFloat(LAST_HP)) < 1.0e-4f) {
            return;
        }
        d.putFloat(LAST_HP, hp);
        sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetHealthPacket(
                hp, sp.getFoodData().getFoodLevel(), sp.getFoodData().getSaturationLevel()));
    }

    private static void spawnParticles(LivingEntity e) {
        double w = e.getBbWidth(), h = e.getBbHeight();
        for (int i = 0; i < 2; ++i) {

            Vec3 p = FaintState.bodyToWorld(e,
                    RandUtils.ranged(-w * 0.6, w * 0.6),
                    RandUtils.ranged(0, h),
                    RandUtils.ranged(-w * 0.6, w * 0.6));
            e.level().addParticle(ParticleTypes.END_ROD, p.x, p.y, p.z, 0, 0, 0);
        }
    }

    private static void release(LivingEntity e) {
        setTicks(e, 0);
        e.setDeltaMovement(Vec3.ZERO);
        e.hasImpulse = true;

        if (!e.level().isClientSide) {
            net.minecraft.nbt.CompoundTag d = e.getPersistentData();
            if (d.contains(NOGRAV)) {
                e.setNoGravity(d.getBoolean(NOGRAV));
                d.remove(NOGRAV);
            } else {
                e.setNoGravity(false);
            }
        }
    }

    public static class DazeEvents {

        @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
        public void onLivingTick(LivingEvent.LivingTickEvent event) {
            if (frozenTick(event.getEntity())) {
                event.setCanceled(true);
            }
        }
    }
}
