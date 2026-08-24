package cn.academy.ability.vanilla.vecmanip.advanced;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class WhiteFeatherField {

    public static final double FALL_SPEED = 0.18;

    private static final double SWAY_AMP_MIN = 0.22, SWAY_AMP_MAX = 0.55;

    private static final double SWAY_OMEGA_MIN = 0.09, SWAY_OMEGA_MAX = 0.17;

    private static final double SWAY_ELLIPSE = 0.42;

    private static final double SWAY_SETTLE_H = 1.6;

    public static double phaseOf(double x, double startY, double z) {
        long h = Double.doubleToLongBits(x) * 0x9E3779B97F4A7C15L
                ^ Double.doubleToLongBits(z) * 0xC2B2AE3D27D4EB4FL
                ^ Double.doubleToLongBits(startY) * 0x165667B19E3779F9L;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return (h >>> 11) * 0x1.0p-53 * (Math.PI * 2.0);
    }

    public static double detOf(int featherId, int salt) {
        long h = (featherId & 0xFFFFFFFFL) * 0x9E3779B97F4A7C15L
                ^ (salt & 0xFFFFFFFFL) * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return (h >>> 11) * 0x1.0p-53;
    }

    public static double detRange(int featherId, int salt, double lo, double hi) {
        return lo + detOf(featherId, salt) * (hi - lo);
    }

    public static final double RECOIL_DIST = 3.0;

    public static final int RECOIL_TICKS = 9;

    public static final double SPIRAL_TURNS = 2.0;

    private static final int SPIRAL_CYCLE = RECOIL_TICKS + 3;

    public static final double SPIRAL_OMEGA = Math.PI * 2.0 * SPIRAL_TURNS / SPIRAL_CYCLE;

    public static final double SPIRAL_RADIUS = 0.7;

    private static final double SPIRAL_NEAR = RECOIL_DIST;

    private static final double SPIRAL_FAR = RECOIL_DIST * 2.0;

    public static double spiralFadeAt(double dist) {
        if (dist <= SPIRAL_NEAR) {
            return 1.0;
        }
        if (dist >= SPIRAL_FAR) {
            return 0.0;
        }
        return 1.0 - (dist - SPIRAL_NEAR) / (SPIRAL_FAR - SPIRAL_NEAR);
    }

    public static final double FEATHER_CHASE = 0.25;

    private static final double RECOIL_YAW = 0.9;

    public static double recoilDistAt(int t) {
        double u = Mth.clamp(t / (double) RECOIL_TICKS, 0.0, 1.0);
        return RECOIL_DIST * u;
    }

    public static final double ORBIT_HIT_SIZE = 3.0;

    public static boolean inHitBox(LivingEntity e, double x, double y, double z) {
        AABB box = e.getBoundingBox();
        double h = ORBIT_HIT_SIZE * 0.5;
        double cx = (box.minX + box.maxX) * 0.5;
        double cy = (box.minY + box.maxY) * 0.5;
        double cz = (box.minZ + box.maxZ) * 0.5;
        return x >= Math.min(box.minX, cx - h) && x <= Math.max(box.maxX, cx + h)
            && y >= Math.min(box.minY, cy - h) && y <= Math.max(box.maxY, cy + h)
            && z >= Math.min(box.minZ, cz - h) && z <= Math.max(box.maxZ, cz + h);
    }

    public static void spiralOffset(double ax, double ay, double az, double angle, double[] out) {

        double refX = 0.0, refY = 1.0, refZ = 0.0;
        if (Math.abs(ay) > 0.9) {
            refX = 1.0;
            refY = 0.0;
        }

        double ux = refY * az - refZ * ay;
        double uy = refZ * ax - refX * az;
        double uz = refX * ay - refY * ax;
        double ul = Math.sqrt(ux * ux + uy * uy + uz * uz);
        if (ul < 1.0e-6) {
            out[0] = out[1] = out[2] = 0.0;
            return;
        }
        ux /= ul;
        uy /= ul;
        uz /= ul;

        double vx = ay * uz - az * uy;
        double vy = az * ux - ax * uz;
        double vz = ax * uy - ay * ux;
        double c = Math.cos(angle) * SPIRAL_RADIUS;
        double s = Math.sin(angle) * SPIRAL_RADIUS;
        out[0] = ux * c + vx * s;
        out[1] = uy * c + vy * s;
        out[2] = uz * c + vz * s;
    }

    public static void recoilDirOf(int featherId, double dx, double dy, double dz, double[] out) {
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0e-4) {

            double a = detOf(featherId, 6) * Math.PI * 2.0;
            out[0] = Math.cos(a);
            out[1] = 0.0;
            out[2] = Math.sin(a);
            return;
        }
        double nx = dx / len, ny = dy / len, nz = dz / len;
        double yaw = detRange(featherId, 7, -RECOIL_YAW, RECOIL_YAW);
        double c = Math.cos(yaw), s = Math.sin(yaw);
        out[0] = nx * c - nz * s;
        out[1] = ny;
        out[2] = nx * s + nz * c;
    }

    private static double ampOf(double phase) {
        return SWAY_AMP_MIN + frac(phase * 0.7351) * (SWAY_AMP_MAX - SWAY_AMP_MIN);
    }

    private static double omegaOf(double phase) {
        return SWAY_OMEGA_MIN + frac(phase * 1.2731) * (SWAY_OMEGA_MAX - SWAY_OMEGA_MIN);
    }

    private static double frac(double v) {
        return v - Math.floor(v);
    }

    public static double swayAngle(double phase, int age) {
        return omegaOf(phase) * age + phase;
    }

    public static double swayDamp(double y, double landY, int age) {
        double h = y - landY;
        double d = h >= SWAY_SETTLE_H ? 1.0 : Math.max(0.0, h / SWAY_SETTLE_H);

        if (age < 8) {
            d *= age / 8.0;
        }
        return d;
    }

    public static double swayX(double phase, int age, double damp) {
        return ampOf(phase) * Math.sin(swayAngle(phase, age)) * damp;
    }

    public static double swayZ(double phase, int age, double damp) {
        return ampOf(phase) * Math.cos(swayAngle(phase, age)) * SWAY_ELLIPSE * damp;
    }

    private static final double GROUND_EPS = 0.02;

    public static boolean hasGroundUnder(BlockGetter level, double x, double y, double z) {
        BlockPos p = BlockPos.containing(x, y - GROUND_EPS, z);
        return !level.getBlockState(p).getCollisionShape(level, p).isEmpty();
    }

    public static final int GROUND_CHECK_EVERY = 4;

    public static boolean groundCheckDue(int age, int salt) {
        return Math.floorMod(age + salt, GROUND_CHECK_EVERY) == 0;
    }

    private static final double MAX_DROP = 48.0;

    private enum State { FALLING, LANDED, CHASE, RECOIL }

    private static final class Feather {

        final int id;

        final boolean empowered;

        double x0, z0;

        double landY;

        double phase;

        double x, y, z;

        int age;

        int life;

        int swayBase;

        long cell = Long.MIN_VALUE;

        State state = State.FALLING;

        int groundTicks;

        int targetId = -1;

        double rx, ry, rz;

        int recoilBase;

        double bx, by, bz;

        double ax, ay, az;

        boolean anchored;

        Feather(int id, double x, double y, double z, double landY, boolean empowered, int life) {
            this.id = id;
            this.empowered = empowered;
            this.life = life;
            this.x0 = x;
            this.z0 = z;
            this.x = x;
            this.y = y;
            this.z = z;
            this.landY = landY;
            this.phase = phaseOf(x, y, z);
        }

        int swayAge() {
            return age - swayBase;
        }
    }

    private final List<Feather> feathers = new ArrayList<>();

    private final Map<Long, Integer> cells = new HashMap<>();

    private final int lifetime;

    private final double empowerFallMul;

    private final int aimDelay;

    private final double launchSpeed;

    private final double aimRange;

    private final int shotCost;

    private int nextId;

    public WhiteFeatherField(int lifetime, double empowerFallMul, int aimDelay,
                             double launchSpeed, double aimRange, int shotCost) {
        this.lifetime = lifetime;
        this.empowerFallMul = empowerFallMul;
        this.aimDelay = aimDelay;
        this.launchSpeed = launchSpeed;
        this.aimRange = aimRange;
        this.shotCost = shotCost;
    }

    public static double fallSpeedOf(boolean empowered, double empowerFallMul) {
        return empowered ? FALL_SPEED * empowerFallMul : FALL_SPEED;
    }

    private static final double CELL_EDGE = 0.08;

    private static final double START_SPREAD = 3.0;

    public double[] spawn(Level level, double cx, double topY, double cz,
                          double radius, int count, RandomSource rand, boolean empowered) {
        if (count <= 0) {
            return null;
        }
        double[] out = new double[count * 5];
        int n = 0;
        for (int i = 0; i < count; i++) {

            double ang = rand.nextDouble() * Math.PI * 2.0;
            double r = radius * Math.sqrt(rand.nextDouble());
            int bx = Mth.floor(cx + Math.cos(ang) * r);
            int bz = Mth.floor(cz + Math.sin(ang) * r);

            double x = bx + CELL_EDGE + rand.nextDouble() * (1.0 - 2.0 * CELL_EDGE);
            double z = bz + CELL_EDGE + rand.nextDouble() * (1.0 - 2.0 * CELL_EDGE);
            double startY = findSpawnY(level, x, topY - rand.nextDouble() * START_SPREAD, z);
            if (Double.isNaN(startY)) {
                continue;
            }

            double landY = findLanding(level, x, startY, z);
            Feather f = new Feather(nextId++, x, startY, z, landY, empowered, lifetime);
            feathers.add(f);
            acquire(f, cellOf(f));

            out[n++] = f.id;
            out[n++] = x;
            out[n++] = startY;
            out[n++] = landY;
            out[n++] = z;
        }

        return n == 0 ? null : (n == out.length ? out : java.util.Arrays.copyOf(out, n));
    }

    private static double findSpawnY(Level level, double x, double startY, double z) {
        int bx = Mth.floor(x), bz = Mth.floor(z);
        int y0 = Mth.floor(startY);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(bx, y0, bz);

        int min = Math.max(level.getMinBuildHeight(), y0 - (int) MAX_DROP);
        if (!level.getBlockState(p).getCollisionShape(level, p).isEmpty()) {
            for (int by = y0 - 1; by >= min; by--) {
                p.set(bx, by, bz);
                if (level.getBlockState(p).getCollisionShape(level, p).isEmpty()) {

                    return by + 0.9;
                }
            }
            return Double.NaN;
        }
        return startY;
    }

    private static double findLanding(Level level, double x, double topY, double z) {
        Vec3 from = new Vec3(x, topY, z);
        Vec3 to = new Vec3(x, topY - MAX_DROP, z);
        BlockHitResult hit = level.clip(new ClipContext(
                from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null));
        return hit.getType() == HitResult.Type.MISS ? topY - MAX_DROP : hit.getLocation().y;
    }

    public double[] tick(Level level, List<? extends LivingEntity> foes) {
        pendingLen = 0;
        struck.clear();

        Iterator<Feather> it = feathers.iterator();
        while (it.hasNext()) {
            Feather f = it.next();

            if (++f.age > f.life) {
                release(f);
                it.remove();
                continue;
            }

            switch (f.state) {
                case FALLING -> {
                    double step = fallSpeedOf(f.empowered, empowerFallMul);
                    f.y = Math.max(f.landY, f.y - step);

                    double damp = swayDamp(f.y, f.landY, f.swayAge());
                    f.x = f.x0 + swayX(f.phase, f.swayAge(), damp);
                    f.z = f.z0 + swayZ(f.phase, f.swayAge(), damp);
                    if (f.y <= f.landY) {
                        f.state = State.LANDED;
                        f.groundTicks = 0;
                    }
                }
                case LANDED -> {

                    if (groundCheckDue(f.age, f.id)
                            && !hasGroundUnder(level, f.x, f.y, f.z)) {
                        dropFromHere(f, level);
                        emit(f.id, -1, f.landY);
                        break;
                    }

                    if (!f.empowered || foes.isEmpty()) {
                        continue;
                    }
                    if (++f.groundTicks < aimDelay) {
                        continue;
                    }
                    LivingEntity tgt = nearest(f, foes);
                    if (tgt == null) {

                        continue;
                    }
                    launchAt(f, tgt, true);
                    emit(f.id, f.targetId, 0.0);
                }
                case CHASE -> {

                    LivingEntity tgt = liveTarget(level, f);
                    if (tgt == null) {
                        retarget(f, foes, level);
                        break;
                    }

                    advanceAnchor(f, tgt);
                    double dx = f.ax - f.bx, dy = f.ay - f.by, dz = f.az - f.bz;
                    double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    double inv = Math.min(1.0, launchSpeed / Math.max(dist, 1.0e-4));
                    f.bx += dx * inv;
                    f.by += dy * inv;
                    f.bz += dz * inv;
                    applySpiral(f);

                    if (inHitBox(tgt, f.x, f.y, f.z)) {
                        struck.add(f.targetId);
                    }
                    if (dist <= launchSpeed) {

                        recoilDirOf(f.id, dx, dy, dz, dirBuf);
                        f.rx = dirBuf[0];
                        f.ry = dirBuf[1];
                        f.rz = dirBuf[2];
                        f.state = State.RECOIL;
                        f.recoilBase = f.age;
                    }
                }
                case RECOIL -> {

                    LivingEntity tgt = liveTarget(level, f);
                    if (tgt == null) {
                        retarget(f, foes, level);
                        break;
                    }
                    advanceAnchor(f, tgt);
                    int t = f.age - f.recoilBase;
                    if (t >= RECOIL_TICKS) {
                        f.state = State.CHASE;
                        break;
                    }
                    double d = recoilDistAt(t);
                    f.bx = f.ax + f.rx * d;
                    f.by = f.ay + f.ry * d;
                    f.bz = f.az + f.rz * d;
                    applySpiral(f);

                    if (inHitBox(tgt, f.x, f.y, f.z)) {
                        struck.add(f.targetId);
                    }
                }
                default -> { }
            }

            long c = cellOf(f);
            if (c != f.cell) {
                release(f);
                acquire(f, c);
            }
        }
        return pendingLen == 0 ? null : java.util.Arrays.copyOf(pending, pendingLen);
    }

    private double[] pending = new double[48];
    private int pendingLen;

    private final double[] dirBuf = new double[3];

    private static void advanceAnchor(Feather f, LivingEntity tgt) {
        AABB tb = tgt.getBoundingBox();
        double cx = (tb.minX + tb.maxX) * 0.5;
        double cy = (tb.minY + tb.maxY) * 0.5;
        double cz = (tb.minZ + tb.maxZ) * 0.5;
        if (!f.anchored) {
            f.anchored = true;
            f.ax = cx;
            f.ay = cy;
            f.az = cz;
            return;
        }
        f.ax += (cx - f.ax) * FEATHER_CHASE;
        f.ay += (cy - f.ay) * FEATHER_CHASE;
        f.az += (cz - f.az) * FEATHER_CHASE;
    }

    private void applySpiral(Feather f) {

        double dx = f.bx - f.ax, dy = f.by - f.ay, dz = f.bz - f.az;
        double fade = spiralFadeAt(Math.sqrt(dx * dx + dy * dy + dz * dz));
        if (fade <= 0.0) {
            f.x = f.bx;
            f.y = f.by;
            f.z = f.bz;
            return;
        }

        if (f.rx == 0.0 && f.ry == 0.0 && f.rz == 0.0) {
            recoilDirOf(f.id, dx, dy, dz, dirBuf);
            f.rx = dirBuf[0];
            f.ry = dirBuf[1];
            f.rz = dirBuf[2];
        }
        spiralOffset(f.rx, f.ry, f.rz, f.age * SPIRAL_OMEGA, dirBuf);
        f.x = f.bx + dirBuf[0] * fade;
        f.y = f.by + dirBuf[1] * fade;
        f.z = f.bz + dirBuf[2] * fade;
    }

    private final it.unimi.dsi.fastutil.ints.IntOpenHashSet struck =
            new it.unimi.dsi.fastutil.ints.IntOpenHashSet();

    public boolean isStruck(Entity e) {
        return !struck.isEmpty() && struck.contains(e.getId());
    }

    private void emit(int featherId, int targetId, double extra) {
        if (pendingLen + 3 > pending.length) {
            pending = java.util.Arrays.copyOf(pending, pending.length * 2);
        }
        pending[pendingLen++] = featherId;
        pending[pendingLen++] = targetId;
        pending[pendingLen++] = extra;
    }

    public static boolean isDown(LivingEntity e) {
        return e.isRemoved() || e.isDeadOrDying() || e.deathTime > 0;
    }

    private static LivingEntity liveTarget(Level level, Feather f) {
        if (f.targetId < 0) {
            return null;
        }
        Entity e = level.getEntity(f.targetId);
        if (!(e instanceof LivingEntity le) || isDown(le)) {
            return null;
        }
        return le;
    }

    private void retarget(Feather f, List<? extends LivingEntity> foes, Level level) {
        LivingEntity next = foes.isEmpty() ? null : nearest(f, foes);
        if (next != null) {
            launchAt(f, next, false);
            emit(f.id, f.targetId, 0.0);
            return;
        }
        dropFromHere(f, level);
        emit(f.id, -1, f.landY);
    }

    private void dropFromHere(Feather f, Level level) {
        f.targetId = -1;
        f.x0 = f.x;
        f.z0 = f.z;
        f.landY = findLanding(level, f.x, f.y, f.z);
        f.phase = phaseOf(f.x, f.y, f.z);
        f.swayBase = f.age;
        f.state = f.y <= f.landY ? State.LANDED : State.FALLING;
        f.groundTicks = 0;
    }

    private LivingEntity nearest(Feather f, List<? extends LivingEntity> foes) {
        LivingEntity best = null;
        double bestSq = aimRange * aimRange;
        for (LivingEntity e : foes) {
            if (isDown(e)) {
                continue;
            }
            double dx = e.getX() - f.x;
            double dy = e.getY() + e.getBbHeight() * 0.5 - f.y;
            double dz = e.getZ() - f.z;
            double d = dx * dx + dy * dy + dz * dz;
            if (d < bestSq) {
                bestSq = d;
                best = e;
            }
        }
        return best;
    }

    private void launchAt(Feather f, LivingEntity tgt, boolean charge) {
        f.state = State.CHASE;

        f.targetId = tgt.getId();

        f.bx = f.x;
        f.by = f.y;
        f.bz = f.z;
        f.anchored = false;

        f.rx = 0.0;
        f.ry = 0.0;
        f.rz = 0.0;

        if (charge) {
            f.life -= shotCost;
        }
    }

    public boolean covers(Entity e) {
        if (cells.isEmpty()) {
            return false;
        }
        AABB box = e.getBoundingBox();
        int x0 = Mth.floor(box.minX), x1 = Mth.floor(box.maxX);
        int y0 = Mth.floor(box.minY), y1 = Mth.floor(box.maxY);
        int z0 = Mth.floor(box.minZ), z1 = Mth.floor(box.maxZ);
        for (int bx = x0; bx <= x1; bx++) {
            for (int by = y0; by <= y1; by++) {
                for (int bz = z0; bz <= z1; bz++) {
                    if (cells.containsKey(BlockPos.asLong(bx, by, bz))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static AABB searchBox(double cx, double topY, double cz, double radius) {
        return new AABB(cx - radius, topY - START_SPREAD - MAX_DROP - 1.0, cz - radius,
                        cx + radius, topY + 1.0, cz + radius);
    }

    public boolean isEmpty() {
        return feathers.isEmpty();
    }

    public int size() {
        return feathers.size();
    }

    public void clear() {
        feathers.clear();
        cells.clear();
        struck.clear();
    }

    private static long cellOf(Feather f) {
        return BlockPos.asLong(Mth.floor(f.x), Mth.floor(f.y), Mth.floor(f.z));
    }

    private void acquire(Feather f, long cell) {
        f.cell = cell;
        cells.merge(cell, 1, Integer::sum);
    }

    private void release(Feather f) {
        if (f.cell == Long.MIN_VALUE) {
            return;
        }
        Integer n = cells.get(f.cell);
        if (n != null) {
            if (n <= 1) {
                cells.remove(f.cell);
            } else {
                cells.put(f.cell, n - 1);
            }
        }
        f.cell = Long.MIN_VALUE;
    }
}
