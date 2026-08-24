package cn.academy.ability.vanilla.vecmanip.advanced;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DualWingAnim {

    private DualWingAnim() {}

    public enum Dir {

        NONE,

        AGO,

        BACK,

        LEFT,

        RIGHT;

        private static final Dir[] VALUES = values();

        public static Dir byId(int id) {
            return id >= 0 && id < VALUES.length ? VALUES[id] : NONE;
        }
    }

    private static final float DEG2RAD = (float) (Math.PI / 180.0);

    private static final float SEC_PER_TICK = 0.05f;

    private static final int STALE_TICKS = 40;

    public static final class State {

        private Dir shown = Dir.NONE;

        private float progress;

        private float prevProgress;

        private float floatPhase;
        private float prevFloatPhase;

        private int fedAt;
    }

    private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();

    public static void tick(Player player, Dir dir) {
        if (player == null) {
            return;
        }
        State s = STATES.computeIfAbsent(player.getUUID(), k -> new State());
        s.fedAt = player.tickCount;

        s.prevProgress = s.progress;
        s.prevFloatPhase = s.floatPhase;

        s.floatPhase += SEC_PER_TICK;
        if (s.floatPhase >= DualWingAnimData.FLOAT_PERIOD) {
            s.floatPhase -= DualWingAnimData.FLOAT_PERIOD;
            s.prevFloatPhase -= DualWingAnimData.FLOAT_PERIOD;
        }

        boolean entering = dir != Dir.NONE && (s.shown == Dir.NONE || s.shown == dir);
        if (entering) {
            s.shown = dir;
            s.progress = Math.min(1.0f, s.progress + SEC_PER_TICK / DualWingAnimData.ENTER_SEC);
        } else {

            s.progress = Math.max(0.0f, s.progress - SEC_PER_TICK / DualWingAnimData.EXIT_SEC);
            if (s.progress <= 0.0f) {
                s.shown = Dir.NONE;
            }
        }
    }

    public static void clear(UUID uuid) {
        if (uuid != null) {
            STATES.remove(uuid);
        }
    }

    public static boolean active(Player player) {
        return isPlaying(player);
    }

    private static void sample(State s, float partialTick, float[] out) {
        float p = s.prevProgress + (s.progress - s.prevProgress) * partialTick;
        float[] target = s.shown == Dir.NONE
                ? DualWingAnimData.IDLE
                : DualWingAnimData.DIR_POSES[s.shown.ordinal() - 1];
        float[] idle = DualWingAnimData.IDLE;
        for (int i = 0; i < out.length; i++) {
            out[i] = idle[i] + (target[i] - idle[i]) * p;
        }
    }

    public static boolean isPlaying(Player player) {
        State s = player == null ? null : STATES.get(player.getUUID());
        if (s == null) {
            return false;
        }
        int age = player.tickCount - s.fedAt;

        return age >= 0 && age <= STALE_TICKS;
    }

    public static float[] pose(Player player, float partialTick) {
        if (!isPlaying(player)) {
            return null;
        }
        float[] out = new float[DualWingAnimData.BONES * DualWingAnimData.STRIDE];
        sample(STATES.get(player.getUUID()), partialTick, out);
        return out;
    }

    public static float floatOffset(Player player, float partialTick) {
        State s = player == null ? null : STATES.get(player.getUUID());
        if (s == null || player.tickCount - s.fedAt > STALE_TICKS) {
            return 0.0f;
        }
        float ph = s.prevFloatPhase + (s.floatPhase - s.prevFloatPhase) * partialTick;
        float t = ph / DualWingAnimData.FLOAT_PERIOD;
        return DualWingAnimData.FLOAT_AMP * 0.5f * (1.0f - (float) Math.cos(2.0 * Math.PI * t));
    }

    public static float bb2mRotX(float deg) {
        return -deg * DEG2RAD;
    }

    public static float bb2mRotY(float deg) {
        return deg * DEG2RAD;
    }

    public static float bb2mRotZ(float deg) {
        return -deg * DEG2RAD;
    }

    public static float bb2mPosY(float px) {
        return -px;
    }

    public static float rootRotX(float deg) {
        return deg;
    }

    public static float rootRotY(float deg) {
        return -deg;
    }

    public static float rootRotZ(float deg) {
        return -deg;
    }

    private static final float ARM_Y = 2.0f, ARM_X = 5.0f, LEG_X = 1.9f, LEG_Y = 12.0f;

    private static boolean dirty;

    public static void resetIfDirty(PlayerModel<?> pm) {
        if (!dirty || pm == null) {
            return;
        }
        dirty = false;
        pm.rightLeg.x = -LEG_X;
        pm.leftLeg.x = LEG_X;

        pm.rightPants.x = -LEG_X;
        pm.leftPants.x = LEG_X;
        pm.head.zRot = 0.0f;
        pm.body.zRot = 0.0f;
        pm.hat.zRot = 0.0f;
        pm.jacket.zRot = 0.0f;
    }

    public static void applyToModel(PlayerModel<?> pm, float[] q) {

        pm.head.xRot += bb2mRotX(q[idx(DualWingAnimData.B_HEAD, 0)]);
        pm.head.yRot += bb2mRotY(q[idx(DualWingAnimData.B_HEAD, 1)]);
        pm.head.zRot = bb2mRotZ(q[idx(DualWingAnimData.B_HEAD, 2)]);

        pm.head.y = 0.0F;
        pm.body.y = 0.0F;
        pm.hat.copyFrom(pm.head);

        setRot(pm.body, q, DualWingAnimData.B_BODY);
        setRot(pm.rightArm, q, DualWingAnimData.B_ARM_RIGHT);
        setRot(pm.leftArm, q, DualWingAnimData.B_ARM_LEFT);
        setRot(pm.rightLeg, q, DualWingAnimData.B_LEG_RIGHT);
        setRot(pm.leftLeg, q, DualWingAnimData.B_LEG_LEFT);

        setPivot(pm.rightArm, q, DualWingAnimData.B_ARM_RIGHT, -ARM_X, ARM_Y, 0.0f);
        setPivot(pm.leftArm, q, DualWingAnimData.B_ARM_LEFT, ARM_X, ARM_Y, 0.0f);
        setPivot(pm.rightLeg, q, DualWingAnimData.B_LEG_RIGHT, -LEG_X, LEG_Y, 0.0f);
        setPivot(pm.leftLeg, q, DualWingAnimData.B_LEG_LEFT, LEG_X, LEG_Y, 0.0f);
        dirty = true;

        pm.jacket.copyFrom(pm.body);
    }

    public static int idx(int bone, int ch) {
        return bone * DualWingAnimData.STRIDE + ch;
    }

    private static void setRot(ModelPart part, float[] q, int bone) {
        part.xRot = bb2mRotX(q[idx(bone, 0)]);
        part.yRot = bb2mRotY(q[idx(bone, 1)]);
        part.zRot = bb2mRotZ(q[idx(bone, 2)]);
    }

    private static void setPivot(ModelPart part, float[] q, int bone,
                                 float baseX, float baseY, float baseZ) {
        part.x = baseX + q[idx(bone, 3)];
        part.y = baseY + bb2mPosY(q[idx(bone, 4)]);
        part.z = baseZ + q[idx(bone, 5)];
    }
}
