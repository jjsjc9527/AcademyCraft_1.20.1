package cn.academy.ability.vanilla.mentalout;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Collections;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class ControlPath {

    private static final int REPLAN_INTERVAL = 10;

    private static final int FAIL_COOLDOWN = 30;

    private static final double GOAL_DRIFT = 2.5;

    private static final double STRAY_DIST = 4.0;

    private static final double ARRIVE = 0.7;

    private static final double LOOKAHEAD = 2.0;

    private List<ControlPathfinder.Step> steps = Collections.emptyList();
    private int cursor;
    private Vec3 plannedFor;
    private long nextPlanAt;

    private boolean lookahead;

    public void clear() {
        steps = Collections.emptyList();
        cursor = 0;
        plannedFor = null;
        nextPlanAt = 0L;
        lookahead = false;
    }

    public void update(LocalPlayer p, Vec3 finalGoal) {
        long now = p.level().getGameTime();
        advance(p);
        boolean exhausted = cursor >= steps.size();
        boolean drifted = plannedFor == null
                || plannedFor.distanceToSqr(finalGoal) > GOAL_DRIFT * GOAL_DRIFT;
        if ((exhausted || drifted || strayed(p)) && now >= nextPlanAt) {
            replan(p, finalGoal);
            nextPlanAt = now + (steps.isEmpty() ? FAIL_COOLDOWN : REPLAN_INTERVAL);
        }
        lookahead = canLookAhead(p);
    }

    private boolean canLookAhead(LocalPlayer p) {
        if (cursor + 1 >= steps.size()
                || steps.get(cursor).move() != ControlPathfinder.Move.WALK
                || steps.get(cursor + 1).move() != ControlPathfinder.Move.WALK) {
            return false;
        }
        Vec3 w = centerOf(steps.get(cursor).pos());
        if (w.distanceToSqr(p.position()) >= LOOKAHEAD * LOOKAHEAD) {
            return false;
        }
        return ControlPathfinder.lineClear(p.level(), p.blockPosition(),
                steps.get(cursor + 1).pos());
    }

    private void replan(LocalPlayer p, Vec3 finalGoal) {
        BlockPos from = p.blockPosition();
        BlockPos to = BlockPos.containing(finalGoal);
        steps = ControlPathfinder.find(p.level(), from, to);
        cursor = 0;
        plannedFor = finalGoal;
    }

    private void advance(LocalPlayer p) {
        while (cursor < steps.size()) {
            Vec3 w = centerOf(steps.get(cursor).pos());
            double dx = w.x - p.getX(), dz = w.z - p.getZ();

            if (dx * dx + dz * dz > ARRIVE * ARRIVE || Math.abs(w.y - p.getY()) > 1.6) {
                return;
            }
            cursor++;
        }
    }

    private boolean strayed(LocalPlayer p) {
        if (cursor >= steps.size()) {
            return false;
        }
        Vec3 w = centerOf(steps.get(cursor).pos());
        double dx = w.x - p.getX(), dz = w.z - p.getZ();
        return dx * dx + dz * dz > STRAY_DIST * STRAY_DIST;
    }

    public Vec3 aim(LocalPlayer p, Vec3 finalGoal) {
        if (cursor >= steps.size()) {
            return finalGoal;
        }

        return centerOf(steps.get(lookahead ? cursor + 1 : cursor).pos());
    }

    public ControlPathfinder.Move move() {
        return cursor < steps.size() ? steps.get(cursor).move() : ControlPathfinder.Move.WALK;
    }

    public int span() {
        return cursor < steps.size() ? steps.get(cursor).span() : 1;
    }

    public boolean active() {
        return cursor < steps.size();
    }

    private static Vec3 centerOf(BlockPos pos) {
        return new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
    }
}
