package cn.academy.ability.vanilla.mentalout;

import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ProxyState {

    public static final String MSG_BEGIN = "ac_proxy_begin";

    public static final String MSG_INPUT = "ac_proxy_input";

    public static final String MSG_ATTACK = "ac_proxy_attack";

    public static final String MSG_END = "ac_proxy_end";

    public static final String MSG_DRIVE = "ac_proxy_drive";

    public static final String MSG_DENY = "ac_proxy_deny";

    public static final int F_JUMP = 1;
    public static final int F_SNEAK = 2;
    public static final int F_SPRINT = 4;

    private static final int INPUT_TIMEOUT = 40;

    private static final int ATTACK_COOLDOWN = 10;

    private static final double ATTACK_REACH = 3.0;

    private static final double PICK_INFLATE = 0.3;

    private static final double STUCK_EPS = 0.01;

    private static final int STUCK_TICKS = 3;

    private static final int DEAD_TICKS = 10;

    private static final double DEAD_EPS = 0.01;

    private static final int FALLBACK_RESET = 20;

    private static final double FALLBACK_SPEED = 0.2;

    private static final double MAX_PUSH_SPEED = 0.25;

    private static final double DIR_TOLERANCE = 0.5;

    private static final float SPRINT_FACTOR = 1.3f;

    private static final float SNEAK_FACTOR = 0.3f;

    private static final float SQUID_SPEED = 0.2f;

    private ProxyState() {}

    public static final class Link {

        public final UUID target;
        public float forward, strafe, yaw, pitch;
        public int flags;

        public long inputAt;

        public long attackAt;

        public double lastX, lastY, lastZ;

        public int stuck;

        public int deadTicks;

        public boolean fallback;

        public int idleTicks;

        public boolean prevNoGravity;

        public boolean gravityTouched;

        Link(UUID target, long now) {
            this.target = target;
            this.inputAt = now;
        }

        public boolean has(int flag) {
            return (flags & flag) != 0;
        }
    }

    private static final Map<UUID, Link> LINKS = new HashMap<>();

    public static boolean isProxyOwner(Entity e) {
        return e != null && LINKS.containsKey(e.getUUID());
    }

    public static boolean isProxied(Entity e) {
        if (e == null || LINKS.isEmpty()) {
            return false;
        }
        UUID id = e.getUUID();
        for (Link l : LINKS.values()) {
            if (id.equals(l.target)) {
                return true;
            }
        }
        return false;
    }

    public static Link linkOf(Entity owner) {
        return owner == null ? null : LINKS.get(owner.getUUID());
    }

    public static Link linkDrivingMob(Entity e) {
        if (e == null || LINKS.isEmpty() || e instanceof Player || e.level().isClientSide) {
            return null;
        }
        UUID id = e.getUUID();
        for (Link l : LINKS.values()) {
            if (id.equals(l.target)) {
                return l;
            }
        }
        return null;
    }

    public static void put(Entity owner, UUID target, long now) {
        if (owner != null && target != null) {
            LINKS.put(owner.getUUID(), new Link(target, now));
        }
    }

    public static void drop(Entity owner) {
        if (owner != null) {
            LINKS.remove(owner.getUUID());
        }
    }

    public static void clearAll() {
        LINKS.clear();
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new ProxyEvents());
    }

    public static class ProxyEvents {

        @SubscribeEvent
        public void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.START || LINKS.isEmpty()) {
                return;
            }
            tickAll(event.getServer());
        }

        @SubscribeEvent
        public void onServerStopping(net.minecraftforge.event.server.ServerStoppingEvent event) {
            clearAll();
        }
    }

    private static void tickAll(net.minecraft.server.MinecraftServer server) {
        List<UUID> owners = new ArrayList<>(LINKS.keySet());
        for (UUID id : owners) {
            Link link = LINKS.get(id);
            if (link == null) {
                continue;
            }
            ServerPlayer owner = server.getPlayerList().getPlayer(id);
            if (owner == null) {
                LINKS.remove(id);
                continue;
            }
            tickOne(owner, link);
        }
    }

    private static void tickOne(ServerPlayer owner, Link link) {
        long now = owner.level().getGameTime();
        if (now - link.inputAt > INPUT_TIMEOUT) {
            release(owner, "timeout");
            return;
        }
        Entity target = ((ServerLevel) owner.level()).getEntity(link.target);
        if (!(target instanceof LivingEntity le) || !le.isAlive() || le == owner) {
            release(owner, "lost");
            return;
        }
        if (owner.isDeadOrDying()) {
            release(owner, "self_down");
            return;
        }

        double maxDist = cn.academy.config.AbilityConfig.stat("wide_cast", "cam_range", 0f);
        if (owner.distanceToSqr(le) > maxDist * maxDist) {
            release(owner, "range_lost");
            return;
        }

        if (MentalImmune.blocked(owner,
                cn.academy.ability.vanilla.mentalout.advanced.FreeManip.INSTANCE, le)) {
            release(owner, "reflected");
            return;
        }
        if (le instanceof ServerPlayer sp) {
            driveRemotePlayer(sp, link);
        } else {
            driveMob(le, link);
        }
    }

    private static void driveMob(LivingEntity mob, Link link) {

        mob.hasImpulse = true;

        if (mob instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon dragon) {
            driveDragon(dragon, link);
            return;
        }

        if (mob instanceof net.minecraft.world.entity.animal.Squid squid) {
            driveSquid(squid, link);
            return;
        }
        mob.setYRot(link.yaw);
        mob.yRotO = link.yaw;
        mob.setYHeadRot(link.yaw);
        mob.yHeadRotO = link.yaw;
        mob.setYBodyRot(link.yaw);
        mob.yBodyRotO = link.yaw;
        mob.setXRot(link.pitch);
        mob.xRotO = link.pitch;

        boolean vertical = canFly(mob) || mob.isInWater() || mob.isInLava();

        boolean sprint = link.has(F_SPRINT);
        if (mob.isSprinting() != sprint) {
            mob.setSprinting(sprint);
        }

        if (mob.getAttributes().hasAttribute(Attributes.MOVEMENT_SPEED)) {
            float spd = (float) mob.getAttributeValue(Attributes.MOVEMENT_SPEED);

            if (!vertical && link.has(F_SNEAK)) {
                spd *= SNEAK_FACTOR;
            }
            mob.setSpeed(spd);
        }

        keepAloft(mob, link, vertical && canFly(mob));

        if (vertical) {

            mob.yya = link.has(F_JUMP) ? 1f : link.has(F_SNEAK) ? -1f : 0f;
            mob.setJumping(false);
            mob.setShiftKeyDown(false);
        } else {
            mob.yya = 0f;
            mob.setShiftKeyDown(link.has(F_SNEAK));

            mob.setJumping(link.has(F_JUMP)
                    || (hopsToMove(mob) && (link.forward != 0f || link.strafe != 0f))
                    || stuckAndWants(mob, link));
        }

        mob.zza = link.forward;
        mob.xxa = link.strafe;

        fallbackPush(mob, link);

        link.lastX = mob.getX();
        link.lastY = mob.getY();
        link.lastZ = mob.getZ();
    }

    private static void fallbackPush(LivingEntity mob, Link link) {
        boolean wants = link.forward != 0f || link.strafe != 0f
                || link.has(F_JUMP) || link.has(F_SNEAK);

        if (link.fallback) {

            if (!wants) {
                if (++link.idleTicks >= FALLBACK_RESET) {
                    link.fallback = false;
                    link.deadTicks = 0;
                    link.idleTicks = 0;
                }
                return;
            }
            link.idleTicks = 0;
            pushDirectly(mob, link);
            return;
        }

        if (!wants || mob.horizontalCollision) {
            link.deadTicks = 0;
            return;
        }

        Vec3 want = wantDir(mob, link);
        if (want == null) {
            link.deadTicks = 0;
            return;
        }
        double dx = mob.getX() - link.lastX;
        double dy = mob.getY() - link.lastY;
        double dz = mob.getZ() - link.lastZ;

        double my = want.y != 0.0 ? dy : 0.0;
        double movedSqr = dx * dx + my * my + dz * dz;
        if (movedSqr > DEAD_EPS * DEAD_EPS) {

            double dot = (dx * want.x + my * want.y + dz * want.z) / Math.sqrt(movedSqr);
            if (dot >= DIR_TOLERANCE) {
                link.deadTicks = 0;
                return;
            }
        }
        if (++link.deadTicks >= DEAD_TICKS) {
            link.fallback = true;
            link.idleTicks = 0;
            pushDirectly(mob, link);
        }
    }

    private static Vec3 wantDir(LivingEntity mob, Link link) {
        float yawRad = link.yaw * ((float) Math.PI / 180f);
        double hx = -Mth.sin(yawRad) * link.forward + Mth.cos(yawRad) * link.strafe;
        double hz = Mth.cos(yawRad) * link.forward + Mth.sin(yawRad) * link.strafe;
        double vy = 0.0;
        if (canFly(mob) || mob.isInWater() || mob.isInLava()) {
            vy = link.has(F_JUMP) ? 1.0 : link.has(F_SNEAK) ? -1.0 : 0.0;
        }
        double len = Math.sqrt(hx * hx + hz * hz + vy * vy);
        return len < 1.0e-4 ? null : new Vec3(hx / len, vy / len, hz / len);
    }

    private static void pushDirectly(LivingEntity mob, Link link) {
        Vec3 dir = wantDir(mob, link);
        if (dir == null) {
            return;
        }

        double spd = Math.min(
                mob.getAttributes().hasAttribute(Attributes.MOVEMENT_SPEED)
                        ? mob.getAttributeValue(Attributes.MOVEMENT_SPEED)
                        : FALLBACK_SPEED,
                MAX_PUSH_SPEED) * manualFactor(mob, link);
        mob.move(net.minecraft.world.entity.MoverType.SELF, dir.scale(spd));
    }

    private static final double DRAGON_REACH = 32.0;

    private static void driveDragon(
            net.minecraft.world.entity.boss.enderdragon.EnderDragon dragon, Link link) {

        if (DragonControl.isDying(dragon)) {
            return;
        }

        float yawRad = link.yaw * ((float) Math.PI / 180f);
        double hx = -Mth.sin(yawRad) * link.forward + Mth.cos(yawRad) * link.strafe;
        double hz = Mth.cos(yawRad) * link.forward + Mth.sin(yawRad) * link.strafe;
        double hLen = Math.sqrt(hx * hx + hz * hz);
        if (hLen < 1.0e-4) {
            float cur = dragon.getYRot() * ((float) Math.PI / 180f);
            hx = -Mth.sin(cur);
            hz = Mth.cos(cur);
        } else {
            hx /= hLen;
            hz /= hLen;
        }

        double climb;
        if (link.has(F_JUMP)) {
            climb = DragonControl.FLY_SPEED;
        } else if (link.has(F_SNEAK)) {
            climb = -DragonControl.FLY_SPEED;
        } else {
            climb = -Mth.sin(link.pitch * ((float) Math.PI / 180f))
                    * link.forward * DragonControl.FLY_SPEED;
        }
        Vec3 goal = new Vec3(
                dragon.getX() + hx * DRAGON_REACH,
                dragon.getY() + climb * DRAGON_REACH,
                dragon.getZ() + hz * DRAGON_REACH);
        DragonControl.steer(dragon, goal);
    }

    private static void driveSquid(net.minecraft.world.entity.animal.Squid squid, Link link) {
        float yawRad = link.yaw * ((float) Math.PI / 180f);
        double hx = -Mth.sin(yawRad) * link.forward + Mth.cos(yawRad) * link.strafe;
        double hz = Mth.cos(yawRad) * link.forward + Mth.sin(yawRad) * link.strafe;
        double vy;
        if (link.has(F_JUMP)) {
            vy = 1.0;
        } else if (link.has(F_SNEAK)) {
            vy = -1.0;
        } else {
            vy = -Mth.sin(link.pitch * ((float) Math.PI / 180f)) * link.forward;
        }
        double len = Math.sqrt(hx * hx + vy * vy + hz * hz);
        if (len < 1.0e-4) {
            squid.setMovementVector(0f, 0f, 0f);
            return;
        }

        float s = SQUID_SPEED * manualFactor(squid, link);
        squid.setMovementVector((float) (hx / len) * s,
                (float) (vy / len) * s,
                (float) (hz / len) * s);
    }

    private static float manualFactor(LivingEntity mob, Link link) {
        float f = 1.0f;
        if (link.has(F_SPRINT)) {
            f *= SPRINT_FACTOR;
        }
        if (link.has(F_SNEAK) && !(canFly(mob) || mob.isInWater() || mob.isInLava())) {
            f *= SNEAK_FACTOR;
        }
        return f;
    }

    private static boolean canFly(LivingEntity e) {
        if (e instanceof net.minecraft.world.entity.FlyingMob) {
            return true;
        }
        if (e instanceof Mob m) {
            return m.getMoveControl()
                    instanceof net.minecraft.world.entity.ai.control.FlyingMoveControl
                    || m.getNavigation()
                    instanceof net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
        }
        return false;
    }

    private static boolean hopsToMove(LivingEntity e) {
        return e instanceof net.minecraft.world.entity.monster.Slime
                || e instanceof net.minecraft.world.entity.animal.Rabbit;
    }

    private static boolean stuckAndWants(LivingEntity mob, Link link) {

        if (link.fallback) {
            return false;
        }
        if ((link.forward == 0f && link.strafe == 0f) || !mob.onGround()) {
            link.stuck = 0;
            return false;
        }
        double dx = mob.getX() - link.lastX;
        double dz = mob.getZ() - link.lastZ;
        if (dx * dx + dz * dz < STUCK_EPS * STUCK_EPS) {
            ++link.stuck;
        } else {
            link.stuck = 0;
        }
        return link.stuck >= STUCK_TICKS;
    }

    private static void keepAloft(LivingEntity mob, Link link, boolean fly) {
        if (!fly) {
            return;
        }
        if (!link.gravityTouched) {
            link.prevNoGravity = mob.isNoGravity();
            link.gravityTouched = true;
        }
        mob.setNoGravity(true);
    }

    private static void driveRemotePlayer(ServerPlayer target, Link link) {
        NetworkMessage.sendTo(target, NetworkMessage.staticCaller(ProxyState.class), MSG_DRIVE,
                target.getId(), link.forward, link.strafe, link.yaw, link.pitch, link.flags);
    }

    public static void release(ServerPlayer owner, String why) {
        if (owner == null || !LINKS.containsKey(owner.getUUID())) {
            return;
        }
        Link link = LINKS.remove(owner.getUUID());

        if (link != null && link.gravityTouched && owner.level() instanceof ServerLevel gl
                && gl.getEntity(link.target) instanceof LivingEntity flyer) {
            flyer.setNoGravity(link.prevNoGravity);
        }

        if (link != null && owner.level() instanceof ServerLevel dl
                && dl.getEntity(link.target)
                instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon dragon) {
            DragonControl.restore(dragon);
        }

        if (link != null && owner.level() instanceof ServerLevel sl
                && sl.getEntity(link.target) instanceof ServerPlayer sp) {
            NetworkMessage.sendTo(sp, NetworkMessage.staticCaller(ProxyState.class), MSG_DRIVE,
                    sp.getId(), 0f, 0f, sp.getYRot(), sp.getXRot(), -1);
        }
        NetworkMessage.sendTo(owner, NetworkMessage.staticCaller(ProxyState.class), MSG_DENY, why);
    }

    private static void proxyAttack(ServerPlayer owner, LivingEntity actor, Link link) {
        long now = actor.level().getGameTime();
        if (now - link.attackAt < ATTACK_COOLDOWN) {
            return;
        }
        link.attackAt = now;

        LivingEntity victim = pickVictim(actor);
        actor.swing(InteractionHand.MAIN_HAND);
        if (victim == null) {
            return;
        }

        if (!cn.academy.ability.AbilityPipeline.canTarget(owner, victim)) {
            return;
        }
        if (actor.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)) {
            actor.doHurtTarget(victim);
        }
    }

    private static LivingEntity pickVictim(LivingEntity actor) {
        Vec3 eye = actor.getEyePosition();
        double reach = ATTACK_REACH + actor.getBbWidth();
        Vec3 to = eye.add(actor.getViewVector(1.0f).scale(reach));
        AABB box = actor.getBoundingBox().expandTowards(actor.getViewVector(1.0f).scale(reach))
                .inflate(PICK_INFLATE);
        EntityHitResult hit = net.minecraft.world.entity.projectile.ProjectileUtil
                .getEntityHitResult(actor, eye, to, box,
                        e -> e != actor && e instanceof LivingEntity && e.isPickable(),
                        reach * reach);
        return hit != null && hit.getEntity() instanceof LivingEntity le ? le : null;
    }

    @Listener(channel = MSG_BEGIN, side = LogicalSide.SERVER)
    private static void s_begin(Player owner, Integer targetId) {
        if (!(owner instanceof ServerPlayer sp) || targetId == null) {
            return;
        }

        if (!cn.academy.ability.vanilla.mentalout.advanced.FreeManip.isLearned(sp)) {
            NetworkMessage.sendTo(sp, NetworkMessage.staticCaller(ProxyState.class),
                    MSG_DENY, "no_skill");
            return;
        }
        Entity target = sp.level().getEntity(targetId);
        if (!(target instanceof LivingEntity le) || !le.isAlive() || le == sp) {
            NetworkMessage.sendTo(sp, NetworkMessage.staticCaller(ProxyState.class),
                    MSG_DENY, "lost");
            return;
        }

        cn.academy.datapart.RemoteData rd = cn.academy.datapart.RemoteData.get(sp);
        if (rd == null || !rd.isAlly(le.getUUID())) {
            NetworkMessage.sendTo(sp, NetworkMessage.staticCaller(ProxyState.class),
                    MSG_DENY, "not_ally");
            return;
        }

        double maxDist = cn.academy.config.AbilityConfig.stat("wide_cast", "cam_range", 0f);
        if (sp.level() != le.level() || sp.distanceToSqr(le) > maxDist * maxDist) {
            NetworkMessage.sendTo(sp, NetworkMessage.staticCaller(ProxyState.class),
                    MSG_DENY, "out_of_range");
            return;
        }

        if (MentalImmune.blocked(sp,
                cn.academy.ability.vanilla.mentalout.advanced.FreeManip.INSTANCE, le)) {
            NetworkMessage.sendTo(sp, NetworkMessage.staticCaller(ProxyState.class),
                    MSG_DENY, "reflected");
            return;
        }
        put(sp, le.getUUID(), sp.level().getGameTime());
    }

    @Listener(channel = MSG_INPUT, side = LogicalSide.SERVER)
    private static void s_input(Player owner, Float forward, Float strafe,
                                Float yaw, Float pitch, Integer flags) {
        Link link = linkOf(owner);
        if (link == null || forward == null || strafe == null || yaw == null || pitch == null) {
            return;
        }
        link.forward = forward;
        link.strafe = strafe;
        link.yaw = yaw;
        link.pitch = pitch;
        link.flags = flags == null ? 0 : flags;
        link.inputAt = owner.level().getGameTime();
    }

    @Listener(channel = MSG_ATTACK, side = LogicalSide.SERVER)
    private static void s_attack(Player owner) {
        Link link = linkOf(owner);
        if (link == null || !(owner instanceof ServerPlayer sp)) {
            return;
        }
        if (sp.level() instanceof ServerLevel sl
                && sl.getEntity(link.target) instanceof LivingEntity actor && actor.isAlive()) {
            proxyAttack(sp, actor, link);
        }
    }

    @Listener(channel = MSG_END, side = LogicalSide.SERVER)
    private static void s_end(Player owner) {
        if (owner instanceof ServerPlayer sp) {
            release(sp, "by_player");
        }
    }

    private static float dFwd, dStrafe, dYaw, dPitch;
    private static int dFlags = -1;
    private static long dAt;

    private static int dTarget = -1;

    public static boolean drivenIs(Entity e) {
        return e != null && dFlags >= 0 && e.getId() == dTarget;
    }

    public static float drivenForward() {
        return dFwd;
    }

    public static float drivenStrafe() {
        return dStrafe;
    }

    public static float drivenYaw() {
        return dYaw;
    }

    public static float drivenPitch() {
        return dPitch;
    }

    public static int drivenFlags() {
        return dFlags;
    }

    public static long drivenAt() {
        return dAt;
    }

    @Listener(channel = MSG_DRIVE, side = LogicalSide.CLIENT)
    private static void c_drive(Integer targetId, Float forward, Float strafe,
                                Float yaw, Float pitch, Integer flags) {
        dTarget = targetId == null ? -1 : targetId;
        dFwd = forward == null ? 0f : forward;
        dStrafe = strafe == null ? 0f : strafe;
        dYaw = yaw == null ? 0f : yaw;
        dPitch = pitch == null ? 0f : pitch;
        dFlags = flags == null ? -1 : flags;
        dAt = net.minecraft.Util.getMillis();
    }

    private static String pendingDeny;

    @Listener(channel = MSG_DENY, side = LogicalSide.CLIENT)
    private static void c_deny(String why) {
        pendingDeny = why == null ? "" : why;
    }

    public static String takeDeny() {
        String s = pendingDeny;
        pendingDeny = null;
        return s;
    }
}
