package cn.academy.block;

public final class WindgenConsts {

    public static final int MIN_PILLARS = 3;
    public static final int MAX_PILLARS = 40;
    public static final double MAX_GEN_SPEED = 15;
    public static final double BUFFER_SIZE = 20000;
    public static final int OBSTACLE_RADIUS = 4;
    public static final int CHECK_INTERVAL = 10;

    private WindgenConsts() {}

    public static double heightFactor(int y) {
        double t = Math.max(0.0, Math.min(1.0, (y - 70.0) / 90.0));
        return 0.5 + 0.5 * t;
    }
}
