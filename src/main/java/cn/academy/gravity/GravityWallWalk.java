package cn.academy.gravity;

import cn.academy.AcademyCraft;
import cn.academy.network.GravitySyncMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = AcademyCraft.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GravityWallWalk {

    private static final Set<UUID> ENABLED = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Integer> COOLDOWN = new ConcurrentHashMap<>();
    private static final int COOLDOWN_TICKS = 3;
    private static final double PROBE = 0.25;

    private GravityWallWalk() {}

    public static boolean toggle(ServerPlayer p) {
        UUID id = p.getUUID();
        if (ENABLED.remove(id)) {
            COOLDOWN.remove(id);
            ACGravity.setGravityDirection(p, Direction.DOWN, true);
            GravitySyncMessage.sync(p, Direction.DOWN, true);
            return false;
        }
        ENABLED.add(id);
        return true;
    }

    public static boolean isEnabled(ServerPlayer p) {
        return ENABLED.contains(p.getUUID());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (!(e.player instanceof ServerPlayer p)) return;
        UUID id = p.getUUID();
        if (!ENABLED.contains(id)) return;

        if (p.isShiftKeyDown()) return;

        if (cn.academy.ability.vanilla.electromaster.MagMovement.isControllingMotion(id)) return;

        Integer cd = COOLDOWN.get(id);
        if (cd != null) {
            if (cd <= 1) COOLDOWN.remove(id); else COOLDOWN.put(id, cd - 1);
            return;
        }

        Direction g = ACGravity.getGravityDirection(p);
        Direction wall = findWall(p, g);
        if (wall == null || wall == g) return;
        ACGravity.setGravityDirection(p, wall, true);
        GravitySyncMessage.sync(p, wall, true);
        COOLDOWN.put(id, COOLDOWN_TICKS);
    }

    private static Direction findWall(ServerPlayer p, Direction g) {

        Vec3 localFwd = RotationUtil.rotToVec(p.getYRot(), 0.0f);
        Vec3 worldFwd = RotationUtil.vecPlayerToWorld(localFwd, g);
        Direction facing = Direction.getNearest(worldFwd.x, worldFwd.y, worldFwd.z);
        if (facing.getAxis() != g.getAxis() && isAgainstWall(p, facing, g)) return facing;

        for (Direction d : Direction.values()) {
            if (d.getAxis() == g.getAxis()) continue;
            if (isAgainstWall(p, d, g)) return d;
        }
        return null;
    }

    private static boolean isAgainstWall(ServerPlayer p, Direction d, Direction g) {
        Level lvl = p.level();
        AABB box = p.getBoundingBox();

        double sx = g.getAxis() == Direction.Axis.X ? 0.1 : 0.0;
        double sy = g.getAxis() == Direction.Axis.Y ? 0.1 : 0.0;
        double sz = g.getAxis() == Direction.Axis.Z ? 0.1 : 0.0;
        AABB probe = box.deflate(sx, sy, sz)
                .expandTowards(d.getStepX() * PROBE, d.getStepY() * PROBE, d.getStepZ() * PROBE);
        if (lvl.noCollision(p, probe)) return false;

        Direction up = g.getOpposite();
        BlockPos base = p.blockPosition();
        int h = Math.max(1, Mth.ceil(p.getBbHeight()));
        for (int i = 0; i < h; i++) {
            BlockPos wp = base.relative(up, i).relative(d);
            if (lvl.getBlockState(wp).getCollisionShape(lvl, wp).isEmpty()) return false;
        }
        return true;
    }
}
