package cn.academy.ability.vanilla.mentalout.skill;

import cn.academy.ability.AbilityPipeline;
import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientContext;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.KeyDelegate;
import cn.academy.ability.context.RegClientContext;
import cn.academy.ability.vanilla.mentalout.ControlState;
import cn.academy.ability.vanilla.mentalout.WideCastable;
import cn.academy.ability.vanilla.mentalout.passiveskill.MindManip;
import cn.academy.client.render.entity.ACEffectEntities;
import cn.academy.config.AbilityConfig;
import cn.academy.datapart.AbilityData;
import cn.academy.entity.EntityMarker;
import cn.academy.util.AimTrace;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.LogicalSide;

public class ForcedControl extends Skill implements WideCastable {

    public static final ForcedControl INSTANCE = new ForcedControl();

    private static final String PENDING = "mo_fc_pending";

    private static final String PENDING_AT = "mo_fc_pending_at";

    private static final int PENDING_TIMEOUT = 400;

    public static final String MSG_CMD_SYNC = "fc_cmd_sync";

    @Listener(channel = MSG_CMD_SYNC, side = LogicalSide.CLIENT)
    private void c_cmdSync(Entity target, Integer ticks, Integer cmd, Long dest, Long home,
                           Boolean leg, Integer targetId) {
        if (!(target instanceof LivingEntity le) || ticks == null || cmd == null) {
            return;
        }
        ControlState.applySync(le, ticks, cmd, dest == null ? 0L : dest,
                home == null ? 0L : home, Boolean.TRUE.equals(leg),
                targetId == null ? -1 : targetId);
    }

    private ForcedControl() {
        super("forced_control", 3);
    }

    static float rangeOf(float exp) {
        return AbilityConfig.stat("forced_control", "range", exp);
    }

    public static ResourceLocation iconOf(ControlState.Command cmd) {
        return cn.academy.Resources.getTexture("abilities/mentalout/commands/" + cmd.key());
    }

    public static Component nameOf(ControlState.Command cmd) {
        return Component.translatable("gui.academy.forced_control.cmd." + cmd.key());
    }

