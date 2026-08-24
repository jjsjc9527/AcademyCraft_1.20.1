package cn.academy.ability.vanilla.mentalout;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public final class ControlPathfinder {

    public enum Move { WALK, JUMP, DROP, LEAP }

    public record Step(BlockPos pos, Move move, int span) {}

    private static final int MAX_NODES = 900;

    private static final int MAX_RANGE = 28;

    private static final int MAX_DROP = 3;

    private static final int MAX_LEAP = 4;

    private static final double LEAP_RISK = 0.3;

    private static final double LEAP_SPAN_PENALTY = 2.0;

    private static final double JUMP_COST = 0.6;

    private static final double DROP_COST = 0.3;

    private static final int STRAIGHTEN_WINDOW = 12;

    private static final double LINE_SAMPLE = 0.25;

    private static final double HALF_WIDTH = 0.3;

    private static final int[][] DIRS = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1},
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1},
    };

    private ControlPathfinder() {}

    private static BlockPathTypes typeAt(BlockGetter level, BlockPos.MutableBlockPos cursor,
                                         int x, int y, int z) {
        return WalkNodeEvaluator.getBlockPathTypeStatic(level, cursor.set(x, y, z));
    }

    private static boolean canPass(BlockGetter level, BlockPos.MutableBlockPos cur, BlockPos p) {
        return typeAt(level, cur, p.getX(), p.getY(), p.getZ()).getMalus() >= 0f;
    }

    private static boolean canStand(BlockGetter level, BlockPos.MutableBlockPos cur, BlockPos p) {
        if (typeAt(level, cur, p.getX(), p.getY(), p.getZ()) != BlockPathTypes.WALKABLE) {
            return false;
        }
        return typeAt(level, cur, p.getX(), p.getY() + 1, p.getZ()).getMalus() >= 0f;
    }

    private static boolean canJumpUp(BlockGetter level, BlockPos.MutableBlockPos cur, BlockPos p) {
        return typeAt(level, cur, p.getX(), p.getY() + 2, p.getZ()).getMalus() >= 0f;
    }

    private static double malusOf(BlockGetter level, BlockPos.MutableBlockPos cur, BlockPos p) {
        return Math.max(0f, typeAt(level, cur, p.getX(), p.getY(), p.getZ()).getMalus());
    }

    private static final class Node implements Comparable<Node> {
        final BlockPos pos;
        Move move = Move.WALK;
        int span;
        Node parent;
        double g = Double.MAX_VALUE;
        double f = Double.MAX_VALUE;
        boolean closed;

        Node(BlockPos pos) {
            this.pos = pos;
        }

        @Override
        public int compareTo(Node o) {
            return Double.compare(this.f, o.f);
        }
    }

    private static double heuristic(BlockPos a, BlockPos b) {
        double dx = a.getX() - b.getX(), dy = a.getY() - b.getY(), dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dz * dz) + Math.abs(dy) * DROP_COST;
    }

    public static List<Step> find(BlockGetter level, BlockPos start, BlockPos goal) {
        BlockPos.MutableBlockPos cur = new BlockPos.MutableBlockPos();
        BlockPos target = resolveGoal(level, cur, goal);
        Map<Long, Node> all = new HashMap<>();
        PriorityQueue<Node> open = new PriorityQueue<>();

        Node from = new Node(start);
        from.g = 0;
        from.f = heuristic(start, target);
        all.put(start.asLong(), from);
        open.add(from);

        Node best = from;
        double bestH = from.f;
        int visited = 0;

        while (!open.isEmpty() && visited < MAX_NODES) {
            Node node = open.poll();
            if (node.closed) {
                continue;
            }
            node.closed = true;
            visited++;

            if (node.pos.equals(target)) {
                return build(level, cur, start, node);
            }
            double h = heuristic(node.pos, target);
            if (h < bestH) {
                bestH = h;
                best = node;
            }
            expand(level, cur, node, start, target, all, open);
        }

        return build(level, cur, start, best);
    }

    private static BlockPos resolveGoal(BlockGetter level, BlockPos.MutableBlockPos cur,
                                        BlockPos goal) {
        if (canStand(level, cur, goal)) {
            return goal;
        }
        for (int k = 1; k <= MAX_DROP + 2; k++) {
            BlockPos down = goal.below(k);
            if (canStand(level, cur, down)) {
                return down;
            }
        }
        for (int k = 1; k <= 2; k++) {
            BlockPos up = goal.above(k);
            if (canStand(level, cur, up)) {
                return up;
            }
        }
        return goal;
    }

    private static void expand(BlockGetter level, BlockPos.MutableBlockPos cur, Node node,
                               BlockPos origin, BlockPos target,
                               Map<Long, Node> all, PriorityQueue<Node> open) {
        BlockPos p = node.pos;
        for (int[] d : DIRS) {
            boolean diagonal = d[0] != 0 && d[1] != 0;
            double base = diagonal ? 1.414 : 1.0;
            if (diagonal) {

                if (!canPass(level, cur, p.offset(d[0], 0, 0))
                        || !canPass(level, cur, p.offset(0, 0, d[1]))) {
                    continue;
                }
            }
            BlockPos flat = p.offset(d[0], 0, d[1]);
            if (Math.abs(flat.getX() - origin.getX()) > MAX_RANGE
                    || Math.abs(flat.getZ() - origin.getZ()) > MAX_RANGE) {
                continue;
            }

            if (canStand(level, cur, flat)) {

                push(node, flat, Move.WALK, 1, base + malusOf(level, cur, flat), target, all, open);
                continue;
            }

            boolean open1 = canPass(level, cur, flat);

            BlockPos up = flat.above();
            if (canStand(level, cur, up) && canJumpUp(level, cur, p)) {
                push(node, up, Move.JUMP, 1, base + JUMP_COST + malusOf(level, cur, up),
                        target, all, open);
            }

            if (!open1) {
                continue;
            }

            for (int k = 1; k <= MAX_DROP; k++) {
                BlockPos down = flat.below(k);
                if (!canPass(level, cur, flat.below(k - 1))) {
                    break;
                }
                if (canStand(level, cur, down)) {
                    push(node, down, Move.DROP, 1,
                            base + DROP_COST * k + malusOf(level, cur, down), target, all, open);
                    break;
                }
            }

            if (!diagonal && canJumpUp(level, cur, p)) {
                leap(level, cur, node, p, d, target, all, open);
            }
        }
    }

    private static void leap(BlockGetter level, BlockPos.MutableBlockPos cur, Node node,
                             BlockPos p, int[] d, BlockPos target,
                             Map<Long, Node> all, PriorityQueue<Node> open) {
        for (int n = 2; n <= MAX_LEAP; n++) {
            BlockPos mid = p.offset(d[0] * (n - 1), 0, d[1] * (n - 1));
            if (!canPass(level, cur, mid) || !canPass(level, cur, mid.above())) {
                return;
            }
            BlockPos far = p.offset(d[0] * n, 0, d[1] * n);
            if (!canPass(level, cur, far)) {
                return;
            }
            for (int k = 0; k <= 2; k++) {
                BlockPos land = far.below(k);
                if (canStand(level, cur, land)) {

                    double over = n - 2;
                    push(node, land, Move.LEAP, n,
                            n + LEAP_RISK + LEAP_SPAN_PENALTY * over * over
                                    + DROP_COST * k + malusOf(level, cur, land),
                            target, all, open);
                    return;
                }
            }
        }
    }

    private static void push(Node from, BlockPos to, Move move, int span, double cost,
                             BlockPos target, Map<Long, Node> all, PriorityQueue<Node> open) {
        long key = to.asLong();
        Node n = all.get(key);
        if (n == null) {
            n = new Node(to);
            all.put(key, n);
        } else if (n.closed) {
            return;
        }
        double g = from.g + cost;
        if (g >= n.g) {
            return;
        }
        n.parent = from;
        n.move = move;
        n.span = span;
        n.g = g;
        n.f = g + heuristic(to, target);
        open.add(n);
    }

    private static List<Step> build(BlockGetter level, BlockPos.MutableBlockPos cur,
                                    BlockPos start, Node end) {
        List<Step> out = new ArrayList<>();
        for (Node n = end; n.parent != null; n = n.parent) {
            out.add(new Step(n.pos, n.move, n.span));
        }
        Collections.reverse(out);
        return straighten(level, cur, start, out);
    }

    private static List<Step> straighten(BlockGetter level, BlockPos.MutableBlockPos cur,
                                         BlockPos start, List<Step> raw) {
        if (raw.size() < 2) {
            return raw;
        }
        List<Step> out = new ArrayList<>(raw.size());
        BlockPos from = start;
        int i = 0;
        while (i < raw.size()) {
            if (raw.get(i).move() != Move.WALK) {
                out.add(raw.get(i));
                from = raw.get(i).pos();
                i++;
                continue;
            }

            int last = i;
            while (last + 1 < raw.size() && raw.get(last + 1).move() == Move.WALK
                    && last + 1 - i < STRAIGHTEN_WINDOW) {
                last++;
            }
            int far = i;
            for (int j = last; j > i; j--) {
                if (walkableLine(level, cur, from, raw.get(j).pos())) {
                    far = j;
                    break;
                }
            }
            out.add(raw.get(far));
            from = raw.get(far).pos();
            i = far + 1;
        }
        return out;
    }

    private static boolean walkableLine(BlockGetter level, BlockPos.MutableBlockPos cur,
                                        BlockPos a, BlockPos b) {
        if (a.getY() != b.getY()) {
            return false;
        }
        double ax = a.getX() + 0.5, az = a.getZ() + 0.5;
        double dx = (b.getX() + 0.5) - ax, dz = (b.getZ() + 0.5) - az;
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1.0e-6) {
            return true;
        }
        double ux = dx / len, uz = dz / len;
        double px = -uz * HALF_WIDTH, pz = ux * HALF_WIDTH;
        int steps = (int) Math.ceil(len / LINE_SAMPLE);
        for (int k = 1; k <= steps; k++) {
            double t = len * k / steps;
            double cx = ax + ux * t, cz = az + uz * t;
            if (!standsAt(level, cur, cx, a.getY(), cz)
                    || !standsAt(level, cur, cx + px, a.getY(), cz + pz)
                    || !standsAt(level, cur, cx - px, a.getY(), cz - pz)) {
                return false;
            }
        }
        return true;
    }

    private static boolean standsAt(BlockGetter level, BlockPos.MutableBlockPos cur,
                                    double x, int y, double z) {
        return canStand(level, cur, BlockPos.containing(x, y, z));
    }

    public static boolean lineClear(BlockGetter level, BlockPos from, BlockPos to) {
        return walkableLine(level, new BlockPos.MutableBlockPos(), from, to);
    }
}
