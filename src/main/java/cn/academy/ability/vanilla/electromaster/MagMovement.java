package cn.academy.ability.vanilla.electromaster;

import cn.academy.util.AimTrace;
import cn.academy.ACSounds;
import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientContext;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.ContextManager;
import cn.academy.ability.context.DelegateState;
import cn.academy.ability.context.KeyDelegate;
import cn.academy.ability.context.RegClientContext;
import cn.academy.client.render.MagLimbBones;
import cn.academy.util.RayReflect;
import cn.academy.client.render.entity.ACEffectEntities;
import cn.academy.client.render.util.ArcPatterns;
import cn.academy.client.sound.FollowEntitySound;
import cn.academy.entity.EntityArc;
import cn.academy.gravity.ACGravity;
import cn.academy.gravity.GravityEntity;
import cn.academy.gravity.RotationUtil;
import cn.academy.network.GravitySyncMessage;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import cn.academy.config.AbilityConfig;
import static cn.lambdalib2.util.MathUtils.lerpf;

@Mod.EventBusSubscriber(modid = "academy", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MagMovement extends Skill {

    public static final MagMovement INSTANCE = new MagMovement();

    static final Set<UUID> FALL_IMMUNE = ConcurrentHashMap.newKeySet();

    public static boolean isControllingMotion(UUID playerId) {
        return FALL_IMMUNE.contains(playerId);
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent evt) {

        if (FALL_IMMUNE.contains(evt.getEntity().getUUID())) {
            evt.setCanceled(true);
        }
    }

    public static final double ACCEL = 0.08, VELOCITY = 1.0;

    private static final double[] TARGET_DIST = {10, 25, 40, 80, 100};

    static double maxDistance(Player player) {
        int lv = Mth.clamp(cn.academy.datapart.AbilityData.get(player).getLevel(), 1, 5);
        return TARGET_DIST[lv - 1];
    }

    public static final double LEASH_XZ = 1.0, LEASH_Y = 2.0;

    public static final double HOVER_SPEED = 0.12;

    public static final double CLIMB_SPEED = 0.3;

    private static int clampTicks(float v, int fallback) {
        return Math.max(1, v > 0 ? (int) v : fallback);
    }

    @OnlyIn(Dist.CLIENT)
    static int chargeTicks() {

        return clampTicks(AbilityConfig.stat("mag_movement", "charge_time", 0f), 4);
    }

    @OnlyIn(Dist.CLIENT)
    static int rechargeTicks() {
        return clampTicks(AbilityConfig.stat("mag_movement", "recharge_time", 0f), 10);
    }

    public static final double GRAV_PULL_SPEED = 1.2;

    public static final double GRAV_ARRIVE_DIST = 0.6;

    public static final int GRAV_STRIKE_INTERVAL = 5;

    public static final int GRAV_STRIKE_LIFE = 9;

    public static final int GRAV_STRIKE_DROP = 5;

    public static final double GRAV_STRIKE_DEV = 0.25;

    public static final double GRAV_SPRING_K = 0.10;

    public static final double GRAV_SPRING_DAMP = 0.12;

    public static final double FLING_ACCEL = 0.12;

    public static final double FLING_ACCEL_GAIN = 0.30;

    public static final double FLING_MAX_VEL = 4.0;

    public static final double PULL_GAIN = 0.6;

    public static final double GRAB_DIST_MIN = 2.0;

    public static final double GRAB_DIST_MAX = 80.0;

    public static final float REFLECT_DIFFICULTY = 0.3f;

    public static final double SCROLL_STEP = 2.0;

    public static final int STABLE_RESET_TICKS = 10;

    public static final double HOLD_STABLE_EPS = 0.1;

    public static final double FALL_SAFE = 3.0;

    public static final int GRAV_EXP_CD = 100;

    public static final float GRAV_EXP_GAIN = 0.01f;

    public static final float GRAV_EXP_CHANCE_MAX = 0.80f, GRAV_EXP_CHANCE_MIN = 0.20f;

    public MagMovement() {
        super("mag_movement", 2);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void activate(ClientRuntime rt, int keyID) {
        rt.addKey(keyID, new ToggleDelegate());
    }

    @OnlyIn(Dist.CLIENT)
    static BlockHitResult computeTarget(Player player) {
        Vec3 eye = player.getEyePosition(1.0f);
        Vec3 look = player.getViewVector(1.0f);
        Vec3 rayEnd = eye.add(look.scale(maxDistance(player)));

        BlockHitResult block = player.level().clip(new ClipContext(
                eye, rayEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (block.getType() != HitResult.Type.BLOCK) return null;
        if (player.level().getBlockState(block.getBlockPos()).isAir()) return null;
        return block;
    }

    static boolean isValidGravityAnchor(net.minecraft.world.level.Level level, BlockHitResult hit) {
        net.minecraft.world.phys.shapes.VoxelShape shape =
                level.getBlockState(hit.getBlockPos()).getCollisionShape(level, hit.getBlockPos());
        if (shape.isEmpty()) return false;
        AABB b = shape.bounds();
        return switch (hit.getDirection().getAxis()) {
            case X -> (b.maxY - b.minY) >= 0.5 && (b.maxZ - b.minZ) >= 0.5;
            case Y -> (b.maxX - b.minX) >= 0.5 && (b.maxZ - b.minZ) >= 0.5;
            case Z -> (b.maxX - b.minX) >= 0.5 && (b.maxY - b.minY) >= 0.5;
        };
    }

    @OnlyIn(Dist.CLIENT)
    static LivingEntity computeEntityTarget(Player player, double reach) {
        Vec3 eye = player.getEyePosition(1.0f);
        Vec3 look = player.getViewVector(1.0f);
        Vec3 end = eye.add(look.scale(reach));
        EntityHitResult ehr = AimTrace.firstResult(player.level(), player, eye, end,
                e -> e instanceof LivingEntity && e.isAlive() && e != player && e.isPickable());
        if (ehr != null && ehr.getEntity() instanceof LivingEntity le) return le;
        return null;
    }

    static double tryAdjust(double from, double to) {
        return tryAdjust(from, to, ACCEL);
    }

    static double tryAdjust(double from, double to, double accel) {
        double d = to - from;
        if (Math.abs(d) < accel) return to;
        return d > 0 ? from + accel : from - accel;
    }

    @OnlyIn(Dist.CLIENT)
    static boolean gravityUnlocked(Player player) {
        return cn.academy.datapart.AbilityData.get(player).isSkillLearned(MagFieldControl.INSTANCE);
    }

    @OnlyIn(Dist.CLIENT)
    private static class ToggleDelegate extends KeyDelegate {

        private Context context;
        private boolean charging = false;
        private boolean chargeEnabled = false;

        private int chargeNeed = 8;
        private int holdTicks = 0;
        private boolean committed = false;
        private MovementContext recharge;

        @Override
        public void onKeyDown() {
            checkContext();
            charging = false; committed = false; holdTicks = 0; recharge = null;

            chargeEnabled = gravityUnlocked(getPlayer());
            MovementContext alive = context instanceof MovementContext mc ? mc : null;
            if (alive == null) {
                alive = ContextManager.instance.findLocal(MovementContext.class).orElse(null);
            }
            if (alive != null) {

                if (chargeEnabled && !alive.flingMode) {
                    recharge = alive;
                    context = alive;
                    charging = true;

                    chargeNeed = rechargeTicks();
                    return;
                }
                alive.terminate();
                context = null;
                committed = true;
                return;
            }
            charging = true;
            chargeNeed = chargeTicks();
        }

        @Override
        public void onKeyTick() {
            checkContext();

            if (charging && !committed && chargeEnabled) {
                if (++holdTicks >= chargeNeed) {
                    committed = true; charging = false;
                    if (recharge != null) {
                        if (recharge.getStatus() != Context.Status.TERMINATED) chargeSwitch(recharge);
                        recharge = null;
                    } else {
                        openGravity();
                    }
                }
            }
        }

        @Override
        public void onKeyUp() {
            checkContext();
            if (charging && !committed) {
                committed = true; charging = false;
                if (recharge != null) {
                    if (recharge.getStatus() != Context.Status.TERMINATED) recharge.terminate();
                    context = null;
                    recharge = null;
                } else {
                    openToggle();
                }
            }
            charging = false; recharge = null;
        }

        private void chargeSwitch(MovementContext mc) {
            Player p = getPlayer();
            BlockHitResult hit = computeTarget(p);
            if (hit == null) return;
            if (!isValidGravityAnchor(p.level(), hit)) return;
            if (mc.gravityMode) {
                mc.clientRetarget(hit.getLocation(), hit.getDirection());
            } else {
                mc.terminate();
                MovementContext nc = new MovementContext(p, hit.getLocation(), hit.getDirection());
                ContextManager.instance.activate(nc);
                context = nc;
            }
        }

        private void openToggle() {
            Player p = getPlayer();
            BlockHitResult hit = computeTarget(p);
            double reach = hit != null
                    ? p.getEyePosition(1.0f).distanceTo(hit.getLocation()) : maxDistance(p);
            LivingEntity ent = computeEntityTarget(p, reach);
            if (ent != null) {
                MovementContext mc = new MovementContext(p, ent.getId());
                ContextManager.instance.activate(mc);
                context = mc;
                return;
            }
            if (hit == null) return;
            boolean ground = hit.getDirection() == Direction.UP;
            MovementContext mc = new MovementContext(p, hit.getLocation(), ground);
            ContextManager.instance.activate(mc);
            context = mc;
        }

        private void openGravity() {
            Player p = getPlayer();
            BlockHitResult hit = computeTarget(p);
            if (hit == null) return;
            if (!isValidGravityAnchor(p.level(), hit)) return;
            MovementContext mc = new MovementContext(p, hit.getLocation(), hit.getDirection());
            ContextManager.instance.activate(mc);
            context = mc;
        }

        private void checkContext() {
            if (context != null && context.getStatus() == Context.Status.TERMINATED) {
                context = null;
            }
        }

        @Override
        public DelegateState getState() {
            checkContext();
            if (context != null) return DelegateState.ACTIVE;
            return ContextManager.instance.findLocal(MovementContext.class).isPresent()
                    ? DelegateState.ACTIVE : DelegateState.IDLE;
        }

        @Override
        public ResourceLocation getIcon() { return INSTANCE.getHintIcon(); }

        @Override
        public int createID() { return 0; }

        @Override
        public Skill getSkill() { return INSTANCE; }
    }

    public static class MovementContext extends Context<MagMovement> {

        static final String MSG_EFFECT_START = "effect_start";
        static final String MSG_EFFECT_UPDATE = "effect_update";
        static final String MSG_SET_TARGET = "set_target";
        static final String MSG_SET_DISTANCE = "set_dist";
        static final String MSG_ARRIVE = "arrive_grav";
        static final String MSG_GRAV_START = "grav_start";
        static final String MSG_STRIKE = "grav_strike";
        static final String MSG_RETARGET = "grav_retarget";
        static final String MSG_DECLARE = "state_declare";
        static final String MSG_GAIN_EXP = "gain_grav_exp";

        private static final java.lang.reflect.Field JUMPING_FIELD = resolveJumping();
        private static java.lang.reflect.Field resolveJumping() {
            try {
                java.lang.reflect.Field f =
                        net.minecraft.world.entity.LivingEntity.class.getDeclaredField("jumping");
                f.setAccessible(true);
                return f;
            } catch (Exception e) {
                cn.academy.AcademyCraft.LOGGER.warn("[MagMovement] LivingEntity.jumping not found, hover jump will not work", e);
                return null;
            }
        }

        private boolean canSpawnEffect = false;
        private double mox, moy, moz;
        private final double sx, sy, sz;

        private boolean hasTarget = false;
        private double tx, ty, tz;

        private boolean groundAnchor = false;

        private boolean gravityMode = false;

        private Direction gravFace = Direction.UP;

        private boolean gravArrived = false;

        private boolean gravSettled = false;

        private int gravSettlePulls = 0;

        private boolean gravSnapped = false;

        private boolean gravApproached = false;

        private boolean gravPoseSent = false;

        private Vec3 gravSpringAnchor = null;

        private int gravExpCdTicks = 0;

        @OnlyIn(Dist.CLIENT)
        static Vec3 academy$footSurface(Player player, Direction g) {
            Vec3 feet = player.position();
            for (int k = 0; k < GRAV_STRIKE_DROP; k++) {
                double d = 0.5 + k;
                BlockPos bp = BlockPos.containing(
                        feet.x + g.getStepX() * d, feet.y + g.getStepY() * d, feet.z + g.getStepZ() * d);
                if (!player.level().getBlockState(bp).getCollisionShape(player.level(), bp).isEmpty()) {
                    Vec3 c = Vec3.atCenterOf(bp);
                    return new Vec3(c.x - g.getStepX() * 0.5, c.y - g.getStepY() * 0.5, c.z - g.getStepZ() * 0.5);
                }
            }
            return null;
        }

        private boolean gravityChanged = false;

        private boolean flingMode = false;

        private int targetEntityId = -1;

        private boolean needSendTarget = false;

        private LivingEntity grabbed;

        private double smx, smy, smz;

        private double grabDistance;

        private double clientGrabDistance = GRAB_DIST_MIN;

        private int refX, refY, refZ, stableTicks;

        private double prevHoldX, prevHoldY, prevHoldZ;

        private double maxDist;

        private boolean wasColliding;

        private boolean prevNoGravity, prevNoAi;

        private DamageSource fallSource;

        private final float exp = ctx.getSkillExp();
        private final float cp = AbilityConfig.cp("mag_movement", exp);
        private final float overload = AbilityConfig.overload("mag_movement", exp);
        private float overloadKeep = 0f;

        public MovementContext(Player player) {
            super(player, INSTANCE);
            sx = player.getX(); sy = player.getY(); sz = player.getZ();
        }

        public MovementContext(Player player, Vec3 target, boolean groundAnchor) {
            this(player);
            hasTarget = true;
            tx = target.x; ty = target.y; tz = target.z;
            this.groundAnchor = groundAnchor;
        }

        public MovementContext(Player player, Vec3 target, Direction hitFace) {
            this(player);
            gravityMode = true;
            tx = target.x; ty = target.y; tz = target.z;
            this.gravFace = hitFace;
        }

        public MovementContext(Player player, int entityId) {
            this(player);
            flingMode = true;
            targetEntityId = entityId;
            needSendTarget = true;
            Entity e = player.level().getEntity(entityId);
            clientGrabDistance = e == null ? GRAB_DIST_MIN : Mth.clamp(
                    player.getEyePosition(1.0f).distanceTo(e.position().add(0, e.getBbHeight() * 0.5, 0)),
                    GRAB_DIST_MIN, GRAB_DIST_MAX);
        }

        @Listener(channel = MSG_MADEALIVE, side = {LogicalSide.SERVER, LogicalSide.CLIENT})
        private void g_onStart() {
            ctx.consume(overload, 0);
            overloadKeep = ctx.cpData.getOverload();
            if (!isRemote()) {
                FALL_IMMUNE.add(player.getUUID());
            }
            if (isRemote() && isLocal()) {
                if (!hasTarget && !flingMode && !gravityMode) { terminate(); return; }
                canSpawnEffect = true;

                if (gravityMode) {
                    sendToServer(MSG_DECLARE, cn.academy.datapart.MagStateData.MODE_GRAVITY,
                            tx, ty, tz, gravFace.get3DDataValue());
                } else if (hasTarget && !flingMode) {
                    sendToServer(MSG_DECLARE, cn.academy.datapart.MagStateData.MODE_ANCHOR,
                            tx, ty, tz, groundAnchor ? 1 : 0);
                }
            }
        }

        @Listener(channel = MSG_DECLARE, side = LogicalSide.SERVER)
        private void s_declare(int mode, double x, double y, double z, int aux) {
            cn.academy.datapart.MagStateData.of(player).declare(mode, x, y, z, aux);
            s_dismountForSelfMotion();
        }

        private void s_dismountForSelfMotion() {
            if (player.isPassenger()) {
                player.stopRiding();
            }
        }

        @Listener(channel = MSG_EFFECT_START, side = LogicalSide.SERVER)
        private void s_relayStart() {
            sendToClient(MSG_EFFECT_START);
        }

        @Listener(channel = MSG_EFFECT_UPDATE, side = LogicalSide.SERVER)
        private void s_relayUpdate(double x, double y, double z) {
            sendToClient(MSG_EFFECT_UPDATE, x, y, z);
        }

        @Listener(channel = MSG_STRIKE, side = LogicalSide.SERVER)
        private void s_relayStrike() {
            sendToClient(MSG_STRIKE);
        }

        @Listener(channel = MSG_RETARGET, side = LogicalSide.SERVER)
        private void s_relayRetarget() {
            sendToClient(MSG_RETARGET);
        }

        @Listener(channel = MSG_GAIN_EXP, side = LogicalSide.SERVER)
        private void s_gainGravExp() {
            cn.academy.datapart.AbilityData data = cn.academy.datapart.AbilityData.get(player);
            if (!data.isSkillLearned(MagFieldControl.INSTANCE)) return;
            float cur = data.getSkillExp(MagFieldControl.INSTANCE);
            if (cur >= 1.0f) return;
            data.setSkillExp(MagFieldControl.INSTANCE, Math.min(1.0f, cur + GRAV_EXP_GAIN));
        }

        @Listener(channel = MSG_SET_TARGET, side = LogicalSide.SERVER)
        private void s_setTarget(int entityId, double dist) {
            if (grabbed != null) return;
            Entity e = player.level().getEntity(entityId);
            if (!(e instanceof LivingEntity le) || !le.isAlive()) { terminate(); return; }
            grabbed = le;
            flingMode = true;
            targetEntityId = entityId;
            FALL_IMMUNE.remove(player.getUUID());
            FALL_IMMUNE.add(le.getUUID());
            prevNoGravity = le.isNoGravity();
            le.setNoGravity(true);
            if (le instanceof Mob mob) { prevNoAi = mob.isNoAi(); mob.setNoAi(true); }

            Vec3 v = le.getDeltaMovement();
            smx = v.x; smy = v.y; smz = v.z;
            refX = Mth.floor(le.getX());
            refY = Mth.floor(le.getY());
            refZ = Mth.floor(le.getZ());
            stableTicks = 0;
            maxDist = 0;
            wasColliding = false;
            grabDistance = Mth.clamp(dist, GRAB_DIST_MIN, GRAB_DIST_MAX);
            Vec3 hold0 = player.getEyePosition(1.0f).add(player.getViewVector(1.0f).scale(grabDistance));
            prevHoldX = hold0.x; prevHoldY = hold0.y; prevHoldZ = hold0.z;
            fallSource = new DamageSource(
                    player.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                            .getHolderOrThrow(DamageTypes.FALL),
                    player);
        }

        @Listener(channel = MSG_SET_DISTANCE, side = LogicalSide.SERVER)
        private void s_setDistance(double dist) {
            grabDistance = Mth.clamp(dist, GRAB_DIST_MIN, GRAB_DIST_MAX);
        }

        private Vec3 academy$clampedStand() {
            BlockPos bp = BlockPos.containing(tx - gravFace.getStepX() * 0.5,
                    ty - gravFace.getStepY() * 0.5, tz - gravFace.getStepZ() * 0.5);
            double cx = tx, cy = ty, cz = tz;
            switch (gravFace.getAxis()) {
                case X -> { cy = Mth.clamp(cy, bp.getY() + 0.3, bp.getY() + 0.7);
                            cz = Mth.clamp(cz, bp.getZ() + 0.3, bp.getZ() + 0.7); }
                case Y -> { cx = Mth.clamp(cx, bp.getX() + 0.3, bp.getX() + 0.7);
                            cz = Mth.clamp(cz, bp.getZ() + 0.3, bp.getZ() + 0.7); }
                case Z -> { cx = Mth.clamp(cx, bp.getX() + 0.3, bp.getX() + 0.7);
                            cy = Mth.clamp(cy, bp.getY() + 0.3, bp.getY() + 0.7); }
            }
            return new Vec3(cx, cy, cz);
        }

        private boolean academy$standPoseFits() {
            Vec3 p = academy$clampedStand();
            double nx = gravFace.getStepX(), ny = gravFace.getStepY(), nz = gravFace.getStepZ();

            double x1 = p.x + (nx == 0 ? -0.3 : 0), x2 = p.x + (nx == 0 ? 0.3 : nx * 2.0);
            double y1 = p.y + (ny == 0 ? -0.3 : 0), y2 = p.y + (ny == 0 ? 0.3 : ny * 2.0);
            double z1 = p.z + (nz == 0 ? -0.3 : 0), z2 = p.z + (nz == 0 ? 0.3 : nz * 2.0);
            AABB pose = new AABB(Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2),
                    Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2)).deflate(1.0E-4);
            return player.level().noCollision(player, pose);
        }

        @Listener(channel = MSG_GRAV_START, side = LogicalSide.SERVER)
        private void s_gravStart() {
            player.setForcedPose(net.minecraft.world.entity.Pose.SWIMMING);
        }

        @Listener(channel = MSG_ARRIVE, side = LogicalSide.SERVER)
        private void s_arrive(int faceIndex) {
            player.setForcedPose(null);

            Direction grav = Direction.from3DDataValue(faceIndex).getOpposite();
            if (ACGravity.getGravityDirection(player) != grav) {
                ACGravity.setGravityDirection(player, grav, true);
                GravitySyncMessage.sync(player, grav, true);
            }
            gravityChanged = true;
        }

        @OnlyIn(Dist.CLIENT)
        void clientRetarget(Vec3 hit, Direction face) {
            tx = hit.x; ty = hit.y; tz = hit.z;
            gravFace = face;
            gravArrived = false;
            gravSettled = false;
            gravSettlePulls = 0;
            gravSnapped = false;
            gravApproached = false;
            gravPoseSent = false;
            gravSpringAnchor = null;
            sendToServer(MSG_RETARGET);

            sendToServer(MSG_DECLARE, cn.academy.datapart.MagStateData.MODE_GRAVITY,
                    tx, ty, tz, face.get3DDataValue());
        }

        @OnlyIn(Dist.CLIENT)
        void clientAdjustDistance(double scrollDelta) {
            if (!flingMode) return;
            clientGrabDistance = Mth.clamp(
                    clientGrabDistance + scrollDelta * SCROLL_STEP, GRAB_DIST_MIN, GRAB_DIST_MAX);
            sendToServer(MSG_SET_DISTANCE, clientGrabDistance);
        }

        @Listener(channel = MSG_TICK, side = LogicalSide.CLIENT)
        private void c_onTick() {
            if (!isLocal()) return;
            if (ctx.cpData.getOverload() < overloadKeep) ctx.cpData.setOverload(overloadKeep);
            if (canSpawnEffect) {
                sendToServer(MSG_EFFECT_START);
                canSpawnEffect = false;
            }
            if (flingMode) {
                if (needSendTarget) {
                    sendToServer(MSG_SET_TARGET, targetEntityId, clientGrabDistance);
                    needSendTarget = false;
                }
                Entity e = player.level().getEntity(targetEntityId);
                if (e != null) {
                    sendToServer(MSG_EFFECT_UPDATE, e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ());
                }
                return;
            }
            if (gravityMode) {
                if (!gravArrived) {
                    sendToServer(MSG_EFFECT_UPDATE, tx, ty, tz);

                    AABB box = player.getBoundingBox();
                    double bx = Math.max(0.0, Math.max(box.minX - tx, tx - box.maxX));
                    double by = Math.max(0.0, Math.max(box.minY - ty, ty - box.maxY));
                    double bz = Math.max(0.0, Math.max(box.minZ - tz, tz - box.maxZ));
                    double boxDist = Math.sqrt(bx * bx + by * by + bz * bz);

                    if (boxDist <= GRAV_ARRIVE_DIST && academy$standPoseFits()) {
                        player.setForcedPose(null);
                        gravArrived = true;
                        sendToServer(MSG_ARRIVE, gravFace.get3DDataValue());
                    } else {

                        if (!gravPoseSent) { gravPoseSent = true; sendToServer(MSG_GRAV_START); }
                        player.setForcedPose(net.minecraft.world.entity.Pose.SWIMMING);
                        Vec3 stand = academy$clampedStand();

                        AABB bb = player.getBoundingBox();
                        double ext = switch (gravFace.getAxis()) {
                            case X -> gravFace.getStepX() > 0 ? player.getX() - bb.minX : bb.maxX - player.getX();
                            case Y -> gravFace.getStepY() > 0 ? player.getY() - bb.minY : bb.maxY - player.getY();
                            case Z -> gravFace.getStepZ() > 0 ? player.getZ() - bb.minZ : bb.maxZ - player.getZ();
                        };
                        double off = ext + 0.4;
                        Vec3 approach = new Vec3(stand.x + gravFace.getStepX() * off,
                                stand.y + gravFace.getStepY() * off, stand.z + gravFace.getStepZ() * off);
                        if (!gravApproached && player.position().distanceTo(approach) < 0.8) gravApproached = true;
                        Vec3 tgt = gravApproached ? stand : approach;
                        double dx = tgt.x - player.getX(), dy = tgt.y - player.getY(), dz = tgt.z - player.getZ();
                        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                        if (dist > 1.0E-4) {
                            double s = GRAV_PULL_SPEED / dist;

                            player.setDeltaMovement(RotationUtil.vecWorldToPlayer(
                                    new Vec3(dx * s, dy * s, dz * s), ACGravity.getGravityDirection(player)));
                        }
                        player.fallDistance = 0.0f;
                    }
                } else if (!gravSettled) {

                    Direction grav = gravFace.getOpposite();
                    boolean flipped = ACGravity.getGravityDirection(player) == grav;
                    boolean animating = player instanceof GravityEntity ge
                            && ge.academy_getRotationAnimation().isInAnimation();
                    AABB box = player.getBoundingBox();
                    double bx = Math.max(0.0, Math.max(box.minX - tx, tx - box.maxX));
                    double by = Math.max(0.0, Math.max(box.minY - ty, ty - box.maxY));
                    double bz = Math.max(0.0, Math.max(box.minZ - tz, tz - box.maxZ));
                    double boxDist = Math.sqrt(bx * bx + by * by + bz * bz);
                    boolean positive = grav.getAxisDirection() == Direction.AxisDirection.POSITIVE;
                    double planeCoord = switch (grav.getAxis()) { case X -> tx; case Y -> ty; case Z -> tz; };
                    double feetCoord = switch (grav.getAxis()) {
                        case X -> positive ? box.maxX : box.minX;
                        case Y -> positive ? box.maxY : box.minY;
                        case Z -> positive ? box.maxZ : box.minZ;
                    };
                    boolean seated = Math.abs(feetCoord - planeCoord) <= 0.05;
                    if (flipped && !animating && player.onGround() && seated
                            && boxDist <= GRAV_ARRIVE_DIST && gravSettlePulls >= 2) {
                        gravSettled = true;
                    } else {

                        if (flipped) {

                            if (!gravSnapped) {
                                Vec3 stand = academy$clampedStand();
                                double px = stand.x - grav.getStepX() * 1.0E-6;
                                double py = stand.y - grav.getStepY() * 1.0E-6;
                                double pz = stand.z - grav.getStepZ() * 1.0E-6;
                                if (player.position().distanceTo(new Vec3(px, py, pz)) <= 2.5) {
                                    gravSnapped = true;
                                    player.setPos(px, py, pz);
                                    player.setDeltaMovement(Vec3.ZERO);
                                    player.xo = px; player.yo = py; player.zo = pz;
                                    player.xOld = px; player.yOld = py; player.zOld = pz;
                                } else {

                                    Vec3 world = new Vec3(px - player.getX(), py - player.getY(), pz - player.getZ());
                                    Vec3 local = RotationUtil.vecWorldToPlayer(world, ACGravity.getGravityDirection(player));
                                    double lat = Math.sqrt(local.x * local.x + local.z * local.z);
                                    Vec3 dm = player.getDeltaMovement();
                                    if (lat > 1.0E-4) {
                                        double sp = Math.min(GRAV_PULL_SPEED, lat * 0.5) / lat;
                                        player.setDeltaMovement(local.x * sp, dm.y, local.z * sp);
                                    }
                                }
                            }

                        } else {

                            Vec3 stand = academy$clampedStand();
                            double dx = stand.x - player.getX(), dy = stand.y - player.getY(), dz = stand.z - player.getZ();
                            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                            if (dist > 1.0E-4) {
                                double speed = Math.min(GRAV_PULL_SPEED, dist * 0.5);
                                double s = speed / dist;
                                player.setDeltaMovement(RotationUtil.vecWorldToPlayer(
                                        new Vec3(dx * s, dy * s, dz * s), ACGravity.getGravityDirection(player)));
                            }
                        }
                        player.fallDistance = 0.0f;
                        gravSettlePulls++;
                    }
                }
                else {

                    if (gravExpCdTicks > 0) {
                        gravExpCdTicks--;
                    } else if (player.zza != 0 || player.xxa != 0) {
                        gravExpCdTicks = GRAV_EXP_CD;
                        float gExp = cn.academy.datapart.AbilityData.get(player).getSkillExp(MagFieldControl.INSTANCE);
                        float chance = lerpf(GRAV_EXP_CHANCE_MAX, GRAV_EXP_CHANCE_MIN, gExp);
                        if (gExp < 1.0f && player.getRandom().nextFloat() < chance) {
                            sendToServer(MSG_GAIN_EXP);
                        }
                    }

                    Direction gNow = ACGravity.getGravityDirection(player);
                    Vec3 fb = academy$footSurface(player, gNow);
                    if (fb != null) gravSpringAnchor = fb;
                    if (gravSpringAnchor != null && !player.onGround() && player.isShiftKeyDown()) {

                        Direction nDir = gNow.getOpposite();
                        Vec3 n = new Vec3(nDir.getStepX(), nDir.getStepY(), nDir.getStepZ());
                        Vec3 rel = player.position().subtract(gravSpringAnchor);
                        double h = rel.x * n.x + rel.y * n.y + rel.z * n.z;
                        Vec3 tangent = rel.subtract(n.scale(h));
                        double gate = Mth.clamp((h - 0.3) / 0.7, 0.0, 1.0);
                        Vec3 accelWorld = n.scale((0.8 - h) * GRAV_SPRING_K)
                                .add(tangent.scale(-GRAV_SPRING_K * gate));

                        Vec3 accelLocal = RotationUtil.vecWorldToPlayer(accelWorld, gNow);
                        player.setDeltaMovement(
                                player.getDeltaMovement().scale(1.0 - GRAV_SPRING_DAMP).add(accelLocal));
                        player.fallDistance = 0.0f;
                    }
                }

                if (gravArrived) sendToServer(MSG_STRIKE);
                return;
            }

            if (!hasTarget) return;

            sendToServer(MSG_EFFECT_UPDATE, tx, ty, tz);

            double px = player.getX(), py = player.getY(), pz = player.getZ();
            boolean inBox = Math.abs(px - tx) <= LEASH_XZ
                    && Math.abs(pz - tz) <= LEASH_XZ
                    && Math.abs(py - ty) <= LEASH_Y;

            if (!inBox || isFeetAir()) {
                aloftTick(px, py, pz, inBox);
            }
        }

        private void aloftTick(double px, double py, double pz, boolean inBox) {
            player.fallDistance = 0.0f;

            Vec3 mo = player.getDeltaMovement();
            if (Math.abs((mox * mox + moy * moy + moz * moz) - mo.lengthSqr()) > 0.5) {
                mox = mo.x; moy = mo.y; moz = mo.z;
            }

            double tvx, tvy, tvz;
            if (!inBox) {
                tvx = axisPull(px, tx, LEASH_XZ);
                tvy = axisPull(py, ty, LEASH_Y);
                tvz = axisPull(pz, tz, LEASH_XZ);

                if (player.horizontalCollision && (tvx != 0 || tvz != 0) && py < ty + LEASH_Y) {
                    tvy = Math.max(tvy, CLIMB_SPEED);
                }
            } else {

                float strafe = player.xxa, forward = player.zza;
                double yawRad = Math.toRadians(player.getYRot());
                double sin = Math.sin(yawRad), cos = Math.cos(yawRad);
                double wx = strafe * cos - forward * sin;
                double wz = forward * cos + strafe * sin;
                double len = Math.sqrt(wx * wx + wz * wz);
                if (len > 1e-4) { tvx = wx / len * HOVER_SPEED; tvz = wz / len * HOVER_SPEED; }
                else { tvx = 0; tvz = 0; }
                if (groundAnchor) {

                    tvy = -VELOCITY;
                } else {
                    tvy = isJumping() ? HOVER_SPEED : 0.0;
                }
            }

            mox = tryAdjust(mox, tvx);
            moy = tryAdjust(moy, tvy);
            moz = tryAdjust(moz, tvz);
            player.setDeltaMovement(mox, moy, moz);
        }

        private double axisPull(double p, double t, double leash) {
            double d = t - p;
            if (Math.abs(d) <= leash) return 0.0;
            return d > 0 ? VELOCITY : -VELOCITY;
        }

        private boolean isFeetAir() {
            BlockPos below = player.blockPosition().below();
            return player.level().getBlockState(below).isAir();
        }

        private boolean isJumping() {
            if (JUMPING_FIELD == null) return false;
            try { return JUMPING_FIELD.getBoolean(player); } catch (Exception e) { return false; }
        }

        @Listener(channel = MSG_TICK, side = LogicalSide.SERVER)
        private void s_onTick() {

            if (!ctx.cpData.canUseAbility() || !ctx.consume(0, cp)) {
                terminate();
                return;
            }
            if (flingMode) {
                if (grabbed == null) return;
                if (!grabbed.isAlive() || grabbed.isRemoved()) { terminate(); return; }
                if (tryEscape()) { terminate(); return; }
                flingTick();
            }
        }

        private boolean tryEscape() {
            Vec3 from = player.getEyePosition(1.0f);
            Vec3 to = grabbed.getEyePosition(1.0f);
            Vec3 d = to.subtract(from);

            final Vec3 dir = d.lengthSqr() < 1.0e-6 ? player.getViewVector(1.0f) : d.normalize();
            return ctx.tryReflect(grabbed, ev -> {

                RayReflect.fill(ev, from, dir, grabbed, 0);
                ev.difficulty = REFLECT_DIFFICULTY;
                ev.deflectable = false;
            });
        }

        private void flingTick() {
            Vec3 eye = player.getEyePosition(1.0f);
            Vec3 look = player.getViewVector(1.0f);
            Vec3 hold = eye.add(look.scale(grabDistance));
            double halfH = grabbed.getBbHeight() * 0.5;
            double dx = hold.x - grabbed.getX();
            double dy = (hold.y - halfH) - grabbed.getY();
            double dz = hold.z - grabbed.getZ();
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

            double tvx = 0, tvy = 0, tvz = 0;
            if (dist > 1e-4) {
                double speed = Math.min(FLING_MAX_VEL, dist * PULL_GAIN);
                tvx = dx / dist * speed;
                tvy = dy / dist * speed;
                tvz = dz / dist * speed;
            }

            double accel = FLING_ACCEL + dist * FLING_ACCEL_GAIN;
            smx = tryAdjust(smx, tvx, accel);
            smy = tryAdjust(smy, tvy, accel);
            smz = tryAdjust(smz, tvz, accel);

            grabbed.move(MoverType.SELF, new Vec3(smx, smy, smz));
            grabbed.setDeltaMovement(smx, smy, smz);
            grabbed.hurtMarked = true;

            int cx = Mth.floor(grabbed.getX());
            int cy = Mth.floor(grabbed.getY());
            int cz = Mth.floor(grabbed.getZ());

            double curDist = Math.sqrt((double) (cx - refX) * (cx - refX)
                    + (double) (cy - refY) * (cy - refY) + (double) (cz - refZ) * (cz - refZ));
            if (curDist > maxDist) maxDist = curDist;

            double holdMove = Math.abs(hold.x - prevHoldX) + Math.abs(hold.y - prevHoldY) + Math.abs(hold.z - prevHoldZ);
            prevHoldX = hold.x; prevHoldY = hold.y; prevHoldZ = hold.z;
            if (holdMove < HOLD_STABLE_EPS) {
                if (++stableTicks >= STABLE_RESET_TICKS) { refX = cx; refY = cy; refZ = cz; maxDist = 0; }
            } else {
                stableTicks = 0;
            }
            boolean colliding = grabbed.horizontalCollision || grabbed.verticalCollision;
            if (colliding) {
                if (!wasColliding) {
                    int dmg = (int) Math.ceil(maxDist - FALL_SAFE);
                    if (dmg > 0) {
                        grabbed.invulnerableTime = 0;

                        if (ctx.canTarget(grabbed)) {
                            grabbed.hurt(fallSource, dmg);
                        }
                        ctx.addSkillExp(0.002f * dmg);
                    }
                }

                refX = cx; refY = cy; refZ = cz; maxDist = 0;
            }
            wasColliding = colliding;
            grabbed.fallDistance = 0f;
        }

        @Listener(channel = MSG_TERMINATED, side = LogicalSide.CLIENT)
        private void c_onEnd() {
            if (isLocal() && gravityMode) player.setForcedPose(null);
        }

        @Listener(channel = MSG_TERMINATED, side = LogicalSide.SERVER)
        private void s_onEnd() {

            cn.academy.datapart.MagStateData.of(player).clearState();
            player.setForcedPose(null);
            FALL_IMMUNE.remove(player.getUUID());
            if (gravityChanged) {
                ACGravity.setGravityDirection(player, Direction.DOWN, true);
                GravitySyncMessage.sync(player, Direction.DOWN, true);
                gravityChanged = false;
            }
            if (grabbed != null) {
                FALL_IMMUNE.remove(grabbed.getUUID());
                grabbed.setNoGravity(prevNoGravity);
                if (grabbed instanceof Mob mob) mob.setNoAi(prevNoAi);
                grabbed.fallDistance = 0.0f;
                grabbed = null;
            }
            double dist = Math.sqrt((player.getX() - sx) * (player.getX() - sx)
                    + (player.getY() - sy) * (player.getY() - sy)
                    + (player.getZ() - sz) * (player.getZ() - sz));
            ctx.addSkillExp(Math.max(0.005f, 0.0011f * (float) dist));
            player.fallDistance = 0.0f;
        }
    }

    @OnlyIn(Dist.CLIENT)
    @RegClientContext(MovementContext.class)
    public static class MovementContextC extends ClientContext {

        private static final double[][] LIMBS = {
                { 0.32, 1.05},
                {-0.32, 1.05},
                { 0.13, 0.10},
                {-0.13, 0.10},
        };

        private EntityArc[] arcs;
        private FollowEntitySound sound;

        private int strikeGen = 0;

        private Vec3 strikeLast = null;

        private boolean strikeStarted = false;

        private final java.util.List<EntityArc> strikeArcs = new java.util.ArrayList<>();

        public MovementContextC(MovementContext par) {
            super(par);
        }

        private void academy$spawnPullArcs() {
            arcs = new EntityArc[LIMBS.length];
            for (int i = 0; i < arcs.length; i++) {
                EntityArc a = new EntityArc(player, ArcPatterns.thinContiniousArc);
                a.lengthFixed = false;
                a.texWiggle = 1;
                a.showWiggle = 0.1;
                a.hideWiggle = 0.6;
                a.boneIndex = i;

                a.viewOptimize = false;
                ACEffectEntities.spawn(a);
                arcs[i] = a;
            }
        }

        @Listener(channel = MovementContext.MSG_EFFECT_START, side = LogicalSide.CLIENT)
        private void c_startEffect() {
            academy$spawnPullArcs();

            MagLimbBones.setActive(player.getUUID());

            sound = new FollowEntitySound(ACSounds.EM_MOVE_LOOP.get(), player, 1.0f);
            Minecraft.getInstance().getSoundManager().play(sound);
        }

        @Listener(channel = MovementContext.MSG_RETARGET, side = LogicalSide.CLIENT)
        private void c_retarget() {
            strikeStarted = false;
            strikeGen = 0;
            strikeLast = null;
            if (arcs == null) academy$spawnPullArcs();
            if (sound != null) sound.requestStop();
            sound = new FollowEntitySound(ACSounds.EM_MOVE_LOOP.get(), player, 1.0f);
            Minecraft.getInstance().getSoundManager().play(sound);
        }

        private Vec3 academy$limbStart(int i) {
            Direction g = ACGravity.getGravityDirection(player);
            double yawRad = Math.toRadians(player.yBodyRot);
            double latX = Math.cos(yawRad), latZ = Math.sin(yawRad);
            Vec3 world = RotationUtil.vecPlayerToWorld(latX * LIMBS[i][0], LIMBS[i][1], latZ * LIMBS[i][0], g);
            return new Vec3(player.getX() + world.x, player.getY() + world.y, player.getZ() + world.z);
        }

        private void academy$aimArc(int i, double ex, double ey, double ez) {
            Vec3 s = academy$limbStart(i);

            arcs[i].xOld = arcs[i].getX();
            arcs[i].yOld = arcs[i].getY();
            arcs[i].zOld = arcs[i].getZ();
            arcs[i].setFromTo(s.x, s.y, s.z, ex, ey, ez);
        }

        @Listener(channel = MovementContext.MSG_EFFECT_UPDATE, side = LogicalSide.CLIENT)
        private void c_updateEffect(double x, double y, double z) {
            if (arcs == null) return;
            for (int i = 0; i < arcs.length; i++) {
                if (arcs[i] == null) continue;
                academy$aimArc(i, x, y, z);
            }
        }

        @Listener(channel = MovementContext.MSG_STRIKE, side = LogicalSide.CLIENT)
        private void c_strike() {

            if (!strikeStarted) {
                strikeStarted = true;
                if (arcs != null) {
                    for (EntityArc a : arcs) if (a != null) a.discard();
                    arcs = null;
                }
            }
            if (strikeGen++ % GRAV_STRIKE_INTERVAL != 0) return;

            Direction g = ACGravity.getGravityDirection(player);
            Vec3 base = academy$footStrikePoint(g);
            if (base == null) return;

            strikeArcs.removeIf(a -> {
                if (a == null || a.isRemoved()) return true;
                if (a.life >= 0 && (cn.lambdalib2.util.GameTimer.getPausableTime() - a.spawnTime) * 20.0
                        > a.life + 10) {
                    a.discard();
                    return true;
                }
                return false;
            });

            Vec3 t1, t2;
            switch (g.getAxis()) {
                case X -> { t1 = new Vec3(0, 1, 0); t2 = new Vec3(0, 0, 1); }
                case Y -> { t1 = new Vec3(1, 0, 0); t2 = new Vec3(0, 0, 1); }
                default -> { t1 = new Vec3(1, 0, 0); t2 = new Vec3(0, 1, 0); }
            }
            for (int i = 0; i < LIMBS.length; i++) {
                double d1 = (player.getRandom().nextDouble() - 0.5) * 2.0 * GRAV_STRIKE_DEV;
                double d2 = (player.getRandom().nextDouble() - 0.5) * 2.0 * GRAV_STRIKE_DEV;
                academy$spawnBolt(i,
                        base.x + t1.x * d1 + t2.x * d2,
                        base.y + t1.y * d1 + t2.y * d2,
                        base.z + t1.z * d1 + t2.z * d2);
            }
        }

        private void academy$spawnBolt(int limbIndex, double ex, double ey, double ez) {
            EntityArc a = new EntityArc(player, ArcPatterns.thinContiniousArc);
            a.lengthFixed = false;
            a.texWiggle = 1;
            a.viewOptimize = false;
            a.showWiggle = 0.0;
            a.hideWiggle = 1.0;
            a.boneIndex = limbIndex;
            a.life = GRAV_STRIKE_LIFE;
            a.fade = true;
            Vec3 s = academy$limbStart(limbIndex);
            a.setFromTo(s.x, s.y, s.z, ex, ey, ez);
            ACEffectEntities.spawn(a);
            strikeArcs.add(a);
        }

        private Vec3 academy$footStrikePoint(Direction g) {
            Vec3 fb = MovementContext.academy$footSurface(player, g);
            if (fb != null) strikeLast = fb;
            return strikeLast;
        }

        @Listener(channel = MSG_TERMINATED, side = LogicalSide.CLIENT)
        private void c_endEffect() {
            if (arcs != null) for (EntityArc a : arcs) if (a != null) a.discard();
            for (EntityArc a : strikeArcs) if (a != null && !a.isRemoved()) a.discard();
            strikeArcs.clear();
            if (sound != null) sound.requestStop();
            MagLimbBones.clearActive(player.getUUID());
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Mod.EventBusSubscriber(modid = "academy", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class RestoreHandler {
        private static int cooldown = 0;

        @SubscribeEvent
        public static void onClientTick(net.minecraftforge.event.TickEvent.ClientTickEvent e) {
            if (e.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) { cooldown = 0; return; }
            if (cooldown > 0) { cooldown--; return; }

            if (cn.lambdalib2.datapart.EntityData.get(mc.player) == null) return;

            cn.academy.datapart.MagStateData st = cn.academy.datapart.MagStateData.of(mc.player);
            if (st.getMode() == cn.academy.datapart.MagStateData.MODE_NONE) return;
            if (ContextManager.instance.findLocal(MovementContext.class).isPresent()) return;
            if (!cn.academy.datapart.AbilityData.get(mc.player).hasCategory()) return;
            Vec3 t = st.getTarget();
            MovementContext ctx = st.getMode() == cn.academy.datapart.MagStateData.MODE_GRAVITY
                    ? new MovementContext(mc.player, t, Direction.from3DDataValue(st.getAux()))
                    : new MovementContext(mc.player, t, st.getAux() != 0);
            ContextManager.instance.activate(ctx);
            cooldown = 100;
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Mod.EventBusSubscriber(modid = "academy", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ClientScrollHandler {
        @SubscribeEvent
        public static void onScroll(InputEvent.MouseScrollingEvent evt) {
            Optional<MovementContext> ctx = ContextManager.instance.findLocal(MovementContext.class);
            if (ctx.isPresent() && ctx.get().flingMode) {
                ctx.get().clientAdjustDistance(evt.getScrollDelta());
                evt.setCanceled(true);
            }
        }
    }
}
