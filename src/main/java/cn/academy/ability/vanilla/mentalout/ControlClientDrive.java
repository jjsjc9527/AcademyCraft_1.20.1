package cn.academy.ability.vanilla.mentalout;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@OnlyIn(Dist.CLIENT)
public final class ControlClientDrive {

    private static final double SPRINT_DIST = 4.0;

    private static final double STOP_DIST = 0.6;

    private static final float TURN_MAX_SPEED = 15.0f;

    private static final float TURN_ACCEL = 3.0f;

    private static final float MAX_FRAME_DT = 1.0f;

    private static final double AIM_MIN_DIST = 0.5;

    private static final float LEAP_ALIGN_DEG = 12.0f;

    private static final double JUMP_TRIGGER = 1.4;

    private static final double LEDGE_PROBE = 0.7;

    private float yawSpeed;
    private float pitchSpeed;

    private final ControlPath path = new ControlPath();

    private ControlClientDrive() {}

    private void relax() {
        yawSpeed = 0f;
        pitchSpeed = 0f;
    }

    private static float stepSpeed(float speed, float delta, float dt) {
        float want = Math.signum(delta)
                * Math.min(TURN_MAX_SPEED, (float) Math.sqrt(2.0 * TURN_ACCEL * Math.abs(delta)));
        float budget = TURN_ACCEL * dt;
        return speed + Mth.clamp(want - speed, -budget, budget);
    }

    private float approachYaw(float cur, float target, float dt) {
        float delta = Mth.wrapDegrees(target - cur);
        yawSpeed = stepSpeed(yawSpeed, delta, dt);
        return cur + Mth.clamp(yawSpeed * dt, -Math.abs(delta), Math.abs(delta));
    }

    private float approachPitch(float cur, float target, float dt) {
        float delta = target - cur;
        pitchSpeed = stepSpeed(pitchSpeed, delta, dt);
        return Mth.clamp(cur + Mth.clamp(pitchSpeed * dt, -Math.abs(delta), Math.abs(delta)),
                -90f, 90f);
    }

    private static void applyView(LocalPlayer p, float yaw, float pitch) {
        p.setYRot(yaw);
        p.yRotO = yaw;
        p.setXRot(pitch);
        p.xRotO = pitch;
        p.yHeadRot = yaw;
        p.yHeadRotO = yaw;
        p.yBodyRot = yaw;
        p.yBodyRotO = yaw;
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new ControlClientDrive());
    }

    private static float goalYawOf(Vec3 from, Vec3 goal) {
        return (float) (Mth.atan2(goal.z - from.z, goal.x - from.x) * (180.0 / Math.PI)) - 90.0f;
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.isPaused()) {
            return;
        }
        Vec3 goal = ControlState.driveGoal(p);
        if (goal == null) {
            relax();
            path.clear();
            return;
        }
        float pt = event.renderTickTime;
        Vec3 at = p.getPosition(pt);
        double flat = Math.sqrt((goal.x - at.x) * (goal.x - at.x) + (goal.z - at.z) * (goal.z - at.z));
        if (flat <= STOP_DIST) {

            relax();
            return;
        }
        float dt = Mth.clamp(mc.getDeltaFrameTime(), 0f, MAX_FRAME_DT);
        if (dt <= 0f) {
            return;
        }

        Vec3 aim = path.aim(p, goal);
        double af = Math.sqrt((aim.x - at.x) * (aim.x - at.x) + (aim.z - at.z) * (aim.z - at.z));
        if (af < AIM_MIN_DIST) {
            aim = goal;
            af = flat;
        }
        double dy = aim.y - p.getEyePosition(pt).y;
        float goalPitch = (float) (-(Mth.atan2(dy, af) * (180.0 / Math.PI)));
        applyView(p,
                approachYaw(p.getYRot(), goalYawOf(at, aim), dt),
                approachPitch(p.getXRot(), goalPitch, dt));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onInput(MovementInputUpdateEvent event) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null || event.getEntity() != p) {
            return;
        }
        Vec3 goal = ControlState.driveGoal(p);
        if (goal == null) {
            return;
        }
        double dx = goal.x - p.getX(), dz = goal.z - p.getZ();
        double flat = Math.sqrt(dx * dx + dz * dz);

        if (flat <= STOP_DIST) {

            event.getInput().forwardImpulse = 0f;
            event.getInput().leftImpulse = 0f;
            p.setSprinting(false);
            return;
        }

        path.update(p, goal);
        Vec3 aim = path.aim(p, goal);
        ControlPathfinder.Move move = path.move();

        float relDeg = Mth.wrapDegrees(goalYawOf(p.position(), aim) - p.getYRot());
        float forward = Math.max(0f, Mth.cos(relDeg * ((float) Math.PI / 180f)));

        if (move == ControlPathfinder.Move.LEAP && Math.abs(relDeg) > LEAP_ALIGN_DEG) {
            forward = 0f;
        }

        event.getInput().forwardImpulse = forward;
        event.getInput().leftImpulse = 0f;
        event.getInput().jumping = wantJump(p, move, aim);

        p.setSprinting(flat > SPRINT_DIST || move == ControlPathfinder.Move.LEAP);
    }

    private static boolean wantJump(LocalPlayer p, ControlPathfinder.Move move, Vec3 aim) {
        if (!p.onGround()) {
            return false;
        }
        if (move == ControlPathfinder.Move.JUMP) {
            double dx = aim.x - p.getX(), dz = aim.z - p.getZ();
            return p.horizontalCollision || dx * dx + dz * dz < JUMP_TRIGGER * JUMP_TRIGGER;
        }
        if (move == ControlPathfinder.Move.LEAP) {
            return atLedge(p);
        }

        return p.horizontalCollision && stepUpAhead(p);
    }

    private static boolean stepUpAhead(LocalPlayer p) {
        double rad = Math.toRadians(p.getYRot());
        net.minecraft.core.BlockPos ahead = net.minecraft.core.BlockPos.containing(
                p.getX() - Math.sin(rad) * LEDGE_PROBE, p.getY(),
                p.getZ() + Math.cos(rad) * LEDGE_PROBE);
        return blocked(p, ahead)
                && !blocked(p, ahead.above())
                && !blocked(p, ahead.above(2));
    }

    private static boolean blocked(LocalPlayer p, net.minecraft.core.BlockPos pos) {
        return !p.level().getBlockState(pos).getCollisionShape(p.level(), pos).isEmpty();
    }

    private static boolean atLedge(LocalPlayer p) {
        double rad = Math.toRadians(p.getYRot());
        double fx = -Math.sin(rad), fz = Math.cos(rad);
        net.minecraft.core.BlockPos ahead = net.minecraft.core.BlockPos.containing(
                p.getX() + fx * LEDGE_PROBE, p.getY() - 0.2, p.getZ() + fz * LEDGE_PROBE);
        return p.level().getBlockState(ahead).getCollisionShape(p.level(), ahead).isEmpty();
    }
}
