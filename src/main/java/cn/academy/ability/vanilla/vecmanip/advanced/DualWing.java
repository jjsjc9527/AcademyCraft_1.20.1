package cn.academy.ability.vanilla.vecmanip.advanced;

import cn.academy.ACSounds;
import cn.academy.ability.AbilityContext;
import cn.academy.ability.Skill;
import cn.academy.ability.SkillTab;
import cn.academy.ability.context.ClientContext;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.ContextManager;
import cn.academy.ability.context.DelegateState;
import cn.academy.ability.context.KeyDelegate;
import cn.academy.ability.context.RegClientContext;
import cn.academy.ability.develop.LearningHelper;
import cn.academy.client.sound.FollowEntitySound;
import cn.academy.ability.vanilla.mentalout.FaintState;
import cn.academy.ability.vanilla.vecmanip.skill.VecDeviation;
import cn.academy.config.AbilityConfig;
import cn.academy.event.ability.FlushControlEvent;
import cn.academy.util.ACPierce;
import cn.academy.util.AimTrace;
import cn.lambdalib2.input.KeyManager;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.LogicalSide;

import java.util.Optional;

@Mod.EventBusSubscriber(modid = "academy", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DualWing extends Skill {

    public static final DualWing INSTANCE = new DualWing();

    public static void init() {
        MinecraftForge.EVENT_BUS.register(WhiteWingGuard.class);

        MinecraftForge.EVENT_BUS.register(LifeVector.class);
    }

    private static final java.util.Map<java.util.UUID, Float> FALL_DOUBLED =
            new java.util.concurrent.ConcurrentHashMap<>();

    private static final java.util.Map<java.util.UUID, Long> WING_ON =
            new java.util.concurrent.ConcurrentHashMap<>();

    private static final long WING_STALE = 5L;

    static void markWingOn(Player p) {
        if (p != null) {
            WING_ON.put(p.getUUID(), p.level().getGameTime());
        }
    }

    public static boolean isWingOn(net.minecraft.world.entity.Entity e) {
        if (e == null || WING_ON.isEmpty()) {
            return false;
        }
        Long at = WING_ON.get(e.getUUID());
        return at != null && e.level().getGameTime() - at <= WING_STALE;
    }

    public static boolean addonWantsWingLongPress() {
        return false;
    }

    public static void addonOnWingLongPress() {
    }

    private static final java.util.Set<java.util.UUID> FLYING_NOW =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    public static boolean isFlyingNow(net.minecraft.world.entity.Entity e) {
        return !FLYING_NOW.isEmpty() && e != null && FLYING_NOW.contains(e.getUUID());
    }

    @SubscribeEvent
    public static void onKnockBack(net.minecraftforge.event.entity.living.LivingKnockBackEvent evt) {
        if (isWingOn(evt.getEntity())
                || cn.academy.api.ACImmortal.isImmortal(evt.getEntity())) {
            evt.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent evt) {
        Float mul = FALL_DOUBLED.get(evt.getEntity().getUUID());
        if (mul != null && mul > 0) {
            evt.setDamageMultiplier(evt.getDamageMultiplier() * mul);
        }
    }

    private static void academy$diagTerminate(cn.academy.datapart.CPData cp, Player p) {
        try {
            if (cn.academy.util.ACDiag.ON)
            org.slf4j.LoggerFactory.getLogger("DualWing").warn(
                    "[dualwing] skill terminated | {} | activated={} overloadFine={} interfering={}"
                            + " | CP={}/{} overload={}/{}\nstack:\n{}",
                    p.getName().getString(),
                    cp.isActivated(), !cp.isOverloadRecovering(), cp.isInterfering(),
                    String.format("%.0f", cp.getCP()), String.format("%.0f", cp.getMaxCP()),
                    String.format("%.1f", cp.getOverload()),
                    String.format("%.1f", cp.getMaxOverload()),
                    StackWalker.getInstance().walk(s -> s.limit(16)
                            .map(f -> "    " + f.getClassName() + "." + f.getMethodName())
                            .reduce("", (a, b) -> a.isEmpty() ? b : a + "\n" + b)));
        } catch (Throwable ignored) {

        }
    }

    static final String CFG = "dual_wing";

    public enum Wing {

        BLACK,

        WHITE;

        public Wing next() {
            return this == BLACK ? WHITE : BLACK;
        }
    }

    public DualWing() {

        super("dual_wing", LearningHelper.ADVANCED_TREE_LEVEL);

        this.tab = SkillTab.ADVANCED;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void activate(ClientRuntime rt, int keyID) {
        rt.addKey(keyID, new ToggleDelegate());
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ToggleDelegate extends KeyDelegate {

        @Override
        public void onKeyDown() {
            Optional<DualWingContext> cur = ContextManager.instance.findLocal(DualWingContext.class);
            if (cur.isPresent()) {
                cur.get().terminate();
            } else {
                ContextManager.instance.activate(new DualWingContext(getPlayer()));
            }
        }

        @Override
        public DelegateState getState() {
            return ContextManager.instance.findLocal(DualWingContext.class).isPresent()
                    ? DelegateState.ACTIVE : DelegateState.IDLE;
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
    }

    public static class DualWingContext extends Context<DualWing> {

        static final String MSG_WING = "wing";

        static final String MSG_SHARP = "sharp";

        static final String MSG_CRUSH = "crush";

        static final String MSG_CRUSH_STATE = "crush_state";

        static final String MSG_GUARD_STATE = "guard_state";

        static final String MSG_FEATHER_STATE = "feather_state";

        static final String MSG_FEATHER_SPAWN = "feather_spawn";

        static final String MSG_FEATHER_LAUNCH = "feather_launch";

        static final String MSG_FLY_STATE = "fly_state";

        static final String MSG_FLY_DIR = "fly_dir";

        static final String MSG_FX_PRESS = "fx_press";

        static final String MSG_FX_PRESS_END = "fx_press_end";

        static final String MSG_PRESS_WRAPPED = "press_wrapped";

        static final int PRESS_WRAP_TIMEOUT = 60;

        static final double PRESS_LOCAL_MAX = 1.0 / 0.65;

        static final String KEY_GROUP = "vm_dual_wing";

        private final float exp = ctx.getSkillExp();

        private final float cpTick = AbilityConfig.cp(CFG, exp);
        private final float overloadTick = AbilityConfig.overload(CFG, exp);

        private final double gustAimWide = AbilityConfig.stat(CFG, "gust_aim_wide", exp);

        private final float sharpMul = AbilityConfig.stat(CFG, "sharp_mul", exp);

        private final double pressRange = AbilityConfig.stat(CFG, "press_range", exp);

        private final float pressDamage = AbilityConfig.stat(CFG, "press_damage", exp);

        private final int pressHurtEvery = (int) AbilityConfig.stat(CFG, "press_hurt_every", exp);

        private final double pressGravity = AbilityConfig.stat(CFG, "press_gravity", exp);

        private final double pressMaxFall = AbilityConfig.stat(CFG, "press_max_fall", exp);

        private final float pressFallMul = AbilityConfig.stat(CFG, "press_fall_mul", exp);

        private final float pressCp = AbilityConfig.stat(CFG, "press_cp", exp);
        private final float pressOverload = AbilityConfig.stat(CFG, "press_overload", exp);

        private final double crushRange = AbilityConfig.stat(CFG, "crush_range", exp);

        private final float crushDamage = AbilityConfig.stat(CFG, "crush_damage", exp);

        private final int crushHurtEvery = (int) AbilityConfig.stat(CFG, "crush_hurt_every", exp);

        private final double crushGravity = AbilityConfig.stat(CFG, "crush_gravity", exp);

        private final double crushMaxFall = AbilityConfig.stat(CFG, "crush_max_fall", exp);

        private final double crushSlow = AbilityConfig.stat(CFG, "crush_slow", exp);

        private final int crushSlowAmp = (int) AbilityConfig.stat(CFG, "crush_slow_amp", exp);

        private final int crushScanEvery = (int) AbilityConfig.stat(CFG, "crush_scan_every", exp);

        private final float crushCp = AbilityConfig.stat(CFG, "crush_cp", exp);
        private final float crushOverload = AbilityConfig.stat(CFG, "crush_overload", exp);

        private final double guardRange = AbilityConfig.stat(CFG, "guard_range", exp);

        private final double featherRange = AbilityConfig.stat(CFG, "feather_range", exp);

        private final double featherHeight = AbilityConfig.stat(CFG, "feather_height", exp);

        private final int featherLife = (int) AbilityConfig.stat(CFG, "feather_life", exp);

        private final float featherDamage = AbilityConfig.stat(CFG, "feather_damage", exp);

        private final int featherHurtEvery = (int) AbilityConfig.stat(CFG, "feather_hurt_every", exp);

        private final int featherPerBatch = (int) AbilityConfig.stat(CFG, "feather_per_batch", exp);
        private final int featherSpawnEvery = (int) AbilityConfig.stat(CFG, "feather_spawn_every", exp);
        private final float featherCp = AbilityConfig.stat(CFG, "feather_cp", exp);
        private final float featherOverload = AbilityConfig.stat(CFG, "feather_overload", exp);

        private final double featherEmpowerSpawnMul =
                AbilityConfig.stat(CFG, "feather_empower_spawn_mul", exp);
        private final double featherEmpowerFallMul =
                AbilityConfig.stat(CFG, "feather_empower_fall_mul", exp);

        private final int featherAimDelay = (int) AbilityConfig.stat(CFG, "feather_aim_delay", exp);

        private final double featherLaunchSpeed =
                AbilityConfig.stat(CFG, "feather_launch_speed", exp);
        private final double featherAimRange = AbilityConfig.stat(CFG, "feather_aim_range", exp);

        private final int featherShotCost = (int) AbilityConfig.stat(CFG, "feather_shot_cost", exp);

        private final double flySpeed = AbilityConfig.stat(CFG, "fly_speed", exp);

        private final double flyDashSpeed = AbilityConfig.stat(CFG, "fly_dash_speed", exp);

        private final int flyDashTicks = (int) AbilityConfig.stat(CFG, "fly_dash_ticks", exp);

        private final double flyDropSpeed = AbilityConfig.stat(CFG, "fly_drop_speed", exp);

        public double flyDropSpeed() {
            return flyDropSpeed;
        }

        public double flySpeed() {
            return flySpeed;
        }

        public double flyDashSpeed() {
            return flyDashSpeed;
        }

        public int flyDashTicks() {
            return flyDashTicks;
        }

        private static final float EXP_INCR = 0.00004f;

        private static final float EXP_ACT = 0.0016f;

        private Wing wing = Wing.BLACK;

        private boolean sharp = false;

        private boolean crushing = false;

        private int crushHurtCd = 0;

        private int crushScanCd = 0;

        private final java.util.List<LivingEntity> crushTargets = new java.util.ArrayList<>();

        private boolean guarding = false;

        private boolean feathering = false;

        private WhiteFeatherField featherField = null;

        private int featherHurtCd = 0;
        private int featherSpawnCd = 0;

        private LivingEntity pressTarget = null;

        private int pressHurtCd = 0;

        private int pressArrive = 0;

        private VecDeviation.DeviationContext deviation = null;

        private boolean ownDeviation = false;

        public DualWingContext(Player player) {
            super(player, INSTANCE);
        }

        public Wing getWing() {
            return wing;
        }

        public boolean isSharp() {
            return sharp;
        }

        @Override
        public void onWatcherJoined(net.minecraft.server.level.ServerPlayer watcher) {
            sendToWatcher(watcher, MSG_WING, wing.ordinal());
            sendToWatcher(watcher, MSG_SHARP, sharp);
            sendToWatcher(watcher, MSG_CRUSH_STATE, crushing);
            sendToWatcher(watcher, MSG_GUARD_STATE, guarding);
            double[] cfg = feathering
                    ? new double[]{featherLife, featherEmpowerFallMul,
                            featherLaunchSpeed, featherShotCost}
                    : null;
            sendToWatcher(watcher, MSG_FEATHER_STATE, feathering, cfg);
            sendToWatcher(watcher, MSG_FLY_STATE, flyingShared);
            sendToWatcher(watcher, MSG_FLY_DIR, flyDir.ordinal());
        }

        private void attachDeviation() {

            Optional<VecDeviation.DeviationContext> cur = isRemote()
                    ? ContextManager.instance.findLocal(VecDeviation.DeviationContext.class)
                    : ContextManager.instance.find(VecDeviation.DeviationContext.class, player);
            if (cur.isPresent()) {
                deviation = cur.get();
                ownDeviation = false;
            } else if (isRemote() && isLocal()) {

                VecDeviation.DeviationContext fresh = new VecDeviation.DeviationContext(player);
                ContextManager.instance.activate(fresh);
                deviation = fresh;
                ownDeviation = true;
            }
            if (deviation != null) {
                deviation.lockMode(VecDeviation.Mode.RETURN);
            }
        }

        private void keepDeviation() {
            if (deviation != null && deviation.getStatus() == Status.TERMINATED) {
                deviation = null;
            }
            if (deviation == null) {
                attachDeviation();
            } else if (deviation.getStatus() == Status.ALIVE) {
                deviation.lockMode(VecDeviation.Mode.RETURN);
            }

        }

        private void detachDeviation() {
            if (deviation == null) {
                return;
            }
            deviation.unlockMode();

            if (ownDeviation && isRemote() && isLocal() && deviation.getStatus() == Status.ALIVE) {
                deviation.terminate();
            }
            deviation = null;
        }

        @Listener(channel = MSG_WING, side = LogicalSide.SERVER)
        private void s_setWing(int ordinal) {
            Wing[] values = Wing.values();
            if (ordinal >= 0 && ordinal < values.length) {
                wing = values[ordinal];
            }

            if (wing != Wing.BLACK) {
                sharp = false;
            }

            if (wing != Wing.WHITE) {
                endFeather(true);
            }

            sendToClient(MSG_WING, wing.ordinal());
        }

        @Listener(channel = MSG_WING, side = LogicalSide.CLIENT)
        private void c_setWing(int ordinal) {
            if (isLocal()) {
                return;
            }
            Wing[] values = Wing.values();
            if (ordinal >= 0 && ordinal < values.length) {
                wing = values[ordinal];
            }
        }

        @Listener(channel = MSG_SHARP, side = LogicalSide.CLIENT)
        private void c_sharp(boolean on) {
            if (isLocal()) {
                return;
            }
            sharp = on && wing == Wing.BLACK;
        }

        @Listener(channel = MSG_SHARP, side = LogicalSide.SERVER)
        private void s_sharp(boolean on) {

            if (wing != Wing.BLACK) {
                if (on) {
                    toggleFeather();
                }
                return;
            }
            sharp = on;

            sendToClient(MSG_SHARP, on);

            if (on) {
                startPress();
            } else {
                endPress(true);
            }
        }

        public boolean isCrushing() {
            return crushing;
        }

        @Listener(channel = MSG_CRUSH, side = LogicalSide.SERVER)
        private void s_crush() {

            if (crushing) {
                endCrush(true);
                player.displayClientMessage(
                        Component.translatable("gui.academy.dual_wing.crush_off"), true);
                return;
            }
            if (guarding) {
                endGuard(true);
                player.displayClientMessage(
                        Component.translatable("gui.academy.dual_wing.guard_off"), true);
                return;
            }
            if (wing != Wing.BLACK) {
                s_guardOn();
                return;
            }
            if (!ctx.canConsumeCP(crushCp)) {
                player.displayClientMessage(
                        Component.translatable("gui.academy.dual_wing.no_cp"), true);
                return;
            }
            crushing = true;
            crushHurtCd = crushHurtEvery;
            crushScanCd = 0;
            sendToClient(MSG_CRUSH_STATE, true);
            ctx.addSkillExp(EXP_ACT);
            player.displayClientMessage(
                    Component.translatable("gui.academy.dual_wing.crush_on"), true);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    ACSounds.VM_STORM_WING.get(), SoundSource.PLAYERS, 1.2f, 0.5f);
        }

        private void endCrush(boolean notify) {
            crushing = false;
            crushTargets.clear();
            crushHurtCd = 0;
            crushScanCd = 0;
            if (notify) {
                sendToClient(MSG_CRUSH_STATE, false);
            }
        }

        @Listener(channel = MSG_CRUSH_STATE, side = LogicalSide.CLIENT)
        private void c_crushState(boolean on) {
            crushing = on;
        }

        public boolean isGuarding() {
            return guarding;
        }

        private void s_guardOn() {
            guarding = true;

            WhiteWingGuard.setBoostField(player, true);
            sendToClient(MSG_GUARD_STATE, true);
            ctx.addSkillExp(EXP_ACT);
            player.displayClientMessage(
                    Component.translatable("gui.academy.dual_wing.guard_on"), true);

            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    ACSounds.VM_STORM_WING.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        }

        private void endGuard(boolean notify) {
            guarding = false;

            WhiteWingGuard.setBoostField(player, false);
            if (notify) {
                sendToClient(MSG_GUARD_STATE, false);
            }
        }

        @Listener(channel = MSG_GUARD_STATE, side = LogicalSide.CLIENT)
        private void c_guardState(boolean on) {
            guarding = on;
        }

        @Listener(channel = MSG_FEATHER_STATE, side = LogicalSide.CLIENT)
        private void c_featherState(boolean on, double[] cfg) {
            feathering = on;
            if (on) {
                PlatinumFeatherFx.begin(player.getId(), cfg);
            } else {
                PlatinumFeatherFx.end(player.getId());
            }
        }

        @Listener(channel = MSG_FEATHER_LAUNCH, side = LogicalSide.CLIENT)
        private void c_featherLaunch(double[] flat) {
            PlatinumFeatherFx.launchBatch(flat);
        }

        @Listener(channel = MSG_FEATHER_SPAWN, side = LogicalSide.CLIENT)
        private void c_featherSpawn(double[] flat, boolean empowered) {
            PlatinumFeatherFx.spawnBatch(player.getId(), flat, empowered);
        }

        private void tickGuard() {

            WhiteWingGuard.diagField(player, guarding, WhiteWingGuard.coveredCount(), 0);
        }

        public boolean isFeathering() {
            return feathering;
        }

        private void toggleFeather() {
            if (feathering) {
                endFeather(true);
                player.displayClientMessage(
                        Component.translatable("gui.academy.dual_wing.feather_off"), true);
                return;
            }
            if (!ctx.canConsumeCP(featherCp)) {
                player.displayClientMessage(
                        Component.translatable("gui.academy.dual_wing.no_cp"), true);
                return;
            }
            feathering = true;
            if (featherField == null) {
                featherField = new WhiteFeatherField(featherLife, featherEmpowerFallMul,
                        featherAimDelay, featherLaunchSpeed, featherAimRange, featherShotCost);
            }
            featherSpawnCd = 0;
            featherHurtCd = featherHurtEvery;
            ctx.addSkillExp(EXP_ACT);

            sendToClient(MSG_FEATHER_STATE, true, new double[]{
                    featherLife, featherEmpowerFallMul, featherLaunchSpeed, featherShotCost});
            player.displayClientMessage(
                    Component.translatable("gui.academy.dual_wing.feather_on"), true);

        }

        private void endFeather(boolean notify) {
            if (!feathering) {
                return;
            }
            feathering = false;
            if (featherField != null) {
                featherField.clear();
            }
            if (notify) {
                sendToClient(MSG_FEATHER_STATE, false, (double[]) null);
            }
        }

        private void tickFeather() {
            WhiteFeatherField field = featherField;
            if (field == null) {
                return;
            }

            boolean empowered = guarding;
            double topY = player.getY() + featherHeight;

            if (feathering && --featherSpawnCd <= 0) {
                featherSpawnCd = featherSpawnEvery;
                int n = empowered
                        ? (int) Math.round(featherPerBatch * featherEmpowerSpawnMul)
                        : featherPerBatch;
                double[] flat = field.spawn(player.level(), player.getX(), topY, player.getZ(),
                        featherRange, n, player.level().random, empowered);
                if (flat != null) {
                    sendToClient(MSG_FEATHER_SPAWN, flat, empowered);
                }
            }

            if (field.isEmpty()) {
                return;
            }

            AABB box = WhiteFeatherField.searchBox(
                    player.getX(), topY, player.getZ(), featherRange);
            java.util.List<LivingEntity> all =
                    player.level().getEntitiesOfClass(LivingEntity.class, box);

            featherFoes.clear();
            for (LivingEntity e : all) {

                if (e.isRemoved()) {
                    continue;
                }
                if (featherFriendly(player, e)) {
                    continue;
                }

                if (WhiteFeatherField.isDown(e)) {
                    continue;
                }
                if (ctx.canTarget(e)) {
                    featherFoes.add(e);
                }
            }

            double[] launched = field.tick(player.level(), featherFoes);
            if (launched != null) {
                sendToClient(MSG_FEATHER_LAUNCH, launched);
            }

            boolean hurtNow = --featherHurtCd <= 0;
            if (hurtNow) {
                featherHurtCd = featherHurtEvery;
            }
            for (LivingEntity e : all) {

                if (e.isRemoved() || !(field.covers(e) || field.isStruck(e))) {
                    continue;
                }
                if (featherFriendly(player, e)) {

                    if (e instanceof Player p) {
                        healToFull(p);
                    }
                } else if (hurtNow && ctx.canTarget(e)) {

                    ACPierce.hurtOrPierce(e, asphyxiation(), ACPierce.ASPHYXIATION_PIERCE,
                            AbilityContext.finalSkillDamage(player, DualWing.INSTANCE, e,
                                    damageOf(featherDamage)));
                }
            }
        }

        private final java.util.List<LivingEntity> featherFoes = new java.util.ArrayList<>();

        private void healToFull(Player p) {
            float max = p.getMaxHealth();
            if (cn.academy.util.ACLife.trueLife(p) >= max - 0.01f) {
                return;
            }
            p.setHealth(max);
            if (cn.academy.util.ACLife.trueLife(p) < max - 0.01f) {
                cn.academy.util.ACLife.forceWriteLife(p, max);
            }
        }

        private static boolean featherFriendly(Player caster, LivingEntity e) {
            if (e instanceof Player) {
                return true;
            }
            return nonPlayerFriendly(caster, e);
        }

        private static boolean nonPlayerFriendly(Player caster, LivingEntity e) {
            if (caster.isAlliedTo(e)) {
                return true;
            }
            if (e instanceof net.minecraft.world.entity.OwnableEntity ownable) {
                LivingEntity owner = ownable.getOwner();
                if (owner == caster || (owner != null && caster.isAlliedTo(owner))) {
                    return true;
                }
            }
            return cn.academy.ability.vanilla.mentalout.advanced.CognitionRewrite
                    .isAllyOf(e, caster);
        }

        private boolean flyingShared;

        private boolean flyAnimOn;

        public boolean isFlyingShared() {
            return flyingShared;
        }

        public boolean isFlyAnimOn() {
            return flyAnimOn;
        }

        @Listener(channel = MSG_FLY_STATE, side = LogicalSide.SERVER)
        private void s_flyState(boolean on) {
            flyingShared = on;

            if (on) {
                FLYING_NOW.add(player.getUUID());
            } else {
                FLYING_NOW.remove(player.getUUID());
            }
            updateFlyAnim();
            sendToClient(MSG_FLY_STATE, on);
        }

        @Listener(channel = MSG_FLY_STATE, side = LogicalSide.CLIENT)
        private void c_flyState(boolean on) {
            flyingShared = on;
            updateFlyAnim();
        }

        private DualWingAnim.Dir flyDir = DualWingAnim.Dir.NONE;

        public DualWingAnim.Dir getFlyDir() {
            return flyDir;
        }

        @Listener(channel = MSG_FLY_DIR, side = LogicalSide.SERVER)
        private void s_flyDir(int id) {
            flyDir = DualWingAnim.Dir.byId(id);
            sendToClient(MSG_FLY_DIR, id);
        }

        @Listener(channel = MSG_FLY_DIR, side = LogicalSide.CLIENT)
        private void c_flyDir(int id) {
            flyDir = DualWingAnim.Dir.byId(id);
        }

        private void setFlyAnim(boolean on) {
            if (on) {
                flyAnimOn = true;

                player.noCulling = true;
            } else if (flyAnimOn) {
                flyAnimOn = false;
                player.noCulling = false;

                player.setForcedPose(null);
            }
        }

        private void updateFlyAnim() {
            boolean want = flyingShared && !nearGround();
            if (want != flyAnimOn) {
                setFlyAnim(want);
            }
        }

        private boolean nearGround() {
            Vec3 from = player.position().add(0, 0.5, 0);
            Vec3 to = player.position().add(0, -0.3, 0);
            return player.level().clip(new net.minecraft.world.level.ClipContext(from, to,
                            net.minecraft.world.level.ClipContext.Block.COLLIDER,
                            net.minecraft.world.level.ClipContext.Fluid.NONE, player))
                    .getType() != net.minecraft.world.phys.HitResult.Type.MISS;
        }

        @Listener(channel = MSG_TICK, side = {LogicalSide.CLIENT, LogicalSide.SERVER})
        private void x_flyAnimTick() {
            updateFlyAnim();
        }

        @Listener(channel = MSG_TERMINATED, side = {LogicalSide.CLIENT, LogicalSide.SERVER})
        private void x_flyTerminated() {
            flyingShared = false;

            FLYING_NOW.remove(player.getUUID());
            flyDir = DualWingAnim.Dir.NONE;
            setFlyAnim(false);

            DualWingAnim.clear(player.getUUID());
        }

        private void tickCrush() {
            if (!crushing) {
                return;
            }
            boolean rescan = --crushScanCd <= 0;
            if (rescan) {
                crushScanCd = crushScanEvery;
                rescanCrush();
            }
            boolean hurtNow = --crushHurtCd <= 0;
            if (hurtNow) {
                crushHurtCd = crushHurtEvery;
            }
            java.util.Iterator<LivingEntity> it = crushTargets.iterator();
            while (it.hasNext()) {
                LivingEntity t = it.next();

                if (!t.isAlive() || t.isRemoved() || t.level() != player.level()) {
                    it.remove();
                    continue;
                }
                crushOne(t, rescan, hurtNow);
            }
        }

        private void rescanCrush() {
            crushTargets.clear();
            double r = crushRange;
            AABB box = player.getBoundingBox().inflate(r);
            for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, box)) {
                if (e == player || !e.isAlive() || e.isRemoved()) {
                    continue;
                }
                if (e.distanceToSqr(player) > r * r) {
                    continue;
                }
                if (!ctx.canTarget(e)) {
                    continue;
                }
                if (nonPlayerFriendly(player, e)) {
                    continue;
                }
                crushTargets.add(e);
            }
        }

        private void crushOne(LivingEntity t, boolean renew, boolean hurtNow) {

            Vec3 mo = t.getDeltaMovement();
            double vy = Math.max(-crushMaxFall, mo.y - crushGravity);
            t.setDeltaMovement(mo.x * crushSlow, vy, mo.z * crushSlow);
            t.hurtMarked = true;

            if (renew && crushSlowAmp >= 0) {
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                        crushScanEvery * 2 + 20, crushSlowAmp, false, false, false));
            }
            if (hurtNow) {

                ACPierce.hurtOrPierce(t, asphyxiation(), ACPierce.ASPHYXIATION_PIERCE,
                        AbilityContext.finalSkillDamage(player, DualWing.INSTANCE, t,
                                damageOf(crushDamage)));
            }
        }

        private DamageSource asphyxiation() {
            Holder<DamageType> type = player.level().registryAccess()
                    .registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(FaintState.ASPHYXIATION);
            return new DamageSource(type, player);
        }

        private void startPress() {
            if (pressTarget != null) {
                return;
            }
            Vec3 from = player.getEyePosition(1.0f);
            Vec3 to = from.add(player.getLookAngle().scale(pressRange));
            java.util.List<LivingEntity> found = AimTrace.nearestLiving(
                    player.level(), player, from, to, gustAimWide, 1,
                    e -> ctx.canTarget(e));
            if (found.isEmpty()) {
                return;
            }
            pressTarget = found.get(0);

            double reach = Math.max(0.05,
                    player.position().distanceTo(pressTarget.position())
                            - cn.academy.entity.EntityDualWing.PRESS_WRAP_R);
            pressArrive = Math.min(PRESS_WRAP_TIMEOUT,
                    cn.academy.entity.EntityDualWing.gustArriveTicks(reach * PRESS_LOCAL_MAX));
            pressHurtCd = 0;
            ctx.addSkillExp(EXP_ACT);

            for (int side = 0; side < 2; side++) {
                sendToClient(MSG_FX_PRESS, side, pressTarget.getId());
            }
            player.level().playSound(null, pressTarget.getX(), pressTarget.getY(), pressTarget.getZ(),
                    ACSounds.VM_STORM_WING.get(), SoundSource.PLAYERS, 1.0f, 0.6f);
        }

        @Listener(channel = MSG_PRESS_WRAPPED, side = LogicalSide.SERVER)
        private void s_pressWrapped() {
            if (pressTarget != null && pressArrive > 0) {
                pressArrive = 0;
            }
        }

        private void endPress(boolean notify) {
            boolean had = pressTarget != null;
            if (had) {

                FALL_DOUBLED.remove(pressTarget.getUUID());
            }
            pressTarget = null;
            pressHurtCd = 0;
            pressArrive = 0;
            if (notify && had) {
                sendToClient(MSG_FX_PRESS_END);
            }
        }

        private void tickPress() {
            LivingEntity t = pressTarget;
            if (t == null) {
                return;
            }
            boolean lost = !t.isAlive() || t.isRemoved()
                    || t.level() != player.level()
                    || t.distanceToSqr(player) > (pressRange * 3) * (pressRange * 3);
            if (lost) {
                endPress(true);
                return;
            }

            Vec3 mo = t.getDeltaMovement();
            double vy = Math.max(-pressMaxFall, mo.y - pressGravity);
            t.setDeltaMovement(mo.x * 0.86, vy, mo.z * 0.86);
            t.hurtMarked = true;

            if (!t.onGround()) {
                FALL_DOUBLED.put(t.getUUID(), pressFallMul);
            }

            if (pressArrive > 0) {
                --pressArrive;
            } else if (--pressHurtCd <= 0) {
                pressHurtCd = pressHurtEvery;

                t.invulnerableTime = -1;
                ctx.attack(t, damageOf(pressDamage));
            }
        }

        private boolean prevMayFly;

        @Listener(channel = MSG_MADEALIVE, side = LogicalSide.SERVER)
        private void s_madeAlive() {
            attachDeviation();
            Abilities ab = player.getAbilities();
            prevMayFly = ab.mayfly;
            ab.mayfly = true;
            player.onUpdateAbilities();
        }

        @Listener(channel = MSG_TICK, side = LogicalSide.SERVER)
        private void s_tick() {

            if (!cn.lambdalib2.datapart.EntityData.isReady(player)) {
                return;
            }

            if (cn.academy.util.ACRespawn.isPendingRebuild(
                    player.getUUID(), player.level().getGameTime())) {
                return;
            }

            if (!ctx.cpData.canUseAbility()) {

                academy$diagTerminate(ctx.cpData, player);
                terminate();
                return;
            }

            markWingOn(player);

            keepDeviation();
            ctx.addSkillExp(EXP_INCR);

            if ((cpTick > 0 || overloadTick > 0) && !ctx.consume(overloadTick, cpTick)) {
                terminate();
                return;
            }

            if (crushing && (crushCp > 0 || crushOverload > 0)
                    && !ctx.consume(crushOverload, crushCp)) {
                endCrush(true);
                player.displayClientMessage(
                        Component.translatable("gui.academy.dual_wing.no_cp"), true);
            }
            tickCrush();

            tickGuard();

            if (feathering && (featherCp > 0 || featherOverload > 0)
                    && !ctx.consume(featherOverload, featherCp)) {
                endFeather(true);
                player.displayClientMessage(
                        Component.translatable("gui.academy.dual_wing.no_cp"), true);
            }

            tickFeather();

            if (pressTarget != null && (pressCp > 0 || pressOverload > 0)
                    && !ctx.consume(pressOverload, pressCp)) {
                endPress(true);
            }
            tickPress();
        }

        private float damageOf(float base) {
            return sharp ? base * sharpMul : base;
        }

        @Listener(channel = MSG_TERMINATED, side = LogicalSide.SERVER)
        private void s_terminated() {
            endCrush(false);
            endGuard(false);
            endFeather(false);
            endPress(false);
            detachDeviation();

            Abilities ab = player.getAbilities();
            ab.mayfly = prevMayFly;
            if (!prevMayFly) {
                ab.flying = false;
            }
            player.onUpdateAbilities();

            ctx.setCooldown((int) AbilityConfig.cooldown(CFG, exp));
        }

        @Listener(channel = MSG_MADEALIVE, side = LogicalSide.CLIENT)
        private void c_madeAlive() {
            if (isLocal()) {
                attachDeviation();
            }
        }

        @Listener(channel = MSG_TICK, side = LogicalSide.CLIENT)
        private void c_tick() {
            if (isLocal()) {

                markWingOn(player);
                keepDeviation();
            }
        }

        @Listener(channel = MSG_TERMINATED, side = LogicalSide.CLIENT)
        private void c_terminated() {
            if (isLocal()) {
                detachDeviation();
            }

            PlatinumFeatherFx.end(player.getId());
        }

    }

    @OnlyIn(Dist.CLIENT)
    @RegClientContext(DualWingContext.class)
    public static class DualWingContextC extends ClientContext {

        private final DualWingContext par;
        private ClientRuntime.IActivateHandler activateHandler;
        private cn.academy.entity.EntityDualWing effect;

        private final cn.academy.entity.EntityGustTornado[] pressFx =
                new cn.academy.entity.EntityGustTornado[2];

        private boolean pressReported = false;

        private final CrushFieldFx.State crushFx = new CrushFieldFx.State();

        private FollowEntitySound crushSound;

        private DualWingFlight flight;

        private DualWingAnim.Dir lastSentDir = DualWingAnim.Dir.NONE;

        private static final float CRUSH_VOLUME = 0.6f;

        private static final float BLACK_VOLUME = 0.6f;

        private static final int LOOP_FADE = 12;

        private FollowEntitySound blackSound;

        public DualWingContextC(DualWingContext par) {
            super(par);
            this.par = par;
        }

        @Listener(channel = MSG_TICK, side = LogicalSide.CLIENT)
        private void c_tickFx() {
            healEffectIfLost();
            if (effect != null) {
                effect.touch();
                effect.setForm(par.getWing() != Wing.BLACK, par.isSharp());
            }

            touchAll(pressFx);

            if (par.isCrushing()) {
                CrushFieldFx.tick(player, crushFx);
            } else {
                crushFx.reset();
            }

            if (par.isCrushing()) {
                if (crushSound == null) {
                    crushSound = new FollowEntitySound(
                            ACSounds.VM_CRUSH_LOOP.get(), player, CRUSH_VOLUME, LOOP_FADE);
                    Minecraft.getInstance().getSoundManager().play(crushSound);
                }
            } else if (crushSound != null) {
                crushSound.requestStop();
                crushSound = null;
            }

            if (par.getWing() == Wing.BLACK && !par.isCrushing()) {
                if (blackSound == null) {
                    blackSound = new FollowEntitySound(
                            ACSounds.VM_STORM_WING.get(), player, BLACK_VOLUME, LOOP_FADE);
                    Minecraft.getInstance().getSoundManager().play(blackSound);
                }
            } else if (blackSound != null) {
                blackSound.requestStop();
                blackSound = null;
            }

            if (flight != null) {
                boolean before = flight.isFlying();
                flight.tick(player, par.flySpeed(), par.flyDashSpeed(), par.flyDropSpeed());

                if (flight.isFlying() != before) {

                    par.sendToServer(DualWingContext.MSG_FLY_STATE, flight.isFlying());
                }

                DualWingAnim.Dir d = flight.dir(player);
                if (d != lastSentDir) {
                    lastSentDir = d;
                    par.sendToServer(DualWingContext.MSG_FLY_DIR, d.ordinal());
                }
            }

            if (par.isFlyAnimOn()) {
                DualWingAnim.tick(player,
                        flight != null ? flight.dir(player) : par.getFlyDir());
            } else {
                DualWingAnim.clear(player.getUUID());
            }

            if (!pressReported && isLocal() && effect != null && effect.pressWrapped()) {
                pressReported = true;

                par.sendToServer(DualWingContext.MSG_PRESS_WRAPPED);
            }
        }

        private void touchAll(cn.academy.entity.EntityGustTornado[] arr) {
            for (int i = 0; i < arr.length; i++) {
                if (arr[i] == null) {
                    continue;
                }
                if (arr[i].isRemoved()) {
                    arr[i] = null;
                } else {
                    arr[i].touch(par.getWing() != Wing.BLACK);
                }
            }
        }

        @Listener(channel = DualWingContext.MSG_FX_PRESS, side = LogicalSide.CLIENT)
        private void l_fxPress(int side, int entityId) {
            net.minecraft.world.entity.Entity t = player.level().getEntity(entityId);
            if (t == null || side < 0 || side >= pressFx.length) {
                return;
            }
            Vec3 tip = effect == null ? null
                    : cn.academy.client.render.entity.DualWingRenderer.wingTipWorld(effect, side);
            if (tip == null) {
                tip = player.getEyePosition(1.0f);
            }
            if (pressFx[side] != null && !pressFx[side].isRemoved()) {
                pressFx[side].end();
            }
            pressFx[side] = new cn.academy.entity.EntityGustTornado(
                    player.level(), effect, side, tip, t, true);
            cn.academy.client.render.entity.ACEffectEntities.spawn(pressFx[side]);
        }

        @Listener(channel = DualWingContext.MSG_FX_PRESS_END, side = LogicalSide.CLIENT)
        private void l_fxPressEnd() {
            endAll(pressFx);
            pressReported = false;
        }

        private void endAll(cn.academy.entity.EntityGustTornado[] arr) {
            for (int i = 0; i < arr.length; i++) {
                if (arr[i] != null) {
                    arr[i].end();
                    arr[i] = null;
                }
            }
        }

        @Listener(channel = MSG_MADEALIVE, side = LogicalSide.CLIENT)
        private void l_madeAlive() {

            effect = new cn.academy.entity.EntityDualWing(player, par);
            cn.academy.client.render.entity.ACEffectEntities.spawn(effect);

            if (!isLocal()) {
                return;
            }

            if (!ClientRuntime.available()) {
                return;
            }
            ClientRuntime rt = ClientRuntime.instance();

            installLocalControls(rt);

            if (flight != null && flight.isFlying()) {
                DualWingFlight.academy$diag(player, "l_madeAlive -> flight object rebuilt, flight state lost");
            }
            flight = new DualWingFlight(par.flyDashTicks());

            activateHandler = new ClientRuntime.IActivateHandler() {
                @Override
                public boolean handles(Player p) {
                    return par.getStatus() == Context.Status.ALIVE;
                }

                @Override
                public void onKeyDown(Player p) {
                    par.terminate();
                }

                @Override
                public String getHint() {
                    return ClientRuntime.IActivateHandler.ENDSPECIAL;
                }
            };
            rt.addActivateHandler(activateHandler);

            hint("gui.academy.dual_wing.enter");
        }

        private void installLocalControls(ClientRuntime rt) {
            rt.pushSuppressDefaultGroup();
            rt.clearKeys(DualWingContext.KEY_GROUP);
            rt.addKey(DualWingContext.KEY_GROUP, KeyManager.MOUSE_MIDDLE, new WingKey());
            rt.addKey(DualWingContext.KEY_GROUP, KeyManager.MOUSE_LEFT, new CrushKey());
            rt.addKey(DualWingContext.KEY_GROUP, KeyManager.MOUSE_RIGHT, new SharpKey());
        }

        @Override
        protected void onRebound() {

            respawnEffect();
            if (!isLocal() || !ClientRuntime.available()) {
                return;
            }
            ClientRuntime rt = ClientRuntime.instance();
            installLocalControls(rt);
            if (activateHandler != null) {
                rt.addActivateHandler(activateHandler);
            }
        }

        private static final double LOST_DIST_SQR = 64.0 * 64.0;

        private int healCooldown;

        private void healEffectIfLost() {
            if (effect == null) {
                return;
            }
            if (--healCooldown > 0) {
                return;
            }
            healCooldown = 20;

            if (effect.isRemoved() || effect.distanceToSqr(player) > LOST_DIST_SQR) {
                respawnEffect();
            }
        }

        private void respawnEffect() {
            if (effect == null) {
                return;
            }
            boolean whiteNow = par.getWing() != Wing.BLACK;
            boolean sharpNow = par.isSharp();
            effect.discard();
            effect = new cn.academy.entity.EntityDualWing(player, par);
            cn.academy.client.render.entity.ACEffectEntities.spawn(effect);
            effect.snapTo(whiteNow, sharpNow);
        }

        @Listener(channel = MSG_TERMINATED, side = LogicalSide.CLIENT)
        private void c_terminated() {

            effect = null;

            endAll(pressFx);
            pressReported = false;

            if (crushSound != null) {
                crushSound.requestStop();
                crushSound = null;
            }

            if (blackSound != null) {
                blackSound.requestStop();
                blackSound = null;
            }

            if (flight != null) {
                flight.reset(player);
                flight = null;
            }

            if (!isLocal()) {
                return;
            }
            if (!ClientRuntime.available()) {
                return;
            }
            ClientRuntime rt = ClientRuntime.instance();
            rt.clearKeys(DualWingContext.KEY_GROUP);
            if (activateHandler != null) {
                rt.removeActiveHandler(activateHandler);
                activateHandler = null;
            }

            rt.popSuppressDefaultGroup();
            MinecraftForge.EVENT_BUS.post(new FlushControlEvent());
        }

        private static void hint(String key, Object... args) {
            Player p = Minecraft.getInstance().player;
            if (p != null) {
                p.displayClientMessage(Component.translatable(key, args), true);
            }
        }

        private final class WingKey extends KeyDelegate {

            private double downAt;

            private static final double LONG_PRESS_SEC = 0.4;

            @Override
            public void onKeyDown() {

                if (par.wing != Wing.BLACK && DualWing.addonWantsWingLongPress()) {
                    downAt = cn.lambdalib2.util.GameTimer.getTime();
                    return;
                }
                switchWing();
            }

            @Override
            public void onKeyTick() {
                if (downAt > 0 && cn.lambdalib2.util.GameTimer.getTime() - downAt >= LONG_PRESS_SEC) {
                    downAt = -1;
                    DualWing.addonOnWingLongPress();
                }
            }

            @Override
            public void onKeyUp() {
                boolean shortPress = downAt > 0;
                downAt = 0;
                if (shortPress) {
                    switchWing();
                }
            }

            @Override
            public void onKeyAbort() {
                downAt = 0;
            }

            private void switchWing() {
                par.wing = par.wing.next();

                if (par.wing != Wing.BLACK) {
                    par.sharp = false;
                }
                par.sendToServer(DualWingContext.MSG_WING, par.wing.ordinal());
                hint(par.wing == Wing.BLACK
                        ? "gui.academy.dual_wing.wing_black" : "gui.academy.dual_wing.wing_white");
            }

            @Override
            public DelegateState getState() {
                return par.getWing() == Wing.BLACK ? DelegateState.ACTIVE : DelegateState.IDLE;
            }

            @Override
            public ResourceLocation getIcon() {
                return INSTANCE.getHintIcon();
            }

            @Override
            public int createID() {
                return 1;
            }

            @Override
            public Skill getSkill() {
                return INSTANCE;
            }
        }

        private final class CrushKey extends KeyDelegate {
            @Override
            public void onKeyDown() {
                par.sendToServer(DualWingContext.MSG_CRUSH);
            }

            @Override
            public DelegateState getState() {
                return par.isCrushing() || par.isGuarding()
                        ? DelegateState.ACTIVE : DelegateState.IDLE;
            }

            @Override
            public ResourceLocation getIcon() {
                return INSTANCE.getHintIcon();
            }

            @Override
            public int createID() {
                return 2;
            }

            @Override
            public Skill getSkill() {
                return INSTANCE;
            }
        }

        private final class SharpKey extends KeyDelegate {

            @Override
            public void onKeyDown() {
                if (par.getWing() != Wing.BLACK) {
                    par.sendToServer(DualWingContext.MSG_SHARP, true);
                    return;
                }
                par.sharp = true;
                par.sendToServer(DualWingContext.MSG_SHARP, true);
            }

            @Override
            public void onKeyUp() {
                if (par.getWing() != Wing.BLACK) {
                    return;
                }
                par.sharp = false;
                par.sendToServer(DualWingContext.MSG_SHARP, false);
            }

            @Override
            public void onKeyAbort() {
                onKeyUp();
            }

            @Override
            public DelegateState getState() {
                boolean on = par.getWing() == Wing.BLACK ? par.isSharp() : par.isFeathering();
                return on ? DelegateState.ACTIVE : DelegateState.IDLE;
            }

            @Override
            public ResourceLocation getIcon() {
                return INSTANCE.getHintIcon();
            }

            @Override
            public int createID() {
                return 3;
            }

            @Override
            public Skill getSkill() {
                return INSTANCE;
            }
        }
    }
}
