package cn.lambdalib2.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;

public final class WorldUtils {

    private WorldUtils() {}

    public static void getBlocksWithin(List<BlockPos> outList, Level world,
                                       double x, double y, double z, double range, int max,
                                       IBlockSelector... filters) {
        outList.clear();
        int r = (int) Math.ceil(range);
        int cx = (int) Math.floor(x), cy = (int) Math.floor(y), cz = (int) Math.floor(z);
        double rangeSq = range * range;

        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    int bx = cx + dx, by = cy + dy, bz = cz + dz;
                    if (MathUtils.distanceSq(x, y, z, bx + 0.5, by + 0.5, bz + 0.5) > rangeSq) {
                        continue;
                    }
                    BlockPos pos = new BlockPos(bx, by, bz);
                    Block block = world.getBlockState(pos).getBlock();
                    boolean ok = true;
                    for (IBlockSelector f : filters) {
                        if (!f.accepts(world, bx, by, bz, block)) {
                            ok = false;
                            break;
                        }
                    }
                    if (ok) {
                        outList.add(pos);
                        if (outList.size() >= max) {
                            return;
                        }
                    }
                }
            }
        }
    }
}
