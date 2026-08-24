package cn.academy.block.tileentity;

import cn.academy.ACBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class ImagPhaseBlockEntity extends BlockEntity {

    public ImagPhaseBlockEntity(BlockPos pos, BlockState state) {
        super(ACBlockEntities.IMAG_PHASE.get(), pos, state);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(getBlockPos()).inflate(0.5, 1.0, 0.5);
    }
}
