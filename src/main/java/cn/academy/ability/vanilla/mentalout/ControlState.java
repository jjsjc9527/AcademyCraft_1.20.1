package cn.academy.ability.vanilla.mentalout;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class ControlState {

    public enum Command {

        ATTACK(true, true, true, false, false),

        MOVE(true, false, true, false, true),

        FOLLOW(false, false, true, false, true),

        PATROL(true, false, false, false, true),

        STAY(false, false, false, true, false),

        STOP(false, false, false, false, false),

        RESTORE(false, false, false, false, false);

        private static final Command[] VALUES = values();

        private final boolean second;
        private final boolean secondEntity;
        private final boolean resumable;
        private final boolean root;
        private final boolean timed;

        Command(boolean second, boolean secondEntity, boolean resumable, boolean root,
                boolean timed) {
            this.second = second;
            this.secondEntity = secondEntity;
            this.resumable = resumable;
            this.root = root;
            this.timed = timed;
        }

        public Command next() {
            return VALUES[(ordinal() + 1) % VALUES.length];
        }

        public static Command byId(int id) {
            return VALUES[Math.floorMod(id, VALUES.length)];
        }

        public boolean needsSecondPick() {
            return second;
        }

        public boolean secondPickIsEntity() {
            return secondEntity;
        }

        public boolean isResumable() {
            return resumable;
        }

        public boolean isRoot() {
            return root;
        }

        public boolean isTimed() {
            return timed;
        }

        public String key() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    private static final String CMD = "mo_fc_cmd";
    private static final String OWNER = "mo_fc_owner";

    private static final String GOAL_AT = "mo_fc_goal_at";

    private static final int GOAL_STALL = 3;

    private static final String TICKS = "mo_fc_ticks";

    private static final String TARGET = "mo_fc_target";

    private static final String DEST = "mo_fc_dest";

    private static final String HOME = "mo_fc_home";

    private static final String LEG = "mo_fc_leg";

    private static final String PV_CMD = "mo_fc_pv_cmd";
    private static final String PV_TICKS = "mo_fc_pv_ticks";
    private static final String PV_DEST = "mo_fc_pv_dest";
    private static final String PV_OWNER = "mo_fc_pv_owner";

    private static final String PV_TARGET = "mo_fc_pv_target";

    private static final String ROOT_CMD = "mo_fc_rt_cmd";
    private static final String ROOT_TICKS = "mo_fc_rt_ticks";
    private static final String ROOT_OWNER = "mo_fc_rt_owner";

    private static final double ARRIVE = 1.6;

    private static final int ATTACK_COOLDOWN = 20;

    private static final double ATTACK_REACH = 2.0;

    private static final int ATTACK_GRACE = 200;

    private static final int ATTACK_LEASE = 100;

    private static final String ATK_SINCE = "mo_fc_atk_since";

    private static final String ATK_HP = "mo_fc_atk_hp";

    private static final String ATK_LAST = "mo_fc_atk_last";

    private static final String ATK_TAKEN_AT = "mo_fc_atk_taken";

    private static final String ATK_REJECT = "mo_fc_atk_reject";

    private static final String ATK_NEAR = "mo_fc_atk_near";

    private static final String ATK_NEAR_AT = "mo_fc_atk_near_at";

    private static final double ATK_CLOSE_SQR = 9.0;

    private static final String ATK_APPR = "mo_fc_atk_appr";

    private static final int APPROACH_GRACE = 40;

    private static int attackReject() {
        return Math.max(1, (int) cn.academy.config.AbilityConfig
                .stat("forced_control", "attack_reject", 0f));
    }

    private ControlState() {}

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new ControlEvents());
    }

    public static boolean isControlled(Entity e) {
        return e.getPersistentData().getInt(TICKS) > 0;
    }

    public static boolean playerCommanded(Entity e) {
        return e instanceof Player && e.getPersistentData().getInt(TICKS) > 0;
    }

    public static boolean playerHeldStill(Entity e) {
        return playerCommanded(e)
                && Command.byId(e.getPersistentData().getInt(CMD)) == Command.STAY;
    }

    public static boolean playerBeingDriven(Entity e) {
        return playerCommanded(e)
                && Command.byId(e.getPersistentData().getInt(CMD)) != Command.STAY;
    }

    public static Vec3 driveGoal(LivingEntity p) {
        if (!playerBeingDriven(p)) {
            return null;
        }
        CompoundTag d = p.getPersistentData();
        switch (Command.byId(d.getInt(CMD))) {
            case ATTACK -> {
                Entity t = d.contains(TARGET) ? p.level().getEntity(d.getInt(TARGET)) : null;
                return t != null && t.isAlive() ? t.position() : null;
            }
            case FOLLOW -> {
                Player owner = ownerOf(p);
                return owner == null ? null : owner.position();
            }
            case MOVE -> {
                return center(BlockPos.of(d.getLong(DEST)));
            }
            case PATROL -> {

                return center(BlockPos.of(d.getBoolean(LEG) ? d.getLong(DEST) : d.getLong(HOME)));
            }
            default -> {
                return null;
            }
        }
    }

    public static Command commandOf(Entity e) {
        return Command.byId(e.getPersistentData().getInt(CMD));
    }

    public static Entity commandedTarget(Mob mob) {
        CompoundTag d = mob.getPersistentData();
        if (d.getInt(TICKS) <= 0 || Command.byId(d.getInt(CMD)) != Command.ATTACK
                || !d.contains(TARGET)) {
            return null;
        }
        Entity t = mob.level().getEntity(d.getInt(TARGET));
        return t != null && t.isAlive() ? t : null;
    }

    public static void issue(LivingEntity mob, Player owner, Command cmd, BlockPos dest,
                             LivingEntity target, int ticks) {

        if (cmd == Command.STOP || cmd == Command.RESTORE) {
            release(mob);
            return;
        }

        snapshotPrev(mob, owner);
        apply(mob, owner.getUUID(), cmd, dest, target, ticks);
    }

    private static void apply(LivingEntity mob, java.util.UUID ownerId, Command cmd, BlockPos dest,
                              LivingEntity target, int ticks) {

        if (mob instanceof Mob m && m.isNoAi()) {
            m.setNoAi(false);
        }
        if (mob.isPassenger()) {
            mob.stopRiding();
        }

        CompoundTag d = mob.getPersistentData();
        d.putInt(CMD, cmd.ordinal());
        d.putUUID(OWNER, ownerId);
        d.putInt(TICKS, ticks);
        if (target != null) {
            d.putInt(TARGET, target.getId());
        } else {
            d.remove(TARGET);
        }
        if (dest != null) {
            d.putLong(DEST, dest.asLong());
        } else {
            d.remove(DEST);
        }

        d.putLong(HOME, mob.blockPosition().asLong());
        d.putBoolean(LEG, true);
        resetAttackBookkeeping(d);

        if (mob instanceof Mob m) {
            if (cmd == Command.STAY) {
                holdStill(m);
            } else {
                ensureGoal(m);
            }
        } else {

            mob.setDeltaMovement(0, mob.getDeltaMovement().y, 0);
            mob.hasImpulse = true;
            syncTo(mob);
        }
    }

    private static void snapshotPrev(LivingEntity mob, Player owner) {
        CompoundTag d = mob.getPersistentData();
        Command cur = Command.byId(d.getInt(CMD));
        if (d.getInt(TICKS) <= 0
                || !d.hasUUID(OWNER) || !d.getUUID(OWNER).equals(owner.getUUID())
                || !cn.academy.ability.vanilla.mentalout.advanced.CognitionRewrite
                        .isAllyOf(mob, owner)) {
            clearPrev(d);
            clearRoot(d);
            return;
        }

        if (cur.isRoot()) {
            d.putInt(ROOT_CMD, cur.ordinal());
            d.putInt(ROOT_TICKS, d.getInt(TICKS));
            d.putUUID(ROOT_OWNER, d.getUUID(OWNER));
            clearPrev(d);
            return;
        }

        if (!cur.isResumable()) {
            clearPrev(d);
            return;
        }
        d.putInt(PV_CMD, cur.ordinal());
        d.putInt(PV_TICKS, d.getInt(TICKS));
        d.putUUID(PV_OWNER, d.getUUID(OWNER));
        if (d.contains(DEST)) {
            d.putLong(PV_DEST, d.getLong(DEST));
        } else {
            d.remove(PV_DEST);
        }

        Entity t = d.contains(TARGET) ? mob.level().getEntity(d.getInt(TARGET)) : null;
        if (t != null) {
            d.putUUID(PV_TARGET, t.getUUID());
        } else {
            d.remove(PV_TARGET);
        }
    }

    private static void clearPrev(CompoundTag d) {
        d.remove(PV_CMD);
        d.remove(PV_TICKS);
        d.remove(PV_DEST);
        d.remove(PV_OWNER);
        d.remove(PV_TARGET);
    }

    private static void clearRoot(CompoundTag d) {
        d.remove(ROOT_CMD);
        d.remove(ROOT_TICKS);
        d.remove(ROOT_OWNER);
    }

    private static void resumeOrRelease(LivingEntity mob) {
        CompoundTag d = mob.getPersistentData();

        if (resumeSlot(mob, d) || resumeRoot(mob, d)) {
            return;
        }
        release(mob);
    }

    private static boolean resumeSlot(LivingEntity mob, CompoundTag d) {
        if (!d.contains(PV_CMD) || !d.hasUUID(PV_OWNER)) {
            return false;
        }
        Command prev = Command.byId(d.getInt(PV_CMD));
        int ticks = d.getInt(PV_TICKS);
        java.util.UUID ownerId = d.getUUID(PV_OWNER);

        LivingEntity victim = null;
        if (prev == Command.ATTACK) {
            Entity t = null;
            if (d.hasUUID(PV_TARGET)
                    && mob.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                t = sl.getEntity(d.getUUID(PV_TARGET));
            }
            if (!(t instanceof LivingEntity le) || !le.isAlive()) {
                clearPrev(d);
                return false;
            }
            victim = le;
        }
        if (ticks <= 0) {
            clearPrev(d);
            return false;
        }
        BlockPos dest = d.contains(PV_DEST) ? BlockPos.of(d.getLong(PV_DEST)) : null;

        clearPrev(d);
        apply(mob, ownerId, prev, dest, victim, ticks);
        return true;
    }

    private static boolean resumeRoot(LivingEntity mob, CompoundTag d) {
        if (!d.contains(ROOT_CMD) || !d.hasUUID(ROOT_OWNER)) {
            return false;
        }
        Command root = Command.byId(d.getInt(ROOT_CMD));
        int ticks = d.getInt(ROOT_TICKS);
        java.util.UUID ownerId = d.getUUID(ROOT_OWNER);
        clearRoot(d);
        if (ticks <= 0 || !root.isRoot()) {
            return false;
        }
        apply(mob, ownerId, root, null, null, ticks);
        return true;
    }

    private static void holdStill(Mob mob) {
        mob.getNavigation().stop();
        mob.setTarget(null);

        mob.setDeltaMovement(0, mob.getDeltaMovement().y, 0);

        mob.setPose(net.minecraft.world.entity.Pose.STANDING);
        mob.swinging = false;
        mob.swingTime = 0;
        mob.attackAnim = 0;
        mob.oAttackAnim = 0;

        net.minecraft.world.entity.ai.Brain<?> brain = mob.getBrain();
        net.minecraft.world.entity.ai.memory.MemoryStatus reg =
                net.minecraft.world.entity.ai.memory.MemoryStatus.REGISTERED;
        for (MemoryModuleType<?> m : new MemoryModuleType<?>[]{
                MemoryModuleType.WALK_TARGET, MemoryModuleType.ATTACK_TARGET,
                MemoryModuleType.LOOK_TARGET, MemoryModuleType.PATH}) {
            if (brain.checkMemory(m, reg)) {
                brain.eraseMemory(m);
            }
        }

        mob.setNoAi(true);
    }

    private static void ensureGoal(Mob mob) {
        if (findGoal(mob) == null) {
            mob.goalSelector.addGoal(0, new ControlGoal(mob));
        }
    }

    private static ControlGoal findGoal(Mob mob) {
        for (net.minecraft.world.entity.ai.goal.WrappedGoal w : mob.goalSelector.getAvailableGoals()) {
            if (w.getGoal() instanceof ControlGoal g) {
                return g;
            }
        }
        return null;
    }

    private static final class ControlGoal extends net.minecraft.world.entity.ai.goal.Goal {

        private final Mob mob;

        ControlGoal(Mob mob) {
            this.mob = mob;

            setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!isControlled(mob)) {
                return false;
            }

            if (commandOf(mob) == Command.STAY) {
                return false;
            }
            return commandOf(mob) != Command.ATTACK || attackTakenOver(mob);
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void tick() {

            mob.getPersistentData().putLong(GOAL_AT, mob.level().getGameTime());
            drive(mob);
        }

        @Override
        public void stop() {
            mob.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }
    }

    public static void release(LivingEntity mob) {
        CompoundTag d = mob.getPersistentData();

        if (Command.byId(d.getInt(CMD)) == Command.STAY
                && mob instanceof Mob m0 && m0.isNoAi()) {
            m0.setNoAi(false);
        }
        d.putInt(TICKS, 0);
        d.remove(OWNER);
        d.remove(TARGET);
        d.remove(DEST);
        d.remove(HOME);
        d.remove(LEG);
        d.remove(GOAL_AT);
        resetAttackBookkeeping(d);

        clearPrev(d);
        clearRoot(d);

        syncTo(mob);

        if (mob instanceof Mob m) {
            ControlGoal g = findGoal(m);
            if (g != null) {
                m.goalSelector.removeGoal(g);
            }
            if (m.getBrain().checkMemory(MemoryModuleType.WALK_TARGET,
                    net.minecraft.world.entity.ai.memory.MemoryStatus.REGISTERED)) {
                m.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            }
            m.getNavigation().stop();

            if (m instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon dragon) {
                DragonControl.restore(dragon);
            }
        }
    }

    public static Player ownerOf(LivingEntity mob) {
        CompoundTag d = mob.getPersistentData();
        return d.hasUUID(OWNER) ? mob.level().getPlayerByUUID(d.getUUID(OWNER)) : null;
    }

    private static void drive(Mob mob) {

        if (ProxyState.isProxied(mob)) {
            return;
        }
        CompoundTag d = mob.getPersistentData();
        Command cmd = Command.byId(d.getInt(CMD));
        Player owner = ownerOf(mob);

        switch (cmd) {
            case ATTACK -> {

                Entity t = mob.level().getEntity(d.getInt(TARGET));
                if (!(t instanceof LivingEntity victim) || !victim.isAlive()) {
                    resumeOrRelease(mob);
                    return;
                }

                if (mob instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon) {
                    return;
                }
                if (mob.distanceToSqr(victim) > 4.0) {
                    navTo(mob, victim.position());
                }
            }
            case MOVE -> {
                BlockPos dest = BlockPos.of(d.getLong(DEST));
                if (near(mob, dest)) {
                    resumeOrRelease(mob);
                    return;
                }
                navTo(mob, center(dest));
            }
            case FOLLOW -> {
                if (owner == null) {

                    release(mob);
                    return;
                }

                if (mob.distanceToSqr(owner) > 9.0) {

                    navTo(mob, owner.position());
                }
            }
            case PATROL -> {
                BlockPos dest = BlockPos.of(d.getLong(DEST));
                BlockPos home = BlockPos.of(d.getLong(HOME));
                boolean toDest = d.getBoolean(LEG);
                BlockPos goal = toDest ? dest : home;
                if (near(mob, goal)) {
                    d.putBoolean(LEG, !toDest);
                    goal = toDest ? home : dest;
                }
                navTo(mob, center(goal));
            }

            case STAY, STOP, RESTORE -> { }
        }
    }

    private static void driveAttack(Mob mob, CompoundTag d) {

        if (ProxyState.isProxied(mob)) {
            return;
        }
        Entity t = mob.level().getEntity(d.getInt(TARGET));
        if (!(t instanceof LivingEntity victim) || !victim.isAlive()) {

            resumeOrRelease(mob);
            return;
        }

        if (mob.getTarget() == victim) {
            d.putInt(ATK_REJECT, 0);
        } else {
            d.putInt(ATK_REJECT, d.getInt(ATK_REJECT) + 1);
        }

        double cur = mob.distanceToSqr(victim);
        long now = mob.level().getGameTime();
        if (!d.contains(ATK_NEAR) || cur < d.getDouble(ATK_NEAR) - 0.5) {
            d.putDouble(ATK_NEAR, cur);
            d.putLong(ATK_NEAR_AT, now);
        }

        d.putBoolean(ATK_APPR, cur > ATK_CLOSE_SQR
                && now - d.getLong(ATK_NEAR_AT) >= APPROACH_GRACE);

        if (mob instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon dragon) {
            DragonControl.attack(dragon, victim);
        }

        aggro(mob, victim);
        tryOwnAttack(mob, victim, d);
    }

    private static boolean approachStalled(Mob mob, CompoundTag d) {
        return d.getBoolean(ATK_APPR);
    }

    private static boolean targetRejected(Mob mob) {
        return mob.getPersistentData().getInt(ATK_REJECT) >= attackReject();
    }

    private static boolean near(LivingEntity mob, BlockPos p) {

        double r = ARRIVE + mob.getBbWidth() * 0.5;
        return mob.position().distanceToSqr(
                new Vec3(p.getX() + 0.5, p.getY(), p.getZ() + 0.5)) <= r * r;
    }

    private static void navTo(Mob mob, Vec3 goal) {
        double reach = Math.max(6.0,
                mob.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE) - 4.0);
        Vec3 self = mob.position();
        Vec3 delta = goal.subtract(self);
        double dist = delta.length();
        Vec3 step = dist > reach ? self.add(delta.scale(reach / dist)) : goal;

        if (mob.getBrain().checkMemory(MemoryModuleType.WALK_TARGET,
                net.minecraft.world.entity.ai.memory.MemoryStatus.REGISTERED)) {
            mob.getBrain().setMemory(MemoryModuleType.WALK_TARGET,
                    new net.minecraft.world.entity.ai.memory.WalkTarget(
                            net.minecraft.core.BlockPos.containing(step.x, step.y, step.z), 1.0f, 1));
        }

        if (mob instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon dragon) {
            DragonControl.steer(dragon, goal);
            return;
        }
        if (mob.getMoveControl() instanceof cn.academy.mixin.SlimeMoveControlAccessor slime) {

            float yaw = (float) (net.minecraft.util.Mth.atan2(step.z - self.z, step.x - self.x)
                    * (180.0 / Math.PI)) - 90.0f;
            slime.academy$setDirection(yaw, true);
            slime.academy$setWantedMovement(1.0);
            return;
        }
        if (mob instanceof cn.academy.mixin.PhantomAccessor phantom) {
            phantom.academy$setMoveTargetPoint(step);
            return;
        }

        if (MobGapLeap.tryLeap(mob, step)) {
            return;
        }

        if (!mob.getNavigation().moveTo(step.x, step.y, step.z, 1.0)) {
            mob.getMoveControl().setWantedPosition(step.x, step.y, step.z, 1.0);
        }
    }

    private static void tryOwnAttack(Mob mob, LivingEntity victim, CompoundTag d) {
        long now = mob.level().getGameTime();
        float hp = victim.getHealth();
        if (!d.contains(ATK_SINCE)) {
            startWatch(d, now, hp);
            return;
        }

        boolean driving = d.contains(ATK_TAKEN_AT);

        if (hp < d.getFloat(ATK_HP) - 1.0e-4f || (!driving && showsAttackActivity(mob, victim, d))) {
            startWatch(d, now, hp);
            return;
        }
        d.putFloat(ATK_HP, hp);

        if (!targetRejected(mob) && now - d.getLong(ATK_SINCE) < ATTACK_GRACE) {
            return;
        }

        if (!driving) {
            d.putLong(ATK_TAKEN_AT, now);
        } else if (now - d.getLong(ATK_TAKEN_AT) >= ATTACK_LEASE) {
            startWatch(d, now, hp);
            return;
        }

        double reach = ATTACK_REACH + mob.getBbWidth();
        if (mob.distanceToSqr(victim) > reach * reach) {
            return;
        }
        if (now - d.getLong(ATK_LAST) < ATTACK_COOLDOWN) {
            return;
        }
        d.putLong(ATK_LAST, now);

        float before = victim.getHealth();
        mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);

        if (mob.getAttributes().hasAttribute(
                net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)) {
            mob.doHurtTarget(victim);
        }

        if (victim.getHealth() >= before - 1.0e-4f) {

            victim.invulnerableTime = 0;
            victim.hurt(MentalCharm.mindControlledDamage(mob),
                    (float) cn.academy.config.AbilityConfig.stat("forced_control", "attack_damage", 0f));
        }
        d.putFloat(ATK_HP, victim.getHealth());
    }

    private static boolean showsAttackActivity(Mob mob, LivingEntity victim, CompoundTag d) {

        if (d.contains(ATK_LAST)
                && mob.level().getGameTime() - d.getLong(ATK_LAST) < OWN_ATTACK_EVIDENCE_TTL) {
            return false;
        }
        return mob.swinging || mob.getLastHurtMob() == victim;
    }

    private static final int OWN_ATTACK_EVIDENCE_TTL = 100;

    private static void startWatch(CompoundTag d, long now, float hp) {
        d.putLong(ATK_SINCE, now);
        d.putFloat(ATK_HP, hp);
        d.remove(ATK_TAKEN_AT);
    }

    private static boolean attackTakenOver(Mob mob) {
        CompoundTag d = mob.getPersistentData();
        if (!d.contains(ATK_SINCE)) {
            return false;
        }
        return targetRejected(mob)
                || mob.level().getGameTime() - d.getLong(ATK_SINCE) >= ATTACK_GRACE;
    }

    private static void resetAttackBookkeeping(CompoundTag d) {
        d.remove(ATK_SINCE);
        d.remove(ATK_HP);
        d.remove(ATK_LAST);
        d.remove(ATK_TAKEN_AT);
        d.remove(ATK_REJECT);
        d.remove(ATK_NEAR);
        d.remove(ATK_NEAR_AT);
        d.remove(ATK_APPR);
    }

    private static Vec3 center(BlockPos p) {
        return new Vec3(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
    }

    private static void aggro(Mob mob, LivingEntity victim) {

        if (mob.getTarget() != victim) {
            mob.setTarget(victim);
        }

        mob.setLastHurtByMob(victim);

        net.minecraft.world.entity.ai.Brain<?> brain = mob.getBrain();
        net.minecraft.world.entity.ai.memory.MemoryStatus reg =
                net.minecraft.world.entity.ai.memory.MemoryStatus.REGISTERED;

        if (brain.checkMemory(MemoryModuleType.ATTACK_TARGET, reg)
                && brain.getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null) != victim) {
            brain.setMemory(MemoryModuleType.ATTACK_TARGET, victim);
        }

        if (brain.checkMemory(MemoryModuleType.HURT_BY_ENTITY, reg)) {
            brain.setMemory(MemoryModuleType.HURT_BY_ENTITY, victim);
        }

        if (brain.checkMemory(MemoryModuleType.ANGRY_AT, reg)) {
            brain.setMemory(MemoryModuleType.ANGRY_AT, victim.getUUID());
        }

        if (mob instanceof net.minecraft.world.entity.NeutralMob n) {
            n.setPersistentAngerTarget(victim.getUUID());
            n.startPersistentAngerTimer();
        }

        if (mob instanceof net.minecraft.world.entity.monster.warden.Warden w) {
            Player owner = ownerOf(mob);
            if (owner != null) {
                w.clearAnger(owner);
            }
            w.increaseAngerAt(victim, WARDEN_ANGER, false);
        }
    }

    private static final int WARDEN_ANGER = 100;

    public static class ControlEvents {

        @SubscribeEvent
        public void onLivingTick(LivingEvent.LivingTickEvent event) {

            LivingEntity mob = event.getEntity();
            if (mob.level().isClientSide) {
                return;
            }
            CompoundTag d = mob.getPersistentData();
            int ticks = d.getInt(TICKS);
            if (ticks <= 0) {
                return;
            }
            if (!mob.isAlive()) {
                release(mob);
                return;
            }
            if (!(mob instanceof Mob)) {
                tickPlayer(mob, d);
                return;
            }

            Command cmd = Command.byId(d.getInt(CMD));
            if (cmd == Command.STAY) {

                boolean proxied = ProxyState.isProxied(mob);
                if (mob instanceof Mob m && m.isNoAi() == proxied) {
                    m.setNoAi(!proxied);
                }
                return;
            }

            if (cmd.isTimed()) {
                d.putInt(TICKS, ticks - 1);
                if (ticks == 1) {
                    resumeOrRelease(mob);
                    return;
                }
            }

            if (cmd == Command.ATTACK) {
                driveAttack((Mob) mob, d);
            } else {
                ensureGoal((Mob) mob);
            }

            long now = mob.level().getGameTime();
            boolean goalStalled = now - d.getLong(GOAL_AT) > GOAL_STALL;
            boolean shouldDrive = cmd != Command.ATTACK
                    || attackTakenOver((Mob) mob)
                    || approachStalled((Mob) mob, d);
            if (goalStalled && shouldDrive) {
                drive((Mob) mob);
            }
        }

        private void tickPlayer(LivingEntity p, CompoundTag d) {
            Command cmd = Command.byId(d.getInt(CMD));
            if (cmd == Command.STAY) {
                return;
            }

            if (cmd.isTimed()) {
                int ticks = d.getInt(TICKS);
                d.putInt(TICKS, ticks - 1);
                if (ticks == 1) {
                    resumeOrRelease(p);
                    return;
                }
            }

            Vec3 goal = driveGoal(p);
            if (goal == null) {

                if (cmd == Command.FOLLOW) {
                    release(p);
                } else {
                    resumeOrRelease(p);
                }
                return;
            }
            switch (cmd) {
                case ATTACK -> {
                    Entity t = p.level().getEntity(d.getInt(TARGET));
                    if (!(t instanceof LivingEntity victim)) {
                        return;
                    }
                    double reach = ATTACK_REACH + p.getBbWidth();
                    if (p.distanceToSqr(victim) > reach * reach) {
                        return;
                    }
                    long now = p.level().getGameTime();
                    if (now - d.getLong(ATK_LAST) < ATTACK_COOLDOWN) {
                        return;
                    }
                    d.putLong(ATK_LAST, now);

                    p.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                    if (p instanceof Player pl) {
                        pl.attack(victim);
                    }
                }
                case MOVE -> {
                    if (near(p, BlockPos.of(d.getLong(DEST)))) {
                        resumeOrRelease(p);
                    }
                }
                case PATROL -> {
                    boolean toDest = d.getBoolean(LEG);
                    if (near(p, BlockPos.of(toDest ? d.getLong(DEST) : d.getLong(HOME)))) {
                        d.putBoolean(LEG, !toDest);
                        syncTo(p);
                    }
                }

                case FOLLOW, STAY, STOP, RESTORE -> { }
            }

            if ((p.level().getGameTime() + p.getId()) % SYNC_INTERVAL == 0) {
                syncTo(p);
            }
        }
    }

    private static final int SYNC_INTERVAL = 10;

    public static void syncTo(LivingEntity p) {
        if (!(p instanceof net.minecraft.server.level.ServerPlayer sp)) {
            return;
        }
        CompoundTag d = p.getPersistentData();
        cn.lambdalib2.s11n.network.NetworkMessage.sendTo(sp,
                cn.academy.ability.vanilla.mentalout.skill.ForcedControl.INSTANCE,
                cn.academy.ability.vanilla.mentalout.skill.ForcedControl.MSG_CMD_SYNC,
                p, d.getInt(TICKS), d.getInt(CMD), d.getLong(DEST), d.getLong(HOME),
                d.getBoolean(LEG), d.contains(TARGET) ? d.getInt(TARGET) : -1);
    }

    public static void applySync(LivingEntity p, int ticks, int cmd, long dest, long home,
                                 boolean leg, int targetId) {
        CompoundTag d = p.getPersistentData();
        d.putInt(TICKS, Math.max(0, ticks));
        d.putInt(CMD, cmd);
        d.putLong(DEST, dest);
        d.putLong(HOME, home);
        d.putBoolean(LEG, leg);
        if (targetId >= 0) {
            d.putInt(TARGET, targetId);
        } else {
            d.remove(TARGET);
        }
    }

}
