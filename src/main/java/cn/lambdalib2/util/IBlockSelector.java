package cn.lambdalib2.util;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public interface IBlockSelector {

    boolean accepts(Level world, int x, int y, int z, Block block);
}
