package cn.academy.ability.vanilla.teleporter.skill;

import cn.academy.ACSounds;
import cn.academy.Resources;
import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientContext;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.ContextManager;
import cn.academy.ability.context.DelegateState;
import cn.academy.ability.context.KeyDelegate;
import cn.academy.ability.context.RegClientContext;
import cn.academy.client.render.entity.ACEffectEntities;
import cn.academy.config.AbilityConfig;
import cn.academy.entity.EntityMarker;
import cn.academy.entity.EntityShiftBlock;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.util.GameTimer;
import cn.lambdalib2.util.RandUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;

import static cn.lambdalib2.util.MathUtils.lerpf;

public class ShiftTeleport extends Skill {

    public static final ShiftTeleport INSTANCE = new ShiftTeleport();

    public ShiftTeleport() {
        super("shift_tp", 4);
    }

    @Override
    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    public void activate(ClientRuntime rt, int keyID) {
        rt.addKey(keyID, new ShiftKeyDelegate());
    }

    static float rangeOf(float exp) {
        return lerpf(25, 35, exp);
    }

    static float damageOf(float exp) {
        return AbilityConfig.stat("shift_tp", "damage", exp);
    }

    static boolean isBlockItem(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof BlockItem bi && bi.getBlock() != Blocks.AIR;
    }

    static boolean isNeedle(ItemStack stack) {
        return !stack.isEmpty() && stack.is(cn.academy.ACItems.NEEDLE.get());
    }

    private static final double NEEDLE_GAP = 0.4;

    private static final double NEEDLE_R_MIN = 1.0, NEEDLE_R_MAX = 2.0;

    private static double surfaceDistance(AABB box, Vec3 dir) {
        double t = Double.MAX_VALUE;
        if (Math.abs(dir.x) > 1.0e-9) t = Math.min(t, box.getXsize() * 0.5 / Math.abs(dir.x));
        if (Math.abs(dir.y) > 1.0e-9) t = Math.min(t, box.getYsize() * 0.5 / Math.abs(dir.y));
        if (Math.abs(dir.z) > 1.0e-9) t = Math.min(t, box.getZsize() * 0.5 / Math.abs(dir.z));
        return t == Double.MAX_VALUE ? 0.0 : t;
    }

    private static boolean insideAny(java.util.List<AABB> boxes, Vec3 p) {
        for (AABB b : boxes) {
            if (b.contains(p)) {
                return true;
            }
        }
        return false;
    }