    static LivingEntity traceMob(Player player, float range) {
        Vec3 eye = player.getEyePosition(1.0f);
        Vec3 end = eye.add(player.getViewVector(1.0f).scale(range));
        BlockHitResult block = player.level().clip(new ClipContext(
                eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        Vec3 clipEnd = block.getType() == HitResult.Type.BLOCK ? block.getLocation() : end;

        EntityHitResult ent = AimTrace.firstResult(player.level(), player, eye, clipEnd,
                e -> e != player && e.isAlive() && e instanceof LivingEntity
                        && AbilityPipeline.canTarget(player, e));
        return ent != null && ent.getEntity() instanceof LivingEntity le ? le : null;
    }

    public static LivingEntity traceVictim(Player player, float range) {
        Vec3 eye = player.getEyePosition(1.0f);
        Vec3 end = eye.add(player.getViewVector(1.0f).scale(range));
        BlockHitResult block = player.level().clip(new ClipContext(
                eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        Vec3 clipEnd = block.getType() == HitResult.Type.BLOCK ? block.getLocation() : end;

        EntityHitResult ent = AimTrace.firstResult(player.level(), player, eye, clipEnd,
                e -> e != player && e.isAlive() && e instanceof LivingEntity
                        && AbilityPipeline.canTarget(player, e));
        return ent != null && ent.getEntity() instanceof LivingEntity le ? le : null;
    }

    static LivingEntity traceAny(Player player, float range) {
        Vec3 eye = player.getEyePosition(1.0f);
        Vec3 end = eye.add(player.getViewVector(1.0f).scale(range));
        BlockHitResult block = player.level().clip(new ClipContext(
                eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        Vec3 clipEnd = block.getType() == HitResult.Type.BLOCK ? block.getLocation() : end;

        EntityHitResult ent = AimTrace.firstResult(player.level(), player, eye, clipEnd,
                e -> e != player && e.isAlive() && e instanceof LivingEntity);
        return ent != null && ent.getEntity() instanceof LivingEntity le ? le : null;
    }

    public static BlockPos traceBlock(Player player, float range) {
        Vec3 eye = player.getEyePosition(1.0f);
        Vec3 end = eye.add(player.getViewVector(1.0f).scale(range));
        BlockHitResult hit = player.level().clip(new ClipContext(
                eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.BLOCK
                ? hit.getBlockPos().relative(hit.getDirection()) : null;
    }

    public static float failChance(Player player, float exp) {
        if (MindManip.obeysAlways(player)) {
            return 0f;
        }
        return AbilityConfig.stat("forced_control", "fail_chance", exp);
    }

    @Override
    public boolean wideAccepts(WideCastable.Call call, LivingEntity target) {

        return true;
    }

    @Override
    public int wideOptionCount() {
        return ControlState.Command.values().length;
    }

    @Override
    public Component wideOptionName(int id) {
        return nameOf(ControlState.Command.byId(id));
    }

    @Override
    public ResourceLocation wideOptionIcon(int id) {
        return iconOf(ControlState.Command.byId(id));
    }

    @Override
    public boolean wideAffectsAlliesWhenOff() {
        return true;
    }

    @Override
    public boolean wideUniquePerProgram() {
        return true;
    }

    @Override
    public boolean wideSwitchesToAim(int commandId) {

        return ControlState.Command.byId(commandId).needsSecondPick();
    }

    @Override
    public boolean releaseFrom(Player caster, LivingEntity target) {

        if (!ControlState.isControlled(target) || ControlState.ownerOf(target) != caster) {
            return false;
        }
        ControlState.release(target);
        return true;
    }

    @Override
    public float wideExp() {
        return 0.004f;
    }

    @Override
    public boolean wideIsRelease(WideCastable.Call call) {
        ControlState.Command cmd = ControlState.Command.byId(call.commandId);
        return cmd == ControlState.Command.RESTORE || cmd == ControlState.Command.STOP;
    }

    @Override
    public boolean wideApply(WideCastable.Call call, LivingEntity target) {
        ControlState.Command cmd = ControlState.Command.byId(call.commandId);

        if (cmd == ControlState.Command.RESTORE) {
            if (WideCastable.releaseAll(call.caster, target) == 0) {
                return false;
            }
            cn.academy.ability.vanilla.mentalout.WideCastFx.at(target,
                    net.minecraft.core.particles.ParticleTypes.ENCHANT, 14, 0.05);
            return true;
        }

        LivingEntity mob = target;

        if (cmd == ControlState.Command.STOP) {
            if (!releaseFrom(call.caster, mob)) {
                return false;
            }
            cn.academy.ability.vanilla.mentalout.WideCastFx.at(mob,
                    net.minecraft.core.particles.ParticleTypes.ENCHANT, 14, 0.05);
            return true;
        }
        LivingEntity victim = null;
        BlockPos dest = null;
        if (cmd.needsSecondPick()) {
            if (cmd.secondPickIsEntity()) {
                victim = call.aimEntity;
                if (victim == null || victim == mob) {
                    return false;
                }
            } else {
                dest = call.aimBlock;
                if (dest == null) {
                    return false;
                }
            }
        }
        if (call.caster.level().getRandom().nextFloat() < failChance(call.caster, call.exp)) {
            return false;
        }
        ControlState.issue(mob, call.caster, cmd, dest, victim,
                (int) AbilityConfig.stat("forced_control", "duration", call.exp));
        cn.academy.ability.vanilla.mentalout.WideCastFx.at(mob,
                net.minecraft.core.particles.ParticleTypes.ENCHANT, 14, 0.05);
        return true;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void activate(ClientRuntime rt, int keyID) {
        activateSingleKey2(rt, keyID, ControlContext::new);
    }

    public static class ControlContext extends Context<ForcedControl> {

        static final String MSG_PICK = "pick";

        static final String MSG_RESULT = "result";

        private final float exp = ctx.getSkillExp();

        public ControlContext(Player player) {
            super(player, INSTANCE);
        }

        @Listener(channel = MSG_KEYUP, side = LogicalSide.CLIENT)
        private void l_onKeyUp() {
            sendToServer(MSG_PICK, MiddleKey.clientCommand().ordinal());
        }

        @Listener(channel = MSG_KEYABORT, side = LogicalSide.CLIENT)
        private void l_onKeyAbort() {
            terminate();
        }

        @Listener(channel = MSG_PICK, side = LogicalSide.SERVER)
        private void s_pick(Integer cmdId) {
            float range = rangeOf(exp);
            ControlState.Command cmd = ControlState.Command.byId(cmdId == null ? 0 : cmdId);
            net.minecraft.nbt.CompoundTag pd = player.getPersistentData();
            long now = player.level().getGameTime();

            if (pd.contains(PENDING) && now - pd.getLong(PENDING_AT) > PENDING_TIMEOUT) {
                pd.remove(PENDING);
            }

            if (cmd == ControlState.Command.RESTORE) {
                pd.remove(PENDING);
                LivingEntity t = traceAny(player, range);
                if (t == null) {
                    finish("no_target");
                    return;
                }
                if (!ctx.consume(AbilityConfig.overload("forced_control", exp),
                        (int) AbilityConfig.cp("forced_control", exp))) {
                    finish("no_cp");
                    return;
                }
                ctx.setCooldown((int) AbilityConfig.cooldown("forced_control", exp));
                ctx.addSkillExp(0.004f);
                int n = cn.academy.ability.vanilla.mentalout.WideCastable.releaseAll(player, t);
                finish(n > 0 ? "released" : "nothing");
                return;
            }

            if (cmd == ControlState.Command.STOP) {
                pd.remove(PENDING);
                LivingEntity m = traceMob(player, range);
                if (m == null) {
                    finish("no_target");
                    return;
                }
                if (!ctx.consume(AbilityConfig.overload("forced_control", exp),
                        (int) AbilityConfig.cp("forced_control", exp))) {
                    finish("no_cp");
                    return;
                }
                ctx.setCooldown((int) AbilityConfig.cooldown("forced_control", exp));
                ctx.addSkillExp(0.004f);
                finish(INSTANCE.releaseFrom(player, m) ? "stopped" : "no_command");
                return;
            }

            if (!pd.contains(PENDING)) {
                LivingEntity mob = traceMob(player, range);
                if (mob == null) {
                    finish("no_target");
                    return;
                }
                if (!cmd.needsSecondPick()) {
                    issue(mob, cmd, null, null);
                    return;
                }
                pd.putInt(PENDING, mob.getId());
                pd.putLong(PENDING_AT, now);
                finish("picked");
                return;
            }

            Entity first = player.level().getEntity(pd.getInt(PENDING));
            pd.remove(PENDING);
            if (!(first instanceof LivingEntity mob) || !mob.isAlive()) {
                finish("lost");
                return;
            }
            if (cmd.secondPickIsEntity()) {
                LivingEntity victim = traceVictim(player, range);
                if (victim == null || victim == mob) {
                    finish("no_target");
                    return;
                }
                issue(mob, cmd, null, victim);
            } else {
                BlockPos dest = traceBlock(player, range);
                if (dest == null) {
                    finish("no_target");
                    return;
                }
                issue(mob, cmd, dest, null);
            }
        }

        private void issue(LivingEntity mob, ControlState.Command cmd, BlockPos dest,
                           LivingEntity victim) {
            if (!ctx.consume(AbilityConfig.overload("forced_control", exp),
                    (int) AbilityConfig.cp("forced_control", exp))) {
                finish("no_cp");
                return;
            }
            ctx.setCooldown((int) AbilityConfig.cooldown("forced_control", exp));
            ctx.addSkillExp(0.004f);

            if (cn.academy.ability.vanilla.mentalout.MentalImmune.blocked(ctx, mob)) {
                finish("immune");
                return;
            }

            if (player.level().getRandom().nextFloat() < failChance(player, exp)) {
                finish("refused");
                return;
            }
            ControlState.issue(mob, player, cmd, dest, victim,
                    (int) AbilityConfig.stat("forced_control", "duration", exp));
            finish("ok");
        }

        private void finish(String result) {
            sendToClient(MSG_RESULT, result);
            terminate();
        }
    }

    @OnlyIn(Dist.CLIENT)
    @RegClientContext(ControlContext.class)
    public static class ControlContextC extends ClientContext {

        private static final int[] COLOR_IDLE = {0xba, 0xba, 0xba};
        private static final int[] COLOR_LOCK = {0xff, 0xcd, 0x46};

        private static boolean awaitingSecond = false;

        private final ControlContext par;
        private EntityMarker marker = null;

        public ControlContextC(ControlContext par) {
            super(par);
            this.par = par;
        }

        @Listener(channel = MSG_MADEALIVE, side = LogicalSide.CLIENT)
        private void l_start() {
            if (isLocal()) {
                marker = new EntityMarker(player.level());
                marker.boxWidth = 0.6f;
                marker.boxHeight = 0.6f;
                marker.moveTo2(player.getX(), player.getY(), player.getZ());
                ACEffectEntities.spawn(marker);
            }
        }

        @Listener(channel = MSG_TICK, side = LogicalSide.CLIENT)
        private void l_tick() {
            if (!isLocal() || marker == null) {
                return;
            }
            float range = rangeOf(par.exp);
            ControlState.Command cmd = MiddleKey.clientCommand();

            if (awaitingSecond && !cmd.secondPickIsEntity()) {

                BlockPos bp = traceBlock(player, range);
                marker.target = null;
                marker.boxWidth = 1.0f;
                marker.boxHeight = 1.0f;
                if (bp != null) {
                    marker.moveTo2(bp.getX() + 0.5, bp.getY(), bp.getZ() + 0.5);
                    marker.color.set(COLOR_LOCK[0], COLOR_LOCK[1], COLOR_LOCK[2], 255);
                } else {
                    Vec3 end = player.getEyePosition(1.0f)
                            .add(player.getViewVector(1.0f).scale(range));
                    marker.moveTo2(end.x, end.y, end.z);
                    marker.color.set(COLOR_IDLE[0], COLOR_IDLE[1], COLOR_IDLE[2], 255);
                }
                marker.touch();
                return;
            }

            LivingEntity t = awaitingSecond ? traceVictim(player, range) : traceMob(player, range);
            marker.boxWidth = 0.6f;
            marker.boxHeight = 0.6f;
            if (t != null) {
                marker.moveTo2(t.getX(), t.getY(), t.getZ());
            } else {
                Vec3 end = player.getEyePosition(1.0f)
                        .add(player.getViewVector(1.0f).scale(range));
                marker.moveTo2(end.x, end.y, end.z);
            }
            marker.target = t;
            int[] c = t != null ? COLOR_LOCK : COLOR_IDLE;
            marker.color.set(c[0], c[1], c[2], 255);
            marker.touch();
        }

        @Listener(channel = MSG_TERMINATED, side = LogicalSide.CLIENT)
        private void l_terminated() {
            dropMarker();
        }

        @Listener(channel = ControlContext.MSG_RESULT, side = LogicalSide.CLIENT)
        private void c_result(String result) {
            dropMarker();
            if (!isLocal() || result == null) {
                return;
            }

            awaitingSecond = "picked".equals(result);
            player.displayClientMessage(
                    Component.translatable("gui.academy.forced_control.result." + result), true);
            boolean good = "ok".equals(result) || "picked".equals(result);
            player.level().playLocalSound(player.getX(), player.getY(), player.getZ(),

                    (good ? cn.academy.ACSounds.V_AMETHYST_CHIME : cn.academy.ACSounds.V_ITEM_BREAK).get(),
                    SoundSource.PLAYERS, 0.7f, good ? 1.4f : 0.8f, false);
        }

        private void dropMarker() {
            if (marker != null) {
                marker.discard();
                marker = null;
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static final class MiddleKey {

        private static final String KEY_GROUP = "MO_ForcedControlCmd";

        private static boolean equipped(net.minecraft.world.entity.player.Player p) {
            if (p == null) {
                return false;
            }

            if (cn.academy.client.auxgui.QuickPlanSelector.holdingRemote(p)) {
                return false;
            }

            if (!cn.lambdalib2.datapart.EntityData.isReady(p)) {
                return false;
            }
            AbilityData data = AbilityData.get(p);
            if (!data.isSkillLearned(INSTANCE)) {
                return false;
            }
            cn.academy.datapart.PresetData.Preset preset =
                    cn.academy.datapart.PresetData.get(p).getCurrentPreset();
            for (int i = 0; i < cn.academy.datapart.PresetData.MAX_KEYS; ++i) {
                if (preset.hasMapping(i) && preset.getControllable(i) == INSTANCE) {
                    return true;
                }
            }
            return false;
        }

        private static ControlState.Command clientCmd = ControlState.Command.ATTACK;
        private static int heldTicks = 0;
        private static boolean wheelOpened = false;

        private MiddleKey() {}

        public static ControlState.Command clientCommand() {
            return clientCmd;
        }

        public static void setClientCommand(ControlState.Command cmd) {
            clientCmd = cmd;
        }

        public static void init() {
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new MiddleKey());
        }

        @net.minecraftforge.eventbus.api.SubscribeEvent
        public void onClientTick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
            if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) {
                return;
            }

            if (!ClientRuntime.available()) {
                return;
            }
            boolean want = equipped(net.minecraft.client.Minecraft.getInstance().player);
            ClientRuntime rt = ClientRuntime.instance();
            boolean has = !rt.getDelegates(KEY_GROUP).isEmpty();
            if (want == has) {
                return;
            }
            if (want) {
                rt.addKey(KEY_GROUP, cn.lambdalib2.input.KeyManager.MOUSE_MIDDLE, new WheelDelegate());
            } else {
                rt.clearKeys(KEY_GROUP);
            }
        }

        private static final class WheelDelegate extends KeyDelegate {

            @Override
            public void onKeyDown() {
                heldTicks = 0;
                wheelOpened = false;
            }

            @Override
            public void onKeyTick() {
                if (wheelOpened) {
                    return;
                }
                heldTicks++;
                if (heldTicks >= holdThreshold()) {
                    wheelOpened = true;
                    cn.academy.client.gui.CommandWheelScreen.open();
                }
            }

            @Override
            public void onKeyUp() {
                if (wheelOpened) {

                    wheelOpened = false;
                    return;
                }
                setClientCommand(clientCmd.next());
                net.minecraft.client.player.LocalPlayer p =
                        net.minecraft.client.Minecraft.getInstance().player;
                if (p != null) {
                    p.displayClientMessage(nameOf(clientCmd), true);
                }
            }

            private int holdThreshold() {
                net.minecraft.client.player.LocalPlayer p =
                        net.minecraft.client.Minecraft.getInstance().player;
                float exp = p != null && cn.lambdalib2.datapart.EntityData.isReady(p)
                        && AbilityData.get(p) != null
                        ? AbilityData.get(p).getSkillExp(INSTANCE) : 0f;
                return Math.max(1, (int) AbilityConfig.stat("forced_control", "wheel_hold", exp));
            }

            @Override
            public ResourceLocation getIcon() {
                return iconOf(clientCmd);
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
    }
}
