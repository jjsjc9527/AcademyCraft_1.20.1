package cn.academy.block.block;

import cn.academy.block.tileentity.ImagPhaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class ImagPhaseBlock extends LiquidBlock implements EntityBlock {

    public ImagPhaseBlock(Supplier<? extends FlowingFluid> fluid, Properties props) {
        super(fluid, props);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ImagPhaseBlockEntity(pos, state);
    }
}
