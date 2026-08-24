package cn.academy.ability.vanilla.vecmanip.advanced;

public final class DualWingAnimData {

    private DualWingAnimData() {}

    public static final int B_HEAD = 0;

    public static final int B_BODY = 1;

    public static final int B_ARM_RIGHT = 2;

    public static final int B_FOREARM_RIGHT = 3;

    public static final int B_ARM_LEFT = 4;

    public static final int B_FOREARM_LEFT = 5;

    public static final int B_LEG_RIGHT = 6;

    public static final int B_SHIN_RIGHT = 7;

    public static final int B_LEG_LEFT = 8;

    public static final int B_SHIN_LEFT = 9;

    public static final int B_POSITION = 10;

    public static final int BONES = 11;

    public static final int STRIDE = 6;

    public static final float ENTER_SEC = 0.16667f;

    public static final float EXIT_SEC = 0.29167f;

    public static final float FLOAT_PERIOD = 0.79167f;

    public static final float FLOAT_AMP = 1.0f;

    public static final float[] IDLE = {
        0f, 0f, 0f, 0f, 0f, 0f,
        0f, 0f, 0f, 0f, 0f, 0f,
        12.5f, 0f, 0f, 0f, 0f, 0f,
        27.5f, 0f, 0f, 0f, 0f, 0f,
        15f, 0f, 0f, 0f, 0f, 0f,
        15f, 0f, 0f, 0f, 0f, 0f,
        22.5f, 0f, 0f, 0f, 0f, 0f,
        -60f, 0f, 0f, 0f, 0f, 0f,
        10f, 0f, 0f, 0f, 0f, 0f,
        -37.5f, 0f, 0f, 0f, 0f, 0f,
        0f, 0f, 0f, 0f, 0f, 0f,
    };

    public static final float[] AGO = {
        0f, 0f, 0f, 0f, 0f, 0f,
        -12.5f, 0f, 0f, 0f, 0f, 0f,
        -30f, 0f, 0f, 0f, 0f, 0f,
        0f, 0f, 0f, 0f, 0f, 0f,
        -10f, 0f, 0f, 0f, 0f, 0f,
        -2.5f, 0f, 0f, 0f, 0f, 0f,
        5f, 0f, 0f, 0f, 0f, 3f,
        -15f, 0f, 0f, 0f, 0f, 0f,
        -5f, 0f, 0f, 0f, 0f, 3f,
        -12.5f, 0f, 0f, 0f, 0f, 0f,
        -37.5f, 0f, 0f, 0f, 0f, 0f,
    };

    public static final float[] BACK = {
        -30f, 0f, 0f, 0f, 0f, 0f,
        0f, 0f, 0f, 0f, 0f, 0f,
        35f, 0f, 0f, 0f, 0f, 0f,
        25f, 0f, 0f, 0f, 0f, 0f,
        27.5f, 0f, 0f, 0f, 0f, 0f,
        25f, 0f, 0f, 0f, 0f, 0f,
        0f, 0f, 0f, 0f, 0f, 0f,
        -12.5f, 0f, 0f, 0f, 0f, 0f,
        10f, 0f, 0f, 0f, 0f, 0f,
        -12.5f, 0f, 0f, 0f, 0f, 0f,
        30f, 0f, 0f, 0f, 0f, 0f,
    };

    public static final float[] LEFT = {
        0f, 0f, 0f, 0f, 0f, 0f,
        0f, 0f, 10f, 0f, 0f, 0f,
        12.0869f, -3.2114f, 14.6598f, 0f, 0f, 0f,
        12.5f, 0f, 0f, 0f, 0f, 0f,
        13.0644f, -7.4357f, 29.1475f, 1f, 1f, 0f,
        22.5f, 0f, 0f, 0f, 0f, 0f,
        21.8064f, -5.6842f, 13.9042f, 2f, 0f, 0f,
        -59.1326f, 12.9526f, 7.6308f, 0f, 0f, 0f,
        9.408f, -3.4049f, 19.7198f, 2f, 0f, 0f,
        -35.7937f, 12.0174f, 16.1064f, 0f, 0f, 0f,
        0f, 0f, 17.5f, 0f, 0f, 0f,
    };

    public static final float[] RIGHT = {
        0f, 0f, 0f, 0f, 0f, 0f,
        0f, 0f, -7.5f, 0f, 0f, 0f,
        11.1251f, 5.7358f, -26.9407f, 0f, 0f, 0f,
        27.5f, 0f, 0f, 0f, 0f, 0f,
        14.5109f, 3.8411f, -14.5108f, 0f, -1f, 0f,
        2.5f, 0f, 0f, 0f, 0f, 0f,
        21.2677f, 7.5207f, -18.5859f, -2f, 0f, 0f,
        -59.1326f, 12.9526f, 7.6308f, 0f, 0f, 0f,
        9.7675f, 2.154f, -12.316f, -2f, 0f, 0f,
        -35.7937f, 12.0174f, 16.1064f, 0f, 0f, 0f,
        0f, 0f, -22.5f, 0f, 0f, 0f,
    };

    public static final float[][] DIR_POSES = { AGO, BACK, RIGHT, LEFT };
}