    static Direction partnerDirOf(BlockState state) {
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            return state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER
                    ? Direction.UP : Direction.DOWN;
        }
        return null;
    }

    static BlockPos primaryPosOf(BlockPos pos, BlockState state) {
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) {
            return pos.below();
        }
        return pos;
    }

    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    public static final class NeedleKeyHandler {

        private static final String KEY_GROUP = "TP_Needle";

        public static void init() {
            MinecraftForge.EVENT_BUS.register(new NeedleKeyHandler());
        }

        @SubscribeEvent
        public void onClientTick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
            if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) {
                return;
            }

            if (!ClientRuntime.available()) {
                return;
            }
            Player p = net.minecraft.client.Minecraft.getInstance().player;

            if (p == null) {
                return;
            }

            boolean want = shouldShow(p);
            ClientRuntime rt = ClientRuntime.instance();

            boolean has = !rt.getDelegates(KEY_GROUP).isEmpty();
            if (want == has) {
                return;
            }
            if (want) {
                rt.addKey(KEY_GROUP, cn.lambdalib2.input.KeyManager.MOUSE_MIDDLE, new NeedleDelegate());
            } else {
                rt.clearKeys(KEY_GROUP);
            }
        }

        private static boolean shouldShow(Player p) {

            if (!isNeedle(p.getMainHandItem())
                    && !cn.academy.client.render.entity.StuckNeedles.hasAnyOwnedBy(p)) {
                return false;
            }
            if (!cn.academy.datapart.CPData.get(p).isActivated()) {
                return false;
            }
            if (!cn.academy.datapart.AbilityData.get(p).isSkillLearned(INSTANCE)) {
                return false;
            }

            cn.academy.datapart.PresetData.Preset preset =
                    cn.academy.datapart.PresetData.get(p).getCurrentPreset();
            for (int i = 0; i < cn.academy.datapart.PresetData.MAX_KEYS; i++) {
                if (preset.hasMapping(i) && preset.getControllable(i) == INSTANCE) {
                    return true;
                }
            }
            return false;
        }
    }

    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    static final class NeedleDelegate extends KeyDelegate {

        private static final int LONG_PRESS = 10;

        private int heldTicks = 0;
        private boolean recalled = false;

        @Override
        public void onKeyDown() {
            heldTicks = 0;
            recalled = false;
        }

        @Override
        public void onKeyTick() {
            if (!recalled && ++heldTicks >= LONG_PRESS) {
                recalled = true;
                fire(STContext.ACT_RECALL);
            }
        }

        @Override
        public void onKeyUp() {
            if (!recalled) {
                fire(STContext.ACT_NEEDLE);
            }
        }

        @Override
        public void onKeyAbort() {
            recalled = true;
        }

        private void fire(int action) {
            STContext c = new STContext(getPlayer(), action);
            ContextManager.instance.activate(c);
            c.sendToSelf(Context.MSG_KEYDOWN);
        }

        @Override
        public ResourceLocation getIcon() {
            return Resources.getTexture("item/needle");
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

    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    class ShiftKeyDelegate extends KeyDelegate {

        private final ResourceLocation iconLocked =
                Resources.getTexture("abilities/teleporter/skills/shift_tp_locked");

        private STContext context;

        @Override
        public void onKeyDown() {
            checkContext();
            if (context != null) {
                context.sendToSelf(Context.MSG_KEYDOWN);
                return;
            }
            STContext c = new STContext(getPlayer());
            ContextManager.instance.activate(c);
            context = c;
            c.sendToSelf(Context.MSG_KEYDOWN);
        }

        private void checkContext() {
            if (context != null && context.getStatus() == Context.Status.TERMINATED) {
                context = null;
            }
        }

        @Override
        public DelegateState getState() {
            checkContext();
            return context != null ? DelegateState.ACTIVE : DelegateState.IDLE;
        }

        @Override
        public ResourceLocation getIcon() {
            checkContext();
            if (context != null && context.locked && GameTimer.getTime() % 0.5 < 0.3) {
                return iconLocked;
            }
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

    public static class STContext extends Context<ShiftTeleport> {

        static final String MSG_LAUNCH = "launch";
        static final String MSG_LOCK = "lock";
        static final String MSG_LOCK_OK = "lock_ok";
        static final String MSG_FIRE = "fire";
        static final String MSG_NEEDLE = "needle";
        static final String MSG_RECALL = "recall";

        static final double RECALL_RADIUS = 64.0;

        static final double AIM_CONE_DEG = 12.0;

        static final int ACT_NORMAL = 0;
        static final int ACT_NEEDLE = 1;
        static final int ACT_RECALL = 2;

        final int keyAction;

        static final int LIFT_HEIGHT = 5;

        static final double LOCK_REACH_LV5 = 12.0;

        final float exp = ctx.getSkillExp();
        final float range = rangeOf(exp);

        public boolean locked = false;
        BlockPos lockedPos = null;

        BlockState lockedState = null;

        public STContext(Player player) {
            this(player, ACT_NORMAL);
        }

        public STContext(Player player, int keyAction) {
            super(player, INSTANCE);
            this.keyAction = keyAction;
        }

        @Listener(channel = MSG_KEYDOWN, side = LogicalSide.CLIENT)
        private void l_onKeyDown() {
            if (locked) {
                LivingEntity t = traceEntity();
                sendToServer(MSG_FIRE, t != null ? t.getId() : -1);
                return;
            }

            if (keyAction == ACT_NEEDLE) {
                LivingEntity t = traceEntity();
                if (t != null) {
                    sendToServer(MSG_NEEDLE, t.getId());
                } else {
                    terminate();
                }
                return;
            }

            if (keyAction == ACT_RECALL) {
                sendToServer(MSG_RECALL);
                return;
            }
            if (isBlockItem(player.getMainHandItem())) {
                sendToServer(MSG_LAUNCH);
                return;
            }
            BlockHitResult hit = traceBlock();
            if (hit != null) {
                BlockPos p = hit.getBlockPos();
                sendToServer(MSG_LOCK, p.getX(), p.getY(), p.getZ());
            } else {
                terminate();
            }
        }

        @Listener(channel = MSG_LOCK_OK, side = LogicalSide.CLIENT)
        private void c_lockOk(int x, int y, int z, int tall) {
            locked = true;
            lockedPos = new BlockPos(x, y, z);
        }

        @Listener(channel = MSG_LAUNCH, side = LogicalSide.SERVER)
        private void s_launch() {
            ItemStack stack = player.getMainHandItem();
            if (!isBlockItem(stack)) {
                terminate();
                return;
            }
            if (!ctx.consume(AbilityConfig.overload("shift_tp", exp), AbilityConfig.cp("shift_tp", exp))) {
                terminate();
                return;
            }
            BlockState state = ((BlockItem) stack.getItem()).getBlock().defaultBlockState();
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            Vec3 look = player.getLookAngle().normalize();
            Vec3 origin = player.getEyePosition().add(look.scale(1.2)).subtract(0, 0.49, 0);

            for (int i = 0; i < 3 && !freeAt(origin); i++) {
                origin = origin.subtract(look.scale(0.4));
            }
            EntityShiftBlock e = new EntityShiftBlock(player, state, origin, look,
                    range, damageOf(exp));
            player.level().addFreshEntity(e);
            finishCast(player.position());
            terminate();
        }

        @Listener(channel = MSG_NEEDLE, side = LogicalSide.SERVER)
        private void s_needle(int targetId) {
            Level level = player.level();
            if (!isNeedle(player.getMainHandItem())) {
                terminate();
                return;
            }
            Entity cand = level.getEntity(targetId);
            if (!(cand instanceof LivingEntity target) || !cand.isAlive() || cand == player
                    || player.distanceToSqr(cand) > (range * 1.5) * (range * 1.5)) {
                terminate();
                return;
            }

            ItemStack held = player.getMainHandItem();
            int count = held.getCount();
            Vec3 c = target.getBoundingBox().getCenter();

            java.util.List<Vec3> spots = new java.util.ArrayList<>();
            final double golden = Math.PI * (3.0 - Math.sqrt(5.0));
            double phase = RandUtils.ranged(0, Math.PI * 2);

            java.util.List<AABB> bodies = new java.util.ArrayList<>();
            if (target.isMultipartEntity() && target.getParts() != null && target.getParts().length > 0) {
                for (net.minecraftforge.entity.PartEntity<?> part : target.getParts()) {
                    bodies.add(part.getBoundingBox().inflate(0.3));
                }
            } else {
                bodies.add(target.getBoundingBox().inflate(0.3));
            }
            for (int i = 0; i < count; i++) {
                double uy = 1.0 - (i + 0.5) * 2.0 / count;
                double rxz = Math.sqrt(Math.max(0, 1.0 - uy * uy));
                double theta = golden * i + phase;
                Vec3 dir = new Vec3(Math.cos(theta) * rxz, uy, Math.sin(theta) * rxz);

                AABB part = bodies.get(i % bodies.size());
                Vec3 pc = part.getCenter();

                double exit = surfaceDistance(part, dir) + NEEDLE_GAP;
                double hi = Math.max(NEEDLE_R_MAX, exit);
                double lo = Math.min(hi, Math.max(NEEDLE_R_MIN, exit));
                for (double r = hi; r >= lo - 1.0e-9; r -= 0.25) {
                    Vec3 p = pc.add(dir.scale(r));

                    if (level.getBlockState(BlockPos.containing(p)).isAir() && !insideAny(bodies, p)) {
                        spots.add(p);
                        break;
                    }
                }
            }
            if (spots.isEmpty()) {
                player.sendSystemMessage(Component.translatable("gui.academy.shift.err_no_room"));
                terminate();
                return;
            }
            if (!ctx.consume(AbilityConfig.overload("shift_tp", exp), AbilityConfig.cp("shift_tp", exp))) {
                terminate();
                return;
            }

            held.shrink(spots.size());

            float each = AbilityConfig.stat("shift_tp", "needle_damage", exp);
            for (Vec3 spot : spots) {
                level.addFreshEntity(new cn.academy.entity.EntityShiftNeedle(
                        player, target, each, spot));
            }

            level.playSound(null, c.x, c.y, c.z, ACSounds.TP_MOVE_BLOCK.get(),
                    SoundSource.AMBIENT, 1.0f, 1.0f);
            ctx.addSkillExp(0.002f);
            ctx.setCooldown((int) AbilityConfig.cooldown("shift_tp", exp));
            terminate();
        }

        java.util.List<cn.academy.entity.EntityShiftNeedle> myStuckNeedles() {
            return player.level().getEntitiesOfClass(cn.academy.entity.EntityShiftNeedle.class,
                    player.getBoundingBox().inflate(RECALL_RADIUS),
                    n -> n.isStuck() && n.isOwnedBy(player));
        }

        boolean hasAnyStuckNeedles() {
            return !myStuckNeedles().isEmpty();
        }

        @Listener(channel = MSG_RECALL, side = LogicalSide.SERVER)
        private void s_recall() {
            java.util.List<cn.academy.entity.EntityShiftNeedle> mine = myStuckNeedles();
            if (mine.isEmpty()) {
                terminate();
                return;
            }
            for (cn.academy.entity.EntityShiftNeedle n : mine) {
                n.retrieveToOwner();
            }

            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    ACSounds.TP_MOVE_BLOCK_SPEED.get(), SoundSource.AMBIENT, 1.0f, 1.0f);
            terminate();
        }

        @Listener(channel = MSG_LOCK, side = LogicalSide.SERVER)
        private void s_lock(int x, int y, int z) {
            if (locked) {
                return;
            }
            Level level = player.level();

            BlockPos pos = primaryPosOf(new BlockPos(x, y, z), level.getBlockState(new BlockPos(x, y, z)));

            String err = lockError(pos);
            if (err != null) {
                player.sendSystemMessage(Component.translatable(err));
                terminate();
                return;
            }

            BlockState state = level.getBlockState(pos);

            BlockPos lift = partnerDirOf(state) != null ? pos : liftTargetFor(pos);

            if (!lift.equals(pos) && state.canSurvive(level, lift)) {
                level.removeBlock(pos, false);
                level.setBlock(lift, state, 3);
                spawnTPBurst(Vec3.atCenterOf(pos));
                spawnTPBurst(Vec3.atCenterOf(lift));

                level.playSound(null, lift.getX() + 0.5, lift.getY() + 0.5, lift.getZ() + 0.5,
                        ACSounds.TP_MOVE_BLOCK.get(), SoundSource.AMBIENT, 1.0f, 1.0f);
                pos = lift;
            }

            locked = true;
            lockedPos = pos;
            lockedState = level.getBlockState(pos);
            suppressFall();

            boolean tall = partnerDirOf(lockedState) == Direction.UP;
            sendToClient(MSG_LOCK_OK, pos.getX(), pos.getY(), pos.getZ(), tall ? 1 : 0);
        }

        private BlockPos liftTargetFor(BlockPos pos) {
            BlockPos best = pos;
            for (int i = 1; i <= LIFT_HEIGHT; i++) {
                BlockPos p = pos.above(i);
                if (!canHostBlock(p)) {
                    break;
                }
                best = p;
            }
            return best;
        }

        private boolean canHostBlock(BlockPos p) {
            Level level = player.level();
            if (!level.getBlockState(p).canBeReplaced()) {
                return false;
            }
            return level.getEntities((Entity) null, new AABB(p),
                    e -> e instanceof LivingEntity && e.isAlive()).isEmpty();
        }

        private void spawnTPBurst(Vec3 at) {
            if (player.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                sl.sendParticles(cn.academy.ACParticles.TP.get(), at.x, at.y, at.z, 14,
                        0.45, 0.45, 0.45, 0.02);
            }
        }

        @Listener(channel = MSG_TICK, side = LogicalSide.SERVER)
        private void s_tick() {
            if (!locked) {
                return;
            }

            if (!ctx.cpData.canUseAbility()) {
                terminate();
                return;
            }
            if (player.level().getBlockState(lockedPos) != lockedState) {
                terminate();
                return;
            }
            suppressFall();
            double breakDist = range * 1.25;
            if (player.distanceToSqr(Vec3.atCenterOf(lockedPos)) > breakDist * breakDist) {
                terminate();
            }
        }

        private boolean isGravityBlock() {
            return lockedState != null && lockedState.getBlock() instanceof FallingBlock;
        }

        private void suppressFall() {
            if (!isGravityBlock() || !(player.level() instanceof ServerLevel sl)) {
                return;
            }
            sl.getBlockTicks().clearArea(new BoundingBox(lockedPos));

            sl.scheduleTick(lockedPos, lockedState.getBlock(), 2);
        }

        @Listener(channel = MSG_TERMINATED, side = LogicalSide.SERVER)
        private void s_onEnd() {
            if (locked && isGravityBlock()
                    && player.level().getBlockState(lockedPos) == lockedState) {
                player.level().scheduleTick(lockedPos, lockedState.getBlock(), 2);
            }
        }

        @Listener(channel = MSG_FIRE, side = LogicalSide.SERVER)
        private void s_fire(int targetId) {
            Level level = player.level();
            if (!locked || level.getBlockState(lockedPos) != lockedState) {
                terminate();
                return;
            }
            double breakDist = range * 1.25;
            if (player.distanceToSqr(Vec3.atCenterOf(lockedPos)) > breakDist * breakDist) {
                terminate();
                return;
            }
            if (!ctx.canBreakBlock(level, lockedPos)) {
                player.sendSystemMessage(Component.translatable("gui.academy.shift.err_protect"));
                terminate();
                return;
            }
            if (!ctx.consume(AbilityConfig.overload("shift_tp", exp), AbilityConfig.cp("shift_tp", exp))) {
                terminate();
                return;
            }

            BlockState state = level.getBlockState(lockedPos);

            Direction pd = partnerDirOf(state);
            BlockState partnerState = null;
            if (pd != null) {
                BlockPos pp = lockedPos.relative(pd);
                BlockState ps = level.getBlockState(pp);
                if (!ps.is(state.getBlock())) {
                    player.sendSystemMessage(Component.translatable("gui.academy.shift.err_structure"));
                    terminate();
                    return;
                }
                partnerState = ps;
                level.setBlock(pp, Blocks.AIR.defaultBlockState(), 2 | 16 | 32);
            }
            level.setBlock(lockedPos, Blocks.AIR.defaultBlockState(), 35);

            Entity target = null;
            if (targetId >= 0) {
                Entity cand = level.getEntity(targetId);
                if (cand instanceof LivingEntity && cand.isAlive() && cand != player
                        && player.distanceToSqr(cand) <= (range * 2.0) * (range * 2.0)) {
                    target = cand;
                }
            }

            Vec3 own = Vec3.atBottomCenterOf(lockedPos);
            Vec3 dir = fireDirFrom(own.add(0, 0.49, 0), target);
            Vec3 origin = own;
            if (!freeAt(own) || !freeAt(own.add(dir))) {
                origin = muzzleFor(lockedPos);
                dir = fireDirFrom(origin.add(0, 0.49, 0), target);
            }

            EntityShiftBlock e = new EntityShiftBlock(player, state, origin, dir,
                    range, damageOf(exp));
            if (partnerState != null) {
                e.setPartner(partnerState, pd);
            }
            if (target != null) {
                e.setHomingTarget(target.getId());
            }
            level.addFreshEntity(e);
            finishCast(Vec3.atCenterOf(lockedPos));
            terminate();
        }

        private void finishCast(Vec3 at) {
            ctx.addSkillExp(0.002f);
            ctx.setCooldown((int) AbilityConfig.cooldown("shift_tp", exp));
            player.level().playSound(null, at.x, at.y, at.z,
                    ACSounds.TP_SHIFT.get(), SoundSource.AMBIENT, 0.5f, 1.0f);
        }

        private Vec3 fireDirFrom(Vec3 from, Entity target) {
            Vec3 d = target != null
                    ? target.getBoundingBox().getCenter().subtract(from)
                    : aimPoint().subtract(from);
            return d.lengthSqr() < 9.0 ? player.getLookAngle() : d.normalize();
        }

        private boolean freeAt(Vec3 feet) {
            AABB box = new AABB(feet.x - 0.49, feet.y, feet.z - 0.49,
                    feet.x + 0.49, feet.y + 0.98, feet.z + 0.49);
            return player.level().noCollision(box.deflate(0.02));
        }

        private Vec3 muzzleFor(BlockPos pos) {
            Vec3 eye = player.getEyePosition(1.0f);
            Vec3 best = null;
            double bestDist = Double.MAX_VALUE;
            for (Direction d : Direction.values()) {
                Vec3 feet = Vec3.atBottomCenterOf(pos.relative(d));
                if (!freeAt(feet)) {
                    continue;
                }
                double dist = feet.distanceToSqr(eye);
                if (dist < bestDist) {
                    bestDist = dist;
                    best = feet;
                }
            }
            return best != null ? best : Vec3.atBottomCenterOf(pos);
        }

        private String lockError(BlockPos pos) {
            Level level = player.level();

            double reach = lockReach() + 1.0;
            if (player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)) > reach * reach) {
                return "gui.academy.shift.err_range";
            }
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                return "gui.academy.shift.err_range";
            }
            if (state.getDestroySpeed(level, pos) < 0) {
                return "gui.academy.shift.err_unbreakable";
            }
            if (level.getBlockEntity(pos) != null) {
                return "gui.academy.shift.err_container";
            }
            if (!ctx.canBreakBlock(level, pos)) {
                return "gui.academy.shift.err_protect";
            }

            Direction pd = partnerDirOf(state);
            if (pd != null) {
                BlockPos pp = pos.relative(pd);
                if (!level.getBlockState(pp).is(state.getBlock())) {
                    return "gui.academy.shift.err_structure";
                }
                if (level.getBlockEntity(pp) != null || !ctx.canBreakBlock(level, pp)) {
                    return "gui.academy.shift.err_protect";
                }
            }

            if (state.getBlock() instanceof net.minecraft.world.level.block.piston.PistonHeadBlock
                    || state.getBlock() instanceof net.minecraft.world.level.block.piston.MovingPistonBlock) {
                return "gui.academy.shift.err_structure";
            }
            return null;
        }

        private double lockReach() {
            double reach = player.getBlockReach();
            return ctx.aData.getLevel() >= 5 ? Math.max(reach, LOCK_REACH_LV5) : reach;
        }

        private BlockHitResult traceBlock() {
            Vec3 eye = player.getEyePosition(1.0f);
            Vec3 end = eye.add(player.getViewVector(1.0f).scale(lockReach()));
            BlockHitResult hit = player.level().clip(new ClipContext(
                    eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
            return hit.getType() == HitResult.Type.BLOCK ? hit : null;
        }

        private LivingEntity traceEntity() {
            Vec3 eye = player.getEyePosition(1.0f);
            Vec3 look = player.getViewVector(1.0f).normalize();
            double cosLimit = Math.cos(Math.toRadians(AIM_CONE_DEG));

            LivingEntity best = null;
            double bestCos = cosLimit;
            for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(range),

                    e -> e != player && e.isAlive() && ctx.canTarget(e))) {
                Vec3 center = e.getBoundingBox().getCenter();
                Vec3 to = center.subtract(eye);
                double dist = to.length();
                if (dist < 1.0e-4 || dist > range) {
                    continue;
                }
                double cos = to.scale(1.0 / dist).dot(look);
                if (cos <= bestCos) {
                    continue;
                }
                if (player.level().clip(new ClipContext(eye, center,
                        ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player))
                        .getType() == HitResult.Type.BLOCK) {
                    continue;
                }
                bestCos = cos;
                best = e;
            }
            return best;
        }

        private Vec3 aimPoint() {
            Vec3 eye = player.getEyePosition(1.0f);
            Vec3 end = eye.add(player.getLookAngle().scale(range));
            BlockHitResult hit = player.level().clip(new ClipContext(
                    eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            return hit.getType() == HitResult.Type.BLOCK ? hit.getLocation() : end;
        }
    }

    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    @RegClientContext(STContext.class)
    public static class STContextC extends ClientContext {

        private final STContext par;
        private EntityMarker marker = null;

        public STContextC(STContext par) {
            super(par);
            this.par = par;
        }

        @Listener(channel = STContext.MSG_LOCK_OK, side = LogicalSide.CLIENT)
        private void c_lockOk(int x, int y, int z, int tall) {
            if (isLocal() && marker == null) {
                marker = new EntityMarker(player.level());
                marker.boxWidth = 1.2f;
                marker.boxHeight = tall != 0 ? 2.2f : 1.2f;
                marker.color.set(235, 81, 81, 180);
                marker.moveTo2(x + 0.5, y, z + 0.5);
                ACEffectEntities.spawn(marker);
            }
        }

        @Listener(channel = MSG_TICK, side = LogicalSide.CLIENT)
        private void l_tick() {
            if (isLocal() && marker != null && par.lockedPos != null) {
                marker.moveTo2(par.lockedPos.getX() + 0.5, par.lockedPos.getY(), par.lockedPos.getZ() + 0.5);
                marker.touch();
            }
        }

        @Listener(channel = MSG_TERMINATED, side = LogicalSide.CLIENT)
        private void c_end() {
            if (isLocal() && marker != null) {
                marker.discard();
                marker = null;
            }
        }
    }
}
