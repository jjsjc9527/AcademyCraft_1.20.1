package cn.academy.ability.vanilla.vecmanip.advanced;

import cn.lambdalib2.util.GameTimer;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class DualWingFlight {

    private boolean flying;

    private boolean prevJump;

    private double lastJumpAt = -999.0;

    private boolean academy$diagMayFly = true;

    private double academy$lastTickAt;

    private Vec3 academy$lastPos;
    private Vec3 academy$lastSet;

    private int academy$lastTickCount = -1;

    public static volatile boolean ACADEMY$CLIENT_FLYING;

    private double academy$lastLeakLogAt;

    static void academy$diag(Player p, String what) {
        try {
            if (cn.academy.util.ACDiag.ON)
            org.apache.logging.log4j.LogManager.getLogger("AcademyCraft/Wing").warn(
                    "[wing-fly] {} | {}\nstack:\n{}",
                    p == null ? "?" : p.getName().getString(), what,
                    StackWalker.getInstance().walk(s -> s.limit(14)
                            .map(f -> "    " + f.getClassName() + "." + f.getMethodName())
                            .reduce("", (a, b) -> a.isEmpty() ? b : a + "\n" + b)));
        } catch (Throwable ignored) {

        }
    }

    private int dashUp, dashDown;

    private float prevFlyingSpeed = 0.05f;

    private static final double DASH_WINDOW_SEC = 0.35;

    private static final int DASH_DOWN_MAX = 40;

    private final int dashTicks;

    public DualWingFlight(int dashTicks) {
        this.dashTicks = dashTicks;
    }

    public void tick(Player p, double speed, double dashSpeed, double dropSpeed) {
        Input in = input(p);
        if (in == null) {
            return;
        }
        Abilities ab = p.getAbilities();

        if (ab.flying) {
            ab.flying = false;
            p.onUpdateAbilities();
        }

        if (academy$diagMayFly != ab.mayfly) {
            academy$diagMayFly = ab.mayfly;
            academy$diag(p, "mayfly changed to " + ab.mayfly
                    + "(flying=" + flying + " vanillaFlying=" + ab.flying + ")");
        }

        if (in.jumping && !prevJump) {

            double now = GameTimer.getTime();
            if (now - lastJumpAt <= DASH_WINDOW_SEC) {
                lastJumpAt = -999.0;
                toggleFlying(ab, p);
            } else {
                lastJumpAt = now;
            }
        }
        prevJump = in.jumping;

        if (dashDown > 0) {
            --dashDown;
            Vec3 mo = p.getDeltaMovement();
            p.setDeltaMovement(mo.x, -dropSpeed, mo.z);
            p.fallDistance = 0;
            if (p.onGround()) {
                dashDown = 0;
            }
            return;
        }

        if (!flying) {
            ACADEMY$CLIENT_FLYING = false;
            return;
        }
        ACADEMY$CLIENT_FLYING = true;

        if (p.isPassenger()) {
            p.stopRiding();
        }

        double academy$now = GameTimer.getTime();
        if (academy$lastTickAt > 0.0) {
            double academy$gap = academy$now - academy$lastTickAt;
            if (academy$gap > 0.120) {
                academy$diag(p, String.format(
                        "client tick interval %.0f ms (normal 50) -- flight velocity override cannot keep up",
                        academy$gap * 1000.0));
            }
        }
        academy$lastTickAt = academy$now;

        Vec3 dir = moveDir(p, in);
        double vertical = (in.jumping ? 1 : 0) - (in.shiftKeyDown ? 1 : 0);
        Vec3 total = (dir == null ? Vec3.ZERO : dir).add(0, vertical, 0);

        if (total.lengthSqr() < 1.0e-8) {
            p.setDeltaMovement(0, 0, 0);
        } else {
            p.setDeltaMovement(total.normalize().scale(speed));
        }

        if (dashUp > 0) {
            --dashUp;
            Vec3 mo = p.getDeltaMovement();
            p.setDeltaMovement(mo.x, dashSpeed, mo.z);
        }

        p.fallDistance = 0;

        if (academy$lastTickCount == p.tickCount) {
            p.move(net.minecraft.world.entity.MoverType.SELF, p.getDeltaMovement());
        }
        academy$lastTickCount = p.tickCount;

        Vec3 academy$want = p.getDeltaMovement();
        if (academy$lastPos != null && academy$lastSet != null) {
            double academy$expect = academy$lastSet.length();
            double academy$moved = p.position().subtract(academy$lastPos).length();

            if (academy$expect > 0.15 && academy$moved < academy$expect * 0.4
                    && academy$now - academy$lastLeakLogAt > 1.0) {
                academy$lastLeakLogAt = academy$now;
                academy$diag(p, String.format(
                        "movement swallowed: last tick set speed %.3f (%.2f,%.2f,%.2f) but actual move was %.3f"
                                + " | onGround=%s collision(horizontal/vertical)=%s/%s noGravity=%s tickCount=%d",
                        academy$expect, academy$lastSet.x, academy$lastSet.y, academy$lastSet.z,
                        academy$moved, p.onGround(),
                        p.horizontalCollision, p.verticalCollision, p.isNoGravity(), p.tickCount));
            }
        }
        academy$lastPos = p.position();
        academy$lastSet = academy$want;
    }

    private void toggleFlying(Abilities ab, Player p) {
        flying = !flying;

        if (!flying) {
            academy$diag(p, "toggleFlying -> flight OFF (double tap detected)");
        }
        if (flying) {

            prevFlyingSpeed = ab.getFlyingSpeed();
            ab.setFlyingSpeed(0f);
            dashUp = dashTicks;
            dashDown = 0;
        } else {

            ab.setFlyingSpeed(prevFlyingSpeed);
            dashDown = DASH_DOWN_MAX;
            dashUp = 0;
        }
    }

    public boolean isFlying() {
        return flying;
    }

    public DualWingAnim.Dir dir(Player p) {
        Input in = input(p);
        if (!flying || in == null) {
            return DualWingAnim.Dir.NONE;
        }
        if (in.up && !in.down) {
            return DualWingAnim.Dir.AGO;
        }
        if (in.down && !in.up) {
            return DualWingAnim.Dir.BACK;
        }
        if (in.left && !in.right) {
            return DualWingAnim.Dir.LEFT;
        }
        if (in.right && !in.left) {
            return DualWingAnim.Dir.RIGHT;
        }
        return DualWingAnim.Dir.NONE;
    }

    private Vec3 moveDir(Player p, Input in) {
        double x = 0, z = 0;
        if (in.up) z += 1;
        if (in.down) z -= 1;
        if (in.left) x += 1;
        if (in.right) x -= 1;
        if (x == 0 && z == 0) {
            return null;
        }
        Vec3 base = new Vec3(x, 0, z).normalize();
        float yaw = (float) Math.toRadians(p.getYHeadRot());
        float pitch = (float) Math.toRadians(p.getXRot());
        return base.xRot(-pitch).yRot(-yaw);
    }

    public void reset(Player p) {

        if (flying) {
            academy$diag(p, "reset() -> flight cleared (skill terminated or context ended)");
        }
        if (flying && p != null) {
            p.getAbilities().setFlyingSpeed(prevFlyingSpeed);
        }
        flying = false;
        prevJump = false;
        lastJumpAt = -999.0;
        dashUp = 0;
        dashDown = 0;
    }

    private static Input input(Player p) {
        return p instanceof LocalPlayer lp ? lp.input : null;
    }
}
